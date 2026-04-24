import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useLoginMutation } from '../authApi';
import { setTokens } from '../authSlice';
import { useAppDispatch } from '../../../app/hooks';

export const LoginForm: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [login, { isLoading }] = useLoginMutation();

  const [form, setForm] = useState({ username: '', password: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.username.trim()) e.username = 'Введите имя пользователя';
    if (!form.password) e.password = 'Введите пароль';
    return e;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError('');
    const e2 = validate();
    if (Object.keys(e2).length) { setErrors(e2); return; }
    setErrors({});
    try {
      const tokens = await login(form).unwrap();
      dispatch(setTokens(tokens));
      navigate('/dashboard');
    } catch (err: any) {
      setServerError(err?.data?.message ?? 'Неверные данные для входа');
    }
  };

  return (
    <div className="card shadow-sm border-0" style={{ maxWidth: 420, width: '100%' }}>
      <div className="card-body p-4 p-md-5">
        <h1 className="h4 fw-bold mb-1 text-center">Вход</h1>
        <p className="text-muted text-center mb-4" style={{ fontSize: '0.9rem' }}>
          Введите данные аккаунта
        </p>

        {serverError && (
          <div className="alert alert-danger py-2" role="alert">
            {serverError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          <div className="mb-3">
            <label htmlFor="username" className="form-label fw-medium">
              Имя пользователя
            </label>
            <input
              id="username"
              type="text"
              className={`form-control ${errors.username ? 'is-invalid' : ''}`}
              placeholder="username"
              value={form.username}
              onChange={(e) => setForm({ ...form, username: e.target.value })}
              autoComplete="username"
            />
            {errors.username && (
              <div className="invalid-feedback">{errors.username}</div>
            )}
          </div>

          <div className="mb-4">
            <label htmlFor="password" className="form-label fw-medium">
              Пароль
            </label>
            <input
              id="password"
              type="password"
              className={`form-control ${errors.password ? 'is-invalid' : ''}`}
              placeholder="••••••••"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              autoComplete="current-password"
            />
            {errors.password && (
              <div className="invalid-feedback">{errors.password}</div>
            )}
          </div>

          <button
            type="submit"
            className="btn btn-primary w-100 py-2 fw-medium"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" />
                Вход...
              </>
            ) : (
              'Войти'
            )}
          </button>
        </form>

        <hr className="my-4" />
        <p className="text-center mb-0" style={{ fontSize: '0.9rem' }}>
          Нет аккаунта?{' '}
          <Link to="/register" className="text-decoration-none fw-medium">
            Зарегистрироваться
          </Link>
        </p>
      </div>
    </div>
  );
};