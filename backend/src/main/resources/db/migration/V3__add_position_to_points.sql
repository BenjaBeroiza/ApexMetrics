-- ============================================================
-- ApexMetrics — Posición 2D en los puntos de telemetría
-- Prerrequisito de la API externa de trazado (OpenStreetMap / Leaflet).
-- Añade columnas opcionales: los puntos antiguos quedan con NULL sin romper nada.
--   pos_x      → iRacing: longitud | Assetto Corsa: coordenada local x
--   pos_y      → iRacing: latitud  | Assetto Corsa: coordenada local z
--   geographic → true = coordenadas GPS (OSM) | false = plano local (CRS.Simple)
-- ============================================================

ALTER TABLE telemetry_points ADD COLUMN IF NOT EXISTS pos_x      DOUBLE PRECISION;
ALTER TABLE telemetry_points ADD COLUMN IF NOT EXISTS pos_y      DOUBLE PRECISION;
ALTER TABLE telemetry_points ADD COLUMN IF NOT EXISTS geographic BOOLEAN;
