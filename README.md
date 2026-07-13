# ApexMetrics

Plataforma de análisis de telemetría para **Sim Racing**. Permite a pilotos subir
sus registros de sesión en formato CSV (iRacing / Assetto Corsa), visualizar el
trazado en el mapa, comparar vueltas y obtener retroalimentación mediante IA.

---

## Equipo

| Rol | Nombre |
|-----|--------|
| Tech Lead / Backend | Benjamin Beroiza |
| Frontend Developer | Luis Jaramillo |
| DevOps Engineer | Jose Villablanca |

---

## URL de despliegue

> **PENDIENTE** — se actualiza al desplegar en el VPS de producción.

---

## Prerrequisitos

| Herramienta | Versión mínima | Necesaria para |
|-------------|----------------|----------------|
| [Docker Desktop](https://www.docker.com/products/docker-desktop/) | 24+ | Levantar el stack completo |
| [Node.js](https://nodejs.org/) | 20 LTS | Desarrollo frontend local |
| [JDK](https://adoptium.net/) | 17 | Desarrollo backend local |
| [Maven](https://maven.apache.org/) | 3.9+ | Compilar y testear el backend |

---

## Levantar el sistema (un solo comando)

```bash
# 1. Copia la plantilla de variables de entorno
cp docker/.env.example docker/.env

# 2. Edita docker/.env si necesitas cambiar contraseñas o la clave de Gemini
#    (los valores por defecto funcionan para desarrollo local)

# 3. Levanta frontend + backend + base de datos
cd docker
docker compose up --build
```

Una vez iniciado, los servicios quedan disponibles en:

| Servicio | URL |
|----------|-----|
| Frontend | <http://localhost:5173> |
| Backend API | <http://localhost:8080> |
| PostgreSQL | `localhost:5433` (mapeado desde el contenedor) |

Para detener:

```bash
docker compose down          # detiene los contenedores
docker compose down -v       # detiene y borra el volumen de datos
```

> **Nota:** El primer `--build` puede tardar varios minutos al descargar las
> dependencias de Maven. Las ejecuciones posteriores usan la caché de Docker.

---

## Ejecución de pruebas

### Script único (backend + frontend)

```bash
# Desde la raíz del repositorio
bash run-tests.sh
```

El script ejecuta en secuencia:

1. `mvn clean verify` → tests unitarios + integración + reporte JaCoCo.
2. `npm run test:cov` → Vitest + cobertura V8.

Reportes de cobertura generados:

| Módulo | Ruta del reporte |
|--------|-----------------|
| Backend (JaCoCo) | `backend/target/site/jacoco/index.html` |
| Frontend (V8) | `frontend/coverage/index.html` |

### Backend (individual)

```bash
cd backend
mvn clean verify
```

Resultado esperado: todos los tests pasan (`BUILD SUCCESS`).  
Cobertura mínima en capa Service: **≥ 85%** (validado por JaCoCo).

### Frontend (individual)

```bash
cd frontend
npm install        # solo la primera vez
npm run test:cov   # tests + reporte de cobertura
```

Resultado esperado: todos los tests pasan (Vitest).  
Reporte de cobertura: `frontend/coverage/index.html`.

---

## Estructura del repositorio

```
ApexMetrics/
├── backend/                         # API REST — Spring Boot 3 / Java 17
│   ├── src/main/java/com/apexmetrics/
│   │   ├── auth/                    # UC01 registro, UC02 login, UC03 reset password
│   │   ├── telemetry/               # Upload CSV, análisis, feedback IA (Gemini)
│   │   ├── leaderboard/             # Clasificación global
│   │   └── shared/                  # Config, excepciones, CSV helpers
│   ├── src/test/java/com/apexmetrics/
│   │   ├── auth/controller/         # Tests de integración: Auth, PasswordReset, UserProfile
│   │   ├── telemetry/               # Tests de integración y unitarios: parser, servicio Gemini
│   │   └── leaderboard/             # Tests de integración: Leaderboard
│   └── Dockerfile
│
├── frontend/                        # SPA — React 19 + Vite + TypeScript
│   ├── src/
│   │   ├── components/              # SessionChart, TrackMap (con tests .test.tsx)
│   │   ├── pages/                   # Login, Register, Dashboard, etc. (con tests .test.tsx)
│   │   ├── services/                # (próxima iteración: extraer fetch a servicios)
│   │   └── styles/                  # CSS por módulo
│   ├── sonar-project.properties     # Config SonarQube frontend
│   └── Dockerfile
│
├── docker/
│   ├── docker-compose.yml           # Stack completo: frontend + backend + db
│   ├── sonarqube-local.yml          # SonarQube + PostgreSQL para análisis local
│   └── .env.example                 # Plantilla de variables de entorno
│
├── docs/
│   ├── samples/                     # CSVs de demostración (iRacing y Assetto Corsa)
│   ├── evidencias/                  # Capturas de SonarQube y mapa OSM
│   └── generate_samples.py          # Script para regenerar CSVs de muestra
│
├── run-tests.sh                     # Script único: tests backend + frontend
├── run-quality.sh                   # Script: tests + SonarQube (Linux/macOS)
├── run-quality.ps1                  # Script: tests + SonarQube (Windows/PowerShell)
└── .env.example                     # Plantilla de tokens SonarQube (raíz)
```

---

## Análisis de calidad con SonarQube

### 1. Levantar SonarQube local

```bash
docker compose -f docker/sonarqube-local.yml up -d
```

Esperar ~2 minutos y acceder a <http://localhost:9000> con `admin` / `admin`.
Crear dos proyectos:

- `apexmetrics-backend`
- `apexmetrics-frontend`

Generar un **token** para cada uno desde **Mi Cuenta → Seguridad → Generar Token**.

### 2. Configurar tokens

Copia `.env.example` a `.env` en la raíz y completa:

```
SONAR_TOKEN_BACKEND=<token-backend>
SONAR_TOKEN_FRONTEND=<token-frontend>
```

### 3. Ejecutar el pipeline

```powershell
# Windows — PowerShell
.\run-quality.ps1
```

```bash
# Linux / macOS
bash run-quality.sh
```

Verificar resultados en <http://localhost:9000>.  
Capturas de referencia en [`docs/evidencias/`](docs/evidencias/EVIDENCIAS.md).

---

## Datos de demostración

Los archivos de muestra en `docs/samples/` permiten probar el flujo completo de
carga sin necesidad de un simulador real:

| Archivo | Simulador | Circuito | Uso recomendado |
|---------|-----------|----------|-----------------|
| `demo_iracing_monza.csv` | iRacing | Monza | Prueba básica de carga |
| `demo_iracing_monza_real.csv` | iRacing | Monza | Prueba con ~300 puntos reales |
| `demo_iracing_spa.csv` | iRacing | Spa | Prueba de trazado largo |
| `demo_assetto_corsa_pos.csv` | Assetto Corsa | — | Prueba de parser alternativo |

**Pasos para cargar una sesión de demostración:**

1. Registra o inicia sesión en la aplicación.
2. Ve a **Subir Telemetría**.
3. Selecciona uno de los archivos de `docs/samples/`.
4. Elige el simulador, circuito y categoría correspondientes.
5. Ingresa un tiempo de vuelta (ej. `82.5` segundos).

---

## Recuperación de contraseña (desarrollo local)

El flujo de reset de contraseña (UC03) genera un token que en desarrollo
se imprime directamente en el **log del backend** (no se envía email).

Para verlo:

```bash
# Si usas Docker:
docker compose logs backend | grep -i "reset"

# Si corres el backend local:
# El token aparece en la consola de Maven con nivel INFO
```

Usa el token en el endpoint:

```
POST /api/v1/auth/reset-password
{ "token": "<token-del-log>", "newPassword": "nueva_contraseña_segura" }
```
