import { certificationsApi, consumablesApi, instrumentsApi, peopleApi, permitsApi, vehiclesApi } from '../api/catalog';
import type { Certification, Consumable, Instrument, Permit, Person, Uuid, Vehicle } from '../api/types';
import { useLoad } from './useLoad';

export interface Catalog {
  people: Person[];
  certifications: Certification[];
  vehicles: Vehicle[];
  instruments: Instrument[];
  consumables: Consumable[];
  permits: Permit[];
}

export const EMPTY_CATALOG: Catalog = {
  people: [],
  certifications: [],
  vehicles: [],
  instruments: [],
  consumables: [],
  permits: [],
};

export function useCatalog() {
  return useLoad(async (): Promise<Catalog> => {
    const [people, certifications, vehicles, instruments, consumables, permits] = await Promise.all([
      peopleApi.all(),
      certificationsApi.all(),
      vehiclesApi.all(),
      instrumentsApi.all(),
      consumablesApi.all(),
      permitsApi.all(),
    ]);
    return { people, certifications, vehicles, instruments, consumables, permits };
  }, []);
}

export function shortId(id: Uuid): string {
  return id.slice(0, 8);
}

export function nameOf<T extends { id: Uuid }>(items: T[], id: Uuid, label: (item: T) => string): string {
  const found = items.find((item) => item.id === id);
  return found ? label(found) : shortId(id);
}
