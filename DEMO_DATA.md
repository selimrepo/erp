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


## Connected business scenarios

After generic entity discovery, `DemoBusinessScenarioSeeder` connects the generated rows into demonstrable business flows:

- Customer → order → sales invoice → invoice-linked payment transaction.
- Product → supplier → stock item → warehouse store, plus product stock-location quantities and physical locations.
- Purchase → purchase detail → supplier/product.
- Journal entry → fiscal year/period → two posted journal lines with equal debit and credit amounts.

The workflow seeder is idempotent for journal lines: it does not add another pair when a journal already has lines. It reuses demo records and updates their links and amounts on startup. The stock item now has an optional JPA reference to its warehouse store, and payment transactions can reference their invoice and contact.

The demo runner needs representative chart-of-account, fiscal-year and fiscal-period records to create journal lines. If those fixtures are unavailable, it logs a warning and leaves journal lines uncreated. Always use a disposable database and inspect the generated data before presenting it as accounting-grade sample data.
