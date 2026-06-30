-- ============================================================
-- ApexMetrics — Número de vuelta por punto de telemetría
-- Permite segmentar los puntos de telemetría por vuelta,
-- habilitando la comparación de vueltas en el mapa (Bloque C).
-- Valor por defecto 1: los puntos existentes sin asignación de
-- vuelta quedan en vuelta 1 sin romper la funcionalidad actual.
-- ============================================================

ALTER TABLE telemetry_points ADD COLUMN IF NOT EXISTS lap_number INTEGER DEFAULT 1;
