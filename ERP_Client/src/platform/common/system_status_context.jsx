import { createContext, useCallback, useContext, useEffect, useMemo, useRef, useState } from 'react';

const system_status_context = createContext(null);

function SystemStatusProvider({ children }) {
  const [status, set_status] = useState({ state: 'checking', checked_at: null, latency_ms: null });
  const request_in_flight = useRef(false);

  const check_status = useCallback(async () => {
    if (request_in_flight.current) return;
    request_in_flight.current = true;
    const started_at = performance.now();
    set_status((current_status) => ({ ...current_status, state: 'checking' }));
    try {
      const response = await fetch('/actuator/health', {
        credentials: 'include',
        cache: 'no-store',
        headers: { Accept: 'application/json' },
      });
      const payload = await response.json().catch(() => null);
      const healthy = response.ok && payload?.status === 'UP';
      set_status({
        state: healthy ? 'online' : 'degraded',
        checked_at: new Date().toISOString(),
        latency_ms: Math.round(performance.now() - started_at),
      });
    } catch {
      set_status({
        state: 'offline',
        checked_at: new Date().toISOString(),
        latency_ms: null,
      });
    } finally {
      request_in_flight.current = false;
    }
  }, []);

  useEffect(() => {
    check_status();
    const timer_id = window.setInterval(check_status, 60_000);
    const on_visibility_change = () => {
      if (document.visibilityState === 'visible') check_status();
    };
    document.addEventListener('visibilitychange', on_visibility_change);
    return () => {
      window.clearInterval(timer_id);
      document.removeEventListener('visibilitychange', on_visibility_change);
    };
  }, [check_status]);

  const value = useMemo(() => ({ ...status, check_status }), [status, check_status]);
  return <system_status_context.Provider value={value}>{children}</system_status_context.Provider>;
}

function useSystemStatus() {
  const context = useContext(system_status_context);
  if (!context) throw new Error('use_system_status must be used inside SystemStatusProvider.');
  return context;
}

const use_system_status = useSystemStatus;

export { SystemStatusProvider, use_system_status };


