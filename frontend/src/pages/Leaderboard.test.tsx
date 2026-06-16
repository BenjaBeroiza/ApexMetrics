import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Leaderboard from './Leaderboard';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderLeaderboard = () =>
  render(
    <MemoryRouter>
      <Leaderboard />
    </MemoryRouter>
  );

describe('Leaderboard — renderizado', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra el título CLASIFICACIÓN GLOBAL', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [], totalPages: 1 }),
    }) as unknown as typeof fetch;

    renderLeaderboard();

    await waitFor(() => {
      expect(screen.getByText('CLASIFICACIÓN GLOBAL')).toBeInTheDocument();
    });
  });

  it('muestra los selectores de filtro de categoría y circuito', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [], totalPages: 1 }),
    }) as unknown as typeof fetch;

    renderLeaderboard();

    await waitFor(() => {
      expect(screen.getByText('Todas las Categorías')).toBeInTheDocument();
      expect(screen.getByText('Todos los Circuitos')).toBeInTheDocument();
    });
  });

  it('muestra los encabezados de la tabla', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [], totalPages: 1 }),
    }) as unknown as typeof fetch;

    renderLeaderboard();

    await waitFor(() => {
      expect(screen.getByText('POS')).toBeInTheDocument();
      expect(screen.getByText('PILOTO')).toBeInTheDocument();
      expect(screen.getByText('TIEMPO DE VUELTA')).toBeInTheDocument();
      expect(screen.getByText('CATEGORÍA')).toBeInTheDocument();
    });
  });
});

describe('Leaderboard — datos', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renderiza filas de la clasificación con datos del backend', async () => {
    const mockEntries = [
      {
        rank: 1,
        username: 'hamiltonl',
        bestLapTime: 70.458,
        categoryName: 'F1',
        uploadedAt: '2026-05-20T10:00:00',
      },
      {
        rank: 2,
        username: 'verstappenm',
        bestLapTime: 70.712,
        categoryName: 'F1',
        uploadedAt: '2026-05-20T11:00:00',
      },
    ];

    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: mockEntries, totalPages: 1 }),
    }) as unknown as typeof fetch;

    renderLeaderboard();

    await waitFor(() => {
      expect(screen.getByText('HAMILTONL')).toBeInTheDocument();
      expect(screen.getByText('VERSTAPPENM')).toBeInTheDocument();
    });
  });

  it('muestra error cuando la API falla', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
    }) as unknown as typeof fetch;

    renderLeaderboard();

    await waitFor(() => {
      expect(screen.getByText(/Error de conexión/i)).toBeInTheDocument();
    });
  });

  it('muestra el indicador de carga mientras espera la respuesta', () => {
    // Promesa que nunca se resuelve para capturar el estado de carga
    global.fetch = vi.fn().mockReturnValue(new Promise(() => {})) as unknown as typeof fetch;

    renderLeaderboard();
    expect(screen.getByText('Sincronizando...')).toBeInTheDocument();
  });
});

describe('Leaderboard — navegación de sidebar', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ content: [], totalPages: 1 }),
    }) as unknown as typeof fetch;
  });

  it('navega a /dashboard al hacer click en DASHBOARD', async () => {
    renderLeaderboard();

    await waitFor(() => {
      fireEvent.click(screen.getByText('DASHBOARD'));
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard');
    });
  });

  it('navega a /upload al hacer click en SUBIR TELEMETRÍA', async () => {
    renderLeaderboard();

    await waitFor(() => {
      fireEvent.click(screen.getByText('SUBIR TELEMETRÍA'));
      expect(mockNavigate).toHaveBeenCalledWith('/upload');
    });
  });
});
