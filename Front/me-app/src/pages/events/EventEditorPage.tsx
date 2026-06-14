
import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useCreateEventMutation,
  useUpdateEventMutation,
  useGetEventByIdQuery,
} from '../../features/events/eventsApi';
import { EventRequest, EventType } from '../../types/event';

const empty: EventRequest = {
  title: '',
  description: '',
  location: '',
  startsAt: '',
  endsAt: '',
  eventType: 'CLOSED',
  allowApplications: false,
};

const EventEditorPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const isEdit = !!id;
  const { data: existing } = useGetEventByIdQuery(Number(id), { skip: !isEdit });
  const [create] = useCreateEventMutation();
  const [update] = useUpdateEventMutation();
  const [form, setForm] = useState<EventRequest>(empty);
  const [serverError, setServerError] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  useEffect(() => {
    if (!existing) return;
    setForm({
      title: existing.title,
      description: existing.description ?? '',
      location: existing.location ?? '',
      startsAt: existing.startsAt?.slice(0, 16) ?? '',
      endsAt: existing.endsAt?.slice(0, 16) ?? '',
      eventType: existing.eventType,
      allowApplications: existing.allowApplications,
    });
  }, [existing]);

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.title.trim()) e.title = 'Введите название';
    if (!form.startsAt) e.startsAt = 'Укажите дату начала';
    if (form.endsAt && form.startsAt && form.endsAt < form.startsAt) {
      e.endsAt = 'Дата окончания не может быть раньше даты начала';
    }
    return e;
  };

  const submit = async (ev: React.FormEvent) => {
    ev.preventDefault();
    const v = validate();
    if (Object.keys(v).length) { setErrors(v); return; }
    setErrors({});
    setServerError('');
    try {
      if (isEdit) {
        await update({ id: Number(id), body: form }).unwrap();
        navigate(`/events/${id}`);
      } else {
        const saved = await create(form).unwrap();
        navigate(`/events/${saved.id}`);
      }
    } catch (e: any) {
      setServerError(e?.data?.message ?? 'Не удалось сохранить мероприятие');
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 720 }}>
      <h1 className="h4 mb-3">{isEdit ? 'Редактирование мероприятия' : 'Новое мероприятие'}</h1>
      {serverError && <div className="alert alert-danger">{serverError}</div>}
      <form onSubmit={submit} noValidate>
        <div className="mb-3">
          <label className="form-label">Название</label>
          <input className={`form-control ${errors.title ? 'is-invalid' : ''}`}
                 value={form.title}
                 onChange={e => setForm({ ...form, title: e.target.value })} />
          {errors.title && <div className="invalid-feedback">{errors.title}</div>}
        </div>
        <div className="mb-3">
          <label className="form-label">Описание</label>
          <textarea className="form-control" rows={4}
                    value={form.description ?? ''}
                    onChange={e => setForm({ ...form, description: e.target.value })} />
        </div>
        <div className="mb-3">
          <label className="form-label">Место проведения</label>
          <input className="form-control"
                 value={form.location ?? ''}
                 onChange={e => setForm({ ...form, location: e.target.value })} />
        </div>
        <div className="row g-3 mb-3">
          <div className="col-12 col-md-6">
            <label className="form-label">Начало</label>
            <input type="datetime-local"
                   className={`form-control ${errors.startsAt ? 'is-invalid' : ''}`}
                   value={form.startsAt}
                   onChange={e => setForm({ ...form, startsAt: e.target.value })} />
            {errors.startsAt && <div className="invalid-feedback">{errors.startsAt}</div>}
          </div>
          <div className="col-12 col-md-6">
            <label className="form-label">Окончание (опц.)</label>
            <input type="datetime-local"
                   className={`form-control ${errors.endsAt ? 'is-invalid' : ''}`}
                   value={form.endsAt ?? ''}
                   onChange={e => setForm({ ...form, endsAt: e.target.value })} />
            {errors.endsAt && <div className="invalid-feedback">{errors.endsAt}</div>}
          </div>
        </div>
        <div className="mb-3">
          <label className="form-label">Тип мероприятия</label>
          <select className="form-select"
                  value={form.eventType}
                  onChange={e => setForm({ ...form, eventType: e.target.value as EventType })}>
            <option value="CLOSED">Закрытое (только по приглашению)</option>
            <option value="OPEN">Открытое (видно всем)</option>
          </select>
        </div>
        {form.eventType === 'OPEN' && (
          <div className="form-check mb-3">
            <input className="form-check-input"
                   id="allowApps"
                   type="checkbox"
                   checked={form.allowApplications}
                   onChange={e => setForm({ ...form, allowApplications: e.target.checked })} />
            <label htmlFor="allowApps" className="form-check-label">
              Принимать заявки на участие
            </label>
          </div>
        )}
        <div className="d-flex gap-2">
          <button className="btn btn-primary" type="submit">
            {isEdit ? 'Сохранить' : 'Создать'}
          </button>
          <button className="btn btn-outline-secondary"
                  type="button"
                  onClick={() => navigate(-1)}>
            Отмена
          </button>
        </div>
      </form>
    </div>
  );
};

export default EventEditorPage;
