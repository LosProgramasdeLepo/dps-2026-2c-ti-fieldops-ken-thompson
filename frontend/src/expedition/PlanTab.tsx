import { useState } from 'react';
import { expeditionsApi } from '../api/expeditions';
import { Drawer } from '../components/Drawer';
import { Empty } from '../components/Empty';
import { ErrorNotice } from '../components/ErrorNotice';
import { Field } from '../components/Field';
import { Icon } from '../components/Icon';
import { Panel } from '../components/Panel';
import { useAction } from '../hooks/useAction';
import { useLoad } from '../hooks/useLoad';
import { RISK_LABELS } from '../labels';
import { formatDuration } from '../time';
import { ActivityFields, newActivity, toActivityRequest } from './ActivityFields';
import { BlockEditor, newBlock, toBlockRequest } from './BlockEditor';
import { activitiesOf, consumptionOf, type PlanTabProps } from './describe';
import { ItineraryTimeline } from './ItineraryTimeline';

type Editor = 'activity' | 'block' | 'dependency';

export function PlanTab({ plan, catalog, onChange }: PlanTabProps) {
  const estimate = useLoad(() => expeditionsApi.estimate(plan.id), [plan]);
  const [editor, setEditor] = useState<Editor>();
  const editable = plan.status === 'DRAFT';
  const activities = activitiesOf(plan.itinerary);
  const close = () => setEditor(undefined);
  const done = () => {
    close();
    onChange();
  };

  return (
    <>
      <Panel
        title="Itinerario"
        description={editable ? 'Las ventanas se cargan en UTC y tienen que caer dentro del período.' : 'El itinerario solo se edita en borrador.'}
        flush
        actions={
          editable && (
            <>
              {activities.length > 1 && (
                <button type="button" className="secondary" onClick={() => setEditor('dependency')}>
                  Agregar dependencia
                </button>
              )}
              <button type="button" className="secondary" onClick={() => setEditor('block')}>
                Agregar bloque
              </button>
              <button type="button" onClick={() => setEditor('activity')}>
                <Icon name="plus" />
                Agregar actividad
              </button>
            </>
          )
        }
      >
        {estimate.data && activities.length > 0 && (
          <dl className="summary">
            <div>
              <dt>Duración estimada</dt>
              <dd>{formatDuration(estimate.data.duration)}</dd>
            </div>
            <div>
              <dt>Riesgo</dt>
              <dd>{RISK_LABELS[estimate.data.risk]}</dd>
            </div>
            <div>
              <dt>Actividades</dt>
              <dd>{activities.length}</dd>
            </div>
            <div>
              <dt>Consumo estimado</dt>
              <dd>{consumptionOf(estimate.data.consumption, catalog)}</dd>
            </div>
          </dl>
        )}
        {plan.itinerary.length === 0 ? (
          <Empty>Todavía no hay actividades. Agregá la primera o armá un bloque secuencial o paralelo.</Empty>
        ) : (
          <ItineraryTimeline plan={plan} catalog={catalog} />
        )}
      </Panel>
      <Drawer title="Agregar actividad" open={editor === 'activity'} onClose={close}>
        <AddActivity {...{ plan, catalog }} onChange={done} />
      </Drawer>
      <Drawer title="Agregar bloque" open={editor === 'block'} onClose={close} wide>
        <AddBlock {...{ plan, catalog }} onChange={done} />
      </Drawer>
      <Drawer title="Agregar dependencia" open={editor === 'dependency'} onClose={close}>
        <AddDependency {...{ plan, catalog }} onChange={done} />
      </Drawer>
    </>
  );
}

function AddActivity({ plan, catalog, onChange }: PlanTabProps) {
  const [draft, setDraft] = useState(() => newActivity(plan));
  const action = useAction(onChange);

  return (
    <form className="form" onSubmit={action.onSubmit(() => expeditionsApi.addActivity(plan.id, toActivityRequest(draft)))}>
      <ActivityFields value={draft} onChange={setDraft} plan={plan} catalog={catalog} />
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Agregar actividad</button>
      </div>
    </form>
  );
}

function AddBlock({ plan, catalog, onChange }: PlanTabProps) {
  const [draft, setDraft] = useState(() => newBlock(plan));
  const action = useAction(onChange);

  return (
    <form className="form" onSubmit={action.onSubmit(() => expeditionsApi.addBlock(plan.id, toBlockRequest(draft)))}>
      <p className="muted">
        En un bloque secuencial cada parte empieza cuando termina la anterior; en uno paralelo las partes corren a la vez y no pueden compartir recursos.
      </p>
      <BlockEditor value={draft} onChange={setDraft} plan={plan} catalog={catalog} />
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Agregar bloque</button>
      </div>
    </form>
  );
}

function AddDependency({ plan, onChange }: PlanTabProps) {
  const activities = activitiesOf(plan.itinerary);
  const [activityId, setActivityId] = useState('');
  const [predecessorId, setPredecessorId] = useState('');
  const action = useAction(onChange);

  return (
    <form className="form" onSubmit={action.onSubmit(() => expeditionsApi.addDependency(plan.id, activityId, predecessorId))}>
      <Field label="Actividad">
        <select required value={activityId} onChange={(event) => setActivityId(event.target.value)}>
          <option value="">Elegir…</option>
          {activities.map((activity) => (
            <option key={activity.id} value={activity.id}>
              {activity.name}
            </option>
          ))}
        </select>
      </Field>
      <Field label="Empieza después de">
        <select required value={predecessorId} onChange={(event) => setPredecessorId(event.target.value)}>
          <option value="">Elegir…</option>
          {activities
            .filter((activity) => activity.id !== activityId)
            .map((activity) => (
              <option key={activity.id} value={activity.id}>
                {activity.name}
              </option>
            ))}
        </select>
      </Field>
      <ErrorNotice error={action.error} />
      <div className="form-actions">
        <button disabled={action.busy}>Agregar dependencia</button>
      </div>
    </form>
  );
}
