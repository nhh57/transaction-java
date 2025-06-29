package com.hainh.transaction.transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hainh.transaction.core.Account;

/**
 * TransactionalDemoController - REST API để test @Transactional behavior
 * 
 * <p>Controller này cung cấp các endpoints để test và demo các khía cạnh
 * khác nhau của @Transactional annotation:</p>
 * 
 * <ul>
 *   <li><strong>Transaction Boundaries</strong>: Public vs Private methods</li>
 *   <li><strong>Self-Invocation Problem</strong>: Internal vs External calls</li>
 *   <li><strong>Exception Handling</strong>: Checked vs Unchecked exceptions</li>
 *   <li><strong>Rollback Rules</strong>: rollbackFor và noRollbackFor</li>
 * </ul>
 * 
 * <h3>Cách sử dụng:</h3>
 * <ol>
 *   <li>Tạo test account bằng POST /api/transactional-demo/setup</li>
 *   <li>Chạy các demo scenarios</li>
 *   <li>Check logs để thấy transaction behavior</li>
 *   <li>Verify account balance để confirm rollback/commit</li>
 * </ol>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@RestController
@RequestMapping("/api/transactional-demo")
public class TransactionalDemoController {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionalDemoController.class);
    
    @Autowired
    private TransactionalDemoService demoService;

    // ==================== SETUP & UTILITY ENDPOINTS ====================
    
    /**
     * Tạo test account cho demos
     */
    @PostMapping("/setup")
    public ResponseEntity<Account> setupTestAccount(@RequestBody Account account) {
        logger.info("Setting up test account: {}", account.getOwner());
        
        Account createdAccount = demoService.createTestAccount(account.getOwner(), account.getBalance());
        
        logger.info("Created test account with ID: {}", createdAccount.getId());
        return ResponseEntity.ok(createdAccount);
    }
    
    /**
     * Lấy thông tin account để verify balance changes
     */
    @GetMapping("/account/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        logger.info("Fetching account with ID: {}", id);
        
        try {
            Account account = demoService.getAccount(id);
            return ResponseEntity.ok(account);
        } catch (RuntimeException e) {
            logger.error("Account not found: {}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ==================== DEMO 1: TRANSACTION BOUNDARIES ====================
    
    /**
     * Demo 1: Public method với @Transactional
     */
    @PutMapping("/demo1/public-method/{accountId}")
    public ResponseEntity<String> demoPublicMethodTransaction(
            @PathVariable Long accountId, 
            @RequestParam Double newBalance) {
        
        logger.info("=== DEMO 1: Testing PUBLIC method with @Transactional ===");
        logger.info("Account ID: {}, New Balance: {}", accountId, newBalance);
        
        try {
            Account updatedAccount = demoService.updateBalanceWithTransaction(accountId, newBalance);
            
            String response = String.format(
                "SUCCESS: Public method transaction worked correctly. " +
                "Account %d balance updated to %.2f", 
                updatedAccount.getId(), updatedAccount.getBalance()
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error in public method demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ==================== DEMO 2: SELF-INVOCATION PROBLEM ====================
    
    /**
     * Demo 2A: Self-Invocation Problem
     */
    @PostMapping("/demo2/self-invocation")
    public ResponseEntity<String> demoSelfInvocation() {
        logger.info("=== DEMO 2A: Testing SELF-INVOCATION problem ===");
        
        try {
            String result = demoService.demonstrateSelfInvocation();
            return ResponseEntity.ok("SELF-INVOCATION DEMO: " + result + 
                    "\n\nCheck logs to see that internalTransactionalMethod() does NOT have its own transaction!");
            
        } catch (Exception e) {
            logger.error("Error in self-invocation demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo 2B: External call to internal method
     */
    @PostMapping("/demo2/external-call/{accountId}")
    public ResponseEntity<String> demoExternalCall(@PathVariable Long accountId) {
        logger.info("=== DEMO 2B: Testing EXTERNAL call to @Transactional method ===");
        logger.info("Calling internalTransactionalMethod() from Controller (external)");
        
        try {
            demoService.callInternalMethodExternally(accountId);
            
            return ResponseEntity.ok("EXTERNAL CALL DEMO: Method called externally - " +
                    "Check logs to see that internalTransactionalMethod() HAS its own transaction!");
            
        } catch (Exception e) {
            logger.error("Error in external call demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ==================== DEMO 3: EXCEPTION & ROLLBACK BEHAVIOR ====================
    
    /**
     * Demo 3A: Unchecked Exception → Automatic Rollback
     */
    @PostMapping("/demo3/unchecked-exception/{accountId}")
    public ResponseEntity<String> demoUncheckedException(@PathVariable Long accountId) {
        logger.info("=== DEMO 3A: Testing UNCHECKED EXCEPTION rollback ===");
        
        Account beforeAccount = demoService.getAccount(accountId);
        Double balanceBefore = beforeAccount.getBalance();
        logger.info("Balance BEFORE test: {}", balanceBefore);
        
        try {
            demoService.demonstrateUncheckedException(accountId);
            return ResponseEntity.ok("This should never be reached!");
            
        } catch (RuntimeException e) {
            Account afterAccount = demoService.getAccount(accountId);
            Double balanceAfter = afterAccount.getBalance();
            
            logger.info("Balance AFTER exception: {}", balanceAfter);
            
            String response = String.format(
                "UNCHECKED EXCEPTION DEMO: Exception thrown - %s\n" +
                "Balance BEFORE: %.2f\n" +
                "Balance AFTER: %.2f\n" +
                "ROLLBACK %s: %s",
                e.getMessage(),
                balanceBefore,
                balanceAfter,
                balanceBefore.equals(balanceAfter) ? "SUCCESS" : "FAILED",
                balanceBefore.equals(balanceAfter) ? "Transaction was rolled back!" : "Transaction was NOT rolled back!"
            );
            
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }
    
    /**
     * Demo 3B: Checked Exception → NO Rollback (by default)
     */
    @PostMapping("/demo3/checked-exception/{accountId}")
    public ResponseEntity<String> demoCheckedException(@PathVariable Long accountId) {
        logger.info("=== DEMO 3B: Testing CHECKED EXCEPTION (no rollback) ===");
        
        Account beforeAccount = demoService.getAccount(accountId);
        Double balanceBefore = beforeAccount.getBalance();
        logger.info("Balance BEFORE test: {}", balanceBefore);
        
        try {
            demoService.demonstrateCheckedException(accountId);
            return ResponseEntity.ok("This should never be reached!");
            
        } catch (Exception e) {
            Account afterAccount = demoService.getAccount(accountId);
            Double balanceAfter = afterAccount.getBalance();
            
            logger.info("Balance AFTER exception: {}", balanceAfter);
            
            String response = String.format(
                "CHECKED EXCEPTION DEMO: Exception thrown - %s\n" +
                "Balance BEFORE: %.2f\n" +
                "Balance AFTER: %.2f\n" +
                "NO ROLLBACK %s: %s",
                e.getMessage(),
                balanceBefore,
                balanceAfter,
                !balanceBefore.equals(balanceAfter) ? "SUCCESS" : "UNEXPECTED",
                !balanceBefore.equals(balanceAfter) ? "Transaction was committed despite exception!" : "Unexpected rollback occurred!"
            );
            
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }
    
    /**
     * Demo 3C: Custom Rollback Rules
     */
    @PostMapping("/demo3/custom-rollback/{accountId}")
    public ResponseEntity<String> demoCustomRollbackRules(@PathVariable Long accountId) {
        logger.info("=== DEMO 3C: Testing CUSTOM ROLLBACK RULES ===");
        
        Account beforeAccount = demoService.getAccount(accountId);
        Double balanceBefore = beforeAccount.getBalance();
        logger.info("Balance BEFORE test: {}", balanceBefore);
        
        try {
            demoService.demonstrateCustomRollbackRules(accountId);
            return ResponseEntity.ok("This should never be reached!");
            
        } catch (Exception e) {
            Account afterAccount = demoService.getAccount(accountId);
            Double balanceAfter = afterAccount.getBalance();
            
            logger.info("Balance AFTER exception: {}", balanceAfter);
            
            String response = String.format(
                "CUSTOM ROLLBACK DEMO: Exception thrown - %s\n" +
                "Balance BEFORE: %.2f\n" +
                "Balance AFTER: %.2f\n" +
                "ROLLBACK %s: %s",
                e.getMessage(),
                balanceBefore,
                balanceAfter,
                balanceBefore.equals(balanceAfter) ? "SUCCESS" : "FAILED",
                balanceBefore.equals(balanceAfter) ? "Custom rollback rule worked!" : "Custom rollback rule failed!"
            );
            
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }

    // ==================== DEMO 4: NO ROLLBACK RULES ====================
    
    /**
     * Demo 4: noRollbackFor rules
     */
    @PostMapping("/demo4/no-rollback/{accountId}")
    public ResponseEntity<String> demoNoRollbackRules(@PathVariable Long accountId) {
        logger.info("=== DEMO 4: Testing NO ROLLBACK RULES ===");
        
        Account beforeAccount = demoService.getAccount(accountId);
        Double balanceBefore = beforeAccount.getBalance();
        logger.info("Balance BEFORE test: {}", balanceBefore);
        
        try {
            demoService.demonstrateNoRollbackRules(accountId);
            return ResponseEntity.ok("This should never be reached!");
            
        } catch (IllegalArgumentException e) {
            Account afterAccount = demoService.getAccount(accountId);
            Double balanceAfter = afterAccount.getBalance();
            
            logger.info("Balance AFTER exception: {}", balanceAfter);
            
            String response = String.format(
                "NO ROLLBACK DEMO: Exception thrown - %s\n" +
                "Balance BEFORE: %.2f\n" +
                "Balance AFTER: %.2f\n" +
                "NO ROLLBACK %s: %s",
                e.getMessage(),
                balanceBefore,
                balanceAfter,
                !balanceBefore.equals(balanceAfter) ? "SUCCESS" : "FAILED",
                !balanceBefore.equals(balanceAfter) ? "noRollbackFor rule worked!" : "Unexpected rollback occurred!"
            );
            
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(response);
        }
    }

    // ==================== DEMO 5: REALISTIC BUSINESS VALIDATION SCENARIOS ====================
    
    /**
     * Demo 5A: Realistic business validation
     */
    @PostMapping("/demo5/business-validation")
    public ResponseEntity<String> demoBusinessValidation(
            @RequestParam Long fromAccountId,
            @RequestParam Long toAccountId,
            @RequestParam Double amount) {
        
        logger.info("=== DEMO 5A: Testing REALISTIC BUSINESS VALIDATION ===");
        logger.info("Transfer: {} -> {}, amount: {}", fromAccountId, toAccountId, amount);
        
        try {
            String result = demoService.demonstrateBusinessValidation(fromAccountId, toAccountId, amount);
            return ResponseEntity.ok("BUSINESS VALIDATION DEMO: " + result);
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("VALIDATION ERROR: " + e.getMessage() + 
                    "\n(This caused ROLLBACK because IllegalArgumentException is unchecked)");
                    
        } catch (RuntimeException e) {
            logger.error("Business rule error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("BUSINESS ERROR: " + e.getMessage() + 
                    "\n(This caused ROLLBACK because RuntimeException is unchecked)");
        }
    }
    
    /**
     * Demo 5B: Validation with noRollbackFor
     */
    @PostMapping("/demo5/validation-no-rollback/{accountId}")
    public ResponseEntity<String> demoValidationNoRollback(
            @PathVariable Long accountId,
            @RequestParam String newOwnerName) {
        
        logger.info("=== DEMO 5B: Testing VALIDATION with NO ROLLBACK ===");
        logger.info("Update owner name for account {}: '{}'", accountId, newOwnerName);
        
        try {
            String result = demoService.demonstrateValidationWithNoRollback(accountId, newOwnerName);
            return ResponseEntity.ok("VALIDATION NO ROLLBACK DEMO: " + result);
            
        } catch (IllegalArgumentException e) {
            logger.error("Validation error (no rollback): {}", e.getMessage());
            return ResponseEntity.badRequest().body("VALIDATION ERROR (NO ROLLBACK): " + e.getMessage() + 
                    "\n(This did NOT cause rollback due to noRollbackFor=IllegalArgumentException.class)");
                    
        } catch (RuntimeException e) {
            logger.error("Runtime error: {}", e.getMessage());
            return ResponseEntity.badRequest().body("RUNTIME ERROR: " + e.getMessage() + 
                    "\n(This caused ROLLBACK because it's not in noRollbackFor list)");
        }
    }

    // ==================== COMPREHENSIVE TEST ENDPOINT ====================
    
    /**
     * Chạy tất cả demos một cách tuần tự
     */
    @PostMapping("/run-all-demos")
    public ResponseEntity<String> runAllDemos() {
        logger.info("=== RUNNING ALL @TRANSACTIONAL DEMOS ===");
        
        StringBuilder results = new StringBuilder();
        results.append("=== @TRANSACTIONAL COMPREHENSIVE DEMO RESULTS ===\n\n");
        
        try {
            // 1. Setup test account
            Account testAccount = demoService.createTestAccount("Demo User", 1000.0);
            Long accountId = testAccount.getId();
            results.append(String.format("1. Created test account: ID=%d, Balance=%.2f\n\n", 
                    accountId, testAccount.getBalance()));
            
            // 2. Demo public method transaction
            results.append("2. PUBLIC METHOD TRANSACTION:\n");
            try {
                Account updated = demoService.updateBalanceWithTransaction(accountId, 1100.0);
                results.append(String.format("   ✅ SUCCESS: Balance updated to %.2f\n\n", updated.getBalance()));
            } catch (Exception e) {
                results.append(String.format("   ❌ FAILED: %s\n\n", e.getMessage()));
            }
            
            // 3. Demo self-invocation
            results.append("3. SELF-INVOCATION PROBLEM:\n");
            try {
                String selfResult = demoService.demonstrateSelfInvocation();
                results.append("   ✅ COMPLETED: Check logs to see transaction behavior\n\n");
            } catch (Exception e) {
                results.append(String.format("   ❌ FAILED: %s\n\n", e.getMessage()));
            }
            
            results.append("\n=== ALL DEMOS COMPLETED ===\n");
            results.append("Check application logs for detailed transaction behavior analysis!");
            
            return ResponseEntity.ok(results.toString());
            
        } catch (Exception e) {
            logger.error("Error running comprehensive demos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error running demos: " + e.getMessage());
        }
    }
} 