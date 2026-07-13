#!/usr/bin/env bash
# run-tests.sh — Ejecuta la suite completa de pruebas (backend + frontend)
# Uso:
#   bash run-tests.sh
#
# Requisitos:
#   - Maven instalado (o wrapper ./mvnw disponible en backend/)
#   - Node.js 20+ y npm instalados
#   - Ejecutar desde la raíz del repositorio

set -e  # Detiene el script si cualquier comando falla

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "=============================================="
echo " ApexMetrics — Suite de pruebas completa"
echo "=============================================="
echo ""

# ── BACKEND ───────────────────────────────────────────────────
echo "[ 1/2 ] BACKEND: tests unitarios + integración + JaCoCo"
echo "----------------------------------------------"

cd "$ROOT_DIR/backend"

# Usar ./mvnw si existe, si no, asumir mvn en el PATH
if [ -f "./mvnw" ]; then
  MVN="./mvnw"
else
  MVN="mvn"
fi

$MVN clean verify -q
echo ""
echo "  [OK] Backend: BUILD SUCCESS"
echo "   Cobertura JaCoCo: backend/target/site/jacoco/index.html"
echo ""

# ── FRONTEND ──────────────────────────────────────────────────
echo "[ 2/2 ] FRONTEND: Vitest + cobertura V8"
echo "----------------------------------------------"

cd "$ROOT_DIR/frontend"

# Instalar dependencias si node_modules no existe
if [ ! -d "node_modules" ]; then
  echo "  Instalando dependencias npm..."
  npm install --silent
fi

npm run test:cov
echo ""
echo "  [OK] Frontend: todos los tests pasaron"
echo "   Cobertura V8: frontend/coverage/index.html"
echo ""

# ── RESUMEN ───────────────────────────────────────────────────
echo "=============================================="
echo "  Suite completa finalizada sin errores"
echo "=============================================="
echo ""
echo " Reportes de cobertura:"
echo "   Backend  → $ROOT_DIR/backend/target/site/jacoco/index.html"
echo "   Frontend → $ROOT_DIR/frontend/coverage/index.html"
echo ""
