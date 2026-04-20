# 📋 PHÂN TÍCH TOÀN BỘ HỆ THỐNG SERVER — ONLINE AUCTION SYSTEM
> Tài liệu này kiểm tra đầy đủ từng lớp (class), từng thư mục (package) trong hệ thống,
> giải thích mối liên kết với nhau, và đánh giá mức độ đáp ứng yêu cầu bài tập lớn.

---

## ✅ KẾT LUẬN NHANH — ĐÁP ỨNG YÊU CẦU BÀI TẬP LỚN

| Yêu cầu bài tập                             | Trạng thái | File thực hiện                                |
|:--------------------------------------------|:----------:|:----------------------------------------------|
| Kiến trúc MVC / phân lớp rõ ràng            | ✅ Hoàn thành | `controller/`, `service/`, `dao/`            |
| Giao tiếp mạng qua Socket TCP               | ✅ Hoàn thành | `ServerApp.java`, `ClientHandler.java`       |
| Xử lý đa luồng (Multi-threading)            | ✅ Hoàn thành | `ServerApp` dùng `ExecutorService`           |
| Giao tiếp bằng JSON (Protocol)              | ✅ Hoàn thành | `Request`, `Response`, `GsonConfig`          |
| Design Pattern: **Singleton**               | ✅ Hoàn thành | `DatabaseConnection`, `AuctionManager`       |
| Design Pattern: **Factory Method**          | ✅ Hoàn thành | `ItemFactory`                                |
| Design Pattern: **Observer**                | ✅ Hoàn thành | `AuctionObserver`, `AuctionManager`          |
| Design Pattern: **Strategy**                | ✅ Hoàn thành | `BidStrategy`, `NormalBid`, `AutoBid`        |
| Tính năng: Đăng nhập / Đăng ký User        | ✅ Hoàn thành | `UserService`, `UserController`              |
| Tính năng: Xem danh sách phiên đấu giá     | ✅ Hoàn thành | `AuctionService.getAllAuctions()`            |
| Tính năng: Xem chi tiết phiên               | ✅ Hoàn thành | `AuctionService.getAuctionDetail()`         |
| Tính năng: Tạo phiên đấu giá               | ✅ Hoàn thành | `AuctionService.createAuction()`            |
| Tính năng: Đặt giá (Bid)                   | ✅ Hoàn thành | `BidService.placeBid()`                     |
| Tính năng: Bid tự động (Auto-Bid)          | ✅ Hoàn thành | `AutoBidStrategy`                            |
| Tính năng: Lịch sử đặt giá                 | ✅ Hoàn thành | `BidService.getBidHistory()`                |
| Tính năng: Cập nhật giá Real-time           | ✅ Hoàn thành | `AuctionManager.notifyBidUpdate()`          |
| Tính năng: Chống bid cuối giờ (Anti-snipe) | ✅ Hoàn thành | Logic trong `BidService.placeBid()`         |
| Tính năng: Đóng phiên, xác định người thắng| ✅ Hoàn thành | `AuctionService.closeAuction()`             |

**→ Kết luận: Server hoàn chỉnh 18/18 yêu cầu. Không thiếu gì cả.**

---

## 🗂️ TOÀN BỘ CẤU TRÚC THƯ MỤC VÀ FILE

```
com.auction/
│
├── model/                          ← Tầng DATA dùng chung giữa Server và Client
│   ├── entity/                     ← Các đối tượng nghiệp vụ cốt lõi (ENTITY)
│   │   ├── Entity.java             ← Lớp cha abstract: chứa id, createdAt
│   │   ├── User.java               ← Người dùng (Thành viên hoặc Admin)
│   │   ├── UserRole.java           ← Enum: ADMIN, MEMBER (Người dùng chung)
│   │   ├── Item.java               ← Sản phẩm đấu giá (lớp cha abstract)
│   │   ├── Vehicle.java            ← Xe cộ (kế thừa Item) — Factory
│   │   ├── Electronics.java        ← Đồ điện tử (kế thừa Item) — Factory
│   │   ├── Art.java                ← Tác phẩm nghệ thuật (kế thừa Item) — Factory
│   │   ├── ItemFactory.java        ← FACTORY PATTERN: Sinh ra Item đúng loại
│   │   ├── Auction.java            ← Phiên đấu giá (liên kết Item + User)
│   │   ├── AuctionStatus.java      ← Enum: PENDING, OPENING, CLOSED
│   │   └── BidTransaction.java     ← Giao dịch đặt giá (1 lần đặt giá = 1 bản ghi)
│   │
│   ├── dto/                        ← Gói dữ liệu truyền giữa Client ↔ Server (DTO)
│   │   ├── LoginDTO.java           ← Gói đăng nhập: username + password
│   │   ├── RegisterDTO.java        ← Gói đăng ký tài khoản
│   │   ├── UserResponseDTO.java    ← Gói trả về sau login (id, username, role...)
│   │   ├── CreateAuctionDTO.java   ← Gói tạo phiên đấu giá mới
│   │   ├── AuctionSummaryDTO.java  ← Gói tóm tắt phiên (dùng cho danh sách)
│   │   ├── AuctionDetailDTO.java   ← Gói chi tiết đầy đủ 1 phiên
│   │   └── BidRequestDTO.java      ← Gói đặt giá: auctionId, amount, bidType, maxBid
│   │
│   └── protocol/                   ← Định nghĩa ngôn ngữ giao tiếp TCP (Protocol)
│       ├── Request.java            ← Gói yêu cầu từ Client gửi lên: type + payload
│       ├── RequestType.java        ← Enum 9 loại request: LOGIN, REGISTER, BID...
│       ├── Response.java           ← Gói phản hồi Server trả về: status + message + data
│       └── ResponseStatus.java     ← Enum: SUCCESS, UNAUTHORIZED, NOT_FOUND, ERROR...
│
└── server/                         ← Tầng SERVER — Xử lý toàn bộ logic & mạng
    ├── ServerApp.java              ← ĐIỂM KHỞI ĐỘNG: Lắp ráp toàn bộ hệ thống
    ├── Main.java                   ← Entry point gọi ServerApp.main()
    │
    ├── network/                    ← Tầng MẠNG: Giao tiếp Socket TCP
    │   ├── ClientHandler.java      ← Phục vụ 1 client trên 1 luồng riêng
    │   └── GsonConfig.java         ← Cấu hình Gson: tự xử lý LocalDateTime, v.v.
    │
    ├── controller/                 ← Tầng ĐIỀU PHỐI: Nhận request, gọi Service
    │   ├── RequestRouter.java      ← Switch-case điều hướng request → Controller
    │   ├── UserController.java     ← Nhận request User: login, register
    │   ├── AuctionController.java  ← Nhận request Auction: list, detail, create, close
    │   └── BidController.java      ← Nhận request Bid: placeBid, getBidHistory
    │
    ├── service/                    ← Tầng NGHIỆP VỤ: Logic kinh doanh, luật lệ
    │   ├── UserService.java        ← Logic mã hóa password, kiểm tra đăng nhập
    │   ├── AuctionService.java     ← Logic tạo phiên, kiểm tra thời gian, đóng phiên
    │   └── BidService.java         ← Logic kiểm tra giá, anti-snipe, lưu giao dịch
    │
    ├── dao/                        ← Tầng DỮ LIỆU: Truy cập Database
    │   ├── UserDAO.java            ← Interface: findById, findByUsername, save, update
    │   ├── UserDAOMock.java        ← Dữ liệu tạm trên RAM (chưa có MySQL thật)
    │   ├── AuctionDAO.java         ← Interface: findById, findAll, save, update
    │   ├── AuctionDAOMock.java     ← Dữ liệu tạm (mock 5 phiên sẵn)
    │   ├── ItemDAO.java            ← Interface: findById, findAll
    │   ├── ItemDAOMock.java        ← Dữ liệu tạm
    │   ├── BidTransactionDAO.java  ← Interface: save, findByAuctionId
    │   └── BidTransactionDAOMock.java ← Lưu lịch sử bid trên RAM
    │
    ├── database/                   ← Tầng KẾT NỐI DATABASE (Singleton)
    │   └── DatabaseConnection.java ← SINGLETON: Đảm bảo chỉ 1 kết nối MySQL duy nhất
    │
    ├── observer/                   ← OBSERVER PATTERN: Cập nhật giá Real-time
    │   ├── AuctionObserver.java    ← Interface: onBidUpdated(auctionId, price, bidder)
    │   └── AuctionManager.java     ← SINGLETON + Subject: Quản lý live-state, phát sóng
    │
    └── strategy/                   ← STRATEGY PATTERN: Các kiểu đặt giá
        ├── BidStrategy.java        ← Interface: validate() + calculateActualBid()
        ├── NormalBidStrategy.java  ← Đặt giá thường: phải cao hơn hiện tại
        └── AutoBidStrategy.java    ← Đặt giá tự động: tăng dần đến maxBidAmount
```

---

## 🔗 SƠ ĐỒ LIÊN KẾT GIỮA CÁC LỚP

### Luồng Đặt giá (PLACE_BID) — Dày nhất, phức tạp nhất:

```
Client
  │── JSON: {type:"PLACE_BID", payload:{auctionId, bidAmount, bidType}}
  ▼
ClientHandler.run()
  │── gson.fromJson() → Request object
  │── router.route(request, currentUserId)
  ▼
RequestRouter.route()
  │── case PLACE_BID → bidController.handlePlaceBid(payload, userId)
  ▼
BidController.handlePlaceBid()
  │── gson.fromJson(payload) → BidRequestDTO
  │── bidService.placeBid(dto, userId)
  ▼
BidService.placeBid()  [synchronized]
  │── auctionDAO.findById(auctionId)       ← Lấy Auction từ RAM/DB
  │── Kiểm tra: OPENING? Còn thời gian?
  │── strategies.get(bidType)              ← STRATEGY PATTERN
  │     ├── "NORMAL" → NormalBidStrategy
  │     └── "AUTO"   → AutoBidStrategy
  │── strategy.validate(auction, amount)   ← Kiểm tra luật
  │── strategy.calculateActualBid()        ← Tính giá thực tế
  │── auction.setCurrentPrice(actualBid)
  │── auctionDAO.update(auction)           ← Lưu giá mới
  │── [Anti-snipe] Nếu bid trong 2 phút cuối → kéo dài thêm 2 phút
  │── bidTransactionDAO.save(transaction)  ← Lưu lịch sử
  │── AuctionManager.notifyBidUpdate()     ← OBSERVER PATTERN
  │     └── Duyệt observerMap[auctionId]
  │           └── clientHandler.onBidUpdated() → gửi JSON realtime
  └── return Response(SUCCESS, "Đặt giá thành công")
  ▼
ClientHandler: gson.toJson(response) → gửi JSON về Client gốc
```

### Luồng Realtime Update (SUBSCRIBE_AUCTION):

```
Client A đăng ký xem phiên "auc-001":
  │── JSON: {type:"SUBSCRIBE_AUCTION", payload:"auc-001"}
  ▼
ClientHandler.handleSubscribe()
  │── AuctionManager.subscribe("auc-001", this)
  │     └── observerMap["auc-001"].add(ClientHandlerA)
  └── Response: "Đã đăng ký thành công"

Client B đặt giá cho phiên "auc-001":
  │── BidService.placeBid() → thành công
  │── AuctionManager.notifyBidUpdate("auc-001", giaMoi, bidderB)
  │     └── for each observer in observerMap["auc-001"]:
  │           └── ClientHandlerA.onBidUpdated() 
  │                 └── gửi JSON {"BID_UPDATE", giaMoi} → Client A
  └── Client A tự động nhận được giá mới mà không cần nhấn F5!
```

---

## 📦 CHI TIẾT TỪNG FILE — VAI TRÒ & LIÊN KẾT

---

### 📂 `model/entity/` — Các đối tượng dữ liệu nghiệp vụ

#### `Entity.java`
- **Vai trò:** Lớp cha chung cho tất cả entity (User, Item, Auction, BidTransaction).
- **Chứa gì:** `id` (String — UUID), `createdAt` (LocalDateTime).
- **Lý do tồn tại:** Tránh phải khai báo lại `id` ở 4 lớp con, giúp code gọn hơn.
- **Liên kết:** `User`, `Item`, `Auction`, `BidTransaction` đều kế thừa `Entity`.

#### `User.java`
- **Vai trò:** Đại diện cho một người dùng trong hệ thống.
- **Chứa gì:** `username`, `password` (đã hash), `fullName`, `email`, `role` (UserRole).
- **Liên kết:** `UserDAO` lưu trữ. `UserService` thao tác. `Auction` tham chiếu qua `sellerId`.

#### `UserRole.java`
- **Vai trò:** Enum xác định quyền hạn cơ bản của tài khoản trên hệ thống.
- **Các giá trị:**
  - `ADMIN`: Quản trị viên (có quyền đóng phiên, quản lý hệ thống).
  - `MEMBER`: Người dùng phổ thông. Một Member có thể vừa là **Người bán (Seller)** khi tạo phiên, vừa là **Người mua (Bidder)** khi đặt giá.
- **Liên kết:** `UserService` dùng để phân loại quyền hạn lúc đăng nhập/thao tác.

#### `Item.java` ← Lớp cha abstract (FACTORY PATTERN)
- **Vai trò:** Định nghĩa chung cho mọi loại sản phẩm.
- **Chứa gì:** `name`, `description`, `category`, `startingPrice`.
- **Liên kết:** `Vehicle`, `Electronics`, `Art` kế thừa. `ItemFactory` tạo ra. `Auction` liên kết.

#### `Vehicle.java`, `Electronics.java`, `Art.java` ← Con của Item
- **Vai trò:** Các loại sản phẩm cụ thể, mỗi loại có thuộc tính riêng.
  - Vehicle: `brand`, `model`, `year`, `mileage`
  - Electronics: `brand`, `warrantyMonths`
  - Art: `artist`, `yearCreated`
- **Liên kết:** Được tạo ra bởi `ItemFactory`. Lưu trong `ItemDAO`.

#### `ItemFactory.java` ← **FACTORY METHOD PATTERN**
- **Vai trò:** Nhà máy tạo Item đúng loại dựa trên `category`.
- **Tại sao cần:** Thay vì viết `if/else` trong Service, ta giao việc tạo object cho Factory.
- **Liên kết:** `AuctionService.createAuction()` gọi `ItemFactory.create(dto)`.

#### `Auction.java`
- **Vai trò:** Trung tâm của hệ thống — đại diện cho một phiên đấu giá.
- **Chứa gì:** `itemId`, `sellerId`, `startTime`, `endTime`, `currentPrice`, `currentWinnerId`, `status`.
- **Liên kết:**
  - `AuctionDAO` lưu trữ.
  - `AuctionService` quản lý logic.
  - `BidService` cập nhật `currentPrice` và `currentWinnerId`.
  - `AuctionManager` theo dõi live-state.

#### `AuctionStatus.java`
- **Vai trò:** Enum trạng thái phiên.
- **Các giá trị:** `PENDING` (chưa bắt đầu), `OPENING` (đang diễn ra), `CLOSED` (đã kết thúc).

#### `BidTransaction.java`
- **Vai trò:** Ghi lại mỗi lần đặt giá thành công (Lịch sử bid).
- **Chứa gì:** `bidderId`, `auctionId`, `bidAmount`, `bidTime`.
- **Liên kết:** `BidService` tạo ra. `BidTransactionDAO` lưu trữ.

---

### 📂 `model/dto/` — Gói dữ liệu truyền giữa Client ↔ Server

> **DTO (Data Transfer Object):** Là "phong bì thư" chứa dữ liệu khi gửi qua mạng.
> Client không được gửi thẳng Entity xuống Server (bảo mật). DTO chỉ chứa đúng
> những gì cần gửi, không thừa không thiếu.

| File | Hướng truyền | Chứa gì |
|:-----|:------------|:--------|
| `LoginDTO` | Client → Server | `username`, `password` |
| `RegisterDTO` | Client → Server | `username`, `password`, `fullName`, `email` |
| `UserResponseDTO` | Server → Client | `id`, `username`, `fullName`, `role` |
| `CreateAuctionDTO` | Client → Server | thông tin sản phẩm + thời gian phiên |
| `AuctionSummaryDTO` | Server → Client | tóm tắt phiên (cho danh sách) |
| `AuctionDetailDTO` | Server → Client | chi tiết đầy đủ 1 phiên |
| `BidRequestDTO` | Client → Server | `auctionId`, `bidAmount`, `bidType`, `maxBidAmount` |

---

### 📂 `model/protocol/` — Ngôn ngữ giao tiếp TCP

#### `Request.java`
- **Vai trò:** Cấu trúc gói tin Client gửi lên Server.
- **Chứa gì:** `type` (RequestType), `payload` (Object — Gson tự parse).
- **Liên kết:** `ClientHandler` đọc Request từ socket, truyền vào `RequestRouter`.

#### `RequestType.java` ← Enum 9 loại
```
LOGIN, REGISTER,
GET_ALL_AUCTIONS, GET_AUCTION_DETAIL, CREATE_AUCTION, CLOSE_AUCTION,
PLACE_BID, GET_BID_HISTORY,
SUBSCRIBE_AUCTION, UNSUBSCRIBE_AUCTION
```

#### `Response.java`
- **Vai trò:** Cấu trúc gói phản hồi Server gửi về Client.
- **Chứa gì:** `status` (ResponseStatus), `message` (String), `payload` (Object).
- **Liên kết:** Mọi Service đều trả `Response`. `ClientHandler` serialize thành JSON gửi đi.

#### `ResponseStatus.java` ← Enum
```
SUCCESS, BAD_REQUEST, UNAUTHORIZED, NOT_FOUND, ERROR
```

---

### 📂 `server/network/` — Tầng Mạng

#### `ClientHandler.java` ← **Trái tim của Server**
- **Vai trò:** Xử lý toàn bộ giao tiếp với 1 Client cụ thể. Mỗi Client có 1 ClientHandler riêng chạy trên 1 luồng riêng.
- **Implement:** `Runnable` (chạy như luồng) + `AuctionObserver` (nhận thông báo real-time).
- **Luồng chạy:**
  1. Đọc JSON từ socket (`DataInputStream.readUTF()`)
  2. Parse JSON → `Request`
  3. Nếu `SUBSCRIBE/UNSUBSCRIBE` → xử lý trực tiếp (cần `this` để đăng ký Observer)
  4. Còn lại → gọi `RequestRouter.route(request, currentUserId)`
  5. Nhận `Response` → serialize JSON → gửi về Client
- **Liên kết:** Tạo bởi `ServerApp`. Gọi `RequestRouter`. Đăng ký vào `AuctionManager`.

#### `GsonConfig.java`
- **Vai trò:** Cấu hình Gson để parse đặc biệt `LocalDateTime` (Java 8 không có sẵn).
- **Liên kết:** Được dùng bởi `ClientHandler` khi parse/serialize JSON.

---

### 📂 `server/controller/` — Tầng Điều Phối

#### `RequestRouter.java`
- **Vai trò:** "Tổng đài" nhận request, `switch/case` theo `RequestType` để gọi đúng Controller.
- **Liên kết:** Nhận request từ `ClientHandler`, gọi `UserController` / `AuctionController` / `BidController`.

#### `UserController.java`
- **Vai trò:** Nhận payload thô → convert sang DTO → gọi `UserService`.
- **Xử lý:** `LOGIN` → `handleLogin()`, `REGISTER` → `handleRegister()`.

#### `AuctionController.java`  
- **Vai trò:** Nhận payload thô → convert sang DTO → gọi `AuctionService`.
- **Xử lý:** `GET_ALL_AUCTIONS`, `GET_AUCTION_DETAIL`, `CREATE_AUCTION`, `CLOSE_AUCTION`.

#### `BidController.java`
- **Vai trò:** Nhận payload thô → convert sang DTO → gọi `BidService`.
- **Xử lý:** `PLACE_BID`, `GET_BID_HISTORY`.

---

### 📂 `server/service/` — Tầng Nghiệp Vụ (Não bộ thực sự)

#### `UserService.java`
- **Logic:** Mã hóa password (SHA-256), kiểm tra username/password khi login.
- **Liên kết:** Nhận `UserDAO` qua constructor. Trả `Response`.

#### `AuctionService.java`
- **Logic:**
  - `getAllAuctions()`: Lấy danh sách phiên OPENING, convert sang `AuctionSummaryDTO`.
  - `getAuctionDetail(id)`: Lấy chi tiết phiên + thông tin item, convert sang `AuctionDetailDTO`.
  - `createAuction(dto)`: Gọi `ItemFactory` tạo item, tạo Auction mới, lưu vào DAO.
  - `closeAuction(id)`: Đổi status → CLOSED, tìm người thắng qua `currentWinnerId`.
- **Liên kết:** Nhận `AuctionDAO`, `ItemDAO`, `UserDAO` qua constructor.

#### `BidService.java` ← Phức tạp nhất, áp dụng 3 Pattern
- **Logic:**
  - `synchronized`: Khóa toàn bộ hàm `placeBid()` → tránh Race Condition đa luồng.
  - **Strategy**: `strategies.get(bidType)` → chọn `NormalBidStrategy` hoặc `AutoBidStrategy`.
  - **Anti-snipe**: Nếu bid trong 2 phút cuối → `endTime += 2 phút`.
  - **Observer**: Sau khi lưu thành công → `AuctionManager.notifyBidUpdate()`.
  - `getBidHistory(auctionId)`: Lấy danh sách `BidTransaction` của phiên.
- **Liên kết:** Nhận `AuctionDAO`, `BidTransactionDAO` qua constructor. Gọi `AuctionManager`.

---

### 📂 `server/dao/` — Tầng Dữ Liệu

#### Interface: `UserDAO`, `AuctionDAO`, `ItemDAO`, `BidTransactionDAO`
- **Vai trò:** "Bản hợp đồng" — định nghĩa Server cần đọc/ghi dữ liệu gì.
- **Lý do dùng Interface:** `Service` không cần biết đang dùng Mock hay MySQL.
  Chỉ cần đổi 4 dòng trong `ServerApp` là toàn bộ hệ thống chạy MySQL thật.

#### Mock: `UserDAOMock`, `AuctionDAOMock`, `ItemDAOMock`, `BidTransactionDAOMock`
- **Vai trò:** Lưu dữ liệu tạm trong RAM bằng `ArrayList`. Dùng khi chưa có MySQL.
- **Lưu ý:** Dữ liệu mất khi Server tắt. Sẽ thay bằng `*Impl.java` (MySQL) sau.

---

### 📂 `server/database/` — SINGLETON PATTERN ①

#### `DatabaseConnection.java`
- **Vai trò:** Đảm bảo toàn Server chỉ mở **1 kết nối MySQL duy nhất** (Singleton).
- **Kỹ thuật:** Double-checked locking + `synchronized` để thread-safe.
- **Lý do cần:** Nếu 100 Client tạo ra 100 Connection → MySQL từ chối, Server sập.
- **Liên kết:** Được gọi tại `ServerApp` (khởi tạo sớm). Các `*Impl.java` DAO sẽ dùng.

---

### 📂 `server/observer/` — OBSERVER PATTERN + SINGLETON ②

#### `AuctionObserver.java`
- **Vai trò:** Interface (hợp đồng) — ai muốn nhận thông báo bid thì phải ký.
- **Phương thức:** `onBidUpdated(String auctionId, double newPrice, String bidderId)`
- **Liên kết:** `ClientHandler` implement interface này.

#### `AuctionManager.java`
- **Vai trò kép:**
  1. **Singleton**: Chỉ có 1 instance duy nhất toàn Server — dùng `ConcurrentHashMap` thread-safe.
  2. **Subject (Observer Pattern)**: Lưu danh sách `observerMap[auctionId → List<ClientHandler>]`.
- **Phương thức:**
  - `subscribe(auctionId, observer)`: Thêm ClientHandler vào danh sách xem phiên.
  - `unsubscribe(auctionId, observer)`: Xóa khi Client thoát trang.
  - `unsubscribeAll(observer)`: Xóa toàn bộ khi Client ngắt kết nối.
  - `notifyBidUpdate(auctionId, newPrice, bidderId)`: Gọi `onBidUpdated()` cho toàn bộ người đang xem.
- **Liên kết:** `BidService` gọi `notifyBidUpdate()`. `ClientHandler` gọi `subscribe/unsubscribe`.

---

### 📂 `server/strategy/` — STRATEGY PATTERN ③

#### `BidStrategy.java`
- **Vai trò:** Interface định nghĩa 2 phương thức bắt buộc cho mọi chiến lược bid.
- **Phương thức:**
  - `validate(auction, amount, bidderId)`: Kiểm tra hợp lệ → trả `null` (OK) hoặc `Response` lỗi.
  - `calculateActualBid(auction, amount)`: Tính giá thực tế sẽ được ghi nhận.

#### `NormalBidStrategy.java`
- **Logic validate:** Giá phải cao hơn `currentPrice` ít nhất 1 đơn vị. Người bid không phải chính mình đang thắng.
- **Logic calculate:** Giá thực tế = giá người dùng nhập.

#### `AutoBidStrategy.java`
- **Logic validate:** `maxBidAmount` phải lớn hơn `currentPrice` + step.
- **Logic calculate:** Tự động tăng lên `currentPrice + step`, tối đa đến `maxBidAmount`.
- **Lý do hay:** Người dùng không cần ngồi canh phiên, hệ thống tự đặt thay họ.

---

### 📂 `server/` — Lớp Khởi Động

#### `ServerApp.java` ← Điểm lắp ráp duy nhất
- **Vai trò:** Khởi tạo toàn bộ hệ thống theo đúng thứ tự, sau đó chạy vòng lặp accept().
- **Thứ tự khởi tạo:**
  1. Singleton: `DatabaseConnection`, `AuctionManager` (khởi tạo sớm để kiểm tra lỗi ngay)
  2. DAO: 4 DAO Mock
  3. Service: 3 Service (nhận DAO qua constructor)
  4. Controller: 3 Controller (nhận Service)
  5. Router: 1 Router (nhận 3 Controller)
  6. Server Socket: `ExecutorService` (20 luồng) + vòng lặp `accept()`
- **Quan trọng nhất:** Đây là nơi duy nhất chứa `new`. Toàn bộ class còn lại **không tự tạo** DAO/Service bên trong → đây chính là kỹ thuật **Dependency Injection thủ công**.

#### `Main.java`
- **Vai trò:** Entry point đơn giản, chỉ gọi `ServerApp.main(args)`.

---

## 🏆 ĐÁNH GIÁ TỔNG THỂ

### Điểm mạnh:
1. **Phân lớp rõ ràng, không đan chéo:** Service không biết Socket. Controller không biết SQL.
2. **Thread-safe hoàn chỉnh:** `synchronized BidService`, `ConcurrentHashMap AuctionManager`.
3. **Dễ mở rộng:** Thêm loại bid mới → tạo Strategy mới, không sờ vào code cũ.
4. **Dễ thay Database:** Đổi Mock → MySQL chỉ ở `ServerApp.java`.
5. **Có đủ 4 Design Pattern** theo đúng yêu cầu bài tập lớn.

### Phần còn chờ nhóm hoàn thiện:
- `UserDAOImpl`, `AuctionDAOImpl`, `ItemDAOImpl`, `BidTransactionDAOImpl` (kết nối MySQL thật).

---
*Tài liệu cập nhật lần cuối: 2026-04-19*
