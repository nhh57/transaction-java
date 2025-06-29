# Kế hoạch học chuyên sâu Transaction trong Spring

## 1. Kiến thức nền tảng (Cốt lõi)
- [ ] Hiểu rõ khái niệm Transaction, ACID, và tầm quan trọng trong hệ thống thực tế
- [ ] Cơ chế hoạt động của Transaction trong Spring (AOP, Proxy, Transaction Manager)
- [ ] Sự khác biệt giữa transaction ở JDBC, JPA, Spring

## 2. @Transactional – Cách hoạt động thực sự
- [ ] Khi nào transaction bắt đầu, commit, rollback?
- [ ] Transaction boundaries: method public, self-invocation, proxy
- [ ] Checked vs Unchecked Exception và ảnh hưởng đến rollback
- [ ] Cách Spring xác định rollback (rollbackFor, noRollbackFor)

## 3. Propagation – Giao tiếp giữa các transaction
- [ ] Các loại propagation: REQUIRED, REQUIRES_NEW, NESTED, SUPPORTS, MANDATORY, NEVER, NOT_SUPPORTED
- [ ] Ảnh hưởng của propagation đến transaction cha/con
- [ ] Tình huống thực tế: Gọi service lồng nhau, ghi log, gửi email, v.v.

## 4. Isolation – Cô lập transaction
- [ ] Các mức isolation: DEFAULT, READ_UNCOMMITTED, READ_COMMITTED, REPEATABLE_READ, SERIALIZABLE
- [ ] Ảnh hưởng đến hiện tượng dirty read, non-repeatable read, phantom read
- [ ] Khi nào cần thay đổi isolation? (ví dụ thực tế)

## 5. Transaction và Performance
- [ ] Transaction ảnh hưởng đến hiệu năng như thế nào?
- [ ] Khi nào nên dùng/nên tránh transaction?
- [ ] Transaction chỉ đọc (readOnly=true) và tối ưu hóa

## 6. Các tình huống thực tế & lỗi thường gặp
- [ ] Transaction không hoạt động? (self-invocation, method private, không phải bean Spring)
- [ ] Transaction bị treo, deadlock, timeout
- [ ] Transaction và batch processing
- [ ] Transaction với nhiều datasource (distributed transaction/XA)

## 7. Debug, kiểm tra và best practices
- [ ] Bật log transaction, quan sát transaction manager
- [ ] Dùng H2 Console hoặc log SQL để kiểm tra dữ liệu
- [ ] Best practices khi thiết kế service, repository với transaction

## 8. Thực hành & Case Study
- [ ] Viết các ví dụ minh họa từng chủ đề trên
- [ ] Thực hành các tình huống lỗi, rollback, propagation, isolation
- [ ] Case study: Xây dựng module chuyển tiền, đặt vé, xử lý đơn hàng có transaction phức tạp

---

**Lộ trình học gợi ý:**
1. Nền tảng & @Transactional (cốt lõi)
2. Propagation (thực chiến)
3. Isolation (cô lập & concurrency)
4. Exception & rollback (thực tế)
5. Performance & best practices
6. Debug & xử lý lỗi
7. Thực hành case study 