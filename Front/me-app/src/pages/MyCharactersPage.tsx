import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import {
  useGetMyCharactersQuery,
  useDeleteCharacterMutation,
  useSetVisibilityMutation,
} from '../features/characters/charactersApi';
import { CharacterVisibility } from '../types/character';

const VISIBILITY_LABELS: Record<CharacterVisibility, string> = {
  PRIVATE:    'Приватный',
  PUBLIC:     'Публичный',
  RESTRICTED: 'Ограниченный',
};

const VISIBILITY_BADGE: Record<CharacterVisibility, string> = {
  PRIVATE:    'bg-secondary',
  PUBLIC:     'bg-success',
  RESTRICTED: 'bg-warning text-dark',
};

const MyCharactersPage: React.FC = () => {
  const navigate = useNavigate();
  const { data: characters, isLoading, isError } = useGetMyCharactersQuery();
  const [deleteCharacter] = useDeleteCharacterMutation();
  const [setVisibility] = useSetVisibilityMutation();
  const [confirmDelete, setConfirmDelete] = useState<number | null>(null);

  if (isLoading) return (
    <div className="container py-5 text-center">
      <div className="spinner-border text-primary" role="status" />
    </div>
  );
  if (isError) return (
    <div className="container py-5">
      <div className="alert alert-danger">Не удалось загрузить персонажей</div>
    </div>
  );

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="h3 mb-0">Мои персонажи</h1>
        <Link to="/characters/new" className="btn btn-primary">+ Создать персонажа</Link>
      </div>

      {!characters?.length ? (
        <div className="text-center py-5 text-muted">
          <p className="mb-3">У вас пока нет персонажей</p>
          <Link to="/characters/new" className="btn btn-outline-primary">
            Создать первого персонажа
          </Link>
        </div>
      ) : (
        <div className="row g-3">
          {characters.map((c) => (
            <div key={c.id} className="col-12 col-md-6 col-lg-4">
              <div className="card h-100 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start mb-2">
                    <h5 className="card-title mb-0">{c.name}</h5>
                    <span className={`badge ${VISIBILITY_BADGE[c.visibility]}`}>
                      {VISIBILITY_LABELS[c.visibility]}
                    </span>
                  </div>
                  <p className="card-text text-muted small">{c.description}</p>
                  <p className="card-text text-muted" style={{ fontSize: '0.8rem' }}>
                    Шаблон #{c.templateId}
                  </p>
                </div>
                <div className="card-footer bg-transparent d-flex gap-2 flex-wrap">
                  <Link
                    to={`/characters/${c.id}`}
                    className="btn btn-sm btn-outline-primary"
                  >
                    Просмотр
                  </Link>
                  <Link
                    to={`/characters/${c.id}/edit`}
                    className="btn btn-sm btn-outline-secondary"
                  >
                    Редактировать
                  </Link>
                  <button
                    className="btn btn-sm btn-outline-success"
                    onClick={() =>
                      setVisibility({
                        id: c.id,
                        body: {
                          visibility: c.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC',
                          allowedUserIds: [],
                        },
                      })
                    }
                  >
                    {c.visibility === 'PUBLIC' ? 'Скрыть' : 'Опубликовать'}
                  </button>
                  {confirmDelete === c.id ? (
                    <>
                      <button
                        className="btn btn-sm btn-danger"
                        onClick={async () => {
                          await deleteCharacter(c.id);
                          setConfirmDelete(null);
                        }}
                      >
                        Удалить
                      </button>
                      <button
                        className="btn btn-sm btn-outline-secondary"
                        onClick={() => setConfirmDelete(null)}
                      >
                        Отмена
                      </button>
                    </>
                  ) : (
                    <button
                      className="btn btn-sm btn-outline-danger"
                      onClick={() => setConfirmDelete(c.id)}
                    >
                      Удалить
                    </button>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default MyCharactersPage;