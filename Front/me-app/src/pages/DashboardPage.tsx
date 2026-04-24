import React from 'react';
import { useGetMeQuery } from '../features/auth/authApi';
import { setUser } from '../features/auth/authSlice';
import { useAppDispatch, useAppSelector } from '../app/hooks';

const DashboardPage: React.FC = () => {
  const dispatch = useAppDispatch();
  const cachedUser = useAppSelector((s) => s.auth.user);
  const { data, isLoading, isError } = useGetMeQuery(undefined, {
    skip: !!cachedUser,
  });

  React.useEffect(() => {
    if (data) dispatch(setUser(data));
  }, [data, dispatch]);

  const user = cachedUser ?? data;

  if (isLoading) return (
    <main className="container py-5 text-center">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">Загрузка...</span>
      </div>
    </main>
  );

  if (isError) return (
    <main className="container py-5">
      <div className="alert alert-danger">Ошибка загрузки данных пользователя</div>
    </main>
  );

  return (
    <main className="container py-5" style={{ maxWidth: 640 }}>
      <h1 className="h4 fw-bold mb-4">Личный кабинет</h1>
      {user && (
        <div className="card shadow-sm border-0">
          <div className="card-body p-4">
            <div className="d-flex align-items-center gap-3 mb-4">
              <div
                className="rounded-circle bg-primary d-flex align-items-center justify-content-center text-white fw-bold"
                style={{ width: 56, height: 56, fontSize: '1.4rem', flexShrink: 0 }}
              >
                {user.username[0].toUpperCase()}
              </div>
              <div>
                <div className="fw-bold fs-5">{user.username}</div>
                <div className="text-muted small">{user.email}</div>
              </div>
            </div>
            <hr />
            <dl className="row mb-0">
              <dt className="col-4 text-muted fw-normal">ID</dt>
              <dd className="col-8 mb-2">{user.id}</dd>
              <dt className="col-4 text-muted fw-normal">Роль</dt>
              <dd className="col-8 mb-0">
                <span className="badge bg-primary-subtle text-primary-emphasis">
                  {user.role}
                </span>
              </dd>
            </dl>
          </div>
        </div>
      )}
    </main>
  );
};

export default DashboardPage;