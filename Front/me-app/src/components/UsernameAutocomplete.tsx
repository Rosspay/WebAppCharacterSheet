/**
 * Reusable username autocomplete input.
 *
 * Backs the invitation and access-grant forms. While the user types the
 * component debounces input (default 250 ms) before calling the
 * `searchUsernames` lazy query, then renders the suggestions as a
 * Bootstrap-styled dropdown directly below the input.
 *
 * Keyboard support:
 *  - ArrowDown / ArrowUp — move the highlight within the suggestion list;
 *  - Enter — pick the highlighted suggestion (or the raw input if no
 *    suggestion is highlighted);
 *  - Escape — close the dropdown without picking.
 *
 * Clicking outside the component or selecting a suggestion closes the
 * dropdown. Selected usernames are reported via the `onPick` callback;
 * the input text itself is fully controlled via `value` / `onChange`.
 * @module
 */
import React, { useEffect, useMemo, useRef, useState } from 'react';
import { useLazySearchUsernamesQuery } from '../features/users/usersApi';


/** Props accepted by `UsernameAutocomplete`. */
export interface UsernameAutocompleteProps {
  /** Current value of the text input (controlled). */
  value: string;
  /** Fired on every keystroke; receives the new raw text. */
  onChange: (value: string) => void;
  /** Fired when the user commits a username (click, Enter on highlighted item, or Enter on raw input). */
  onPick: (username: string) => void;
  /** Placeholder text shown when the input is empty. */
  placeholder?: string;
  /** CSS class for the underlying `<input>`. Defaults to Bootstrap `form-control`. */
  inputClassName?: string;
  /** Usernames to hide from the suggestion list (e.g. already-invited users). */
  exclude?: string[];
  /** Minimum query length before the autocomplete request fires. Default 1. */
  minLength?: number;
  /** Maximum number of suggestions to request from the API. Default 10. */
  limit?: number;
  /** Debounce delay between keystrokes and the API request, in ms. Default 250. */
  debounceMs?: number;
}


const UsernameAutocomplete: React.FC<UsernameAutocompleteProps> = ({
  value,
  onChange,
  onPick,
  placeholder,
  inputClassName,
  exclude = [],
  minLength = 1,
  limit = 10,
  debounceMs = 250,
}) => {
  const [trigger, { data, isFetching }] = useLazySearchUsernamesQuery();
  const [open, setOpen] = useState(false);
  const [highlighted, setHighlighted] = useState(0);
  const wrapperRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    const handle = window.setTimeout(() => {
      const q = value.trim();
      if (q.length >= minLength) {
        trigger({ q, limit });
        setOpen(true);
      } else {
        setOpen(false);
      }
    }, debounceMs);
    return () => window.clearTimeout(handle);
  }, [value, minLength, limit, debounceMs, trigger]);

  useEffect(() => {
    const onDocMouseDown = (e: MouseEvent) => {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    };
    document.addEventListener('mousedown', onDocMouseDown);
    return () => document.removeEventListener('mousedown', onDocMouseDown);
  }, []);

  const excludeSet = useMemo(() => new Set(exclude), [exclude]);
  const suggestions = useMemo(
    () => (data ?? []).filter((u) => !excludeSet.has(u)),
    [data, excludeSet],
  );

  useEffect(() => {
    setHighlighted(0);
  }, [suggestions.length]);

  const pickAt = (idx: number) => {
    const chosen = suggestions[idx];
    if (chosen) {
      onPick(chosen);
      setOpen(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (suggestions.length > 0) {
        setHighlighted((p) => (p + 1) % suggestions.length);
        setOpen(true);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (suggestions.length > 0) {
        setHighlighted((p) => (p - 1 + suggestions.length) % suggestions.length);
        setOpen(true);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (open && suggestions.length > 0) {
        pickAt(highlighted);
      } else {
        const name = value.trim();
        if (name) onPick(name);
      }
    } else if (e.key === 'Escape') {
      setOpen(false);
    }
  };

  return (
    <div ref={wrapperRef} className="position-relative flex-grow-1">
      <input
        type="text"
        className={inputClassName ?? 'form-control'}
        placeholder={placeholder}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        onFocus={() => value.trim().length >= minLength && setOpen(true)}
        onKeyDown={handleKeyDown}
        autoComplete="off"
        spellCheck={false}
      />
      {open && (suggestions.length > 0 || isFetching) && (
        <ul
          className="list-group position-absolute w-100 shadow-sm"
          style={{ zIndex: 1050, top: '100%', marginTop: 2, maxHeight: 220, overflowY: 'auto' }}
        >
          {isFetching && suggestions.length === 0 && (
            <li className="list-group-item text-muted small fst-italic">Поиск…</li>
          )}
          {suggestions.map((u, idx) => (
            <li
              key={u}
              className={
                'list-group-item list-group-item-action py-1' +
                (idx === highlighted ? ' active' : '')
              }
              role="button"
              onMouseDown={(e) => {
                e.preventDefault();
                pickAt(idx);
              }}
              onMouseEnter={() => setHighlighted(idx)}
            >
              {u}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
};

export default UsernameAutocomplete;
