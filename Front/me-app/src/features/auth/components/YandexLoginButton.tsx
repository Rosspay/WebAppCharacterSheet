
import React from 'react';


export const YandexLoginButton: React.FC = () => {
  const clientId = process.env.REACT_APP_YANDEX_CLIENT_ID;
  const redirectUri =
    process.env.REACT_APP_YANDEX_REDIRECT_URI ||
    `${window.location.origin}/auth/yandex/callback`;

  const handleClick = () => {
    if (!clientId) {

      alert('REACT_APP_YANDEX_CLIENT_ID не задан — Яндекс‑вход отключён.');
      return;
    }
    const url = new URL('https://oauth.yandex.ru/authorize');
    url.searchParams.set('response_type', 'code');
    url.searchParams.set('client_id', clientId);
    url.searchParams.set('redirect_uri', redirectUri);
    window.location.href = url.toString();
  };

  return (
    <button
      type="button"
      className="btn btn-warning w-100 py-2 fw-medium"
      onClick={handleClick}
    >
      Войти через Яндекс
    </button>
  );
};
