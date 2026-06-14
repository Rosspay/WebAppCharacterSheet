import React, { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../app/hooks';
import {
  loadTemplate, resetEditor,
  setTitle, setDescription, setPublic,
} from '../features/atemplates/templateEditorSlice';
import {
  useGetTemplateByIdQuery,
  useCreateTemplateMutation,
  useUpdateTemplateMutation,
} from '../features/atemplates/templatesApi';
import { NodeEditorList } from '../features/atemplates/components/NodeEditorList';
import { AddNodeMenu } from '../features/atemplates/components/AddNodeMenu';

const TemplateEditorPage: React.FC = () => {
  const { id } = useParams<{ id?: string }>();
  const isNew = !id;
  const navigate = useNavigate();
  const dispatch = useAppDispatch();

  const editor = useAppSelector((s) => s.templateEditor);
  const { data: existing } = useGetTemplateByIdQuery(Number(id), { skip: isNew });
  const [createTemplate, { isLoading: creating }] = useCreateTemplateMutation();
  const [updateTemplate, { isLoading: updating }] = useUpdateTemplateMutation();

  useEffect(() => {
    if (existing) {
      dispatch(loadTemplate({
        id: existing.id,
        title: existing.title,
        description: existing.description,
        isPublic: existing.isPublic,
        content: existing.content,
      }));
    } else if (isNew) {
      dispatch(resetEditor());
    }
  }, [existing, isNew, dispatch]);

  const isSaving = creating || updating;

  const handleSave = async () => {
    const body = {
      title: editor.title,
      description: editor.description,
      isPublic: editor.isPublic,
      content: editor.content,
    };
    try {
      if (isNew) {
        const result = await createTemplate(body).unwrap();
        navigate(`/templates/${result.id}/edit`, { replace: true });
      } else {
        await updateTemplate({ id: Number(id), body }).unwrap();
      }
    } catch {

    }
  };

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-center mb-4">
        <h1 className="h3 mb-0">{isNew ? 'Новый шаблон' : 'Редактировать шаблон'}</h1>
        <button
          className="btn btn-primary"
          onClick={handleSave}
          disabled={isSaving || (!editor.isDirty && !isNew)}
        >
          {isSaving ? 'Сохранение...' : 'Сохранить'}
        </button>
      </div>

      {}
      <div className="card mb-4 shadow-sm">
        <div className="card-body">
          <div className="mb-3">
            <label className="form-label fw-semibold">Название</label>
            <input
              type="text"
              className="form-control"
              value={editor.title}
              onChange={(e) => dispatch(setTitle(e.target.value))}
              placeholder="Название шаблона"
            />
          </div>
          <div className="mb-3">
            <label className="form-label fw-semibold">Описание</label>
            <textarea
              className="form-control"
              rows={2}
              value={editor.description}
              onChange={(e) => dispatch(setDescription(e.target.value))}
              placeholder="Краткое описание шаблона"
            />
          </div>
          <div className="form-check">
            <input
              type="checkbox"
              className="form-check-input"
              id="isPublic"
              checked={editor.isPublic}
              onChange={(e) => dispatch(setPublic(e.target.checked))}
            />
            <label className="form-check-label" htmlFor="isPublic">
              Публичный шаблон
            </label>
          </div>
        </div>
      </div>

      {}
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Содержимое</h2>
        <AddNodeMenu parentId={null} />
      </div>

      <NodeEditorList nodes={editor.content} parentId={null} />
    </div>
  );
};

export default TemplateEditorPage;