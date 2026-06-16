import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Dashboard from './Dashboard';

// Mock de useNavigate
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderDashboard = () =>
  render(
    <MemoryRouter>
      <Dashboard />
    </MemoryRouter>
  );

describe('Dashboard — sin sesión activa', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('redirige a login si no hay token', () => {
    renderDashboard();
    expect(mockNavigate).toHaveBeenCalledWith('/login');
  });
});

describe('Dashboard — con sesión activa', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('apex_token', 'fake-jwt-token');
    localStorage.setItem('apex_username', 'PilotoTest');
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('muestra el título MIS SESIONES', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('MIS SESIONES')).toBeInTheDocument();
    });
  });

  it('muestra el mensaje de estado vacío cuando no hay sesiones', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/No hay sesiones registradas/i)).toBeInTheDocument();
    });
  });

  it('muestra la tabla con sesiones cuando la API devuelve datos', async () => {
    const mockSesiones = [
      {
        sessionId: 1,
        trackName: 'Spa-Francorchamps',
        categoryName: 'GT3',
        bestLapTime: 135.89,
        uploadedAt: '2026-05-22T14:30:00',
      },
    ];

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => mockSesiones,
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText('SPA-FRANCORCHAMPS')).toBeInTheDocument();
      expect(screen.getByText('GT3')).toBeInTheDocument();
    });
  });

  it('muestra error de conexión cuando la API falla', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 500,
      json: async () => ({}),
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      expect(screen.getByText(/No se pudo conectar/i)).toBeInTheDocument();
    });
  });

  it('redirige a login cuando la API responde 401', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
      json: async () => ({}),
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });

  it('el botón NUEVA SESIÓN navega a /upload', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      const btn = screen.getByText('NUEVA SESIÓN');
      fireEvent.click(btn);
      expect(mockNavigate).toHaveBeenCalledWith('/upload');
    });
  });

  it('el botón CERRAR SESIÓN limpia localStorage y redirige a login', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [],
    }) as unknown as typeof fetch;

    renderDashboard();

    await waitFor(() => {
      fireEvent.click(screen.getByText('CERRAR SESIÓN'));
      expect(localStorage.getItem('apex_token')).toBeNull();
      expect(mockNavigate).toHaveBeenCalledWith('/login');
    });
  });
});
