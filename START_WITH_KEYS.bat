@echo off
REM Start backend with Stripe keys configured
REM This script sets environment variables and starts the Spring Boot application

echo ========================================
echo Starting E-Commerce Backend with Stripe
echo ========================================
echo.

REM Set Stripe keys
set STRIPE_SECRET_KEY=sk_test_51SNugmA9qWDROwYGIOgmDdIiz2lwLdfaaFTsFDvrk0SQ1KS5pHSYonIa8jXMwR7TFvFjXFIR8aw5CwLjU9AkqsFl00DRmX6b3r
set STRIPE_WEBHOOK_SECRET=whsec_32e682d541e4d933c446aaa7e83e5c561590f418edd12b9a61c5c68d7e24ae16
set FRONTEND_URL=http://localhost:4200

echo Environment variables set:
echo - STRIPE_SECRET_KEY: sk_test_51SNugm... [CONFIGURED]
echo - STRIPE_WEBHOOK_SECRET: whsec_32e682... [CONFIGURED]
echo - FRONTEND_URL: %FRONTEND_URL%
echo.

REM Navigate to backend directory
cd backend

echo Starting Spring Boot application...
echo.
mvn spring-boot:run

pause

