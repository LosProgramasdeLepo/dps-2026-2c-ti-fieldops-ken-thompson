import { http } from './http';
import { PAGE_SIZE } from './collections';
import type { EventType, Page, RecordedEvent, Uuid } from './types';

function query(page: number, size: number, types: EventType[]): string {
  const params = new URLSearchParams({ page: String(page), size: String(size) });
  types.forEach((type) => params.append('type', type));
  return params.toString();
}

export const eventsApi = {
  ofExpedition: (id: Uuid, page: number, types: EventType[] = [], size = PAGE_SIZE) =>
    http.get<Page<RecordedEvent>>(`/v1/expeditions/${id}/events?${query(page, size, types)}`),
  get: (eventId: Uuid) => http.get<RecordedEvent>(`/v1/events/${eventId}`),
  retry: (eventId: Uuid, consumer: string) =>
    http.post(`/v1/events/${eventId}/deliveries/${encodeURIComponent(consumer)}/retry`),
};
