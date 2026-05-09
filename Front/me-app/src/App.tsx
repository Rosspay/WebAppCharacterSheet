import React, { useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { Navbar } from './components/layout/Navbar';
import { ProtectedRoute } from './components/layout/ProtectedRoute';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import MyTemplatesPage from './pages/MyTemplatesPage';
import PublicTemplatesPage from './pages/PublicTemplatesPage';
import TemplateViewPage from './pages/TemplateViewPage';
import TemplateEditorPage from './pages/TemplateEditorPage';
import CharactersPage from './pages/CharactersPage';
import MyCharactersPage from './pages/MyCharactersPage';
import AvailableCharactersPage from './pages/AvailableCharactersPage';
import CharacterEditorPage from './pages/CharacterEditorPage';
import CharacterViewPage from './pages/CharacterViewPage';
import VisibilityPage from './pages/VisibilityPage';
import { useGetMeQuery } from './features/auth/authApi';
import { useAppDispatch, useAppSelector } from './app/hooks';
import { setUser } from './features/auth/authSlice';

const AuthLoader: React.FC = () => {
  const dispatch = useAppDispatch();
  const isAuthenticated = useAppSelector((s) => s.auth.isAuthenticated);
  const { data } = useGetMeQuery(undefined, { skip: !isAuthenticated });

  useEffect(() => {
    if (data) dispatch(setUser(data));
  }, [data, dispatch]);

  return null;
};

const App: React.FC = () => (
  <BrowserRouter>
    <Navbar />
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/dashboard" element={<DashboardPage />} />
        <Route path="/templates/my"       element={<MyTemplatesPage />} />
        <Route path="/templates/public"   element={<PublicTemplatesPage />} />
        <Route path="/templates/new"      element={<TemplateEditorPage />} />
        <Route path="/templates/:id"      element={<TemplateViewPage />} />
        <Route path="/templates/:id/edit" element={<TemplateEditorPage />} />
        <Route path="/characters/my" element={<MyCharactersPage />} />
        <Route path="/characters/available" element={<AvailableCharactersPage />} />
        <Route path="/characters/new" element={<CharacterEditorPage />} />
        <Route path="/characters/new/:templateId" element={<CharacterEditorPage />} />
        <Route path="/characters/:id" element={<CharacterViewPage />} />
        <Route path="/characters/:id/edit" element={<CharacterEditorPage />} />
        <Route path="/characters/:id/visibility" element={<VisibilityPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  </BrowserRouter>
);

export default App;