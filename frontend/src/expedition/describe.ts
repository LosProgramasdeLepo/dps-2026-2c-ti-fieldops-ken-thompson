import type { ActivityItem, Assignment, Consumption, Expedition, ItineraryItem, Uuid } from '../api/types';
import { nameOf, shortId, type Catalog } from '../hooks/useCatalog';
import { formatPeriod } from '../time';

export interface PlanTabProps {
  plan: Expedition;
  catalog: Catalog;
  onChange: () => void;
}

export function passengers(count: number): string {
  return count === 1 ? '1 pasajero' : `${count} pasajeros`;
}

export function activitiesOf(items: ItineraryItem[]): ActivityItem[] {
  return items.flatMap((item) => (item.node === 'ACTIVITY' ? [item] : activitiesOf(item.parts)));
}

export function activityName(plan: Expedition, id: Uuid): string {
  return nameOf(activitiesOf(plan.itinerary), id, (activity) => activity.name);
}

export function personName(catalog: Catalog, id: Uuid): string {
  return nameOf(catalog.people, id, (person) => person.name);
}

export function certificationName(catalog: Catalog, id: Uuid): string {
  return nameOf(catalog.certifications, id, (certification) => certification.name);
}

export function resourceOf(assignment: Assignment, catalog: Catalog): string {
  switch (assignment.type) {
    case 'PERSON':
      return personName(catalog, assignment.personId);
    case 'VEHICLE':
      return nameOf(catalog.vehicles, assignment.vehicleId, (vehicle) => `Vehículo ${shortId(vehicle.id)}, ${passengers(vehicle.capacity)}`);
    case 'INSTRUMENT':
      return nameOf(catalog.instruments, assignment.instrumentId, (instrument) => instrument.kind);
    case 'CONSUMABLE':
      return `${nameOf(catalog.consumables, assignment.consumableId, (consumable) => consumable.name)} × ${assignment.quantity}`;
  }
}

export function consumptionOf(consumption: Consumption, catalog: Catalog): string {
  const entries = Object.entries(consumption);
  if (entries.length === 0) {
    return '—';
  }
  return entries
    .map(([id, amount]) => `${nameOf(catalog.consumables, id, (consumable) => consumable.name)}: ${amount}`)
    .join(', ');
}

export function changesBetween(original: Expedition, suggested: Expedition): string[] {
  const next = new Map(activitiesOf(suggested.itinerary).map((activity) => [activity.id, activity]));
  const changes: string[] = [];
  for (const activity of activitiesOf(original.itinerary)) {
    const replanned = next.get(activity.id);
    if (!replanned) {
      changes.push(`Cancela «${activity.name}»`);
    } else if (formatPeriod(replanned.window) !== formatPeriod(activity.window)) {
      changes.push(`Reprograma «${activity.name}» a ${formatPeriod(replanned.window)}`);
    }
  }
  const key = (assignment: Assignment) => JSON.stringify(assignment);
  const before = new Set(original.assignments.map(key));
  const after = new Set(suggested.assignments.map(key));
  const added = suggested.assignments.filter((assignment) => !before.has(key(assignment))).length;
  const released = original.assignments.filter((assignment) => !after.has(key(assignment))).length;
  if (added > 0 || released > 0) {
    changes.push(`Reasigna recursos: libera ${released} y suma ${added}`);
  }
  return changes.length > 0 ? changes : ['Sin cambios sobre el plan vigente'];
}
