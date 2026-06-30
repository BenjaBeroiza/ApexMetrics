# Auditoría de Frontend — ApexMetrics

> Documento de diagnóstico previo a la migración del estilo visual hacia un lenguaje
> tipo **Apple** (minimalista, mucho whitespace, jerarquía tipográfica clara, sombras
> suaves, radios consistentes, microinteracciones sutiles).
>
> **Estado actual:** estética "cyberpunk / HUD de simulador" — fondo negro, acentos
> neón cian, mayúsculas omnipresentes, tipografía monoespaciada, glow/sombras duras.
> Funciona como identidad, pero es exactamente lo que se percibe como "robótico/genérico".
>
> **Fecha:** 2026-06-28 · **Alcance:** solo diagnóstico. No se modificó código.

---

## 1. Stack y librerías de UI

| Capa | Tecnología | Versión | Notas |
|---|---|---|---|
| Framework | **React** | 19.2.5 | SPA con `StrictMode` |
| Build / dev server | **Vite** | 8.0.x | proxy `/api` → `http://localhost:8080` |
| Lenguaje | **TypeScript** | ~6.0 | algunas páginas con `// @ts` laxo e inline styles tipados |
| Routing | **react-router-dom** | 7.2.0 | `BrowserRouter` en `App.tsx`, sin guard central |
| Gráficos | **recharts** | 3.9.0 | `LineChart` en análisis y comparación |
| Iconos | **lucide-react** | 1.17.0 | usado en sidebar, navbar, formularios |
| Testing | Vitest + Testing Library + jsdom | — | hay `.test.tsx` por página |

### Sistema de estilos
- **CSS plano** importado por archivo. **No hay** Tailwind, CSS Modules,
  styled-components, ni librería de componentes (Material, shadcn, Chakra, etc.).
- Los estilos viven en `src/styles/*.css` y se importan dentro de cada página
  (`import '../styles/dashboard.css'`), por lo que **son globales** (sin scoping).
- **Uso intensivo de `style={{…}}` inline** en casi todas las páginas: colores,
  espaciados, tamaños y bordes hardcodeados directamente en JSX. Este es el mayor
  obstáculo para una migración limpia: el rediseño no se puede hacer solo tocando CSS.

### Archivos de estilo
| Archivo | Líneas | Estado |
|---|---|---|
| `src/index.css` | 36 | Reset + import de fuente Inter + scrollbar custom. **Activo** (lo importa `main.tsx`). |
| `src/styles/auth.css` | 211 | Login/Register. Define el `:root` con los **tokens neón** reales. **Activo**. |
| `src/styles/dashboard.css` | 422 | Layout app (sidebar, tabla, dropzone, navbar…). **Activo**. |
| `src/styles/leaderboard.css` | 1 | **Vacío** (solo un comentario). |
| `src/App.css` | 184 | **Boilerplate de Vite** (`.hero`, `.vite`, `#next-steps`…). **Muerto / no importado.** |
| `src/styles/main.css` | 64 | Otro `:root` con paleta **azul distinta** (`#0b1020`, `#8ea3ff`). **No importado / muerto.** |

---

## 2. Design tokens actuales

> ⚠️ **No hay un único sistema de tokens.** Conviven **tres** definiciones de `:root`
> con paletas distintas (dos de ellas en archivos muertos), más decenas de colores
> hardcodeados inline. Esto debe consolidarse antes/durante la migración.

### 2.1 Paleta de color — fuente real (`auth.css`, en uso)
| Token | Valor | Uso |
|---|---|---|
| `--bg-dark` | `#111111` | fondo global de la app |
| `--bg-card` | `#1c1c1c` | tarjetas de auth |
| `--neon-cyan` | `#00ffff` | **acento primario** (títulos, links activos, valores, bordes focus) |
| `--neon-cyan-dim` | `rgba(0,255,255,0.2)` | glows y fondos sutiles |
| `--error-red` | `#ff6b6b` | errores, botón de logout, eje de velocidad |
| `--error-red-dim` | `rgba(255,107,107,0.2)` | glow de error |
| `--text-main` | `#ffffff` | texto principal |
| `--text-muted` | `#888888` | texto secundario / labels |
| `--border-color` | `#333333` | bordes y separadores |

### 2.2 Colores hardcodeados (NO tokenizados) encontrados en el código
Estos aparecen sueltos en CSS y en `style={{}}` inline — fragmentan la paleta:
`#151515`, `#1a1a1a`, `#111`, `#333`, `#444`, `#666`, `#00ff00` (verde éxito),
`#ffaa00` (naranja "processing"), `#E63946` (rojo gráfico), `#1B6CA8` (azul gráfico),
`rgba(255,255,255,0.02 / .05 / .12)`, `rgba(0,255,255,0.04 / .05 / .08)`.

### 2.3 Tokens de archivos muertos (a eliminar, no usar como referencia)
- `main.css` → paleta azul: `#0b1020`, `#060913`, `#8ea3ff`, gradiente radial.
- `App.css` → usa `var(--accent)`, `var(--border)`, `var(--text-h)`, `var(--shadow)`
  que **nunca se definen** (boilerplate Vite roto).

### 2.4 Tipografía
| Aspecto | Valor actual |
|---|---|
| Familia principal | **Inter** (Google Fonts, weights 400/500/600/700) |
| Familia secundaria | **monospace** del sistema — usada en inputs, celdas de tabla, paginación, `secure-text` |
| Escala (no formal) | `0.7 / 0.75 / 0.8 / 0.85 / 0.9 / 0.95 / 1 / 1.2 / 1.8 / 2 rem` + `h1` de página `2rem` + `clamp()` en boilerplate |
| Weights | mezcla de `bold` y `font-weight: 700`; labels con `letter-spacing` 1–4px |
| Transformación | **`text-transform: uppercase` casi universal** (títulos, labels, botones, nav, celdas) — rasgo central del look "robótico" |

**Para el target Apple:** reducir mayúsculas drásticamente, eliminar `letter-spacing`
amplio, abandonar monospace para datos de UI (reservarlo, si acaso, para tiempos de
vuelta), y formalizar una escala tipográfica (p.ej. 12/14/16/20/24/32/40).

### 2.5 Espaciado
- Sin escala definida. Mezcla de unidades: **`rem`** en CSS (`0.4 / 0.5 / 0.8 / 1 / 1.2 / 1.5 / 2 / 2.5 / 3 rem`), **`px`** en inline y boilerplate, valores sueltos.
- Paddings de página: `2rem 2.5rem` (main), `2rem` (tarjetas) — relativamente consistentes, pero el resto es ad-hoc.

### 2.6 Radios de borde — **muy inconsistentes**
| Elemento | Radio |
|---|---|
| `.auth-card` | **0** (esquinas vivas, borde superior neón) |
| inputs de auth | **0** |
| `.nav-item` | `4px` |
| search input, status-item | `4px` |
| dropzone, tarjetas de perfil | `8px` |
| `.page-btn` (paginación) | `2px` |
| `.counter` (boilerplate) | `5px` |
| avatar / dots | `50%` |
| `.hero-card` (muerto) | `28px` |

→ El target Apple exige **un radio base consistente** (p.ej. 10–14px) en todo.

### 2.7 Sombras
- Predominan **sombras duras + glow neón**, no suaves:
  - `box-shadow: 0 0 12px var(--neon-cyan-dim)` (focus inputs)
  - `0 0 15px/20px` glows en texto y botones
  - `0 15px 40px rgba(0,0,0,0.6)` (auth-card), `0 24px 80px rgba(0,0,0,.35)` (muerto)
  - `text-shadow` neón en títulos y links
- **Para Apple:** reemplazar por sombras suaves y difusas de baja opacidad
  (`0 1px 2px rgba(0,0,0,.06)`, `0 8px 24px rgba(0,0,0,.08)`), eliminar glows.

### 2.8 Breakpoints
Definidos solo en `dashboard.css` (y parcialmente en boilerplate):
- `≤ 1024px` (tablet) · `≤ 768px` (móvil/tablet pequeña) · `≤ 480px` (móvil pequeño).
- `auth.css` **no es responsive** salvo un ajuste de `.auth-card` dentro del bloque 480px de dashboard.

### 2.9 Microinteracciones / transiciones
- `transition: all 0.2s/0.3s ease` en varios elementos (hover de nav, inputs, botones).
- `@keyframes blink` (texto "Cargando…" parpadeante, estética terminal).
- Sin easing curado ni transforms sutiles (escala/translate en hover) típicos de Apple.

---

## 3. Inventario de componentes

> El proyecto **prácticamente no tiene componentes reutilizables**: solo `SessionChart`.
> El resto son páginas monolíticas que **duplican** el shell (sidebar + navbar) por copia.
> Esto es relevante: para el rediseño conviene extraer primero `Sidebar`, `TopNavbar`,
> `Button`, `Card`, `Input`, `Table` y `PageHeader`.

### Componentes reales
| Componente | Path | Props | Estilos que usa |
|---|---|---|---|
| `SessionChart` | `src/components/SessionChart.tsx` | `sessionId: number \| string` | recharts inline (`stroke #E63946`/`#1B6CA8`, grid `#333`); estados `.loading-state` / `.blink-text`; colores inline |
| `App` (router) | `src/App.tsx` | — | ninguno (solo rutas) |

### "Pseudo-componentes" repetidos inline (candidatos a extraer)
| Patrón | Dónde aparece | Estilos |
|---|---|---|
| **Sidebar** (logo + nav + logout) | Dashboard, Leaderboard, Upload, Profile (4 copias casi idénticas) | `.sidebar`, `.sidebar-header`, `.sidebar-nav`, `.nav-item(.active)`, `.neon-button` |
| **TopNavbar** (search + iconos campana/ajustes/usuario) | Dashboard, Leaderboard, Upload, Profile | `.top-navbar`, `.search-bar`, `.search-icon`, `.top-icons` |
| **PageHeader** (`h1` + `p`) | todas las páginas privadas | `.page-header` + overrides inline |
| **Botón primario** | todas | `.neon-button` (+ muchos overrides inline de width/padding/color) |
| **Tabla** | Dashboard, Leaderboard | `.minimal-table` (+ `th`/`td` con colores inline) |
| **Input + label** | Auth, Profile, Upload | `.input-group`, `.input-wrapper(.success/.error)` |
| **Select** | Leaderboard, Upload | `.neon-select` ⚠️ **clase sin definición CSS** (ver §6) |
| **Dropzone** | Upload | `.dropzone(.has-file)`, `.dropzone-icon` |
| **Estado de carga** | varias | `.loading-state`, `.blink-text` |
| **Paginación** | Leaderboard | `.pagination`, `.page-btn(.active)` |
| **Cola de ingestión** | Upload | `.upload-status-area`, `.status-item(.processing/.success/.error)` |

---

## 4. Páginas / rutas

Definidas en `src/App.tsx`. Protección de rutas **descentralizada**: cada página
privada lee `apex_token` de `localStorage` y redirige a `/login` si falta (no hay
guard común — pendiente según comentario en el código).

| Ruta | Página | RF | Descripción |
|---|---|---|---|
| `/login` | `Login.tsx` | RF02 | Tarjeta centrada de inicio de sesión. Email + password (mín. 16). Glow neón, "CONEXIÓN SEGURA", dots de estado decorativos. |
| `/register` | `Register.tsx` | RF01 | Registro de "operador": alias, correo, país, clave + confirmación. Copys muy temáticos ("CLAVE DE AUTORIZACIÓN", "ENCRYPTING_"). |
| `/leaderboard` | `Leaderboard.tsx` | RF07 | Clasificación global pública con filtros por categoría/circuito y paginación. Sidebar + navbar + tabla. **Es pública pero igual muestra el sidebar de app.** |
| `/dashboard` | `Dashboard.tsx` | RF08/09 | Historial de sesiones del piloto en tabla (circuito, categoría, mejor tiempo, fecha, acciones ver/eliminar). Botón "COMPARAR VUELTAS" si ≥2 sesiones. |
| `/dashboard/sesiones/:id/analisis` | `SessionAnalysis.tsx` | RF05 | Curvas velocidad/freno de una sesión vía `SessionChart`. **Sin sidebar** (solo botón "volver"). |
| `/comparacion` | `ComparacionVueltas.tsx` | RF06 | Compara dos sesiones superponiendo curvas (línea sólida vs punteada). Selectores A/B. **Sin sidebar.** |
| `/upload` | `UploadTelemetry.tsx` | RF04 | Form de carga CSV: simulador, circuito, categoría, mejor vuelta + dropzone + cola de estado. Sidebar + navbar. |
| `/profile` | `Profile.tsx` | RF03 | Perfil del piloto (avatar + datos read-only). Carga del backend con fallback a localStorage. |
| `/` | → `Navigate` | — | Redirige a `/login`. |

---

## 5. Capturas de pantalla

Generadas con **Playwright (Chromium headless)** levantando el dev server de Vite y
**mockeando las respuestas de `/api`** (el backend Spring Boot no estaba corriendo),
con un token inyectado en `localStorage` para renderizar las páginas privadas pobladas.
Viewport 1440×900 @2x. Guardadas en **`/design-audit/`**:

| Archivo | Pantalla |
|---|---|
| `login.png` | Login |
| `register.png` | Registro |
| `dashboard.png` | Dashboard / Mis sesiones |
| `leaderboard.png` | Clasificación global |
| `upload.png` | Subir telemetría |
| `profile.png` | Mi perfil |
| `session-analysis.png` | Análisis de sesión (gráfico recharts) |
| `comparacion.png` | Comparación de vueltas |

**Lectura visual rápida (las 5 más representativas):**
- **Login** — tarjeta oscura centrada, título cian con glow, borde superior neón,
  inputs monospace con esquinas vivas, botón negro con borde cian. Estética "terminal".
- **Dashboard** — sidebar oscura izquierda con nav en mayúsculas, navbar superior con
  search y 3 iconos, tabla minimalista con borde superior cian; tiempos en cian monospace.
- **Leaderboard** — mismo shell; **los `<select>` de filtro se ven con fondo BLANCO**
  (estilo nativo del navegador), rompiendo por completo el tema oscuro (ver §6).
- **Session Analysis** — gráfico recharts (velocidad rojo / freno azul) sobre fondo
  negro, **sin sidebar**: la página "flota" sin la navegación del resto.
- **Profile** — dos tarjetas (avatar + datos) con radios 8px, campos read-only atenuados.

---

## 6. Inconsistencias detectadas

### 🔴 Bugs visuales / de sistema
1. **`.neon-select` no existe en ningún CSS.** Se usa en `Leaderboard.tsx` y
   `UploadTelemetry.tsx`, por lo que los `<select>` caen al estilo **nativo del navegador
   (fondo blanco)** — claramente visible en `leaderboard.png`. Rompe el tema oscuro.
2. **Layout incoherente:** `SessionAnalysis` y `ComparacionVueltas` **no tienen sidebar**,
   mientras Dashboard/Leaderboard/Upload/Profile sí. La navegación aparece y desaparece.
3. **Tres `:root` con paletas distintas** (`auth.css` neón cian — real; `main.css` azul —
   muerto; `App.css` con vars nunca definidas — muerto). Fuente de verdad ambigua.

### 🟠 Inconsistencias de componentes
4. **Botones:** un único `.neon-button` reutilizado para acciones muy distintas
   (login, logout, comparar, sincronizar, examinar, guardar) con **overrides inline**
   de `width`, `padding`, `fontSize`, `color`, `borderColor`. No hay variantes
   (primario/secundario/peligro) formalizadas → misma acción se ve distinta según la página.
5. **Acciones de tabla** (`VER ANÁLISIS`, `ELIMINAR`) son `<button>` con **estilo 100% inline**,
   sin clase, distintos de los demás botones.
6. **Sidebar y TopNavbar duplicados por copy-paste** en 4 páginas → divergencias
   inevitables al mantener (p.ej. el logout del Dashboard usa rojo inline; el de
   Leaderboard es "SINCRONIZAR DATOS" cian — mismo slot, distinto propósito y color).

### 🟡 Inconsistencias de tokens / estilo
7. **Radios de borde por todos lados:** 0, 2, 4, 5, 8, 28px y `50%` (ver §2.6).
8. **Mezcla de unidades:** `rem` (CSS de auth/dashboard) + `px` (inline y boilerplate) +
   unitless. Sin escala de espaciado.
9. **Colores hardcodeados** fuera de tokens: `#151515`, `#1a1a1a`, `#00ff00`, `#ffaa00`,
   `#666`, etc. (ver §2.2). El verde/naranja de estados no están tokenizados.
10. **Mayúsculas + `letter-spacing` + monospace** omnipresentes: principal causa de la
    percepción "robótica". Inconsistente además (algunos textos en Title Case, otros UPPER).
11. **Contraste bajo:** `--text-muted #888` y `secure-text #666` sobre fondos `#111`/`#1c1c1c`
    quedan por debajo de WCAG AA para texto pequeño.

### ⚪ Limpieza / código muerto (no visual, pero afecta la migración)
12. **`src/App.css` y `src/styles/main.css`** son boilerplate de Vite no importado → eliminar.
13. **`src/styles/leaderboard.css`** está vacío (solo comentario).
14. **`src/main.jsx` duplica a `src/main.tsx`** (dos entrypoints; Vite usa `main.tsx` vía `index.html`).
15. **Doble import de Inter** (Google Fonts en `index.css` + referencia en `main.css` muerto).
16. **`apex_role`** se lee en `Profile` pero nunca se escribe en `localStorage`.
17. **Login** muestra el check verde de "éxito" en el email **siempre** (clase `success`
    hardcodeada), sin validar realmente.

---

## 7. Recomendaciones para la migración (resumen accionable)

> No forma parte del diagnóstico pedido, pero deja el camino trazado.

1. **Crear un sistema de tokens único** (CSS variables o un `tokens.css`): paleta neutra
   clara + 1 acento, escala tipográfica, escala de espaciado (4/8pt), radios (1 base),
   sombras suaves. Eliminar las 3 `:root` actuales y los hex sueltos.
2. **Extraer componentes** antes de re-estilizar: `AppShell` (Sidebar + TopNavbar),
   `Button` (variantes), `Card`, `Input`, `Select`, `Table`, `PageHeader`. Mover todos
   los `style={{}}` inline a estos componentes.
3. **Reducir mayúsculas/monospace/letter-spacing**, subir whitespace, suavizar sombras
   y unificar radios → estética Apple.
4. **Unificar el layout**: que análisis y comparación usen el mismo `AppShell`.
5. **Arreglar `.neon-select`** (o reemplazar `<select>` por un componente propio).
6. **Borrar código muerto** (`App.css`, `main.css`, `main.jsx`, `leaderboard.css`).
7. Evaluar adoptar **Tailwind o CSS Modules** para evitar CSS global y disciplinar tokens
   (decisión de equipo).

---

*Generado sin modificar código de la aplicación. Capturas en `/design-audit/`.*
