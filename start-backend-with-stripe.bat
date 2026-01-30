@echo off
REM Start the Spring Boot backend with Stripe configuration

echo Setting Stripe environment variables...

set STRIPE_SECRET_KEY=sk_test_51SNugmA9qWDROwYGIOgmDdIiz2lwLdfaaFTsFDvrk0SQ1KS5pHSYonIa8jXMwR7TFvFjXFIR8aw5CwLjU9AkqsFl00DRmX6b3r
set STRIPE_WEBHOOK_SECRET=whsec_32e682d541e4d933c446aaa7e83e5c561590f418edd12b9a61c5c68d7e24ae16
set FRONTEND_URL=http://localhost:3000

echo.
echo Stripe keys configured!
echo Starting backend...
echo.

cd backend
mvnw.cmd spring-boot:run
