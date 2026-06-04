import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { CloudUpload, FileText } from 'lucide-react';
import '../styles/dashboard.css';

export default function UploadTelemetry() {
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');

  const [formData, setFormData] = useState({
    simulatorType: 'IRACING',
    trackId: '',
    categoryId: '',
    bestLapTime: ''
  });
  const [file, setFile] = useState<File | null>(null);
  
  const [uploadState, setUploadState] = useState({ status: 'idle', message: '' });

  // Si no hay token, fuera
  if (!token) {
    navigate('/login');
    return null;
  }

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selectedFile = e.target.files[0];
      if (!selectedFile.name.endsWith('.csv')) {
        setUploadState({ status: 'error', message: 'ERROR: SOLO SE ADMITEN ARCHIVOS .CSV' });
        setFile(null);
        return;
      }
      setFile(selectedFile);
      setUploadState({ status: 'idle', message: '' });
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    
    if (!file || !formData.trackId || !formData.categoryId || !formData.bestLapTime) {
      setUploadState({ status: 'error', message: 'ERROR: COMPLETE TODOS LOS CAMPOS' });
      return;
    }

    setUploadState({ status: 'processing', message: 'PROCESANDO...' });

    const payload = new FormData();
    payload.append('file', file);
    payload.append('simulatorType', formData.simulatorType);
    payload.append('trackId', formData.trackId);
    payload.append('categoryId', formData.categoryId);
    payload.append('bestLapTime', formData.bestLapTime);

    try {
      const response = await fetch('/api/v1/telemetry/upload', {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` },
        body: payload
      });

      if (response.status === 413) {
        throw new Error('EL ARCHIVO SUPERA EL TAMAÑO MÁXIMO (10 MB)');
      }

      if (!response.ok) {
        const errData = await response.json();
        throw new Error(errData.message || 'FALLÓ LA VALIDACIÓN DEL ARCHIVO');
      }

      setUploadState({ status: 'success', message: 'ÉXITO - ANALIZADO 100%' });
      
      // Limpieza tras éxito
      setTimeout(() => {
        setFile(null);
        setFormData({ ...formData, bestLapTime: '' });
        setUploadState({ status: 'idle', message: '' });
      }, 3000);

    } catch (err) {
      setUploadState({ status: 'error', message: `ERROR: ${(err as Error).message.toUpperCase()}` });
    }
  };

  return (
    <div className="app-layout">
      {/* SIDEBAR LATERAL (Como en tu diseño) */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>APEXMETRICS</h2>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>ENGINEERING V2.0</span>
        </div>
        <nav className="sidebar-nav">
          <button className="nav-item" onClick={() => navigate('/dashboard')}>DASHBOARD</button>
          <button className="nav-item" onClick={() => navigate('/leaderboard')}>CLASIFICACIÓN</button>
          <button className="nav-item active">SUBIR TELEMETRÍA</button>
          <button className="nav-item">AJUSTES</button>
        </nav>
      </aside>

      {/* ÁREA PRINCIPAL */}
      <main className="main-content">
        <div className="page-header">
          <h1>Ingesta de Datos</h1>
          <p>Configure los metadatos de la sesión y seleccione el archivo de registro generado por su simulador. El motor de análisis procesará automáticamente los canales de datos para su visualización en el dashboard.</p>
        </div>

        <form onSubmit={handleSubmit} className="ingesta-form">
          
          {/* LOS PARÁMETROS OBLIGATORIOS DEL BACKEND */}
          <div className="form-grid">
            <div className="input-group">
              <label>SIMULADOR</label>
              <select name="simulatorType" value={formData.simulatorType} onChange={handleChange} className="neon-select" required>
                <option value="IRACING">iRacing</option>
                <option value="ASSETTO_CORSA">Assetto Corsa</option>
              </select>
            </div>
            <div className="input-group">
              <label>CIRCUITO</label>
              <select name="trackId" value={formData.trackId} onChange={handleChange} className="neon-select" required>
                <option value="">Seleccione circuito...</option>
                <option value="1">Interlagos</option>
                <option value="2">Spa-Francorchamps</option>
                <option value="3">Le Mans</option>
              </select>
            </div>
            <div className="input-group">
              <label>CATEGORÍA</label>
              <select name="categoryId" value={formData.categoryId} onChange={handleChange} className="neon-select" required>
                <option value="">Seleccione categoría...</option>
                <option value="1">F1</option>
                <option value="2">GT3</option>
                <option value="3">WEC</option>
              </select>
            </div>
            <div className="input-group">
              <label>MEJOR TIEMPO (S)</label>
              <input type="number" step="0.001" name="bestLapTime" value={formData.bestLapTime} onChange={handleChange} className="neon-select" placeholder="Ej. 70.450" required />
            </div>
          </div>

          {/* LA ZONA DE DRAG & DROP DE TU IMAGEN */}
          <div className={`dropzone ${file ? 'has-file' : ''}`}>
            <input type="file" accept=".csv" onChange={handleFileChange} />
            <div className="dropzone-icon"><CloudUpload size={40} /></div>
            <h3 style={{ color: 'var(--text-main)', marginBottom: '0.5rem' }}>
              {file ? file.name : 'Subir Archivo de Telemetría'}
            </h3>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', letterSpacing: '1px' }}>SOPORTA .CSV</p>
            
            {!file && (
              <button type="button" className="neon-button" style={{ width: 'auto', display: 'inline-block', padding: '0.8rem 2rem', marginTop: '1rem' }}>
                EXAMINAR LOCAL
              </button>
            )}
          </div>

          <button type="submit" className="neon-button" disabled={uploadState.status === 'processing'} style={{ padding: '1rem', marginTop: 0 }}>
            {uploadState.status === 'processing' ? 'SINCRONIZANDO...' : 'INICIAR INGESTA'}
          </button>
        </form>

        {/* COLA DE INGESTIÓN (Solo se muestra cuando hay un archivo o estado) */}
        {uploadState.status !== 'idle' && (
          <div className="upload-status-area">
            <div className="status-header">
              <span>COLA DE INGESTIÓN ACTIVA</span>
              <span>1 Archivo</span>
            </div>
            <div className={`status-item ${uploadState.status}`}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
                <span><FileText size={18} /></span>
                <span className="filename">{file ? file.name : 'archivo.csv'}</span>
              </div>
              <span className="status-msg">{uploadState.message}</span>
            </div>
          </div>
        )}

      </main>
    </div>
  );
}