import { Link, useParams, useSearchParams } from 'react-router';
import { expeditionsApi, findRun } from '../api/expeditions';
import type { Expedition } from '../api/types';
import { ErrorNotice } from '../components/ErrorNotice';
import { PageHeader } from '../components/PageHeader';
import { Tabs } from '../components/Tabs';
import { EMPTY_CATALOG, useCatalog, type Catalog } from '../hooks/useCatalog';
import { useLoad } from '../hooks/useLoad';
import { formatSpan } from '../time';
import { personName } from './describe';
import { Lifecycle } from './Lifecycle';
import { PlanTab } from './PlanTab';
import { ReplanTab } from './ReplanTab';
import { ReportTab } from './ReportTab';
import { ResourcesTab } from './ResourcesTab';
import { ReviewTab } from './ReviewTab';
import { RunTab } from './RunTab';

const TABS = [
  { id: 'plan', label: 'Itinerario', content: PlanTab },
  { id: 'resources', label: 'Recursos', content: ResourcesTab },
  { id: 'review', label: 'Revisión y aprobación', content: ReviewTab },
  { id: 'run', label: 'Ejecución', content: RunTab },
  { id: 'replan', label: 'Replanificación', content: ReplanTab },
  { id: 'report', label: 'Informes', content: ReportTab },
] as const;

type TabId = (typeof TABS)[number]['id'];

export function ExpeditionPage() {
  const { id = '' } = useParams();
  const plan = useLoad(() => expeditionsApi.get(id), [id]);
  const run = useLoad(() => findRun(id), [plan.data]);
  const catalog = useCatalog();
  const [params, setParams] = useSearchParams();
  const active = TABS.find((tab) => tab.id === params.get('tab')) ?? TABS[0];

  if (plan.error) {
    return <ErrorNotice error={plan.error} />;
  }
  if (!plan.data || plan.data.id !== id) {
    return <p className="muted">Cargando…</p>;
  }
  const Content = active.content;
  const lookups = catalog.data ?? EMPTY_CATALOG;
  return (
    <>
      <Header plan={plan.data} catalog={lookups} />
      <Lifecycle plan={plan.data} run={run.data ?? null} />
      <ErrorNotice error={catalog.error} />
      <Tabs<TabId> tabs={[...TABS]} active={active.id} onChange={(tab) => setParams({ tab })} />
      <Content key={`${active.id}-${id}`} plan={plan.data} catalog={lookups} onChange={plan.reload} />
    </>
  );
}

function Header({ plan, catalog }: { plan: Expedition; catalog: Catalog }) {
  const charter = plan.charter;
  const [title, ...objectives] = charter.objectives;

  return (
    <>
      <PageHeader
        breadcrumb={
          <>
            <Link to="/expeditions">Expediciones</Link>
            <span>/</span>
            <span>{plan.supersedes ? `Revisión v${plan.version}` : `Versión ${plan.version}`}</span>
          </>
        }
        title={title}
        description={objectives.length > 0 ? `También: ${objectives.join('; ')}.` : undefined}
      />
      <dl className="meta">
        <div>
          <dt>Período</dt>
          <dd>{formatSpan(charter.period)}</dd>
        </div>
        <div>
          <dt>Zonas</dt>
          <dd>{charter.zones.join(', ')}</dd>
        </div>
        <div>
          <dt>Responsables</dt>
          <dd>{charter.responsibles.map((id) => personName(catalog, id)).join(', ')}</dd>
        </div>
        {charter.restrictions.length > 0 && (
          <div>
            <dt>Restricciones</dt>
            <dd>{charter.restrictions.join('; ')}</dd>
          </div>
        )}
        {plan.supersedes && (
          <div>
            <dt>Revisa a</dt>
            <dd>
              <Link to={`/expeditions/${plan.supersedes}`}>Plan anterior</Link>
            </dd>
          </div>
        )}
      </dl>
    </>
  );
}
