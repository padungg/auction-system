# Kiến Trúc, Design Patterns & Điểm Đặc Sắc Của Dự Án

Dự án Đấu giá Trực tuyến không chỉ là một ứng dụng CRUD thông thường mà mang hình dáng của một hệ thống phân tán, xử lý đồng thời (Concurrent System) với kiến trúc mạng thời gian thực. Dưới đây là phân tích chuyên sâu về hệ thống.

---

## Phần 1: Các Mẫu Thiết Kế (Design Patterns) Được Sử Dụng

### 1. Observer Pattern (Mẫu Quan Sát Viên)
Đây là "trái tim" của tính năng Real-time trong dự án.
*   **Ở phía Server:** `AuctionManager` đóng vai trò là *Subject*. Các luồng kết nối `ClientHandler` đóng vai trò là *Observer*. Khi có người đặt giá (`BidService`), Server gọi `notifyBidUpdate()` để phát sóng (Push JSON) sự kiện cho toàn bộ các Client đang mở màn hình phiên đấu giá đó.
*   **Ở phía Client:** `AuctionEventDispatcher` quản lý danh sách các UI Controller. Giao diện tự động cập nhật lại nhãn Giá Hiện Tại hoặc gia hạn Thời Gian ngay lập tức khi nhận được sự kiện ngầm từ mạng.

### 2. Singleton Pattern (Mẫu Khởi Tạo Duy Nhất)
Đảm bảo hệ thống tiết kiệm RAM và chỉ có duy nhất một bộ quản lý tập trung cho toàn ứng dụng.
*   `ClientSocketManager.getInstance()`: Duy trì duy nhất 1 đường ống kết nối mạng từ Client lên Server.
*   `AuctionManager.getInstance()`: Đài phát thanh trung tâm của hệ thống.
*   `ImageCache.getInstance()`: Quản lý bộ nhớ đệm hình ảnh tập trung.

### 3. MVC (Model - View - Controller)
Kiến trúc kinh điển giúp phân tách rõ ràng giao diện và logic.
*   **View:** Các file `.fxml` (Giao diện người dùng JavaFX).
*   **Controller:** Các file Controller (`ProductCardController`, `MainController`) xử lý sự kiện và đẩy dữ liệu lên View.
*   **Model:** Các thực thể (Auction, User, BidTransaction) và DTO.

### 4. DAO Pattern (Data Access Object)
Che giấu sự phức tạp của cơ sở dữ liệu. Tầng Service (`BidService`) không cần quan tâm chi tiết về SQL, chúng chỉ gọi `auctionDAO.findById()`. Các Class DAO đảm nhiệm việc viết câu lệnh truy vấn và ánh xạ kết quả.

### 5. Strategy / Command Router Pattern (Tại Server)
Thay vì dùng REST API thông thường, hệ thống tự dựng giao thức TCP.
Tại lớp xử lý mạng, mỗi gói tin JSON gửi lên có chứa `RequestType` (`PLACE_BID`, `LOGIN`). Hệ thống định tuyến lệnh đó đến một Handler cụ thể (như `BidHandler`, `AuthHandler`). Điều này giúp mã nguồn Server cực kỳ dễ bảo trì và mở rộng.

---

## Phần 2: Những Điểm Đặc Sắc & Kỹ Thuật Nâng Cao

### 1. Xử Lý Bất Đồng Bộ Đa Luồng (Async / Multithreading)
*   Đẩy tất cả tác vụ gọi mạng (Network I/O) xuống luồng ngầm qua `ClientSocketManager.getInstance().execute()`.
*   **Đa luồng giải mã hình ảnh:** Khi tải danh sách sản phẩm, việc giải mã chuỗi Base64 dài được đưa xuống `CompletableFuture.supplyAsync()`, và chỉ dùng `Platform.runLater()` khi ảnh sẵn sàng, giúp UI mượt mà không bị treo.

### 2. Quản Lý Đấu Giá Đồng Thời (Concurrent Bidding - Chống Race Condition)
Hệ thống áp dụng cơ chế khóa chi tiết **Fine-Grained Locking** thông qua `LockManager.getAuctionLock(auctionId)`. Khi hàng trăm người cùng "Đặt giá", Server sẽ xếp hàng xử lý tuần tự theo từng ID phiên đấu giá. Điều này chặn đứng hoàn toàn lỗi Lost Update (mất dữ liệu) hay "hai người cùng thắng".

### 3. Thuật Toán Chống Bắn Tỉa (Anti-Sniping) Đồng Bộ Realtime
Để ngăn "đợi giây cuối cùng mới đặt giá để cướp hàng", hệ thống tự động kiểm tra: Nếu có người Bid trong 60s cuối, Server tự cộng thêm 120s. Server nén `newEndTime` vào gói tin Event phát đi. Màn hình Client đang xem sẽ bắt được gói tin này và tự động cộng dồn đồng hồ đếm ngược ngay lập tức.

### 4. Hệ Thống Đấu Giá Tự Động (Auto-Bid Engine)
Người dùng nhập mức giá tối đa (Max) và Bước nhảy (Inc). Khi có người khác đặt giá, hàm `runAutoBids` sẽ tự động nhảy vào đại diện cho chủ nhân "đấu tay đôi" ngay trong vòng 1 mili-giây, đẩy giá lên liên tục cho đến khi một bên kiệt sức, tất cả gói gọn trong một giao dịch an toàn đa luồng.

### 5. Giao Thức Mạng Tự Hành Bằng Socket TCP
Hệ thống tự code TCP Socket + JSON multiplexing. Tính năng ánh xạ Request-Response bằng `requestId` kết hợp với thẻ `CompletableFuture` (chờ đợi tối đa 10s cho Timeout) mô phỏng chính xác cách các framework lớn xử lý I/O bất đồng bộ.

### 6. Quản Lý Connection Pooling Hiện Đại (HikariCP + Supabase)
Hệ thống áp dụng `HikariDataSource`, tự động duy trì một nhóm các kết nối cơ sở dữ liệu trên mây sống sẵn trong RAM. Nó cấp phát kết nối ngay lập tức thay vì khởi tạo lại từ đầu, gia tăng tốc độ tải trang lên gấp nhiều lần.
