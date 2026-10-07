import { useState } from 'react';
import { expeditionsApi } from '../api/expeditions';
import type { Expedition, Issue } from '../api/types';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import type { Catalog } from '../hooks/useCatalog';
import { useLoad } from '../hooks/useLoad';
import { SEVERITY_LABELS } from '../labels';
import { personName, type PlanTabProps } from './describe';

const sameIssue = (left: Issue, right: Issue) =>
  left.severity === right.severity && left.code === right.code && left.message === right.message;

function plural(count: number, one: string, many: string): string {
  return `${count} ${count === 1 ? one : many}`;
}

export function ReviewTab({ plan, catalog, onChange }: PlanTabProps) {
  const validation = useLoad(() => expeditionsApi.validation(plan.id), [plan]);
  const action = useAction(onChange);
  const accepted = () => {
    action.reset();
    onChange();
  };
  const issues = validation.data?.issues ?? [];
  const isAccepted = (issue: Issue) => plan.acceptedWarnings.some((warning) => sameIssue(warning, issue));
  const critical = issues.filter((issue) => issue.severity === 'CRITICAL').length;
  const pending = issues.filter((issue) => issue.severity === 'WARNING' && !isAccepted(issue)).length;

  return (
    <>
      <Panel title="Aprobación">
        <p>
          {plan.status === 'DRAFT' && 'Cuando el plan no tenga errores críticos, envialo a revisión.'}
          {plan.status === 'IN_REVIEW' && 'Para aprobarlo no puede haber errores críticos y cada advertencia tiene que estar aceptada por un responsable.'}
          {plan.status === 'APPROVED' && 'El plan está aprobado. Para cambiarlo, creá una revisión desde Replanificación.'}
          {plan.status === 'SUPERSEDED' && 'Una revisión aprobada reemplazó a este plan.'}
        </p>
        {validation.data && (plan.status === 'DRAFT' || plan.status === 'IN_REVIEW') && (
          <p className="muted">
            {plural(critical, 'error crítico', 'errores críticos')} y {plural(pending, 'advertencia sin aceptar', 'advertencias sin aceptar')}.
          </p>
        )}
        {(plan.status === 'DRAFT' || plan.status === 'IN_REVIEW') && (
          <div className="actions">
            {plan.status === 'DRAFT' && (
              <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.submit(plan.id))}>
                Enviar a revisión
              </button>
            )}
            {plan.status === 'IN_REVIEW' && (
              <>
                <button type="button" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.approve(plan.id))}>
                  Aprobar
                </button>
                <button type="button" className="secondary" disabled={action.busy} onClick={() => action.run(() => expeditionsApi.returnToDraft(plan.id))}>
                  Volver a borrador
                </button>
              </>
            )}
          </div>
        )}
        <ErrorNotice error={action.error} />
      </Panel>
      <Panel
        title="Validación"
        description="Superposiciones, recursos faltantes, certificaciones, capacidad, permisos, stock y conflictos entre ramas paralelas."
        flush
        actions={
          <button type="button" className="secondary" onClick={validation.reload}>
            Revalidar
          </button>
        }
      >
        <ErrorNotice error={validation.error} />
        {validation.data && issues.length === 0 && <Empty>Sin observaciones: el plan es válido.</Empty>}
        {issues.length > 0 && (
          <ul className="list">
            {issues.map((issue) => (
              <li key={`${issue.code}-${issue.message}`}>
                <div className="grow issue">
                  <span>
                    <span className={`pill pill-${issue.severity.toLowerCase()}`}>{SEVERITY_LABELS[issue.severity]}</span>
                  </span>
                  <span>
                    <span className="code">{issue.code}</span>
                    <br />
                    {issue.message}
                  </span>
                  {issue.severity === 'WARNING' && (
                    <span className="resolution">
                      {isAccepted(issue) ? (
                        <span className="pill pill-accepted">Aceptada</span>
                      ) : (
                        plan.status === 'IN_REVIEW' && <AcceptWarning plan={plan} issue={issue} catalog={catalog} onDone={accepted} />
                      )}
                    </span>
                  )}
                </div>
              </li>
            ))}
          </ul>
        )}
      </Panel>
      {plan.acceptedWarnings.length > 0 && (
        <Panel title="Advertencias aceptadas" flush>
          <ul className="list">
            {plan.acceptedWarnings.map((warning) => (
              <li key={`${warning.code}-${warning.message}`}>
                <div className="grow">
                  <strong>{warning.message}</strong>
                  <p className="muted">
                    «{warning.justification}», aceptada por {personName(catalog, warning.acceptedBy)}.
                  </p>
                </div>
              </li>
            ))}
          </ul>
        </Panel>
      )}
    </>
  );
}

interface AcceptWarningProps {
  plan: Expedition;
  issue: Issue;
  catalog: Catalog;
  onDone: () => void;
}

function AcceptWarning({ plan, issue, catalog, onDone }: AcceptWarningProps) {
  const [justification, setJustification] = useState('');
  const [responsible, setResponsible] = useState(plan.charter.responsibles[0] ?? '');
  const action = useAction(onDone);

  return (
    <form className="form inline" onSubmit={action.onSubmit(() => expeditionsApi.acceptWarning(plan.id, issue, justification, responsible))}>
      <Field label="Justificación">
        <input required value={justification} onChange={(event) => setJustification(event.target.value)} />
      </Field>
      <Field label="Responsable">
        <select value={responsible} onChange={(event) => setResponsible(event.target.value)}>
          {plan.charter.responsibles.map((id) => (
            <option key={id} value={id}>
              {personName(catalog, id)}
            </option>
          ))}
        </select>
      </Field>
      <button className="secondary" disabled={action.busy}>
        Aceptar advertencia
      </button>
      <ErrorNotice error={action.error} />
    </form>
  );
}
