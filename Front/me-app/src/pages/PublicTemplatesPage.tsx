import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useGetPublicTemplatesQuery } from '../features/atemplates/templatesApi';

const PublicTemplatesPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);

  const { data, isLoading, isError } = useGetPublicTemplatesQuery({ query, page, size: 12 });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setQuery(search);
    setPage(0);
  };

  return (
    <div className="container py-4">
      <h1 className="h3 mb-4">Публичные шаблоны</h1>

      <form className="d-flex gap-2 mb-4" onSubmit={handleSearch}>
        <input
          type="text"
          className="form-control"
          placeholder="Поиск по названию..."
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <button type="submit" className="btn btn-primary px-4">Найти</button>
      </form>

      {isLoading && (
        <div className="text-center py-5">
          <div className="spinner-border text-primary" role="status" />
        </div>
      )}

      {isError && (
        <div className="alert alert-danger">Не удалось загрузить шаблоны</div>
      )}

      {data && (
        <>
          {!data.items.length ? (
            <p className="text-muted text-center py-5">Шаблонов не найдено</p>
          ) : (
            <div className="row g-3">
              {data.items.map((t) => (
                <div key={t.id} className="col-12 col-md-6 col-lg-4">
                  <div className="card h-100 shadow-sm">
                    <div className="card-body">
                      <h5 className="card-title">{t.title}</h5>
                      <p className="card-text text-muted small">{t.description || '—'}</p>
                    </div>
                    <div className="card-footer bg-transparent">
                      <Link to={`/templates/${t.id}`} className="btn btn-sm btn-outline-primary">
                        Открыть
                      </Link>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          )}

          {data.totalPages > 1 && (
            <nav className="mt-4">
              <ul className="pagination justify-content-center">
                {Array.from({ length: data.totalPages }, (_, i) => (
                  <li key={i} className={`page-item ${page === i ? 'active' : ''}`}>
                    <button className="page-link" onClick={() => setPage(i)}>{i + 1}</button>
                  </li>
                ))}
              </ul>
            </nav>
          )}
        </>
      )}
    </div>
  );
};

export default PublicTemplatesPage;