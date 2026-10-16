import { icon } from './navigation.js';
import { esc } from '../utils/format.js';
export function createUi({ ctx }) {
  const badge = (label, color = 'green') =>
    `<span class="pill ${color}">${esc(label)}</span>`;

  const pcell = (p) =>
    `<div class="product-cell"><div><strong>${esc(p.name)}</strong><small>${esc(p.id)} · ${esc(p.unit)}</small></div></div>`;

  const emptyRow = (columns) =>
    `<tr><td colspan="${columns}" class="empty"><div class="table-empty"><span class="empty-icon" aria-hidden="true">${icon('inventory')}</span><strong>No matching records.</strong><span>Use the page actions to add records, or adjust your filters.</span></div></td></tr>`;

  const table = (headers, rows) => {
    const numeric = headers
      .map((h, i) =>
        /^(quantity|current stock|reorder level|total stock|recorded|counted|difference|tasks|orders)/i.test(
          h,
        )
          ? 'numeric-col-' + (i + 1)
          : '',
      )
      .join(' ');
    return `<div class="table-wrap" role="region" aria-label="${esc(headers.join(', '))} table" tabindex="0"><table class="${numeric}"><thead><tr>${headers.map((h) => `<th scope="col">${h}</th>`).join('')}</tr></thead><tbody>${rows || emptyRow(headers.length)}</tbody></table></div>`;
  };

  const options = (items, label, selected = '') =>
    items
      .map(
        (i) =>
          `<option value="${esc(i.id)}" ${i.id === selected ? 'selected' : ''}>${esc(label(i))}</option>`,
      )
      .join('');

  function warehouseFilter() {
    return `<select id="warehouse-filter" aria-label="Filter by warehouse"><option value="">All warehouses</option>${options(ctx.state.warehouses, (w) => w.name, ctx.filters.warehouse)}</select>`;
  }

  function toolbar(placeholder = 'Search by product ID or name…', extra = '') {
    return `<div class="toolbar"><label class="filter-field search-field"><span>Search</span><input class="search" id="search" type="search" value="${esc(ctx.filters.search)}" placeholder="${placeholder}" aria-label="Search"></label><label class="filter-field"><span>Warehouse</span>${warehouseFilter()}</label>${extra}</div>`;
  }

  function heading(title, subtitle, action = '') {
    action += `<button class="button secondary" id="refresh-data" ${ctx.refreshing ? 'disabled' : ''}>${ctx.refreshing ? 'Loading…' : icon('refresh') + ' Refresh'}</button>`;
    return `<div class="page-heading"><div><p class="eyebrow">WAREHOUSE MANAGEMENT</p><h1>${title}</h1><p class="subtitle">${subtitle}</p></div><div class="heading-actions">${action}</div></div>`;
  }

  function actionButton(type, label, variant = 'primary') {
    return `<button class="button ${variant}" data-operation="${type}">${icon('plus')} ${label}</button>`;
  }

  function recordActions(type, id) {
    return `<div class="record-actions"><button class="button quiet" data-operation="${type}" data-edit="${esc(id)}" aria-label="Edit ${type} ${esc(id)}">Edit</button><button class="button quiet danger" data-operation="delete-${type}" data-edit="${esc(id)}" aria-label="Delete ${type} ${esc(id)}">Delete</button></div>`;
  }

  function cardTitle(title, subtitle = '', action = '') {
    return `<div class="card-heading"><div><h2>${title}</h2>${subtitle ? `<p>${subtitle}</p>` : ''}</div>${action}</div>`;
  }

  function emptyState(title, description = '', action = '') {
    return (
      '<section class="card empty-state"><h2>' +
      esc(title) +
      '</h2><p>' +
      esc(description) +
      '</p>' +
      action +
      '</section>'
    );
  }

  function loadingState(message = 'Loading warehouse data…') {
    return (
      '<section class="card loading-state" role="status"><span class="spinner" aria-hidden="true"></span><span>' +
      esc(message) +
      '</span></section>'
    );
  }

  function statusBanner() {
    if (ctx.pendingMutation)
      return (
        '<div class="alert-box" role="alert"><div><strong>Unconfirmed save result</strong>Do not create another record. Retry the previous request to confirm its result without duplicating it.</div><button class="button secondary" id="retry-pending" ' +
        (ctx.submitting ? 'disabled' : '') +
        '>Retry previous request</button></div>'
      );
    if (ctx.stale || ctx.refreshError)
      return (
        '<div class="alert-box" role="alert"><div><strong>' +
        esc(
          ctx.stale
            ? 'Displayed data may be outdated'
            : 'Unable to refresh data',
        ) +
        '</strong>' +
        esc(ctx.refreshError) +
        '</div><button class="button secondary" id="retry-data" ' +
        (ctx.refreshing ? 'disabled' : '') +
        '>Refresh data</button></div>'
      );
    return ctx.refreshing ? loadingState('Refreshing data…') : '';
  }
  return {
    badge,
    pcell,
    emptyRow,
    table,
    options,
    warehouseFilter,
    toolbar,
    heading,
    actionButton,
    recordActions,
    cardTitle,
    emptyState,
    loadingState,
    statusBanner,
  };
}
