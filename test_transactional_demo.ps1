# @Transactional Demo Test Script
# This script demonstrates various @Transactional behaviors and edge cases

Write-Host "=== @TRANSACTIONAL COMPREHENSIVE DEMO ===" -ForegroundColor Green

$baseUrl = "http://localhost:1122/api/transactional-demo"

# Function to make HTTP requests with error handling
function Invoke-DemoRequest {
    param(
        [string]$Method,
        [string]$Uri,
        [object]$Body = $null,
        [string]$Description
    )
    
    Write-Host "`n--- $Description ---" -ForegroundColor Yellow
    Write-Host "Request: $Method $Uri" -ForegroundColor Cyan
    
    try {
        if ($Body) {
            $bodyJson = $Body | ConvertTo-Json
            Write-Host "Body: $bodyJson" -ForegroundColor Gray
            $response = Invoke-RestMethod -Uri $Uri -Method $Method -Headers @{"Content-Type"="application/json"} -Body $bodyJson
        } else {
            $response = Invoke-RestMethod -Uri $Uri -Method $Method
        }
        
        Write-Host "Response: $($response | ConvertTo-Json -Depth 3)" -ForegroundColor Green
        return $response
    }
    catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        $errorBody = ""
        
        try {
            $stream = $_.Exception.Response.GetResponseStream()
            $reader = New-Object System.IO.StreamReader($stream)
            $errorBody = $reader.ReadToEnd()
        } catch {}
        
        Write-Host "Error ($statusCode): $errorBody" -ForegroundColor Red
        return $null
    }
}

Write-Host "`nSTEP 1: Setup Test Account" -ForegroundColor Magenta

# Create test account
$testAccount = @{
    owner = "Transactional Demo User"
    balance = 1000.0
}

$account = Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/setup" -Body $testAccount -Description "Creating test account"

if (-not $account) {
    Write-Host "Failed to create test account. Exiting..." -ForegroundColor Red
    exit 1
}

$accountId = $account.id
Write-Host "`nTest account created with ID: $accountId" -ForegroundColor Green

Write-Host "`nSTEP 2: Transaction Boundaries Demo" -ForegroundColor Magenta

# Demo 1: Public method with @Transactional
Invoke-DemoRequest -Method "PUT" -Uri "$baseUrl/demo1/public-method/$accountId" -Body @{newBalance=1200} -Description "Demo 1: Public method with @Transactional"

# Check account balance after demo 1
$accountAfterDemo1 = Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$accountId" -Description "Checking balance after Demo 1"
Write-Host "Balance after Demo 1: $($accountAfterDemo1.balance)" -ForegroundColor Cyan

Write-Host "`nSTEP 3: Self-Invocation Problem Demo" -ForegroundColor Magenta

# Demo 2A: Self-Invocation Problem
Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo2/self-invocation" -Description "Demo 2A: Self-Invocation Problem"

# Demo 2B: External call (should work correctly)
Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo2/external-call/$accountId" -Description "Demo 2B: External call to @Transactional method"

# Check account balance after demo 2
$accountAfterDemo2 = Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$accountId" -Description "Checking balance after Demo 2"
Write-Host "Balance after Demo 2: $($accountAfterDemo2.balance)" -ForegroundColor Cyan

Write-Host "`nSTEP 4: Exception & Rollback Behavior Demo" -ForegroundColor Magenta

# Create separate account for rollback tests to avoid interference
$rollbackTestAccount = @{
    owner = "Rollback Test User"
    balance = 2000.0
}

$rollbackAccount = Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/setup" -Body $rollbackTestAccount -Description "Creating rollback test account"
$rollbackAccountId = $rollbackAccount.id

Write-Host "`n--- Testing Exception Scenarios ---" -ForegroundColor Yellow

# Demo 3A: Unchecked Exception (should rollback)
Write-Host "`nDemo 3A: Unchecked Exception - Should ROLLBACK" -ForegroundColor Yellow
$balanceBefore3A = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance before unchecked exception test").balance
Write-Host "Balance BEFORE unchecked exception test: $balanceBefore3A" -ForegroundColor Cyan

Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo3/unchecked-exception/$rollbackAccountId" -Description "Demo 3A: Unchecked Exception (RuntimeException)"

$balanceAfter3A = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance after unchecked exception test").balance
Write-Host "Balance AFTER unchecked exception test: $balanceAfter3A" -ForegroundColor Cyan

if ($balanceBefore3A -eq $balanceAfter3A) {
    Write-Host "ROLLBACK SUCCESS: Balance unchanged ($balanceBefore3A to $balanceAfter3A)" -ForegroundColor Green
} else {
    Write-Host "ROLLBACK FAILED: Balance changed ($balanceBefore3A to $balanceAfter3A)" -ForegroundColor Red
}

# Demo 3B: Checked Exception (should NOT rollback by default)
Write-Host "`nDemo 3B: Checked Exception - Should NOT ROLLBACK" -ForegroundColor Yellow
$balanceBefore3B = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance before checked exception test").balance
Write-Host "Balance BEFORE checked exception test: $balanceBefore3B" -ForegroundColor Cyan

Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo3/checked-exception/$rollbackAccountId" -Description "Demo 3B: Checked Exception (should NOT rollback)"

$balanceAfter3B = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance after checked exception test").balance
Write-Host "Balance AFTER checked exception test: $balanceAfter3B" -ForegroundColor Cyan

if ($balanceBefore3B -ne $balanceAfter3B) {
    Write-Host "NO ROLLBACK SUCCESS: Balance changed ($balanceBefore3B to $balanceAfter3B)" -ForegroundColor Green
} else {
    Write-Host "UNEXPECTED ROLLBACK: Balance unchanged ($balanceBefore3B to $balanceAfter3B)" -ForegroundColor Red
}

# Demo 3C: Custom Rollback Rules (checked exception with rollbackFor)
Write-Host "`nDemo 3C: Custom Rollback Rules - Should ROLLBACK" -ForegroundColor Yellow
$balanceBefore3C = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance before custom rollback test").balance
Write-Host "Balance BEFORE custom rollback test: $balanceBefore3C" -ForegroundColor Cyan

Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo3/custom-rollback/$rollbackAccountId" -Description "Demo 3C: Custom Rollback Rules (rollbackFor=Exception.class)"

$balanceAfter3C = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance after custom rollback test").balance
Write-Host "Balance AFTER custom rollback test: $balanceAfter3C" -ForegroundColor Cyan

if ($balanceBefore3C -eq $balanceAfter3C) {
    Write-Host "CUSTOM ROLLBACK SUCCESS: Balance unchanged ($balanceBefore3C to $balanceAfter3C)" -ForegroundColor Green
} else {
    Write-Host "CUSTOM ROLLBACK FAILED: Balance changed ($balanceBefore3C to $balanceAfter3C)" -ForegroundColor Red
}

Write-Host "`nSTEP 5: No Rollback Rules Demo" -ForegroundColor Magenta

# Demo 4: noRollbackFor (unchecked exception but should NOT rollback)
Write-Host "`nDemo 4: noRollbackFor Rules - Should NOT ROLLBACK" -ForegroundColor Yellow
$balanceBefore4 = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance before noRollbackFor test").balance
Write-Host "Balance BEFORE noRollbackFor test: $balanceBefore4" -ForegroundColor Cyan

Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/demo4/no-rollback/$rollbackAccountId" -Description "Demo 4: noRollbackFor Rules (IllegalArgumentException)"

$balanceAfter4 = (Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Balance after noRollbackFor test").balance
Write-Host "Balance AFTER noRollbackFor test: $balanceAfter4" -ForegroundColor Cyan

if ($balanceBefore4 -ne $balanceAfter4) {
    Write-Host "NO ROLLBACK SUCCESS: Balance changed ($balanceBefore4 to $balanceAfter4)" -ForegroundColor Green
} else {
    Write-Host "UNEXPECTED ROLLBACK: Balance unchanged ($balanceBefore4 to $balanceAfter4)" -ForegroundColor Red
}

Write-Host "`nSTEP 6: Comprehensive Demo" -ForegroundColor Magenta

# Run all demos in sequence
Invoke-DemoRequest -Method "POST" -Uri "$baseUrl/run-all-demos" -Description "Running comprehensive demo suite"

Write-Host "`nSTEP 7: Final Account Status" -ForegroundColor Magenta

# Check final account balances
$finalAccount1 = Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$accountId" -Description "Final status of main test account"
$finalAccount2 = Invoke-DemoRequest -Method "GET" -Uri "$baseUrl/account/$rollbackAccountId" -Description "Final status of rollback test account"

Write-Host "`n=== FINAL RESULTS ===" -ForegroundColor Green
Write-Host "Main Test Account (ID: $accountId):" -ForegroundColor Cyan
Write-Host "  Owner: $($finalAccount1.owner)" -ForegroundColor White
Write-Host "  Final Balance: $($finalAccount1.balance)" -ForegroundColor White
Write-Host "  Started with: 1000.0" -ForegroundColor Gray

Write-Host "`nRollback Test Account (ID: $rollbackAccountId):" -ForegroundColor Cyan
Write-Host "  Owner: $($finalAccount2.owner)" -ForegroundColor White
Write-Host "  Final Balance: $($finalAccount2.balance)" -ForegroundColor White
Write-Host "  Started with: 2000.0" -ForegroundColor Gray

Write-Host "`n@TRANSACTIONAL DEMO COMPLETED!" -ForegroundColor Green
Write-Host "Check the application logs for detailed transaction behavior analysis." -ForegroundColor Yellow
Write-Host "Key learnings:" -ForegroundColor Cyan
Write-Host "  Public methods with @Transactional work correctly" -ForegroundColor White
Write-Host "  Self-invocation does NOT create separate transactions" -ForegroundColor White
Write-Host "  Unchecked exceptions trigger rollback by default" -ForegroundColor White
Write-Host "  Checked exceptions do NOT trigger rollback by default" -ForegroundColor White
Write-Host "  rollbackFor and noRollbackFor can customize behavior" -ForegroundColor White 