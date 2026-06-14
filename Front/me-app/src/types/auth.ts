/**
 * Authentication-related DTO and state types shared between RTK Query
 * APIs, the auth slice and React components.
 * @module
 */

/** Registration request payload (username, e-mail, password). */
export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
}

/** Login request payload (username + password). */
export interface LoginRequest {
  username: string;
  password: string;
}

/** Response of a successful authentication: access/refresh tokens and TTL. */
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}

/** Body of `POST /auth/refresh`: refresh token to rotate. */
export interface RefreshRequest {
  refreshToken: string;
}

/** Body of `POST /auth/yandex/callback`: authorization code and the redirect
 * URI used during the Yandex authorization request. */
export interface YandexCallbackRequest {
  code: string;
  redirectUri?: string;
}

/** Authenticated user profile as returned by `GET /auth/me`. */
export interface UserResponse {
  id: number;
  username: string;
  email: string;
  role: string;
}

/** Shape of the `auth` Redux slice. */
export interface AuthState {
  user: UserResponse | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
}
