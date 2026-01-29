@echo off
REM Updated start script for Next.js + Spring Boot + Docker setup
echo ========================================
echo Starting Automotive Website with Docker
echo ========================================
echo.

echo Step 1: Checking Docker Desktop...
docker --version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Docker is not running or not installed
    echo Please start Docker Desktop and try again
    pause
    exit /b 1
)
echo Docker is running!
echo.

echo Step 2: Starting PostgreSQL database...
docker-compose up -d postgres
echo Waiting for database to be ready...
timeout /t 10 /nobreak >nul
echo.

echo Step 3: Running database migrations...
echo Adding Stripe Checkout columns...
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS stripe_checkout_session_id VARCHAR(255);" 2>nul
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS currency VARCHAR(3) NOT NULL DEFAULT 'eur';" 2>nul
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "CREATE INDEX IF NOT EXISTS idx_orders_checkout_session ON orders(stripe_checkout_session_id);" 2>nul
echo Adding cart reservation support columns...
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE cart_items ADD COLUMN IF NOT EXISTS reserved_until TIMESTAMP;" 2>nul
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "CREATE INDEX IF NOT EXISTS idx_cart_items_reserved_until ON cart_items(reserved_until);" 2>nul
docker-compose exec -T postgres psql -U postgres -d william_normann_shop -c "ALTER TABLE orders ADD COLUMN IF NOT EXISTS inventory_locked BOOLEAN NOT NULL DEFAULT FALSE;" 2>nul
echo Migration completed!
echo.

echo Step 4: Building and starting Spring Boot backend...
docker-compose up -d --build backend
echo.

echo Step 5: Waiting for backend to start...
timeout /t 15 /nobreak >nul
echo.

echo ========================================
echo Services Status:
echo ========================================
docker-compose ps
echo.

echo ========================================
echo Configuration:
echo ========================================
echo - Backend API: http://localhost:8080
echo - Frontend (Next.js): http://localhost:3000 (start manually)
echo - Database: localhost:5432
echo - Database Name: william_normann_shop
echo - Database User: postgres
echo - Stripe: Configured via environment variables
echo.

echo ========================================
echo Next Steps:
echo ========================================
echo 1. Create .env.local file in project root:
echo    NEXT_PUBLIC_SHOP_API_BASE_URL=http://localhost:8080
echo.
echo 2. Start Next.js frontend (in project root):
echo    npm run dev
echo.
echo 3. Open browser: http://localhost:3000
echo.
echo 4. Test authentication:
echo    - Login page: http://localhost:3000/login
echo    - Shop: http://localhost:3000/shop
echo    - Admin: http://localhost:3000/admin (owner role required)
echo.
echo ========================================
echo Useful Commands:
echo ========================================
echo View backend logs: docker-compose logs -f backend
echo View database logs: docker-compose logs -f postgres
echo Stop all services: docker-compose down
echo Restart backend: docker-compose restart backend
echo.
echo ========================================
echo Note:
echo ========================================
echo Make sure your backend CORS allows:
echo - http://localhost:3000 (Next.js frontend)
echo.
echo Update docker-compose.yml ALLOWED_ORIGINS if needed.
echo.

pause
