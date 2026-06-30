import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SessionAnalysis from './SessionAnalysis';

vi.mock('../components/SessionChart', () => ({
  default: ({ sessionId }: { sessionId: string | number }) => (
    <div data-testid="session-chart" data-session-id={String(sessionId)} />
  ),
}));

describe('SessionAnalysis', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-token');
  });
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  const renderRoute = (id = '7') =>
    render(
      <MemoryRouter initialEntries={[`/dashboard/sesiones/${id}/analisis`]}>
        <Routes>
          <Route path="/dashboard/sesiones/:id/analisis" element={<SessionAnalysis />} />
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route path="/login" element={<div>Login</div>} />
        </Routes>
      </MemoryRouter>
    );

  it('muestra el título ANÁLISIS DE SESIÓN', () => {
    renderRoute();
    expect(screen.getByText(/ANÁLISIS DE SESIÓN/i)).toBeInTheDocument();
  });

  it('pasa el sessionId correcto al SessionChart', () => {
    renderRoute('15');
    expect(screen.getByTestId('session-chart')).toHaveAttribute('data-session-id', '15');
  });

  it('navega al dashboard al hacer clic en VOLVER AL DASHBOARD', () => {
    renderRoute();
    fireEvent.click(screen.getByText(/VOLVER AL DASHBOARD/i));
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('redirige a /login si no hay token', () => {
    localStorage.removeItem('apex_token');
    renderRoute();
    expect(screen.getByText('Login')).toBeInTheDocument();
  });
});
