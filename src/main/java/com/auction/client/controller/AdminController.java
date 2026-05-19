package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.UserResponseDTO;
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
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Controller điều phối phân hệ quản trị hệ thống (Admin Dashboard).
 * Quản lý vòng đời dữ liệu tài khoản người dùng, giám sát các phiên đấu giá, điều khiển phân trang UI và kết xuất số liệu báo cáo.
 */
public class AdminController {

    /**
     * Khởi tạo thành phần Logger theo tiêu chuẩn an toàn đa luồng.
     * Hệ thống ghi log này giúp theo dõi dòng chảy dữ liệu, phân loại mức độ cảnh báo (INFO, SEVERE)
     * và hỗ trợ lưu vết lịch sử vận hành vào tệp tin để phục vụ công tác bảo trì hệ thống từ xa.
     */
    private static final Logger logger = Logger.getLogger(AdminController.class.getName());

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

    // Nhãn hiển thị chỉ số đo lường định lượng của tài khoản người dùng
    @FXML private javafx.scene.control.Label statTotalUsers;
    @FXML private javafx.scene.control.Label statActiveUsers;
    @FXML private javafx.scene.control.Label statLockedUsers;
    @FXML private javafx.scene.control.Label statAdminUsers;

    // Bộ điều khiển luồng phân trang lớp giao diện người dùng
    @FXML private javafx.scene.layout.HBox pageButtonBox;
    @FXML private javafx.scene.control.Label lblPageInfo;
    @FXML private javafx.scene.control.Button btnFirstPage;
    @FXML private javafx.scene.control.Button btnPrevPage;
    @FXML private javafx.scene.control.Button btnNextPage;
    @FXML private javafx.scene.control.Button btnLastPage;

    // Cấu hình tham số và bộ nhớ đệm phân trang tài khoản
    private static final int PAGE_SIZE = 15;
    private int currentPage = 0; // Chỉ mục trang hiện tại dựa trên gốc 0 (0-indexed)
    private java.util.List<UserResponseDTO> allUsersList = new java.util.ArrayList<>();

    private ObservableList<UserResponseDTO> usersList = FXCollections.observableArrayList();
    // Cấu trúc ánh xạ để duy trì trạng thái lưu trữ của cột Checkbox độc lập với chu kỳ nạp dữ liệu
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

    // Nhãn hiển thị chỉ số đo lường định lượng trạng thái các phiên đấu giá
    @FXML private javafx.scene.control.Label aucStatTotal;
    @FXML private javafx.scene.control.Label aucStatRunning;
    @FXML private javafx.scene.control.Label aucStatOpen;
    @FXML private javafx.scene.control.Label aucStatFinished;
    @FXML private javafx.scene.control.Label aucStatPaid;
    @FXML private javafx.scene.control.Label aucStatCanceled;

    // Bộ điều khiển luồng phân trang lớp giao diện danh sách đấu giá
    @FXML private javafx.scene.layout.HBox aucPageButtonBox;
    @FXML private javafx.scene.control.Label aucLblPageInfo;
    @FXML private javafx.scene.control.Button aucBtnFirst;
    @FXML private javafx.scene.control.Button aucBtnPrev;
    @FXML private javafx.scene.control.Button aucBtnNext;
    @FXML private javafx.scene.control.Button aucBtnLast;

    // Cấu hình tham số và bộ nhớ đệm phân trang phiên đấu giá
    private static final int AUC_PAGE_SIZE = 5;
    private int aucCurrentPage = 0;
    private java.util.List<AuctionSummaryDTO> allAuctionsList = new java.util.ArrayList<>();

    private ObservableList<AuctionSummaryDTO> auctionsList = FXCollections.observableArrayList();

    /**
     * Phương thức khởi tạo cấu hình vòng đời của phân hệ JavaFX (Lifecycle Hook).
     * Thiết lập trạng thái kết nối layout của các cấu trúc bảng và kích hoạt tiến trình tải thông tin ban đầu.
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
     * Thiết lập cấu trúc dữ liệu thuộc tính (Property Binding) và logic render tùy biến giao diện cho bảng Người dùng.
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

        // Custom hiển thị nhãn phân quyền theo dạng Badge màu trực quan
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("role"));
        colUserRole.setCellFactory(tc -> new TableCell<UserResponseDTO, Object>() {
            @Override
            protected void updateItem(Object roleObj, boolean empty) {
                super.updateItem(roleObj, empty);
                if (empty || roleObj == null) {
                    setGraphic(null);
                } else {
                    String role = roleObj.toString();
                    Label lbl = new Label(role);
                    if ("ADMIN".equalsIgnoreCase(role)) {
                        lbl.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #7e22ce; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #047857; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    }
                    setGraphic(lbl);
                }
            }
        });

        // Chuẩn hóa định dạng hiển thị tiền tệ quốc gia (VND) cho thông tin số dư tài khoản
        colUserBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
        colUserBalance.setCellFactory(tc -> new TableCell<UserResponseDTO, Double>() {
            @Override
            protected void updateItem(Double balance, boolean empty) {
                super.updateItem(balance, empty);
                if (empty || balance == null) setText(null);
                else setText(String.format("%,.0fđ", balance));
            }
        });

        // Tự động phân cấp giao diện dựa trên trạng thái bảo mật của tài khoản người dùng
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("active"));
        colUserStatus.setCellFactory(tc -> new TableCell<UserResponseDTO, Boolean>() {
            @Override
            protected void updateItem(Boolean isActive, boolean empty) {
                super.updateItem(isActive, empty);
                if (empty || isActive == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label lbl = new Label(isActive ? "✅ Hoạt động" : "🔒 Đã khóa");
                    if (isActive) {
                        lbl.setStyle("-fx-text-fill: #059669; -fx-font-weight: bold;");
                    } else {
                        lbl.setStyle("-fx-text-fill: #d97706; -fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                    setText(null);
                }
            }
        });

        tableUsers.setItems(usersList);
        tableUsers.setEditable(true); // Kích hoạt quyền chỉnh sửa để CheckBoxTableCell ghi nhận trực tiếp tương tác click chuột
    }

    /**
     * Xây dựng cấu trúc Cell đồ họa phức hợp lồng nhau cho cột thông tin sản phẩm và gán các bộ lắng nghe sự kiện nghiệp vụ.
     */
    private void setupAuctionsTable() {
        colAucProduct.setCellValueFactory(new PropertyValueFactory<>("itemName"));
        colAucProduct.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String itemName, boolean empty) {
                super.updateItem(itemName, empty);
                if (empty || itemName == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    AuctionSummaryDTO dto = getTableView().getItems().get(getIndex());
                    javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4);

                    Label nameLbl = new Label(itemName);
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
                    // Không áp dụng thuộc tính wrapText nhằm bảo vệ kiến trúc layout phân dòng không bị xô lệch chiều cao mặc định

                    javafx.scene.layout.HBox infoBox = new javafx.scene.layout.HBox(6);
                    Label idLbl = new Label("#" + dto.getAuctionId());
                    idLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

                    Label typeLbl = new Label(dto.getItemType());
                    if ("ELECTRONICS".equalsIgnoreCase(dto.getItemType())) {
                        typeLbl.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #7e22ce; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 9px;");
                    } else if ("ART".equalsIgnoreCase(dto.getItemType())) {
                        typeLbl.setStyle("-fx-background-color: #fce7f3; -fx-text-fill: #be185d; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 9px;");
                    } else if ("VEHICLE".equalsIgnoreCase(dto.getItemType())) {
                        typeLbl.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #047857; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 9px;");
                    } else {
                        typeLbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-weight: bold; -fx-font-size: 9px;");
                    }

                    infoBox.getChildren().addAll(typeLbl, idLbl);
                    box.getChildren().addAll(nameLbl, infoBox);
                    setGraphic(box);
                    setText(null);
                }
            }
        });

        colAucPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colAucPrice.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                    setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
                }
            }
        });

        colAucBids.setCellValueFactory(new PropertyValueFactory<>("bidCount"));
        colAucBids.setStyle("-fx-alignment: CENTER;");

        // Thuật toán giả lập phân bổ tỷ lệ lượt xem căn cứ theo khối lượng đặt giá thực tế
        colAucViews.setCellValueFactory(cellData -> {
            int bids = cellData.getValue().getBidCount();
            int views = bids == 0 ? new java.util.Random().nextInt(5) : bids * 3 + new java.util.Random().nextInt(15);
            return new javafx.beans.property.SimpleIntegerProperty(views).asObject();
        });
        colAucViews.setStyle("-fx-alignment: CENTER; -fx-text-fill: #64748b;");

        colAucWinner.setCellValueFactory(new PropertyValueFactory<>("currentWinnerId"));
        colAucWinner.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String winner, boolean empty) {
                super.updateItem(winner, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else if (winner == null || winner.isEmpty()) {
                    setText("undefined");
                    setStyle("-fx-text-fill: #64748b;");
                } else {
                    setText(winner);
                    setStyle("-fx-text-fill: #1e293b;");
                }
            }
        });

        colAucSeller.setCellValueFactory(new PropertyValueFactory<>("sellerName"));
        colAucSeller.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String seller, boolean empty) {
                super.updateItem(seller, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else if (seller == null || seller.isEmpty()) {
                    setText("Không rõ");
                    setStyle("-fx-text-fill: #64748b; -fx-font-style: italic;");
                } else {
                    setText(seller);
                    setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                }
            }
        });

        // Phân tách trường thông tin Ngày và Giờ xuống dòng nhằm tối ưu hóa diện tích hiển thị của cột bảng
        colAucEnd.setCellValueFactory(new PropertyValueFactory<>("endTime"));
        colAucEnd.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String end, boolean empty) {
                super.updateItem(end, empty);
                if (empty || end == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String[] parts = end.split(" ");
                    if (parts.length == 2) {
                        setText(parts[1] + "\n" + parts[0]);
                    } else {
                        setText(end);
                    }
                    setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                }
            }
        });

        colAucStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colAucStatus.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) setGraphic(null);
                else {
                    Label lbl = new Label(status);
                    if ("RUNNING".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if ("OPEN".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-background-color: #ffe4e6; -fx-text-fill: #e11d48; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if ("FINISHED".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #64748b; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if ("PAID".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-background-color: #d1fae5; -fx-text-fill: #059669; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else if ("CANCELED".equalsIgnoreCase(status)) {
                        lbl.setStyle("-fx-background-color: #f3e8ff; -fx-text-fill: #9333ea; -fx-padding: 4 12; -fx-background-radius: 12; -fx-font-weight: bold; -fx-font-size: 11px;");
                    } else {
                        lbl.setStyle("-fx-font-weight: bold;");
                    }
                    setGraphic(lbl);
                }
            }
        });

        // Thiết lập bộ chứa các cấu trúc nút bấm chức năng điều khiển thay đổi linh hoạt theo từng trạng thái phiên
        if (colAucAction != null) {
            colAucAction.setCellFactory(tc -> new TableCell<AuctionSummaryDTO, Void>() {
                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        AuctionSummaryDTO row = getTableView().getItems().get(getIndex());
                        javafx.scene.layout.HBox box = new javafx.scene.layout.HBox(6);
                        box.setAlignment(javafx.geometry.Pos.CENTER);

                        Button btnView = new Button("👁 Xem");
                        btnView.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #334155; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                        btnView.setOnAction(e -> MainController.getInstance().openAuctionDetail(row.getAuctionId()));

                        String status = row.getStatus();
                        if ("RUNNING".equalsIgnoreCase(status) || "OPEN".equalsIgnoreCase(status)) {
                            Button btnClose = new Button("Đóng");
                            btnClose.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                            btnClose.setOnAction(e -> forceCloseAuction(row.getAuctionId()));

                            Button btnCancel = new Button("Cancel");
                            btnCancel.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                            btnCancel.setOnAction(e -> cancelAuctionAdmin(row.getAuctionId()));

                            box.getChildren().addAll(btnView, btnClose, btnCancel);
                        } else if ("FINISHED".equalsIgnoreCase(status)) {
                            Button btnPaid = new Button("Paid");
                            btnPaid.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                            btnPaid.setOnAction(e -> markPaidAdmin(row.getAuctionId()));

                            Button btnCancel = new Button("Cancel");
                            btnCancel.setStyle("-fx-background-color: white; -fx-text-fill: #475569; -fx-border-color: #cbd5e1; -fx-border-radius: 6; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 4 8;");
                            btnCancel.setOnAction(e -> cancelAuctionAdmin(row.getAuctionId()));

                            box.getChildren().addAll(btnView, btnPaid, btnCancel);
                        } else {
                            box.getChildren().add(btnView);
                        }

                        setGraphic(box);
                    }
                }
            });
        }

        // Khống chế cao dòng cố định để ngăn chặn hiện tượng phá vỡ tính đồng đều của bố cục lưới TableView
        tableAuctions.setFixedCellSize(60);
        tableAuctions.setItems(auctionsList);
    }

    /**
     * Đồng bộ danh sách thực thể người dùng từ Server bằng việc khởi chạy một Worker Thread chạy nền tách biệt.
     */
    private void loadUsers() {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_USERS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    UserResponseDTO[] arr = gson.fromJson(gson.toJson(response.getPayload()), UserResponseDTO[].class);
                    Platform.runLater(() -> {
                        allUsersList.clear();
                        if (arr != null) java.util.Collections.addAll(allUsersList, arr);
                        currentPage = 0;
                        updateStats();
                        renderPage();
                    });
                }
            } catch (Exception e) {
                // Đăng ký thông tin lỗi ngoại lệ I/O kết nối vào Logger hệ thống với mức độ cảnh báo SEVERE
                logger.log(Level.SEVERE, "Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_USERS: ", e);
            }
        }).start();
    }

    /**
     * Phân tích và tổng hợp số lượng phân loại tài khoản người dùng dựa trên tập dữ liệu nguồn lưu trong RAM.
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
     * Phân mảnh dữ liệu danh sách người dùng hiển thị cho trang hiện tại và tái định hình chuỗi nút số trang động.
     */
    private void renderPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, allUsersList.size());

        usersList.clear();
        selectedUsersMap.clear();
        usersList.addAll(allUsersList.subList(from, to));

        // Chuẩn hóa văn bản hiển thị thông tin trang hiện hành
        if (lblPageInfo != null)
            lblPageInfo.setText("Trang " + (currentPage + 1) + " / " + totalPages
                    + "  (" + allUsersList.size() + " người dùng)");

        // Đồng bộ trạng thái vô hiệu hóa (Disabled State) của tập hợp nút bấm chuyển trang nhanh đầu/cuối
        if (btnFirstPage != null) btnFirstPage.setDisable(currentPage == 0);
        if (btnPrevPage != null)  btnPrevPage.setDisable(currentPage == 0);
        if (btnNextPage != null)  btnNextPage.setDisable(currentPage >= totalPages - 1);
        if (btnLastPage != null)  btnLastPage.setDisable(currentPage >= totalPages - 1);

        // Khởi tạo các cấu trúc nút số trang tương tác (Giới hạn vùng hiển thị đồng thời tối đa 5 nút số trang)
        if (pageButtonBox != null) {
            pageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, currentPage - maxBtn / 2);
            int endP = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                javafx.scene.control.Button btn = new javafx.scene.control.Button(String.valueOf(p + 1));
                if (p == currentPage) {
                    btn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 32; -fx-pref-width: 32;");
                } else {
                    btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 32; -fx-pref-width: 32;");
                }
                btn.setOnAction(e -> { currentPage = pg; renderPage(); });
                pageButtonBox.getChildren().add(btn);
            }
        }
    }

    // Nhóm các phương thức tiếp nhận sự kiện kích hoạt nút phân trang người dùng
    @FXML public void goFirstPage(javafx.event.ActionEvent e) { currentPage = 0; renderPage(); }

    @FXML public void goPrevPage(javafx.event.ActionEvent e) {
        if (currentPage > 0) { currentPage--; renderPage(); }
    }

    @FXML public void goNextPage(javafx.event.ActionEvent e) {
        int totalPages = (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE);
        if (currentPage < totalPages - 1) { currentPage++; renderPage(); }
    }

    @FXML public void goLastPage(javafx.event.ActionEvent e) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allUsersList.size() / PAGE_SIZE));
        currentPage = totalPages - 1;
        renderPage();
    }

    /**
     * Kích hoạt tiến trình kết nối mạng bất đồng bộ nhằm đồng bộ toàn bộ lịch sử bản ghi phiên đấu giá từ máy chủ.
     */
    private void loadAuctions() {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.GET_ALL_AUCTIONS, null);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                if (response.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = ClientSocketManager.getInstance().getGson();
                    AuctionSummaryDTO[] arr = gson.fromJson(gson.toJson(response.getPayload()), AuctionSummaryDTO[].class);
                    Platform.runLater(() -> {
                        allAuctionsList.clear();
                        if (arr != null) java.util.Collections.addAll(allAuctionsList, arr);
                        aucCurrentPage = 0;
                        updateAucStats();
                        renderAucPage();
                    });
                }
            } catch (Exception e) {
                // Đăng ký thông tin ngoại lệ hệ thống mạng kết nối vào Logger hệ thống với mức độ cảnh báo SEVERE
                logger.log(Level.SEVERE, "Gặp sự cố lỗi mạng khi thực thi yêu cầu GET_ALL_AUCTIONS: ", e);
            }
        }).start();
    }

    /**
     * Tập hợp dữ liệu từ danh sách bộ nhớ đệm và kết xuất các chỉ số định lượng trạng thái phiên đấu giá lên UI labels.
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
     * Thực hiện việc bóc tách danh sách và cập nhật hiển thị dữ liệu đấu giá cho trang hiện tại.
     */
    private void renderAucPage() {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        int from = aucCurrentPage * AUC_PAGE_SIZE;
        int to   = Math.min(from + AUC_PAGE_SIZE, allAuctionsList.size());

        auctionsList.clear();
        auctionsList.addAll(allAuctionsList.subList(from, to));

        // Chuẩn hóa văn bản hiển thị thông tin trang hiện hành của bảng đấu giá
        if (aucLblPageInfo != null)
            aucLblPageInfo.setText("Trang " + (aucCurrentPage + 1) + " / " + totalPages
                    + "  (" + allAuctionsList.size() + " phiên)");

        if (aucBtnFirst != null) aucBtnFirst.setDisable(aucCurrentPage == 0);
        if (aucBtnPrev  != null) aucBtnPrev.setDisable(aucCurrentPage == 0);
        if (aucBtnNext  != null) aucBtnNext.setDisable(aucCurrentPage >= totalPages - 1);
        if (aucBtnLast  != null) aucBtnLast.setDisable(aucCurrentPage >= totalPages - 1);

        if (aucPageButtonBox != null) {
            aucPageButtonBox.getChildren().clear();
            int maxBtn = 5;
            int startP = Math.max(0, aucCurrentPage - maxBtn / 2);
            int endP   = Math.min(totalPages, startP + maxBtn);
            if (endP - startP < maxBtn) startP = Math.max(0, endP - maxBtn);
            for (int p = startP; p < endP; p++) {
                final int pg = p;
                javafx.scene.control.Button btn = new javafx.scene.control.Button(String.valueOf(p + 1));
                if (p == aucCurrentPage) {
                    btn.setStyle("-fx-background-color: #2563eb; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 32; -fx-pref-width: 32;");
                } else {
                    btn.setStyle("-fx-background-color: #f1f5f9; -fx-text-fill: #475569; -fx-background-radius: 8; -fx-cursor: hand; -fx-min-width: 32; -fx-pref-width: 32;");
                }
                btn.setOnAction(e -> { aucCurrentPage = pg; renderAucPage(); });
                aucPageButtonBox.getChildren().add(btn);
            }
        }
    }

    // Nhóm các phương thức tiếp nhận sự kiện kích hoạt nút phân trang phiên đấu giá
    @FXML public void aucGoFirstPage(javafx.event.ActionEvent e) { aucCurrentPage = 0; renderAucPage(); }

    @FXML public void aucGoPrevPage(javafx.event.ActionEvent e) {
        if (aucCurrentPage > 0) { aucCurrentPage--; renderAucPage(); }
    }

    @FXML public void aucGoNextPage(javafx.event.ActionEvent e) {
        int totalPages = (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE);
        if (aucCurrentPage < totalPages - 1) { aucCurrentPage++; renderAucPage(); }
    }

    @FXML public void aucGoLastPage(javafx.event.ActionEvent e) {
        int totalPages = Math.max(1, (int) Math.ceil((double) allAuctionsList.size() / AUC_PAGE_SIZE));
        aucCurrentPage = totalPages - 1;
        renderAucPage();
    }

    /**
     * Đón nhận hành động yêu cầu Khóa tài khoản người dùng từ giao diện điều khiển.
     */
    @FXML
    void handleLockUser(ActionEvent event) {
        java.util.List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            showAlert("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để khóa!");
            return;
        }

        new Thread(() -> {
            try {
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.LOCK_USER, selected.getId());
                    ClientSocketManager.getInstance().sendRequest(request);
                }
                Platform.runLater(() -> {
                    showAlert("Thành công", "Đã khóa " + selectedUsers.size() + " người dùng được chọn.");
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                // Đăng ký chi tiết ngoại lệ I/O truyền tải mạng vào tập tin Log giám sát hệ thống
                logger.log(Level.SEVERE, "Gặp lỗi kết nối khi truyền chỉ thị khóa tài khoản người dùng: ", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Đón nhận hành động yêu cầu Mở khóa tài khoản người dùng từ giao diện điều khiển.
     */
    @FXML
    void handleUnlockUser(ActionEvent event) {
        java.util.List<UserResponseDTO> selectedUsers = getSelectedUsers();
        if (selectedUsers.isEmpty()) {
            showAlert("Lỗi", "Vui lòng tick chọn ít nhất một người dùng để mở khóa!");
            return;
        }

        new Thread(() -> {
            try {
                for (UserResponseDTO selected : selectedUsers) {
                    Request request = new Request(RequestType.UNLOCK_USER, selected.getId());
                    ClientSocketManager.getInstance().sendRequest(request);
                }
                Platform.runLater(() -> {
                    showAlert("Thành công", "Đã mở khóa " + selectedUsers.size() + " người dùng được chọn.");
                    selectedUsersMap.clear();
                    loadUsers();
                });
            } catch (Exception e) {
                // Đăng ký chi tiết ngoại lệ I/O truyền tải mạng vào tập tin Log giám sát hệ thống
                logger.log(Level.SEVERE, "Gặp lỗi kết nối khi truyền chỉ thị mở khóa tài khoản người dùng: ", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Phân tích giải quyết trích xuất danh sách đối tượng người dùng đang được nhắm tới để xử lý tác vụ.
     * Thứ tự chiến lược giải quyết ưu tiên:
     * 1. Thu thập toàn bộ các bản ghi mang giá trị kích hoạt hộp kiểm (Checkbox = TRUE) lưu tại selectedUsersMap.
     * 2. Nếu tập hợp trống, dự phòng lấy ra thực thể dòng đơn lẻ đang được click chuột bôi đậm trên TableView.
     */
    private java.util.List<UserResponseDTO> getSelectedUsers() {
        java.util.List<UserResponseDTO> list = new java.util.ArrayList<>();
        // 1. Quét tìm kiếm dữ liệu theo cờ Checkbox trên toàn bộ danh sách nguồn allUsersList
        for (UserResponseDTO user : allUsersList) {
            if (selectedUsersMap.containsKey(user.getId()) && selectedUsersMap.get(user.getId()).get()) {
                list.add(user);
            }
        }
        // 2. Thuật toán dự phòng: Trích xuất bản ghi dòng đang tập trung click chuột highlight
        if (list.isEmpty()) {
            UserResponseDTO selectedRow = tableUsers.getSelectionModel().getSelectedItem();
            if (selectedRow != null) {
                list.add(selectedRow);
            }
        }
        return list;
    }

    /**
     * Tiếp nhận sự kiện xử lý Cưỡng chế đóng phiên đấu giá chỉ định được chọn từ thanh công cụ ngoại vi.
     */
    @FXML
    void handleForceClose(ActionEvent event) {
        AuctionSummaryDTO selected = tableAuctions.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert("Lỗi", "Vui lòng chọn một phiên đấu giá để đóng!");
            return;
        }
        forceCloseAuction(selected.getAuctionId());
    }

    /**
     * Đóng gói request CLOSE_AUCTION và truyền dữ liệu chỉ thị xử lý trực tiếp lên phía máy chủ từ xa.
     */
    private void forceCloseAuction(String auctionId) {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.CLOSE_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response.getStatus() == ResponseStatus.SUCCESS) {
                        showAlert("Thành công", "Đã đóng phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                // Lưu vết sự cố I/O mạng nghiêm trọng phục vụ công tác rà soát lỗi crash hệ thống mạng từ xa
                logger.log(Level.SEVERE, "Gặp sự cố lỗi kết nối mạng khi cưỡng chế đóng phiên đấu giá: ", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Đóng gói mã chỉ thị xử lý hủy bỏ hoàn toàn phiên đấu giá cụ thể lên phía máy chủ Server.
     */
    private void cancelAuctionAdmin(String auctionId) {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.ADMIN_CANCEL_AUCTION, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response.getStatus() == ResponseStatus.SUCCESS) {
                        showAlert("Thành công", "Đã hủy phiên đấu giá: " + auctionId);
                        loadAuctions();
                    } else {
                        showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                // Đăng ký ngoại lệ xử lý lỗi I/O Socket mạng vào bộ ghi log tệp tin tập trung
                logger.log(Level.SEVERE, "Gặp sự cố lỗi kết nối mạng khi admin yêu cầu hủy phiên đấu giá: ", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Gửi yêu cầu cập nhật ghi nhận trạng thái hóa đơn phiên đấu giá đã hoàn tất tất toán thanh toán (Paid) lên Server.
     */
    private void markPaidAdmin(String auctionId) {
        new Thread(() -> {
            try {
                Request request = new Request(RequestType.ADMIN_MARK_PAID, auctionId);
                Response response = ClientSocketManager.getInstance().sendRequest(request);

                Platform.runLater(() -> {
                    if (response.getStatus() == ResponseStatus.SUCCESS) {
                        showAlert("Thành công", "Đã đánh dấu Paid cho phiên: " + auctionId);
                        loadAuctions();
                    } else {
                        showAlert("Lỗi", response.getMessage() != null ? response.getMessage() : "Lỗi không xác định");
                    }
                });
            } catch (Exception e) {
                // Đăng ký ngoại lệ xử lý lỗi tất toán thông tin mạng vào bộ ghi log tệp tin tập trung
                logger.log(Level.SEVERE, "Gặp sự cố lỗi kết nối mạng khi admin yêu cầu đánh dấu tất toán phiên: ", e);
                Platform.runLater(() -> showAlert("Lỗi kết nối", e.getMessage()));
            }
        }).start();
    }

    /**
     * Khởi tạo cấu trúc và kích hoạt hiển thị hộp thoại thông báo cảnh báo (Modal Alert Dialog) dạng WARNING lên lớp giao diện.
     */
    private void showAlert(String title, String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}