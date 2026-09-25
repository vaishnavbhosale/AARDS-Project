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
| `/dashboard` | all roles | Dashboard (placeholder, real charts in Step 6) |
| `/upload` | FACULTY, ADMIN | Coming in Step 5 |
| `/validation/:batchId` | FACULTY, ADMIN | Coming in Step 5 |
| `/admin/users` | ADMIN | Manage users |
| `/admin/departments` | ADMIN | Manage departments |
| `/admin/subjects` | ADMIN | Manage subjects |

## Structure

- `src/services/` — one file per backend area (auth, upload, validation, dashboard)
- `src/context/AuthContext.jsx` — user + token, `useAuth()` hook
- `src/components/` — ProtectedRoute, Sidebar, Navbar, Button, Input, Select, Modal, Card, Table, Alert, Loader, SkeletonCard
- `src/layouts/AppLayout.jsx` — sidebar + navbar shell
- `src/pages/` — Login, Dashboard (placeholder), NotFound, admin/*
