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
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h2>EditAuctionDialogController</h2>
 * <p>
 * Controller quản lý giao diện hộp thoại Pop-up tương tác (Modal Dialog) dùng để cập nhật thông tin phiên đấu giá công khai.
 * </p>
 *
 * <p><b>Các cơ chế nghiệp vụ tích hợp:</b></p>
 * <ul>
 *   <li><b>Layout động (Dynamic Form Panes):</b> Kiểm soát cấu trúc hiển thị biểu mẫu động, tự động bật/tắt các Container đồ họa mở rộng (VBox) theo phân loại mặt hàng (Điện tử, Nghệ thuật, Phương tiện).</li>
 *   <li><b>Đồng bộ dữ liệu nền:</b> Kích hoạt tiến trình Worker Thread kéo thông tin chi tiết phiên (Description) bổ sung luồng ngầm nhằm tối ưu trải nghiệm phản hồi.</li>
 *   <li><b>Xác thực và Đóng gói (Validation & Padding):</b> Kiểm tra tính toàn vẹn của dữ liệu trường bắt buộc (*), phân tích kiểu dữ liệu số an toàn, đóng gói gói tin DTO thích ứng theo danh mục sản phẩm và đồng bộ qua Socket.</li>
 * </ul>
 *
 * @since 1.0
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.model.dto.UpdateAuctionDTO
 */
public class EditAuctionDialogController {

    /**
     * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm lưu vết luồng IO kết nối, cô lập sự cố truyền tin mạng khi phát lệnh cập nhật phiên.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAuctionDialogController.class);

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - TRƯỜNG THÔNG TIN NỀN TẢNG (BASE FIELDS)
    // =========================================================================
    @FXML private TextField txtName;
    @FXML private ComboBox<String> cbCondition;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtDesc;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - PHÂN HỆ ĐỒ HỌA ĐIỆN TỬ (ELECTRONICS PANE)
    // =========================================================================
    @FXML private VBox paneElectronics;
    @FXML private TextField txtElecBrand;
    @FXML private TextField txtElecWarranty;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - PHÂN HỆ ĐỒ HỌA NGHỆ THUẬT (ART PANE)
    // =========================================================================
    @FXML private VBox paneArt;
    @FXML private TextField txtArtArtist;
    @FXML private TextField txtArtMaterial;
    @FXML private TextField txtArtYear;

    // =========================================================================
    // THÀNH PHẦN GIAO DIỆN FXML - PHÂN HỆ ĐỒ HỌA PHƯƠNG TIỆN (VEHICLE PANE)
    // =========================================================================
    @FXML private VBox paneVehicle;
    @FXML private TextField txtVehBrand;
    @FXML private TextField txtVehModel;
    @FXML private TextField txtVehYear;
    @FXML private TextField txtVehKm;

    // =========================================================================
    // THAM SỐ ĐIỀU HƯỚNG VÀ VÒNG ĐỜI DIALOG (DIALOG STATES)
    // =========================================================================
    /** Mã định danh duy nhất của phiên đấu giá cần chỉnh sửa. */
    private String auctionId;

    /** Định dạng chuỗi phân loại danh mục sản phẩm (ELECTRONICS, ART, VEHICLE). */
    private String itemType;

    /** Luồng hàm callback (Runnable) thực thi làm mới giao diện cha sau khi lưu dữ liệu thành công. */
    private Runnable onSuccessCallback;

    /** Thực thể Stage đại diện cho cửa sổ Pop-up hiện tại. */
    private Stage dialogStage;

    /**
     * Phương thức khởi tạo cấu hình dữ liệu và định hình cấu trúc giao diện Dialog ban đầu.
     * Ánh xạ các giá trị nền tảng lên Form, tự động tính toán kích hoạt vùng nhập liệu mở rộng tương thích
     * theo phân loại sản phẩm và phát lệnh kéo thông tin đồng bộ bổ sung từ Server.
     *
     * @param auctionId          Mã định danh duy nhất của phiên đấu giá
     * @param initialName        Tên sản phẩm hiện tại
     * @param initialPrice       Giá khởi điểm hiện tại
     * @param itemType           Danh mục phân loại mặt hàng sản phẩm
     * @param dialogStage        Thực thể cửa sổ Stage của Dialog popup
     * @param onSuccessCallback  Hàm callback kích hoạt khi xử lý thành công
     */
    public void initData(String auctionId, String initialName, double initialPrice, String itemType, Stage dialogStage, Runnable onSuccessCallback) {
        this.auctionId = auctionId;
        this.itemType = itemType;
        this.dialogStage = dialogStage;
        this.onSuccessCallback = onSuccessCallback;

        // Điền các trường thông số thuộc tính cơ bản lên Form nhập liệu
        txtName.setText(initialName);
        txtPrice.setText(String.format("%.0f", initialPrice));
        cbCondition.setValue("Như mới");

        // Điều phối cơ chế quản lý không gian hiển thị (Visible & Managed) cho các Container động dựa trên ItemType
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

        // Thực hiện lệnh gọi mạng bất đồng bộ bổ sung trường thông tin mô tả sản phẩm
        fetchAuctionDetail();
    }

    /**
     * Khởi chạy nhiệm vụ luồng ngầm (Worker Thread) truyền tải gói tin GET_AUCTION_DETAIL lên Server.
     * Trích xuất văn bản mô tả (Description) của phiên đấu giá và đẩy tiến trình làm mới text UI về an toàn luồng JavaFX.
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
     * Đón nhận hành động nhấn nút Lưu thay đổi (Save Click Action).
     * Xác thực tính toàn vẹn của dữ liệu đầu vào, ép kiểu định dạng số thực an toàn cho tài chính,
     * đóng gói tập hợp thuộc tính tĩnh và động chuyên biệt của mặt hàng vào UpdateAuctionDTO
     * và chuyển giao mạng không đồng bộ.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    void handleSave(ActionEvent event) {
        // Thực hiện kiểm tra ràng buộc trường nhập liệu bắt buộc trống trước khi xử lý
        if (txtName.getText().trim().isEmpty() || txtPrice.getText().trim().isEmpty()) {
            AlertUtils.showError("Lỗi", "Vui lòng nhập đủ các trường có dấu *");
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
            dto.setItemType(itemType); // Gán thông số danh mục bắt buộc cho DTO cấu trúc máy chủ

            // Đóng gói có chọn lọc các thông số kỹ thuật mở rộng tương thích với từng phân hệ
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

            // Giao tiếp truyền nhận gói tin IO luồng ngầm qua Socket kết nối mạng
            ClientSocketManager.getInstance().execute(() -> {
                try {
                    Request request = new Request(RequestType.UPDATE_AUCTION, dto);
                    Response response = ClientSocketManager.getInstance().sendRequest(request);

                    Platform.runLater(() -> {
                        if (response != null && response.getStatus() == ResponseStatus.SUCCESS) {
                            if (onSuccessCallback != null) onSuccessCallback.run(); // Thực thi hàm khôi phục làm mới View cha
                            dialogStage.close(); // Giải phóng cấu trúc cửa sổ Pop-up hiện hành
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
     * Đón nhận hành động đóng biểu mẫu hủy bỏ thao tác chỉnh sửa (Close Dialog Action).
     * Giải phóng hoàn toàn không gian hiển thị của Stage hiện hành mà không lưu lại thuộc tính.
     *
     * @param event Sự kiện Action gửi từ giao diện người dùng FXML
     */
    @FXML
    void handleClose(ActionEvent event) {
        dialogStage.close();
    }
}