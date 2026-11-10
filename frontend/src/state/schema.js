// Preserve existing identities and quantities; derive missing parent locations from old bins.
export function alignSchema(saved) {
  const s = structuredClone(saved);
  s.version = 5;
  for (const p of s.products) delete p.code;
  for (const b of s.bins) {
    delete b.storage;
    delete b.createdAt;
    b.name ||= '';
  }
  for (const t of s.tasks) delete t.warehouseId;
  for (const name of ['receipts', 'issues', 'counts'])
    for (const d of s[name]) {
      delete d.staffId;
      if (name === 'issues') {
        d.status ||= 'recorded';
        d.stateOfGood ||= '';
        d.lines.forEach((line, index) => {
          line.id ||= `${d.id}-line-${index + 1}`;
          delete line.lineNo;
        });
      }
    }
  s.history.forEach((h, index) => {
    h.id ||= `legacy-history-${index + 1}`;
    delete h.delta;
    delete h.reference;
  });
  s.zones ||= [];
  s.aisles ||= [];
  for (const b of s.bins) {
    const zoneKey = [b.warehouseId, b.zone].join('|');
    if (!s.zones.some((z) => z.id === zoneKey))
      s.zones.push({
        id: zoneKey,
        warehouseId: b.warehouseId,
        zoneId: b.zone,
        name: b.zone,
      });
    const aisleKey = [b.warehouseId, b.zone, b.aisle].join('|');
    if (!s.aisles.some((a) => a.id === aisleKey))
      s.aisles.push({
        id: aisleKey,
        warehouseId: b.warehouseId,
        zone: b.zone,
        aisleId: b.aisle,
        name: b.aisle,
      });
  }
  for (const person of s.staff) {
    person.phones = Array.isArray(person.phones)
      ? person.phones
      : person.phone
        ? [person.phone]
        : [];
    delete person.phone;
    person.warehouseId ||= '';
    person.supervisorId ||= '';
  }
  return s;
}
