#!/bin/bash
# Shell script to set Stripe environment variables
# Run this before starting the Spring Boot backend
# Usage: source set_stripe_keys.sh

echo "Setting Stripe environment variables..."

export STRIPE_SECRET_KEY="sk_test_51SNugmA9qWDROwYGIOgmDdIiz2lwLdfaaFTsFDvrk0SQ1KS5pHSYonIa8jXMwR7TFvFjXFIR8aw5CwLjU9AkqsFl00DRmX6b3r"
export STRIPE_WEBHOOK_SECRET="whsec_32e682d541e4d933c446aaa7e83e5c561590f418edd12b9a61c5c68d7e24ae16"
export FRONTEND_URL="http://localhost:4200"

echo ""
echo "✅ Stripe keys configured!"
echo "- Secret Key: ${STRIPE_SECRET_KEY:0:20}..."
echo "- Webhook Secret: ${STRIPE_WEBHOOK_SECRET:0:20}..."
echo "- Frontend URL: $FRONTEND_URL"
echo ""
echo "You can now start the backend with: mvn spring-boot:run"
echo ""

