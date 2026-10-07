import { useState } from 'react';
import { vehiclesApi } from '../api/catalog';
import type { Availability, Vehicle } from '../api/types';
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
import { passengers } from '../expedition/describe';
import { defaultPeriod } from '../time';

export function VehiclesTab() {
  const vehicles = usePaged(vehiclesApi.page);
  const [registering, setRegistering] = useState(false);

  return (
    <>
      <PageHeader
        title="Vehículos"
        description="Botes, camionetas y otros medios de traslado. La capacidad limita cuántas personas pueden viajar en cada actividad."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar vehículo
          </button>
        }
      />
      <Panel title="Registrados" flush>
        <ErrorNotice error={vehicles.error} />
        {vehicles.data && vehicles.data.totalItems === 0 && <Empty>Todavía no hay vehículos registrados.</Empty>}
        {vehicles.data && vehicles.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Vehículo</th>
                  <th>Capacidad</th>
                  <th>Disponibilidad</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {vehicles.data.items.map((vehicle) => (
                  <VehicleRow key={vehicle.id} vehicle={vehicle} onDone={vehicles.reload} />
                ))}
              </tbody>
            </table>
            <Pager page={vehicles.data} onChange={vehicles.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar vehículo" open={registering} onClose={() => setRegistering(false)}>
        <RegisterVehicle
          onDone={() => {
            setRegistering(false);
            vehicles.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterVehicle({ onDone }: { onDone: () => void }) {
  const [capacity, setCapacity] = useState(4);
  const [availability, setAvailability] = useState<Availability>({ periods: [defaultPeriod(30)] });
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => vehiclesApi.register(capacity, availability))}>
      <Field label="Capacidad (pasajeros)">
        <input type="number" min={0} required value={capacity} onChange={(event) => setCapacity(Number(event.target.value))} />
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

function VehicleRow({ vehicle, onDone }: { vehicle: Vehicle; onDone: () => void }) {
  const [editing, setEditing] = useState(false);

  return (
    <>
      <tr>
        <td>
          <strong>Vehículo {shortId(vehicle.id)}</strong>
        </td>
        <td>{passengers(vehicle.capacity)}</td>
        <td>
          <AvailabilityText availability={vehicle.availability} />
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
              initial={vehicle.availability}
              save={(availability) => vehiclesApi.changeAvailability(vehicle.id, availability)}
              onDone={onDone}
            />
          </td>
        </tr>
      )}
    </>
  );
}
