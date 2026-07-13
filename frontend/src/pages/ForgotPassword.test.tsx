import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import ForgotPassword from './ForgotPassword';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const renderForgot = () =>
  render(
    <MemoryRouter>
      <ForgotPassword />
    </MemoryRouter>
  );

beforeEach(() => {
  mockFetch.mockReset();
});

describe('ForgotPassword — renderizado', () => {
  it('muestra el título de la vista', () => {
    renderForgot();
    expect(screen.getByText('RESTABLECER CONTRASEÑA')).toBeInTheDocument();
  });

  it('muestra el campo de correo y el botón de envío', () => {
    renderForgot();
    expect(screen.getByPlaceholderText('correo@ejemplo.com')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /ENVIAR ENLACE/i })).toBeInTheDocument();
  });

  it('muestra el enlace para volver al login', () => {
    renderForgot();
    expect(screen.getByText(/Volver a iniciar sesión/i)).toBeInTheDocument();
  });
});

describe('ForgotPassword — flujo de solicitud', () => {
  it('muestra el mensaje genérico de éxito tras enviar el correo (anti-enumeración)', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      json: () => Promise.resolve({ message: 'Si el correo está registrado, recibirás instrucciones...' }),
    });

    renderForgot();
    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.click(screen.getByRole('button', { name: /ENVIAR ENLACE/i }));

    await waitFor(() => {
      expect(screen.getByText(/Si el correo está registrado/i)).toBeInTheDocument();
    });
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/auth/forgot-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'piloto@apex.sim' }),
    }));
  });

  it('oculta el formulario después del envío exitoso', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, status: 200, json: () => Promise.resolve({ message: 'ok' }) });

    renderForgot();
    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.click(screen.getByRole('button', { name: /ENVIAR ENLACE/i }));

    await waitFor(() => {
      expect(screen.queryByRole('button', { name: /ENVIAR ENLACE/i })).not.toBeInTheDocument();
    });
  });

  it('muestra error de conexión si la red falla', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    renderForgot();
    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.click(screen.getByRole('button', { name: /ENVIAR ENLACE/i }));

    await waitFor(() => {
      expect(screen.getByText(/Error de conexión con el servidor/i)).toBeInTheDocument();
    });
  });
});
