import { createContext, useContext, useEffect, useMemo, useState } from 'react';
import { ensure_csrf_token, request_api } from '../common/api_client';

const auth_context = createContext(null);

function AuthProvider({ children }) {
  const [current_user, set_current_user] = useState(null);
  const [is_loading, set_is_loading] = useState(true);

  useEffect(() => {
    let is_active = true;
    async function load_session() {
      try {
        await ensure_csrf_token();
        const response = await request_api('/api/v1/auth/me');
        if (is_active) {
          set_current_user(response.data);
        }
      } catch {
        if (is_active) {
          set_current_user(null);
        }
      } finally {
        if (is_active) {
          set_is_loading(false);
        }
      }
    }
    load_session();
    return () => { is_active = false; };
  }, []);

  const login = async (username, password) => {
    await ensure_csrf_token();
    const response = await request_api('/api/v1/auth/login', {
      method: 'POST',
      body: JSON.stringify({ username, password }),
    });
    set_current_user(response.data);
    return response.data;
  };

  const logout = async () => {
    try {
      await request_api('/api/v1/auth/logout', { method: 'POST' });
    } finally {
      set_current_user(null);
    }
  };

  const value = useMemo(() => ({ current_user, is_loading, login, logout }), [current_user, is_loading]);
  return <auth_context.Provider value={value}>{children}</auth_context.Provider>;
}

function useAuth() {
  const context = useContext(auth_context);
  if (!context) {
    throw new Error('use_auth must be used inside AuthProvider.');
  }
  return context;
}

const use_auth = useAuth;

export { AuthProvider, use_auth };