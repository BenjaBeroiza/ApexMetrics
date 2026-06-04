import { useState } from 'react';
import { Link } from 'react-router-dom'; // Importante para navegar
import '../styles/auth.css';

export default function Login() {
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
    if (e.target.name === 'password' && e.target.value.length >= 16) {
      setError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (formData.password.length < 16) {
      setError('Error: Mínimo 16 caracteres requeridos');
      return;
    }
    
    setLoading(true);
    try {
      const response = await fetch('/api/v1/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });

     if (response.ok) {
        const data = await response.json();
        console.log('Token recibido:', data.token);
        localStorage.setItem('apex_token', data.token);
        localStorage.setItem('apex_username', data.username);
        window.location.href = '/dashboard';
      } else {
        setError('Error: Credenciales inválidas');
      }
    } catch (err) {
      setError('Error de conexión con el servidor');
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
        <h2 className="card-title">AUTENTICACIÓN</h2>
        
        <form onSubmit={handleSubmit} className="auth-form">
          <div className="input-group">
            <label>ID OPERADOR (CORREO)</label>
            <div className="input-wrapper success">
              <input 
                type="email" 
                name="email"
                placeholder="dtaylor@apex.sim"
                value={formData.email}
                onChange={handleChange}
                required 
              />
              <span className="icon-success">✔</span>
            </div>
          </div>

          <div className="input-group">
            <label className={error ? 'text-error' : ''}>CLAVE DE AUTORIZACIÓN</label>
            <div className={`input-wrapper ${error ? 'error' : ''}`}>
              <input 
                type="password" 
                name="password"
                placeholder="........"
                value={formData.password}
                onChange={handleChange}
                required 
              />
              {error && <span className="icon-error">⚠</span>}
            </div>
            {error && <p className="error-message">{error}</p>}
          </div>

          <button type="submit" className="neon-button" disabled={loading}>
            {loading ? 'CONECTANDO...' : 'INICIAR SESIÓN'} <span>→</span>
          </button>
          
          <div style={{ textAlign: 'center', marginTop: '1rem' }}>
            <Link to="/register" style={{ color: 'var(--text-muted)', fontSize: '0.8rem', textDecoration: 'none' }}>
              ¿NUEVO OPERADOR? SOLICITAR ACCESO
            </Link>
          </div>
        </form>

        <div className="auth-footer">
          <div className="status-dots">
            <span className="dot active"></span>
            <span className="dot"></span>
            <span className="dot"></span>
          </div>
          <span className="secure-text">SECURE CONN_</span>
        </div>
      </div>
    </div>
  );
}