import { esc } from '../utils/format.js';

// Presentation only: associates existing controls, hints and validation messages.
export function describeForm(root) {
  [...root.querySelectorAll('.bin-address,.stock-hint,.line-error')].forEach(
    (hint, index) => {
      hint.id = `operation-hint-${index}`;
    },
  );
  const controls = [...root.querySelectorAll('input,select')];
  controls.forEach((control, index) => {
    control.id = `operation-field-${index}`;
    const label = control.closest('.field');
    label?.setAttribute('for', control.id);
    const title = label?.querySelector('span');
    if (title) {
      title.id = `operation-label-${index}`;
      control.setAttribute('aria-labelledby', title.id);
    }
    const row = control.closest('.line-row');
    const hints = [
      label?.querySelector('.bin-address'),
      row?.querySelector('.stock-hint'),
      row?.querySelector('.line-error'),
    ].filter(Boolean);
    if (!row && ['quantity', 'fromBinId', 'toBinId'].includes(control.name)) {
      const hint = root.querySelector('.movement-hint');
      if (hint) hints.push(hint);
    }
    hints.forEach((hint, n) => {
      if (!hint.id) hint.id = `operation-hint-${index}-${n}`;
    });
    if (hints.length)
      control.setAttribute(
        'aria-describedby',
        hints.map((h) => h.id).join(' '),
      );
  });
  [...root.querySelectorAll('.remove-line')].forEach((button, index) =>
    button.setAttribute('aria-label', `Remove line ${index + 1}`),
  );
}

export function clearFormErrors(root, summary) {
  root
    .querySelectorAll('[aria-invalid]')
    .forEach((el) => el.removeAttribute('aria-invalid'));
  summary.hidden = true;
  summary.innerHTML = '';
}

export function presentFormErrors(root, summary, error) {
  const entries = [];
  const controls = [...root.querySelectorAll('input,select')];
  const rows = [...root.querySelectorAll('.line-row')];
  const mark = (control, message) => {
    if (!control) return;
    control.setAttribute('aria-invalid', 'true');
    if (!control.closest('.line-row'))
      control.setAttribute('aria-describedby', 'form-error');
    entries.push(`<li><a href="#${esc(control.id)}">${esc(message)}</a></li>`);
  };
  for (const [path, message] of Object.entries(error.fields || {})) {
    const match = /^lines\[(\d+)\]\.(productId|binId|quantity)$/.exec(path);
    const control = match
      ? rows[Number(match[1])]?.querySelector(`[name="${match[2]}"]`)
      : controls.find((c) => c.name === path);
    mark(control, message);
  }
  controls
    .filter((c) => !c.validity.valid)
    .forEach((c) =>
      mark(
        c,
        c.closest('.field')?.querySelector('span')?.textContent +
          ': ' +
          c.validationMessage,
      ),
    );
  if (!entries.length && Number.isInteger(error.lineIndex)) {
    const row = rows[error.lineIndex];
    row
      ?.querySelectorAll('input,select')
      .forEach((c) => c.setAttribute('aria-invalid', 'true'));
    mark(row?.querySelector('[name="quantity"]'), error.message);
  }
  summary.innerHTML = `<p>${esc(error.message)}</p>${entries.length ? `<ul>${entries.join('')}</ul>` : ''}`;
  summary.hidden = false;
  summary.focus();
}
