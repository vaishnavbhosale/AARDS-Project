# AARDS Frontend

React + Vite + TailwindCSS 3 + Axios + React Router + Chart.js.

## Setup

```bash
cd frontend
cp .env.example .env   # optional, defaults to http://localhost:8080/api/v1
npm install
npm run dev            # http://localhost:5173
npm run build          # production build in dist/
```

## Config

- API base URL: `VITE_API_BASE_URL` in `.env` (see `.env.example`).
- Token storage: `localStorage` key `aards_token`, user in `aards_user`.
- Backend must run at `http://localhost:8080` (or set `VITE_API_BASE_URL`).

## Login

- Default admin: `admin / admin123`.
- On 401 the app clears login and redirects to `/login`.

## Routes

| Path | Who | Page |
|---|---|---|
| `/login` | public | Login |
| `/dashboard` | all roles | Dashboard: filters + 10 stat cards + topper + 3 Chart.js charts + subject table |

## Dashboard

- Filter bar (session, department, year, semester) loads options from
  `GET /dashboard/filters`, auto-loads with the first available values,
  `Apply Filters` reloads and `Reset` restores defaults.
- Data comes from `GET /dashboard` via `src/services/dashboardService.js`.
- Shows 10 stat cards (Passed = green, Failed = red, rest = blue), a full-width
  topper card, Chart.js bar/doughnut/bar charts (subject pass vs fail %,
  backlog 0/1/2/3+, grades O–F), a subject table with colored pass-% bars,
  and a grayed-out "AI Recommendations" placeholder (coming in Half 2).
- Empty state when 0 students: prompts to upload a PDF or change filters.
| `/upload` | FACULTY, ADMIN | Upload result PDF + recent uploads |
| `/validation/:batchId` | FACULTY, ADMIN | Fix errors, approve batch |
| `/admin/users` | ADMIN | Manage users |
| `/admin/departments` | ADMIN | Manage departments |
| `/admin/subjects` | ADMIN | Manage subjects |

## Upload + validation flow

1. Faculty opens `/upload`, drops a PDF (max 20MB) and clicks Upload.
2. `VALIDATED` → records saved, go to dashboard. `PARSED` → review at
   `/validation/:batchId`: fix each row (Save), then Approve All & Finalize
   to save data and generate analytics.

## Structure

- `src/services/` — one file per backend area (auth, upload, validation, dashboard)
- `src/context/AuthContext.jsx` — user + token, `useAuth()` hook
- `src/components/` — ProtectedRoute, Sidebar, Navbar, Button, Input, Select, Modal, Card, Table, Alert, Loader, SkeletonCard
- `src/layouts/AppLayout.jsx` — sidebar + navbar shell
- `src/pages/` — Login, Dashboard (placeholder), NotFound, admin/*
