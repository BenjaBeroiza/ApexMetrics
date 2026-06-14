import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Globe, Shield, LayoutDashboard, Trophy, Upload, LogOut } from 'lucide-react';
import '../styles/dashboard.css';

interface ProfileData {
  username: string;
  email: string;
  country: string;
}

export default function Profile() {
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');
  const username = localStorage.getItem('apex_username') || 'Piloto';

  const [profile] = useState<ProfileData>({
    username,
    email: '—',
    country: '—',
  });

  const [saveStatus, setSaveStatus] = useState<'idle' | 'success' | 'error'>('idle');

  // Redirige si no hay sesión activa
  useEffect(() => {
    if (!token) {
      navigate('/login');
    }
  }, [token, navigate]);

  if (!token) {
    return null;
  }

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const handleSave = async () => {
    // Placeholder para futura integración con PUT /api/v1/users/me
    setSaveStatus('success');
    setTimeout(() => setSaveStatus('idle'), 2500);
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
          <button className="nav-item" onClick={() => navigate('/dashboard')}>
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
          <button className="nav-item active">
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

      {/* CONTENIDO PRINCIPAL */}
      <main className="main-content">
        <div className="page-header">
          <h1>MI PERFIL</h1>
          <p>Información de tu cuenta de piloto. Próximamente: edición completa de datos.</p>
        </div>

        {/* TARJETA DE PERFIL */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 2fr', gap: '2rem', maxWidth: '900px' }}>

          {/* AVATAR */}
          <div style={{
            background: '#151515',
            border: '1px solid var(--border-color)',
            borderRadius: '8px',
            padding: '2rem',
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: '1rem',
          }}>
            <div style={{
              width: '80px',
              height: '80px',
              borderRadius: '50%',
              background: 'rgba(0,255,255,0.08)',
              border: '2px solid var(--neon-cyan)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}>
              <User size={36} color="var(--neon-cyan)" />
            </div>
            <div style={{ textAlign: 'center' }}>
              <p style={{ fontWeight: 'bold', letterSpacing: '1px', margin: 0 }}>{profile.username.toUpperCase()}</p>
              <p style={{ color: 'var(--neon-cyan)', fontSize: '0.7rem', letterSpacing: '2px', margin: '0.3rem 0 0' }}>PILOTO</p>
            </div>
            <div style={{
              width: '100%',
              padding: '0.5rem',
              background: 'rgba(0,255,255,0.04)',
              borderRadius: '4px',
              textAlign: 'center',
              fontSize: '0.75rem',
              color: 'var(--text-muted)',
              letterSpacing: '1px',
            }}>
              <Shield size={10} style={{ marginRight: '0.3rem' }} />
              CUENTA VERIFICADA
            </div>
          </div>

          {/* DATOS DE PERFIL */}
          <div style={{
            background: '#151515',
            border: '1px solid var(--border-color)',
            borderRadius: '8px',
            padding: '2rem',
            display: 'flex',
            flexDirection: 'column',
            gap: '1.5rem',
          }}>
            <h3 style={{ margin: 0, fontSize: '0.85rem', letterSpacing: '2px', color: 'var(--text-muted)', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.8rem' }}>
              DATOS DEL OPERADOR
            </h3>

            {/* Campo: Nombre de usuario */}
            <div className="input-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <User size={12} />
                NOMBRE CLAVE / ALIAS
              </label>
              <div className="input-wrapper">
                <input
                  type="text"
                  value={profile.username}
                  readOnly
                  style={{ opacity: 0.7, cursor: 'not-allowed' }}
                />
              </div>
            </div>

            {/* Campo: Correo */}
            <div className="input-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Mail size={12} />
                ID OPERADOR (CORREO)
              </label>
              <div className="input-wrapper">
                <input
                  type="email"
                  value={profile.email}
                  readOnly
                  style={{ opacity: 0.7, cursor: 'not-allowed' }}
                />
              </div>
            </div>

            {/* Campo: País */}
            <div className="input-group">
              <label style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}>
                <Globe size={12} />
                PAÍS DE ORIGEN
              </label>
              <div className="input-wrapper">
                <input
                  type="text"
                  value={profile.country}
                  readOnly
                  style={{ opacity: 0.7, cursor: 'not-allowed' }}
                />
              </div>
            </div>

            {/* Estado del guardado */}
            {saveStatus === 'success' && (
              <p style={{ color: '#00ff00', fontSize: '0.8rem', margin: 0 }}>
                Perfil sincronizado correctamente.
              </p>
            )}

            <button
              className="neon-button"
              onClick={handleSave}
              style={{ padding: '0.8rem', marginTop: 'auto' }}
              disabled={saveStatus !== 'idle'}
            >
              {saveStatus === 'success' ? 'SINCRONIZADO' : 'GUARDAR CAMBIOS'}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
