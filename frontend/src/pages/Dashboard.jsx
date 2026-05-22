import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import '../styles/leaderboard.css'; // Reutilizaremos los estilos de la tabla

export default function Dashboard() {
  const [sesiones, setSesiones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  // Obtenemos los datos del piloto desde localStorage
  const token = localStorage.getItem('apex_token');
  const username = localStorage.getItem('apex_username') || 'Piloto';

  useEffect(() => {
    // Si no hay token, lo mandamos al login (Protección de ruta básica)
    if (!token) {
      navigate('/login');
      return;
    }
    cargarHistorial();
  }, [navigate, token]);

  const cargarHistorial = async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await fetch('/api/v1/telemetry/sesiones', {
        headers: {
          'Authorization': `Bearer ${token}` // Enviamos el JWT
        }
      });

      if (response.status === 403 || response.status === 401) {
        // Si el token expiró [cite: 235, 237, 238]
        localStorage.removeItem('apex_token');
        navigate('/login');
        return;
      }

      if (!response.ok) throw new Error('Error al cargar el historial');
      
      const data = await response.json();
      setSesiones(data);
    } catch (err) {
      setError('Error de conexión_');
    } finally {
      setLoading(false);
    }
  };

  const handleEliminarSesion = async (id) => {
    if (!window.confirm('¿Seguro que deseas eliminar esta sesión de telemetría? Esta acción no se puede deshacer.')) {
      return;
    }

    try {
      const response = await fetch(`/api/v1/telemetry/sesiones/${id}`, {
        method: 'DELETE',
        headers: {
          'Authorization': `Bearer ${token}`
        }
      });

      if (response.ok) {
        // Si se borró en el backend[cite: 143, 157], actualizamos la vista filtrando la sesión eliminada
        setSesiones(sesiones.filter(sesion => sesion.sessionId !== id));
      } else {
        alert('Error al intentar eliminar la sesión');
      }
    } catch (err) {
      alert('Error de conexión_');
    }
  };

  const handleLogout = () => {
    localStorage.removeItem('apex_token');
    localStorage.removeItem('apex_username');
    navigate('/login');
  };

  const formatLapTime = (seconds) => {
    if (!seconds) return '--:--.---';
    const mins = Math.floor(seconds / 60);
    const secs = (seconds % 60).toFixed(3);
    return `${mins.toString().padStart(2, '0')}:${secs.padStart(6, '0')}`;
  };

  const formatDate = (isoString) => {
    if (!isoString) return '--';
    const date = new Date(isoString);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute:'2-digit' });
  };

  return (
    <div className="leaderboard-container">
      <div className="leaderboard-header" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', width: '100%', maxWidth: '1000px', marginBottom: '2rem' }}>
        <div style={{ textAlign: 'left' }}>
          <h1 className="neon-text" style={{ fontSize: '2rem' }}>PANEL DE OPERADOR</h1>
          <p className="sub-text">BIENVENIDO, {username.toUpperCase()}</p>
        </div>
        <button onClick={handleLogout} className="neon-button" style={{ width: 'auto', marginTop: 0, padding: '0.8rem 1.5rem', fontSize: '0.8rem' }}>
          DESCONECTAR
        </button>
      </div>

      <div className="leaderboard-card">
        <h2 className="card-title" style={{ textAlign: 'left', marginBottom: '1.5rem', color: 'var(--neon-cyan)' }}>HISTORIAL DE SESIONES</h2>
        
        <div className="table-wrapper">
          {loading ? (
            <div className="loading-state">
              <span className="blink-text">Sincronizando base de datos...</span>
            </div>
          ) : error ? (
            <div className="loading-state">
              <span className="blink-text" style={{ color: 'var(--error-red)' }}>{error}</span>
            </div>
          ) : sesiones.length === 0 ? (
            <div className="loading-state" style={{ flexDirection: 'column', gap: '1rem' }}>
              <span className="sub-text" style={{ color: 'var(--text-muted)' }}>No tienes sesiones registradas.</span>
              {/* Botón temporal que luego enlazaremos a la pantalla de carga (RF04) */}
              <button className="neon-button" style={{ width: 'auto', padding: '0.8rem 2rem' }}>
                SUBIR NUEVA TELEMETRÍA <span>+</span>
              </button>
            </div>
          ) : (
            <table className="neon-table">
              <thead>
                <tr>
                  <th>ID SESIÓN</th>
                  <th>CIRCUITO</th>
                  <th>CATEGORÍA</th>
                  <th>MEJOR TIEMPO</th>
                  <th>FECHA CARGA</th>
                  <th>ACCIONES</th>
                </tr>
              </thead>
              <tbody>
                {sesiones.map((sesion) => (
                  <tr key={sesion.sessionId}>
                    <td style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>#{sesion.sessionId}</td>
                    <td className="pilot-name">{sesion.trackName}</td>
                    <td><span className={`badge cat-${sesion.categoryName ? sesion.categoryName.toLowerCase() : 'desc'}`}>{sesion.categoryName}</span></td>
                    <td className="lap-time">{formatLapTime(sesion.bestLapTime)}</td>
                    <td className="date-cell">{formatDate(sesion.uploadedAt)}</td>
                    <td>
                      <button 
                        onClick={() => handleEliminarSesion(sesion.sessionId)}
                        style={{
                          background: 'none',
                          border: '1px solid var(--error-red)',
                          color: 'var(--error-red)',
                          padding: '0.4rem 0.8rem',
                          cursor: 'pointer',
                          fontFamily: 'monospace',
                          fontSize: '0.8rem',
                          transition: 'all 0.3s ease'
                        }}
                        onMouseOver={(e) => {
                          e.target.style.backgroundColor = 'var(--error-red-dim)';
                          e.target.style.boxShadow = '0 0 10px var(--error-red-dim)';
                        }}
                        onMouseOut={(e) => {
                          e.target.style.backgroundColor = 'transparent';
                          e.target.style.boxShadow = 'none';
                        }}
                      >
                        BORRAR
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}