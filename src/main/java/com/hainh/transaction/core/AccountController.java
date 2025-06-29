package com.hainh.transaction.core;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AccountController - REST API Controller cho Account operations
 * 
 * <p>Controller layer chịu trách nhiệm handle HTTP requests và responses.
 * Layer này không chứa business logic mà delegate tất cả operations
 * cho Service layer để maintain separation of concerns.</p>
 * 
 * <h3>Tại sao cần Controller Layer?</h3>
 * <ul>
 *   <li><strong>HTTP Protocol Handling</strong>: Convert HTTP requests thành Java method calls</li>
 *   <li><strong>Request/Response Mapping</strong>: Serialize/deserialize JSON ↔ Java objects</li>
 *   <li><strong>Exception Handling</strong>: Convert service exceptions thành HTTP error responses</li>
 *   <li><strong>Validation</strong>: Validate input parameters và request format</li>
 *   <li><strong>Documentation</strong>: Serve as API documentation cho client developers</li>
 * </ul>
 * 
 * <h3>RESTful API Design Principles:</h3>
 * <ul>
 *   <li><strong>GET</strong>: Retrieve data (idempotent, safe)</li>
 *   <li><strong>POST</strong>: Create new resources hoặc non-idempotent operations</li>
 *   <li><strong>PUT</strong>: Update existing resources (idempotent)</li>
 *   <li><strong>DELETE</strong>: Remove resources (idempotent)</li>
 * </ul>
 * 
 * <h3>Transaction Context:</h3>
 * <p>Controller methods KHÔNG có @Transactional annotations vì:</p>
 * <ul>
 *   <li>Transaction scope nên được define ở Service layer</li>
 *   <li>Controller chỉ handle HTTP concerns, không phải business logic</li>
 *   <li>Cho phép fine-grained transaction control ở service methods</li>
 * </ul>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@RestController // Combines @Controller + @ResponseBody - tự động serialize responses thành JSON
@RequestMapping("/api/accounts") // Base path cho tất cả endpoints trong controller này
public class AccountController {
    
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);
    
    /**
     * Service layer dependency để handle business logic
     * 
     * <p>Controller delegate tất cả business operations cho service layer.
     * Trong production, nên sử dụng constructor injection thay vì field injection.</p>
     */
    @Autowired
    private AccountService accountService;

    /**
     * Tạo tài khoản mới
     * 
     * <p>HTTP POST endpoint để tạo account mới. Client gửi JSON request body
     * chứa thông tin account, server response với account đã được tạo bao gồm ID.</p>
     * 
     * <h4>Request Example:</h4>
     * <pre>
     * POST /api/accounts
     * Content-Type: application/json
     * 
     * {
     *   "owner": "Nguyen Van A",
     *   "balance": 1000.0
     * }
     * </pre>
     * 
     * <h4>Response Example:</h4>
     * <pre>
     * HTTP 200 OK
     * Content-Type: application/json
     * 
     * {
     *   "id": 1,
     *   "owner": "Nguyen Van A", 
     *   "balance": 1000.0
     * }
     * </pre>
     * 
     * @param account Account object từ request body (JSON được auto-deserialize)
     * @return ResponseEntity chứa created account với HTTP 200 status
     * @throws IllegalArgumentException nếu input không hợp lệ (auto-converted to HTTP 400)
     */
    @PostMapping
    public ResponseEntity<Account> createAccount(@RequestBody Account account) {
        logger.info("Received request to create account for owner: {}", account.getOwner());
        
        try {
            // Delegate business logic cho service layer
            Account createdAccount = accountService.createAccount(account.getOwner(), account.getBalance());
            
            logger.info("Successfully created account with ID: {}", createdAccount.getId());
            
            // Return 200 OK với created account
            return ResponseEntity.ok(createdAccount);
            
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid input for account creation: {}", e.getMessage());
            // Spring sẽ tự động convert exception thành HTTP 400 Bad Request
            throw e;
        }
    }

    /**
     * Lấy thông tin tài khoản theo ID
     * 
     * <p>HTTP GET endpoint để retrieve account information. Sử dụng path variable
     * để specify account ID trong URL.</p>
     * 
     * <h4>Request Example:</h4>
     * <pre>
     * GET /api/accounts/1
     * </pre>
     * 
     * <h4>Response Examples:</h4>
     * <pre>
     * // Tìm thấy account:
     * HTTP 200 OK
     * {
     *   "id": 1,
     *   "owner": "Nguyen Van A",
     *   "balance": 1000.0
     * }
     * 
     * // Không tìm thấy account:
     * HTTP 404 Not Found
     * </pre>
     * 
     * @param id Account ID từ URL path
     * @return ResponseEntity với account nếu tìm thấy (200), hoặc 404 nếu không tìm thấy
     */
    @GetMapping("/{id}")
    public ResponseEntity<Account> getAccount(@PathVariable Long id) {
        logger.debug("Received request to get account with ID: {}", id);
        
        Optional<Account> account = accountService.getAccount(id);
        
        // Sử dụng Optional.map để elegant handling
        return account
            .map(acc -> {
                logger.debug("Found account: {}", acc);
                return ResponseEntity.ok(acc); // 200 OK với account data
            })
            .orElseGet(() -> {
                logger.warn("Account not found with ID: {}", id);
                return ResponseEntity.notFound().build(); // 404 Not Found
            });
    }

    /**
     * Lấy danh sách tất cả tài khoản
     * 
     * <p>HTTP GET endpoint để retrieve tất cả accounts trong hệ thống.
     * Trong production, nên implement pagination để tránh load quá nhiều data.</p>
     * 
     * <h4>Request Example:</h4>
     * <pre>
     * GET /api/accounts
     * </pre>
     * 
     * <h4>Response Example:</h4>
     * <pre>
     * HTTP 200 OK
     * [
     *   {
     *     "id": 1,
     *     "owner": "Nguyen Van A",
     *     "balance": 1000.0
     *   },
     *   {
     *     "id": 2,
     *     "owner": "Tran Thi B", 
     *     "balance": 500.0
     *   }
     * ]
     * </pre>
     * 
     * @return List của tất cả accounts (Spring tự động serialize thành JSON array)
     */
    @GetMapping
    public List<Account> getAllAccounts() {
        logger.debug("Received request to get all accounts");
        
        List<Account> accounts = accountService.getAllAccounts();
        logger.debug("Returning {} accounts", accounts.size());
        
        return accounts; // Spring tự động wrap trong ResponseEntity và serialize thành JSON
    }

    /**
     * Cập nhật thông tin tài khoản
     * 
     * <p>HTTP PUT endpoint để update existing account. Sử dụng PUT vì operation
     * này idempotent - multiple calls với same data sẽ có same result.</p>
     * 
     * <h4>Request Example:</h4>
     * <pre>
     * PUT /api/accounts/1
     * Content-Type: application/json
     * 
     * {
     *   "owner": "Nguyen Van A Updated",
     *   "balance": 1500.0
     * }
     * </pre>
     * 
     * @param id Account ID từ URL path
     * @param account Updated account data từ request body
     * @return ResponseEntity với updated account
     * @throws RuntimeException nếu account không tồn tại
     */
    @PutMapping("/{id}")
    public ResponseEntity<Account> updateAccount(@PathVariable Long id, @RequestBody Account account) {
        logger.info("Received request to update account ID: {} with data: {}", id, account);
        
        try {
            Account updatedAccount = accountService.updateAccount(id, account.getOwner(), account.getBalance());
            
            logger.info("Successfully updated account ID: {}", id);
            return ResponseEntity.ok(updatedAccount);
            
        } catch (RuntimeException e) {
            logger.warn("Failed to update account ID: {}, error: {}", id, e.getMessage());
            throw e; // Spring sẽ convert thành appropriate HTTP error status
        }
    }

    /**
     * Xóa tài khoản theo ID
     * 
     * <p>HTTP DELETE endpoint để remove account. Response 204 No Content
     * indicates successful deletion without returning data.</p>
     * 
     * <h4>Request Example:</h4>
     * <pre>
     * DELETE /api/accounts/1
     * </pre>
     * 
     * <h4>Response:</h4>
     * <pre>
     * HTTP 204 No Content
     * </pre>
     * 
     * @param id Account ID cần xóa
     * @return ResponseEntity với 204 No Content status
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        logger.info("Received request to delete account ID: {}", id);
        
        try {
            accountService.deleteAccount(id);
            
            logger.info("Successfully deleted account ID: {}", id);
            return ResponseEntity.noContent().build(); // 204 No Content
            
        } catch (RuntimeException e) {
            logger.warn("Failed to delete account ID: {}, error: {}", id, e.getMessage());
            throw e;
        }
    }

    /**
     * Chuyển tiền giữa hai tài khoản - CORE TRANSACTION ENDPOINT
     * 
     * <p>Đây là endpoint quan trọng nhất để demo transaction functionality.
     * Sử dụng POST vì operation này không idempotent - multiple calls
     * sẽ transfer money multiple times.</p>
     * 
     * <p><strong>Transaction Handling:</strong> Controller không có @Transactional
     * annotation. Transaction được handle ở Service layer (accountService.transfer()).
     * Điều này đảm bảo proper separation of concerns.</p>
     * 
     * <h4>Request Examples:</h4>
     * <pre>
     * // Successful transfer:
     * POST /api/accounts/transfer?fromId=1&toId=2&amount=200
     * 
     * // Failed transfer (insufficient balance):
     * POST /api/accounts/transfer?fromId=2&toId=1&amount=10000
     * </pre>
     * 
     * <h4>Response Examples:</h4>
     * <pre>
     * // Success:
     * HTTP 200 OK
     * "Transfer successful"
     * 
     * // Failure:
     * HTTP 400 Bad Request  
     * "Insufficient balance. Available: 500.0, Required: 10000.0"
     * </pre>
     * 
     * <h4>Error Scenarios:</h4>
     * <ul>
     *   <li>Source account không tồn tại → 400 Bad Request</li>
     *   <li>Destination account không tồn tại → 400 Bad Request</li>
     *   <li>Insufficient balance → 400 Bad Request</li>
     *   <li>Invalid amount (≤ 0) → 400 Bad Request</li>
     *   <li>Same fromId và toId → 400 Bad Request</li>
     * </ul>
     * 
     * @param fromId ID của tài khoản nguồn (query parameter)
     * @param toId ID của tài khoản đích (query parameter)
     * @param amount Số tiền cần chuyển (query parameter)
     * @return ResponseEntity với success message hoặc error message
     */
    @PostMapping("/transfer")
    public ResponseEntity<String> transfer(
            @RequestParam Long fromId, 
            @RequestParam Long toId, 
            @RequestParam Double amount) {
        
        logger.info("Received transfer request: {} -> {}, amount: {}", fromId, toId, amount);
        
        try {
            // Delegate transaction logic cho service layer
            // Service method có @Transactional annotation để handle transaction
            accountService.transfer(fromId, toId, amount);
            
            String successMessage = String.format("Transfer successful: %.2f transferred from account %d to account %d", 
                                                 amount, fromId, toId);
            logger.info("Transfer completed: {} -> {}, amount: {}", fromId, toId, amount);
            
            return ResponseEntity.ok(successMessage); // 200 OK
            
        } catch (IllegalArgumentException e) {
            // Input validation errors
            logger.warn("Invalid transfer parameters: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Invalid parameters: " + e.getMessage()); // 400 Bad Request
            
        } catch (RuntimeException e) {
            // Business logic errors (insufficient balance, account not found, etc.)
            logger.error("Transfer failed: {} -> {}, amount: {}, error: {}", 
                        fromId, toId, amount, e.getMessage());
            return ResponseEntity.badRequest().body("Transfer failed: " + e.getMessage()); // 400 Bad Request
            
        } catch (Exception e) {
            // Unexpected system errors
            logger.error("Unexpected error during transfer: {} -> {}, amount: {}", 
                        fromId, toId, amount, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("System error occurred. Please try again later."); // 500 Internal Server Error
        }
    }
    
    /**
     * Lấy danh sách tài khoản theo chủ sở hữu
     * 
     * <p>Demo endpoint để show custom repository query usage.</p>
     * 
     * @param owner Tên chủ sở hữu
     * @return Danh sách tài khoản của chủ sở hữu
     */
    @GetMapping("/by-owner")
    public List<Account> getAccountsByOwner(@RequestParam String owner) {
        logger.debug("Received request to get accounts for owner: {}", owner);
        return accountService.getAccountsByOwner(owner);
    }
    
    /**
     * Kiểm tra tài khoản có đủ số dư không
     * 
     * <p>Utility endpoint để check balance trước khi thực hiện transaction.</p>
     * 
     * @param accountId ID tài khoản
     * @param amount Số tiền cần kiểm tra
     * @return true nếu đủ số dư, false nếu không
     */
    @GetMapping("/{id}/sufficient-balance")
    public ResponseEntity<Boolean> checkSufficientBalance(
            @PathVariable("id") Long accountId, 
            @RequestParam Double amount) {
        
        logger.debug("Checking sufficient balance for account {}, required amount: {}", accountId, amount);
        
        boolean hasSufficientBalance = accountService.hasSufficientBalance(accountId, amount);
        return ResponseEntity.ok(hasSufficientBalance);
    }
} 