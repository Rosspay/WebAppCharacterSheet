import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { YandexLoginButton } from './YandexLoginButton';

describe('YandexLoginButton', () => {
  const originalEnv = process.env;
  const originalLocation = window.location;

  const setMockLocation = (href: string, origin: string) => {
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: { href, origin } as unknown as Location,
    });
  };

  beforeEach(() => {
    jest.resetModules();
    process.env = { ...originalEnv };
    setMockLocation('', 'http://localhost:3000');
  });

  afterAll(() => {
    process.env = originalEnv;
    Object.defineProperty(window, 'location', {
      configurable: true,
      writable: true,
      value: originalLocation,
    });
  });

  test('UT-F-20: без CLIENT_ID показывает alert, не редиректит', async () => {
    delete process.env.REACT_APP_YANDEX_CLIENT_ID;
    const alertSpy = jest.spyOn(window, 'alert').mockImplementation(() => {});

    render(<YandexLoginButton />);
    await userEvent.click(screen.getByRole('button'));

    expect(alertSpy).toHaveBeenCalledWith(
      expect.stringContaining('REACT_APP_YANDEX_CLIENT_ID'),
    );
    expect(window.location.href).toBe('');
    alertSpy.mockRestore();
  });

  test('UT-F-21: с CLIENT_ID строит корректный URL и редиректит', async () => {
    process.env.REACT_APP_YANDEX_CLIENT_ID = 'cli-id-1';

    render(<YandexLoginButton />);
    await userEvent.click(screen.getByRole('button'));

    expect(window.location.href).toContain('https://oauth.yandex.ru/authorize');
    const url = new URL(window.location.href);
    expect(url.searchParams.get('response_type')).toBe('code');
    expect(url.searchParams.get('client_id')).toBe('cli-id-1');
    expect(url.searchParams.get('redirect_uri'))
      .toBe('http://localhost:3000/auth/yandex/callback');
  });
});
