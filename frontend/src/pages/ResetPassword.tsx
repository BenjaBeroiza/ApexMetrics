import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { resetPassword } from '../services/auth.service';
import { ApiError } from '../services/http';
import '../styles/auth.css';

/**
 * UC03 (RF11) — Canjear el token de reseteo por una nueva contraseña.
 *
 * El token llega como query param (?token=...) desde el enlace generado por
 * el backend. Es de un solo uso y expira a los 30 minutos: en ese caso el
 * backend responde 400 y se muestra un error controlado.
 */
export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const token = searchParams.get('token') ?? '';

  const [formData, setFormData] = useState({ password: '', confirmPassword: '' });
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    setError(null);
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (formData.password.length < 8) {
      setError('Error: Mínimo 8 caracteres requeridos');
      return;
    }
    if (formData.password !== formData.confirmPassword) {
      setError('Error: Las contraseñas no coinciden');
      return;
    }

    setLoading(true);
    try {
      await resetPassword(token, formData.password);
      setSuccess(true);
      setTimeout(() => navigate('/login'), 2500);
    } catch (err) {
      if (err instanceof ApiError) {
        setError('El enlace de reseteo no es válido o ya expiró. Solicita uno nuevo.');
      } else {
        setError('Error de conexión con el servidor');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-container">
      <div className="auth-header">
        <h1 className="neon-text">ApexMetrics</h1>
        <p className="sub-text">Recuperación de acceso</p>
      </div>

      <div className="auth-card">
        <h2 className="card-title">NUEVA CONTRASEÑA</h2>

        {!token ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <p className="error-message">El enlace no contiene un token de reseteo válido.</p>
            <div style={{ marginTop: '1.5rem' }}>
              <Link to="/forgot-password" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
                Solicitar un enlace nuevo
              </Link>
            </div>
          </div>
        ) : success ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <p style={{ color: '#00ff00', fontSize: '0.85rem', lineHeight: 1.6 }}>
              Tu contraseña ha sido restablecida correctamente. Redirigiendo al login…
            </p>
            <div style={{ marginTop: '1.5rem' }}>
              <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
                Ir a iniciar sesión ahora
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            <div className="input-group">
              <label>Nueva contraseña</label>
              <div className={`input-wrapper ${error ? 'error' : ''}`}>
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
              <label>Confirmar contraseña</label>
              <div className={`input-wrapper ${error ? 'error' : ''}`}>
                <input
                  type="password"
                  name="confirmPassword"
                  placeholder="Repetir contraseña"
                  value={formData.confirmPassword}
                  onChange={handleChange}
                  required
                />
              </div>
              {error && <p className="error-message">{error}</p>}
            </div>

            <button type="submit" className="neon-button" disabled={loading}>
              {loading ? 'GUARDANDO...' : 'RESTABLECER CONTRASEÑA'} <span>→</span>
            </button>

            <div style={{ textAlign: 'center', marginTop: '1rem' }}>
              <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
                ← Volver a iniciar sesión
              </Link>
            </div>
          </form>
        )}

        <div className="auth-footer">
          <div className="status-dots">
            <span className="dot active"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </div>
          <span className="secure-text">CONEXIÓN SEGURA</span>
        </div>
      </div>
    </div>
  );
}
