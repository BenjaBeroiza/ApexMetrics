# ApexMetrics

Plataforma de análisis de telemetría para Sim Racing. Permite a los pilotos 
subir sus registros de sesión en formato CSV, visualizar su historial personal 
y competir en una clasificación global por circuito y categoría.

## Equipo

| Rol | Nombre |
|-----|--------|
| Tech Lead / Backend | Benjamin Beroiza |
| Frontend Developer | Luis Jaramillo |
| DevOps Engineer | Jose Villablanca |

## Stack tecnológico

| Capa | Tecnología |
|------|-----------|
| Frontend | React 18 + Vite, Node 20 |
| Backend | Spring Boot 3.3.5, Java 17, Maven |
| Base de datos | PostgreSQL 15 |
| ORM / Migraciones | JPA + Hibernate + Flyway |
| Autenticación | JWT (jjwt 0.12.3) |
| Infraestructura | Docker + Docker Compose |

## Estructura del repositorio

```
ApexMetrics/
├── frontend/          # Aplicación React + Vite
│   ├── src/
│   ├── Dockerfile
│   └── nginx.conf
├── backend/           # API REST Spring Boot
│   ├── src/
│   └── Dockerfile
├── docker/
│   └── docker-compose.yml
└── docs/
```

## Requisitos previos

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) instalado y corriendo
- Para desarrollo local del frontend: Node.js 20+

---

## Levantar el sistema completo con Docker

Este es el método recomendado para el equipo. Levanta frontend, backend 
y base de datos en un solo comando.

```bash
# Desde la raíz del proyecto
cd docker
docker compose up --build
```

Una vez iniciado, los servicios quedan disponibles en:

| Servicio | URL |
|----------|-----|
| Frontend | http://localhost:5173 |
| Backend API | http://localhost:8080 |
| PostgreSQL | localhost:5432 |

Para detener todos los servicios:

```bash
docker compose down
```

Para detener y eliminar los datos de la base de datos:

```bash
docker compose down -v
```

> **Nota:** El primer `--build` puede tardar varios minutos porque descarga 
> las dependencias de Maven y compila el JAR. Las ejecuciones siguientes 
> son más rápidas al usar la caché de Docker.

---

## Modo desarrollo (frontend local + backend Docker)

Útil cuando se trabaja activamente en el frontend y se necesita hot-reload.

**1. Levantar solo backend y base de datos:**

```bash
cd docker
docker compose up backend db --build
```

**2. En otra terminal, levantar el frontend con Vite:**

```bash
cd frontend
npm install
npm run dev
```

El frontend queda en http://localhost:5173. Las llamadas a `/api/*` 
son redirigidas automáticamente por el proxy de Vite al backend en 
http://localhost:8080.

---

## Variables de entorno

Las siguientes variables son configurables al correr el stack con Docker. 
Los valores por defecto funcionan para desarrollo local.

| Variable | Valor por defecto | Descripción |
|----------|-------------------|-------------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/apexmetrics_db` | URL de conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `apexuser` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD` | `apexpass` | Contraseña de la base de datos |
| `JWT_SECRET` | `apexmetrics-dev-secret-...` | Clave secreta para firmar tokens JWT |
| `JWT_EXPIRATION_MS` | `3600000` | Duración del token JWT (1 hora) |

> **Importante:** Cambiar `JWT_SECRET` en cualquier entorno de producción.

---

## Endpoints de la API

Base URL: `http://localhost:8080/api/v1`

### Autenticación (público)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/auth/register` | Registrar nuevo usuario |
| `POST` | `/auth/login` | Iniciar sesión, retorna JWT |

**Body de registro:**
```json
{
  "username": "string",
  "email": "string",
  "password": "string (mínimo 16 caracteres)"
}
```

**Body de login:**
```json
{
  "email": "string",
  "password": "string"
}
```

### Leaderboard (público)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `GET` | `/leaderboard` | Clasificación global paginada |

Parámetros opcionales: `categoryId`, `trackId`, `page` (default 0), `size` (default 20).

### Telemetría (requiere JWT)

Incluir header: `Authorization: Bearer <token>`

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| `POST` | `/telemetry/upload` | Subir sesión CSV (multipart/form-data) |
| `GET` | `/telemetry/sesiones` | Historial de sesiones del usuario |
| `DELETE` | `/telemetry/sesiones/{id}` | Eliminar sesión propia |

**Campos del upload:**
- `file` — archivo `.csv` (máx. 10 MB)
- `simulatorType` — `IRACING` o `ASSETTO_CORSA`
- `trackId` — ID del circuito
- `categoryId` — ID de la categoría
- `bestLapTime` — tiempo en segundos (ej. `70.450`)

---

## Datos de seed

La base de datos se inicializa automáticamente con Flyway al primer arranque.

**Circuitos disponibles:**

| ID | Nombre | País |
|----|--------|------|
| 1 | Autodromo Nazionale di Monza | Italia |
| 2 | Circuit de Spa-Francorchamps | Bélgica |
| 3 | Nürburgring Nordschleife | Alemania |
| 4 | Circuit de Catalunya | España |
| 5 | Autodromo Enzo e Dino Ferrari | Italia |

**Categorías disponibles:**

| ID | Nombre |
|----|--------|
| 1 | GT3 |
| 3 | F1 |
| 4 | WEC |

> Los IDs 2 y 5 (GT4 y TCR) fueron eliminados por la migración V2.

---

## Tests

### Backend

```bash
cd backend
mvn verify
```

Ejecuta tests unitarios y de integración `@SpringBootTest`. Resultado esperado:
- **45/45 tests pasan** (`BUILD SUCCESS`)
- Reporte JaCoCo en `backend/target/site/jacoco/index.html` (cobertura ≥ 85% en capa Service)

### Frontend

```bash
cd frontend
npm install        # solo la primera vez
npm run test:cov   # tests + reporte de cobertura
```

Resultado esperado:
- **39/39 tests pasan** (Vitest)
- Reporte de cobertura en `frontend/coverage/lcov.info`

### Pipeline de calidad completo (SonarQube local)

**1. Levantar SonarQube:**

```bash
docker compose -f docker/sonarqube-local.yml up -d
```

Esperar ~2 min y acceder a `http://localhost:9000` con `admin` / `admin`. Crear dos proyectos (`apexmetrics-backend` y `apexmetrics-frontend`) y generar un token para cada uno desde **Mi Cuenta → Seguridad → Generar Token**.

**2. Copiar `.env.example` a `.env` y completar los tokens:**

```
SONAR_TOKEN_BACKEND=<token-generado>
SONAR_TOKEN_FRONTEND=<token-generado>
```

**3. Ejecutar el pipeline:**

```powershell
# PowerShell (Windows)
$env:SONAR_TOKEN_BACKEND="<token>"; $env:SONAR_TOKEN_FRONTEND="<token>"
.\run-quality.ps1
```

```bash
# Bash (Linux/macOS)
export SONAR_TOKEN_BACKEND="<token>" && export SONAR_TOKEN_FRONTEND="<token>"
bash run-quality.sh
```

Verificar resultados en `http://localhost:9000`.

---

## Simuladores soportados

| Valor del campo | Simulador |
|-----------------|-----------|
| `IRACING` | iRacing |
| `ASSETTO_CORSA` | Assetto Corsa |
