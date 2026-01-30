# Heroku Backend Deployment Script (PowerShell)
# Run this from your project root directory

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Heroku Backend Deployment Script" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

$APP_NAME = Read-Host "Enter your Heroku app name"

Write-Host "`nChecking git status..." -ForegroundColor Yellow
git status --short

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Choose deployment method:" -ForegroundColor Cyan
Write-Host "1. Subtree split method (recommended)" -ForegroundColor Green
Write-Host "2. Force push entire repo (not recommended)" -ForegroundColor Yellow
Write-Host "3. Show troubleshooting help" -ForegroundColor White
Write-Host "========================================`n" -ForegroundColor Cyan

$METHOD = Read-Host "Enter choice (1, 2, or 3)"

switch ($METHOD) {
    "1" {
        Write-Host "`nAdding Heroku remote (if not exists)..." -ForegroundColor Green
        $remotes = git remote
        if ($remotes -notcontains "heroku") {
            heroku git:remote -a $APP_NAME
        }

        Write-Host "`nCreating deployment branch from backend folder..." -ForegroundColor Green
        git subtree split --prefix backend -b backend-deploy

        if ($LASTEXITCODE -eq 0) {
            Write-Host "`nPushing to Heroku..." -ForegroundColor Green
            git push heroku backend-deploy:main --force

            Write-Host "`nCleaning up temporary branch..." -ForegroundColor Green
            git branch -D backend-deploy

            Write-Host "`n========================================" -ForegroundColor Green
            Write-Host "Deployment complete!" -ForegroundColor Green
            Write-Host "========================================`n" -ForegroundColor Green
        } else {
            Write-Host "`nError creating subtree split. Try method 3 for help." -ForegroundColor Red
        }
    }
    "2" {
        Write-Host "`nWARNING: This will push the entire repository to Heroku." -ForegroundColor Yellow
        Write-Host "Heroku expects pom.xml in the root. This may not work.`n" -ForegroundColor Yellow
        $confirm = Read-Host "Are you sure? (yes/no)"
        
        if ($confirm -eq "yes") {
            Write-Host "`nAdding Heroku remote (if not exists)..." -ForegroundColor Green
            $remotes = git remote
            if ($remotes -notcontains "heroku") {
                heroku git:remote -a $APP_NAME
            }

            Write-Host "`nPushing to Heroku..." -ForegroundColor Green
            git push heroku main --force
        } else {
            Write-Host "Cancelled." -ForegroundColor Yellow
        }
    }
    "3" {
        Write-Host "`n========================================" -ForegroundColor Cyan
        Write-Host "TROUBLESHOOTING GUIDE" -ForegroundColor Cyan
        Write-Host "========================================`n" -ForegroundColor Cyan
        
        Write-Host "If subtree isn't working, try these alternatives:`n" -ForegroundColor Yellow
        
        Write-Host "Method A: Manual branch creation" -ForegroundColor Green
        Write-Host @"
  1. Commit all changes:
     git add .
     git commit -m "Ready for deployment"
  
  2. Create deployment branch:
     git subtree split --prefix backend -b backend-deploy
  
  3. Push to Heroku:
     git push heroku backend-deploy:main --force
  
  4. Clean up:
     git branch -D backend-deploy

"@

        Write-Host "Method B: Direct JAR deployment" -ForegroundColor Green
        Write-Host @"
  1. Build the JAR:
     cd backend
     mvn clean package -DskipTests
  
  2. Install Heroku Java plugin (first time only):
     heroku plugins:install java
  
  3. Deploy JAR directly:
     heroku deploy:jar target/backend-0.0.1-SNAPSHOT.jar --app $APP_NAME
     cd ..

"@

        Write-Host "Method C: Temporary copy method" -ForegroundColor Green
        Write-Host @"
  1. Create temp folder:
     mkdir ..\heroku-temp
     cd ..\heroku-temp
  
  2. Copy backend files:
     Copy-Item -Path ..\william-normann-automotive\backend\* -Destination . -Recurse
  
  3. Initialize and push:
     git init
     heroku git:remote -a $APP_NAME
     git add .
     git commit -m "Deploy"
     git push heroku main --force
  
  4. Clean up:
     cd ..\william-normann-automotive
     Remove-Item ..\heroku-temp -Recurse -Force

"@

        Write-Host "See HEROKU_DEPLOY_ALTERNATIVES.md for more options.`n" -ForegroundColor Cyan
    }
    default {
        Write-Host "`nInvalid choice!" -ForegroundColor Red
        exit
    }
}

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Useful commands:" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "View logs:   heroku logs --tail -a $APP_NAME" -ForegroundColor White
Write-Host "Open app:    heroku open -a $APP_NAME" -ForegroundColor White
Write-Host "View config: heroku config -a $APP_NAME" -ForegroundColor White
Write-Host "Restart app: heroku restart -a $APP_NAME`n" -ForegroundColor White
