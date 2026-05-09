import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '../auth/authApi';
import type {
  CharacterResponse,
  CharacterSummaryResponse,
  CharacterRequest,
  VisibilityRequest,
} from '../../types/character';
import type { PageResponse } from '../../types/template';

export const charactersApi = createApi({
  reducerPath: 'charactersApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Character', 'MyCharacters'],
  endpoints: (builder) => ({
    getMyCharacters: builder.query<CharacterSummaryResponse[], void>({
      query: () => 'characters/my',
      providesTags: ['MyCharacters'],
    }),

    getAvailableCharacters: builder.query<
      PageResponse<CharacterSummaryResponse>,
      { query?: string; page?: number; size?: number }
    >({
      query: ({ query, page = 0, size = 12 } = {}) =>
        `characters/available?${query ? `query=${query}&` : ''}page=${page}&size=${size}`,
      providesTags: (result) =>
        result
          ? [
              ...result.items.map(({ id }) => ({ type: 'Character' as const, id })),
              { type: 'Character', id: 'AVAILABLE_LIST' },
            ]
          : [{ type: 'Character', id: 'AVAILABLE_LIST' }],
    }),

    getCharacterById: builder.query<CharacterResponse, number>({
      query: (id) => `characters/${id}`,
      providesTags: (r, e, id) => [{ type: 'Character', id }],
    }),

    createCharacter: builder.mutation<CharacterResponse, CharacterRequest>({
      query: (body) => ({ url: 'characters', method: 'POST', body }),
      invalidatesTags: ['MyCharacters'],
    }),

    updateCharacter: builder.mutation<CharacterResponse, { id: number; body: CharacterRequest }>({
      query: ({ id, body }) => ({ url: `characters/${id}`, method: 'PUT', body }),
      invalidatesTags: (r, e, { id }) => [
        { type: 'Character', id },
        'MyCharacters',
      ],
    }),

    setVisibility: builder.mutation<CharacterResponse, { id: number; body: VisibilityRequest }>({
      query: ({ id, body }) => ({ url: `characters/${id}/visibility`, method: 'PATCH', body }),
      invalidatesTags: (r, e, { id }) => [
        { type: 'Character', id },
        { type: 'Character', id: 'AVAILABLE_LIST' },
        'MyCharacters',
      ],
    }),

    deleteCharacter: builder.mutation<void, number>({
      query: (id) => ({ url: `characters/${id}`, method: 'DELETE' }),
      invalidatesTags: (r, e, id) => [
        { type: 'Character', id },
        { type: 'Character', id: 'AVAILABLE_LIST' },
        'MyCharacters',
      ],
    }),
  }),
});

export const {
  useGetMyCharactersQuery,
  useGetAvailableCharactersQuery,
  useGetCharacterByIdQuery,
  useCreateCharacterMutation,
  useUpdateCharacterMutation,
  useSetVisibilityMutation,
  useDeleteCharacterMutation,
} = charactersApi;