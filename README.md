#  HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN (UET Auction System)

[![Java Version](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/projects/jdk/25/)
[![JavaFX](https://img.shields.io/badge/JavaFX-22.0.1-blue.svg)](https://openjfx.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Supabase-336791.svg)](https://supabase.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-red.svg)](https://maven.apache.org/)
[![Build Status](https://img.shields.io/github/actions/workflow/status/padungg/auction-system/maven.yml?branch=main&label=CI%2FCD)](https://github.com/padungg/auction-system/actions)

---

##  MỤC LỤC
1. [Mô tả ngắn gọn bài toán và phạm vi hệ thống](#1-mô-tả-ngắn-gọn-bài-toán-và-phạm-vi-hệ-thống)
2. [Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt](#2-công-nghệ-sử-dụng-môi-trường-chạy-và-yêu-cầu-cài-đặt)
3. [Cấu trúc thư mục hoặc các module chính](#3-cấu-trúc-thư-mục-hoặc-các-module-chính)
4. [Vị trí các file .jar](#4-vị-trí-các-file-jar)
5. [Hướng dẫn chạy Server/Client theo thứ tự cụ thể](#5-hướng-dẫn-chạy-serverclient-theo-thứ-tự-cụ-thể)
6. [Danh sách chức năng đã hoàn thành](#6-danh-sách-chức-năng-đã-hoàn-thành)
7. [Phân công nhiệm vụ trong nhóm](#7-phân-công-nhiệm-vụ-trong-nhóm)
8. [Link báo cáo PDF và video demo](#8-link-báo-cáo-pdf-và-video-demo)

---

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống

### Bài toán Đặt ra
Hệ thống giải quyết bài toán **mô phỏng quy trình đấu giá tài sản trực tuyến theo thời gian thực (Real-time)**. 
- **Người bán** có nhu cầu thanh lý sản phẩm với mức giá tốt nhất thông qua cơ chế đặt giá cạnh tranh công khai trong khoảng thời gian xác định.
- **Người mua** có nhu cầu tìm kiếm, theo dõi và tham gia đấu giá các sản phẩm quan tâm một cách minh bạch, an toàn.
- **Hệ thống** chịu trách nhiệm điều phối dòng dữ liệu, tính toán giá thầu cao nhất tức thời, xử lý thanh toán tự động khi kết thúc phiên đấu giá và giải quyết tranh chấp đồng thời khi nhiều người đặt giá cùng một thời điểm.

### Phạm vi Hệ thống
Hệ thống được giới hạn trong việc cung cấp một nền tảng giao dịch trực tuyến qua mạng (LAN/Internet) với các phạm vi chính sau:
* **Môi trường & Kiến trúc triển khai**: Ứng dụng Desktop giao tiếp theo mô hình Client - Server thông qua kết nối TCP Socket (truyền tải dữ liệu JSON).
* **Quản lý Tài khoản & Phân quyền**: Phân tách rõ ràng chức năng của Quản trị viên (Admin) trong việc điều hành, và Thành viên (Member) tham gia với tư cách người mua/người bán.
* **Nghiệp vụ Đấu giá cốt lõi**: Hỗ trợ khởi tạo, giám sát và vận hành các phiên đấu giá tài sản (Phương tiện, Điện tử, Nghệ thuật) theo thời gian thực (bao gồm đặt giá thủ công, đặt giá tự động và chống bắn tỉa).
* **Quản lý Tài chính nội bộ**: Tích hợp hệ thống Ví điện tử (ảo) để ràng buộc điều kiện số dư khi tham gia đấu giá và thanh toán tự động ngay khi phiên kết thúc.

---

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ Sử dụng
* **Core Language**: Java 25 (OpenJDK).
* **Desktop UI**: JavaFX 22.0.1 (Kèm Scene Builder hỗ trợ kéo thả FXML).
* **Database**: PostgreSQL (Supabase Cloud Database).
* **JSON Serialization**: Google Gson 2.10.1.
* **Connection Pooling**: HikariCP 5.1.0 (Tối ưu hóa quản lý kết nối đến Database Cloud).
* **Build Tool**: Apache Maven.
* **Logging System**: SLF4J & Logback Classic.
* **Unit Testing**: JUnit 5 (Sử dụng **Manual Stubs In-Memory** để kiểm thử logic nghiệp vụ nhanh gọn, độc lập và tránh xung đột thư viện trên Java 25).
* **Coding Convention**: Checkstyle (Đảm bảo mã nguồn sạch theo chuẩn Google Java Style Guide).
* **CI/CD Pipeline**: GitHub Actions (Tự động biên dịch, chạy Unit Test và đóng gói file JAR trên server).

### Yêu cầu Môi trường Chạy & Cài đặt
* **Hệ điều hành**: Windows 10/11, macOS, hoặc Linux.
* **Java Development Kit**: JDK 25 cài sẵn trên máy (đã cấu hình biến môi trường `JAVA_HOME`).
* **Database Server**: Không cần cài đặt (Sử dụng Cloud Database PostgreSQL từ Supabase kết nối qua cổng Pooler 6543).
* **Cổng mạng Socket**: Server sử dụng cổng `8080` mặc định (Cần mở cổng nếu chạy trong mạng LAN/Internet).

---

## 3. Cấu trúc thư mục hoặc các module chính

Hệ thống được tổ chức chặt chẽ theo mô hình Client - Server tách biệt, phân chia thành các package rõ ràng:

```
copy/
├── .github/workflows/          # Cấu hình GitHub Actions CI/CD Pipeline
├── target/                     # Chứa mã nguồn đã biên dịch và file .jar sau khi đóng gói
├── src/
│   ├── main/
│   │   ├── java/com/auction/
│   │   │   ├── client/         # MODULE CLIENT (Giao diện người dùng JavaFX)
│   │   │   │   ├── controller/ # Các controller liên kết với file FXML xử lý sự kiện
│   │   │   │   ├── network/    # Kết nối Socket Client, xử lý gửi/nhận dữ liệu JSON
│   │   │   │   ├── observer/   # Pattern theo dõi cập nhật Realtime UI cho Client
│   │   │   │   ├── util/       # Các lớp tiện ích phía Client (validate, format,...)
│   │   │   │   ├── ClientApp.java # Lớp khởi chạy ứng dụng JavaFX chính
│   │   │   │   └── Launcher.java  # Điểm kích hoạt Client (Fix lỗi module path JavaFX)
│   │   │   ├── model/          # SHARED MODEL (Dùng chung cho cả Client & Server)
│   │   │   │   ├── dto/        # Lớp truyền nhận dữ liệu DTO giúp bảo mật thông tin
│   │   │   │   ├── entity/     # Thực thể nghiệp vụ (User, Item, Art, Vehicle,...)
│   │   │   │   ├── protocol/   # Cấu trúc Request/Response định nghĩa giao thức mạng
│   │   │   │   └── util/       # Tiện ích dùng chung cho Model
│   │   │   └── server/         # MODULE SERVER (Xử lý Socket, Services và Cơ sở dữ liệu)
│   │   │       ├── config/     # Dependency Injection container tự quản (AppConfig)
│   │   │       ├── controller/ # RequestController tiếp nhận và phân phối gói tin
│   │   │       ├── dao/        # Tầng truy xuất dữ liệu PostgreSQL (Interfaces & Impl)
│   │   │       ├── database/   # DatabaseConnection & DatabaseInitializer (Cấu hình DB)
│   │   │       ├── network/    # Luồng Socket Server & ClientHandler quản lý đa kết nối
│   │   │       ├── observer/   # Quản lý phát tin (Event/Notify) cho các Client
│   │   │       ├── service/    # Logic nghiệp vụ trung tâm (Auction, Bid, Wallet,...)
│   │   │       ├── util/       # Các lớp tiện ích hỗ trợ phía Server
│   │   │       └── ServerApp.java # Điểm khởi chạy Server chính
│   │   └── resources/          # THƯ MỤC TÀI NGUYÊN
│   │       ├── com/auction/client/ # Chứa toàn bộ các file giao diện .fxml và CSS
│   │       └── logback.xml     # Cấu hình hiển thị và lưu trữ log hệ thống
│   └── test/                   # THƯ MỤC KIỂM THỬ (Unit Test)
│       └── java/com/auction/   # Chứa các bộ test service sử dụng cấu trúc DB Stub
```

---

## 4. Vị trí các file .jar

Dự án sử dụng hệ thống CI/CD (GitHub Actions) để tự động đóng gói mã nguồn thành một file **Cross-Platform Fat JAR** duy nhất (đã tích hợp sẵn driver PostgreSQL, Gson, HikariCP và thư viện đồ họa JavaFX cho cả Windows, Linux, macOS). Để lấy file thực thi **`auction-system-1.0-SNAPSHOT.jar`** mà không cần biên dịch thủ công, thực hiện theo các bước sau:

1. **Truy cập**: Mục [Actions trên GitHub](https://github.com/padungg/auction-system/actions).
2. **Chọn bản build**: Bấm vào lần chạy (Workflow Run) gần nhất ở trên cùng có dấu tích xanh `✓`.
3. **Tải xuống**: Cuộn xuống cuối trang, tại phần **Artifacts**, tải tệp **`auction-system-executable`** (định dạng `.zip`).
4. **Giải nén**: Giải nén file `.zip` vừa tải để lấy file chạy chính **`auction-system-1.0-SNAPSHOT.jar`**.

---

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể

Hệ thống sử dụng cơ sở dữ liệu đám mây **Supabase PostgreSQL** đã được cấu hình sẵn dữ liệu mẫu. Người dùng không cần cài đặt SQL cục bộ, chỉ cần đảm bảo máy tính chạy Server **có kết nối mạng Internet**.

### Bước 1: Khởi chạy Server (Chạy đầu tiên)
1. Mở dòng lệnh (Terminal/Command Prompt) tại thư mục chứa file `.jar` đã tải về.
2. Chạy lệnh sau để bật Server:
   ```bash
   java -cp auction-system-1.0-SNAPSHOT.jar com.auction.server.ServerApp
   ```

### Bước 2: Khởi chạy Client (Giao diện người dùng)
Sau khi Server báo khởi động thành công, người dùng có thể mở nhiều Client song song để giả lập tình huống nhiều người tham gia đấu giá cùng lúc:
1. Mở một cửa sổ dòng lệnh mới tại thư mục chứa file `.jar`.
2. Chạy lệnh sau để khởi động Client:
   ```bash
   java -cp auction-system-1.0-SNAPSHOT.jar com.auction.client.ClientApp
   ```

### Bước 3: Đăng nhập và Trải nghiệm
Dự án đã chuẩn bị sẵn các tài khoản có sẵn dữ liệu mẫu để thử nghiệm ngay lập tức:
* **Tài khoản Admin (Quản trị viên)**:
  * Tên đăng nhập: `admin` | Mật khẩu: `123456`
* **Tài khoản Thành viên (Đóng vai trò người mua/bán)**:
  * Tên đăng nhập: `member` | Mật khẩu: `123456`

---

## 6. Danh sách chức năng đã hoàn thành

###  Quản lý Người dùng & Phân quyền
- Đăng ký / Đăng nhập tài khoản an toàn.
- Hệ thống phân vai trò (Roles) linh hoạt:
  - **Admin**: Quản lý toàn bộ hệ thống, người dùng và các phiên đấu giá.
  - **Member (Đóng vai trò Seller)**: Đăng bán, quản lý sản phẩm đấu giá của cá nhân.
  - **Member (Đóng vai trò Bidder)**: Tham gia đặt giá, đua thầu các sản phẩm quan tâm.
- Giao diện (GUI) JavaFX với các màn hình chuyên biệt cho từng tính năng.
- Xem và cập nhật thông tin cá nhân (Profile).

###  Quản lý Sản phẩm & Phiên đấu giá
- Quản lý sản phẩm: Thêm, sửa, xóa sản phẩm và thông tin phiên đấu giá.
- Cung cấp đầy đủ thông tin: Tên, mô tả, giá khởi điểm, giá hiện tại, thời gian bắt đầu & kết thúc.
- Khởi tạo các sản phẩm đặc thù (Điện tử, Nghệ thuật, Phương tiện) sử dụng mẫu thiết kế **Factory Method**.
- Tải và lưu trữ ảnh sản phẩm trực tiếp (mã hóa Base64) xuống Database.

###  Tham gia Đấu giá
- Xem danh sách phiên đấu giá đang mở, lọc theo trạng thái và loại sản phẩm.
- **Đặt giá thủ công (Manual Bidding)**: Người dùng đặt giá cao hơn giá hiện tại. Hệ thống kiểm tra tính hợp lệ của giá đấu và cập nhật người dẫn đầu phiên.
- **Đấu giá tự động (Auto-Bidding) [Nâng cao]**: Thiết lập giá tối đa (`maxBid`) và bước giá (`increment`). Hệ thống tự động trả giá thay người dùng khi có đối thủ cạnh tranh, so sánh và ưu tiên người đăng ký trước, đảm bảo không vượt quá `maxBid`.

###  Xử lý Thời gian thực & Kỹ thuật Nâng cao
- **Realtime Update (Observer/Socket) [Nâng cao]**: Toàn bộ client đang xem phiên được cập nhật ngay lập tức khi có bid mới, không sử dụng cơ chế polling gây nặng máy.
- **Xử lý đấu giá đồng thời (Concurrent Bidding) [Nâng cao]**: Đảm bảo an toàn luồng (Thread-safe) khi nhiều bidder đặt giá cùng lúc. Giải quyết triệt để vấn đề *Lost update*, *Rollback* giá và cam kết không có tình trạng hai người cùng thắng.
- **Gia hạn phiên đấu giá (Anti-sniping) [Nâng cao]**: Tự động kéo dài thời gian phiên (cộng thêm 60s) nếu có lượt đặt giá mới lọt vào 30 giây cuối cùng của phiên, đảm bảo cạnh tranh công bằng.
- **Bid History Visualization [Nâng cao]**: Hiển thị biểu đồ đường (Line chart) biến động giá đấu theo thời gian thực (Trục X: Timestamp, Trục Y: Giá hiện tại). Biểu đồ tự động cập nhật khi có bid hợp lệ mà không cần tải lại trang.

###  Xử lý Kết thúc Phiên & Ngoại lệ
- **Tự động kết thúc phiên**: Luồng quét ngầm tự động đóng phiên khi hết thời gian, xác định chính xác người thắng cuộc.
- **Chuyển đổi trạng thái chặt chẽ**: `OPEN` → `RUNNING` → `FINISHED` → `PAID` / `CANCELED`.
- **An toàn Giao dịch & Rollback (Fault Tolerance)**: Hệ thống xử lý hoàn hảo các sự cố khi lưu Database (Manual Compensation Rollback). Khôi phục tự động bộ nhớ đệm (RAM Cache) nếu lưu dữ liệu đặt giá thất bại và đảo ngược các giao dịch tài chính (hoàn tiền) nếu thanh toán bị lỗi giữa chừng, đảm bảo không thất thoát tiền hay sai lệch dữ liệu.
- **Bắt lỗi nghiệp vụ đấu giá**: Chặn đặt giá thấp hơn hoặc bằng giá hiện tại, chặn đặt giá chính mình, kiểm tra số dư ví (không đủ tiền không cho đấu giá), chặn hành vi đấu giá khi phiên chưa bắt đầu hoặc đã đóng.
- **Bảo vệ toàn vẹn dữ liệu**: Validate chặt chẽ Form dữ liệu đầu vào (kiểm tra rỗng, sai định dạng chữ/số), đảm bảo hệ thống không bị crash (văng ứng dụng) khi người dùng thao tác sai.
- **Kiểm soát kết nối mạng & Socket**: Xử lý an toàn các tình huống đứt mạng, rớt kết nối đột ngột (Connection Reset/Timeout). Server tự động bắt `IOException` và dọn dẹp (cleanup) các luồng Client rác.
- **Xử lý ngoại lệ bảo mật**: Cảnh báo tài khoản/mật khẩu không chính xác, tự động từ chối các thao tác truy cập trái phép hoặc khi tài khoản đã bị Admin khóa (Banned).

###  Quản lý & Vận hành (Admin)
- Xem danh sách toàn bộ các phiên đấu giá trên hệ thống.
- Đóng phiên đấu giá thủ công trước thời hạn.
- Hủy các phiên đấu giá vi phạm chính sách.
- Quản lý thành viên: Xem danh sách, khóa / mở khóa tài khoản vi phạm.

###  Quản lý Ví điện tử & Thanh toán tự động
- **Ví điện tử cá nhân**: Nạp tiền, rút tiền, kiểm tra số dư ví. Ràng buộc kiểm tra số dư thực tế trước khi cho phép đặt giá.
- **Thanh toán tự động**: Thanh toán hóa đơn mua sản phẩm, hệ thống tự động trích tiền từ ví người mua chuyển sang ví người bán sau khi giao dịch thành công.
- Xem lịch sử đặt giá thầu và lịch sử giao dịch hóa đơn.

###  Tiêu chuẩn kỹ thuật & Chất lượng code
- **Unit Test (JUnit 5)**: Kiểm thử độc lập logic nghiệp vụ (User, Auction, Bid, AutoBid) bằng DB Stub In-Memory.
- **Coding Convention**: Sử dụng **Checkstyle** (Google Java Style Guide) để chuẩn hóa mã nguồn.
- **Logging System**: Tích hợp **SLF4J & Logback Classic** ghi nhận vết hoạt động và hỗ trợ debug lỗi.
- **Graceful Shutdown**: Đăng ký **Shutdown Hook** giải phóng tài nguyên, đóng Socket Server/Scheduler an toàn.
- **CI/CD Pipeline**: Tự động hóa kiểm tra Checkstyle, chạy test và đóng gói JAR bằng **GitHub Actions**.

---

## 7. Phân công nhiệm vụ trong nhóm

Dưới đây là bảng phân công công việc chi tiết của các thành viên trong dự án:

| STT | Họ và tên | MSSV | GitHub | Chuyên trách chính | Chi tiết công việc tương ứng với Cấu trúc thư mục |
|:---:|---|:---:|---|---|---|
| 1 | **Phùng Anh Dũng** | 25020077 | `padungg` | **Server Logic & Socket Server** | Xây dựng Socket Server đa luồng, điều phối logic và xử lý nghiệp vụ trung tâm phía Server. Tham gia thiết kế gói Model chung. |
| 2 | **Nguyễn Minh Nhật Anh** | 25020022 | `nhatdog34` | **Database, Testing & CI/CD** | Thiết kế CSDL (PostgreSQL/Supabase, DAO Layer), viết Unit Test, thiết lập GitHub Actions CI/CD. Hỗ trợ Dũng xử lý Server khi cần. Tham gia thiết kế gói Model chung. |
| 3 | **Nguyễn Viết Hưng** | 25020196 | `ngvh2312` | **Client Logic & Socket Client** | Xây dựng Socket Client kết nối tới Server, xử lý truyền nhận dữ liệu JSON/Gson và lập trình Controller điều khiển sự kiện UI. Tham gia thiết kế gói Model chung. |
| 4 | **Đậu Đình Gia Bảo** | 25020034 | `baothebean` | **Giao diện (UI/UX FXML)** | Thiết kế toàn bộ giao diện JavaFX bằng các file `.fxml`, tổ chức tài nguyên hình ảnh, đảm bảo UI/UX. Hỗ trợ Hưng xử lý code phía Client. Tham gia thiết kế gói Model chung. |

---

## 8. Tài liệu Báo cáo & Video Demo

* **Tài liệu Báo cáo PDF**: [nhom10_baocao_auction-system_2526II_UET.CS2043_1.pdf](./nhom10_baocao_auction-system_2526II_UET.CS2043_1.pdf?raw=true)
* **Thiết kế UML & Luồng hệ thống**: [UML_Auction_System.md](./UML_Auction_System.md)
* **Video Demo Hệ thống**:
