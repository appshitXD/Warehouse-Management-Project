import { documentCode, date, time, esc, num } from '../utils/format.js';
import { canChangeDocuments } from '../config.js';
export function createDocuments({ ctx, config, selectors, ui }) {
  function issuePage() {
    const selected = ctx.filters.detail;
    return (
      ui.heading(
        'Goods issues',
        'Select a goods issue to view its lines.',
        ui.actionButton('issue', 'Create issue'),
      ) +
      '<section class="card documents-card">' +
      ui.cardTitle('Goods issue list') +
      `<div class="toolbar"><label class="filter-field search-field"><span>Search issues</span><input class="search" id="search" type="search" value="${esc(ctx.filters.search)}" placeholder="Issue, order or customer"></label></div>` +
      ui.directoryContent(
        'issues',
        'goods issues',
        (issues) =>
          ui.table(
            [
              'Issue / date',
              'Order',
              'Customer',
              'Status',
              'State of goods',
              'Actions',
            ],
            issues
              .map(
                (row) =>
                  `<tr class="issue-row" data-issue="${esc(row.id)}"><td><button type="button" class="button quiet" aria-controls="issue-detail" aria-pressed="${row.id === selected}">${esc(row.id)}</button><br><small class="muted">${date(row.date)} · ${time(row.date)}</small></td><td>${esc(row.orderId)}</td><td>${esc(row.customerName)}</td><td>${ui.badge(row.status, 'gray')}</td><td>${esc(row.stateOfGood || '—')}</td><td>${ui.documentActions('issue', row.id, { edit: row.status !== 'Completed', remove: row.status === 'Pending' })}</td></tr>`,
              )
              .join(''),
          ) +
          `<div class="table-footer">${issues.length} ${issues.length === 1 ? 'issue' : 'issues'}</div>`,
      ) +
      '</section>' +
      `<section class="card" id="issue-detail" aria-labelledby="issue-detail-heading"><div class="card-heading"><h2 id="issue-detail-heading" tabindex="-1">${selected ? 'Lines for ' + esc(selected) : 'Issue lines'}</h2></div>` +
      (selected
        ? ui.directoryContent('issueLines', 'issue lines', (lines) =>
            ui.table(
              ['Line', 'Product', 'Bin', 'Quantity'],
              lines
                .map(
                  (line) =>
                    `<tr><td>${esc(line.id)}</td><td>${esc(line.productId)} · ${esc(line.productName)}</td><td>${esc(selectors.binName(line.binId))}</td><td class="number">${num(line.quantity)} ${esc(line.unit)}</td></tr>`,
                )
                .join(''),
            ),
          )
        : '<div class="card-body"><p class="empty">Select a goods issue above.</p></div>') +
      '</section>'
    );
  }

  function documentsPage(type) {
    if (type === 'issues' && config.mode === 'http') return issuePage();
    const receipt = type === 'receipts',
      actions = receipt && canChangeDocuments(config, 'receipt'),
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
      `<section class="card documents-card">${ui.toolbar('Search document ID or product…')}${ui.table(['Document / date', receipt ? 'Supplier' : 'Customer orders', 'Document lines', ...(receipt ? [] : ['Status', 'State of goods']), ...(actions ? ['Actions'] : [])], docs.map((d) => `<tr><td><strong>${documentCode(d.id)}</strong><br><small class="muted">${date(d.date)} · ${time(d.date)}</small></td><td>${esc(receipt ? ctx.state.suppliers.find((s) => s.id === d.supplierId)?.name : d.orderId + ' · ' + selectors.customerName(ctx.state.orders.find((o) => o.id === d.orderId)?.customerId))}</td><td class="inline-detail document-lines">${d.lines.map((l) => `<div><span class="muted">${receipt ? '#' + l.lineNo : esc(l.id)}</span> ${esc(selectors.product(l.productId).id)} · <strong>${num(l.quantity)} ${esc(selectors.product(l.productId).unit)}</strong> → ${esc(selectors.binName(l.binId))}</div>`).join('')}</td>${receipt ? '' : `<td>${ui.badge(d.status, 'gray')}</td><td>${esc(d.stateOfGood || '—')}</td>`}${actions ? `<td>${ui.documentActions(receipt ? 'receipt' : 'issue', d.id)}</td>` : ''}</tr>`).join(''))}<div class="table-footer">${docs.length} ${docs.length === 1 ? 'document' : 'documents'} · ${receipt ? 'Receipt lines use receipt ID and line number' : 'Issue lines use Issue Line ID'}</div></section>`
    );
  }
  return { documentsPage };
}
