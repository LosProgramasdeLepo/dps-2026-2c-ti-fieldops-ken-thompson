import { pageOf } from './collections';
import type { Notification, Uuid } from './types';

export const notificationsApi = {
  ofPerson: (personId: Uuid, page: number) => pageOf<Notification>(`/v1/people/${personId}/notifications`, page),
};
