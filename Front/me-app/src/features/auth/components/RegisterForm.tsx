import React, { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useRegisterMutation } from '../authApi';
import { setTokens } from '../authSlice';
import { useAppDispatch } from '../../../app/hooks';

export const RegisterForm: React.FC = () => {
  const navigate = useNavigate();
  const dispatch = useAppDispatch();
  const [register, { isLoading }] = useRegisterMutation();

  const [form, setForm] = useState({ username: '', email: '', password: '', confirm: '' });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [serverError, setServerError] = useState('');

  const validate = () => {
    const e: Record<string, string> = {};
    if (!form.username.trim() || form.username.length < 3)
      e.username = 'Минимум 3 символа';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email))
      e.email = 'Введите корректный email';
    if (form.password.length < 8)
      e.password = 'Минимум 8 символов';
    if (form.password !== form.confirm)
      e.confirm = 'Пароли не совпадают';
    return e;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setServerError('');
    const e2 = validate();
    if (Object.keys(e2).length) { setErrors(e2); return; }
    setErrors({});
    try {
      const tokens = await register({
        username: form.username,
        email: form.email,
        password: form.password,
      }).unwrap();
      dispatch(setTokens(tokens));
      navigate('/dashboard');
    } catch (err: any) {
      setServerError(err?.data?.message ?? 'Ошибка регистрации');
    }
  };

  const field = (
    id: keyof typeof form,
    label: string,
    type = 'text',
    placeholder = '',
    autoComplete = ''
  ) => (
    <div className="mb-3">
      <label htmlFor={id} className="form-label fw-medium">{label}</label>
      <input
        id={id}
        type={type}
        className={`form-control ${errors[id] ? 'is-invalid' : ''}`}
        placeholder={placeholder}
        value={form[id]}
        onChange={(e) => setForm({ ...form, [id]: e.target.value })}
        autoComplete={autoComplete}
      />
      {errors[id] && <div className="invalid-feedback">{errors[id]}</div>}
    </div>
  );

  return (
    <div className="card shadow-sm border-0" style={{ maxWidth: 440, width: '100%' }}>
      <div className="card-body p-4 p-md-5">
        <h1 className="h4 fw-bold mb-1 text-center">Регистрация</h1>
        <p className="text-muted text-center mb-4" style={{ fontSize: '0.9rem' }}>
          Создайте новый аккаунт
        </p>

        {serverError && (
          <div className="alert alert-danger py-2" role="alert">{serverError}</div>
        )}

        <form onSubmit={handleSubmit} noValidate>
          {field('username', 'Имя пользователя', 'text', 'username', 'username')}
          {field('email', 'Email', 'email', 'you@example.com', 'email')}
          {field('password', 'Пароль', 'password', '••••••••', 'new-password')}
          {field('confirm', 'Подтвердите пароль', 'password', '••••••••', 'new-password')}

          <button
            type="submit"
            className="btn btn-primary w-100 py-2 fw-medium mt-1"
            disabled={isLoading}
          >
            {isLoading ? (
              <>
                <span className="spinner-border spinner-border-sm me-2" role="status" />
                Регистрация...
              </>
            ) : (
              'Создать аккаунт'
            )}
          </button>
        </form>

        <hr className="my-4" />
        <p className="text-center mb-0" style={{ fontSize: '0.9rem' }}>
          Уже есть аккаунт?{' '}
          <Link to="/login" className="text-decoration-none fw-medium">Войти</Link>
        </p>
      </div>
    </div>
  );
};