export type NodeType = 'CONTAINER' | 'BLOCK' | 'COUNTER' | 'TABLE' | 'TEXT_FIELD';

export interface BaseNode {
  id: string;
  order: number;
  type: NodeType;
}

export interface ContainerNode extends BaseNode {
  type: 'CONTAINER';
  title: string;
  children: TemplateNode[];
}

export interface BlockNode extends BaseNode {
  type: 'BLOCK';
  label: string;
  defaultValue: number | null;
}

export interface CounterNode extends BaseNode {
  type: 'COUNTER';
  label: string;
  currentValue: number;
  maxValue: number | null;
}

export interface TableNode extends BaseNode {
  type: 'TABLE';
  label: string;
  rows: number;
  columns: number;
}

export interface TextFieldNode extends BaseNode {
  type: 'TEXT_FIELD';
  placeholder: string;
}

export type TemplateNode =
  | ContainerNode
  | BlockNode
  | CounterNode
  | TableNode
  | TextFieldNode;

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

export interface TemplateSummaryResponse {
  id: number;
  ownerId: number;
  title: string;
  description: string;
  isPublic: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TemplateRequest {
  title: string;
  description: string;
  isPublic: boolean;
  content: TemplateNode[];
}

export interface PageResponse<T> {
  items: T[];
  total: number;
  totalPages: number;
  page: number;
  size: number;
}