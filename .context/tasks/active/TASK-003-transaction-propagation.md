---
title: "TASK-003: Propagation – Giao tiếp giữa các transaction"
type: "task"
status: "active"
created: "2025-06-27T09:34:00"
updated: "2025-06-27T15:35:00"
id: "TASK-003"
priority: "high"
memory_types: ["procedural", "semantic"]
dependencies: ["TASK-002"]
tags: ["transaction", "spring", "propagation"]
---

## Description
Tìm hiểu các loại propagation trong transaction, ảnh hưởng đến transaction cha/con, thực hành các tình huống thực tế để hiểu rõ cách Spring quản lý propagation.

## Objectives
- Hiểu rõ các loại propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, MANDATORY, NEVER, NOT_SUPPORTED.
- Nắm được ảnh hưởng của propagation đến transaction cha/con.
- Thực hành các tình huống thực tế: Gọi service lồng nhau, ghi log, gửi email, v.v.
- Thực hành rollback/commit ở từng propagation.

## Checklist

### 1. Propagation Demo
- [x] Tạo package `com.hainh.transaction.propagation`
- [x] Tạo class `PropagationDemoService`
    - [x] `src/main/java/com/hainh/transaction/propagation/PropagationDemoService.java`
    - [x] Các method với propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, MANDATORY, NEVER, NOT_SUPPORTED
    - [x] Annotation: `@Service`, `@Transactional(propagation=...)`
    - [x] Notes: Minh họa propagation, rollback/commit với TransactionSynchronizationManager
- [x] Tạo class `PropagationDemoController`
    - [x] `src/main/java/com/hainh/transaction/propagation/PropagationDemoController.java`
    - [x] REST API test các case propagation
    - [x] Annotation: `@RestController`, `@RequestMapping`
    - [x] Notes: Cách test API, quan sát propagation qua API

### 2. Case Studies
- [x] Gọi lồng nhau các service với propagation khác nhau
- [x] Thử rollback ở service cha/con, quan sát kết quả với demonstrateRollbackScenario
- [x] Notes: Complex scenarios với multiple propagations, independent rollback behavior

### 3. Test & Verification
- [x] Viết test case hoặc hướng dẫn test API (PowerShell script)
    - [x] `test_propagation_simple.ps1` - comprehensive test script
    - [x] Gọi các API, quan sát rollback, propagation behavior
    - [x] Notes: Balance verification, transaction status logging

### 4. Tổng hợp & So sánh
- [x] Ghi chú: Khi nào nên dùng từng loại propagation, khi nào không (ưu/nhược điểm, thực tế)
- [x] Tổng hợp giá trị cốt lõi (documentations/transaction_propagation_summary.md)

## Progress
- [x] Propagation Demo
- [x] Case Studies
- [x] Test & Verification
- [x] Tổng hợp & So sánh

## Key Considerations
- **Tại sao sử dụng propagation?**: Kiểm soát cách transaction lồng nhau hoạt động, quyết định rollback/commit độc lập hoặc phụ thuộc.
- **Ưu điểm**:
    - Linh hoạt trong quản lý transaction lồng nhau.
    - Có thể tách biệt các nghiệp vụ quan trọng.
- **Nhược điểm**:
    - Dễ gây nhầm lẫn nếu không hiểu rõ từng loại propagation.
    - Có thể gây bug khó phát hiện khi rollback không như mong muốn.
- **Khi nào nên dùng?**: Khi có nhiều service lồng nhau, cần kiểm soát transaction độc lập hoặc phụ thuộc.
- **Khi nào không nên dùng?**: Khi nghiệp vụ đơn giản, không có transaction lồng nhau.

## Notes
- Mỗi bước đều phải có notes giải thích tại sao dùng, ưu nhược điểm, khi nào nên dùng/không nên dùng.
- Khi hoàn thành phải có file tổng hợp giá trị cốt lõi.
- Nên thực hành với nhiều case lồng nhau để hiểu rõ propagation.

## Discussion
- Phân tích các lỗi thường gặp khi dùng propagation sai cách.
- Thực tế triển khai propagation trong các hệ thống lớn.

## Next Steps
- Sau khi hoàn thành, chuyển sang TASK-004: Isolation – Cô lập transaction.

## Current Status
- [x] Core Implementation: ✅ HOÀN THÀNH - PropagationDemoService với 7 loại propagation
- [x] API Test: ✅ HOÀN THÀNH - test_propagation_simple.ps1 với comprehensive testing
- [x] Tổng hợp & So sánh: ✅ HOÀN THÀNH - transaction_propagation_summary.md

## Achievements
- ✅ Triển khai thành công tất cả 7 loại propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, MANDATORY, NEVER, NOT_SUPPORTED
- ✅ Demo complex scenarios với multiple nested transactions
- ✅ Test rollback behaviors và independent transaction handling
- ✅ Comprehensive documentation với best practices và common mistakes
- ✅ Practical testing với PowerShell script showing real transaction behaviors

## Key Learnings
- **REQUIRES_NEW** rất hữu ích cho logging/audit vì commit độc lập
- **NESTED** sử dụng savepoint cho partial failure tolerance
- **Circular dependency** là vấn đề thường gặp khi inject self-reference
- **Self-invocation problem** không trigger transaction propagation
- **TransactionSynchronizationManager** hữu ích để debug transaction status

### Dependencies

- TASK-002

### Notes

- Mỗi bước đều phải có notes giải thích tại sao dùng, ưu nhược điểm, khi nào nên dùng/không nên dùng.
- Khi hoàn thành phải có file tổng hợp giá trị cốt lõi.
- Nên thực hành với nhiều case lồng nhau để hiểu rõ propagation.

### Key Considerations

- **Tại sao sử dụng propagation?**: Kiểm soát cách transaction lồng nhau hoạt động, quyết định rollback/commit độc lập hoặc phụ thuộc.
- **Ưu điểm**:
    - Linh hoạt trong quản lý transaction lồng nhau.
    - Có thể tách biệt các nghiệp vụ quan trọng.
- **Nhược điểm**:
    - Dễ gây nhầm lẫn nếu không hiểu rõ từng loại propagation.
    - Có thể gây bug khó phát hiện khi rollback không như mong muốn.
- **Khi nào nên dùng?**: Khi có nhiều service lồng nhau, cần kiểm soát transaction độc lập hoặc phụ thuộc.
- **Khi nào không nên dùng?**: Khi nghiệp vụ đơn giản, không có transaction lồng nhau.

### Thảo luận sâu hơn

- Phân tích các lỗi thường gặp khi dùng propagation sai cách.
- Thực tế triển khai propagation trong các hệ thống lớn.

### Next Steps

- Sau khi hoàn thành, chuyển sang TASK-004: Isolation – Cô lập transaction. 