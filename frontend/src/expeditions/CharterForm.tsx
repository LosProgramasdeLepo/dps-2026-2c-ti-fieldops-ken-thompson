import { useState } from 'react';
import { peopleApi } from '../api/catalog';
import { expeditionsApi } from '../api/expeditions';
import type { Period, Uuid } from '../api/types';
import { CheckList } from '../components/CheckList';
import { ErrorNotice } from '../components/ErrorNotice';
import { FieldGroup } from '../components/Field';
import { PeriodInput } from '../components/PeriodInput';
import { TagInput } from '../components/TagInput';
import { useAction } from '../hooks/useAction';
import { useLoad } from '../hooks/useLoad';
import { defaultPeriod } from '../time';

export function CharterForm({ onCreated }: { onCreated: (id: Uuid) => void }) {
  const people = useLoad(() => peopleApi.all(), []);
  const [objectives, setObjectives] = useState<string[]>([]);
  const [period, setPeriod] = useState<Period>(defaultPeriod());
  const [zones, setZones] = useState<string[]>([]);
  const [responsibles, setResponsibles] = useState<Uuid[]>([]);
  const [restrictions, setRestrictions] = useState<string[]>([]);
  const action = useAction();

  const draft = async () => {
    const created = await expeditionsApi.draft({ objectives, period, zones, responsibles, restrictions });
    onCreated(created.id);
  };

  return (
    <form className="form" onSubmit={action.onSubmit(draft)}>
      <FieldGroup label="Objetivos">
        <TagInput label="Objetivos" values={objectives} onChange={setObjectives} placeholder="Escribí un objetivo y presioná Enter" required />
      </FieldGroup>
      <FieldGroup label="Período">
        <PeriodInput value={period} onChange={setPeriod} />
      </FieldGroup>
      <FieldGroup label="Zonas de trabajo">
        <TagInput label="Zonas de trabajo" values={zones} onChange={setZones} placeholder="Por ejemplo Delta, y Enter" required />
      </FieldGroup>
      <FieldGroup label="Responsables">
        <CheckList
          options={(people.data ?? []).map((person) => ({ value: person.id, label: person.name }))}
          selected={responsibles}
          onChange={setResponsibles}
        />
      </FieldGroup>
      <FieldGroup label="Restricciones generales">
        <TagInput label="Restricciones generales" values={restrictions} onChange={setRestrictions} placeholder="Opcional: escribí una restricción y presioná Enter" />
      </FieldGroup>
      <ErrorNotice error={people.error ?? action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Crear borrador</button>
      </div>
    </form>
  );
}
