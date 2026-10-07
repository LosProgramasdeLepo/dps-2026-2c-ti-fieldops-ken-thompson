export type Uuid = string;
export type Instant = string;
export type IsoDuration = string;
export type Consumption = Record<Uuid, number>;

export interface Period {
  start: Instant;
  end: Instant;
}

export interface Availability {
  periods: Period[];
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Created {
  id: Uuid;
}

export interface Certification {
  id: Uuid;
  name: string;
}

export interface Person {
  id: Uuid;
  name: string;
  certifications: Certification[];
  availability: Availability;
}

export interface Vehicle {
  id: Uuid;
  capacity: number;
  availability: Availability;
}

export interface Instrument {
  id: Uuid;
  kind: string;
  availability: Availability;
}

export interface Consumable {
  id: Uuid;
  name: string;
  stock: number;
}

export interface Permit {
  id: Uuid;
  kind: string;
  zone: string;
  validity: Period;
}

export type PlanStatus = 'DRAFT' | 'IN_REVIEW' | 'APPROVED' | 'SUPERSEDED';
export type RunStatus = 'IN_PROGRESS' | 'SUSPENDED' | 'FINISHED';
export type OperationalStatus = PlanStatus | RunStatus;
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH';
export type Arrangement = 'SEQUENTIAL' | 'PARALLEL';
export type Severity = 'CRITICAL' | 'WARNING';
export type Decision = 'PENDING' | 'ACCEPTED' | 'REJECTED';
export type ActivityKind = 'SAMPLING' | 'MEASUREMENT' | 'TRANSIT' | 'NIGHT' | 'DIVE' | 'CAMP';

export interface Charter {
  objectives: string[];
  period: Period;
  zones: string[];
  responsibles: Uuid[];
  restrictions: string[];
}

export interface Requirements {
  certifications: Uuid[];
  heldByEveryone: Uuid[];
  instruments: string[];
  specialPermits: string[];
  vehicles: number;
}

export interface ActivityItem {
  node: 'ACTIVITY';
  id: Uuid;
  name: string;
  estimatedDuration: IsoDuration;
  risk: RiskLevel;
  consumption: Consumption;
  zone: string;
  window: Period;
  predecessors: Uuid[];
  requirements: Requirements;
}

export interface BlockItem {
  node: 'BLOCK';
  arrangement: Arrangement;
  parts: ItineraryItem[];
}

export type ItineraryItem = ActivityItem | BlockItem;

export type Assignment =
  | { type: 'PERSON'; activityId: Uuid; personId: Uuid }
  | { type: 'VEHICLE'; activityId: Uuid; vehicleId: Uuid }
  | { type: 'INSTRUMENT'; activityId: Uuid; instrumentId: Uuid }
  | { type: 'CONSUMABLE'; activityId: Uuid; consumableId: Uuid; quantity: number };

export interface Issue {
  severity: Severity;
  code: string;
  message: string;
}

export interface AcceptedWarning extends Issue {
  justification: string;
  acceptedBy: Uuid;
}

export interface ExpeditionSummary {
  id: Uuid;
  version: number;
  supersedes?: Uuid;
  status: PlanStatus;
  charter: Charter;
}

export interface Expedition extends ExpeditionSummary {
  itinerary: ItineraryItem[];
  assignments: Assignment[];
  permits: Uuid[];
  acceptedWarnings: AcceptedWarning[];
}

export interface Validation {
  expeditionId: Uuid;
  version: number;
  issues: Issue[];
}

export interface Estimate {
  duration: IsoDuration;
  risk: RiskLevel;
  consumption: Consumption;
}

export interface Incident {
  description: string;
  at: Instant;
  activityId?: Uuid;
}

export interface Observation {
  text: string;
  at: Instant;
}

export interface ActivityExecution {
  activityId: Uuid;
  startedAt: Instant;
  finishedAt?: Instant;
  result?: string;
}

export interface Run {
  expeditionId: Uuid;
  inForceId: Uuid;
  status: RunStatus;
  activities: ActivityExecution[];
  incidents: Incident[];
  observations: Observation[];
}

export interface Report {
  status: OperationalStatus;
  plannedActivities: number;
  startedActivities: number;
  finishedActivities: number;
  duration: IsoDuration;
  risk: RiskLevel;
  consumption: Consumption;
  estimatedConsumption: Consumption;
  incidents: Incident[];
  observations: Observation[];
  activityResults: { activityId: Uuid; result: string }[];
}

export interface Proposal {
  id: Uuid;
  originalId: Uuid;
  incident: Incident;
  decision: Decision;
  decidedBy?: Uuid;
  decidedAt?: Instant;
  suggested: Expedition;
}

export interface CharterRequest {
  objectives: string[];
  period: Period;
  zones: string[];
  responsibles: Uuid[];
  restrictions: string[];
}

export interface ActivityRequest {
  kind: ActivityKind;
  certification?: Uuid;
  instrument?: string;
  name: string;
  estimatedDuration: IsoDuration;
  risk: RiskLevel;
  consumption: Consumption;
  zone: string;
  window: Period;
  predecessors: Uuid[];
}

export type ItineraryNodeRequest =
  | { node: 'ACTIVITY'; activity: ActivityRequest }
  | { node: 'BLOCK'; arrangement: Arrangement; parts: ItineraryNodeRequest[] };

export interface BlockRequest {
  arrangement: Arrangement;
  parts: ItineraryNodeRequest[];
}
