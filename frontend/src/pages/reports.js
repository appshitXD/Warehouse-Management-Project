import { num, date, esc, documentCode, round } from '../utils/format.js';
export function createReports({ ui, ctx, config, selectors, stock }) {
  function reports() {
    const header = ui.heading(
      'Reports & reconciliation',
      'Stock availability, movement history and count reconciliation.',
    );
    const picker =
      '<div class="report-filter"><label for="product-filter">Movement product</label><select id="product-filter"><option value="">All products</option>' +
      ui.options(
        ctx.state.products,
        (p) => p.id + ' · ' + p.name,
        ctx.filters.product,
      ) +
      '</select></div>';
    const http = config.mode === 'http';
    if (http && !ctx.remoteReports)
      return (
        header +
        picker +
        (ctx.reportsError
          ? ui.emptyState(
              'Unable to load reports',
              ctx.reportsError,
              '<button class="button secondary" id="retry-reports">Retry reports</button>',
            )
          : ui.loadingState('Loading reports from the API…'))
      );
    const cutoff = new Date();
    cutoff.setMonth(cutoff.getMonth() - 1);
    const low = http
      ? ctx.remoteReports.lowStock
      : selectors.lowProducts().map((p) => ({
          productId: p.id,
          totalQuantity: selectors.total(p.id),
          reorderLevel: p.reorderLevel,
        }));
    const moves = http
      ? ctx.remoteReports.movements.items
      : ctx.state.movements.filter(
          (m) =>
            new Date(m.date) >= cutoff &&
            (!ctx.filters.product || m.productId === ctx.filters.product),
        );
    const emptyBins = http
      ? ctx.remoteReports.emptyBins.items.map((r) => selectors.bin(r.binId))
      : stock.emptyLastWeek();
    const week = stock.lastWeek(),
      from = http ? ctx.remoteReports.emptyBins.from : week.start,
      to = http ? ctx.remoteReports.emptyBins.to : week.end;
    const c = http ? ctx.remoteReports.latestCount : ctx.state.counts[0];
    return (
      header +
      picker +
      '<div class="demo-banner">' +
      (http
        ? 'Report results are provided by the API.'
        : 'Reports are calculated from history stored in this browser.') +
      '</div><div class="report-grid">' +
      '<section class="card">' +
      ui.cardTitle(
        '01 · Products below reorder level',
        'Total stock across all warehouses.',
      ) +
      '<div class="card-body">' +
      (low
        .map(
          (r) =>
            '<div class="activity">' +
            ui.pcell(selectors.product(r.productId)) +
            '<span class="number stock-warning report-stock-total">' +
            num(r.totalQuantity) +
            ' / ' +
            num(r.reorderLevel) +
            '</span></div>',
        )
        .join('') ||
        (ctx.state.products.length
          ? '<p class="empty">All products meet their reorder levels.</p>'
          : '<p class="empty">No products yet.</p>')) +
      '</div></section>' +
      '<section class="card">' +
      ui.cardTitle(
        '02 · Bins empty throughout last week',
        date(from) + ' – ' + date(new Date(new Date(to).getTime() - 1)),
      ) +
      '<div class="card-body"><div class="report-value">' +
      emptyBins.length +
      ` <small class="muted">${emptyBins.length === 1 ? 'bin' : 'bins'}</small></div>` +
      emptyBins
        .map((b) => ui.badge(selectors.binName(b.id), 'gray'))
        .join(' ') +
      '<p class="info-note">' +
      (http
        ? 'Time range and timezone: ' +
          esc(ctx.remoteReports.emptyBins.timezone)
        : 'Only bins with a stock-history record before the week are eligible. Never-used bins have insufficient historical evidence.') +
      '</p></div></section>' +
      '<section class="card full-report">' +
      ui.cardTitle(
        '03 · Movements over the last month',
        date(http ? ctx.remoteReports.movements.from : cutoff) +
          ' – ' +
          date(http ? ctx.remoteReports.movements.to : new Date()),
      ) +
      ui.table(
        ['Product', 'Date', 'From bin', 'To bin', 'Quantity'],
        moves
          .map(
            (m) =>
              '<tr><td>' +
              esc(selectors.product(m.productId).name) +
              '</td><td>' +
              date(m.date) +
              '</td><td>' +
              esc(selectors.binName(m.fromBinId)) +
              '</td><td>' +
              esc(selectors.binName(m.toBinId)) +
              '</td><td>' +
              num(m.quantity) +
              ' ' +
              esc(selectors.product(m.productId).unit) +
              '</td></tr>',
          )
          .join(''),
      ) +
      '</section>' +
      '<section class="card full-report">' +
      ui.cardTitle(
        '04 · Latest stock count differences',
        c ? documentCode(c.id) + ' · ' + date(c.date) : 'No stock counts yet',
      ) +
      ui.table(
        ['Product', 'Bin', 'Recorded', 'Counted', 'Difference'],
        c?.lines
          .map(
            (l) =>
              '<tr><td>' +
              esc(selectors.product(l.productId).name) +
              '</td><td>' +
              esc(selectors.binName(l.binId)) +
              '</td><td>' +
              num(l.recordedQuantity) +
              '</td><td>' +
              num(l.countedQuantity) +
              '</td><td class="number ' +
              (l.countedQuantity !== l.recordedQuantity ? 'danger' : '') +
              '">' +
              num(round(l.countedQuantity - l.recordedQuantity)) +
              '</td></tr>',
          )
          .join(''),
      ) +
      '</section></div>'
    );
  }
  return { reports };
}
