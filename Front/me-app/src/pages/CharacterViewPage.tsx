
import React, { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  useGetCharacterByIdQuery,
  useDeleteCharacterMutation,
  useSetVisibilityMutation,
} from '../features/characters/charactersApi';
import { useGetTemplateByIdQuery } from '../features/atemplates/templatesApi';
import { useAppSelector } from '../app/hooks';
import { downloadCharacterPdf } from '../features/characters/downloadCharacterPdf';
import { TemplateNode } from '../types/template';
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

const renderFieldView = (
  node: TemplateNode,
  values: Record<string, unknown>
): React.ReactNode => {
  switch (node.type) {
    case 'BLOCK': {
      const val = values[node.id];
      return (
        <div key={node.id} className="mb-2 d-flex gap-2">
          <span className="fw-medium text-muted" style={{ minWidth: 180 }}>
            {node.label}
          </span>
          <span>{val != null ? String(val) : '—'}</span>
        </div>
      );
    }

    case 'COUNTER': {
      const val = values[node.id];
      return (
        <div key={node.id} className="mb-2 d-flex gap-2">
          <span className="fw-medium text-muted" style={{ minWidth: 180 }}>
            {node.label}
          </span>
          <span>
            {val != null ? String(val) : String(node.currentValue)}
            {node.maxValue != null && (
              <span className="text-muted"> / {node.maxValue}</span>
            )}
          </span>
        </div>
      );
    }

    case 'TEXT_FIELD': {
      const val = (values[node.id] as string) ?? '';
      return (
        <div key={node.id} className="mb-3">
          {node.placeholder && (
            <p className="fw-medium text-muted mb-1">{node.placeholder}</p>
          )}
          <p
            className="mb-0"
            style={{ whiteSpace: 'pre-wrap', minHeight: '1.5rem' }}
          >
            {val || <span className="text-muted fst-italic">Не заполнено</span>}
          </p>
        </div>
      );
    }

    case 'TABLE': {
      return (
        <div key={node.id} className="mb-3">
          <p className="fw-medium text-muted mb-1">{node.label}</p>
          <div className="table-responsive">
            <table className="table table-bordered table-sm mb-0">
              <tbody>
                {Array.from({ length: node.rows }, (_, r) => (
                  <tr key={r}>
                    {Array.from({ length: node.columns }, (_, c) => {
                      const cellKey = `${node.id}_${r}_${c}`;
                      return (
                        <td key={c} className="align-middle">
                          {(values[cellKey] as string) ?? ''}
                        </td>
                      );
                    })}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      );
    }

    case 'CONTAINER': {
      return (
        <div key={node.id} className="card mb-3">
          <div className="card-header fw-medium bg-light">{node.title}</div>
          <div className="card-body py-3">
            {node.children.length === 0 ? (
              <span className="text-muted fst-italic">Нет полей</span>
            ) : (
              node.children.map((child) => renderFieldView(child, values))
            )}
          </div>
        </div>
      );
    }

    default:
      return null;
  }
};

const CharacterViewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const currentUserId = useAppSelector((s) => s.auth.user?.id);

  const {
    data: character,
    isLoading,
    isError,
  } = useGetCharacterByIdQuery(Number(id));

  const { data: template, isLoading: templateLoading } = useGetTemplateByIdQuery(
    character?.templateId as number,
    { skip: character?.templateId == null }
  );

  const [deleteCharacter] = useDeleteCharacterMutation();
  const [setVisibility]   = useSetVisibilityMutation();
  const [showConfirmDelete, setShowConfirmDelete] = useState(false);
  const [pdfDownloading, setPdfDownloading] = useState(false);
  const [pdfError, setPdfError] = useState<string | null>(null);
  const accessToken = useAppSelector(s => s.auth.accessToken);

  if (isLoading) return (
    <div className="container py-5 text-center">
      <div className="spinner-border text-primary" role="status" />
    </div>
  );

  if (isError || !character) return (
    <div className="container py-5">
      <div className="alert alert-danger">
        Персонаж не найден или недоступен
      </div>
    </div>
  );

  const isOwner = currentUserId === character.ownerId;

  const handleDelete = async () => {
    await deleteCharacter(character.id).unwrap();
    navigate('/characters/my');
  };

  const handleDownloadPdf = async () => {
    if (!character) return;
    setPdfError(null);
    setPdfDownloading(true);
    try {
      await downloadCharacterPdf(character.id, character.name, accessToken);
    } catch (e: any) {
      setPdfError(e?.message ?? 'Не удалось скачать PDF');
    } finally {
      setPdfDownloading(false);
    }
  };

  const togglePublic = () => {
    const newVis: CharacterVisibility =
      character.visibility === 'PUBLIC' ? 'PRIVATE' : 'PUBLIC';
    setVisibility({
      id: character.id,
      body: { visibility: newVis, allowedUsernames: [] },
    });
  };

  return (
    <div className="container py-4" style={{ maxWidth: 720 }}>
      {}
      <div className="d-flex justify-content-between align-items-start mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h3 mb-1">{character.name}</h1>
          {character.description && (
            <p className="text-muted mb-1">{character.description}</p>
          )}
          <span
            className={`badge ${VISIBILITY_BADGE[character.visibility]}`}
          >
            {VISIBILITY_LABELS[character.visibility]}
          </span>
          {template && (
            <span className="ms-2 text-muted small">
              Шаблон: <strong>{template.title}</strong>
            </span>
          )}
        </div>

        <div className="d-flex gap-2 flex-wrap">
          <button
            className="btn btn-sm btn-outline-primary"
            onClick={handleDownloadPdf}
            disabled={pdfDownloading}
          >
            {pdfDownloading ? 'Готовим PDF…' : 'Скачать PDF'}
          </button>
        </div>

        {isOwner && (
          <div className="d-flex gap-2 flex-wrap">
            <button
              className="btn btn-sm btn-outline-success"
              onClick={togglePublic}
            >
              {character.visibility === 'PUBLIC' ? 'Скрыть' : 'Опубликовать'}
            </button>
            <button
              className="btn btn-sm btn-outline-secondary"
              onClick={() => navigate(`/characters/${id}/visibility`)}
            >
              Доступ
            </button>
            <button
              className="btn btn-sm btn-primary"
              onClick={() => navigate(`/characters/${id}/edit`)}
            >
              Редактировать
            </button>
            {showConfirmDelete ? (
              <>
                <button className="btn btn-sm btn-danger" onClick={handleDelete}>
                  Удалить
                </button>
                <button
                  className="btn btn-sm btn-outline-secondary"
                  onClick={() => setShowConfirmDelete(false)}
                >
                  Отмена
                </button>
              </>
            ) : (
              <button
                className="btn btn-sm btn-outline-danger"
                onClick={() => setShowConfirmDelete(true)}
              >
                Удалить
              </button>
            )}
          </div>
        )}
      </div>

      {pdfError && (
        <div className="alert alert-warning py-2">{pdfError}</div>
      )}

      <h2 className="h5 mb-3">Характеристики</h2>

      {templateLoading ? (
        <div className="d-flex align-items-center gap-2 text-muted">
          <div className="spinner-border spinner-border-sm" role="status" />
          <span>Загрузка шаблона...</span>
        </div>
      ) : template?.content?.length ? (
        <div>
          {template.content.map((node) =>
            renderFieldView(node, character.fieldValues)
          )}
        </div>
      ) : (
        <p className="text-muted fst-italic">
          {template ? 'Шаблон не содержит полей' : 'Шаблон недоступен'}
        </p>
      )}
    </div>
  );
};

export default CharacterViewPage;