import type { Availability } from '../api/types';
import { formatPeriod } from '../time';

const ALWAYS_FROM = '1970-01-01';
const ALWAYS_UNTIL = '9999-12-31';

export function AvailabilityText({ availability }: { availability: Availability }) {
  const always = availability.periods.some(
    (period) => period.start.startsWith(ALWAYS_FROM) && period.end.startsWith(ALWAYS_UNTIL),
  );
  if (always) {
    return <span>Siempre</span>;
  }
  if (availability.periods.length === 0) {
    return <span className="muted">Nunca</span>;
  }
  return (
    <span className="stack">
      {availability.periods.map((period) => (
        <span key={period.start}>{formatPeriod(period)}</span>
      ))}
    </span>
  );
}
