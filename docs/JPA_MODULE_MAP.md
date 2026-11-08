# JPA domain inventory and ERP module boundaries

This inventory is derived from the JPA domain package tree under `src/main/java/com/erplancers/erp`. It defines business ownership for the frontends; it does not split the Spring Boot deployment or database.

| ERP module | JPA entities / domain classes |
|---|---|
| CRM & Customer Management | Contact, Address, City, Country, Location, Customer, CustomerGroup, CustomerTransaction, CustomerTransactionDirection, Lead, Prospect, ProspectList, Opportunity, Call, Meeting, Note, Campaign, CampaignLog, Contract |
| Sales & Order Management | Sale, Order, OrderData, OrderOption, OrderStatus, Quote, Coupon, ProductDiscount, ProductSpecials |
| Procurement & Supplier Management | Supplier, Purchase, PurchaseDetail |
| Product & Inventory | Product, Manufacturer, ProductOption, ProductOptionValue, ProductQuote, ProductsFeatured, StockItem, StockCategory, StockStatus, ProductStockLocation, Store |
| Finance & Receivables | Currency, Invoice, InvoiceStatus, InvoiceType, PaymentTransaction, PaymentTransactionDirection |
| Financial Accounting | Account, AccountType, BalanceType, FiscalYear, FiscalPeriod, JournalEntry, JournalEntryLine, JournalEntryStatus, JournalEntryType, CostCenter, ProfitCenter |
| Tax Management | TaxClass, TaxRate, Zone |
| Project Management | Project, ProjectTask, Assignment, BusinessHour |
| Customer Support | Case, CaseEvent, CaseUpdate |
| Communication | Email, EmailAddress, EmailMarketing, EmailTask, EmailText, InboundMail, OutboundMail, Message |
| Document & Knowledge Management | Document, DocumentLink, DocumentRevision, Folder, Favorite, FolderSubscription, Help, HelpCategory, Knowledgebase, KnowledgebaseCategory |
| Events | Event, EventLocation |
| Surveys | Survey, SurveyQuestion, SurveyQuestionOption, SurveyQuestionResponse, SurveyResponse |
| Platform & Administration | ErpUser, ErpPolicy, Category, Alert, Announcement, Action, Condition |
| Business Intelligence | BiQueryAudit, BiReportExecution, BiSavedReport |
| Shared kernel | EntityBase and common repository/security infrastructure |

## Boundary rationale

- CRM owns party/contact identity and customer lifecycle: leads, prospects, opportunities, activities, campaigns and contracts. Customer/supplier are roles around shared party data, not duplicate masters.
- Sales owns quote-to-order, order status/options and pricing. Inventory owns stock and locations; Sales consumes availability.
- Procurement owns suppliers and purchase documents; Inventory owns resulting stock movements and balances.
- Finance owns invoices, currency and payment movements. Accounting owns ledger, fiscal periods, journal lines and cost/profit centers. Posting must remain a backend transaction, not a frontend-side sequence.
- Tax is a reusable capability consumed by Sales, Procurement and Finance.
- Communication, content, events and surveys are standalone cross-functional capabilities.
- BI owns saved analysis/report execution/audit metadata, not operational transactions.
- Platform administration owns users, policies, classifications, notifications and workflow configuration. Backend authorization is always authoritative.

## Independent React applications

Every application has its own HTML entry, Vite configuration, development port and deployable `dist` directory. They share API/auth source utilities and a metadata-driven workspace; they do not require a shared runtime shell.

| App | Port | Build command |
|---|---:|---|
| CRM | 5174 | npm run build:crm |
| Sales | 5175 | npm run build:sales |
| Inventory | 5176 | npm run build:inventory |
| Finance | 5177 | npm run build:finance |
| Accounting | 5178 | npm run build:accounting |
| Procurement | 5179 | npm run build:procurement |
| BI | 5180 | npm run build:bi |
| Projects | 5181 | npm run build:projects |
| Support | 5182 | npm run build:support |
| Communication | 5183 | npm run build:communication |
| Content | 5184 | npm run build:content |
| Events | 5185 | npm run build:events |
| Surveys | 5186 | npm run build:surveys |
| Tax | 5187 | npm run build:tax |
| Administration | 5188 | npm run build:admin |

The shared workspace provides login, module-scoped resource discovery, dashboard counts, metadata-driven create/edit forms, deletion confirmation, search and pagination using existing REST resource endpoints. Relationship-heavy fields and domain commands such as journal posting, invoice settlement and stock movement need dedicated screens/endpoints; generic CRUD deliberately does not emulate those business transactions.

Backend entities, controllers, routes and database schema are unchanged.

## Relationship and transaction observations from the JPA mappings

The aggregate boundaries are connected by real entity references, not just naming conventions:

- `Contact` owns a lazy one-to-many collection of `Address`; `Customer` references `Contact` and `CustomerGroup`, and exposes purchase, sale and customer-transaction collections.
- `Order` references `Invoice`, `Customer`, shipping `Contact` and `Currency`, and has coupon children. This is a Sales → CRM/Finance/Shared-data integration point.
- `Invoice` has billing/shipping contact collections and a currency reference. `PaymentTransaction` references invoice/contact/currency; this forms the Finance receivables lifecycle.
- `JournalEntryLine` references its journal header, account, currency, fiscal year/period and cost/profit centers. The ledger module is therefore the authoritative accounting aggregate.
- `StockItem` references Product, Supplier and Store. `Purchase` currently references Customer and owns PurchaseDetail rows; Supplier is a separate procurement entity. Confirm the intended purchasing party semantics before designing a supplier-facing purchase workflow.
- `Project` groups tasks/assignments and references other business resources; `Event` groups event locations and related collections; Survey groups questions and response records.

### JPA risks to resolve before domain-specific CRUD

Several owning-side `@ManyToOne` mappings currently declare `cascade = CascadeType.ALL`, including Customer → Contact/CustomerGroup, Order → Invoice/Customer/Contact/Currency, StockItem → Product/Supplier, and Purchase → Customer. Cascading REMOVE from a child toward shared master data can delete or mutate a shared Contact, Product, Supplier, Currency or Customer unexpectedly. Prefer narrowly scoped cascade rules (often none on many-to-one) and explicit aggregate ownership after reviewing existing persistence services and migrations.

Some operational associations are unidirectional and omit explicit `mappedBy`/join-column metadata. Do not assume that the Java collection is the owning side or that its database join shape is obvious. Inspect the generated schema and owning-side mappings before adding forms that manage both ends.

The current generic resource API is useful for straightforward scalar CRUD, but it is not a substitute for domain commands. In particular, invoice/order/payment transitions, stock receipts/issues/transfers and balanced journal posting must be implemented as backend transactions with authorization, validation and idempotency. The module frontends intentionally expose only generic resource operations until those commands are available.
