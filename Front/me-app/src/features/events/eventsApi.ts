/**
 * RTK Query API for events: CRUD, invitations by username, participation
 * applications, the owner/participant lists and the public OPEN feed.
 *
 * Cache tagging keeps the various event views (`MY`, `PARTICIPATING`,
 * `OPEN` feed and individual `Event:<id>`) coherent after mutations.
 * @module
 */
import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '../auth/authApi';
import type {
  EventRequest,
  EventResponse,
  EventSummaryResponse,
  InviteRequest,
  InvitationResponse,
  ApplicationRequest,
  ApplicationResponse,
  StatusRequest,
} from '../../types/event';
import type { PageResponse } from '../../types/template';

export const eventsApi = createApi({
  reducerPath: 'eventsApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Event', 'MyEvents', 'Participating', 'OpenEvents',
    'Invitations', 'MyInvitations', 'Applications'],
  endpoints: (builder) => ({
    getMyEvents: builder.query<EventSummaryResponse[], void>({
      query: () => 'events/my',
      providesTags: ['MyEvents'],
    }),

    getParticipating: builder.query<EventSummaryResponse[], void>({
      query: () => 'events/participating',
      providesTags: ['Participating'],
    }),

    getOpenEvents: builder.query<
      PageResponse<EventSummaryResponse>,
      { query?: string; page?: number; size?: number }
    >({
      query: ({ query, page = 0, size = 12 } = {}) =>
        `events/open?${query ? `query=${encodeURIComponent(query)}&` : ''}` +
        `page=${page}&size=${size}`,
      providesTags: ['OpenEvents'],
    }),

    getEventById: builder.query<EventResponse, number>({
      query: (id) => `events/${id}`,
      providesTags: (r, e, id) => [{ type: 'Event', id }],
    }),

    createEvent: builder.mutation<EventResponse, EventRequest>({
      query: (body) => ({ url: 'events', method: 'POST', body }),
      invalidatesTags: ['MyEvents', 'OpenEvents'],
    }),

    updateEvent: builder.mutation<EventResponse, { id: number; body: EventRequest }>({
      query: ({ id, body }) => ({ url: `events/${id}`, method: 'PUT', body }),
      invalidatesTags: (r, e, { id }) => [
        { type: 'Event', id }, 'MyEvents', 'OpenEvents',
      ],
    }),

    deleteEvent: builder.mutation<void, number>({
      query: (id) => ({ url: `events/${id}`, method: 'DELETE' }),
      invalidatesTags: ['MyEvents', 'OpenEvents', 'Participating'],
    }),

    inviteUser: builder.mutation<InvitationResponse, { id: number; body: InviteRequest }>({
      query: ({ id, body }) => ({
        url: `events/${id}/invitations`, method: 'POST', body,
      }),
      invalidatesTags: (r, e, { id }) => [
        { type: 'Event', id }, 'Invitations', 'MyInvitations',
      ],
    }),

    getInvitations: builder.query<InvitationResponse[], number>({
      query: (id) => `events/${id}/invitations`,
      providesTags: ['Invitations'],
    }),

    cancelInvitation: builder.mutation<void, { id: number; invitationId: number }>({
      query: ({ id, invitationId }) => ({
        url: `events/${id}/invitations/${invitationId}`, method: 'DELETE',
      }),
      invalidatesTags: ['Invitations', 'MyInvitations'],
    }),

    getMyInvitations: builder.query<InvitationResponse[], void>({
      query: () => 'events/invitations/my',
      providesTags: ['MyInvitations'],
    }),

    respondToInvitation: builder.mutation<
      InvitationResponse,
      { invitationId: number; body: StatusRequest }
    >({
      query: ({ invitationId, body }) => ({
        url: `events/invitations/${invitationId}`, method: 'PATCH', body,
      }),
      invalidatesTags: ['MyInvitations', 'Participating'],
    }),

    apply: builder.mutation<ApplicationResponse, { id: number; body: ApplicationRequest }>({
      query: ({ id, body }) => ({
        url: `events/${id}/applications`, method: 'POST', body,
      }),
      invalidatesTags: (r, e, { id }) => [
        { type: 'Event', id }, 'Applications',
      ],
    }),

    getApplications: builder.query<ApplicationResponse[], number>({
      query: (id) => `events/${id}/applications`,
      providesTags: ['Applications'],
    }),

    reviewApplication: builder.mutation<
      ApplicationResponse,
      { id: number; applicationId: number; body: StatusRequest }
    >({
      query: ({ id, applicationId, body }) => ({
        url: `events/${id}/applications/${applicationId}`,
        method: 'PATCH',
        body,
      }),
      invalidatesTags: ['Applications', 'Participating'],
    }),
  }),
});

export const {
  useGetMyEventsQuery,
  useGetParticipatingQuery,
  useGetOpenEventsQuery,
  useGetEventByIdQuery,
  useCreateEventMutation,
  useUpdateEventMutation,
  useDeleteEventMutation,
  useInviteUserMutation,
  useGetInvitationsQuery,
  useCancelInvitationMutation,
  useGetMyInvitationsQuery,
  useRespondToInvitationMutation,
  useApplyMutation,
  useGetApplicationsQuery,
  useReviewApplicationMutation,
} = eventsApi;
