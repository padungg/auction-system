# 📋 Kế Hoạch Hoàn Thiện Server — So Sánh Với Yêu Cầu Đề Bài

---

## Bảng đánh giá: Đã làm được gì? Thiếu gì?

| # | Yêu cầu đề bài | Đã có? | Ghi chú |
|:-:|:----------------|:------:|:--------|
| 1 | Multi-thread Server (thread pool) | ✅ | `ServerApp` + `ExecutorService(20)` |
| 2 | JSON Protocol (Request/Response qua Socket) | ✅ | `ClientHandler` + `GsonConfig` |
| 3 | MVC: Controller → Service → DAO | ✅ | 3 Controller + 3 Service + 4 DAO interface |
| 4 | Login / Register | ✅ | `UserService` + `UserController` |
| 5 | Xem danh sách phiên đấu giá | ✅ | `AuctionService.getAllAuctions()` |
| 6 | Xem chi tiết phiên đấu giá | ✅ | `AuctionService.getAuctionDetail()` |
| 7 | Tạo phiên đấu giá | ✅ | `AuctionService.createAuction()` |
| 8 | Đặt giá (Bid) + synchronized | ✅ | `BidService.placeBid()` |
| 9 | DTO Pattern (tách dữ liệu server/client) | ✅ | 7 DTO classes |
| 10 | Interface-based DAO (dễ swap Mock ↔ MySQL) | ✅ | 4 Interface + 4 Mock |
| 11 | **Factory Method** (tạo loại Item) | ✅ | `ItemFactory` (Vehicle, Electronics, Art) |
| 12 | **Singleton** (quản lý kết nối DB) | ❌ THIẾU | Chưa có `DatabaseConnection` |
| 13 | **Observer** (realtime update giá bid) | ❌ THIẾU | Chưa có `AuctionManager` + `AuctionObserver` |
| 14 | **Strategy** (xử lý loại bid khác nhau) | ❌ THIẾU | Chưa có `BidStrategy` |
| 15 | Auto-Bid (đặt giá tối đa, hệ thống tự tăng) | ❌ THIẾU | Chưa có logic |
| 16 | Anti-Sniping (kéo dài nếu bid cuối giờ) | ❌ THIẾU | Chưa có logic |
| 17 | Lịch sử bid của 1 phiên | ⚠️ | Có lưu BidTransaction, chưa có API trả về danh sách |
| 18 | Đóng phiên + xác định winner | ⚠️ | Có auto-close khi bid hết giờ, chưa có RequestType riêng |
| 19 | `BidRequestDTO` thiếu trường `bidType` | ❌ THIẾU | Cần thêm để Strategy biết dùng chiến lược nào |
| 20 | `RequestType` thiếu các loại mới | ❌ THIẾU | Cần thêm `GET_BID_HISTORY`, `SUBSCRIBE_AUCTION` |

---

## Kế hoạch triển khai — 5 Phase

### Phase 1: Singleton Pattern — `DatabaseConnection`
> Đơn giản nhất, 1 file duy nhất.

#### [NEW] `server/database/DatabaseConnection.java`

```java
package com.auction.server.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;

    private static final String URL = "jdbc:mysql://localhost:3306/auction_db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    // Constructor PRIVATE → không ai tạo được từ bên ngoài
    private DatabaseConnection() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println(">>> [DB] Kết nối MySQL thành công!");
        } catch (SQLException e) {
            System.out.println(">>> [DB] LỖI: " + e.getMessage());
        }
    }

    // Synchronized → thread-safe, chỉ 1 thread tạo instance tại 1 thời điểm
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    public Connection getConnection() { return connection; }
}
```

**Không cần sửa file nào khác** — chỉ khi thành viên làm DAO viết `*DAOImpl` thì họ sẽ gọi `DatabaseConnection.getInstance().getConnection()`.

---

### Phase 2: Observer Pattern — Realtime Bid Update

#### Bước 2.1: [NEW] `server/observer/AuctionObserver.java`
```java
package com.auction.server.observer;

/**
 * Interface Observer — Ai muốn nhận thông báo khi có bid mới thì implement cái này.
 */
public interface AuctionObserver {
    void onBidUpdated(String auctionId, double newPrice, String bidderId);
}
```

#### Bước 2.2: [NEW] `server/observer/AuctionManager.java`
```java
package com.auction.server.observer;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Subject — Quản lý danh sách observer và thông báo khi có bid mới.
 * Dùng Singleton để toàn server dùng chung 1 manager.
 */
public class AuctionManager {
    private static AuctionManager instance;
    private final Map<String, List<AuctionObserver>> observers = new HashMap<>();

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) instance = new AuctionManager();
        return instance;
    }

    public synchronized void subscribe(String auctionId, AuctionObserver observer) {
        observers.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(observer);
    }

    public synchronized void unsubscribe(String auctionId, AuctionObserver observer) {
        List<AuctionObserver> list = observers.get(auctionId);
        if (list != null) list.remove(observer);
    }

    public void notifyBidUpdate(String auctionId, double newPrice, String bidderId) {
        List<AuctionObserver> list = observers.get(auctionId);
        if (list != null) {
            for (AuctionObserver o : list) {
                try { o.onBidUpdated(auctionId, newPrice, bidderId); }
                catch (Exception e) { /* observer lỗi không ảnh hưởng observer khác */ }
            }
        }
    }
}
```

#### Bước 2.3: [MODIFY] `network/ClientHandler.java`
- Implement `AuctionObserver`
- Thêm method `onBidUpdated()` → gửi JSON thông báo giá mới về client
- Xử lý `SUBSCRIBE_AUCTION` → đăng ký xem phiên

#### Bước 2.4: [MODIFY] `service/BidService.java`
- Sau khi bid thành công → gọi `AuctionManager.getInstance().notifyBidUpdate(...)`

#### Bước 2.5: [MODIFY] `protocol/RequestType.java`
- Thêm `SUBSCRIBE_AUCTION` để client đăng ký xem realtime

---

### Phase 3: Strategy Pattern — Nhiều loại Bid

#### Bước 3.1: [MODIFY] `dto/BidRequestDTO.java`
- Thêm trường `bidType` (String: "NORMAL", "AUTO")
- Thêm trường `maxBidAmount` (cho Auto-Bid)

#### Bước 3.2: [NEW] `server/strategy/BidStrategy.java`
```java
package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.Response;

/**
 * Interface Strategy — Mỗi loại bid có 1 chiến lược riêng.
 */
public interface BidStrategy {
    /**
     * Xác nhận bid hợp lệ hay không, và thực hiện logic đặc biệt nếu cần.
     * @return null nếu hợp lệ (cho phép bid), Response lỗi nếu không hợp lệ
     */
    Response validate(Auction auction, double bidAmount, String bidderId);
    
    /**
     * Tính giá bid thực tế (dành cho AutoBid — giá có thể khác giá gửi lên).
     */
    double calculateActualBid(Auction auction, double bidAmount);
}
```

#### Bước 3.3: [NEW] `server/strategy/NormalBidStrategy.java`
```java
package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.*;

public class NormalBidStrategy implements BidStrategy {
    @Override
    public Response validate(Auction auction, double bidAmount, String bidderId) {
        if (bidAmount <= auction.getCurrentPrice()) {
            return new Response(ResponseStatus.BAD_REQUEST,
                "Giá phải cao hơn " + String.format("%,.0f", auction.getCurrentPrice()) + " VNĐ", null);
        }
        if (bidderId.equals(auction.getCurrentWinnerId())) {
            return new Response(ResponseStatus.BAD_REQUEST, "Bạn đang thắng rồi!", null);
        }
        return null; // hợp lệ
    }
    
    @Override
    public double calculateActualBid(Auction auction, double bidAmount) {
        return bidAmount; // Bid thường: giá giữ nguyên
    }
}
```

#### Bước 3.4: [NEW] `server/strategy/AutoBidStrategy.java`
```java
package com.auction.server.strategy;

import com.auction.model.entity.Auction;
import com.auction.model.protocol.*;

/**
 * Auto-Bid: User đặt giá tối đa, hệ thống tự tăng từng bước.
 */
public class AutoBidStrategy implements BidStrategy {
    private static final double INCREMENT = 10000; // Mỗi bước tăng 10K

    @Override
    public Response validate(Auction auction, double maxBidAmount, String bidderId) {
        if (maxBidAmount <= auction.getCurrentPrice()) {
            return new Response(ResponseStatus.BAD_REQUEST,
                "Giá tối đa phải cao hơn giá hiện tại", null);
        }
        return null;
    }
    
    @Override
    public double calculateActualBid(Auction auction, double maxBidAmount) {
        double actual = auction.getCurrentPrice() + INCREMENT;
        return Math.min(actual, maxBidAmount); // Không vượt max
    }
}
```

#### Bước 3.5: [MODIFY] `service/BidService.java`
- Thêm `Map<String, BidStrategy> strategies`
- Trong `placeBid()`: chọn strategy → `validate()` → `calculateActualBid()` → cập nhật

---

### Phase 4: Tính năng bổ sung

#### 4.1 Anti-Sniping (Kéo dài thời gian)
Trong `BidService.placeBid()`, thêm:
```java
// Nếu bid trong 2 phút cuối → kéo dài thêm 2 phút
if (auction.getEndTime().minusMinutes(2).isBefore(LocalDateTime.now())) {
    auction.setEndTime(auction.getEndTime().plusMinutes(2));
    System.out.println(">>> [Anti-Snipe] Kéo dài thời gian phiên " + auction.getId());
}
```

#### 4.2 Lịch sử bid của 1 phiên
- [MODIFY] `RequestType.java` → thêm `GET_BID_HISTORY`
- [MODIFY] `BidService.java` → thêm method `getBidHistory(auctionId)`
- [MODIFY] `BidController.java` → thêm `handleGetBidHistory()`
- [MODIFY] `RequestRouter.java` → thêm case `GET_BID_HISTORY`

#### 4.3 Đóng phiên thủ công (Admin/Seller)
- [MODIFY] `RequestType.java` → thêm `CLOSE_AUCTION`
- [MODIFY] `AuctionService.java` → thêm method `closeAuction(auctionId, userId)`
- [MODIFY] `AuctionController.java` → thêm `handleCloseAuction()`
- [MODIFY] `RequestRouter.java` → thêm case `CLOSE_AUCTION`

---

### Phase 5: Cập nhật file protocol, controller, router

#### [MODIFY] `protocol/RequestType.java` — thêm các loại mới:
```java
public enum RequestType {
    LOGIN,
    REGISTER,
    PLACE_BID,
    GET_ALL_AUCTIONS,
    GET_AUCTION_DETAIL,
    CREATE_AUCTION,
    // === MỚI ===
    GET_BID_HISTORY,       // Lấy lịch sử bid của 1 phiên
    SUBSCRIBE_AUCTION,     // Đăng ký nhận realtime update
    UNSUBSCRIBE_AUCTION,   // Hủy đăng ký
    CLOSE_AUCTION          // Đóng phiên (Admin/Seller)
}
```

#### [MODIFY] `dto/BidRequestDTO.java` — thêm trường:
```java
public class BidRequestDTO {
    private String auctionId;
    private double bidAmount;
    private String bidType;      // "NORMAL" hoặc "AUTO"
    private double maxBidAmount; // Cho Auto-Bid
    // ... getter/setter
}
```

#### [MODIFY] `controller/RequestRouter.java` — thêm case:
```java
case GET_BID_HISTORY:
    return bidController.handleGetBidHistory(request.getPayload());
case SUBSCRIBE_AUCTION:
    // Gọi AuctionManager.subscribe()
case CLOSE_AUCTION:
    return auctionController.handleCloseAuction(request.getPayload(), currentUserId);
```

---

## Tóm tắt file cần tạo/sửa

### File MỚI (7 file):
| # | File | Pattern | Thư mục |
|:-:|:-----|:--------|:--------|
| 1 | `DatabaseConnection.java` | Singleton | `server/database/` |
| 2 | `AuctionObserver.java` | Observer | `server/observer/` |
| 3 | `AuctionManager.java` | Observer + Singleton | `server/observer/` |
| 4 | `BidStrategy.java` | Strategy | `server/strategy/` |
| 5 | `NormalBidStrategy.java` | Strategy | `server/strategy/` |
| 6 | `AutoBidStrategy.java` | Strategy | `server/strategy/` |
| 7 | `SnipeBidStrategy.java` | Strategy | `server/strategy/` |

### File SỬA (7 file):
| # | File | Sửa gì |
|:-:|:-----|:-------|
| 1 | `RequestType.java` | +4 enum: `GET_BID_HISTORY`, `SUBSCRIBE_AUCTION`, `UNSUBSCRIBE_AUCTION`, `CLOSE_AUCTION` |
| 2 | `BidRequestDTO.java` | +2 trường: `bidType`, `maxBidAmount` |
| 3 | `ClientHandler.java` | Implement `AuctionObserver` + xử lý subscribe |
| 4 | `BidService.java` | Dùng Strategy + gọi Observer + Anti-Sniping + `getBidHistory()` |
| 5 | `BidController.java` | +`handleGetBidHistory()` |
| 6 | `AuctionController.java` | +`handleCloseAuction()` |
| 7 | `RequestRouter.java` | +4 case mới |

### Thứ tự triển khai:
```
Phase 1: Singleton (1 file)           → 10 phút
Phase 2: Observer (2 file + sửa 2)    → 30 phút
Phase 3: Strategy (3 file + sửa 2)    → 30 phút
Phase 4: Tính năng bổ sung (sửa 5)    → 20 phút
Phase 5: Cập nhật Protocol/Router     → 10 phút
                              Tổng:   ~ 1.5 giờ
```

> [!IMPORTANT]
> Bạn duyệt kế hoạch này và cho tôi biết: triển khai tất cả cùng lúc hay từng Phase một?
