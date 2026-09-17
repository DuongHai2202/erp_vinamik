# persistence strategy

## decision

The database is created and upgraded by Flyway migrations in PostgreSQL. JPA, using Spring Data JPA and Hibernate, is the only application persistence API. Hibernate uses `ddl-auto=validate` so it checks mappings without creating or changing tables.

Repositories use Spring Data JPA/JPQL and projections for CRUD, joins, filtering and paging. A repository may use native queries through Spring Data JPA or `EntityManager` when PostgreSQL semantics are part of the business rule, including inventory ledger posting, balance projections, stocktake adjustments, transfers, pessimistic row locks, append-only movement checks and high-volume joined projections. Application code must not use `JdbcTemplate`, Spring JDBC or the JDBC API directly. The PostgreSQL JDBC driver remains an internal Hibernate/JPA connection dependency.

## module boundary

Each feature keeps its persistence code inside its owner package:

    human_resources/
      employee/
        entity/employee_entity.java
        repository/human_resources_employee_repository.java
        repository/human_resources_employee_read_repository.java
        dto records
        service
        controller
    inventory/
      issue/
        repository/
        service
    production/
      order/
        repository/
        service

A JPA entity is never returned directly from an API and is never imported by another business module. Cross-module references remain scalar IDs and public contracts. For example, Production calls the Inventory contract to validate a stock item or post a receipt; it does not inject the Inventory repository.

## why this split

JPA repositories reduce mapping and persistence boilerplate for ordinary create/update/deactivate operations. The inventory ledger has stronger requirements than ordinary CRUD: append-only movements, deterministic lock order, idempotency and non-negative balances. Keeping those statements in one JPA repository, using bound native queries where needed, makes the transaction boundary visible and preserves PostgreSQL locking behavior. The service depends on a repository abstraction, so persistence details are not spread across controllers or business rules.

## migration order

1. Add JPA dependency and PostgreSQL mapping validation.
2. Migrate HR employee and Inventory stock item master-data writes.
3. Migrate the remaining HR and Inventory master-data aggregates.
4. Migrate Production plan, BOM, order and assignment CRUD.
5. Keep receipt, issue, transfer, stocktake, balance and production handoff repositories in JPA; use bound native queries for PostgreSQL locking and idempotency rules.
6. Remove all direct JDBC/Spring JDBC APIs from application code; SQL belongs in JPA read or command repositories.

No migration uses Hibernate schema generation. Flyway remains the only source of database structure.





