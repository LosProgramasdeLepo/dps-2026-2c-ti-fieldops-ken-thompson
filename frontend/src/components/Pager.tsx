import type { Page } from '../api/types';

interface PagerProps {
  page: Page<unknown>;
  onChange: (page: number) => void;
}

export function Pager({ page, onChange }: PagerProps) {
  const pages = Math.max(page.totalPages, 1);
  return (
    <div className="pager">
      <span className="muted">
        {page.totalItems === 1 ? '1 registro' : `${page.totalItems} registros`}
        {pages > 1 && `, página ${Math.min(page.page + 1, pages)} de ${pages}`}
      </span>
      {pages > 1 && (
        <div className="actions">
          <button type="button" className="secondary small" disabled={page.page === 0} onClick={() => onChange(page.page - 1)}>
            Anterior
          </button>
          <button type="button" className="secondary small" disabled={page.page + 1 >= page.totalPages} onClick={() => onChange(page.page + 1)}>
            Siguiente
          </button>
        </div>
      )}
    </div>
  );
}
