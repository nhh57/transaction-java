# Transaction Core Summary - Tổng hợp Kiến thức Nền tảng

## 1. Khái niệm Transaction & ACID

### Transaction là gì?
Transaction là một đơn vị công việc hoàn chỉnh, bao gồm một hoặc nhiều thao tác cơ sở dữ liệu phải được thực hiện như một khối duy nhất. Hoặc tất cả thành công, hoặc tất cả thất bại.

### ACID Properties

| Property | Mô tả | Ví dụ thực tế |
|----------|-------|---------------|
| **Atomicity** | Tính nguyên tử - Hoặc tất cả thành công, hoặc tất cả thất bại | Chuyển tiền: Trừ tiền A và cộng tiền B phải cùng thành công |
| **Consistency** | Tính nhất quán - Dữ liệu luôn ở trạng thái hợp lệ | Tổng tiền trong hệ thống không thay đổi sau chuyển tiền |
| **Isolation** | Tính cô lập - Các transaction không ảnh hưởng lẫn nhau | 2 người cùng chuyển tiền không can thiệp vào nhau |
| **Durability** | Tính bền vững - Dữ liệu được lưu trữ vĩnh viễn | Sau khi commit, dữ liệu không mất dù hệ thống crash |

## 2. So sánh Transaction ở các tầng

### JDBC Transaction

```java
// JDBC - Quản lý transaction thủ công
Connection conn = DriverManager.getConnection(url);
try {
    conn.setAutoCommit(false); // Bắt đầu transaction
    
    // Thực hiện các SQL statements
    PreparedStatement stmt1 = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
    PreparedStatement stmt2 = conn.prepareStatement("UPDATE accounts SET balance = ? WHERE id = ?");
    
    stmt1.executeUpdate();
    stmt2.executeUpdate();
    
    conn.commit(); // Commit transaction
} catch (SQLException e) {
    conn.rollback(); // Rollback khi có lỗi
} finally {
    conn.close();
}
```

**Ưu điểm:**
- Kiểm soát hoàn toàn transaction
- Hiệu suất cao
- Linh hoạt trong xử lý

**Nhược điểm:**
- Nhiều boilerplate code
- Dễ quên rollback/commit
- Khó maintain

### JPA Transaction

```java
// JPA - Sử dụng EntityManager
EntityManager em = emf.createEntityManager();
EntityTransaction tx = em.getTransaction();
try {
    tx.begin();
    
    Account from = em.find(Account.class, fromId);
    Account to = em.find(Account.class, toId);
    
    from.setBalance(from.getBalance() - amount);
    to.setBalance(to.getBalance() + amount);
    
    tx.commit();
} catch (Exception e) {
    tx.rollback();
} finally {
    em.close();
}
```

**Ưu điểm:**
- Object-oriented approach
- Automatic dirty checking
- Caching mechanism

**Nhược điểm:**
- Vẫn phải quản lý transaction thủ công
- Phức tạp hơn JDBC

### Spring Transaction

```java
// Spring - Declarative transaction với @Transactional
@Service
public class AccountService {
    
    @Transactional
    public void transfer(Long fromId, Long toId, Double amount) {
        Account from = accountRepository.findById(fromId).orElseThrow();
        Account to = accountRepository.findById(toId).orElseThrow();
        
        if (from.getBalance() < amount) {
            throw new RuntimeException("Insufficient balance");
        }
        
        from.setBalance(from.getBalance() - amount);
        to.setBalance(to.getBalance() + amount);
        
        accountRepository.save(from);
        accountRepository.save(to);
        // Tự động rollback khi có exception
    }
}
```

**Ưu điểm:**
- Declarative - Chỉ cần annotation
- Automatic rollback khi có exception
- Clean code, dễ đọc
- AOP-based, không xâm nhập business logic

**Nhược điểm:**
- Ít kiểm soát chi tiết
- Cần hiểu cơ chế AOP Proxy
- Có thể có performance overhead

## 3. Cơ chế hoạt động của Spring @Transactional

### AOP Proxy Mechanism

```
Client → Proxy → Target Method
         ↓
    Transaction Begin
         ↓
    Business Logic
         ↓
    Transaction Commit/Rollback
```

### Transaction Manager
Spring sử dụng `PlatformTransactionManager` để quản lý transaction:
- `DataSourceTransactionManager` - Cho JDBC
- `JpaTransactionManager` - Cho JPA
- `JtaTransactionManager` - Cho distributed transactions

### Rollback Rules
- **Default**: Rollback với `RuntimeException` và `Error`
- **Checked Exception**: Không rollback (phải cấu hình explicit)
- **Custom**: `@Transactional(rollbackFor = Exception.class)`

## 4. Thực hành với Spring Boot

### Cấu trúc dự án
```
src/main/java/com/hainh/transaction/core/
├── Account.java          # Entity với @Entity, @Id, @GeneratedValue
├── AccountRepository.java # Repository extends JpaRepository
├── AccountService.java   # Service với @Transactional
└── AccountController.java # REST Controller
```

### Kết quả test thực tế
```
1. Tạo Account A: 1000.0
2. Tạo Account B: 500.0
3. Transfer 200: A=800.0, B=700.0 ✅
4. Transfer 1000 từ B (chỉ có 500): "Insufficient balance" ✅ Rollback
```

## 5. Khi nào nên dùng Transaction?

### ✅ Nên dùng khi:
1. **Thao tác nhiều bước liên quan**: Chuyển tiền, đặt hàng, cập nhật inventory
2. **Cần đảm bảo consistency**: Dữ liệu phải nhất quán
3. **Business logic phức tạp**: Nhiều entities liên quan
4. **Concurrent access**: Nhiều user cùng thao tác

### ❌ Không nên dùng khi:
1. **Chỉ đọc dữ liệu**: SELECT queries đơn giản
2. **Single operation**: Chỉ INSERT/UPDATE một record
3. **Performance critical**: Khi cần tốc độ cao nhất
4. **Long-running operations**: Tránh lock quá lâu

## 6. Best Practices

### Do's ✅
- Giữ transaction ngắn gọn
- Sử dụng appropriate isolation level
- Handle exceptions properly
- Test rollback scenarios
- Monitor transaction performance

### Don'ts ❌
- Tránh nested transactions phức tạp
- Không gọi external services trong transaction
- Không để transaction quá dài
- Tránh transaction cho read-only operations

## 7. Common Pitfalls & Solutions

### Pitfall 1: @Transactional không hoạt động
**Nguyên nhân**: Gọi method từ cùng class (self-invocation)
**Giải pháp**: Tách ra class khác hoặc inject self

### Pitfall 2: Checked Exception không rollback
**Nguyên nhân**: Spring chỉ rollback với RuntimeException
**Giải pháp**: `@Transactional(rollbackFor = Exception.class)`

### Pitfall 3: Performance issues
**Nguyên nhân**: Transaction quá dài, lock nhiều
**Giải pháp**: Tối ưu query, giảm scope transaction

## 8. Monitoring & Debugging

### H2 Console
- URL: `http://localhost:8080/h2-console`
- JDBC URL: `jdbc:h2:mem:testdb`
- Xem real-time data changes

### Logging
```properties
logging.level.org.springframework.transaction=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### Metrics
- Transaction success/failure rate
- Average transaction duration
- Rollback frequency

## 9. Kết luận

Transaction là công cụ quan trọng để đảm bảo tính toàn vẹn dữ liệu. Spring Boot với @Transactional annotation cung cấp cách tiếp cận declarative, clean và hiệu quả. Tuy nhiên, cần hiểu rõ cơ chế hoạt động để sử dụng đúng cách và tránh các lỗi phổ biến.

**Key Takeaways:**
1. Transaction đảm bảo ACID properties
2. Spring @Transactional sử dụng AOP proxy
3. Default rollback với RuntimeException
4. Giữ transaction ngắn gọn và focused
5. Test cả success và failure scenarios 