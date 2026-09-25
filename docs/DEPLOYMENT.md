# AARDS Deployment

## Backend — Railway
1. Push code to GitHub
2. Go to railway.app → New Project → Deploy from GitHub
3. Select the AARDS-Project repo
4. Set Root Directory to `backend`
5. Add PostgreSQL plugin from Railway → copy DATABASE_URL
6. Set environment variables:
   - SPRING_PROFILES_ACTIVE = prod (loads `application-prod.properties`)
   - DB_URL = Railway PostgreSQL URL
   - DB_USERNAME = postgres
   - DB_PASSWORD = (from Railway)
   - JWT_SECRET = (any long random string)
   - CORS_ORIGINS = https://your-frontend.vercel.app
   - UPLOAD_DIR = /app/uploads
7. Add a Volume mounted at /app/uploads (for PDF storage)
8. Deploy → copy the public URL

## Frontend — Vercel
1. Go to vercel.com → Import Project
2. Select the AARDS-Project repo
3. Set Root Directory to `frontend`
4. Framework preset: Vite
5. Environment variable:
   - VITE_API_BASE_URL = https://your-railway-url.up.railway.app/api/v1
6. Deploy → copy the public URL
7. Go back to Railway → update CORS_ORIGINS to include the Vercel URL
8. Redeploy backend

## Test Live
- Open Vercel URL
- Login admin / admin123
- Upload PDF → dashboard → download report
- Everything should work identically to local
