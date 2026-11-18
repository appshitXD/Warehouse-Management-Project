import { round } from '../utils/format.js';

export function localStockReports(state, asOf) {
  const bins = new Map(state.bins.map((bin) => [bin.id, bin.warehouseId]));
  const balances = new Map();
  for (const { productId, binId, quantity } of state.stock) {
    const balance = balances.get(productId) ?? {
      total: 0,
      warehouses: new Set(),
    };
    balance.total += quantity;
    balance.warehouses.add(bins.get(binId));
    balances.set(productId, balance);
  }

  const cutoff = new Date(asOf + 'T00:00:00Z');
  cutoff.setUTCDate(cutoff.getUTCDate() - 90);
  const start = cutoff.toISOString().slice(0, 10);
  const recentlyIssued = new Set();
  for (const issue of state.issues) {
    const day = issue.date.slice(0, 10);
    if (day < start) continue;
    for (const line of issue.lines) recentlyIssued.add(line.productId);
  }

  const overstock = [];
  const slowMoving = [];
  for (const product of state.products) {
    const balance = balances.get(product.id);
    const totalQuantity = round(balance?.total ?? 0);
    const identity = { productId: product.id, productName: product.name };
    if (totalQuantity >= 3 * product.reorderLevel)
      overstock.push({
        ...identity,
        unit: product.unit,
        reorderLevel: product.reorderLevel,
        totalQuantity,
        numWarehouses: balance?.warehouses.size ?? 0,
      });
    if (totalQuantity > 0 && !recentlyIssued.has(product.id))
      slowMoving.push({ ...identity, onHand: totalQuantity });
  }
  return {
    overstock: overstock.sort(
      (a, b) =>
        b.totalQuantity / b.reorderLevel - a.totalQuantity / a.reorderLevel,
    ),
    slowMoving: slowMoving.sort((a, b) => b.onHand - a.onHand),
  };
}

export function localWaitingOrders(state, customerName) {
  const issued = new Set(state.issues.map((issue) => issue.orderId));
  return state.orders
    .filter((order) => !issued.has(order.id))
    .map((order) => ({
      orderId: order.id,
      time: order.date,
      status: order.status,
      customerName: customerName(order.customerId),
    }))
    .sort((a, b) => (a.time ?? '').localeCompare(b.time ?? ''));
}

export function localTopCustomers(state) {
  const orders = new Map(
    state.orders.map((order) => [order.id, order.customerId]),
  );
  const totals = new Map();
  for (const issue of state.issues) {
    const customerId = orders.get(issue.orderId);
    if (!customerId) continue;
    const units = issue.lines.reduce((sum, line) => sum + line.quantity, 0);
    totals.set(customerId, (totals.get(customerId) ?? 0) + units);
  }
  return state.customers
    .filter((customer) => totals.has(customer.id))
    .map((customer) => ({
      customerId: customer.id,
      customerName: customer.name,
      unitsShipped: round(totals.get(customer.id)),
    }))
    .sort(
      (a, b) =>
        b.unitsShipped - a.unitsShipped ||
        a.customerId.localeCompare(b.customerId),
    )
    .slice(0, 10);
}

export function localProductHistory(state, productId, warehouseId) {
  const documents = (rows, type, sign) =>
    rows.flatMap((record) =>
      record.lines
        .filter((line) => line.productId === productId)
        .map((line) => ({
          activityDate: record.date,
          type,
          documentId: record.id,
          quantity: sign * line.quantity,
          location: warehouseId(line.binId),
        })),
    );
  const rows = [
    ...documents(state.receipts, 'Receipt', 1),
    ...documents(state.issues, 'Issue', -1),
    ...state.movements
      .filter((row) => row.productId === productId)
      .map((row) => ({
        activityDate: row.date,
        type: 'Movement',
        documentId: row.id,
        quantity: row.quantity,
        location: warehouseId(row.fromBinId) + ' → ' + warehouseId(row.toBinId),
      })),
  ];
  // The SQL UNION collapses identical activity rows.
  return [
    ...new Map(rows.map((row) => [JSON.stringify(row), row])).values(),
  ].sort((a, b) => Date.parse(a.activityDate) - Date.parse(b.activityDate));
}
