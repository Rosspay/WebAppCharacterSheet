
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  useGetMyEventsQuery,
  useGetParticipatingQuery,
  useGetOpenEventsQuery,
  useGetMyInvitationsQuery,
} from '../../features/events/eventsApi';

const fmt = (s: string | null) =>
  !s ? '—' : new Date(s).toLocaleString('ru-RU', {
    day: '2-digit', month: '2-digit', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });

const EventListPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: my = [], isLoading: l1 } = useGetMyEventsQuery();
  const { data: part = [], isLoading: l2 } = useGetParticipatingQuery();
  const { data: open, isLoading: l3 } = useGetOpenEventsQuery({});
  const { data: invs = [], isLoading: l4 } = useGetMyInvitationsQuery();

  if (l1 || l2 || l3 || l4) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" />
      </div>
    );
  }

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">Мероприятия</h1>
        <button className="btn btn-primary"
                onClick={() => navigate('/events/new')}>
          + Создать
        </button>
      </div>

      <section className="mb-4">
        <h2 className="h5">Мои мероприятия</h2>
        {my.length === 0 ? (
          <p className="text-muted fst-italic">Вы пока не создавали мероприятия.</p>
        ) : (
          <div className="row g-3">
            {my.map(e => (
              <div className="col-12 col-md-6 col-lg-4" key={e.id}>
                <Link to={`/events/${e.id}`}
                      className="card text-decoration-none text-reset h-100">
                  <div className="card-body">
                    <h5 className="card-title">{e.title}</h5>
                    <p className="text-muted small mb-1">
                      Тип: {e.eventType === 'OPEN' ? 'Открытое' : 'Закрытое'}
                    </p>
                    <p className="text-muted small mb-1">{e.location ?? '—'}</p>
                    <p className="mb-0 small">Начало: {fmt(e.startsAt)}</p>
                  </div>
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>

      <section className="mb-4">
        <h2 className="h5">Где я участвую</h2>
        {part.length === 0 ? (
          <p className="text-muted fst-italic">Нет активных приглашений/участий.</p>
        ) : (
          <ul className="list-group">
            {part.map(e => (
              <li className="list-group-item d-flex justify-content-between
                             align-items-center"
                  key={e.id}>
                <span>
                  <strong>{e.title}</strong>{' '}
                  <small className="text-muted">— {fmt(e.startsAt)}</small>
                </span>
                <Link to={`/events/${e.id}`} className="btn btn-sm btn-outline-primary">
                  Открыть
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mb-4">
        <h2 className="h5">Мои приглашения</h2>
        {invs.length === 0 ? (
          <p className="text-muted fst-italic">Приглашений нет.</p>
        ) : (
          <ul className="list-group">
            {invs.map(inv => (
              <li className="list-group-item d-flex justify-content-between" key={inv.id}>
                <span>Событие #{inv.eventId} — статус <em>{inv.status}</em></span>
                <Link to={`/events/${inv.eventId}`}
                      className="btn btn-sm btn-outline-secondary">
                  Перейти
                </Link>
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="mb-4">
        <h2 className="h5">Открытые мероприятия</h2>
        {!open || open.items.length === 0 ? (
          <p className="text-muted fst-italic">Открытых мероприятий нет.</p>
        ) : (
          <div className="row g-3">
            {open.items.map(e => (
              <div className="col-12 col-md-6 col-lg-4" key={e.id}>
                <Link to={`/events/${e.id}`}
                      className="card text-decoration-none text-reset h-100">
                  <div className="card-body">
                    <h5 className="card-title">{e.title}</h5>
                    <p className="text-muted small mb-1">{e.location ?? '—'}</p>
                    <p className="mb-0 small">Начало: {fmt(e.startsAt)}</p>
                  </div>
                </Link>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  );
};

export default EventListPage;
