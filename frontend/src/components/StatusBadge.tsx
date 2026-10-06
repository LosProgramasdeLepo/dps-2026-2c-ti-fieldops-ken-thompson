import type { OperationalStatus } from '../api/types';
import { STATUS_LABELS } from '../labels';

export function StatusBadge({ status }: { status: OperationalStatus }) {
  return <span className={`pill pill-${status.toLowerCase()}`}>{STATUS_LABELS[status]}</span>;
}
