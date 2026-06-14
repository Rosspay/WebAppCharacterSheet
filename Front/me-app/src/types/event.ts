/**
 * Domain types describing events on the client side.
 * @module
 */

/** Event visibility kind: open (`OPEN`) or invitation-only (`CLOSED`). */
export type EventType = 'OPEN' | 'CLOSED';

/** Lifecycle status of an invitation to an event. */
export type InvitationStatus = 'INVITED' | 'ACCEPTED' | 'DECLINED';

/** Lifecycle status of a user application to an open event. */
export type ApplicationStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED';

/** Request body to create or update an event. */
export interface EventRequest {
  title: string;
  description?: string;
  location?: string;
  startsAt: string;
  endsAt?: string;
  eventType: EventType;
  allowApplications: boolean;
}

/** Event card used in feeds and lists. */
export interface EventSummaryResponse {
  id: number;
  ownerId: number;
  title: string;
  location: string | null;
  startsAt: string;
  endsAt: string | null;
  eventType: EventType;
  allowApplications: boolean;
  createdAt: string;
}

/** Full event card with description. */
export interface EventResponse extends EventSummaryResponse {
  description: string | null;
  updatedAt: string;
}

/** Request body to invite a user by username. */
export interface InviteRequest {
  username: string;
}

/** Request body of an application to participate (optional message). */
export interface ApplicationRequest {
  message?: string;
}

/** Generic status-change request body (e.g. invitation/application status). */
export interface StatusRequest {
  status: string;
}

/** Invitation of a user to an event. */
export interface InvitationResponse {
  id: number;
  eventId: number;
  username: string;
  status: InvitationStatus;
  createdAt: string;
}

/** Application of a user to participate in an open event. */
export interface ApplicationResponse {
  id: number;
  eventId: number;
  username: string;
  message: string | null;
  status: ApplicationStatus;
  createdAt: string;
}
