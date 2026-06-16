#!/usr/bin/env bash
# run-quality.sh — ejecuta tests + cobertura + SonarQube en ambos modulos
# Requiere:
#   - $SONAR_TOKEN_BACKEND  (token del proyecto backend en SonarQube)
#   - $SONAR_TOKEN_FRONTEND (token del proyecto frontend en SonarQube)
#   - SonarQube corriendo en http://localhost:9000
#   - sonar-scanner instalado globalmente (npm install -g sonar-scanner)

set -e

echo "== BACKEND: tests + JaCoCo + SonarQube =="
( cd backend && mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar \
    -Dsonar.host.url=http://localhost:9000 \
    -Dsonar.token="$SONAR_TOKEN_BACKEND" )

echo "== FRONTEND: vitest + lcov + SonarQube =="
( cd frontend && npm run test:cov && \
    sonar-scanner -Dsonar.token="$SONAR_TOKEN_FRONTEND" )

echo "OK: revisar dashboards en http://localhost:9000"
