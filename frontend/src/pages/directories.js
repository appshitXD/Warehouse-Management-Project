import { esc, date, time, documentCode, num } from '../utils/format.js';
import { taskLabels, orderLabels } from '../config.js';
export function createDirectories({ ctx, ui, selectors }) {
  const editButton = (type, id) =>
    '<button class="button quiet" data-operation="' +
    type +
    '" data-edit="' +
    esc(id) +
    '">Edit</button>';

  const viewButton = (id) =>
    '<button class="button quiet" data-detail="' +
    esc(id) +
    '">View related records</button>';

  function directorySearch(placeholder, extra = '') {
    return (
      '<div class="toolbar"><label class="filter-field search-field"><span>Search</span><input class="search" id="search" type="search" value="' +
      esc(ctx.filters.search) +
      '" placeholder="' +
      placeholder +
      '" aria-label="Search"></label>' +
      extra +
      '</div>'
    );
  }

  function peoplePage(type) {
    const isStaff = type === 'staff',
      rows = ctx.state[isStaff ? 'staff' : 'customers'],
      selected = rows.find((p) => p.id === ctx.filters.detail);
    const title = isStaff ? 'Staff' : 'Customers';
    let html =
      ui.heading(
        title,
        isStaff
          ? 'Staff profiles and assigned tasks.'
          : 'Customer profiles and related orders.',
        ui.actionButton(type, isStaff ? 'Add staff' : 'Add customer'),
      ) +
      '<section class="card directory-card">' +
      ui.cardTitle(title + ' directory', 'Profiles and related records.') +
      directorySearch('Search ID, name, phone or address…') +
      ui.table(
        [
          'ID',
          'Name',
          'Phone',
          'Address',
          isStaff ? 'Tasks' : 'Orders',
          'Actions',
        ],
        rows
          .filter(selectors.personMatches)
          .map(
            (p) =>
              '<tr><td>' +
              esc(p.id) +
              '</td><td>' +
              esc(p.name) +
              '</td><td>' +
              esc(p.phone || '—') +
              '</td><td class="address-cell">' +
              esc(p.address || '—') +
              '</td><td>' +
              ctx.state[isStaff ? 'tasks' : 'orders'].filter(
                (r) => r[isStaff ? 'staffId' : 'customerId'] === p.id,
              ).length +
              '</td><td class="row-actions">' +
              viewButton(p.id) +
              editButton(type, p.id) +
              '</td></tr>',
          )
          .join(''),
      ) +
      '</section>';
    if (selected) {
      html +=
        '<section class="card">' +
        ui.cardTitle(
          esc(selected.name),
          isStaff ? 'Assigned tasks' : 'Customer orders',
        );
      if (isStaff)
        html += ui.table(
          ['Task ID', 'Name', 'Status'],
          ctx.state.tasks
            .filter((t) => t.staffId === selected.id)
            .map(
              (t) =>
                '<tr><td>' +
                esc(t.id) +
                '</td><td>' +
                esc(t.name) +
                '</td><td>' +
                ui.badge(taskLabels[t.status], 'gray') +
                '</td></tr>',
            )
            .join(''),
        );
      else
        html += orderTable(
          ctx.state.orders.filter((o) => o.customerId === selected.id),
          false,
        );
      html += '</section>';
    }
    return html;
  }

  function orderTable(rows, actions = true) {
    return ui.table(
      [
        'Order ID',
        'Customers',
        'Date',
        'Status',
        'Goods issues',
        ...(actions ? ['Actions'] : []),
      ],
      rows
        .map(
          (o) =>
            '<tr><td>' +
            esc(o.id) +
            '</td><td>' +
            esc(selectors.customerName(o.customerId)) +
            '</td><td>' +
            (o.date
              ? date(o.date) + ' · ' + time(o.date)
              : 'Date unavailable') +
            '</td><td>' +
            ui.badge(
              orderLabels[o.status],
              o.status === 'open' ? 'green' : 'gray',
            ) +
            '</td><td>' +
            ctx.state.issues.filter((i) => i.orderId === o.id).length +
            '</td>' +
            (actions
              ? '<td class="row-actions">' +
                viewButton(o.id) +
                editButton('order', o.id) +
                '</td>'
              : '') +
            '</tr>',
        )
        .join(''),
    );
  }

  function ordersPage() {
    const selected = ctx.state.orders.find((o) => o.id === ctx.filters.detail),
      query = ctx.filters.search.toLocaleLowerCase('en');
    const rows = ctx.state.orders.filter(
      (o) =>
        (o.id + ' ' + selectors.customerName(o.customerId))
          .toLocaleLowerCase('en')
          .includes(query) &&
        (!ctx.filters.customer || o.customerId === ctx.filters.customer),
    );
    let html =
      ui.heading(
        'Customer orders',
        'Connect customers to their goods issues.',
        ui.actionButton('order', 'Add order'),
      ) +
      '<section class="card orders-card">' +
      ui.cardTitle(
        'Customer orders',
        'Select a record to see its goods issues.',
      ) +
      directorySearch(
        'Search order ID or customer name…',
        '<label class="filter-field"><span>Customers</span><select id="customer-filter"><option value="">All customers</option>' +
          ui.options(ctx.state.customers, (c) => c.name, ctx.filters.customer) +
          '</select></label>',
      ) +
      orderTable(rows) +
      '</section>';
    if (selected)
      html +=
        '<section class="card">' +
        ui.cardTitle(
          'Goods issues for ' + esc(selected.id),
          esc(selectors.customerName(selected.customerId)),
        ) +
        ui.table(
          ['Document ID', 'Date', 'Status', 'Lines'],
          ctx.state.issues
            .filter((i) => i.orderId === selected.id)
            .map(
              (i) =>
                '<tr><td>' +
                documentCode(i.id) +
                '</td><td>' +
                date(i.date) +
                '</td><td>' +
                esc(i.status) +
                '</td><td>' +
                i.lines
                  .map(
                    (l) =>
                      esc(selectors.product(l.productId).id) +
                      ' · ' +
                      num(l.quantity) +
                      ' ' +
                      esc(selectors.product(l.productId).unit),
                  )
                  .join('<br>') +
                '</td></tr>',
            )
            .join(''),
        ) +
        '</section>';
    return html;
  }

  function suppliersPage() {
    return (
      ui.heading(
        'Suppliers',
        'Manage suppliers before receiving deliveries.',
        ui.actionButton('supplier', 'Add supplier'),
      ) +
      '<section class="card directory-card">' +
      ui.cardTitle(
        'Supplier directory',
        'Contact details for goods receipts.',
      ) +
      directorySearch('Search ID, name, phone or address…') +
      ui.table(
        ['ID', 'Name', 'Phone', 'Address', 'Actions'],
        ctx.state.suppliers
          .filter(selectors.personMatches)
          .map(
            (p) =>
              '<tr><td>' +
              esc(p.id) +
              '</td><td>' +
              esc(p.name) +
              '</td><td>' +
              esc(p.phone || '—') +
              '</td><td class="address-cell">' +
              esc(p.address || '—') +
              '</td><td>' +
              editButton('supplier', p.id) +
              '</td></tr>',
          )
          .join(''),
      ) +
      '</section>'
    );
  }
  return {
    editButton,
    viewButton,
    directorySearch,
    peoplePage,
    orderTable,
    ordersPage,
    suppliersPage,
  };
}
