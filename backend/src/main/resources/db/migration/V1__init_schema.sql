-- ============================================================
-- ApexMetrics — Migración inicial del esquema
-- RF01/RF02: users | RF04: tracks, categories, sessions, points
-- RF07: leaderboard queries sobre telemetry_sessions
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    email         VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    country       VARCHAR(100),
    role          VARCHAR(20)  NOT NULL DEFAULT 'PILOT',
    privacy_flag  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tracks (
    id             BIGSERIAL PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    country        VARCHAR(100),
    length_meters  INTEGER
);

CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL,
    regulation  VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS telemetry_sessions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT       NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    track_id      BIGINT       NOT NULL REFERENCES tracks(id),
    category_id   BIGINT       NOT NULL REFERENCES categories(id),
    uploaded_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    best_lap_time DOUBLE PRECISION
);

CREATE TABLE IF NOT EXISTS telemetry_points (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT          NOT NULL REFERENCES telemetry_sessions(id) ON DELETE CASCADE,
    distance    DOUBLE PRECISION,
    speed       DOUBLE PRECISION,
    brake       DOUBLE PRECISION,
    throttle    DOUBLE PRECISION
);

-- Índices para consultas del leaderboard (RF07)
CREATE INDEX IF NOT EXISTS idx_sessions_track    ON telemetry_sessions(track_id);
CREATE INDEX IF NOT EXISTS idx_sessions_category ON telemetry_sessions(category_id);
CREATE INDEX IF NOT EXISTS idx_sessions_user     ON telemetry_sessions(user_id);
CREATE INDEX IF NOT EXISTS idx_points_session    ON telemetry_points(session_id);

-- ============================================================
-- Datos de seed — circuitos y categorías referenciales
-- ============================================================
INSERT INTO tracks (name, country, length_meters) VALUES
    ('Autodromo Nazionale di Monza',  'Italia',    5793),
    ('Circuit de Spa-Francorchamps',  'Bélgica',   7004),
    ('Nürburgring Nordschleife',      'Alemania',  20832),
    ('Circuit de Catalunya',          'España',    4655),
    ('Autodromo Enzo e Dino Ferrari', 'Italia',    4909)
ON CONFLICT DO NOTHING;

INSERT INTO categories (name, regulation) VALUES
    ('GT3',        'Regulación Técnica FIA GT3 2024'),
    ('GT4',        'Regulación Técnica FIA GT4 2024'),
    ('Formula 2',  'Regulación Técnica FIA F2 2024'),
    ('Hypercar',   'Regulación Técnica FIA Hypercar WEC 2024'),
    ('TCR',        'Regulación Técnica TCR International 2024')
ON CONFLICT DO NOTHING;
