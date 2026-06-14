/**
 * Redux slice that holds the authenticated session state: access and
 * refresh tokens, the current user profile and an `isAuthenticated` flag.
 *
 * The tokens are mirrored to `localStorage` so a page reload restores the
 * session; `logout` wipes both Redux state and storage. The slice exposes
 * three action creators consumed by the UI and by RTK Query layers:
 * `setTokens`, `setUser`, `logout`.
 * @module
 */
import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { AuthState, TokenResponse, UserResponse } from '../../types/auth';

const initialState: AuthState = {
  user: null,
  accessToken: localStorage.getItem('accessToken'),
  refreshToken: localStorage.getItem('refreshToken'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
};

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    /**
     * Stores a freshly issued token pair and marks the user as authenticated.
     * Mirrors both tokens to `localStorage` so the session survives a reload.
     */
    setTokens(state, action: PayloadAction<TokenResponse>) {
      state.accessToken = action.payload.accessToken;
      state.refreshToken = action.payload.refreshToken;
      state.isAuthenticated = true;
      localStorage.setItem('accessToken', action.payload.accessToken);
      localStorage.setItem('refreshToken', action.payload.refreshToken);
    },
    /**
     * Stores the resolved user profile (usually emitted by `getMe`).
     * Token state is intentionally not modified here.
     */
    setUser(state, action: PayloadAction<UserResponse>) {
      state.user = action.payload;
    },
    /**
     * Wipes the in-memory session and the persisted tokens from
     * `localStorage`. Combined with the root-level `rootReducer` reset,
     * this guarantees no stale slice state survives across users.
     */
    logout(state) {
      state.user = null;
      state.accessToken = null;
      state.refreshToken = null;
      state.isAuthenticated = false;
      localStorage.removeItem('accessToken');
      localStorage.removeItem('refreshToken');
    },
  },
});

export const { setTokens, setUser, logout } = authSlice.actions;
export default authSlice.reducer;