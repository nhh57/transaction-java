# Transaction Isolation Levels in Spring

## 1. Khái niệm Isolation

Trong cơ sở dữ liệu, **Isolation** (Tính cô lập) là một trong bốn thuộc tính ACID (Atomicity, Consistency, Isolation, Durability) của các giao dịch (transactions). Nó đảm bảo rằng các giao dịch đồng thời (concurrent transactions) không can thiệp vào nhau. Nói cách khác, một giao dịch sẽ không nhìn thấy những thay đổi chưa được commit của một giao dịch khác.

Mức độ cô lập càng cao thì dữ liệu càng chính xác, nhưng hiệu suất có thể bị ảnh hưởng do phải khóa nhiều tài nguyên hơn. Ngược lại, mức độ cô lập thấp hơn có thể tăng hiệu suất nhưng có nguy cơ gặp phải các vấn đề về đọc dữ liệu không nhất quán.

## 2. Các vấn đề phát sinh khi Isolation không đủ

Có ba vấn đề chính mà các mức độ cô lập khác nhau cố gắng giải quyết:

### 2.1. Dirty Reads (Đọc dữ liệu bẩn)

*   **Mô tả:** Xảy ra khi một giao dịch đọc dữ liệu mà một giao dịch khác đã thay đổi nhưng chưa được commit. Nếu giao dịch thay đổi dữ liệu đó sau này bị rollback, thì dữ liệu mà giao dịch đầu tiên đã đọc sẽ không bao giờ tồn tại trong cơ sở dữ liệu.
*   **Ví dụ:**
    1.  **Giao dịch A** bắt đầu.
    2.  **Giao dịch A** cập nhật số dư tài khoản từ `100` thành `50` (chưa commit).
    3.  **Giao dịch B** bắt đầu.
    4.  **Giao dịch B** đọc số dư tài khoản và thấy là `50`.
    5.  **Giao dịch A** bị lỗi và rollback số dư về `100`.
    6.  **Giao dịch B** đã đọc một giá trị "bẩn" (`50`) mà không bao giờ được commit.

### 2.2. Non-Repeatable Reads (Đọc không lặp lại)

*   **Mô tả:** Xảy ra khi một giao dịch đọc cùng một hàng dữ liệu hai lần, và giữa hai lần đọc đó, một giao dịch khác commit thay đổi vào hàng dữ liệu đó. Kết quả là, hai lần đọc trả về các giá trị khác nhau cho cùng một hàng.
*   **Ví dụ:**
    1.  **Giao dịch A** bắt đầu.
    2.  **Giao dịch A** đọc số dư tài khoản là `100`.
    3.  **Giao dịch B** bắt đầu.
    4.  **Giao dịch B** cập nhật số dư tài khoản từ `100` thành `150` và commit.
    5.  **Giao dịch A** đọc lại số dư tài khoản và thấy là `150`.
    6.  **Giao dịch A** nhận được hai giá trị khác nhau cho cùng một dữ liệu trong cùng một transaction.

### 2.3. Phantom Reads (Đọc dữ liệu "ma")

*   **Mô tả:** Tương tự như Non-Repeatable Reads, nhưng xảy ra khi một giao dịch thực hiện một truy vấn tìm kiếm (ví dụ: `SELECT ... WHERE ...`) và sau đó thực hiện lại cùng một truy vấn, nhưng giữa hai lần truy vấn đó, một giao dịch khác đã chèn hoặc xóa các hàng dữ liệu phù hợp với điều kiện tìm kiếm. Kết quả là, số lượng hàng trả về thay đổi.
*   **Ví dụ:**
    1.  **Giao dịch A** bắt đầu.
    2.  **Giao dịch A** thực hiện `SELECT COUNT(*) FROM Accounts WHERE balance > 0` và nhận được `5` hàng.
    3.  **Giao dịch B** bắt đầu.
    4.  **Giao dịch B** chèn một tài khoản mới có số dư dương và commit.
    5.  **Giao dịch A** thực hiện lại `SELECT COUNT(*) FROM Accounts WHERE balance > 0` và nhận được `6` hàng.
    6.  **Giao dịch A** thấy "hàng ma" (phantom row) xuất hiện mà trước đó không có.

## 3. Các cấp độ Isolation trong Spring

Spring Framework hỗ trợ các cấp độ cô lập giao dịch (Isolation Levels) được định nghĩa trong JDBC và SQL standard. Bạn có thể cấu hình chúng bằng cách sử dụng thuộc tính `isolation` trong annotation `@Transactional`.

### 3.1. `Isolation.READ_UNCOMMITTED`

*   **Mô tả:** Đây là cấp độ cô lập thấp nhất. Một giao dịch có thể đọc dữ liệu mà các giao dịch khác đã thay đổi nhưng chưa được commit.
*   **Vấn đề có thể xảy ra:** Chịu ảnh hưởng của **Dirty Reads**, **Non-Repeatable Reads**, và **Phantom Reads**.
*   **Ưu điểm:** Hiệu suất cao nhất vì không có khóa đọc (read lock) hoặc rất ít.
*   **Nhược điểm:** Dễ dẫn đến dữ liệu không nhất quán, không phù hợp cho hầu hết các ứng dụng doanh nghiệp.
*   **Sử dụng:** Rất hiếm khi được sử dụng trong thực tế, có thể trong các trường hợp mà dữ liệu không yêu cầu độ chính xác cao và cần hiệu suất cực kỳ nhanh.

### 3.2. `Isolation.READ_COMMITTED`

*   **Mô tả:** Một giao dịch chỉ có thể đọc dữ liệu đã được commit bởi các giao dịch khác. Đây là cấp độ cô lập mặc định trong nhiều hệ quản trị cơ sở dữ liệu (ví dụ: PostgreSQL, SQL Server, Oracle).
*   **Vấn đề giải quyết:** Ngăn chặn **Dirty Reads**.
*   **Vấn đề có thể xảy ra:** Vẫn có thể xảy ra **Non-Repeatable Reads** và **Phantom Reads**.
*   **Ưu điểm:** Cân bằng tốt giữa hiệu suất và tính nhất quán.
*   **Sử dụng:** Phổ biến nhất và thường là lựa chọn tốt cho hầu hết các ứng dụng.

### 3.3. `Isolation.REPEATABLE_READ`

*   **Mô tả:** Một giao dịch đảm bảo rằng nếu nó đọc một hàng dữ liệu, thì bất kỳ lần đọc nào tiếp theo của hàng đó trong cùng một giao dịch sẽ trả về cùng một giá trị, ngay cả khi các giao dịch khác đã commit thay đổi cho hàng đó. Điều này thường đạt được bằng cách khóa các hàng đã đọc.
*   **Vấn đề giải quyết:** Ngăn chặn **Dirty Reads** và **Non-Repeatable Reads**.
*   **Vấn đề có thể xảy ra:** Vẫn có thể xảy ra **Phantom Reads**. (Trong MySQL, `REPEATABLE_READ` ngăn chặn cả Phantom Reads bằng cách sử dụng next-key locking).
*   **Ưu điểm:** Tăng tính nhất quán dữ liệu so với `READ_COMMITTED`.
*   **Nhược điểm:** Hiệu suất có thể giảm do cần duy trì khóa đọc lâu hơn.
*   **Sử dụng:** Khi cần đảm bảo rằng các lần đọc lặp lại của cùng một dữ liệu trong một giao dịch sẽ trả về kết quả giống nhau.

### 3.4. `Isolation.SERIALIZABLE`

*   **Mô tả:** Đây là cấp độ cô lập cao nhất. Nó đảm bảo rằng các giao dịch đồng thời được thực hiện theo cách tương đương với việc chúng được thực hiện tuần tự (từng cái một). Điều này thường đạt được bằng cách khóa toàn bộ bảng hoặc sử dụng các cơ chế khóa phức tạp khác.
*   **Vấn đề giải quyết:** Ngăn chặn tất cả các vấn đề: **Dirty Reads**, **Non-Repeatable Reads**, và **Phantom Reads**.
*   **Ưu điểm:** Đảm bảo tính nhất quán dữ liệu cao nhất, không có bất kỳ sự không nhất quán nào do tương tranh.
*   **Nhược điểm:** Hiệu suất thấp nhất vì nó có thể gây ra nhiều tranh chấp khóa và làm giảm khả năng đồng thời (concurrency).
*   **Sử dụng:** Chỉ nên sử dụng khi tính nhất quán dữ liệu là cực kỳ quan trọng và bạn có thể chấp nhận sự đánh đổi về hiệu suất (ví dụ: các hệ thống tài chính với các báo cáo cực kỳ nhạy cảm).

### 3.5. Tóm tắt các vấn đề được giải quyết bởi các cấp độ Isolation

| Isolation Level     | Dirty Read | Non-Repeatable Read | Phantom Read |
| :------------------ | :--------: | :-----------------: | :----------: |
| `READ_UNCOMMITTED`  |            |                     |              |
| `READ_COMMITTED`    |     ✓      |                     |              |
| `REPEATABLE_READ`   |     ✓      |          ✓          |              |
| `SERIALIZABLE`      |     ✓      |          ✓          |      ✓       |

**Lưu ý:** Hành vi chính xác của từng cấp độ cô lập có thể hơi khác nhau giữa các hệ quản trị cơ sở dữ liệu (DBMS) khác nhau (ví dụ: MySQL, PostgreSQL, Oracle, SQL Server) do cách triển khai khóa và MVCC (Multi-Version Concurrency Control). Tuy nhiên, các nguyên tắc cơ bản và các vấn đề mà chúng giải quyết vẫn nhất quán.

## 4. Hướng dẫn chạy và kiểm tra các ví dụ Isolation

Để chạy và kiểm tra các ví dụ về Isolation, bạn cần thực hiện các bước sau:

### 4.1. Khởi động ứng dụng Spring Boot

Đảm bảo rằng bạn đã cấu hình cơ sở dữ liệu (ví dụ: H2 in-memory hoặc MySQL/PostgreSQL) trong `src/main/resources/application.properties` và ứng dụng có thể kết nối thành công.

Bạn có thể chạy ứng dụng từ IDE (như VS Code, IntelliJ IDEA) bằng cách chạy file `src/main/java/com/hainh.Main.java` hoặc từ terminal bằng lệnh Maven:

```bash
./mvnw spring-boot:run
```

### 4.2. Khởi tạo dữ liệu

Trước khi chạy các demo, bạn cần khởi tạo dữ liệu ban đầu. Mở công cụ kiểm tra API (như Postman, Insomnia, hoặc dùng `curl`) và gửi một request POST đến endpoint sau:

*   **URL:** `http://localhost:8080/api/isolation-demo/init-data`
*   **Method:** `POST`

Request này sẽ xóa tất cả các tài khoản hiện có và tạo hai tài khoản mới: "Alice" với số dư 1000.0 và "Bob" với số dư 500.0.

### 4.3. Chạy các ví dụ minh họa

Sử dụng Postman/Insomnia hoặc `curl` để gửi các request GET đến các endpoint sau. Để thấy rõ hiệu ứng của Isolation, bạn cần gửi các request này *gần như đồng thời* hoặc theo trình tự được hướng dẫn trong mô tả của từng demo.

**Lưu ý quan trọng:** Các demo này sử dụng `ExecutorService` để mô phỏng các giao dịch chạy đồng thời. Tuy nhiên, để thấy rõ nhất các vấn đề, bạn có thể cần chạy các request từ nhiều tab/cửa sổ Postman/terminal khác nhau hoặc sử dụng các công cụ kiểm tra hiệu năng.

#### 4.3.1. Dirty Read Demo (`Isolation.READ_UNCOMMITTED`)

*   **URL:** `http://localhost:8080/api/isolation-demo/dirty-read/{accountId}` (thay `{accountId}` bằng ID của tài khoản Alice sau khi `init-data`)
*   **Method:** `GET`

Endpoint này sẽ mô phỏng:
*   Một "DirtyWriter" transaction bắt đầu cập nhật tài khoản và sau đó cố ý gây ra lỗi để rollback.
*   Một "DirtyReader" transaction (với `Isolation.READ_UNCOMMITTED`) sẽ đọc dữ liệu tài khoản trong khi "DirtyWriter" đang cập nhật (trạng thái "bẩn").
Bạn sẽ thấy "DirtyReader" đọc được giá trị `9999.0` (giá trị chưa commit) và sau đó giá trị cuối cùng sau khi writer rollback sẽ trở về `1000.0`.

#### 4.3.2. Non-Repeatable Read Demo (`Isolation.READ_COMMITTED`)

*   **URL:** `http://localhost:8080/api/isolation-demo/non-repeatable-read/{accountId}` (thay `{accountId}` bằng ID của tài khoản Alice)
*   **Method:** `GET`

Endpoint này sẽ mô phỏng:
*   Một "Reader" transaction đọc số dư tài khoản lần đầu.
*   Một "Writer" transaction cập nhật số dư tài khoản và commit.
*   "Reader" transaction đọc lại số dư tài khoản lần thứ hai.
Bạn sẽ thấy rằng số dư đọc lần đầu và lần hai của "Reader" là khác nhau, mặc dù nó đang ở cấp độ `READ_COMMITTED` (ngăn Dirty Reads).

#### 4.3.3. Phantom Read Demo (`Isolation.REPEATABLE_READ`)

*   **URL:** `http://localhost:8080/api/isolation-demo/phantom-read`
*   **Method:** `GET`

Endpoint này sẽ mô phỏng:
*   Một "Reader" transaction đếm số lượng tài khoản với một tiền tố owner nhất định lần đầu.
*   Một "Writer" transaction thêm một tài khoản mới với tiền tố owner đó và commit.
*   "Reader" transaction đếm lại số lượng tài khoản lần thứ hai.
Bạn sẽ thấy rằng số lượng tài khoản đếm lần đầu và lần hai của "Reader" là khác nhau, mặc dù nó đang ở cấp độ `REPEATABLE_READ` (ngăn Non-Repeatable Reads).

#### 4.3.4. Serializable Demo (`Isolation.SERIALIZABLE`)

*   **URL:** `http://localhost:8080/api/isolation-demo/serializable-demo/{accountId}` (thay `{accountId}` bằng ID của tài khoản Alice)
*   **Method:** `GET`

Endpoint này sẽ mô phỏng:
*   Một "Reader" transaction (với `Isolation.SERIALIZABLE`) đọc số dư tài khoản lần đầu.
*   Một "Writer" transaction cố gắng cập nhật số dư tài khoản.
*   "Reader" transaction đọc lại số dư tài khoản lần thứ hai.
Bạn sẽ thấy rằng "Reader" transaction sẽ đọc được cùng một giá trị ở cả hai lần, và "Writer" transaction có thể bị block hoặc gặp lỗi `LockAcquisitionException`/`DeadlockLoserDataAccessException` tùy thuộc vào cài đặt DB và thời gian thực thi. Điều này chứng tỏ `SERIALIZABLE` ngăn chặn tất cả các vấn đề.

### 4.4. Phân tích kết quả và Log

*   **Kiểm tra phản hồi API:** Mỗi endpoint demo sẽ trả về một chuỗi kết quả mô tả những gì đã xảy ra.
*   **Theo dõi Log:** Quan trọng nhất, hãy theo dõi log console của ứng dụng Spring Boot. Các thông điệp `logger.info` và `logger.debug` trong `IsolationDemoService` và `IsolationDemoController` sẽ cung cấp cái nhìn chi tiết về trình tự các sự kiện và giá trị đọc được tại các thời điểm khác nhau trong các giao dịch đồng thời. Điều này giúp bạn hiểu rõ hơn cách các cấp độ Isolation hoạt động.

## 5. Khuyến nghị và Best Practices khi lựa chọn Isolation Level

Việc lựa chọn Isolation Level phù hợp là rất quan trọng vì nó ảnh hưởng trực tiếp đến hiệu suất và tính nhất quán dữ liệu của ứng dụng. Dưới đây là một số khuyến nghị và best practices:

### 5.1. Luôn ưu tiên `Isolation.READ_COMMITTED`

*   **Lý do:** Đây là cấp độ mặc định và phổ biến nhất trong hầu hết các hệ quản trị cơ sở dữ liệu hiện đại (ngoại trừ MySQL mặc định là `REPEATABLE_READ`). Nó cung cấp sự cân bằng tốt giữa hiệu suất và tính nhất quán. `READ_COMMITTED` ngăn chặn Dirty Reads, đủ tốt cho đa số các ứng dụng.
*   **Khi nào sử dụng:** Hầu hết các giao dịch đọc và ghi thông thường.

### 5.2. Cẩn thận với `Isolation.READ_UNCOMMITTED`

*   **Lý do:** Cấp độ này cho phép Dirty Reads, dẫn đến dữ liệu không nhất quán và khó dự đoán.
*   **Khi nào sử dụng:** Rất hiếm khi được sử dụng. Chỉ xem xét trong các trường hợp mà hiệu suất là tối thượng và việc đọc dữ liệu không nhất quán tạm thời là chấp nhận được (ví dụ: các hệ thống báo cáo không quan trọng, nơi dữ liệu có thể được làm mới sau này). Tuyệt đối không sử dụng cho các giao dịch liên quan đến tài chính hoặc dữ liệu nhạy cảm.

### 5.3. Sử dụng `Isolation.REPEATABLE_READ` khi cần đọc nhất quán

*   **Lý do:** Ngăn chặn cả Dirty Reads và Non-Repeatable Reads, đảm bảo rằng một giao dịch sẽ luôn đọc cùng một giá trị cho một hàng cụ thể trong suốt thời gian tồn tại của nó.
*   **Khi nào sử dụng:**
    *   Khi bạn cần thực hiện nhiều lần đọc cùng một dữ liệu trong một giao dịch và yêu cầu tất cả các lần đọc đó phải trả về cùng một kết quả.
    *   Các báo cáo hoặc tính toán phức tạp dựa trên một tập hợp dữ liệu không thay đổi trong suốt giao dịch.
*   **Lưu ý:** MySQL sử dụng `REPEATABLE_READ` làm mặc định và thường ngăn chặn cả Phantom Reads thông qua cơ chế next-key locking. Tuy nhiên, hành vi này có thể khác ở các DBMS khác.

### 5.4. `Isolation.SERIALIZABLE` là lựa chọn cuối cùng

*   **Lý do:** Cấp độ này cung cấp mức độ nhất quán cao nhất bằng cách ngăn chặn tất cả các vấn đề (Dirty Reads, Non-Repeatable Reads, Phantom Reads). Tuy nhiên, nó đạt được điều này bằng cách giảm đáng kể tính đồng thời (concurrency) thông qua việc khóa rộng rãi hoặc tuần tự hóa các giao dịch. Điều này có thể dẫn đến tắc nghẽn hiệu suất nghiêm trọng và Deadlocks.
*   **Khi nào sử dụng:**
    *   Chỉ khi bạn có yêu cầu cực kỳ nghiêm ngặt về tính nhất quán dữ liệu và không thể chấp nhận bất kỳ hình thức đọc không nhất quán nào.
    *   Trong các hệ thống tài chính, kiểm toán, hoặc các giao dịch pháp lý nơi dữ liệu phải chính xác 100% mọi lúc.
    *   Khi bạn đã thử các cấp độ thấp hơn và vẫn gặp phải các vấn đề không thể chấp nhận được do tương tranh.
*   **Cân nhắc:** Trước khi sử dụng `SERIALIZABLE`, hãy cân nhắc kỹ lưỡng về tác động hiệu suất và tìm kiếm các giải pháp khác như tối ưu hóa truy vấn, thiết kế lại lược đồ database, hoặc sử dụng các cơ chế khóa ứng dụng (application-level locking) nếu phù hợp.

### 5.5. Một số Best Practices khác

*   **Hiểu rõ DBMS của bạn:** Mỗi hệ quản trị cơ sở dữ liệu có thể có cách triển khai Isolation Levels hơi khác nhau. Hãy đọc tài liệu của DBMS bạn đang sử dụng để hiểu rõ hành vi chính xác.
*   **Giữ giao dịch càng ngắn càng tốt:** Giao dịch dài làm tăng khả năng xảy ra tranh chấp khóa và làm giảm hiệu suất.
*   **Sử dụng @Transactional một cách có chọn lọc:** Chỉ áp dụng `@Transactional` cho các phương thức thực sự cần transaction.
*   **Xử lý ngoại lệ:** Đảm bảo rằng bạn xử lý các ngoại lệ trong giao dịch đúng cách để đảm bảo rollback khi cần thiết.
*   **Theo dõi và điều chỉnh:** Sử dụng các công cụ giám sát hiệu suất và log để theo dõi hành vi của các giao dịch trong môi trường thực tế và điều chỉnh Isolation Level nếu cần.
</content>
</content>