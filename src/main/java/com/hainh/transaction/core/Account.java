package com.hainh.transaction.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Account Entity - Đại diện cho một tài khoản ngân hàng trong hệ thống
 * 
 * <p>Đây là JPA Entity được map với bảng 'account' trong database.
 * Entity này được sử dụng để thực hiện các transaction demo như chuyển tiền,
 * giúp minh họa cơ chế hoạt động của @Transactional trong Spring.</p>
 * 
 * <h3>Tại sao sử dụng JPA Entity?</h3>
 * <ul>
 *   <li><strong>Object-Relational Mapping</strong>: Tự động map giữa Java object và database table</li>
 *   <li><strong>Type Safety</strong>: Compile-time checking thay vì raw SQL strings</li>
 *   <li><strong>Automatic Persistence</strong>: JPA tự động handle INSERT/UPDATE/DELETE</li>
 *   <li><strong>Transaction Integration</strong>: Hoạt động seamlessly với Spring @Transactional</li>
 * </ul>
 * 
 * @author hainh Development Team
 * @version 1.0
 * @since 2025-06-27
 */
@Entity
@Table(name = "account") // Explicit table name để rõ ràng
public class Account {
    
    /**
     * Primary Key - ID duy nhất cho mỗi tài khoản
     * 
     * <p>Sử dụng IDENTITY strategy để database tự động generate ID.
     * Điều này đảm bảo mỗi tài khoản có ID unique và không bị conflict
     * trong môi trường concurrent.</p>
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    /**
     * Tên chủ tài khoản
     * 
     * <p>Field này lưu trữ thông tin định danh của chủ tài khoản.
     * Trong thực tế, đây có thể là foreign key tới bảng Customer,
     * nhưng để đơn giản hóa demo, ta sử dụng String.</p>
     */
    @Column(name = "owner", nullable = false, length = 100)
    private String owner;
    
    /**
     * Số dư tài khoản
     * 
     * <p>Đây là field quan trọng nhất trong transaction demo.
     * Mọi thao tác chuyển tiền đều liên quan đến việc thay đổi balance.
     * Sử dụng Double cho đơn giản, trong thực tế nên dùng BigDecimal
     * để tránh floating point precision issues.</p>
     * 
     * <strong>Lưu ý:</strong> Field này phải được protected bởi transaction
     * để tránh race condition khi nhiều user cùng thao tác.
     */
    @Column(name = "balance", nullable = false)
    private Double balance;

    /**
     * Default constructor - Bắt buộc cho JPA
     * 
     * <p>JPA yêu cầu entity phải có no-arg constructor để có thể
     * instantiate object từ database result set.</p>
     */
    public Account() {
        // Constructor rỗng cho JPA
    }

    /**
     * Constructor với tham số - Để tạo tài khoản mới
     * 
     * @param owner Tên chủ tài khoản
     * @param balance Số dư ban đầu
     */
    public Account(String owner, Double balance) {
        this.owner = owner;
        this.balance = balance;
    }

    // Getter và Setter methods với JavaDoc

    /**
     * Lấy ID của tài khoản
     * @return ID duy nhất của tài khoản
     */
    public Long getId() { 
        return id; 
    }
    
    /**
     * Set ID cho tài khoản (thường không cần thiết vì auto-generated)
     * @param id ID mới
     */
    public void setId(Long id) { 
        this.id = id; 
    }

    /**
     * Lấy tên chủ tài khoản
     * @return Tên chủ tài khoản
     */
    public String getOwner() { 
        return owner; 
    }
    
    /**
     * Set tên chủ tài khoản
     * @param owner Tên chủ tài khoản mới
     */
    public void setOwner(String owner) { 
        this.owner = owner; 
    }

    /**
     * Lấy số dư hiện tại của tài khoản
     * 
     * <p><strong>Quan trọng:</strong> Method này thường được gọi trong transaction
     * để kiểm tra số dư trước khi thực hiện chuyển tiền.</p>
     * 
     * @return Số dư hiện tại
     */
    public Double getBalance() { 
        return balance; 
    }
    
    /**
     * Set số dư mới cho tài khoản
     * 
     * <p><strong>Cảnh báo:</strong> Method này thay đổi trạng thái tài khoản.
     * Phải được gọi trong transaction context để đảm bảo data consistency.</p>
     * 
     * @param balance Số dư mới
     */
    public void setBalance(Double balance) { 
        this.balance = balance; 
    }
    
    /**
     * Override toString để debug dễ dàng
     * @return String representation của Account
     */
    @Override
    public String toString() {
        return String.format("Account{id=%d, owner='%s', balance=%.2f}", 
                           id, owner, balance);
    }
} 