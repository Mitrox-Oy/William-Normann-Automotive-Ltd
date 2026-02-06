# Script to create demo customer user on Heroku
# Privileged OWNER/ADMIN users are managed separately (see scripts/sync-privileged-users.js)

$BACKEND_URL = "https://william-normann-330b912b355b.herokuapp.com"

Write-Host "`n========================================" -ForegroundColor Cyan
Write-Host "Creating Demo Customer on Heroku" -ForegroundColor Cyan
Write-Host "========================================`n" -ForegroundColor Cyan

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
        $null = Invoke-RestMethod -Uri "$BACKEND_URL/api/auth/register" `
            -Method Post `
            -ContentType "application/json" `
            -Body $body `
            -ErrorAction Stop

        Write-Host "OK: User created" -ForegroundColor Green
        return $true
    }
    catch {
        $errorMessage = $_.Exception.Message
        if ($errorMessage -like "*already*") {
            Write-Host "OK: User already exists" -ForegroundColor Yellow
            return $true
        }

        Write-Host "ERROR: $errorMessage" -ForegroundColor Red
        return $false
    }
}

Write-Host "Step 1: Creating Customer Account" -ForegroundColor Cyan
Write-Host "-----------------------------------" -ForegroundColor Cyan
$customerCreated = Create-User -Email "customer@example.com" -Password "password" -FirstName "Demo" -LastName "Customer"

if ($customerCreated) {
    Write-Host "`nNext: Privileged accounts" -ForegroundColor Cyan
    Write-Host "- Use: node scripts/sync-privileged-users.js" -ForegroundColor White
    Write-Host "- Provide DATABASE_URL + WNA_* env vars (see script header)" -ForegroundColor White
}

Write-Host "`nPress any key to continue..." -ForegroundColor Gray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
