#!/usr/bin/env bash
set -euo pipefail

DEFAULT_FORWARD_URL="http://localhost:8080/api/stripe/webhook"
FORWARD_URL="${1:-$DEFAULT_FORWARD_URL}"
SECRET_DIR=".stripe"
SECRET_FILE="$SECRET_DIR/webhook-secret"

if ! command -v stripe >/dev/null 2>&1; then
  echo "Error: Stripe CLI is not installed or not on your PATH." >&2
  echo "Install it from https://stripe.com/docs/stripe-cli before running this script." >&2
  exit 1
fi

mkdir -p "$SECRET_DIR"

echo "📡 Starting Stripe CLI listener..."
echo "➡️  Forwarding events to: $FORWARD_URL"
echo "💾 Webhook secret will be written to: $SECRET_FILE"
echo

stripe listen --forward-to "$FORWARD_URL" --events checkout.session.completed --print-secret \
  | awk -v file="$SECRET_FILE" '
      NR==1 {
        secret=$0
        print secret > file
        close(file)
        printf("✅ Saved webhook secret to %s\n", file)
        printf("   Export it for your backend with:\n")
        printf("   export STRIPE_WEBHOOK_SECRET=%s\n\n", secret)
      }
      { print }
    '
