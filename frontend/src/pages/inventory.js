import { esc, storageName } from '../utils/format.js';
export function createInventory({ config, ctx, selectors, ui }) {
  function inventory() {
    const num = new Intl.NumberFormat('en-GB', { maximumFractionDigits: 3 })
      .format;
    // Render-local indexes avoid repeatedly scanning the same arrays per row.
    const products = new Map(ctx.state.products.map((p) => [p.id, p]));
    const bins = new Map(ctx.state.bins.map((b) => [b.id, b]));
    const warehouses = new Map(ctx.state.warehouses.map((w) => [w.id, w]));
    const totals = new Map();
    for (const row of ctx.state.stock)
      totals.set(
        row.productId,
        (totals.get(row.productId) || 0) + row.quantity,
      );
    const total = (id) => totals.get(id) || 0;
    const query = ctx.filters.search.toLocaleLowerCase('en');
    const matchingProducts = new Set(
      ctx.state.products
        .filter((p) =>
          (p.id + ' ' + p.name).toLocaleLowerCase('en').includes(query),
        )
        .map((p) => p.id),
    );
    const binName = (id) => {
      const b = bins.get(id);
      return b ? [b.warehouseId, b.zone, b.aisle, b.position].join(' / ') : id;
    };
    const source =
      config.mode === 'http' && ctx.filters.product
        ? ctx.remoteLocations || []
        : ctx.state.stock;
    const rows = source.filter(
      (s) =>
        s.quantity > 0 &&
        matchingProducts.has(s.productId) &&
        (!ctx.filters.warehouse ||
          bins.get(s.binId)?.warehouseId === ctx.filters.warehouse) &&
        (!ctx.filters.product || s.productId === ctx.filters.product),
    );
    return (
      ui.heading(
        'Inventory',
        'Find products and their current storage locations.',
        `<button class="button secondary" id="export-stock">↓ Export CSV</button>${ui.actionButton('product', 'Add product')}`,
      ) +
      `<section class="card inventory-stock">${ui.cardTitle('Stock by location', 'Warehouse and product filters apply to this table.')}${ui.toolbar(undefined, `<label class="filter-field"><span>Product</span><select id="inventory-product-filter"><option value="">All products</option>${ui.options(ctx.state.products, (p) => p.id + ' · ' + p.name, ctx.filters.product)}</select></label>`)}${
        ctx.locationsBusy
          ? ui.loadingState('Loading product locations…')
          : ctx.locationsError
            ? ui.emptyState(
                'Unable to load locations',
                ctx.locationsError,
                '<button class="button secondary" id="retry-locations">Retry</button>',
              )
            : ui.table(
                [
                  'Product',
                  'Warehouse',
                  'Bin address',
                  'Quantity',
                  'Storage requirement',
                  'Total stock / reorder level',
                ],
                rows
                  .map((s) => {
                    const p = products.get(s.productId);
                    return `<tr><td>${ui.pcell(p)}</td><td>${esc(warehouses.get(bins.get(s.binId).warehouseId).name)}</td><td>${ui.badge(binName(s.binId), 'gray')}</td><td class="number">${num(s.quantity)} <small class="muted">${esc(p.unit)}</small></td><td>${esc(storageName(p.storage))}</td><td>${num(total(p.id))} / ${num(p.reorderLevel)} ${total(p.id) < p.reorderLevel ? ui.badge('Low', 'amber') : ui.badge('Sufficient')}</td></tr>`;
                  })
                  .join(''),
              )
      }<div class="table-footer">${rows.length} product–bin ${rows.length === 1 ? 'pair' : 'pairs'} · Quantities use each product's unit</div></section>` +
      `<section class="card inventory-catalogue">${ui.cardTitle('Product catalogue · All warehouses', 'Search applies here; includes products with no stock yet.')} ${ui.table(
        [
          'Product ID',
          'Product name',
          'Unit',
          'Storage',
          'Reorder level',
          'Total stock',
          'Actions',
        ],
        ctx.state.products
          .filter((p) => matchingProducts.has(p.id))
          .map(
            (p) =>
              `<tr><td>${esc(p.id)}</td><td>${esc(p.name)}</td><td>${esc(p.unit)}</td><td>${esc(storageName(p.storage))}</td><td>${num(p.reorderLevel)}</td><td class="number">${num(total(p.id))}</td><td>${ui.recordActions('product', p.id)}</td></tr>`,
          )
          .join(''),
      )}</section>`
    );
  }
  return { inventory };
}
