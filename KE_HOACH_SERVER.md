# 🛠️ Kế hoạch làm lại Server — Từng bước một

> **Nguyên tắc:** Mỗi bước chỉ thêm **1 thứ mới**. Làm xong bước trước → hiểu rồi → mới sang bước sau.
> Cuối mỗi bước đều có cách **test thử** để biết chắc nó chạy đúng.

---

## Tổng quan 7 bước

```
Bước 1: Server đơn giản nhất → nhận 1 chuỗi, trả 1 chuỗi
Bước 2: Thêm MULTI-THREAD    → phục vụ nhiều client cùng lúc
Bước 3: Thêm GSON (JSON)     → nhận/gửi JSON thay vì chuỗi thô
Bước 4: Thêm ROUTER          → nhìn loại request → gọi đúng chỗ
Bước 5: Thêm USER SERVICE    → xử lý login + register
Bước 6: Thêm AUCTION SERVICE → xem danh sách + chi tiết + tạo phiên
Bước 7: Thêm BID SERVICE     → đặt giá + synchronized
```

---

## Bước 1: Server đơn giản nhất

### Mục tiêu
Tạo server **nhận 1 chuỗi** từ client và **trả lại 1 chuỗi**. Chỉ vậy thôi.

### Bạn cần hiểu
- `ServerSocket` = mở 1 cổng trên máy, chờ kết nối
- `accept()` = chặn lại, đợi cho đến khi có client kết nối
- `DataInputStream.readUTF()` = đọc chuỗi từ client
- `DataOutputStream.writeUTF()` = gửi chuỗi về client

### File cần tạo/sửa

#### `ServerApp.java` — viết lại đơn giản nhất
```java
package com.auction.server;

import java.io.*;
import java.net.*;

public class ServerApp {
    public static void main(String[] args) {
        try {
            // Mở cổng 1234
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println(">>> Server đang chạy tại cổng 1234...");

            while (true) {
                // Chờ client kết nối
                Socket socket = serverSocket.accept();
                System.out.println(">>> Có client kết nối!");

                // Tạo ống đọc/ghi
                DataInputStream in = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                // Đọc chuỗi từ client
                String message = in.readUTF();
                System.out.println(">>> Nhận được: " + message);

                // Trả lại 1 chuỗi
                out.writeUTF("Server đã nhận: " + message);
                out.flush();

                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Cách test
Tạo 1 file test tạm để thử:

#### `TestClient.java` (tạo trong package server, dùng để test rồi xóa)
```java
package com.auction.server;

import java.io.*;
import java.net.*;

public class TestClient {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 1234);
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // Gửi 1 chuỗi
            out.writeUTF("Xin chào Server!");
            out.flush();

            // Đọc phản hồi
            String response = in.readUTF();
            System.out.println("Server trả lời: " + response);

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Cách chạy
1. Chạy `ServerApp.main()` trước
2. Chạy `TestClient.main()` sau
3. Kết quả mong đợi:
   - Server in: `>>> Nhận được: Xin chào Server!`
   - Client in: `Server trả lời: Server đã nhận: Xin chào Server!`

### ⚠️ Vấn đề của bước này
- Server chỉ phục vụ **1 client rồi đóng** → client tiếp phải chờ
- Gửi chuỗi thô → không có cấu trúc (kiểu gì? dữ liệu gì?)

→ **Bước 2 sẽ sửa vấn đề "1 client"**

---

## Bước 2: Thêm Multi-thread

### Mục tiêu
Tách phần "xử lý 1 client" ra **class riêng** chạy trên **thread riêng**.

### Bạn cần hiểu
- `Runnable` = interface có method `run()` — đặt code vào đây để chạy trên thread riêng
- `ExecutorService` = "nhóm thread", bạn đưa Runnable vào → nó tự chạy trên thread trống
- Lý do: Khi client A đang nói chuyện, client B cũng có thể nói cùng lúc

### File cần tạo

#### `ClientHandler.java` (MỚI) — tách phần xử lý 1 client ra riêng
```java
package com.auction.server.network;

import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // Code này chạy trên THREAD RIÊNG
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // Vòng lặp: đọc nhiều tin nhắn cho đến khi client tắt
            while (true) {
                String message = in.readUTF();
                System.out.println(">>> Nhận: " + message);

                out.writeUTF("Server đã nhận: " + message);
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println(">>> Client đã ngắt kết nối");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

#### `ServerApp.java` — sửa lại dùng thread pool
```java
package com.auction.server;

import com.auction.server.network.ClientHandler;
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ServerApp {
    public static void main(String[] args) {
        // Tạo nhóm 20 thread
        ExecutorService threadPool = Executors.newFixedThreadPool(20);

        try {
            ServerSocket serverSocket = new ServerSocket(1234);
            System.out.println(">>> Server đang chạy...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println(">>> Client mới kết nối!");

                // Tạo handler và đưa vào thread pool
                ClientHandler handler = new ClientHandler(socket);
                threadPool.execute(handler);
                // → handler.run() sẽ chạy trên thread riêng
                // → while(true) ở trên TIẾP TỤC accept() client tiếp theo
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Cách test
- Chạy Server
- Mở 2-3 TestClient cùng lúc
- Tất cả đều được phục vụ đồng thời

### ✅ Bước này đã đáp ứng yêu cầu đề bài
> 🔥 **Multi-thread Server** — xử lý đồng thời nhiều Client kết nối cùng lúc

---

## Bước 3: Thêm JSON (Gson)

### Mục tiêu
Thay vì gửi chuỗi thô `"Xin chào"`, gửi **JSON** có cấu trúc dùng `Request` và `Response` (đã có sẵn).

### Bạn cần hiểu
- JSON = text có cấu trúc: `{"type":"LOGIN","payload":{"username":"admin"}}`
- Gson = thư viện chuyển Java Object ↔ JSON string
- `gson.toJson(object)` = Java Object → JSON string
- `gson.fromJson(jsonString, Class)` = JSON string → Java Object
- `Request` và `Response` đã được thành viên 1 tạo sẵn

### File cần tạo

#### `GsonConfig.java` (MỚI) — cấu hình Gson
```java
package com.auction.server.network;

import com.google.gson.*;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class GsonConfig {

    public static Gson createGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, 
                    (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }
}
```

> **Tại sao cần GsonConfig?** Vì `AuctionDetailDTO` có trường `LocalDateTime` — Gson mặc định không biết convert nó → bị lỗi. GsonConfig dạy Gson cách đọc/ghi `LocalDateTime`.

#### `ClientHandler.java` — sửa lại dùng JSON
```java
package com.auction.server.network;

import com.auction.model.protocol.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.*;

public class ClientHandler implements Runnable {

    private Socket socket;
    private Gson gson;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.gson = GsonConfig.createGson();
    }

    @Override
    public void run() {
        try {
            DataInputStream in = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            while (true) {
                // 1. Đọc JSON string
                String jsonRequest = in.readUTF();
                System.out.println(">>> Nhận JSON: " + jsonRequest);

                // 2. Chuyển JSON string → Request object
                Request request = gson.fromJson(jsonRequest, Request.class);

                // 3. TẠM THỜI: trả response cứng
                Response response = new Response(
                    ResponseStatus.SUCCESS, 
                    "Server nhận được request loại: " + request.getType(), 
                    null
                );

                // 4. Chuyển Response → JSON string, gửi về client
                String jsonResponse = gson.toJson(response);
                out.writeUTF(jsonResponse);
                out.flush();
            }
        } catch (EOFException e) {
            System.out.println(">>> Client ngắt kết nối");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```

### Cách test
Sửa TestClient để gửi JSON:
```java
// TestClient gửi:
Gson gson = GsonConfig.createGson();
LoginDTO loginDTO = new LoginDTO("admin", "123");
Request request = new Request(RequestType.LOGIN, loginDTO);
String json = gson.toJson(request);
out.writeUTF(json);

// Đọc response:
String jsonResponse = in.readUTF();
Response response = gson.fromJson(jsonResponse, Response.class);
System.out.println("Status: " + response.getStatus());
System.out.println("Message: " + response.getMessage());
```

### ✅ Bước này đã đáp ứng yêu cầu đề bài
> 🔥 **JSON Protocol** — giao thức Request/Response chuẩn hóa qua Socket

---

## Bước 4: Thêm Router

### Mục tiêu
Thay vì trả response cứng, **nhìn type** trong Request → **gọi đúng chỗ xử lý**.

### Bạn cần hiểu
- Hiện tại ClientHandler trả response cứng cho MỌI request → sai
- Cần 1 class **nhìn type** → quyết định gọi ai:
  - `LOGIN` → gọi xử lý đăng nhập
  - `PLACE_BID` → gọi xử lý đặt giá
  - ...

### File cần tạo

#### `RequestRouter.java` (MỚI)
```java
package com.auction.server.network;

import com.auction.model.protocol.*;

public class RequestRouter {

    // Sau này sẽ thêm Service ở đây

    public Response route(Request request, String currentUserId) {
        if (request == null || request.getType() == null) {
            return new Response(ResponseStatus.BAD_REQUEST, "Request không hợp lệ", null);
        }

        switch (request.getType()) {
            case LOGIN:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý login", null);

            case REGISTER:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý register", null);

            case GET_ALL_AUCTIONS:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý lấy danh sách", null);

            case GET_AUCTION_DETAIL:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý xem chi tiết", null);

            case CREATE_AUCTION:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý tạo phiên", null);

            case PLACE_BID:
                return new Response(ResponseStatus.SUCCESS, "TODO: xử lý đặt giá", null);

            default:
                return new Response(ResponseStatus.BAD_REQUEST, "Không hỗ trợ: " + request.getType(), null);
        }
    }
}
```

#### `ClientHandler.java` — sửa: gọi Router thay vì trả cứng
```java
// Thêm vào ClientHandler:
private RequestRouter router;
private String currentUserId;  // ai đang dùng socket này

public ClientHandler(Socket socket, RequestRouter router) {
    this.socket = socket;
    this.router = router;
    this.currentUserId = null;  // chưa login
    this.gson = GsonConfig.createGson();
}

// Trong run(), thay dòng trả response cứng bằng:
Response response = router.route(request, currentUserId);
```

#### `ServerApp.java` — sửa: tạo Router và truyền vào ClientHandler
```java
// Trong main(), thêm trước vòng while:
RequestRouter router = new RequestRouter();

// Trong vòng while, sửa:
ClientHandler handler = new ClientHandler(socket, router);
```

### Cách test
- Gửi request `LOGIN` → nhận `"TODO: xử lý login"`
- Gửi request `PLACE_BID` → nhận `"TODO: xử lý đặt giá"`
- → Router đã biết phân biệt loại request!

### ⏭️ Giờ chỉ cần thay "TODO" bằng code thật → đó là **Service**

---

## Bước 5: Thêm UserService (Login + Register)

### Mục tiêu
Thay "TODO: xử lý login" bằng **logic thật**: tìm user, so password, trả kết quả.

### Bạn cần hiểu
- Service = class chứa logic xử lý (kiểm tra, tính toán, ra quyết định)
- Service cần DAO để đọc/ghi dữ liệu
- DAO Interface = "hợp đồng" — Service chỉ biết hợp đồng, không biết dữ liệu ở đâu

### File cần tạo (theo thứ tự)

#### Bước 5.1: Tạo `UserDAO.java` (interface) — hợp đồng

```java
package com.auction.server.dao;

import com.auction.model.entity.User;

// Interface = hợp đồng. Chỉ khai báo method, KHÔNG viết code bên trong.
public interface UserDAO {
    User findByUsername(String username);   // Tìm user theo tên
    User findById(String id);              // Tìm user theo mã
    boolean save(User user);               // Lưu user mới
    boolean existsByUsername(String username); // Kiểm tra trùng tên
}
```

> **Tại sao dùng interface?** Để bạn tạo Mock (test bằng ArrayList), người khác tạo Impl (MySQL thật). Service không cần biết dùng cái nào.

#### Bước 5.2: Tạo `UserDAOMock.java` — giả lập bằng ArrayList

```java
package com.auction.server.dao;

import com.auction.model.entity.*;
import java.util.*;

public class UserDAOMock implements UserDAO {
    private List<User> users = new ArrayList<>();

    public UserDAOMock() {
        // Dữ liệu mẫu để test
        users.add(new User("user-001", "admin", "123", "admin@test.com",
            "Admin", "0901234567", "HN", true, UserRole.ADMIN, 10000000, null, 5.0));
        users.add(new User("user-002", "member1", "123", "m1@test.com",
            "Nguyễn Văn A", "0912345678", "HCM", true, UserRole.MEMBER, 5000000, null, 4.0));
    }

    @Override
    public User findByUsername(String username) {
        for (User u : users) {
            if (u.getUsername().equals(username)) return u;
        }
        return null;
    }

    @Override
    public User findById(String id) {
        for (User u : users) {
            if (u.getId().equals(id)) return u;
        }
        return null;
    }

    @Override
    public boolean save(User user) {
        return users.add(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        return findByUsername(username) != null;
    }
}
```

#### Bước 5.3: Tạo `UserService.java` — logic login + register

```java
package com.auction.server.service;

import com.auction.model.dto.*;
import com.auction.model.entity.*;
import com.auction.model.protocol.*;
import com.auction.server.dao.UserDAO;
import java.util.UUID;

public class UserService {
    private UserDAO userDAO;  // Chỉ biết interface, không biết Mock hay MySQL

    public UserService(UserDAO userDAO) {
        this.userDAO = userDAO;  // Nhận DAO từ bên ngoài truyền vào
    }

    public Response login(LoginDTO dto) {
        // 1. Tìm user
        User user = userDAO.findByUsername(dto.getUsername());
        if (user == null) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Tài khoản không tồn tại", null);
        }

        // 2. So password
        if (!user.getPassword().equals(dto.getPassword())) {
            return new Response(ResponseStatus.UNAUTHORIZED, "Sai mật khẩu", null);
        }

        // 3. Tạo DTO trả về (KHÔNG CÓ PASSWORD)
        UserResponseDTO responseDTO = new UserResponseDTO(
            user.getId(), user.getUsername(), user.getEmail(),
            user.getRole(), user.getBalance()
        );

        return new Response(ResponseStatus.SUCCESS, "Đăng nhập thành công!", responseDTO);
    }

    public Response register(RegisterDTO dto) {
        // 1. Kiểm tra trùng username
        if (userDAO.existsByUsername(dto.getUsername())) {
            return new Response(ResponseStatus.BAD_REQUEST, "Username đã tồn tại", null);
        }

        // 2. Tạo user mới
        User newUser = new User(
            UUID.randomUUID().toString(), dto.getUsername(), dto.getPassword(),
            dto.getEmail(), dto.getFullName(), dto.getPhone(), dto.getAddress(),
            true, UserRole.MEMBER, 0.0, null, 0.0
        );

        // 3. Lưu
        userDAO.save(newUser);
        return new Response(ResponseStatus.SUCCESS, "Đăng ký thành công!", null);
    }
}
```

#### Bước 5.4: Kết nối vào Router

```java
// RequestRouter.java — thêm UserService
public class RequestRouter {
    private UserService userService;
    private Gson gson;

    public RequestRouter(UserService userService) {
        this.userService = userService;
        this.gson = GsonConfig.createGson();
    }

    public Response route(Request request, String currentUserId) {
        switch (request.getType()) {
            case LOGIN:
                // Convert payload → LoginDTO
                String json = gson.toJson(request.getPayload());
                LoginDTO loginDTO = gson.fromJson(json, LoginDTO.class);
                return userService.login(loginDTO);

            case REGISTER:
                String json2 = gson.toJson(request.getPayload());
                RegisterDTO registerDTO = gson.fromJson(json2, RegisterDTO.class);
                return userService.register(registerDTO);

            // ... các case khác vẫn TODO
        }
    }
}
```

#### Bước 5.5: Kết nối trong ServerApp

```java
// ServerApp.java — tạo DAO → Service → Router
UserDAO userDAO = new UserDAOMock();
UserService userService = new UserService(userDAO);
RequestRouter router = new RequestRouter(userService);
```

#### Bước 5.6: Lưu userId sau khi login (trong ClientHandler)

```java
// Trong ClientHandler.run(), sau khi gọi router.route():
if (request.getType() == RequestType.LOGIN 
    && response.getStatus() == ResponseStatus.SUCCESS) {
    // Lấy userId từ response
    String payloadJson = gson.toJson(response.getPayload());
    UserResponseDTO userDTO = gson.fromJson(payloadJson, UserResponseDTO.class);
    this.currentUserId = userDTO.getId();
    System.out.println(">>> User đã login: " + userDTO.getUsername());
}
```

### Cách test
Sửa TestClient:
```java
// Gửi login
LoginDTO loginDTO = new LoginDTO("admin", "123");
Request request = new Request(RequestType.LOGIN, loginDTO);
out.writeUTF(gson.toJson(request));

// Đọc response → SUCCESS + UserResponseDTO
```

Test thêm: login sai password → UNAUTHORIZED

---

## Bước 6: Thêm AuctionService

### Mục tiêu
Xử lý: xem danh sách, xem chi tiết, tạo phiên đấu giá. Tương tự như UserService.

### File cần tạo (theo thứ tự)

```
1. ItemDAO.java (interface)       → findById, save
2. AuctionDAO.java (interface)    → findAllByStatus, findById, save, update
3. ItemDAOMock.java               → implements ItemDAO bằng ArrayList
4. AuctionDAOMock.java            → implements AuctionDAO bằng ArrayList
5. AuctionService.java            → getAllAuctions, getAuctionDetail, createAuction
6. Sửa RequestRouter              → thêm 3 case mới
7. Sửa ServerApp                  → tạo thêm DAO + Service
```

### Logic mỗi method (viết tương tự UserService)

**`getAllAuctions()`:**
```
1. auctionDAO.findAllByStatus(OPENING)  → danh sách Auction
2. Với mỗi Auction → itemDAO.findById() → lấy tên sản phẩm
3. Tạo List<AuctionSummaryDTO>
4. Trả Response SUCCESS + list
```

**`getAuctionDetail(auctionId)`:**
```
1. auctionDAO.findById(auctionId)       → tìm phiên đấu giá
2. itemDAO.findById(auction.getItemId()) → lấy sản phẩm
3. userDAO.findById(item.getSellerId())  → lấy tên seller
4. userDAO.findById(auction.getCurrentWinnerId()) → lấy tên winner
5. Gộp tất cả vào AuctionDetailDTO
6. Trả Response SUCCESS
```

**`createAuction(dto, sellerId)`:**
```
1. Dùng ItemFactory tạo Item đúng loại (switch itemType)
2. itemDAO.save(item)
3. Tạo Auction (startTime = bây giờ, endTime = bây giờ + N ngày)
4. auctionDAO.save(auction)
5. Trả Response SUCCESS
```

---

## Bước 7: Thêm BidService (synchronized)

### Mục tiêu
Xử lý đặt giá — **quan trọng nhất** vì dùng `synchronized`.

### File cần tạo

```
1. BidTransactionDAO.java (interface)  → save, findByAuctionId
2. BidTransactionDAOMock.java          → implements bằng ArrayList
3. BidService.java                     → placeBid() [synchronized]
4. Sửa RequestRouter                   → thêm case PLACE_BID
5. Sửa ServerApp                       → tạo thêm DAO + Service
```

### Logic `placeBid` (synchronized):
```
1. Tìm auction theo auctionId
2. Kiểm tra status == OPENING (đang mở)
3. Kiểm tra chưa hết giờ
4. Kiểm tra giá bid > giá hiện tại
5. Cập nhật giá + winner
6. Lưu BidTransaction (lịch sử)
7. Trả SUCCESS
```

> **Tại sao `synchronized`?** Khi 2 người bid cùng lúc → synchronized đảm bảo chỉ 1 người vào tại 1 thời điểm → tránh lỗi.

---

## 📋 Checklist tổng hợp

| Bước | Bạn tạo gì | Kiến thức mới | Test bằng cách |
|:----:|:-----------|:--------------|:---------------|
| 1 | `ServerApp` đơn giản | Socket, readUTF/writeUTF | Gửi chuỗi, nhận chuỗi |
| 2 | `ClientHandler` | Runnable, Thread, ExecutorService | Mở 2-3 client cùng lúc |
| 3 | `GsonConfig`, sửa ClientHandler | Gson, JSON, Request/Response | Gửi JSON, nhận JSON |
| 4 | `RequestRouter` | Switch/case, điều hướng | Gửi LOGIN vs PLACE_BID, nhận khác nhau |
| 5 | `UserDAO` + `UserDAOMock` + `UserService` | Interface, Mock, Logic Login/Register | Login đúng/sai, Register trùng |
| 6 | `ItemDAO` + `AuctionDAO` + Mocks + `AuctionService` | ItemFactory, DTO chuyển đổi | Tạo auction, xem danh sách |
| 7 | `BidTransactionDAO` + Mock + `BidService` | **synchronized**, race condition | 2 client bid cùng lúc |

---

## ⚡ Mẹo khi làm

1. **Làm TỪNG bước** — đừng nhảy cóc. Bước 1 chạy rồi mới sang bước 2.
2. **Test sau MỖI bước** — chạy TestClient để kiểm tra.
3. **Đọc lỗi** — nếu có lỗi, đọc dòng đầu tiên của stacktrace, nó chỉ đúng file + dòng bị lỗi.
4. **Code tôi đã tạo sẵn** — bạn có thể tham khảo code hiện có trong project, nhưng nên **tự gõ lại** thay vì copy để hiểu.

> [!TIP]
> Nếu bước nào bạn bí, hãy hỏi tôi — tôi sẽ giải thích ĐÚNG bước đó thôi, không nhảy sang bước khác.
