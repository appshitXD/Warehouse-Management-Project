export const esc = (value) =>
  String(value ?? '').replace(
    /[&<>"']/g,
    (c) =>
      ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[
        c
      ],
  );

export const documentCode = (value) => esc(value);

export const num = (value) =>
  new Intl.NumberFormat('en-GB', { maximumFractionDigits: 3 }).format(value);

export const date = (value) => new Date(value).toLocaleDateString('en-GB');

export const time = (value) =>
  new Date(value).toLocaleTimeString('en-GB', {
    hour: '2-digit',
    minute: '2-digit',
  });

export const now = () => new Date().toISOString();

export const round = (n) => Math.round(n * 1000) / 1000;

export const storageName = (value) =>
  value === 'cold'
    ? 'Cold storage'
    : value === 'ambient'
      ? 'Ambient storage'
      : value;
