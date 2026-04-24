import React from 'react';
import { RegisterForm } from '../features/auth/components/RegisterForm';

const RegisterPage: React.FC = () => (
  <main className="d-flex justify-content-center align-items-center"
    style={{ minHeight: 'calc(100vh - 60px)', background: '#f8f9fa', padding: '2rem 1rem' }}>
    <RegisterForm />
  </main>
);

export default RegisterPage;