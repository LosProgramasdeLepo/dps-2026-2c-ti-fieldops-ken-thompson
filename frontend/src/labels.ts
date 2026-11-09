import type { ActivityKind, Arrangement, Assignment, Decision, DeliveryStatus, EventType, OperationalStatus, RiskLevel, Severity } from './api/types';

export const STATUS_LABELS: Record<OperationalStatus, string> = {
  DRAFT: 'Borrador',
  IN_REVIEW: 'En revisión',
  APPROVED: 'Aprobada',
  SUPERSEDED: 'Reemplazada',
  IN_PROGRESS: 'En ejecución',
  SUSPENDED: 'Suspendida',
  FINISHED: 'Finalizada',
};

export const RISK_LABELS: Record<RiskLevel, string> = {
  LOW: 'Bajo',
  MEDIUM: 'Medio',
  HIGH: 'Alto',
};

export const ARRANGEMENT_LABELS: Record<Arrangement, string> = {
  SEQUENTIAL: 'Secuencial',
  PARALLEL: 'Paralelo',
};

export const SEVERITY_LABELS: Record<Severity, string> = {
  CRITICAL: 'Crítico',
  WARNING: 'Advertencia',
};

export const DECISION_LABELS: Record<Decision, string> = {
  PENDING: 'Pendiente',
  ACCEPTED: 'Aceptada',
  REJECTED: 'Rechazada',
};

export const EVENT_LABELS: Record<EventType, string> = {
  ExpeditionApproved: 'Plan aprobado',
  RunStarted: 'Corrida iniciada',
  ActivityStarted: 'Actividad iniciada',
  ActivityCompleted: 'Actividad finalizada',
  IncidentReported: 'Incidente registrado',
  RunFinished: 'Corrida finalizada',
  ReplanProposed: 'Replanificación propuesta',
  ReplanDecided: 'Propuesta decidida',
};

export const DELIVERY_LABELS: Record<DeliveryStatus, string> = {
  PENDING: 'Pendiente',
  RETRYING: 'Reintentando',
  DELIVERED: 'Entregado',
  FAILED: 'Fallido',
};

export const REPLAN_CONSUMER = 'replan-on-incident';

const CONSUMER_LABELS: Record<string, string> = {
  [REPLAN_CONSUMER]: 'Propuesta de replanificación',
  'notify-responsibles': 'Aviso a responsables',
};

export function consumerLabel(consumer: string): string {
  return CONSUMER_LABELS[consumer] ?? consumer;
}

export const ASSIGNMENT_LABELS: Record<Assignment['type'], string> = {
  PERSON: 'Persona',
  VEHICLE: 'Vehículo',
  INSTRUMENT: 'Instrumento',
  CONSUMABLE: 'Consumible',
};

export interface KindTraits {
  label: string;
  certification?: string;
  instrument?: boolean;
}

export const ACTIVITY_KINDS: Record<ActivityKind, KindTraits> = {
  SAMPLING: { label: 'Muestreo', certification: 'Certificación de muestreo' },
  MEASUREMENT: { label: 'Medición', certification: 'Certificación de medición', instrument: true },
  TRANSIT: { label: 'Traslado' },
  NIGHT: { label: 'Nocturna', certification: 'Certificación de operación nocturna' },
  DIVE: { label: 'Buceo', certification: 'Certificación de buceo' },
  CAMP: { label: 'Campamento' },
};

export const PERMIT_KINDS = ['zone', 'night operation'];
export const INSTRUMENT_KINDS = ['lighting', 'diving gear', 'camp gear'];
