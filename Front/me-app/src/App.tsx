import React from 'react';
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
import MyCharactersPage from './pages/MyCharactersPage';
import AvailableCharactersPage from './pages/AvailableCharactersPage';
import CharacterEditorPage from './pages/CharacterEditorPage';
import CharacterViewPage from './pages/CharacterViewPage';
import VisibilityPage from './pages/VisibilityPage';
import EventListPage from './pages/events/EventListPage';
import EventEditorPage from './pages/events/EventEditorPage';
import EventDetailsPage from './pages/events/EventDetailsPage';
import YandexCallbackPage from './pages/YandexCallbackPage';

const App: React.FC = () => (
  <BrowserRouter>
    <Navbar />
    <Routes>
      <Route path="/login"    element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/auth/yandex/callback" element={<YandexCallbackPage />} />

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
        <Route path="/events" element={<EventListPage />} />
        <Route path="/events/new" element={<EventEditorPage />} />
        <Route path="/events/:id" element={<EventDetailsPage />} />
        <Route path="/events/:id/edit" element={<EventEditorPage />} />
      </Route>

      <Route path="*" element={<Navigate to="/login" replace />} />
    </Routes>
  </BrowserRouter>
);

export default App;