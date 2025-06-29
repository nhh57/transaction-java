# @Transactional - Cách hoạt động thực sự

## Tổng quan

`@Transactional` là annotation quan trọng nhất trong Spring để quản lý transaction declaratively. Tuy nhiên, để sử dụng hiệu quả, cần hiểu rõ cách hoạt động bên trong và các "cạm bẫy" phổ biến.

## 🔧 Cách hoạt động - Spring AOP Proxy

### Cơ chế Proxy
```java
// Khi Spring tạo bean với @Transactional
@Service
public class MyService {
    @Transactional
    public void doSomething() { ... }
}

// Spring tạo proxy:
MyService proxy = new MyServiceProxy(actualService);
```

### Luồng thực thi
1. **Client gọi method** → Spring Proxy intercepts
2. **Proxy bắt đầu transaction** → TransactionManager.begin()
3. **Proxy gọi actual method** → method thực thi
4. **Method hoàn thành** → Proxy commit/rollback transaction

## 🚨 Transaction Boundaries - Những điều QUAN TRỌNG

### ✅ Khi nào @Transactional hoạt động
- Method được gọi từ **BÊN NGOÀI** class (qua Spring proxy)
- Method có visibility **PUBLIC**
- Bean được quản lý bởi Spring container

### ❌ Khi nào @Transactional KHÔNG hoạt động
- **Self-invocation**: Gọi method @Transactional từ chính class đó
- **Private/Protected methods**: Proxy không thể intercept
- **Internal calls**: Gọi trực tiếp không qua proxy

### Demo Self-Invocation Problem
```java
@Service
public class TransactionalDemoService {
    
    @Transactional
    public void externalMethod() {
        // Transaction BẮT ĐẦU tại đây
        
        // SELF-INVOCATION - KHÔNG tạo transaction mới!
        internalTransactionalMethod(); // ← Gọi trực tiếp, không qua proxy
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void internalTransactionalMethod() {
        // Annotation này bị IGNORE khi gọi từ externalMethod()!
        // Chạy trong cùng transaction với externalMethod()
    }
}
```

## 💥 Exception & Rollback Behavior

### Quy tắc mặc định
| Exception Type | Rollback | Lý do |
|----------------|----------|-------|
| **RuntimeException** (Unchecked) | ✅ YES | Spring cho rằng đây là lỗi nghiêm trọng |
| **Exception** (Checked) | ❌ NO | Spring cho rằng đây là business logic |
| **Error** | ✅ YES | Lỗi hệ thống nghiêm trọng |

### Ví dụ thực tế
```java
@Transactional
public void demonstrateUncheckedException() {
    account.setBalance(1000.0);
    accountRepository.save(account); // Thay đổi dữ liệu
    
    throw new RuntimeException("Error!"); // → ROLLBACK, balance không thay đổi
}

@Transactional
public void demonstrateCheckedException() throws Exception {
    account.setBalance(1000.0);
    accountRepository.save(account); // Thay đổi dữ liệu
    
    throw new Exception("Business error!"); // → COMMIT, balance VẪN thay đổi!
}
```

## 🔧 Tùy chỉnh Rollback Rules

### rollbackFor - Force rollback cho specific exceptions
```java
@Transactional(rollbackFor = Exception.class) // Rollback cho TẤT CẢ exceptions
public void customRollbackRules() throws Exception {
    account.setBalance(1000.0);
    accountRepository.save(account);
    
    throw new Exception("This WILL cause rollback!"); // → ROLLBACK
}
```

### noRollbackFor - Prevent rollback cho specific exceptions
```java
@Transactional(noRollbackFor = IllegalArgumentException.class)
public void noRollbackRules() {
    account.setBalance(1000.0);
    accountRepository.save(account);
    
    throw new IllegalArgumentException("This will NOT cause rollback!"); // → COMMIT
}
```

### Kết hợp nhiều rules
```java
@Transactional(
    rollbackFor = {Exception.class, IOException.class},
    noRollbackFor = {IllegalArgumentException.class, ValidationException.class}
)
public void complexRollbackRules() {
    // Custom logic here
}
```

## 📊 Kết quả Demo thực tế

Từ test script của chúng ta:

| Test Case | Balance Before | Balance After | Result | Explanation |
|-----------|----------------|---------------|---------|-------------|
| **Unchecked Exception** | 2000.0 | 2000.0 | ✅ ROLLBACK | RuntimeException → tự động rollback |
| **Checked Exception** | 2000.0 | 2300.0 | ✅ NO ROLLBACK | Exception → không rollback |
| **Custom rollbackFor** | 2300.0 | 2300.0 | ✅ ROLLBACK | rollbackFor=Exception.class |
| **noRollbackFor** | 2300.0 | 2450.0 | ✅ NO ROLLBACK | noRollbackFor=IllegalArgumentException.class |

## 🎯 Best Practices

### 1. Luôn gọi @Transactional methods từ bên ngoài
```java
// ❌ WRONG - Self-invocation
@Service
public class BadService {
    @Transactional
    public void methodA() {
        methodB(); // ← Không có transaction!
    }
    
    @Transactional
    public void methodB() { ... }
}

// ✅ CORRECT - External call
@Service
public class GoodService {
    @Autowired
    private AnotherService anotherService;
    
    @Transactional
    public void methodA() {
        anotherService.methodB(); // ← Có transaction!
    }
}
```

### 2. Explicit rollback rules
```java
// ✅ GOOD - Explicit về rollback behavior
@Transactional(rollbackFor = Exception.class)
public void businessMethod() throws Exception {
    // Clear intention: rollback for ANY exception
}
```

### 3. Proper exception handling
```java
@Transactional
public void processPayment(Payment payment) {
    try {
        // Business logic
        paymentService.process(payment);
    } catch (PaymentException e) {
        // Log error but let transaction rollback
        logger.error("Payment failed: {}", e.getMessage());
        throw e; // Re-throw để trigger rollback
    } catch (Exception e) {
        // Wrap unexpected exceptions
        throw new PaymentException("Unexpected error", e);
    }
}
```

## ⚠️ Những lỗi thường gặp

### 1. Self-Invocation Trap
```java
// ❌ COMMON MISTAKE
@Service
public class OrderService {
    @Transactional
    public void createOrder() {
        // ... create order logic
        sendNotification(); // ← Transaction không apply!
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendNotification() {
        // Annotation này bị ignore!
    }
}
```

### 2. Private Method Trap
```java
// ❌ COMMON MISTAKE
@Service
public class UserService {
    public void registerUser() {
        saveUser(); // ← Không có transaction!
    }
    
    @Transactional // ← Annotation này bị IGNORE!
    private void saveUser() {
        // Private method không thể được proxy
    }
}
```

### 3. Exception Swallowing
```java
// ❌ BAD - Nuốt exception
@Transactional
public void processOrder() {
    try {
        orderService.process();
    } catch (Exception e) {
        logger.error("Error", e);
        // Không re-throw → transaction sẽ COMMIT!
    }
}

// ✅ GOOD - Re-throw exception
@Transactional
public void processOrder() {
    try {
        orderService.process();
    } catch (Exception e) {
        logger.error("Error", e);
        throw e; // Re-throw để trigger rollback
    }
}
```

## 🔍 Debug và Monitoring

### 1. Enable transaction logging
```properties
# application.properties
logging.level.org.springframework.transaction=DEBUG
logging.level.org.springframework.orm.jpa=DEBUG
logging.level.org.hibernate.SQL=DEBUG
```

### 2. Kiểm tra transaction status
```java
@Transactional
public void debugTransaction() {
    boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
    logger.info("In transaction: {}", inTransaction);
    
    String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
    logger.info("Transaction name: {}", transactionName);
}
```

## 📈 Performance Considerations

### 1. Transaction scope
- Giữ transaction **ngắn gọn** và **focused**
- Tránh long-running operations trong transaction
- Tách read-only operations ra ngoài nếu có thể

### 2. Read-only optimization
```java
@Transactional(readOnly = true)
public List<Account> getAllAccounts() {
    // Hibernate có thể optimize cho read-only
    return accountRepository.findAll();
}
```

### 3. Timeout configuration
```java
@Transactional(timeout = 30) // 30 seconds timeout
public void longRunningOperation() {
    // Prevent hanging transactions
}
```

## 🎓 Kết luận

### Khi nào nên dùng @Transactional?
- ✅ Business operations cần ACID properties
- ✅ Multi-step operations cần consistency
- ✅ Data modification operations
- ✅ Khi cần automatic rollback

### Khi nào KHÔNG nên dùng?
- ❌ Simple read operations
- ❌ Operations không cần consistency
- ❌ Long-running background tasks
- ❌ Khi performance là ưu tiên hàng đầu

### Nguyên tắc vàng
1. **Hiểu rõ proxy mechanism** - Chỉ external calls mới có transaction
2. **Explicit về rollback rules** - Đừng dựa vào default behavior
3. **Keep transactions short** - Minimize lock time
4. **Test thoroughly** - Verify rollback behavior
5. **Monitor performance** - Watch for long-running transactions

---

**Lưu ý quan trọng**: @Transactional là công cụ mạnh mẽ nhưng cần hiểu rõ để tránh các pitfall phổ biến. Luôn test kỹ transaction behavior trong môi trường gần giống production! 