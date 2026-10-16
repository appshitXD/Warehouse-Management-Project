import { documentCode, date, num, esc } from '../utils/format.js';
export function createMovements({ ctx, selectors, ui }) {
  function movements() {
    const rows = ctx.state.movements.filter(
      (m) =>
        selectors.matchesProduct(m.productId) &&
        (selectors.matchesBin(m.fromBinId) || selectors.matchesBin(m.toBinId)),
    );
    return (
      ui.heading(
        'Stock movements',
        'Transfer stock between bins or warehouses in one operation.',
        ui.actionButton('movement', 'Create movement'),
      ) +
      `<section class="card movements-card">${ui.toolbar()}${ui.table(['ID / date', 'Product', 'From bin', 'To bin', 'Quantity', 'Performed by'], rows.map((m) => `<tr><td>${documentCode(m.id)}<br><small class="muted">${date(m.date)}</small></td><td>${ui.pcell(selectors.product(m.productId))}</td><td>${ui.badge(selectors.binName(m.fromBinId), 'gray')}</td><td>${ui.badge(selectors.binName(m.toBinId), 'green')}</td><td class="number">${num(m.quantity)} ${esc(selectors.product(m.productId).unit)}</td><td>${esc(selectors.staffName(m.staffId))}</td></tr>`).join(''))}</section>`
    );
  }
  return { movements };
}
