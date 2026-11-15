## Add the 5 extra report queries to the app and switch the frontend to HTTP mode

### Context
The app runs on the team's database script **`Sua moi nhat checked 16-11.sql`** (all 20 tables + data). On top of the 5 required topic queries, we add 5 extra queries that give meaningful results on our data. All 5 are in **`app_extra_queries.sql`** and were tested on MySQL 8.0 against that script.

| # | Feature | SQL (in `app_extra_queries.sql`) | Expected result on our data |
|---|---|---|---|
| 1 | Overstocked products (≥ 3 × reorder level) | Query 15, uses view `PRODUCT_STOCK` | 11 products, e.g. Receipt paper roll 235 vs reorder level 45 |
| 2 | Slow-moving stock (no goods issue in the last 90 days) | Query 13 | 12 products, e.g. Staples 190 |
| 3 | Customer orders waiting to ship (no goods issue yet) | Query 11 | 17 orders (CO11–CO30 except CO17, CO22, CO27) |
| 4 | Top 10 customers by units shipped | Query 7 | Cristiano Ronaldo 83, Erling Haaland 49, Stephen Curry 35, … |
| 5 | Full activity history of one product | Query 17 (UNION) | P03 (Paper A4): 8 rows, in date order |

---

### Backend

**1. Publish the database files in `database/`**
- [ ] Add the newest database script `Sua moi nhat checked 16-11.sql`.
- [ ] Add the view `PRODUCT_STOCK` (the `CREATE OR REPLACE VIEW` at the top of `app_extra_queries.sql`) to the end of the database script, so the app only runs a `SELECT` on it.
- [ ] Add `app_extra_queries.sql` (the 5 queries).
- [ ] Set `DB_URL` to the database the script creates: `jdbc:mysql://localhost:3306/WarehouseManagement` (the default in `application.properties` is still `warehouse_db`).

**2. Add 5 read-only endpoints**

| # | Endpoint | Parameters | Response (suggested) |
|---|---|---|---|
| 1 | `GET /api/reports/overstock` | – | `{ items: [{ productId, productName, unit, reorderLevel, totalQuantity, numWarehouses }] }` |
| 2 | `GET /api/reports/slow-moving?asOf=2026-10-18` | `asOf` (date, default `2026-10-18`) | `{ asOf, items: [{ productId, productName, onHand }] }` |
| 3 | `GET /api/orders/awaiting-issue` | – | `{ items: [{ orderId, time, status, customerName }] }` |
| 4 | `GET /api/reports/top-customers` | – | `{ items: [{ customerId, customerName, unitsShipped }] }` |
| 5 | `GET /api/products/{id}/history` | `id` = product ID | `{ productId, items: [{ activityDate, type, documentId, quantity, location }] }` |

- [ ] Copy the SQL from `app_extra_queries.sql`. Do not rewrite the logic.
- [ ] Use `PreparedStatement` with `?` for every input: `@today` → `?` in #2, `@p` → `?` (3 times, same product ID) in #5. Never build the SQL by joining strings.
- [ ] Return `404` with a readable message if the product in #5 does not exist.
- [ ] Field names in camelCase, like the existing endpoints (`productId`, `totalQuantity`, …).

---

### Frontend

**1. Add the 5 features**
- [ ] **Reports** (`pages/reports.js`): add two cards next to "Low stock":
  - **Overstocked products**, from #1
  - **Slow-moving stock**, from #2, using the same "as of" date as the other reports (default `2026-10-18`)
- [ ] **Customer orders** (`pages/directories.js`, `ordersPage`): add a **"Waiting to ship"** list, from #3.
- [ ] **Dashboard** (`pages/dashboard.js`): add a **Top 10 customers** bar chart, from #4.
- [ ] **Inventory / product detail** (`pages/inventory.js`): add an **Activity history** table for the selected product, from #5. Receipts show a positive quantity and issues a negative one.
- [ ] Add a normalizer for each response in `api/contracts.js`, and load the new reports in `services/warehouse.js`, the same way `loadReports()` loads the 4 existing ones.

**2. Switch to HTTP mode**
- [ ] In `src/config.js` change `mode: 'demo'` to `mode: 'http'`.
- [ ] Update the status values to match the database. `contracts.js` currently only accepts the demo values, so in HTTP mode every order and task will fail validation:

  | Table | Frontend now | Database |
  |---|---|---|
  | Customer orders | `open`, `closed`, `cancelled` | `Pending`, `Shipped`, `Delivered`, `Cancelled` |
  | Tasks | `todo`, `doing`, `done` | `Pending`, `In Progress`, `Completed` |

  Update `orderLabels` / `taskLabels` in `config.js` and the `choice(...)` lists in `contracts.js` (lines 178 and 239).

**Order of work:** switch to HTTP only after the backend endpoints for a page exist. Otherwise that page shows "Unable to load". The 5 new features can be built and tested first.

---

### Done when
- [ ] Every new card or list shows the expected result from the Context table above, with the as-of date `2026-10-18`.
- [ ] **Demo check 1:** creating a goods issue for an order in the app removes that order from "Waiting to ship" after a reload.
- [ ] **Demo check 2:** creating a goods receipt or issue for a product adds a new row to that product's Activity history.
- [ ] **SQL injection check:** entering `P03' OR '1'='1` as the product ID in #5 returns 404, not data.
- [ ] The app runs in HTTP mode with no "Unable to load" errors on Reports, Orders, Dashboard and Inventory.

---

**IF ANY PROBLEM WERE TO BE FOUND, CONTACT ME ASAP.**
