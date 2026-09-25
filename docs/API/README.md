# AARDS API (V1 Skeleton)

Base: `http://localhost:8080/api/v1/`

| Method | Path | Role | Description |
|---|---|---|---|
| POST | `/auth/login` | public | Login, returns `{ token, user }` |
| POST | `/uploads` | FACULTY | Upload class result PDF (multipart `file`) |
| GET | `/validation/{uploadId}` | FACULTY | List validation errors: Student, Field, Extracted Value, Correct Value |
| POST | `/validation/{uploadId}/approve` | FACULTY | Approve corrections, trigger analytics |
| GET | `/dashboard?session=&year=&semester=&department=` | FACULTY,HOD,PRINCIPAL | Cards, charts, subject analysis |
| GET | `/students` | FACULTY,HOD | List parsed students |
| GET | `/subjects` | FACULTY,HOD | List subjects |
| GET | `/analytics?uploadId=` | FACULTY,HOD | Analytics Agent output |
| GET | `/recommendations?uploadId=` | FACULTY,HOD,PRINCIPAL | Problem, Reason, Recommendation, Priority |
| GET | `/reports?type=institute&uploadId=` | FACULTY,HOD,PRINCIPAL | Generate PDF report (institute, department, subject, SE, TE, BE) |
| GET | `/audit` | ADMIN | Audit logs |
| GET | `/users` | ADMIN | List users |

Response format:
- Success: `{ "success": true, "message": "...", "data": {} }`
- Error: `{ "success": false, "message": "...", "errors": [], "timestamp": "" }`
