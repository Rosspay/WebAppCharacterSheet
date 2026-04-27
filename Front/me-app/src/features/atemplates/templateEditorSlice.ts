import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { TemplateNode, NodeType } from '../../types/template';
import { nanoid } from '@reduxjs/toolkit';

interface EditorState {
  id: number | null;
  title: string;
  description: string;
  isPublic: boolean;
  content: TemplateNode[];
  isDirty: boolean;
}

const initialState: EditorState = {
  id: null,
  title: '',
  description: '',
  isPublic: false,
  content: [],
  isDirty: false,
};

function makeNode(type: NodeType, order: number): TemplateNode {
  const base = { id: nanoid(), order, type } as const;
  switch (type) {
    case 'CONTAINER':  return { ...base, type: 'CONTAINER', title: 'Новый раздел', children: [] };
    case 'BLOCK':      return { ...base, type: 'BLOCK', label: 'Новый блок', defaultValue: null };
    case 'COUNTER':    return { ...base, type: 'COUNTER', label: 'Счётчик', currentValue: 0, maxValue: null };
    case 'TABLE':      return { ...base, type: 'TABLE', label: 'Таблица', rows: 3, columns: 3 };
    case 'TEXT_FIELD': return { ...base, type: 'TEXT_FIELD', placeholder: 'Введите текст...' };
  }
}

export const templateEditorSlice = createSlice({
  name: 'templateEditor',
  initialState,
  reducers: {
    loadTemplate(state, action: PayloadAction<{ id: number; title: string; description: string; isPublic: boolean; content: TemplateNode[] }>) {
      const { id, title, description, isPublic, content } = action.payload;
      state.id = id;
      state.title = title;
      state.description = description;
      state.isPublic = isPublic;
      state.content = content;
      state.isDirty = false;
    },

    resetEditor(state) {
      Object.assign(state, initialState);
    },

    setTitle(state, action: PayloadAction<string>) {
      state.title = action.payload;
      state.isDirty = true;
    },

    setDescription(state, action: PayloadAction<string>) {
      state.description = action.payload;
      state.isDirty = true;
    },

    setPublic(state, action: PayloadAction<boolean>) {
      state.isPublic = action.payload;
      state.isDirty = true;
    },

    addRootNode(state, action: PayloadAction<NodeType>) {
      const node = makeNode(action.payload, state.content.length);
      state.content.push(node);
      state.isDirty = true;
    },

    addChildNode(state, action: PayloadAction<{ parentId: string; nodeType: NodeType }>) {
      const { parentId, nodeType } = action.payload;
      const parent = state.content.find(
        (n) => n.type === 'CONTAINER' && n.id === parentId
      );
      if (parent && parent.type === 'CONTAINER') {
        const child = makeNode(nodeType, parent.children.length);
        parent.children.push(child);
        state.isDirty = true;
      }
    },

    updateNode(state, action: PayloadAction<{ id: string; patch: Partial<TemplateNode> }>) {
      const { id, patch } = action.payload;
      const idx = state.content.findIndex((n) => n.id === id);
      if (idx !== -1) {
        state.content[idx] = { ...state.content[idx], ...patch } as TemplateNode;
        state.isDirty = true;
        return;
      }
      for (const node of state.content) {
        if (node.type === 'CONTAINER') {
          const cidx = node.children.findIndex((c) => c.id === id);
          if (cidx !== -1) {
            node.children[cidx] = { ...node.children[cidx], ...patch } as TemplateNode;
            state.isDirty = true;
            return;
          }
        }
      }
    },

    deleteNode(state, action: PayloadAction<string>) {
      const id = action.payload;
      const idx = state.content.findIndex((n) => n.id === id);
      if (idx !== -1) {
        state.content.splice(idx, 1);
        state.content.forEach((n, i) => { n.order = i; });
        state.isDirty = true;
        return;
      }
      for (const node of state.content) {
        if (node.type === 'CONTAINER') {
          const cidx = node.children.findIndex((c) => c.id === id);
          if (cidx !== -1) {
            node.children.splice(cidx, 1);
            node.children.forEach((c, i) => { c.order = i; });
            state.isDirty = true;
            return;
          }
        }
      }
    },
  },
});

export const {
  loadTemplate, resetEditor,
  setTitle, setDescription, setPublic,
  addRootNode, addChildNode,
  updateNode, deleteNode,
} = templateEditorSlice.actions;