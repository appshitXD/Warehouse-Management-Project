import { esc, num } from '../utils/format.js';

export function createLocations({ ui, ctx, selectors }) {
  function locations() {
    const s = ctx.state;
    const query = ctx.filters.search.toLocaleLowerCase('en');
    const matches = (...values) =>
      values.join(' ').toLocaleLowerCase('en').includes(query);
    const warehouseMatches = (w) => matches(w.id, w.name, w.address);
    const zoneMatches = (z) => matches(z.zoneId, z.name);
    const aisleMatches = (a) => matches(a.aisleId, a.name);
    const binsFor = (w, z, a) =>
      s.bins.filter(
        (b) =>
          b.warehouseId === w.id &&
          b.zone === z.zoneId &&
          b.aisle === a.aisleId,
      );
    const zonesFor = (w) => s.zones.filter((z) => z.warehouseId === w.id);
    const aislesFor = (w, z) =>
      s.aisles.filter((a) => a.warehouseId === w.id && a.zone === z.zoneId);
    const binMatches = (b) =>
      matches(b.id, b.name, b.zone, b.aisle, b.position);
    const zoneVisible = (w, z) =>
      warehouseMatches(w) ||
      zoneMatches(z) ||
      aislesFor(w, z).some(
        (a) => aisleMatches(a) || binsFor(w, z, a).some(binMatches),
      );
    const warehouses = s.warehouses.filter(
      (w) =>
        (!ctx.filters.warehouse || ctx.filters.warehouse === w.id) &&
        (warehouseMatches(w) || zonesFor(w).some((z) => zoneVisible(w, z))),
    );
    const actions =
      ui.actionButton('warehouse', 'Add warehouse') +
      ui.actionButton('zone', 'Add zone', 'secondary') +
      ui.actionButton('aisle', 'Add aisle', 'secondary') +
      ui.actionButton('bin', 'Add bin', 'secondary');
    const heading = ui.heading(
      'Locations',
      'Warehouse / Zone / Aisle / Position. Each location belongs to its parent.',
      actions,
    );
    if (!s.warehouses.length)
      return (
        heading +
        ui.emptyState(
          'No warehouses yet.',
          'Create a warehouse, then add zones, aisles and bins.',
        )
      );
    return (
      heading +
      ui.toolbar('Search location IDs, names or addresses…') +
      (!warehouses.length
        ? ui.emptyState(
            'No matching locations.',
            'Adjust the search or warehouse filter.',
          )
        : '') +
      `<div class="location-grid">${warehouses
        .map(
          (w) =>
            `<section class="card">${ui.cardTitle(esc(w.name), `${esc(w.id)} · ${esc(w.address)}`, ui.recordActions('warehouse', w.id))}<div class="card-body">${
              zonesFor(w)
                .filter((z) => zoneVisible(w, z))
                .map(
                  (z) =>
                    `<section class="location-zone"><div class="row-between"><h3 class="zone-heading">Zone ${esc(z.zoneId)} · ${esc(z.name)}</h3>${ui.recordActions('zone', z.id)}</div>${
                      aislesFor(w, z)
                        .filter(
                          (a) =>
                            warehouseMatches(w) ||
                            zoneMatches(z) ||
                            aisleMatches(a) ||
                            binsFor(w, z, a).some(binMatches),
                        )
                        .map(
                          (a) =>
                            `<section class="location-aisle"><div class="row-between"><h4>Aisle ${esc(a.aisleId)} · ${esc(a.name)}</h4>${ui.recordActions('aisle', a.id)}</div><div class="bin-grid">${
                              binsFor(w, z, a)
                                .filter(
                                  (b) =>
                                    warehouseMatches(w) ||
                                    zoneMatches(z) ||
                                    aisleMatches(a) ||
                                    binMatches(b),
                                )
                                .map((b) => {
                                  const stock = s.stock.filter(
                                    (row) =>
                                      row.binId === b.id && row.quantity > 0,
                                  );
                                  return `<div class="bin-tile ${stock.length ? '' : 'empty-bin'}"><strong>${esc(b.position)}</strong><small>${esc(b.name || 'Unnamed bin')}</small><small>${esc(selectors.binName(b.id))}</small><div class="bin-summary">${ui.badge(stock.length ? `${stock.length} ${stock.length === 1 ? 'product' : 'products'}` : 'Empty', stock.length ? 'green' : 'gray')}</div>${stock.map((row) => `<small>${esc(row.productId)} · ${num(row.quantity)} ${esc(selectors.product(row.productId).unit)}</small>`).join('')}${ui.recordActions('bin', b.id)}</div>`;
                                })
                                .join('') ||
                              '<p class="muted">No bins in this aisle.</p>'
                            }</div></section>`,
                        )
                        .join('') ||
                      '<p class="muted">No aisles in this zone.</p>'
                    }</section>`,
                )
                .join('') || '<p class="muted">No zones in this warehouse.</p>'
            }</div></section>`,
        )
        .join('')}</div>`
    );
  }
  return { locations };
}
