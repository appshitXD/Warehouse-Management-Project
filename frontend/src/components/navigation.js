import { esc } from '../utils/format.js';

const paths = {
  dashboard:
    '<rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/>',
  inventory: '<path d="m12 3 9 5-9 5-9-5 9-5ZM3 8v9l9 5 9-5V8M12 13v9"/>',
  receipts: '<path d="M12 3v12m-5-5 5 5 5-5M4 17v4h16v-4"/>',
  issues: '<path d="M12 15V3m-5 5 5-5 5 5M4 17v4h16v-4"/>',
  movements: '<path d="M3 7h18m-4-4 4 4-4 4M21 17H3m4-4-4 4 4 4"/>',
  counts:
    '<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 4V2h6v2M8 12l2 2 5-5M8 18h8"/>',
  locations: '<path d="m3 9 9-6 9 6v12H3V9ZM7 21v-8h10v8M7 17h10"/>',
  staff:
    '<circle cx="9" cy="7" r="3"/><path d="M3 21v-3a6 6 0 0 1 12 0v3M17 4a3 3 0 0 1 0 6M18 14a5 5 0 0 1 3 4v3"/>',
  customers:
    '<circle cx="12" cy="7" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  suppliers:
    '<path d="M3 6h12v12H3V6Zm12 5h4l3 4v3h-7"/><circle cx="7" cy="19" r="2"/><circle cx="18" cy="19" r="2"/>',
  orders: '<path d="M6 3h12v18l-3-2-3 2-3-2-3 2V3ZM9 8h6M9 12h6"/>',
  tasks: '<path d="m3 6 2 2 3-4M11 6h10M3 13l2 2 3-4M11 13h10M11 20h10"/>',
  reports: '<path d="M4 3v18h17M8 16v-5M13 16V7M18 16V4"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  refresh: '<path d="M20 7V3l-3 3a8 8 0 1 0 3 8M20 7h-5"/>',
};

export function icon(name) {
  return `<svg class="ui-icon" aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">${paths[name] || paths.inventory}</svg>`;
}

export function renderNavigation(routes, current, discrepancy) {
  const link = (id) => {
    const label = routes.find((r) => r[0] === id)[2];
    return `<a class="nav-link ${current === id ? 'active' : ''}" href="#${id}" aria-label="${esc(label)}" title="${esc(label)}" ${current === id ? 'aria-current="page"' : ''}><span class="nav-icon">${icon(id)}</span><span class="nav-text">${esc(label)}</span>${id === 'counts' && discrepancy ? '<span class="nav-count" aria-label="Discrepancies">!</span>' : ''}</a>`;
  };
  return (
    link('dashboard') +
    [
      [
        'Operations',
        ['inventory', 'receipts', 'issues', 'movements', 'counts'],
      ],
      ['Orders & tasks', ['orders', 'tasks']],
      ['Master data', ['locations', 'suppliers', 'staff', 'customers']],
    ]
      .map(
        ([label, ids]) =>
          `<div class="nav-group"><p class="nav-label">${label}</p>${ids.map(link).join('')}</div>`,
      )
      .join('') +
    link('reports')
  );
}

export function bindShell() {
  const sidebar = document.querySelector('.sidebar');
  document.querySelector('.skip-link').addEventListener('click', (event) => {
    event.preventDefault();
    document.querySelector('#main').focus();
  });
  const toggle = document.querySelector('#sidebar-toggle');
  const mobile = document.querySelector('#mobile-navigation');
  const backdrop = document.querySelector('#navigation-backdrop');
  const media = window.matchMedia('(max-width: 760px)');
  const close = (restore = false) => {
    document.body.classList.remove('navigation-open');
    mobile.setAttribute('aria-expanded', 'false');
    document.querySelector('.shell').removeAttribute('inert');
    if (media.matches) sidebar.setAttribute('inert', '');
    if (media.matches) toggle.removeAttribute('aria-expanded');
    if (restore) mobile.focus();
  };
  const sync = () => {
    close();
    if (!media.matches) sidebar.removeAttribute('inert');
    if (!media.matches)
      toggle.setAttribute(
        'aria-expanded',
        String(!document.body.classList.contains('sidebar-collapsed')),
      );
    toggle.setAttribute(
      'aria-label',
      media.matches
        ? 'Close navigation'
        : document.body.classList.contains('sidebar-collapsed')
          ? 'Expand sidebar'
          : 'Collapse sidebar',
    );
  };
  toggle.addEventListener('click', () => {
    if (media.matches) return close(true);
    const collapsed = document.body.classList.toggle('sidebar-collapsed');
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.setAttribute(
      'aria-label',
      collapsed ? 'Expand sidebar' : 'Collapse sidebar',
    );
  });
  mobile.addEventListener('click', () => {
    sidebar.removeAttribute('inert');
    document.querySelector('.shell').setAttribute('inert', '');
    document.body.classList.add('navigation-open');
    mobile.setAttribute('aria-expanded', 'true');
    toggle.focus();
  });
  backdrop.addEventListener('click', () => close(true));
  document.addEventListener('keydown', (event) => {
    if (!media.matches || !document.body.classList.contains('navigation-open'))
      return;
    if (event.key === 'Escape') {
      event.preventDefault();
      close(true);
    }
    if (event.key === 'Tab') {
      const controls = [...sidebar.querySelectorAll('a,button')];
      const first = controls[0],
        last = controls.at(-1);
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault();
        first.focus();
      }
    }
  });
  window.addEventListener('hashchange', () => close());
  media.addEventListener('change', sync);
  sync();
}
