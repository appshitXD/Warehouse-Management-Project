import {
  normalizeReports,
  normalizeLocations,
  normalizeBootstrap,
  normalizeOverstock,
  normalizeSlowMoving,
  normalizeWaitingOrders,
  normalizeTopCustomers,
  normalizeProductHistory,
  normalizeIssueList,
  normalizeIssueLines,
  normalizeCustomers,
} from '../api/contracts.js';
import { canWrite, endpoints, showOperationReport } from '../config.js';

const operationReports = {
  overstock: ['/reports/overstock', normalizeOverstock],
  waitingOrders: ['/orders/awaiting-issue', normalizeWaitingOrders],
  topCustomers: ['/reports/top-customers', normalizeTopCustomers],
};

const routeReports = {
  reports: ['overstock', 'slowMoving'],
  orders: ['waitingOrders'],
  dashboard: ['topCustomers'],
};
export function createServices({
  config,
  ctx,
  controller,
  stock,
  request,
  storage,
  env,
}) {
  async function loadOperationReport(key, retry = false) {
    const cache = ctx.operationReports;
    if (config.mode !== 'http' || !showOperationReport(config, key)) return;
    const productId = key === 'productHistory' ? ctx.filters.product : '';
    if (key === 'productHistory' && !productId) return;
    const asOf = ctx.reportsAsOf;
    let endpoint = operationReports[key];
    if (productId)
      endpoint = [
        '/products/' + encodeURIComponent(productId) + '/history',
        (raw) => normalizeProductHistory(raw, productId),
      ];
    if (key === 'slowMoving')
      endpoint = [
        '/reports/slow-moving?' + new URLSearchParams({ asOf }),
        (raw) => normalizeSlowMoving(raw, asOf),
      ];
    if (!endpoint) return;
    const [path, normalize] = endpoint;
    if (!retry && cache.get(key)?.path === path) return;
    const pending = { path, productId, asOf };
    cache.set(key, pending);
    if (retry) controller.render();
    try {
      pending.items = normalize(await request(path));
    } catch (error) {
      pending.error = error.message;
    }
    const visible =
      key === 'productHistory'
        ? ctx.route === 'inventory' && ctx.filters.product === productId
        : routeReports[ctx.route]?.includes(key);
    if (visible && ctx.operationReports === cache && cache.get(key) === pending)
      controller.render();
  }

  function loadRouteReports() {
    for (const key of routeReports[ctx.route] || [])
      void loadOperationReport(key);
    if (ctx.route === 'inventory') void loadOperationReport('productHistory');
  }

  async function loadDirectory(key, { retry = false, debounce = false } = {}) {
    if (config.mode !== 'http') return;
    const cache = ctx.remoteDirectories;
    const issueId = ctx.filters.detail;
    const query = ctx.filters.search.trim();
    const paths = {
      customers: '/customers',
      issues: '/issues',
      issueLines: issueId
        ? '/issues/' + encodeURIComponent(issueId) + '/lines'
        : '',
    };
    const base = paths[key];
    if (!base) return;
    const path =
      base +
      (query && key !== 'issueLines'
        ? '?' + new URLSearchParams({ q: query })
        : '');
    if (!retry && cache.get(key)?.path === path) return;
    const entry = { path, selection: key === 'issueLines' ? issueId : query };
    cache.set(key, entry);
    if (retry || debounce) controller.render();
    if (debounce) await new Promise((resolve) => env.setTimeout(resolve, 250));
    const current = () =>
      ctx.remoteDirectories === cache &&
      cache.get(key) === entry &&
      ctx.route === (key === 'customers' ? 'customers' : 'issues');
    if (!current()) {
      if (cache.get(key) === entry) cache.delete(key);
      return;
    }
    try {
      const raw = await request(path);
      entry.items =
        key === 'customers'
          ? normalizeCustomers(raw)
          : key === 'issues'
            ? normalizeIssueList(raw)
            : normalizeIssueLines(raw, issueId);
    } catch (error) {
      entry.error = error.message;
    }
    if (current()) controller.render();
  }

  function loadRouteDirectory() {
    if (ctx.route === 'customers') void loadDirectory('customers');
    if (ctx.route === 'issues') {
      void loadDirectory('issues');
      if (ctx.filters.detail) void loadDirectory('issueLines');
    }
  }

  async function loadReports() {
    if (config.mode !== 'http') return;
    const token = ++ctx.reportsGeneration;
    ctx.reportsBusy = true;
    ctx.reportsError = '';
    ctx.remoteReports = null;
    controller.render();
    // Reports use the same fixed date as the SQL queries (@today = reportsAsOf,
    // 2026-10-18), so the app shows the same results as the report.
    // Q3 "last month" = the calendar month before that date,
    // Q4 "last week" = Monday to Sunday of the week before it.
    const asOf = new Date(ctx.reportsAsOf + 'T00:00:00'),
      end = new Date(asOf.getFullYear(), asOf.getMonth(), 1),
      from = new Date(asOf.getFullYear(), asOf.getMonth() - 1, 1);
    const week = stock.lastWeek(asOf);
    const query = new URLSearchParams({
      from: from.toISOString(),
      to: end.toISOString(),
    });
    if (ctx.filters.product) query.set('productId', ctx.filters.product);
    const weekQuery = new URLSearchParams({
      from: week.start.toISOString(),
      to: week.end.toISOString(),
      timezone: Intl.DateTimeFormat().resolvedOptions().timeZone,
    });
    const selected = ctx.filters.product;
    try {
      const [lowStock, emptyBins, movements, latestCount] = await Promise.all([
        request('/reports/low-stock'),
        request('/reports/empty-bins?' + weekQuery),
        request('/reports/movements?' + query),
        request('/reports/latest-count-differences'),
      ]);
      if (token !== ctx.reportsGeneration) return;
      const result = normalizeReports(
        { lowStock, emptyBins, movements, latestCount },
        ctx.state,
      );
      if (
        selected &&
        result.movements.items.some((m) => m.productId !== selected)
      )
        throw new Error(
          'The API report returned a different product from the selection.',
        );
      ctx.remoteReports = result;
    } catch (error) {
      if (token === ctx.reportsGeneration) ctx.reportsError = error.message;
    } finally {
      if (token === ctx.reportsGeneration) {
        ctx.reportsBusy = false;
        controller.render();
      }
    }
  }

  async function loadLocations() {
    if (config.mode !== 'http') return;
    const selected = ctx.filters.product,
      token = ++ctx.locationsGeneration;
    ctx.locationsError = '';
    ctx.remoteLocations = null;
    if (!selected) {
      ctx.locationsBusy = false;
      controller.render();
      return;
    }
    ctx.locationsBusy = true;
    controller.render();
    try {
      const result = await request(
        '/reports/product-locations?' +
          new URLSearchParams({ productId: selected }),
      );
      if (token === ctx.locationsGeneration)
        ctx.remoteLocations = normalizeLocations(result, ctx.state, selected);
    } catch (error) {
      if (token === ctx.locationsGeneration) ctx.locationsError = error.message;
    } finally {
      if (token === ctx.locationsGeneration) {
        ctx.locationsBusy = false;
        controller.render();
      }
    }
  }

  async function reload() {
    const next =
      config.mode === 'http'
        ? normalizeBootstrap(await request('/bootstrap'))
        : storage.loadLocal();
    ctx.state = next;
    ctx.operationReports = new Map();
    ctx.remoteDirectories = new Map();
    ctx.stale = false;
    ctx.refreshError = '';
    ctx.remoteReports = null;
    ctx.reportsError = '';
    ctx.reportsBusy = false;
    ctx.reportsGeneration++;
    ctx.remoteLocations = null;
    ctx.locationsError = '';
    ctx.locationsBusy = false;
    ctx.locationsGeneration++;
  }

  async function sendMutation(path, body, method = 'POST', intent = null) {
    const entry = intent || {
      key: env.crypto.randomUUID(),
      path,
      method,
      body,
      baseUrl: config.baseUrl,
    };
    storage.storePending(entry);
    try {
      const result = await request(path, {
        method,
        body,
        idempotencyKey: entry.key,
      });
      storage.storePending(null);
      return result;
    } catch (error) {
      if (!error.uncertain) storage.storePending(null);
      throw error;
    }
  }

  function saveOperation(payload) {
    const operation = ctx.operation;
    if (!canWrite(config, operation))
      throw new Error('This operation is read-only.');
    const deleting = operation.startsWith('delete-');
    const type = operation.replace(/^delete-/, '');
    const method = deleting ? 'DELETE' : ctx.editingId ? 'PATCH' : 'POST';
    if (['zone', 'aisle', 'bin'].includes(type)) {
      const [warehouseId, zoneId, aisleId, position] = (
        ctx.editingId || payload.id
      )
        .split('|')
        .map(encodeURIComponent);
      let path = '/warehouses/' + warehouseId + '/zones';
      if (type !== 'zone') path += '/' + zoneId + '/aisles';
      if (type === 'bin') path += '/' + aisleId + '/bins';
      if (method !== 'POST')
        path +=
          '/' +
          (type === 'zone' ? zoneId : type === 'aisle' ? aisleId : position);
      const key = type === 'bin' ? 'position' : 'id';
      const code =
        type === 'zone'
          ? payload?.zoneId
          : type === 'aisle'
            ? payload?.aisleId
            : payload?.position;
      const body = deleting
        ? undefined
        : method === 'POST'
          ? { [key]: code, name: payload.name }
          : { name: payload.name };
      return sendMutation(path, body, method);
    }
    // An order's ID cannot change: PATCH /orders/{id} only takes time, status and customerId.
    const body =
      deleting || !payload
        ? undefined
        : method === 'PATCH' && type === 'order'
          ? { customerId: payload.customerId, status: payload.status }
          : payload;
    return sendMutation(
      endpoints[type] +
        (ctx.editingId ? '/' + encodeURIComponent(ctx.editingId) : ''),
      body,
      method,
    );
  }
  return {
    loadReports,
    loadLocations,
    loadOperationReport,
    loadRouteReports,
    loadRouteDirectory,
    loadDirectory,
    reload,
    sendMutation,
    saveOperation,
  };
}
