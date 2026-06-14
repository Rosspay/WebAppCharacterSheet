import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  useGetMyTemplatesQuery,
  useDeleteTemplateMutation,
  useTogglePublishMutation,
} from '../features/atemplates/templatesApi';

const MyTemplatesPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: templates, isLoading, isError } = useGetMyTemplatesQuery();
  const [deleteTemplate] = useDeleteTemplateMutation();
  const [togglePublish] = useTogglePublishMutation();
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);

  if (isLoading) return (
    <div className="container py-5 text-center">
      <div className="spinner-border text-primary" role="status" />
    </div>
  );

  if (isError) return (
    <div className="container py-5">
      <div className="alert alert-danger">Не удалось загрузить шаблоны</div>
    </div>
  );

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="h3 mb-0">Мои шаблоны</h1>
        <button
          className="btn btn-primary"
          onClick={() => navigate('/templates/new')}
        >
          + Создать шаблон
        </button>
      </div>

      {!templates?.length ? (
        <div className="text-center py-5 text-muted">
          <p className="mb-3">У вас пока нет шаблонов</p>
          <button className="btn btn-outline-primary" onClick={() => navigate('/templates/new')}>
            Создать первый шаблон
          </button>
        </div>
      ) : (
        <div className="row g-3">
          {templates.map((t) => (
            <div key={t.id} className="col-12 col-md-6 col-lg-4">
              <div className="card h-100 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <h5 className="card-title mb-0">{t.title}</h5>
                    <span className={`badge ${t.isPublic ? 'bg-success' : 'bg-secondary'}`}>
                      {t.isPublic ? 'Публичный' : 'Приватный'}
                    </span>
                  </div>
                  <p className="card-text text-muted small">{t.description || '—'}</p>
                  <p className="text-muted" style={{ fontSize: '0.75rem' }}>
                    Обновлён: {new Date(t.updatedAt).toLocaleDateString('ru-RU')}
                  </p>
                </div>
                <div className="card-footer bg-transparent d-flex gap-2 flex-wrap">
                  <Link to={`/templates/${t.id}`} className="btn btn-sm btn-outline-secondary">
                    Открыть
                  </Link>
                  <Link to={`/templates/${t.id}/edit`} className="btn btn-sm btn-outline-primary">
                    Редактировать
                  </Link>
                  <button
                    className="btn btn-sm btn-outline-secondary"
                    onClick={() => togglePublish(t.id)}
                  >
                    {t.isPublic ? 'Скрыть' : 'Опубликовать'}
                  </button>
                  <button
                    className="btn btn-sm btn-outline-danger ms-auto"
                    onClick={() => setConfirmDelete(t.id)}
                  >
                    Удалить
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {}
      {confirmDelete !== null && (
        <div className="modal show d-block" style={{ background: 'rgba(0,0,0,0.4)' }}>
          <div className="modal-dialog modal-dialog-centered">
            <div className="modal-content">
              <div className="modal-header">
                <h5 className="modal-title">Удалить шаблон?</h5>
                <button className="btn-close" onClick={() => setConfirmDelete(null)} />
              </div>
              <div className="modal-body">
                Это действие необратимо. Шаблон будет удалён навсегда.
              </div>
              <div className="modal-footer">
                <button className="btn btn-secondary" onClick={() => setConfirmDelete(null)}>
                  Отмена
                </button>
                <button
                  className="btn btn-danger"
                  onClick={async () => {
                    await deleteTemplate(confirmDelete);
                    setConfirmDelete(null);
                  }}
                >
                  Удалить
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MyTemplatesPage;