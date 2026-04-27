import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '../auth/authApi';
import type {
  TemplateResponse,
  TemplateSummaryResponse,
  TemplateRequest,
  PageResponse,
} from '../../types/template';

export const templatesApi = createApi({
  reducerPath: 'templatesApi',
  baseQuery: baseQueryWithReauth,
  tagTypes: ['Template', 'MyTemplates'],
  endpoints: (builder) => ({
    getMyTemplates: builder.query<TemplateSummaryResponse[], void>({
      query: () => '/templates/my',
      providesTags: ['MyTemplates'],
    }),

    getPublicTemplates: builder.query<
      PageResponse<TemplateSummaryResponse>,
      { query?: string; page?: number; size?: number }
    >({
      query: ({ query = '', page = 0, size = 20 }) =>
        `/templates/public?query=${query}&page=${page}&size=${size}`,
      providesTags: ['Template'],
    }),

    getTemplateById: builder.query<TemplateResponse, number>({
      query: (id) => `/templates/${id}`,
      providesTags: (_r, _e, id) => [{ type: 'Template', id }],
    }),

    createTemplate: builder.mutation<TemplateResponse, TemplateRequest>({
      query: (body) => ({ url: '/templates', method: 'POST', body }),
      invalidatesTags: ['MyTemplates'],
    }),

    updateTemplate: builder.mutation<
      TemplateResponse,
      { id: number; body: TemplateRequest }
    >({
      query: ({ id, body }) => ({
        url: `/templates/${id}`,
        method: 'PUT',
        body,
      }),
      invalidatesTags: (_r, _e, { id }) => [
        { type: 'Template', id },
        'MyTemplates',
      ],
    }),

    togglePublish: builder.mutation<TemplateResponse, number>({
      query: (id) => ({ url: `/templates/${id}/publish`, method: 'PATCH' }),
      invalidatesTags: (_r, _e, id) => [
        { type: 'Template', id },
        'MyTemplates',
      ],
    }),

    deleteTemplate: builder.mutation<void, number>({
      query: (id) => ({ url: `/templates/${id}`, method: 'DELETE' }),
      invalidatesTags: ['MyTemplates'],
    }),
  }),
});

export const {
  useGetMyTemplatesQuery,
  useGetPublicTemplatesQuery,
  useGetTemplateByIdQuery,
  useCreateTemplateMutation,
  useUpdateTemplateMutation,
  useTogglePublishMutation,
  useDeleteTemplateMutation,
} = templatesApi;