# Deployment Checklist

## Before Deploying

### 1. Update URLs in Configuration Files
- [ ] Update `netlify.toml` - Replace `your-app-name` with your actual Heroku app name
- [ ] Update `.env.production` - Replace `your-app-name` with your actual Heroku app name

### 2. Heroku Backend Setup

#### Already Completed ✅
- [x] Java buildpack configured
- [x] PostgreSQL addon added
- [x] Config vars set:
  - [x] DATABASE_URL (auto-set by Postgres)
  - [x] JWT_SECRET
  - [x] SPRING_PROFILES_ACTIVE
  - [x] STRIPE_SECRET_KEY
  - [x] STRIPE_WEBHOOK_SECRET

#### Still Need to Do
- [ ] Set `ALLOWED_ORIGINS` to your Netlify URL (e.g., `https://your-site.netlify.app`)
- [ ] Set `FRONTEND_URL` to your Netlify URL
- [ ] Add email configuration (optional):
  ```bash
  heroku config:set MAIL_HOST=smtp.gmail.com -a your-app-name
  heroku config:set MAIL_PORT=587 -a your-app-name
  heroku config:set MAIL_USERNAME=your-email@gmail.com -a your-app-name
  heroku config:set MAIL_PASSWORD=your-app-password -a your-app-name
  heroku config:set MAIL_FROM=noreply@yoursite.com -a your-app-name
  ```

### 3. Deploy Backend to Heroku
```bash
# Make sure you're in the project root
cd c:\Users\hurme\Documents\william-normann-automotive

# Add Heroku remote (if not added)
heroku git:remote -a your-app-name

# Deploy backend subdirectory
git subtree push --prefix backend heroku main

# Or force push if needed
git push heroku `git subtree split --prefix backend main`:main --force
```

### 4. Run Database Migrations on Heroku
```bash
# Connect to Heroku Postgres
heroku pg:psql -a your-app-name

# Then paste the content of each migration file:
# - database_migration_phase1.sql
# - database_migration_cart_reservations.sql
# - database_migration_stripe_checkout.sql
# - database_migration_analytics_alerts.sql
# - database_migration_analytics_dashboards.sql
# - database_migration_analytics_event.sql
# - database_migration_alert_history.sql
# - database_migration_info_sections_expansion.sql
```

### 5. Netlify Frontend Setup

#### Option A: Connect to GitHub (Recommended)
1. [ ] Push your code to GitHub
2. [ ] Go to [Netlify](https://app.netlify.com)
3. [ ] Click "Add new site" → "Import an existing project"
4. [ ] Select your GitHub repository
5. [ ] Build settings:
   - Base directory: (leave empty)
   - Build command: `npm run build`
   - Publish directory: `.next`
6. [ ] Add environment variable:
   - `NEXT_PUBLIC_API_URL` = `https://your-app-name.herokuapp.com`
7. [ ] Click "Deploy site"
8. [ ] Note your Netlify URL (e.g., `https://random-name-123.netlify.app`)

#### Option B: Manual Deployment
```bash
# Install Netlify CLI
npm install -g netlify-cli

# Login
netlify login

# Deploy
npm run build
netlify deploy --prod
```

### 6. Update CORS After Getting Netlify URL
Once you have your Netlify URL:
```bash
heroku config:set ALLOWED_ORIGINS=https://your-actual-netlify-url.netlify.app -a your-app-name
heroku config:set FRONTEND_URL=https://your-actual-netlify-url.netlify.app -a your-app-name
```

### 7. Test Everything
- [ ] Backend health check: `https://your-app-name.herokuapp.com/api/health`
- [ ] Frontend loads: Visit your Netlify URL
- [ ] User registration works
- [ ] User login works
- [ ] Product listing loads
- [ ] Cart functionality works
- [ ] Checkout flow works

## Common Issues & Solutions

### CORS Errors
- Make sure `ALLOWED_ORIGINS` exactly matches your Netlify URL (including `https://`)
- No trailing slash in the URL

### Backend Won't Start
- Check Heroku logs: `heroku logs --tail -a your-app-name`
- Verify all required config vars are set
- Check Java version in `system.properties` matches `pom.xml`

### Frontend Can't Connect to Backend
- Verify `NEXT_PUBLIC_API_URL` is set in Netlify environment variables
- Check browser console for actual error
- Test backend URL directly in browser

### Database Connection Issues
- Verify Postgres addon is attached
- Check `DATABASE_URL` is set: `heroku config -a your-app-name`

## Useful Commands

### Heroku
```bash
# View logs
heroku logs --tail -a your-app-name

# View config
heroku config -a your-app-name

# Restart app
heroku restart -a your-app-name

# Access database
heroku pg:psql -a your-app-name
```

### Netlify
```bash
# View deploy status
netlify status

# View logs
netlify logs

# Redeploy
netlify deploy --prod
```
