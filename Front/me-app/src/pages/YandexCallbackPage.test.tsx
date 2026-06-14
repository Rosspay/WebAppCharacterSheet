
import React from 'react';
import { Provider } from 'react-redux';
import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import YandexCallbackPage from './YandexCallbackPage';
import authReducer from '../features/auth/authSlice';
import { authApi } from '../features/auth/authApi';

function buildStore() {
  return configureStore({
    reducer: combineReducers({
      auth: authReducer,
      [authApi.reducerPath]: authApi.reducer,
    }),
    middleware: (g) => g().concat(authApi.middleware),
  });
}

describe('YandexCallbackPage', () => {
  const realFetch = global.fetch;
  beforeEach(() => { localStorage.clear(); });
  afterEach(() => { global.fetch = realFetch; });

  test('FT-F-40: успех — токены сохраняются, выполняется переход на /dashboard',
    async () => {
      global.fetch = jest.fn(() =>
        Promise.resolve(new Response(JSON.stringify({
          accessToken: 'ya-acc', refreshToken: 'ya-ref',
          tokenType: 'Bearer', expiresIn: 3600,
        }), { status: 200, headers: { 'Content-Type': 'application/json' } })),
      ) as unknown as typeof fetch;

      render(
        <Provider store={buildStore()}>
          <MemoryRouter initialEntries={['/auth/yandex/callback?code=abc']}>
            <Routes>
              <Route path="/auth/yandex/callback" element={<YandexCallbackPage />} />
              <Route path="/dashboard" element={<div>DASHBOARD</div>} />
            </Routes>
          </MemoryRouter>
        </Provider>,
      );

      await waitFor(() => {
        expect(screen.getByText('DASHBOARD')).toBeInTheDocument();
      });
      expect(localStorage.getItem('accessToken')).toBe('ya-acc');
      expect(localStorage.getItem('refreshToken')).toBe('ya-ref');
    },
  );

  test('FT-F-41: нет параметра code → видна ошибка', async () => {
    render(
      <Provider store={buildStore()}>
        <MemoryRouter initialEntries={['/auth/yandex/callback']}>
          <Routes>
            <Route path="/auth/yandex/callback" element={<YandexCallbackPage />} />
          </Routes>
        </MemoryRouter>
      </Provider>,
    );

    expect(await screen.findByText(/не передан параметр code/i)).toBeInTheDocument();
  });

  test('FT-F-42: при error_description от Яндекса → текст ошибки виден', async () => {
    render(
      <Provider store={buildStore()}>
        <MemoryRouter
          initialEntries={['/auth/yandex/callback?error=access_denied&error_description=Denied%20by%20user']}>
          <Routes>
            <Route path="/auth/yandex/callback" element={<YandexCallbackPage />} />
          </Routes>
        </MemoryRouter>
      </Provider>,
    );

    expect(await screen.findByText(/Denied by user/i)).toBeInTheDocument();
  });
});
