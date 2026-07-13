import { describe, it, expect, vi, beforeEach } from 'vitest';
import { getProfile, updateProfile } from './user.service';

const mockFetch = vi.fn();
global.fetch = mockFetch;

const jsonResponse = (status: number, body: unknown) => ({
  ok: status >= 200 && status < 300,
  status,
  json: () => Promise.resolve(body),
});

const TOKEN = 'jwt-de-prueba';

beforeEach(() => {
  mockFetch.mockReset();
});

describe('user.service — getProfile', () => {
  it('retorna el perfil del usuario autenticado', async () => {
    const profile = { username: 'piloto', email: 'p@apex.sim', country: 'Chile', role: 'PILOT' };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, profile));

    await expect(getProfile(TOKEN)).resolves.toEqual(profile);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/users/profile', {
      headers: { Authorization: `Bearer ${TOKEN}` },
    });
  });

  it('lanza ApiError 403 con token inválido', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(403, {}));

    await expect(getProfile('token-invalido')).rejects.toMatchObject({ status: 403 });
  });

  it('propaga el error de red', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(getProfile(TOKEN)).rejects.toBeInstanceOf(TypeError);
  });
});

describe('user.service — updateProfile', () => {
  it('envía el país por PUT con Content-Type y JWT, y retorna el perfil actualizado', async () => {
    const updated = { username: 'piloto', email: 'p@apex.sim', country: 'Argentina' };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, updated));

    await expect(updateProfile(TOKEN, { country: 'Argentina' })).resolves.toEqual(updated);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/users/profile', expect.objectContaining({
      method: 'PUT',
      headers: expect.objectContaining({
        'Content-Type': 'application/json',
        Authorization: `Bearer ${TOKEN}`,
      }),
      body: JSON.stringify({ country: 'Argentina' }),
    }));
  });

  it('lanza ApiError 500 ante un error del servidor', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    await expect(updateProfile(TOKEN, { country: 'Chile' })).rejects.toMatchObject({ status: 500 });
  });

  it('propaga el error de red (timeout)', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('NetworkError'));

    await expect(updateProfile(TOKEN, { country: 'Chile' })).rejects.toBeInstanceOf(TypeError);
  });
});
