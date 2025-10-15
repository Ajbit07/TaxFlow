# TaxFlow

GST & Income Tax platform for Indian small businesses — invoicing, inventory, expenses, GST computation (GSTR-style summaries), income-tax estimation, PDF/Excel reports, notifications and a full audit trail.

Built with **Java 21 + Spring Boot 3** on the backend and **React 19 + TypeScript + Vite** on the frontend. No Docker, no cloud services, no external APIs — PostgreSQL and a local disk are the only dependencies.

---

## Tech stack

| Backend | Frontend |
|---|---|
| Java 21, Spring Boot 3.3 | React 19, TypeScript 5.7 |
| Spring Security (JWT + refresh tokens) | Vite 6, React Router 7 |
| Spring Data JPA / Hibernate | TanStack React Query 5 |
| PostgreSQL 15+, Flyway migrations | React Hook Form + Zod |
| Lombok, Bean Validation | Tailwind CSS (shadcn-style components) |
| OpenPDF (invoice/report PDFs) | Recharts (dashboard charts) |
| Apache POI (Excel reports) | Framer Motion, lucide-react |
| springdoc-openapi (Swagger UI) | Axios with auto token refresh |

---

## Quick start

### Prerequisites

- Java 21+
- Maven 3.9+ (or use an IDE)
- Node.js 20+
- PostgreSQL 15+

### 1. Database

```sql
CREATE DATABASE taxflow;
CREATE USER taxflow WITH PASSWORD 'taxflow';
GRANT ALL PRIVILEGES ON DATABASE taxflow TO taxflow;
```

### 2. Backend

```bash
cd backend
mvn spring-boot:run
```

- Starts on **http://localhost:8080**; Flyway creates the schema automatically.
- Swagger UI: **http://localhost:8080/swagger-ui.html**
- Configuration is environment-driven with local defaults — see [.env.example](.env.example).

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

- Starts on **http://localhost:5173** and proxies `/api` to the backend.
- Sign up → create your business (PAN required, GSTIN optional) → start invoicing.

---

## Features

- **Auth**: register, login, JWT access + rotating refresh tokens, forgot/reset password, email verification token, change password
- **Roles**: Business Owner, Employee (per-module scoped access), Admin
- **Customers**: CRUD, GSTIN validation, search, pagination, outstanding balance, lifetime sales
- **Products**: CRUD, HSN/SAC, GST rate, stock with low-stock alerts, margins, inventory movements
- **Invoices**: GST invoices, purchase bills, credit/debit notes; auto numbering (`TF-2026-00001`); drafts; duplicate; automatic taxable/GST/total computation; stock sync; professional PDF with CGST/SGST vs IGST split; UPI pay link
- **GST engine**: period summary (output GST, ITC, net payable, late fee, interest), compliance checks (duplicate/incomplete invoices, tax mismatches, missing documents, negative stock), 5-step filing wizard with history and submission
- **Expenses**: CRUD with categories and GST paid (feeds ITC)
- **Income tax**: slab-based estimator with 80C caps, advance-tax schedule, P&L, saving suggestions
- **Dashboard**: revenue trend, GST position, expense breakdown, KPIs, financial health score, insights
- **Reports**: sales / GST / expense / inventory as **PDF and Excel** downloads
- **Documents**: local file storage with versioning, download, content-type allowlist
- **Notifications**: unread badge, list, mark-read
- **Global search**: invoices, customers, products, documents
- **Audit log**: every mutation recorded (user, action, old/new value) and viewable in Settings
- **UI**: responsive, dark/light mode, sidebar + topbar + breadcrumbs, skeleton loaders, toasts, empty states, 404

---

## Project structure

```
TaxFlow/
├── backend/
│   └── src/main/java/com/taxflow/
│       ├── web/                  # 16 REST controllers
│       ├── application/
│       │   ├── service/          # 18 services (GST engine, ITR, PDF, Excel, …)
│       │   ├── dto/              # request/response records + validation
│       │   └── mapper/           # entity → DTO mappers
│       ├── domain/
│       │   ├── model/            # 16 JPA entities
│       │   └── enums/
│       ├── infrastructure/
│       │   ├── repository/       # Spring Data repositories
│       │   └── storage/          # local file storage
│       ├── security/             # JWT, filters, rate limiter, RBAC
│       └── common/               # ApiResponse, exceptions, sanitizer
│   └── src/main/resources/
│       ├── application.yml
│       └── db/migration/         # Flyway V1__init_schema.sql
├── frontend/
│   └── src/
│       ├── api/                  # axios client + typed endpoint modules
│       ├── components/           # UI primitives, layout, forms
│       ├── context/              # Auth, Business, Theme
│       ├── hooks/
│       ├── pages/                # 18 routed pages
│       └── types/                # API types mirroring backend DTOs
├── docs/
│   ├── API.md                    # REST API reference
│   └── BUILD_GUIDE.md            # detailed setup & deployment
└── ARCHITECTURE.md               # diagrams and design decisions
```

## Database design

16 normalized tables (see `backend/src/main/resources/db/migration/V1__init_schema.sql`): `users`, `businesses`, `business_members` (+`business_member_scopes`), `refresh_tokens`, `customers`, `products`, `inventory_movements`, `invoices`, `invoice_lines`, `expenses`, `documents`, `tax_filings`, `notifications`, `business_rules`, `audit_logs`. UUID primary keys, `NUMERIC(15,2)` money columns, FK indexes, and an ER structure where every business-owned table references `businesses(id)` with cascade rules.

## API documentation

Interactive docs at `/swagger-ui.html` when the backend is running; a written reference is in [docs/API.md](docs/API.md). All responses use a uniform envelope:

```json
{ "success": true, "message": "OK", "data": { }, "timestamp": "2026-07-18T10:00:00Z" }
```

## Testing

```bash
cd backend && mvn test        # JUnit 5 + Mockito unit tests
cd frontend && npm run build  # strict TypeScript compile + production build
```

## Screenshots

> _Add screenshots here: dashboard, invoice editor, GST filing wizard, dark mode._

## Future improvements

- Email delivery (SMTP) for verification and password-reset links
- E-invoicing (IRN/QR) and GSTN API integration for direct filing
- Recurring invoice scheduler and payment gateway integration
- Multi-currency support and CA (accountant) review workflows
- Attachment OCR for automatic expense capture
