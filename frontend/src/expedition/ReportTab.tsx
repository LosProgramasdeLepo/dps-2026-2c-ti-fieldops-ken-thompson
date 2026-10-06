import { expeditionsApi } from '../api/expeditions';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Panel } from '../components/Panel';
import { StatusBadge } from '../components/StatusBadge';
import { useLoad } from '../hooks/useLoad';
import { RISK_LABELS } from '../labels';
import { formatDuration, formatInstant } from '../time';
import { activityName, consumptionOf, type PlanTabProps } from './describe';

export function ReportTab({ plan, catalog }: PlanTabProps) {
  const report = useLoad(() => expeditionsApi.report(plan.id), [plan]);

  if (report.error) {
    return <ErrorNotice error={report.error} />;
  }
  if (!report.data) {
    return <p className="muted">Cargando…</p>;
  }
  const summary = report.data;
  return (
    <>
      <dl className="stats">
        <div>
          <dt>Estado</dt>
          <dd>
            <StatusBadge status={summary.status} />
          </dd>
        </div>
        <div>
          <dt>Actividades finalizadas</dt>
          <dd>
            {summary.finishedActivities} de {summary.plannedActivities}
          </dd>
        </div>
        <div>
          <dt>{summary.startedActivities > 0 ? 'Duración real' : 'Duración estimada'}</dt>
          <dd>{formatDuration(summary.duration)}</dd>
        </div>
        <div>
          <dt>Riesgo</dt>
          <dd>{RISK_LABELS[summary.risk]}</dd>
        </div>
      </dl>
      <Panel title="Consumo de recursos" flush>
        <table>
          <thead>
            <tr>
              <th>Estimado por el plan</th>
              <th>{summary.startedActivities > 0 ? 'Consumido en lo finalizado' : 'Asignado'}</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{consumptionOf(summary.estimatedConsumption, catalog)}</td>
              <td>{consumptionOf(summary.consumption, catalog)}</td>
            </tr>
          </tbody>
        </table>
      </Panel>
      <Panel title="Incidentes" flush>
        {summary.incidents.length === 0 ? (
          <Empty>Sin incidentes.</Empty>
        ) : (
          <ul className="list">
            {summary.incidents.map((incident) => (
              <li key={`${incident.at}-${incident.description}`}>
                <span className="when">{formatInstant(incident.at)}</span>
                <span className="grow">
                  {incident.description}
                  {incident.activityId && <span className="muted"> en {activityName(plan, incident.activityId)}</span>}
                </span>
              </li>
            ))}
          </ul>
        )}
      </Panel>
      <Panel title="Resultados y observaciones" flush>
        {summary.activityResults.length === 0 && summary.observations.length === 0 ? (
          <Empty>Todavía no hay resultados ni observaciones.</Empty>
        ) : (
          <ul className="list">
            {summary.activityResults.map((result) => (
              <li key={result.activityId}>
                <span className="when">{activityName(plan, result.activityId)}</span>
                <span className="grow">{result.result}</span>
              </li>
            ))}
            {summary.observations.map((observation) => (
              <li key={`${observation.at}-${observation.text}`}>
                <span className="when">{formatInstant(observation.at)}</span>
                <span className="grow">{observation.text}</span>
              </li>
            ))}
          </ul>
        )}
      </Panel>
    </>
  );
}
