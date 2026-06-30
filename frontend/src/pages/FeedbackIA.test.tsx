import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { fireEvent } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import FeedbackIA from './FeedbackIA';

const FEEDBACK_RESPONSE = {
  sessionId: 42,
  feedback: `ANÁLISIS DE FRENADA\nFrenas demasiado tarde.\n\nANÁLISIS DE ACELERACIÓN\nBuena salida de curva.\n\nGESTIÓN DE VELOCIDAD\nVelocidad media aceptable.\n\nPUNTOS DE MEJORA\n- Frenar antes.\n\nCONCLUSIÓN\nBuen trabajo en general.`,
};

const renderRoute = (id = '42') =>
  render(
    <MemoryRouter initialEntries={[`/dashboard/sesiones/${id}/feedback-ia`]}>
      <Routes>
        <Route path="/dashboard/sesiones/:id/feedback-ia" element={<FeedbackIA />} />
        <Route path="/dashboard" element={<div>Dashboard</div>} />
        <Route path="/login" element={<div>Login</div>} />
      </Routes>
    </MemoryRouter>
  );

describe('FeedbackIA', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-token');
  });
  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('muestra estado de carga inicialmente', () => {
    global.fetch = vi.fn().mockReturnValue(new Promise(() => {})) as unknown as typeof fetch;
    renderRoute();
    expect(screen.getByText(/ANALIZANDO TELEMETRÍA/i)).toBeInTheDocument();
  });

  it('muestra el título ANÁLISIS IA y el número de sesión', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => FEEDBACK_RESPONSE,
    }) as unknown as typeof fetch;

    renderRoute('42');

    await waitFor(() => {
      expect(screen.getByText(/ANÁLISIS IA/i)).toBeInTheDocument();
    });
    expect(screen.getByText(/Sesión #42/i)).toBeInTheDocument();
  });

  it('renderiza las secciones de la retroalimentación', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => FEEDBACK_RESPONSE,
    }) as unknown as typeof fetch;

    renderRoute();

    await waitFor(() => {
      expect(screen.getByText('ANÁLISIS DE FRENADA')).toBeInTheDocument();
    });
    expect(screen.getByText('ANÁLISIS DE ACELERACIÓN')).toBeInTheDocument();
    expect(screen.getByText('GESTIÓN DE VELOCIDAD')).toBeInTheDocument();
    expect(screen.getByText('PUNTOS DE MEJORA')).toBeInTheDocument();
    expect(screen.getByText('CONCLUSIÓN')).toBeInTheDocument();
  });

  it('muestra mensaje de error cuando la API responde 403', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
    }) as unknown as typeof fetch;

    renderRoute();

    await waitFor(() => {
      expect(screen.getByText(/Sin permiso/i)).toBeInTheDocument();
    });
  });

  it('navega al dashboard al hacer clic en VOLVER AL DASHBOARD', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => FEEDBACK_RESPONSE,
    }) as unknown as typeof fetch;

    renderRoute();

    await waitFor(() => {
      expect(screen.getByText(/ANÁLISIS IA/i)).toBeInTheDocument();
    });

    fireEvent.click(screen.getByText(/VOLVER AL DASHBOARD/i));
    expect(screen.getByText('Dashboard')).toBeInTheDocument();
  });

  it('redirige a /login si no hay token', () => {
    localStorage.removeItem('apex_token');
    renderRoute();
    expect(screen.getByText('Login')).toBeInTheDocument();
  });
});
