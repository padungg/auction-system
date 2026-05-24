package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionDetailDTO;
import com.auction.model.dto.UpdateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.auction.client.util.AlertUtils;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller quản lý dialog chỉnh sửa thông tin đấu giá.
 */
public class EditAuctionDialogController {

    private static final Logger LOGGER = LoggerFactory.getLogger(EditAuctionDialogController.class);

    // BASE FIELDS FXML
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtDesc;

    // ELECTRONICS FXML
    @FXML private VBox paneElectronics;
    @FXML private TextField txtElecBrand;
    @FXML private TextField txtElecWarranty;

    // ART FXML
    @FXML private VBox paneArt;
    @FXML private TextField txtArtArtist;
    @FXML private TextField txtArtMaterial;
    @FXML private TextField txtArtYear;

    // VEHICLE FXML
    @FXML private VBox paneVehicle;
    @FXML private TextField txtVehBrand;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtVehYear;
    @FXML private TextField txtVehKm;

    private String auctionId;
    private String itemType;
    private Runnable onSuccessCallback;
    private Stage dialogStage;

    public void initData(String auctionId, String initialName, double initialPrice, String itemType, Stage dialogStage, Runnable onSuccessCallback) {
        this.auctionId = auctionId;
        this.itemType = itemType;
        this.dialogStage = dialogStage;
        this.onSuccessCallback = onSuccessCallback;

        txtName.setText(initialName);
        txtPrice.setText(String.format("%.0f", initialPrice));
        cbCondition.setValue("Như mới");

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

        fetchAuctionDetail();
    }

    /**
     * Tải thông tin mô tả chi tiết đấu giá từ server.
     */
    private void fetchAuctionDetail() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request req = new Request(RequestType.GET_AUCTION_DETAIL, auctionId);
                Response res = ClientSocketManager.getInstance().sendRequest(req);
                if (res != null && res.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionDetailDTO detail = res.getPayloadAs(AuctionDetailDTO.class);
                    if (detail != null) {
                        Platform.runLater(() -> {
                            if (detail.getDescription() != null) {
                                txtDesc.setText(detail.getDescription());
                            }
                        });
                    }
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi truy vấn thông tin chi tiết phiên đấu giá phục vụ chỉnh sửa", e);
            }
        });
    }

    /**
     * Lưu thay đổi thông tin đấu giá.
     */
    @FXML
    void handleSave() {
        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty()) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đủ các trường có dấu *");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            UpdateAuctionDTO dto = buildUpdateAuctionDTO(price);

            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request request = new Request(RequestType.UPDATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                            if (onSuccessCallback != null) onSuccessCallback.run();
                            dialogStage.close();
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Không thể kết nối máy chủ";
                            AlertUtils.showError("Cập nhật thất bại", errorMsg);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi gói tin cập nhật UPDATE_AUCTION", e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi", "Không thể kết nối máy chủ"));
                }
            });
        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi", "Giá khởi điểm phải là số hợp lệ!");
        }
    }

    /**
     * Đóng dialog hủy bỏ thao tác.
     */
    @FXML
    void handleClose() {
        dialogStage.close();
    }

    /**
     * Đóng gói thông tin cập nhật vào DTO.
     */
    private UpdateAuctionDTO buildUpdateAuctionDTO(double price) {
        UpdateAuctionDTO dto = new UpdateAuctionDTO();
        dto.setAuctionId(auctionId);
        dto.setName(txtName.getText().trim());
        dto.setStartingPrice(price);
        dto.setCondition(cbCondition.getValue() != null ? cbCondition.getValue() : "Như mới");
        dto.setDescription(txtDesc.getText().trim());
        dto.setItemType(itemType);

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
        return dto;
    }
}