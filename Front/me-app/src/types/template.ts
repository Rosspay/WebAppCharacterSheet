/**
 * Domain types describing character-sheet templates on the client side.
 *
 * Template nodes form a tree: a `CONTAINER` node may hold child nodes,
 * while the remaining types (`BLOCK`, `COUNTER`, `TABLE`, `TEXT_FIELD`) are
 * leaf nodes.
 * @module
 */

/** Kind of a template node. */
export type NodeType = 'CONTAINER' | 'BLOCK' | 'COUNTER' | 'TABLE' | 'TEXT_FIELD';

/** Fields common to every template node. */
export interface BaseNode {
  id: string;
  order: number;
  type: NodeType;
}

/** Container node grouping child nodes (a sheet section). */
export interface ContainerNode extends BaseNode {
  type: 'CONTAINER';
  title: string;
  children: TemplateNode[];
}

/** Numeric block (e.g. the "Strength" attribute). */
export interface BlockNode extends BaseNode {
  type: 'BLOCK';
  label: string;
  defaultValue: number | null;
}

/** Counter with current and maximum value (e.g. hit points). */
export interface CounterNode extends BaseNode {
  type: 'COUNTER';
  label: string;
  currentValue: number;
  maxValue: number | null;
}

/** Table with a fixed number of rows and columns. */
export interface TableNode extends BaseNode {
  type: 'TABLE';
  label: string;
  rows: number;
  columns: number;
}

/** Multi-line text field with a placeholder. */
export interface TextFieldNode extends BaseNode {
  type: 'TEXT_FIELD';
  placeholder: string;
}

/** Discriminated union of every template node type. */
export type TemplateNode =
  | ContainerNode
  | BlockNode
  | CounterNode
  | TableNode
  | TextFieldNode;

/** Full template card returned by the API. */
export interface TemplateResponse {
  id: number;
  ownerId: number;
  title: string;
  description: string;
  isPublic: boolean;
  content: TemplateNode[];
  createdAt: string;
  updatedAt: string;
}

/** Template card without the node tree (for listings). */
export interface TemplateSummaryResponse {
  id: number;
  ownerId: number;
  title: string;
  description: string;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

/** Request body to create or update a template. */
export interface TemplateRequest {
  title: string;
  description: string;
  isPublic: boolean;
  content: TemplateNode[];
}

/** Generic paginated response envelope. */
export interface PageResponse<T> {
  items: T[];
  total: number;
  totalPages: number;
  page: number;
  size: number;
}
