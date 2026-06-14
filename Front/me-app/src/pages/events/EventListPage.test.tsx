
import React from 'react';
import { Provider } from 'react-redux';
import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import EventListPage from './EventListPage';
import authReducer from '../../features/auth/authSlice';
import { authApi } from '../../features/auth/authApi';
import { eventsApi } from '../../features/events/eventsApi';

function buildStore() {
  return configureStore({
    reducer: combineReducers({
      auth: authReducer,
      [authApi.reducerPath]: authApi.reducer,
      [eventsApi.reducerPath]: eventsApi.reducer,
    }),
    middleware: (g) => g().concat(authApi.middleware).concat(eventsApi.middleware),
  });
}

interface FetchCase {
  match: (url: string) => boolean;
  body: unknown;
}

function mockFetch(cases: FetchCase[]) {
  return jest.fn((input: RequestInfo | URL) => {
    const url = typeof input === 'string' ? input : (input as Request).url ?? String(input);
    const match = cases.find((c) => c.match(url));
    const body = match ? match.body : [];
    return Promise.resolve(new Response(JSON.stringify(body), {
      status: 200,
      headers: { 'Content-Type': 'application/json' },
    }));
  });
}

describe('EventListPage', () => {
  const realFetch = global.fetch;

  afterEach(() => {
    global.fetch = realFetch;
  });

  test('FT-F-30: выводит загруженные «Мои» и «Открытые» мероприятия', async () => {
    global.fetch = mockFetch([
      {
        match: (u) => u.endsWith('/events/my'),
        body: [{
          id: 1, ownerId: 9, title: 'Кампания дракона',
          location: 'Москва', startsAt: '2030-01-01T19:00',
          endsAt: null, eventType: 'OPEN', allowApplications: true,
          createdAt: '2030-01-01T10:00',
        }],
      },
      { match: (u) => u.endsWith('/events/participating'), body: [] },
      {
        match: (u) => u.includes('/events/open'),
        body: {
          items: [{
            id: 2, ownerId: 11, title: 'Открытое собрание',
            location: null, startsAt: '2030-02-02T18:00',
            endsAt: null, eventType: 'OPEN', allowApplications: true,
            createdAt: '2030-01-15T10:00',
          }],
          totalItems: 1, totalPages: 1, currentPage: 0, pageSize: 12,
        },
      },
      { match: (u) => u.endsWith('/events/invitations/my'), body: [] },
    ]) as unknown as typeof fetch;

    render(
      <Provider store={buildStore()}>
        <MemoryRouter>
          <EventListPage />
        </MemoryRouter>
      </Provider>,
    );

    expect(await screen.findByText('Кампания дракона')).toBeInTheDocument();
    expect(await screen.findByText('Открытое собрание')).toBeInTheDocument();
  });

  test('FT-F-31: для пустых коллекций показывает плейсхолдеры', async () => {
    global.fetch = mockFetch([
      { match: () => true, body: [] },

    ]) as unknown as typeof fetch;


    const orig = global.fetch;
    global.fetch = jest.fn((input: RequestInfo | URL) => {
      const url = typeof input === 'string' ? input : (input as Request).url;
      if (url.includes('/events/open')) {
        return Promise.resolve(new Response(JSON.stringify({
          items: [], totalItems: 0, totalPages: 0, currentPage: 0, pageSize: 12,
        }), { status: 200, headers: { 'Content-Type': 'application/json' } }));
      }
      return (orig as any)(input);
    }) as unknown as typeof fetch;

    render(
      <Provider store={buildStore()}>
        <MemoryRouter>
          <EventListPage />
        </MemoryRouter>
      </Provider>,
    );

    await waitFor(() => {
      expect(screen.getByText(/Вы пока не создавали мероприятия/i))
        .toBeInTheDocument();
    });
    expect(screen.getByText(/Открытых мероприятий нет/i)).toBeInTheDocument();
  });

  test('FT-F-32: клик «+ Создать» уводит на /events/new', async () => {
    global.fetch = mockFetch([
      { match: (u) => u.includes('/events/open'),
        body: { items: [], totalItems: 0, totalPages: 0, currentPage: 0, pageSize: 12 } },
      { match: () => true, body: [] },
    ]) as unknown as typeof fetch;

    render(
      <Provider store={buildStore()}>
        <MemoryRouter initialEntries={['/events']}>
          <Routes>
            <Route path="/events" element={<EventListPage />} />
            <Route path="/events/new" element={<div>СОЗДАНИЕ</div>} />
          </Routes>
        </MemoryRouter>
      </Provider>,
    );


    await screen.findByRole('button', { name: /Создать/ });
    await userEvent.click(screen.getByRole('button', { name: /Создать/ }));
    expect(await screen.findByText('СОЗДАНИЕ')).toBeInTheDocument();
  });
});
