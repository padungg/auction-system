package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controller quản lý sản phẩm đăng bán dành cho Người bán phía Client.
 */
public class ManageSellerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManageSellerController.class);

    // BẢNG DANH SÁCH SẢN PHẨM CỦA NGƯỜI BÁN FXML
    @FXML private TableView<AuctionSummaryDTO> tableProducts;
    @FXML private TableColumn<AuctionSummaryDTO, String> colId;
    @FXML private TableColumn<AuctionSummaryDTO, String> colName;
    @FXML private TableColumn<AuctionSummaryDTO, String> colType;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colStartPrice;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colStatus;
    @FXML private TableColumn<AuctionSummaryDTO, Void> colAction;

    // BIỂU MẪU NHẬP LIỆU THÔNG TIN CƠ BẢN FXML
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtPrice;
    @FXML private TextField txtDurationHours;
    @FXML private TextField txtDurationMinutes;
    @FXML private TextField txtStepPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<String> cbStartHour;
    @FXML private ComboBox<String> cbStartMinute;
    @FXML private Label lblImageStatus;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea txtDesc;
    @FXML private Label lblSuccess;

    private String imageBase64;

    // CÁC KHUNG NHẬP LIỆU ĐẶC THÙ CHO TỪNG LOẠI DANH MỤC FXML
    @FXML private javafx.scene.layout.VBox paneElectronics;
    @FXML private TextField txtElecBrand;
    @FXML private TextField txtElecWarranty;

    @FXML private javafx.scene.layout.VBox paneArt;
    @FXML private TextField txtArtArtist;
    @FXML private TextField txtArtMaterial;
    @FXML private TextField txtArtYear;

    @FXML private javafx.scene.layout.VBox paneVehicle;
    @FXML private TextField txtVehBrand;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtVehYear;
    @FXML private TextField txtVehKm;

    private final ObservableList<AuctionSummaryDTO> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadMyAuctions();

        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }
        cbStartHour.setItems(hours);
        cbStartHour.setValue("00");

        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }
        cbStartMinute.setItems(minutes);
        cbStartMinute.setValue("00");

        cbType.valueProperty().addListener((_, _, newVal) -> {
            paneElectronics.setVisible(false); paneElectronics.setManaged(false);
            paneArt.setVisible(false); paneArt.setManaged(false);
            paneVehicle.setVisible(false); paneVehicle.setManaged(false);

            if ("ELECTRONICS".equals(newVal)) {
                paneElectronics.setVisible(true); paneElectronics.setManaged(true);
            } else if ("ART".equals(newVal)) {
                paneArt.setVisible(true); paneArt.setManaged(true);
            } else if ("VEHICLE".equals(newVal)) {
                paneVehicle.setVisible(true); paneVehicle.setManaged(true);
            }
        });
    }

    /**
     * Cấu hình TableView hiển thị sản phẩm của người bán.
     */
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        colType.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        colType.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String displayType = type;
                    if ("ELECTRONICS".equalsIgnoreCase(type)) {
                        displayType = "Điện tử";
                    } else if ("ART".equalsIgnoreCase(type)) {
                        displayType = "Nghệ thuật";
                    } else if ("VEHICLE".equalsIgnoreCase(type)) {
                        displayType = "Phương tiện";
                    }
                    Label lbl = new Label(displayType);
                    lbl.getStyleClass().add("seller-type-badge");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartPrice.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setGraphic(null);
                }
            }
        });

        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setGraphic(null);
                    getStyleClass().remove("seller-current-price");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setGraphic(null);
                    if (!getStyleClass().contains("seller-current-price")) {
                        getStyleClass().add("seller-current-price");
                    }
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setGraphic(createStatusLabel(status));
                    setText(null);
                }
            }
        });

        colAction.setCellFactory(_ -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Button btnView = new Button("👁 Xem");
                    btnView.getStyleClass().add("seller-btn-view");
                    btnView.setOnAction(_ -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        MainController.getInstance().openAuctionDetail(row.getAuctionId());
                    });

                    Button btnEdit = new Button("✏️ Sửa");
                    btnEdit.getStyleClass().add("seller-btn-edit");
                    btnEdit.setOnAction(_ -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        if (row.getStatus().equalsIgnoreCase("FINISHED") || row.getStatus().equalsIgnoreCase("PAID") || row.getStatus().equalsIgnoreCase("CLOSED")) {
                            AlertUtils.showError("Lỗi", "Không thể sửa phiên đấu giá đã kết thúc!");
                            return;
                        }
                        if (row.getStatus().equalsIgnoreCase("RUNNING") && row.getCurrentPrice() > row.getStartingPrice()) {
                            AlertUtils.showError("Lỗi", "Không thể sửa sản phẩm đã có người đặt giá!");
                            return;
                        }

                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/EditAuctionDialog.fxml"));
                            javafx.scene.Parent root = loader.load();

                            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                            dialogStage.setTitle("Chỉnh sửa sản phẩm");
                            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                            dialogStage.setScene(new javafx.scene.Scene(root));

                            EditAuctionDialogController controller = loader.getController();
                            controller.initData(row.getAuctionId(), row.getItemName(), row.getStartingPrice(), row.getItemType(), dialogStage, () -> {
                                loadMyAuctions();
                                AlertUtils.showInfo("Thành công", "Đã cập nhật sản phẩm thành công!");
                            });

                            dialogStage.showAndWait();
                        } catch (IOException ex) {
                            LOGGER.error("Gặp sự cố lỗi nghiêm trọng khi khởi tạo cửa sổ chỉnh sửa thông tin đấu giá fxml", ex);
                        }
                    });

                    Button btnDel = new Button("🗑");
                    btnDel.getStyleClass().add("seller-btn-del");
                    btnDel.setOnAction(_ -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        handleDeleteRow(row);
                    });

                    box.getChildren().addAll(btnView, btnEdit, btnDel);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        tableProducts.setItems(productList);
    }

    /**
     * Tải danh sách phiên đấu giá cá nhân của người bán.
     */
    private void loadMyAuctions() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionSummaryDTO[] arr = response.getPayloadAs(AuctionSummaryDTO[].class);

                    String myUserId = SessionManager.getInstance().getCurrentUser().getId();
                    String myUsername = SessionManager.getInstance().getCurrentUser().getUsername();

                    Platform.runLater(() -> {
                        productList.clear();
                        if (arr != null) {
                            for (AuctionSummaryDTO dto : arr) {
                                if ((dto.getSellerId() != null && dto.getSellerId().equals(myUserId)) ||
                                        (dto.getSellerName() != null && dto.getSellerName().equals(myUsername))) {
                                    productList.add(dto);
                                }
                            }
                        }
                    });
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi tải danh sách sản phẩm cá nhân từ máy chủ", e);
            }
        });
    }

    /**
     * Xử lý hành động chọn ảnh sản phẩm.
     */
    @FXML
    void handleChooseImage() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(txtName.getScene().getWindow());
        if (selectedFile != null) {
            long sizeInBytes = selectedFile.length();
            if (sizeInBytes >= 5 * 1024 * 1024) {
                AlertUtils.showError("Lỗi", "Kích thước ảnh phải dưới 5MB!");
                return;
            }
            try {
                byte[] compressedBytes = compressImage(selectedFile);
                this.imageBase64 = java.util.Base64.getEncoder().encodeToString(compressedBytes);
                lblImageStatus.setText("Đã chọn: " + selectedFile.getName());
            } catch (IOException e) {
                LOGGER.error("Lỗi khi đọc hoặc nén file ảnh", e);
                AlertUtils.showError("Lỗi", "Không thể đọc hoặc nén file ảnh!");
            }
        }
    }

    /**
     * Nén ảnh sản phẩm tối ưu dung lượng và kích thước.
     */
    private byte[] compressImage(java.io.File file) throws IOException {
        // Đọc ảnh gốc từ File đầu vào
        java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Không thể đọc định dạng hình ảnh!");
        }

        int maxDimension = 1024; // Kích thước cạnh tối đa được phép (để giảm dung lượng bộ nhớ)
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        java.awt.image.BufferedImage scaledImage;

        // Nếu một trong các cạnh lớn hơn maxDimension, thực hiện co giãn tỉ lệ ảnh gốc
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            double scale = Math.min((double) maxDimension / originalWidth, (double) maxDimension / originalHeight);
            int targetWidth = (int) (originalWidth * scale);
            int targetHeight = (int) (originalHeight * scale);

            // Khởi tạo ảnh đích với kích thước mới và dải màu RGB tiêu chuẩn
            scaledImage = new java.awt.image.BufferedImage(targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = scaledImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE); // Phủ nền trắng mặc định tránh lỗi kênh màu
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            // Sử dụng bộ lọc nội suy song tuyến tính (Bilinear) để giữ độ mượt mà của ảnh khi co
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
        } else {
            // Giữ nguyên kích thước nếu ảnh nằm trong giới hạn an toàn
            scaledImage = new java.awt.image.BufferedImage(originalWidth, originalHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = scaledImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, originalWidth, originalHeight);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();
        }

        // Bắt đầu quá trình nén và xuất ảnh JPEG chất lượng cao tối ưu dung lượng Base64
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.util.Iterator<javax.imageio.ImageWriter> writers = javax.imageio.ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) {
            throw new IOException("Không tìm thấy trình xuất ảnh JPEG!");
        }
        javax.imageio.ImageWriter writer = writers.next();
        try (javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                // Thiết lập cơ chế nén thủ công với độ phân giải đạt 70% tối ưu
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.7f);
            }
            writer.write(null, new javax.imageio.IIOImage(scaledImage, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * Tạo mới phiên đấu giá.
     */
    @FXML
    void handleCreate() {
        boolean emptyHours = txtDurationHours.getText().trim().isEmpty();
        boolean emptyMinutes = txtDurationMinutes == null || txtDurationMinutes.getText().trim().isEmpty();

        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                (emptyHours && emptyMinutes) || txtStepPrice.getText().trim().isEmpty() || cbType.getValue() == null) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đủ các trường có dấu * (Cần ít nhất Giờ hoặc Phút)");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            double stepPrice = Double.parseDouble(txtStepPrice.getText().trim());
            int durationHours = emptyHours ? 0 : Integer.parseInt(txtDurationHours.getText().trim());
            int durationMinutes = emptyMinutes ? 0 : Integer.parseInt(txtDurationMinutes.getText().trim());

            if (price <= 0) {
                AlertUtils.showError("Lỗi", "Giá khởi điểm phải là số dung!");
                return;
            }
            if (stepPrice <= 0) {
                AlertUtils.showError("Lỗi", "Bước giá tối thiểu phải là số dương!");
                return;
            }
            if (durationHours < 0 || durationMinutes < 0 || (durationHours == 0 && durationMinutes == 0)) {
                AlertUtils.showError("Lỗi", "Thời hạn đấu giá phải lớn hơn 0!");
                return;
            }

            String startTimeStr = "";
            if (dpStartDate.getValue() != null) {
                String dateStr = dpStartDate.getValue().toString();
                String hourStr = cbStartHour.getValue() != null ? cbStartHour.getValue() : "00";
                String minuteStr = cbStartMinute.getValue() != null ? cbStartMinute.getValue() : "00";
                startTimeStr = dateStr + " " + hourStr + ":" + minuteStr;

                try {
                    java.time.LocalDateTime selectedDateTime = java.time.LocalDateTime.parse(startTimeStr, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
                    if (selectedDateTime.isBefore(java.time.LocalDateTime.now())) {
                        AlertUtils.showError("Lỗi", "Thời gian bắt đầu phải từ hiện tại hoặc tương lai!");
                        return;
                    }
                } catch (Exception e) {
                    AlertUtils.showError("Lỗi", "Thời gian bắt đầu không đúng định dạng yyyy-MM-dd HH:mm");
                    return;
                }
            }

            CreateAuctionDTO dto = new CreateAuctionDTO();

            dto.setName(txtName.getText().trim());
            dto.setItemType(cbType.getValue());
            dto.setStartingPrice(price);
            dto.setStepPrice(stepPrice);
            dto.setDurationHours(durationHours);
            dto.setDurationMinutes(durationMinutes);
            dto.setStartTimeStr(startTimeStr);
            dto.setImageBase64(this.imageBase64);
            dto.setCondition(cbCondition.getValue() != null ? cbCondition.getValue() : "Mới");
            dto.setDescription(txtDesc.getText().trim());

            if ("ELECTRONICS".equals(cbType.getValue())) {
                dto.setBrand(txtElecBrand.getText().trim());
                try { dto.setWarrantyMonths(Integer.parseInt(txtElecWarranty.getText().trim())); } catch (Exception ignored) {}
            } else if ("ART".equals(cbType.getValue())) {
                dto.setArtistName(txtArtArtist.getText().trim());
                dto.setMaterial(txtArtMaterial.getText().trim());
                try { dto.setCreationYear(Integer.parseInt(txtArtYear.getText().trim())); } catch (Exception ignored) {}
            } else if ("VEHICLE".equals(cbType.getValue())) {
                dto.setBrand(txtVehBrand.getText().trim());
                dto.setModel(txtVehModel.getText().trim());
                try { dto.setYear(Integer.parseInt(txtVehYear.getText().trim())); } catch (Exception ignored) {}
                try { dto.setKm(Integer.parseInt(txtVehKm.getText().trim())); } catch (Exception ignored) {}
            }

            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request request = new Request(RequestType.CREATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                            handleClear();
                            loadMyAuctions();
                            lblSuccess.setText("🎉 Tạo phiên đấu giá thành công!");
                            lblSuccess.setVisible(true);
                            lblSuccess.setManaged(true);

                            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                            delay.setOnFinished(_ -> {
                                lblSuccess.setVisible(false);
                                lblSuccess.setManaged(false);
                            });
                            delay.play();
                        } else {
                            String errorMsg = (response != null) ? response.getMessage() : "Không thể kết nối máy chủ";
                            AlertUtils.showError("Lỗi", errorMsg);
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi gói chỉ thị CREATE_AUCTION", e);
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Không thể kết nối máy chủ"));
                }
            });

        } catch (NumberFormatException e) {
            AlertUtils.showError("Lỗi", "Giá hoặc thời hạn hoặc bước giá tối thiểu không hợp lệ (phải là số)!");
        }
    }

    /**
     * Làm sạch biểu mẫu nhập liệu.
     */
    @FXML
    void handleClear() {
        txtName.clear();
        cbType.setValue(null);
        txtPrice.clear();
        txtDurationHours.clear();
        if (txtDurationMinutes != null) txtDurationMinutes.clear();
        txtStepPrice.clear();
        if (dpStartDate != null) dpStartDate.setValue(null);
        if (cbStartHour != null) cbStartHour.setValue("00");
        if (cbStartMinute != null) cbStartMinute.setValue("00");
        lblImageStatus.setText("Chưa chọn ảnh");
        this.imageBase64 = null;
        cbCondition.setValue(null);
        txtDesc.clear();

        txtElecBrand.clear();
        txtElecWarranty.clear();

        txtArtArtist.clear();
        txtArtMaterial.clear();
        txtArtYear.clear();

        txtVehBrand.clear();
        txtVehModel.clear();
        txtVehYear.clear();
        txtVehKm.clear();
    }

    @Deprecated
    @FXML
    void handleDelete() {
        // Phương thức cũ — không còn sử dụng
    }

    /**
     * Xóa bỏ phiên đấu giá.
     */
    private void handleDeleteRow(AuctionSummaryDTO selected) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.DELETE_AUCTION, selected.getAuctionId());
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        loadMyAuctions();
                    } else {
                        String errorMsg = (response != null) ? "Phiên đấu giá đang chạy hoặc Server chưa hỗ trợ xóa." : "Không thể kết nối máy chủ";
                        AlertUtils.showError("Lỗi", errorMsg);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi chỉ thị xóa phiên đấu giá: {}", selected.getAuctionId(), e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Không thể kết nối máy chủ"));
            }
        });
    }

    /**
     * Tạo nhãn hiển thị trạng thái đấu giá.
     */
    private Label createStatusLabel(String status) {
        String displayStatus = status;
        if ("RUNNING".equalsIgnoreCase(status)) {
            displayStatus = "Đang diễn ra";
        } else if ("FINISHED".equalsIgnoreCase(status)) {
            displayStatus = "Đã kết thúc";
        } else if ("PAID".equalsIgnoreCase(status)) {
            displayStatus = "Đã thanh toán";
        } else if ("CANCELED".equalsIgnoreCase(status)) {
            displayStatus = "Đã hủy";
        } else if ("OPEN".equalsIgnoreCase(status)) {
            displayStatus = "Sắp diễn ra";
        }
        Label lbl = new Label("● " + displayStatus);
        lbl.getStyleClass().removeAll("seller-status-running", "seller-status-finished", "seller-status-other");
        if ("RUNNING".equalsIgnoreCase(status)) {
            lbl.getStyleClass().add("seller-status-running");
        } else if ("FINISHED".equalsIgnoreCase(status) || "PAID".equalsIgnoreCase(status)) {
            lbl.getStyleClass().add("seller-status-finished");
        } else {
            lbl.getStyleClass().add("seller-status-other");
        }
        return lbl;
    }
}