import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useGetCharacterByIdQuery,
  useSetVisibilityMutation,
} from '../features/characters/charactersApi';
import { CharacterVisibility } from '../types/character';

const VisibilityPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: character } = useGetCharacterByIdQuery(Number(id));
  const [setVisibility, { isLoading }] = useSetVisibilityMutation();

  const [vis, setVis]             = useState<CharacterVisibility>('PRIVATE');
  const [allowedIds, setAllowedIds] = useState<number[]>([]);
  const [input, setInput]           = useState('');

  useEffect(() => {
    if (character) {
      setVis(character.visibility);
      setAllowedIds(character.allowedUserIds ?? []);
    }
  }, [character]);

  const handleSave = async () => {
    await setVisibility({ id: Number(id), body: { visibility: vis, allowedUserIds: allowedIds } }).unwrap();
    navigate(`/characters/${id}`);
  };

  return (
    <div className="container py-4" style={{ maxWidth: 540 }}>
      <h1 className="h4 mb-4">Настройки доступа к персонажу</h1>

      <div className="mb-3">
        <label className="form-label fw-medium">Видимость</label>
        <select className="form-select" value={vis} onChange={(e) => setVis(e.target.value as CharacterVisibility)}>
          <option value="PRIVATE">Приватный — только я</option>
          <option value="PUBLIC">Публичный — все пользователи</option>
          <option value="RESTRICTED">Ограниченный — выбранные пользователи</option>
        </select>
      </div>

      {vis === 'RESTRICTED' && (
        <div className="mb-3 p-3 border rounded bg-light">
          <label className="form-label fw-medium">ID пользователей с доступом</label>
          <div className="d-flex gap-2 mb-2">
            <input
              type="number"
              className="form-control"
              placeholder="ID пользователя"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  const uid = Number(input.trim());
                  if (uid && !allowedIds.includes(uid)) setAllowedIds((p) => [...p, uid]);
                  setInput('');
                }
              }}
            />
            <button
              className="btn btn-outline-primary"
              type="button"
              onClick={() => {
                const uid = Number(input.trim());
                if (uid && !allowedIds.includes(uid)) setAllowedIds((p) => [...p, uid]);
                setInput('');
              }}
            >
              Добавить
            </button>
          </div>
          <div className="d-flex flex-wrap gap-1">
            {allowedIds.map((uid) => (
              <span key={uid} className="badge bg-secondary d-flex align-items-center gap-1">
                #{uid}
                <button
                  type="button"
                  className="btn-close btn-close-white"
                  style={{ fontSize: '0.6rem' }}
                  onClick={() => setAllowedIds((p) => p.filter((x) => x !== uid))}
                  aria-label="Удалить"
                />
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="d-flex gap-2 mt-3">
        <button className="btn btn-primary" onClick={handleSave} disabled={isLoading}>
          {isLoading && <span className="spinner-border spinner-border-sm me-2" />}
          Сохранить
        </button>
        <button className="btn btn-outline-secondary" onClick={() => navigate(-1)}>Отмена</button>
      </div>
    </div>
  );
};

export default VisibilityPage;