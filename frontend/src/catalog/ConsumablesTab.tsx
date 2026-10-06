import { useState } from 'react';
import { consumablesApi } from '../api/catalog';
import type { Consumable } from '../api/types';
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

export function ConsumablesTab() {
  const consumables = usePaged(consumablesApi.page);
  const [registering, setRegistering] = useState(false);

  return (
    <>
      <PageHeader
        title="Consumibles"
        description="Insumos con stock: lo que se asigna entre todas las expediciones activas no puede superar lo que hay en depósito."
        actions={
          <button type="button" onClick={() => setRegistering(true)}>
            <Icon name="plus" />
            Registrar consumible
          </button>
        }
      />
      <Panel title="En depósito" flush>
        <ErrorNotice error={consumables.error} />
        {consumables.data && consumables.data.totalItems === 0 && <Empty>Todavía no hay consumibles registrados.</Empty>}
        {consumables.data && consumables.data.totalItems > 0 && (
          <>
            <table>
              <thead>
                <tr>
                  <th>Nombre</th>
                  <th>Stock</th>
                </tr>
              </thead>
              <tbody>
                {consumables.data.items.map((consumable) => (
                  <ConsumableRow key={consumable.id} consumable={consumable} onDone={consumables.reload} />
                ))}
              </tbody>
            </table>
            <Pager page={consumables.data} onChange={consumables.setPage} />
          </>
        )}
      </Panel>
      <Drawer title="Registrar consumible" open={registering} onClose={() => setRegistering(false)}>
        <RegisterConsumable
          onDone={() => {
            setRegistering(false);
            consumables.reload();
          }}
        />
      </Drawer>
    </>
  );
}

function RegisterConsumable({ onDone }: { onDone: () => void }) {
  const [name, setName] = useState('');
  const [stock, setStock] = useState(10);
  const action = useAction(onDone);

  return (
    <form className="form" onSubmit={action.onSubmit(() => consumablesApi.register(name, stock))}>
      <Field label="Nombre">
        <input required value={name} onChange={(event) => setName(event.target.value)} />
      </Field>
      <Field label="Stock">
        <input type="number" min={0} required value={stock} onChange={(event) => setStock(Number(event.target.value))} />
      </Field>
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Registrar</button>
      </div>
    </form>
  );
}

function ConsumableRow({ consumable, onDone }: { consumable: Consumable; onDone: () => void }) {
  const [stock, setStock] = useState(consumable.stock);
  const action = useAction(onDone);

  return (
    <tr>
      <td>
        <strong>{consumable.name}</strong>
      </td>
      <td>
        <form className="form inline" onSubmit={action.onSubmit(() => consumablesApi.changeStock(consumable.id, stock))}>
          <input
            type="number"
            aria-label={`Stock de ${consumable.name}`}
            min={0}
            required
            value={stock}
            onChange={(event) => setStock(Number(event.target.value))}
          />
          <button className="secondary small" disabled={action.busy || stock === consumable.stock}>
            Actualizar stock
          </button>
          <ErrorNotice error={action.error} />
        </form>
      </td>
    </tr>
  );
}
