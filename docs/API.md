# TaxFlow — REST API Reference

Base URL: `http://localhost:8080/api`
Interactive docs: `http://localhost:8080/swagger-ui.html`

All endpoints (except `/auth/**`) require `Authorization: Bearer <accessToken>`.

Every response uses the envelope:

```json
{ "success": true, "message": "OK", "data": { }, "timestamp": "2026-07-18T10:00:00Z" }
```

Paginated endpoints accept `page`, `size`, `sort` (e.g. `sort=name,asc`) and return:

```json
{ "content": [], "page": 0, "size": 20, "totalElements": 0, "totalPages": 0, "first": true, "last": true }
```

---

## Auth — `/auth`

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{email, password, fullName, phone?, role?}` | Returns tokens; default role `BUSINESS_OWNER` |
| POST | `/auth/login` | `{email, password}` | Returns tokens |
| POST | `/auth/refresh` | `{refreshToken}` | Rotates the refresh token |
| POST | `/auth/logout` | `{refreshToken}` | Revokes the refresh token |
| POST | `/auth/forgot-password` | `{email}` | Returns `{resetToken}` (email delivery not configured) |
| POST | `/auth/reset-password` | `{token, newPassword}` | |
| POST | `/auth/verify-email?token=` | — | |

## Account — `/account`

| Method | Path | Notes |
|---|---|---|
| GET | `/account/me` | Current user profile |
| PUT | `/account/password` | `{currentPassword, newPassword}` |

## Businesses — `/businesses`

| Method | Path | Notes |
|---|---|---|
| GET | `/businesses` | Businesses the user owns or is a member of |
| POST | `/businesses` | Create (GSTIN optional, PAN validated) |
| PUT | `/businesses/{id}` | Owner/admin only |
| POST | `/businesses/{id}/members` | `{email, scopes[]}` — assign employee access |

## Customers — `/businesses/{businessId}/customers`

CRUD: `GET` (params `query`, paging) · `GET /search?query=` · `POST` · `PUT /{id}` · `DELETE /{id}`

## Products — `/businesses/{businessId}/products`

CRUD plus `GET /low-stock` and `POST /movements` (`{productId, type, quantity, unitCost, reason, referenceNumber?}`).

## Invoices — `/businesses/{businessId}/invoices`

| Method | Path | Notes |
|---|---|---|
| GET | `/` | Filters: `query`, `status`, `type`, `from`, `to` + paging |
| GET | `/next-number` | Suggested invoice number |
| GET | `/{id}` | Full invoice with lines |
| GET | `/{id}/pdf` | Tax-invoice PDF (binary) |
| POST | `/` | Create — totals & GST computed server-side, stock updated |
| PUT | `/{id}` | Update (stock re-synced) |
| POST | `/{id}/duplicate` | Copy as new draft |
| DELETE | `/{id}` | Stock changes reversed |

Invoice body:

```json
{
  "customerId": null,
  "invoiceNumber": null,
  "type": "GST_INVOICE",
  "status": "DRAFT",
  "invoiceDate": "2026-07-18",
  "dueDate": "2026-08-02",
  "recurring": false,
  "paidAmount": 0,
  "notes": "Thank you!",
  "lines": [
    { "productId": null, "description": "Basmati rice 5kg", "hsnCode": "1006", "quantity": 2, "unitPrice": 450, "gstRate": 5 }
  ]
}
```

## Expenses — `/businesses/{businessId}/expenses`

CRUD with filters `category`, `from`, `to`. Body: `{category, vendor, amount, gstAmount, expenseDate, description?}`.

## Documents — `/businesses/{businessId}/documents`

| Method | Path | Notes |
|---|---|---|
| GET | `/` | Paged list |
| POST | `/?documentType=RECEIPT` | multipart `file` — stored on local disk |
| GET | `/{id}` | Metadata |
| GET | `/{id}/download` | Binary download |
| GET | `/versions?documentType=` | Version history |
| DELETE | `/{id}` | Removes DB row and stored file |

## Tax — `/businesses/{businessId}/tax`

| Method | Path | Notes |
|---|---|---|
| GET | `/gst-summary?from=&to=` | Output GST, ITC, net payable, late fee, interest |
| POST | `/income-tax` | Slab-based estimate: `{businessIncome, salaryIncome, otherIncome, investments, deductions, businessExpenses}` |
| GET | `/recommendations?from=&to=` | Tax-saving recommendations |
| GET | `/compliance` | Blocking/warning issues |
| GET | `/health` | Financial health score + grade |
| GET | `/insights` | Narrative insights |
| GET | `/kpis` | Margin, turnover, AOV, growth |
| GET | `/filings` | Filing history (paged) |
| POST | `/filings/wizard` | `{filingType, periodStart, periodEnd, step, answers}` |
| POST | `/filings/{filingId}/submit` | Marks SUBMITTED |

## Dashboard — `GET /businesses/{businessId}/dashboard`

Metrics, charts, insights, notifications, KPIs and health score in one call.

## Reports — `/businesses/{businessId}/reports`

`GET /{type}/pdf` and `GET /{type}/excel` with optional `from`/`to`. Types: `sales`, `gst`, `expense`, `inventory`.

## Notifications — `/businesses/{businessId}/notifications`

`GET /` (paged) · `GET /unread-count` · `PUT /{id}/read`

## Search — `GET /businesses/{businessId}/search?q=`

Unified results across customers, products, invoices and documents.

## Audit — `GET /businesses/{businessId}/audit`

Paged, immutable audit trail (settings scope required).

## Backup

`GET /businesses/{businessId}/backup/export` — JSON export.
`POST /api/backup/import?businessId=` — admin-only import.

## Admin — `/admin` (role `ADMIN`)

`GET /users` · `GET /businesses` · `GET /analytics` · `GET|POST /rules`

---

## Error handling

| Status | Meaning |
|---|---|
| 400 | Validation failure (`data` holds field→message map) or business rule violation |
| 401 | Missing/expired token — clients should call `/auth/refresh` |
| 403 | Role/scope denied |
| 404 | Resource not found (or belongs to another business) |
| 429 | Rate limit exceeded |
| 500 | Unexpected error (logged server-side) |
