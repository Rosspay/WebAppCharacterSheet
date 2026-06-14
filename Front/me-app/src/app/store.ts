/**
 * Root Redux store of the client application. Combines all RTK Query APIs
 * (auth, templates, characters, events, users) and feature slices
 * (`authSlice`, `templateEditorSlice`); on the `auth/logout` action the
 * entire state is reset to the initial value so no stale data persists
 * across logout/login.
 * @module
 */
import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { authApi } from '../features/auth/authApi';
import { templatesApi } from '../features/atemplates/templatesApi';
import authReducer from '../features/auth/authSlice';
import { templateEditorSlice } from '../features/atemplates/templateEditorSlice';
import { charactersApi } from '../features/characters/charactersApi';
import { eventsApi } from '../features/events/eventsApi';
import { usersApi } from '../features/users/usersApi';

const combinedReducer = combineReducers({
  auth: authReducer,
  templateEditor: templateEditorSlice.reducer,
  [authApi.reducerPath]: authApi.reducer,
  [templatesApi.reducerPath]: templatesApi.reducer,
  [charactersApi.reducerPath]: charactersApi.reducer,
  [eventsApi.reducerPath]: eventsApi.reducer,
  [usersApi.reducerPath]: usersApi.reducer,
});

const rootReducer = (state: any, action: any) => {
  if (action.type === 'auth/logout') {
    state = undefined;
  }
  return combinedReducer(state, action);
};

export const store = configureStore({
  reducer: rootReducer,
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware()
  .concat(authApi.middleware)
  .concat(templatesApi.middleware)
  .concat(charactersApi.middleware)
  .concat(eventsApi.middleware)
  .concat(usersApi.middleware),
});

/** Inferred shape of the root state, used as a type parameter throughout the app. */
export type RootState = ReturnType<typeof store.getState>;

/** Dispatch type with all middleware-extended signatures (thunks, RTK Query). */
export type AppDispatch = typeof store.dispatch;