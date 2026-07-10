# BUG — Ingesta de CSV y alineación de trazados sobre pistas reales (OSM)

> Documento de contexto para la Fase 1 del Hito 6 (B1). Autor: Benjamín.
> Destinatarios: Luis (Fase 3, revisión de la vista de mapa) y la sección
> "manejo de errores de la API externa" de la presentación final.

## 1. Síntoma reportado

El recorrido dibujado a partir del CSV **no se superponía correctamente sobre la
pista real** en el mapa (TileLayer de OpenStreetMap). Se observaban dos variantes:

1. La *polyline* de velocidad (capa Canvas) **se desplazaba respecto a los
   marcadores de sector** al panear o hacer zoom: los círculos de sector (nativos
   de Leaflet) quedaban en un sitio y el gradiente de velocidad en otro.
2. Para archivos de **Assetto Corsa** con posición local, el trazado se dibujaba
   sobre tiles de OSM en una ubicación **sin sentido geográfico** (coordenadas de
   mundo local interpretadas como lat/lon).

## 2. Con qué CSV se reproduce

- `docs/samples/demo_iracing_monza.csv` y `docs/samples/demo_iracing_spa.csv`
  (iRacing, columnas `Lat,Lon`) → variante (1), desalineación Canvas vs. marcadores.
- `docs/samples/demo_assetto_corsa_pos.csv` (Assetto Corsa, columnas `posX,posZ`)
  → variante (2), coordenadas locales sobre OSM.
- Fixture de alta fidelidad añadido en esta fase:
  `backend/src/test/resources/telemetry/iracing_monza_real.csv`
  (equivalente demo: `docs/samples/demo_iracing_monza_real.csv`).

## 3. Componentes involucrados

| Capa | Archivo | Rol |
|------|---------|-----|
| Backend parser | `telemetry/parser/IracingCsvParser.java` | mapea `Lon→posX`, `Lat→posY`, `geographic=true` |
| Backend parser | `telemetry/parser/AssettoCorsaCsvParser.java` | mapea `posX→posX`, `posZ→posY`, `geographic=false` |
| Backend servicio | `telemetry/service/TelemetryService#obtenerTrazado` | filtra puntos con posición y expone `geographic` |
| Frontend | `components/TrackMap.tsx` | elige CRS, dibuja gradiente (Canvas) + marcadores + tiles |

## 4. Causas raíz y estado

### Causa A — Desalineación Canvas ↔ Leaflet (CORREGIDA)
`GradientLayer` dibujaba usando coordenadas de *container point* mientras que los
marcadores de Leaflet viven en *layer point*. Al panear/zoomar (que aplica un
`transform` CSS sobre `overlayPane`), ambos sistemas divergían.
**Fix aplicado** (rama `fix/csv-circuits-map`, ya mergeada): reposicionar el canvas
con `map.containerPointToLayerPoint([0,0])` + `L.DomUtil.setPosition`, y proyectar
cada punto con `map.latLngToLayerPoint([lat,lon])`. Ver comentario "FIX 2" en
`TrackMap.tsx`.

### Causa B — CRS incorrecto para posición local (CORREGIDA)
Las coordenadas locales de Assetto Corsa (`posX/posZ`, metros) se renderizaban con
`CRS.EPSG3857` + tiles OSM. **Fix aplicado**: en `TrackMap.tsx` se selecciona
`CRS.Simple` **sin TileLayer** cuando `geographic === false`, y `CRS.EPSG3857` con
tiles OSM cuando `geographic === true`. El backend distingue ambos casos con el flag
`geographic` en cada `TelemetryPoint`.

### Causa C — Fidelidad de las coordenadas GPS (LIMITACIÓN CONOCIDA)
Con A y B corregidos, el orden lat/lon y el sistema de coordenadas son correctos
(`posX=Lon`, `posY=Lat`; en Leaflet se dibuja `[y,x]=[lat,lon]`). El desajuste
**residual** que aún puede verse es de **precisión del trazado**: los waypoints GPS
de los CSV de muestra son **sintéticos-realistas** (trazados a mano sobre el
recorrido real en `docs/generate_samples.py`), por lo que sobre OSM siguen la forma
general del circuito pero **no calzan al asfalto con precisión de topógrafo**.

**Para alineación exacta se requiere un export GPS real del simulador.** El fixture
`iracing_monza_real.csv` mejora la densidad de muestreo (~360 pts/vuelta) y valida el
flujo, pero sigue siendo sintético-realista: no sustituye a un `.ibt`/telemetría real.

## 5. Consideración de unidades (documentada, sin cambio de comportamiento)

Los exports crudos de iRacing entregan el canal `Speed` en **m/s**. La aplicación
asume **km/h** en todo el stack (gráficos, leyenda del mapa, leaderboard). Los CSV de
muestra ya vienen en km/h para respetar ese contrato. Si en el futuro se ingiere un
export crudo real, hará falta **normalizar la unidad** (×3.6) en el parser o en la
carga. No se cambió ahora para no alterar el comportamiento existente (fuera del
alcance de B1); queda anotado como riesgo para Luis/despliegue.

## 6. Verificación en esta fase (B1)

- Flujo cubierto por tests: parseo → posición GPS → detección de vueltas
  (`IracingRealCsvFixtureTest`), con caso feliz sobre CSV real + errores (cabecera
  inválida, archivo vacío).
- El parser ignora columnas extra reales (`Gear`, `RPM`) y tolera orden de columnas
  no canónico (índice por nombre de cabecera).
- Demo manual sugerida: subir `demo_iracing_monza_real.csv` y verificar el trazado
  sobre Monza + gradiente de velocidad alineado con los marcadores de sector.

## 7. Manejo de error de la API externa (para la presentación)

`TrackMap.tsx` escucha el evento `tileerror` de Leaflet y muestra un banner
("Error al cargar el mapa…") **manteniendo la polyline** sobre fondo neutro. Para la
evidencia: DevTools → Network → Offline y recargar la vista del mapa (captura
`docs/evidencias/mapa-osm-fallo.png`, la organiza José en J6).
