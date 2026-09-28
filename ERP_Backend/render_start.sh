#!/bin/sh
set -eu

# Render exposes its managed PostgreSQL connection as postgresql://...;
# Spring Boot expects the equivalent jdbc:postgresql://... URL.
if [ -z "${ERP_DB_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "${DATABASE_URL}" in
    jdbc:*) export ERP_DB_URL="${DATABASE_URL}" ;;
    *) export ERP_DB_URL="jdbc:${DATABASE_URL}" ;;
  esac
fi

exec java -jar /app/erp_backend.jar
