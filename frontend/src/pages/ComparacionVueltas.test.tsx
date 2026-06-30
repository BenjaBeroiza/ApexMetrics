import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ComparacionVueltas from './ComparacionVueltas';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderPage = () =>
  render(
    <MemoryRouter>
      <ComparacionVueltas />
    </MemoryRouter>
  );

describe('ComparacionVueltas — RF06', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-jwt-token');
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('muestra aviso cuando hay menos de dos sesiones', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [{ sessionId: 1, trackName: 'Monza', uploadedAt: '2026-05-01T10:00:00' }],
    }) as unknown as typeof fetch;

    renderPage();

    await waitFor(() => {
      expect(screen.getByText(/al menos dos sesiones/i)).toBeInTheDocument();
    });
  });

  it('muestra los selectores de Sesión A y B cuando hay dos o más sesiones', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => [
        { sessionId: 1, trackName: 'Monza', uploadedAt: '2026-05-01T10:00:00' },
        { sessionId: 2, trackName: 'Spa', uploadedAt: '2026-05-02T10:00:00' },
      ],
    }) as unknown as typeof fetch;

    renderPage();

    await waitFor(() => {
      expect(screen.getByLabelText('Sesión A')).toBeInTheDocument();
      expect(screen.getByLabelText('Sesión B')).toBeInTheDocument();
    });
  });
});
