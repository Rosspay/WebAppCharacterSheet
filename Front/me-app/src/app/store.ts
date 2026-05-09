import { configureStore, combineReducers } from '@reduxjs/toolkit';
import { authApi } from '../features/auth/authApi';
import { templatesApi } from '../features/atemplates/templatesApi';
import authReducer from '../features/auth/authSlice';
import { templateEditorSlice } from '../features/atemplates/templateEditorSlice';
import { charactersApi } from '../features/characters/charactersApi';

const combinedReducer = combineReducers({
  auth: authReducer,
  templateEditor: templateEditorSlice.reducer,
  [authApi.reducerPath]: authApi.reducer,
  [templatesApi.reducerPath]: templatesApi.reducer,
  [charactersApi.reducerPath]: charactersApi.reducer,
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
  .concat(charactersApi.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;