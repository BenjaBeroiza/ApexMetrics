import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import TrackMap from '../components/TrackMap';
import '../styles/dashboard.css';

/**
 * Página de trazado de una sesión. Lee el id de la sesión desde la ruta
 * (/dashboard/sesiones/:id/trazado) y delega el dibujo del recorrido sobre el
 * mapa al componente TrackMap.
 *
 * Implementa el trazado de pistas (Bloque B — OpenStreetMap / Leaflet).
 */
export default function TrackView() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');

  useEffect(() => {
    if (!token) navigate('/login');
  }, [token, navigate]);

  return (
    <div className="app-layout">
      <main className="main-content">
        <div className="page-header" style={{ marginBottom: '2rem' }}>
          <button
            onClick={() => navigate('/dashboard')}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'inline-flex',
              alignItems: 'center',
              gap: '0.4rem',
              marginBottom: '1rem',
            }}
          >
            <ArrowLeft size={16} /> VOLVER AL DASHBOARD
          </button>
          <h1 style={{ textTransform: 'uppercase' }}>TRAZADO DE PISTA</h1>
          <p>Recorrido de la sesión sobre el mapa del circuito.</p>
        </div>

        {id && <TrackMap sessionId={id} />}
      </main>
    </div>
  );
}
