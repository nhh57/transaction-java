# Test script cho Transaction Propagation Demo
# Chạy script này để test tất cả các propagation behaviors

Write-Host "=== TRANSACTION PROPAGATION DEMO TEST SCRIPT ===" -ForegroundColor Cyan
Write-Host "Testing Spring Transaction Propagation behaviors..." -ForegroundColor Green
Write-Host ""

$baseUrl = "http://localhost:1122/api/propagation-demo"

# Function to make HTTP requests with error handling
function Invoke-ApiCall {
    param(
        [string]$Method,
        [string]$Url,
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )
    
    try {
        if ($Body) {
            $response = Invoke-RestMethod -Method $Method -Uri $Url -Body $Body -ContentType $ContentType
        } else {
            $response = Invoke-RestMethod -Method $Method -Uri $Url
        }
        return @{ Success = $true; Response = $response }
    } catch {
        return @{ Success = $false; Error = $_.Exception.Message; Response = $_.Exception.Response }
    }
}

# Function to get account balance
function Get-AccountBalance {
    param([long]$AccountId)
    
    $result = Invoke-ApiCall -Method "GET" -Url "$baseUrl/account/$AccountId"
    if ($result.Success) {
        return $result.Response.balance
    } else {
        Write-Host "   ❌ Error getting account balance: $($result.Error)" -ForegroundColor Red
        return $null
    }
}

# Function to display balance change
function Show-BalanceChange {
    param(
        [string]$TestName,
        [long]$AccountId,
        [double]$BeforeBalance,
        [double]$AfterBalance,
        [string]$ExpectedBehavior = ""
    )
    
    $change = $AfterBalance - $BeforeBalance
    if ($change -eq 0) {
        Write-Host "   💰 Balance: $BeforeBalance → $AfterBalance (NO CHANGE)" -ForegroundColor Yellow
    } elseif ($change -gt 0) {
        Write-Host "   💰 Balance: $BeforeBalance → $AfterBalance (+$change)" -ForegroundColor Green
    } else {
        Write-Host "   💰 Balance: $BeforeBalance → $AfterBalance ($change)" -ForegroundColor Red
    }
    
    if ($ExpectedBehavior) {
        Write-Host "   📝 Expected: $ExpectedBehavior" -ForegroundColor Cyan
    }
}

Write-Host "🚀 Step 1: Setting up test account..." -ForegroundColor Blue

# Create test account
$accountData = @{
    owner = "Propagation Test User"
    balance = 1000.0
} | ConvertTo-Json

$setupResult = Invoke-ApiCall -Method "POST" -Url "$baseUrl/setup" -Body $accountData
if (-not $setupResult.Success) {
    Write-Host "❌ Failed to create test account: $($setupResult.Error)" -ForegroundColor Red
    exit 1
}

$accountId = $setupResult.Response.id
$initialBalance = $setupResult.Response.balance

Write-Host "✅ Created test account: ID=$accountId, Balance=$initialBalance" -ForegroundColor Green
Write-Host ""

# ==================== INDIVIDUAL PROPAGATION TESTS ====================

Write-Host "🧪 Step 2: Testing Individual Propagation Types..." -ForegroundColor Blue
Write-Host ""

# Test 1: REQUIRED
Write-Host "📋 Test 1: REQUIRED Propagation" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/required/$accountId" -Body "amount=100" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ REQUIRED: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "REQUIRED" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should increase by 100"
} else {
    Write-Host "   ❌ REQUIRED failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 2: REQUIRES_NEW
Write-Host "📋 Test 2: REQUIRES_NEW Propagation" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/requires-new/$accountId" -Body "amount=50" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ REQUIRES_NEW: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "REQUIRES_NEW" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should increase by 50"
} else {
    Write-Host "   ❌ REQUIRES_NEW failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 3: NESTED
Write-Host "📋 Test 3: NESTED Propagation" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/nested/$accountId" -Body "amount=25" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ NESTED: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "NESTED" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should increase by 25"
} else {
    Write-Host "   ❌ NESTED failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 4: SUPPORTS
Write-Host "📋 Test 4: SUPPORTS Propagation (Read-only)" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "GET" -Url "$baseUrl/demo/supports/$accountId"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ SUPPORTS: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "SUPPORTS" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should NOT change"
} else {
    Write-Host "   ❌ SUPPORTS failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 5: MANDATORY (should fail when called directly)
Write-Host "📋 Test 5: MANDATORY Propagation (Direct call - should fail)" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/mandatory/$accountId" -Body "amount=75" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ⚠️  MANDATORY succeeded unexpectedly: $($result.Response)" -ForegroundColor Yellow
} else {
    Write-Host "   ✅ MANDATORY failed as expected: No existing transaction" -ForegroundColor Green
    Write-Host "      Error: $($result.Error)" -ForegroundColor Gray
}
Show-BalanceChange -TestName "MANDATORY" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should NOT change"
Write-Host ""

# Test 6: NEVER
Write-Host "📋 Test 6: NEVER Propagation (Direct call - should work)" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "GET" -Url "$baseUrl/demo/never/$accountId"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ NEVER: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "NEVER" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should NOT change (read-only)"
} else {
    Write-Host "   ❌ NEVER failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test 7: NOT_SUPPORTED
Write-Host "📋 Test 7: NOT_SUPPORTED Propagation" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "GET" -Url "$baseUrl/demo/not-supported/$accountId"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ NOT_SUPPORTED: $($result.Response)" -ForegroundColor Green
    Show-BalanceChange -TestName "NOT_SUPPORTED" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should NOT change"
} else {
    Write-Host "   ❌ NOT_SUPPORTED failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# ==================== COMPLEX SCENARIO TESTS ====================

Write-Host "🔬 Step 3: Testing Complex Scenarios..." -ForegroundColor Blue
Write-Host ""

# Test Complex Scenario
Write-Host "📋 Test 8: Complex Scenario (Multiple Propagations)" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/complex-scenario/$accountId"
$afterBalance = Get-AccountBalance -AccountId $accountId

if ($result.Success) {
    Write-Host "   ✅ COMPLEX SCENARIO completed successfully" -ForegroundColor Green
    Write-Host "   📊 Result:" -ForegroundColor Cyan
    $result.Response -split "`n" | ForEach-Object { 
        if ($_.Trim()) { Write-Host "      $_" -ForegroundColor White }
    }
    Show-BalanceChange -TestName "COMPLEX SCENARIO" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "Balance should increase by 175"
} else {
    Write-Host "   ❌ COMPLEX SCENARIO failed: $($result.Error)" -ForegroundColor Red
}
Write-Host ""

# Test Rollback Scenario 1: Fail in REQUIRES_NEW
Write-Host "📋 Test 9: Rollback Scenario - Fail in REQUIRES_NEW" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/rollback-scenario/$accountId" -Body "failInRequiresNew=true&failInNested=false" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

Write-Host "   📊 ROLLBACK SCENARIO (REQUIRES_NEW fails):" -ForegroundColor Cyan
if ($result.Success) {
    Write-Host "   ✅ Parent transaction completed despite REQUIRES_NEW failure" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Parent transaction may have failed" -ForegroundColor Yellow
}

$result.Response -split "`n" | ForEach-Object { 
    if ($_.Trim()) { Write-Host "      $_" -ForegroundColor White }
}
Show-BalanceChange -TestName "ROLLBACK (REQUIRES_NEW fails)" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "REQUIRED should commit"
Write-Host ""

# Test Rollback Scenario 2: Fail in NESTED
Write-Host "📋 Test 10: Rollback Scenario - Fail in NESTED" -ForegroundColor Magenta
$beforeBalance = Get-AccountBalance -AccountId $accountId
$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/demo/rollback-scenario/$accountId" -Body "failInRequiresNew=false&failInNested=true" -ContentType "application/x-www-form-urlencoded"
$afterBalance = Get-AccountBalance -AccountId $accountId

Write-Host "   📊 ROLLBACK SCENARIO (NESTED fails):" -ForegroundColor Cyan
if ($result.Success) {
    Write-Host "   ✅ Parent transaction handled NESTED failure gracefully" -ForegroundColor Green
} else {
    Write-Host "   ⚠️  Parent transaction may have been affected by NESTED failure" -ForegroundColor Yellow
}

$result.Response -split "`n" | ForEach-Object { 
    if ($_.Trim()) { Write-Host "      $_" -ForegroundColor White }
}
Show-BalanceChange -TestName "ROLLBACK (NESTED fails)" -AccountId $accountId -BeforeBalance $beforeBalance -AfterBalance $afterBalance -ExpectedBehavior "REQUIRED should commit"
Write-Host ""

# ==================== COMPREHENSIVE TEST ====================

Write-Host "🎯 Step 4: Running Comprehensive Test..." -ForegroundColor Blue
Write-Host ""

$result = Invoke-ApiCall -Method "POST" -Url "$baseUrl/run-all-demos"

if ($result.Success) {
    Write-Host "✅ COMPREHENSIVE TEST completed successfully" -ForegroundColor Green
    Write-Host "📊 Results:" -ForegroundColor Cyan
    $result.Response -split "`n" | ForEach-Object { 
        if ($_.Trim()) { Write-Host "   $_" -ForegroundColor White }
    }
} else {
    Write-Host "❌ COMPREHENSIVE TEST failed: $($result.Error)" -ForegroundColor Red
}

Write-Host ""

# ==================== FINAL SUMMARY ====================

Write-Host "📊 Final Account Status:" -ForegroundColor Blue
$finalBalance = Get-AccountBalance -AccountId $accountId
if ($finalBalance -ne $null) {
    Write-Host "   Account ID: $accountId" -ForegroundColor White
    Write-Host "   Initial Balance: $initialBalance" -ForegroundColor White
    Write-Host "   Final Balance: $finalBalance" -ForegroundColor White
    $totalChange = $finalBalance - $initialBalance
    if ($totalChange -gt 0) {
        Write-Host "   Total Change: +$totalChange" -ForegroundColor Green
    } elseif ($totalChange -lt 0) {
        Write-Host "   Total Change: $totalChange" -ForegroundColor Red
    } else {
        Write-Host "   Total Change: $totalChange (No change)" -ForegroundColor Yellow
    }
}

Write-Host ""
Write-Host "=== PROPAGATION DEMO TEST COMPLETED ===" -ForegroundColor Cyan
Write-Host "📝 Key Observations:" -ForegroundColor Blue
Write-Host "   • REQUIRED: Uses existing transaction or creates new one" -ForegroundColor White
Write-Host "   • REQUIRES_NEW: Always creates new transaction, independent rollback" -ForegroundColor White
Write-Host "   • NESTED: Creates savepoint, can rollback independently" -ForegroundColor White
Write-Host "   • SUPPORTS: Uses transaction if available, otherwise non-transactional" -ForegroundColor White
Write-Host "   • MANDATORY: Requires existing transaction, fails if none" -ForegroundColor White
Write-Host "   • NEVER: Must not have transaction, fails if in transaction" -ForegroundColor White
Write-Host "   • NOT_SUPPORTED: Suspends transaction, runs non-transactionally" -ForegroundColor White
Write-Host ""
Write-Host "🔍 Check application logs for detailed transaction behavior analysis!" -ForegroundColor Yellow
Write-Host "📚 See documentation for best practices and use cases." -ForegroundColor Yellow 