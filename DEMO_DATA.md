# Demo data

The ERP can generate disposable sample records for every mapped JPA entity using JavaFaker.

## Enable

Demo generation is **disabled by default**. Enable it only for a local/demo database:

```properties
erp.demo-data.enabled=true
erp.demo-data.records-per-entity=3
```

Or pass the same settings as environment variables:

```bash
ERP_DEMO_DATA_ENABLED=true
ERP_DEMO_DATA_RECORDS_PER_ENTITY=3
```

The seeder:
- discovers mapped entities from the JPA metamodel (no manually maintained entity list);
- generates values based on field names and Java types;
- links single-valued relationships to existing or previously generated rows;
- leaves collection relationships empty to avoid unintended cascade graphs;
- skips entity tables that already contain records, so reruns do not duplicate populated tables;
- hashes generated demo-user passwords with BCrypt.

Generated demo users use the password `Demo123!`; their usernames start with `demo-`. Do not enable demo generation in production or expose generated demo accounts to untrusted users.

The generator is intended for an empty, disposable demo database. It is not a production migration or a replacement for domain-specific business fixtures.
