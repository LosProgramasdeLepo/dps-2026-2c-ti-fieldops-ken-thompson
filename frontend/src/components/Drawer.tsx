import { useEffect, useRef, type ReactNode } from 'react';
import { Icon } from './Icon';

interface DrawerProps {
  title: string;
  open: boolean;
  onClose: () => void;
  wide?: boolean;
  children: ReactNode;
}

export function Drawer({ title, open, onClose, wide = false, children }: DrawerProps) {
  const dialog = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const element = dialog.current;
    if (open && !element?.open) {
      element?.showModal();
    }
    if (!open && element?.open) {
      element.close();
    }
  }, [open]);

  return (
    <dialog
      ref={dialog}
      className={wide ? 'drawer wide' : 'drawer'}
      aria-label={title}
      onClose={onClose}
      onClick={(event) => event.target === dialog.current && onClose()}
    >
      <header>
        <h2>{title}</h2>
        <button type="button" className="icon-button" aria-label="Cerrar" onClick={onClose}>
          <Icon name="close" />
        </button>
      </header>
      <div className="drawer-body">{open && children}</div>
    </dialog>
  );
}
