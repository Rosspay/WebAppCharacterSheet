/**
 * RTK Query API for authentication endpoints.
 *
 * The custom `baseQueryWithReauth` wraps the standard `fetchBaseQuery`:
 * when a request returns 401 it transparently calls `POST /auth/refresh`
 * with the stored refresh token, persists the new token pair via
 * `setTokens` and retries the original request once. If the refresh fails
 * (or no refresh token is available) the user is logged out via
 * `dispatch(logout())`.
 *
 * @module
 */
import {
  createApi,
  fetchBaseQuery,
  BaseQueryFn,
  FetchArgs,
  FetchBaseQueryError,
} from '@reduxjs/toolkit/query/react';
import { RootState } from '../../app/store';
import { setTokens, logout } from './authSlice';
import {
  LoginRequest,
  RegisterRequest,
  TokenResponse,
  RefreshRequest,
  UserResponse,
  YandexCallbackRequest,
} from '../../types/auth';

const baseQuery = fetchBaseQuery({
  baseUrl: '/api/v1',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.accessToken;
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  },
});


/**
 * Base query that transparently rotates the refresh token on 401.
 *
 * Algorithm:
 * 1. Forward the original request through `baseQuery`.
 * 2. If the server returns 401 and a refresh token is available, POST it to
 *    `/refresh`; on success dispatch `setTokens` and retry the original
 *    request once with the new access token.
 * 3. If the refresh itself fails (or there is no refresh token to begin
 *    with), dispatch `logout` so the UI returns the user to the sign-in
 *    screen instead of looping forever.
 *
 * Note: this implementation is best-effort and not single-flight — two
 * concurrent 401s may both trigger a refresh; the second one will succeed
 * with the already-rotated pair because `setTokens` is idempotent for the
 * same response.
 */
export const baseQueryWithReauth: BaseQueryFn<
  string | FetchArgs,
  unknown,
  FetchBaseQueryError
> = async (args, api, extraOptions) => {
  let result = await baseQuery(args, api, extraOptions);

  if (result.error?.status === 401) {
    const state = api.getState() as RootState;
    const refreshToken = state.auth.refreshToken;

    if (refreshToken) {
      const refreshResult = await baseQuery(
        {
          url: '/refresh',
          method: 'POST',
          body: { refreshToken } satisfies RefreshRequest,
        },
        api,
        extraOptions
      );

      if (refreshResult.data) {
        api.dispatch(setTokens(refreshResult.data as TokenResponse));
        result = await baseQuery(args, api, extraOptions);
      } else {
        api.dispatch(logout());
      }
    } else {
      api.dispatch(logout());
    }
  }

  return result;
};

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: baseQueryWithReauth,
  endpoints: (builder) => ({
    register: builder.mutation<TokenResponse, RegisterRequest>({
      query: (body) => ({ url: '/auth/register', method: 'POST', body }),
    }),
    login: builder.mutation<TokenResponse, LoginRequest>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
    }),
    refresh: builder.mutation<TokenResponse, RefreshRequest>({
      query: (body) => ({ url: '/auth/refresh', method: 'POST', body }),
    }),
    logout: builder.mutation<void, void>({
      query: () => ({ url: '/auth/logout', method: 'POST' }),
    }),
    getMe: builder.query<UserResponse, void>({
      query: () => '/auth/me',
    }),
    yandexCallback: builder.mutation<TokenResponse, YandexCallbackRequest>({
      query: (body) => ({ url: '/auth/yandex/callback', method: 'POST', body }),
    }),
  }),
});

export const {
  useRegisterMutation,
  useLoginMutation,
  useRefreshMutation,
  useLogoutMutation,
  useGetMeQuery,
  useYandexCallbackMutation,
} = authApi;