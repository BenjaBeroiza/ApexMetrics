import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  getSesiones,
  eliminarSesion,
  uploadTelemetry,
  getPuntos,
  getTrazado,
  getComparacion,
  getFeedbackIA,
} from './telemetry.service';
import { ApiError } from './http';

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

describe('telemetry.service — getSesiones', () => {
  it('retorna el historial y envía el JWT en la cabecera', async () => {
    const sesiones = [{ sessionId: 1, trackName: 'Monza', categoryName: 'GT3', bestLapTime: 104.5, uploadedAt: '2026-07-01' }];
    mockFetch.mockResolvedValueOnce(jsonResponse(200, sesiones));

    const data = await getSesiones(TOKEN);

    expect(data).toEqual(sesiones);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/telemetry/sesiones', {
      headers: { Authorization: `Bearer ${TOKEN}` },
    });
  });

  it('lanza ApiError 401 cuando el token expiró', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(401, {}));

    await expect(getSesiones(TOKEN)).rejects.toMatchObject({ status: 401 });
  });

  it('propaga el error de red', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(getSesiones(TOKEN)).rejects.toBeInstanceOf(TypeError);
  });
});

describe('telemetry.service — eliminarSesion', () => {
  it('resuelve sin error cuando el backend responde 204', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, status: 204, json: () => Promise.resolve(null) });

    await expect(eliminarSesion(TOKEN, 7)).resolves.toBeUndefined();
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/telemetry/sesiones/7', expect.objectContaining({ method: 'DELETE' }));
  });

  it('lanza ApiError 403 cuando la sesión pertenece a otro piloto', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(403, {}));

    await expect(eliminarSesion(TOKEN, 7)).rejects.toMatchObject({ status: 403 });
  });
});

describe('telemetry.service — uploadTelemetry', () => {
  const formData = new FormData();

  it('lanza ApiError 413 con mensaje de tamaño máximo', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(413, {}));

    await expect(uploadTelemetry(TOKEN, formData))
      .rejects.toMatchObject({ status: 413, message: 'EL ARCHIVO SUPERA EL TAMAÑO MÁXIMO (10 MB)' });
  });

  it('lanza ApiError con el mensaje de validación del backend ante un 400', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(400, { message: 'Cabecera CSV inválida' }));

    await expect(uploadTelemetry(TOKEN, formData))
      .rejects.toMatchObject({ status: 400, message: 'Cabecera CSV inválida' });
  });

  it('lanza ApiError 500 con mensaje genérico si el error no trae cuerpo JSON', async () => {
    mockFetch.mockResolvedValueOnce({ ok: false, status: 500, json: () => Promise.reject(new SyntaxError()) });

    await expect(uploadTelemetry(TOKEN, formData))
      .rejects.toMatchObject({ status: 500, message: 'FALLÓ LA VALIDACIÓN DEL ARCHIVO' });
  });

  it('retorna el resumen de la sesión creada en el caso feliz', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(201, { sessionId: 10 }));

    await expect(uploadTelemetry(TOKEN, formData)).resolves.toEqual({ sessionId: 10 });
  });
});

describe('telemetry.service — getPuntos / getTrazado', () => {
  it('getPuntos lanza ApiError 403 sobre sesión ajena', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(403, {}));

    await expect(getPuntos(TOKEN, 3)).rejects.toMatchObject({ status: 403 });
  });

  it('getTrazado retorna el trazado con bandera geographic', async () => {
    const path = { geographic: true, points: [{ x: 45.6, y: 9.28, speed: 210, distance: 0, lapNumber: 1 }] };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, path));

    await expect(getTrazado(TOKEN, 3)).resolves.toEqual(path);
  });

  it('getTrazado propaga error de red (OSM/backend caído)', async () => {
    mockFetch.mockRejectedValueOnce(new TypeError('Failed to fetch'));

    await expect(getTrazado(TOKEN, 3)).rejects.toBeInstanceOf(TypeError);
  });
});

describe('telemetry.service — getComparacion', () => {
  it('retorna ambas series de puntos', async () => {
    const resp = { sesionA: [], sesionB: [] };
    mockFetch.mockResolvedValueOnce(jsonResponse(200, resp));

    await expect(getComparacion(TOKEN, '1', '2')).resolves.toEqual(resp);
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/telemetry/comparacion?sessionA=1&sessionB=2',
      expect.anything()
    );
  });

  it('lanza ApiError 403 si alguna sesión no es del usuario', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(403, {}));

    await expect(getComparacion(TOKEN, '1', '2')).rejects.toMatchObject({ status: 403 });
  });
});

describe('telemetry.service — getFeedbackIA', () => {
  it('retorna el feedback generado', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(200, { sessionId: 5, feedback: 'ANÁLISIS DE FRENADA\nok' }));

    await expect(getFeedbackIA(TOKEN, 5)).resolves.toMatchObject({ sessionId: 5 });
  });

  it('lanza ApiError con mensaje "Sin permiso" ante un 403', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(403, {}));

    await expect(getFeedbackIA(TOKEN, 5)).rejects.toMatchObject({ status: 403, message: 'Sin permiso' });
  });

  it('lanza ApiError con mensaje "Error del servidor" ante un 500', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    await expect(getFeedbackIA(TOKEN, 5)).rejects.toMatchObject({ status: 500, message: 'Error del servidor' });
  });

  it('el error lanzado es instancia de ApiError y de Error', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(500, {}));

    const error = await getFeedbackIA(TOKEN, 5).catch((e) => e);
    expect(error).toBeInstanceOf(ApiError);
    expect(error).toBeInstanceOf(Error);
  });
});
