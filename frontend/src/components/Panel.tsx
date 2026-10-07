import type { ReactNode } from 'react';

interface PanelProps {
  title: string;
  description?: ReactNode;
  actions?: ReactNode;
  flush?: boolean;
  children: ReactNode;
}

export function Panel({ title, description, actions, flush = false, children }: PanelProps) {
  return (
    <section className="panel">
      <header>
        <div>
          <h2>{title}</h2>
          {description && <p className="muted">{description}</p>}
        </div>
        {actions && <div className="actions">{actions}</div>}
      </header>
      <div className={flush ? 'panel-body flush' : 'panel-body'}>{children}</div>
    </section>
  );
}
