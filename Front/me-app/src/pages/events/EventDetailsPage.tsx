
import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useGetEventByIdQuery,
  useDeleteEventMutation,
  useInviteUserMutation,
  useGetInvitationsQuery,
  useCancelInvitationMutation,
  useGetApplicationsQuery,
  useReviewApplicationMutation,
  useApplyMutation,
  useGetMyInvitationsQuery,
  useRespondToInvitationMutation,
} from '../../features/events/eventsApi';
import { useAppSelector } from '../../app/hooks';
import UsernameAutocomplete from '../../components/UsernameAutocomplete';

const fmt = (s: string | null) =>
  !s ? '—' : new Date(s).toLocaleString('ru-RU');

const EventDetailsPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const eventId = Number(id);
  const navigate = useNavigate();
  const userId = useAppSelector(s => s.auth.user?.id);

  const { data: ev, isLoading, isError } = useGetEventByIdQuery(eventId);
  const [deleteEvent] = useDeleteEventMutation();
  const [invite] = useInviteUserMutation();
  const [cancelInv] = useCancelInvitationMutation();
  const [apply] = useApplyMutation();
  const [review] = useReviewApplicationMutation();
  const [respond] = useRespondToInvitationMutation();

  const isOwner = !!ev && userId === ev.ownerId;
  const { data: invs = [] } = useGetInvitationsQuery(eventId,
    { skip: !ev || !isOwner });
  const { data: apps = [] } = useGetApplicationsQuery(eventId,
    { skip: !ev || !isOwner });
  const { data: myInvs = [] } = useGetMyInvitationsQuery();
  const myInvitation = myInvs.find(i => i.eventId === eventId);

  const [inviteUsername, setInviteUsername] = useState('');
  const [appMessage, setAppMessage] = useState('');
  const [serverError, setServerError] = useState('');

  if (isLoading) {
    return (
      <div className="container py-5 text-center">
        <div className="spinner-border text-primary" />
      </div>
    );
  }
  if (isError || !ev) {
    return (
      <div className="container py-4">
        <div className="alert alert-danger">Мероприятие не найдено или недоступно</div>
      </div>
    );
  }

  const handleDelete = async () => {
    if (!window.confirm('Удалить мероприятие?')) return;
    await deleteEvent(eventId).unwrap();
    navigate('/events');
  };

  const sendInvite = async (override?: string) => {
    setServerError('');
    const name = (override ?? inviteUsername).trim();
    if (!name) return;
    try {
      await invite({ id: eventId, body: { username: name } }).unwrap();
      setInviteUsername('');
    } catch (e: any) {
      setServerError(e?.data?.message ?? 'Не удалось отправить приглашение');
    }
  };

  const sendApplication = async () => {
    setServerError('');
    try {
      await apply({ id: eventId, body: { message: appMessage } }).unwrap();
      setAppMessage('');
    } catch (e: any) {
      setServerError(e?.data?.message ?? 'Не удалось подать заявку');
    }
  };

  return (
    <div className="container py-4" style={{ maxWidth: 820 }}>
      <div className="d-flex justify-content-between align-items-start gap-2 mb-3">
        <div>
          <h1 className="h3 mb-1">{ev.title}</h1>
          <p className="text-muted mb-1">{ev.description ?? ''}</p>
          <p className="mb-0 small">
            <span className={`badge ${ev.eventType === 'OPEN' ? 'bg-success' : 'bg-secondary'}`}>
              {ev.eventType === 'OPEN' ? 'Открытое' : 'Закрытое'}
            </span>
            <span className="ms-2 text-muted">Место: {ev.location ?? '—'}</span>
          </p>
          <p className="mb-0 small">
            Начало: <strong>{fmt(ev.startsAt)}</strong>{' '}
            {ev.endsAt && <>· Окончание: <strong>{fmt(ev.endsAt)}</strong></>}
          </p>
        </div>
        {isOwner && (
          <div className="d-flex gap-2">
            <button className="btn btn-sm btn-primary"
                    onClick={() => navigate(`/events/${ev.id}/edit`)}>
              Редактировать
            </button>
            <button className="btn btn-sm btn-outline-danger"
                    onClick={handleDelete}>
              Удалить
            </button>
          </div>
        )}
      </div>

      {serverError && <div className="alert alert-danger">{serverError}</div>}

      {}
      {!isOwner && myInvitation && myInvitation.status === 'INVITED' && (
        <div className="card mb-3">
          <div className="card-body d-flex gap-2 align-items-center justify-content-between">
            <span>Вы приглашены. Подтвердите участие:</span>
            <div className="d-flex gap-2">
              <button className="btn btn-success btn-sm"
                      onClick={() => respond({
                        invitationId: myInvitation.id,
                        body: { status: 'ACCEPTED' },
                      })}>
                Принять
              </button>
              <button className="btn btn-outline-secondary btn-sm"
                      onClick={() => respond({
                        invitationId: myInvitation.id,
                        body: { status: 'DECLINED' },
                      })}>
                Отклонить
              </button>
            </div>
          </div>
        </div>
      )}

      {!isOwner && ev.eventType === 'OPEN' && ev.allowApplications && (
        <div className="card mb-3">
          <div className="card-body">
            <h2 className="h6">Подать заявку на участие</h2>
            <textarea className="form-control mb-2" rows={3}
                      placeholder="Сообщение для организатора"
                      value={appMessage}
                      onChange={e => setAppMessage(e.target.value)} />
            <button className="btn btn-primary btn-sm" onClick={sendApplication}>
              Отправить заявку
            </button>
          </div>
        </div>
      )}

      {}
      {isOwner && (
        <section className="card mb-3">
          <div className="card-body">
            <h2 className="h6">Приглашения</h2>
            <div className="d-flex gap-2 mb-3">
              <UsernameAutocomplete
                value={inviteUsername}
                onChange={setInviteUsername}
                onPick={(u) => { setInviteUsername(u); sendInvite(u); }}
                placeholder="Логин пользователя"
                exclude={invs.map(i => i.username)}
              />
              <button className="btn btn-primary" onClick={() => sendInvite()}>
                Пригласить
              </button>
            </div>
            {invs.length === 0
              ? <p className="text-muted fst-italic mb-0">Пока никого не пригласили.</p>
              : (
                <ul className="list-group">
                  {invs.map(inv => (
                    <li key={inv.id}
                        className="list-group-item d-flex justify-content-between
                                   align-items-center">
                      <span><strong>{inv.username}</strong> — <em>{inv.status}</em></span>
                      <button className="btn btn-sm btn-outline-danger"
                              onClick={() => cancelInv({ id: eventId, invitationId: inv.id })}>
                        Отозвать
                      </button>
                    </li>
                  ))}
                </ul>
              )}
          </div>
        </section>
      )}

      {}
      {isOwner && ev.eventType === 'OPEN' && ev.allowApplications && (
        <section className="card mb-3">
          <div className="card-body">
            <h2 className="h6">Заявки</h2>
            {apps.length === 0
              ? <p className="text-muted fst-italic mb-0">Пока заявок нет.</p>
              : (
                <ul className="list-group">
                  {apps.map(a => (
                    <li key={a.id} className="list-group-item">
                      <div className="d-flex justify-content-between">
                        <strong>{a.username}</strong>
                        <em className="text-muted">{a.status}</em>
                      </div>
                      {a.message && <p className="mb-2 small">{a.message}</p>}
                      {a.status === 'PENDING' && (
                        <div className="d-flex gap-2">
                          <button className="btn btn-sm btn-success"
                                  onClick={() => review({
                                    id: eventId, applicationId: a.id,
                                    body: { status: 'ACCEPTED' },
                                  })}>
                            Принять
                          </button>
                          <button className="btn btn-sm btn-outline-secondary"
                                  onClick={() => review({
                                    id: eventId, applicationId: a.id,
                                    body: { status: 'REJECTED' },
                                  })}>
                            Отклонить
                          </button>
                        </div>
                      )}
                    </li>
                  ))}
                </ul>
              )}
          </div>
        </section>
      )}
    </div>
  );
};

export default EventDetailsPage;
