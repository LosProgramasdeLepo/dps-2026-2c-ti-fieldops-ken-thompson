import { useState } from 'react';
import { Link } from 'react-router';
import { expeditionsApi, findRun } from '../api/expeditions';
import type { ActivityExecution, ActivityItem, Expedition } from '../api/types';
import { ConsumptionInput, toConsumption, type ConsumptionRow } from '../components/ConsumptionInput';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Panel } from '../components/Panel';
import { StatusBadge } from '../components/StatusBadge';
import { useAction } from '../hooks/useAction';
import type { Catalog } from '../hooks/useCatalog';
import { useLoad } from '../hooks/useLoad';
import { usePolling } from '../hooks/usePolling';
import { REPLAN_CONSUMER } from '../labels';
import { formatInstant, formatPeriod } from '../time';
import { activitiesOf, activityName, consumptionOf, type PlanTabProps } from './describe';

async function startedAncestor(plan: Expedition): Promise<string | null> {
  for (let previous = plan.supersedes; previous; ) {
    if (await findRun(previous)) {
      return previous;
    }
    previous = (await expeditionsApi.get(previous)).supersedes;
  }
  return null;
}

interface LogEntry {
  at: string;
  kind: 'incident' | 'observation';
  text: string;
  activityId?: string;
}

export function RunTab({ plan, catalog, onChange }: PlanTabProps) {
  const run = useLoad(() => findRun(plan.id), [plan]);
  const inForceId = run.data?.inForceId;
  const governing = useLoad(() => (inForceId && inForceId !== plan.id ? expeditionsApi.get(inForceId) : Promise.resolve(plan)), [inForceId, plan]);
  const owner = useLoad(() => (run.data === null ? startedAncestor(plan) : Promise.resolve(null)), [run.data, plan]);
  const refresh = () => {
    run.reload();
    onChange();
  };
  const action = useAction(refresh);

  if (run.error ?? owner.error) {
    return <ErrorNotice error={run.error ?? owner.error} />;
  }
  if (run.data === undefined || (run.data === null && owner.data === undefined)) {
    return <p className="muted">Cargando…</p>;
  }
  if (run.data === null) {
    return (
      <Panel title="Corrida">
        {owner.data ? (
          <p>
            La corrida se registra en la versión donde empezó la expedición. <Link to={`/expeditions/${owner.data}?tab=run`}>Ir a la corrida</Link>.
          </p>
        ) : plan.status === 'APPROVED' ? (
          <>
            <p>El plan está aprobado. La expedición se puede iniciar dentro de su período.</p>
            <div className="actions">
              <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.start(plan.id))}>
                Iniciar expedición
              </button>
            </div>
          </>
        ) : plan.status === 'SUPERSEDED' ? (
          <p className="muted">Una revisión aprobada reemplazó a este plan: la expedición se inicia desde la versión vigente.</p>
        ) : (
          <p className="muted">La expedición se inicia cuando el plan está aprobado.</p>
        )}
        <ErrorNotice error={action.error} />
      </Panel>
    );
  }

  const current = run.data;
  const inForce = governing.data ?? plan;
  const active = current.status !== 'FINISHED';
  const log: LogEntry[] = [
    ...current.incidents.map((incident) => ({ at: incident.at, kind: 'incident' as const, text: incident.description, activityId: incident.activityId })),
    ...current.observations.map((observation) => ({ at: observation.at, kind: 'observation' as const, text: observation.text })),
  ].sort((left, right) => left.at.localeCompare(right.at));

  return (
    <>
      <Panel
        title="Corrida"
        actions={
          <>
            {current.status === 'IN_PROGRESS' && (
              <>
                <button type="button" className="secondary" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.suspend(plan.id))}>
                  Suspender
                </button>
                <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.finish(plan.id))}>
                  Finalizar expedición
                </button>
              </>
            )}
            {current.status === 'SUSPENDED' && (
              <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.resume(plan.id))}>
                Reanudar
              </button>
            )}
          </>
        }
      >
        <div className="row">
          <StatusBadge status={current.status} />
          <span className="muted">
            {current.status === 'IN_PROGRESS' && 'Cada actividad se inicia dentro de su ventana y después de sus predecesoras.'}
            {current.status === 'SUSPENDED' && 'Con la corrida suspendida se pueden registrar incidentes y observaciones, pero no avanzar actividades.'}
            {current.status === 'FINISHED' && 'La expedición terminó.'}
          </span>
        </div>
        {inForce.id !== plan.id && (
          <p className="notice">
            Rige la revisión v{inForce.version}: las actividades y sus ventanas son las de esa versión, y las propuestas por incidentes se registran en{' '}
            <Link to={`/expeditions/${inForce.id}?tab=replan`}>su replanificación</Link>.
          </p>
        )}
        <ErrorNotice error={action.error ?? governing.error} />
      </Panel>
      <Panel title="Actividades" flush>
        <table>
          <thead>
            <tr>
              <th>Actividad</th>
              <th>Estado</th>
              <th>Inicio</th>
              <th>Cierre y resultado</th>
            </tr>
          </thead>
          <tbody>
            {activitiesOf(inForce.itinerary).map((activity) => (
              <ActivityRow
                key={activity.id}
                plan={plan}
                catalog={catalog}
                activity={activity}
                execution={current.activities.find((execution) => execution.activityId === activity.id)}
                running={current.status === 'IN_PROGRESS'}
                onDone={refresh}
              />
            ))}
          </tbody>
        </table>
      </Panel>
      {active && (
        <Panel title="Registrar en la bitácora" description="Un incidente sobre una actividad de un plan aprobado genera, en segundo plano, una propuesta de replanificación.">
          <RecordObservation plan={plan} onDone={refresh} />
          <RecordIncident plan={plan} inForce={inForce} onDone={refresh} />
        </Panel>
      )}
      <Panel title="Bitácora" flush>
        {log.length === 0 ? (
          <Empty>Sin incidentes ni observaciones.</Empty>
        ) : (
          <ul className="list">
            {log.map((entry) => (
              <li key={`${entry.at}-${entry.kind}-${entry.text}`}>
                <span className="when">{formatInstant(entry.at)}</span>
                <span className={`pill pill-${entry.kind}`}>{entry.kind === 'incident' ? 'Incidente' : 'Observación'}</span>
                <span className="grow">
                  {entry.text}
                  {entry.activityId && <span className="muted"> en {activityName(inForce, entry.activityId)}</span>}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Panel>
    </>
  );
}

interface ActivityRowProps {
  plan: Expedition;
  catalog: Catalog;
  activity: ActivityItem;
  execution?: ActivityExecution;
  running: boolean;
  onDone: () => void;
}

function ActivityRow({ plan, catalog, activity, execution, running, onDone }: ActivityRowProps) {
  const [result, setResult] = useState('');
  const [consumed, setConsumed] = useState<ConsumptionRow[]>([]);
  const action = useAction(onDone);
  const state = execution?.finishedAt ? 'done' : execution ? 'running' : 'waiting';

  return (
    <tr>
      <td>
        <strong>{activity.name}</strong>
        <div className="muted small">{formatPeriod(activity.window)}</div>
      </td>
      <td>
        <span className={`pill pill-${state}`}>{{ done: 'Finalizada', running: 'En curso', waiting: 'Sin iniciar' }[state]}</span>
      </td>
      <td>
        {execution ? (
          formatInstant(execution.startedAt)
        ) : (
          running && (
            <button type="button" className="secondary small" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.startActivity(plan.id, activity.id))}>
              Iniciar
            </button>
          )
        )}
      </td>
      <td>
        {execution?.finishedAt && (
          <>
            {formatInstant(execution.finishedAt)}
            <div className="muted small">{execution.result}</div>
            {Object.keys(execution.consumption).length > 0 && (
              <div className="muted small">Consumo: {consumptionOf(execution.consumption, catalog)}</div>
            )}
          </>
        )}
        {execution && !execution.finishedAt && running && (
          <form className="form inline" onSubmit={action.onSubmit(() => expeditionsApi.finishActivity(plan.id, activity.id, result, toConsumption(consumed)))}>
            <input required aria-label={`Resultado de ${activity.name}`} placeholder="Resultado" value={result} onChange={(event) => setResult(event.target.value)} />
            <ConsumptionInput rows={consumed} onChange={setConsumed} catalog={catalog} />
            <button className="small" disabled={action.busy}>
              Finalizar
            </button>
          </form>
        )}
        <ErrorNotice error={action.error} />
      </td>
    </tr>
  );
}

function RecordObservation({ plan, onDone }: { plan: Expedition; onDone: () => void }) {
  const [text, setText] = useState('');
  const action = useAction(() => {
    setText('');
    onDone();
  });

  return (
    <form className="form inline" onSubmit={action.onSubmit(() => expeditionsApi.observe(plan.id, text))}>
      <Field label="Observación">
        <input required value={text} onChange={(event) => setText(event.target.value)} />
      </Field>
      <button className="secondary" disabled={action.busy}>
        Registrar observación
      </button>
      <ErrorNotice error={action.error} />
    </form>
  );
}

function RecordIncident({ plan, inForce, onDone }: { plan: Expedition; inForce: Expedition; onDone: () => void }) {
  const [description, setDescription] = useState('');
  const [activityId, setActivityId] = useState('');
  const [awaited, setAwaited] = useState<string>();
  const action = useAction(() => {
    setDescription('');
    setActivityId('');
    onDone();
  });

  async function record() {
    const affected = activityId || undefined;
    const created = await expeditionsApi.recordIncident(plan.id, description, affected);
    setAwaited(affected ? created.id : undefined);
  }

  return (
    <form className="form inline" onSubmit={action.onSubmit(record)}>
      <Field label="Incidente">
        <input required value={description} onChange={(event) => setDescription(event.target.value)} />
      </Field>
      <Field label="Actividad afectada">
        <select value={activityId} onChange={(event) => setActivityId(event.target.value)}>
          <option value="">Ninguna</option>
          {activitiesOf(inForce.itinerary).map((activity) => (
            <option key={activity.id} value={activity.id}>
              {activity.name}
            </option>
          ))}
        </select>
      </Field>
      <button className="secondary" disabled={action.busy}>
        Registrar incidente
      </button>
      <ErrorNotice error={action.error} />
      {awaited && <ProposalProgress key={awaited} plan={plan} inForce={inForce} incidentId={awaited} />}
    </form>
  );
}

function ProposalProgress({ plan, inForce, incidentId }: { plan: Expedition; inForce: Expedition; incidentId: string }) {
  const incident = useLoad(() => expeditionsApi.incident(plan.id, incidentId), [plan.id, incidentId]);
  const replan = incident.data?.processing.find((delivery) => delivery.consumer === REPLAN_CONSUMER);
  const settled = incident.error !== undefined || incident.data?.proposalId !== undefined || replan?.status === 'FAILED' || replan?.status === 'DELIVERED';
  usePolling(incident.reload, !settled);

  if (incident.error) {
    return <ErrorNotice error={incident.error} />;
  }
  if (incident.data?.proposalId) {
    return (
      <p className="notice" role="status">
        La propuesta de replanificación está lista: <Link to={`/expeditions/${inForce.id}?tab=replan`}>revisarla</Link>.
      </p>
    );
  }
  if (replan?.status === 'FAILED') {
    return (
      <div className="error" role="alert">
        <div>
          <strong>No se pudo preparar la propuesta después de {replan.attempts} intentos.</strong>
          {replan.lastError && <p className="small">{replan.lastError}</p>}
          <p className="small">
            Se puede reintentar desde <Link to={`/expeditions/${plan.id}?tab=audit`}>Eventos y auditoría</Link>.
          </p>
        </div>
      </div>
    );
  }
  if (replan?.status === 'DELIVERED') {
    return (
      <p className="muted" role="status">
        El plan cambió antes de procesar el incidente: no hace falta una propuesta.
      </p>
    );
  }
  return (
    <p className="muted" role="status">
      Propuesta en preparación…
      {replan?.status === 'RETRYING' && ` Reintentando (intento ${replan.attempts + 1}).`}
    </p>
  );
}
