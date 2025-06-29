package com.hainh.transaction.propagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hainh.transaction.core.Account;

/**
 * PropagationDemoController - REST API để test Transaction Propagation behavior
 * 
 * <p>Controller này cung cấp các endpoints để test và demo các loại propagation
 * khác nhau và cách chúng ảnh hưởng đến transaction cha/con:</p>
 * 
 * <ul>
 *   <li><strong>Individual Propagation Tests</strong>: Test từng loại propagation riêng lẻ</li>
 *   <li><strong>Nested Scenarios</strong>: Test transaction lồng nhau với propagation khác nhau</li>
 *   <li><strong>Rollback Scenarios</strong>: Test rollback behavior với mixed propagations</li>
 *   <li><strong>Complex Scenarios</strong>: Test scenarios phức tạp với multiple propagations</li>
 * </ul>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@RestController
@RequestMapping("/api/propagation-demo")
public class PropagationDemoController {
    
    private static final Logger logger = LoggerFactory.getLogger(PropagationDemoController.class);
    
    @Autowired
    private PropagationDemoService demoService;

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

    // ==================== INDIVIDUAL PROPAGATION DEMOS ====================
    
    /**
     * Demo REQUIRED propagation
     */
    @PostMapping("/demo/required/{accountId}")
    public ResponseEntity<String> demoRequired(
            @PathVariable Long accountId,
            @RequestParam Double amount) {
        
        logger.info("=== DEMO: REQUIRED propagation ===");
        
        try {
            String result = demoService.demonstrateRequired(accountId, amount);
            return ResponseEntity.ok("REQUIRED DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in REQUIRED demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo REQUIRES_NEW propagation
     */
    @PostMapping("/demo/requires-new/{accountId}")
    public ResponseEntity<String> demoRequiresNew(
            @PathVariable Long accountId,
            @RequestParam Double amount) {
        
        logger.info("=== DEMO: REQUIRES_NEW propagation ===");
        
        try {
            String result = demoService.demonstrateRequiresNew(accountId, amount);
            return ResponseEntity.ok("REQUIRES_NEW DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in REQUIRES_NEW demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo NESTED propagation
     */
    @PostMapping("/demo/nested/{accountId}")
    public ResponseEntity<String> demoNested(
            @PathVariable Long accountId,
            @RequestParam Double amount) {
        
        logger.info("=== DEMO: NESTED propagation ===");
        
        try {
            String result = demoService.demonstrateNested(accountId, amount);
            return ResponseEntity.ok("NESTED DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in NESTED demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo SUPPORTS propagation
     */
    @GetMapping("/demo/supports/{accountId}")
    public ResponseEntity<String> demoSupports(@PathVariable Long accountId) {
        
        logger.info("=== DEMO: SUPPORTS propagation ===");
        
        try {
            String result = demoService.demonstrateSupports(accountId);
            return ResponseEntity.ok("SUPPORTS DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in SUPPORTS demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo MANDATORY propagation
     */
    @PostMapping("/demo/mandatory/{accountId}")
    public ResponseEntity<String> demoMandatory(
            @PathVariable Long accountId,
            @RequestParam Double amount) {
        
        logger.info("=== DEMO: MANDATORY propagation ===");
        
        try {
            String result = demoService.demonstrateMandatory(accountId, amount);
            return ResponseEntity.ok("MANDATORY DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in MANDATORY demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("MANDATORY ERROR: " + e.getMessage() + 
                    "\n(This happens when MANDATORY is called without existing transaction)");
        }
    }
    
    /**
     * Demo NEVER propagation
     */
    @GetMapping("/demo/never/{accountId}")
    public ResponseEntity<String> demoNever(@PathVariable Long accountId) {
        
        logger.info("=== DEMO: NEVER propagation ===");
        
        try {
            String result = demoService.demonstrateNever(accountId);
            return ResponseEntity.ok("NEVER DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in NEVER demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("NEVER ERROR: " + e.getMessage() + 
                    "\n(This happens when NEVER is called within existing transaction)");
        }
    }
    
    /**
     * Demo NOT_SUPPORTED propagation
     */
    @GetMapping("/demo/not-supported/{accountId}")
    public ResponseEntity<String> demoNotSupported(@PathVariable Long accountId) {
        
        logger.info("=== DEMO: NOT_SUPPORTED propagation ===");
        
        try {
            String result = demoService.demonstrateNotSupported(accountId);
            return ResponseEntity.ok("NOT_SUPPORTED DEMO: " + result);
            
        } catch (Exception e) {
            logger.error("Error in NOT_SUPPORTED demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    // ==================== COMPLEX SCENARIO DEMOS ====================
    
    /**
     * Demo complex scenario với multiple propagations
     */
    @PostMapping("/demo/complex-scenario/{accountId}")
    public ResponseEntity<String> demoComplexScenario(@PathVariable Long accountId) {
        
        logger.info("=== DEMO: COMPLEX SCENARIO with multiple propagations ===");
        
        try {
            String result = demoService.demonstrateComplexScenario(accountId);
            return ResponseEntity.ok("COMPLEX SCENARIO DEMO:\n" + result + 
                    "\n\nCheck logs to see different propagation behaviors!");
            
        } catch (Exception e) {
            logger.error("Error in COMPLEX SCENARIO demo: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }
    
    /**
     * Demo rollback scenario với mixed propagations
     */
    @PostMapping("/demo/rollback-scenario/{accountId}")
    public ResponseEntity<String> demoRollbackScenario(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "false") boolean failInRequiresNew,
            @RequestParam(defaultValue = "false") boolean failInNested) {
        
        logger.info("=== DEMO: ROLLBACK SCENARIO with mixed propagations ===");
        
        try {
            String result = demoService.demonstrateRollbackScenario(accountId, failInRequiresNew, failInNested);
            return ResponseEntity.ok("ROLLBACK SCENARIO DEMO:\n" + result + 
                    "\n\nCheck logs and account balance to see rollback behaviors!");
            
        } catch (Exception e) {
            logger.error("Error in ROLLBACK SCENARIO demo: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED)
                    .body("ROLLBACK SCENARIO DEMO:\nParent transaction failed: " + e.getMessage() + 
                          "\n\nCheck logs and account balance to see what was rolled back!");
        }
    }

    // ==================== COMPREHENSIVE TEST ENDPOINT ====================
    
    /**
     * Chạy tất cả propagation demos một cách tuần tự
     */
    @PostMapping("/run-all-demos")
    public ResponseEntity<String> runAllDemos() {
        logger.info("=== RUNNING ALL PROPAGATION DEMOS ===");
        
        StringBuilder results = new StringBuilder();
        results.append("=== PROPAGATION COMPREHENSIVE DEMO RESULTS ===\n\n");
        
        try {
            // Setup test account
            Account testAccount = demoService.createTestAccount("Propagation Demo User", 1000.0);
            Long accountId = testAccount.getId();
            results.append(String.format("1. Created test account: ID=%d, Balance=%.2f\n\n", 
                    accountId, testAccount.getBalance()));
            
            // Test individual propagations
            results.append("2. INDIVIDUAL PROPAGATION TESTS:\n");
            
            try {
                String requiredResult = demoService.demonstrateRequired(accountId, 100.0);
                results.append("   ✅ REQUIRED completed\n");
            } catch (Exception e) {
                results.append(String.format("   ❌ REQUIRED failed: %s\n", e.getMessage()));
            }
            
            try {
                String requiresNewResult = demoService.demonstrateRequiresNew(accountId, 50.0);
                results.append("   ✅ REQUIRES_NEW completed\n");
            } catch (Exception e) {
                results.append(String.format("   ❌ REQUIRES_NEW failed: %s\n", e.getMessage()));
            }
            
            try {
                String complexResult = demoService.demonstrateComplexScenario(accountId);
                results.append("   ✅ COMPLEX SCENARIO completed\n");
            } catch (Exception e) {
                results.append(String.format("   ❌ COMPLEX SCENARIO failed: %s\n", e.getMessage()));
            }
            
            results.append("\n=== ALL PROPAGATION DEMOS COMPLETED ===\n");
            results.append("Check application logs for detailed propagation behavior analysis!");
            
            return ResponseEntity.ok(results.toString());
            
        } catch (Exception e) {
            logger.error("Error running comprehensive demos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error running demos: " + e.getMessage());
        }
    }
} 