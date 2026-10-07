import type { ActivityKind, ActivityRequest, Expedition, Instant, Period, RiskLevel, Uuid } from '../api/types';
import { CheckList } from '../components/CheckList';
import { Field, FieldGroup } from '../components/Field';
import { PeriodInput } from '../components/PeriodInput';
import type { Catalog } from '../hooks/useCatalog';
import { ACTIVITY_KINDS, RISK_LABELS } from '../labels';
import { addHours, isoDuration } from '../time';
import { activitiesOf } from './describe';

interface ConsumptionRow {
  consumableId: Uuid;
  amount: number;
}

export interface ActivityDraft {
  kind: ActivityKind;
  certification: Uuid;
  instrument: string;
  name: string;
  hours: number;
  minutes: number;
  risk: RiskLevel;
  zone: string;
  window: Period;
  predecessors: Uuid[];
  consumption: ConsumptionRow[];
}

export function newActivity(plan: Expedition, start: Instant = plan.charter.period.start): ActivityDraft {
  return {
    kind: 'TRANSIT',
    certification: '',
    instrument: '',
    name: '',
    hours: 2,
    minutes: 0,
    risk: 'LOW',
    zone: plan.charter.zones[0] ?? '',
    window: { start, end: addHours(start, 2) },
    predecessors: [],
    consumption: [],
  };
}

export function toActivityRequest(draft: ActivityDraft): ActivityRequest {
  const traits = ACTIVITY_KINDS[draft.kind];
  return {
    kind: draft.kind,
    certification: traits.certification ? draft.certification : undefined,
    instrument: traits.instrument ? draft.instrument : undefined,
    name: draft.name,
    estimatedDuration: isoDuration(draft.hours, draft.minutes),
    risk: draft.risk,
    consumption: Object.fromEntries(draft.consumption.map((row) => [row.consumableId, row.amount])),
    zone: draft.zone,
    window: draft.window,
    predecessors: draft.predecessors,
  };
}

interface ActivityFieldsProps {
  value: ActivityDraft;
  onChange: (draft: ActivityDraft) => void;
  plan: Expedition;
  catalog: Catalog;
}

export function ActivityFields({ value, onChange, plan, catalog }: ActivityFieldsProps) {
  const set = <K extends keyof ActivityDraft>(key: K, changed: ActivityDraft[K]) => onChange({ ...value, [key]: changed });
  const traits = ACTIVITY_KINDS[value.kind];
  const predecessors = activitiesOf(plan.itinerary).map((activity) => ({ value: activity.id, label: activity.name }));

  return (
    <div className="form-grid">
      <Field label="Tipo">
        <select value={value.kind} onChange={(event) => set('kind', event.target.value as ActivityKind)}>
          {Object.entries(ACTIVITY_KINDS).map(([kind, kindTraits]) => (
            <option key={kind} value={kind}>
              {kindTraits.label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Nombre">
        <input required value={value.name} onChange={(event) => set('name', event.target.value)} />
      </Field>
      {traits.certification && (
        <Field label={traits.certification}>
          <select required value={value.certification} onChange={(event) => set('certification', event.target.value)}>
            <option value="">Elegir…</option>
            {catalog.certifications.map((certification) => (
              <option key={certification.id} value={certification.id}>
                {certification.name}
              </option>
            ))}
          </select>
        </Field>
      )}
      {traits.instrument && (
        <Field label="Tipo de instrumento">
          <input required value={value.instrument} onChange={(event) => set('instrument', event.target.value)} />
        </Field>
      )}
      <FieldGroup label="Duración estimada">
        <span className="row">
          <input type="number" aria-label="Horas" min={0} value={value.hours} onChange={(event) => set('hours', Number(event.target.value))} /> h
          <input type="number" aria-label="Minutos" min={0} max={59} value={value.minutes} onChange={(event) => set('minutes', Number(event.target.value))} /> min
        </span>
      </FieldGroup>
      <Field label="Riesgo base">
        <select value={value.risk} onChange={(event) => set('risk', event.target.value as RiskLevel)}>
          {Object.entries(RISK_LABELS).map(([risk, label]) => (
            <option key={risk} value={risk}>
              {label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Zona">
        <select value={value.zone} onChange={(event) => set('zone', event.target.value)}>
          {plan.charter.zones.map((zone) => (
            <option key={zone} value={zone}>
              {zone}
            </option>
          ))}
        </select>
      </Field>
      <FieldGroup label="Ventana" wide>
        <PeriodInput value={value.window} onChange={(window) => set('window', window)} />
      </FieldGroup>
      <FieldGroup label="Depende de" wide>
        <CheckList options={predecessors} selected={value.predecessors} onChange={(selected) => set('predecessors', selected)} />
      </FieldGroup>
      <FieldGroup label="Consumo estimado" wide>
        <ConsumptionInput rows={value.consumption} onChange={(rows) => set('consumption', rows)} catalog={catalog} />
      </FieldGroup>
    </div>
  );
}

interface ConsumptionInputProps {
  rows: ConsumptionRow[];
  onChange: (rows: ConsumptionRow[]) => void;
  catalog: Catalog;
}

function ConsumptionInput({ rows, onChange, catalog }: ConsumptionInputProps) {
  const replace = (index: number, row: ConsumptionRow) => onChange(rows.map((current, position) => (position === index ? row : current)));
  const first = catalog.consumables[0];

  return (
    <div className="stack">
      {rows.map((row, index) => (
        <span key={index} className="row">
          <select aria-label="Consumible" value={row.consumableId} onChange={(event) => replace(index, { ...row, consumableId: event.target.value })}>
            {catalog.consumables.map((consumable) => (
              <option key={consumable.id} value={consumable.id}>
                {consumable.name}
              </option>
            ))}
          </select>
          <input type="number" aria-label="Cantidad" min={0} value={row.amount} onChange={(event) => replace(index, { ...row, amount: Number(event.target.value) })} />
          <button type="button" className="link" onClick={() => onChange(rows.filter((_, position) => position !== index))}>
            Quitar
          </button>
        </span>
      ))}
      {first && (
        <button type="button" className="link" onClick={() => onChange([...rows, { consumableId: first.id, amount: 1 }])}>
          Agregar consumible
        </button>
      )}
    </div>
  );
}
