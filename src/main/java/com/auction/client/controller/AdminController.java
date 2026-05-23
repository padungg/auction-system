package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.AlertUtils;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.UserResponseDTO;
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

/**
 * <h2>AdminController</h2>
 * <p>
 * Controller trung tâm điều phối toàn bộ phân hệ giao diện Quản trị hệ thống (Admin Dashboard).
 * </p>
 *
 * <p><b>Các nghiệp vụ quản trị cốt lõi bao gồm:</b></p>
 * <ul>
 *   <li><b>Quản lý người dùng:</b> Giám sát danh sách thành viên, cập nhật trạng thái bảo mật thông qua cơ chế khóa (Lock) và mở khóa (Unlock) tài khoản hàng loạt (Batch Processing).</li>
 *   <li><b>Quản lý phiên đấu giá:</b> Giám sát trạng thái thời gian thực của các phiên, thực thi các đặc quyền can thiệp cấp cao như cưỡng chế đóng (Force Close), hủy bỏ (Cancel) hoặc xác nhận tất toán tài chính (Mark Paid).</li>
 *   <li><b>Kiểm soát UI:</b> Tích hợp thuật toán phân trang độc lập cho cả danh sách thực thể người dùng và phiên đấu giá, đồng thời tính toán số liệu thống kê định lượng trực quan.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.model.protocol.Request
 */
public class AdminController {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo cơ chế an toàn đa luồng (Thread-safe).
     * Phục vụ mục đích theo dõi dấu vết luồng IO mạng, cô lập ngoại lệ vận hành và lưu vết tệp tin nhật ký hệ thống.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);


    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - QUẢN LÝ NGƯỜI DÙNG (USERS MANAGEMENT)
    // =========================================================================
    @FXML private TableView<UserResponseDTO> tableUsers;
    @FXML private TableColumn<UserResponseDTO, Boolean> colSelect;
    @FXML private TableColumn<UserResponseDTO, String> colUserId;
    @FXML private TableColumn<UserResponseDTO, String> colUserUsername;
    @FXML private TableColumn<UserResponseDTO, String> colUserFullName;
    @FXML private TableColumn<UserResponseDTO, String> colUserEmail;
    @FXML private TableColumn<UserResponseDTO, Object> colUserRole;
    @FXML private TableColumn<UserResponseDTO, Double> colUserBalance;
    @FXML private TableColumn<UserResponseDTO, Boolean> colUserStatus;

    // Nhãn hiển thị chỉ số tổng hợp định lượng trạng thái tài khoản người dùng
    @FXML private javafx.scene.control.Label statTotalUsers;
    @FXML private javafx.scene.control.Label statActiveUsers;
    @FXML private javafx.scene.control.Label statLockedUsers;
    @FXML private javafx.scene.control.Label statAdminUsers;

    // Thành phần điều khiển luồng hiển thị phân trang người dùng
    @FXML private javafx.scene.layout.HBox pageButtonBox;
    @FXML private javafx.scene.control.Label lblPageInfo;
    @FXML private javafx.scene.control.Button btnFirstPage;
    @FXML private javafx.scene.control.Button btnPrevPage;
    @FXML private javafx.scene.control.Button btnNextPage;
    @FXML private javafx.scene.control.Button btnLastPage;

    // Cấu hình tham số phân trang và cấu trúc bộ nhớ đệm danh sách người dùng
    private static final int PAGE_SIZE = 15;
    private int currentPage = 0; // Chỉ mục trang hiện tại dựa trên gốc 0 (0-indexed)
    private java.util.List<UserResponseDTO> allUsersList = new java.util.ArrayList<>();

    private ObservableList<UserResponseDTO> usersList = FXCollections.observableArrayList();

    /** Ánh xạ bộ nhớ duy trì trạng thái của các checkbox hàng chọn, độc lập với chu kỳ nạp lại dữ liệu grid. */
    private java.util.Map<String, javafx.beans.property.BooleanProperty> selectedUsersMap = new java.util.HashMap<>();

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - QUẢN LÝ PHIÊN ĐẤU GIÁ (AUCTIONS MANAGEMENT)
    // =========================================================================
    @FXML private TableView<AuctionSummaryDTO> tableAuctions;
    @FXML private TableColumn<AuctionSummaryDTO, String> colAucProduct;
    @FXML private TableColumn<AuctionSummaryDTO, Double> colAucPrice;
    @FXML private TableColumn<AuctionSummaryDTO, Integer> colAucViews;
    @FXML private TableColumn<AuctionSummaryDTO, Integer> colAucBids;
    @FXML private TableColumn<AuctionSummaryDTO, String> colAucWinner;
    @FXML private TableColumn<AuctionSummaryDTO, String> colAucSeller;
    @FXML private TableColumn<AuctionSummaryDTO, String> colAucEnd;
    @FXML private TableColumn<AuctionSummaryDTO, String> colAucStatus;
    @FXML private TableColumn<AuctionSummaryDTO, Void> colAucAction;

    // Nhãn hiển thị chỉ số đo lường định lượng trạng thái các phiên đấu giá toàn cục
    @FXML private javafx.scene.control.Label aucStatTotal;
    @FXML private javafx.scene.control.Label aucStatRunning;
    @FXML private javafx.scene.control.Label aucStatOpen;
    @FXML private javafx.scene.control.Label aucStatFinished;
    @FXML private javafx.scene.control.Label aucStatPaid;
    @FXML private javafx.scene.control.Label aucStatCanceled;

    // Thành phần điều khiển luồng hiển thị phân trang danh sách đấu giá
    @FXML private javafx.scene.layout.HBox aucPageButtonBox;
    @FXML private javafx.scene.control.Label aucLblPageInfo;
    @FXML private javafx.scene.control.Button aucBtnFirst;
    @FXML private javafx.scene.control.Button aucBtnPrev;
    @FXML private javafx.scene.control.Button aucBtnNext;
    @FXML private javafx.scene.control.Button aucBtnLast;

    // Cấu hình tham số phân trang và cấu trúc bộ nhớ đệm danh sách đấu giá
    private static final int AUC_PAGE_SIZE = 5;
    private int aucCurrentPage = 0;
    private java.util.List<AuctionSummaryDTO> allAuctionsList = new java.util.ArrayList<>();

    private ObservableList<AuctionSummaryDTO> auctionsList = FXCollections.observableArrayList();

    /**
     * Phương thức khởi tạo cấu hình vòng đời của phân hệ JavaFX (Lifecycle Hook).
     * Được tự động kích hoạt sau khi kiến trúc tệp tin thiết kế FXML được nạp thành công vào hệ thống.
     * Kiểm tra tính sẵn sàng của cấu trúc giao diện và phát lệnh kéo dữ liệu ban đầu từ mạng.
     */
    @FXML
    public void initialize() {
        if (tableUsers != null) {
            setupUsersTable();
            loadUsers();
        }
        if (tableAuctions != null) {
            setupAuctionsTable();
            loadAuctions();
        }
    }

    /**
     * Định hình quy tắc liên kết thuộc tính đối tượng (Property Binding) và logic render tùy biến giao diện cho bảng Người dùng.
     * Bao gồm cấu hình cột Checkbox chọn hàng loạt, định dạng tiền tệ VND, Badge phân quyền và nhãn hiển thị trạng thái bảo mật.
     */
    private void setupUsersTable() {
        if (colSelect != null) {
            colSelect.setCellValueFactory(cellData -> {
                String id = cellData.getValue().getId();
                selectedUsersMap.putIfAbsent(id, new javafx.beans.property.SimpleBooleanProperty(false));
                return selectedUsersMap.get(id);
            });
            colSelect.setCellFactory(javafx.scene.control.cell.CheckBoxTableCell.forTableColumn(colSelect));
        }

        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUserUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserFullName.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        colUserEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Định dạng cột hiển thị vai trò: Áp dụng Badge đồ họa phân cấp màu tương ứng với quyền hạn quản trị
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserRole.setCellFactory(tc -> new TableCell<UserResponseDTO, Object>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(Object roleObj, boolean empty) {
                super.updateItem(roleObj, empty);
                if (empty || roleObj == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    String role = roleObj.toString();
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        badge.setText("QUẢN TRỊ VIÊN");
                        badge.getStyleClass().setAll("role-badge-admin");
                    } else {
                        badge.setText("THÀNH VIÊN");
                        badge.getStyleClass().setAll("role-badge-user");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Định dạng cột hiển thị số dư tài khoản: Áp dụng quy chuẩn cấu trúc phân tách hàng nghìn cho tiền tệ quốc gia
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colUserBalance.setCellFactory(tc -> new TableCell<UserResponseDTO, Double>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) setText(null);
                else setText(String.format("%,.0fđ", balance));
            }
        });

        // Định dạng cột trạng thái: Tạo nhãn text trực quan kèm biểu tượng emoji tương ứng với trạng thái kích hoạt tài khoản
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colUserStatus.setCellFactory(tc -> new TableCell<UserResponseDTO, Boolean>() {
            private final Label lbl = new Label();
            @Override
            protected void updateItem(Boolean isActive, boolean empty) {
                super.updateItem(isActive, empty);
                if (empty || isActive == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    lbl.setText(isActive ? "✅ Hoạt động" : "🔒 Đã khóa");
                    lbl.getStyleClass().setAll(isActive ? "status-lbl-active" : "status-lbl-locked");
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        tableUsers.setItems(usersList);
        tableUsers.setEditable(true); // Bật quyền chỉnh sửa để CheckBoxTableCell tiếp nhận trực tiếp và cập nhật tương tác click chuột từ quản trị viên
    }

    /**
     * Xây dựng kiến trúc Cell đồ họa phức hợp (VBox/HBox) hiển thị đa trường thông tin cho cột Sản phẩm,
     * đồng thời thực hiện định dạng Badge trạng thái và gán tập hợp các bộ lắng nghe hành động (Action Buttons) cho mỗi dòng.
     */
    private void setupAuctionsTable() {
        colAucProduct.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucProduct.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            private final Label nameLbl = new Label();
            private final Label idLbl = new Label();
            private final Label typeLbl = new Label();
            private final javafx.scene.layout.HBox infoBox = new javafx.scene.layout.HBox(6);
            private final javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4);

            {
                nameLbl.getStyleClass().add("prod-title-lbl");
                idLbl.getStyleClass().add("prod-id-lbl");
                infoBox.getChildren().addAll(typeLbl, idLbl);
                box.getChildren().addAll(nameLbl, infoBox);
            }

            @Override
            protected void updateItem(String itemName, boolean empty) {
                super.updateItem(itemName, empty);
                if (empty || itemName == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    int index = getIndex();
                    if (index >= 0 && index < getTableView().getItems().size()) {
                        AuctionSummaryDTO dto = getTableView().getItems().get(index);
                        nameLbl.setText(itemName);
                        idLbl.setText("#" + dto.getAuctionId());

                        // Phân tách màu sắc nhãn đại diện dựa theo danh mục phân loại mặt hàng đấu giá
                        String type = dto.getItemType();
                        typeLbl.getStyleClass().clear();
                        if ("ELECTRONICS".equalsIgnoreCase(type)) {
                            typeLbl.setText("Điện tử");
                            typeLbl.getStyleClass().add("prod-type-electronics");
                        } else if ("ART".equalsIgnoreCase(type)) {
                            typeLbl.setText("Nghệ thuật");
                            typeLbl.getStyleClass().add("prod-type-art");
                        } else if ("VEHICLE".equalsIgnoreCase(type)) {
                            typeLbl.setText("Phương tiện");
                            typeLbl.getStyleClass().add("prod-type-vehicle");
                        } else {
                            typeLbl.setText(type);
                            typeLbl.getStyleClass().add("prod-type-default");
                        }

                        setGraphic(box);
                        setText(null);
                    } else {
                        setGraphic(null);
                        setText(null);
                    }
                }
            }
        });

        // Định dạng hiển thị cột giá tiền hiện hành: Làm nổi bật font chữ kèm đơn vị tiền tệ tiêu chuẩn
        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    getStyleClass().remove("price-highlight");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    if (!getStyleClass().contains("price-highlight")) {
                        getStyleClass().add("price-highlight");
                    }
                }
            }
        });

        colAucBids.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
        colAucBids.setStyle("-fx-alignment: CENTER;");

        // Sử dụng thuật toán băm tất định (Deterministic Hash) để giả lập số lượt xem dựa vào ID và số lượt đặt giá hiện tại
        colAucViews.setCellValueFactory(cellData -> {
            int bids = cellData.getValue().getBidCount();
            int hash = cellData.getValue().getAuctionId() != null ? Math.abs(cellData.getValue().getAuctionId().hashCode()) : 0;
            int views = bids == 0 ? (hash % 5) : bids * 3 + (hash % 15);
            return new javafx.beans.property.SimpleIntegerProperty(views).asObject();
        });
        colAucViews.setStyle("-fx-alignment: CENTER; -fx-text-fill: #64748b;");

        // Tùy biến hiển thị cho cột định danh tài khoản người chiến thắng phiên
        colAucWinner.setCellValueFactory(new PropertyValueFactory<>("currentWinnerId"));
        colAucWinner.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String winner, boolean empty) {
                super.updateItem(winner, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().removeAll("winner-undefined", "winner-defined");
                } else if (winner == null || winner.isEmpty()) {
                    setText("Chưa có");
                    getStyleClass().remove("winner-defined");
                    if (!getStyleClass().contains("winner-undefined")) {
                        getStyleClass().add("winner-undefined");
                    }
                } else {
                    setText(winner);
                    getStyleClass().remove("winner-undefined");
                    if (!getStyleClass().contains("winner-defined")) {
                        getStyleClass().add("winner-defined");
                    }
                }
            }
        });

        // Tùy biến hiển thị cho cột định danh tài khoản người đăng bán sản phẩm
        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAucSeller.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String seller, boolean empty) {
                super.updateItem(seller, empty);
                if (empty) {
                    setText(null);
                    getStyleClass().removeAll("seller-undefined", "seller-defined");
                } else if (seller == null || seller.isEmpty()) {
                    setText("Không rõ");
                    getStyleClass().remove("seller-defined");
                    if (!getStyleClass().contains("seller-undefined")) {
                        getStyleClass().add("seller-undefined");
                    }
                } else {
                    setText(seller);
                    getStyleClass().remove("seller-undefined");
                    if (!getStyleClass().contains("seller-defined")) {
                        getStyleClass().add("seller-defined");
                    }
                }
            }
        });

        // Tách biệt trường thời gian kết thúc thành 2 dòng (Giờ và Ngày) nhằm mục đích tối ưu hóa không gian hiển thị cột
        colAucEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colAucEnd.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String end, boolean empty) {
                super.updateItem(end, empty);
                if (empty || end == null) {
                    setText(null);
                    getStyleClass().remove("time-lbl");
                } else {
                    String[] parts = end.split(" ");
                    if (parts.length == 2) {
                        setText(parts[1] + "\n" + parts[0]);
                    } else {
                        setText(end);
                    }
                    if (!getStyleClass().contains("time-lbl")) {
                        getStyleClass().add("time-lbl");
                    }
                }
            }
        });

        // Khởi tạo hệ thống màu sắc nhận diện đặc trưng cho từng trạng thái thực thi phiên đấu giá
        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAucStatus.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            private final Label badge = new Label();
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    badge.getStyleClass().clear();
                    badge.setStyle(null);
                    if ("RUNNING".equalsIgnoreCase(status)) {
                        badge.setText("Đang diễn ra");
                        badge.getStyleClass().add("auc-badge-running");
                    } else if ("OPEN".equalsIgnoreCase(status)) {
                        badge.setText("Sắp diễn ra");
                        badge.getStyleClass().add("auc-badge-open");
                    } else if ("FINISHED".equalsIgnoreCase(status)) {
                        badge.setText("Đã kết thúc");
                        badge.getStyleClass().add("auc-badge-finished");
                    } else if ("PAID".equalsIgnoreCase(status)) {
                        badge.setText("Đã thanh toán");
                        badge.getStyleClass().add("auc-badge-paid");
                    } else if ("CANCELED".equalsIgnoreCase(status) || "CANCELLED".equalsIgnoreCase(status)) {
                        badge.setText("Đã hủy");
                        badge.getStyleClass().add("auc-badge-canceled");
                    } else {
                        badge.setText(status);
                        badge.setStyle("-fx-font-weight: bold;");
                    }
                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Thiết lập bộ chứa tổ hợp nút chức năng quản trị, tự động ẩn/hiện linh hoạt dựa theo trạng thái của phiên
        if (colAucAction != null) {
            colAucAction.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Void>() {
                private final Button btnView = new Button("👁 Xem");
                private final Button btnClose = new Button("Đóng");
                private final Button btnCancel = new Button("Hủy");
                private final Button btnPaid = new Button("Thanh toán");
                private final javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6);

                {
                    btnView.getStyleClass().add("btn-action-view");
                    btnClose.getStyleClass().add("btn-action-close");
                    btnCancel.getStyleClass().add("btn-action-cancel");
                    btnPaid.getStyleClass().add("btn-action-paid");
                    box.setAlignment(javafx.geometry.Pos.CENTER);
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        int index = getIndex();
                        if (index >= 0 && index < getTableView().getItems().size()) {
                            AuctionSummaryDTO row = getTableView().getItems().get(index);

                            btnView.setOnAction(e -> MainController.getInstance().openAuctionDetail(row.getAuctionId()));
                            btnClose.setOnAction(e -> forceCloseAuction(row.getAuctionId()));
                            btnCancel.setOnAction(e -> cancelAuctionAdmin(row.getAuctionId()));
                            btnPaid.setOnAction(e -> markPaidAdmin(row.getAuctionId()));

                            box.getChildren().clear();
                            String status = row.getStatus();
                            if ("RUNNING".equalsIgnoreCase(status) || "OPEN".equalsIgnoreCase(status)) {
                                box.getChildren().addAll(btnView, btnClose, btnCancel);
                            } else if ("FINISHED".equalsIgnoreCase(status)) {
                                box.getChildren().addAll(btnView, btnPaid, btnCancel);
                            } else {
                                box.getChildren().add(btnView);
                            }

                            setGraphic(box);
                            setText(null);
                        } else {
                            setGraphic(null);
                            setText(null);
                        }
                    }
                }
            });
        }

        tableAuctions.setFixedCellSize(60); // Khống chế chiều cao dòng cố định để đảm bảo bố cục hiển thị đồng đều cấu trúc bảng
        tableAuctions.setItems(auctionsList);
    }

    /**
     * Kích hoạt luồng Worker Thread ngầm gửi chỉ thị mạng đồng bộ danh sách toàn bộ người dùng từ máy chủ về thiết bị Client.
     */
    private void loadUsers() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_USERS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    UserResponseDTO[] arr = response.getPayloadAs(UserResponseDTO[].class);
                    Platform.runLater(() -> {
                        allUsersList.clear();
                        if (arr != null) java.util.Collections.addAll(allUsersList, arr);
                        currentPage = 0;
                        updateStats();
                        renderPage();
                    });
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Không thể tải danh sách người dùng từ máy chủ."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_USERS", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Gặp sự cố lỗi mạng khi tải danh sách người dùng."));
            }
        });
    }

    /**
     * Phân tích, tổng hợp dữ liệu danh sách người dùng trong RAM để kết xuất các chỉ số định lượng lên nhóm nhãn thống kê.
     */
    private void updateStats() {
        int total = allUsersList.size();
        long active = allUsersList.stream().filter(u -> u.isActive()).count();
        long locked = total - active;
        long admins = allUsersList.stream()
                .filter(u -> u.getRole() != null && "ADMIN".equalsIgnoreCase(u.getRole().toString()))
                .count();
        if (statTotalUsers != null) statTotalUsers.setText(String.valueOf(total));
        if (statActiveUsers != null) statActiveUsers.setText(String.valueOf(active));
        if (statLockedUsers != null) statLockedUsers.setText(String.valueOf(locked));
        if (statAdminUsers != null) statAdminUsers.setText(String.valueOf(admins));
    }

    /**
     * Trích xuất phân mảnh dữ liệu (Sub-list) tương ứng với chỉ mục trang hiện tại để kết xuất lên giao diện người dùng,
     * đồng thời thực hiện kiểm soát tính vô hiệu hóa nút bấm và sinh mã thanh chọn trang số động.
     */
    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allUsersList.size());

        usersList.clear();
        selectedUsersMap.clear();
        usersList.addAll(allUsersList.subList(from, to));

        if (lblPageInfo != null)
            lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages
                    + "  (" + allUsersList.size() + " người dùng)");

        if (btnFirstPage != null) btnFirstPage.setDisable(currentPage == 0);
        if (btnPrevPage != null)  btnPrevPage.setDisable(currentPage == 0);
        if (btnNextPage != null)  btnNextPage.setDisable(currentPage >= totalPages - 1);
        if (btnLastPage != null)  btnLastPage.setDisable(currentPage >= totalPages - 1);

        // Khởi tạo thanh chỉ mục trang số động dạng trượt (Hiển thị tối đa cố định 5 nút liền kề)
        if (pageButtonBox != null) {
            pageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, currentPage - maxBtn / 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                javafx.scene.control.Button btn = new javafx.scene.control.Button(String.valueOf(p + 1));
                btn.getStyleClass().setAll("button", p == currentPage ? "page-btn-active" : "page-btn-normal");
                btn.setOnAction(e -> { currentPage = pg; renderPage(); });
                pageButtonBox.getChildren().add(btn);
            }
        }
    }

    /**
     * Quay về trang đầu tiên của danh sách người dùng.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void goFirstPage(javafx.event.ActionEvent e) { currentPage = 0; renderPage(); }

    /**
     * Lùi về trang trước của danh sách người dùng.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void goPrevPage(javafx.event.ActionEvent e) {
        if (currentPage > 0) { currentPage--; renderPage(); }
    }

    /**
     * Tiến tới trang sau của danh sách người dùng.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void goNextPage(javafx.event.ActionEvent e) {
        int totalPages = (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) { currentPage++; renderPage(); }
    }

    /**
     * Nhảy thẳng tới trang cuối cùng của danh sách người dùng.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void goLastPage(javafx.event.ActionEvent e) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        currentPage = totalPages - 1;
        renderPage();
    }

    /**
     * Kích hoạt luồng gửi chỉ thị mạng đồng bộ thông tin danh sách toàn bộ các phiên đấu giá đang lưu vết tại Server.
     */
    private void loadAuctions() {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                    AuctionSummaryDTO[] arr = response.getPayloadAs(AuctionSummaryDTO[].class);
                    Platform.runLater(() -> {
                        allAuctionsList.clear();
                        if (arr != null) java.util.Collections.addAll(allAuctionsList, arr);
                        aucCurrentPage = 0;
                        updateAucStats();
                        renderAucPage();
                    });
                } else {
                    Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Không thể tải danh sách phiên đấu giá từ máy chủ."));
                }
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", "Gặp sự cố lỗi mạng khi tải danh sách đấu giá."));
            }
        });
    }

    /**
     * Phân tích cấu trúc danh sách bộ nhớ đệm để tính toán và kết xuất các chỉ số trạng thái phiên đấu giá lên UI labels.
     */
    private void updateAucStats() {
        long total    = allAuctionsList.size();
        long running  = allAuctionsList.stream().filter(a -> "RUNNING".equalsIgnoreCase(a.getStatus())).count();
        long open     = allAuctionsList.stream().filter(a -> "OPEN".equalsIgnoreCase(a.getStatus())).count();
        long finished = allAuctionsList.stream().filter(a -> "FINISHED".equalsIgnoreCase(a.getStatus())).count();
        long paid     = allAuctionsList.stream().filter(a -> "PAID".equalsIgnoreCase(a.getStatus())).count();
        long canceled = allAuctionsList.stream().filter(a -> "CANCELED".equalsIgnoreCase(a.getStatus())).count();
        if (aucStatTotal    != null) aucStatTotal.setText(String.valueOf(total));
        if (aucStatRunning  != null) aucStatRunning.setText(String.valueOf(running));
        if (aucStatOpen     != null) aucStatOpen.setText(String.valueOf(open));
        if (aucStatFinished != null) aucStatFinished.setText(String.valueOf(finished));
        if (aucStatPaid     != null) aucStatPaid.setText(String.valueOf(paid));
        if (aucStatCanceled != null) aucStatCanceled.setText(String.valueOf(canceled));
    }

    /**
     * Bóc tách phân mảnh dữ liệu danh sách phiên đấu giá tương ứng chỉ mục hiện hành phục vụ kết xuất giao diện bảng.
     */
    private void renderAucPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        int from = aucCurrentPage * AUC_PAGE_SIZE;
        int to   = Math.min(from + AUC_PAGE_SIZE, allAuctionsList.size());

        auctionsList.clear();
        auctionsList.addAll(allAuctionsList.subList(from, to));

        if (aucLblPageInfo != null)
            aucLblPageInfo.setText("Trang " + (aucCurrentPage + 1) + " / " + totalPages
                    + "  (" + allAuctionsList.size() + " phiên)");

        if (aucBtnFirst != null) aucBtnFirst.setDisable(aucCurrentPage == 0);
        if (aucBtnPrev  != null) aucBtnPrev.setDisable(aucCurrentPage == 0);
        if (aucBtnNext  != null) aucBtnNext.setDisable(aucCurrentPage >= totalPages - 1);
        if (aucBtnLast  != null) aucBtnLast.setDisable(aucCurrentPage >= totalPages - 1);

        // Sinh cấu trúc các nút bấm số trang tương tác động cho danh mục quản lý phiên đấu giá
        if (aucPageButtonBox != null) {
            aucPageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, aucCurrentPage - maxBtn / 2);
            int endP   = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                javafx.scene.control.Button btn = new javafx.scene.control.Button(String.valueOf(p + 1));
                btn.getStyleClass().setAll("button", p == aucCurrentPage ? "page-btn-active" : "page-btn-normal");
                btn.setOnAction(e -> { aucCurrentPage = pg; renderAucPage(); });
                aucPageButtonBox.getChildren().add(btn);
            }
        }
    }

    /**
     * Điều hướng về trang đầu tiên của bảng danh sách phiên đấu giá.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void aucGoFirstPage(javafx.event.ActionEvent e) { aucCurrentPage = 0; renderAucPage(); }

    /**
     * Lùi về trang trước của bảng danh sách phiên đấu giá.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void aucGoPrevPage(javafx.event.ActionEvent e) {
        if (aucCurrentPage > 0) { aucCurrentPage--; renderAucPage(); }
    }

    /**
     * Tiến tới trang tiếp theo của bảng danh sách phiên đấu giá.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void aucGoNextPage(javafx.event.ActionEvent e) {
        int totalPages = (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE);
        if (aucCurrentPage < totalPages - 1) { aucCurrentPage++; renderAucPage(); }
    }

    /**
     * Nhảy thẳng tới trang cuối cùng của bảng danh sách phiên đấu giá.
     * @param e Sự kiện Action được gửi từ UI
     */
    @FXML public void aucGoLastPage(javafx.event.ActionEvent e) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        aucCurrentPage = totalPages - 1;
        renderAucPage();
    }

    /**
     * Tiếp nhận lệnh xử lý khóa tài khoản người dùng từ thanh công cụ.
     * Quét tập hợp các tài khoản được chọn, gửi tuần tự lệnh LOCK_USER lên Server luồng ngầm và làm mới cấu trúc bảng dữ liệu.
     *
     * @param event Sự kiện Action nhấn nút từ giao diện UI
     */
    @FXML
    void handleLockUser(ActionEvent event) {
        java.util.List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để khóa!");
            return;
        }

        ClientSocketManager.getInstance().execute(() -> {
            try {
                boolean allSuccess = true;
                String firstError = null;
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.LOCK_USER, selected.getId());
                    Response res = ClientSocketManager.getInstance().sendRequest(request);
                    if (res == null || res.getStatus() != ResponseStatus.SUCCESS) {
                        allSuccess = false;
                        if (res != null && res.getMessage() != null) {
                            firstError = res.getMessage();
                        }
                    }
                }
                final boolean success = allSuccess;
                final String errorMsg = firstError;
                Platform.runLater(() -> {
                    if (success) {
                        AlertUtils.showInfo("Thành công", "Đã khóa " + selectedUsers.size() + " người dùng được chọn.");
                    } else {
                        AlertUtils.showError("Lỗi", errorMsg != null ? errorMsg : "Một số hoặc toàn bộ yêu cầu khóa tài khoản thất bại.");
                    }
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi kết nối khi truyền chỉ thị khóa tài khoản người dùng", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Tiếp nhận lệnh xử lý mở khóa tài khoản người dùng từ thanh công cụ ngoại vi.
     * Gửi tuần tự các gói tin yêu cầu mạng UNLOCK_USER và trả kết quả phản hồi hiển thị lên lớp giao diện.
     *
     * @param event Sự kiện Action nhấn nút từ giao diện UI
     */
    @FXML
    void handleUnlockUser(ActionEvent event) {
        java.util.List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            AlertUtils.showWarning("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để mở khóa!");
            return;
        }

        ClientSocketManager.getInstance().execute(() -> {
            try {
                boolean allSuccess = true;
                String firstError = null;
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.UNLOCK_USER, selected.getId());
                    Response res = ClientSocketManager.getInstance().sendRequest(request);
                    if (res == null || res.getStatus() != ResponseStatus.SUCCESS) {
                        allSuccess = false;
                        if (res != null && res.getMessage() != null) {
                            firstError = res.getMessage();
                        }
                    }
                }
                final boolean success = allSuccess;
                final String errorMsg = firstError;
                Platform.runLater(() -> {
                    if (success) {
                        AlertUtils.showInfo("Thành công", "Đã mở khóa " + selectedUsers.size() + " người dùng được chọn.");
                    } else {
                        AlertUtils.showError("Lỗi", errorMsg != null ? errorMsg : "Một số hoặc toàn bộ yêu cầu mở khóa tài khoản thất bại.");
                    }
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                LOGGER.error("Gặp lỗi kết nối khi truyền chỉ thị mở khóa tài khoản người dùng", e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Phương thức phân tích trích xuất danh sách đối tượng người dùng đang được nhắm tới để thực thi xử lý nghiệp vụ.
     * Chiến lược quét ưu tiên:
     * 1. Thu thập toàn bộ các bản ghi mang giá trị Checkbox = TRUE lưu tại selectedUsersMap.
     * 2. Nếu tập hợp trống, tiến hành lấy thực thể dòng đơn lẻ đang được nhấp chuột bôi đậm highlight tại TableView.
     *
     * @return {@link java.util.List} Danh sách các đối tượng UserResponseDTO được chọn.
     */
    private java.util.List<UserResponseDTO> getSelectedUsers() {
        java.util.List<UserResponseDTO> list = new java.util.ArrayList<>();

        // 1. Quét tìm kiếm dữ liệu theo trạng thái cờ Checkbox trên toàn bộ danh sách nguồn lưu trong RAM
        for (UserResponseDTO user : allUsersList) {
            if (selectedUsersMap.containsKey(user.getId()) && selectedUsersMap.get(user.getId()).get()) {
                list.add(user);
            }
        }

        // 2. Kế hoạch dự phòng: Trích xuất bản ghi dòng đơn lẻ đang được click chọn trực tiếp trên bảng
        if (list.isEmpty()) {
            UserResponseDTO selectedRow = tableUsers.getSelectionModel().getSelectedItem();
            if (selectedRow != null) {
                list.add(selectedRow);
            }
        }
        return list;
    }

    /**
     * Tiếp nhận hành động xử lý cưỡng chế dừng khẩn cấp phiên đấu giá chỉ định từ thanh công cụ.
     *
     * @param event Sự kiện kích hoạt Action từ giao diện UI
     */
    @FXML
    void handleForceClose(ActionEvent event) {
        AuctionSummaryDTO selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            AlertUtils.showWarning("Lỗi", "Vui lòng chọn một phiên đấu giá để đóng!");
            return;
        }
        forceCloseAuction(selected.getAuctionId());
    }

    /**
     * Đóng gói mã yêu cầu mạng CLOSE_AUCTION và đẩy dữ liệu lên máy chủ để cưỡng chế đóng phiên đấu giá.
     *
     * @param auctionId Mã định danh duy nhất của phiên đấu giá mục tiêu cần can thiệp đóng
     */
    private void forceCloseAuction(String auctionId) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.CLOSE_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đã đóng phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        AlertUtils.showError("Lỗi", (response != null && response.getMessage() != null) ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi cưỡng chế đóng phiên đấu giá: {}", auctionId, e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Đóng gói mã chỉ thị xử lý ADMIN_CANCEL_AUCTION đẩy dữ liệu lên máy chủ từ xa nhằm hủy bỏ hoàn toàn phiên đấu giá mục tiêu.
     *
     * @param auctionId Mã định danh duy nhất của phiên đấu giá cần can thiệp hủy bỏ
     */
    private void cancelAuctionAdmin(String auctionId) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.ADMIN_CANCEL_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đã hủy phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        AlertUtils.showError("Lỗi", (response != null && response.getMessage() != null) ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi admin yêu cầu hủy phiên đấu giá: {}", auctionId, e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }

    /**
     * Đóng gói yêu cầu mạng ADMIN_MARK_PAID gửi lên máy chủ để ghi nhận trạng thái hóa đơn phiên đấu giá đã hoàn tất tất toán tài chính.
     *
     * @param auctionId Mã định danh duy nhất của phiên đấu giá mục tiêu cần cập nhật hóa đơn
     */
    private void markPaidAdmin(String auctionId) {
        ClientSocketManager.getInstance().execute(() -> {
            try {
                Request request = new Request(RequestType.ADMIN_MARK_PAID, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                        AlertUtils.showInfo("Thành công", "Đã đánh dấu Đã thanh toán cho phiên: " + auctionId);
                        loadAuctions();
                    } else {
                        AlertUtils.showError("Lỗi", (response != null && response.getMessage() != null) ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                LOGGER.error("Gặp sự cố lỗi kết nối mạng khi admin yêu cầu đánh dấu tất toán phiên: {}", auctionId, e);
                Platform.runLater(() -> AlertUtils.showError("Lỗi kết nối", e.getMessage()));
            }
        });
    }
}