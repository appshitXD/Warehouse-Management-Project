import {
  normalizeReports,
  normalizeLocations,
  normalizeBootstrap,
  normalizeOverstock,
  normalizeSlowMoving,
  normalizeWaitingOrders,
  normalizeTopCustomers,
  normalizeProductHistory,
} from '../api/contracts.js';
import { endpoints } from '../config.js';

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
    if (config.mode !== 'http') return;
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

  async function loadReports() {
    if (config.mode !== 'http') return;
    const token = ++ctx.reportsGeneration;
    ctx.reportsBusy = true;
    ctx.reportsError = '';
    ctx.remoteReports = null;
    controller.render();
    const end = new Date(),
      from = new Date(end);
    from.setMonth(from.getMonth() - 1);
    const week = stock.lastWeek();
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
    if (
      ['zone', 'aisle', 'delete-zone', 'delete-aisle'].includes(ctx.operation)
    )
      throw new Error(
        'Zone and aisle APIs are not available in the current backend. Use local mode until these endpoints are implemented.',
      );
    if (ctx.operation.startsWith('delete-'))
      return sendMutation(
        endpoints[ctx.operation.slice(7)] +
          '/' +
          encodeURIComponent(ctx.editingId),
        undefined,
        'DELETE',
      );
    return sendMutation(
      endpoints[ctx.operation] +
        (ctx.editingId ? '/' + encodeURIComponent(ctx.editingId) : ''),
      payload,
      ctx.editingId ? 'PATCH' : 'POST',
    );
  }
  return {
    loadReports,
    loadLocations,
    loadOperationReport,
    loadRouteReports,
    reload,
    sendMutation,
    saveOperation,
  };
}
