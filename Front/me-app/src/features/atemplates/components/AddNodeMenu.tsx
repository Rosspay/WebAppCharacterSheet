import React, { useState, useRef, useEffect } from 'react';
import { useAppDispatch } from '../../../app/hooks';
import { addRootNode, addChildNode } from '../templateEditorSlice';
import type { NodeType } from '../../../types/template';

const NODE_TYPES: { type: NodeType; label: string }[] = [
  { type: 'CONTAINER',  label: '📦 Раздел (CONTAINER)' },
  { type: 'BLOCK',      label: '🔢 Блок (BLOCK)' },
  { type: 'COUNTER',    label: '⚡ Счётчик (COUNTER)' },
  { type: 'TABLE',      label: '📋 Таблица (TABLE)' },
  { type: 'TEXT_FIELD', label: '📝 Текстовое поле (TEXT_FIELD)' },
];

interface Props { parentId: string | null }

export const AddNodeMenu: React.FC<Props> = ({ parentId }) => {
  const dispatch = useAppDispatch();
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  const add = (type: NodeType) => {
    if (parentId) dispatch(addChildNode({ parentId, nodeType: type }));
    else dispatch(addRootNode(type));
    setOpen(false);
  };

  return (
    <div className="dropdown" ref={ref}>
      <button
        className="btn btn-outline-primary btn-sm"
        onClick={() => setOpen((o) => !o)}
      >
        + Добавить узел
      </button>
      {open && (
        <ul className="dropdown-menu show shadow" style={{ minWidth: 220 }}>
          {NODE_TYPES.map(({ type, label }) => (
            <li key={type}>
              <button className="dropdown-item" onClick={() => add(type)}>
                {label}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};