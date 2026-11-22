package com.group4.warehouse.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.DocumentIds;
import com.group4.warehouse.common.References;
import com.group4.warehouse.common.Transactions;

/** All SQL for TASK. PreparedStatement parameters only, try-with-resources, readable errors. */
@Repository
public class TaskRepository {

    private static final String SELECT_TASKS = """
            SELECT t.Task_ID, t.Task_Name, t.Task_Status, t.Staff_ID, s.Staff_Name
            FROM TASK t
            JOIN STAFF s ON s.Staff_ID = t.Staff_ID
            """;

    private final DataSource dataSource;

    public TaskRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Browse + search: q matches task ID, task name or staff name. */
    public List<Task> findAll(String q) {
        String sql = SELECT_TASKS + """
                WHERE ? IS NULL OR t.Task_ID LIKE ? OR t.Task_Name LIKE ? OR s.Staff_Name LIKE ?
                ORDER BY t.Task_ID
                """;
        String pattern = q == null ? null : "%" + q + "%";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<Task> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(map(rs));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "task");
        }
    }

    public Optional<Task> findById(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_TASKS + " WHERE t.Task_ID = ?")) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "task");
        }
    }

    /** status null = the column DEFAULT ('Pending'). @return the task ID (generated if id is null) */
    public String insert(String id, String name, String staffId, String status) {
        return Transactions.run(dataSource, "task", conn -> {
            References.require(conn, "STAFF", "Staff_ID", staffId, "Staff member");
            String taskId = id != null ? id : DocumentIds.next(conn, "TASK", "Task_ID", "T");
            // Leaving Task_Status out of the INSERT lets DEFAULT 'Pending' fill it in.
            String sql = status == null
                    ? "INSERT INTO TASK (Task_ID, Task_Name, Staff_ID) VALUES (?, ?, ?)"
                    : "INSERT INTO TASK (Task_ID, Task_Name, Staff_ID, Task_Status) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, taskId);
                ps.setString(2, name);
                ps.setString(3, staffId);
                if (status != null) {
                    ps.setString(4, status);
                }
                ps.executeUpdate();
            }
            return taskId;
        });
    }

    /** null = keep. @return false if no task has this ID */
    public boolean update(String id, String name, String staffId, String status) {
        return Transactions.run(dataSource, "task", conn -> {
            if (staffId != null) {
                References.require(conn, "STAFF", "Staff_ID", staffId, "Staff member");
            }
            try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE TASK
                    SET Task_Name   = COALESCE(?, Task_Name),
                        Staff_ID    = COALESCE(?, Staff_ID),
                        Task_Status = COALESCE(?, Task_Status)
                    WHERE Task_ID = ?
                    """)) {
                ps.setString(1, name);
                ps.setString(2, staffId);
                ps.setString(3, status);
                ps.setString(4, id);
                return ps.executeUpdate() > 0;
            }
        });
    }

    /** @return false if no task has this ID */
    public boolean delete(String id) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM TASK WHERE Task_ID = ?")) {
            ps.setString(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "task");
        }
    }

    private static Task map(ResultSet rs) throws SQLException {
        return new Task(
                rs.getString("Task_ID"),
                rs.getString("Task_Name"),
                rs.getString("Task_Status"),
                rs.getString("Staff_ID"),
                rs.getString("Staff_Name"));
    }
}
