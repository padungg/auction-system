# HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN
### Online Auction System

## Giới thiệu

**Hệ thống Đấu giá Trực tuyến** là một ứng dụng phân tán Client-Server cho phép nhiều người dùng tham gia mua bán, đấu giá sản phẩm theo thời gian thực thông qua mạng.

Dự án giải quyết bài toán **mô phỏng quy trình đấu giá trực tuyến**: người bán đăng sản phẩm lên hệ thống, nhiều người mua cùng tham gia đặt giá cạnh tranh trong một khoảng thời gian giới hạn, hệ thống tự động xác định người thắng cuộc khi phiên đấu giá kết thúc. Ứng dụng hỗ trợ phân quyền Admin/Member, quản lý nhiều loại sản phẩm (Phương tiện, Đồ điện tử, Nghệ thuật,...) và xử lý đồng thời nhiều kết nối từ các client khác nhau.

---

## Cấu trúc thư mục (Dự kiến)
Dự án được tổ chức chặt chẽ theo mô hình Client-Server với cấu trúc gói (package) như sau:
* `com.auction.client`: Chứa toàn bộ logic phía Client (Controller, Network xử lý giao tiếp, Util khởi chạy app).
* `com.auction.model`: Chứa các lớp thực thể (Entity, DTO), Request/Response dùng chung cho cả Client và Server.
* `com.auction.server`: Chứa logic phía Server (Socket Server đa luồng, xử lý Database, DAO, Service).
* `resources`: Chứa các tài nguyên giao diện, hình ảnh và tệp `.fxml`.

---

## Thành viên nhóm & Phân công công việc

**Công việc chung của toàn nhóm:** Thiết kế Model (Entity, DTO, Request/Response Protocol) và thảo luận thiết kế Cấu trúc kiến trúc phân tầng của dự án.

| STT | Họ và tên                 | MSSV | GitHub       | Chuyên trách chính        | Chi tiết công việc tương ứng với Cấu trúc thư mục |
|:---:|:--------------------------|:-----|:-------------|:--------------------------|:--------------------------------------------------|
| 1   | **Phùng Anh Dũng** | —    | `padungg`    | **Model Layer** | Chịu trách nhiệm gói `com.auction.model`. Xây dựng các lớp đối tượng cốt lõi (Entity), định nghĩa cấu trúc gói tin giao tiếp (Request/Response Protocol) và áp dụng các tính chất OOP. |
| 2   | **Nguyễn Minh Nhật Anh** | —    | `nhatdog34`     | **Server & Database** | Chịu trách nhiệm gói `com.auction.server`. Xây dựng Server Socket đa luồng, xử lý Database (MySQL, DAO Layer), điều hướng logic và xử lý nghiệp vụ trung tâm. |
| 3   | **Nguyễn Viết Hưng** | —    | `ngvh2312`   | **Client Logic & Network**| Chịu trách nhiệm gói `com.auction.client`. Xây dựng Socket Client kết nối đến Server, xử lý luồng dữ liệu (JSON/Gson) và lập trình các lớp Controller điều khiển sự kiện trên UI. |
| 4   | **Đậu Đình Gia Bảo** | —    | `baothebean` | **Giao diện (UI/UX FXML)**| Chịu trách nhiệm thư mục `resources`. Thiết kế toàn bộ giao diện JavaFX bằng các file `.fxml` (Login, Admin, Dashboard, Manage, View...), tổ chức tài nguyên hình ảnh, đảm bảo UI/UX. |****

## 📐 Sơ đồ kiến trúc hệ thống

```mermaid
flowchart TB
    subgraph CLIENT["🖥️ CLIENT (JavaFX)"]
        Main_C["Main.java"]
        ClientApp["ClientApp.java"]
        FXML["Giao diện FXML"]
        Controller["Controller"]
        Main_C --> ClientApp --> FXML --> Controller
    end

    subgraph SHARED["📦 MODEL (Shared giữa Client & Server)"]
        direction TB
        Entity_Layer["Entity: User, Item, Vehicle, Electronics,<br/>Art, Auction, BidTransaction, ItemFactory"]
        DTO_Layer["DTO: LoginDTO, RegisterDTO, UserResponseDTO,<br/>AuctionSummaryDTO, AuctionDetailDTO,<br/>BidRequestDTO, CreateAuctionDTO"]
        Protocol_Layer["Protocol: Request, Response,<br/>RequestType, ResponseStatus"]
    end

    subgraph SERVER["⚙️ SERVER (Java Socket)"]
        Main_S["Main.java"]
        ServerApp["ServerApp.java<br/>(Multi-thread)"]
        Service["Service Layer"]
        DAO["DAO Layer"]
        DB[("MySQL<br/>Database")]
        Main_S --> ServerApp --> Service --> DAO --> DB
    end

    Controller <-->|"TCP Socket<br/>JSON (Gson)"| ServerApp
    CLIENT -.-> SHARED
    SERVER -.-> SHARED
```

---

## 📐 Sơ đồ lớp (Class Diagram)

### 1. Entity Layer — Các lớp thực thể

```mermaid
classDiagram
    direction TB

    class Entity {
        <<abstract>>
        -String id
        +getId() String
        +setId(String) void
    }

    class User {
        -String username
        -String password
        -String email
        -String fullName
        -String phone
        -String address
        -boolean isActive
        -UserRole role
        -double balance
        -String storeName
        -double rating
    }

    class Item {
        <<abstract>>
        -String name
        -String description
        -String condition
        -String sellerId
        -double startingPrice
        +getDetailInfo() String*
    }

    class Vehicle {
        -String brand
        -String model
        -int year
        -int km
        +getDetailInfo() String
    }

    class Electronics {
        -String brand
        -int warrantyMonths
        +getDetailInfo() String
    }

    class Art {
        -String artistName
        -String material
        -int creationYear
        +getDetailInfo() String
    }

    class Auction {
        -String itemId
        -String currentWinnerId
        -double currentPrice
        -LocalDateTime startTime
        -LocalDateTime endTime
        -AuctionStatus status
    }

    class BidTransaction {
        -String bidderId
        -String auctionId
        -double bidAmount
        -LocalDateTime bidTime
        +getInfo() String
    }

    class UserRole {
        <<enumeration>>
        ADMIN
        MEMBER
    }

    class AuctionStatus {
        <<enumeration>>
        PENDING
        OPENING
        CLOSED
        CANCELLED
    }

    class ItemFactory {
        +createElectronics(...)$ Electronics
        +createArt(...)$ Art
        +createVehicle(...)$ Vehicle
    }

    Entity <|-- User
    Entity <|-- Item
    Entity <|-- Auction
    Entity <|-- BidTransaction
    Item <|-- Vehicle
    Item <|-- Electronics
    Item <|-- Art

    User --> UserRole
    Auction --> AuctionStatus
    Auction --> Item : itemId
    Auction --> User : currentWinnerId
    BidTransaction --> User : bidderId
    BidTransaction --> Auction : auctionId
    Item --> User : sellerId
    ItemFactory ..> Electronics : creates
    ItemFactory ..> Art : creates
    ItemFactory ..> Vehicle : creates
```

### 2. DTO & Protocol Layer

```mermaid
classDiagram
    direction LR

    class Request {
        -RequestType type
        -Object payload
    }

    class Response {
        -ResponseStatus status
        -String message
        -Object payload
    }

    class RequestType {
        <<enumeration>>
        LOGIN
        REGISTER
        PLACE_BID
        GET_ALL_AUCTIONS
        GET_AUCTION_DETAIL
        CREATE_AUCTION
    }

    class ResponseStatus {
        <<enumeration>>
        SUCCESS
        ERROR
        UNAUTHORIZED
        NOT_FOUND
        BAD_REQUEST
    }

    class LoginDTO {
        -String username
        -String password
    }

    class RegisterDTO {
        -String username
        -String password
        -String email
        -String fullName
        -String phone
        -String address
    }

    class UserResponseDTO {
        -String id
        -String username
        -String email
        -UserRole role
        -double balance
    }

    class BidRequestDTO {
        -String auctionId
        -double bidAmount
    }

    class CreateAuctionDTO {
        -String itemType
        -String name
        -String description
        -double startingPrice
        -int durationDays
    }

    class AuctionSummaryDTO {
        -String auctionId
        -String itemName
        -double currentPrice
        -String status
    }

    class AuctionDetailDTO {
        -String auctionId
        -String itemName
        -String itemDetails
        -double startingPrice
        -double currentPrice
        -String sellerName
        -String currentWinnerName
        -LocalDateTime startTime
        -LocalDateTime endTime
        -String status
    }

    Request --> RequestType
    Response --> ResponseStatus
    Request ..> LoginDTO : payload
    Request ..> RegisterDTO : payload
    Request ..> BidRequestDTO : payload
    Request ..> CreateAuctionDTO : payload
    Response ..> UserResponseDTO : payload
    Response ..> AuctionSummaryDTO : payload
    Response ..> AuctionDetailDTO : payload
```

---

## 🎨 Design Patterns áp dụng

| Pattern | Vị trí áp dụng |
|:--------|:----------------|
| **Factory Pattern** | `ItemFactory` — Tạo các loại Item (Vehicle, Electronics, Art) |
| **DTO Pattern** | Tách biệt dữ liệu truyền mạng và entity, bảo mật password |
| **DAO Pattern** | `UserDAOImpl` — Tách riêng logic truy xuất cơ sở dữ liệu |
| **MVC Pattern** | Controller (JavaFX) → Service → DAO |
| **Envelope Pattern** | `Request` / `Response` — Đóng gói thống nhất giao thức mạng |

---

<div align="center">
