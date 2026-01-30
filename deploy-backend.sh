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
