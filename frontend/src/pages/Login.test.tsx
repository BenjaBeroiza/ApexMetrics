import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Login from './Login';

// Helper: renderiza Login envuelto en MemoryRouter
const renderLogin = () =>
  render(
    <MemoryRouter>
      <Login />
    </MemoryRouter>
  );

describe('Login — renderizado', () => {
  it('muestra el título ApexMetrics', () => {
    renderLogin();
    expect(screen.getByText('ApexMetrics')).toBeInTheDocument();
  });

  it('muestra el campo de correo electrónico', () => {
    renderLogin();
    expect(screen.getByPlaceholderText('correo@ejemplo.com')).toBeInTheDocument();
  });

  it('muestra el campo de contraseña', () => {
    renderLogin();
    expect(screen.getByPlaceholderText('Mínimo 16 caracteres')).toBeInTheDocument();
  });

  it('muestra el botón de inicio de sesión', () => {
    renderLogin();
    expect(screen.getByRole('button', { name: /INICIAR SESIÓN/i })).toBeInTheDocument();
  });

  it('muestra el enlace a registro', () => {
    renderLogin();
    expect(screen.getByText(/Regístrate aquí/i)).toBeInTheDocument();
  });
});

describe('Login — validaciones de formulario', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra error si la contraseña tiene menos de 16 caracteres al enviar', async () => {
    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'test@apex.sim' },
    });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 16 caracteres'), {
      target: { name: 'password', value: 'corta' },
    });
    fireEvent.click(screen.getByRole('button', { name: /INICIAR SESIÓN/i }));

    await waitFor(() => {
      expect(screen.getByText(/Mínimo 16 caracteres requeridos/i)).toBeInTheDocument();
    });
  });

  it('no hay error visible antes de enviar el formulario', () => {
    renderLogin();
    expect(screen.queryByText(/Error/i)).not.toBeInTheDocument();
  });
});

describe('Login — integración con API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('guarda el token en localStorage al recibir respuesta exitosa', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ token: 'jwt-test-token', username: 'piloto1' }),
    }) as unknown as typeof fetch;

    // Mock de window.location.href para evitar errores de navegación en test
    Object.defineProperty(window, 'location', {
      value: { href: '' },
      writable: true,
    });

    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 16 caracteres'), {
      target: { name: 'password', value: 'clave_super_segura_32' },
    });
    fireEvent.click(screen.getByRole('button', { name: /INICIAR SESIÓN/i }));

    await waitFor(() => {
      expect(localStorage.getItem('apex_token')).toBe('jwt-test-token');
      expect(localStorage.getItem('apex_username')).toBe('piloto1');
    });
  });

  it('muestra error de credenciales cuando la API responde con error', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 401,
    }) as unknown as typeof fetch;

    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 16 caracteres'), {
      target: { name: 'password', value: 'clave_incorrecta_pero_larga' },
    });
    fireEvent.click(screen.getByRole('button', { name: /INICIAR SESIÓN/i }));

    await waitFor(() => {
      expect(screen.getByText(/Credenciales inválidas/i)).toBeInTheDocument();
    });
  });

  it('muestra error de conexión cuando la API falla por red', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Network Error')) as unknown as typeof fetch;

    renderLogin();

    fireEvent.change(screen.getByPlaceholderText('correo@ejemplo.com'), {
      target: { name: 'email', value: 'piloto@apex.sim' },
    });
    fireEvent.change(screen.getByPlaceholderText('Mínimo 16 caracteres'), {
      target: { name: 'password', value: 'clave_valida_para_test_larga' },
    });
    fireEvent.click(screen.getByRole('button', { name: /INICIAR SESIÓN/i }));

    await waitFor(() => {
      expect(screen.getByText(/Error de conexión/i)).toBeInTheDocument();
    });
  });
});
