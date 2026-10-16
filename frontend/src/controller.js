import { renderNavigation } from './components/navigation.js';
import { routes } from './config.js';
export function createController({
  $,
  $$,
  ctx,
  env,
  config,
  pages,
  ui,
  services,
  selectors,
}) {
  function render() {
    if (!ctx.state) return;
    if (ctx.filters.product && !selectors.product(ctx.filters.product))
      ctx.filters.product = '';
    if (ctx.filters.warehouse && !selectors.warehouse(ctx.filters.warehouse))
      ctx.filters.warehouse = '';
    ctx.route = env.location.hash.slice(1) || 'dashboard';
    if (!routes.some((r) => r[0] === ctx.route)) ctx.route = 'dashboard';
    $('#navigation').innerHTML = renderNavigation(
      routes,
      ctx.route,
      ctx.state.counts[0]?.lines.some(
        (l) => l.countedQuantity !== l.recordedQuantity,
      ),
    );
    $('#breadcrumb').textContent = routes.find((r) => r[0] === ctx.route)[2];
    env.document.title = `${$('#breadcrumb').textContent} · Warehouse`;
    $('.sidebar-bottom strong').textContent =
      config.mode === 'demo' ? 'Local data' : 'Backend connection';
    $('.sidebar-bottom small').textContent =
      config.mode === 'demo' ? 'Saved in this browser' : 'Data from API';
    const routePages = {
      dashboard: pages.dashboard,
      inventory: pages.inventory,
      receipts: () => pages.documentsPage('receipts'),
      issues: () => pages.documentsPage('issues'),
      movements: pages.movements,
      counts: pages.counts,
      locations: pages.locations,
      tasks: pages.tasks,
      reports: pages.reports,
      staff: () => pages.peoplePage('staff'),
      customers: () => pages.peoplePage('customer'),
      orders: pages.ordersPage,
      suppliers: pages.suppliersPage,
    };
    $('#main').innerHTML = ui.statusBanner() + routePages[ctx.route]();
    $('#main').setAttribute('aria-busy', String(ctx.refreshing));
    if (
      ctx.route === 'reports' &&
      config.mode === 'http' &&
      !ctx.remoteReports &&
      !ctx.reportsBusy &&
      !ctx.reportsError
    )
      void services.loadReports();
    if (
      ctx.route === 'inventory' &&
      config.mode === 'http' &&
      ctx.filters.product &&
      !ctx.remoteLocations &&
      !ctx.locationsBusy &&
      !ctx.locationsError
    )
      void services.loadLocations();
  }

  function notify(message) {
    $('#toast').textContent = message;
    $('#toast').classList.add('visible');
    env.clearTimeout(notify.timer);
    notify.timer = env.setTimeout(
      () => $('#toast').classList.remove('visible'),
      4000,
    );
  }

  async function refreshData() {
    if (ctx.refreshing || ctx.submitting) return;
    ctx.refreshing = true;
    ctx.refreshError = '';
    if (ctx.state) render();
    else {
      $('#main').innerHTML = ui.loadingState();
      $('#main').setAttribute('aria-busy', 'true');
    }
    try {
      await services.reload();
    } catch (error) {
      ctx.refreshError = error.message;
      ctx.stale = !!ctx.state;
    } finally {
      ctx.refreshing = false;
      if (ctx.state) render();
      else {
        $('#main').setAttribute('aria-busy', 'false');
        $('#main').innerHTML = ui.emptyState(
          'Unable to load data',
          ctx.refreshError,
          '<button class="button secondary" id="retry-data">Retry</button>',
        );
      }
    }
  }

  async function afterWrite(message) {
    try {
      await services.reload();
      render();
      notify(message);
    } catch (error) {
      ctx.stale = true;
      ctx.refreshError =
        'Changes were saved, but updated stock could not be loaded. ' +
        error.message;
      render();
      notify('Changes saved. Refresh the data instead of submitting again.');
    }
  }

  async function retryPending() {
    if (!ctx.pendingMutation || ctx.submitting || ctx.refreshing) return;
    const intent = ctx.pendingMutation;
    ctx.submitting = true;
    render();
    try {
      await services.sendMutation(
        intent.path,
        intent.body,
        intent.method,
        intent,
      );
      await afterWrite('Previous request confirmed.');
    } catch (error) {
      ctx.refreshError = error.message;
      notify(error.message);
    } finally {
      ctx.submitting = false;
      render();
    }
  }

  function resetRouteFilters() {
    ctx.filters = { search: '', warehouse: '', product: '' };
    ctx.remoteReports = null;
    ctx.remoteLocations = null;
    ctx.reportsGeneration++;
    ctx.locationsGeneration++;
    ctx.reportsBusy = false;
    ctx.locationsBusy = false;
    ctx.reportsError = '';
    ctx.locationsError = '';
  }

  function exportStock() {
    if (ctx.locationsBusy || ctx.locationsError) {
      notify(
        'Wait for product locations to load successfully before exporting CSV.',
      );
      return;
    }
    const cell = (value) =>
      '"' +
      String(value)
        .replace(/^[=+@-]/, "'")
        .replaceAll('"', '""') +
      '"';
    const source =
      config.mode === 'http' && ctx.filters.product
        ? ctx.remoteLocations || []
        : ctx.state.stock;
    const rows = [
      [
        'Product ID',
        'Name',
        'Warehouse',
        'Zone',
        'Aisle',
        'Position',
        'Quantity',
        'Unit',
      ],
      ...source
        .filter(
          (s) =>
            s.quantity > 0 &&
            selectors.matchesProduct(s.productId) &&
            selectors.matchesBin(s.binId) &&
            (!ctx.filters.product || s.productId === ctx.filters.product),
        )
        .map((s) => {
          const p = selectors.product(s.productId),
            b = selectors.bin(s.binId);
          return [
            p.id,
            p.name,
            b.warehouseId,
            b.zone,
            b.aisle,
            b.position,
            s.quantity,
            p.unit,
          ];
        }),
    ];
    const url = env.URL.createObjectURL(
      new env.Blob(
        ['\uFEFF' + rows.map((r) => r.map(cell).join(',')).join('\r\n')],
        { type: 'text/csv;charset=utf-8;' },
      ),
    );
    const a = env.document.createElement('a');
    a.href = url;
    a.download = 'warehouse-stock.csv';
    a.click();
    env.setTimeout(() => env.URL.revokeObjectURL(url), 1000);
  }
  return {
    render,
    notify,
    refreshData,
    afterWrite,
    retryPending,
    resetRouteFilters,
    exportStock,
  };
}
