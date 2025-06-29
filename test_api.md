# Transaction API Test Guide

## 1. Tạo tài khoản (Create Accounts)

### Tạo tài khoản A
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner": "Nguyen Van A", "balance": 1000.0}'
```

### Tạo tài khoản B
```bash
curl -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"owner": "Tran Thi B", "balance": 500.0}'
```

## 2. Xem tất cả tài khoản (Get All Accounts)
```bash
curl http://localhost:8080/api/accounts
```

## 3. Xem tài khoản theo ID (Get Account by ID)
```bash
curl http://localhost:8080/api/accounts/1
curl http://localhost:8080/api/accounts/2
```

## 4. Chuyển tiền thành công (Successful Transfer)
```bash
curl -X POST "http://localhost:8080/api/accounts/transfer?fromId=1&toId=2&amount=200"
```

## 5. Chuyển tiền thất bại - Không đủ số dư (Failed Transfer - Insufficient Balance)
```bash
curl -X POST "http://localhost:8080/api/accounts/transfer?fromId=2&toId=1&amount=1000"
```

## 6. Kiểm tra số dư sau giao dịch
```bash
curl http://localhost:8080/api/accounts
```

## Expected Results:

### Sau khi tạo tài khoản:
- Account 1: Nguyen Van A - 1000.0
- Account 2: Tran Thi B - 500.0

### Sau chuyển tiền thành công (200 từ A sang B):
- Account 1: Nguyen Van A - 800.0
- Account 2: Tran Thi B - 700.0

### Sau chuyển tiền thất bại (1000 từ B sang A):
- Lỗi: "Insufficient balance"
- Số dư không thay đổi (rollback thành công)

## H2 Console Access:
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
Username: sa
Password: (empty)
``` 