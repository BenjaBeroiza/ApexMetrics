import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
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
import '../styles/dashboard.css';

interface Sesion {
  sessionId: number;
  trackName: string;
  uploadedAt: string;
}

interface Punto {
  distance: number;
  speed: number;
  brake: number;
  throttle: number;
}

interface FilaComparacion {
  distance: number;
  speedA?: number;
  brakeA?: number;
  speedB?: number;
  brakeB?: number;
}

/**
 * Comparación de vueltas: permite elegir dos sesiones propias y superpone sus
 * curvas de velocidad y frenado en un mismo gráfico para identificar diferencias.
 * Consume GET /api/v1/telemetry/comparacion?sessionA&sessionB con el JWT del usuario.
 *
 * Implementa RF06 — Comparación de vueltas.
 */
export default function ComparacionVueltas() {
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');

  const [sesiones, setSesiones] = useState<Sesion[]>([]);
  const [sesionA, setSesionA] = useState<string>('');
  const [sesionB, setSesionB] = useState<string>('');
  const [datos, setDatos] = useState<FilaComparacion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Carga el historial del usuario para poblar los selectores de sesión.
  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    fetch('/api/v1/telemetry/sesiones', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((data: Sesion[]) => setSesiones(data))
      .catch(() => setError('No se pudo cargar el historial de sesiones.'));
  }, [token, navigate]);

  // Cuando ambas sesiones están elegidas (y son distintas), pide la comparación.
  useEffect(() => {
    if (!sesionA || !sesionB || sesionA === sesionB) {
      setDatos([]);
      return;
    }
    let activo = true;
    setLoading(true);
    setError(null);
    fetch(`/api/v1/telemetry/comparacion?sessionA=${sesionA}&sessionB=${sesionB}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => {
        if (r.status === 403) return Promise.reject('forbidden');
        if (!r.ok) return Promise.reject('error');
        return r.json();
      })
      .then((resp: { sesionA: Punto[]; sesionB: Punto[] }) => {
        if (!activo) return;
        setDatos(combinarPuntos(resp.sesionA, resp.sesionB));
      })
      .catch((e) => {
        if (!activo) return;
        setError(e === 'forbidden' ? 'No tienes permiso sobre alguna de las sesiones.' : 'No se pudo cargar la comparación.');
      })
      .finally(() => activo && setLoading(false));
    return () => {
      activo = false;
    };
  }, [sesionA, sesionB, token]);

  // Alinea por índice de muestra los puntos de ambas sesiones en una sola serie.
  const combinarPuntos = (a: Punto[], b: Punto[]): FilaComparacion[] => {
    const max = Math.max(a.length, b.length);
    const filas: FilaComparacion[] = [];
    for (let i = 0; i < max; i++) {
      filas.push({
        distance: a[i]?.distance ?? b[i]?.distance ?? i,
        speedA: a[i]?.speed,
        brakeA: a[i]?.brake,
        speedB: b[i]?.speed,
        brakeB: b[i]?.brake,
      });
    }
    return filas;
  };

  const etiqueta = (s: Sesion) =>
    `${s.trackName} · ${new Date(s.uploadedAt).toLocaleDateString()} (#${s.sessionId})`;

  return (
    <div className="app-layout">
      <main className="main-content">
        <div className="page-header" style={{ marginBottom: '1.5rem' }}>
          <button
            onClick={() => navigate('/dashboard')}
            style={{ background: 'none', border: 'none', color: 'var(--text-muted)', cursor: 'pointer', display: 'inline-flex', alignItems: 'center', gap: '0.4rem', marginBottom: '1rem' }}
          >
            <ArrowLeft size={16} /> VOLVER AL DASHBOARD
          </button>
          <h1 style={{ textTransform: 'uppercase' }}>COMPARACIÓN DE VUELTAS</h1>
          <p>Superpone las curvas de velocidad y frenado de dos sesiones.</p>
        </div>

        {sesiones.length < 2 ? (
          <div style={{ textAlign: 'center', padding: '3rem', border: '1px dashed #333' }}>
            <p style={{ color: 'var(--text-muted)' }}>Necesitas al menos dos sesiones para comparar.</p>
          </div>
        ) : (
          <>
            <div style={{ display: 'flex', gap: '1.5rem', marginBottom: '2rem', flexWrap: 'wrap' }}>
              <div className="input-group">
                <label style={{ color: '#E63946' }}>SESIÓN A</label>
                <select value={sesionA} onChange={(e) => setSesionA(e.target.value)} aria-label="Sesión A">
                  <option value="">— Selecciona —</option>
                  {sesiones.map((s) => (
                    <option key={s.sessionId} value={s.sessionId}>{etiqueta(s)}</option>
                  ))}
                </select>
              </div>
              <div className="input-group">
                <label style={{ color: '#1B6CA8' }}>SESIÓN B</label>
                <select value={sesionB} onChange={(e) => setSesionB(e.target.value)} aria-label="Sesión B">
                  <option value="">— Selecciona —</option>
                  {sesiones.map((s) => (
                    <option key={s.sessionId} value={s.sessionId}>{etiqueta(s)}</option>
                  ))}
                </select>
              </div>
            </div>

            {sesionA && sesionB && sesionA === sesionB && (
              <p style={{ color: 'var(--error-red)' }}>Elige dos sesiones distintas.</p>
            )}
            {loading && <div className="loading-state"><span className="blink-text">Cargando comparación...</span></div>}
            {error && <div className="loading-state"><span style={{ color: 'var(--error-red)' }}>{error}</span></div>}

            {!loading && !error && datos.length > 0 && (
              <div style={{ width: '100%', height: 460 }}>
                <ResponsiveContainer width="100%" height="100%">
                  <LineChart data={datos} margin={{ top: 16, right: 24, left: 8, bottom: 16 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#333" />
                    <XAxis dataKey="distance" stroke="#888" tick={{ fontSize: 11 }} />
                    <YAxis yAxisId="brake" orientation="left" stroke="#1B6CA8" domain={[0, 1]} tick={{ fontSize: 11 }} />
                    <YAxis yAxisId="speed" orientation="right" stroke="#E63946" tick={{ fontSize: 11 }} />
                    <Tooltip contentStyle={{ background: '#111', border: '1px solid #333' }} />
                    <Legend />
                    <Line yAxisId="speed" type="monotone" dataKey="speedA" name="Velocidad A" stroke="#E63946" dot={false} strokeWidth={2} connectNulls isAnimationActive={false} />
                    <Line yAxisId="speed" type="monotone" dataKey="speedB" name="Velocidad B" stroke="#E63946" strokeDasharray="5 4" dot={false} strokeWidth={2} connectNulls isAnimationActive={false} />
                    <Line yAxisId="brake" type="monotone" dataKey="brakeA" name="Freno A" stroke="#1B6CA8" dot={false} strokeWidth={2} connectNulls isAnimationActive={false} />
                    <Line yAxisId="brake" type="monotone" dataKey="brakeB" name="Freno B" stroke="#1B6CA8" strokeDasharray="5 4" dot={false} strokeWidth={2} connectNulls isAnimationActive={false} />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            )}
          </>
        )}
      </main>
    </div>
  );
}
