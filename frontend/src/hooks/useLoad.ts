import { useCallback, useEffect, useState, type DependencyList } from 'react';

export interface Loaded<T> {
  data?: T;
  error?: unknown;
  reload: () => void;
}

export function useLoad<T>(load: () => Promise<T>, deps: DependencyList): Loaded<T> {
  const [state, setState] = useState<{ data?: T; error?: unknown }>({});
  const [version, setVersion] = useState(0);

  useEffect(() => {
    let current = true;
    load().then(
      (data) => current && setState({ data }),
      (error: unknown) => current && setState({ error }),
    );
    return () => {
      current = false;
    };
  }, [...deps, version]);

  const reload = useCallback(() => setVersion((value) => value + 1), []);
  return { ...state, reload };
}
