# Embedded analytics deployment configuration

The backend mounts this directory read-only at `/run/config`.

Before enabling the embedded Spark engine:

1. Copy `analytics-datasets.properties.example` to `analytics-datasets.properties`.
2. Replace every `YOUR_...` placeholder with a registered JPA entity name and valid JPA attribute paths from the deployed ERP model.
3. The embedded engine reads analytical data through the ERP `EntityManager`; the catalogue contains JPA entity/attribute names, never SQL table names. Never let API requests choose entities or attributes.
4. Verify the configured scope relation against active tenant and row-scope grants. Use `tenant-column=none` only if row-scope grants provide the required tenant isolation.
5. Set `ANALYTICS_ENGINE_ENABLED=true` in `.env`, then recreate the backend.

```sh
cp deploy/analytics-datasets.properties.example deploy/analytics-datasets.properties
# Edit deploy/analytics-datasets.properties before enabling the engine.
docker compose up -d --build
docker compose logs -f backend
```

Do not commit `analytics-datasets.properties` if it contains deployment-specific schema details. JPA extraction is paged and capped at one million rows per job; use approved aggregate/projection entities or bounded date windows for larger datasets. The engine stays disabled until mappings and grants are configured.
