@echo off
REM Quick Deploy Script for Heroku Backend (Windows)

echo Deploying backend to Heroku...

set /p APP_NAME="Enter your Heroku app name: "

REM Add remote if not exists
git remote | findstr "heroku" >nul
if errorlevel 1 (
    echo Adding Heroku remote...
    heroku git:remote -a %APP_NAME%
)

REM Deploy using subtree
echo Pushing backend to Heroku...
git subtree push --prefix backend heroku main

echo.
echo Deployment complete!
echo View logs: heroku logs --tail -a %APP_NAME%
echo Open app: heroku open -a %APP_NAME%
pause
