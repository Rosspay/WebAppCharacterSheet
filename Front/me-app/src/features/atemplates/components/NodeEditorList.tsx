import React from 'react';
import type { TemplateNode } from '../../../types/template';
import { NodeEditor } from './NodeEditor';

interface Props {
  nodes: TemplateNode[];
  parentId: string | null;
}

export const NodeEditorList: React.FC<Props> = ({ nodes, parentId }) => {
  if (!nodes.length) {
    return (
      <div className="text-center text-muted py-4 border rounded">
        Нет узлов. Добавьте первый элемент.
      </div>
    );
  }

  return (
    <div className="d-flex flex-column gap-3">
      {[...nodes]
        .sort((a, b) => a.order - b.order)
        .map((node) => (
          <NodeEditor key={node.id} node={node} parentId={parentId} />
        ))}
    </div>
  );
};