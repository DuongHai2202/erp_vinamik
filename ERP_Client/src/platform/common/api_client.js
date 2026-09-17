const csrf_cookie_name = 'XSRF-TOKEN';
const csrf_header_name = 'X-XSRF-TOKEN';

function read_cookie(cookie_name) {
  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(`${cookie_name}=`));
  return cookie ? decodeURIComponent(cookie.substring(cookie_name.length + 1)) : null;
}

async function ensure_csrf_token() {
  const response = await fetch('/api/v1/auth/csrf', { credentials: 'include' });
  if (!response.ok) {
    throw new Error('Unable to initialize the security session.');
  }
  return response.json();
}

async function request_api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const headers = new Headers(options.headers || {});
  if (options.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }
  if (method !== 'GET' && method !== 'HEAD' && method !== 'OPTIONS') {
    if (!read_cookie(csrf_cookie_name)) {
      await ensure_csrf_token();
    }
    const csrf_token = read_cookie(csrf_cookie_name);
    if (csrf_token) {
      headers.set(csrf_header_name, csrf_token);
    }
  }

  const response = await fetch(path, {
    ...options,
    method,
    headers,
    credentials: 'include',
  });
  let payload = null;
  try {
    payload = await response.json();
  } catch {
    payload = null;
  }
  if (!response.ok) {
    const error = new Error(payload?.message || 'The request could not be completed.');
    error.status = response.status;
    error.code = payload?.code || 'REQUEST_FAILED';
    error.field_errors = payload?.field_errors || {};
    throw error;
  }
  return payload;
}

export { ensure_csrf_token, request_api };