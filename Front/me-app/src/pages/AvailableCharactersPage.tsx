import React, { useState } from 'react';
import { Link } from 'react-router-dom';
import { useGetAvailableCharactersQuery } from '../features/characters/charactersApi';
import { CharacterVisibility } from '../types/character';

const VISIBILITY_BADGE: Record<CharacterVisibility, string> = {
  PRIVATE:    'bg-secondary',
  PUBLIC:     'bg-success',
  RESTRICTED: 'bg-warning text-dark',
};

const AvailableCharactersPage: React.FC = () => {
  const [search, setSearch] = useState('');
  const [query, setQuery]   = useState('');
  const [page, setPage]     = useState(0);

  const { data, isLoading, isError } = useGetAvailableCharactersQuery({ query, page, size: 12 });

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setQuery(search);
    setPage(0);
  };

  return (
    <div className="container py-4">
      <h1 className="h3 mb-4">Персонажи</h1>

      <form className="d-flex gap-2 mb-4" onSubmit={handleSearch}>
        <input
          type="text"
          className="form-control"
          placeholder="Поиск по имени или описанию..."
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
      {isError && <div className="alert alert-danger">Не удалось загрузить персонажей</div>}

      {data && !data.items.length && (
        <p className="text-muted text-center py-5">Персонажей не найдено</p>
      )}

      {data && data.items.length > 0 && (
        <>
          <div className="row g-3">
            {data.items.map((c) => (
              <div key={c.id} className="col-12 col-md-6 col-lg-4">
                <div className="card h-100 shadow-sm">
                  <div className="card-body">
                    <div className="d-flex justify-content-between align-items-start mb-2">
                      <h5 className="card-title mb-0">{c.name}</h5>
                      <span className={`badge ${VISIBILITY_BADGE[c.visibility]}`}>
                        {c.visibility === 'RESTRICTED' ? 'Только для вас' : 'Публичный'}
                      </span>
                    </div>
                    <p className="card-text text-muted small">{c.description}</p>
                  </div>
                  <div className="card-footer bg-transparent">
                    <Link to={`/characters/${c.id}`} className="btn btn-sm btn-outline-primary">
                      Просмотреть
                    </Link>
                  </div>
                </div>
              </div>
            ))}
          </div>

          {data.totalPages > 1 && (
            <nav className="mt-4">
              <ul className="pagination justify-content-center">
                {Array.from({ length: data.totalPages }, (_, i) => (
                  <li key={i} className={`page-item${page === i ? ' active' : ''}`}>
                    <button className="page-link" onClick={() => setPage(i)}>
                      {i + 1}
                    </button>
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

export default AvailableCharactersPage;