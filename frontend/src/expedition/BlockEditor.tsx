import type { Arrangement, BlockRequest, Expedition, Instant, ItineraryNodeRequest } from '../api/types';
import type { Catalog } from '../hooks/useCatalog';
import { ARRANGEMENT_LABELS } from '../labels';
import { addHours } from '../time';
import { ActivityFields, newActivity, toActivityRequest, type ActivityDraft } from './ActivityFields';

export interface BlockDraft {
  arrangement: Arrangement;
  parts: NodeDraft[];
}

type NodeDraft = { node: 'ACTIVITY'; activity: ActivityDraft } | { node: 'BLOCK'; block: BlockDraft };

export function newBlock(plan: Expedition, arrangement: Arrangement = 'SEQUENTIAL', start: Instant = plan.charter.period.start): BlockDraft {
  const second = arrangement === 'SEQUENTIAL' ? addHours(start, 2) : start;
  return {
    arrangement,
    parts: [
      { node: 'ACTIVITY', activity: newActivity(plan, start) },
      { node: 'ACTIVITY', activity: newActivity(plan, second) },
    ],
  };
}

function leavesOf(block: BlockDraft): ActivityDraft[] {
  return block.parts.flatMap((part) => (part.node === 'ACTIVITY' ? [part.activity] : leavesOf(part.block)));
}

function nextStart(block: BlockDraft): Instant {
  const windows = leavesOf(block).map((activity) => activity.window);
  return block.arrangement === 'SEQUENTIAL'
    ? windows.map((window) => window.end).reduce((latest, end) => (end > latest ? end : latest))
    : windows.map((window) => window.start).reduce((earliest, start) => (start < earliest ? start : earliest));
}

export function toBlockRequest(block: BlockDraft): BlockRequest {
  return { arrangement: block.arrangement, parts: block.parts.map(toNodeRequest) };
}

function toNodeRequest(part: NodeDraft): ItineraryNodeRequest {
  return part.node === 'ACTIVITY'
    ? { node: 'ACTIVITY', activity: toActivityRequest(part.activity) }
    : { node: 'BLOCK', ...toBlockRequest(part.block) };
}

interface BlockEditorProps {
  value: BlockDraft;
  onChange: (block: BlockDraft) => void;
  plan: Expedition;
  catalog: Catalog;
}

export function BlockEditor({ value, onChange, plan, catalog }: BlockEditorProps) {
  const replace = (index: number, part: NodeDraft) =>
    onChange({ ...value, parts: value.parts.map((current, position) => (position === index ? part : current)) });
  const remove = (index: number) => onChange({ ...value, parts: value.parts.filter((_, position) => position !== index) });
  const add = (part: NodeDraft) => onChange({ ...value, parts: [...value.parts, part] });

  return (
    <fieldset className={`block-editor block-${value.arrangement.toLowerCase()}`}>
      <legend>
        <select value={value.arrangement} onChange={(event) => onChange({ ...value, arrangement: event.target.value as Arrangement })}>
          {Object.entries(ARRANGEMENT_LABELS).map(([arrangement, label]) => (
            <option key={arrangement} value={arrangement}>
              Bloque {label.toLowerCase()}
            </option>
          ))}
        </select>
      </legend>
      {value.parts.map((part, index) => (
        <div key={index} className="part">
          <div className="part-header">
            <span className="muted">Parte {index + 1}</span>
            {value.parts.length > 2 && (
              <button type="button" className="link" onClick={() => remove(index)}>
                Quitar
              </button>
            )}
          </div>
          {part.node === 'ACTIVITY' ? (
            <ActivityFields
              value={part.activity}
              onChange={(activity) => replace(index, { node: 'ACTIVITY', activity })}
              plan={plan}
              catalog={catalog}
            />
          ) : (
            <BlockEditor value={part.block} onChange={(block) => replace(index, { node: 'BLOCK', block })} plan={plan} catalog={catalog} />
          )}
        </div>
      ))}
      <div className="row">
        <button type="button" className="link" onClick={() => add({ node: 'ACTIVITY', activity: newActivity(plan, nextStart(value)) })}>
          Agregar actividad
        </button>
        <button type="button" className="link" onClick={() => add({ node: 'BLOCK', block: newBlock(plan, 'PARALLEL', nextStart(value)) })}>
          Agregar bloque anidado
        </button>
      </div>
    </fieldset>
  );
}
