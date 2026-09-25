# AARDS API (V1)

Base: `http://localhost:8080/api/v1/`

## Auth

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/auth/login` | public | Login with username+password, returns `{ token, username, fullName, role, departmentId }` |
| POST | `/auth/register` | public for first admin, else ADMIN | Create user. First call with role ADMIN bootstraps admin. Body: `{ username, password, fullName, email, role, departmentId }` |
| GET | `/auth/me` | logged in | Returns current user |

Default seed admin: `admin / admin123` (change on first login).

## Admin (ADMIN only)

| Method | Path | Description |
|---|---|---|
| GET | `/admin/users` | List all users |
| GET | `/admin/users/{id}` | Get one user |
| PUT | `/admin/users/{id}/activate` | Set active=true |
| PUT | `/admin/users/{id}/deactivate` | Set active=false |

## Departments

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/departments` | ADMIN | Create. Body: `{ name, code }` |
| GET | `/departments` | logged in | List all |
| GET | `/departments/{id}` | logged in | Get one |

## Subjects

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/subjects` | ADMIN | Create. Body: `{ code, name, departmentId, year, semester, credits, maxMarks, passingMarks }` |
| GET | `/subjects` | logged in | List all |
| GET | `/subjects?departmentId=&year=&semester=` | logged in | Filtered list |

## Uploads / Results

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/uploads` | FACULTY | Upload class result PDF (multipart `file`) |
| GET | `/uploads` | logged in | List batches for current user |
| GET | `/uploads/{id}` | logged in | Batch details |
| GET | `/validation/{uploadId}` | FACULTY | List validation errors |
| GET | `/dashboard?session=&year=&semester=&department=` | FACULTY,HOD,PRINCIPAL | Cards, charts, subject analysis |
| GET | `/students` | FACULTY,HOD | List parsed students |
| GET | `/analytics?uploadId=` | FACULTY,HOD | Analytics output |
| GET | `/recommendations?uploadId=` | FACULTY,HOD,PRINCIPAL | Problem, Reason, Recommendation, Priority |
| GET | `/reports?type=institute&uploadId=` | FACULTY,HOD,PRINCIPAL | Generate PDF report |
| GET | `/audit` | ADMIN | Audit logs |

Response format:
- Success: `{ "success": true, "message": "...", "data": {} }`
- Error: `{ "success": false, "message": "...", "errors": [], "timestamp": "" }`

Auth errors:
- No token → 401, wrong role → 403, bad login → 401 "Invalid username or password".
