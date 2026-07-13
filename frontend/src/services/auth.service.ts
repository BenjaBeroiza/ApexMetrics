/**
 * Servicio de autenticación — RF01 (registro), RF02 (login) y UC03
 * (recuperación de contraseña, endpoints forgot-password / reset-password).
 */
import { ApiError } from './http';

export interface LoginCredentials {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  username: string;
  email: string;
  country?: string | null;
}

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
  country: string;
}

export interface RegisterResponse {
  email?: string;
  country?: string;
}

export interface MessageResponse {
  message: string;
}

export async function login(credentials: LoginCredentials): Promise<LoginResponse> {
  const response = await fetch('/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(credentials),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Credenciales inválidas');
  }
  return response.json();
}

export async function register(payload: RegisterPayload): Promise<RegisterResponse> {
  const response = await fetch('/api/v1/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  });
  if (!response.ok) {
    const errData = await response.json().catch(() => null);
    throw new ApiError(response.status, errData?.message || 'Error en el registro');
  }
  return response.json();
}

/**
 * UC03 paso 1: solicita el enlace de reseteo. El backend responde siempre 200
 * exista o no el correo (anti-enumeración de usuarios); en desarrollo el
 * enlace se imprime en los logs del backend.
 */
export async function forgotPassword(email: string): Promise<MessageResponse> {
  const response = await fetch('/api/v1/auth/forgot-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email }),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'No se pudo procesar la solicitud');
  }
  return response.json();
}

/**
 * UC03 paso 2: canjea el token de reseteo (un solo uso, expira a los 30 min)
 * por una nueva contraseña. 400 = token inexistente, usado o expirado.
 */
export async function resetPassword(token: string, newPassword: string): Promise<MessageResponse> {
  const response = await fetch('/api/v1/auth/reset-password', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ token, newPassword }),
  });
  if (!response.ok) {
    const errData = await response.json().catch(() => null);
    throw new ApiError(response.status, errData?.message || 'El enlace de reseteo no es válido o ya expiró');
  }
  return response.json();
}
