-- ============================================================
-- ApexMetrics — UC03: Restablecer contraseña
-- Tabla de tokens de reseteo (un solo uso, expiración 30 min).
-- El backend registra el enlace en logs en desarrollo (sin servidor de correo).
-- ============================================================

CREATE TABLE IF NOT EXISTS password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(100) NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Búsqueda directa del token al canjearlo (reset-password).
CREATE INDEX IF NOT EXISTS idx_reset_tokens_token ON password_reset_tokens(token);
