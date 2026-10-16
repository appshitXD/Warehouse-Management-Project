// API boundary: IDs are strings; DECIMAL JSON strings are normalized once here.
export class DataError extends Error {
  constructor(message) {
    super(`Invalid API data: ${message}`);
    this.name = 'DataError';
  }
}
const fail = (message) => {
  throw new DataError(message);
};
const object = (v, at) =>
  v && typeof v === 'object' && !Array.isArray(v)
    ? v
    : fail(`${at} must be an object.`);
const list = (v, at) =>
  Array.isArray(v) ? v : fail(`${at} must be an array (use [] when empty).`);
const text = (v, at) =>
  typeof v === 'string' && v.trim()
    ? v.trim()
    : fail(`${at} must be a non-empty string.`);
const id = (v, at) =>
  (typeof v === 'string' && v.trim()) ||
  (typeof v === 'number' && Number.isSafeInteger(v))
    ? String(v).trim()
    : fail(`${at} requires a valid ID.`);
const amount = (v, at, min = 0) => {
  if (!['number', 'string'].includes(typeof v) || String(v).trim() === '')
    fail(`${at} requires a quantity.`);
  const n = Number(v);
  if (
    !Number.isFinite(n) ||
    n < min ||
    !Number.isSafeInteger(Math.round(n * 1000)) ||
    Math.abs(n * 1000 - Math.round(n * 1000)) > 0.00001
  )
    fail(
      `${at} must be a valid number with at most 3 decimal places within the safe numeric range.`,
    );
  return n;
};
const positive = (v, at) => {
  const n = amount(v, at);
  if (n <= 0) fail(`${at} must be greater than zero.`);
  return n;
};
const timestamp = (v, at) => {
  if (
    typeof v !== 'string' ||
    !/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(v) ||
    !Number.isFinite(Date.parse(v))
  )
    fail(`${at} requires an ISO 8601 timestamp with a timezone.`);
  return new Date(v).toISOString();
};
const choice = (v, values, at) =>
  values.includes(v) ? v : fail(`${at} is not supported.`);
const unique = (rows, key, at) => {
  const seen = new Set();
  for (const r of rows) {
    const k = key(r);
    if (seen.has(k)) fail(`${at} duplicate key ${k}.`);
    seen.add(k);
  }
};
const newest = (rows) => rows.sort((a, b) => b.date.localeCompare(a.date));
const ref = (v, rows, at) => {
  const k = id(v, at);
  if (!rows.some((r) => r.id === k))
    fail(`${at} references an unknown ID: ${k}.`);
  return k;
};
const mapRows = (d, name, map) =>
  list(d[name], name).map((r, i) =>
    map(object(r, `${name}[${i}]`), `${name}[${i}]`),
  );
function validateLines(lines, at) {
  if (!lines.length) fail(`${at} requires at least one line.`);
  if (lines.some((l) => !Number.isInteger(l.lineNo)))
    fail(`${at}.lineNo must be an integer.`);
  unique(lines, (l) => l.lineNo, `${at}.lines`);
}
function countDto(raw, s, at) {
  const r = object(raw, at);
  const result = {
    id: id(r.id, `${at}.id`),
    date: timestamp(r.date, `${at}.date`),
    status: choice(r.status, ['completed'], `${at}.status`),
    lines: list(r.lines, `${at}.lines`).map((l, i) => {
      object(l, `${at}.lines[${i}]`);
      return {
        lineNo: positive(l.lineNo, `${at}.lineNo`),
        productId: ref(l.productId, s.products, `${at}.productId`),
        binId: ref(l.binId, s.bins, `${at}.binId`),
        recordedQuantity: amount(l.recordedQuantity, `${at}.recordedQuantity`),
        countedQuantity: amount(l.countedQuantity, `${at}.countedQuantity`),
      };
    }),
  };
  validateLines(result.lines, at);
  unique(result.lines, (l) => `${l.productId}|${l.binId}`, at);
  return result;
}
function movementDto(r, s, at) {
  object(r, at);
  const m = {
    id: id(r.id, `${at}.id`),
    date: timestamp(r.date, `${at}.date`),
    productId: ref(r.productId, s.products, `${at}.productId`),
    fromBinId: ref(r.fromBinId, s.bins, `${at}.fromBinId`),
    toBinId: ref(r.toBinId, s.bins, `${at}.toBinId`),
    staffId: ref(r.staffId, s.staff, `${at}.staffId`),
    quantity: positive(r.quantity, `${at}.quantity`),
  };
  if (m.fromBinId === m.toBinId)
    fail(`${at}: source and destination bins are identical.`);
  return m;
}
export function normalizeBootstrap(raw) {
  const d = object(raw, 'bootstrap');
  const s = {
    warehouses: mapRows(d, 'warehouses', (r, at) => ({
      id: id(r.id, at),
      name: text(r.name, at),
      address: typeof r.address === 'string' ? r.address : '',
    })),
    products: mapRows(d, 'products', (r, at) => ({
      id: id(r.id, at),
      name: text(r.name, `${at}.name`),
      unit: text(r.unit, `${at}.unit`),
      storage: text(r.storage, `${at}.storage`),
      reorderLevel: amount(r.reorderLevel, `${at}.reorderLevel`),
    })),
    suppliers: mapRows(d, 'suppliers', (r, at) => ({
      id: id(r.id, at),
      name: text(r.name, at),
      phone: typeof r.phone === 'string' ? r.phone : '',
      address: typeof r.address === 'string' ? r.address : '',
    })),
    customers: mapRows(d, 'customers', (r, at) => ({
      id: id(r.id, at),
      name: text(r.name, at),
      phone: typeof r.phone === 'string' ? r.phone : '',
      address: typeof r.address === 'string' ? r.address : '',
    })),
    staff: mapRows(d, 'staff', (r, at) => ({
      id: id(r.id, at),
      name: text(r.name, at),
      phone: typeof r.phone === 'string' ? r.phone : '',
      address: typeof r.address === 'string' ? r.address : '',
    })),
  };
  s.orders = mapRows(d, 'orders', (r, at) => ({
    id: id(r.id, at),
    customerId: ref(r.customerId, s.customers, at + '.customerId'),
    date: timestamp(r.date, at + '.date'),
    status: choice(r.status, ['open', 'closed', 'cancelled'], at + '.status'),
  }));
  s.bins = mapRows(d, 'bins', (r, at) => ({
    id: id(r.id, at),
    warehouseId: ref(r.warehouseId, s.warehouses, `${at}.warehouseId`),
    zone: id(r.zone, `${at}.zone`),
    aisle: id(r.aisle, `${at}.aisle`),
    position: id(r.position, `${at}.position`),
    name: typeof r.name === 'string' ? r.name : '',
  }));
  s.stock = mapRows(d, 'stock', (r, at) => ({
    productId: ref(r.productId, s.products, at),
    binId: ref(r.binId, s.bins, at),
    quantity: amount(r.quantity, `${at}.quantity`),
  }));
  for (const name of ['receipts', 'issues'])
    s[name] = newest(
      mapRows(d, name, (r, at) => {
        const result = {
          id: id(r.id, at),
          date: timestamp(r.date, `${at}.date`),
          lines: list(r.lines, `${at}.lines`).map((l) => {
            object(l, at);
            return {
              ...(name === 'receipts'
                ? { lineNo: positive(l.lineNo, `${at}.lineNo`) }
                : { id: id(l.id, `${at}.issueLineId`) }),
              productId: ref(l.productId, s.products, `${at}.productId`),
              binId: ref(l.binId, s.bins, `${at}.binId`),
              quantity: positive(l.quantity, `${at}.quantity`),
            };
          }),
        };
        if (name === 'receipts')
          result.supplierId = ref(
            r.supplierId,
            s.suppliers,
            `${at}.supplierId`,
          );
        else {
          result.orderId = ref(r.orderId, s.orders, `${at}.orderId`);
          result.status = text(r.status, `${at}.status`);
          result.stateOfGood =
            typeof r.stateOfGood === 'string' ? r.stateOfGood : '';
        }
        if (name === 'receipts') validateLines(result.lines, at);
        else {
          if (!result.lines.length) fail(`${at} requires at least one line.`);
          unique(result.lines, (l) => l.id, `${at}.lines`);
        }
        return result;
      }),
    );
  s.movements = newest(
    mapRows(d, 'movements', (r, at) => movementDto(r, s, at)),
  );
  s.counts = newest(mapRows(d, 'counts', (r, at) => countDto(r, s, at)));
  unique(
    s.issues.flatMap((issue) => issue.lines),
    (line) => line.id,
    'issues.lines',
  );
  s.tasks = mapRows(d, 'tasks', (r, at) => ({
    id: id(r.id, at),
    name: text(r.name, at),
    staffId: ref(r.staffId, s.staff, `${at}.staffId`),
    status: choice(r.status, ['todo', 'doing', 'done'], `${at}.status`),
  }));
  // Server reports do not require exposing the entire ledger to the browser.
  s.history =
    d.history === undefined
      ? []
      : mapRows(d, 'history', (r, at) => ({
          productId: ref(r.productId, s.products, at),
          binId: ref(r.binId, s.bins, at),
          date: timestamp(r.date, at),
          quantity: amount(r.quantity, at),
          id: id(r.id, at),
        }));
  for (const [key, rows] of Object.entries(s))
    if (!['stock', 'history'].includes(key)) unique(rows, (r) => r.id, key);
  unique(s.products, (p) => p.id.toLowerCase(), 'products.id');
  unique(
    s.bins,
    (b) => `${b.warehouseId}|${b.zone}|${b.aisle}|${b.position}`,
    'bins.address',
  );
  unique(s.stock, (r) => `${r.productId}|${r.binId}`, 'stock');
  unique(s.history, (r) => r.id, 'history');
  return s;
}
export function normalizeLocations(raw, s, productId) {
  const rows = list(object(raw, 'locations').items, 'locations.items').map(
    (r) => ({
      productId: ref(r.productId, s.products, 'locations.productId'),
      binId: ref(r.binId, s.bins, 'locations.binId'),
      quantity: positive(r.quantity, 'locations.quantity'),
    }),
  );
  if (rows.some((r) => r.productId !== productId))
    fail('Product locations do not match the selected product.');
  unique(rows, (r) => `${r.productId}|${r.binId}`, 'locations');
  return rows;
}
export function normalizeReports(raw, s) {
  const lowStock = list(
    object(raw.lowStock, 'lowStock').items,
    'lowStock.items',
  ).map((r) => {
    const row = {
      productId: ref(r.productId, s.products, 'lowStock.productId'),
      totalQuantity: amount(r.totalQuantity, 'lowStock.totalQuantity'),
      reorderLevel: amount(r.reorderLevel, 'lowStock.reorderLevel'),
    };
    if (row.totalQuantity >= row.reorderLevel)
      fail('lowStock includes an item that is not below its reorder level.');
    return row;
  });
  unique(lowStock, (r) => r.productId, 'lowStock');
  const empty = object(raw.emptyBins, 'emptyBins');
  const emptyBins = {
    from: timestamp(empty.from, 'emptyBins.from'),
    to: timestamp(empty.to, 'emptyBins.to'),
    timezone: text(empty.timezone, 'emptyBins.timezone'),
    items: list(empty.items, 'emptyBins.items').map((r) => ({
      binId: ref(r.binId, s.bins, 'emptyBins.binId'),
    })),
  };
  unique(emptyBins.items, (r) => r.binId, 'emptyBins');
  const moves = object(raw.movements, 'movements');
  const movements = {
    from: timestamp(moves.from, 'movements.from'),
    to: timestamp(moves.to, 'movements.to'),
    items: newest(
      list(moves.items, 'movements.items').map((r, i) =>
        movementDto(r, s, `movements[${i}]`),
      ),
    ),
  };
  if (emptyBins.from >= emptyBins.to || movements.from >= movements.to)
    fail('Invalid report time range.');
  const latest = object(raw.latestCount, 'latestCount');
  if (latest.count === undefined) fail('latestCount requires count or null.');
  return {
    lowStock,
    emptyBins,
    movements,
    latestCount:
      latest.count === null ? null : countDto(latest.count, s, 'latestCount'),
  };
}
