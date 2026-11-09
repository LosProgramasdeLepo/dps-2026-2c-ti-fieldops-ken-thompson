import { useEffect } from 'react';

export const POLL_INTERVAL_MS = 2000;

export function usePolling(poll: () => void, active: boolean, intervalMs = POLL_INTERVAL_MS) {
  useEffect(() => {
    if (!active) {
      return undefined;
    }
    const timer = window.setInterval(poll, intervalMs);
    return () => window.clearInterval(timer);
  }, [poll, active, intervalMs]);
}
