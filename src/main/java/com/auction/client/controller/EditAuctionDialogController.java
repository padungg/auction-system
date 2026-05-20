package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.model.util.GsonProvider;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bộ điều khiển hộp thoại Chỉnh sửa thông tin đấu giá (Edit Auction Dialog Controller).
 * Đảm nhiệm vai trò quản lý cửa sổ Pop-up chỉnh sửa dữ liệu phiên đấu giá, nạp động các phân hệ thuộc tính
 * đặc thù theo danh mục sản phẩm (Điện tử, Nghệ thuật, Phương tiện) và đồng bộ kết quả cập nhật về máy chủ Server.
 */
public class EditAuctionDialogController {

    /**
     * Khởi tạo thành phần Logger theo tiêu chuẩn SLF4J nhằm phục vụ công tác giám sát luồng vận hành,
     * lưu vết các ngoại lệ ngắt kết nối I/O Socket trong tiến trình gửi gói tin cập nhật thuộc tính phiên.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAuctionDialogController.class);

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtDesc;

    // Nhóm bố cục phân hệ động (Dynamic Panes)
    @FXML private VBox paneElectronics;
    @FXML private TextField txtElecBrand;
    @FXML private TextField txtElecWarranty;

    @FXML private VBox paneArt;
    @FXML private TextField txtArtArtist;
    @FXML private TextField txtArtMaterial;
    @FXML private TextField txtArtYear;

    @FXML private VBox paneVehicle;
    @FXML private TextField txtVehBrand;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtVehYear;
    @FXML private TextField txtVehKm;

    private String auctionId;
    private String itemType;
    private Runnable onSuccessCallback;
    private Stage dialogStage;

    /**
     * Khởi tạo và thiết lập các tham số cấu hình ban đầu, gán thông tin cơ bản lên các trường nhập liệu
     * và tự động hiển thị phân hệ Container đồ họa (VBox) đặc thù tương ứng với danh mục của sản phẩm.
     */
    public void initData(String auctionId, String initialName, double initialPrice, String itemType, Stage dialogStage, Runnable onSuccessCallback) {
        this.auctionId = auctionId;
        this.itemType = itemType;
        this.dialogStage = dialogStage;
        this.onSuccessCallback = onSuccessCallback;

        // Cập nhật thông tin nền tảng ban đầu
        txtName.setText(initialName);
        txtPrice.setText(String.format("%.0f", initialPrice));
        cbCondition.setValue("Như mới");

        // Điều phối ẩn hiện phân hệ đồ họa động dựa trên phân loại sản phẩm
        if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
            paneElectronics.setVisible(true);
            paneElectronics.setManaged(true);
        } else if ("ART".equalsIgnoreCase(itemType)) {
            paneArt.setVisible(true);
            paneArt.setManaged(true);
        } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
            paneVehicle.setVisible(true);
            paneVehicle.setManaged(true);
        }

        // Thực hiện truy vấn bất đồng bộ thông tin chi tiết bổ sung (Mô tả sản phẩm)
        fetchAuctionDetail();
    }

    /**
     * Khởi chạy Worker Thread nền để đồng bộ thông tin chi tiết, lấy ra chuỗi mô tả (Description) của phiên.
     */
    private void fetchAuctionDetail() {
        new Thread(() -> {
            try {
                Request req = new Request(RequestType.GET_AUCTION_DETAIL, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionDetailDTO detail = GsonProvider.getInstance().fromJson(
                            GsonProvider.getInstance().toJson(res.getPayload()), AuctionDetailDTO.class);
                    Platform.runLater(() -> {
                        if (detail.getDescription() != null) {
                            txtDesc.setText(detail.getDescription());
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi truy vấn thông tin chi tiết phiên đấu giá phục vụ chỉnh sửa", e);
            }
        }).start();
    }

    /**
     * Tiếp nhận hành động Lưu thay đổi của người dùng. Kiểm tra tính toàn vẹn của dữ liệu bắt buộc,
     * đóng gói cấu trúc UpdateAuctionDTO và gửi chỉ thị UPDATE_AUCTION lên máy chủ từ xa.
     */
    @FXML
    void handleSave(ActionEvent event) {
        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Vui lòng nhập đủ các trường có dấu *");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());

            UpdateAuctionDTO dto = new UpdateAuctionDTO();
            dto.setAuctionId(auctionId);
            dto.setName(txtName.getText().trim());
            dto.setStartingPrice(price);
            dto.setCondition(cbCondition.getValue() != null ? cbCondition.getValue() : "Như mới");
            dto.setDescription(txtDesc.getText().trim());
            dto.setItemType(itemType); // Tham số bắt buộc trong cấu trúc UpdateAuctionDTO

            // Đóng gói các thuộc tính mở rộng chuyên biệt theo từng danh mục mặt hàng
            if ("ELECTRONICS".equalsIgnoreCase(itemType)) {
                dto.setBrand(txtElecBrand.getText().trim());
                try { dto.setWarrantyMonths(Integer.parseInt(txtElecWarranty.getText().trim())); } catch (Exception ignored) {}
            } else if ("ART".equalsIgnoreCase(itemType)) {
                dto.setArtistName(txtArtArtist.getText().trim());
                dto.setMaterial(txtArtMaterial.getText().trim());
                try { dto.setCreationYear(Integer.parseInt(txtArtYear.getText().trim())); } catch (Exception ignored) {}
            } else if ("VEHICLE".equalsIgnoreCase(itemType)) {
                dto.setBrand(txtVehBrand.getText().trim());
                dto.setModel(txtVehModel.getText().trim());
                try { dto.setYear(Integer.parseInt(txtVehYear.getText().trim())); } catch (Exception ignored) {}
                try { dto.setKm(Integer.parseInt(txtVehKm.getText().trim())); } catch (Exception ignored) {}
            }

            new Thread(() -> {
                try {
                    Request request = new Request(RequestType.UPDATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response.getStatus() == ResponseStatus.SUCCESS) {
                            if (onSuccessCallback != null) onSuccessCallback.run();
                            dialogStage.close();
                        } else {
                            showAlert(Alert.AlertType.ERROR, "Cập nhật thất bại", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi gói tin cập nhật UPDATE_AUCTION", e);
                    Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Lỗi", "Không thể kết nối máy chủ"));
                }
            }).start();
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi", "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    /**
     * Đóng cửa sổ hộp thoại Pop-up hiện hành mà không thực hiện lưu trữ thông tin chỉnh sửa.
     */
    @FXML
    void handleClose(ActionEvent event) {
        dialogStage.close();
    }

    /**
     * Đóng gói khởi tạo và hiển thị cấu trúc hộp thoại Pop-up (Modal Alert Dialog) thông báo trên lớp UI Thread.
     */
    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}