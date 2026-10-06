import type { Availability, Period } from '../api/types';
import { defaultPeriod } from '../time';
import { PeriodInput } from './PeriodInput';

interface AvailabilityInputProps {
  value: Availability;
  onChange: (availability: Availability) => void;
}

export function AvailabilityInput({ value, onChange }: AvailabilityInputProps) {
  const replace = (index: number, period: Period) =>
    onChange({ periods: value.periods.map((current, position) => (position === index ? period : current)) });
  const remove = (index: number) => onChange({ periods: value.periods.filter((_, position) => position !== index) });

  return (
    <div className="stack">
      {value.periods.map((period, index) => (
        <div key={index} className="row">
          <PeriodInput value={period} onChange={(changed) => replace(index, changed)} />
          <button type="button" className="link" onClick={() => remove(index)}>
            Quitar
          </button>
        </div>
      ))}
      {value.periods.length === 0 && <span className="muted">Sin períodos: nunca disponible</span>}
      <button type="button" className="link" onClick={() => onChange({ periods: [...value.periods, defaultPeriod(30)] })}>
        Agregar período
      </button>
    </div>
  );
}
