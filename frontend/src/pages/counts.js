import { documentCode, date, esc, round, num } from '../utils/format.js';
export function createCounts({ ui, ctx, selectors, config }) {
  function counts() {
    return (
      ui.heading(
        'Stock counts',
        'Compare physical counts with recorded stock at the time of submission.',
        ui.actionButton('count', 'Record stock count'),
      ) +
      (config?.mode === 'http'
        ? `<div class="demo-banner">A count changes stock only when it is Completed: each bin is set to the counted quantity. Difference = counted − recorded before adjustment. Pending counts can be edited or deleted; Completed counts are locked.</div>`
        : `<div class="demo-banner">New counts update stock to counted quantities and write stock history. Difference = counted − recorded before adjustment. Existing counts retain their original snapshots.</div>`) +
      (ctx.state.counts.length
        ? ctx.state.counts
            .map(
              (c, i) =>
                `<section class="card count-card">${ui.cardTitle(`${documentCode(c.id)} ${i === 0 ? ui.badge('Latest', 'gray') : ''}`, `${date(c.date)} · ${esc(c.status)}`, ui.badge(c.lines.some((l) => l.countedQuantity !== l.recordedQuantity) ? 'Discrepancy' : 'Matched', c.lines.some((l) => l.countedQuantity !== l.recordedQuantity) ? 'amber' : 'green') + ui.documentActions('count', c.id, { edit: c.status !== 'Completed', remove: c.status === 'Pending' }))}${ui.table(
                  [
                    'Product',
                    'Bin',
                    'Recorded stock',
                    'Counted quantity',
                    'Difference',
                  ],
                  c.lines
                    .map((l) => {
                      const diff = round(
                        l.countedQuantity - l.recordedQuantity,
                      );
                      return `<tr><td>${ui.pcell(selectors.product(l.productId))}</td><td>${esc(selectors.binName(l.binId))}</td><td>${num(l.recordedQuantity)}</td><td>${num(l.countedQuantity)}</td><td class="number ${diff ? 'danger' : ''}">${diff > 0 ? '+' : ''}${num(diff)} ${esc(selectors.product(l.productId).unit)}</td></tr>`;
                    })
                    .join(''),
                )}</section>`,
            )
            .join('')
        : ui.emptyState(
            'No stock counts yet.',
            'Record your first stock count to compare quantities.',
            ui.actionButton('count', 'Record stock count', 'secondary'),
          ))
    );
  }
  return { counts };
}
