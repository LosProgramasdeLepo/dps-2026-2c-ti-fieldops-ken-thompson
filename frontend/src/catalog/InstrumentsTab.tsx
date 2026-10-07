import { useState } from 'react';
import { instrumentsApi } from '../api/catalog';
import type { Availability, Instrument } from '../api/types';
import { AvailabilityEditor } from '../components/AvailabilityEditor';
import { AvailabilityInput } from '../components/AvailabilityInput';
import { AvailabilityText } from '../components/AvailabilityText';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field, FieldGroup } from '../components/Field';
import { Icon } from '../components/Icon';
import { PageHeader } from '../components/PageHeader';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { shortId } from '../hooks/useCatalog';
import { usePaged } from '../hooks/usePaged';
import { INSTRUMENT_KINDS } from '../labels';
import { defaultPeriod } from '../time';

export function InstrumentsTab() {
  const instruments = usePaged(instrumentsApi.page);
  const [registering, setRegistering] = useState(false);

  return (
    <>
      <PageHeader
        title="Instrumentos"
        description="Equipamiento por tipo. Las actividades nocturnas piden iluminación (lighting), el buceo diving gear y los campamentos camp gear."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar instrumento
          </button>
        }
      />
      <Panel title="Registrados" flush>
        <ErrorNotice error={instruments.error} />
        {instruments.data && instruments.data.totalItems === 0 && <Empty>Todavía no hay instrumentos registrados.</Empty>}
        {instruments.data && instruments.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Tipo</th>
                  <th>Identificador</th>
                  <th>Disponibilidad</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {instruments.data.items.map((instrument) => (
                  <InstrumentRow key={instrument.id} instrument={instrument} onDone={instruments.reload} />
                ))}
              </tbody>
            </table>
            <Pager page={instruments.data} onChange={instruments.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar instrumento" open={registering} onClose={() => setRegistering(false)}>
        <RegisterInstrument
          onDone={() => {
            setRegistering(false);
            instruments.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterInstrument({ onDone }: { onDone: () => void }) {
  const [kind, setKind] = useState('');
  const [availability, setAvailability] = useState<Availability>({ periods: [defaultPeriod(30)] });
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => instrumentsApi.register(kind, availability))}>
      <Field label="Tipo">
        <input required list="instrument-kinds" value={kind} onChange={(event) => setKind(event.target.value)} />
        <datalist id="instrument-kinds">
          {INSTRUMENT_KINDS.map((known) => (
            <option key={known} value={known} />
          ))}
        </datalist>
      </Field>
      <FieldGroup label="Disponibilidad">
        <AvailabilityInput value={availability} onChange={setAvailability} />
      </FieldGroup>
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Registrar</button>
      </div>
    </form>
  );
}

function InstrumentRow({ instrument, onDone }: { instrument: Instrument; onDone: () => void }) {
  const [editing, setEditing] = useState(false);

  return (
    <>
      <tr>
        <td>
          <strong>{instrument.kind}</strong>
        </td>
        <td className="muted small">{shortId(instrument.id)}</td>
        <td>
          <AvailabilityText availability={instrument.availability} />
        </td>
        <td className="end">
          <button type="button" className="secondary small" onClick={() => setEditing(!editing)}>
            {editing ? 'Cerrar' : 'Editar'}
          </button>
        </td>
      </tr>
      {editing && (
        <tr className="editor">
          <td colSpan={4}>
            <AvailabilityEditor
              initial={instrument.availability}
              save={(availability) => instrumentsApi.changeAvailability(instrument.id, availability)}
              onDone={onDone}
            />
          </td>
        </tr>
      )}
    </>
  );
}
