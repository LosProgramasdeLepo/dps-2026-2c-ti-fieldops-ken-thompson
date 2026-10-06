import { useState, type FormEvent } from 'react';

export function useAction(onDone?: () => void) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);

  async function run(action: () => Promise<unknown>) {
    setBusy(true);
    setError(undefined);
    try {
      await action();
      onDone?.();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  function onSubmit(action: () => Promise<unknown>) {
    return (event: FormEvent) => {
      event.preventDefault();
      void run(action);
    };
  }

  return { run, onSubmit, reset: () => setError(undefined), error, busy };
}
