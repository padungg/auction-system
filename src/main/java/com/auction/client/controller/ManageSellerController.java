package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.CreateAuctionDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;
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
 * Bộ điều khiển phân hệ Quản lý dành cho Người bán (Manage Seller Controller).
 * Đảm nhiệm vai trò quản lý danh sách sản phẩm do chính tài khoản đăng tải, khởi tạo phiên đấu giá mới,
 * điều phối ẩn hiện các Form động theo danh mục mặt hàng, và xử lý các tác vụ sửa/xóa phiên đấu giá.
 */
public class ManageSellerController {

    /**
     * Khởi tạo thành phần Logger theo tiêu chuẩn SLF4J nhằm phục vụ công tác giám sát luồng vận hành,
     * lưu vết các ngoại lệ ngắt kết nối mạng bất đồng bộ khi thực hiện gửi chỉ thị tạo/sửa/xóa phiên đấu giá.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ManageSellerController.class);

    @FXML private TableView<AuctionSummaryDTO> tableProducts;
    @FXML private TableColumn<AuctionSummaryDTO, String> colId;
    @FXML private TableColumn<AuctionSummaryDTO, String> colName;
    @FXML private TableColumn<AuctionSummaryDTO, String> colType;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colStartPrice;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colCurrentPrice;
    @FXML private TableColumn<AuctionSummaryDTO, String> colStatus;
    @FXML private TableColumn<AuctionSummaryDTO, Void> colAction;

    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbType;
    @FXML private TextField txtPrice;
    @FXML private TextField txtDays;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextArea txtDesc;
    @FXML private Label lblSuccess;

    // Các phân hệ biểu mẫu động (Dynamic Panels)
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

    private ObservableList<AuctionSummaryDTO> productList = FXCollections.observableArrayList();
    private String currentEditAuctionId = null; // Giá trị null biểu thị cho chế độ tạo mới (Create Mode)

    @FXML
    public void initialize() {
        setupTable();
        loadMyAuctions();

        // Listener lắng nghe thay đổi danh mục để chuyển đổi động các phân hệ form nhập liệu
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
     * Cấu hình liên kết dữ liệu thuộc tính DTO và xây dựng các nút chức năng (Xem, Sửa, Xóa) lồng nhau trong TableCell.
     */
    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("auctionId"));
        colName.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colType.setCellValueFactory(new PropertyValueFactory<>("itemType"));
        colType.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String type, boolean empty) {
                super.updateItem(type, empty);
                if (empty || type == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label(type);
                    lbl.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #9333ea; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colStartPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) setText(null);
                else setText(String.format("%,.0f VNĐ", price));
            }
        });

        colCurrentPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colCurrentPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setStyle("-fx-text-fill: #2563eb; -fx-font-weight: bold;");
                }
            }
        });

        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label lbl = new Label("● " + status);
                    if (status.equals("RUNNING")) {
                        lbl.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if (status.equals("FINISHED") || status.equals("PAID")) {
                        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04; -fx-padding: 4 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        colAction.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(8);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Button btnView = new Button("👁 Xem");
                    btnView.setStyle("-fx-background-color: transparent; -fx-text-fill: #475569; -fx-cursor: hand;");
                    btnView.setOnAction(e -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        MainController.getInstance().openAuctionDetail(row.getAuctionId());
                    });

                    Button btnEdit = new Button("✏️ Sửa");
                    btnEdit.setStyle("-fx-background-color: #f59e0b; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                    btnEdit.setOnAction(e -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        if (row.getStatus().equalsIgnoreCase("FINISHED") || row.getStatus().equalsIgnoreCase("PAID") || row.getStatus().equalsIgnoreCase("CLOSED")) {
                            showAlert("Lỗi", "Không thể sửa phiên đấu giá đã kết thúc!");
                            return;
                        }
                        if (row.getStatus().equalsIgnoreCase("RUNNING") && row.getCurrentPrice() > row.getStartingPrice()) {
                            showAlert("Lỗi", "Không thể sửa sản phẩm đã có người đặt giá!");
                            return;
                        }

                        try {
                            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/edit_auction_dialog.fxml"));
                            javafx.scene.Parent root = loader.load();

                            javafx.stage.Stage dialogStage = new javafx.stage.Stage();
                            dialogStage.setTitle("Chỉnh sửa sản phẩm");
                            dialogStage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                            dialogStage.setScene(new javafx.scene.Scene(root));

                            EditAuctionDialogController controller = loader.getController();
                            controller.initData(row.getAuctionId(), row.getItemName(), row.getStartingPrice(), row.getItemType(), dialogStage, () -> {
                                loadMyAuctions();
                                showAlert("Thành công", "Đã cập nhật sản phẩm thành công!");
                            });

                            dialogStage.showAndWait();
                        } catch (IOException ex) {
                            LOGGER.error("Gặp sự cố lỗi nghiêm trọng khi khởi tạo cửa sổ chỉnh sửa thông tin đấu giá fxml", ex);
                        }
                    });

                    Button btnDel = new Button("🗑");
                    btnDel.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                    btnDel.setOnAction(e -> {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        handleDeleteRow(row);
                    });

                    box.getChildren().addAll(btnView, btnEdit, btnDel);
                    setGraphic(box);
                }
            }
        });

        tableProducts.setItems(productList);
    }

    /**
     * Khởi chạy Worker Thread đồng bộ toàn bộ danh sách phiên đấu giá hiện hành,
     * thực hiện bộ lọc theo ID/Tên của tài khoản hiện tại để hiển thị đúng sản phẩm của người bán.
     */
    private void loadMyAuctions() {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    AuctionSummaryDTO[] arr = gson.fromJson(gson.toJson(response.getPayload()), AuctionSummaryDTO[].class);

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
        }).start();
    }

    /**
     * Tiếp nhận hành động Đăng ký sản phẩm mới. Kiểm tra tính hợp lệ dữ liệu biểu mẫu,
     * đóng gói cấu trúc dữ liệu mở rộng theo danh mục chuyên biệt và gửi chỉ thị CREATE_AUCTION lên máy chủ Server.
     */
    @FXML
    void handleCreate(ActionEvent event) {
        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty() ||
                txtDays.getText().trim().isEmpty() || cbType.getValue() == null) {
            showAlert("Lỗi", "Vui lòng nhập đủ các trường có dấu *");
            return;
        }

        try {
            double price = Double.parseDouble(txtPrice.getText().trim());
            int days = Integer.parseInt(txtDays.getText().trim());

            CreateAuctionDTO dto = new CreateAuctionDTO();

            dto.setName(txtName.getText().trim());
            dto.setItemType(cbType.getValue());
            dto.setStartingPrice(price);
            dto.setDurationDays(days);
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

            new Thread(() -> {
                try {
                    Request request = new Request(RequestType.CREATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response.getStatus() == ResponseStatus.SUCCESS) {
                            handleClear(null);
                            loadMyAuctions();
                            lblSuccess.setText("🎉 Tạo phiên đấu giá thành công!");
                            lblSuccess.setVisible(true);
                            lblSuccess.setManaged(true);

                            new Thread(() -> {
                                try { Thread.sleep(3000); } catch (Exception ignored) {}
                                Platform.runLater(() -> {
                                    lblSuccess.setVisible(false);
                                    lblSuccess.setManaged(false);
                                });
                            }).start();
                        } else {
                            showAlert("Lỗi", response.getMessage());
                        }
                    });
                } catch (Exception e) {
                    LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi gói chỉ thị CREATE_AUCTION", e);
                }
            }).start();

        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Giá hoặc số ngày không hợp lệ (phải là số)!");
        }
    }

    /**
     * Dọn dẹp văn bản hiển thị cũ và đưa toàn bộ các trường nhập liệu trên Biểu mẫu về trạng thái rỗng.
     */
    @FXML
    void handleClear(ActionEvent event) {
        txtName.clear();
        cbType.setValue(null);
        txtPrice.clear();
        txtDays.clear();
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

    @FXML
    void handleDelete(ActionEvent event) {
        // Phương thức cũ — đã chuyển đổi tích hợp xử lý trực tiếp thông qua nút đồ họa trong bảng
    }

    /**
     * Đóng gói mã chỉ thị và thực hiện truyền tải chỉ thị xóa bỏ phiên đấu giá cụ thể lên phía máy chủ Server.
     */
    private void handleDeleteRow(AuctionSummaryDTO selected) {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.DELETE_AUCTION, selected.getAuctionId());
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response.getStatus() == ResponseStatus.SUCCESS) {
                        loadMyAuctions();
                    } else {
                        showAlert("Lỗi", "Phiên đấu giá đang chạy hoặc Server chưa hỗ trợ xóa.");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi gửi chỉ thị xóa phiên đấu giá: {}", selected.getAuctionId(), e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Đóng gói cấu trúc khởi tạo và hiển thị cấu trúc hộp thoại thông báo Pop-up cảnh báo chạy trên giao diện.
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}