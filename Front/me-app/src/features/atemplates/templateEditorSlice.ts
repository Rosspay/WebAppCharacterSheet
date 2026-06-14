/**
 * Redux slice for the template editor. All actions perform immutable tree
 * operations (add, update, delete) on the node array; Immer takes care of
 * the structural sharing.
 *
 * The slice tracks an `isDirty` flag that is set on every mutation and
 * cleared by `loadTemplate` / `resetEditor`, so the UI can prompt the user
 * before navigating away from unsaved changes.
 * @module
 */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { TemplateNode, NodeType } from '../../types/template';
import { nanoid } from '@reduxjs/toolkit';

/** Internal shape of the template editor state. */
export interface EditorState {
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

/**
 * Builds a fresh template node with sensible defaults for the given type.
 * The `order` is supplied by the caller so the new node can be appended at
 * the end of its siblings.
 */
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
    /** Replaces the entire editor state with the contents of an existing template and clears the dirty flag. */
    loadTemplate(state, action: PayloadAction<{ id: number; title: string; description: string; isPublic: boolean; content: TemplateNode[] }>) {
      const { id, title, description, isPublic, content } = action.payload;
      state.id = id;
      state.title = title;
      state.description = description;
      state.isPublic = isPublic;
      state.content = content;
      state.isDirty = false;
    },

    /** Resets the editor back to a blank template (no id, empty tree). */
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

    /** Appends a node of the given type to the root of the template tree. */
    addRootNode(state, action: PayloadAction<NodeType>) {
      const node = makeNode(action.payload, state.content.length);
      state.content.push(node);
      state.isDirty = true;
    },

    /**
     * Appends a node inside the container with `parentId`. No-op if the
     * parent is missing or is not a CONTAINER (the only nestable type).
     */
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

    /**
     * Shallow-merges `patch` into the node with `id`. Searches the root
     * tier first, then descends into containers; first match wins.
     */
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

    /**
     * Removes the node with the given id and renumbers `order` of its
     * remaining siblings so the sequence stays gap-free. Searches the root
     * tier first, then descends into containers.
     */
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