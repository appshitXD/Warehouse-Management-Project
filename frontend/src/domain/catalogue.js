export const storageRequirements = [
  'Ambient',
  'Dry Ambient',
  'Cold',
  'Hazardous',
];

export const locationKey = (...parts) => parts.join('|');


export function validateCatalogue(type, payload, ctx) {
  const collections = {
    product: 'products',
    warehouse: 'warehouses',
    supplier: 'suppliers',
    customer: 'customers',
    staff: 'staff',
    zone: 'zones',
    aisle: 'aisles',
    bin: 'bins',
  };
  if (!collections[type]) return false;
  const s = ctx.state,
    rows = s[collections[type]];
  const titles = {
    id: 'ID',
    zoneId: 'Zone ID',
    aisleId: 'Aisle ID',
    zone: 'Zone',
    aisle: 'Aisle',
    position: 'Position',
    name: 'Name',
    unit: 'Unit',
    address: 'Address',
  };
  const fail = (field, message) => {
    const error = new Error(message);
    error.fields = { [field]: message };
    throw error;
  };
  const check = (condition, field, message) => {
    if (!condition) fail(field, message);
  };
  const code = (value, field, max = 40) =>
    check(
      typeof value === 'string' &&
        new RegExp(`^[A-Za-z0-9_-]{1,${max}}$`).test(value),
      field,
      `${titles[field] || field} must use 1–${max} letters, digits, underscores or hyphens.`,
    );
  const text = (value, field, max, required = true) =>
    check(
      typeof value === 'string' &&
        (!required || value.trim().length > 0) &&
        value.length <= max,
      field,
      `${titles[field] || field} ${required ? 'is required and ' : ''}must be at most ${max} characters.`,
    );
  const phone = (value, field) =>
    check(
      typeof value === 'string' && /^[0-9+() -]{6,20}$/.test(value),
      field,
      'Phone must be 6–20 characters using digits, spaces, +, -, ( or ).',
    );
  const equal = (a, b) =>
    String(a).toLocaleLowerCase('en') === String(b).toLocaleLowerCase('en');
  const old = rows.find((r) => r.id === ctx.editingId);
  check(
    !ctx.editingId || old,
    'id',
    'Record not found. Refresh the data and try again.',
  );
  check(
    !old || old.id === payload.id,
    'id',
    'Record identifiers cannot be changed.',
  );
  check(
    !rows.some((r) => r.id !== ctx.editingId && equal(r.id, payload.id)),
    'id',
    'This ID already exists.',
  );
  if (['zone', 'aisle', 'bin'].includes(type)) {
    check(
      s.warehouses.some((w) => w.id === payload.warehouseId),
      'warehouseId',
      'Select a valid warehouse.',
    );
    if (type === 'zone') {
      code(payload.zoneId, 'zoneId');
      check(
        payload.id === locationKey(payload.warehouseId, payload.zoneId),
        'zoneId',
        'Invalid zone address.',
      );
    } else {
      code(payload.zone, 'zone');
      check(
        s.zones.some(
          (z) =>
            z.warehouseId === payload.warehouseId && z.zoneId === payload.zone,
        ),
        'zone',
        'Select a zone in this warehouse.',
      );
      if (type === 'aisle') {
        code(payload.aisleId, 'aisleId');
        check(
          payload.id ===
            locationKey(payload.warehouseId, payload.zone, payload.aisleId),
          'aisleId',
          'Invalid aisle address.',
        );
      } else {
        code(payload.aisle, 'aisle');
        code(payload.position, 'position');
        check(
          s.aisles.some(
            (a) =>
              a.warehouseId === payload.warehouseId &&
              a.zone === payload.zone &&
              a.aisleId === payload.aisle,
          ),
          'aisle',
          'Select an aisle in this zone.',
        );
        check(
          payload.id ===
            locationKey(
              payload.warehouseId,
              payload.zone,
              payload.aisle,
              payload.position,
            ),
          'position',
          'Invalid bin address.',
        );
      }
    }
    if (old)
      for (const key of [
        'warehouseId',
        'zoneId',
        'zone',
        'aisleId',
        'aisle',
        'position',
      ])
        if (Object.hasOwn(old, key))
          check(
            old[key] === payload[key],
            key,
            'Location addresses cannot be changed. Create a new location instead.',
          );
    text(payload.name ?? '', 'name', 120, type !== 'bin');
    if (type !== 'bin')
      check(
        !rows.some(
          (r) =>
            r.id !== ctx.editingId &&
            r.warehouseId === payload.warehouseId &&
            (type === 'zone' || r.zone === payload.zone) &&
            equal(r.name, payload.name),
        ),
        'name',
        'This name already exists in the parent location.',
      );
    return true;
  }
  code(payload.id, 'id', type === 'product' ? 10 : 40);
  text(payload.name, 'name', type === 'product' ? 100 : 120);
  if (type === 'product') {
    text(payload.unit, 'unit', 20);
    check(
      storageRequirements.includes(payload.storage),
      'storage',
      'Select Ambient, Dry Ambient, Cold or Hazardous.',
    );
    check(
      Number.isInteger(payload.reorderLevel) &&
        payload.reorderLevel > 0 &&
        payload.reorderLevel <= 2147483647,
      'reorderLevel',
      'Reorder level must be a positive integer up to 2147483647.',
    );
  } else {
    text(payload.address ?? '', 'address', 250, false);
    if (type === 'warehouse')
      check(
        !rows.some(
          (r) => r.id !== ctx.editingId && equal(r.name, payload.name),
        ),
        'name',
        'A warehouse with this name already exists.',
      );
    else if (type === 'staff') {
      check(
        s.warehouses.some((w) => w.id === payload.warehouseId),
        'warehouseId',
        'Select a warehouse for this staff member.',
      );
      check(Array.isArray(payload.phones), 'phones', 'Provide a phone list.');
      const seenPhones = new Set();
      payload.phones.forEach((value, index) => {
        phone(value, `phones[${index}]`);
        check(
          !seenPhones.has(value),
          `phones[${index}]`,
          'This phone number is already listed.',
        );
        seenPhones.add(value);
      });
      let supervisor = payload.supervisorId;
      const visited = new Set([payload.id]);
      while (supervisor) {
        check(
          !visited.has(supervisor),
          'supervisorId',
          'A staff member cannot supervise themselves or form a supervision cycle.',
        );
        visited.add(supervisor);
        const person = s.staff.find((r) => r.id === supervisor);
        check(person, 'supervisorId', 'Select a valid supervisor.');
        supervisor = person.supervisorId;
      }
    } else if (payload.phone) phone(payload.phone, 'phone');
  }
  return true;
}
