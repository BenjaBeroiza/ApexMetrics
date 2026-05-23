import { useState } from 'react';
import { Link } from 'react-router-dom';
import '../styles/auth.css';

export default function Register() {
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    confirmPassword: '',
    country: 'Chile' 
  });
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError(null);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (formData.password.length < 16) {
      setError('Error: Mínimo 16 caracteres requeridos para la clave');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Error: Las claves de autorización no coinciden');
      return;
    }

    const { confirmPassword, ...payload } = formData;
    setLoading(true);

    try {
      const response = await fetch('/api/v1/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (response.ok) {
        navigate('/login');
      } else {
        const errData = await response.json();
        setError(errData.message || 'Error en el registro');
      }
    } catch (err) {
      setError('Error de conexión');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-header">
        <h1 className="neon-text">ApexMetrics</h1>
        <p className="sub-text">Plataforma de Análisis de Telemetría</p>
      </div>

      <div className="auth-card">
        <h2 className="card-title">CREAR CUENTA</h2>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="input-group">
            <label>NOMBRE DE USUARIO</label>
            <div className="input-wrapper">
              <input
                type="text"
                name="username"
                placeholder="Ej. JohnDoe"
                value={formData.username}
                onChange={handleChange}
                required 
              />
            </div>
          </div>

          <div className="input-group">
            <label>CORREO ELECTRÓNICO</label>
            <div className="input-wrapper">
              <input
                type="email"
                name="email"
                placeholder="correo@ejemplo.com"
                value={formData.email}
                onChange={handleChange}
                required 
              />
            </div>
          </div>

          <div className="input-group">
            <label>CONTRASEÑA</label>
            <div className={`input-wrapper ${error && error.includes('16') ? 'error' : ''}`}>
              <input 
                type="password" 
                name="password"
                placeholder="Mínimo 16 caracteres"
                value={formData.password}
                onChange={handleChange}
                required 
              />
            </div>
          </div>

          <div className="input-group">
            <label>CONFIRMAR CONTRASEÑA</label>
            <div className={`input-wrapper ${error && error.includes('coinciden') ? 'error' : ''}`}>
              <input 
                type="password" 
                name="confirmPassword"
                placeholder="Repite tu contraseña"
                value={formData.confirmPassword}
                onChange={handleChange}
                required 
              />
            </div>
            {error && <p className="error-message">{error}</p>}
          </div>

          <button type="submit" className="neon-button" disabled={loading}>
            {loading ? 'PROCESANDO...' : 'CREAR CUENTA'} <span>→</span>
          </button>

          <div style={{ textAlign: 'center', marginTop: '1rem' }}>
            <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
              ¿Ya tienes cuenta? Inicia sesión
            </Link>
          </div>
        </form>

        <div className="auth-footer">
          <div className="status-dots">
            <span className="dot"></span>
            <span className="dot active"></span>
            <span className="dot"></span>
          </div>
          <span className="secure-text">CONEXIÓN SEGURA</span>
        </div>
      </div>
    </div>
  );
}