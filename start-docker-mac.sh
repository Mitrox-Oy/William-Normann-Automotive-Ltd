#!/usr/bin/env bash

# macOS start script for Next.js + Spring Boot + PostgreSQL using Docker/OrbStack
# Mirrors the behavior of start-docker.bat for local development on macOS.

set -euo pipefail

echo "========================================"
echo "Starting Automotive Website with Docker (macOS)"
echo "========================================"
echo

echo "Step 1: Checking Docker CLI (OrbStack / Docker Desktop)..."
if ! command -v docker >/dev/null 2>&1; then
  echo "ERROR: Docker CLI not found."
  echo "Make sure OrbStack (or Docker Desktop) is installed and running."
  exit 1
fi
echo "Docker CLI is available!"
echo

# Prefer docker-compose if available; otherwise fall back to 'docker compose'
if command -v docker-compose >/dev/null 2>&1; then
  DC="docker-compose"
else
  DC="docker compose"
fi

POSTGRES_CONTAINER="william-normann-shop-db"
BACKEND_CONTAINER="william-normann-shop-backend"

best_effort() {
  # Match the Windows script behavior (ignore migration errors).
  # We still print a warning so failures are visible.
  if ! "$@" 2>/dev/null; then
    echo "WARN: command failed (ignored): $*"
  fi
}

echo "Step 2: Starting PostgreSQL database..."
$DC up -d postgres
echo "Waiting for database to be ready..."
for _ in {1..30}; do
  if $DC exec -T postgres pg_isready -U postgres -d william_normann_shop >/dev/null 2>&1; then
    break
  fi
  sleep 1
done
echo

echo "Step 3: Running database migrations..."
echo "Adding Stripe Checkout columns..."
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS stripe_checkout_session_id VARCHAR(255);"
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'eur';"
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "CREATE INDEX IF NOT EXISTS idx_orders_checkout_session ON orders(stripe_checkout_session_id);"

echo "Adding cart reservation support columns..."
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP;"
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "CREATE INDEX IF NOT EXISTS idx_cart_items_reserved_until ON cart_items(reserved_until);"
best_effort $DC exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS inventory_locked BOOLEAN NOT NULL DEFAULT FALSE;"
echo "Migration completed!"
echo

echo "Step 4: Building and starting Spring Boot backend..."
$DC up -d --build backend
echo

echo "Step 5: Waiting for backend to start..."
sleep 15
echo

if ! docker inspect -f '{{.State.Running}}' "$BACKEND_CONTAINER" >/dev/null 2>&1; then
  echo "ERROR: Backend container not found ($BACKEND_CONTAINER)."
  echo "Services:"
  $DC ps || true
  exit 1
fi

if [[ "$(docker inspect -f '{{.State.Running}}' "$BACKEND_CONTAINER" 2>/dev/null || echo false)" != "true" ]]; then
  echo "ERROR: Backend container is not running."
  echo
  echo "Last backend logs:"
  $DC logs --tail=200 backend || true
  echo
  echo "Services:"
  $DC ps || true
  exit 1
fi

echo "========================================"
echo "Services Status:"
echo "========================================"
$DC ps
echo

echo "========================================"
echo "Configuration:"
echo "========================================"
echo "- Backend API: http://localhost:8080"
echo "- Frontend (Next.js): http://localhost:3000 (start manually)"
echo "- Database: localhost:5432"
echo "- Database Name: william_normann_shop"
echo "- Database User: postgres"
echo "- Stripe: Configured via environment variables"
echo

echo "========================================"
echo "Next Steps:"
echo "========================================"
echo "1. Create .env.local file in project root (if not already present):"
echo "   NEXT_PUBLIC_SHOP_API_BASE_URL=http://localhost:8080"
echo
echo "2. Start Next.js frontend (in project root):"
echo "   npm run dev"
echo
echo "3. Open browser: http://localhost:3000"
echo
echo "4. Test authentication:"
echo "   - Login page: http://localhost:3000/login"
echo "   - Shop: http://localhost:3000/shop"
echo "   - Admin: http://localhost:3000/admin (owner role required)"
echo

echo "========================================"
echo "Useful Commands:"
echo "========================================"
echo "View backend logs: $DC logs -f backend"
echo "View database logs: $DC logs -f postgres"
echo "Stop all services: $DC down"
echo "Restart backend: $DC restart backend"
echo

echo "========================================"
echo "Note:"
echo "========================================"
echo "Make sure your backend CORS allows:"
echo "- http://localhost:3000 (Next.js frontend)"
echo
echo "Update docker-compose.yml ALLOWED_ORIGINS if needed."
echo

echo "Done."
