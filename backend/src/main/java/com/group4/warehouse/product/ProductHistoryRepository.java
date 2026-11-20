package com.group4.warehouse.product;

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

/** Query 17 in database/queries.sql: the full activity history of one product. */
@Repository
public class ProductHistoryRepository {

    /*
     * Three SELECTs with the same 5 columns, stacked with UNION ALL:
     *   receipts  (+quantity, warehouse)
     *   issues    (-quantity, warehouse)
     *   movements (quantity, "from -> to")
     * UNION ALL instead of UNION: UNION removes rows that are exactly equal, so two lines of
     * the same receipt with the same quantity into the same warehouse (different bins)
     * would show as one. UNION ALL keeps every line and is also faster (no duplicate check).
     * The product ID is used 3 times, so it is bound to 3 "?" placeholders.
     */
    private static final String SQL = """
            SELECT gr.Receipt_Date AS Activity_Date, 'Receipt' AS Type, gr.Receipt_ID AS Document_ID,
                   grl.Quantity AS Quantity, grl.Warehouse_ID AS Location
            FROM   GOODS_RECEIPT gr
            JOIN   GOODS_RECEIPT_LINE grl ON grl.Receipt_ID = gr.Receipt_ID
            WHERE  grl.Product_ID = ?
            UNION ALL
            SELECT gi.Issue_Date, 'Issue', gi.Issue_ID, -gil.Quantity, gil.Warehouse_ID
            FROM   GOODS_ISSUE gi
            JOIN   GOODS_ISSUE_LINE gil ON gil.Issue_ID = gi.Issue_ID
            WHERE  gil.Product_ID = ?
            UNION ALL
            SELECT m.Movement_Date, 'Movement', m.Movement_ID, m.Quantity,
                   CONCAT(m.From_Warehouse_ID, ' -> ', m.To_Warehouse_ID)
            FROM   STOCK_MOVEMENT m
            WHERE  m.Product_ID = ?
            ORDER BY Activity_Date, Type, Document_ID
            """;

    private final DataSource dataSource;

    public ProductHistoryRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<ProductActivity> findByProduct(String productId) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setString(1, productId);
            ps.setString(2, productId);
            ps.setString(3, productId);
            try (ResultSet rs = ps.executeQuery()) {
                List<ProductActivity> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new ProductActivity(
                            IsoTime.date(rs, "Activity_Date"),
                            rs.getString("Type"),
                            rs.getString("Document_ID"),
                            rs.getInt("Quantity"),
                            rs.getString("Location")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw DatabaseErrors.translate(e, "product history");
        }
    }
}
