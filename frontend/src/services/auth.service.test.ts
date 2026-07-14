import { describe, it, expect, vi, beforeEach } from 'vitest';
import { login, register, forgotPassword, resetPassword } from './auth.service';
import { ApiError } from './http';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const jsonResponse = (status: number, body: unknown) => ({
  ok: status >= 200 && status < 300,
  status,
  json: () => Promise.resolve(body),
});

beforeEach(() => {
  mockFetch.mockReset();
});

describe('auth.service — login', () => {
  it('retorna los datos de sesión cuando las credenciales son válidas', async () => {
    const payload = { token: 'jwt-123', username: 'piloto', email: 'p@apex.sim', country: 'Chile' };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, payload));

    const data = await login({ email: 'p@apex.sim', password: 'clave12345' });

    expect(data).toEqual(payload);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/auth/login', expect.objectContaining({
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
    }));
  });

  it('lanza ApiError con status 401 ante credenciales inválidas', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(401, { message: 'Unauthorized' }));

    await expect(login({ email: 'p@apex.sim', password: 'mala12345' }))
      .rejects.toMatchObject({ name: 'ApiError', status: 401 });
  });

  it('lanza ApiError con status 500 ante un error del servidor', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    await expect(login({ email: 'p@apex.sim', password: 'clave12345' }))
      .rejects.toBeInstanceOf(ApiError);
  });

  it('propaga el error de red (fetch rechazado) sin convertirlo en ApiError', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(login({ email: 'p@apex.sim', password: 'clave12345' }))
      .rejects.toBeInstanceOf(TypeError);
  });
});

describe('auth.service — register', () => {
  const payload = { username: 'piloto', email: 'p@apex.sim', password: 'clave12345', country: 'Chile' };

  it('retorna los datos del usuario creado', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(201, { email: 'p@apex.sim', country: 'Chile' }));

    const data = await register(payload);

    expect(data.email).toBe('p@apex.sim');
  });

  it('lanza ApiError con el mensaje del backend ante un 400', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(400, { message: 'El correo ya está registrado' }));

    await expect(register(payload))
      .rejects.toMatchObject({ status: 400, message: 'El correo ya está registrado' });
  });

  it('lanza ApiError con mensaje genérico si el cuerpo del error no es JSON', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: false,
      status: 500,
      json: () => Promise.reject(new SyntaxError('not json')),
    });

    await expect(register(payload))
      .rejects.toMatchObject({ status: 500, message: 'Error en el registro' });
  });

  it('propaga el error de red (timeout / conexión caída)', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('NetworkError'));

    await expect(register(payload)).rejects.toBeInstanceOf(TypeError);
  });
});

describe('auth.service — forgotPassword (UC03 paso 1)', () => {
  it('envía el email y retorna el mensaje genérico del backend', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(200, { message: 'Si el correo está registrado, recibirás instrucciones...' }));

    const data = await forgotPassword('piloto@apex.sim');

    expect(data.message).toMatch(/correo está registrado/);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/auth/forgot-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ email: 'piloto@apex.sim' }),
    }));
  });

  it('lanza ApiError 500 ante un error del servidor', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    await expect(forgotPassword('piloto@apex.sim')).rejects.toMatchObject({ status: 500 });
  });

  it('propaga el error de red', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(forgotPassword('piloto@apex.sim')).rejects.toBeInstanceOf(TypeError);
  });
});

describe('auth.service — resetPassword (UC03 paso 2)', () => {
  it('canjea el token por la nueva contraseña', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(200, { message: 'Tu contraseña ha sido restablecida correctamente.' }));

    const data = await resetPassword('tok-123', 'claveNueva123');

    expect(data.message).toMatch(/restablecida/);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/auth/reset-password', expect.objectContaining({
      method: 'POST',
      body: JSON.stringify({ token: 'tok-123', newPassword: 'claveNueva123' }),
    }));
  });

  it('lanza ApiError 400 con token expirado, usado o inexistente', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(400, { message: 'Token inválido' }));

    await expect(resetPassword('tok-viejo', 'claveNueva123'))
      .rejects.toMatchObject({ status: 400, message: 'Token inválido' });
  });

  it('usa mensaje genérico si el 400 no trae cuerpo JSON', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 400, json: () => Promise.reject(new SyntaxError()) });

    await expect(resetPassword('tok-viejo', 'claveNueva123'))
      .rejects.toMatchObject({ message: 'El enlace de reseteo no es válido o ya expiró' });
  });
});
