# HƯỚNG DẪN TESTING & CI/CD TOÀN DIỆN CHO NHÓM (CẬP NHẬT MỚI)

Tài liệu này là phiên bản cập nhật toàn diện nhất về hệ thống Testing và CI/CD của dự án Đấu giá trực tuyến. Toàn bộ các công đoạn cấu hình khó khăn nhất đều đã được hoàn thiện. Dưới đây là hướng dẫn chi tiết để các thành viên nắm bắt và sử dụng.

---

## PHẦN 1: TỔNG QUAN VỀ HỆ THỐNG KIỂM THỬ (TESTING)

### 1. Chúng ta đang test cái gì?
Hiện tại, dự án đã được tích hợp một bộ **Unit Test** vững chắc cho tầng **Service** (tầng chứa toàn bộ logic nghiệp vụ cốt lõi nhất), bao gồm:
- **`BidServiceTest`**: Kiểm tra việc đặt giá thầu, từ chối giá thấp hơn, và kích hoạt Anti-Sniping (gia hạn giờ nếu bid vào phút chót).
- **`AuctionServiceTest`**: Kiểm tra chức năng tạo phiên đấu giá, đóng phiên, và các logic thanh toán.
- **`UserServiceTest`**: Kiểm thử các khâu đăng nhập, đăng ký, và xử lý tài khoản bị khóa.
- **`AutoBidServiceTest`**: Kiểm tra quá trình đăng ký và phản giá tự động của robot (Auto-bid).

### 2. Sự khác biệt: Sử dụng "Manual Stub" thay vì "Mockito"
Theo hướng dẫn cũ, nhóm dự định dùng thư viện Mockito để giả lập Database. Tuy nhiên, do dự án đang chạy trên **Java 25**, Mockito gặp một số vấn đề về khả năng tương thích. 
Vì vậy, nhóm đã nâng cấp bằng cách sử dụng **Manual Stub** (Tự viết các class giả lập Database như `AuctionDAOStub`, `UserDAOStub` chạy trực tiếp trên bộ nhớ RAM ngay bên trong file test). 
- **Ưu điểm vượt trội:** Test chạy cực kỳ nhanh (mili-giây), không phụ thuộc vào MySQL (kể cả máy bạn chưa cài XAMPP/MySQL vẫn test được), và vĩnh viễn không lo lỗi xung đột thư viện.
- 💡 **Mẹo làm báo cáo:** Hãy tự hào trình bày với Giảng viên rằng: *"Nhóm đã tự thiết kế các lớp DAO In-Memory (Stub) để phục vụ Unit Test nhằm cô lập nghiệp vụ, đảm bảo hiệu năng và độ ổn định cao nhất cho CI/CD"*.

---

## PHẦN 2: HỆ THỐNG CI/CD BẰNG GITHUB ACTIONS

### 1. Cơ chế hoạt động (Pipeline)
Mỗi khi một thành viên đẩy (Push) code mới lên nhánh `main` hoặc `develop`, GitHub sẽ tự động "triệu hồi" một máy chủ Ubuntu và làm các việc sau:
1. **Khởi tạo:** Cài đặt **Java 25** và Maven.
2. **Xử lý đồ họa:** Cài đặt **Xvfb (Màn hình ảo)**. Điều này giúp hệ thống mô phỏng một chiếc màn hình để các giao diện JavaFX không bị "văng" (crash) khi chạy test trên máy chủ dòng lệnh.
3. **Continuous Integration (CI):** Chạy lệnh `mvn -B verify` để biên dịch code và tự động thi hành toàn bộ các bài Unit Test.
4. **Continuous Deployment (CD):** Nếu tất cả Test đều **Xanh (Pass)**, hệ thống sẽ gõ lệnh `mvn package` để nén dự án thành file chạy cuối cùng.
5. Upload file `.jar` lên mục **Artifacts** của GitHub.

### 2. Cách tải bản Build (.jar) cho Giảng viên (Không cần tự Build)
Nhờ có bước CD, mỗi khi code mới lên GitHub, bạn đã có sẵn file để nộp.
- Bước 1: Mở kho lưu trữ GitHub của dự án.
- Bước 2: Chuyển sang tab **Actions**.
- Bước 3: Bấm vào lần chạy (Workflow Run) mới nhất có biểu tượng **dấu tích xanh**.
- Bước 4: Cuộn trang xuống dưới cùng, ở phần **Artifacts**, sẽ có file mang tên `auction-system-executable`.
- Bước 5: Bấm để tải file `.jar` đó về và gửi thẳng cho Giảng viên (Thầy cô có thể click đúp vào là ứng dụng chạy ngay)!

---

---

## PHẦN 3: HƯỚNG DẪN PUSH CODE ĐỂ KÍCH HOẠT CI/CD (LÀM VIỆC NHÓM)

Để hệ thống CI/CD tự động kiểm tra code và tránh gây lỗi hoặc ghi đè code của các bạn khác, các thành viên tuyệt đối **không push thẳng lên nhánh chính**, mà hãy làm theo luồng tạo nhánh (Branch) sau:

**Bước 1: Tạo nhánh riêng cho tính năng của bạn**
Khi bắt đầu code một chức năng mới, hãy tạo một nhánh độc lập:
`git checkout -b <tên-nhánh-của-bạn>`
*(Ví dụ: `git checkout -b feature/cap-nhat-unit-test`)*

**Bước 2: Gom file và Commit lưu lịch sử**
Sau khi code xong, hãy chạy thử lệnh `mvn test` để đảm bảo không có lỗi trên máy bạn. Sau đó:
- Gom file đã thay đổi: `git add .`
- Ghi lại lịch sử: `git commit -m "Tính năng: <mô tả nội dung bạn vừa làm>"`

**Bước 3: Lấy code mới nhất từ kho chung (Pull)**
Trong lúc bạn đang code, rất có thể ai đó đã đẩy code mới lên mạng. Bạn cần kéo code mới nhất (thường là từ nhánh `develop` hoặc `main`) về máy để hợp nhất và tránh xung đột (Conflict):
`git pull origin develop`
*(Nếu báo có xung đột, hãy mở code ra để chọn đoạn code muốn giữ lại, sau đó gõ lại lệnh `git add .` và `git commit -m "Fix conflict"`)*.

**Bước 4: Đẩy nhánh của bạn lên GitHub (Push)**
Đẩy toàn bộ tính năng hoàn chỉnh của bạn lên kho mạng:
`git push origin <tên-nhánh-của-bạn>`
*(Ví dụ: `git push origin feature/cap-nhat-unit-test`)*

**Bước 5: Tạo Pull Request (PR) để kích hoạt CI/CD**
- Bạn mở trang GitHub của nhóm, sẽ thấy gợi ý tạo **Compare & pull request**. Hãy bấm vào đó.
- 👉 **Ngay lúc này**, CI/CD sẽ được kích hoạt! Bạn sẽ thấy một biểu tượng màu vàng xoay vòng báo hiệu "Robot" đang chạy kiểm tra (Chạy Test, Check lỗi) đối với đoạn code của bạn. 
- Đợi 1-2 phút, nếu hệ thống báo **Tích xanh (All checks have passed)**, các thành viên khác hoặc nhóm trưởng mới được quyền bấm **Merge pull request** để gộp an toàn vào code chung!

---

## PHẦN 4: CÁC TASK CÒN LẠI VÀ LƯU Ý CHO NHÓM

Mặc dù hạ tầng (DevOps) đã hoàn tất 100%, nhóm vẫn cần chú ý một vài thao tác để sản phẩm hoàn hảo nhất:

### Task 1: Clean Code & Checkstyle (Rất quan trọng)
- **Thực hiện:** Mọi thành viên trong nhóm.
- Hãy mở Terminal (hoặc CMD) ngay trong thư mục dự án và gõ lệnh: 
  `mvn checkstyle:check`
- **Mục đích:** Nếu có dòng code nào báo lỗi (ví dụ: tên biến sai quy tắc Java, thụt lề sai, khai báo thừa biến không sử dụng), các bạn hãy sửa ngay lập tức. Code cần phải thật "sạch" theo chuẩn doanh nghiệp.

### Task 2: Chạy Test kiểm chứng trên máy tính cá nhân
- **Cách 1 (Khuyên dùng - Bằng IDE):** Trong VS Code hoặc IntelliJ, mở thư mục `src/test/java`, click chuột phải vào thư mục `com.auction` và chọn **Run Tests**. Một bảng kết quả xanh lá cây sẽ hiện ra!
- **Cách 2 (Bằng Terminal):** Gõ lệnh `mvn test` vào terminal. Nếu dòng cuối cùng hiện `BUILD SUCCESS`, mọi thứ đã hoàn hảo.

### Task 3: Cập nhật Báo Cáo
- Trong cuốn báo cáo môn học, nhóm nhớ dành một mục lớn để "khoe" quy trình tự động này. 
- Hãy chụp ảnh màn hình tab Actions của GitHub (có các workflow đang tích xanh), chụp ảnh code chạy Unit Test. Đây là điểm cộng cực lớn chứng minh sự chuyên nghiệp và kỹ năng thực tế của nhóm!
