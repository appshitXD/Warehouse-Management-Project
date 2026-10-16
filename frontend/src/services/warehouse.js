import {
  normalizeReports,
  normalizeLocations,
  normalizeBootstrap,
} from '../api/contracts.js';
import { endpoints } from '../config.js';
export function createServices({
  config,
  ctx,
  controller,
  stock,
  request,
  storage,
  env,
}) {
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
  return { loadReports, loadLocations, reload, sendMutation, saveOperation };
}
