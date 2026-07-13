import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ResetPassword from './ResetPassword';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const renderReset = (url = '/reset-password?token=tok-123') =>
  render(
    <MemoryRouter initialEntries={[url]}>
      <ResetPassword />
    </MemoryRouter>
  );

const fillForm = (password: string, confirm: string) => {
  fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
    target: { name: 'password', value: password },
  });
  fireEvent.change(screen.getByPlaceholderText('Repetir contraseña'), {
    target: { name: 'confirmPassword', value: confirm },
  });
};

beforeEach(() => {
  mockFetch.mockReset();
});

describe('ResetPassword — renderizado y validaciones', () => {
  it('muestra el formulario cuando la URL trae token', () => {
    renderReset();
    expect(screen.getByText('NUEVA CONTRASEÑA')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i })).toBeInTheDocument();
  });

  it('muestra aviso si la URL no trae token', () => {
    renderReset('/reset-password');
    expect(screen.getByText(/no contiene un token de reseteo válido/i)).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
  });

  it('valida el mínimo de 8 caracteres sin llamar al backend', async () => {
    renderReset();
    fillForm('corta', 'corta');
    fireEvent.click(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i }));

    await waitFor(() => {
      expect(screen.getByText(/Mínimo 8 caracteres requeridos/i)).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });

  it('valida que ambas contraseñas coincidan sin llamar al backend', async () => {
    renderReset();
    fillForm('claveNueva123', 'claveDistinta123');
    fireEvent.click(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i }));

    await waitFor(() => {
      expect(screen.getByText(/no coinciden/i)).toBeInTheDocument();
    });
    expect(mockFetch).not.toHaveBeenCalled();
  });
});

describe('ResetPassword — flujo de canje del token', () => {
  it('envía token y nueva contraseña, y muestra confirmación', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ message: 'Tu contraseña ha sido restablecida correctamente.' }),
    });

    renderReset();
    fillForm('claveNueva123', 'claveNueva123');
    fireEvent.click(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i }));

    await waitFor(() => {
      expect(screen.getByText(/restablecida correctamente/i)).toBeInTheDocument();
    });
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/auth/reset-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ token: 'tok-123', newPassword: 'claveNueva123' }),
    }));
  });

  it('muestra error controlado cuando el token expiró o ya fue usado (400)', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ message: 'Token inválido' }),
    });

    renderReset();
    fillForm('claveNueva123', 'claveNueva123');
    fireEvent.click(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i }));

    await waitFor(() => {
      expect(screen.getByText(/no es válido o ya expiró/i)).toBeInTheDocument();
    });
  });

  it('muestra error de conexión si la red falla', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    renderReset();
    fillForm('claveNueva123', 'claveNueva123');
    fireEvent.click(screen.getByRole('button', { name: /RESTABLECER CONTRASEÑA/i }));

    await waitFor(() => {
      expect(screen.getByText(/Error de conexión con el servidor/i)).toBeInTheDocument();
    });
  });
});
