/**
 * Utilidades compartidas de la capa de servicios HTTP.
 *
 * Todos los servicios lanzan `ApiError` cuando el backend responde con un
 * estado no exitoso, de modo que los componentes puedan distinguir errores
 * de negocio (401, 403, 413, 500…) de errores de red (fetch rechazado),
 * que llegan como `TypeError` nativo del navegador.
 */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

/** Cabecera de autorización JWT para los endpoints protegidos. */
export function authHeaders(token: string | null): Record<string, string> {
  return { Authorization: `Bearer ${token}` };
}
