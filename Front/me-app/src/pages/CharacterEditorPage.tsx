import React, { useEffect, useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { useGetMyTemplatesQuery, useGetTemplateByIdQuery } from '../features/atemplates/templatesApi';
import {
  useCreateCharacterMutation,
  useUpdateCharacterMutation,
  useGetCharacterByIdQuery,
} from '../features/characters/charactersApi';
import { TemplateNode } from '../types/template';
import { CharacterVisibility } from '../types/character';
import UsernameAutocomplete from '../components/UsernameAutocomplete';

const renderField = (
  node: TemplateNode,
  values: Record<string, unknown>,
  onChange: (id: string, val: unknown) => void
) => {
  const val = values[node.id];
  switch (node.type) {
    case 'BLOCK':
      return (
        <div key={node.id} className="mb-3">
          <label className="form-label fw-medium">{node.label}</label>
          <input
            type="number"
            className="form-control"
            value={(val as number) ?? node.defaultValue ?? ''}
            onChange={(e) => onChange(node.id, Number(e.target.value))}
          />
        </div>
      );
    case 'COUNTER':
      return (
        <div key={node.id} className="mb-3">
          <label className="form-label fw-medium">{node.label}</label>
          <div className="d-flex gap-2 align-items-center">
            <input
              type="number"
              className="form-control"
              style={{ maxWidth: 100 }}
              value={(val as number) ?? node.currentValue}
              onChange={(e) => onChange(node.id, Number(e.target.value))}
            />
            {node.maxValue != null && (
              <span className="text-muted">/ {node.maxValue}</span>
            )}
          </div>
        </div>
      );
    case 'TEXT_FIELD':
      return (
        <div key={node.id} className="mb-3">
          <textarea
            className="form-control"
            rows={3}
            placeholder={node.placeholder}
            value={(val as string) ?? ''}
            onChange={(e) => onChange(node.id, e.target.value)}
          />
        </div>
      );
    case 'TABLE':
      return (
        <div key={node.id} className="mb-3">
          <label className="form-label fw-medium">{node.label}</label>
          <div className="table-responsive">
            <table className="table table-bordered table-sm">
              <tbody>
                {Array.from({ length: node.rows }, (_, r) => (
                  <tr key={r}>
                    {Array.from({ length: node.columns }, (_, c) => {
                      const cellKey = `${node.id}_${r}_${c}`;
                      return (
                        <td key={c} className="p-1">
                          <input
                            type="text"
                            className="form-control form-control-sm border-0"
                            value={(values[cellKey] as string) ?? ''}
                            onChange={(e) => onChange(cellKey, e.target.value)}
                          />
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
    case 'CONTAINER':
      return (
        <div key={node.id} className="card mb-3">
          <div className="card-header fw-medium">{node.title}</div>
          <div className="card-body">
            {node.children.map((child) => renderField(child, values, onChange))}
          </div>
        </div>
      );
    default:
      return null;
  }
};

const CharacterEditorPage: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const isNew = !id;
  const navigate = useNavigate();

  const { data: existing } = useGetCharacterByIdQuery(Number(id), { skip: isNew });

  const [searchParams] = useSearchParams();
  const templateIdFromQuery = searchParams.get('templateId');
  const [selectedTemplateId, setSelectedTemplateId] = useState<number | null>(null);
  const resolvedTemplateId: number | null =
  existing?.templateId ??
  (templateIdFromQuery ? Number(templateIdFromQuery) : null);

  const { data: myTemplates, isLoading: templatesLoading } = useGetMyTemplatesQuery(
    undefined,
    { skip: !isNew }
  );

  const { data: template, isFetching: templateFetching } = useGetTemplateByIdQuery(
    resolvedTemplateId as number,
    { skip: resolvedTemplateId == null || isNaN(resolvedTemplateId) }
  );



  const [createCharacter, { isLoading: creating }] = useCreateCharacterMutation();
  const [updateCharacter, { isLoading: updating }] = useUpdateCharacterMutation();

  const [name, setName]               = useState('');
  const [description, setDescription] = useState('');
  const [visibility, setVisibility]   = useState<CharacterVisibility>('PRIVATE');
  const [fieldValues, setFieldValues] = useState<Record<string, unknown>>({});
  const [allowedUsernames, setAllowedUsernames] = useState<string[]>([]);
  const [allowedInput, setAllowedInput] = useState('');

  useEffect(() => {
    if (existing) {
      setName(existing.name);
      setDescription(existing.description ?? '');
      setVisibility(existing.visibility);
      setFieldValues(existing.fieldValues ?? {});
      setAllowedUsernames(existing.allowedUsernames ?? []);
    }
  }, [existing]);

  useEffect(() => {
    if (isNew) setFieldValues({});
  }, [selectedTemplateId, isNew]);

  const handleFieldChange = (nodeId: string, val: unknown) => {
    setFieldValues((prev) => ({ ...prev, [nodeId]: val }));
  };

  const handleSave = async () => {
    if (!resolvedTemplateId) return;
    const body = {
      templateId: resolvedTemplateId,
      name,
      description,
      visibility,
      fieldValues,
      allowedUsernames,
    };
    if (isNew) {
      const result = await createCharacter(body).unwrap();
      navigate(`/characters/${result.id}`);
    } else {
      await updateCharacter({ id: Number(id), body }).unwrap();
      navigate(`/characters/${id}`);
    }
  };

  const addAllowedUsername = () => {
    const name = allowedInput.trim();
    if (name && !allowedUsernames.includes(name)) {
      setAllowedUsernames((prev) => [...prev, name]);
      setAllowedInput('');
    }
  };

  const isSaving = creating || updating;
  const canSave  = !!name.trim() && resolvedTemplateId != null && !isSaving;

  return (
    <div className="container py-4" style={{ maxWidth: 720 }}>
      <h1 className="h3 mb-4">
        {isNew ? 'Создать персонажа' : 'Редактировать персонажа'}
      </h1>

      {isNew && (
        <div className="mb-4">
          <label className="form-label fw-medium">
            Шаблон персонажа <span className="text-danger">*</span>
          </label>

          {templatesLoading ? (
            <div className="d-flex align-items-center gap-2 text-muted">
              <div className="spinner-border spinner-border-sm" role="status" />
              <span>Загрузка шаблонов...</span>
            </div>
          ) : !myTemplates?.length ? (
            <div className="alert alert-warning">
              У вас нет шаблонов. Сначала{' '}
              <a href="/templates/public">выберите публичный шаблон</a> или{' '}
              <a href="/templates/new">создайте свой</a>.
            </div>
          ) : (
            <select
              className="form-select"
              value={selectedTemplateId ?? ''}
              onChange={(e) =>
                setSelectedTemplateId(e.target.value ? Number(e.target.value) : null)
              }
            >
              <option value="">— Выберите шаблон —</option>
              {myTemplates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.title}
                </option>
              ))}
            </select>
          )}
        </div>
      )}

      {(!isNew || resolvedTemplateId != null) && (
        <>
          <div className="mb-3">
            <label className="form-label fw-medium">
              Имя персонажа <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              className="form-control"
              value={name}
              maxLength={150}
              onChange={(e) => setName(e.target.value)}
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-medium">Описание</label>
            <textarea
              className="form-control"
              rows={3}
              maxLength={1000}
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="mb-3">
            <label className="form-label fw-medium">Видимость</label>
            <select
              className="form-select"
              value={visibility}
              onChange={(e) => setVisibility(e.target.value as CharacterVisibility)}
            >
              <option value="PRIVATE">Приватный (только я)</option>
              <option value="PUBLIC">Публичный (все)</option>
              <option value="RESTRICTED">Ограниченный (выбранные пользователи)</option>
            </select>
          </div>

          {visibility === 'RESTRICTED' && (
            <div className="mb-3 p-3 border rounded bg-light">
              <label className="form-label fw-medium">
                Логины пользователей с доступом
              </label>
              <div className="d-flex gap-2 mb-2">
                <UsernameAutocomplete
                  value={allowedInput}
                  onChange={setAllowedInput}
                  onPick={(u) => {
                    if (!allowedUsernames.includes(u)) {
                      setAllowedUsernames((prev) => [...prev, u]);
                    }
                    setAllowedInput('');
                  }}
                  placeholder="Логин пользователя"
                  exclude={allowedUsernames}
                />
                <button
                  className="btn btn-outline-primary"
                  onClick={addAllowedUsername}
                  type="button"
                >
                  Добавить
                </button>
              </div>
              <div className="d-flex flex-wrap gap-1">
                {allowedUsernames.map((name) => (
                  <span
                    key={name}
                    className="badge bg-secondary d-flex align-items-center gap-1"
                  >
                    {name}
                    <button
                      type="button"
                      className="btn-close btn-close-white"
                      style={{ fontSize: '0.6rem' }}
                      onClick={() =>
                        setAllowedUsernames((p) => p.filter((x) => x !== name))
                      }
                      aria-label="Удалить"
                    />
                  </span>
                ))}
              </div>
            </div>
          )}

          <hr className="my-4" />

          <h2 className="h5 mb-3">Характеристики</h2>
          {templateFetching ? (
            <div className="d-flex align-items-center gap-2 text-muted py-2">
              <div className="spinner-border spinner-border-sm" role="status" />
              <span>Загрузка полей шаблона...</span>
            </div>
          ) : template?.content?.length ? (
            template.content.map((node) =>
              renderField(node, fieldValues, handleFieldChange)
            )
          ) : (
            <p className="text-muted fst-italic">
              Этот шаблон не содержит полей
            </p>
          )}

          <div className="d-flex gap-2 mt-4">
            <button
              className="btn btn-primary"
              onClick={handleSave}
              disabled={!canSave}
            >
              {isSaving && (
                <span
                  className="spinner-border spinner-border-sm me-2"
                  role="status"
                />
              )}
              {isNew ? 'Создать' : 'Сохранить'}
            </button>
            <button
              className="btn btn-outline-secondary"
              onClick={() => navigate(-1)}
            >
              Отмена
            </button>
          </div>
        </>
      )}
    </div>
  );
};

export default CharacterEditorPage;