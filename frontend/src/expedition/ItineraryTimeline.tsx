import { Fragment, useState, type CSSProperties, type ReactNode } from 'react';
import type { ActivityItem, BlockItem, Expedition, Instant, ItineraryItem, Period } from '../api/types';
import type { Catalog } from '../hooks/useCatalog';
import { ARRANGEMENT_LABELS, RISK_LABELS } from '../labels';
import { formatDay, formatDuration, formatPeriod } from '../time';
import { activitiesOf, activityName, certificationName, consumptionOf } from './describe';

const DAY = 86_400_000;
const QUARTER = DAY / 4;
const MAX_LABELS = 12;
const LABEL_ROOM = 6;

interface Row {
  key: string;
  depth: number;
  item: ItineraryItem;
}

interface Tick {
  position: number;
  major: boolean;
  label?: string;
}

class Scale {
  private readonly start: number;
  private readonly length: number;

  constructor(start: number, end: number) {
    this.start = start;
    this.length = Math.max(end - start, 1);
  }

  static around(windows: Period[], fallback: Period): Scale {
    if (windows.length === 0) {
      return new Scale(Date.parse(fallback.start), Date.parse(fallback.end));
    }
    const start = Math.floor(Math.min(...windows.map((window) => Date.parse(window.start))) / DAY) * DAY;
    const end = Math.ceil(Math.max(...windows.map((window) => Date.parse(window.end))) / DAY) * DAY;
    return new Scale(start, Math.max(end, start + DAY));
  }

  position(instant: Instant | number): number {
    const at = typeof instant === 'number' ? instant : Date.parse(instant);
    return Math.min(Math.max(((at - this.start) / this.length) * 100, 0), 100);
  }

  span(window: Period): CSSProperties {
    const left = this.position(window.start);
    return { left: `${left}%`, width: `${this.position(window.end) - left}%` };
  }

  now(): number | undefined {
    const now = Date.now();
    return now >= this.start && now <= this.start + this.length ? this.position(now) : undefined;
  }

  ticks(): Tick[] {
    const days = this.length / DAY;
    const step = days <= 4 ? QUARTER : DAY;
    const every = Math.max(Math.ceil(days / MAX_LABELS), 1);
    const ticks: Tick[] = [];
    for (let at = Math.ceil(this.start / step) * step; at < this.start + this.length; at += step) {
      const major = at % DAY === 0;
      const day = Math.round((at - this.start) / DAY);
      ticks.push({
        position: this.position(at),
        major,
        label: major && day % every === 0 ? formatDay(new Date(at).toISOString()) : undefined,
      });
    }
    return ticks;
  }
}

function rowsOf(items: ItineraryItem[], depth = 0): Row[] {
  return items.flatMap((item) =>
    item.node === 'ACTIVITY'
      ? [{ key: item.id, depth, item }]
      : [{ key: `block-${depth}-${activitiesOf(item.parts)[0]?.id}`, depth, item }, ...rowsOf(item.parts, depth + 1)],
  );
}

function extentOf(block: BlockItem): Period {
  const windows = activitiesOf(block.parts).map((activity) => activity.window);
  return {
    start: windows.map((window) => window.start).sort()[0],
    end: windows.map((window) => window.end).sort().at(-1) ?? windows[0].end,
  };
}

interface ItineraryTimelineProps {
  plan: Expedition;
  catalog: Catalog;
}

export function ItineraryTimeline({ plan, catalog }: ItineraryTimelineProps) {
  const [open, setOpen] = useState<string>();
  const scale = Scale.around(activitiesOf(plan.itinerary).map((activity) => activity.window), plan.charter.period);
  const ticks = scale.ticks();
  const now = scale.now();
  const labelled = ticks.filter(
    (tick) => tick.label && tick.position < 100 - LABEL_ROOM && (now === undefined || Math.abs(tick.position - now) > LABEL_ROOM),
  );

  const track = (content: ReactNode) => (
    <div className="timeline-track">
      {ticks.map((tick) => (
        <span key={tick.position} className={tick.major ? 'tick' : 'tick minor'} style={{ left: `${tick.position}%` }} />
      ))}
      {now !== undefined && <span className="now" style={{ left: `${now}%` }} />}
      {content}
    </div>
  );
  const guides = (depth: number) => Array.from({ length: depth }, (_, index) => <span key={index} className="depth" />);

  return (
    <>
      <div className="timeline">
        <div className="timeline-corner">Actividad</div>
        <div className="timeline-axis">
          {labelled.map((tick) => (
            <span key={tick.position} style={{ left: `${tick.position}%` }}>
              {tick.label}
            </span>
          ))}
          {now !== undefined && (
            <span className={`now-label${now > 100 - LABEL_ROOM ? ' end' : now < LABEL_ROOM ? ' start' : ''}`} style={{ left: `${now}%` }}>
              Ahora
            </span>
          )}
        </div>
        {rowsOf(plan.itinerary).map(({ key, depth, item }) =>
          item.node === 'BLOCK' ? (
            <Fragment key={key}>
              <div className={`timeline-label group ${item.arrangement.toLowerCase()}`}>
                {guides(depth)}
                Bloque {ARRANGEMENT_LABELS[item.arrangement].toLowerCase()}
              </div>
              {track(<span className={`timeline-span ${item.arrangement.toLowerCase()}`} style={scale.span(extentOf(item))} />)}
            </Fragment>
          ) : (
            <Fragment key={key}>
              <div className="timeline-label">
                {guides(depth)}
                <button type="button" className="link" aria-expanded={open === item.id} onClick={() => setOpen(open === item.id ? undefined : item.id)}>
                  {item.name}
                  <span>{formatPeriod(item.window)}</span>
                </button>
              </div>
              {track(
                <span
                  className={`timeline-bar risk-${item.risk.toLowerCase()}`}
                  style={scale.span(item.window)}
                  title={`${item.name}: ${formatPeriod(item.window)}`}
                />,
              )}
              {open === item.id && <ActivityDetail plan={plan} activity={item} catalog={catalog} />}
            </Fragment>
          ),
        )}
      </div>
      <div className="legend">
        <span>
          <i /> Riesgo bajo
        </span>
        <span>
          <i className="risk-medium" /> Riesgo medio
        </span>
        <span>
          <i className="risk-high" /> Riesgo alto
        </span>
        {now !== undefined && (
          <span>
            <i className="now" /> Ahora
          </span>
        )}
        <span>Elegí una actividad para ver sus requisitos.</span>
      </div>
    </>
  );
}

function ActivityDetail({ plan, activity, catalog }: { plan: Expedition; activity: ActivityItem; catalog: Catalog }) {
  const requirements = activity.requirements;
  const needs = [
    ...requirements.certifications.map((id) => `${certificationName(catalog, id)} (alguien del equipo)`),
    ...requirements.heldByEveryone.map((id) => `${certificationName(catalog, id)} (todo el equipo)`),
    ...requirements.instruments.map((kind) => `Instrumento ${kind}`),
    ...requirements.specialPermits.map((kind) => `Permiso ${kind}`),
    ...(requirements.vehicles > 0 ? [requirements.vehicles === 1 ? '1 vehículo' : `${requirements.vehicles} vehículos`] : []),
  ];

  return (
    <dl className="timeline-detail">
      <dt>Ventana</dt>
      <dd>{formatPeriod(activity.window)}</dd>
      <dt>Duración estimada</dt>
      <dd>{formatDuration(activity.estimatedDuration)}</dd>
      <dt>Riesgo</dt>
      <dd>{RISK_LABELS[activity.risk]}</dd>
      <dt>Zona</dt>
      <dd>{activity.zone}</dd>
      <dt>Requiere</dt>
      <dd>
        {needs.length === 0 ? (
          <span className="muted">Nada en particular</span>
        ) : (
          <span className="chips">
            {needs.map((need) => (
              <span key={need} className="chip">
                {need}
              </span>
            ))}
          </span>
        )}
      </dd>
      <dt>Después de</dt>
      <dd>{activity.predecessors.length === 0 ? <span className="muted">Sin dependencias</span> : activity.predecessors.map((id) => activityName(plan, id)).join(', ')}</dd>
      <dt>Consumo estimado</dt>
      <dd>{consumptionOf(activity.consumption, catalog)}</dd>
    </dl>
  );
}
