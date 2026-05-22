import { useState, useEffect } from 'react';
import '../styles/leaderboard.css';

export default function Leaderboard() {
  const [leaderboardData, setLeaderboardData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  // Filtros
  const [filters, setFilters] = useState({ categoryId: '', trackId: '' });

  useEffect(() => {
    const fetchLeaderboard = async () => {
      setLoading(true);
      setError(null);
      
      try {
        // Construimos los query parameters solo si tienen un valor seleccionado
        const params = new URLSearchParams();
        if (filters.categoryId) params.append('categoryId', filters.categoryId);
        if (filters.trackId) params.append('trackId', filters.trackId);
        
        const response = await fetch(`/api/v1/leaderboard?${params.toString()}`);
        
        if (!response.ok) {
          throw new Error('Fallo al obtener la telemetría');
        }
        
        const data = await response.json();
        
        // El backend de Spring devuelve un Page<T>, los datos vienen en data.content
        setLeaderboardData(data.content || []);
      } catch (err) {
        console.error("Error al cargar el leaderboard:", err);
        setError("Error de conexión_");
        setLeaderboardData([]); // Limpiamos la tabla en caso de error
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboard();
  }, [filters]);

  const handleFilterChange = (e) => {
    setFilters({ ...filters, [e.target.name]: e.target.value });
  };

  // Función para formatear segundos (ej: 70.450 -> 01:10.450)
  const formatLapTime = (seconds) => {
    if (!seconds) return '--:--.---';
    const mins = Math.floor(seconds / 60);
    const secs = (seconds % 60).toFixed(3);
    return `${mins.toString().padStart(2, '0')}:${secs.padStart(6, '0')}`;
  };


  const formatDate = (isoString) => {
    if (!isoString) return '--';
    const date = new Date(isoString);
    return date.toLocaleDateString('es-ES', { day: '2-digit', month: 'short', year: 'numeric' });
  };

  return (
    <div className="leaderboard-container">
      <div className="leaderboard-header">
        <h1 className="neon-text">GLOBAL LEADERBOARD</h1>
        <p className="sub-text">LIVE TELEMETRY RANKINGS</p>
      </div>

      <div className="leaderboard-card">
        
        {/* SECCIÓN DE FILTROS */}
        <div className="filters-section">
          <div className="filter-group">
            <label>CATEGORÍA</label>
            <select name="categoryId" value={filters.categoryId} onChange={handleFilterChange} className="neon-select">
              <option value="">Todas las Categorías</option>
              {/* Estos values deben coincidir con los IDs reales de tu tabla 'categories' */}
              <option value="1">F1</option>
              <option value="2">GT3</option>
              <option value="3">WEC</option>
            </select>
          </div>
          
          <div className="filter-group">
            <label>CIRCUITO</label>
            <select name="trackId" value={filters.trackId} onChange={handleFilterChange} className="neon-select">
              <option value="">Todos los Circuitos</option>
              {/* Estos values deben coincidir con los IDs reales de tu tabla 'tracks' */}
              <option value="1">Interlagos</option>
              <option value="2">Spa-Francorchamps</option>
              <option value="3">Le Mans</option>
            </select>
          </div>
        </div>

        {/* TABLA DE RESULTADOS */}
        <div className="table-wrapper">
          {loading ? (
            <div className="loading-state">
              <span className="blink-text">RECIBIENDO DATOS...</span>
            </div>
          ) : error ? (
            <div className="loading-state">
              <span className="blink-text" style={{ color: 'var(--error-red)' }}>{error}</span>
            </div>
          ) : leaderboardData.length === 0 ? (
            <div className="loading-state">
              <span className="sub-text" style={{ color: 'var(--text-muted)' }}>NO HAY REGISTROS DISPONIBLES</span>
            </div>
          ) : (
            <table className="neon-table">
              <thead>
                <tr>
                  <th>POS</th>
                  <th>PILOTO</th>
                  <th>PAÍS</th>
                  <th>CIRCUITO</th>
                  <th>CAT</th>
                  <th>MEJOR TIEMPO</th>
                  <th>FECHA</th>
                </tr>
              </thead>
              <tbody>
                {leaderboardData.map((entry) => (
                  <tr key={`${entry.username}-${entry.uploadedAt}`} className={entry.rank <= 3 ? `top-${entry.rank}` : ''}>
                    <td className="rank-cell">
                      {entry.rank === 1 ? '🥇' : entry.rank === 2 ? '🥈' : entry.rank === 3 ? '🥉' : entry.rank}
                    </td>
                    <td className="pilot-name">{entry.username}</td>
                    <td>{entry.country || '--'}</td>
                    <td>{entry.trackName}</td>
                    <td><span className={`badge cat-${entry.categoryName ? entry.categoryName.toLowerCase() : 'desconocida'}`}>{entry.categoryName}</span></td>
                    <td className="lap-time">{formatLapTime(entry.bestLapTime)}</td>
                    <td className="date-cell">{formatDate(entry.uploadedAt)}</td>
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