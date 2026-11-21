export const API_CONFIG = {
  mode: 'http',
  baseUrl: 'http://localhost:8080/api',
  timeoutMs: 10000,
};

// Add report keys here as their endpoints become available on the backend.
export const httpOperationReports = [];

const readOnlyOperations = ['receipt', 'issue', 'count', 'task', 'order'];
export const canWrite = (config, operation) =>
  config.mode !== 'http' ||
  !readOnlyOperations.includes(operation.replace(/^delete-/, ''));

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
