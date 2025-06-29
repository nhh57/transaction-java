---
title: "TASK-002: @Transactional – Cách hoạt động thực sự"
type: "task"
status: "completed"
created: "2025-06-27T09:34:00"
updated: "2025-06-27T13:45:00"
id: "TASK-002"
priority: "high"
memory_types: ["procedural", "semantic"]
dependencies: ["TASK-001"]
tags: ["transaction", "spring", "transactional"]
---

## Description
Tìm hiểu chi tiết về annotation @Transactional, khi nào transaction bắt đầu, commit, rollback, ảnh hưởng của exception, self-invocation, proxy. Thực hành các case thực tế để hiểu rõ cách Spring quản lý transaction.

## Objectives
- Hiểu rõ khi nào transaction bắt đầu, commit, rollback.
- Nắm được transaction boundaries: method public, self-invocation, proxy.
- Phân biệt Checked vs Unchecked Exception và ảnh hưởng đến rollback.
- Biết cách Spring xác định rollback (rollbackFor, noRollbackFor).
- Thực hành các case thực tế, test rollback, proxy, boundaries.

## Checklist

### 1. Transactional Demo
- [x] Tạo package `com.hainh.transaction.transactional`
- [x] Tạo class `TransactionalDemoService`
    - [x] `src/main/java/com/hainh/transaction/transactional/TransactionalDemoService.java`
    - [x] Method public/private, self-invocation, proxy
    - [x] Annotation: `@Service`, `@Transactional`
    - [x] Notes: Minh họa boundaries, proxy, self-invocation
- [x] Tạo class `TransactionalDemoController`
    - [x] `src/main/java/com/hainh/transaction/transactional/TransactionalDemoController.java`
    - [x] REST API test các case trên
    - [x] Annotation: `@RestController`, `@RequestMapping`
    - [x] Notes: Cách test API, quan sát rollback qua API

### 2. Exception & Rollback
- [x] Thêm method ném Checked/Unchecked Exception
- [x] Thử rollbackFor, noRollbackFor
- [x] Notes: Khi nào rollback, khi nào không, best practices

### 3. Proxy & Boundaries
- [x] Thử gọi method @Transactional từ chính class (self-invocation)
- [x] Thử gọi từ bean khác (qua controller/service khác)
- [x] Notes: Giải thích vì sao self-invocation không kích hoạt transaction

### 4. Test & Verification
- [x] Viết test case hoặc hướng dẫn test API (Postman/curl)
    - [x] Gọi các API, quan sát rollback, boundaries, proxy
    - [x] Notes: Cách kiểm tra dữ liệu, log, H2 Console

### 5. Tổng hợp & So sánh
- [x] Ghi chú: Khi nào nên dùng @Transactional, khi nào không (ưu/nhược điểm, thực tế)
- [x] Tổng hợp giá trị cốt lõi (documentations/transaction_transactional_summary.md)

## Progress
- [x] Transactional Demo
- [x] Exception & Rollback
- [x] Proxy & Boundaries
- [x] Test & Verification
- [x] Tổng hợp & So sánh

## Key Considerations
- **Tại sao sử dụng @Transactional?**: Đơn giản hóa quản lý transaction, tự động rollback khi có lỗi, dễ bảo trì code.
- **Ưu điểm**:
    - Tự động hóa transaction, giảm code lặp.
    - Dễ kiểm soát rollback/commit.
    - Có thể cấu hình chi tiết (propagation, isolation, rollbackFor...)
- **Nhược điểm**:
    - Dễ bị hiểu nhầm về boundaries (self-invocation, private method không được proxy).
    - Có thể gây bug khó phát hiện nếu không hiểu rõ cơ chế hoạt động.
- **Khi nào nên dùng?**: Khi cần quản lý transaction phức tạp, nhiều bước, rollback tự động.
- **Khi nào không nên dùng?**: Với các thao tác đơn giản, chỉ đọc, hoặc không cần rollback.

## Notes
- Mỗi bước đều phải có notes giải thích tại sao dùng, ưu nhược điểm, khi nào nên dùng/không nên dùng.
- Khi hoàn thành phải có file tổng hợp giá trị cốt lõi.
- Nên thực hành với nhiều case exception để hiểu rõ rollback.

## Discussion
- Phân tích các lỗi thường gặp khi dùng @Transactional sai cách (self-invocation, boundaries).
- Thực tế triển khai @Transactional trong các hệ thống lớn.

## Next Steps
- Sau khi hoàn thành, chuyển sang TASK-003: Propagation – Giao tiếp giữa các transaction.

## Current Status
- [x] Core Implementation: HOÀN THÀNH - Đã implement TransactionalDemoService và TransactionalDemoController
- [x] API Test: HOÀN THÀNH - Đã tạo test script và verify tất cả scenarios
- [x] Tổng hợp & So sánh: HOÀN THÀNH - Đã tạo transaction_transactional_summary.md với phân tích chi tiết

## Achievements
- ✅ Tạo được comprehensive demo về @Transactional behavior
- ✅ Verify được tất cả exception rollback scenarios
- ✅ Demonstrate được self-invocation problem
- ✅ Tạo được detailed documentation với best practices
- ✅ Test script chạy thành công với kết quả như mong đợi

## Key Learnings
- Self-invocation KHÔNG tạo transaction mới (cần gọi từ external bean)
- Unchecked exceptions tự động rollback, checked exceptions thì không
- rollbackFor và noRollbackFor có thể customize behavior
- Proxy mechanism chỉ hoạt động với public methods và external calls
- Transaction boundaries rất quan trọng để hiểu behavior 