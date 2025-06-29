# Simple Test script cho Transaction Propagation Demo
Write-Host "=== TRANSACTION PROPAGATION DEMO TEST ===" -ForegroundColor Cyan
Write-Host ""

$baseUrl = "http://localhost:1122/api/propagation-demo"

# Function to make HTTP requests
function Test-Api($method, $url, $body = $null, $contentType = "application/x-www-form-urlencoded") {
    try {
        if ($body) {
            $response = Invoke-RestMethod -Method $method -Uri $url -Body $body -ContentType $contentType
        } else {
            $response = Invoke-RestMethod -Method $method -Uri $url
        }
        return @{ Success = $true; Response = $response }
    } catch {
        return @{ Success = $false; Error = $_.Exception.Message }
    }
}

# Function to get account balance
function Get-Balance($accountId) {
    $result = Test-Api "GET" "$baseUrl/account/$accountId"
    if ($result.Success) {
        return $result.Response.balance
    } else {
        Write-Host "Error getting balance: $($result.Error)" -ForegroundColor Red
        return 0
    }
}

Write-Host "Step 1: Creating test account..." -ForegroundColor Blue

# Create test account
$accountData = '{"owner":"Propagation Test User","balance":1000.0}'
$setupResult = Test-Api "POST" "$baseUrl/setup" $accountData "application/json"

if (-not $setupResult.Success) {
    Write-Host "Failed to create account: $($setupResult.Error)" -ForegroundColor Red
    exit 1
}

$accountId = $setupResult.Response.id
$initialBalance = $setupResult.Response.balance

Write-Host "Created account: ID=$accountId, Balance=$initialBalance" -ForegroundColor Green
Write-Host ""

Write-Host "Step 2: Testing Individual Propagations..." -ForegroundColor Blue
Write-Host ""

# Test 1: REQUIRED
Write-Host "Test 1: REQUIRED Propagation" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "POST" "$baseUrl/demo/required/$accountId" "amount=100"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: $($result.Response)" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance" -ForegroundColor White
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 2: REQUIRES_NEW
Write-Host "Test 2: REQUIRES_NEW Propagation" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "POST" "$baseUrl/demo/requires-new/$accountId" "amount=50"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: $($result.Response)" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance" -ForegroundColor White
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 3: NESTED
Write-Host "Test 3: NESTED Propagation" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "POST" "$baseUrl/demo/nested/$accountId" "amount=25"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: $($result.Response)" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance" -ForegroundColor White
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 4: SUPPORTS
Write-Host "Test 4: SUPPORTS Propagation" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "GET" "$baseUrl/demo/supports/$accountId"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: $($result.Response)" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance (should be same)" -ForegroundColor White
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 5: MANDATORY (should fail)
Write-Host "Test 5: MANDATORY Propagation (should fail)" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "POST" "$baseUrl/demo/mandatory/$accountId" "amount=75"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  UNEXPECTED SUCCESS: $($result.Response)" -ForegroundColor Yellow
} else {
    Write-Host "  EXPECTED FAILURE: No transaction context" -ForegroundColor Green
}
Write-Host "  Balance: $beforeBalance -> $afterBalance (should be same)" -ForegroundColor White
Write-Host ""

# Test 6: NEVER
Write-Host "Test 6: NEVER Propagation" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "GET" "$baseUrl/demo/never/$accountId"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: $($result.Response)" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance (should be same)" -ForegroundColor White
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 7: Complex Scenario
Write-Host "Test 7: Complex Scenario" -ForegroundColor Magenta
$beforeBalance = Get-Balance $accountId
$result = Test-Api "POST" "$baseUrl/demo/complex-scenario/$accountId"
$afterBalance = Get-Balance $accountId

if ($result.Success) {
    Write-Host "  SUCCESS: Complex scenario completed" -ForegroundColor Green
    Write-Host "  Balance: $beforeBalance -> $afterBalance" -ForegroundColor White
    Write-Host "  Details:" -ForegroundColor Cyan
    $result.Response -split "`n" | ForEach-Object { 
        if ($_.Trim()) { Write-Host "    $_" -ForegroundColor White }
    }
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 8: Comprehensive Test
Write-Host "Test 8: Running Comprehensive Test" -ForegroundColor Magenta
$result = Test-Api "POST" "$baseUrl/run-all-demos"

if ($result.Success) {
    Write-Host "  SUCCESS: All demos completed" -ForegroundColor Green
    Write-Host "  Results:" -ForegroundColor Cyan
    $result.Response -split "`n" | ForEach-Object { 
        if ($_.Trim()) { Write-Host "    $_" -ForegroundColor White }
    }
} else {
    Write-Host "  FAILED: $($result.Error)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=== FINAL SUMMARY ===" -ForegroundColor Cyan
$finalBalance = Get-Balance $accountId
Write-Host "Account ID: $accountId" -ForegroundColor White
Write-Host "Initial Balance: $initialBalance" -ForegroundColor White
Write-Host "Final Balance: $finalBalance" -ForegroundColor White
$totalChange = $finalBalance - $initialBalance
Write-Host "Total Change: $totalChange" -ForegroundColor $(if ($totalChange -gt 0) { "Green" } else { "Yellow" })

Write-Host ""
Write-Host "Check application logs for detailed transaction analysis!" -ForegroundColor Yellow 