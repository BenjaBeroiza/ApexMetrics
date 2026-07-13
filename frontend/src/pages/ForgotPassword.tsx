import { useState } from 'react';
import { Link } from 'react-router-dom';
import { forgotPassword } from '../services/auth.service';
import '../styles/auth.css';

/**
 * UC03 (RF11) — Solicitar restablecimiento de contraseña.
 *
 * El backend responde siempre 200 exista o no el correo, por lo que la vista
 * muestra un mensaje genérico de éxito. En desarrollo, el enlace de reseteo
 * se imprime en los logs del backend (no hay servidor de correo).
 */
export default function ForgotPassword() {
  const [email, setEmail] = useState('');
  const [sent, setSent] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      await forgotPassword(email);
      setSent(true);
    } catch {
      setError('Error de conexión con el servidor');
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
        <h2 className="card-title">RESTABLECER CONTRASEÑA</h2>

        {sent ? (
          <div style={{ textAlign: 'center', padding: '1rem 0' }}>
            <p style={{ color: '#00ff00', fontSize: '0.85rem', lineHeight: 1.6 }}>
              Si el correo está registrado, recibirás instrucciones para restablecer tu contraseña.
            </p>
            <div style={{ marginTop: '1.5rem' }}>
              <Link to="/login" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
                ← Volver a iniciar sesión
              </Link>
            </div>
          </div>
        ) : (
          <form onSubmit={handleSubmit} className="auth-form">
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', lineHeight: 1.5 }}>
              Ingresa el correo de tu cuenta y te enviaremos un enlace para crear una nueva contraseña.
            </p>

            <div className="input-group">
              <label>Correo</label>
              <div className="input-wrapper">
                <input
                  type="email"
                  name="email"
                  placeholder="correo@ejemplo.com"
                  value={email}
                  onChange={(e) => { setEmail(e.target.value); setError(null); }}
                  required
                />
              </div>
              {error && <p className="error-message">{error}</p>}
            </div>

            <button type="submit" className="neon-button" disabled={loading}>
              {loading ? 'ENVIANDO...' : 'ENVIAR ENLACE'} <span>→</span>
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
