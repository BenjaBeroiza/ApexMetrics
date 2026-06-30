import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import TrackView from './TrackView';

vi.mock('../components/TrackMap', () => ({
  default: ({ sessionId }: { sessionId: string | number }) => (
    <div data-testid="track-map" data-session-id={String(sessionId)} />
  ),
}));

describe('TrackView', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-token');
  });
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  const renderRoute = (id = '42') =>
    render(
      <MemoryRouter initialEntries={[`/dashboard/sesiones/${id}/trazado`]}>
        <Routes>
          <Route path="/dashboard/sesiones/:id/trazado" element={<TrackView />} />
          <Route path="/dashboard" element={<div>Dashboard</div>} />
          <Route path="/login" element={<div>Login</div>} />
        </Routes>
      </MemoryRouter>
    );

  it('muestra el título TRAZADO DE PISTA', () => {
    renderRoute();
    expect(screen.getByText(/TRAZADO DE PISTA/i)).toBeInTheDocument();
  });

  it('pasa el sessionId correcto al TrackMap', () => {
    renderRoute('99');
    expect(screen.getByTestId('track-map')).toHaveAttribute('data-session-id', '99');
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
