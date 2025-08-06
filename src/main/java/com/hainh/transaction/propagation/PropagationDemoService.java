package com.hainh.transaction.propagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hainh.transaction.core.Account;
import com.hainh.transaction.core.AccountRepository;

/**
 * PropagationDemoService - Demo chi tiết về Transaction Propagation
 * 
 * <p>Service này được thiết kế để minh họa các loại propagation khác nhau và cách chúng
 * ảnh hưởng đến transaction cha/con:</p>
 * 
 * <ul>
 *   <li><strong>REQUIRED</strong>: Sử dụng transaction hiện tại hoặc tạo mới</li>
 *   <li><strong>REQUIRES_NEW</strong>: Luôn tạo transaction mới, suspend transaction hiện tại</li>
 *   <li><strong>NESTED</strong>: Tạo nested transaction (savepoint)</li>
 *   <li><strong>SUPPORTS</strong>: Sử dụng transaction nếu có, không thì chạy non-transactional</li>
 *   <li><strong>MANDATORY</strong>: Yêu cầu phải có transaction, không thì throw exception</li>
 *   <li><strong>NEVER</strong>: Không được có transaction, có thì throw exception</li>
 *   <li><strong>NOT_SUPPORTED</strong>: Suspend transaction hiện tại, chạy non-transactional</li>
 * </ul>
 * 
 * <h3>Quan trọng - Propagation Behavior:</h3>
 * <p>Propagation quyết định cách transaction con relate với transaction cha:</p>
 * <ul>
 *   <li><strong>Same Transaction</strong>: REQUIRED, SUPPORTS, MANDATORY</li>
 *   <li><strong>New Transaction</strong>: REQUIRES_NEW, NESTED</li>
 *   <li><strong>No Transaction</strong>: NEVER, NOT_SUPPORTED</li>
 * </ul>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@Service
public class PropagationDemoService {
    
    private static final Logger logger = LoggerFactory.getLogger(PropagationDemoService.class);
    
    @Autowired
    private AccountRepository accountRepository;
    
    // Note: We don't need self-injection for this demo as we're not calling methods from within the same class

    // ==================== UTILITY METHODS ====================
    
    /**
     * Utility method để log transaction status
     */
    private void logTransactionStatus(String methodName) {
        boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        
        logger.info("=== {} ===", methodName);
        logger.info("In Transaction: {}", inTransaction);
        logger.info("Transaction Name: {}", transactionName);
        logger.info("Read Only: {}", readOnly);
        logger.info("Transaction Isolation: {}", 
                TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
    }

    // ==================== DEMO 1: REQUIRED (Default) ====================
    
    /**
     * REQUIRED - Sử dụng transaction hiện tại, hoặc tạo mới nếu không có
     * 
     * <p>Đây là propagation mặc định. Nếu đã có transaction, sử dụng transaction đó.
     * Nếu chưa có, tạo transaction mới.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Có transaction cha → Tham gia vào transaction cha</li>
     *   <li>Không có transaction cha → Tạo transaction mới</li>
     *   <li>Rollback → Rollback toàn bộ transaction (bao gồm cả cha)</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String demonstrateRequired(Long accountId, Double amount) {
        logTransactionStatus("PROPAGATION.REQUIRED");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("REQUIRED: Updated balance {} → {}", oldBalance, account.getBalance());
        return String.format("REQUIRED: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }
    
    /**
     * Nested call với REQUIRED - cùng transaction
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String nestedRequired(Long accountId, Double amount) {
        logTransactionStatus("NESTED REQUIRED");
        
        // Gọi method khác cũng có REQUIRED
        String result1 = demonstrateRequired(accountId, amount);
        
        // Thêm logic nữa
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        logger.info("NESTED REQUIRED: Final balance: {}", account.getBalance());
        return result1 + " | NESTED: Same transaction used";
    }

    // ==================== DEMO 2: REQUIRES_NEW ====================
    
    /**
     * REQUIRES_NEW - Luôn tạo transaction mới, suspend transaction hiện tại
     * 
     * <p>Propagation này luôn tạo transaction mới, bất kể có transaction cha hay không.
     * Transaction cha sẽ bị suspend cho đến khi transaction con hoàn thành.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Luôn tạo transaction mới</li>
     *   <li>Transaction cha bị suspend</li>
     *   <li>Rollback → Chỉ rollback transaction con, không ảnh hưởng transaction cha</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String demonstrateRequiresNew(Long accountId, Double amount) {
        logTransactionStatus("PROPAGATION.REQUIRES_NEW");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("REQUIRES_NEW: Updated balance {} → {}", oldBalance, account.getBalance());
        return String.format("REQUIRES_NEW: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }
    
    /**
     * Test REQUIRES_NEW với exception để demo independent rollback
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String demonstrateRequiresNewWithException(Long accountId, Double amount, boolean shouldFail) {
        logTransactionStatus("REQUIRES_NEW WITH EXCEPTION");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("REQUIRES_NEW: Updated balance {} → {}", oldBalance, account.getBalance());
        
        if (shouldFail) {
            logger.error("REQUIRES_NEW: Throwing exception - only this transaction will rollback!");
            throw new RuntimeException("REQUIRES_NEW transaction failed!");
        }
        
        return String.format("REQUIRES_NEW: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }

    // ==================== DEMO 3: NESTED ====================
    
    /**
     * NESTED - Tạo nested transaction (savepoint)
     * 
     * <p>Tạo nested transaction sử dụng savepoint. Nếu transaction con rollback,
     * chỉ rollback về savepoint, transaction cha vẫn có thể continue.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Tạo savepoint trong transaction hiện tại</li>
     *   <li>Rollback → Rollback về savepoint, transaction cha không bị ảnh hưởng</li>
     *   <li>Nếu không có transaction cha → Hoạt động như REQUIRED</li>
     * </ul>
     * 
     * <p><strong>Lưu ý:</strong> Không phải tất cả database đều support nested transaction!</p>
     */
    @Transactional(propagation = Propagation.NESTED)
    public String demonstrateNested(Long accountId, Double amount) {
        logTransactionStatus("PROPAGATION.NESTED");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("NESTED: Updated balance {} → {}", oldBalance, account.getBalance());
        return String.format("NESTED: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }
    
    /**
     * Test NESTED với exception
     */
    @Transactional(propagation = Propagation.NESTED)
    public String demonstrateNestedWithException(Long accountId, Double amount, boolean shouldFail) {
        logTransactionStatus("NESTED WITH EXCEPTION");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("NESTED: Updated balance {} → {}", oldBalance, account.getBalance());
        
        if (shouldFail) {
            logger.error("NESTED: Throwing exception - will rollback to savepoint!");
            throw new RuntimeException("NESTED transaction failed!");
        }
        
        return String.format("NESTED: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }

    // ==================== DEMO 4: SUPPORTS ====================
    
    /**
     * SUPPORTS - Sử dụng transaction nếu có, không thì chạy non-transactional
     * 
     * <p>Nếu có transaction hiện tại, tham gia vào transaction đó.
     * Nếu không có transaction, chạy non-transactional.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Có transaction cha → Tham gia vào transaction cha</li>
     *   <li>Không có transaction cha → Chạy non-transactional</li>
     *   <li>Thường dùng cho read-only operations</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
    public String demonstrateSupports(Long accountId) {
        logTransactionStatus("PROPAGATION.SUPPORTS");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        logger.info("SUPPORTS: Reading account balance: {}", account.getBalance());
        return String.format("SUPPORTS: Account balance is %.2f", account.getBalance());
    }

    // ==================== DEMO 5: MANDATORY ====================
    
    /**
     * MANDATORY - Yêu cầu phải có transaction, không thì throw exception
     * 
     * <p>Method này yêu cầu phải được gọi trong context của một transaction.
     * Nếu không có transaction, sẽ throw IllegalTransactionStateException.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Có transaction cha → Tham gia vào transaction cha</li>
     *   <li>Không có transaction cha → Throw IllegalTransactionStateException</li>
     *   <li>Dùng để enforce transaction requirement</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public String demonstrateMandatory(Long accountId, Double amount) {
        logTransactionStatus("PROPAGATION.MANDATORY");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        Double oldBalance = account.getBalance();
        account.setBalance(oldBalance + amount);
        accountRepository.save(account);
        
        logger.info("MANDATORY: Updated balance {} → {}", oldBalance, account.getBalance());
        return String.format("MANDATORY: Balance updated from %.2f to %.2f", oldBalance, account.getBalance());
    }

    // ==================== DEMO 6: NEVER ====================
    
    /**
     * NEVER - Không được có transaction, có thì throw exception
     * 
     * <p>Method này không được gọi trong context của transaction.
     * Nếu có transaction, sẽ throw IllegalTransactionStateException.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Có transaction cha → Throw IllegalTransactionStateException</li>
     *   <li>Không có transaction cha → Chạy non-transactional</li>
     *   <li>Dùng cho operations không cần transaction</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.NEVER)
    public String demonstrateNever(Long accountId) {
        logTransactionStatus("PROPAGATION.NEVER");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        logger.info("NEVER: Reading account balance (non-transactional): {}", account.getBalance());
        return String.format("NEVER: Account balance is %.2f (read non-transactionally)", account.getBalance());
    }

    // ==================== DEMO 7: NOT_SUPPORTED ====================
    
    /**
     * NOT_SUPPORTED - Suspend transaction hiện tại, chạy non-transactional
     * 
     * <p>Suspend transaction hiện tại (nếu có) và chạy method non-transactional.
     * Transaction sẽ được resume sau khi method hoàn thành.</p>
     * 
     * <h4>Behavior:</h4>
     * <ul>
     *   <li>Có transaction cha → Suspend transaction cha, chạy non-transactional</li>
     *   <li>Không có transaction cha → Chạy non-transactional</li>
     *   <li>Dùng cho operations không muốn ảnh hưởng bởi transaction</li>
     * </ul>
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public String demonstrateNotSupported(Long accountId) {
        logTransactionStatus("PROPAGATION.NOT_SUPPORTED");
        
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
        
        logger.info("NOT_SUPPORTED: Reading account balance (suspended transaction): {}", account.getBalance());
        return String.format("NOT_SUPPORTED: Account balance is %.2f (transaction suspended)", account.getBalance());
    }

    // ==================== COMPLEX SCENARIO DEMOS ====================

    /**
     * Demo complex scenario: Parent transaction với multiple child transactions
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String demonstrateComplexScenario(Long accountId) {
        logTransactionStatus("COMPLEX SCENARIO - PARENT");
        
        StringBuilder results = new StringBuilder();
        results.append("COMPLEX SCENARIO RESULTS:\n");
        
        try {
            // 1. REQUIRED - same transaction
            String result1 = demonstrateRequired(accountId, 100.0);
            results.append("1. ").append(result1).append("\n");
            
            // 2. REQUIRES_NEW - new transaction
            String result2 = demonstrateRequiresNew(accountId, 50.0);
            results.append("2. ").append(result2).append("\n");
            
            // 3. NESTED - nested transaction
            String result3 = demonstrateNested(accountId, 25.0);
            results.append("3. ").append(result3).append("\n");
            
            // 4. SUPPORTS - same transaction
            String result4 = demonstrateSupports(accountId);
            results.append("4. ").append(result4).append("\n");
            
            logger.info("COMPLEX SCENARIO: All operations completed successfully");
            
        } catch (Exception e) {
            logger.error("COMPLEX SCENARIO: Error occurred - {}", e.getMessage());
            results.append("ERROR: ").append(e.getMessage()).append("\n");
            throw e; // Re-throw to trigger rollback
        }
        
        return results.toString();
    }
    
    /**
     * Demo rollback scenario với mixed propagations
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public String demonstrateRollbackScenario(Long accountId, boolean failInRequiresNew, boolean failInNested) {
        logTransactionStatus("ROLLBACK SCENARIO - PARENT");
        
        StringBuilder results = new StringBuilder();
        results.append("ROLLBACK SCENARIO:\n");
        
        try {
            // 1. REQUIRED - same transaction
            String result1 = demonstrateRequired(accountId, 100.0);
            results.append("1. ").append(result1).append("\n");
            
            // 2. REQUIRES_NEW - independent transaction
            try {
                String result2 = demonstrateRequiresNewWithException(accountId, 50.0, failInRequiresNew);
                results.append("2. ").append(result2).append("\n");
            } catch (Exception e) {
                logger.warn("REQUIRES_NEW failed but parent continues: {}", e.getMessage());
                results.append("2. REQUIRES_NEW FAILED (independent): ").append(e.getMessage()).append("\n");
            }
            
            // 3. NESTED - can be rolled back to savepoint
            try {
                String result3 = demonstrateNestedWithException(accountId, 25.0, failInNested);
                results.append("3. ").append(result3).append("\n");
            } catch (Exception e) {
                logger.warn("NESTED failed but parent can continue: {}", e.getMessage());
                results.append("3. NESTED FAILED (rollback to savepoint): ").append(e.getMessage()).append("\n");
                // In real scenario, parent could handle this and continue
            }
            
            logger.info("ROLLBACK SCENARIO: Completed with mixed results");
            
        } catch (Exception e) {
            logger.error("ROLLBACK SCENARIO: Parent transaction failed - {}", e.getMessage());
            results.append("PARENT FAILED: ").append(e.getMessage()).append("\n");
            throw e; // Re-throw to trigger parent rollback
        }
        
        return results.toString();
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
     * Lấy account để kiểm tra balance
     */
    public Account getAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found: " + accountId));
    }
} 