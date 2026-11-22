export const API_CONFIG = {
  mode: 'http',
  baseUrl: 'http://localhost:8080/api',
  timeoutMs: 10000,
};

// Extra reports loaded from the backend in HTTP mode. Remove a key to hide its card.
export const httpOperationReports = [
  'overstock',
  'slowMoving',
  'topCustomers',
  'waitingOrders',
  'productHistory',
];

// Every operation can be saved through the API. Add a type here to make it read-only again.
const readOnlyOperations = [];
export const canWrite = (config, operation) =>
  config.mode !== 'http' ||
  !readOnlyOperations.includes(operation.replace(/^delete-/, ''));

// Editing and deleting receipts, issues, counts and tasks is done by the backend
// (it also moves the stock), so it is only offered in HTTP mode.
export const canChangeDocuments = (config, type) =>
  config.mode === 'http' && canWrite(config, type);

export const showOperationReport = (config, key) =>
  config.mode !== 'http' || httpOperationReports.includes(key);

export const routes = [
  ['dashboard', '▦', 'Overview'],
  ['inventory', '▤', 'Inventory'],
  ['suppliers', '◇', 'Suppliers'],
  ['receipts', '↓', 'Goods receipts'],
  ['issues', '↑', 'Goods issues'],
  ['movements', '⇄', 'Stock movements'],
  ['counts', '◎', 'Stock counts'],
  ['locations', '▧', 'Locations'],
  ['staff', '♙', 'Staff'],
  ['customers', '♧', 'Customers'],
  ['orders', '▤', 'Customer orders'],
  ['tasks', '☷', 'Tasks'],
  ['reports', '◷', 'Reports'],
];

export const orderLabels = {
  open: 'Open',
  closed: 'Closed',
  cancelled: 'Cancelled',
};

export const taskLabels = { todo: 'To do', doing: 'In progress', done: 'Done' };

export const httpOrderLabels = {
  Pending: 'Pending',
  Shipped: 'Shipped',
  Delivered: 'Delivered',
  Cancelled: 'Cancelled',
};

export const httpTaskLabels = {
  Pending: 'Pending',
  'In Progress': 'In progress',
  Completed: 'Completed',
};

// CHECK CHK_GI_STATUS: stock leaves the bins when an issue becomes Shipped or Completed.
export const httpIssueLabels = {
  Pending: 'Pending',
  Shipped: 'Shipped',
  Completed: 'Completed',
};

// CHECK CHK_SC_STATUS: stock is set to the counted quantities when a count is Completed.
export const httpCountLabels = {
  Pending: 'Pending',
  'In Progress': 'In progress',
  Completed: 'Completed',
};

export const reportsAsOf = '2026-10-18';

export const endpoints = {
  receipt: '/receipts',
  issue: '/issues',
  movement: '/movements',
  count: '/counts',
  product: '/products',
  task: '/tasks',
  staff: '/staff',
  customer: '/customers',
  order: '/orders',
  supplier: '/suppliers',
  warehouse: '/warehouses',
  bin: '/bins',
};
