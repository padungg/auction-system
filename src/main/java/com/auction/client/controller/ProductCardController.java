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
 * Bộ điều khiển thẻ sản phẩm tóm tắt phía Client.
 */
public class ProductCardController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProductCardController.class);
    private static final DateTimeFormatter DT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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

    private String currentAuctionId;

    /**
     * Gán dữ liệu từ DTO vào các thành phần đồ họa của thẻ.
     */
    public void setData(AuctionSummaryDTO auction) {
        currentAuctionId = auction.getAuctionId();

        lblProductName.setText(auction.getItemName() != null ? auction.getItemName() : "—");
        lblSeller.setText("Người bán: " + (auction.getSellerName() != null ? auction.getSellerName() : "N/A"));

        lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblStartPrice.setText("Khởi điểm: " + String.format("%,.0f VNĐ", auction.getStartingPrice()));
        lblBidCount.setText("🔨 " + auction.getBidCount() + " lượt đặt giá");

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

        String imageBase64 = auction.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));

                imgProduct.setImage(image);
                imgProduct.setVisible(true);
                lblEmoji.setVisible(false);
            } catch (Exception e) {
                LOGGER.warn("Không thể giải mã ảnh Base64 cho sản phẩm {}", auction.getAuctionId());
                imgProduct.setVisible(false);
                lblEmoji.setVisible(true);
            }
        } else {
            imgProduct.setVisible(false);
            lblEmoji.setVisible(true);
        }

        applyBadgeStyle(auction.getStatus());
        applyTimeLeft(auction.getEndTime(), auction.getStatus());
    }

    private void applyBadgeStyle(String status) {
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

    private void applyTimeLeft(String endTimeStr, String status) {
        lblTimeLeft.getStyleClass().removeAll("card-timer", "card-timer-urgent");

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
                lblTimeLeft.getStyleClass().add(totalMinutes < 60 ? "card-timer-urgent" : "card-timer");
            }
        } catch (Exception e) {
            LOGGER.warn("Không thể parse endTime: {}", endTimeStr);
            lblTimeLeft.setText("⏱ N/A");
            lblTimeLeft.getStyleClass().add("card-timer");
        }
    }

    @FXML
    private void handleCardClick() {
        if (currentAuctionId == null) return;

        MainController mainController = MainController.getInstance();
        if (mainController != null) {
            mainController.openAuctionDetail(currentAuctionId);
        } else {
            LOGGER.warn("MainController.getInstance() trả về null — không thể mở chi tiết phiên {}", currentAuctionId);
        }
    }
}