import { useState } from 'react';
import { certificationsApi, peopleApi } from '../api/catalog';
import type { Availability, Person } from '../api/types';
import { AvailabilityEditor } from '../components/AvailabilityEditor';
import { AvailabilityInput } from '../components/AvailabilityInput';
import { AvailabilityText } from '../components/AvailabilityText';
import { CheckList, type Option } from '../components/CheckList';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field, FieldGroup } from '../components/Field';
import { Icon } from '../components/Icon';
import { PageHeader } from '../components/PageHeader';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { useLoad } from '../hooks/useLoad';
import { usePaged } from '../hooks/usePaged';
import { defaultPeriod } from '../time';

export function PeopleTab() {
  const people = usePaged(peopleApi.page);
  const certifications = useLoad(() => certificationsApi.all(), []);
  const [registering, setRegistering] = useState(false);
  const options = (certifications.data ?? []).map((certification) => ({ value: certification.id, label: certification.name }));

  return (
    <>
      <PageHeader
        title="Personas"
        description="Participantes que se pueden asignar a las actividades, con sus certificaciones y su disponibilidad."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar persona
          </button>
        }
      />
      <Panel title="Registradas" flush>
        <ErrorNotice error={people.error} />
        {people.data && people.data.totalItems === 0 && <Empty>Todavía no hay personas. Registrá la primera para poder asignarla.</Empty>}
        {people.data && people.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Certificaciones</th>
                  <th>Disponibilidad</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {people.data.items.map((person) => (
                  <PersonRow key={person.id} person={person} certifications={options} onDone={people.reload} />
                ))}
              </tbody>
            </table>
            <Pager page={people.data} onChange={people.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar persona" open={registering} onClose={() => setRegistering(false)}>
        <RegisterPerson
          certifications={options}
          onDone={() => {
            setRegistering(false);
            people.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterPerson({ certifications, onDone }: { certifications: Option[]; onDone: () => void }) {
  const [name, setName] = useState('');
  const [held, setHeld] = useState<string[]>([]);
  const [availability, setAvailability] = useState<Availability>({ periods: [defaultPeriod(30)] });
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => peopleApi.register(name, held, availability))}>
      <Field label="Nombre">
        <input required value={name} onChange={(event) => setName(event.target.value)} />
      </Field>
      <FieldGroup label="Certificaciones">
        <CheckList options={certifications} selected={held} onChange={setHeld} />
      </FieldGroup>
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

interface PersonRowProps {
  person: Person;
  certifications: Option[];
  onDone: () => void;
}

function PersonRow({ person, certifications, onDone }: PersonRowProps) {
  const [editing, setEditing] = useState(false);

  return (
    <>
      <tr>
        <td>
          <strong>{person.name}</strong>
        </td>
        <td>
          {person.certifications.length === 0 ? (
            <span className="muted">Sin certificaciones</span>
          ) : (
            <span className="chips">
              {person.certifications.map((certification) => (
                <span key={certification.id} className="chip">
                  {certification.name}
                </span>
              ))}
            </span>
          )}
        </td>
        <td>
          <AvailabilityText availability={person.availability} />
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
            <div className="stack">
              <Certify person={person} certifications={certifications} onDone={onDone} />
              <AvailabilityEditor
                initial={person.availability}
                save={(availability) => peopleApi.changeAvailability(person.id, availability)}
                onDone={onDone}
              />
            </div>
          </td>
        </tr>
      )}
    </>
  );
}

function Certify({ person, certifications, onDone }: PersonRowProps) {
  const [certification, setCertification] = useState('');
  const action = useAction(onDone);
  const missing = certifications.filter((option) => !person.certifications.some((held) => held.id === option.value));

  if (missing.length === 0) {
    return null;
  }
  return (
    <form className="form inline" onSubmit={action.onSubmit(() => peopleApi.certify(person.id, certification))}>
      <Field label="Agregar certificación">
        <select required value={certification} onChange={(event) => setCertification(event.target.value)}>
          <option value="">Elegir…</option>
          {missing.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </Field>
      <button disabled={action.busy}>Certificar</button>
      <ErrorNotice error={action.error} />
    </form>
  );
}
