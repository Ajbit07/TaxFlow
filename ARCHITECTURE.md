# TaxFlow — System Architecture

## Overview

TaxFlow is a two-tier SaaS application: a React 19 single-page app served by Vite, and a Spring Boot 3 REST API backed by PostgreSQL. Uploaded files are stored on the local filesystem. There are no external service dependencies — no message brokers, no cloud storage, no third-party APIs.

## Architecture Diagram

```
┌───────────────────────────────────────────────┐
│                   BROWSER                     │
│        React 19 + TypeScript + Vite           │
│                                               │
│  Pages ── React Query ── Axios (JWT + refresh)│
│  Tailwind / shadcn-style UI / Recharts        │
└──────────────────────┬────────────────────────┘
                       │ /api/** (JSON over HTTP)
                       ▼
┌───────────────────────────────────────────────┐
│           SPRING BOOT API  (:8080)            │
│                                               │
│  web/          REST controllers (16)          │
│    │  ApiResponse wrapper · Bean Validation   │
│  security/     JWT filter · rate limiter ·    │
│    │           RBAC (OWNER / EMPLOYEE / ADMIN)│
│  application/  services (18) · DTOs · mappers │
│    │  GST engine · ITR slabs · PDF (OpenPDF)  │
│    │  Excel (Apache POI) · compliance rules   │
│  domain/       entities (16) · enums          │
│  infrastructure/ JPA repositories · storage   │
└──────┬─────────────────────────┬──────────────┘
       │ JPA / Flyway            │ java.nio
       ▼                         ▼
┌───────────────┐        ┌───────────────┐
│  PostgreSQL   │        │  Local disk   │
│   (:5432)     │        │  ./storage    │
│               │        │               │
│ users         │        │ <business>/   │
│ businesses    │        │   pan/        │
│ customers     │        │   receipt/    │
│ products      │        │   invoice/    │
│ invoices      │        │   …           │
│ expenses      │        └───────────────┘
│ documents     │
│ tax_filings   │
│ notifications │
│ audit_logs    │
│ …16 tables    │
└───────────────┘
```

## Layered design (backend)

| Layer | Package | Responsibility |
|---|---|---|
| Web | `com.taxflow.web` | REST controllers, request validation, response wrapping, file downloads |
| Application | `com.taxflow.application` | Business logic, DTOs, entity→DTO mappers, tax engines, reports |
| Domain | `com.taxflow.domain` | JPA entities and enums — no framework logic beyond mapping |
| Infrastructure | `com.taxflow.infrastructure` | Spring Data repositories, local file storage |
| Security | `com.taxflow.security` | JWT issue/parse, auth filter, sliding-window rate limiter, RBAC |
| Common | `com.taxflow.common` | `ApiResponse`, `PageResponse`, exceptions, input sanitizer |

## Data flows

1. **Invoice creation**: Controller → `InvoiceService` validates + computes line taxes (taxable, GST, totals with `BigDecimal HALF_UP`) → stock is decremented per line → audit log written → response DTO.
2. **GST summary**: `TaxEngineService` sums output GST (sales + debit notes) and input GST (purchase bills + expense GST) for the period, then derives net payable, late fee (₹50/day past due) and 18% p.a. interest.
3. **Invoice PDF**: `InvoicePdfService` renders a tax invoice with OpenPDF — CGST/SGST split for intra-state supplies, IGST for inter-state (decided by GSTIN state-code prefix).
4. **Document upload**: multipart file → `FileStorageService` writes under `storage/<businessId>/<type>/` with a random prefix (path-traversal safe, content-type allowlist) → metadata row in `documents`.
5. **Filing wizard**: each saved step recomputes GST + compliance; blocking errors set status `BLOCKED`; step 5 marks `READY_FOR_REVIEW`; submission stamps `SUBMITTED`.

## Security layers

1. Stateless JWT auth (HS256, 30-min access tokens, hashed rotating refresh tokens with 14-day expiry)
2. BCrypt (strength 12) password hashing
3. Role-based access: `BUSINESS_OWNER`, `EMPLOYEE` (scope-restricted via `business_member_scopes`), `ADMIN`
4. Per-business data isolation — every query is keyed by business id and ownership/membership is verified per request
5. In-memory sliding-window rate limiting per IP+path
6. Bean Validation on every request DTO + input sanitizer stripping HTML metacharacters
7. Immutable audit trail on every mutation (user, time, action, old/new value)
8. CORS locked to configured origins; CSRF disabled only for the stateless API
