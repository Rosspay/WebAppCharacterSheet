
import React, { useEffect, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useYandexCallbackMutation } from '../features/auth/authApi';
import { setTokens } from '../features/auth/authSlice';
import { useAppDispatch } from '../app/hooks';

const YandexCallbackPage: React.FC = () => {
  const [params] = useSearchParams();
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [yandexCallback] = useYandexCallbackMutation();
  const [error, setError] = useState<string | null>(null);


  const startedRef = useRef(false);

  useEffect(() => {
    if (startedRef.current) return;
    startedRef.current = true;

    const code = params.get('code');
    const errorMsg = params.get('error_description') || params.get('error');
    if (errorMsg) { setError(errorMsg); return; }
    if (!code) { setError('Не передан параметр code'); return; }

    const redirectUri =
      process.env.REACT_APP_YANDEX_REDIRECT_URI ||
      `${window.location.origin}/auth/yandex/callback`;

    yandexCallback({ code, redirectUri })
      .unwrap()
      .then(tokens => {
        dispatch(setTokens(tokens));
        navigate('/dashboard', { replace: true });
      })
      .catch(err => {
        setError(err?.data?.message ?? 'Ошибка входа через Яндекс');
      });
  }, [params, yandexCallback, dispatch, navigate]);

  return (
    <main className="container py-5 text-center">
      {error ? (
        <div className="alert alert-danger d-inline-block">
          <strong>Ошибка:</strong> {error}
          <div>
            <a href="/login" className="btn btn-link">Вернуться на страницу входа</a>
          </div>
        </div>
      ) : (
        <>
          <div className="spinner-border text-primary mb-2" role="status" />
          <p>Завершаем вход через Яндекс…</p>
        </>
      )}
    </main>
  );
};

export default YandexCallbackPage;
