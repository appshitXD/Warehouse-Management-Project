export function initialState() {
  return {
    version: 5,
    warehouses: [],
    zones: [],
    aisles: [],
    bins: [],
    products: [],
    stock: [],
    suppliers: [],
    orders: [],
    customers: [],
    staff: [],
    receipts: [],
    issues: [],
    movements: [],
    counts: [],
    history: [],
    tasks: [],
  };
}
export function createStore() {
  return {
    state: undefined,
    route: 'dashboard',
    operation: '',
    editingId: '',
    filters: { search: '', warehouse: '', product: '' },
    submitting: false,
    refreshing: false,
    stale: false,
    refreshError: '',
    remoteReports: null,
    reportsBusy: false,
    reportsError: '',
    reportsGeneration: 0,
    remoteLocations: null,
    locationsBusy: false,
    locationsError: '',
    locationsGeneration: 0,
    pendingMutation: null,
  };
}
