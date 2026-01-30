# Script to create demo users on Heroku
# This uses the backend API to create and promote users

$BACKEND_URL = "https://william-normann-330b912b355b.herokuapp.com"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Creating Demo Users on Heroku" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

# Function to create a user
function Create-User {
    param (
        [string]$Email,
        [string]$Password,
        [string]$FirstName,
        [string]$LastName
    )
    
    Write-Host "Creating user: $Email..." -ForegroundColor Yellow
    
    $body = @{
        username = $Email
        password = $Password
        firstName = $FirstName
        lastName = $LastName
    } | ConvertTo-Json
    
    try {
        $response = Invoke-RestMethod -Uri "$BACKEND_URL/api/auth/register" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body `
            -ErrorAction Stop
        
        Write-Host "✅ User created successfully!" -ForegroundColor Green
        return $true
    }
    catch {
        $errorMessage = $_.Exception.Message
        if ($errorMessage -like "*already*") {
            Write-Host "⚠️  User already exists" -ForegroundColor Yellow
            return $true
        }
        else {
            Write-Host "❌ Error: $errorMessage" -ForegroundColor Red
            return $false
        }
    }
}

Write-Host "Step 1: Creating Customer Account" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
$customerCreated = Create-User -Email "customer@example.com" -Password "password" -FirstName "Demo" -LastName "Customer"

Write-Host "`nStep 2: Creating Owner Account" -ForegroundColor Cyan
Write-Host "-------------------------------" -ForegroundColor Cyan
$ownerCreated = Create-User -Email "owner@example.com" -Password "password" -FirstName "Store" -LastName "Owner"

if ($customerCreated -and $ownerCreated) {
    Write-Host "`n========================================" -ForegroundColor Green
    Write-Host "⚠️  IMPORTANT: Manual Step Required!" -ForegroundColor Yellow
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Both users were created as CUSTOMER role." -ForegroundColor White
    Write-Host "You need to promote owner@example.com to OWNER role." -ForegroundColor White
    Write-Host ""
    Write-Host "Option 1: Use Heroku Dashboard" -ForegroundColor Cyan
    Write-Host "  1. Go to https://data.heroku.com" -ForegroundColor White
    Write-Host "  2. Select your app: william-normann" -ForegroundColor White
    Write-Host "  3. Click on your PostgreSQL database" -ForegroundColor White
    Write-Host "  4. Click 'Dataclips' or 'Settings' → 'View Credentials'" -ForegroundColor White
    Write-Host "  5. Use the credentials to connect via pgAdmin or another client" -ForegroundColor White
    Write-Host ""
    Write-Host "Option 2: Install PostgreSQL Client" -ForegroundColor Cyan
    Write-Host "  Install PostgreSQL from: https://www.postgresql.org/download/windows/" -ForegroundColor White
    Write-Host "  Then run: heroku pg:psql -a william-normann" -ForegroundColor White
    Write-Host "  Execute: UPDATE users SET role = 'OWNER' WHERE username = 'owner@example.com';" -ForegroundColor White
    Write-Host ""
    Write-Host "Option 3: Use SQL from Heroku Dashboard" -ForegroundColor Cyan
    Write-Host "  1. Get database credentials: heroku pg:credentials:url -a william-normann" -ForegroundColor White
    Write-Host "  2. Use an online PostgreSQL client like https://sqliteonline.com/" -ForegroundColor White
    Write-Host "  3. Connect and run: UPDATE users SET role = 'OWNER' WHERE username = 'owner@example.com';" -ForegroundColor White
    Write-Host ""
    Write-Host "SQL to run:" -ForegroundColor Yellow
    Write-Host "  UPDATE users SET role = 'OWNER' WHERE username = 'owner@example.com';" -ForegroundColor White
    Write-Host ""
}
else {
    Write-Host "`n❌ Failed to create one or more users" -ForegroundColor Red
}

Write-Host "`nPress any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
