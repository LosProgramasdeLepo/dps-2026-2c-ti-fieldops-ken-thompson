import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { expeditionsApi } from '../api/expeditions';
import type { Expedition, Proposal } from '../api/types';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import type { Catalog } from '../hooks/useCatalog';
import { usePaged } from '../hooks/usePaged';
import { DECISION_LABELS } from '../labels';
import { formatInstant } from '../time';
import { activitiesOf, activityName, changesBetween, personName, type PlanTabProps } from './describe';

export function ReplanTab({ plan, catalog, onChange }: PlanTabProps) {
  const proposals = usePaged((page) => expeditionsApi.proposals(plan.id, page));

  return (
    <>
      <Panel
        title="Propuestas por incidentes"
        description="Al registrar un incidente sobre una actividad, el sistema propone reprogramar, reemplazar recursos o cancelar. El plan vigente no cambia hasta que se aprueba la revisión."
        flush
      >
        <ErrorNotice error={proposals.error} />
        {proposals.data && proposals.data.totalItems === 0 && <Empty>Sin propuestas. Aparecen cuando se registra un incidente durante la corrida.</Empty>}
        {proposals.data?.items.map((proposal) => (
          <ProposalItem key={proposal.id} plan={plan} proposal={proposal} catalog={catalog} onDone={proposals.reload} />
        ))}
        {proposals.data && proposals.data.totalPages > 1 && <Pager page={proposals.data} onChange={proposals.setPage} />}
      </Panel>
      {plan.status === 'APPROVED' && <Revise plan={plan} />}
      {plan.status === 'DRAFT' && plan.supersedes && <AdjustRevision plan={plan} catalog={catalog} onChange={onChange} />}
    </>
  );
}

interface ProposalItemProps {
  plan: Expedition;
  proposal: Proposal;
  catalog: Catalog;
  onDone: () => void;
}

function ProposalItem({ plan, proposal, catalog, onDone }: ProposalItemProps) {
  const [responsible, setResponsible] = useState(plan.charter.responsibles[0] ?? '');
  const action = useAction(onDone);
  const incident = proposal.incident;

  return (
    <article className="proposal">
      <div className="row">
        <span className={`pill pill-${proposal.decision.toLowerCase()}`}>{DECISION_LABELS[proposal.decision]}</span>
        <strong>«{incident.description}»</strong>
      </div>
      <p className="muted">
        Incidente registrado el {formatInstant(incident.at)}
        {incident.activityId && ` sobre ${activityName(plan, incident.activityId)}`}.
      </p>
      <ul>
        {changesBetween(plan, proposal.suggested).map((change) => (
          <li key={change}>{change}</li>
        ))}
      </ul>
      {proposal.decidedBy && proposal.decidedAt && (
        <p className="muted">
          {proposal.decision === 'ACCEPTED' ? 'Aceptada' : 'Rechazada'} por {personName(catalog, proposal.decidedBy)} el {formatInstant(proposal.decidedAt)}.
        </p>
      )}
      {proposal.decision === 'ACCEPTED' && (
        <p>
          <Link to={`/expeditions/${proposal.suggested.id}?tab=review`}>Abrir la revisión v{proposal.suggested.version}</Link> para enviarla a revisión y
          aprobarla.
        </p>
      )}
      {proposal.decision === 'PENDING' && (
        <div className="form inline">
          <Field label="Decide">
            <select value={responsible} onChange={(event) => setResponsible(event.target.value)}>
              {plan.charter.responsibles.map((id) => (
                <option key={id} value={id}>
                  {personName(catalog, id)}
                </option>
              ))}
            </select>
          </Field>
          <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.acceptProposal(proposal.id, responsible))}>
            Aceptar propuesta
          </button>
          <button type="button" className="danger" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.rejectProposal(proposal.id, responsible))}>
            Rechazar
          </button>
        </div>
      )}
      <ErrorNotice error={action.error} />
    </article>
  );
}

function Revise({ plan }: { plan: Expedition }) {
  const navigate = useNavigate();
  const action = useAction();
  const revise = async () => {
    const revision = await expeditionsApi.revise(plan.id);
    navigate(`/expeditions/${revision.id}?tab=replan`);
  };

  return (
    <Panel title="Replanificación manual" description="Un plan aprobado no se edita: se crea una revisión en borrador que rige cuando se aprueba.">
      <div className="actions">
        <button type="button" className="secondary" disabled={action.busy} onClick={() => action.run(revise)}>
          Crear revisión
        </button>
      </div>
      <ErrorNotice error={action.error} />
    </Panel>
  );
}

function AdjustRevision({ plan, onChange }: PlanTabProps) {
  const activities = activitiesOf(plan.itinerary);
  const [activityId, setActivityId] = useState('');
  const [hours, setHours] = useState(1);
  const action = useAction(onChange);

  return (
    <Panel title="Ajustar la revisión" description="Cancelá o retrasá actividades, o reemplazá los recursos que dejaron de estar disponibles.">
      <div className="form inline">
        <Field label="Actividad">
          <select value={activityId} onChange={(event) => setActivityId(event.target.value)}>
            <option value="">Elegir…</option>
            {activities.map((activity) => (
              <option key={activity.id} value={activity.id}>
                {activity.name}
              </option>
            ))}
          </select>
        </Field>
        <Field label="Retraso (horas)">
          <input type="number" min={1} value={hours} onChange={(event) => setHours(Number(event.target.value))} />
        </Field>
        <button type="button" className="secondary" disabled={!activityId || action.busy} onClick={() => action.run(() => expeditionsApi.delayActivity(plan.id, activityId, `PT${hours}H`))}>
          Retrasar
        </button>
        <button type="button" className="danger" disabled={!activityId || action.busy} onClick={() => action.run(() => expeditionsApi.cancelActivity(plan.id, activityId))}>
          Cancelar actividad
        </button>
      </div>
      <div className="actions">
        <button type="button" className="secondary" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.replaceUnavailable(plan.id))}>
          Reemplazar recursos no disponibles
        </button>
      </div>
      <ErrorNotice error={action.error} />
    </Panel>
  );
}
