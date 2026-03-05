#!/usr/bin/env bash

# Heroku Backend Deployment Script (macOS / Linux, Bash)
# Run this from your project root directory.
# Mirrors the behavior of deploy-backend.ps1.

set -euo pipefail

echo
echo "========================================"
echo "Heroku Backend Deployment Script"
echo "========================================"
echo

read -rp "Enter your Heroku app name: " APP_NAME

echo
echo "Checking git status..."
git status --short

echo
echo "========================================"
echo "Choose deployment method:"
echo "1. Subtree split method (recommended)"
echo "2. Force push entire repo (not recommended)"
echo "3. Show troubleshooting help"
echo "========================================"
echo

read -rp "Enter choice (1, 2, or 3): " METHOD

case "$METHOD" in
  "1")
    echo
    echo "Adding Heroku remote (if not exists)..."
    if ! git remote | grep -q "^heroku$"; then
      heroku git:remote -a "$APP_NAME"
    fi

    echo
    echo "Creating deployment branch from backend folder..."
    if git subtree split --prefix backend -b backend-deploy; then
      echo
      echo "Pushing to Heroku..."
      git push heroku backend-deploy:main --force

      echo
      echo "Cleaning up temporary branch..."
      git branch -D backend-deploy

      echo
      echo "========================================"
      echo "Deployment complete!"
      echo "========================================"
      echo
    else
      echo
      echo "Error creating subtree split. Try method 3 for help."
    fi
    ;;

  "2")
    echo
    echo "WARNING: This will push the entire repository to Heroku."
    echo "Heroku expects pom.xml in the root. This may not work."
    echo
    read -rp "Are you sure? (yes/no): " confirm

    if [[ "$confirm" == "yes" ]]; then
      echo
      echo "Adding Heroku remote (if not exists)..."
      if ! git remote | grep -q "^heroku$"; then
        heroku git:remote -a "$APP_NAME"
      fi

      echo
      echo "Pushing to Heroku..."
      git push heroku main --force
    else
      echo "Cancelled."
    fi
    ;;

  "3")
    echo
    echo "========================================"
    echo "TROUBLESHOOTING GUIDE"
    echo "========================================"
    echo

    cat <<'EOF'
If subtree isn't working, try these alternatives:

Method A: Manual branch creation
  1. Commit all changes:
     git add .
     git commit -m "Ready for deployment"

  2. Create deployment branch:
     git subtree split --prefix backend -b backend-deploy

  3. Push to Heroku:
     git push heroku backend-deploy:main --force

  4. Clean up:
     git branch -D backend-deploy


Method B: Direct JAR deployment
  1. Build the JAR:
     cd backend
     mvn clean package -DskipTests

  2. Install Heroku Java plugin (first time only):
     heroku plugins:install java

  3. Deploy JAR directly:
     heroku deploy:jar target/backend-0.0.1-SNAPSHOT.jar --app <YOUR_APP_NAME>
     cd ..


Method C: Temporary copy method
  1. Create temp folder:
     mkdir ../heroku-temp
     cd ../heroku-temp

  2. Copy backend files:
     cp -R ../william-normann-automotive/backend/* .

  3. Initialize and push:
     git init
     heroku git:remote -a <YOUR_APP_NAME>
     git add .
     git commit -m "Deploy"
     git push heroku main --force

  4. Clean up:
     cd ../william-normann-automotive
     rm -rf ../heroku-temp


For more options, see HEROKU_DEPLOY_ALTERNATIVES.md if present in the repo.
EOF
    ;;

  *)
    echo
    echo "Invalid choice!"
    exit 1
    ;;
esac

echo
echo "========================================"
echo "Useful commands:"
echo "========================================"
echo "View logs:   heroku logs --tail -a $APP_NAME"
echo "Open app:    heroku open -a $APP_NAME"
echo "View config: heroku config -a $APP_NAME"
echo "Restart app: heroku restart -a $APP_NAME"
echo

#!/bin/bash
# Quick Deploy Script for Heroku Backend

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${YELLOW}Deploying backend to Heroku...${NC}"

# Get the Heroku app name
read -p "Enter your Heroku app name: " APP_NAME

# Add remote if not exists
if ! git remote | grep -q heroku; then
    echo -e "${GREEN}Adding Heroku remote...${NC}"
    heroku git:remote -a $APP_NAME
fi

# Deploy using subtree
echo -e "${GREEN}Pushing backend to Heroku...${NC}"
git subtree push --prefix backend heroku main

echo -e "${GREEN}Deployment complete!${NC}"
echo -e "View logs: ${YELLOW}heroku logs --tail -a $APP_NAME${NC}"
echo -e "Open app: ${YELLOW}heroku open -a $APP_NAME${NC}"
