# TriVoKo hosting values (Phase 12 step 3) - NO passwords in this file

Secrets (database password, DEMO_PASSWORD, Cloudinary secret) live only in the hosting dashboards and in `.env`.

## Aiven - MySQL (created 2 Nov 2026)
| What | Value |
|---|---|
| Service | `trivoko-mysql` - MySQL 8.4, **Free** tier (Free-1-1gb: 1 CPU, 1 GB RAM, 1 GB disk) |
| Region | DigitalOcean `blr` (Bengaluru) |
| Host | `trivoko-mysql-madhavansuresh000-749c.b.aivencloud.com` |
| Port | `10088` |
| Database | `defaultdb` |
| User | `avnadmin` |
| Password | in Aiven -> trivoko-mysql -> Overview -> Password (eye icon). Never in GitHub or chat. |
| For Render `DB_URL` | `jdbc:mysql://trivoko-mysql-madhavansuresh000-749c.b.aivencloud.com:10088/defaultdb?sslMode=REQUIRED` |

Free tier note: Aiven powers the service off after a long time without use (email first). The uptime check (step 5)
keeps it in use; if it is off, Aiven -> trivoko-mysql -> Power on.

Also in the Aiven project: `pg-228c49d2` (PostgreSQL, Developer tier, created by Aiven's sign-up). Not used by
TriVoKo; it runs on the 30-day trial credits - **delete it before 2 Dec 2026** (Service settings -> Delete service).

## Render - backend (next)
| What | Value |
|---|---|
| Service | `trivoko-api` (Blueprint `render.yaml`), Docker, free, Singapore |
| Address | (filled in after creation) |

## Vercel - frontend (next)
| What | Value |
|---|---|
| Project | root directory `frontend` |
| Address | (filled in after creation) |
