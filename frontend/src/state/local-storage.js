import { initialState } from './store.js';
import { alignSchema } from './schema.js';
export function createStorage({ env, storage, ctx, config }) {
  const pendingKey = 'stockroom.pending-api.v1';
  function readLocal() {
    const raw = env.localStorage.getItem(storage.key);
    if (raw === null) return null;
    let saved;
    try {
      saved = JSON.parse(raw);
    } catch {
      throw new Error(
        'Saved local data is invalid JSON. It has been kept unchanged. Restore the saved data and retry.',
      );
    }
    if (!saved || ![3, 4].includes(saved.version))
      throw new Error(
        'Saved local data has an unsupported format. It has been kept unchanged. Restore the saved data and retry.',
      );
    return saved;
  }

  function loadLocal() {
    // Validate before any cleanup or backup; unreadable data must remain untouched.
    const saved = readLocal();
    let next = saved || initialState();
    if (saved?.version === 3) {
      const backupKey = storage.key + '.schema-v3-backup';
      if (!env.localStorage.getItem(backupKey))
        env.localStorage.setItem(backupKey, JSON.stringify(saved));
      next = alignSchema(saved);
    }
    // Retain the existing cleanup of obsolete sample stores after a successful read.
    for (let i = env.localStorage.length - 1; i >= 0; i--) {
      const key = env.localStorage.key(i);
      if (key === 'stockroom.demo.v1' || key?.startsWith('stockroom.demo.v1:'))
        env.localStorage.removeItem(key);
    }
    return next;
  }

  function persist(next) {
    // Recheck storage in case another tab corrupted it after the last read.
    readLocal();
    env.localStorage.setItem(storage.key, JSON.stringify(next));
    ctx.state = next;
  }

  function readPendingMutation() {
    try {
      const p = JSON.parse(
        env.sessionStorage.getItem('stockroom.pending-api.v1'),
      );
      return config.mode === 'http' && p?.baseUrl === config.baseUrl ? p : null;
    } catch {
      return null;
    }
  }

  function storePending(value) {
    ctx.pendingMutation = value;
    try {
      if (value) env.sessionStorage.setItem(pendingKey, JSON.stringify(value));
      else env.sessionStorage.removeItem(pendingKey);
    } catch {}
  }
  return { loadLocal, persist, readPendingMutation, storePending };
}
