# AARDS — Agentic AI Academic Result Analytics & Decision Support System

> SPPU-only web application that processes entire class result PDFs, extracts student data,
> generates academic analytics, and provides AI-based recommendations for Faculty, HODs and Principal.

## Local Setup

### Backend
1. Install PostgreSQL and create a database: `CREATE DATABASE aards;`
2. Copy `backend/src/main/resources/application.properties.example` to `application.properties`
3. Update the password (or set env var `DB_PASSWORD`)
4. Run: `cd backend && mvn spring-boot:run`

### Frontend
1. `cd frontend && npm install`
2. Create `.env` with `VITE_API_BASE_URL=http://localhost:8080/api/v1`
3. Run: `npm run dev`

### Default Admin
- Username: `admin`
- Password: `admin123`
- Change immediately after first login (feature coming soon)

## Workflow

Faculty Login → Upload PDF → Orchestrator → Check PDF type (Digital → PDFBox, else OCR)
→ Parser Agent → Validation Agent → Manual Validation Screen (if errors)
→ Analytics Agent → Business Rules → Gemini AI → Recommendations
→ Dashboard → Generate PDF Reports

**AI Agents (5 only):** Orchestrator, Parser, Validation, Analytics, Recommendation.
No chatbot, no LangGraph.

**Recommendation Flow:** Analytics → Business Rules → Gemini → Professional Recommendation.
Gemini only rewrites recommendations professionally; business decisions stay in-app.

## Tech Stack

- **Backend:** Java 21, Spring Boot, Spring Security + JWT, Spring Data JPA, PostgreSQL
- **Frontend:** React (Vite), TailwindCSS, Axios, React Router, Chart.js
- **AI:** Gemini API
- **PDF:** Apache PDFBox, Tesseract OCR
- **Deploy:** Frontend on Vercel, Backend + PostgreSQL on Railway

## Repository Structure

```
aards/
├── backend/       # Spring Boot (Maven, Java 21, package com.aards)
├── frontend/      # React + Vite + TailwindCSS
├── database/      # schema.sql
├── docs/
│   ├── SDD/
│   ├── API/
│   ├── ERD/
│   ├── PPT/
│   └── Images/
├── sample-pdfs/   # sample SPPU class result PDFs
├── README.md
└── .gitignore
```

## API Base

`http://localhost:8080/api/v1/` — See `docs/API/README.md` for endpoints:
`/auth`, `/uploads`, `/dashboard`, `/students`, `/subjects`,
`/analytics`, `/recommendations`, `/reports`, `/validation`, `/audit`.

## Quick Start

### Prerequisites

- Java 21, Maven 3.9+
- Node.js 20+ / npm
- PostgreSQL 15+
- Gemini API key, Tesseract installed (for OCR fallback)

### Backend

```bash
cd backend
# configure DB + secrets in src/main/resources/application.properties
mvn clean compile
mvn spring-boot:run
# serves on http://localhost:8080
```

Required config keys (`application.properties`):

```properties
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/aards}
spring.datasource.username=${DB_USERNAME:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
app.jwt.secret=${JWT_SECRET:change-me}
app.gemini.api-key=${GEMINI_API_KEY:change-me}
```

### Frontend

```bash
cd frontend
npm install
npm run dev      # http://localhost:5173
npm run build    # production build
```

Axios base URL: `http://localhost:8080/api/v1` (see `src/services/api.js`).

### Database

See `database/schema.sql` for placeholder schema.

## User Roles

- **ADMIN** — create Faculty, HOD, Principal
- **FACULTY** — upload PDF, review validation errors, view dashboard, generate reports
- **HOD** — view department analytics, generate department reports
- **PRINCIPAL** — view institute analytics, generate institute reports

## Dashboard (V1)

Filters: Academic Session → Year → Semester → Department.
Cards: Total Students, Passed, Failed, Overall Pass %, Average SGPA,
Highest SGPA, Lowest SGPA, 1 Backlog, 2 Backlogs, 3+ Backlogs, Topper.
Charts (Chart.js): Subject-wise Performance, Pass Percentage, Backlog Distribution, Grade Distribution.
Plus Subject Analysis + Recommendation Panel (Problem, Reason, Recommendation, Priority).

## Reports (PDF only)

Overall Institute Report, Department Report, Subject Report,
Second Year Report, Third Year Report, Fourth Year Report.

## Conventions

- DTO rule: Entities are NEVER returned directly. Entity → Mapper → DTO → Response.
- One `GlobalExceptionHandler` only. No try-catch spam in controllers.
- SLF4J only. Never `System.out.println()`.
- API response: `{ success, message, data }` / error: `{ success, message, errors, timestamp }`
- Commits: `feat:`, `fix:`, `refactor:`, `docs:`, `style:` (never "update"/"changes"/"new code")
- Workflow: Design → Implement → Test → Git Commit → Next Feature
- Golden rule: "Would a real college actually use this?" Yes → build, No → skip.

## V1 Scope / Out of Scope

**In V1:** Auth, Upload PDF, PDF Parsing, Validation, Dashboard, Analytics, AI Recommendations, PDF Reports, RBAC.
**Not in V1:** Chatbot, Student Login, Mobile App, Email Notifications, Attendance/Fee/Timetable modules,
Multi-University Support, Predictive Analytics, Result Versioning.
