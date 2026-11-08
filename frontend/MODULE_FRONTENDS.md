# Independent React module frontends

The ERP backend and its REST contract remain unchanged. These frontends are separate Vite applications that reuse the existing frontend API/auth client and call the same `/api` endpoints.

## Applications

| Module | Development port | Build command | Output |
|---|---:|---|---|
| CRM | 5174 | `npm run build:crm` | `apps/crm/dist` |
| Sales | 5175 | `npm run build:sales` | `apps/sales/dist` |
| Inventory & Warehouse | 5176 | `npm run build:inventory` | `apps/inventory/dist` |
| Finance | 5177 | `npm run build:finance` | `apps/finance/dist` |
| Accounting | 5178 | `npm run build:accounting` | `apps/accounting/dist` |
| Procurement | 5179 | `npm run build:procurement` | `apps/procurement/dist` |
| BI resource explorer | 5180 | `npm run build:bi` | `apps/bi/dist` |

Run from `frontend/` after `npm install`, while the Spring Boot API is available at `http://localhost:8080`.

Example: `npm run dev -- --config apps/crm/vite.config.ts` starts the CRM UI. The other module configs work the same way. Build all independent apps with `npm run build:modules`.

Each application has its own HTML entry, Vite config, dev port and deployable `dist` directory. Authentication and API access are shared source utilities, not duplicated backend logic. This is a frontend-only separation: no Java controller, entity, service, route or database contract is changed by this architecture.

## Current scope

The initial independent shells provide module-scoped resource discovery, live dashboard counters and a read-only paginated resource explorer. Existing full ERP workspace remains available as before. Module-specific CRUD screens, navigation, permissions and BI workflows can be expanded app-by-app against the already exposed API.
