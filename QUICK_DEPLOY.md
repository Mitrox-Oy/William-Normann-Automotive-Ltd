# Quick Deploy Commands

## Heroku Backend - Use One of These:

### Method 1: PowerShell Script (Easiest)
```powershell
.\deploy-backend.ps1
```

### Method 2: Direct Commands (Recommended)
```powershell
git add .
git commit -m "Deploy to Heroku"
git subtree split --prefix backend -b backend-deploy
git push heroku backend-deploy:main --force
git branch -D backend-deploy
```

### Method 3: Git Bash (If PowerShell is slow)
```bash
git subtree split --prefix backend -b backend-deploy && \
git push heroku backend-deploy:main --force && \
git branch -D backend-deploy
```

### Method 4: JAR Deployment (If git fails)
```powershell
cd backend
mvn clean package -DskipTests
heroku deploy:jar target/backend-0.0.1-SNAPSHOT.jar --app william-normann-330b912b355b
cd ..
```

## Netlify Frontend

### Auto Deploy (If connected to GitHub)
```bash
git push origin main
```

### Manual Deploy
```bash
npm run build
netlify deploy --prod
```

## After First Deploy - Update CORS

```powershell
# Replace with your actual Netlify URL
heroku config:set ALLOWED_ORIGINS=https://your-site.netlify.app -a william-normann-330b912b355b
heroku config:set FRONTEND_URL=https://your-site.netlify.app -a william-normann-330b912b355b
heroku restart -a william-normann-330b912b355b
```

## Monitor Deployments

### Heroku
```powershell
heroku logs --tail -a william-normann-330b912b355b
heroku open -a william-normann-330b912b355b
```

### Netlify
```bash
netlify logs
netlify open
```
