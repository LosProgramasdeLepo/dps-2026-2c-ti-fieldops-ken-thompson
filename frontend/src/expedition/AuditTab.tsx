import { useState } from 'react';
import { Link } from 'react-router';
import { eventsApi } from '../api/events';
import type { Delivery, EventType, RecordedEvent } from '../api/types';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Pager } from '../components/Pager';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { useLoad } from '../hooks/useLoad';
import { usePolling } from '../hooks/usePolling';
import { consumerLabel, DELIVERY_LABELS, EVENT_LABELS } from '../labels';
import { formatInstant } from '../time';
import { eventSummary, type PlanTabProps } from './describe';

const EVENT_TYPES = Object.keys(EVENT_LABELS) as EventType[];

function inProgress(delivery: Delivery): boolean {
  return delivery.status === 'PENDING' || delivery.status === 'RETRYING';
}

export function AuditTab({ plan, catalog }: PlanTabProps) {
  const [type, setType] = useState<EventType | ''>('');
  const [page, setPage] = useState(0);
  const events = useLoad(() => eventsApi.ofExpedition(plan.id, page, type ? [type] : []), [plan.id, page, type]);
  const processing = events.data?.items.some((event) => event.deliveries.some(inProgress)) ?? false;
  usePolling(events.reload, processing);

  function filterBy(next: EventType | '') {
    setType(next);
    setPage(0);
  }

  return (
    <Panel
      title="Eventos y auditoría"
      description="Los hechos de este plan, sus revisiones y su corrida, en el orden en que se registraron, con el estado de cada consumidor que los procesa fuera del pedido."
      actions={
        <Field label="Tipo">
          <select value={type} onChange={(event) => filterBy(event.target.value as EventType | '')}>
            <option value="">Todos</option>
            {EVENT_TYPES.map((value) => (
              <option key={value} value={value}>
                {EVENT_LABELS[value]}
              </option>
            ))}
          </select>
        </Field>
      }
      flush
    >
      <ErrorNotice error={events.error} />
      {events.data && events.data.totalItems === 0 && <Empty>Sin eventos todavía. Aparecen al aprobar, ejecutar y replanificar.</Empty>}
      {events.data && events.data.totalItems > 0 && (
        <>
          <ul className="list">
            {events.data.items.map((event) => (
              <EventItem key={event.id} event={event} summary={eventSummary(event, plan, catalog)} ownPlan={event.expeditionId === plan.id} onRetried={events.reload} />
            ))}
          </ul>
          <Pager page={events.data} onChange={setPage} />
        </>
      )}
    </Panel>
  );
}

interface EventItemProps {
  event: RecordedEvent;
  summary: string;
  ownPlan: boolean;
  onRetried: () => void;
}

function EventItem({ event, summary, ownPlan, onRetried }: EventItemProps) {
  return (
    <li>
      <span className="when">{formatInstant(event.occurredAt)}</span>
      <div className="grow stack-tight">
        <div className="row">
          <strong>{EVENT_LABELS[event.type]}</strong>
          {summary && <span>{summary}</span>}
          {!ownPlan && (
            <Link className="small" to={`/expeditions/${event.expeditionId}?tab=audit`}>
              otra versión
            </Link>
          )}
        </div>
        {event.deliveries.length > 0 && (
          <ul className="deliveries">
            {event.deliveries.map((delivery) => (
              <DeliveryItem key={delivery.consumer} eventId={event.id} delivery={delivery} onRetried={onRetried} />
            ))}
          </ul>
        )}
      </div>
    </li>
  );
}

function DeliveryItem({ eventId, delivery, onRetried }: { eventId: string; delivery: Delivery; onRetried: () => void }) {
  const action = useAction(onRetried);

  return (
    <li>
      <span className={`pill pill-${delivery.status.toLowerCase()}`}>{DELIVERY_LABELS[delivery.status]}</span>
      <span>{consumerLabel(delivery.consumer)}</span>
      <span className="muted small">{delivery.attempts === 1 ? '1 intento' : `${delivery.attempts} intentos`}</span>
      {delivery.status === 'FAILED' && (
        <button type="button" className="secondary small" disabled={action.busy} onClick={() => action.run(() => eventsApi.retry(eventId, delivery.consumer))}>
          Reintentar
        </button>
      )}
      {delivery.lastError && delivery.status !== 'DELIVERED' && <span className="muted small error-text">{delivery.lastError}</span>}
      <ErrorNotice error={action.error} />
    </li>
  );
}
