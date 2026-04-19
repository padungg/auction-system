# KẾ HOẠCH VÀ LỘ TRÌNH PHÁT TRIỂN SERVER (DỰA TRÊN KIẾN TRÚC THỰC TẾ)

**Vai trò thực hiện:** Lập trình viên Backend / Server
**Mục tiêu:** Báo cáo chi tiết quá trình xây dựng hệ thống phần mềm Server cho ứng dụng Đấu giá trực tuyến. Kiến trúc thực tế kết hợp Socket đa luồng, mô hình Controller-Service-DAO và các Mẫu thiết kế (Design Patterns) chuẩn xác dựa trên codebase hiện tại.

Dưới đây là phần giải trình 5 giai đoạn phát triển để gửi đến Thầy Cô, khớp hoàn toàn với cấu trúc source code trong gói thư mục `com.auction.server`.

---

## Giai đoạn 1: Nền tảng luồng giao tiếp và Định hướng dữ liệu (Tuần 1)
*Xây dựng cổng giao tiếp mạng vững chắc và điều phối (Routing) gói tin thông minh trước khi làm logic.*

* **Nhiệm vụ 1.1: Trái tim Server (ServerApp & ClientHandler):**
    * Ứng dụng **Java Sockets**. Khởi tạo class trung tâm `ServerApp` lắng nghe tại một Port tĩnh.
    * Giải quyết bài toán đa luồng bằng Thread Pool (`Executors.newFixedThreadPool`): Mỗi kết nối Client được giao cho một `ClientHandler` độc lập.
* **Nhiệm vụ 1.2: Định dạng dữ liệu và Khởi tạo Gson:**
    * Lựa chọn chuẩn truyền thông là JSON vì tính dễ đọc và phân tích. Cài đặt lớp `GsonConfig` để lo việc serialize / deserialize các đối tượng Java sang JSON.
* **Nhiệm vụ 1.3: Cài đặt hệ thống Định tuyến Request (Router):**
    * Viết lớp `RequestRouter`: Khi `ClientHandler` nhận data, nó ném Payload vào Router. Router sẽ đọc lệnh (Command) và chuyển tiếp đúng đến chức năng cần giải quyết để tránh viết Code dồn cục ở Socket.

---

## Giai đoạn 2: Kiến trúc N-Tier và Khởi tạo Mẫu Thiết Kế (Tuần 2)
*Áp dụng Kiến trúc chuẩn và chuẩn bị dữ liệu.*

* **Nhiệm vụ 2.1: Chia tách cấu trúc Controller - Service - DAO:**
    * Tuân thủ triệt để nguyên lý Single Responsibility. 
    * Xây dựng tầng `Controller` (`UserController`, `AuctionController`, `BidController`) để nhận JSON, giải mã.
    * Tạo tầng `Service` (`UserService`, `AuctionService`, `BidService`) để thực hiện các tính toán logic.
    * Tạo tầng `DAO` kết nối DB.
* **Nhiệm vụ 2.2: Lập trình với Mock Object:**
    * Do thời gian đầu chưa chuẩn hóa được Database cùng team, chủ động viết các lớp tạo dữ liệu giả lập (`UserDAOMock`, `ItemDAOMock`, `AuctionDAOMock`, `BidTransactionDAOMock`). Nhờ thế tầng Logic gõ code mà không bị gián đoạn.
* **Nhiệm vụ 2.3: Quản lý bộ nhớ bằng Singleton Pattern:**
    * Thiết lập giới hạn duy nhất 1 phiên bản bộ nhớ cho `DatabaseConnection` (không cho kết nối dư thừa đến DB) và `AuctionManager` (Trung tâm đầu não quản lý trạng thái các phòng đấu).

---

## Giai đoạn 3: Phát triển Xử lý Mức giá và Thuật Toán Đấu Giá (Tuần 3)
*Tập trung xử lý logic ở tầng Service và sự linh hoạt của Luật Đấu giá.*

* **Nhiệm vụ 3.1: Xác minh giá thầu (Bid Engine):**
    * Cài đặt `BidService` kiểm tra kĩ càng (giá trị đặt phải lớn hơn giá hiện hành, phiên đấu giá đó còn mở hay không), viết xuống DAO lịch sử giao dịch.
* **Nhiệm vụ 3.2: Áp dụng Strategy Pattern (Chiến lược Đặt Giá):**
    * Triển khai mẫu Strategy (trong package `strategy`) với interface `BidStrategy`. 
    * Phân nhánh lớp thuật toán ra thành `NormalBidStrategy` (dành cho người bấm nút tay đặt từng giá) và `AutoBidStrategy` (dành cho bot hoặc những người dùng cài đặt máy tự đấu theo hạn mức). Cấu trúc này giúp dễ dàng thêm mới các bộ luật sau này để biểu diễn với giáo viên.
* **Nhiệm vụ 3.3: Thread-Safety (Bảo vệ dữ liệu):**
    * Thêm từ khóa `synchronized` và giải quyết xung đột khi luồng (Thread) gọi vào Service. Đặc biệt khi hệ thống đánh giá xem ai là người gõ búa nhanh nhất ở mili-giây cuối.

---

## Giai đoạn 4: Push Notification và Trí tuệ hệ thống (Tuần 4)
*Biến một Server đơn điệu chuyển sang chế độ Active-Push với Client bằng Observer Pattern.*

* **Nhiệm vụ 4.1: Cập nhật thông báo theo Observer Pattern:**
    * Tạo cấu trúc package `observer`. Áp dụng luồng `AuctionObserver`.
    * **Thực thi qua mạng:** Khi `BidService` ghi nhận có người đấu giá thủng sàn, `AuctionManager` (Subject) lập tức báo động và duyệt danh sách những `ClientHandler` nào đang đăng ký theo dõi món hàng này (Observers) để Push JSON cập nhật giá. Client sẽ nảy số Real-time ngay lập tức.
* **Nhiệm vụ 4.2: Luồng kết nối với tầng Repository:**
    * Chuyển hóa toàn bộ `DAOMock` thành DAO thật đọc ghi thẳng bằng thư viện JDBC/Hibernate xuống bảng MySQL/SQL Server thông qua `DatabaseConnection` Singleton.

---

## Giai đoạn 5: Tối ưu và Báo Cáo Triển Khai (Tuần 5)
*Hoàn chỉnh đường đi dữ liệu và test lỗi.*

* **Nhiệm vụ 5.1: Xử lý đứt mạng, Garbage Collection:**
    * Bắt sự kiện IOException ở `ClientHandler` khi Client rơi mạng đột ngột. Gỡ Observer của Client đó khỏi `AuctionManager` để giải phóng bộ nhớ.
    * Tắt Server êm dịu (Graceful Shutdown) bằng lệnh `threadPool.shutdown()`.
* **Nhiệm vụ 5.2: Viết báo cáo giải trình (Documentation):**
    * Demo sơ đồ chạy của Code dựa trên **ServerApp** (Client → ClientHandler → Router → Controller → Service → DAO).
    * Vẽ UML để giải trình bộ Strategy và hệ sinh thái Observer.
