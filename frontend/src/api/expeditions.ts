import { collection, pageOf } from './collections';
import { http, isNotFound } from './http';
import type {
  ActivityRequest,
  Assignment,
  BlockItem,
  BlockRequest,
  CharterRequest,
  Created,
  Estimate,
  Expedition,
  ExpeditionSummary,
  Issue,
  IsoDuration,
  Proposal,
  Report,
  Run,
  Uuid,
  Validation,
} from './types';

const expedition = (id: Uuid) => `/v1/expeditions/${id}`;
const run = (id: Uuid) => `${expedition(id)}/run`;

export const expeditionsApi = {
  ...collection<ExpeditionSummary>('/v1/expeditions'),
  get: (id: Uuid) => http.get<Expedition>(expedition(id)),
  draft: (charter: CharterRequest) => http.post<Created>('/v1/expeditions', charter),

  addActivity: (id: Uuid, activity: ActivityRequest) => http.post<Created>(`${expedition(id)}/activities`, activity),
  addBlock: (id: Uuid, block: BlockRequest) => http.post<BlockItem>(`${expedition(id)}/blocks`, block),
  addDependency: (id: Uuid, activityId: Uuid, predecessorId: Uuid) =>
    http.put(`${expedition(id)}/activities/${activityId}/predecessors/${predecessorId}`),
  estimate: (id: Uuid) => http.get<Estimate>(`${expedition(id)}/estimate`),

  addAssignment: (id: Uuid, assignment: Assignment) => http.post(`${expedition(id)}/assignments`, assignment),
  attachPermit: (id: Uuid, permitId: Uuid) => http.put(`${expedition(id)}/permits/${permitId}`),
  suggestions: (id: Uuid) => http.get<Assignment[]>(`${expedition(id)}/assignment-suggestions`),

  validation: (id: Uuid) => http.get<Validation>(`${expedition(id)}/validation`),
  submit: (id: Uuid) => http.post(`${expedition(id)}/submission`),
  returnToDraft: (id: Uuid) => http.delete(`${expedition(id)}/submission`),
  acceptWarning: (id: Uuid, issue: Issue, justification: string, acceptedBy: Uuid) =>
    http.post(`${expedition(id)}/accepted-warnings`, { issue, justification, acceptedBy }),
  approve: (id: Uuid) => http.post(`${expedition(id)}/approval`),

  run: (id: Uuid) => http.get<Run>(run(id)),
  start: (id: Uuid) => http.post(run(id)),
  suspend: (id: Uuid) => http.post(`${run(id)}/suspension`),
  resume: (id: Uuid) => http.delete(`${run(id)}/suspension`),
  finish: (id: Uuid) => http.post(`${run(id)}/completion`),
  startActivity: (id: Uuid, activityId: Uuid) => http.post(`${run(id)}/activities`, { activityId }),
  finishActivity: (id: Uuid, activityId: Uuid, result: string) =>
    http.post(`${run(id)}/activities/${activityId}/completion`, { result }),
  observe: (id: Uuid, text: string) => http.post(`${run(id)}/observations`, { text }),
  recordIncident: (id: Uuid, description: string, activityId?: Uuid) =>
    http.post(`${expedition(id)}/incidents`, { description, activityId }),

  revise: (id: Uuid) => http.post<Created>(`${expedition(id)}/revisions`),
  cancelActivity: (id: Uuid, activityId: Uuid) => http.delete(`${expedition(id)}/activities/${activityId}`),
  delayActivity: (id: Uuid, activityId: Uuid, delay: IsoDuration) =>
    http.post(`${expedition(id)}/activities/${activityId}/delay`, { delay }),
  replaceUnavailable: (id: Uuid) => http.post(`${expedition(id)}/reassignments`),

  proposals: (id: Uuid, page: number) => pageOf<Proposal>(`${expedition(id)}/replan-proposals`, page),
  acceptProposal: (proposalId: Uuid, responsible: Uuid) =>
    http.post(`/v1/replan-proposals/${proposalId}/acceptance`, { responsible }),
  rejectProposal: (proposalId: Uuid, responsible: Uuid) =>
    http.post(`/v1/replan-proposals/${proposalId}/rejection`, { responsible }),

  report: (id: Uuid) => http.get<Report>(`${expedition(id)}/report`),
};

export async function findRun(id: Uuid): Promise<Run | null> {
  try {
    return await expeditionsApi.run(id);
  } catch (error) {
    if (isNotFound(error)) {
      return null;
    }
    throw error;
  }
}
