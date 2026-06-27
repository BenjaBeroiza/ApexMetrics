import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Profile from './Profile';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return { ...actual, useNavigate: () => mockNavigate };
});

const renderProfile = () =>
  render(
    <MemoryRouter>
      <Profile />
    </MemoryRouter>
  );

describe('Profile — RF03', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.setItem('apex_token', 'fake-jwt-token');
    localStorage.setItem('apex_username', 'CacheUser');
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('muestra los datos del perfil traídos del backend', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        username: 'BackendUser',
        email: 'backend@apexmetrics.com',
        country: 'Chile',
        role: 'PILOT',
      }),
    }) as unknown as typeof fetch;

    renderProfile();

    await waitFor(() => {
      expect(screen.getByDisplayValue('BackendUser')).toBeInTheDocument();
      expect(screen.getByDisplayValue('backend@apexmetrics.com')).toBeInTheDocument();
      expect(screen.getByDisplayValue('Chile')).toBeInTheDocument();
    });
  });

  it('conserva el fallback de localStorage si el backend falla', async () => {
    localStorage.setItem('apex_email', 'cache@apexmetrics.com');
    global.fetch = vi.fn().mockRejectedValue(new Error('network')) as unknown as typeof fetch;

    renderProfile();

    await waitFor(() => {
      expect(screen.getByDisplayValue('CacheUser')).toBeInTheDocument();
      expect(screen.getByDisplayValue('cache@apexmetrics.com')).toBeInTheDocument();
    });
  });
});
