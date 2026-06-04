import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Search, Bell, Settings, User } from 'lucide-react';
import '../styles/dashboard.css';

export default function Leaderboard() {
  const navigate = useNavigate();
  const [leaderboardData, setLeaderboardData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [totalPages, setTotalPages] = useState(0);
  
  
  const [filters, setFilters] = useState({ categoryId: '', trackId: '', page: 0 });

  useEffect(() => {
    const fetchLeaderboard = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const params = new URLSearchParams();
        if (filters.categoryId) params.append('categoryId', filters.categoryId);
        if (filters.trackId) params.append('trackId', filters.trackId);
        params.append('page', filters.page);
        params.append('size', 10); // 10 resultados por página
        
        const response = await fetch(`/api/v1/leaderboard?${params.toString()}`);
        if (!response.ok) throw new Error('Fallo al obtener la clasificación');
        
        const data = await response.json();
        setLeaderboardData(data.content || []);
        setTotalPages(data.totalPages || 1);
      } catch (err) {
        setError("Error de conexión con el servidor");
        setLeaderboardData([]);
      } finally {
        setLoading(false);
      }
    };

    fetchLeaderboard();
  }, [filters]);

  const handleFilterChange = (e) => {
    setFilters({ ...filters, [e.target.name]: e.target.value, page: 0 }); // Volver a pág 0 al filtrar
  };

  const handlePageChange = (newPage) => {
    setFilters({ ...filters, page: newPage });
  };

  const formatLapTime = (seconds) => {
    if (!seconds) return '--:--.---';
    const mins = Math.floor(seconds / 60);
    const secs = (seconds % 60).toFixed(3);
    return `${mins}:${secs.padStart(6, '0')}`;
  };

  return (
    <div className="app-layout">
    
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>APEXMETRICS</h2>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>ENGINEERING V2.0</span>
        </div>
        <nav className="sidebar-nav">
          <button className="nav-item" onClick={() => navigate('/dashboard')}>DASHBOARD</button>
          <button className="nav-item active">CLASIFICACIÓN</button>
          <button className="nav-item" onClick={() => navigate('/upload')}>SUBIR TELEMETRÍA</button>
          <button className="nav-item">AJUSTES</button>
        </nav>
        <div style={{ marginTop: 'auto', padding: '0 1rem' }}>
          <button className="neon-button" style={{ fontSize: '0.7rem', padding: '0.8rem' }} onClick={() => setFilters({...filters})}>
            ↻ SINCRONIZAR DATOS
          </button>
        </div>
      </aside>


      <main className="main-content">
        
        {/* TOP NAVBAR */}
        <div className="top-navbar">
          <div className="search-bar">
            <span className="search-icon"><Search size={16} /></span>
            <input type="text" placeholder="Buscar piloto..." />
          </div>
          <div className="top-icons">
            <span><Bell size={18} /></span>
            <span><Settings size={18} /></span>
            <span><User size={18} /></span>
          </div>
        </div>

        <div className="page-header" style={{ marginBottom: '1.5rem' }}>
          <h1>CLASIFICACIÓN GLOBAL</h1>
          <p style={{ color: 'var(--neon-cyan)', fontSize: '0.8rem', letterSpacing: '1px' }}>
            <span style={{ display: 'inline-block', width: '8px', height: '8px', background: 'var(--neon-cyan)', borderRadius: '50%', marginRight: '8px' }}></span>
            LIVE TELEMETRY FEED
          </p>
        </div>

        {/* FILTROS SUTILES */}
        <div style={{ display: 'flex', gap: '1rem', marginBottom: '2rem' }}>
          <select name="categoryId" value={filters.categoryId} onChange={handleFilterChange} className="neon-select" style={{ padding: '0.5rem', fontSize: '0.8rem' }}>
            <option value="">Todas las Categorías</option>
            <option value="1">F1</option>
            <option value="2">GT3</option>
            <option value="3">WEC</option>
          </select>
          <select name="trackId" value={filters.trackId} onChange={handleFilterChange} className="neon-select" style={{ padding: '0.5rem', fontSize: '0.8rem' }}>
            <option value="">Todos los Circuitos</option>
            <option value="1">Interlagos</option>
            <option value="2">Spa-Francorchamps</option>
            <option value="3">Le Mans</option>
          </select>
        </div>

        {/* TABLA */}
        {loading ? (
          <div className="loading-state"><span className="blink-text">Sincronizando...</span></div>
        ) : error ? (
          <div className="loading-state"><span style={{ color: 'var(--error-red)' }}>{error}</span></div>
        ) : (
          <>
            <table className="minimal-table">
              <thead>
                <tr>
                  <th style={{ width: '10%' }}>POS</th>
                  <th style={{ width: '40%' }}>PILOTO</th>
                  <th style={{ width: '30%' }}>TIEMPO DE VUELTA</th>
                  <th style={{ width: '20%', textAlign: 'right' }}>CATEGORÍA</th>
                </tr>
              </thead>
              <tbody>
                {leaderboardData.map((entry) => (
                  <tr key={`${entry.username}-${entry.uploadedAt}`}>
                    <td style={{ color: 'var(--neon-cyan)' }}>{entry.rank}</td>
                    <td style={{ fontWeight: 'bold' }}>{entry.username.toUpperCase()}</td>
                    <td style={{ color: 'var(--neon-cyan)' }}>{formatLapTime(entry.bestLapTime)}</td>
                    <td style={{ textAlign: 'right', color: 'var(--text-muted)' }}>{entry.categoryName}</td>
                  </tr>
                ))}
              </tbody>
            </table>

            {/* CONTROLES DE PAGINACIÓN */}
            <div className="pagination">
              <button 
                className="page-btn" 
                disabled={filters.page === 0} 
                onClick={() => handlePageChange(filters.page - 1)}
              >&lt;</button>
              
              <button className="page-btn active">{filters.page + 1}</button>
              
              <button 
                className="page-btn" 
                disabled={filters.page >= totalPages - 1} 
                onClick={() => handlePageChange(filters.page + 1)}
              >&gt;</button>
            </div>
          </>
        )}
      </main>
    </div>
  );
}