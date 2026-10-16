import { createStorage } from './state/local-storage.js';
import { createSelectors } from './state/selectors.js';
import { createUi } from './components/ui.js';
import { createStock } from './domain/stock.js';
import { createDashboard } from './pages/dashboard.js';
import { createInventory } from './pages/inventory.js';
import { createDocuments } from './pages/documents.js';
import { createMovements } from './pages/movements.js';
import { createCounts } from './pages/counts.js';
import { createLocations } from './pages/locations.js';
import { createTasks } from './pages/tasks.js';
import { createReports } from './pages/reports.js';
import { createDirectories } from './pages/directories.js';
import { createServices } from './services/warehouse.js';
import { createForms } from './forms/operations.js';
import { createController } from './controller.js';
import { createStore } from './state/store.js';
import { API_CONFIG } from './config.js';
import { request } from './api/client.js';
import { bindEvents } from './events.js';
export function createApplication({
  config = API_CONFIG,
  request: httpRequest = request,
  env = {
    document: globalThis.document,
    window: globalThis.window,
    location: globalThis.location,
    localStorage: globalThis.localStorage,
    sessionStorage: globalThis.sessionStorage,
    crypto: globalThis.crypto,
    FormData: globalThis.FormData,
    URL: globalThis.URL,
    Blob: globalThis.Blob,
    setTimeout: globalThis.setTimeout.bind(globalThis),
    clearTimeout: globalThis.clearTimeout.bind(globalThis),
  },
} = {}) {
  const ctx = createStore();
  const $ = (selector, root = env.document) => root.querySelector(selector);
  const $$ = (selector, root = env.document) => [
    ...root.querySelectorAll(selector),
  ];
  // Shared callbacks are filled after services/pages are constructed; imports stay acyclic.
  const controller = {};
  const pages = {};
  const storage = {
    key:
      'warehouse.local.v3' +
      (() => {
        const session = new URLSearchParams(env.location.search || '').get(
          'demoSession',
        );
        return session && /^[a-z0-9-]{1,50}$/.test(session)
          ? ':' + session
          : '';
      })(),
  };
  Object.assign(storage, createStorage({ env, storage, ctx, config }));
  ctx.pendingMutation = storage.readPendingMutation();
  const selectors = createSelectors({ ctx });
  const ui = createUi({ ctx });
  const stock = createStock({ selectors, ctx, storage, env });
  const services = createServices({
    config,
    ctx,
    controller,
    stock,
    request: httpRequest,
    storage,
    env,
  });
  Object.assign(pages, createDashboard({ ctx, selectors, ui, config }));
  Object.assign(pages, createInventory({ config, ctx, selectors, ui }));
  Object.assign(pages, createDocuments({ ctx, selectors, ui }));
  Object.assign(pages, createMovements({ ctx, selectors, ui }));
  Object.assign(pages, createCounts({ ui, ctx, selectors }));
  Object.assign(pages, createLocations({ ui, ctx, selectors }));
  Object.assign(pages, createTasks({ ctx, ui, selectors }));
  Object.assign(pages, createReports({ ui, ctx, config, selectors, stock }));
  Object.assign(pages, createDirectories({ ctx, ui, selectors }));
  const forms = createForms({
    ui,
    ctx,
    stock,
    selectors,
    controller,
    env,
    $,
    $$,
  });
  Object.assign(
    controller,
    createController({
      ctx,
      env,
      config,
      pages,
      ui,
      services,
      selectors,
      $,
      $$,
    }),
  );
  return {
    ctx,
    storage,
    selectors,
    ui,
    stock,
    services,
    pages,
    forms,
    controller,
    start: async () => {
      bindEvents({
        ctx,
        env,
        config,
        $,
        $$,
        storage,
        selectors,
        ui,
        stock,
        services,
        pages,
        forms,
        controller,
      });
      await controller.refreshData();
    },
  };
}
