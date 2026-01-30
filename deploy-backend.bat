@echo off
REM Quick Deploy Script for Heroku Backend (Windows)

echo ========================================
echo Heroku Backend Deployment Script
echo ========================================
echo.

set /p APP_NAME="Enter your Heroku app name: "

echo.
echo Checking git status...
git status --short

echo.
echo ========================================
echo Choose deployment method:
echo 1. Git subtree (recommended)
echo 2. Force push with subtree
echo 3. Help - I'm having issues
echo ========================================
set /p METHOD="Enter choice (1, 2, or 3): "

if "%METHOD%"=="1" goto NORMAL
if "%METHOD%"=="2" goto FORCE
if "%METHOD%"=="3" goto HELP
goto INVALID

:NORMAL
echo.
echo Adding Heroku remote (if not exists)...
git remote | findstr "heroku" >nul
if errorlevel 1 (
    heroku git:remote -a %APP_NAME%
)

echo.
echo Deploying backend to Heroku...
git subtree push --prefix backend heroku main
goto END

:FORCE
echo.
echo Adding Heroku remote (if not exists)...
git remote | findstr "heroku" >nul
if errorlevel 1 (
    heroku git:remote -a %APP_NAME%
)

echo.
echo Force pushing backend to Heroku...
for /f "delims=" %%i in ('git subtree split --prefix backend main') do set SPLIT_SHA=%%i
git push heroku %SPLIT_SHA%:main --force
goto END

:HELP
echo.
echo ========================================
echo TROUBLESHOOTING TIPS:
echo ========================================
echo.
echo If git subtree isn't working, try these alternatives:
echo.
echo Method A: Create a separate branch with only backend
echo   git subtree split --prefix backend -b backend-deploy
echo   git push heroku backend-deploy:main --force
echo   git branch -D backend-deploy
echo.
echo Method B: Verify your setup
echo   1. Make sure you've committed all changes:
echo      git add .
echo      git commit -m "Ready for deployment"
echo.
echo   2. Check if Heroku remote exists:
echo      git remote -v
echo.
echo   3. If heroku remote is missing, add it:
echo      heroku git:remote -a %APP_NAME%
echo.
echo   4. Verify the backend folder has pom.xml:
echo      dir backend\pom.xml
echo.
echo Method C: Manual deployment via Heroku CLI
echo   cd backend
echo   mvn clean package
echo   heroku deploy:jar target/*.jar --app %APP_NAME%
echo   cd ..
echo.
goto END

:INVALID
echo Invalid choice!
goto END

:END
echo.
echo ========================================
echo Useful commands:
echo   heroku logs --tail -a %APP_NAME%
echo   heroku open -a %APP_NAME%
echo   heroku config -a %APP_NAME%
echo ========================================
echo.
pause
