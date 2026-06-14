/**
 * Domain types describing characters on the client side.
 * @module
 */
import { TemplateNode } from './template';

/** Character visibility mode: private, public, or restricted to a list of usernames. */
export type CharacterVisibility = 'PRIVATE' | 'PUBLIC' | 'RESTRICTED';

/** Character card used in lists (without field values). */
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

/** Full character card with field values and the list of usernames granted access. */
export interface CharacterResponse extends CharacterSummaryResponse {
  fieldValues: Record<string, unknown>;
  allowedUsernames: string[];
}

/** Request body to create or update a character. */
export interface CharacterRequest {
  templateId: number;
  name: string;
  description: string;
  visibility: CharacterVisibility;
  fieldValues: Record<string, unknown>;
  allowedUsernames: string[];
}

/** Request body to change a character's visibility and access list. */
export interface VisibilityRequest {
  visibility: CharacterVisibility;
  allowedUsernames: string[];
}
