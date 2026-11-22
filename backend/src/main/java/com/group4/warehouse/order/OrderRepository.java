package com.group4.warehouse.order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.DocumentIds;
import com.group4.warehouse.common.IsoTime;
import com.group4.warehouse.common.References;
import com.group4.warehouse.common.Transactions;

/**
 * SQL for customer orders (CUSTOMER_ORDER). Creating or editing an order does not change stock;
 * stock leaves through the goods issue of the order.
 */
@Repository
public class OrderRepository {

    private final DataSource dataSource;

    public OrderRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Query 11 in database/queries.sql: orders with no goods issue yet.
     * NOT EXISTS = "there is no GOODS_ISSUE row for this order" (an anti-join);
     * once an issue is created for an order, the order leaves this list.
     */
    public List<WaitingOrder> findAwaitingIssue() {
        String sql = """
                SELECT co.Order_ID, co.Time, co.Status, c.Name AS Customer_Name
                FROM   CUSTOMER_ORDER co
                JOIN   CUSTOMER c ON c.Customer_ID = co.Customer_ID
                WHERE  NOT EXISTS (SELECT 1 FROM GOODS_ISSUE gi WHERE gi.Order_ID = co.Order_ID)
                ORDER BY co.Time, co.Order_ID
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<WaitingOrder> result = new ArrayList<>();
            while (rs.next()) {
                result.add(new WaitingOrder(
                        rs.getString("Order_ID"),
                        IsoTime.date(rs, "Time"),
                        rs.getString("Status"),
                        rs.getString("Customer_Name")));
            }
            return result;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer order");
        }
    }

    private static final String SELECT_ORDERS = """
            SELECT co.Order_ID, co.Time, co.Status, co.Customer_ID, c.Name AS Customer_Name
            FROM CUSTOMER_ORDER co
            JOIN CUSTOMER c ON c.Customer_ID = co.Customer_ID
            """;

    /** Browse + search: q matches order ID or customer name. Newest first. */
    public List<CustomerOrder> findAll(String q) {
        String sql = SELECT_ORDERS + """
                WHERE ? IS NULL OR co.Order_ID LIKE ? OR c.Name LIKE ?
                ORDER BY co.Time DESC, co.Order_ID DESC
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<CustomerOrder> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer order");
        }
    }

    public Optional<CustomerOrder> findById(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_ORDERS + " WHERE co.Order_ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer order");
        }
    }

    /** status null = the column DEFAULT ('Pending'). @return the order ID (generated if id is null) */
    public String insert(String id, LocalDateTime time, String status, String customerId) {
        return Transactions.run(dataSource, "customer order", conn -> {
            References.require(conn, "CUSTOMER", "Customer_ID", customerId, "Customer");
            String orderId = id != null ? id : DocumentIds.next(conn, "CUSTOMER_ORDER", "Order_ID", "CO");
            String sql = status == null
                    ? "INSERT INTO CUSTOMER_ORDER (Order_ID, `Time`, Customer_ID) VALUES (?, ?, ?)"
                    : "INSERT INTO CUSTOMER_ORDER (Order_ID, `Time`, Customer_ID, Status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, orderId);
                ps.setObject(2, time);
                ps.setString(3, customerId);
                if (status != null) {
                    ps.setString(4, status);
                }
                ps.executeUpdate();
            }
            return orderId;
        });
    }

    /** null = keep. @return false if no order has this ID */
    public boolean update(String id, LocalDateTime time, String status, String customerId) {
        return Transactions.run(dataSource, "customer order", conn -> {
            if (customerId != null) {
                References.require(conn, "CUSTOMER", "Customer_ID", customerId, "Customer");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE CUSTOMER_ORDER
                    SET `Time`      = COALESCE(?, `Time`),
                        Status      = COALESCE(?, Status),
                        Customer_ID = COALESCE(?, Customer_ID)
                    WHERE Order_ID = ?
                    """)) {
                ps.setObject(1, time);
                ps.setString(2, status);
                ps.setString(3, customerId);
                ps.setString(4, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /**
     * An order that already has a goods issue cannot be deleted: FK_ISSUE_ORDER is ON DELETE
     * RESTRICT, and DatabaseErrors turns that into a readable 409 IN_USE message.
     * @return false if no order has this ID
     */
    public boolean delete(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM CUSTOMER_ORDER WHERE Order_ID = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "customer order");
        }
    }

    private static CustomerOrder map(ResultSet rs) throws SQLException {
        return new CustomerOrder(
                rs.getString("Order_ID"),
                IsoTime.dateTime(rs, "Time"),
                rs.getString("Status"),
                rs.getString("Customer_ID"),
                rs.getString("Customer_Name"));
    }
}
