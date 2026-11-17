-- ============================================================
-- Warehouse Management: 5 extra queries for the app
-- Run on the WarehouseManagement database (Suamoinhat.sql), MySQL 8.0+.
--
-- In the backend: copy the SELECT into a PreparedStatement and replace
-- @today / @p with a ? parameter (ps.setDate / ps.setString).
-- @today is fixed at 2026-10-18 here so the results match the mock data;
-- in the app it comes from the "as of" date picker.
-- ============================================================
USE WarehouseManagement;


-- ============================================================
-- 1. Overstocked products (Query 15 in warehouse_queries.md)
-- Page:     Reports, next to the low-stock card
-- Endpoint: GET /api/reports/overstock
-- Result:   11 products, e.g. Receipt paper roll 235 vs reorder level 45,
--           Paper A4 425 vs 100
-- Note:     the view only needs to be created once (put it in Suamoinhat.sql
--           so the app can just SELECT from it).
-- ============================================================
CREATE OR REPLACE VIEW PRODUCT_STOCK AS
SELECT p.Product_ID, p.Product_Name, p.Unit, p.Reorder_Level,
       COALESCE(SUM(pb.Quantity), 0)   AS Total_Qty,
       COUNT(DISTINCT pb.Warehouse_ID) AS Num_Warehouses
FROM   PRODUCT p
LEFT JOIN PRODUCT_BIN pb ON pb.Product_ID = p.Product_ID
GROUP BY p.Product_ID, p.Product_Name, p.Unit, p.Reorder_Level;

SELECT *
FROM   PRODUCT_STOCK
WHERE  Total_Qty >= 3 * Reorder_Level
ORDER BY Total_Qty / Reorder_Level DESC;


-- ============================================================
-- 2. Slow-moving stock: in stock, no goods issue in the last 90 days
--    (Query 13)
-- Page:     Reports, uses the same "as of" date as the other reports
-- Endpoint: GET /api/reports/slow-moving?asOf=2026-10-18
-- Result:   12 products, e.g. Staples 190, Mechanical Keyboard Switches 115,
--           Desk 105
-- App SQL:  replace both @today with ?
-- ============================================================
SET @today = DATE('2026-10-18');

SELECT p.Product_ID, p.Product_Name, SUM(pb.Quantity) AS On_Hand
FROM   PRODUCT p
JOIN   PRODUCT_BIN pb ON pb.Product_ID = p.Product_ID
WHERE  pb.Quantity > 0
  AND  NOT EXISTS (
         SELECT 1
         FROM   GOODS_ISSUE gi
         JOIN   GOODS_ISSUE_LINE gil ON gil.Issue_ID = gi.Issue_ID
         WHERE  gil.Product_ID = p.Product_ID
           AND  gi.Issue_Date >= DATE_SUB(@today, INTERVAL 90 DAY))
GROUP BY p.Product_ID, p.Product_Name
ORDER BY On_Hand DESC;


-- ============================================================
-- 3. Customer orders with no goods issue yet (Query 11)
-- Page:     Orders, as a "Waiting to ship" list
-- Endpoint: GET /api/orders/awaiting-issue
-- Result:   17 orders (CO11 to CO30), all Pending
-- Demo:     create a goods issue for one of them in the app, reload,
--           and the order disappears from this list
-- ============================================================
SELECT co.Order_ID, co.Time, co.Status, c.Name AS Customer_Name
FROM   CUSTOMER_ORDER co
JOIN   CUSTOMER c ON c.Customer_ID = co.Customer_ID
WHERE  NOT EXISTS (SELECT 1 FROM GOODS_ISSUE gi WHERE gi.Order_ID = co.Order_ID)
ORDER BY co.Time;


-- ============================================================
-- 4. Top 10 customers by units shipped (Query 7)
-- Page:     Dashboard, as a bar chart
-- Endpoint: GET /api/reports/top-customers
-- Result:   Cristiano Ronaldo 83, Erling Haaland 49, Stephen Curry 35, ...
-- ============================================================
SELECT c.Customer_ID, c.Name AS Customer_Name, SUM(gil.Quantity) AS Units_Shipped
FROM   CUSTOMER c
JOIN   CUSTOMER_ORDER co    ON co.Customer_ID = c.Customer_ID
JOIN   GOODS_ISSUE gi       ON gi.Order_ID    = co.Order_ID
JOIN   GOODS_ISSUE_LINE gil ON gil.Issue_ID   = gi.Issue_ID
GROUP BY c.Customer_ID, c.Name
ORDER BY Units_Shipped DESC
LIMIT 10;


-- ============================================================
-- 5. Full activity history of one product (Query 17)
-- Page:     Product detail, as a timeline
-- Endpoint: GET /api/products/{id}/history
-- Result:   P03 Paper A4: 3 movements, 3 receipts, 2 issues (8 rows)
-- Demo:     add a receipt or movement in the app, reload, and the new
--           row shows up at the bottom
-- App SQL:  replace the three @p with ?, ? and ? (same product ID 3 times)
-- ============================================================
SET @p = 'P03';

SELECT gr.Receipt_Date AS Activity_Date, 'Receipt' AS Type, gr.Receipt_ID AS Document_ID,
       grl.Quantity AS Quantity, grl.Warehouse_ID AS Location
FROM   GOODS_RECEIPT gr
JOIN   GOODS_RECEIPT_LINE grl ON grl.Receipt_ID = gr.Receipt_ID
WHERE  grl.Product_ID = @p
UNION
SELECT gi.Issue_Date, 'Issue', gi.Issue_ID, -gil.Quantity, gil.Warehouse_ID
FROM   GOODS_ISSUE gi
JOIN   GOODS_ISSUE_LINE gil ON gil.Issue_ID = gi.Issue_ID
WHERE  gil.Product_ID = @p
UNION
SELECT m.Movement_Date, 'Movement', m.Movement_ID, m.Quantity,
       CONCAT(m.From_Warehouse_ID, ' -> ', m.To_Warehouse_ID)
FROM   STOCK_MOVEMENT m
WHERE  m.Product_ID = @p
ORDER BY Activity_Date;
