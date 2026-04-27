import React, { useState } from 'react';
import { useAppDispatch } from '../../../app/hooks';
import { updateNode, deleteNode } from '../templateEditorSlice';
import { AddNodeMenu } from './AddNodeMenu';
import { NodeEditorList } from './NodeEditorList';
import type { TemplateNode } from '../../../types/template';

interface Props {
  node: TemplateNode;
  parentId: string | null;
}

export const NodeEditor: React.FC<Props> = ({ node, parentId }) => {
  const dispatch = useAppDispatch();
  const [collapsed, setCollapsed] = useState(false);

  const update = (patch: Partial<TemplateNode>) =>
    dispatch(updateNode({ id: node.id, patch }));

  const badge = (
    <span className="badge bg-secondary me-2" style={{ fontSize: '0.65rem' }}>
      {node.type}
    </span>
  );

  const deleteBtn = (
    <button
      className="btn btn-sm btn-outline-danger ms-auto"
      onClick={() => dispatch(deleteNode(node.id))}
    >
      ✕
    </button>
  );

  switch (node.type) {
    case 'CONTAINER':
      return (
        <div className="card shadow-sm">
          <div className="card-header d-flex align-items-center gap-2">
            {badge}
            <input
              className="form-control form-control-sm"
              value={node.title}
              onChange={(e) => update({ title: e.target.value } as any)}
              placeholder="Название раздела"
              style={{ maxWidth: 300 }}
            />
            <button
              className="btn btn-sm btn-outline-secondary ms-2"
              onClick={() => setCollapsed((c) => !c)}
            >
              {collapsed ? '▼' : '▲'}
            </button>
            {deleteBtn}
          </div>
          {!collapsed && (
            <div className="card-body">
              <NodeEditorList nodes={node.children} parentId={node.id} />
              <div className="mt-3">
                <AddNodeMenu parentId={node.id} />
              </div>
            </div>
          )}
        </div>
      );

    case 'BLOCK':
      return (
        <div className="card shadow-sm">
          <div className="card-body d-flex align-items-center gap-2 flex-wrap">
            {badge}
            <input
              className="form-control form-control-sm"
              value={node.label}
              onChange={(e) => update({ label: e.target.value } as any)}
              placeholder="Метка"
              style={{ maxWidth: 200 }}
            />
            <input
              type="number"
              className="form-control form-control-sm"
              value={node.defaultValue ?? ''}
              onChange={(e) => update({ defaultValue: e.target.value ? Number(e.target.value) : null } as any)}
              placeholder="Значение"
              style={{ maxWidth: 100 }}
            />
            {deleteBtn}
          </div>
        </div>
      );

    case 'COUNTER':
      return (
        <div className="card shadow-sm">
          <div className="card-body d-flex align-items-center gap-2 flex-wrap">
            {badge}
            <input
              className="form-control form-control-sm"
              value={node.label}
              onChange={(e) => update({ label: e.target.value } as any)}
              placeholder="Метка"
              style={{ maxWidth: 200 }}
            />
            <input
              type="number"
              className="form-control form-control-sm"
              value={node.currentValue}
              onChange={(e) => update({ currentValue: Number(e.target.value) } as any)}
              placeholder="Текущее"
              style={{ maxWidth: 100 }}
            />
            <span className="text-muted">/</span>
            <input
              type="number"
              className="form-control form-control-sm"
              value={node.maxValue ?? ''}
              onChange={(e) => update({ maxValue: e.target.value ? Number(e.target.value) : null } as any)}
              placeholder="Макс."
              style={{ maxWidth: 100 }}
            />
            {deleteBtn}
          </div>
        </div>
      );

    case 'TABLE':
      return (
        <div className="card shadow-sm">
          <div className="card-body d-flex align-items-center gap-2 flex-wrap">
            {badge}
            <input
              className="form-control form-control-sm"
              value={node.label}
              onChange={(e) => update({ label: e.target.value } as any)}
              placeholder="Метка"
              style={{ maxWidth: 200 }}
            />
            <input
              type="number"
              className="form-control form-control-sm"
              value={node.rows}
              min={1}
              onChange={(e) => update({ rows: Number(e.target.value) } as any)}
              placeholder="Строк"
              style={{ maxWidth: 80 }}
            />
            <span className="text-muted">×</span>
            <input
              type="number"
              className="form-control form-control-sm"
              value={node.columns}
              min={1}
              onChange={(e) => update({ columns: Number(e.target.value) } as any)}
              placeholder="Столбцов"
              style={{ maxWidth: 80 }}
            />
            {deleteBtn}
          </div>
        </div>
      );

    case 'TEXT_FIELD':
      return (
        <div className="card shadow-sm">
          <div className="card-body d-flex align-items-center gap-2 flex-wrap">
            {badge}
            <input
              className="form-control form-control-sm"
              value={node.placeholder}
              onChange={(e) => update({ placeholder: e.target.value } as any)}
              placeholder="Placeholder текстового поля"
            />
            {deleteBtn}
          </div>
        </div>
      );
  }
};