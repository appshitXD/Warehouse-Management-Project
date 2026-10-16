import { esc, num } from '../utils/format.js';
export function createLocations({ ui, ctx, selectors }) {
  function locations() {
    const query = ctx.filters.search.toLocaleLowerCase('en');
    const warehouseMatches = (w) =>
      [w.id, w.name, w.address]
        .join(' ')
        .toLocaleLowerCase('en')
        .includes(query);
    const visibleBins = (w) =>
      ctx.state.bins.filter(
        (b) =>
          b.warehouseId === w.id &&
          (warehouseMatches(w) ||
            [b.id, b.zone, b.aisle, b.position, b.name]
              .join(' ')
              .toLocaleLowerCase('en')
              .includes(query)),
      );
    const warehouses = ctx.state.warehouses.filter(
      (w) =>
        (!ctx.filters.warehouse || w.id === ctx.filters.warehouse) &&
        (warehouseMatches(w) || visibleBins(w).length),
    );
    const actions =
      ui.actionButton('warehouse', 'Add warehouse') +
      ui.actionButton('bin', 'Add bin', 'secondary');
    if (!ctx.state.warehouses.length)
      return (
        ui.heading('Locations', 'Warehouse → Zone → Aisle → Bin.', actions) +
        ui.emptyState(
          'No warehouses yet.',
          'Add a warehouse and bins to start storing products.',
        )
      );
    return (
      ui.heading(
        'Locations',
        'Warehouse / Zone / Aisle / Position. Multiple products can share a bin.',
        actions,
      ) +
      ui.toolbar('Search warehouse, bin address or name…') +
      (!warehouses.length
        ? '<p class="empty">No matching locations.</p>'
        : '') +
      `<div class="location-grid">${warehouses
        .map(
          (w) =>
            `<section class="card">${ui.cardTitle(esc(w.name), `${esc(w.id)} · ${esc(w.address)}`, ui.recordActions('warehouse', w.id))}<div class="card-body">${[
              ...new Set(visibleBins(w).map((b) => b.zone)),
            ]
              .map(
                (z) =>
                  `<h3 class="zone-heading">Zone ${esc(z)}</h3><div class="bin-grid">${visibleBins(
                    w,
                  )
                    .filter((b) => b.warehouseId === w.id && b.zone === z)
                    .map((b) => {
                      const stocks = ctx.state.stock.filter(
                        (s) => s.binId === b.id && s.quantity > 0,
                      );
                      return `<div class="bin-tile ${stocks.length ? '' : 'empty-bin'}"><strong>${esc(b.zone)}-${esc(b.aisle)}-${esc(b.position)}</strong><small>${esc(b.name || 'Unnamed bin')}</small><div class="bin-summary">${ui.badge(stocks.length ? `${stocks.length} ${stocks.length === 1 ? 'product' : 'products'}` : 'Empty', stocks.length ? 'green' : 'gray')}</div>${stocks.map((s) => `<small>${esc(selectors.product(s.productId).id)} · ${num(s.quantity)} ${esc(selectors.product(s.productId).unit)}</small>`).join('')}${ui.recordActions('bin', b.id)}</div>`;
                    })
                    .join('')}</div>`,
              )
              .join('')}</div></section>`,
        )
        .join('')}</div>`
    );
  }
  return { locations };
}
