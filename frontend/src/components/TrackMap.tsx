import { useState, useEffect } from 'react';
import { MapContainer, TileLayer, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import 'leaflet/dist/leaflet.css';

interface TrackPoint {
  x: number;
  y: number;
  speed: number;
}

interface TrackPath {
  geographic: boolean;
  points: TrackPoint[];
}

interface TrackMapProps {
  /** Identificador de la sesión cuyo trazado se quiere dibujar. */
  sessionId: number | string;
}

/**
 * Ajusta la vista del mapa para que toda la traza quede visible. Se usa tanto en
 * modo geográfico (OSM) como en plano local (CRS.Simple), donde no hay zoom por
 * defecto que encuadre los puntos.
 */
function FitBounds({ positions }: { positions: L.LatLngExpression[] }) {
  const map = useMap();
  useEffect(() => {
    if (positions.length > 0) {
      map.fitBounds(L.latLngBounds(positions), { padding: [20, 20] });
    }
  }, [map, positions]);
  return null;
}

/**
 * Dibuja el trazado (recorrido 2D) de una sesión sobre un mapa Leaflet.
 * Consume GET /api/v1/telemetry/sesiones/{id}/trazado con el JWT del usuario.
 *
 * Leaflet usa el orden [lat, lng], por lo que cada punto {x, y} se mapea a [y, x].
 *  - geographic = true  → coordenadas GPS reales (iRacing) sobre tiles de
 *    OpenStreetMap, con la atribución visible exigida por la licencia de OSM.
 *  - geographic = false → plano local del circuito (Assetto Corsa) usando
 *    CRS.Simple, sin tiles externos.
 * Si la sesión no tiene puntos con posición, muestra un aviso en lugar de un mapa vacío.
 *
 * Implementa el trazado de pistas (Bloque B — OpenStreetMap / Leaflet).
 */
export default function TrackMap({ sessionId }: TrackMapProps) {
  const [path, setPath] = useState<TrackPath | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const token = localStorage.getItem('apex_token');

  useEffect(() => {
    let activo = true;
    const cargarTrazado = async () => {
      setLoading(true);
      setError(null);
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
    return () => {
      activo = false;
    };
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

  // Leaflet usa [lat, lng] → cada punto {x, y} se invierte a [y, x].
  const positions: L.LatLngExpression[] = path.points.map((p) => [p.y, p.x]);

  return (
    <div style={{ width: '100%', height: 480 }}>
      {path.geographic ? (
        <MapContainer
          style={{ width: '100%', height: '100%' }}
          center={positions[0]}
          zoom={15}
          scrollWheelZoom
        >
          <TileLayer
            attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          />
          <Polyline positions={positions} pathOptions={{ color: '#E63946', weight: 3 }} />
          <FitBounds positions={positions} />
        </MapContainer>
      ) : (
        <MapContainer
          style={{ width: '100%', height: '100%' }}
          crs={L.CRS.Simple}
          center={positions[0]}
          zoom={0}
          scrollWheelZoom
        >
          <Polyline positions={positions} pathOptions={{ color: '#E63946', weight: 3 }} />
          <FitBounds positions={positions} />
        </MapContainer>
      )}
    </div>
  );
}
