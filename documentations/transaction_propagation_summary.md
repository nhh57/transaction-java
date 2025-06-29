# Transaction Propagation - Tổng hợp kiến thức cốt lõi

## Tổng quan về Transaction Propagation

**Transaction Propagation** là cơ chế quyết định cách một transaction con (nested transaction) tương tác với transaction cha khi được gọi trong context của một transaction đang tồn tại.

### Tại sao cần Propagation?
- **Kiểm soát transaction lồng nhau**: Khi service A gọi service B, cần quyết định B có tham gia vào transaction của A hay tạo transaction riêng
- **Quản lý rollback độc lập**: Một số nghiệp vụ cần rollback độc lập, không ảnh hưởng đến transaction cha
- **Tối ưu performance**: Tránh tạo transaction không cần thiết cho các operation đơn giản

## Các loại Propagation trong Spring

### 1. REQUIRED (Mặc định)
```java
@Transactional(propagation = Propagation.REQUIRED)
public void method() { ... }
```

**Behavior:**
- Có transaction cha → Tham gia vào transaction cha
- Không có transaction cha → Tạo transaction mới
- Rollback → Rollback toàn bộ transaction (bao gồm cả cha)

**Khi nào sử dụng:**
- ✅ Nghiệp vụ cần đảm bảo tính nhất quán với transaction cha
- ✅ Default choice cho hầu hết các trường hợp
- ❌ Khi cần rollback độc lập

### 2. REQUIRES_NEW
```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void method() { ... }
```

**Behavior:**
- Luôn tạo transaction mới
- Transaction cha bị suspend
- Rollback → Chỉ rollback transaction con, không ảnh hưởng transaction cha

**Khi nào sử dụng:**
- ✅ Ghi log, audit (cần commit ngay cả khi business logic fail)
- ✅ Gửi email, notification (không muốn rollback khi main business fail)
- ✅ Cập nhật counter, statistics
- ❌ Khi cần rollback cùng với transaction cha

### 3. NESTED
```java
@Transactional(propagation = Propagation.NESTED)
public void method() { ... }
```

**Behavior:**
- Tạo savepoint trong transaction hiện tại
- Rollback → Rollback về savepoint, transaction cha có thể tiếp tục
- Nếu không có transaction cha → Hoạt động như REQUIRED

**Khi nào sử dụng:**
- ✅ Thử nghiệp vụ phụ, nếu fail thì skip và tiếp tục
- ✅ Batch processing với partial failure tolerance
- ❌ Database không support savepoint

### 4. SUPPORTS
```java
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public void method() { ... }
```

**Behavior:**
- Có transaction cha → Tham gia vào transaction cha
- Không có transaction cha → Chạy non-transactional

**Khi nào sử dụng:**
- ✅ Read-only operations
- ✅ Utility methods có thể gọi từ cả transactional và non-transactional context
- ❌ Operations cần đảm bảo transaction

### 5. MANDATORY
```java
@Transactional(propagation = Propagation.MANDATORY)
public void method() { ... }
```

**Behavior:**
- Có transaction cha → Tham gia vào transaction cha
- Không có transaction cha → Throw IllegalTransactionStateException

**Khi nào sử dụng:**
- ✅ Enforce transaction requirement
- ✅ Critical operations phải chạy trong transaction
- ❌ Methods có thể gọi từ non-transactional context

### 6. NEVER
```java
@Transactional(propagation = Propagation.NEVER)
public void method() { ... }
```

**Behavior:**
- Có transaction cha → Throw IllegalTransactionStateException
- Không có transaction cha → Chạy non-transactional

**Khi nào sử dụng:**
- ✅ Operations không được chạy trong transaction (performance sensitive)
- ✅ Long-running operations
- ❌ Operations cần transaction safety

### 7. NOT_SUPPORTED
```java
@Transactional(propagation = Propagation.NOT_SUPPORTED)
public void method() { ... }
```

**Behavior:**
- Có transaction cha → Suspend transaction cha, chạy non-transactional
- Không có transaction cha → Chạy non-transactional

**Khi nào sử dụng:**
- ✅ Long-running operations trong transaction context
- ✅ Operations không cần transaction nhưng được gọi từ transactional method
- ❌ Operations cần transaction safety

## Kết quả Test thực tế

Từ test script `test_propagation_simple.ps1`, chúng ta quan sát được:

```
Initial Balance: 1000.0
After REQUIRED (+100): 1100.0      ✅ Transaction created/joined
After REQUIRES_NEW (+50): 1150.0   ✅ New independent transaction  
After NESTED (+25): 1175.0         ✅ Savepoint created
After SUPPORTS: 1175.0             ✅ Read-only, no change
After MANDATORY fail: 1175.0       ✅ Failed as expected (no transaction)
After NEVER: 1175.0                ✅ Non-transactional read
After Complex Scenario: 1350.0     ✅ Multiple propagations (+175)
Final Balance: 1350.0              ✅ All transactions committed properly
```

## Best Practices

### 1. Chọn Propagation phù hợp
```java
// ✅ Business logic chính - REQUIRED (default)
@Transactional
public void processOrder(Order order) { ... }

// ✅ Logging/Audit - REQUIRES_NEW  
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void logActivity(String activity) { ... }

// ✅ Read operations - SUPPORTS
@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
public Order findOrder(Long id) { ... }

// ✅ Critical operations - MANDATORY
@Transactional(propagation = Propagation.MANDATORY)
public void updateCriticalData(Data data) { ... }
```

### 2. Monitoring và Debug
```java
private void logTransactionStatus(String methodName) {
    boolean inTransaction = TransactionSynchronizationManager.isActualTransactionActive();
    String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
    
    log.info("=== {} ===", methodName);
    log.info("In Transaction: {}", inTransaction);
    log.info("Transaction Name: {}", transactionName);
}
```

## Common Mistakes và Cách tránh

### 1. Self-invocation Problem
```java
// ❌ Sai - self-invocation không trigger transaction
@Service
public class OrderService {
    @Transactional
    public void processOrder() {
        this.validateOrder(); // Không có transaction!
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void validateOrder() { ... }
}

// ✅ Đúng - external call
@Service 
public class OrderService {
    @Autowired
    private ValidationService validationService;
    
    @Transactional
    public void processOrder() {
        validationService.validateOrder(); // Có transaction!
    }
}
```

### 2. Circular Dependencies
```java
// ❌ Sai - circular dependency
@Service
public class ServiceA {
    @Autowired
    private ServiceA self; // Circular reference!
}

// ✅ Đúng - tách thành separate service
@Service
public class ServiceA {
    @Autowired
    private ServiceB serviceB;
}
```

## Performance Considerations

### 1. Transaction Overhead
- **REQUIRED**: Ít overhead nhất khi reuse transaction
- **REQUIRES_NEW**: Overhead cao nhất do tạo transaction mới
- **SUPPORTS**: Không overhead khi không có transaction

### 2. Database Connection Pool
- Cẩn thận với REQUIRES_NEW trong loops - có thể cạn kiệt connection pool
- Sử dụng batch processing khi có thể

## Tổng kết

Transaction Propagation là công cụ mạnh mẽ để kiểm soát behavior của nested transactions. Key takeaways:

1. **REQUIRED**: Default choice, tham gia hoặc tạo mới
2. **REQUIRES_NEW**: Dùng cho logging, audit, notification
3. **NESTED**: Dùng cho partial failure tolerance
4. **SUPPORTS**: Dùng cho read operations
5. **MANDATORY/NEVER**: Dùng để enforce transaction rules

**Nguyên tắc vàng**: Chọn propagation dựa trên business requirement về rollback behavior và transaction isolation.

## Tài liệu tham khảo

- Implemented code: `src/main/java/com/hainh/transaction/propagation/`
- Test script: `test_propagation_simple.ps1`
- Spring Framework Transaction Documentation 