/**
 * Servicio de perfil de usuario — RF03 (ver y actualizar perfil).
 */
import { ApiError, authHeaders } from './http';

export interface ProfileData {
  username: string;
  email: string;
  country: string;
  role?: string;
}

export async function getProfile(token: string | null): Promise<ProfileData> {
  const response = await fetch('/api/v1/users/profile', {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al cargar el perfil');
  }
  return response.json();
}

export async function updateProfile(
  token: string | null,
  changes: { country: string }
): Promise<ProfileData> {
  const response = await fetch('/api/v1/users/profile', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...authHeaders(token),
    },
    body: JSON.stringify(changes),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al guardar los cambios');
  }
  return response.json();
}
