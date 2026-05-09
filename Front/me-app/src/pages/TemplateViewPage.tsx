import React from 'react';
import { useParams, Link, useNavigate } from 'react-router-dom';
import { useGetTemplateByIdQuery, useTogglePublishMutation } from '../features/atemplates/templatesApi';
import { NodeViewer } from '../features/atemplates/components/NodeViewer';
import { useAppSelector } from '../app/hooks';

const TemplateViewPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const currentUserId = useAppSelector((s) => s.auth.user?.id);

  const { data: template, isLoading, isError } = useGetTemplateByIdQuery(Number(id));
  const [togglePublish] = useTogglePublishMutation();

  if (isLoading) return (
    <div className="container py-5 text-center">
      <div className="spinner-border text-primary" role="status" />
    </div>
  );

  if (isError || !template) return (
    <div className="container py-5">
      <div className="alert alert-danger">Шаблон не найден</div>
    </div>
  );

  const isOwner = currentUserId === template.ownerId;

  return (
    <div className="container py-4">
      <div className="d-flex justify-content-between align-items-start mb-4 flex-wrap gap-2">
        <div>
          <h1 className="h3 mb-1">{template.title}</h1>
          <p className="text-muted mb-0">{template.description}</p>
        </div>
        <div className="d-flex gap-2 flex-wrap">
          {isOwner && (
            <>
              <button
                className="btn btn-outline-secondary btn-sm"
                onClick={() => togglePublish(template.id)}
              >
                {template.isPublic ? 'Скрыть' : 'Опубликовать'}
              </button>
              <Link to={`/templates/${template.id}/edit`} className="btn btn-primary btn-sm">
                Редактировать
              </Link>
            </>
          )}
          <button 
            className="btn btn-outline-success btn-sm"
            onClick={() => navigate(`/characters/new?templateId=${template.id}`)}
          >
            Использовать как персонажа
          </button>
        </div>
      </div>

      <div className="row g-3">
        {template.content.map((node) => (
          <div key={node.id} className="col-12">
            <NodeViewer node={node} />
          </div>
        ))}
      </div>
    </div>
  );
};

export default TemplateViewPage;