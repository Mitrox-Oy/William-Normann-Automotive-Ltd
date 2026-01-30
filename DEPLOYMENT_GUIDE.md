# Deployment Guide

## Backend Deployment (Heroku)

### Prerequisites
- Heroku CLI installed
- Git repository initialized
- Java buildpack configured ✅
- PostgreSQL addon added ✅

### Configuration

#### 1. Heroku Config Vars (Already Set)
```
ALLOWED_ORIGINS=https://your-netlify-site.netlify.app
DATABASE_URL=<auto-set by Postgres addon>
FRONTEND_URL=https://your-netlify-site.netlify.app
JWT_SECRET=<your-secure-secret>
SPRING_PROFILES_ACTIVE=prod
STRIPE_SECRET_KEY=<your-stripe-secret>
STRIPE_WEBHOOK_SECRET=<your-stripe-webhook-secret>
```

#### 2. Additional Config Vars to Add
```bash
heroku config:set MAIL_HOST=smtp.gmail.com -a your-app-name
heroku config:set MAIL_PORT=587 -a your-app-name
heroku config:set MAIL_USERNAME=your-email@gmail.com -a your-app-name
heroku config:set MAIL_PASSWORD=your-app-password -a your-app-name
heroku config:set MAIL_FROM=noreply@yoursite.com -a your-app-name
```

### Deployment Steps

#### Option 1: Deploy from subdirectory (Recommended)
```bash
# Add Heroku remote if not already added
heroku git:remote -a your-app-name

# Deploy only the backend folder
git subtree push --prefix backend heroku main
```

#### Option 2: Using Heroku CLI
```bash
# Login to Heroku
heroku login

# Create app (if not created)
heroku create your-app-name

# Add PostgreSQL (already done)
# heroku addons:create heroku-postgresql:mini

# Set buildpack to Java (already done)
# heroku buildpacks:set heroku/java

# Deploy
git subtree push --prefix backend heroku main
```

### Verify Deployment
```bash
# Check logs
heroku logs --tail -a your-app-name

# Open app
heroku open -a your-app-name
```

### Database Migrations
Run your SQL migration files manually via Heroku Postgres:
```bash
heroku pg:psql -a your-app-name < database_migration_phase1.sql
```

---

## Frontend Deployment (Netlify)

### Prerequisites
- Netlify account
- GitHub repository (or manual deployment)

### Configuration

#### 1. Create/Update `.env.production`
Update the backend URL to your actual Heroku app name:
```
NEXT_PUBLIC_API_URL=https://your-app-name.herokuapp.com
NEXT_PUBLIC_BASE_PATH=
```

#### 2. Update `netlify.toml`
Update the redirect URL to your Heroku backend:
```toml
[[redirects]]
  from = "/api/*"
  to = "https://your-app-name.herokuapp.com/api/:splat"
  status = 200
  force = true
```

### Deployment Steps

#### Option 1: Deploy from GitHub (Recommended)
1. Go to [Netlify Dashboard](https://app.netlify.com)
2. Click "Add new site" → "Import an existing project"
3. Connect to GitHub and select your repository
4. Configure build settings:
   - **Base directory:** Leave empty (root)
   - **Build command:** `npm run build`
   - **Publish directory:** `.next`
5. Add environment variables:
   - `NEXT_PUBLIC_API_URL` = `https://your-app-name.herokuapp.com`
6. Click "Deploy site"

#### Option 2: Manual Deployment via Netlify CLI
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Initialize (first time only)
netlify init

# Build and deploy
npm run build
netlify deploy --prod
```

### Environment Variables in Netlify
Add these in Netlify Dashboard → Site settings → Environment variables:
```
NEXT_PUBLIC_API_URL=https://your-app-name.herokuapp.com
NEXT_PUBLIC_BASE_PATH=
```

---

## Update Backend CORS Configuration

After deploying to Netlify, update your Heroku config:
```bash
heroku config:set ALLOWED_ORIGINS=https://your-netlify-site.netlify.app -a your-app-name
heroku config:set FRONTEND_URL=https://your-netlify-site.netlify.app -a your-app-name
```

---

## Testing the Deployment

1. **Test Backend:**
   ```bash
   curl https://your-app-name.herokuapp.com/api/health
   ```

2. **Test Frontend:**
   - Visit your Netlify site
   - Check browser console for API connection errors
   - Test user registration/login flow

---

## Troubleshooting

### Backend Issues
- **Check logs:** `heroku logs --tail -a your-app-name`
- **Restart dyno:** `heroku restart -a your-app-name`
- **Check config:** `heroku config -a your-app-name`

### Frontend Issues
- **Check Netlify deploy logs** in the Netlify dashboard
- **Verify environment variables** are set correctly
- **Check browser console** for CORS or API errors

### CORS Issues
Make sure `ALLOWED_ORIGINS` in Heroku matches your Netlify URL exactly (including https://)

---

## Project Structure (No Changes Needed!)

Your current structure is perfect:
```
/                    → Frontend (Next.js) - Deploy to Netlify
/backend             → Backend (Spring Boot) - Deploy to Heroku
```

**You do NOT need to move frontend to a separate folder!**
- Netlify deploys from root
- Heroku deploys from `/backend` subdirectory using git subtree

---

## Quick Reference

### Redeploy Backend
```bash
git add .
git commit -m "Update backend"
git push origin main
git subtree push --prefix backend heroku main
```

### Redeploy Frontend
If connected to GitHub, just push:
```bash
git push origin main
```
Netlify will auto-deploy!

Or manually:
```bash
npm run build
netlify deploy --prod
```
