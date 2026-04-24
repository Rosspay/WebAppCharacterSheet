import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '../../app/hooks';
import { logout } from '../../features/auth/authSlice';
import { useLogoutMutation } from '../../features/auth/authApi';

export const Navbar: React.FC = () => {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const isAuthenticated = useAppSelector((s) => s.auth.isAuthenticated);
  const user = useAppSelector((s) => s.auth.user);
  const [logoutApi] = useLogoutMutation();

  const handleLogout = async () => {
    try { await logoutApi().unwrap(); } catch {}
    dispatch(logout());
    navigate('/login');
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark bg-primary shadow-sm">
      <div className="container">
        <Link className="navbar-brand fw-bold" to="/">
          TTRPG World
        </Link>
        <div className="d-flex align-items-center gap-2">
          {isAuthenticated ? (
            <>
              <span className="text-white-50 small d-none d-sm-inline">
                {user?.username}
              </span>
              <Link to="/dashboard" className="btn btn-outline-light btn-sm">
                Профиль
              </Link>
              <button onClick={handleLogout} className="btn btn-light btn-sm">
                Выйти
              </button>
            </>
          ) : (
            <>
              <Link to="/login" className="btn btn-outline-light btn-sm">Войти</Link>
              <Link to="/register" className="btn btn-light btn-sm">Регистрация</Link>
            </>
          )}
        </div>
      </div>
    </nav>
  );
};