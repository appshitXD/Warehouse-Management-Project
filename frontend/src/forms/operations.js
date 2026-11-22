import {
  describeForm,
  clearFormErrors,
  presentFormErrors,
} from './accessibility.js';
import { esc, num } from '../utils/format.js';
import {
  canWrite,
  httpCountLabels,
  httpIssueLabels,
  httpOrderLabels,
  httpTaskLabels,
  orderLabels,
} from '../config.js';
import { storageRequirements, locationKey } from '../domain/catalogue.js';
export function createForms({
  $,
  $$,
  ui,
  ctx,
  stock,
  selectors,
  controller,
  env,
  config,
}) {
  const orderStatuses = config.mode === 'http' ? httpOrderLabels : orderLabels;
  const initialOrderStatus = config.mode === 'http' ? 'Pending' : 'open';
  const field = (label, name, control, full = false) =>
    `<label class="field ${full ? 'full-width' : ''}"><span>${label}</span>${control}</label>`;

  const select = (name, items, label) =>
    `<select name="${name}" required>${ui.options(items, label)}</select>`;

  const input = (name, type = 'text', extra = '') =>
    `<input name="${name}" type="${type}" required ${extra}>`;

  function operationMissing(type) {
    const missing = [];
    if (['product', 'customer', 'supplier', 'warehouse'].includes(type))
      return missing;
    if (['staff', 'zone', 'aisle', 'bin'].includes(type)) {
      if (!ctx.state.warehouses.length) missing.push('warehouses');
      if (['aisle', 'bin'].includes(type) && !ctx.state.zones.length)
        missing.push('zones');
      if (type === 'bin' && !ctx.state.aisles.length) missing.push('aisles');
      return missing;
    }
    if (type === 'order') {
      if (!ctx.state.customers.length) missing.push('customers');
      return missing;
    }
    if (['task', 'movement'].includes(type) && !ctx.state.staff.length)
      missing.push('staff');
    if (type === 'task') return missing;
    if (!ctx.state.products.length) missing.push('products');
    if (!ctx.state.bins.length) missing.push('bin');
    if (type === 'receipt' && !ctx.state.suppliers.length)
      missing.push('suppliers');
    if (
      type === 'issue' &&
      !ctx.state.orders.some((o) => o.status === initialOrderStatus)
    )
      missing.push('customer orders eligible for issue');
    return missing;
  }

  // line = a saved line when editing a document, so the row starts with its values.
  function lineForm(line = null) {
    const p = line ? selectors.product(line.productId) : ctx.state.products[0],
      bins = p ? stock.eligibleBins(ctx.operation, p.id) : [];
    if (
      line &&
      !bins.some((x) => x.id === line.binId) &&
      selectors.bin(line.binId)
    )
      bins.unshift(selectors.bin(line.binId));
    const b = line ? selectors.bin(line.binId) : bins[0];
    const address =
      '<small class="bin-address" aria-live="polite">' +
      esc(b ? selectors.binName(b.id) : 'No bins available.') +
      '</small>';
    // The database stores whole quantities (INT), so HTTP mode only accepts whole numbers.
    const whole = config.mode === 'http';
    const quantityField = input(
      'quantity',
      'number',
      'min="' +
        (ctx.operation === 'count' ? 0 : whole ? 1 : 0.001) +
        '" step="' +
        (whole ? 1 : 0.001) +
        '" value="' +
        (line
          ? (line.countedQuantity ?? line.quantity)
          : ctx.operation === 'count'
            ? 0
            : 1) +
        '"',
    );
    return (
      '<div class="line-row">' +
      field(
        'Product',
        'productId',
        `<select name="productId" required>${ui.options(ctx.state.products, (p) => p.id + ' · ' + p.name, p?.id || '')}</select>`,
      ) +
      field(
        'Bin',
        'binId',
        `<select name="binId" required>${ui.options(bins, (b) => selectors.binName(b.id), b?.id || '')}</select>` +
          address,
      ) +
      field(
        ctx.operation === 'count' ? 'Counted quantity' : 'Quantity',
        'quantity',
        quantityField,
      ) +
      '<button type="button" class="icon-button remove-line" aria-label="Remove line">×</button><small class="stock-hint" aria-live="polite"></small><p class="line-error" role="alert"></p></div>'
    );
  }

  function syncLine(row) {
    const pid = $('[name=productId]', row).value,
      control = $('[name=binId]', row),
      old = control.value;
    const bins = stock.eligibleBins(ctx.operation, pid);
    control.innerHTML = bins.length
      ? ui.options(
          bins,
          (b) => selectors.binName(b.id),
          bins.some((b) => b.id === old) ? old : bins[0].id,
        )
      : '<option value="">No bins with stock</option>';
    const b = selectors.bin(control.value),
      p = selectors.product(pid),
      q = b ? selectors.quantity(pid, b.id) : 0;
    $('.bin-address', row).textContent = b
      ? selectors.binName(b.id)
      : 'Select another product or add a bin and stock.';
    $('.stock-hint', row).textContent =
      b && p
        ? (ctx.operation === 'count'
            ? 'Reference stock: '
            : 'Current stock: ') +
          num(q) +
          ' ' +
          p.unit +
          (ctx.operation === 'issue'
            ? ' · Availability is checked again before issuing.'
            : '')
        : '';
    const quantityInput = $('[name=quantity]', row);
    if (ctx.operation === 'issue') quantityInput.max = q;
    else quantityInput.removeAttribute('max');
    $('.line-error', row).textContent = '';
  }

  function syncMovement() {
    const form = $('#operation-form'),
      pid = $('[name=productId]', form).value,
      source = $('[name=fromBinId]', form),
      target = $('[name=toBinId]', form);
    const sourceBins = stock.eligibleBins('movement-source', pid),
      oldSource = source.value;
    source.innerHTML = sourceBins.length
      ? ui.options(
          sourceBins,
          (b) => selectors.binName(b.id),
          sourceBins.some((b) => b.id === oldSource)
            ? oldSource
            : sourceBins[0].id,
        )
      : '<option value="">No source stock available</option>';
    const targets = stock.eligibleBins('movement-target', pid, source.value),
      oldTarget = target.value;
    target.innerHTML = targets.length
      ? ui.options(
          targets,
          (b) => selectors.binName(b.id),
          targets.some((b) => b.id === oldTarget) ? oldTarget : targets[0].id,
        )
      : '<option value="">No destination bin</option>';
    const q = selectors.quantity(pid, source.value),
      p = selectors.product(pid);
    $('[name=quantity]', form).max = q;
    $('.movement-hint', form).textContent =
      source.value && target.value
        ? 'Current source stock: ' +
          num(q) +
          ' ' +
          p.unit +
          ' · Destination: ' +
          selectors.binName(target.value)
        : 'A source with stock and a different destination are required.';
  }

  // Status options a document may move to: never back to an earlier status.
  function statusSelect(labels, current, name = 'status') {
    const order = Object.keys(labels),
      from = Math.max(0, order.indexOf(current));
    return `<select name="${name}" required>${order
      .slice(from)
      .map(
        (v) =>
          `<option value="${esc(v)}" ${v === current ? 'selected' : ''}>${esc(labels[v])}</option>`,
      )
      .join('')}</select>`;
  }

  // Goods receipt, goods issue or stock count: header fields, then one row per line.
  // record = the saved document when editing.
  function documentForm(type, record) {
    const http = config.mode === 'http';
    // A shipped issue has already left the warehouse: only its status can move on.
    if (type === 'issue' && record && record.status !== 'Pending')
      return `<div class="form-grid">${field('Status', 'status', statusSelect(httpIssueLabels, record.status))}</div><p class="info-note">${esc(record.id)} is ${esc(record.status)}: its goods have already left the bins, so only its status can still move on (Shipped → Completed).</p>`;
    const orders = ctx.state.orders.filter(
      (o) => o.status === initialOrderStatus || o.id === record?.orderId,
    );
    const header =
      type === 'receipt'
        ? field(
            'Supplier',
            'supplierId',
            `<select name="supplierId" required>${ui.options(ctx.state.suppliers, (x) => x.name, record?.supplierId || '')}</select>`,
          )
        : type === 'issue'
          ? field(
              'Customer order',
              'orderId',
              `<select name="orderId" required>${ui.options(orders, (o) => o.id + ' · ' + selectors.customerName(o.customerId), record?.orderId || '')}</select>`,
            ) +
            field(
              'Status',
              'status',
              http
                ? statusSelect(httpIssueLabels, record?.status || 'Pending')
                : input('status', 'text', 'maxlength="40" value="recorded"'),
            ) +
            field(
              'State of goods',
              'stateOfGood',
              `<input name="stateOfGood" maxlength="50" required placeholder="Good, Fragile, Perishable…" value="${esc(record?.stateOfGood || '')}">`,
            )
          : http
            ? field(
                'Status',
                'status',
                statusSelect(httpCountLabels, record?.status || 'Pending'),
              )
            : '';
    const notes = http
      ? {
          receipt:
            'Saving adds each line to the stock of its bin. Editing the lines changes stock by the difference.',
          issue:
            'Stock leaves the bins only when the status is Shipped or Completed. A Pending issue can still be edited or deleted.',
          count:
            'Stock changes only when the count is Completed: each bin is set to the counted quantity, and the stock just before is kept as the recorded quantity.',
        }
      : {
          receipt: 'Select bins. Every line is checked before updating stock.',
          issue:
            'Saving deducts stock immediately. Status is descriptive and does not create a draft.',
          count:
            'Saving replaces stock with the counted quantities and writes stock history. The previous stock is retained for reconciliation. Complete the count before resuming movements.',
        };
    const rows = record?.lines?.length
      ? record.lines.map((l) => lineForm(l)).join('')
      : lineForm();
    return `<div class="form-grid">${header}</div><h3 class="line-heading">${type === 'count' ? 'Count lines' : 'Goods lines'}</h3><p class="info-note">${notes[type]}</p><div id="form-lines">${rows}</div><button type="button" id="add-line" class="button quiet add-line-button">＋ Add line</button>`;
  }

  function openOperation(type, id = '') {
    if (!canWrite(config, type)) return false;
    if (ctx.submitting || ctx.refreshing || ctx.stale || ctx.pendingMutation) {
      controller.notify(
        ctx.pendingMutation
          ? 'Confirm the pending request before creating another operation.'
          : 'Refresh the data before saving changes.',
      );
      return false;
    }
    clearFormErrors($('#form-fields'), $('#form-error-summary'));
    const dialog = $('#form-dialog');
    dialog.classList.remove('transaction-dialog');
    if (['receipt', 'issue', 'count', 'movement'].includes(type))
      dialog.classList.add('transaction-dialog');
    ctx.operation = type;
    ctx.editingId = id;
    $('#form-error').textContent = '';
    $('#submit-operation').hidden = false;
    $('#submit-operation').disabled = false;
    const deleting = type.startsWith('delete-');
    const entity = deleting ? type.slice(7) : type;
    const missing = deleting ? [] : operationMissing(type);
    if (missing.length) {
      $('#dialog-title').textContent = 'Unable to create this operation';
      $('#form-fields').innerHTML = ui.emptyState(
        'Missing records: ' + missing.join(', '),
        'Add the missing records in the corresponding menu, then try again.',
      );
      $('#submit-operation').hidden = true;
      $('#form-dialog').showModal();
      return false;
    }
    const titles = {
      receipt: 'Create goods receipt',
      issue: 'Create goods issue',
      movement: 'Transfer stock',
      count: 'Record stock count',
      product: 'Add product',
      task: 'Add task',
      staff: 'Add staff',
      customer: 'Add customer',
      order: 'Add customer order',
      supplier: 'Add supplier',
      warehouse: 'Add warehouse',
      zone: 'Add zone',
      aisle: 'Add aisle',
      bin: 'Add bin',
    };
    $('#dialog-title').textContent = id
      ? 'Edit ' +
        {
          staff: 'staff',
          customer: 'customer',
          order: 'customer order',
          supplier: 'supplier',
          warehouse: 'warehouse',
          zone: 'zone',
          aisle: 'aisle',
          product: 'product',
          bin: 'bin',
          receipt: 'goods receipt',
          issue: 'goods issue',
          count: 'stock count',
          task: 'task',
        }[entity]
      : titles[type];
    if (deleting) $('#dialog-title').textContent = 'Delete ' + entity;
    $('#submit-operation').textContent = deleting
      ? 'Delete record'
      : type === 'count' && !id
        ? 'Record count'
        : 'Save';
    const record = id
      ? ctx.state[
          {
            staff: 'staff',
            customer: 'customers',
            order: 'orders',
            supplier: 'suppliers',
            warehouse: 'warehouses',
            zone: 'zones',
            aisle: 'aisles',
            product: 'products',
            bin: 'bins',
            receipt: 'receipts',
            issue: 'issues',
            count: 'counts',
            task: 'tasks',
          }[entity]
        ]?.find((r) => r.id === id)
      : null;
    if (id && !record) {
      controller.notify('Record not found. Refresh the data and try again.');
      return false;
    }
    let html = '';
    if (deleting) {
      const deleteNotes = {
        receipt:
          'Deleting a goods receipt takes its quantities back out of the bins. It is refused if that stock has already left.',
        issue:
          'Only Pending goods issues can be deleted. Nothing has left the bins yet, so stock does not change.',
        count:
          'Only Pending stock counts can be deleted. Stock does not change.',
        order: 'An order that already has a goods issue cannot be deleted.',
        task: 'The task is removed. Stock does not change.',
      };
      html =
        '<p class="info-note">' +
        esc(
          deleteNotes[entity] ||
            'Only unused records can be deleted. Stock and transaction history are preserved.',
        ) +
        '</p><p>Delete <strong>' +
        esc(record.name || record.id) +
        '</strong>? Enter <strong>' +
        esc(record.id) +
        '</strong> to confirm.</p>' +
        field(
          'Record ID confirmation',
          'confirmId',
          input('confirmId', 'text', 'autocomplete="off"'),
        );
    } else if (type === 'staff') {
      const phones = record?.phones || [];
      html = `<div class="form-grid">${field('Staff ID', 'id', input('id', 'text', 'maxlength="10"'))}${field('Name', 'name', input('name', 'text', 'maxlength="100"'))}${field(
        'Warehouse',
        'warehouseId',
        select(
          'warehouseId',
          ctx.state.warehouses,
          (w) => w.id + ' · ' + w.name,
        ),
      )}${field(
        'Supervisor',
        'supervisorId',
        `<select name="supervisorId"><option value="">No supervisor</option>${ui.options(
          ctx.state.staff.filter((p) => p.id !== id),
          (p) => p.id + ' · ' + p.name,
          record?.supervisorId || '',
        )}</select>`,
      )}${field('Address', 'address', '<input name="address" maxlength="150" required>', true)}</div><section class="staff-phones"><h3>Phone numbers</h3><p class="muted">Optional. Each number is stored separately.</p><div id="staff-phone-list">${(phones.length ? phones : ['']).map(phoneForm).join('')}</div><button type="button" class="button secondary" id="add-staff-phone">Add phone number</button></section>`;
    } else if (['customer', 'supplier'].includes(type)) {
      html =
        '<div class="form-grid">' +
        field(
          'ID',
          'id',
          input(
            'id',
            'text',
            'maxlength="10" value="' +
              esc(record?.id || '') +
              '" ' +
              (id ? 'readonly' : ''),
          ),
        ) +
        field(
          'Name',
          'name',
          input(
            'name',
            'text',
            'maxlength="100" value="' + esc(record?.name || '') + '"',
          ),
        ) +
        field(
          'Phone',
          'phone',
          '<input name="phone" type="tel" maxlength="20" required value="' +
            esc(record?.phone || '') +
            '">',
        ) +
        field(
          'Address',
          'address',
          '<input name="address" maxlength="100" required value="' +
            esc(record?.address || '') +
            '">',
          true,
        ) +
        '</div>';
    } else if (type === 'warehouse') {
      html =
        '<div class="form-grid">' +
        field('Warehouse ID', 'id', input('id', 'text', 'maxlength="10"')) +
        field(
          'Warehouse name',
          'name',
          input('name', 'text', 'maxlength="100"'),
        ) +
        field(
          'Address',
          'address',
          '<input name="address" maxlength="255" required>',
          true,
        ) +
        '</div>';
    } else if (['zone', 'aisle', 'bin'].includes(type)) {
      html =
        '<div class="form-grid">' +
        field(
          'Warehouse',
          'warehouseId',
          select(
            'warehouseId',
            ctx.state.warehouses,
            (w) => w.id + ' · ' + w.name,
          ),
        ) +
        (type === 'zone'
          ? field(
              'Zone ID',
              'zoneId',
              input('zoneId', 'text', 'maxlength="10"'),
            )
          : field('Zone', 'zone', '<select name="zone" required></select>')) +
        (type === 'aisle'
          ? field(
              'Aisle ID',
              'aisleId',
              input('aisleId', 'text', 'maxlength="10"'),
            )
          : type === 'bin'
            ? field('Aisle', 'aisle', '<select name="aisle" required></select>')
            : '') +
        (type === 'bin'
          ? field(
              'Position',
              'position',
              input('position', 'text', 'maxlength="10"'),
            )
          : '') +
        field(
          type === 'bin' ? 'Bin name' : 'Name',
          'name',
          `<input name="name" maxlength="${type === 'bin' ? 50 : 100}" required>`,
        ) +
        `</div><p class="info-note">${type === 'zone' ? 'Zone IDs are unique within a warehouse.' : type === 'aisle' ? 'Aisle IDs are unique within a warehouse and zone.' : 'A bin is uniquely identified by its warehouse, zone, aisle and position.'}</p>`;
    } else if (type === 'order') {
      html =
        '<div class="form-grid">' +
        field(
          'Order ID',
          'id',
          input(
            'id',
            'text',
            'maxlength="10" value="' +
              esc(record?.id || '') +
              '" ' +
              (id ? 'readonly' : ''),
          ),
        ) +
        field(
          'Customers',
          'customerId',
          '<select name="customerId" required>' +
            ui.options(
              ctx.state.customers,
              (c) => c.id + ' · ' + c.name,
              record?.customerId || '',
            ) +
            '</select>',
        ) +
        field(
          'Status',
          'status',
          '<select name="status" required>' +
            Object.entries(orderStatuses)
              .map(
                ([v, l]) =>
                  '<option value="' +
                  v +
                  '" ' +
                  (v === (record?.status || initialOrderStatus)
                    ? 'selected'
                    : '') +
                  '>' +
                  l +
                  '</option>',
              )
              .join('') +
            '</select>',
        ) +
        '</div><p class="info-note">Open orders can be selected for goods issues. Creating an order does not change stock.</p>';
    } else if (['receipt', 'issue', 'count'].includes(type)) {
      html = documentForm(type, record);
    } else if (type === 'movement')
      html = `<div class="form-grid">${field(
        'Product',
        'productId',
        select('productId', ctx.state.products, (p) => p.id + ' · ' + p.name),
        true,
      )}${field(
        'From bin',
        'fromBinId',
        select('fromBinId', ctx.state.bins, (b) => selectors.binName(b.id)),
      )}${field(
        'To bin',
        'toBinId',
        select('toBinId', ctx.state.bins, (b) => selectors.binName(b.id)),
      )}${field('Quantity', 'quantity', input('quantity', 'number', config.mode === 'http' ? 'min="1" step="1" value="1"' : 'min="0.001" step="0.001" value="1"'))}${field(
        'Performed by',
        'staffId',
        select('staffId', ctx.state.staff, (s) => s.name),
      )}</div><p class="stock-hint movement-hint" aria-live="polite"></p><p class="info-note">Source and destination quantities are updated together.</p>`;
    else if (type === 'product')
      html = `<div class="form-grid">${field('Product ID', 'id', input('id', 'text', 'maxlength="10" placeholder="P-001"'))}${field('Product name', 'name', input('name', 'text', 'maxlength="100"'))}${field('Unit', 'unit', input('unit', 'text', 'maxlength="20" placeholder="Box, bottle, kg…"'))}${field(
        'Storage requirement',
        'storage',
        select(
          'storage',
          storageRequirements.map((id) => ({ id })),
          (p) => p.id,
        ),
      )}${field('Reorder level', 'reorderLevel', input('reorderLevel', 'number', 'min="1" max="2147483647" step="1" value="1"'))}</div>`;
    else
      html = `<div class="form-grid">${field('Task name', 'name', input('name', 'text', 'maxlength="100"'), true)}${field(
        'Staff',
        'staffId',
        select('staffId', ctx.state.staff, (s) => s.name),
      )}${
        config.mode === 'http'
          ? field(
              'Status',
              'status',
              `<select name="status" required>${Object.entries(httpTaskLabels)
                .map(
                  ([v, l]) =>
                    `<option value="${esc(v)}" ${v === (record?.status || 'Pending') ? 'selected' : ''}>${esc(l)}</option>`,
                )
                .join('')}</select>`,
            )
          : ''
      }</div>`;
    $('#form-fields').innerHTML = html;
    if (['zone', 'aisle', 'bin'].includes(type) && !deleting)
      syncLocation(record);
    if (record && !deleting) {
      $$('input,select', $('#form-fields')).forEach((control) => {
        if (control.name !== 'phones' && Object.hasOwn(record, control.name))
          control.value = record[control.name];
        if (
          control.name === 'id' ||
          (['zone', 'aisle', 'bin'].includes(type) &&
            ['zoneId', 'aisleId', 'position'].includes(control.name))
        )
          control.readOnly = true;
        if (
          ['zone', 'aisle', 'bin'].includes(type) &&
          ['warehouseId', 'zone', 'aisle'].includes(control.name)
        ) {
          const option = [...control.options].find(
            (o) => o.value === record[control.name],
          );
          if (option) control.innerHTML = option.outerHTML;
        }
      });
    }
    $$('.line-row').forEach(syncLine);
    if (type === 'movement') syncMovement();
    describeForm($('#form-fields'));
    $('#form-dialog').showModal();
    return true;
  }

  function collectPayload() {
    const form = $('#operation-form'),
      values = Object.fromEntries(new env.FormData(form));
    // A shipped issue being edited has no lines on the form: only its status is sent.
    if (ctx.operation === 'issue' && !$$('.line-row', form).length)
      return { status: values.status };
    if (['receipt', 'issue', 'count'].includes(ctx.operation))
      return {
        ...(ctx.operation === 'receipt'
          ? { supplierId: values.supplierId }
          : ctx.operation === 'issue'
            ? {
                orderId: values.orderId,
                status: values.status.trim(),
                stateOfGood: values.stateOfGood.trim(),
              }
            : values.status
              ? { status: values.status }
              : {}),
        lines: $$('.line-row', form).map((row) => ({
          productId: $('[name=productId]', row).value,
          binId: $('[name=binId]', row).value,
          quantity: Number($('[name=quantity]', row).value),
        })),
      };
    const result = Object.fromEntries(
      Object.entries(values).map(([key, value]) => [
        key,
        ['quantity', 'reorderLevel'].includes(key)
          ? Number(value)
          : value.trim(),
      ]),
    );
    if (ctx.operation === 'staff') {
      result.phones = $$('[name="phones"]', form)
        .map((el) => el.value.trim())
        .filter(Boolean);
      delete result.phone;
    }
    if (ctx.operation === 'zone')
      result.id =
        ctx.editingId || locationKey(result.warehouseId, result.zoneId);
    if (ctx.operation === 'aisle')
      result.id =
        ctx.editingId ||
        locationKey(result.warehouseId, result.zone, result.aisleId);
    if (ctx.operation === 'bin')
      result.id =
        ctx.editingId ||
        [result.warehouseId, result.zone, result.aisle, result.position].join(
          '|',
        );
    return result;
  }

  function phoneForm(value = '') {
    return `<div class="phone-row">${field('Phone number', 'phones', `<input name="phones" type="tel" maxlength="20" value="${esc(value)}">`)}<button type="button" class="button quiet danger remove-staff-phone" aria-label="Remove phone number">Remove</button></div>`;
  }

  function syncLocation(record = null) {
    const root = $('#form-fields'),
      warehouse = $('[name="warehouseId"]', root);
    if (record) warehouse.value = record.warehouseId;
    const zone = $('[name="zone"]', root),
      aisle = $('[name="aisle"]', root);
    if (zone) {
      const chosen = record?.zone ?? zone.value;
      zone.innerHTML =
        ui.options(
          ctx.state.zones
            .filter((z) => z.warehouseId === warehouse.value)
            .map((z) => ({ id: z.zoneId, name: z.name })),
          (z) => z.id + ' · ' + z.name,
          chosen,
        ) || '<option value="">No zones in this warehouse</option>';
    }
    if (aisle) {
      const chosen = record?.aisle ?? aisle.value;
      aisle.innerHTML =
        ui.options(
          ctx.state.aisles
            .filter(
              (a) => a.warehouseId === warehouse.value && a.zone === zone.value,
            )
            .map((a) => ({ id: a.aisleId, name: a.name })),
          (a) => a.id + ' · ' + a.name,
          chosen,
        ) || '<option value="">No aisles in this zone</option>';
    }
  }

  function setSaving(value) {
    ctx.submitting = value;
    const button = $('#submit-operation');
    button.setAttribute('aria-disabled', String(value));
    button.setAttribute('aria-busy', String(value));
    button.textContent = value
      ? 'Saving…'
      : ctx.operation === 'count'
        ? 'Record count'
        : ctx.operation.startsWith('delete-')
          ? 'Delete record'
          : 'Save';
    $('#close-dialog').disabled = $('#cancel-dialog').disabled = value;
    if (value) $('#form-fields').setAttribute('inert', '');
    else $('#form-fields').removeAttribute('inert');
  }

  function showFormError(error) {
    $('#form-error').textContent = error.message;
    if (Number.isInteger(error.lineIndex)) {
      const row = $$('.line-row')[error.lineIndex];
      if (row) {
        $('.line-error', row).textContent = error.message;
        $('[name=quantity]', row).focus();
      }
    }
    for (const [path, message] of Object.entries(error.fields || {})) {
      const match = /^lines\[(\d+)\]\.(productId|binId|quantity)$/.exec(path);
      if (match) {
        const row = $$('.line-row')[Number(match[1])];
        if (row)
          $('.line-error', row).textContent =
            'Line ' + (Number(match[1]) + 1) + ': ' + message;
      }
    }
    presentFormErrors($('#form-fields'), $('#form-error-summary'), error);
  }
  return {
    describe: () => describeForm($('#form-fields')),
    clearErrors: () =>
      clearFormErrors($('#form-fields'), $('#form-error-summary')),
    field,
    select,
    input,
    operationMissing,
    lineForm,
    syncLine,
    syncMovement,
    openOperation,
    collectPayload,
    setSaving,
    showFormError,
    phoneForm,
    syncLocation,
  };
}
