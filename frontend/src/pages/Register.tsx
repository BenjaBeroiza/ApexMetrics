import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import '../styles/auth.css';

const COUNTRIES = [
  { group: '── SUDAMÉRICA ──', options: ['Argentina', 'Bolivia', 'Brasil', 'Chile', 'Colombia', 'Ecuador', 'Paraguay', 'Perú', 'Uruguay', 'Venezuela'] },
  { group: '── EUROPA ──', options: ['Alemania', 'Bélgica', 'Dinamarca', 'España', 'Finlandia', 'Francia', 'Grecia', 'Hungría', 'Italia', 'Países Bajos', 'Polonia', 'Portugal', 'Reino Unido', 'República Checa', 'Rumania', 'Suecia', 'Suiza'] },
  { group: '── OTROS ──', options: ['Otro'] },
];

export default function Register() {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    country: ''
  });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (formData.password.length < 8) {
      setError('Error: Mínimo 8 caracteres requeridos para la clave');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Error: Las claves de autorización no coinciden');
      return;
    }

    const { confirmPassword: _confirmPassword, ...payload } = formData;
    setLoading(true);

    try {
      const response = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        const data = await response.json();
        localStorage.setItem('apex_email', data.email ?? formData.email);
        localStorage.setItem('apex_country', data.country ?? formData.country);
        navigate('/login');
      } else {
        const errData = await response.json();
        setError(errData.message || 'Error en el registro');
      }
    } catch (_err) {
      setError('Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-header">
        <h1 className="neon-text">APEXMETRICS</h1>
        <p className="sub-text">ENGINEERING V2.0</p>
      </div>

      <div className="auth-card">
        <h2 className="card-title">REGISTRO DE OPERADOR</h2>

        <form onSubmit={handleSubmit} className="auth-form">
          <div className="input-group">
            <label>NOMBRE CLAVE / ALIAS</label>
            <div className="input-wrapper">
              <input
                type="text"
                name="username"
                placeholder="Ej. Piloto 1"
                value={formData.username}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="input-group">
            <label>ID OPERADOR (CORREO)</label>
            <div className="input-wrapper">
              <input
                type="email"
                name="email"
                placeholder="nuevo@apex.sim"
                value={formData.email}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="input-group">
            <label>PAÍS</label>
            <select
              name="country"
              className="neon-select"
              value={formData.country}
              onChange={handleChange}
              required
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

          <div className="input-group">
            <label>NUEVA CLAVE DE AUTORIZACIÓN</label>
            <div className={`input-wrapper ${error && error.includes('8') ? 'error' : ''}`}>
              <input
                type="password"
                name="password"
                placeholder="Mínimo 8 caracteres"
                value={formData.password}
                onChange={handleChange}
                required
              />
            </div>
          </div>

          <div className="input-group">
            <label>CONFIRMAR CLAVE</label>
            <div className={`input-wrapper ${error && error.includes('coinciden') ? 'error' : ''}`}>
              <input
                type="password"
                name="confirmPassword"
                placeholder="Repetir clave"
                value={formData.confirmPassword}
                onChange={handleChange}
                required
              />
            </div>
            {error && <p className="error-message">{error}</p>}
          </div>

          <button type="submit" className="neon-button" disabled={loading}>
            {loading ? 'PROCESANDO...' : 'SOLICITAR ACCESO'} <span>→</span>
          </button>

          <div style={{ textAlign: 'center', marginTop: '1rem' }}>
            <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
              ¿YA TIENES ACCESO? INICIAR SESIÓN
            </Link>
          </div>
        </form>

        <div className="auth-footer">
          <div className="status-dots">
            <span className="dot"></span>
            <span className="dot active"></span>
            <span className="dot"></span>
          </div>
          <span className="secure-text">ENCRYPTING_</span>
        </div>
      </div>
    </div>
  );
}
