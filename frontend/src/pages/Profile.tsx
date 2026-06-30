import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { User, Mail, Globe, Shield, LayoutDashboard, Trophy, Upload, LogOut, Search, Bell, Settings } from 'lucide-react';
import '../styles/dashboard.css';

const COUNTRIES = [
  { group: '── SUDAMÉRICA ──', options: ['Argentina', 'Bolivia', 'Brasil', 'Chile', 'Colombia', 'Ecuador', 'Paraguay', 'Perú', 'Uruguay', 'Venezuela'] },
  { group: '── EUROPA ──', options: ['Alemania', 'Bélgica', 'Dinamarca', 'España', 'Finlandia', 'Francia', 'Grecia', 'Hungría', 'Italia', 'Países Bajos', 'Polonia', 'Portugal', 'Reino Unido', 'República Checa', 'Rumania', 'Suecia', 'Suiza'] },
  { group: '── OTROS ──', options: ['Otro'] },
];

interface ProfileData {
  username: string;
  email: string;
  country: string;
  role?: string;
}

export default function Profile() {
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');
  const username = localStorage.getItem('apex_username') || 'Piloto';

  // Estado inicial desde localStorage (fallback inmediato); luego se reemplaza
  // con los datos reales del backend cuando la petición responde.
  const [profile, setProfile] = useState<ProfileData>({
    username,
    email: localStorage.getItem('apex_email') || '—',
    country: localStorage.getItem('apex_country') || '—',
    role: localStorage.getItem('apex_role') || undefined,
  });

  const [saveStatus, setSaveStatus] = useState<'idle' | 'saving' | 'success' | 'error'>('idle');
  const [saveError, setSaveError] = useState<string | null>(null);

  // Redirige si no hay sesión activa
  useEffect(() => {
    if (!token) {
      navigate('/login');
    }
  }, [token, navigate]);

  // Carga el perfil real desde el backend (RF03). Si falla, mantiene el fallback de localStorage.
  useEffect(() => {
    if (!token) return;
    fetch('/api/v1/users/profile', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((r) => (r.ok ? r.json() : Promise.reject()))
      .then((data: ProfileData) => setProfile({ ...data, country: data.country ?? '' }))
      .catch(() => { /* se conserva el perfil cacheado de localStorage */ });
  }, [token]);

  if (!token) {
    return null;
  }

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const handleSave = async () => {
    if (!profile.country) {
      setSaveStatus('error');
      setSaveError('Por favor selecciona un país antes de guardar');
      return;
    }
    setSaveStatus('saving');
    setSaveError(null);
    try {
      const response = await fetch('/api/v1/users/profile', {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify({ country: profile.country }),
      });
      if (response.ok) {
        const updated = await response.json();
        setProfile(updated);
        localStorage.setItem('apex_country', updated.country ?? '');
        setSaveStatus('success');
        setTimeout(() => setSaveStatus('idle'), 2500);
      } else {
        setSaveStatus('error');
        setSaveError('Error al guardar los cambios');
      }
    } catch {
      setSaveStatus('error');
      setSaveError('Error de conexión');
    }
  };

  return (
    <div className="app-layout">
      {/* SIDEBAR */}
      <aside className="sidebar">
        <div className="sidebar-header">
          <h2>APEXMETRICS</h2>
          <span style={{ color: 'var(--text-muted)', fontSize: '0.7rem' }}>v2.0</span>
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
        <div className="top-navbar">
          <div className="search-bar">
            <span className="search-icon"><Search size={16} /></span>
            <input type="text" placeholder="Buscar..." />
          </div>
          <div className="top-icons">
            <span><Bell size={18} /></span>
            <span><Settings size={18} /></span>
            <span title={username}><User size={18} /></span>
          </div>
        </div>
        <div className="page-header">
          <h1>MI PERFIL</h1>
          <p>Información de tu cuenta de piloto. Puedes actualizar tu país de origen.</p>
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
              <p style={{ color: 'var(--neon-cyan)', fontSize: '0.7rem', letterSpacing: '2px', margin: '0.3rem 0 0' }}>{(profile.role || 'PILOTO').toUpperCase()}</p>
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
              <select
                className="neon-select"
                value={profile.country || ''}
                onChange={(e) => setProfile({ ...profile, country: e.target.value })}
              >
                <option value="" disabled>Selecciona tu país</option>
                {COUNTRIES.map(({ group, options }) => (
                  <optgroup key={group} label={group}>
                    {options.map((c) => (
                      <option key={c} value={c}>{c}</option>
                    ))}
                  </optgroup>
                ))}
              </select>
            </div>

            {saveStatus === 'success' && (
              <p style={{ color: '#00ff00', fontSize: '0.8rem', margin: 0 }}>
                Perfil sincronizado correctamente.
              </p>
            )}
            {saveStatus === 'error' && saveError && (
              <p style={{ color: 'var(--error-red)', fontSize: '0.8rem', margin: 0 }}>
                {saveError}
              </p>
            )}

            <button
              className="neon-button"
              onClick={handleSave}
              style={{ padding: '0.8rem', marginTop: 'auto' }}
              disabled={saveStatus === 'saving'}
            >
              {saveStatus === 'saving' ? 'GUARDANDO...' : saveStatus === 'success' ? 'SINCRONIZADO' : 'GUARDAR CAMBIOS'}
            </button>
          </div>
        </div>
      </main>
    </div>
  );
}
