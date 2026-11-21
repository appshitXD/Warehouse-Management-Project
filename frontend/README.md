# Warehouse Management System

Warehouse Management System is a frontend application for tracking inventory across warehouses and bin locations. It covers stock receipts, issues, transfers and counts, with customer orders and staff task assignments.

Built with HTML, CSS and JavaScript ES modules. The application runs without a frontend framework or build step.

## Features

- **Overview** — stock indicators, replenishment alerts, bin occupancy and recent activity.
- **Inventory** — product catalogue, stock by location, search, filters and CSV export.
- **Stock operations** — receipt and count history, goods issue details and bin-to-bin transfers.
- **Locations** — warehouse, zone, aisle and bin organisation.
- **Directories and assignments** — suppliers, staff, customers, orders and tasks.
- **Reports** — low stock, movement history, bins empty throughout the previous week and latest count differences.

## Getting started

Requires **Node.js 24+** and npm. From the project root:

```bash
npm ci
npm start
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). Stop the server with `Ctrl+C`.

Start the Spring Boot API and MySQL using the [backend setup guide](../backend/README.md). The frontend connects to `http://localhost:8080/api` by default. Check `GET /api/health` for `"db":"up"` before opening the frontend.

### VS Code Live Server

Open `frontend/index.html` with the Live Server extension and use the address it opens. Serve the application over HTTP; opening the file directly does not support its ES module setup.

### Custom port

Set `PORT` before starting the server. In PowerShell:

```powershell
$env:PORT = '5174'
npm start
```

The application will be available at `http://127.0.0.1:5174`.
Include this origin in the backend's `CORS_ORIGINS` when using a custom port or Live Server.

## Working with inventory

In **Locations**, create a warehouse, a zone, an aisle and a bin in that order. Use **Stock movements** to transfer inventory between bins.

Goods receipts, goods issues, stock counts, customer orders and tasks can be created, edited and deleted in HTTP mode. The backend applies the stock rules:

- **Goods receipt:** saving adds its lines to stock; editing changes stock by the difference; deleting takes the stock back out (refused if it has already left the bin).
- **Goods issue:** stock leaves when the status becomes Shipped or Completed. Pending issues can be edited or deleted; a shipped issue can only move on to Completed.
- **Stock count:** stock is set to the counted quantities when the count becomes Completed. Pending counts can be deleted; Completed counts are locked.
- **Tasks:** changing the status on a task card saves it straight away.

Staff records belong to a warehouse and can have a supervisor and multiple phone numbers. Supervisor cycles and duplicate phone numbers within one staff record are rejected. Existing staff without a warehouse remain visible as **Not assigned** until updated.

Product rules follow the current backend: IDs use up to 10 letters, digits, underscores or hyphens; names use up to 100 characters; reorder levels are positive integers. Storage requirements are **Ambient**, **Dry Ambient**, **Cold** or **Hazardous**.

### Reviewing the frontend against MySQL

1. In **Customers**, add a test customer with an apostrophe in their name, then search for that name. Verify the customer appears in MySQL and still appears after a page reload.
2. In **Goods issues**, click an issue and check that its lines load in the separate table below the list.
3. Check **Inventory** and **Reports** with the seeded data, then use **Locations** to create, rename and delete a test bin.

## Data and API configuration

API settings are in `frontend/src/config.js`. The default mode is `http`. The five optional operational reports are hidden in HTTP mode until their endpoints are enabled in `httpOperationReports`.

Development mode `demo` uses browser-local storage under `warehouse.local.v3` on the current origin. Local records stay in the browser when switching to HTTP mode.

## Project structure

```text
frontend/
  index.html       Application shell
  server.js        Local static server
  src/
    api/           HTTP client and response adapters
    components/    Shared UI and navigation
    domain/        Stock rules and validation
    forms/         Form rendering and payload collection
    pages/         Page renderers
    services/      Data loading and API operations
    state/         Application state and local persistence
    utils/         Formatting helpers
  styles/          Design tokens, components and responsive layouts
```

## Development commands

| Command                | Purpose                                        |
| ---------------------- | ---------------------------------------------- |
| `npm start`            | Start the local static server                  |
| `npm run format`       | Format the frontend source with Prettier       |
| `npm run format:check` | Check source formatting without changing files |
