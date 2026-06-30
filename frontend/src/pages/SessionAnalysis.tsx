import { useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { LayoutDashboard, Trophy, Upload, User, LogOut } from 'lucide-react';
import SessionChart from '../components/SessionChart';
import '../styles/dashboard.css';

export default function SessionAnalysis() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');
  const username = localStorage.getItem('apex_username') || 'Piloto';

  useEffect(() => {
    if (!token) navigate('/login');
  }, [token, navigate]);

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>APEXMETRICS</h2>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>v2.0</span>
        </div>
        <nav className="sidebar-nav">
          <button className="nav-item active" onClick={() => navigate('/dashboard')}>
            <LayoutDashboard size={14} style={{ marginRight: '0.5rem' }} />
            DASHBOARD
          </button>
          <button className="nav-item" onClick={() => navigate('/leaderboard')}>
            <Trophy size={14} style={{ marginRight: '0.5rem' }} />
            CLASIFICACIÓN
          </button>
          <button className="nav-item" onClick={() => navigate('/upload')}>
            <Upload size={14} style={{ marginRight: '0.5rem' }} />
            SUBIR TELEMETRÍA
          </button>
          <button className="nav-item" onClick={() => navigate('/profile')}>
            <User size={14} style={{ marginRight: '0.5rem' }} />
            MI PERFIL
          </button>
        </nav>
        <div style={{ marginTop: 'auto', padding: '0 1rem' }}>
          <button
            className="neon-button"
            style={{ fontSize: '0.7rem', padding: '0.8rem', background: 'transparent', color: 'var(--error-red)', borderColor: 'var(--error-red)' }}
            onClick={handleLogout}
          >
            <LogOut size={12} style={{ marginRight: '0.4rem' }} />
            CERRAR SESIÓN
          </button>
        </div>
      </aside>

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
            ← VOLVER AL DASHBOARD
          </button>
          <h1 style={{ textTransform: 'uppercase' }}>ANÁLISIS DE SESIÓN</h1>
          <p>Curvas de velocidad y frenado sincronizadas por distancia recorrida.</p>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem' }}>
            Usuario: {username.toUpperCase()}
          </p>
        </div>

        {id && <SessionChart sessionId={id} />}
      </main>
    </div>
  );
}
