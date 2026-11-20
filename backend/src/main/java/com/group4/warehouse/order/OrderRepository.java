package com.group4.warehouse.order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.stereotype.Repository;

import com.group4.warehouse.common.DatabaseErrors;
import com.group4.warehouse.common.IsoTime;

/** SQL for customer orders. */
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
}
