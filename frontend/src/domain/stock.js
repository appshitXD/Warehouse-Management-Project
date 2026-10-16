import { orderLabels } from '../config.js';
import { num, round, now } from '../utils/format.js';
export function createStock({ selectors, ctx, storage, env }) {
  function assert(condition, message) {
    if (!condition) throw new Error(message);
  }

  function validateLine(l, count = false) {
    assert(
      selectors.product(l.productId) && selectors.bin(l.binId),
      'Select a valid product and bin.',
    );
    assert(
      Number.isFinite(l.quantity) &&
        (count ? l.quantity >= 0 : l.quantity > 0) &&
        Math.abs(l.quantity * 1000 - Math.round(l.quantity * 1000)) < 0.00001,
      'Quantity must be valid with at most 3 decimal places' +
        (count ? ' and non-negative.' : ' and greater than zero.'),
    );
  }

  function validatePayload(type, payload) {
    if (type.startsWith('delete-')) {
      assert(
        payload.confirmId === ctx.editingId,
        'Enter the record ID to confirm deletion.',
      );
      validateDeletion(type.slice(7), ctx.editingId);
      return;
    }
    if (
      ['staff', 'customer', 'supplier', 'warehouse', 'order'].includes(type)
    ) {
      const rows =
        ctx.state[
          {
            staff: 'staff',
            customer: 'customers',
            order: 'orders',
            supplier: 'suppliers',
            warehouse: 'warehouses',
          }[type]
        ];
      assert(
        !ctx.editingId || rows.some((r) => r.id === ctx.editingId),
        'Record not found. Refresh the data and try again.',
      );
      assert(
        payload.id && /^[\p{L}\p{N}._-]+$/u.test(payload.id),
        'IDs can contain letters, numbers, periods, underscores and hyphens.',
      );
      assert(
        !rows.some(
          (r) =>
            r.id.toLowerCase() === payload.id.toLowerCase() &&
            r.id !== ctx.editingId,
        ),
        'This ID already exists.',
      );
      assert(
        !ctx.editingId || payload.id === ctx.editingId,
        'Record IDs cannot be changed.',
      );
      if (type === 'order') {
        assert(
          ctx.state.customers.some((c) => c.id === payload.customerId),
          'Invalid customer.',
        );
        assert(orderLabels[payload.status], 'Invalid order status.');
        if (
          ctx.editingId &&
          ctx.state.issues.some((i) => i.orderId === ctx.editingId)
        ) {
          const old = rows.find((r) => r.id === ctx.editingId);
          assert(
            old.customerId === payload.customerId,
            'This order already has goods issues. Its customer cannot be changed.',
          );
          assert(
            payload.status !== 'cancelled',
            'This order already has goods issues and cannot be cancelled.',
          );
        }
      } else {
        assert(payload.name, 'Name cannot be empty.');
        assert(
          !payload.phone || /^[+()\d .-]{5,30}$/.test(payload.phone),
          'Invalid phone number.',
        );
      }
      return;
    }
    if (type === 'bin') {
      assert(
        selectors.warehouse(payload.warehouseId),
        'Select a valid warehouse.',
      );
      for (const key of ['zone', 'aisle', 'position'])
        assert(
          payload[key] && /^[\p{L}\p{N}._-]{1,40}$/u.test(payload[key]),
          'Zone, aisle and position must contain letters, numbers, periods, underscores or hyphens.',
        );
      assert(
        !ctx.state.bins.some(
          (b) =>
            b.warehouseId === payload.warehouseId &&
            b.zone.toLowerCase() === payload.zone.toLowerCase() &&
            b.aisle.toLowerCase() === payload.aisle.toLowerCase() &&
            b.position.toLowerCase() === payload.position.toLowerCase() &&
            b.id !== ctx.editingId,
        ),
        'This bin address already exists.',
      );
      assert(
        !ctx.editingId || payload.id === ctx.editingId,
        'Bin addresses cannot be changed. Create a new bin and move stock instead.',
      );
      const old = selectors.bin(ctx.editingId);
      assert(
        !ctx.editingId || old,
        'Record not found. Refresh the data and try again.',
      );
      assert(
        !old ||
          ['warehouseId', 'zone', 'aisle', 'position'].every(
            (key) => old[key] === payload[key],
          ),
        'Bin addresses cannot be changed. Create a new bin and move stock instead.',
      );
      return;
    }
    if (type === 'product') {
      assert(
        payload.id && payload.name && payload.unit,
        'Enter a product ID, name and unit.',
      );
      assert(
        /^[\p{L}\p{N}._-]{1,40}$/u.test(payload.id),
        'Invalid Product ID.',
      );
      assert(
        typeof payload.storage === 'string' && payload.storage.trim(),
        'Enter a storage requirement.',
      );
      assert(
        !ctx.state.products.some(
          (p) =>
            p.id.toLowerCase() === payload.id.toLowerCase() &&
            p.id !== ctx.editingId,
        ),
        'Product ID already exists.',
      );
      assert(
        !ctx.editingId || payload.id === ctx.editingId,
        'Product IDs cannot be changed.',
      );
      const old = selectors.product(ctx.editingId);
      assert(
        !ctx.editingId || old,
        'Record not found. Refresh the data and try again.',
      );
      assert(
        !old || old.unit === payload.unit || !hasReferences('product', old.id),
        'Units cannot be changed after stock or transaction history exists.',
      );
      assert(
        Number.isFinite(payload.reorderLevel) && payload.reorderLevel >= 0,
        'Reorder level cannot be negative.',
      );
    }
    if (type === 'task') assert(payload.name, 'Task name cannot be empty.');
    if (!['receipt', 'issue', 'count', 'movement'].includes(type)) return;
    if (type === 'movement') {
      validateLine({ ...payload, binId: payload.toBinId });
      assert(selectors.bin(payload.fromBinId), 'Select a source bin.');
      assert(
        payload.fromBinId !== payload.toBinId,
        'Source and destination bins must be different.',
      );
      assert(
        payload.quantity <=
          selectors.quantity(payload.productId, payload.fromBinId),
        'Quantity exceeds the displayed source stock. Refresh if stock has changed.',
      );
      return;
    }
    assert(payload.lines.length, 'At least one line is required.');
    const balances = new Map(),
      seen = new Set();
    payload.lines.forEach((l, i) => {
      try {
        validateLine(l, type === 'count');
        const key = l.productId + '|' + l.binId;
        if (type === 'count') {
          assert(
            !seen.has(key),
            'This product–bin pair was already counted on an earlier line.',
          );
          seen.add(key);
        }
        if (type === 'issue') {
          const available = balances.has(key)
            ? balances.get(key)
            : selectors.quantity(l.productId, l.binId);
          assert(
            l.quantity <= available,
            `Only ${num(available)} ${selectors.product(l.productId).unit} remaining after previous lines.`,
          );
          balances.set(key, round(available - l.quantity));
        }
      } catch (error) {
        error.lineIndex = i;
        error.message = `Line ${i + 1}: ${error.message}`;
        throw error;
      }
    });
  }

  function hasReferences(type, id) {
    const s = ctx.state;
    if (type === 'warehouse') return s.bins.some((b) => b.warehouseId === id);
    const field = type === 'product' ? 'productId' : 'binId';
    return (
      s.stock.some((r) => r[field] === id) ||
      s.history.some((r) => r[field] === id) ||
      ['receipts', 'issues', 'counts'].some((key) =>
        s[key].some((d) => d.lines.some((l) => l[field] === id)),
      ) ||
      s.movements.some((m) =>
        type === 'product'
          ? m.productId === id
          : m.fromBinId === id || m.toBinId === id,
      )
    );
  }

  function validateDeletion(type, id) {
    const key = { product: 'products', warehouse: 'warehouses', bin: 'bins' }[
      type
    ];
    assert(key && ctx.state[key].some((r) => r.id === id), 'Record not found.');
    assert(
      !hasReferences(type, id),
      type === 'warehouse'
        ? 'Remove unused bins before deleting this warehouse.'
        : 'This record has stock or transaction history and cannot be deleted.',
    );
  }

  function adjust(s, pid, bid, delta, reference, at) {
    let row = s.stock.find((x) => x.productId === pid && x.binId === bid);
    if (!row) {
      row = { productId: pid, binId: bid, quantity: 0 };
      s.stock.push(row);
    }
    const next = round(row.quantity + delta);
    assert(
      next >= 0,
      `Insufficient stock for ${selectors.product(pid).id} in bin ${selectors.binName(bid)} (available: ${num(row.quantity)} ${selectors.product(pid).unit}).`,
    );
    row.quantity = next;
    s.history.push({
      id: env.crypto.randomUUID(),
      productId: pid,
      binId: bid,
      quantity: next,
      date: at,
    });
  }

  function applyLocal(type, payload) {
    // Clone first: failed validation never partially modifies stock or documents.
    if (type.startsWith('delete-')) {
      validatePayload(type, payload);
      const s = structuredClone(ctx.state);
      const key = { product: 'products', warehouse: 'warehouses', bin: 'bins' }[
        type.slice(7)
      ];
      s[key] = s[key].filter((r) => r.id !== ctx.editingId);
      storage.persist(s);
      return;
    }
    if (
      ['staff', 'customer', 'supplier', 'warehouse', 'order'].includes(type)
    ) {
      validatePayload(type, payload);
      const s = structuredClone(ctx.state),
        key = {
          staff: 'staff',
          customer: 'customers',
          order: 'orders',
          supplier: 'suppliers',
          warehouse: 'warehouses',
        }[type],
        old = s[key].find((r) => r.id === ctx.editingId);
      if (old) Object.assign(old, payload);
      else
        s[key].push({
          ...payload,
          ...(type === 'order' ? { date: now() } : {}),
        });
      storage.persist(s);
      return;
    }
    if (type === 'bin') {
      validatePayload(type, payload);
      const s = structuredClone(ctx.state);
      const row = {
        id: payload.id,
        warehouseId: payload.warehouseId,
        zone: payload.zone,
        aisle: payload.aisle,
        position: payload.position,
        name: payload.name || '',
      };
      const old = s.bins.find((b) => b.id === ctx.editingId);
      if (old) Object.assign(old, row);
      else s.bins.push(row);
      storage.persist(s);
      return;
    }
    const s = structuredClone(ctx.state),
      at = now(),
      id = `${{ receipt: 'GR', issue: 'GI', movement: 'MV', count: 'SC', product: 'P', task: 'T' }[type]}-${Date.now().toString(36).toUpperCase()}-${env.crypto.randomUUID().slice(0, 4)}`;
    if (['movement', 'task'].includes(type))
      assert(
        s.staff.some((x) => x.id === payload.staffId),
        'Invalid staff member.',
      );
    if (['receipt', 'issue', 'count'].includes(type)) {
      assert(payload.lines.length > 0, 'At least one line is required.');
      if (type === 'receipt')
        assert(
          s.suppliers.some((x) => x.id === payload.supplierId),
          'Invalid supplier.',
        );
      if (type === 'issue')
        assert(
          s.orders.some((x) => x.id === payload.orderId && x.status === 'open'),
          'Invalid order.',
        );
      const pairs = new Set();
      const lines = payload.lines.map((l, i) => {
        validateLine(l, type === 'count');
        const key = l.productId + '|' + l.binId;
        if (type === 'count') {
          assert(
            !pairs.has(key),
            'Each product–bin pair can only be counted once per stock count.',
          );
          pairs.add(key);
          const recordedQuantity = selectors.quantity(l.productId, l.binId, s);
          adjust(
            s,
            l.productId,
            l.binId,
            round(l.quantity - recordedQuantity),
            id,
            at,
          );
          return {
            lineNo: i + 1,
            productId: l.productId,
            binId: l.binId,
            recordedQuantity,
            countedQuantity: l.quantity,
          };
        }
        adjust(
          s,
          l.productId,
          l.binId,
          type === 'receipt' ? l.quantity : -l.quantity,
          id,
          at,
        );
        return {
          productId: l.productId,
          binId: l.binId,
          quantity: l.quantity,
          ...(type === 'receipt'
            ? { lineNo: i + 1 }
            : { id: env.crypto.randomUUID() }),
        };
      });
      s[
        { receipt: 'receipts', issue: 'issues', count: 'counts' }[type]
      ].unshift({
        ...(type === 'receipt'
          ? { supplierId: payload.supplierId }
          : type === 'issue'
            ? {
                orderId: payload.orderId,
                status: payload.status || 'recorded',
                stateOfGood: payload.stateOfGood || '',
              }
            : {}),
        id,
        date: at,
        lines,
        ...(type === 'count' ? { status: 'completed' } : {}),
      });
    } else if (type === 'movement') {
      assert(
        payload.fromBinId !== payload.toBinId,
        'Source and destination bins must be different.',
      );
      validateLine({ ...payload, binId: payload.toBinId });
      assert(selectors.bin(payload.fromBinId), 'Invalid source bin.');
      adjust(
        s,
        payload.productId,
        payload.fromBinId,
        -payload.quantity,
        id,
        at,
      );
      adjust(s, payload.productId, payload.toBinId, payload.quantity, id, at);
      s.movements.unshift({ ...payload, id, date: at });
    } else if (type === 'product') {
      validatePayload(type, payload);
      assert(
        payload.id && payload.name && payload.unit,
        'Enter a product ID, name and unit.',
      );
      assert(
        Number.isFinite(payload.reorderLevel) && payload.reorderLevel >= 0,
        'Reorder level cannot be negative.',
      );
      const row = {
        id: payload.id,
        name: payload.name,
        unit: payload.unit,
        storage: payload.storage,
        reorderLevel: payload.reorderLevel,
      };
      const old = s.products.find((p) => p.id === ctx.editingId);
      if (old) Object.assign(old, row);
      else s.products.push(row);
    } else {
      assert(payload.name, 'Task name cannot be empty.');
      s.tasks.unshift({
        name: payload.name,
        staffId: payload.staffId,
        id,
        status: 'todo',
      });
    }
    storage.persist(s);
  }

  function eligibleBins(type, pid, from = '') {
    const p = selectors.product(pid);
    if (!p) return [];
    return ctx.state.bins.filter(
      (b) =>
        b.id !== from &&
        (!['issue', 'movement-source'].includes(type) ||
          selectors.quantity(pid, b.id) > 0),
    );
  }

  function lastWeek() {
    const end = new Date();
    end.setHours(0, 0, 0, 0);
    end.setDate(end.getDate() - ((end.getDay() + 6) % 7));
    const start = new Date(end);
    start.setDate(start.getDate() - 7);
    return { start, end };
  }

  function emptyLastWeek() {
    const { start, end } = lastWeek();
    return ctx.state.bins.filter((b) => {
      const history = ctx.state.history
        .filter((h) => h.binId === b.id)
        .sort((a, c) => a.date.localeCompare(c.date));
      if (!history.some((h) => new Date(h.date) < start)) return false;
      const balances = new Map();
      for (const h of history.filter((h) => new Date(h.date) < start))
        balances.set(h.productId, h.quantity);
      if ([...balances.values()].some((q) => q > 0)) return false;
      return !history.some(
        (h) =>
          new Date(h.date) >= start && new Date(h.date) < end && h.quantity > 0,
      );
    });
  }
  return {
    assert,
    validateLine,
    validatePayload,
    validateDeletion,
    adjust,
    applyLocal,
    eligibleBins,
    lastWeek,
    emptyLastWeek,
  };
}
