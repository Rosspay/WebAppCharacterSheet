import React from 'react';
import { LoginForm } from '../features/auth/components/LoginForm';

const LoginPage: React.FC = () => (
  <main className="d-flex justify-content-center align-items-center"
    style={{ minHeight: 'calc(100vh - 60px)', background: '#f8f9fa', padding: '2rem 1rem' }}>
    <LoginForm />
  </main>
);

export default LoginPage;