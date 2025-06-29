package com.hainh.transaction.core;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * AccountRepository - Data Access Layer cho Account Entity
 * 
 * <p>Interface này extend JpaRepository để cung cấp các CRUD operations
 * cơ bản cho Account entity. Spring Data JPA sẽ tự động tạo implementation
 * tại runtime, giúp giảm boilerplate code đáng kể.</p>
 * 
 * <h3>Tại sao sử dụng JpaRepository?</h3>
 * <ul>
 *   <li><strong>Automatic Implementation</strong>: Spring tự động tạo implementation</li>
 *   <li><strong>Built-in Methods</strong>: Có sẵn save(), findById(), findAll(), delete()...</li>
 *   <li><strong>Custom Queries</strong>: Hỗ trợ derived queries và @Query annotation</li>
 *   <li><strong>Transaction Integration</strong>: Tự động participate trong Spring transactions</li>
 *   <li><strong>Exception Translation</strong>: Convert database exceptions thành Spring exceptions</li>
 * </ul>
 * 
 * <h3>So sánh với JDBC truyền thống:</h3>
 * <pre>
 * // JDBC cũ - Nhiều boilerplate code
 * public Account findById(Long id) {
 *     String sql = "SELECT * FROM account WHERE id = ?";
 *     // PreparedStatement, ResultSet, exception handling...
 *     // 20-30 lines code
 * }
 * 
 * // Spring Data JPA - Chỉ cần khai báo
 * Optional&lt;Account&gt; findById(Long id); // Đã có sẵn từ JpaRepository!
 * </pre>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@Repository // Annotation này đánh dấu đây là Repository bean và enable exception translation
public interface AccountRepository extends JpaRepository<Account, Long> {
    
    // JpaRepository đã cung cấp các method cơ bản:
    // - save(Account) - Lưu hoặc update account
    // - findById(Long) - Tìm account theo ID
    // - findAll() - Lấy tất cả accounts
    // - deleteById(Long) - Xóa account theo ID
    // - count() - Đếm số lượng accounts
    // - existsById(Long) - Kiểm tra account có tồn tại không
    
    /**
     * Tìm tất cả tài khoản theo tên chủ sở hữu
     * 
     * <p>Đây là derived query - Spring Data JPA sẽ tự động parse method name
     * và generate SQL query tương ứng: SELECT * FROM account WHERE owner = ?</p>
     * 
     * @param owner Tên chủ tài khoản
     * @return Danh sách các tài khoản của chủ sở hữu này
     */
    List<Account> findByOwner(String owner);
    
    /**
     * Tìm tài khoản có số dư lớn hơn hoặc bằng số tiền chỉ định
     * 
     * <p>Derived query khác: WHERE balance >= ?</p>
     * 
     * @param balance Số dư tối thiểu
     * @return Danh sách tài khoản có đủ số dư
     */
    List<Account> findByBalanceGreaterThanEqual(Double balance);
    
    /**
     * Đếm số lượng tài khoản theo chủ sở hữu
     * 
     * @param owner Tên chủ sở hữu
     * @return Số lượng tài khoản
     */
    long countByOwner(String owner);
    
    /**
     * Custom query sử dụng JPQL để tìm tài khoản theo range số dư
     * 
     * <p>Khi derived query không đủ phức tạp, ta có thể sử dụng @Query
     * với JPQL (Java Persistence Query Language) hoặc native SQL.</p>
     * 
     * @param minBalance Số dư tối thiểu
     * @param maxBalance Số dư tối đa
     * @return Danh sách tài khoản trong range số dư
     */
    @Query("SELECT a FROM Account a WHERE a.balance BETWEEN :minBalance AND :maxBalance")
    List<Account> findAccountsByBalanceRange(@Param("minBalance") Double minBalance, 
                                           @Param("maxBalance") Double maxBalance);
    
    /**
     * Tìm tài khoản theo owner với case-insensitive search
     * 
     * <p>Sử dụng JPQL với UPPER function để search không phân biệt hoa thường</p>
     * 
     * @param owner Tên chủ tài khoản (không phân biệt hoa thường)
     * @return Optional chứa account nếu tìm thấy
     */
    @Query("SELECT a FROM Account a WHERE UPPER(a.owner) = UPPER(:owner)")
    Optional<Account> findByOwnerIgnoreCase(@Param("owner") String owner);
} 