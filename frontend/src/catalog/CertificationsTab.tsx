import { useState } from 'react';
import { certificationsApi } from '../api/catalog';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Icon } from '../components/Icon';
import { PageHeader } from '../components/PageHeader';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { usePaged } from '../hooks/usePaged';

export function CertificationsTab() {
  const certifications = usePaged(certificationsApi.page);
  const [registering, setRegistering] = useState(false);

  return (
    <>
      <PageHeader
        title="Certificaciones"
        description="Habilitaciones que exigen las actividades: muestreo, buceo, operación nocturna y otras."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar certificación
          </button>
        }
      />
      <Panel title="Registradas" flush>
        <ErrorNotice error={certifications.error} />
        {certifications.data && certifications.data.totalItems === 0 && <Empty>Todavía no hay certificaciones registradas.</Empty>}
        {certifications.data && certifications.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Identificador</th>
                </tr>
              </thead>
              <tbody>
                {certifications.data.items.map((certification) => (
                  <tr key={certification.id}>
                    <td>
                      <strong>{certification.name}</strong>
                    </td>
                    <td className="muted small">{certification.id}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pager page={certifications.data} onChange={certifications.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar certificación" open={registering} onClose={() => setRegistering(false)}>
        <RegisterCertification
          onDone={() => {
            setRegistering(false);
            certifications.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterCertification({ onDone }: { onDone: () => void }) {
  const [name, setName] = useState('');
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => certificationsApi.register(name))}>
      <Field label="Nombre">
        <input required value={name} onChange={(event) => setName(event.target.value)} />
      </Field>
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Registrar</button>
      </div>
    </form>
  );
}
