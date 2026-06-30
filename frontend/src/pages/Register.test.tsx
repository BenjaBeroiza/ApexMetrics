import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import Register from './Register';

// Helper: renderiza Register envuelto en MemoryRouter
const renderRegister = () =>
  render(
    <MemoryRouter>
      <Register />
    </MemoryRouter>
  );

// Helper: completa el formulario con valores válidos
const fillValidForm = () => {
  fireEvent.change(screen.getByPlaceholderText('Ej. Piloto 1'), {
    target: { name: 'username', value: 'PilotoTest' },
  });
  fireEvent.change(screen.getByPlaceholderText('nuevo@apex.sim'), {
    target: { name: 'email', value: 'test@apex.sim' },
  });
  fireEvent.change(screen.getByPlaceholderText('Ej. Chile'), {
    target: { name: 'country', value: 'Chile' },
  });
};

describe('Register — renderizado', () => {
  it('muestra el título APEXMETRICS', () => {
    renderRegister();
    expect(screen.getByText('APEXMETRICS')).toBeInTheDocument();
  });

  it('muestra el campo de nombre de usuario', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('Ej. Piloto 1')).toBeInTheDocument();
  });

  it('muestra el campo de correo electrónico', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('nuevo@apex.sim')).toBeInTheDocument();
  });

  it('muestra el campo de país', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('Ej. Chile')).toBeInTheDocument();
  });

  it('muestra el campo de contraseña', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('Mínimo 8 caracteres')).toBeInTheDocument();
  });

  it('muestra el campo de confirmación de contraseña', () => {
    renderRegister();
    expect(screen.getByPlaceholderText('Repetir clave')).toBeInTheDocument();
  });

  it('muestra el botón de solicitar acceso', () => {
    renderRegister();
    expect(screen.getByRole('button', { name: /SOLICITAR ACCESO/i })).toBeInTheDocument();
  });

  it('muestra el enlace al inicio de sesión', () => {
    renderRegister();
    expect(screen.getByText(/YA TIENES ACCESO/i)).toBeInTheDocument();
  });
});

describe('Register — validaciones de formulario', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('muestra error si la contraseña tiene menos de 8 caracteres', async () => {
    renderRegister();
    fillValidForm();

    fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
      target: { name: 'password', value: 'corta' },
    });
    fireEvent.change(screen.getByPlaceholderText('Repetir clave'), {
      target: { name: 'confirmPassword', value: 'corta' },
    });
    fireEvent.click(screen.getByRole('button', { name: /SOLICITAR ACCESO/i }));

    await waitFor(() => {
      expect(screen.getByText(/Mínimo 8 caracteres/i)).toBeInTheDocument();
    });
  });

  it('muestra error si las contraseñas no coinciden', async () => {
    renderRegister();
    fillValidForm();

    fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
      target: { name: 'password', value: 'clave_segura_larga_16_chars' },
    });
    fireEvent.change(screen.getByPlaceholderText('Repetir clave'), {
      target: { name: 'confirmPassword', value: 'clave_diferente_tambien_larga' },
    });
    fireEvent.click(screen.getByRole('button', { name: /SOLICITAR ACCESO/i }));

    await waitFor(() => {
      expect(screen.getByText(/no coinciden/i)).toBeInTheDocument();
    });
  });
});

describe('Register — integración con API', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('redirige a login tras un registro exitoso', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ username: 'PilotoTest' }),
    }) as unknown as typeof fetch;

    renderRegister();
    fillValidForm();

    fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
      target: { name: 'password', value: 'clave_super_segura_32chars' },
    });
    fireEvent.change(screen.getByPlaceholderText('Repetir clave'), {
      target: { name: 'confirmPassword', value: 'clave_super_segura_32chars' },
    });
    fireEvent.click(screen.getByRole('button', { name: /SOLICITAR ACCESO/i }));

    // Verificar que el fetch se llamó con los datos correctos
    await waitFor(() => {
      expect(global.fetch).toHaveBeenCalledWith(
        '/api/v1/auth/register',
        expect.objectContaining({ method: 'POST' })
      );
    });
  });

  it('muestra mensaje de error cuando la API responde con error', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      json: async () => ({ message: 'El correo ya está registrado' }),
    }) as unknown as typeof fetch;

    renderRegister();
    fillValidForm();

    fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
      target: { name: 'password', value: 'clave_super_segura_32chars' },
    });
    fireEvent.change(screen.getByPlaceholderText('Repetir clave'), {
      target: { name: 'confirmPassword', value: 'clave_super_segura_32chars' },
    });
    fireEvent.click(screen.getByRole('button', { name: /SOLICITAR ACCESO/i }));

    await waitFor(() => {
      expect(screen.getByText(/ya está registrado/i)).toBeInTheDocument();
    });
  });

  it('muestra error de conexión cuando la API falla por red', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('Network Error')) as unknown as typeof fetch;

    renderRegister();
    fillValidForm();

    fireEvent.change(screen.getByPlaceholderText('Mínimo 8 caracteres'), {
      target: { name: 'password', value: 'clave_super_segura_32chars' },
    });
    fireEvent.change(screen.getByPlaceholderText('Repetir clave'), {
      target: { name: 'confirmPassword', value: 'clave_super_segura_32chars' },
    });
    fireEvent.click(screen.getByRole('button', { name: /SOLICITAR ACCESO/i }));

    await waitFor(() => {
      expect(screen.getByText(/Error de conexión/i)).toBeInTheDocument();
    });
  });
});
