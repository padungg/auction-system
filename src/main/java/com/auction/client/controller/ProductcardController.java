package com.auction.client.controller;

import com.auction.model.dto.AuctionSummaryDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;

/**
 * <h2>ProductCardController</h2>
 * <p>
 * Bộ điều khiển Thẻ sản phẩm tóm tắt (Product Component Card Controller) phía Client.
 * Đảm nhiệm vai trò quản lý vòng đời render, ánh xạ dữ liệu và tương tác đồ họa cho từng ô phần tử
 * hiển thị trong mạng lưới danh sách sản phẩm đấu giá (Grid/ListView Layout).
 * </p>
 * * <p><b>Các giải pháp kỹ thuật và cơ chế đồ họa JavaFX UI:</b></p>
 * <ul>
 * <li><b>Giải mã luồng ảnh bất đồng bộ (Base64 Byte-Stream Image Rendering):</b> Tiếp nhận chuỗi mã hóa ảnh nhị phân Base64 từ DTO, chuyển dịch qua luồng bộ đệm `ByteArrayInputStream` để dựng thực thể `Image` cấu hình hiển thị lên `ImageView` một cách an toàn.</li>
 * <li><b>Phân nhánh logic giao diện (Dynamic UI Polling):</b> Tự động tính toán hoán đổi trạng thái hiển thị giữa Emoji ký tự và ImageView đồ họa dựa trên sự tồn tại của dữ liệu tệp ảnh gốc.</li>
 * <li><b>Kiến trúc Style Class động (Dynamic CSS State Swapping):</b> Gỡ bỏ và tái cấu hình các lớp định dạng CSS (`getStyleClass()`) theo thời gian thực dựa trên trạng thái vòng đời phiên đấu giá (`RUNNING`, `OPEN`, `FINISHED`) và mốc thời gian cảnh báo khẩn cấp dưới 1 giờ.</li>
 * <li><b>Mẫu thiết kế Mediator điều phối điều hướng (Loose Coupling Navigation):</b> Ủy thác hành vi xử lý nhấn chuột thẻ (`handleCardClick`) thông qua thực thể Singleton của `MainController` để thực hiện chuyển hướng màn hình chi tiết, giảm thiểu liên kết cứng.</li>
 * </ul>
 * * @since 1.0
 * @see com.auction.model.dto.AuctionSummaryDTO
 * @see javafx.fxml.Initializable
 */
public class ProductCardController {

    /** Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng phục vụ công tác giám sát tiến trình render UI. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCardController.class);

    /** Bộ định dạng thời gian tiêu chuẩn hệ thống phục vụ bóc tách chuỗi mốc ngày giờ kết thúc phiên đấu giá. */
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // PHÂN VÙNG BINDING THÀNH PHẦN ĐỒ HỌA FXML (UI COMPONENT BINDINGS)
    // =========================================================================
    @FXML private Label lblEmoji;
    @FXML private ImageView imgProduct;
    @FXML private Label lblBadge;
    @FXML private Label lblType;
    @FXML private Label lblProductName;
    @FXML private Label lblSeller;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;
    @FXML private Label lblStartPrice;
    @FXML private Label lblBidCount;

    /** Mã số định danh duy nhất của phiên đấu giá hiện tại đang liên kết với thẻ card đồ họa, phục vụ lệnh điều hướng. */
    private String currentAuctionId;

    /**
     * Đồng bộ và kết xuất dữ liệu từ đối tượng DTO vào các thành phần đồ họa của thẻ (Data Binding Binder).
     * <p>
     * Phương thức thực hiện gán văn bản, phân loại danh mục sản phẩm, tính toán giải mã byte ảnh nhị phân
     * và phát động lời gọi hàm bổ trợ để gán các lớp định dạng CSS động cho Badge trạng thái và khối đếm thời gian.
     * </p>
     * * @param auction Thực thể đối tượng tóm tắt thông tin phòng phiên {@link AuctionSummaryDTO} dội về từ Server
     */
    public void setData(AuctionSummaryDTO auction) {
        currentAuctionId = auction.getAuctionId();

        // -------------------------------------------------------------------------
        // 1. LIÊN KẾT THÔNG TIN VĂN BẢN (TEXT PROPERTY BINDING)
        // -------------------------------------------------------------------------
        lblProductName.setText(auction.getItemName() != null ? auction.getItemName() : "—");
        lblSeller.setText("Người bán: " + (auction.getSellerName() != null ? auction.getSellerName() : "N/A"));

        // -------------------------------------------------------------------------
        // 2. LIÊN KẾT VÀ ĐỊNH DẠNG SỐ SÀN GIÁ (FINANCIAL NUMBER FORMATTING)
        // -------------------------------------------------------------------------
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblStartPrice.setText("Khởi điểm: " + String.format("%,.0f VNĐ", auction.getStartingPrice()));
        lblBidCount.setText("🔨 " + auction.getBidCount() + " lượt đặt giá");

        // -------------------------------------------------------------------------
        // 3. XỬ LÝ EMOJI VÀ DANH MỤC SẢN PHẨM (CATEGORY TO EMOJI MAPPING)
        // -------------------------------------------------------------------------
        String type = auction.getItemType();
        if (type != null) {
            switch (type.toUpperCase()) {
                case "ELECTRONICS": lblEmoji.setText("💻"); lblType.setText("Điện tử"); break;
                case "ART":         lblEmoji.setText("🎨"); lblType.setText("Nghệ thuật"); break;
                case "VEHICLE":     lblEmoji.setText("🚗"); lblType.setText("Phương tiện"); break;
                default:            lblEmoji.setText("📦"); lblType.setText(type); break;
            }
        } else {
            lblEmoji.setText("📦");
            lblType.setText("Khác");
        }

        // -------------------------------------------------------------------------
        // 4. GIẢI MÃ VÀ HIỂN THỊ HÌNH ẢNH SẢN PHẨM (BASE64 IMAGE RENDERING PIPELINE)
        // -------------------------------------------------------------------------
        String imageBase64 = auction.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            try {
                // Giải mã mảng byte nhị phân từ chuỗi văn bản Base64 an toàn
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                // Tạo lập đối tượng đồ họa Image thông qua luồng đọc dòng bộ đệm InputStream
                Image image = new Image(new ByteArrayInputStream(imageBytes));

                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                lblEmoji.setVisible(false); // Ẩn biểu tượng ký tự để ưu tiên hiển thị ảnh thực tế
            } catch (Exception e) {
                LOGGER.warn("Không thể giải mã ảnh Base64 cho sản phẩm {}", auction.getAuctionId());
                imgProduct.setVisible(false);
                lblEmoji.setVisible(true); // Cơ chế Fallback: Hiện lại Emoji đại diện danh mục nếu ảnh lỗi
            }
        } else {
            imgProduct.setVisible(false);
            lblEmoji.setVisible(true); // Cơ chế Fallback mặc định khi sản phẩm không đính kèm tệp ảnh
        }

        // -------------------------------------------------------------------------
        // 5. CẤU HÌNH ĐỒ HỌA PHỤ TRỢ DỰA TRÊN TRẠNG THÁI (LIFECYCLE BADGE & TIMER STYLING)
        // -------------------------------------------------------------------------
        applyBadgeStyle(auction.getStatus());
        applyTimeLeft(auction.getEndTime(), auction.getStatus());
    }

    /**
     * Tái định hình nhãn trạng thái và quản lý lớp giao diện CSS động cho Badge (CSS Class Lifecycle Mutator).
     * Thực hiện bóc tách toàn bộ các style class cũ để phòng ngừa xung đột kế thừa đồ họa của cấu trúc JavaFX Node.
     * * @param status Chuỗi mã ký tự trạng thái hiện hành của phòng phiên (ví dụ: RUNNING, OPEN, FINISHED)
     */
    private void applyBadgeStyle(String status) {
        // Giải phóng và dọn sạch toàn bộ các bộ lọc CSS Class cũ trên nhãn Node
        lblBadge.getStyleClass().removeAll(
                "card-badge-running", "card-badge-open",
                "card-badge-finished", "card-badge-paid", "card-badge-cancelled");

        if (status == null) {
            lblBadge.setText("—");
            return;
        }

        // Áp dụng lớp CSS và nội dung bản dịch hiển thị tương ứng với trạng thái nghiệp vụ
        switch (status.toUpperCase()) {
            case "RUNNING":
                lblBadge.setText("Đang diễn ra");
                lblBadge.getStyleClass().add("card-badge-running");
                break;
            case "OPEN":
                lblBadge.setText("Sắp diễn ra");
                lblBadge.getStyleClass().add("card-badge-open");
                break;
            case "FINISHED":
                lblBadge.setText("Đã kết thúc");
                lblBadge.getStyleClass().add("card-badge-finished");
                break;
            case "PAID":
                lblBadge.setText("Đã thanh toán");
                lblBadge.getStyleClass().add("card-badge-paid");
                break;
            case "CANCELED":
            case "CANCELLED":
                lblBadge.setText("Đã hủy");
                lblBadge.getStyleClass().add("card-badge-cancelled");
                break;
            default:
                lblBadge.setText(status);
                break;
        }
    }

    /**
     * Tính toán khoảng biến động thời gian đếm ngược và gán Style Class khẩn cấp (Asynchronous Countdown Styling Core).
     * <p>
     * Sử dụng thư viện `ChronoUnit` để đo lường khoảng cách phút. Nếu thời gian phòng phiên rơi vào trạng thái khẩn cấp
     * dưới 60 phút, nhãn sẽ tự động được gán thêm lớp CSS `card-timer-urgent` để cảnh báo trực quan cho người dùng.
     * </p>
     * * @param endTimeStr Chuỗi ngày giờ kết thúc phiên định dạng chuỗi văn bản nhận về từ Server
     * @param status     Trạng thái hiện hành của phiên đấu giá dùng để chốt chặn điều kiện hiển thị bộ đếm
     */
    private void applyTimeLeft(String endTimeStr, String status) {
        lblTimeLeft.getStyleClass().removeAll("card-timer", "card-timer-urgent");

        // Bộ đếm ngược thời gian chỉ có ý nghĩa hiển thị đối với phiên đang chạy hoặc sắp mở thưởng
        if (!"RUNNING".equalsIgnoreCase(status) && !"OPEN".equalsIgnoreCase(status)) {
            lblTimeLeft.setText("");
            return;
        }

        if (endTimeStr == null || endTimeStr.isBlank()) {
            lblTimeLeft.setText("⏱ N/A");
            lblTimeLeft.getStyleClass().add("card-timer");
            return;
        }

        try {
            LocalDateTime endTime = LocalDateTime.parse(endTimeStr, DT_FORMATTER);
            LocalDateTime now = LocalDateTime.now();

            // Đo đạc khoảng biến động thời gian theo đơn vị phút
            long totalMinutes = ChronoUnit.MINUTES.between(now, endTime);

            if (totalMinutes <= 0) {
                lblTimeLeft.setText("⏱ Kết thúc");
                lblTimeLeft.getStyleClass().add("card-timer");
            } else {
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                lblTimeLeft.setText(String.format("⏱ %dg %dp", hours, minutes));

                // CHỐT CHẶN KHẨN CẤP (URGENT STATE TRIGGER): Dưới 1 tiếng, kích hoạt thêm CSS nguy cấp (Ví dụ: Đổi màu chữ đỏ/nhấp nháy)
                lblTimeLeft.getStyleClass().add(totalMinutes < 60 ? "card-timer-urgent" : "card-timer");
            }
        } catch (Exception e) {
            LOGGER.warn("Không thể parse endTime: {}", endTimeStr);
            lblTimeLeft.setText("⏱ N/A");
            lblTimeLeft.getStyleClass().add("card-timer");
        }
    }

    /**
     * Tiếp nhận và phân phối hành động nhấn chuột vào vùng không gian thẻ Card sản phẩm (UI Event Routing Trigger).
     * Giao tiếp với lớp Trung gian `MainController` thông qua kiến trúc Singleton để kích hoạt mở màn hình chi tiết phòng phiên.
     */
    @FXML
    private void handleCardClick() {
        if (currentAuctionId == null) return;

        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            // Kích hoạt cơ chế định tuyến chuyển đổi Scene hiển thị chi tiết vật phẩm đấu giá
            mainController.openAuctionDetail(currentAuctionId);
        } else {
            LOGGER.warn("MainController.getInstance() trả về null — không thể mở chi tiết phiên {}", currentAuctionId);
        }
    }
}