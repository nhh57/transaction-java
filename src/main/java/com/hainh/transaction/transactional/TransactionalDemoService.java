package com.hainh.transaction.transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hainh.transaction.core.Account;
import com.hainh.transaction.core.AccountRepository;

/**
 * TransactionalDemoService - Demo chi tiết về @Transactional behavior
 * 
 * <p>Service này được thiết kế để minh họa các khía cạnh quan trọng của @Transactional:</p>
 * <ul>
 *   <li><strong>Transaction Boundaries</strong>: Khi nào transaction bắt đầu và kết thúc</li>
 *   <li><strong>Rollback Behavior</strong>: Checked vs Unchecked exceptions</li>
 *   <li><strong>Self-Invocation Problem</strong>: Tại sao gọi method trong cùng class không work</li>
 *   <li><strong>Proxy Mechanism</strong>: Cách Spring AOP intercept method calls</li>
 * </ul>
 * 
 * <h3>Quan trọng - Transaction Boundaries:</h3>
 * <p>@Transactional chỉ hoạt động khi:</p>
 * <ul>
 *   <li>Method được gọi từ BÊN NGOÀI class (qua Spring proxy)</li>
 *   <li>Method có visibility là PUBLIC</li>
 *   <li>Method không được gọi từ chính class đó (self-invocation)</li>
 * </ul>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@Service
public class TransactionalDemoService {
    
    private static final Logger logger = LoggerFactory.getLogger(TransactionalDemoService.class);
    
    @Autowired
    private AccountRepository accountRepository;

    // ==================== DEMO 1: TRANSACTION BOUNDARIES ====================
    
    /**
     * Method PUBLIC với @Transactional - SẼ HOẠT ĐỘNG
     * 
     * <p>Khi method này được gọi từ bên ngoài (Controller, Service khác), 
     * Spring AOP proxy sẽ intercept và tạo transaction.</p>
     * 
     * <h4>Cách hoạt động:</h4>
     * <ol>
     *   <li>Client gọi method → Spring Proxy intercepts</li>
     *   <li>Proxy bắt đầu transaction</li>
     *   <li>Proxy gọi actual method</li>
     *   <li>Method thực thi logic</li>
     *   <li>Proxy commit transaction (nếu thành công)</li>
     * </ol>
     * 
     * @param accountId ID của account cần update
     * @param newBalance Balance mới
     * @return Account đã được update
     */
    @Transactional
    public Account updateBalanceWithTransaction(Long accountId, Double newBalance) {
        logger.info("=== DEMO 1: PUBLIC method with @Transactional ===");
        logger.info("Transaction STARTED for updateBalanceWithTransaction");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(newBalance);
        
        Account savedAccount = accountRepository.save(account);
        
        logger.info("Updated account {} balance: {} → {}", accountId, oldBalance, newBalance);
        logger.info("Transaction will COMMIT when method ends");
        
        return savedAccount;
    }
    
    /**
     * Method PRIVATE với @Transactional - KHÔNG HOẠT ĐỘNG
     * 
     * <p><strong>Lưu ý quan trọng:</strong> Method private không thể được proxy,
     * do đó @Transactional annotation sẽ bị IGNORE hoàn toàn!</p>
     * 
     * <p>Đây là một trong những lỗi phổ biến nhất khi sử dụng @Transactional.</p>
     */
    @Transactional // ← Annotation này sẽ bị IGNORE!
    private Account updateBalancePrivateMethod(Long accountId, Double newBalance) {
        logger.warn("=== DEMO: PRIVATE method with @Transactional - IGNORED! ===");
        logger.warn("This method runs WITHOUT transaction context!");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        account.setBalance(newBalance);
        return accountRepository.save(account);
    }

    // ==================== DEMO 2: SELF-INVOCATION PROBLEM ====================
    
    /**
     * Demo Self-Invocation Problem - Gọi method @Transactional từ chính class
     * 
     * <p>Đây là một pitfall cực kỳ phổ biến! Khi method này được gọi từ bên ngoài,
     * chỉ có method này được wrap trong transaction. Khi nó gọi 
     * internalTransactionalMethod(), call đó sẽ KHÔNG qua proxy!</p>
     * 
     * <h4>Vấn đề:</h4>
     * <ul>
     *   <li>externalMethod() có transaction</li>
     *   <li>internalTransactionalMethod() KHÔNG có transaction riêng</li>
     *   <li>Cả hai cùng chạy trong 1 transaction của externalMethod()</li>
     * </ul>
     */
    @Transactional
    public String demonstrateSelfInvocation() {
        logger.info("=== DEMO 2: SELF-INVOCATION PROBLEM ===");
        logger.info("externalMethod() - Transaction STARTED");
        
        // Tạo account để test
        Account testAccount = new Account("Self-Invocation Test", 1000.0);
        testAccount = accountRepository.save(testAccount);
        
        logger.info("Created test account: {}", testAccount);
        
        // SELF-INVOCATION - Gọi method @Transactional từ chính class này
        // Method call này sẽ KHÔNG qua Spring proxy!
        try {
            internalTransactionalMethod(testAccount.getId());
            return "Self-invocation completed - Check logs to see transaction behavior";
        } catch (Exception e) {
            logger.error("Exception in self-invocation demo: {}", e.getMessage());
            throw e; // Re-throw để trigger rollback
        }
    }
    
    /**
     * Method này sẽ KHÔNG có transaction riêng khi được gọi từ demonstrateSelfInvocation()
     * 
     * <p>Mặc dù có @Transactional annotation, nhưng khi được gọi từ method khác
     * trong cùng class, nó sẽ KHÔNG qua Spring proxy, do đó không có transaction context riêng.</p>
     */
    @Transactional
    public void internalTransactionalMethod(Long accountId) {
        logger.warn("internalTransactionalMethod() - This should have its own transaction...");
        logger.warn("But it DOESN'T because of self-invocation!");
        logger.warn("It runs in the same transaction as the calling method");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        account.setBalance(account.getBalance() + 100);
        accountRepository.save(account);
        
        logger.info("Updated account balance to: {}", account.getBalance());
    }

    // ==================== DEMO 3: EXCEPTION & ROLLBACK BEHAVIOR ====================
    
    /**
     * Demo Unchecked Exception → Automatic Rollback
     * 
     * <p>RuntimeException và các subclass sẽ tự động trigger rollback.
     * Đây là behavior mặc định của Spring @Transactional.</p>
     */
    @Transactional
    public String demonstrateUncheckedException(Long accountId) {
        logger.info("=== DEMO 3A: UNCHECKED EXCEPTION - AUTOMATIC ROLLBACK ===");
        logger.info("Transaction STARTED");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Double originalBalance = account.getBalance();
        logger.info("Original balance: {}", originalBalance);
        
        // Thực hiện thay đổi
        account.setBalance(originalBalance + 500);
        accountRepository.save(account);
        logger.info("Balance updated to: {}", account.getBalance());
        
        // Ném RuntimeException → Sẽ trigger ROLLBACK
        logger.error("Throwing RuntimeException - Transaction will ROLLBACK!");
        throw new RuntimeException("Simulated error - This will cause ROLLBACK");
    }
    
    /**
     * Demo Checked Exception → NO Rollback (by default)
     * 
     * <p>Checked exceptions KHÔNG tự động trigger rollback trong Spring.
     * Đây là một điểm khác biệt quan trọng so với unchecked exceptions.</p>
     */
    @Transactional
    public String demonstrateCheckedException(Long accountId) throws Exception {
        logger.info("=== DEMO 3B: CHECKED EXCEPTION - NO ROLLBACK (by default) ===");
        logger.info("Transaction STARTED");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Double originalBalance = account.getBalance();
        logger.info("Original balance: {}", originalBalance);
        
        // Thực hiện thay đổi
        account.setBalance(originalBalance + 300);
        accountRepository.save(account);
        logger.info("Balance updated to: {}", account.getBalance());
        
        // Ném Checked Exception → KHÔNG trigger rollback
        logger.warn("Throwing checked Exception - Transaction will COMMIT!");
        logger.warn("Changes will be PERSISTED despite the exception!");
        throw new Exception("Checked exception - This will NOT cause rollback");
    }
    
    /**
     * Demo Custom Rollback Rules - Force rollback for checked exceptions
     * 
     * <p>Sử dụng rollbackFor để force rollback cho specific exceptions,
     * bao gồm cả checked exceptions.</p>
     */
    @Transactional(rollbackFor = Exception.class) // Rollback cho TẤT CẢ exceptions
    public String demonstrateCustomRollbackRules(Long accountId) throws Exception {
        logger.info("=== DEMO 3C: CUSTOM ROLLBACK RULES - Force rollback for checked exceptions ===");
        logger.info("Transaction STARTED with rollbackFor = Exception.class");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Double originalBalance = account.getBalance();
        logger.info("Original balance: {}", originalBalance);
        
        // Thực hiện thay đổi
        account.setBalance(originalBalance + 200);
        accountRepository.save(account);
        logger.info("Balance updated to: {}", account.getBalance());
        
        // Ném Checked Exception → SẼ trigger rollback vì có rollbackFor = Exception.class
        logger.error("Throwing checked Exception with rollbackFor - Transaction will ROLLBACK!");
        throw new Exception("Checked exception with custom rollback rule - This WILL cause rollback");
    }

    // ==================== DEMO 4: NO ROLLBACK RULES ====================
    
    /**
     * Demo noRollbackFor - Prevent rollback for specific exceptions
     * 
     * <p>Sử dụng noRollbackFor để prevent rollback cho specific exceptions,
     * ngay cả khi đó là unchecked exceptions.</p>
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String demonstrateNoRollbackRules(Long accountId) {
        logger.info("=== DEMO 4: NO ROLLBACK RULES - Prevent rollback for specific exceptions ===");
        logger.info("Transaction STARTED with noRollbackFor = IllegalArgumentException.class");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        Double originalBalance = account.getBalance();
        logger.info("Original balance: {}", originalBalance);
        
        // Thực hiện thay đổi
        account.setBalance(originalBalance + 150);
        accountRepository.save(account);
        logger.info("Balance updated to: {}", account.getBalance());
        
        // Ném IllegalArgumentException → KHÔNG trigger rollback vì có noRollbackFor
        logger.warn("Throwing IllegalArgumentException with noRollbackFor - Transaction will COMMIT!");
        throw new IllegalArgumentException("This unchecked exception will NOT cause rollback due to noRollbackFor");
    }

    // ==================== DEMO 5: REALISTIC BUSINESS VALIDATION SCENARIOS ====================
    
    /**
     * Demo realistic business validation scenarios
     * 
     * <p>Minh họa các trường hợp thực tế khi IllegalArgumentException và ValidationException xảy ra
     * trong business logic và cách chúng ảnh hưởng đến transaction behavior.</p>
     */
    @Transactional
    public String demonstrateBusinessValidation(Long fromAccountId, Long toAccountId, Double amount) {
        logger.info("=== DEMO 5: REALISTIC BUSINESS VALIDATION ===");
        logger.info("Transfer request: {} -> {}, amount: {}", fromAccountId, toAccountId, amount);
        
        // 1. Input validation - IllegalArgumentException
        if (fromAccountId == null || toAccountId == null) {
            logger.error("Account IDs cannot be null");
            throw new IllegalArgumentException("Account IDs cannot be null");
        }
        
        if (amount == null || amount <= 0) {
            logger.error("Invalid amount: {}", amount);
            throw new IllegalArgumentException("Amount must be positive, got: " + amount);
        }
        
        if (fromAccountId.equals(toAccountId)) {
            logger.error("Cannot transfer to same account: {}", fromAccountId);
            throw new IllegalArgumentException("Cannot transfer to same account");
        }
        
        // 2. Data validation - IllegalArgumentException
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> {
                logger.error("From account not found: {}", fromAccountId);
                return new IllegalArgumentException("From account not found: " + fromAccountId);
            });
            
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> {
                logger.error("To account not found: {}", toAccountId);
                return new IllegalArgumentException("To account not found: " + toAccountId);
            });
        
        // 3. Business rule validation - Custom ValidationException hoặc RuntimeException
        if (fromAccount.getBalance() < amount) {
            logger.error("Insufficient balance: {} < {}", fromAccount.getBalance(), amount);
            throw new RuntimeException("Insufficient balance: " + fromAccount.getBalance() + " < " + amount);
        }
        
        // 4. Business validation - ValidationException (if we had it)
        if (amount > 10000.0) {
            logger.error("Transfer amount exceeds daily limit: {}", amount);
            // Trong thực tế, đây có thể là ValidationException
            throw new RuntimeException("Transfer amount exceeds daily limit: " + amount);
        }
        
        // 5. Perform the transfer
        logger.info("Performing transfer: {} -> {}, amount: {}", fromAccountId, toAccountId, amount);
        fromAccount.setBalance(fromAccount.getBalance() - amount);
        toAccount.setBalance(toAccount.getBalance() + amount);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        logger.info("Transfer completed successfully");
        return String.format("Transfer completed: %.2f from account %d to account %d", 
                amount, fromAccountId, toAccountId);
    }
    
    /**
     * Demo với noRollbackFor cho validation errors
     * 
     * <p>Trong một số trường hợp, bạn có thể muốn log validation errors nhưng không rollback
     * transaction nếu đã có một số thay đổi quan trọng cần preserve.</p>
     */
    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public String demonstrateValidationWithNoRollback(Long accountId, String newOwnerName) {
        logger.info("=== DEMO 5B: VALIDATION WITH NO ROLLBACK ===");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        // Log the access for audit purposes (this should be preserved)
        logger.info("Account {} accessed for owner name update", accountId);
        
        // Update access timestamp (in real scenario, this might be important to keep)
        // account.setLastAccessTime(LocalDateTime.now());
        // accountRepository.save(account); // This change should be preserved
        
        // Validation that we don't want to rollback for
        if (newOwnerName == null || newOwnerName.trim().isEmpty()) {
            logger.warn("Invalid owner name provided: '{}'", newOwnerName);
            throw new IllegalArgumentException("Owner name cannot be empty");
        }
        
        if (newOwnerName.length() > 100) {
            logger.warn("Owner name too long: {} characters", newOwnerName.length());
            throw new IllegalArgumentException("Owner name cannot exceed 100 characters");
        }
        
        // If validation passes, update the name
        account.setOwner(newOwnerName);
        accountRepository.save(account);
        
        return "Owner name updated successfully";
    }

    // ==================== UTILITY METHODS ====================
    
    /**
     * Tạo test account cho demos
     */
    public Account createTestAccount(String owner, Double balance) {
        logger.info("Creating test account: owner={}, balance={}", owner, balance);
        Account account = new Account(owner, balance);
        return accountRepository.save(account);
    }
    
    /**
     * Lấy account để kiểm tra balance sau các demos
     */
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }
    
    /**
     * Method để gọi internalTransactionalMethod từ BÊN NGOÀI class
     * 
     * <p>Khi method này được gọi từ Controller, internalTransactionalMethod()
     * sẽ có transaction riêng vì được gọi qua Spring proxy.</p>
     */
    public void callInternalMethodExternally(Long accountId) {
        logger.info("=== DEMO: Calling internalTransactionalMethod from EXTERNAL call ===");
        logger.info("This time it WILL have its own transaction!");
        internalTransactionalMethod(accountId);
    }
} 