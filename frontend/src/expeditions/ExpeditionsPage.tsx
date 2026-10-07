import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { expeditionsApi } from '../api/expeditions';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Icon } from '../components/Icon';
import { PageHeader } from '../components/PageHeader';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { StatusBadge } from '../components/StatusBadge';
import { usePaged } from '../hooks/usePaged';
import { formatSpan } from '../time';
import { CharterForm } from './CharterForm';

export function ExpeditionsPage() {
  const expeditions = usePaged(expeditionsApi.page);
  const [drafting, setDrafting] = useState(false);
  const navigate = useNavigate();

  return (
    <>
      <PageHeader
        title="Expediciones"
        description="Cada plan pasa por borrador, revisión y aprobación antes de salir a campo. Las revisiones de un plan aprobado aparecen como versiones nuevas."
        actions={
          <button type="button" onClick={() => setDrafting(true)}>
            <Icon name="plus" />
            Nueva expedición
          </button>
        }
      />
      <Panel title="Planes" flush>
        <ErrorNotice error={expeditions.error} />
        {expeditions.data && expeditions.data.totalItems === 0 && (
          <Empty>Todavía no hay expediciones. Creá la primera con su objetivo, período, zonas y responsables.</Empty>
        )}
        {expeditions.data && expeditions.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Objetivo</th>
                  <th>Período</th>
                  <th>Zonas</th>
                  <th>Versión</th>
                  <th>Estado</th>
                </tr>
              </thead>
              <tbody>
                {expeditions.data.items.map((expedition) => (
                  <tr key={expedition.id}>
                    <td>
                      <Link className="title-link" to={`/expeditions/${expedition.id}`}>
                        {expedition.charter.objectives[0]}
                      </Link>
                    </td>
                    <td>{formatSpan(expedition.charter.period)}</td>
                    <td>{expedition.charter.zones.join(', ')}</td>
                    <td>
                      {expedition.supersedes ? `Revisión v${expedition.version}` : `v${expedition.version}`}
                    </td>
                    <td>
                      <StatusBadge status={expedition.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pager page={expeditions.data} onChange={expeditions.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Nueva expedición" open={drafting} onClose={() => setDrafting(false)}>
        <CharterForm onCreated={(id) => navigate(`/expeditions/${id}`)} />
      </Drawer>
    </>
  );
}
