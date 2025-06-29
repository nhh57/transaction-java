---
title: "TASK-001: Nền tảng & @Transactional (cốt lõi)"
type: "task"
status: "completed"
created: "2025-06-27T09:34:00"
updated: "2025-06-27T10:30:00"
id: "TASK-001"
priority: "high"
memory_types: ["procedural", "semantic"]
dependencies: []
tags: ["transaction", "spring", "core"]
---

## Description
Nắm vững kiến thức nền tảng về transaction, ACID, cơ chế hoạt động trong Spring, sự khác biệt giữa JDBC, JPA, Spring. Thực hành CRUD, chuyển tiền, rollback, so sánh transaction các tầng, hiểu sâu bản chất và ứng dụng thực tế.

## Objectives
- Hiểu rõ khái niệm Transaction, ACID, và tầm quan trọng trong hệ thống thực tế.
- Nắm được cơ chế hoạt động của Transaction trong Spring (AOP, Proxy, Transaction Manager).
- So sánh transaction ở JDBC, JPA, Spring.
- Thực hành CRUD, chuyển tiền, rollback, kiểm tra dữ liệu.
- Biết khi nào nên dùng transaction, khi nào không.

## Checklist

### 1. Entity Layer
- [x] Tạo package `com.hainh.transaction.core`
- [x] Tạo class `Account` (Entity)
    - [x] `src/main/java/com/hainh/transaction/core/Account.java`
    - [x] Các field: id, owner, balance
    - [x] Annotation: `@Entity`, `@Id`, `@GeneratedValue`
    - [x] Constructors, getters/setters
    - [x] Notes: Entity mapping DB table, JPA annotations cho persistence

### 2. Repository Layer
- [x] Tạo interface `AccountRepository` extends `JpaRepository<Account, Long>`
    - [x] `src/main/java/com/hainh/transaction/core/AccountRepository.java`
    - [x] Annotation: `@Repository`
    - [x] Notes: Spring Data JPA tự động tạo implementation, giảm boilerplate code

### 3. Service Layer
- [x] Tạo class `AccountService`
    - [x] `src/main/java/com/hainh/transaction/core/AccountService.java`
    - [x] CRUD method: create, get, update, delete
    - [x] Method chuyển tiền giữa 2 tài khoản (transfer)
    - [x] Annotation: `@Service`, `@Transactional` (minh họa rollback)
    - [x] Thử rollback khi lỗi (ví dụ: chuyển tiền khi không đủ số dư)
    - [x] Notes: @Transactional đảm bảo ACID, rollback tự động khi exception

### 4. Controller Layer
- [x] Tạo class `AccountController`
    - [x] `src/main/java/com/hainh/transaction/core/AccountController.java`
    - [x] REST API: tạo tài khoản, chuyển tiền, xem tài khoản
    - [x] Annotation: `@RestController`, `@RequestMapping`
    - [x] Notes: REST endpoints cho client, exception handling

### 5. Test & Verification
- [x] Viết test case hoặc hướng dẫn test API (Postman/curl)
    - [x] Tạo tài khoản, chuyển tiền thành công/thất bại
    - [x] Quan sát rollback khi lỗi
    - [x] Notes: H2 Console tại /h2-console, PowerShell test script

### 6. So sánh & Tổng hợp
- [x] So sánh transaction ở JDBC, JPA, Spring (notes + bảng so sánh)
- [x] Ghi chú: Khi nào nên dùng transaction, khi nào không (ưu/nhược điểm, thực tế)
- [x] Tổng hợp giá trị cốt lõi (documentations/transaction_core_summary.md)

## Progress
- [x] Entity Layer - ✅ Account entity with JPA annotations
- [x] Repository Layer - ✅ AccountRepository extends JpaRepository
- [x] Service Layer - ✅ AccountService with @Transactional transfer method
- [x] Controller Layer - ✅ REST API endpoints for CRUD and transfer
- [x] Test & Verification - ✅ API tested successfully with rollback scenarios
- [x] So sánh & Tổng hợp - ✅ Comprehensive summary document created

## Key Considerations
- **Tại sao sử dụng transaction?**: Đảm bảo tính toàn vẹn dữ liệu, tránh mất mát hoặc sai lệch khi có lỗi hoặc thao tác đồng thời.
- **Ưu điểm**:
    - Đảm bảo ACID, dữ liệu nhất quán.
    - Dễ rollback khi có lỗi.
    - Quản lý đồng thời tốt hơn.
- **Nhược điểm**:
    - Có thể ảnh hưởng hiệu năng nếu lạm dụng.
    - Cần hiểu rõ cơ chế rollback, isolation để tránh bug khó phát hiện.
- **Khi nào nên dùng?**: Khi thao tác nhiều bước liên quan đến dữ liệu, cần đảm bảo toàn vẹn.
- **Khi nào không nên dùng?**: Với các thao tác chỉ đọc lớn, hoặc không cần đảm bảo toàn vẹn dữ liệu.

## Notes
- Mỗi bước đều phải có notes giải thích tại sao dùng, ưu nhược điểm, khi nào nên dùng/không nên dùng.
- Khi hoàn thành phải có file tổng hợp giá trị cốt lõi.
- Nên thực hành với ví dụ thực tế để hiểu sâu bản chất.
- Có thể mở rộng thêm test, logging, monitoring nếu cần.

## Thảo luận sâu hơn
- So sánh chi tiết giữa transaction thủ công (JDBC), transaction JPA, và transaction Spring (AOP/proxy).
- Phân tích các lỗi thường gặp khi dùng transaction sai cách.
- Thực tế triển khai transaction trong các hệ thống lớn.

## Next Steps
- Sau khi hoàn thành, chuyển sang TASK-002: @Transactional – Cách hoạt động thực sự.

## Current Status
- [x] Core Implementation: ✅ HOÀN THÀNH - Entity, Repository, Service, Controller đã implement
- [x] API Test: ✅ HOÀN THÀNH - Test thành công cả success và rollback scenarios
- [x] Tổng hợp & So sánh: ✅ HOÀN THÀNH - Document chi tiết tại documentations/transaction_core_summary.md

**TASK-001 ĐÃ HOÀN THÀNH THÀNH CÔNG! 🎉**

### Achievements:
- ✅ Hiểu rõ khái niệm Transaction và ACID properties
- ✅ Implement thành công transaction với Spring Boot @Transactional
- ✅ Test rollback mechanism hoạt động chính xác
- ✅ So sánh chi tiết JDBC vs JPA vs Spring transactions
- ✅ Tạo comprehensive documentation và best practices
- ✅ Xây dựng test scripts và API endpoints hoàn chỉnh 