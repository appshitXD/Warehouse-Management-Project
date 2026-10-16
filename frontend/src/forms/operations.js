import {
  describeForm,
  clearFormErrors,
  presentFormErrors,
} from './accessibility.js';
import { esc, num } from '../utils/format.js';
import { orderLabels } from '../config.js';
export function createForms({
  $,
  $$,
  ui,
  ctx,
  stock,
  selectors,
  controller,
  env,
}) {
  const field = (label, name, control, full = false) =>
    `<label class="field ${full ? 'full-width' : ''}"><span>${label}</span>${control}</label>`;

  const select = (name, items, label) =>
    `<select name="${name}" required>${ui.options(items, label)}</select>`;

  const input = (name, type = 'text', extra = '') =>
    `<input name="${name}" type="${type}" required ${extra}>`;

  function operationMissing(type) {
    const missing = [];
    if (
      ['product', 'staff', 'customer', 'supplier', 'warehouse'].includes(type)
    )
      return missing;
    if (type === 'bin') {
      if (!ctx.state.warehouses.length) missing.push('warehouses');
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
    if (type === 'issue' && !ctx.state.orders.some((o) => o.status === 'open'))
      missing.push('open customer orders');
    return missing;
  }

  function lineForm() {
    const p = ctx.state.products[0],
      bins = p ? stock.eligibleBins(ctx.operation, p.id) : [],
      b = bins[0];
    const address =
      '<small class="bin-address" aria-live="polite">' +
      esc(b ? selectors.binName(b.id) : 'No bins available.') +
      '</small>';
    const quantityField = input(
      'quantity',
      'number',
      'min="' +
        (ctx.operation === 'count' ? 0 : 0.001) +
        '" step="0.001" value="' +
        (ctx.operation === 'count' ? 0 : 1) +
        '"',
    );
    return (
      '<div class="line-row">' +
      field(
        'Product',
        'productId',
        select('productId', ctx.state.products, (p) => p.id + ' · ' + p.name),
      ) +
      field(
        'Bin',
        'binId',
        select('binId', bins, (b) => selectors.binName(b.id)) + address,
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

  function openOperation(type, id = '') {
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
          product: 'product',
          bin: 'bin',
        }[entity]
      : titles[type];
    if (deleting) $('#dialog-title').textContent = 'Delete ' + entity;
    $('#submit-operation').textContent = deleting
      ? 'Delete record'
      : type === 'count'
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
            product: 'products',
            bin: 'bins',
          }[entity]
        ]?.find((r) => r.id === id)
      : null;
    if (id && !record) {
      controller.notify('Record not found. Refresh the data and try again.');
      return false;
    }
    let html = '';
    if (deleting) {
      html =
        '<p class="info-note">Only unused records can be deleted. Stock and transaction history are preserved.</p><p>Delete <strong>' +
        esc(record.name || record.id) +
        '</strong>? Enter <strong>' +
        esc(record.id) +
        '</strong> to confirm.</p>' +
        field(
          'Record ID confirmation',
          'confirmId',
          input('confirmId', 'text', 'autocomplete="off"'),
        );
    } else if (['staff', 'customer', 'supplier'].includes(type)) {
      html =
        '<div class="form-grid">' +
        field(
          'ID',
          'id',
          input(
            'id',
            'text',
            'maxlength="40" value="' +
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
            'maxlength="120" value="' + esc(record?.name || '') + '"',
          ),
        ) +
        field(
          'Phone',
          'phone',
          '<input name="phone" type="tel" maxlength="30" value="' +
            esc(record?.phone || '') +
            '">',
        ) +
        field(
          'Address',
          'address',
          '<input name="address" maxlength="250" value="' +
            esc(record?.address || '') +
            '">',
          true,
        ) +
        '</div>';
    } else if (type === 'warehouse') {
      html =
        '<div class="form-grid">' +
        field('Warehouse ID', 'id', input('id', 'text', 'maxlength="40"')) +
        field(
          'Warehouse name',
          'name',
          input('name', 'text', 'maxlength="120"'),
        ) +
        field(
          'Address',
          'address',
          '<input name="address" maxlength="250">',
          true,
        ) +
        '</div>';
    } else if (type === 'bin') {
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
        field('Zone', 'zone', input('zone', 'text', 'maxlength="40"')) +
        field('Aisle', 'aisle', input('aisle', 'text', 'maxlength="40"')) +
        field(
          'Position',
          'position',
          input('position', 'text', 'maxlength="40"'),
        ) +
        field('Bin name', 'name', '<input name="name" maxlength="120">') +
        '</div><p class="info-note">A bin is uniquely identified by its warehouse, zone, aisle and position.</p>';
    } else if (type === 'order') {
      html =
        '<div class="form-grid">' +
        field(
          'Order ID',
          'id',
          input(
            'id',
            'text',
            'maxlength="40" value="' +
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
            Object.entries(orderLabels)
              .map(
                ([v, l]) =>
                  '<option value="' +
                  v +
                  '" ' +
                  (v === (record?.status || 'open') ? 'selected' : '') +
                  '>' +
                  l +
                  '</option>',
              )
              .join('') +
            '</select>',
        ) +
        '</div><p class="info-note">Open orders can be selected for goods issues. Creating an order does not change stock.</p>';
    } else if (['receipt', 'issue', 'count'].includes(type)) {
      html = `<div class="form-grid">${
        type === 'receipt'
          ? field(
              'Supplier',
              'supplierId',
              select('supplierId', ctx.state.suppliers, (s) => s.name),
            )
          : type === 'issue'
            ? field(
                'Customer order',
                'orderId',
                select(
                  'orderId',
                  ctx.state.orders.filter((o) => o.status === 'open'),
                  (o) => o.id + ' · ' + selectors.customerName(o.customerId),
                ),
              )
            : ''
      }${type === 'issue' ? field('Status', 'status', input('status', 'text', 'maxlength="40" value="recorded"')) + field('State of goods', 'stateOfGood', '<input name="stateOfGood" maxlength="120" placeholder="Describe the condition of the goods">') : ''}</div><h3 class="line-heading">${type === 'count' ? 'Count lines' : 'Goods lines'}</h3><p class="info-note">${type === 'count' ? 'Saving replaces stock with the counted quantities and writes stock history. The previous stock is retained for reconciliation. Complete the count before resuming movements.' : type === 'issue' ? 'Saving deducts stock immediately. Status is descriptive and does not create a draft.' : 'Select bins. Every line is checked before updating stock.'}</p><div id="form-lines">${lineForm()}</div><button type="button" id="add-line" class="button quiet add-line-button">＋ Add line</button>`;
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
      )}${field('Quantity', 'quantity', input('quantity', 'number', 'min="0.001" step="0.001" value="1"'))}${field(
        'Performed by',
        'staffId',
        select('staffId', ctx.state.staff, (s) => s.name),
      )}</div><p class="stock-hint movement-hint" aria-live="polite"></p><p class="info-note">Source and destination quantities are updated together.</p>`;
    else if (type === 'product')
      html = `<div class="form-grid">${field('Product ID', 'id', input('id', 'text', 'maxlength="40" placeholder="P-001"'))}${field('Product name', 'name', input('name', 'text', 'maxlength="120"'))}${field('Unit', 'unit', input('unit', 'text', 'maxlength="20" placeholder="Box, bottle, kg…"'))}${field(
        'Storage requirement',
        'storage',
        input(
          'storage',
          'text',
          'maxlength="120" placeholder="Ambient, cold, dry…"',
        ),
      )}${field('Reorder level', 'reorderLevel', input('reorderLevel', 'number', 'min="0" step="0.001" value="0"'))}</div>`;
    else
      html = `<div class="form-grid">${field('Task name', 'name', input('name', 'text', 'maxlength="120"'), true)}${field(
        'Staff',
        'staffId',
        select('staffId', ctx.state.staff, (s) => s.name),
      )}</div>`;
    $('#form-fields').innerHTML = html;
    if (record && !deleting) {
      $$('input,select', $('#form-fields')).forEach((control) => {
        if (Object.hasOwn(record, control.name))
          control.value = record[control.name];
        if (
          control.name === 'id' ||
          (type === 'bin' &&
            ['zone', 'aisle', 'position'].includes(control.name))
        )
          control.readOnly = true;
        if (type === 'bin' && control.name === 'warehouseId')
          control.innerHTML = ui.options(
            [selectors.warehouse(record.warehouseId)],
            (w) => w.id + ' · ' + w.name,
            record.warehouseId,
          );
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
    if (ctx.operation === 'bin')
      result.id =
        ctx.editingId ||
        [result.warehouseId, result.zone, result.aisle, result.position].join(
          '|',
        );
    return result;
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
  };
}
