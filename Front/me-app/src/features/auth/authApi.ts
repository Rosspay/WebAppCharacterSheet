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
} from '../../types/auth';

const baseQuery = fetchBaseQuery({
  baseUrl: '/api/v1/auth',
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.accessToken;
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    return headers;
  },
});


const baseQueryWithReauth: BaseQueryFn<
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
      query: (body) => ({ url: '/register', method: 'POST', body }),
    }),
    login: builder.mutation<TokenResponse, LoginRequest>({
      query: (body) => ({ url: '/login', method: 'POST', body }),
    }),
    refresh: builder.mutation<TokenResponse, RefreshRequest>({
      query: (body) => ({ url: '/refresh', method: 'POST', body }),
    }),
    logout: builder.mutation<void, void>({
      query: () => ({ url: '/logout', method: 'POST' }),
    }),
    getMe: builder.query<UserResponse, void>({
      query: () => '/me',
    }),
  }),
});

export const {
  useRegisterMutation,
  useLoginMutation,
  useRefreshMutation,
  useLogoutMutation,
  useGetMeQuery,
} = authApi;