# Independent ERP module frontends

See [JPA module map](../docs/JPA_MODULE_MAP.md) for entity ownership, app ports and build commands. Each app is a separate Vite/React build with its own entry point and deployable `dist` output. Start Spring Boot on port 8080, run `npm install` in `frontend/`, then use `npm run build:<module>` or `npm run build:modules`.

The shared workspace uses the existing authenticated REST resource API and JPA catalog metadata for search, pagination and generic create/edit/delete. Backend authorization and validation remain authoritative. Domain-specific transactions such as journal posting, invoice settlement and stock movements require dedicated business commands and are not simulated through generic CRUD.
