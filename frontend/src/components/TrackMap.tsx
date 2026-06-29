import { useState, useEffect, useCallback, useRef } from 'react';
import { MapContainer, TileLayer, useMap, CircleMarker, Tooltip } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

// ─────────────────────────────────────────────
// Tipos
// ─────────────────────────────────────────────

interface TrackPoint {
  x: number;
  y: number;
  speed: number;
  distance: number;
  lapNumber: number;
}

interface TrackPath {
  geographic: boolean;
  points: TrackPoint[];
}

interface TrackMapProps {
  /** Identificador de la sesión cuyo trazado se quiere dibujar. */
  sessionId: number | string;
}

// ─────────────────────────────────────────────
// Utilidades de color
// ─────────────────────────────────────────────

/** Calcula el color HSL para una velocidad normalizada t ∈ [0, 1].
 *  t = 0 → azul (velocidad baja)  |  t = 1 → rojo (velocidad alta)
 *  Fórmula del Hito 5: hsl(240 - t*240, 90%, 50%)
 */
function speedColorRgb(t: number): [number, number, number] {
  const hue = (240 - t * 240) / 360;
  // Conversión HSL → RGB (s=0.9, l=0.5)
  const s = 0.9;
  const l = 0.5;
  const hue2rgb = (p: number, q: number, h: number) => {
    if (h < 0) h += 1;
    if (h > 1) h -= 1;
    if (h < 1 / 6) return p + (q - p) * 6 * h;
    if (h < 1 / 2) return q;
    if (h < 2 / 3) return p + (q - p) * (2 / 3 - h) * 6;
    return p;
  };
  const q = l < 0.5 ? l * (1 + s) : l + s - l * s;
  const p = 2 * l - q;
  return [
    Math.round(hue2rgb(p, q, hue + 1 / 3) * 255),
    Math.round(hue2rgb(p, q, hue) * 255),
    Math.round(hue2rgb(p, q, hue - 1 / 3) * 255),
  ];
}

// ─────────────────────────────────────────────
// Componente: FitBounds
// ─────────────────────────────────────────────

function FitBounds({ positions }: { positions: L.LatLngExpression[] }) {
  const map = useMap();
  useEffect(() => {
    if (positions.length > 0) {
      map.fitBounds(positions as L.LatLngBoundsExpression, { padding: [20, 20] });
    }
  }, [map, positions]);
  return null;
}

// ─────────────────────────────────────────────
// Componente: Capa Canvas para gradiente de velocidad
// ─────────────────────────────────────────────

interface GradientLayerProps {
  points: TrackPoint[];
  minSpeed: number;
  maxSpeed: number;
  onHover: (speed: number | null) => void;
}

function GradientLayer({ points, minSpeed, maxSpeed, onHover }: GradientLayerProps) {
  const map = useMap();
  const canvasRef = useRef<HTMLCanvasElement | null>(null);

  const draw = useCallback(() => {
    if (!canvasRef.current || points.length < 2) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // FIX 2 — Alinear el canvas con el panel de Leaflet.
    // overlayPane se desplaza mediante CSS transform al panear/zoomar,
    // así que debemos reposicionar el canvas en el origen del layer-space
    // para que el trazado dibujado coincida con los marcadores nativos de Leaflet.
    const topLeft = map.containerPointToLayerPoint([0, 0]);
    L.DomUtil.setPosition(canvas, topLeft);

    const size = map.getSize();
    canvas.width = size.x;
    canvas.height = size.y;
    ctx.clearRect(0, 0, size.x, size.y);

    const speedRange = maxSpeed - minSpeed || 1;

    for (let i = 0; i < points.length - 1; i++) {
      const p1 = points[i];
      const p2 = points[i + 1];

      // Usar latLngToLayerPoint (espacio de capa) en lugar de latLngToContainerPoint
      // para que las coordenadas estén en el mismo sistema que el canvas reposicionado.
      const sp1 = map.latLngToLayerPoint([p1.y, p1.x]);
      const sp2 = map.latLngToLayerPoint([p2.y, p2.x]);

      const t = (p1.speed - minSpeed) / speedRange;
      const [r, g, b] = speedColorRgb(Math.max(0, Math.min(1, t)));

      ctx.beginPath();
      ctx.moveTo(sp1.x, sp1.y);
      ctx.lineTo(sp2.x, sp2.y);
      ctx.strokeStyle = `rgb(${r},${g},${b})`;
      ctx.lineWidth = 3;
      ctx.lineCap = 'round';
      ctx.stroke();
    }
  }, [map, points, minSpeed, maxSpeed]);

  useEffect(() => {
    const overlayPane = map.getPanes().overlayPane;
    if (!overlayPane) return;

    // Crear canvas overlay
    const canvas = document.createElement('canvas');
    canvas.style.position = 'absolute';
    canvas.style.top = '0';
    canvas.style.left = '0';
    canvas.style.pointerEvents = 'none';
    canvas.style.zIndex = '400';
    overlayPane.appendChild(canvas);
    canvasRef.current = canvas;

    const redraw = () => draw();
    map.on('move zoom resize viewreset', redraw);

    // Hover: encontrar punto más cercano al cursor
    const handleMouseMove = (e: L.LeafletMouseEvent) => {
      if (points.length === 0) return;
      const cursor = e.latlng;
      let closestDist = Infinity;
      let closestSpeed: number | null = null;
      for (const p of points) {
        // En Leaflet, distanceTo acepta [lat, lng] directamente
        const dist = cursor.distanceTo([p.y, p.x]);
        if (dist < closestDist) {
          closestDist = dist;
          closestSpeed = p.speed;
        }
      }
      onHover(closestDist < 500 ? closestSpeed : null);
    };
    const handleMouseOut = () => onHover(null);
    map.on('mousemove', handleMouseMove);
    map.on('mouseout', handleMouseOut);

    draw();

    return () => {
      map.off('move zoom resize viewreset', redraw);
      map.off('mousemove', handleMouseMove);
      map.off('mouseout', handleMouseOut);
      canvas.remove();
    };
  }, [map, draw, points, onHover]);

  return null;
}

// ─────────────────────────────────────────────
// Componente: Leyenda de gradiente
// ─────────────────────────────────────────────

function SpeedLegend({ minSpeed, maxSpeed }: { minSpeed: number; maxSpeed: number }) {
  return (
    <div className="track-legend">
      <p className="track-legend__title">VELOCIDAD (km/h)</p>
      <div className="track-legend__bar" />
      <div className="track-legend__labels">
        <span>{Math.round(minSpeed)}</span>
        <span>{Math.round(maxSpeed)}</span>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Componente: Marcadores de sector
// ─────────────────────────────────────────────

interface SectorMarkersProps {
  positions: L.LatLngExpression[];
  geographic: boolean;
}

function SectorMarkers({ positions, geographic }: SectorMarkersProps) {
  if (positions.length < 6) return null;
  const total = positions.length;
  const s1Idx = Math.floor(total / 3);
  const s2Idx = Math.floor((2 * total) / 3);

  const markers = [
    { idx: 0,      label: 'Inicio / Meta', color: '#00FFFF' },
    { idx: s1Idx,  label: 'Sector 1',      color: '#E63946' },
    { idx: s2Idx,  label: 'Sector 2',      color: '#FFD700' },
  ];

  return (
    <>
      {markers.map(({ idx, label, color }) => (
        <CircleMarker
          key={label}
          center={positions[idx] as L.LatLngExpression}
          radius={geographic ? 8 : 6}
          pathOptions={{ color, fillColor: color, fillOpacity: 0.9, weight: 2 }}
        >
          <Tooltip direction="top" permanent={false}>
            <span style={{ fontFamily: 'monospace', fontSize: '0.8rem' }}>{label}</span>
          </Tooltip>
        </CircleMarker>
      ))}
    </>
  );
}

// ─────────────────────────────────────────────
// Componente: MapContent (Definido FUERA para evitar ciclos de remounting en React)
// ─────────────────────────────────────────────

interface MapContentProps {
  path: TrackPath;
  filteredPoints: TrackPoint[];
  minSpeed: number;
  maxSpeed: number;
  positions: L.LatLngExpression[];
  setHoveredSpeed: (speed: number | null) => void;
  setTileError: (error: boolean) => void;
}

function MapContent({
  path,
  filteredPoints,
  minSpeed,
  maxSpeed,
  positions,
  setHoveredSpeed,
  setTileError,
}: MapContentProps) {
  const map = useMap();

  useEffect(() => {
    const onTileError = () => setTileError(true);
    map.on('tileerror', onTileError);
    return () => {
      map.off('tileerror', onTileError);
    };
  }, [map, setTileError]);

  return (
    <>
      {path.geographic && (
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />
      )}
      <GradientLayer
        points={filteredPoints}
        minSpeed={minSpeed}
        maxSpeed={maxSpeed}
        onHover={setHoveredSpeed}
      />
      <SectorMarkers positions={positions} geographic={path.geographic} />
      <FitBounds positions={positions} />
    </>
  );
}

// ─────────────────────────────────────────────
// Componente principal: TrackMap
// ─────────────────────────────────────────────

/**
 * Dibuja el trazado (recorrido 2D) de una sesión sobre un mapa Leaflet con:
 *  - Gradiente de velocidad via Canvas overlay (azul lento → rojo rápido)
 *  - Marcadores de sectores (Inicio/Meta, Sector 1, Sector 2)
 *  - Selector de vuelta: permite elegir la vuelta a visualizar o superponerlas
 *  - Tooltip de velocidad al pasar el cursor sobre la pista
 *  - Leyenda de velocidad mín/máx
 *  - Manejo de errores de tiles OSM con banner de alerta
 *
 * Consume GET /api/v1/telemetry/sesiones/{id}/trazado con JWT del usuario.
 *
 * Leaflet usa [lat, lng] → cada punto {x, y} se mapea a [y, x].
 *  - geographic = true  → coordenadas GPS (iRacing) sobre tiles OSM
 *  - geographic = false → plano local del circuito (Assetto Corsa) con CRS.Simple
 *
 * Implementa el trazado de pistas Bloque C — Gradiente, Sectores y Comparación de Vueltas.
 */
export default function TrackMap({ sessionId }: TrackMapProps) {
  const [path, setPath] = useState<TrackPath | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [tileError, setTileError] = useState(false);

  // Vuelta seleccionada: 0 = todas, N = vuelta concreta
  const [selectedLap, setSelectedLap] = useState<number>(0);
  const [hoveredSpeed, setHoveredSpeed] = useState<number | null>(null);

  const token = localStorage.getItem('apex_token');

  useEffect(() => {
    let activo = true;
    const cargarTrazado = async () => {
      setLoading(true);
      setError(null);
      setTileError(false);
      try {
        const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}/trazado`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 403) {
          if (activo) setError('No tienes permiso para ver esta sesión.');
          return;
        }
        if (!response.ok) throw new Error('Error al cargar el trazado');
        const data: TrackPath = await response.json();
        if (activo) setPath(data);
      } catch (_err) {
        if (activo) setError('No se pudo conectar con el servidor.');
      } finally {
        if (activo) setLoading(false);
      }
    };
    cargarTrazado();
    return () => { activo = false; };
  }, [sessionId, token]);

  if (loading) {
    return (
      <div className="loading-state">
        <span className="blink-text">Cargando trazado...</span>
      </div>
    );
  }

  if (error) {
    return (
      <div className="loading-state">
        <span style={{ color: 'var(--error-red)' }}>{error}</span>
      </div>
    );
  }

  if (!path || path.points.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem', border: '1px dashed #333' }}>
        <p style={{ color: 'var(--text-muted)' }}>
          Esta sesión no tiene datos de posición para dibujar el trazado.
        </p>
      </div>
    );
  }

  // ── Vueltas disponibles ──────────────────────────────────
  const laps = Array.from(new Set(path.points.map((p) => p.lapNumber))).sort((a, b) => a - b);
  const hasMultipleLaps = laps.length > 1;

  // Filtrar puntos según la vuelta seleccionada
  const filteredPoints =
    selectedLap === 0 ? path.points : path.points.filter((p) => p.lapNumber === selectedLap);

  // Leaflet usa [lat, lng] → [y, x]
  const positions: L.LatLngExpression[] = filteredPoints.map((p) => [p.y, p.x]);

  // ── Estadísticas de velocidad ─────────────────────────────
  const speeds = filteredPoints.map((p) => p.speed);
  const minSpeed = Math.min(...speeds);
  const maxSpeed = Math.max(...speeds);

  // FIX 1 — Pasar siempre un CRS concreto a MapContainer.
  // Si se pasa undefined, Leaflet lo interpreta como CRS=undefined y rompe
  // la proyección internamente (latLngToPoint falla). Usamos EPSG3857 para
  // mapas geográficos (tiles OSM) y Simple para el plano local (Assetto Corsa).
  const mapCrs = L && L.CRS
    ? (path.geographic ? L.CRS.EPSG3857 : L.CRS.Simple)
    : L.CRS.EPSG3857;

  return (
    <div className="trackmap-wrapper">
      {/* ── Banner de error de tiles ─────────────────────── */}
      {tileError && (
        <div className="trackmap-tile-error">
          ⚠ Error al cargar el mapa. Verifica tu conexión o el servidor de mapas.
        </div>
      )}

      {/* ── Controles superiores ─────────────────────────── */}
      <div className="trackmap-controls">
        {hasMultipleLaps && (
          <div className="trackmap-lap-selector">
            <label className="trackmap-lap-label">VUELTA</label>
            <div className="trackmap-lap-buttons">
              <button
                className={`trackmap-lap-btn${selectedLap === 0 ? ' active' : ''}`}
                onClick={() => setSelectedLap(0)}
              >
                TODAS
              </button>
              {laps.map((lap) => (
                <button
                  key={lap}
                  className={`trackmap-lap-btn${selectedLap === lap ? ' active' : ''}`}
                  onClick={() => setSelectedLap(lap)}
                >
                  V{lap}
                </button>
              ))}
            </div>
          </div>
        )}

        {/* ── Tooltip de velocidad al hover ────────────────── */}
        {hoveredSpeed !== null && (
          <div className="trackmap-speed-tooltip">
            <span className="trackmap-speed-value">{Math.round(hoveredSpeed)}</span>
            <span className="trackmap-speed-unit"> km/h</span>
          </div>
        )}
      </div>

      {/* ── Mapa ─────────────────────────────────────────── */}
      <div style={{ width: '100%', height: 480, position: 'relative' }}>
        <MapContainer
          style={{ width: '100%', height: '100%' }}
          crs={mapCrs}
          center={positions[0]}
          zoom={path.geographic ? 15 : 0}
          scrollWheelZoom
        >
          <MapContent
            path={path}
            filteredPoints={filteredPoints}
            minSpeed={minSpeed}
            maxSpeed={maxSpeed}
            positions={positions}
            setHoveredSpeed={setHoveredSpeed}
            setTileError={setTileError}
          />
        </MapContainer>

        {/* ── Leyenda de gradiente ─────────────────────────── */}
        <SpeedLegend minSpeed={minSpeed} maxSpeed={maxSpeed} />
      </div>
    </div>
  );
}
