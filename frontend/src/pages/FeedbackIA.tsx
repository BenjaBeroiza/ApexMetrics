import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ArrowLeft, Brain, AlertCircle } from 'lucide-react';
import '../styles/dashboard.css';

interface FeedbackResponse {
  sessionId: number;
  feedback: string;
}

const SECTIONS = [
  'ANÁLISIS DE FRENADA',
  'ANÁLISIS DE ACELERACIÓN',
  'GESTIÓN DE VELOCIDAD',
  'PUNTOS DE MEJORA',
  'CONCLUSIÓN',
];

function parseSections(text: string): { title: string; body: string }[] {
  const result: { title: string; body: string }[] = [];
  let remaining = text;

  for (let i = 0; i < SECTIONS.length; i++) {
    const current = SECTIONS[i];
    const next = SECTIONS[i + 1];
    const start = remaining.indexOf(current);
    if (start === -1) continue;

    const afterTitle = remaining.indexOf('\n', start);
    const end = next ? remaining.indexOf(next, afterTitle) : remaining.length;
    const body = remaining
      .slice(afterTitle !== -1 ? afterTitle : start + current.length, end !== -1 ? end : undefined)
      .trim();

    result.push({ title: current, body });
  }

  return result.length > 0 ? result : [{ title: 'RETROALIMENTACIÓN', body: text.trim() }];
}

export default function FeedbackIA() {
  const { id } = useParams();
  const navigate = useNavigate();
  const token = localStorage.getItem('apex_token');

  const [feedback, setFeedback] = useState<FeedbackResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!token) { navigate('/login'); return; }

    fetch(`/api/v1/telemetry/sesiones/${id}/feedback-ia`, {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then((res) => {
        if (!res.ok) throw new Error(res.status === 403 ? 'Sin permiso' : 'Error del servidor');
        return res.json() as Promise<FeedbackResponse>;
      })
      .then((data) => setFeedback(data))
      .catch((err: Error) => setError(err.message))
      .finally(() => setLoading(false));
  }, [id, token, navigate]);

  const sections = feedback ? parseSections(feedback.feedback) : [];

  return (
    <div className="app-layout">
      <main className="main-content">
        <div className="page-header" style={{ marginBottom: '2rem' }}>
          <button
            onClick={() => navigate('/dashboard')}
            style={{
              background: 'none',
              border: 'none',
              color: 'var(--neon-cyan)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              fontSize: '0.85rem',
              fontWeight: 'bold',
              letterSpacing: '1px',
              marginBottom: '1.5rem',
              padding: 0,
            }}
          >
            <ArrowLeft size={16} /> VOLVER AL DASHBOARD
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
            <Brain size={28} color="var(--neon-cyan)" />
            <div>
              <h1 style={{ margin: 0, fontSize: '1.8rem', letterSpacing: '2px' }}>
                ANÁLISIS IA
              </h1>
              <p style={{ margin: 0, color: 'var(--text-muted)', fontSize: '0.85rem' }}>
                Retroalimentación generada por Gemini 2.5 Flash · Sesión #{id}
              </p>
            </div>
          </div>
        </div>

        {loading && (
          <div className="loading-state" style={{ padding: '4rem 0' }}>
            <p className="blink-text" style={{ color: 'var(--neon-cyan)' }}>
              ANALIZANDO TELEMETRÍA...
            </p>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.8rem', marginTop: '0.5rem' }}>
              Gemini está procesando tus datos. Esto puede tomar unos segundos.
            </p>
          </div>
        )}

        {error && (
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              background: 'rgba(255,107,107,0.08)',
              border: '1px solid var(--error-red)',
              borderRadius: '8px',
              padding: '1.25rem 1.5rem',
              color: 'var(--error-red)',
            }}
          >
            <AlertCircle size={20} />
            <span style={{ fontSize: '0.9rem' }}>{error}</span>
          </div>
        )}

        {!loading && !error && sections.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
            {sections.map(({ title, body }) => (
              <div
                key={title}
                style={{
                  background: 'rgba(255,255,255,0.03)',
                  border: '1px solid #333',
                  borderLeft: '3px solid var(--neon-cyan)',
                  borderRadius: '4px',
                  padding: '1.25rem 1.5rem',
                }}
              >
                <h3
                  style={{
                    margin: '0 0 0.75rem 0',
                    fontSize: '0.8rem',
                    letterSpacing: '2px',
                    color: 'var(--neon-cyan)',
                  }}
                >
                  {title}
                </h3>
                <p
                  style={{
                    margin: 0,
                    color: '#ccc',
                    fontSize: '0.9rem',
                    lineHeight: '1.65',
                    whiteSpace: 'pre-wrap',
                  }}
                >
                  {body}
                </p>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
