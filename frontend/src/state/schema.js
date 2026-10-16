// Upgrade local records without deleting the original v3 storage entry backup.
export function alignSchema(saved) {
  const s = structuredClone(saved);
  s.version = 4;
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
  return s;
}
