/**
 * Typed React-Redux hooks. Prefer these over the raw `useDispatch`/`useSelector`
 * from `react-redux` so that all components inherit `RootState` typing and
 * the dispatch type knows about thunks/RTK Query middleware.
 * @module
 */
import { useDispatch, useSelector, TypedUseSelectorHook } from 'react-redux';
import type { RootState, AppDispatch } from './store';

/**
 * Returns the typed Redux dispatch function. The return type is inferred from
 * the store's middleware, so thunk/RTK Query dispatches are type-safe.
 */
export const useAppDispatch = () => useDispatch<AppDispatch>();

/**
 * Typed selector hook bound to the application's `RootState`.
 */
export const useAppSelector: TypedUseSelectorHook<RootState> = useSelector;
