import { TemplateNode } from './template';

export type CharacterVisibility = 'PRIVATE' | 'PUBLIC' | 'RESTRICTED';

export interface CharacterSummaryResponse {
  id: number;
  ownerId: number;
  templateId: number;
  name: string;
  description: string;
  visibility: CharacterVisibility;
  createdAt: string;
  updatedAt: string;
}

export interface CharacterResponse extends CharacterSummaryResponse {
  fieldValues: Record<string, unknown>;
  allowedUserIds: number[];
}

export interface CharacterRequest {
  templateId: number;
  name: string;
  description: string;
  visibility: CharacterVisibility;
  fieldValues: Record<string, unknown>;
  allowedUserIds: number[];
}

export interface VisibilityRequest {
  visibility: CharacterVisibility;
  allowedUserIds: number[];
}