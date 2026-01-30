# Heroku Deployment - Alternative Methods

If `git subtree` isn't working, here are proven alternatives:

## Method 1: Temporary Branch Method (RECOMMENDED)

This is the most reliable method:

```bash
# 1. Make sure all changes are committed
git add .
git commit -m "Ready for deployment"

# 2. Create a deployment branch from just the backend folder
git subtree split --prefix backend -b backend-deploy

# 3. Push that branch to Heroku
git push heroku backend-deploy:main --force

# 4. Delete the temporary branch
git branch -D backend-deploy
```

## Method 2: Direct JAR Deployment

If git isn't cooperating, deploy the JAR file directly:

```bash
# 1. Build the project locally
cd backend
mvn clean package -DskipTests

# 2. Deploy the JAR directly (requires Heroku CLI plugin)
# Install the deploy plugin first (one time only):
heroku plugins:install java

# Then deploy:
heroku deploy:jar target/backend-0.0.1-SNAPSHOT.jar --app your-app-name

cd ..
```

## Method 3: Create Heroku App in Backend Directory

This method creates a separate Heroku app just for the backend:

```bash
# 1. Navigate to backend directory
cd backend

# 2. Initialize git (if not already a git repo)
git init

# 3. Create Heroku app
heroku create your-app-name

# 4. Add PostgreSQL
heroku addons:create heroku-postgresql:essential-0

# 5. Set config vars
heroku config:set JWT_SECRET=your-secret-here
heroku config:set SPRING_PROFILES_ACTIVE=prod
heroku config:set STRIPE_SECRET_KEY=your-key
heroku config:set STRIPE_WEBHOOK_SECRET=your-secret
heroku config:set ALLOWED_ORIGINS=https://your-netlify-site.netlify.app
heroku config:set FRONTEND_URL=https://your-netlify-site.netlify.app

# 6. Deploy
git add .
git commit -m "Initial commit"
git push heroku main

# 7. Go back to main directory
cd ..
```

## Method 4: Using Heroku's Git Worktree

```bash
# 1. Create a worktree for backend
git worktree add ../backend-heroku backend

# 2. Move backend content to root of worktree
cd ../backend-heroku/backend
cp -r * ..
cd ..

# 3. Initialize and push
git init
heroku git:remote -a your-app-name
git add .
git commit -m "Backend deploy"
git push heroku main --force

# 4. Clean up
cd ../william-normann-automotive
git worktree remove ../backend-heroku
```

## Method 5: Manual Copy and Push

```powershell
# PowerShell script
# Create a temporary deployment folder
mkdir ../heroku-deploy-temp
cd ../heroku-deploy-temp

# Initialize git
git init
heroku git:remote -a your-app-name

# Copy backend files
Copy-Item -Path ../william-normann-automotive/backend/* -Destination . -Recurse

# Commit and push
git add .
git commit -m "Deploy backend"
git push heroku main --force

# Clean up
cd ../william-normann-automotive
Remove-Item ../heroku-deploy-temp -Recurse -Force
```

## Troubleshooting "Could not find a pom.xml" Error

This error means Heroku is looking for `pom.xml` in the root directory, but it's in `/backend`. The subtree method should handle this, but if it fails:

### Verify the split worked correctly:
```bash
git subtree split --prefix backend main
```

This should output a commit SHA. If it does, you can push it directly:
```bash
git push heroku <SHA>:main --force
```

### Check what's in your Heroku repo:
```bash
heroku git:clone -a your-app-name temp-check
cd temp-check
ls
# You should see pom.xml in the root
cd ..
rm -rf temp-check
```

## Recommended Workflow

1. **First time setup:**
   ```bash
   git subtree split --prefix backend -b backend-deploy
   heroku git:remote -a your-app-name
   git push heroku backend-deploy:main --force
   git branch -D backend-deploy
   ```

2. **Subsequent deployments:**
   ```bash
   # Make changes in backend/
   git add .
   git commit -m "Update backend"
   git push origin main  # Push to GitHub
   
   # Deploy to Heroku
   git subtree split --prefix backend -b backend-deploy
   git push heroku backend-deploy:main --force
   git branch -D backend-deploy
   ```

## Windows-Specific Notes

If you're on Windows and git subtree is very slow or hangs:

1. Use Git Bash instead of PowerShell
2. Or use the JAR deployment method
3. Or use the temporary folder copy method

## Quick Deploy Script (PowerShell)

Save this as `deploy-backend.ps1`:

```powershell
$APP_NAME = Read-Host "Enter your Heroku app name"

Write-Host "Creating deployment branch..." -ForegroundColor Green
git subtree split --prefix backend -b backend-deploy

Write-Host "Pushing to Heroku..." -ForegroundColor Green
git push heroku backend-deploy:main --force

Write-Host "Cleaning up..." -ForegroundColor Green
git branch -D backend-deploy

Write-Host "Deployment complete!" -ForegroundColor Green
Write-Host "View logs: heroku logs --tail -a $APP_NAME"
```

Run it with:
```powershell
.\deploy-backend.ps1
```
