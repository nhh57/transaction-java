# Transaction Isolation Levels - Cân bằng giữa Nhất quán và Hiệu suất

## 1. Tại sao Isolation Level quan trọng?

Transaction Isolation là một trong 4 tính chất ACID. Nó quyết định mức độ một transaction được "cô lập" khỏi các transaction khác đang chạy đồng thời. Việc chọn Isolation Level ảnh hưởng trực tiếp đến:

- **Tính toàn vẹn dữ liệu (Data Integrity)**: Mức độ cô lập càng cao, dữ liệu càng an toàn khỏi các hiệu ứng không mong muốn từ các transaction khác.
- **Hiệu suất (Performance)**: Mức độ cô lập càng cao, database càng cần nhiều cơ chế locking, dẫn đến giảm khả năng xử lý đồng thời (concurrency) và có thể làm giảm hiệu suất.

**Đây chính là sự đánh đổi cốt lõi: Nhất quán vs. Hiệu suất.**

## 2. Các vấn đề về Concurrency (Concurrency Phenomena)

Khi nhiều transaction chạy song song, các vấn đề sau có thể xảy ra nếu mức độ cô lập không đủ:

| Vấn đề | Mô tả | Ví dụ |
| :--- | :--- | :--- |
| **Dirty Read** | Một transaction đọc dữ liệu chưa được commit bởi một transaction khác. | Tx1 cập nhật số dư tài khoản nhưng chưa commit. Tx2 đọc số dư mới này. Nếu Tx1 bị rollback, Tx2 đã đọc phải dữ liệu "bẩn", không còn tồn tại. |
| **Non-Repeatable Read** | Trong cùng một transaction, đọc lại một dòng dữ liệu nhưng nhận được kết quả khác nhau do một transaction khác đã cập nhật và commit. | Tx1 đọc số dư tài khoản là 1000. Tx2 chuyển tiền vào và commit, số dư mới là 1200. Tx1 đọc lại và thấy số dư là 1200. Dữ liệu đã đọc không thể lặp lại. |
| **Phantom Read** | Trong cùng một transaction, thực hiện một query nhiều lần nhưng nhận được số lượng dòng (rows) khác nhau do một transaction khác đã thêm hoặc xóa dữ liệu. | Tx1 đếm số lượng nhân viên trong phòng "IT", kết quả là 10. Tx2 thêm một nhân viên mới vào phòng "IT" và commit. Tx1 đếm lại và thấy 11 nhân viên. Một "bóng ma" đã xuất hiện. |

## 3. Bốn cấp độ Isolation chính

Đây là 4 cấp độ isolation tiêu chuẩn, được sắp xếp theo mức độ cô lập tăng dần.

| Isolation Level | Dirty Read | Non-Repeatable Read | Phantom Read | Hiệu suất |
| :--- | :---: | :---: | :---: | :---: |
| **READ_UNCOMMITTED** | Allowed | Allowed | Allowed | Cao nhất |
| **READ_COMMITTED** | Prevented | Allowed | Allowed | Cao |
| **REPEATABLE_READ** | Prevented | Prevented | Allowed | Trung bình |
| **SERIALIZABLE** | Prevented | Prevented | Prevented | Thấp nhất |

---

### a. `READ_UNCOMMITTED`
- **Mô tả**: Cho phép đọc cả dữ liệu chưa được commit.
- **Ưu điểm**: Hiệu suất cao nhất, ít locking nhất.
- **Nhược điểm**: Rủi ro cao nhất, dữ liệu đọc có thể không chính xác.
- **Khi nào dùng?**: Rất hiếm khi. Có thể dùng cho các tác vụ thống kê không yêu cầu độ chính xác tuyệt đối (ví dụ: đếm số lượt truy cập gần đúng).

### b. `READ_COMMITTED`
- **Mô tả**: Chỉ đọc được dữ liệu đã được commit. Ngăn chặn Dirty Read. Đây là mức **mặc định của hầu hết các database** (PostgreSQL, SQL Server, Oracle).
- **Ưu điểm**: Cân bằng tốt giữa hiệu suất và tính nhất quán.
- **Nhược điểm**: Vẫn có thể xảy ra Non-Repeatable Read và Phantom Read.
- **Khi nào dùng?**: Hầu hết các tác vụ thông thường của ứng dụng web. Các thao tác đọc và ghi không yêu cầu dữ liệu phải nhất quán tuyệt đối trong suốt một transaction dài.

### c. `REPEATABLE_READ`
- **Mô tả**: Đảm bảo rằng trong cùng một transaction, việc đọc lại một dòng dữ liệu sẽ luôn cho cùng một kết quả. Ngăn chặn Non-Repeatable Read. Đây là mức **mặc định của MySQL**.
- **Ưu điểm**: Tăng cường tính nhất quán cho các transaction cần đọc dữ liệu nhiều lần và xử lý dựa trên kết quả đó.
- **Nhược điểm**: Có thể gây giảm hiệu suất do giữ lock lâu hơn. Vẫn có thể xảy ra Phantom Read.
- **Khi nào dùng?**: Khi một transaction thực hiện logic phức tạp: đọc dữ liệu, xử lý, rồi ghi lại dựa trên dữ liệu đã đọc. Ví dụ: cập nhật số lượng tồn kho dựa trên số lượng đã đọc lúc đầu.

### d. `SERIALIZABLE`
- **Mô tả**: Mức cô lập cao nhất. Các transaction được thực thi một cách tuần tự (từng cái một). Ngăn chặn tất cả các vấn đề concurrency.
- **Ưu điểm**: Đảm bảo tính toàn vẹn dữ liệu ở mức cao nhất.
- **Nhược điểm**: Hiệu suất thấp nhất, giảm mạnh khả năng xử lý đồng thời.
- **Khi nào dùng?**: Khi tính chính xác của dữ liệu là tối quan trọng và không thể có sai sót. Ví dụ: các hệ thống tài chính, xử lý giao dịch ngân hàng, hoặc khi tạo báo cáo tài chính yêu cầu snapshot dữ liệu tại một thời điểm.

## 4. Cách cấu hình trong Spring

Bạn có thể dễ dàng thiết lập Isolation Level trong annotation `@Transactional`.

```java
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    // Sử dụng mức mặc định của DB (thường là READ_COMMITTED)
    @Transactional
    public void normalTransfer(...) { ... }

    // Dùng cho các báo cáo cần dữ liệu nhất quán trong suốt quá trình đọc
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public Report generateReport(...) { ... }

    // Dùng cho các tác vụ chỉ đọc, không quan trọng dữ liệu có thay đổi hay không
    @Transactional(isolation = Isolation.READ_COMMITTED, readOnly = true)
    public AccountDetails getAccountDetails(...) { ... }

    // Dùng cho các tác vụ cực kỳ quan trọng, ví dụ quyết toán cuối ngày
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void performFinancialClosing(...) { ... }
}
```

## 5. Kết luận

- **Không có "one-size-fits-all"**: Lựa chọn Isolation Level phụ thuộc hoàn toàn vào **yêu cầu nghiệp vụ** và **sự đánh đổi** bạn sẵn sàng chấp nhận.
- **Bắt đầu với `READ_COMMITTED`**: Đây là mức mặc định hợp lý cho hầu hết các trường hợp.
- **Chỉ tăng mức Isolation khi thực sự cần thiết**: Nếu nghiệp vụ yêu cầu đọc dữ liệu nhất quán (ví dụ: đọc, tính toán, ghi), hãy cân nhắc `REPEATABLE_READ`.
- **Sử dụng `SERIALIZABLE` một cách thận trọng**: Chỉ dùng cho các nghiệp vụ tối quan trọng và chấp nhận giảm hiệu suất.
- **Kết hợp với `readOnly = true`**: Đối với các tác vụ chỉ đọc, việc đặt `readOnly = true` giúp Spring và persistence provider thực hiện các tối ưu hóa, cải thiện hiệu suất.