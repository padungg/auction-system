package com.auction.client.controller;

import com.auction.model.dto.AuctionSummaryDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bộ điều khiển thành phần Thẻ sản phẩm (Product Card Controller).
 * Đảm nhiệm vai trò quản lý vòng đời hiển thị thu nhỏ của một phiên đấu giá trên lưới danh sách (Grid/List View),
 * kết xuất trạng thái Badge, biểu tượng cảm xúc (Emoji) theo phân loại và tiếp nhận sự kiện click chuột để chuyển vùng chi tiết.
 */
public class ProductcardController {

    /**
     * Khởi tạo hệ thống ghi nhật ký log theo tiêu chuẩn SLF4J nhằm phục vụ công tác giám sát luồng tương tác,
     * theo dõi hành vi nhấn thẻ sản phẩm và kiểm tra tính toàn vẹn của dữ liệu DTO nạp vào giao diện.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProductcardController.class);

    @FXML private Label lblEmoji;
    @FXML private Label lblBadge;
    @FXML private Label lblType;
    @FXML private Label lblProductName;
    @FXML private Label lblSeller;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;
    @FXML private Label lblStartPrice;
    @FXML private Label lblBidCount;

    private String auctionId;

    /**
     * Nạp dữ liệu từ đối tượng DTO và cấu hình trạng thái hiển thị chi tiết cho các thành phần Label đồ họa trên Thẻ.
     * Tự động điều chỉnh màu sắc của Badge trạng thái và biểu tượng cảm xúc căn cứ theo phân loại sản phẩm.
     */
    public void setData(AuctionSummaryDTO auction) {
        this.auctionId = auction.getAuctionId();

        lblProductName.setText(auction.getItemName());
        lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblStartPrice.setText(String.format("Khởi điểm: %,.0f VNĐ", auction.getStartingPrice()));
        lblSeller.setText(auction.getSellerName() != null ? String.format("Người bán: %s", auction.getSellerName()) : "Người bán: N/A");
        lblType.setText(auction.getItemType() != null ? auction.getItemType() : "Khác");
        lblBidCount.setText(String.format("🔨 %d lượt bid", auction.getBidCount()));

        // TRÍCH XUẤT BIỂU TƯỢNG CẢM XÚC DỰA TRÊN PHÂN LOẠI DANH MỤC SẢN PHẨM
        if (auction.getItemType() != null) {
            switch (auction.getItemType()) {
                case "ELECTRONICS": lblEmoji.setText("💻"); break;
                case "ART": lblEmoji.setText("🎨"); break;
                case "VEHICLE": lblEmoji.setText("🚗"); break;
                default: lblEmoji.setText("📦");
            }
        }

        // CẤU HÌNH NHÃN TRẠNG THÁI VÀ PHÂN CẤP THUỘC TÍNH GIAO DIỆN TRỰC QUAN (STYLE BADGE)
        String status = auction.getStatus();
        lblBadge.setText(status);
        lblBadge.getStyleClass().removeAll("card-badge-running", "card-badge-open", "card-badge-finished");

        if ("RUNNING".equalsIgnoreCase(status)) {
            lblBadge.setText("Đang diễn ra");
            lblBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: #16a34a; -fx-font-weight: bold; -fx-font-size: 11;");
        } else if ("OPEN".equalsIgnoreCase(status)) {
            lblBadge.setText("Chờ đấu giá");
            lblBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: #d97706; -fx-font-weight: bold; -fx-font-size: 11;");
        } else if ("PAID".equalsIgnoreCase(status)) {
            lblBadge.setText("Đã thanh toán");
            lblBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: #2563eb; -fx-font-weight: bold; -fx-font-size: 11;");
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            lblBadge.setText("Bị hủy");
            lblBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444; -fx-font-weight: bold; -fx-font-size: 11;");
        } else {
            lblBadge.setText("Đã kết thúc");
            lblBadge.setStyle("-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-weight: bold; -fx-font-size: 11;");
        }

        // THIẾT LẬP CẤU TRÚC ĐỒNG HỒ ĐẾM THỜI GIAN DỰA TRÊN CÁC TRẠNG THÁI VÒNG ĐỜI PHIÊN ĐẤU GIÁ
        if ("FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
            lblTimeLeft.setText("⏱ Đã kết thúc");
            lblTimeLeft.setStyle("-fx-text-fill: #d97706; -fx-font-size: 12; -fx-font-weight: bold;");
        } else {
            lblTimeLeft.setText(auction.getEndTime() != null ? String.format("⏱ %s", auction.getEndTime()) : "⏱ --:--");
            lblTimeLeft.setStyle("-fx-text-fill: #d97706; -fx-font-size: 12; -fx-font-weight: bold;");
        }
    }

    /**
     * Đón nhận hành động click chuột của người dùng lên vùng không gian Thẻ sản phẩm.
     * Phát tín hiệu chuyển tiếp và yêu cầu Bộ điều khiển chính (MainController) nạp giao diện Chi tiết phiên đấu giá tương ứng.
     */
    @FXML
    void handleCardClick(MouseEvent event) {
        if (MainController.getInstance() != null) {
            LOGGER.info("Người dùng tương tác click mở chi tiết phiên đấu giá: {}", auctionId);
            MainController.getInstance().openAuctionDetail(auctionId);
        }
    }
}