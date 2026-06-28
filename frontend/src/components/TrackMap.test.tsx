import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import TrackMap from './TrackMap';

// Leaflet manipula el DOM real (tamaños, canvas) que jsdom no provee; reemplazamos
// react-leaflet por marcadores simples que exponen las props relevantes para el test.
vi.mock('react-leaflet', () => ({
  MapContainer: ({ children, crs }: { children: React.ReactNode; crs?: unknown }) => (
    <div data-testid="map-container" data-crs-simple={crs ? 'true' : 'false'}>
      {children}
    </div>
  ),
  TileLayer: ({ attribution }: { attribution: string }) => (
    <div data-testid="tile-layer">{attribution}</div>
  ),
  Polyline: ({ positions }: { positions: unknown[] }) => (
    <div data-testid="polyline" data-count={positions.length} />
  ),
  useMap: () => ({ fitBounds: vi.fn() }),
}));

describe('TrackMap — Trazado de pistas (Bloque B)', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-jwt-token');
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  it('muestra estado de carga inicialmente', () => {
    global.fetch = vi.fn().mockReturnValue(new Promise(() => {})) as unknown as typeof fetch;
    render(<TrackMap sessionId={1} />);
    expect(screen.getByText(/Cargando trazado/i)).toBeInTheDocument();
  });

  it('dibuja mapa OSM con atribución y polyline cuando geographic=true', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        geographic: true,
        points: [
          { x: 9.281, y: 45.62, speed: 150 },
          { x: 9.282, y: 45.621, speed: 160 },
        ],
      }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });
    // Tiles de OSM con atribución visible (requisito legal)
    expect(screen.getByTestId('tile-layer')).toHaveTextContent(/OpenStreetMap/i);
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-crs-simple', 'false');
    // Leaflet usa [lat, lng]: cada punto {x, y} → [y, x]
    expect(screen.getByTestId('polyline')).toHaveAttribute('data-count', '2');
  });

  it('dibuja mapa en plano local (CRS.Simple, sin tiles) cuando geographic=false', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({
        geographic: false,
        points: [
          { x: 100, y: 200, speed: 120 },
          { x: 110, y: 210, speed: 130 },
          { x: 120, y: 220, speed: 140 },
        ],
      }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-crs-simple', 'true');
    expect(screen.queryByTestId('tile-layer')).not.toBeInTheDocument();
    expect(screen.getByTestId('polyline')).toHaveAttribute('data-count', '3');
  });

  it('muestra aviso (no un mapa vacío) cuando la sesión no tiene posición', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: false, points: [] }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/no tiene datos de posición/i)).toBeInTheDocument();
    });
    expect(screen.queryByTestId('map-container')).not.toBeInTheDocument();
  });

  it('muestra aviso de permiso cuando la API responde 403', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: false,
      status: 403,
      json: async () => ({}),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/No tienes permiso/i)).toBeInTheDocument();
    });
  });

  it('muestra error de conexión cuando fetch falla', async () => {
    global.fetch = vi.fn().mockRejectedValue(new Error('network')) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByText(/No se pudo conectar/i)).toBeInTheDocument();
    });
  });
});
