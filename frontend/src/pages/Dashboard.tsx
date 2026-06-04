import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Bell, Settings, User } from 'lucide-react';
import '../styles/dashboard.css';

interface Sesion {
  sessionId: number;
  trackName: string;
  categoryName: string;
  bestLapTime: number;
  uploadedAt: string;
}

export default function Dashboard() {
  const [sesiones, setSesiones] = useState<Sesion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error] = useState<string | null>(null);
  const navigate = useNavigate();

  const token = localStorage.getItem('apex_token');
  const username = localStorage.getItem('apex_username') || 'Piloto';

  useEffect(() => {
    if (!token) {
      navigate('/login');
      return;
    }
    cargarHistorial();
  }, [navigate, token]);

  const cargarHistorial = async () => {
    setLoading(true);
    // Simulación mientras el backend se levanta (puedes reemplazar esto por el fetch real después)
    setTimeout(() => {
      setSesiones([
        { sessionId: 101, trackName: 'Spa-Francorchamps', categoryName: 'GT3', bestLapTime: 135.890, uploadedAt: '2026-05-22T14:30:00' },
        { sessionId: 102, trackName: 'Le Mans', categoryName: 'WEC', bestLapTime: 205.100, uploadedAt: '2026-05-20T09:15:00' }
      ]);
      setLoading(false);
    }, 800);
  };

  const handleEliminarSesion = async (id: number) => {
    if (!window.confirm('¿Seguro que deseas eliminar esta sesión?')) return;
    try {
      const response = await fetch(`/api/v1/telemetry/sesiones/${id}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
      });
      if (response.ok) {
        setSesiones(sesiones.filter(s => s.sessionId !== id));
      } else {
        alert('Error al eliminar la sesión');
      }
    } catch (err) {
      alert('Error de conexión');
    }
  };

  const formatLapTime = (seconds: number) => {
    if (!seconds) return '--:--.---';
    const mins = Math.floor(seconds / 60);
    const secs = (seconds % 60).toFixed(3);
    return `${mins}:${secs.padStart(6, '0')}`;
  };

  return (
    <div className="app-layout">
      {/* SIDEBAR */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>APEXMETRICS</h2>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>ENGINEERING V2.0</span>
        </div>
        <nav className="sidebar-nav">
          <button className="nav-item active">DASHBOARD</button>
          <button className="nav-item" onClick={() => navigate('/leaderboard')}>CLASIFICACIÓN</button>
          <button className="nav-item" onClick={() => navigate('/upload')}>SUBIR TELEMETRÍA</button>
          <button className="nav-item">AJUSTES</button>
        </nav>
        <div style={{ marginTop: 'auto', padding: '0 1rem' }}>
          <button className="neon-button" style={{ fontSize: '0.7rem', padding: '0.8rem', background: 'transparent', color: 'var(--error-red)', borderColor: 'var(--error-red)' }} onClick={() => { localStorage.clear(); navigate('/login'); }}>
            CERRAR SESIÓN
          </button>
        </div>
      </aside>

    
      <main className="main-content">
        
       
        <div className="top-navbar">
          <div className="search-bar">
            <span className="search-icon"><Search size={16} /></span>
            <input type="text" placeholder="Buscar sesión..." />
          </div>
          <div className="top-icons">
            <span><Bell size={18} /></span>
            <span><Settings size={18} /></span>
            <span title={username}><User size={18} /></span>
          </div>
        </div>

        <div className="page-header" style={{ marginBottom: '2rem' }}>
          <h1 style={{ textTransform: 'uppercase' }}>MIS SESIONES</h1>
          <p>Historial de telemetría personal. Próximamente: Análisis gráfico detallado.</p>
        </div>

        {loading ? (
          <div className="loading-state"><span className="blink-text">Cargando sesiones...</span></div>
        ) : error ? (
          <div className="loading-state"><span style={{ color: 'var(--error-red)' }}>{error}</span></div>
        ) : sesiones.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '4rem', border: '1px dashed #333' }}>
            <p style={{ color: 'var(--text-muted)', marginBottom: '1rem' }}>No hay telemetría registrada.</p>
            <button onClick={() => navigate('/upload')} className="neon-button" style={{ width: 'auto', display: 'inline-block', padding: '0.5rem 1.5rem' }}>NUEVA INGESTA</button>
          </div>
        ) : (
          <table className="minimal-table">
            <thead>
              <tr>
                <th>CIRCUITO</th>
                <th>CATEGORÍA</th>
                <th>MEJOR TIEMPO</th>
                <th>FECHA</th>
                <th style={{ textAlign: 'right' }}>ACCIONES</th>
              </tr>
            </thead>
            <tbody>
              {sesiones.map((sesion) => (
                <tr key={sesion.sessionId}>
                  <td style={{ fontWeight: 'bold' }}>{sesion.trackName.toUpperCase()}</td>
                  <td style={{ color: 'var(--text-muted)' }}>{sesion.categoryName}</td>
                  <td style={{ color: 'var(--neon-cyan)' }}>{formatLapTime(sesion.bestLapTime)}</td>
                  <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{new Date(sesion.uploadedAt).toLocaleDateString()}</td>
                  <td style={{ textAlign: 'right' }}>
                    <button 
                      onClick={() => handleEliminarSesion(sesion.sessionId)}
                      style={{ background: 'none', border: 'none', color: 'var(--error-red)', cursor: 'pointer', fontSize: '0.8rem', fontWeight: 'bold' }}
                    >
                      ELIMINAR
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </main>
    </div>
  );
}