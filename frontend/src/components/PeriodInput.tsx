import type { Period } from '../api/types';
import { fromInput, toInput } from '../time';

interface PeriodInputProps {
  value: Period;
  onChange: (period: Period) => void;
}

export function PeriodInput({ value, onChange }: PeriodInputProps) {
  return (
    <span className="period">
      <input
        type="datetime-local"
        aria-label="Desde"
        required
        value={toInput(value.start)}
        onChange={(event) => onChange({ ...value, start: fromInput(event.target.value) })}
      />
      <span className="muted">a</span>
      <input
        type="datetime-local"
        aria-label="Hasta"
        required
        value={toInput(value.end)}
        onChange={(event) => onChange({ ...value, end: fromInput(event.target.value) })}
      />
      <span className="muted">UTC</span>
    </span>
  );
}
