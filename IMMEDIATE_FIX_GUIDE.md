# IMMEDIATE FIX GUIDE

## Issues Encountered

### ❌ Netlify Build Error
**Error:** `useSearchParams() should be wrapped in a suspense boundary`
**Status:** ✅ FIXED

### ❌ Heroku Build Error  
**Error:** `Could not find a pom.xml file`
**Status:** ⚠️ NEEDS ACTION

---

## What Was Fixed

### ✅ Netlify Fix (Already Applied)
1. Wrapped `useSearchParams()` in Suspense boundary in [app/login/page.tsx](app/login/page.tsx)
2. Removed `trailingSlash: true` from [next.config.mjs](next.config.mjs) for Netlify compatibility
3. Made `output: 'export'` conditional - only for GitHub Pages builds

**Next Steps for Netlify:**
```bash
# Commit the changes
git add .
git commit -m "Fix: Wrap useSearchParams in Suspense for Netlify build"
git push origin main
```

Netlify will auto-deploy if connected to GitHub!

---

## Heroku Fix - Action Required

The Heroku error happens because the `git subtree push` command isn't correctly isolating the backend folder. Here are your options:

### ⭐ RECOMMENDED: Use the Temporary Branch Method

This is the most reliable approach:

```powershell
# 1. Make sure you're in the project root and changes are committed
cd C:\Users\hurme\Documents\william-normann-automotive
git add .
git commit -m "Ready for Heroku deployment"

# 2. Add Heroku remote (if not already added)
heroku git:remote -a william-normann-330b912b355b

# 3. Create a temporary branch containing ONLY the backend folder
git subtree split --prefix backend -b backend-deploy

# 4. Push that branch to Heroku's main branch
git push heroku backend-deploy:main --force

# 5. Delete the temporary branch (cleanup)
git branch -D backend-deploy
```

### 🔄 ALTERNATIVE: Use the PowerShell Script

Run the deployment script I created:

```powershell
.\deploy-backend.ps1
```

Then choose option 1 when prompted.

### 🛠️ ALTERNATIVE: Direct JAR Deployment

If git methods fail, deploy the compiled JAR directly:

```powershell
# 1. Build the backend
cd backend
mvn clean package -DskipTests

# 2. Install Heroku Java plugin (first time only)
heroku plugins:install java

# 3. Deploy the JAR
heroku deploy:jar target/backend-0.0.1-SNAPSHOT.jar --app william-normann-330b912b355b

cd ..
```

---

## Complete Deployment Sequence

### Step 1: Deploy Backend to Heroku

```powershell
# Commit your fixes
git add .
git commit -m "Fix: Netlify build issues and prepare for deployment"

# Deploy backend using subtree method
git subtree split --prefix backend -b backend-deploy
git push heroku backend-deploy:main --force
git branch -D backend-deploy

# Check logs
heroku logs --tail -a william-normann-330b912b355b
```

### Step 2: Verify Backend is Running

```powershell
# Open the backend URL
heroku open -a william-normann-330b912b355b

# Or test the API directly
curl https://william-normann-330b912b355b.herokuapp.com/api/health
```

### Step 3: Deploy Frontend to Netlify

```bash
# Push to GitHub (if connected to Netlify)
git push origin main
```

Or manually:
```bash
npm run build
netlify deploy --prod
```

### Step 4: Update CORS Configuration

Once you have your Netlify URL (e.g., `https://william-normann.netlify.app`):

```powershell
# Update Heroku config with your actual Netlify URL
heroku config:set ALLOWED_ORIGINS=https://your-actual-site.netlify.app -a william-normann-330b912b355b
heroku config:set FRONTEND_URL=https://your-actual-site.netlify.app -a william-normann-330b912b355b

# Restart the backend
heroku restart -a william-normann-330b912b355b
```

---

## Troubleshooting

### If subtree split hangs or is very slow on Windows:

**Option 1:** Use Git Bash instead of PowerShell
```bash
# Open Git Bash
git subtree split --prefix backend -b backend-deploy
git push heroku backend-deploy:main --force
git branch -D backend-deploy
```

**Option 2:** Use the temporary folder method
```powershell
# Create a temporary deployment folder
mkdir ..\heroku-deploy-temp
cd ..\heroku-deploy-temp

# Copy backend files
Copy-Item -Path ..\william-normann-automotive\backend\* -Destination . -Recurse

# Initialize git and deploy
git init
heroku git:remote -a william-normann-330b912b355b
git add .
git commit -m "Deploy backend"
git push heroku main --force

# Clean up
cd ..\william-normann-automotive
Remove-Item ..\heroku-deploy-temp -Recurse -Force
```

### If backend starts but can't connect to database:

```powershell
# Verify DATABASE_URL is set
heroku config -a william-normann-330b912b355b

# Check if PostgreSQL addon is attached
heroku addons -a william-normann-330b912b355b

# View database connection logs
heroku logs --tail -a william-normann-330b912b355b | Select-String "DATABASE"
```

### If you see "Spring profile not set" errors:

```powershell
# Ensure SPRING_PROFILES_ACTIVE is set
heroku config:set SPRING_PROFILES_ACTIVE=prod -a william-normann-330b912b355b
```

---

## Quick Reference

### Check Heroku Status
```powershell
heroku logs --tail -a william-normann-330b912b355b
heroku ps -a william-normann-330b912b355b
heroku config -a william-normann-330b912b355b
```

### Check Netlify Status
```bash
netlify status
netlify open
```

### Redeploy Everything
```powershell
# Backend
git subtree split --prefix backend -b backend-deploy && git push heroku backend-deploy:main --force && git branch -D backend-deploy

# Frontend (if using Netlify CLI)
npm run build
netlify deploy --prod
```

---

## Files Modified

- ✅ [app/login/page.tsx](app/login/page.tsx) - Added Suspense wrapper
- ✅ [next.config.mjs](next.config.mjs) - Fixed static export config
- ✅ [deploy-backend.bat](deploy-backend.bat) - Enhanced deployment script
- ✅ [deploy-backend.ps1](deploy-backend.ps1) - New PowerShell script
- ✅ [HEROKU_DEPLOY_ALTERNATIVES.md](HEROKU_DEPLOY_ALTERNATIVES.md) - Comprehensive guide

---

## Next Actions

1. ✅ Commit the Netlify fixes
2. ⏳ Deploy backend to Heroku using recommended method
3. ⏳ Verify backend is accessible
4. ⏳ Deploy frontend to Netlify
5. ⏳ Update CORS settings with actual Netlify URL
6. ⏳ Test the complete flow
