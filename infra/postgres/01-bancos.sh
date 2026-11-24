#!/bin/bash
# Cria um banco e um usuário por serviço (ADR-001: cada serviço só enxerga o próprio banco).
set -euo pipefail
for svc in inv chk tkt keycloak; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<SQL
CREATE USER ${svc}_app WITH PASSWORD '${APP_DB_PASSWORD}';
CREATE DATABASE ${svc} OWNER ${svc}_app;
REVOKE ALL ON DATABASE ${svc} FROM PUBLIC;
SQL
done
