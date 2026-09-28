#!/bin/sh
set -eu

# Render exposes its managed PostgreSQL connection as postgresql://...;
# Spring Boot expects the equivalent jdbc:postgresql://... URL.
if [ -z "${ERP_DB_URL:-}" ] && [ -n "${DATABASE_URL:-}" ]; then
  case "${DATABASE_URL}" in
    jdbc:*) export ERP_DB_URL="${DATABASE_URL}" ;;
    postgresql://*|postgres://*)
      # Render exposes the managed database URL as a PostgreSQL URI with
      # credentials in its authority section. The PostgreSQL JDBC driver
      # receives credentials separately through ERP_DB_USERNAME and
      # ERP_DB_PASSWORD, so remove the URI credentials before adding the
      # jdbc: scheme. Render percent-encodes reserved characters in these
      # credentials, therefore the first authority separator is safe here.
      database_target="${DATABASE_URL#*://}"
      database_target="${database_target#*@}"
      export ERP_DB_URL="jdbc:postgresql://${database_target}"
      ;;
    *) export ERP_DB_URL="jdbc:${DATABASE_URL}" ;;
  esac
fi

exec java -jar /app/erp_backend.jar
