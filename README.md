# Warehouse Management System

IT079IU Principles of Database Management, Semester 1, 2026-2027
Group 4, Topic 21

A web application for managing the stock of several warehouses. Each warehouse is split into
zones, aisles and bins, and every quantity is tracked per product and bin. Staff record goods
receipts from suppliers, goods issues for customer orders, transfers between bins and stock
counts, and the application keeps the stock up to date.

## Project structure

| Folder | Contents |
|---|---|
| `database/` | MySQL scripts (see below) |
| `backend/` | Spring Boot REST API, Java 17, plain JDBC with `PreparedStatement`. See [backend/README.md](backend/README.md) |
| `frontend/` | HTML, CSS and JavaScript ES modules, no framework. See [frontend/README.md](frontend/README.md) |

### Database scripts

| File | What it is |
|---|---|
| `database/PDM_Database_Group4.sql` | The submitted SQL script. Run the whole file from top to bottom. Part 1 creates the database (20 tables, constraints, indexes, sample data), Part 2 has the 18 queries, Part 3 the INSERT, UPDATE and DELETE statements, Part 4 the indexes and the COMMIT / ROLLBACK transaction, and Part 5 the 32 constraint tests. |
| `database/warehouse.sql` | Part 1 on its own: the database the application uses, with clean sample data. |
| `database/queries.sql` | The extra queries behind the overstock, slow-moving, top customers, waiting orders and product history reports. |

The queries use a fixed date, 18/10/2026, so their results match the sample data.

## Features

- **Overview:** stock indicators, products to replenish, bin occupancy, recent activity and top customers.
- **Inventory:** product catalogue, stock by bin, search, filters, CSV export and a product's activity history.
- **Goods receipts, goods issues and stock counts:** create, edit and delete, with the stock rules applied in the backend.
- **Stock movements:** transfer stock between bins.
- **Locations:** warehouses, zones, aisles and bins.
- **Suppliers, staff, customers, customer orders and tasks:** create, edit and delete.
- **Reports:** products below reorder level, overstock, slow-moving stock, bins empty throughout last week, movements over the last month and the latest stock count differences.

## How to run

Requirements: MySQL 8.0.16 or newer, JDK 17 or newer, Node.js 24 or newer.

1. Create the database. Run `database/warehouse.sql` in MySQL Workbench, or from the project folder:

   ```
   mysql -u root -p -e "source database/warehouse.sql"
   ```

2. Start the backend on http://localhost:8080. In PowerShell:

   ```
   cd backend
   $env:DB_PASSWORD = 'your MySQL password'
   .\mvnw spring-boot:run
   ```

   On macOS or Linux, use `DB_PASSWORD=... ./mvnw spring-boot:run`. Open http://localhost:8080/api/health and check that it shows `"db":"up"`.

3. Start the frontend. In a second terminal, from the project folder:

   ```
   npm ci
   npm start
   ```

   Open http://127.0.0.1:5173.

Running `PDM_Database_Group4.sql` changes the sample data (Parts 3 and 4 add, edit and delete rows). Run `warehouse.sql` again before using the application.

## Team

| Name | Student ID | Role |
|---|---|---|
| Nguyen Chi Toan | ITCSIU24087 | Team leader, Normalization |
| Phan Hoang Khang | ITITIU24079 | Database developer |
| To Nhat Phat | MAMAIU25023 | Database developer |
| Nguyen Duc Dai | ITCSIU22180 | Java developer |
| Le Minh Dat | ITDSIU24012 | Normalization |
| Phan Quang Phu | ITITIU21281 | Frontend developer |
| Nguyen Minh Quang | ITCSIU24071 | Data modeller |
| Le Nguyen Truc Phuong | ITDSIU25029 | Query developer |
| Hoang Thi Cam Tu | ITITWE24071 | Java developer |
| Tran Ngoc Toan | MAMAIU25033 | Tester and writer |
| Tran Quoc Tri | MAMAIU25035 | Data modeller |
