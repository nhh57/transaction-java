# Transaction API Test Script

Write-Host "=== Testing Transaction API ===" -ForegroundColor Green

# 1. Create Account A
Write-Host "`n1. Creating Account A..." -ForegroundColor Yellow
$accountA = @{
    owner = "Nguyen Van A"
    balance = 1000.0
} | ConvertTo-Json

try {
    $responseA = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts" -Method POST -Headers @{"Content-Type"="application/json"} -Body $accountA
    Write-Host "Account A created: $($responseA | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Host "Error creating Account A: $($_.Exception.Message)" -ForegroundColor Red
}

# 2. Create Account B
Write-Host "`n2. Creating Account B..." -ForegroundColor Yellow
$accountB = @{
    owner = "Tran Thi B"
    balance = 500.0
} | ConvertTo-Json

try {
    $responseB = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts" -Method POST -Headers @{"Content-Type"="application/json"} -Body $accountB
    Write-Host "Account B created: $($responseB | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Host "Error creating Account B: $($_.Exception.Message)" -ForegroundColor Red
}

# 3. Get all accounts
Write-Host "`n3. Getting all accounts..." -ForegroundColor Yellow
try {
    $allAccounts = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts" -Method GET
    Write-Host "All accounts: $($allAccounts | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Host "Error getting accounts: $($_.Exception.Message)" -ForegroundColor Red
}

# 4. Transfer money successfully
Write-Host "`n4. Transferring 200 from Account 1 to Account 2..." -ForegroundColor Yellow
try {
    $transferResult = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts/transfer?fromId=1&toId=2&amount=200" -Method POST
    Write-Host "Transfer successful: $transferResult" -ForegroundColor Green
} catch {
    Write-Host "Error in transfer: $($_.Exception.Message)" -ForegroundColor Red
}

# 5. Check accounts after successful transfer
Write-Host "`n5. Checking accounts after successful transfer..." -ForegroundColor Yellow
try {
    $accountsAfterTransfer = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts" -Method GET
    Write-Host "Accounts after transfer: $($accountsAfterTransfer | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Host "Error getting accounts: $($_.Exception.Message)" -ForegroundColor Red
}

# 6. Transfer money - should fail (insufficient balance)
Write-Host "`n6. Attempting to transfer 1000 from Account 2 to Account 1 (should fail)..." -ForegroundColor Yellow
try {
    $failedTransfer = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts/transfer?fromId=2&toId=1&amount=1000" -Method POST
    Write-Host "Unexpected success: $failedTransfer" -ForegroundColor Red
} catch {
    Write-Host "Expected failure: $($_.Exception.Message)" -ForegroundColor Green
}

# 7. Check accounts after failed transfer (should be unchanged)
Write-Host "`n7. Checking accounts after failed transfer (should be unchanged)..." -ForegroundColor Yellow
try {
    $accountsAfterFailedTransfer = Invoke-RestMethod -Uri "http://localhost:1122/api/accounts" -Method GET
    Write-Host "Accounts after failed transfer: $($accountsAfterFailedTransfer | ConvertTo-Json)" -ForegroundColor Green
} catch {
    Write-Host "Error getting accounts: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Green 