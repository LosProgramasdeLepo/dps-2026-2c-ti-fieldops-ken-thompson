import { useState } from 'react';
import type { Availability } from '../api/types';
import { useAction } from '../hooks/useAction';
import { AvailabilityInput } from './AvailabilityInput';
import { ErrorNotice } from './ErrorNotice';
import { FieldGroup } from './Field';

interface AvailabilityEditorProps {
  initial: Availability;
  save: (availability: Availability) => Promise<unknown>;
  onDone: () => void;
}

export function AvailabilityEditor({ initial, save, onDone }: AvailabilityEditorProps) {
  const [availability, setAvailability] = useState(initial);
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => save(availability))}>
      <FieldGroup label="Disponibilidad">
        <AvailabilityInput value={availability} onChange={setAvailability} />
      </FieldGroup>
      <ErrorNotice error={action.error} />
      <button disabled={action.busy}>Guardar disponibilidad</button>
    </form>
  );
}
