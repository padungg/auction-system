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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * <h2>ManageSellerController</h2>
 * <p>
 * Controller chịu trách nhiệm điều phối phân hệ Quản lý danh mục sản phẩm dành cho Người bán (Seller Dashboard) trên Client.
 * </p>
 *
 * <p><b>Các nghiệp vụ tích hợp cốt lõi:</b></p>
 * <ul>
 *   <li><b>Quản lý vòng đời sản phẩm (CRUD Operations):</b> Đăng tải phiên đấu giá mới, chỉnh sửa thông tin qua Modal Dialog và thực thi lệnh xóa bản ghi luồng ngầm qua Socket.</li>
 *   <li><b>Biểu mẫu động (Dynamic Form Matrix):</b> Lắng nghe sự thay đổi danh mục (ComboBox Listener) để chuyển đổi hiển thị các Container dữ liệu đặc thù (Điện tử, Nghệ thuật, Phương tiện).</li>
 *   <li><b>Xử lý hình ảnh nâng cao (Image Compression Engine):</b> Tích hợp thuật toán nén đồ họa (Bilinear Interpolation) giới hạn biên độ 1024px và xuất luồng JPEG chất lượng 0.7 trước khi mã hóa Base64 để tối ưu hóa băng thông truyền tải mạng.</li>
 *   <li><b>Xác thực điều kiện ngặt (Rigid Validation):</b> Kiểm soát tính toàn vẹn dữ liệu số tài chính, thời hạn và ràng buộc chặn thời gian bắt đầu phải thuộc thì hiện tại hoặc tương lai.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.model.dto.CreateAuctionDTO
 */
public class ManageSellerController {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Lưu vết luồng IO mạng, cô lập các lỗi kết nối truyền dẫn gói tin và giám sát chu kỳ nạp tài nguyên.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageSellerController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - CẤU TRÚC BẢNG SẢN PHẨM ĐĂNG BÁN (GRID VIEW)
    // =========================================================================
    @FXML private TableView<AuctionSummaryDTO> tableProducts;
    @FXML private TableColumn<AuctionSummaryDTO, String> colId;
    @FXML private TableColumn<AuctionSummaryDTO, String> colName;
    @FXML private TableColumn<AuctionSummaryDTO, String> colType;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colStartPrice;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colStatus;
    @FXML private TableColumn<AuctionSummaryDTO, Void> colAction;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - KHỐI NHẬP LIỆU NỀN TẢNG (BASE INPUT FORM)
    // =========================================================================
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtPrice;
    @FXML private TextField txtDurationHours;
    @FXML private TextField txtStepPrice;
    @FXML private DatePicker dpStartDate;
    @FXML private ComboBox<String> cbStartHour;
    @FXML private ComboBox<String> cbStartMinute;
    @FXML private Label lblImageStatus;
    @FXML private Button btnChooseImage;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea txtDesc;
    @FXML private Label lblSuccess;

    /** Chuỗi văn bản nhị phân lưu giữ mã hóa dữ liệu hình ảnh sản phẩm sau nén. */
    private String imageBase64;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - PHÂN HỆ BIỂU MẪU ĐỘNG (DYNAMIC CONTAINERS)
    // =========================================================================
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

    @FXML private Button btnCreateSubmit;

    /** Danh sách quan sát (ObservableList) liên kết dữ liệu tóm tắt phiên đấu giá cá nhân trực tiếp lên TableView. */
    private ObservableList<AuctionSummaryDTO> productList = FXCollections.observableArrayList();

    /**
     * Phương thức vòng đời khởi tạo phân hệ giao diện (Lifecycle Hook).
     * Thiết lập liên kết cấu trúc bảng, phát lệnh nạp dữ liệu mạng, sinh mảng danh sách giờ/phút cho cấu trúc ComboBox,
     * và cấu hình bộ lắng nghe thay đổi thuộc tính để hoán đổi ma trận Form nhập liệu theo danh mục mặt hàng.
     */
    @FXML
    public void initialize() {
        setupTable();
        loadMyAuctions();

        // Khởi tạo tập hợp dữ liệu số đại diện cho 24 giờ trong ngày
        ObservableList<String> hours = FXCollections.observableArrayList();
        for (int i = 0; i < 24; i++) {
            hours.add(String.format("%02d", i));
        }
        cbStartHour.setItems(hours);
        cbStartHour.setValue("00");

        // Khởi tạo tập hợp dữ liệu số đại diện cho 60 phút trong giờ
        ObservableList<String> minutes = FXCollections.observableArrayList();
        for (int i = 0; i < 60; i++) {
            minutes.add(String.format("%02d", i));
        }
        cbStartMinute.setItems(minutes);
        cbStartMinute.setValue("00");

        // Đính kèm bộ giám sát thuộc tính (Value Property Listener) hỗ trợ hoán đổi cấu trúc layout động
        cbType.valueProperty().addListener((obs, oldVal, newVal) -> {
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
     * Định hình cấu trúc phân bổ các cột thuộc tính bên trong TableView và định nghĩa quy tắc render Cell.
     * Tích hợp Badge màu nhận diện danh mục sản phẩm, định dạng tiền tệ quốc gia VND, Badge văn bản biểu thị trạng thái vòng đời phiên,
     * và xây dựng tổ hợp nút lệnh chức năng tương tác (Xem, Sửa, Xóa) đính kèm logic bảo mật nghiệp vụ dòng dữ liệu.
     */
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));

        // Định dạng cột hiển thị danh mục mặt hàng: Ánh xạ từ khóa mã máy sang ngôn ngữ tự nhiên tiếng Việt chuẩn hóa
        colType.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        colType.setCellFactory(tc -> new TableCell<>() {
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

        // Định dạng cột hiển thị số vốn giá sàn khởi điểm: Chuẩn hóa phân tách dấu hàng nghìn
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
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

        // Định dạng cột hiển thị mức giá đặt hiện tại: Gán style CSS highlight làm nổi bật trường tiền tệ
        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
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

        // Định dạng cột trạng thái: Tự động phân tách và đính kèm class phong cách CSS màu sắc nhận diện vòng đời phiên
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
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
                    if (status.equals("RUNNING")) {
                        lbl.getStyleClass().add("seller-status-running");
                    } else if (status.equals("FINISHED") || status.equals("PAID")) {
                        lbl.getStyleClass().add("seller-status-finished");
                    } else {
                        lbl.getStyleClass().add("seller-status-other");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        // Kiến trúc cột chức năng tương tác hành động: Sinh các nút bấm đồ họa động và thiết lập bộ rào cản xác thực nghiệp vụ
        colAction.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                    setText(null);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Thiết lập nút điều hướng truy cập nhanh phòng phiên chi tiết sản phẩm
                    Button btnView = new Button("👁 Xem");
                    btnView.getStyleClass().add("seller-btn-view");
                    btnView.setOnAction(e -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        MainController.getInstance().openAuctionDetail(row.getAuctionId());
                    });

                    // Thiết lập nút can thiệp chỉnh sửa thông số: Tích hợp chốt chặn bảo mật điều kiện trạng thái kinh doanh
                    Button btnEdit = new Button("✏️ Sửa");
                    btnEdit.getStyleClass().add("seller-btn-edit");
                    btnEdit.setOnAction(e -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        // Chốt chặn 1: Khóa tuyệt đối quyền can thiệp nếu phiên đấu giá đã kết thúc tất toán lịch sử
                        if (row.getStatus().equalsIgnoreCase("FINISHED") || row.getStatus().equalsIgnoreCase("PAID") || row.getStatus().equalsIgnoreCase("CLOSED")) {
                            AlertUtils.showError("Lỗi", "Không thể sửa phiên đấu giá đã kết thúc!");
                            return;
                        }
                        // Chốt chặn 2: Ngăn cản thay đổi thuộc tính kết cấu khi phiên đang chạy và đã phát sinh giao dịch đặt giá
                        if (row.getStatus().equalsIgnoreCase("RUNNING") && row.getCurrentPrice() > row.getStartingPrice()) {
                            AlertUtils.showError("Lỗi", "Không thể sửa sản phẩm đã có người đặt giá!");
                            return;
                        }

                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/EditAuctionDialog.fxml"));
                            javafx.scene.Parent root = loader.load();

                            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                            dialogStage.setTitle("Chỉnh sửa sản phẩm");
                            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL); // Khóa quyền kiểm soát tương tác bên ngoài cửa sổ Modal popup
                            dialogStage.setScene(new javafx.scene.Scene(root));

                            EditAuctionDialogController controller = loader.getController();
                            controller.initData(row.getAuctionId(), row.getItemName(), row.getStartingPrice(), row.getItemType(), dialogStage, () -> {
                                loadMyAuctions(); // Thực thi hàm Callback làm mới danh sách cục bộ ngay khi lưu thành công
                                AlertUtils.showInfo("Thành công", "Đã cập nhật sản phẩm thành công!");
                            });

                            dialogStage.showAndWait();
                        } catch (IOException ex) {
                            LOGGER.error("Gặp sự cố lỗi nghiêm trọng khi khởi tạo cửa sổ chỉnh sửa thông tin đấu giá fxml", ex);
                        }
                    });

                    // Thiết lập nút phát tín hiệu lệnh xóa bỏ bản ghi khỏi hệ thống máy chủ
                    Button btnDel = new Button("🗑");
                    btnDel.getStyleClass().add("seller-btn-del");
                    btnDel.setOnAction(e -> {
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
     * Kích hoạt luồng Worker Thread ngầm truyền gói tin yêu cầu mạng GET_ALL_AUCTIONS.
     * Tiếp nhận mảng dữ liệu phân phối toàn cục từ Server và ứng dụng thuật toán lọc trích xuất
     * các phần tử thuộc quyền sở hữu cá nhân dựa vào đối sánh thông số Session định danh hiện hành.
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
                                // Thực hiện mệnh đề kiểm tra đối sánh lồng nhau để phân tách đúng bản ghi của người bán
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
     * Tiếp nhận hành động click chọn tệp tin đồ họa hình ảnh sản phẩm đại diện (Choose Image Action).
     * Khởi tạo FileChooser bọc bộ lọc định dạng mở rộng ảnh, xác thực kích cỡ dung lượng biên dưới 5MB
     * trước khi chuyển giao dòng File qua động cơ nén nhị phân.
     *
     * @param event Sự kiện kích hoạt Action gửi từ giao diện người dùng FXML
     */
    @FXML
    void handleChooseImage(ActionEvent event) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new javafx.stage.FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        java.io.File selectedFile = fileChooser.showOpenDialog(txtName.getScene().getWindow());
        if (selectedFile != null) {
            long sizeInBytes = selectedFile.length();
            // Bộ rào cản ngăn chặn tải tệp tin có dung lượng quá khổ gây nghẽn đường truyền mạng Socket
            if (sizeInBytes >= 5 * 1024 * 1024) {
                AlertUtils.showError("Lỗi", "Kích thước ảnh phải dưới 5MB!");
                return;
            }
            try {
                byte[] compressedBytes = compressImage(selectedFile); // Kích hoạt động cơ bóp nghẹt dung lượng ảnh
                this.imageBase64 = java.util.Base64.getEncoder().encodeToString(compressedBytes); // Mã hóa chuỗi Base64 chu quy chuẩn truyền dẫn
                lblImageStatus.setText("Đã chọn: " + selectedFile.getName());
            } catch (IOException e) {
                LOGGER.error("Lỗi khi đọc hoặc nén file ảnh", e);
                AlertUtils.showError("Lỗi", "Không thể đọc hoặc nén file ảnh!");
            }
        }
    }

    /**
     * Động cơ giải mã và tái cấu trúc kích thước nén luồng ảnh đồ họa (Flyweight Image Compression Engine).
     * Phân tích tệp dữ liệu BufferedImage, tính toán tỷ lệ co giãn thu nhỏ biên độ hình học (Scale Ratio)
     * dựa theo trục định vị tối đa 1024px, áp dụng bộ lọc làm mịn nội suy tuyến tính song song (Bilinear Interpolation)
     * và kết xuất luồng dữ liệu byte nhị phân định dạng JPEG cấu hình tham số chất lượng nén cố định 0.7.
     *
     * @param file Đối tượng File trỏ tới vị trí tệp tin hình ảnh nguồn trên đĩa cứng
     * @return Mảng dữ liệu byte nhị phân `byte[]` biểu diễn ảnh sau xử lý nén
     * @throws IOException Ngoại lệ phát sinh khi luồng truy cập file hoặc bộ mã hóa đồ họa gặp sự cố lỗi I/O
     */
    private byte[] compressImage(java.io.File file) throws IOException {
        java.awt.image.BufferedImage originalImage = javax.imageio.ImageIO.read(file);
        if (originalImage == null) {
            throw new IOException("Không thể đọc định dạng hình ảnh!");
        }

        int maxDimension = 1024;
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();
        java.awt.image.BufferedImage scaledImage;

        // Thuật toán kiểm tra và tính toán lát cắt kích cỡ hình học theo tỷ lệ thu nhỏ
        if (originalWidth > maxDimension || originalHeight > maxDimension) {
            double scale = Math.min((double) maxDimension / originalWidth, (double) maxDimension / originalHeight);
            int targetWidth = (int) (originalWidth * scale);
            int targetHeight = (int) (originalHeight * scale);

            scaledImage = new java.awt.image.BufferedImage(targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = scaledImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, targetWidth, targetHeight);
            g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawImage(originalImage, 0, 0, targetWidth, targetHeight, null);
            g2d.dispose();
        } else {
            scaledImage = new java.awt.image.BufferedImage(originalWidth, originalHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g2d = scaledImage.createGraphics();
            g2d.setColor(java.awt.Color.WHITE);
            g2d.fillRect(0, 0, originalWidth, originalHeight);
            g2d.drawImage(originalImage, 0, 0, null);
            g2d.dispose();
        }

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
                param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(0.7f); // Thiết lập nén hao hụt chất lượng ở mức tối ưu 70%
            }
            writer.write(null, new javax.imageio.IIOImage(scaledImage, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    /**
     * Đon nhận hành động nhấn nút khởi tạo Đăng ký phiên đấu giá mới (Create Submit Action).
     * Tiến hành kiểm soát xác thực ranh giới trường dữ liệu nhập, bóc tách cấu trúc ghép chuỗi văn bản Ngày Giờ
     * định dạng chuẩn hệ thống, xác thực mốc thời gian bắt đầu thuộc tương lai, đóng gói cấu trúc CreateAuctionDTO
     * tích hợp thông số mở rộng mở rộng theo danh mục chuyên biệt và gửi chỉ thị qua kênh truyền Socket.
     *
     * @param event Sự kiện kích hoạt Action gửi từ giao diện người dùng FXML
     */
    @FXML
    void handleCreate(ActionEvent event) {
        // Kiểm tra ràng buộc ngăn chặn gửi biểu mẫu chứa các phân vùng thông tin bắt buộc bị trống
        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                txtDurationHours.getText().trim().isEmpty() || txtStepPrice.getText().trim().isEmpty() || cbType.getValue() == null) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đủ các trường có dấu *");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            double stepPrice = Double.parseDouble(txtStepPrice.getText().trim());
            int durationHours = Integer.parseInt(txtDurationHours.getText().trim());

            // Xác thực tính logic dương của các tham số số liệu tài chính hệ thống
            if (price <= 0) {
                AlertUtils.showError("Lỗi", "Giá khởi điểm phải là số dương!");
                return;
            }
            if (stepPrice <= 0) {
                AlertUtils.showError("Lỗi", "Bước giá tối thiểu phải là số dương!");
                return;
            }
            if (durationHours <= 0) {
                AlertUtils.showError("Lỗi", "Thời hạn đấu giá phải lớn hơn 0 giờ!");
                return;
            }

            // Tiến trình bóc tách giải mã và ghép chuỗi cấu trúc trường mốc thời gian bắt đầu kích hoạt phòng phiên
            String startTimeStr = "";
            if (dpStartDate.getValue() != null) {
                String dateStr = dpStartDate.getValue().toString(); // Trích xuất cấu trúc văn bản: yyyy-MM-dd
                String hourStr = cbStartHour.getValue() != null ? cbStartHour.getValue() : "00";
                String minuteStr = cbStartMinute.getValue() != null ? cbStartMinute.getValue() : "00";
                startTimeStr = dateStr + " " + hourStr + ":" + minuteStr; // Kết quả định dạng tổng hợp: yyyy-MM-dd HH:mm

                // Thuật toán rào cản xác thực thời gian bắt đầu phòng phiên bắt buộc phải nằm ở thì tương lai
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
            dto.setStartTimeStr(startTimeStr);
            dto.setImageBase64(this.imageBase64);
            dto.setCondition(cbCondition.getValue() != null ? cbCondition.getValue() : "Mới");
            dto.setDescription(txtDesc.getText().trim());

            // Thực hiện đóng gói bổ sung có chọn lọc các thuộc tính mở rộng chuyên biệt của từng loại thực thể sản phẩm
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

            // Chuyển giao gói tin DTO mạng sang luồng Worker Thread IO ngầm kết nối Socket
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request request = new Request(RequestType.CREATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                            handleClear(null); // Thực hiện làm rỗng dọn dẹp Form ngay khi tạo thành công
                            loadMyAuctions(); // Làm mới lại lưới TableView bản ghi
                            lblSuccess.setText("🎉 Tạo phiên đấu giá thành công!");
                            lblSuccess.setVisible(true);
                            lblSuccess.setManaged(true);

                            // Khởi chạy dòng thời gian tạm hoãn (PauseTransition) để tự động ẩn nhãn thông báo thành công sau 3 giây
                            javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(3));
                            delay.setOnFinished(e -> {
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
     * Đón nhận hành động Làm sạch biểu mẫu (Reset Form Action).
     * Tiến hành xóa sạch toàn bộ nội dung văn bản hiển thị và đưa tất cả cấu trúc trường thuộc tính về trạng thái rỗng nguyên bản.
     *
     * @param event Sự kiện kích hoạt Action gửi từ giao diện người dùng FXML (Có thể nhận null khi gọi nội bộ)
     */
    @FXML
    void handleClear(ActionEvent event) {
        txtName.clear();
        cbType.setValue(null);
        txtPrice.clear();
        txtDurationHours.clear();
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

    /**
     * @deprecated Phương thức điều khiển cũ kế thừa — logic xử lý nghiệp vụ xóa bỏ bản ghi hiện tại
     * đã được chuyển đổi tích hợp trực tiếp thông qua cơ chế TableCell Factory đồ họa trong kiến trúc bảng.
     */
    @Deprecated
    @FXML
    void handleDelete(ActionEvent event) {
        // Phương thức cũ — không còn sử dụng trong kiến trúc nghiệp vụ hiện hành
    }

    /**
     * Đóng gói mã chỉ thị và thực hiện truyền tải lệnh yêu cầu mạng DELETE_AUCTION lên Server luồng ngầm.
     * Thực hiện bóc tách, cô lập và kết xuất thông điệp cảnh báo phù hợp lên lớp giao diện Client nếu phiên đấu giá
     * mục tiêu đang ở trạng thái chạy kích hoạt hoặc Server từ chối lệnh xóa.
     *
     * @param selected Thực thể đối tượng tóm tắt phiên đấu giá AuctionSummaryDTO đang được nhắm chọn xóa
     */
    private void handleDeleteRow(AuctionSummaryDTO selected) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.DELETE_AUCTION, selected.getAuctionId());
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        loadMyAuctions(); // Làm mới lại cấu trúc lưới TableView bản ghi ngay khi máy chủ phản hồi xóa thành công
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
}