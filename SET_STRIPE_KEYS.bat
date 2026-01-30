@echo off
REM Windows batch script to set Stripe environment variables
REM Run this before starting the Spring Boot backend

echo Setting Stripe environment variables...

set STRIPE_SECRET_KEY=sk_test_51SNugmA9qWDROwYGIOgmDdIiz2lwLdfaaFTsFDvrk0SQ1KS5pHSYonIa8jXMwR7TFvFjXFIR8aw5CwLjU9AkqsFl00DRmX6b3r
set STRIPE_WEBHOOK_SECRET=whsec_32e682d541e4d933c446aaa7e83e5c561590f418edd12b9a61c5c68d7e24ae16
set FRONTEND_URL=http://localhost:3000
set STRIPE_PUBLISHABLE_KEY=pk_test_51SNugmA9qWDROwYGqg7nKMrNh0dYG6T5GfzfJc249xhSB8KkG5JI5JmL8pbzFDvl08bGIHhQPjEBa0z3LYKw00jZ00Re6XIWf7

echo.
echo Stripe keys configured!
echo - Secret Key: %STRIPE_SECRET_KEY:~0,20%...
echo - Webhook Secret: %STRIPE_WEBHOOK_SECRET:~0,20%...
echo - Frontend URL: %FRONTEND_URL%
echo.
echo You can now start the backend with: mvn spring-boot:run
echo.

