import type { Instant, IsoDuration, Period } from './api/types';

const HOUR = 3_600_000;
const DURATION = /^PT(?:(\d+)H)?(?:(\d+)M)?(?:[\d.]+S)?$/;
const UTC = { timeZone: 'UTC' } as const;
const DAY = new Intl.DateTimeFormat('es-AR', { ...UTC, day: 'numeric', month: 'short' });
const DAY_IN_YEAR = new Intl.DateTimeFormat('es-AR', { ...UTC, day: 'numeric', month: 'short', year: 'numeric' });
const TIME = new Intl.DateTimeFormat('es-AR', { ...UTC, hour: '2-digit', minute: '2-digit', hourCycle: 'h23' });

export function toInput(instant: Instant): string {
  return instant.slice(0, 16);
}

export function fromInput(value: string): Instant {
  return `${value}:00Z`;
}

export function formatDay(instant: Instant): string {
  return DAY.format(new Date(instant));
}

export function formatInstant(instant: Instant): string {
  return `${formatDay(instant)}, ${TIME.format(new Date(instant))}`;
}

export function formatPeriod(period: Period): string {
  if (period.start.slice(0, 10) === period.end.slice(0, 10)) {
    return `${formatInstant(period.start)}–${TIME.format(new Date(period.end))}`;
  }
  return `${formatInstant(period.start)} – ${formatInstant(period.end)}`;
}

export function formatSpan(period: Period): string {
  return DAY_IN_YEAR.formatRange(new Date(period.start), new Date(period.end));
}

export function addHours(instant: Instant, hours: number): Instant {
  return new Date(new Date(instant).getTime() + hours * HOUR).toISOString().replace('.000Z', 'Z');
}

export function defaultPeriod(days = 7): Period {
  const today = new Date();
  today.setUTCHours(0, 0, 0, 0);
  const start = today.toISOString().replace('.000Z', 'Z');
  return { start, end: addHours(start, days * 24) };
}

export function isoDuration(hours: number, minutes: number): IsoDuration {
  return `PT${hours}H${minutes}M`;
}

export function formatDuration(duration: IsoDuration): string {
  const match = DURATION.exec(duration);
  if (!match) {
    return duration;
  }
  const hours = Number(match[1] ?? 0);
  const minutes = Number(match[2] ?? 0);
  const parts = [hours > 0 ? `${hours} h` : '', minutes > 0 ? `${minutes} min` : ''].filter(Boolean);
  return parts.length > 0 ? parts.join(' ') : '0 min';
}
