# TaxFlow — Build & Run Guide

No Docker, no cloud services. You need: **Java 21+, Maven 3.9+, Node.js 20+, PostgreSQL 15+**.

---

## 1. Database setup

```sql
CREATE DATABASE taxflow;
CREATE USER taxflow WITH PASSWORD 'taxflow';
GRANT ALL PRIVILEGES ON DATABASE taxflow TO taxflow;
-- PostgreSQL 15+: also grant schema rights
\c taxflow
GRANT ALL ON SCHEMA public TO taxflow;
```

Defaults match `application.yml` (`jdbc:postgresql://localhost:5432/taxflow`, user/password `taxflow`). Override via environment variables — see [.env.example](../.env.example).

## 2. Backend

```bash
cd backend
mvn spring-boot:run          # dev
mvn clean package            # build jar → target/taxflow-backend-1.0.0.jar
mvn test                     # unit tests
```

- API: http://localhost:8080/api
- Health: http://localhost:8080/actuator/health
- Swagger: http://localhost:8080/swagger-ui.html
- Flyway runs `db/migration/V1__init_schema.sql` on first start.
- Uploaded files land in `./storage` (configurable with `STORAGE_PATH`).

## 3. Frontend

```bash
cd frontend
npm install
npm run dev        # http://localhost:5173 (proxies /api → :8080)
npm run build      # strict tsc + production bundle → dist/
npm run preview    # serve the production build locally
```

## 4. First run

1. Open http://localhost:5173 and **sign up**.
2. Complete **business onboarding** (PAN required, GSTIN optional but enables GST splits).
3. Add **products** (HSN + GST rate) and **customers**.
4. Create an **invoice** — totals, GST and stock update automatically; download the PDF.
5. Record **expenses** with GST paid to build input tax credit.
6. Visit **GST Filing** for the period summary, compliance checks and the filing wizard.
7. Use **Reports** for PDF/Excel exports and **Settings → Audit log** for the trail.

## 5. Production deployment (single VM)

```bash
# Backend
export DB_URL=jdbc:postgresql://<host>:5432/taxflow
export DB_USERNAME=taxflow
export DB_PASSWORD=<strong-password>
export JWT_SECRET=<64+ random chars>
export CORS_ALLOWED_ORIGINS=https://your-domain.example
export STORAGE_PATH=/var/lib/taxflow/storage
java -jar backend/target/taxflow-backend-1.0.0.jar
```

Serve `frontend/dist/` with any static web server (nginx, Caddy, Apache) and reverse-proxy `/api` to port 8080. Terminate TLS at the web server.

Production checklist:

- [ ] Strong `JWT_SECRET` (64+ random characters)
- [ ] Unique DB password; restrict PostgreSQL to localhost/private network
- [ ] `CORS_ALLOWED_ORIGINS` set to your exact domain
- [ ] HTTPS enabled at the reverse proxy
- [ ] Nightly `pg_dump` backups + copy of the `storage/` directory
- [ ] Log rotation for the backend process

## 6. Troubleshooting

| Symptom | Fix |
|---|---|
| `Flyway validate failed` | Schema drifted — drop and recreate the dev database |
| 401 loops in the UI | Clear browser localStorage (stale tokens) |
| CORS errors | Add the frontend origin to `CORS_ALLOWED_ORIGINS` |
| Upload rejected | Only PDF, PNG, JPEG, WebP, CSV, XLS(X) are allowed |
| Port already in use | Set `SERVER_PORT` / change Vite port in `vite.config.ts` |
