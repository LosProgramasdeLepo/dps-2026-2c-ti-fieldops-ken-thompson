import type { ReactNode } from 'react';

export function Empty({ children }: { children: ReactNode }) {
  return <p className="empty">{children}</p>;
}
