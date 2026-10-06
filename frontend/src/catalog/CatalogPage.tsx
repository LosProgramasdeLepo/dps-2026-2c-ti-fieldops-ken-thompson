import type { ComponentType } from 'react';
import { Navigate, useParams } from 'react-router';
import type { IconName } from '../components/Icon';
import { CertificationsTab } from './CertificationsTab';
import { ConsumablesTab } from './ConsumablesTab';
import { InstrumentsTab } from './InstrumentsTab';
import { PeopleTab } from './PeopleTab';
import { PermitsTab } from './PermitsTab';
import { VehiclesTab } from './VehiclesTab';

interface CatalogSection {
  id: string;
  label: string;
  icon: IconName;
  content: ComponentType;
}

export const CATALOG_SECTIONS: CatalogSection[] = [
  { id: 'people', label: 'Personas', icon: 'person', content: PeopleTab },
  { id: 'certifications', label: 'Certificaciones', icon: 'badge', content: CertificationsTab },
  { id: 'vehicles', label: 'Vehículos', icon: 'truck', content: VehiclesTab },
  { id: 'instruments', label: 'Instrumentos', icon: 'compass', content: InstrumentsTab },
  { id: 'consumables', label: 'Consumibles', icon: 'box', content: ConsumablesTab },
  { id: 'permits', label: 'Permisos', icon: 'permit', content: PermitsTab },
];

export function CatalogPage() {
  const { section } = useParams();
  const active = CATALOG_SECTIONS.find((candidate) => candidate.id === section);
  if (!active) {
    return <Navigate to="/catalog/people" replace />;
  }
  const Content = active.content;
  return <Content key={active.id} />;
}
