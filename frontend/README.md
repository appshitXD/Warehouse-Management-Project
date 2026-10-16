# Warehouse Management System

Warehouse Management System is a frontend application for tracking inventory across warehouses and bin locations. It covers stock receipts, issues, transfers and counts, with customer orders and staff task assignments.

Built with HTML, CSS and JavaScript ES modules. The application runs without a frontend framework or build step.

## Features

- **Overview** ΓÇö stock indicators, replenishment alerts, bin occupancy and recent activity.
- **Inventory** ΓÇö product catalogue, stock by location, search, filters and CSV export.
- **Stock operations** ΓÇö supplier receipts, issues against customer orders, bin-to-bin transfers and stock counts with discrepancy tracking.
- **Locations** ΓÇö warehouse, zone, aisle and bin organisation.
- **Directories and assignments** ΓÇö suppliers, staff, customers, orders and tasks.
- **Reports** ΓÇö low stock, movement history, bins empty throughout the previous week and latest count differences.

## Getting started

Requires **Node.js 24+** and npm. From the project root:

```bash
npm ci
npm start
```

Open [http://127.0.0.1:5173](http://127.0.0.1:5173). Stop the server with `Ctrl+C`.

### VS Code Live Server

Open `frontend/index.html` with the Live Server extension and use the address it opens. Serve the application over HTTP; opening the file directly does not support its ES module setup.

### Custom port

Set `PORT` before starting the server. In PowerShell:

```powershell
$env:PORT = '5174'
npm start
```

The application will be available at `http://127.0.0.1:5174`.

## Working with inventory

New workspaces start empty. Create a warehouse and bin in **Locations**, then add a product and supplier before recording a goods receipt.

Goods issues require an open customer order. Transfers and task assignments require a staff record. Saving a stock count adjusts the quantity and retains the recorded-versus-counted difference.

## Data and API configuration

This version uses browser-local storage. Entries persist under `warehouse.local.v3` on the current origin. A different hostname or port uses a separate storage area.

API settings are defined in `frontend/src/config.js`. The default mode is `demo`; the configured API URL is a development placeholder. Backend and database services are not included, and local records are not automatically transferred to a backend.

The HTTP client, response adapters and service calls are organised under `src/api/` and `src/services/` for integration with the team's API. Agree on endpoints and payloads before enabling HTTP mode.

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
