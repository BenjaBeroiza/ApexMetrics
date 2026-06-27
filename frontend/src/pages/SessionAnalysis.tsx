import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import SessionChart from '../components/SessionChart';
import '../styles/dashboard.css';

/**
 * Página de análisis de una sesión. Lee el id de la sesión desde la ruta
 * (/dashboard/sesiones/:id/analisis) y delega el renderizado de las curvas
 * de velocidad y frenado al componente SessionChart.
 *
 * Implementa RF05 — Dashboard analítico.
 */
export default function SessionAnalysis() {
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
          <h1 style={{ textTransform: 'uppercase' }}>ANÁLISIS DE SESIÓN</h1>
          <p>Curvas de velocidad y frenado sincronizadas por distancia recorrida.</p>
        </div>

        {id && <SessionChart sessionId={id} />}
      </main>
    </div>
  );
}
