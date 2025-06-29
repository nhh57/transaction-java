---
title: "TASK-004: Isolation – Cô lập transaction"
type: "task"
status: "active"
created: "2025-06-27T09:34:00"
updated: "2025-06-27T17:11:10"
id: "TASK-004"
priority: "high"
memory_types: ["procedural", "semantic"]
dependencies: ["TASK-003"]
tags: ["transaction", "spring", "isolation"]
---

## Description
Tìm hiểu các mức isolation trong transaction, hiện tượng dirty read, non-repeatable read, phantom read, thực hành concurrency để hiểu rõ ảnh hưởng của isolation.

## Objectives
- Hiểu rõ các mức isolation: DEFAULT, READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE.
- Nắm được ảnh hưởng của isolation đến hiện tượng dirty read, non-repeatable read, phantom read.
- Biết khi nào cần thay đổi isolation, ví dụ thực tế.
- Thực hành concurrency, test isolation.

## Checklist

### 1. Isolation Demo
- [ ] Tạo package `com.hainh.transaction.isolation`
- [ ] Tạo class `IsolationDemoService`
    - [ ] `src/main/java/com/hainh/transaction/isolation/IsolationDemoService.java`
    - [ ] Các method với isolation: READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE...
    - [ ] Annotation: `@Service`, `@Transactional(isolation=...)`
    - [ ] Notes: Minh họa isolation, dirty read, phantom read
- [ ] Tạo class `IsolationDemoController`
    - [ ] `src/main/java/com/hainh/transaction/isolation/IsolationDemoController.java`
    - [ ] REST API test các case isolation
    - [ ] Annotation: `@RestController`, `@RequestMapping`
    - [ ] Notes: Cách test API, quan sát isolation qua API

### 2. Case Studies
- [ ] Thực hiện thao tác đồng thời (multi-thread) để quan sát dirty read, non-repeatable read, phantom read
- [ ] Thay đổi isolation để thấy sự khác biệt
- [ ] Notes: Khi nào nên dùng từng mức isolation, best practices

### 3. Test & Verification
- [ ] Viết test case hoặc hướng dẫn test API (Postman/curl)
    - [ ] Gọi các API, quan sát hiện tượng isolation
    - [ ] Notes: Cách kiểm tra dữ liệu, log, H2 Console

### 4. Tổng hợp & So sánh
- [ ] Ghi chú: Khi nào nên dùng từng mức isolation, khi nào không (ưu/nhược điểm, thực tế)
- [ ] Tổng hợp giá trị cốt lõi (documentations/transaction_isolation_summary.md)

## Progress
- [ ] Isolation Demo
- [ ] Case Studies
- [ ] Test & Verification
- [ ] Tổng hợp & So sánh

## Key Considerations
- **Tại sao sử dụng isolation?**: Kiểm soát mức độ nhìn thấy dữ liệu giữa các transaction đồng thời, tránh lỗi dữ liệu khi nhiều người thao tác.
- **Ưu điểm**:
    - Đảm bảo tính nhất quán dữ liệu khi có nhiều transaction đồng thời.
    - Có thể điều chỉnh phù hợp với nhu cầu thực tế.
- **Nhược điểm**:
    - Isolation cao (SERIALIZABLE) có thể làm giảm hiệu năng.
    - Isolation thấp (READ_UNCOMMITTED) dễ gây lỗi dữ liệu.
- **Khi nào nên dùng?**: Khi cần kiểm soát chặt chẽ dữ liệu, tránh dirty read, phantom read.
- **Khi nào không nên dùng?**: Khi hiệu năng quan trọng hơn tính nhất quán tuyệt đối.

## Notes
- Mỗi bước đều phải có notes giải thích tại sao dùng, ưu nhược điểm, khi nào nên dùng/không nên dùng.
- Khi hoàn thành phải có file tổng hợp giá trị cốt lõi.
- Nên thực hành với nhiều thread để quan sát rõ isolation.

## Discussion
- Phân tích các lỗi thường gặp khi chọn sai isolation.
- Thực tế triển khai isolation trong các hệ thống lớn.

## Next Steps
- Sau khi hoàn thành, chuyển sang TASK-005: Transaction và Performance.

## Current Status
- [ ] Core Implementation: Chưa bắt đầu
- [ ] API Test: Chưa bắt đầu
- [ ] Tổng hợp & So sánh: Chưa bắt đầu
