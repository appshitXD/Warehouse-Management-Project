package com.group4.warehouse.staff;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.ApiException;
import com.group4.warehouse.common.DatabaseErrors;

/**
 * SQL for STAFF and STAFF_PHONE.
 *
 * A staff member is stored in two tables, so create and update run in ONE transaction:
 * either the person and all phones are saved, or nothing is (no staff without phones,
 * no phones without staff). Delete needs no transaction: ON DELETE CASCADE on
 * STAFF_PHONE removes the phones in the same statement.
 */
@Repository
public class StaffRepository {

    private static final Logger log = LoggerFactory.getLogger(StaffRepository.class);

    private static final int NO_PARENT_ROW = 1452;
    private static final int ROW_IS_REFERENCED = 1451;

    // LEFT JOIN: a person still appears if (by some old data) they have no phone.
    private static final String SELECT_WITH_PHONES = """
            SELECT s.Staff_ID, s.Staff_Name, s.Staff_Address, s.Warehouse_ID, s.Supervisor_ID, sp.Phone
            FROM STAFF s
            LEFT JOIN STAFF_PHONE sp ON sp.Staff_ID = s.Staff_ID
            """;

    private final DataSource dataSource;

    public StaffRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** q searches ID, name and phone; warehouseId filters. Both optional (null = no filter). */
    public List<Staff> findAll(String q, String warehouseId) {
        String sql = SELECT_WITH_PHONES + """
                WHERE (? IS NULL OR s.Warehouse_ID = ?)
                  AND (? IS NULL OR s.Staff_ID LIKE ? OR s.Staff_Name LIKE ?
                       OR EXISTS (SELECT 1 FROM STAFF_PHONE p WHERE p.Staff_ID = s.Staff_ID AND p.Phone LIKE ?))
                ORDER BY s.Staff_ID, sp.Phone
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, warehouseId);
            ps.setString(2, warehouseId);
            ps.setString(3, q);
            ps.setString(4, pattern);
            ps.setString(5, pattern);
            ps.setString(6, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return groupPhones(rs);
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "staff member");
        }
    }

    public Optional<Staff> findById(String id) {
        String sql = SELECT_WITH_PHONES + " WHERE s.Staff_ID = ? ORDER BY sp.Phone";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                List<Staff> found = groupPhones(rs);
                return found.isEmpty() ? Optional.empty() : Optional.of(found.get(0));
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "staff member");
        }
    }

    /** Supervisor of one person, used to detect cycles (A supervises B supervises A). */
    public Optional<String> findSupervisorId(String id) {
        String sql = "SELECT Supervisor_ID FROM STAFF WHERE Staff_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.ofNullable(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "staff member");
        }
    }

    /** INSERT STAFF + INSERT every phone, in one transaction. */
    public void insert(Staff staff) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = """
                        INSERT INTO STAFF (Staff_ID, Staff_Name, Staff_Address, Warehouse_ID, Supervisor_ID)
                        VALUES (?, ?, ?, ?, ?)
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, staff.id());
                    ps.setString(2, staff.name());
                    ps.setString(3, staff.address());
                    ps.setString(4, staff.warehouseId());
                    ps.setString(5, staff.supervisorId());
                    ps.executeUpdate();
                }
                insertPhones(conn, staff.id(), staff.phones());
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /**
     * UPDATE STAFF (only the fields sent) and, if phones != null, replace all phones,
     * in one transaction. The caller has already checked that the staff member exists.
     * For warehouseId / supervisorId: null = keep, "" = set NULL, other = new value.
     */
    public void update(String id, String name, String address, String warehouseId,
                       String supervisorId, List<String> phones) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                String sql = """
                        UPDATE STAFF
                        SET Staff_Name    = COALESCE(?, Staff_Name),
                            Staff_Address = COALESCE(?, Staff_Address),
                            Warehouse_ID  = IF(? IS NULL, Warehouse_ID, NULLIF(?, '')),
                            Supervisor_ID = IF(? IS NULL, Supervisor_ID, NULLIF(?, ''))
                        WHERE Staff_ID = ?
                        """;
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, name);
                    ps.setString(2, address);
                    ps.setString(3, warehouseId);
                    ps.setString(4, warehouseId);
                    ps.setString(5, supervisorId);
                    ps.setString(6, supervisorId);
                    ps.setString(7, id);
                    ps.executeUpdate();
                }
                if (phones != null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM STAFF_PHONE WHERE Staff_ID = ?")) {
                        ps.setString(1, id);
                        ps.executeUpdate();
                    }
                    insertPhones(conn, id, phones);
                }
                conn.commit();
            } catch (SQLException | RuntimeException e) {
                rollbackQuietly(conn);
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw translate(e);
        }
    }

    /** Phones go with the person (ON DELETE CASCADE). Tasks, movements and supervised staff block it. */
    public boolean delete(String id) {
        String sql = "DELETE FROM STAFF WHERE Staff_ID = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            if (e.getErrorCode() == ROW_IS_REFERENCED) {
                throw new ApiException(HttpStatus.CONFLICT, "IN_USE",
                        "Staff member " + id + " cannot be deleted: " + references(id)
                                + ". Reassign or delete those first.", e);
            }
            throw translate(e);
        }
    }

    private static void insertPhones(Connection conn, String staffId, List<String> phones) throws SQLException {
        String sql = "INSERT INTO STAFF_PHONE (Staff_ID, Phone) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String phone : phones) {
                ps.setString(1, staffId);
                ps.setString(2, phone);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    /** Tells the user what blocks the delete, e.g. "still has 2 task(s) and 1 stock movement(s)". */
    private String references(String id) {
        String sql = """
                SELECT (SELECT COUNT(*) FROM TASK           WHERE Staff_ID = ?)      AS tasks,
                       (SELECT COUNT(*) FROM STOCK_MOVEMENT WHERE Staff_ID = ?)      AS movements,
                       (SELECT COUNT(*) FROM STAFF          WHERE Supervisor_ID = ?) AS supervised
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, id);
            ps.setString(3, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                List<String> parts = new ArrayList<>();
                if (rs.getInt("tasks") > 0) {
                    parts.add(rs.getInt("tasks") + " task(s)");
                }
                if (rs.getInt("movements") > 0) {
                    parts.add(rs.getInt("movements") + " stock movement(s)");
                }
                if (rs.getInt("supervised") > 0) {
                    parts.add(rs.getInt("supervised") + " supervised staff member(s)");
                }
                if (parts.isEmpty()) {
                    return "other records still use it";
                }
                // "still has 1 task(s), 3 stock movement(s) and 8 supervised staff member(s)"
                String last = parts.remove(parts.size() - 1);
                return "still has " + (parts.isEmpty() ? last : String.join(", ", parts) + " and " + last);
            }
        } catch (SQLException e) {
            return "other records still use it";
        }
    }

    /** Readable messages for the foreign keys of STAFF; everything else goes to DatabaseErrors. */
    private static ApiException translate(SQLException e) {
        String text = e.getMessage() == null ? "" : e.getMessage();
        if (e.getErrorCode() == NO_PARENT_ROW && text.contains("FK_STAFF_WAREHOUSE")) {
            return new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                    "The selected warehouse does not exist.", e);
        }
        if (e.getErrorCode() == NO_PARENT_ROW && text.contains("FK_STAFF_SUPERVISOR")) {
            return new ApiException(HttpStatus.CONFLICT, "REFERENCE_NOT_FOUND",
                    "The selected supervisor does not exist.", e);
        }
        return DatabaseErrors.translate(e, "staff member");
    }

    /** The JOIN gives one row per phone; this folds them back into one Staff per person. */
    private static List<Staff> groupPhones(ResultSet rs) throws SQLException {
        Map<String, String[]> people = new LinkedHashMap<>();
        Map<String, List<String>> phones = new LinkedHashMap<>();
        while (rs.next()) {
            String id = rs.getString("Staff_ID");
            people.putIfAbsent(id, new String[] {
                    rs.getString("Staff_Name"), rs.getString("Staff_Address"),
                    rs.getString("Warehouse_ID"), rs.getString("Supervisor_ID")});
            List<String> list = phones.computeIfAbsent(id, k -> new ArrayList<>());
            String phone = rs.getString("Phone");
            if (phone != null) {
                list.add(phone);
            }
        }
        List<Staff> result = new ArrayList<>();
        people.forEach((id, p) -> result.add(new Staff(id, p[0], p[1], p[2], p[3], phones.get(id))));
        return result;
    }

    private static void rollbackQuietly(Connection conn) {
        try {
            conn.rollback();
        } catch (SQLException e) {
            log.error("Rollback failed", e);
        }
    }
}
