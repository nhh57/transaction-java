package com.hainh.transaction.core;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * AccountService - Business Logic Layer cho Account operations
 * 
 * <p>Service layer chứa business logic và orchestrates các operations giữa
 * multiple entities. Đây là nơi chính để apply @Transactional annotations
 * để đảm bảo data consistency.</p>
 * 
 * <h3>Tại sao cần Service Layer?</h3>
 * <ul>
 *   <li><strong>Business Logic Separation</strong>: Tách biệt business rules khỏi presentation layer</li>
 *   <li><strong>Transaction Boundary</strong>: Định nghĩa transaction scope cho business operations</li>
 *   <li><strong>Reusability</strong>: Business logic có thể được reuse bởi multiple controllers</li>
 *   <li><strong>Testability</strong>: Dễ dàng unit test business logic riêng biệt</li>
 * </ul>
 * 
 * <h3>@Transactional hoạt động như thế nào?</h3>
 * <p>Spring sử dụng AOP (Aspect-Oriented Programming) để wrap service methods
 * với transaction management code. Khi method được gọi:</p>
 * <ol>
 *   <li>Spring tạo AOP proxy cho service instance</li>
 *   <li>Proxy intercepts method call</li>
 *   <li>Begin transaction trước khi gọi actual method</li>
 *   <li>Execute business logic</li>
 *   <li>Commit transaction nếu thành công</li>
 *   <li>Rollback transaction nếu có exception</li>
 * </ol>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@Service // Đánh dấu đây là Service bean, Spring sẽ tự động tạo instance
public class AccountService {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    
    /**
     * Repository để truy cập data layer
     * 
     * <p>Sử dụng @Autowired để Spring tự động inject AccountRepository instance.
     * Constructor injection sẽ tốt hơn field injection trong production.</p>
     */
    @Autowired
    private AccountRepository accountRepository;

    /**
     * Tạo tài khoản mới
     * 
     * <p>Method này không cần @Transactional vì chỉ thực hiện single operation.
     * JPA sẽ tự động wrap single save() operation trong transaction.</p>
     * 
     * <p><strong>Lưu ý:</strong> Trong thực tế, nên validate input parameters
     * và check business rules (ví dụ: balance >= 0, owner không null).</p>
     * 
     * @param owner Tên chủ tài khoản (không được null hoặc empty)
     * @param balance Số dư ban đầu (nên >= 0)
     * @return Account đã được tạo với ID được generate
     * @throws IllegalArgumentException nếu parameters không hợp lệ
     */
    public Account createAccount(String owner, Double balance) {
        logger.info("Creating new account for owner: {}, initial balance: {}", owner, balance);
        
        // Validate input - Best practice trong production
        if (owner == null || owner.trim().isEmpty()) {
            throw new IllegalArgumentException("Owner name cannot be null or empty");
        }
        if (balance == null || balance < 0) {
            throw new IllegalArgumentException("Initial balance must be non-negative");
        }
        
        Account newAccount = new Account(owner, balance);
        Account savedAccount = accountRepository.save(newAccount);
        
        logger.info("Successfully created account with ID: {}", savedAccount.getId());
        return savedAccount;
    }

    /**
     * Lấy thông tin tài khoản theo ID
     * 
     * <p>Read-only operation, không cần transaction. Sử dụng Optional để
     * handle trường hợp không tìm thấy account một cách elegant.</p>
     * 
     * @param id ID của tài khoản
     * @return Optional chứa Account nếu tìm thấy, empty nếu không
     */
    public Optional<Account> getAccount(Long id) {
        logger.debug("Fetching account with ID: {}", id);
        return accountRepository.findById(id);
    }

    /**
     * Lấy danh sách tất cả tài khoản
     * 
     * <p>Read-only operation. Trong production, nên implement phân trang
     * để tránh load quá nhiều data cùng lúc.</p>
     * 
     * @return Danh sách tất cả tài khoản
     */
    public List<Account> getAllAccounts() {
        logger.debug("Fetching all accounts");
        return accountRepository.findAll();
    }

    /**
     * Cập nhật thông tin tài khoản
     * 
     * <p>Sử dụng @Transactional để đảm bảo update operation được thực hiện
     * trong transaction context. Nếu có lỗi, changes sẽ được rollback.</p>
     * 
     * @param id ID của tài khoản cần update
     * @param owner Tên chủ tài khoản mới
     * @param balance Số dư mới
     * @return Account đã được update
     * @throws RuntimeException nếu không tìm thấy account
     */
    @Transactional // Cần transaction để đảm bảo consistency
    public Account updateAccount(Long id, String owner, Double balance) {
        logger.info("Updating account ID: {}, new owner: {}, new balance: {}", id, owner, balance);
        
        Account account = accountRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Account not found with ID: " + id));
        
        // Update fields
        account.setOwner(owner);
        account.setBalance(balance);
        
        // JPA dirty checking sẽ tự động detect changes và update database
        Account updatedAccount = accountRepository.save(account);
        
        logger.info("Successfully updated account ID: {}", id);
        return updatedAccount;
    }

    /**
     * Xóa tài khoản theo ID
     * 
     * <p>Delete operation cần transaction để đảm bảo data consistency.
     * Trong thực tế, nên implement soft delete thay vì hard delete.</p>
     * 
     * @param id ID của tài khoản cần xóa
     */
    @Transactional
    public void deleteAccount(Long id) {
        logger.info("Deleting account with ID: {}", id);
        
        // Kiểm tra account có tồn tại không
        if (!accountRepository.existsById(id)) {
            throw new RuntimeException("Account not found with ID: " + id);
        }
        
        accountRepository.deleteById(id);
        logger.info("Successfully deleted account ID: {}", id);
    }

    /**
     * Chuyển tiền giữa hai tài khoản - CORE TRANSACTION DEMO
     * 
     * <p>Đây là method quan trọng nhất để demo transaction mechanism.
     * Method này thực hiện multiple database operations và phải đảm bảo
     * ACID properties:</p>
     * 
     * <ul>
     *   <li><strong>Atomicity</strong>: Hoặc cả hai accounts được update, hoặc không có gì thay đổi</li>
     *   <li><strong>Consistency</strong>: Tổng số tiền trong hệ thống không đổi</li>
     *   <li><strong>Isolation</strong>: Transaction này không bị ảnh hưởng bởi concurrent transactions</li>
     *   <li><strong>Durability</strong>: Sau khi commit, changes được lưu vĩnh viễn</li>
     * </ul>
     * 
     * <h4>Transaction Configuration:</h4>
     * <ul>
     *   <li><strong>Propagation.REQUIRED</strong>: Sử dụng existing transaction hoặc tạo mới</li>
     *   <li><strong>Isolation.READ_COMMITTED</strong>: Tránh dirty reads</li>
     *   <li><strong>rollbackFor = Exception.class</strong>: Rollback cho mọi exception</li>
     * </ul>
     * 
     * <h4>Rollback Scenarios:</h4>
     * <ul>
     *   <li>Account không tồn tại</li>
     *   <li>Số dư không đủ</li>
     *   <li>Database connection error</li>
     *   <li>Bất kỳ RuntimeException nào khác</li>
     * </ul>
     * 
     * @param fromId ID tài khoản nguồn (sẽ bị trừ tiền)
     * @param toId ID tài khoản đích (sẽ được cộng tiền)
     * @param amount Số tiền cần chuyển (phải > 0)
     * @throws RuntimeException nếu có lỗi trong quá trình chuyển tiền
     */
    @Transactional(
        propagation = Propagation.REQUIRED,    // Yêu cầu transaction
        isolation = Isolation.READ_COMMITTED,  // Isolation level để tránh dirty reads
        rollbackFor = Exception.class,         // Rollback cho mọi exception
        timeout = 30                          // Timeout sau 30 giây
    )
    public void transfer(Long fromId, Long toId, Double amount) {
        logger.info("Starting money transfer: {} -> {}, amount: {}", fromId, toId, amount);
        
        // Validate input parameters
        if (fromId == null || toId == null) {
            throw new IllegalArgumentException("Account IDs cannot be null");
        }
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Transfer amount must be positive");
        }
        if (fromId.equals(toId)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }
        
        // Fetch source account - Nếu không tìm thấy, transaction sẽ rollback
        Account fromAccount = accountRepository.findById(fromId)
            .orElseThrow(() -> new RuntimeException("Source account not found with ID: " + fromId));
        
        // Fetch destination account - Nếu không tìm thấy, transaction sẽ rollback  
        Account toAccount = accountRepository.findById(toId)
            .orElseThrow(() -> new RuntimeException("Destination account not found with ID: " + toId));
        
        logger.debug("Before transfer - From account balance: {}, To account balance: {}", 
                    fromAccount.getBalance(), toAccount.getBalance());
        
        // Check sufficient balance - Nếu không đủ, transaction sẽ rollback
        if (fromAccount.getBalance() < amount) {
            logger.warn("Insufficient balance for transfer. Required: {}, Available: {}", 
                       amount, fromAccount.getBalance());
            throw new RuntimeException("Insufficient balance. Available: " + fromAccount.getBalance() + 
                                     ", Required: " + amount);
        }
        
        // Perform the actual transfer - Đây là critical section
        // Cả hai operations phải thành công, nếu một trong hai fail thì rollback
        Double originalFromBalance = fromAccount.getBalance();
        Double originalToBalance = toAccount.getBalance();
        
        try {
            // Debit from source account
            fromAccount.setBalance(fromAccount.getBalance() - amount);
            accountRepository.save(fromAccount);
            logger.debug("Debited {} from account {}, new balance: {}", 
                        amount, fromId, fromAccount.getBalance());
            
            // Credit to destination account  
            toAccount.setBalance(toAccount.getBalance() + amount);
            accountRepository.save(toAccount);
            logger.debug("Credited {} to account {}, new balance: {}", 
                        amount, toId, toAccount.getBalance());
            
            // Simulate potential error after partial completion
            // Uncomment dòng dưới để test rollback mechanism:
            // if (amount > 100) throw new RuntimeException("Simulated error for testing rollback");
            
            logger.info("Transfer completed successfully: {} -> {}, amount: {}", fromId, toId, amount);
            logger.debug("After transfer - From account balance: {}, To account balance: {}", 
                        fromAccount.getBalance(), toAccount.getBalance());
            
        } catch (Exception e) {
            // Log error trước khi transaction rollback
            logger.error("Error during transfer, transaction will rollback. From: {} -> To: {}, Amount: {}", 
                        fromId, toId, amount, e);
            
            // Spring sẽ tự động rollback transaction khi có exception
            // Không cần manually restore balances
            throw e; // Re-throw để Spring có thể handle rollback
        }
    }
    
    /**
     * Lấy danh sách tài khoản theo chủ sở hữu
     * 
     * <p>Sử dụng custom repository method để demo derived queries.</p>
     * 
     * @param owner Tên chủ sở hữu
     * @return Danh sách tài khoản của chủ sở hữu này
     */
    public List<Account> getAccountsByOwner(String owner) {
        logger.debug("Fetching accounts for owner: {}", owner);
        return accountRepository.findByOwner(owner);
    }
    
    /**
     * Kiểm tra tài khoản có đủ số dư để thực hiện giao dịch không
     * 
     * @param accountId ID tài khoản
     * @param requiredAmount Số tiền cần thiết
     * @return true nếu đủ số dư, false nếu không
     */
    public boolean hasSufficientBalance(Long accountId, Double requiredAmount) {
        Optional<Account> account = accountRepository.findById(accountId);
        return account.map(acc -> acc.getBalance() >= requiredAmount).orElse(false);
    }
} 