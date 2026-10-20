import { documentCode, date, time, esc, num } from '../utils/format.js';
export function createDocuments({ ctx, selectors, ui }) {
  function documentsPage(type) {
    const receipt = type === 'receipts',
      docs = ctx.state[type].filter(
        (d) =>
          d.lines.some((l) => selectors.matchesBin(l.binId)) &&
          `${d.id} ${d.lines.map((l) => selectors.product(l.productId).name).join(' ')}`
            .toLocaleLowerCase('en')
            .includes(ctx.filters.search.toLocaleLowerCase('en')),
      );
    return (
      ui.heading(
        receipt ? 'Goods receipts' : 'Goods issues',
        receipt
          ? 'Receive supplier deliveries into bins.'
          : 'Issue stock against customer orders after checking availability.',
        ui.actionButton(
          receipt ? 'receipt' : 'issue',
          receipt ? 'Create receipt' : 'Create issue',
        ),
      ) +
      `<section class="card documents-card">${ui.toolbar('Search document ID or product…')}${ui.table(['Document / date', receipt ? 'Supplier' : 'Customer orders', 'Document lines', ...(receipt ? [] : ['Status', 'State of goods'])], docs.map((d) => `<tr><td><strong>${documentCode(d.id)}</strong><br><small class="muted">${date(d.date)} · ${time(d.date)}</small></td><td>${esc(receipt ? ctx.state.suppliers.find((s) => s.id === d.supplierId)?.name : d.orderId + ' · ' + selectors.customerName(ctx.state.orders.find((o) => o.id === d.orderId)?.customerId))}</td><td class="inline-detail document-lines">${d.lines.map((l) => `<div><span class="muted">${receipt ? '#' + l.lineNo : esc(l.id)}</span> ${esc(selectors.product(l.productId).id)} · <strong>${num(l.quantity)} ${esc(selectors.product(l.productId).unit)}</strong> → ${esc(selectors.binName(l.binId))}</div>`).join('')}</td>${receipt ? '' : `<td>${ui.badge(d.status, 'gray')}</td><td>${esc(d.stateOfGood || '—')}</td>`}</tr>`).join(''))}<div class="table-footer">${docs.length} ${docs.length === 1 ? 'document' : 'documents'} · ${receipt ? 'Receipt lines use receipt ID and line number' : 'Issue lines use Issue Line ID'}</div></section>`
    );
  }
  return { documentsPage };
}
