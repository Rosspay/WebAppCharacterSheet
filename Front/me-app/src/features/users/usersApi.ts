/**
 * RTK Query API for username search.
 *
 * Backs the `UsernameAutocomplete` component on the invitation and
 * access-grant screens. Uses the lazy hook variant so the request is
 * issued explicitly by the debounced effect rather than on every render.
 * @module
 */
import { createApi } from '@reduxjs/toolkit/query/react';
import { baseQueryWithReauth } from '../auth/authApi';


export const usersApi = createApi({
  reducerPath: 'usersApi',
  baseQuery: baseQueryWithReauth,
  endpoints: (builder) => ({
    searchUsernames: builder.query<string[], { q: string; limit?: number }>({
      query: ({ q, limit = 10 }) =>
        `users/search?q=${encodeURIComponent(q)}&limit=${limit}`,
    }),
  }),
});

export const { useSearchUsernamesQuery, useLazySearchUsernamesQuery } = usersApi;
