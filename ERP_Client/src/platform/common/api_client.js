const csrf_cookie_name = 'XSRF-TOKEN';
const csrf_header_name = 'X-XSRF-TOKEN';
let csrf_request = null;

function read_cookie(cookie_name) {
  const cookie = document.cookie
    .split('; ')
    .find((item) => item.startsWith(cookie_name + '='));
  return cookie ? decodeURIComponent(cookie.substring(cookie_name.length + 1)) : null;
}

async function ensure_csrf_token() {
  if (csrf_request) return csrf_request;
  csrf_request = fetch('/api/v1/auth/csrf', { credentials: 'include', cache: 'no-store' })
    .then(async (response) => {
      let payload = null;
      try { payload = await response.json(); } catch { payload = null; }
      if (!response.ok) {
        const error = new Error(payload?.message || 'Unable to initialize the security session.');
        error.status = response.status;
        error.code = payload?.code || 'CSRF_INITIALIZATION_FAILED';
        throw error;
      }
      return payload;
    })
    .finally(() => { csrf_request = null; });
  return csrf_request;
}

async function request_api(path, options = {}) {
  const method = (options.method || 'GET').toUpperCase();
  const is_mutation = !['GET', 'HEAD', 'OPTIONS'].includes(method);

  async function send_request(force_csrf_refresh = false) {
    if (is_mutation && (force_csrf_refresh || !read_cookie(csrf_cookie_name))) {
      await ensure_csrf_token();
    }
    const headers = new Headers(options.headers || {});
    if (options.body && !headers.has('Content-Type')) {
      headers.set('Content-Type', 'application/json');
    }
    if (is_mutation) {
      const csrf_token = read_cookie(csrf_cookie_name);
      if (csrf_token) headers.set(csrf_header_name, csrf_token);
    }
    return fetch(path, {
      ...options,
      method,
      headers,
      credentials: 'include',
    });
  }

  let response = await send_request();
  // A browser can keep an XSRF cookie from a previous backend process. Spring
  // reports a rejected CSRF token as a generic 403, so retry one time with a
  // fresh token. Permission 403 responses remain errors after the retry.
  if (is_mutation && response.status === 403) {
    response = await send_request(true);
  }
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