import { useState, useEffect } from 'react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';

interface PuntoTelemetria {
  distance: number;
  speed: number;
  brake: number;
  throttle: number;
}

interface SessionChartProps {
  /** Identificador de la sesión cuyas curvas se quieren graficar. */
  sessionId: number | string;
}

/**
 * Dashboard analítico de una sesión: grafica las curvas de velocidad y frenado
 * sincronizadas por distancia recorrida en pista. Consume el endpoint
 * GET /api/v1/telemetry/sesiones/{id}/puntos con el JWT del usuario autenticado.
 *
 * Eje X: distancia (m). Eje Y derecho: velocidad (rojo). Eje Y izquierdo: freno (azul).
 *
 * Implementa RF05 — Dashboard analítico.
 */
export default function SessionChart({ sessionId }: SessionChartProps) {
  const [puntos, setPuntos] = useState<PuntoTelemetria[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const token = localStorage.getItem('apex_token');

  useEffect(() => {
    let activo = true;
    const cargarPuntos = async () => {
      setLoading(true);
      setError(null);
      try {
        const response = await fetch(`/api/v1/telemetry/sesiones/${sessionId}/puntos`, {
          headers: { Authorization: `Bearer ${token}` },
        });
        if (response.status === 403) {
          if (activo) setError('No tienes permiso para ver esta sesión.');
          return;
        }
        if (!response.ok) throw new Error('Error al cargar los puntos');
        const data: PuntoTelemetria[] = await response.json();
        if (activo) setPuntos(data);
      } catch (_err) {
        if (activo) setError('No se pudo conectar con el servidor.');
      } finally {
        if (activo) setLoading(false);
      }
    };
    cargarPuntos();
    return () => {
      activo = false;
    };
  }, [sessionId, token]);

  if (loading) {
    return (
      <div className="loading-state">
        <span className="blink-text">Cargando análisis...</span>
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

  if (puntos.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '3rem', border: '1px dashed #333' }}>
        <p style={{ color: 'var(--text-muted)' }}>
          Esta sesión no tiene puntos de telemetría para graficar.
        </p>
      </div>
    );
  }

  return (
    <div style={{ width: '100%', height: 420 }}>
      <ResponsiveContainer width="100%" height="100%">
        <LineChart data={puntos} margin={{ top: 16, right: 24, left: 8, bottom: 16 }}>
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis
            dataKey="distance"
            stroke="#888"
            tick={{ fontSize: 11 }}
            label={{ value: 'Distancia (m)', position: 'insideBottom', offset: -8, fill: '#888' }}
          />
          <YAxis
            yAxisId="brake"
            orientation="left"
            stroke="#1B6CA8"
            domain={[0, 1]}
            tick={{ fontSize: 11 }}
            label={{ value: 'Freno', angle: -90, position: 'insideLeft', fill: '#1B6CA8' }}
          />
          <YAxis
            yAxisId="speed"
            orientation="right"
            stroke="#E63946"
            tick={{ fontSize: 11 }}
            label={{ value: 'Velocidad', angle: 90, position: 'insideRight', fill: '#E63946' }}
          />
          <Tooltip
            contentStyle={{ background: '#111', border: '1px solid #333' }}
            labelFormatter={(d) => `Distancia: ${d} m`}
          />
          <Legend />
          <Line
            yAxisId="speed"
            type="monotone"
            dataKey="speed"
            name="Velocidad"
            stroke="#E63946"
            dot={false}
            strokeWidth={2}
            isAnimationActive={false}
          />
          <Line
            yAxisId="brake"
            type="monotone"
            dataKey="brake"
            name="Freno"
            stroke="#1B6CA8"
            dot={false}
            strokeWidth={2}
            isAnimationActive={false}
          />
        </LineChart>
      </ResponsiveContainer>
    </div>
  );
}
