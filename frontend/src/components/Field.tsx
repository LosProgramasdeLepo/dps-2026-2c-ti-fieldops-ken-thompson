import type { ReactNode } from 'react';

interface FieldProps {
  label: string;
  wide?: boolean;
  children: ReactNode;
}

export function Field({ label, wide = false, children }: FieldProps) {
  return (
    <label className={wide ? 'field span' : 'field'}>
      <span>{label}</span>
      {children}
    </label>
  );
}

export function FieldGroup({ label, wide = false, children }: FieldProps) {
  return (
    <div role="group" aria-label={label} className={wide ? 'field span' : 'field'}>
      <span>{label}</span>
      {children}
    </div>
  );
}
