import { Link } from 'react-router';
import type { Expedition, Run } from '../api/types';
import { Icon } from '../components/Icon';

const STAGES = ['Borrador', 'En revisión', 'Aprobada', 'En ejecución', 'Finalizada'];
const RUNNING = 3;

function stageOf(plan: Expedition, run: Run | null): number {
  if (run) {
    return run.status === 'FINISHED' ? STAGES.length - 1 : RUNNING;
  }
  return { DRAFT: 0, IN_REVIEW: 1, APPROVED: 2, SUPERSEDED: 2 }[plan.status];
}

export function Lifecycle({ plan, run }: { plan: Expedition; run: Run | null }) {
  const current = stageOf(plan, run);
  const finished = run?.status === 'FINISHED';

  return (
    <>
      <ol className="lifecycle" aria-label="Etapa del plan">
        {STAGES.map((label, index) => {
          const done = index < current || (finished && index === current);
          const live = index === RUNNING && run?.status === 'IN_PROGRESS';
          const state = done ? 'done' : index === current ? `current${live ? ' live' : ''}` : '';
          return (
            <li key={label} className={state} aria-current={index === current ? 'step' : undefined}>
              <span className="marker">{done && <Icon name="check" size={12} />}</span>
              {index === RUNNING && run?.status === 'SUSPENDED' ? 'Suspendida' : label}
            </li>
          );
        })}
      </ol>
      {plan.status === 'SUPERSEDED' && (
        <p className="notice lifecycle-note">
          Una revisión aprobada reemplazó a este plan. Queda como historial{run ? ' y la corrida se sigue registrando acá' : ''}.
          {run && run.inForceId !== plan.id && (
            <>
              {' '}
              <Link to={`/expeditions/${run.inForceId}`}>Ver el plan vigente</Link>.
            </>
          )}
        </p>
      )}
      {plan.supersedes && plan.status === 'APPROVED' && (
        <p className="notice lifecycle-note">
          Esta revisión rige la expedición. La corrida se registra en <Link to={`/expeditions/${plan.supersedes}?tab=run`}>el plan anterior</Link>.
        </p>
      )}
    </>
  );
}
