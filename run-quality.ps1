# run-quality.ps1 — ejecuta tests + cobertura + SonarQube en ambos modulos
# Requiere:
#   - $env:SONAR_TOKEN_BACKEND  (token del proyecto backend en SonarQube)
#   - $env:SONAR_TOKEN_FRONTEND (token del proyecto frontend en SonarQube)
#   - SonarQube corriendo en http://localhost:9000
#   - sonar-scanner instalado globalmente (npm install -g sonar-scanner)

$ErrorActionPreference = "Stop"

Write-Host "== BACKEND: tests + JaCoCo + SonarQube ==" -ForegroundColor Cyan
Push-Location backend
mvn clean verify org.sonarsource.scanner.maven:sonar-maven-plugin:4.0.0.4121:sonar `
  "-Dsonar.host.url=http://localhost:9000" `
  "-Dsonar.token=$env:SONAR_TOKEN_BACKEND"
Pop-Location

Write-Host "== FRONTEND: vitest + lcov + SonarQube ==" -ForegroundColor Cyan
Push-Location frontend
npm run test:cov
sonar-scanner "-Dsonar.token=$env:SONAR_TOKEN_FRONTEND"
Pop-Location

Write-Host "OK: revisar dashboards en http://localhost:9000" -ForegroundColor Green
