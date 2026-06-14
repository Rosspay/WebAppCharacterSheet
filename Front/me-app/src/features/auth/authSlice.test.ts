
import authReducer, { setTokens, setUser, logout } from './authSlice';
import type { AuthState, TokenResponse, UserResponse } from '../../types/auth';


const emptyState: AuthState = {
  user: null,
  accessToken: null,
  refreshToken: null,
  isAuthenticated: false,
};

describe('authSlice', () => {
  beforeEach(() => {
    localStorage.clear();
  });

  test('UT-F-01: setTokens сохраняет токены и поднимает флаг', () => {
    const tokens: TokenResponse = {
      accessToken: 'ACC',
      refreshToken: 'REF',
      tokenType: 'Bearer',
      expiresIn: 3600,
    };
    const next = authReducer(emptyState, setTokens(tokens));

    expect(next.accessToken).toBe('ACC');
    expect(next.refreshToken).toBe('REF');
    expect(next.isAuthenticated).toBe(true);
    expect(localStorage.getItem('accessToken')).toBe('ACC');
    expect(localStorage.getItem('refreshToken')).toBe('REF');
  });

  test('UT-F-02: setUser кладёт данные пользователя в state', () => {
    const user: UserResponse = {
      id: 7,
      username: 'denis',
      email: 'denis@example.com',
      role: 'ROLE_USER',
    };
    const next = authReducer(emptyState, setUser(user));
    expect(next.user).toEqual(user);
  });

  test('UT-F-03: logout очищает state и localStorage', () => {
    localStorage.setItem('accessToken', 'X');
    localStorage.setItem('refreshToken', 'Y');
    const authed: AuthState = {
      user: { id: 1, username: 'u', email: 'e', role: 'ROLE_USER' },
      accessToken: 'X',
      refreshToken: 'Y',
      isAuthenticated: true,
    };

    const next = authReducer(authed, logout());

    expect(next.user).toBeNull();
    expect(next.accessToken).toBeNull();
    expect(next.refreshToken).toBeNull();
    expect(next.isAuthenticated).toBe(false);
    expect(localStorage.getItem('accessToken')).toBeNull();
    expect(localStorage.getItem('refreshToken')).toBeNull();
  });

  test('UT-F-04: неизвестное действие не меняет state', () => {
    const next = authReducer(emptyState, { type: '@@unknown' });
    expect(next).toEqual(emptyState);
  });
});
