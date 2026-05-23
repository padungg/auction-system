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
 * Controller quản lý vòng đời hiển thị của một Thẻ sản phẩm tóm tắt (Product Card Component)
 * bên trong mạng lưới lưới hiển thị (FlowPane Grid) của ứng dụng Client.
 * </p>
 *
 * <p><b>Các cơ chế kỹ thuật tích hợp:</b></p>
 * <ul>
 *   <li><b>Đồng bộ dữ liệu (Flyweight Data Binding):</b> Ánh xạ luồng dữ liệu DTO động từ Server lên tập hợp các phần tử nhãn đầu cuối (UI Elements).</li>
 *   <li><b>Giải mã nhị phân ảnh (Base64 Stream Decoding):</b> Chuyển đổi chuỗi ký tự Base64 thành luồng nhị phân `ByteArrayInputStream` để render trực tiếp lên ImageView, tích hợp cơ chế dự phòng sang đồ họa Emoji nếu luồng dữ liệu lỗi hoặc trống.</li>
 *   <li><b>Ma trận phong cách động (Dynamic Styling Matrix):</b> Tự động phân tách và gán/gỡ bỏ lớp class CSS dựa trên trạng thái vòng đời phiên đấu giá và mức độ khẩn cấp của thời gian đếm ngược (Dưới 1 giờ áp dụng lớp khẩn cấp `card-timer-urgent`).</li>
 *   <li><b>Định tuyến tương tác (Deep Link Navigation Trigger):</b> Tiếp nhận hành động nhấp chuột vào phân vùng thẻ để phát tín hiệu định tuyến sang phòng chi tiết phiên đấu giá thông qua cơ chế UI Singleton của MainController.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.model.dto.AuctionSummaryDTO
 * @see com.auction.client.controller.MainController
 */
public class ProductCardController {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm lưu vết tiến trình xử lý render thẻ, cô lập các cảnh báo phân tích định dạng ngày giờ và giải mã ảnh.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCardController.class);

    /** Bộ định dạng thời gian tiêu chuẩn để phân tích cú pháp chuỗi Ngày/Tháng/Năm từ máy chủ. */
    private static final DateTimeFormatter DT_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - LIÊN KẾT ĐỒ HỌA THẺ (UI FIELDS BINDING)
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

    /** Mã định danh duy nhất của phiên đấu giá hiện tại, dùng để truyền tham số khi chuyển tiếp view chi tiết. */
    private String currentAuctionId;

    /**
     * Phương thức nạp và đồng bộ hóa dữ liệu DTO lên giao diện cấu trúc thẻ sản phẩm.
     * Tiến hành gán văn bản định dạng tiền tệ tách biệt hàng nghìn, phân bổ Emoji theo danh mục mặt hàng,
     * thực hiện giải mã luồng văn bản Base64, đồng bộ Badge trạng thái và tính toán biên thời gian khẩn cấp.
     *
     * @param auction Đối tượng đối tượng DTO chứa dữ liệu tóm tắt của phiên đấu giá
     */
    public void setData(AuctionSummaryDTO auction) {
        currentAuctionId = auction.getAuctionId();

        // Đồng bộ hóa các trường thông tin tiêu đề sản phẩm và định danh người đăng bán
        lblProductName.setText(auction.getItemName() != null ? auction.getItemName() : "—");
        lblSeller.setText("Người bán: " + (auction.getSellerName() != null ? auction.getSellerName() : "N/A"));

        // Định dạng chuỗi văn bản tiền tệ quốc gia VND và kết xuất số lượt đặt giá
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblStartPrice.setText("Khởi điểm: " + String.format("%,.0f VNĐ", auction.getStartingPrice()));
        lblBidCount.setText("🔨 " + auction.getBidCount() + " lượt đặt giá");

        // Động cơ phân tích từ khóa danh mục: Ánh xạ mã danh mục sang nhãn chữ tiếng Việt và Emoji đại diện tương thích
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

        // Tiến trình giải mã luồng văn bản Base64 thành cấu trúc ảnh JavaFX Image hiển thị lên ImageView
        String imageBase64 = auction.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                lblEmoji.setVisible(false); // Ẩn nhãn Emoji dự phòng khi ảnh nạp thành công
            } catch (Exception e) {
                LOGGER.warn("Không thể giải mã ảnh Base64 cho sản phẩm {}", auction.getAuctionId());
                imgProduct.setVisible(false);
                lblEmoji.setVisible(true); // Khôi phục hiển thị Emoji đại diện nếu chuỗi mã hóa bị lỗi font/gãy luồng
            }
        } else {
            imgProduct.setVisible(false);
            lblEmoji.setVisible(true);
        }

        // Kích hoạt công cụ định hình phong cách đồ họa cho khối Badge trạng thái phiên
        applyBadgeStyle(auction.getStatus());

        // Kích hoạt công cụ phân tích thời lượng đếm ngược biên còn lại của phiên phòng đấu
        applyTimeLeft(auction.getEndTime(), auction.getStatus());
    }

    /**
     * Động cơ cấu hình phong cách đồ họa nhãn trạng thái (Dynamic Badge Styling Core).
     * Thực hiện dọn sạch mảng các lớp phong cách hoạt động cũ của JavaFX, đối chiếu từ khóa trạng thái vòng đời
     * để cập nhật nội dung nhãn chữ tiếng Việt và đính kèm class phong cách CSS màu sắc đặc trưng tương thích.
     *
     * @param status Chuỗi mã máy biểu thị trạng thái vòng đời phiên đấu giá (RUNNING, OPEN, FINISHED, PAID, CANCELED)
     */
    private void applyBadgeStyle(String status) {
        // Dọn dẹp dọn sạch toàn bộ tập hợp class CSS style cũ trước khi tái cấu trúc
        lblBadge.getStyleClass().removeAll(
                "card-badge-running", "card-badge-open",
                "card-badge-finished", "card-badge-paid", "card-badge-cancelled");

        if (status == null) {
            lblBadge.setText("—");
            return;
        }
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
     * Động cơ tính toán phân tích biên thời gian đếm ngược (Countdown Chrono Engine).
     * Kiểm tra trạng thái hợp lệ, phân tích chuỗi văn bản ngày kết thúc thành ChronoUnit phút so với mốc thời gian thực hiện tại.
     * Tự động điều phối kết xuất chuỗi văn bản hiển thị dạng Giờ-Phút, đồng thời bóc tách ranh giới thời gian
     * để tự động kích hoạt gán lớp CSS cảnh báo nguy cấp đỏ (`card-timer-urgent`) nếu thời lượng phòng phiên còn lại ít hơn 60 phút.
     *
     * @param endTimeStr Chuỗi văn bản mốc thời gian kết thúc phiên đấu giá nhận diện từ Server DTO
     * @param status     Chuỗi trạng thái vòng đời phiên hiện tại
     */
    private void applyTimeLeft(String endTimeStr, String status) {
        lblTimeLeft.getStyleClass().removeAll("card-timer", "card-timer-urgent");

        // Chốt chặn loại biên: Không hiển thị đồng hồ đếm ngược nếu phiên không thuộc trạng thái đang chạy hoặc chờ kích hoạt
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
            long totalMinutes = ChronoUnit.MINUTES.between(now, endTime);

            if (totalMinutes <= 0) {
                lblTimeLeft.setText("⏱ Kết thúc");
                lblTimeLeft.getStyleClass().add("card-timer");
            } else {
                long hours = totalMinutes / 60;
                long minutes = totalMinutes % 60;
                lblTimeLeft.setText(String.format("⏱ %dg %dp", hours, minutes));

                // Thuật toán chốt chặn biên khẩn cấp: Tự động gán style đỏ cảnh báo nguy cấp khi thời gian phòng phiên còn lại dưới 1 giờ
                lblTimeLeft.getStyleClass().add(totalMinutes < 60 ? "card-timer-urgent" : "card-timer");
            }
        } catch (Exception e) {
            LOGGER.warn("Không thể parse endTime: {}", endTimeStr);
            lblTimeLeft.setText("⏱ N/A");
            lblTimeLeft.getStyleClass().add("card-timer");
        }
    }

    /**
     * Đơn nhận hành động nhấp chuột trực tiếp lên khu vực Thẻ thành phần (Card Component Click Action Handler).
     * Giải phóng và chuyển tiếp mã định danh phòng phiên `currentAuctionId` tới động cơ điều hướng Singleton của MainController
     * để thực hiện hoán đổi View mở phòng chi tiết sản phẩm.
     */
    @FXML
    private void handleCardClick() {
        if (currentAuctionId == null) return;
        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.openAuctionDetail(currentAuctionId); // Kích hoạt lệnh chuyển view sâu từ xa
        } else {
            LOGGER.warn("MainController.getInstance() trả về null — không thể mở chi tiết phiên {}", currentAuctionId);
        }
    }
}