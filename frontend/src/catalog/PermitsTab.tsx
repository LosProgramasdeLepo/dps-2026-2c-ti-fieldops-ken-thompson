import { useState } from 'react';
import { permitsApi } from '../api/catalog';
import type { Period } from '../api/types';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field, FieldGroup } from '../components/Field';
import { Icon } from '../components/Icon';
import { PageHeader } from '../components/PageHeader';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { PeriodInput } from '../components/PeriodInput';
import { useAction } from '../hooks/useAction';
import { usePaged } from '../hooks/usePaged';
import { PERMIT_KINDS } from '../labels';
import { defaultPeriod, formatSpan } from '../time';

export function PermitsTab() {
  const permits = usePaged(permitsApi.page);
  const [registering, setRegistering] = useState(false);

  return (
    <>
      <PageHeader
        title="Permisos"
        description="Autorizaciones por zona y vigencia. Las actividades nocturnas necesitan además un permiso de operación nocturna."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar permiso
          </button>
        }
      />
      <Panel title="Vigentes y vencidos" flush>
        <ErrorNotice error={permits.error} />
        {permits.data && permits.data.totalItems === 0 && <Empty>Todavía no hay permisos registrados.</Empty>}
        {permits.data && permits.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Tipo</th>
                  <th>Zona</th>
                  <th>Vigencia</th>
                </tr>
              </thead>
              <tbody>
                {permits.data.items.map((permit) => (
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
            <Pager page={permits.data} onChange={permits.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar permiso" open={registering} onClose={() => setRegistering(false)}>
        <RegisterPermit
          onDone={() => {
            setRegistering(false);
            permits.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterPermit({ onDone }: { onDone: () => void }) {
  const [kind, setKind] = useState(PERMIT_KINDS[0]);
  const [zone, setZone] = useState('');
  const [validity, setValidity] = useState<Period>(defaultPeriod(30));
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => permitsApi.register(kind, zone, validity))}>
      <Field label="Tipo">
        <input required list="permit-kinds" value={kind} onChange={(event) => setKind(event.target.value)} />
        <datalist id="permit-kinds">
          {PERMIT_KINDS.map((known) => (
            <option key={known} value={known} />
          ))}
        </datalist>
      </Field>
      <Field label="Zona">
        <input required value={zone} onChange={(event) => setZone(event.target.value)} />
      </Field>
      <FieldGroup label="Vigencia">
        <PeriodInput value={validity} onChange={setValidity} />
      </FieldGroup>
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Registrar</button>
      </div>
    </form>
  );
}
