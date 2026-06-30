# Revisión de Pares — Hito 5 (ApexMetrics)

**Equipo revisor:** equipo par  
**Fecha de la auditoría:** 2026-06-28  
**Alcance:** diagnóstico frontend (diseño, consistencia visual, deuda técnica)  
**Artefactos entregados:** `audit-frontend.md` + capturas en `design-audit/`

---

## Resumen de hallazgos

### Bugs visuales críticos (🔴)

| # | Hallazgo | Archivo afectado | Estado |
|---|---|---|---|
| 1 | `.neon-select` no definido en ningún CSS → `<select>` renderiza con fondo blanco nativo | `Leaderboard.tsx`, `UploadTelemetry.tsx` | Pendiente |
| 2 | `SessionAnalysis` y `ComparacionVueltas` no tienen sidebar; Dashboard/Leaderboard/Upload/Profile sí — navegación aparece y desaparece | múltiples páginas | Pendiente |
| 3 | Tres definiciones `:root` distintas (`auth.css` activo, `main.css` y `App.css` muertos) con paletas incompatibles | `src/styles/` | Pendiente |

### Inconsistencias de componentes (🟠)

| # | Hallazgo |
|---|---|
| 4 | `.neon-button` reutilizado con overrides inline para acciones muy distintas; no hay variantes formalizadas |
| 5 | Botones de acción de tabla (`VER ANÁLISIS`, `ELIMINAR`) 100% inline sin clase CSS |
| 6 | Sidebar y TopNavbar duplicados por copy-paste en 4 páginas → divergencias inevitables |

### Inconsistencias de tokens/estilo (🟡)

| # | Hallazgo |
|---|---|
| 7 | Radios de borde: 0 / 2 / 4 / 5 / 8 / 28px y 50% sin criterio unificado |
| 8 | Unidades mixtas: `rem` en CSS + `px` en inline + sin escala de espaciado |
| 9 | Colores hardcodeados fuera de tokens: `#151515`, `#00ff00`, `#ffaa00`, etc. |
| 10 | `text-transform: uppercase` casi universal → percepción "robótica/genérica" |
| 11 | `--text-muted #888` y `#666` sobre `#111` no alcanzan WCAG AA para texto pequeño |

### Código muerto (⚪)

| # | Hallazgo |
|---|---|
| 12 | `src/App.css` y `src/styles/main.css` — boilerplate Vite sin importar, a eliminar |
| 13 | `src/styles/leaderboard.css` — vacío |
| 14 | `src/main.jsx` duplica `src/main.tsx` (Vite usa `main.tsx`) |
| 15 | Login muestra check verde en email **siempre** (clase `success` hardcodeada) |
| 16 | `apex_role` leído en Profile pero nunca escrito en localStorage |

---

## Acciones tomadas en este hito

| Acción | Estado |
|---|---|
| Cobertura de tests frontend llevada de 61.73 % → 70.01 % (72 tests) | ✅ Completo |
| Tests añadidos: `TrackView.test.tsx`, `SessionAnalysis.test.tsx`, `UploadTelemetry.test.tsx` | ✅ Completo |
| Fix `TrackMap.test.tsx`: aserción `data-crs-simple` adaptada al CRS refactorizado por José | ✅ Completo |
| Feature de retroalimentación IA (Gemini 2.5 Flash) implementado con tests | ✅ Completo |

## Acciones pendientes (post-hito)

Las mejoras de diseño (refactor visual hacia estética Apple, extracción de componentes, sistema de tokens único) son una tarea de rediseño completo documentada en `audit-frontend.md` y `design-audit/`. Su alcance excede el hito actual; se propone abordarlas en un sprint dedicado de UI.

---

*Auditoría completa: ver [audit-frontend.md](audit-frontend.md) y capturas en [design-audit/](design-audit/).*
