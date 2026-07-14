/**
 * Servicio de telemetría — RF04 (upload), RF05 (puntos), RF06 (comparación),
 * RF08/RF09 (historial y eliminación), trazado (UC15) y feedback IA.
 *
 * Los tipos de datos reflejan los DTOs del backend
 * (backend/src/main/java/com/apexmetrics/telemetry/dto).
 */
import { ApiError, authHeaders } from './http';

export interface SesionResumen {
  sessionId: number;
  trackName: string;
  categoryName: string;
  bestLapTime: number;
  uploadedAt: string;
}

export interface PuntoTelemetria {
  distance: number;
  speed: number;
  brake: number;
  throttle: number;
}

export interface TrackPoint {
  x: number;
  y: number;
  speed: number;
  distance: number;
  lapNumber: number;
}

export interface TrackPath {
  geographic: boolean;
  points: TrackPoint[];
}

export interface ComparacionResponse {
  sesionA: PuntoTelemetria[];
  sesionB: PuntoTelemetria[];
}

export interface FeedbackResponse {
  sessionId: number;
  feedback: string;
}

export async function getSesiones(token: string | null): Promise<SesionResumen[]> {
  const response = await fetch('/api/v1/telemetry/sesiones', {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al cargar las sesiones');
  }
  return response.json();
}

export async function eliminarSesion(token: string | null, sessionId: number): Promise<void> {
  const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}`, {
    method: 'DELETE',
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al eliminar la sesión');
  }
}

export async function uploadTelemetry(token: string | null, payload: FormData): Promise<unknown> {
  const response = await fetch('/api/v1/telemetry/upload', {
    method: 'POST',
    headers: authHeaders(token),
    body: payload,
  });
  if (response.status === 413) {
    throw new ApiError(413, 'EL ARCHIVO SUPERA EL TAMAÑO MÁXIMO (10 MB)');
  }
  if (!response.ok) {
    const errData = await response.json().catch(() => null);
    throw new ApiError(response.status, errData?.message || 'FALLÓ LA VALIDACIÓN DEL ARCHIVO');
  }
  return response.json();
}

export async function getPuntos(
  token: string | null,
  sessionId: number | string
): Promise<PuntoTelemetria[]> {
  const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}/puntos`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al cargar los puntos');
  }
  return response.json();
}

export async function getTrazado(
  token: string | null,
  sessionId: number | string
): Promise<TrackPath> {
  const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}/trazado`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al cargar el trazado');
  }
  return response.json();
}

export async function getComparacion(
  token: string | null,
  sessionA: string,
  sessionB: string
): Promise<ComparacionResponse> {
  const response = await fetch(
    `/api/v1/telemetry/comparacion?sessionA=${sessionA}&sessionB=${sessionB}`,
    { headers: authHeaders(token) }
  );
  if (!response.ok) {
    throw new ApiError(response.status, 'Error al cargar la comparación');
  }
  return response.json();
}

export async function getFeedbackIA(
  token: string | null,
  sessionId: number | string
): Promise<FeedbackResponse> {
  const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}/feedback-ia`, {
    headers: authHeaders(token),
  });
  if (!response.ok) {
    throw new ApiError(response.status, response.status === 403 ? 'Sin permiso' : 'Error del servidor');
  }
  return response.json();
}
