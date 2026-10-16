export function createSelectors({ ctx }) {
  const customerName = (id) =>
    ctx.state.customers.find((c) => c.id === id)?.name || id;

  const personMatches = (p) =>
    [p.id, p.name, p.phone, p.address]
      .join(' ')
      .toLocaleLowerCase('en')
      .includes(ctx.filters.search.toLocaleLowerCase('en'));

  const product = (id) => ctx.state.products.find((p) => p.id === id);

  const bin = (id) => ctx.state.bins.find((b) => b.id === id);

  const warehouse = (id) => ctx.state.warehouses.find((w) => w.id === id);

  const staffName = (id) =>
    ctx.state.staff.find((s) => s.id === id)?.name || id;

  const binName = (id) => {
    const b = bin(id);
    return b ? `${b.warehouseId} / ${b.zone} / ${b.aisle} / ${b.position}` : id;
  };

  const quantity = (pid, bid, s = ctx.state) =>
    s.stock.find((x) => x.productId === pid && x.binId === bid)?.quantity || 0;

  const total = (pid) =>
    ctx.state.stock
      .filter((s) => s.productId === pid)
      .reduce((sum, s) => sum + s.quantity, 0);

  const lowProducts = () =>
    ctx.state.products.filter((p) => total(p.id) < p.reorderLevel);

  function matchesProduct(pid) {
    const p = product(pid);
    return `${p.id} ${p.name}`
      .toLocaleLowerCase('en')
      .includes(ctx.filters.search.toLocaleLowerCase('en'));
  }

  function matchesBin(bid) {
    return (
      !ctx.filters.warehouse || bin(bid)?.warehouseId === ctx.filters.warehouse
    );
  }
  return {
    customerName,
    personMatches,
    product,
    bin,
    warehouse,
    staffName,
    binName,
    quantity,
    total,
    lowProducts,
    matchesProduct,
    matchesBin,
  };
}
