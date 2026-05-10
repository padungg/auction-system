package com.auction.client.controller;

import com.auction.client.network.ClientSocketManager;
import com.auction.client.util.SessionManager;
import com.auction.model.dto.AuctionSummaryDTO;
import com.auction.model.dto.BidRequestDTO;
import com.auction.model.protocol.Request;
import com.auction.model.protocol.RequestType;
import com.auction.model.protocol.Response;
import com.auction.model.protocol.ResponseStatus;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class PaymentController {

    @FXML private Label lblItemName;
    @FXML private Label lblStatusBadge;
    @FXML private ImageView imgProduct;
    @FXML private Label lblSpec1;
    @FXML private Label lblSpec2;
    @FXML private Label lblSpec3;
    @FXML private Label lblDescription;

    @FXML private Label lblCurrentPrice;
    @FXML private Label lblStartPrice;
    @FXML private TextField txtBidInput;
    @FXML private Button btnBid;

    @FXML private TableView<Bid> tableBidHistory;
    @FXML private TableColumn<Bid, String> colBidUser;
    @FXML private TableColumn<Bid, String> colBidPrice;
    @FXML private TableColumn<Bid, String> colBidTime;

    private long currentPrice = 0;
    private String auctionId = "";
    private ObservableList<Bid> list = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        if (colBidUser != null) colBidUser.setCellValueFactory(new PropertyValueFactory<>("user"));
        if (colBidPrice != null) colBidPrice.setCellValueFactory(new PropertyValueFactory<>("price"));
        if (colBidTime != null) colBidTime.setCellValueFactory(new PropertyValueFactory<>("time"));
        if (tableBidHistory != null) tableBidHistory.setItems(list);

        if (btnBid != null) {
            btnBid.setOnAction(e -> handleBid());
        }

        registerRealTimeListener();
    }

    /**
     * Nhận dữ liệu từ Dashboard truyền sang
     */
    public void initData(AuctionSummaryDTO auction) {
        this.auctionId = auction.getAuctionId();
        this.currentPrice = (long) auction.getCurrentPrice();

        lblItemName.setText(auction.getItemName());
        lblStartPrice.setText("Giá khởi điểm: " + currentPrice + " đ");
        lblCurrentPrice.setText(currentPrice + " đ");
        lblStatusBadge.setText(auction.getStatus().equals("OPENING") ? "🟢 ĐANG DIỄN RA" : auction.getStatus());

        loadBidHistory();

        // Đăng ký nhận notify cho auction này
        subscribeToAuction();
    }

    private void loadBidHistory() {
        if (auctionId == null || auctionId.isEmpty()) return;
        new Thread(() -> {
            try {
                ClientSocketManager client = ClientSocketManager.getInstance();
                if (!client.isConnected()) return;

                Request request = new Request(RequestType.GET_BID_HISTORY, auctionId);
                Response response = client.sendRequest(request);

                if (response.getStatus() == ResponseStatus.SUCCESS) {
                    Gson gson = client.getGson();
                    // Server trả về List<BidResponseDTO>, nhưng tạm thời map sang Bid class
                    // Cần parse payload cẩn thận
                    // Tạm thời để trống hoặc dùng local mock nếu server chưa hỗ trợ chuẩn
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void subscribeToAuction() {
        new Thread(() -> {
            try {
                ClientSocketManager client = ClientSocketManager.getInstance();
                if (client.isConnected() && auctionId != null) {
                    client.sendRequest(new Request(RequestType.SUBSCRIBE_AUCTION, auctionId));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    @FXML
    private void handleBid() {
        if (txtBidInput == null || auctionId.isEmpty()) return;
        String input = txtBidInput.getText();
        if (input.isEmpty()) return;

        try {
            long newPrice = Long.parseLong(input);
            if (newPrice <= currentPrice) {
                showAlert("Lỗi", "Giá đặt phải lớn hơn giá hiện tại!");
                return;
            }

            new Thread(() -> {
                try {
                    ClientSocketManager client = ClientSocketManager.getInstance();
                    if (client.isConnected()) {
                        BidRequestDTO dto = new BidRequestDTO(auctionId, (double) newPrice);
                        Request request = new Request(RequestType.PLACE_BID, dto);
                        Response response = client.sendRequest(request);

                        Platform.runLater(() -> {
                            if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                                currentPrice = newPrice;
                                lblCurrentPrice.setText(currentPrice + " đ");
                                txtBidInput.clear();
                                // Note: Không add vào list ngay, chờ Server push BID_UPDATE
                            } else {
                                showAlert("Lỗi", response != null ? response.getMessage() : "Timeout");
                            }
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        } catch (NumberFormatException e) {
            showAlert("Lỗi", "Vui lòng nhập số hợp lệ");
        }
    }

    private void registerRealTimeListener() {
        ClientSocketManager client = ClientSocketManager.getInstance();
        client.setNotificationListener(push -> {
            Platform.runLater(() -> {
                try {
                    String event = push.has("event") ? push.get("event").getAsString() : "";
                    if ("BID_UPDATE".equals(event)) {
                        String pushAuctionId = push.get("auctionId").getAsString();
                        if (pushAuctionId.equals(this.auctionId)) {
                            long newPrice = push.get("newPrice").getAsLong();
                            String bidderId = push.has("bidderId") ? push.get("bidderId").getAsString() : "Khách";
                            String bidTime = push.has("bidTime") ? push.get("bidTime").getAsString() : "Vừa xong";

                            this.currentPrice = newPrice;
                            lblCurrentPrice.setText(newPrice + " đ");

                            list.add(0, new Bid(bidTime, bidderId, String.valueOf(newPrice)));
                        }
                    } else if ("AUCTION_CLOSED".equals(event)) {
                        String pushAuctionId = push.get("auctionId").getAsString();
                        if (pushAuctionId.equals(this.auctionId)) {
                            lblStatusBadge.setText("🔴 ĐÃ KẾT THÚC");
                            btnBid.setDisable(true);
                            txtBidInput.setDisable(true);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleDashboard(ActionEvent event) {
        // Rời khỏi chi tiết thì unsubscribe
        new Thread(() -> {
            try {
                ClientSocketManager client = ClientSocketManager.getInstance();
                if (client.isConnected() && auctionId != null && !auctionId.isEmpty()) {
                    client.sendRequest(new Request(RequestType.UNSUBSCRIBE_AUCTION, auctionId));
                    client.setNotificationListener(null);
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Danh sách đấu giá");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleViewDetail(ActionEvent event) {}

    public static class Bid {
        private String time;
        private String user;
        private String price;

        public Bid(String time, String user, String price) {
            this.time = time;
            this.user = user;
            this.price = price;
        }

        public String getTime() { return time; }
        public String getUser() { return user; }
        public String getPrice() { return price; }
    }
}