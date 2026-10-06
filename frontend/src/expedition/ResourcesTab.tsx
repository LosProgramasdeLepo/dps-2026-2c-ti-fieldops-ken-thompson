import { useState } from 'react';
import { expeditionsApi } from '../api/expeditions';
import type { Assignment } from '../api/types';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Icon } from '../components/Icon';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { shortId, type Catalog } from '../hooks/useCatalog';
import { ASSIGNMENT_LABELS } from '../labels';
import { formatSpan } from '../time';
import { activitiesOf, activityName, passengers, resourceOf, type PlanTabProps } from './describe';

export function ResourcesTab({ plan, catalog, onChange }: PlanTabProps) {
  const editable = plan.status === 'DRAFT';
  const [assigning, setAssigning] = useState(false);
  const [suggested, setSuggested] = useState<Assignment[]>();
  const propose = useAction();
  const apply = useAction(() => {
    setSuggested(undefined);
    onChange();
  });

  const loadSuggestions = () => propose.run(async () => setSuggested(await expeditionsApi.suggestions(plan.id)));
  const applyAll = (assignments: Assignment[]) =>
    apply.run(async () => {
      for (const assignment of assignments) {
        await expeditionsApi.addAssignment(plan.id, assignment);
      }
    });

  return (
    <>
      <Panel
        title="Asignaciones"
        description={editable ? 'Asigná a mano o pedí una propuesta: el sistema elige recursos disponibles y libres en cada ventana.' : 'Las asignaciones solo se editan en borrador.'}
        flush
        actions={
          editable && (
            <>
              <button type="button" className="secondary" disabled={propose.busy} onClick={loadSuggestions}>
                Proponer asignaciones
              </button>
              <button type="button" onClick={() => setAssigning(true)}>
                <Icon name="plus" />
                Asignar recurso
              </button>
            </>
          )
        }
      >
        <ErrorNotice error={propose.error} />
        {plan.assignments.length === 0 ? (
          <Empty>Todavía no hay recursos asignados.</Empty>
        ) : (
          <AssignmentTable assignments={plan.assignments} plan={plan} catalog={catalog} />
        )}
      </Panel>
      {suggested && (
        <Panel
          title="Propuesta del sistema"
          description="Nada se asigna hasta que lo apliques."
          flush
          actions={
            <>
              <button type="button" className="secondary" onClick={() => setSuggested(undefined)}>
                Descartar
              </button>
              {suggested.length > 0 && (
                <button type="button" disabled={apply.busy} onClick={() => applyAll(suggested)}>
                  Aplicar todas
                </button>
              )}
            </>
          }
        >
          <ErrorNotice error={apply.error} />
          {suggested.length === 0 ? (
            <Empty>No hace falta asignar nada más, o no hay recursos libres que cumplan los requisitos.</Empty>
          ) : (
            <AssignmentTable assignments={suggested} plan={plan} catalog={catalog} apply={(assignment) => applyAll([assignment])} />
          )}
        </Panel>
      )}
      <Permits plan={plan} catalog={catalog} onChange={onChange} />
      <Drawer title="Asignar recurso" open={assigning} onClose={() => setAssigning(false)}>
        <AssignResource
          plan={plan}
          catalog={catalog}
          onChange={() => {
            setAssigning(false);
            onChange();
          }}
        />
      </Drawer>
    </>
  );
}

interface AssignmentTableProps {
  assignments: Assignment[];
  plan: PlanTabProps['plan'];
  catalog: Catalog;
  apply?: (assignment: Assignment) => void;
}

function AssignmentTable({ assignments, plan, catalog, apply }: AssignmentTableProps) {
  const order = activitiesOf(plan.itinerary).map((activity) => activity.id);
  const sorted = [...assignments].sort((left, right) => order.indexOf(left.activityId) - order.indexOf(right.activityId));
  return (
    <table>
      <thead>
        <tr>
          <th>Actividad</th>
          <th>Recurso</th>
          <th>Tipo</th>
          {apply && <th />}
        </tr>
      </thead>
      <tbody>
        {sorted.map((assignment) => (
          <tr key={JSON.stringify(assignment)}>
            <td>
              <strong>{activityName(plan, assignment.activityId)}</strong>
            </td>
            <td>{resourceOf(assignment, catalog)}</td>
            <td className="muted">{ASSIGNMENT_LABELS[assignment.type]}</td>
            {apply && (
              <td className="end">
                <button type="button" className="secondary small" onClick={() => apply(assignment)}>
                  Aplicar
                </button>
              </td>
            )}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

function resourceOptions(type: Assignment['type'], catalog: Catalog) {
  switch (type) {
    case 'PERSON':
      return catalog.people.map((person) => ({ value: person.id, label: person.name }));
    case 'VEHICLE':
      return catalog.vehicles.map((vehicle) => ({ value: vehicle.id, label: `Vehículo ${shortId(vehicle.id)}, ${passengers(vehicle.capacity)}` }));
    case 'INSTRUMENT':
      return catalog.instruments.map((instrument) => ({ value: instrument.id, label: `${instrument.kind} (${shortId(instrument.id)})` }));
    case 'CONSUMABLE':
      return catalog.consumables.map((consumable) => ({ value: consumable.id, label: `${consumable.name}, stock ${consumable.stock}` }));
  }
}

function toAssignment(type: Assignment['type'], activityId: string, resourceId: string, quantity: number): Assignment {
  switch (type) {
    case 'PERSON':
      return { type, activityId, personId: resourceId };
    case 'VEHICLE':
      return { type, activityId, vehicleId: resourceId };
    case 'INSTRUMENT':
      return { type, activityId, instrumentId: resourceId };
    case 'CONSUMABLE':
      return { type, activityId, consumableId: resourceId, quantity };
  }
}

function AssignResource({ plan, catalog, onChange }: PlanTabProps) {
  const [type, setType] = useState<Assignment['type']>('PERSON');
  const [activityId, setActivityId] = useState('');
  const [resourceId, setResourceId] = useState('');
  const [quantity, setQuantity] = useState(1);
  const action = useAction(onChange);

  return (
    <form className="form" onSubmit={action.onSubmit(() => expeditionsApi.addAssignment(plan.id, toAssignment(type, activityId, resourceId, quantity)))}>
      <Field label="Actividad">
        <select required value={activityId} onChange={(event) => setActivityId(event.target.value)}>
          <option value="">Elegir…</option>
          {activitiesOf(plan.itinerary).map((activity) => (
            <option key={activity.id} value={activity.id}>
              {activity.name}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Tipo de recurso">
        <select
          value={type}
          onChange={(event) => {
            setType(event.target.value as Assignment['type']);
            setResourceId('');
          }}
        >
          {Object.entries(ASSIGNMENT_LABELS).map(([value, label]) => (
            <option key={value} value={value}>
              {label}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Recurso">
        <select required value={resourceId} onChange={(event) => setResourceId(event.target.value)}>
          <option value="">Elegir…</option>
          {resourceOptions(type, catalog).map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </Field>
      {type === 'CONSUMABLE' && (
        <Field label="Cantidad">
          <input type="number" min={1} value={quantity} onChange={(event) => setQuantity(Number(event.target.value))} />
        </Field>
      )}
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Asignar</button>
      </div>
    </form>
  );
}

function Permits({ plan, catalog, onChange }: PlanTabProps) {
  const [permitId, setPermitId] = useState('');
  const action = useAction(onChange);
  const attached = catalog.permits.filter((permit) => plan.permits.includes(permit.id));
  const available = catalog.permits.filter((permit) => !plan.permits.includes(permit.id));

  return (
    <Panel title="Permisos" description="Cada actividad necesita un permiso de su zona vigente en su ventana, más los permisos especiales que pida." flush>
      {attached.length === 0 ? (
        <Empty>Todavía no hay permisos adjuntos.</Empty>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Tipo</th>
              <th>Zona</th>
              <th>Vigencia</th>
            </tr>
          </thead>
          <tbody>
            {attached.map((permit) => (
              <tr key={permit.id}>
                <td>
                  <strong>{permit.kind}</strong>
                </td>
                <td>{permit.zone}</td>
                <td>{formatSpan(permit.validity)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
      {plan.status === 'DRAFT' && available.length > 0 && (
        <div className="panel-body">
          <form className="form inline" onSubmit={action.onSubmit(() => expeditionsApi.attachPermit(plan.id, permitId))}>
            <Field label="Adjuntar permiso">
              <select required value={permitId} onChange={(event) => setPermitId(event.target.value)}>
                <option value="">Elegir…</option>
                {available.map((permit) => (
                  <option key={permit.id} value={permit.id}>
                    {permit.kind}, {permit.zone}, {formatSpan(permit.validity)}
                  </option>
                ))}
              </select>
            </Field>
            <button className="secondary" disabled={action.busy}>
              Adjuntar
            </button>
            <ErrorNotice error={action.error} />
          </form>
        </div>
      )}
    </Panel>
  );
}
