import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
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
  CircleMarker: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="circle-marker">{children}</div>
  ),
  Tooltip: ({ children }: { children: React.ReactNode }) => (
    <span data-testid="marker-tooltip">{children}</span>
  ),
  useMap: () => ({
    fitBounds: vi.fn(),
    getSize: () => ({ x: 800, y: 480 }),
    getPanes: () => ({ overlayPane: document.createElement('div') }),
    latLngToContainerPoint: () => ({ x: 0, y: 0 }),
    on: vi.fn(),
    off: vi.fn(),
  }),
}));

// Muestra de puntos de telemetría con lapNumber y distance para Bloque C
const geoPoints = [
  { x: 9.281, y: 45.62, speed: 80, distance: 0, lapNumber: 1 },
  { x: 9.282, y: 45.621, speed: 150, distance: 100, lapNumber: 1 },
  { x: 9.283, y: 45.622, speed: 200, distance: 200, lapNumber: 1 },
  { x: 9.284, y: 45.623, speed: 120, distance: 300, lapNumber: 1 },
  { x: 9.285, y: 45.624, speed: 90, distance: 400, lapNumber: 1 },
  { x: 9.286, y: 45.625, speed: 180, distance: 500, lapNumber: 1 },
];

const multiLapPoints = [
  { x: 9.281, y: 45.62, speed: 80, distance: 0, lapNumber: 1 },
  { x: 9.282, y: 45.621, speed: 150, distance: 100, lapNumber: 1 },
  { x: 9.283, y: 45.622, speed: 200, distance: 200, lapNumber: 2 },
  { x: 9.284, y: 45.623, speed: 120, distance: 300, lapNumber: 2 },
];

const localPoints = [
  { x: 100, y: 200, speed: 80, distance: 0, lapNumber: 1 },
  { x: 110, y: 210, speed: 120, distance: 1, lapNumber: 1 },
  { x: 120, y: 220, speed: 140, distance: 2, lapNumber: 1 },
  { x: 130, y: 230, speed: 160, distance: 3, lapNumber: 1 },
  { x: 140, y: 240, speed: 180, distance: 4, lapNumber: 1 },
  { x: 150, y: 250, speed: 100, distance: 5, lapNumber: 1 },
];

describe('TrackMap — Trazado de pistas (Bloque B + C)', () => {
  beforeEach(() => {
    localStorage.setItem('apex_token', 'fake-jwt-token');
  });

  afterEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  // ── Bloque B — Tests heredados ───────────────────────────

  it('muestra estado de carga inicialmente', () => {
    global.fetch = vi.fn().mockReturnValue(new Promise(() => {})) as unknown as typeof fetch;
    render(<TrackMap sessionId={1} />);
    expect(screen.getByText(/Cargando trazado/i)).toBeInTheDocument();
  });

  it('dibuja mapa OSM con atribución cuando geographic=true', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: true, points: geoPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });
    // Tiles de OSM con atribución visible (requisito legal)
    expect(screen.getByTestId('tile-layer')).toHaveTextContent(/OpenStreetMap/i);
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-crs-simple', 'false');
  });

  it('dibuja mapa en plano local (CRS.Simple, sin tiles) cuando geographic=false', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: false, points: localPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });
    expect(screen.getByTestId('map-container')).toHaveAttribute('data-crs-simple', 'true');
    expect(screen.queryByTestId('tile-layer')).not.toBeInTheDocument();
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

  // ── Bloque C — Nuevas funcionalidades ────────────────────

  it('Bloque C — renderiza marcadores de sector cuando hay suficientes puntos', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: true, points: geoPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });
    // Deben aparecer marcadores de sector (Inicio/Meta, Sector 1, Sector 2)
    const markers = screen.getAllByTestId('circle-marker');
    expect(markers.length).toBeGreaterThanOrEqual(3);

    const tooltips = screen.getAllByTestId('marker-tooltip');
    const labels = tooltips.map((t) => t.textContent);
    expect(labels).toContain('Inicio / Meta');
    expect(labels).toContain('Sector 1');
    expect(labels).toContain('Sector 2');
  });

  it('Bloque C — muestra selector de vueltas cuando hay múltiples lapNumbers', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: false, points: multiLapPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={2} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });

    // El selector de vueltas debe aparecer con botones TODAS, V1 y V2
    expect(screen.getByText('TODAS')).toBeInTheDocument();
    expect(screen.getByText('V1')).toBeInTheDocument();
    expect(screen.getByText('V2')).toBeInTheDocument();
  });

  it('Bloque C — no muestra selector de vueltas con una sola vuelta', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: true, points: geoPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });

    // No debe aparecer el selector si solo hay una vuelta
    expect(screen.queryByText('TODAS')).not.toBeInTheDocument();
    expect(screen.queryByText('V1')).not.toBeInTheDocument();
  });

  it('Bloque C — filtra puntos al seleccionar una vuelta específica', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: false, points: multiLapPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={2} />);

    await waitFor(() => {
      expect(screen.getByText('V1')).toBeInTheDocument();
    });

    // Seleccionar vuelta 1
    fireEvent.click(screen.getByText('V1'));

    // El mapa aún debe estar visible tras cambio de vuelta
    expect(screen.getByTestId('map-container')).toBeInTheDocument();
  });

  it('Bloque C — muestra la leyenda de velocidad en el mapa', async () => {
    global.fetch = vi.fn().mockResolvedValue({
      ok: true,
      status: 200,
      json: async () => ({ geographic: true, points: geoPoints }),
    }) as unknown as typeof fetch;

    render(<TrackMap sessionId={1} />);

    await waitFor(() => {
      expect(screen.getByTestId('map-container')).toBeInTheDocument();
    });

    // Leyenda de gradiente de velocidad
    expect(screen.getByText(/VELOCIDAD/i)).toBeInTheDocument();
  });
});
