import { API_CONFIG } from '../config.js';
export { API_CONFIG } from '../config.js';
export class ApiError extends Error {
  constructor(
    message,
    { status = 0, code = 'NETWORK_ERROR', fields = {}, uncertain = false } = {},
  ) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fields = fields;
    this.uncertain = uncertain;
  }
}
export async function request(
  path,
  { method = 'GET', body, signal, idempotencyKey } = {},
) {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), API_CONFIG.timeoutMs);
  const abort = () => controller.abort();
  if (signal?.aborted) controller.abort();
  signal?.addEventListener('abort', abort, { once: true });
  const writing = !['GET', 'HEAD'].includes(method);
  try {
    const response = await fetch(
      `${API_CONFIG.baseUrl.replace(/\/$/, '')}${path}`,
      {
        method,
        headers: {
          Accept: 'application/json',
          ...(body ? { 'Content-Type': 'application/json' } : {}),
          ...(idempotencyKey ? { 'Idempotency-Key': idempotencyKey } : {}),
        },
        ...(body ? { body: JSON.stringify(body) } : {}),
        signal: controller.signal,
      },
    );
    if (response.status === 204) return null;
    let data;
    try {
      data = await response.json();
    } catch {
      throw new ApiError(
        `API returned a non-JSON response (${response.status}).`,
        {
          status: response.status,
          code: 'INVALID_RESPONSE',
          uncertain: writing && (response.ok || response.status >= 500),
        },
      );
    }
    if (!response.ok)
      throw new ApiError(
        typeof data?.message === 'string'
          ? data.message
          : `Request failed (${response.status}).`,
        {
          status: response.status,
          code: data?.code,
          fields:
            data?.fields && typeof data.fields === 'object' ? data.fields : {},
          uncertain: writing && response.status >= 500,
        },
      );
    return data;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (error.name === 'AbortError' && signal?.aborted) throw error;
    throw new ApiError(
      error.name === 'AbortError'
        ? 'Connection timed out.'
        : 'Unable to connect to the API. Check the backend, API URL and CORS configuration.',
      {
        code: error.name === 'AbortError' ? 'TIMEOUT' : 'NETWORK_ERROR',
        uncertain: writing,
      },
    );
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener('abort', abort);
  }
}
