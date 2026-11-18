import { date } from '../utils/format.js';

export function reportDateLabel(value) {
  if (!value) return '—';
  if (/^\d{4}-\d{2}-\d{2}$/.test(value))
    return value.split('-').reverse().join('/');
  return date(value);
}

export function reportContent(
  ui,
  ctx,
  config,
  key,
  localItems,
  renderItems,
  emptyMessage,
) {
  const report = ctx.operationReports.get(key);
  if (config.mode === 'http') {
    if (
      (key === 'productHistory' && report?.productId !== ctx.filters.product) ||
      (key === 'slowMoving' && report?.asOf !== ctx.reportsAsOf)
    )
      return ui.loadingState('Loading report…');
    if (report?.error)
      return ui.emptyState(
        'Unable to load this report',
        report.error,
        `<button class="button secondary" data-retry-operation-report="${key}">Retry</button>`,
      );
    if (!report?.items) return ui.loadingState('Loading report…');
  }
  const items = config.mode === 'http' ? report.items : localItems();
  return items.length
    ? renderItems(items)
    : `<div class="card-body"><p class="empty">${emptyMessage}</p></div>`;
}
