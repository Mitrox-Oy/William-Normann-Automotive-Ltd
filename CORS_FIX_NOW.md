# CORS FIX - COMPLETE THIS NOW

## ✅ What I Fixed

1. **Set Heroku CORS config** - DONE ✓
   - ALLOWED_ORIGINS = https://william-normann.netlify.app
   - FRONTEND_URL = https://william-normann.netlify.app

2. **Fixed trailing slash in .env.production** - DONE ✓
   - Changed from: `https://william-normann-330b912b355b.herokuapp.com/`
   - Changed to: `https://william-normann-330b912b355b.herokuapp.com` (no trailing slash)

## ⚠️ URGENT: Update Netlify Environment Variable

The `.env.production` file is NOT deployed to Netlify (it's gitignored). You MUST update the environment variable in Netlify directly:

### Option 1: Via Netlify Dashboard (RECOMMENDED)

1. Go to https://app.netlify.com
2. Select your **william-normann** site
3. Go to **Site configuration** → **Environment variables**
4. Find or add: `NEXT_PUBLIC_API_URL`
5. Set value to: `https://william-normann-330b912b355b.herokuapp.com`
   **⚠️ NO TRAILING SLASH!**
6. Click **Save**
7. Go to **Deploys** tab
8. Click **Trigger deploy** → **Clear cache and deploy site**

### Option 2: Via Netlify CLI

```powershell
netlify env:set NEXT_PUBLIC_API_URL "https://william-normann-330b912b355b.herokuapp.com"
netlify deploy --prod --build
```

## Why This Matters

The double slash (`//api/auth/login`) was causing CORS preflight failures because:
- Your API URL ended with `/`
- Frontend code adds `/api/...`
- Result: `https://...herokuapp.com//api/...` ← double slash breaks CORS

## Test After Updating

Once you've updated Netlify:

1. **Clear browser cache** (Ctrl+Shift+Delete)
2. **Hard refresh** (Ctrl+F5)
3. Try logging in with:
   - Email: `owner@example.com`
   - Password: `password`

4. Check browser console - you should see:
   - ✅ No CORS errors
   - ✅ Successful API calls to backend

## If Still Not Working

### Check Backend Logs
```powershell
heroku logs --tail -a william-normann
```

Look for:
- Incoming requests from your Netlify domain
- CORS configuration being applied
- Any 403 errors

### Restart Backend
```powershell
heroku restart -a william-normann
```

### Verify Config
```powershell
heroku config -a william-normann
```

Should show:
```
ALLOWED_ORIGINS: https://william-normann.netlify.app
FRONTEND_URL: https://william-normann.netlify.app
```

## Common Issues

### If you still see CORS errors:

1. **Make sure Netlify environment variable is updated**
   - NO trailing slash in the URL
   - Exact URL: `https://william-normann-330b912b355b.herokuapp.com`

2. **Trigger a new deploy in Netlify**
   - Clear cache and redeploy
   - Environment changes require a new build

3. **Clear browser cache completely**
   - Old cached API URLs might still be used

### If login fails with 401/403:

This might mean the backend database doesn't have users yet. Run:

```powershell
# Connect to Heroku database
heroku pg:psql -a william-normann

# Check if users exist
SELECT email, role FROM users;

# If empty, you need to run database migrations
# Exit psql first (\q), then:
```

Copy the content of `database_migration_phase1.sql` and paste it into the psql session.

---

## Summary

**RIGHT NOW, DO THIS:**

1. ✅ Backend CORS is configured (already done)
2. ⏳ **GO TO NETLIFY** → Update `NEXT_PUBLIC_API_URL` (remove trailing slash)
3. ⏳ **REDEPLOY** Netlify site
4. ✅ Test login

**The issue is simply the trailing slash causing double slashes in URLs!**
