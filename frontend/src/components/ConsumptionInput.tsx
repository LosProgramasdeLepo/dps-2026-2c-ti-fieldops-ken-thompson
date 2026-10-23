import type { Consumption, Uuid } from '../api/types';
import type { Catalog } from '../hooks/useCatalog';

export interface ConsumptionRow {
  consumableId: Uuid;
  amount: number;
}

export function toConsumption(rows: ConsumptionRow[]): Consumption {
  return Object.fromEntries(rows.map((row) => [row.consumableId, row.amount]));
}

interface ConsumptionInputProps {
  rows: ConsumptionRow[];
  onChange: (rows: ConsumptionRow[]) => void;
  catalog: Catalog;
}

export function ConsumptionInput({ rows, onChange, catalog }: ConsumptionInputProps) {
  const replace = (index: number, row: ConsumptionRow) => onChange(rows.map((current, position) => (position === index ? row : current)));
  const first = catalog.consumables[0];

  return (
    <div className="stack">
      {rows.map((row, index) => (
        <span key={index} className="row">
          <select aria-label="Consumible" value={row.consumableId} onChange={(event) => replace(index, { ...row, consumableId: event.target.value })}>
            {catalog.consumables.map((consumable) => (
              <option key={consumable.id} value={consumable.id}>
                {consumable.name}
              </option>
            ))}
          </select>
          <input type="number" aria-label="Cantidad" min={1} required value={row.amount} onChange={(event) => replace(index, { ...row, amount: Number(event.target.value) })} />
          <button type="button" className="link" onClick={() => onChange(rows.filter((_, position) => position !== index))}>
            Quitar
          </button>
        </span>
      ))}
      {first && (
        <button type="button" className="link" onClick={() => onChange([...rows, { consumableId: first.id, amount: 1 }])}>
          Agregar consumible
        </button>
      )}
    </div>
  );
}
