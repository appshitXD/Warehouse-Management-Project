import { icon } from '../components/navigation.js';
import { documentCode, num, esc, date } from '../utils/format.js';
export function createDashboard({ ctx, selectors, ui, config }) {
  function dashboard() {
    const occupied = ctx.state.bins.filter((b) =>
      ctx.state.stock.some((s) => s.binId === b.id && s.quantity > 0),
    ).length;
    const latest = ctx.state.counts[0];
    const discrepancies =
      latest?.lines.filter((l) => l.countedQuantity !== l.recordedQuantity) ||
      [];
    const stats = [
      [
        'Products managed',
        ctx.state.products.length,
        'Each product has its own unit',
        '▣',
        'good',
      ],
      [
        'Occupied bins',
        `${occupied} / ${ctx.state.bins.length}`,
        'Across all warehouses',
        '▧',
        '',
      ],
      [
        'Below reorder level',
        selectors.lowProducts().length,
        'Stock needs replenishment',
        '↘',
        'warn',
      ],
      [
        'Count discrepancies',
        discrepancies.length,
        'Discrepancies in the latest count',
        '◎',
        discrepancies.length ? 'warn' : 'good',
      ],
    ];
    const events = [
      ...ctx.state.receipts.map((r) => ({
        ...r,
        type: 'Goods receipts',
        icon: '↓',
        detail: ctx.state.suppliers.find((s) => s.id === r.supplierId)?.name,
      })),
      ...ctx.state.issues.map((r) => ({
        ...r,
        type: 'Goods issues',
        icon: '↑',
        detail: r.orderId,
      })),
      ...ctx.state.movements.map((r) => ({
        ...r,
        type: 'Stock movements',
        icon: '⇄',
        detail: selectors.product(r.productId)?.name,
      })),
    ]
      .sort((a, b) => b.date.localeCompare(a.date))
      .slice(0, 5);
    const emptyPanel = (symbol, title, description) =>
      `<div class="overview-empty"><span class="overview-empty-icon" aria-hidden="true">${icon(symbol)}</span><strong>${title}</strong><p>${description}</p></div>`;
    return (
      '<div class="dashboard-overview">' +
      ui.heading(
        'Warehouse overview',
        'Stock visibility and the operations that need your attention.',
        '<a href="#reports" class="button secondary">View reports</a>',
      ) +
      `<div class="overview-actions"><div class="quick-actions" aria-label="Stock operations">${ui.actionButton('receipt', 'Create receipt')}${ui.actionButton('issue', 'Create issue', 'secondary')}${ui.actionButton('movement', 'Transfer stock', 'secondary')}${ui.actionButton('count', 'Record count', 'secondary')}</div><div class="demo-banner">${config.mode === 'demo' ? 'Local data · Saved in this browser' : 'Connected to backend · Data from the API'}</div></div>` +
      `<section class="stats" aria-label="Inventory summary">${stats.map(([label, value, note, glyph, color]) => `<div class="stat ${color}"><div class="stat-label">${label}<span class="stat-icon">${icon(glyph === '▣' ? 'inventory' : glyph === '▧' ? 'locations' : glyph === '↘' ? 'receipts' : 'counts')}</span></div><div class="stat-value">${value}</div><div class="stat-note ${color}">${note}</div></div>`).join('')}</section>` +
      (discrepancies.length
        ? `<div class="alert-box"><div><strong>${discrepancies.length} stock count ${discrepancies.length === 1 ? 'discrepancy' : 'discrepancies'}</strong>Review the recorded differences in count ${documentCode(latest.id)}.</div><a class="button secondary" href="#counts">View differences →</a></div>`
        : '') +
      `<div class="dashboard-grid"><section class="card overview-replenish">${ui.cardTitle('Products to replenish', 'Reorder levels apply across all warehouses.', '<a href="#inventory" class="button quiet">View inventory →</a>')}${ui.table(
        ['Product', 'Current stock', 'Reorder level'],
        selectors
          .lowProducts()
          .map(
            (p) =>
              `<tr><td>${ui.pcell(p)}</td><td class="number stock-warning">${num(selectors.total(p.id))} <small>${esc(p.unit)}</small></td><td>${num(p.reorderLevel)} ${esc(p.unit)}</td></tr>`,
          )
          .join('') ||
          `<tr><td colspan="3">${emptyPanel('inventory', 'No products to replenish', 'Products below their reorder level appear here.')}</td></tr>`,
      )}</section><section class="card overview-count">${ui.cardTitle('Latest stock count', latest ? documentCode(latest.id) + ' · ' + date(latest.date) : 'No stock count recorded yet.', '<a href="#counts" class="button quiet">View counts →</a>')}${
        latest
          ? ui.table(
              ['Product / count', 'Difference'],
              latest.lines
                .map((line) => {
                  const product = selectors.product(line.productId);
                  const difference =
                    line.countedQuantity - line.recordedQuantity;
                  return `<tr><td>${ui.pcell(product)}<small class="count-detail">${esc(selectors.binName(line.binId))}<br>Recorded ${num(line.recordedQuantity)} · Counted ${num(line.countedQuantity)}</small></td><td class="number ${difference ? 'danger' : ''}">${num(difference)} ${esc(product.unit)}</td></tr>`;
                })
                .join(''),
            )
          : emptyPanel(
              'counts',
              'No stock count yet',
              'Record a count to compare physical and recorded stock.',
            )
      }</section><section class="card overview-occupancy">${ui.cardTitle('Bin occupancy', 'Occupied locations by warehouse.', '<a href="#locations" class="button quiet">View locations →</a>')}<div class="card-body">${
        ctx.state.warehouses
          .map((w) => {
            const bins = ctx.state.bins.filter((b) => b.warehouseId === w.id),
              used = bins.filter((b) =>
                ctx.state.stock.some(
                  (row) => row.binId === b.id && row.quantity > 0,
                ),
              ).length;
            return `<div class="warehouse-row"><div class="row-between"><h3>${esc(w.name)}</h3><small>${used}/${bins.length} ${bins.length === 1 ? 'bin' : 'bins'}</small></div><div class="progress-track" role="img" aria-label="${esc(w.name)}: ${used} of ${bins.length} bins occupied"><div class="progress-fill" style="width:${bins.length ? (used / bins.length) * 100 : 0}%"></div></div></div>`;
          })
          .join('') ||
        emptyPanel(
          'locations',
          'No warehouse locations yet',
          'Add warehouses and bins in Locations to see occupancy.',
        )
      }<p class="info-note">Occupied locations, not storage capacity.</p></div></section><section class="card overview-activity">${ui.cardTitle('Recent activity', 'Latest recorded stock transactions.')}<div class="card-body">${events.map((e) => `<div class="activity"><span class="activity-icon">${icon(e.type === 'Goods receipts' ? 'receipts' : e.type === 'Goods issues' ? 'issues' : 'movements')}</span><div><p><strong>${e.type}</strong> · ${documentCode(e.id)}</p><small>${esc(e.detail)}${e.type === 'Stock movements' ? ' · ' + esc(selectors.staffName(e.staffId)) : ''}</small></div><time>${date(e.date)}</time></div>`).join('') || emptyPanel('movements', 'No stock activity yet', 'Recorded receipts, issues and transfers appear here.')}</div></section></div></div>`
    );
  }
  return { dashboard };
}
