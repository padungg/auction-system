package com.auction.client.util;

import javafx.scene.control.Alert;

/**
 * <h2>AlertUtils</h2>
 * <p>
 * Lớp tiện ích tĩnh quản lý hiển thị các hộp thoại thông báo tập trung (Central Modal Alerts Utility) của ứng dụng Client.
 * </p>
 * * <p><b>Các nguyên lý kiến trúc áp dụng:</b></p>
 * <ul>
 * <li><b>Nguyên lý DRY (Don't Repeat Yourself):</b> Tập trung hóa toàn bộ logic khởi tạo, cấu hình tiêu đề và kích hoạt hộp thoại Modal chặn tương tác (Blocking Dialogs) về một nơi duy nhất, giúp loại bỏ mã nguồn trùng lặp trong hệ thống các Controller.</li>
 * <li><b>Chuẩn hóa trải nghiệm người dùng (UX Standard):</b> Đồng bộ hóa cấu trúc hiển thị thông điệp, tự động loại bỏ vùng tiêu đề phụ (`headerText = null`) giúp giao diện thông báo trở nên gọn gàng, trực quan và scannable hơn.</li>
 * </ul>
 * * @since 1.0
 * @see javafx.scene.control.Alert
 * @see javafx.scene.control.Alert.AlertType
 */
public class AlertUtils {

    /**
     * Hiển thị một hộp thoại thông báo cảnh báo nghiệp vụ (Warning Modal Alert).
     * Thường dùng để nhắc nhở người dùng về các ràng buộc dữ liệu hoặc rủi ro hệ thống chưa đến mức nghiêm trọng.
     * * @param title Tiêu đề xuất hiện trên thanh cửa sổ hộp thoại
     * @param msg   Nội dung chuỗi văn bản chi tiết mô tả thông điệp cảnh báo
     */
    public static void showWarning(String title, String msg) {
        showAlert(Alert.AlertType.WARNING, title, msg);
    }

    /**
     * Hiển thị một hộp thoại thông báo thông tin thành công hoặc trạng thái (Information Modal Alert).
     * Thường dùng để xác nhận hoàn tất các tác vụ đăng ký, tất toán hóa đơn hoặc đấu giá thành công.
     * * @param title Tiêu đề xuất hiện trên thanh cửa sổ hộp thoại
     * @param msg   Nội dung chuỗi văn bản chi tiết mô tả thông điệp thông tin
     */
    public static void showInfo(String title, String msg) {
        showAlert(Alert.AlertType.INFORMATION, title, msg);
    }

    /**
     * Hiển thị một hộp thoại báo lỗi hệ thống hoặc vi phạm chốt chặn bảo mật (Error Modal Alert).
     * Thường dùng để cô lập các ngoại lệ I/O kết nối mạng Socket, thâm hụt số dư ví tài khoản hoặc sai lệch dữ liệu.
     * * @param title Tiêu đề xuất hiện trên thanh cửa sổ hộp thoại
     * @param msg   Nội dung chuỗi văn bản chi tiết mô tả thông điệp lỗi kỹ thuật
     */
    public static void showError(String title, String msg) {
        showAlert(Alert.AlertType.ERROR, title, msg);
    }

    /**
     * Phương thức nhà máy cốt lõi (Core Factory & Dispatch Method).
     * <p>
     * Đảm nhiệm vai trò khởi tạo thực thể JavaFX {@link Alert} theo phân loại cấu trúc chỉ định,
     * thiết lập các thông số hình học văn bản và kích hoạt hiển thị dạng chặn luồng tương tác nền ngầm (`showAndWait`).
     * </p>
     * * @param type  Phân loại danh mục thông báo đồ họa {@link javafx.scene.control.Alert.AlertType}
     * @param title Tiêu đề chuỗi văn bản xuất hiện trên thanh cửa sổ hộp thoại
     * @param msg   Nội dung chuỗi văn bản chi tiết hiển thị tại phân vùng trung tâm hộp thoại
     */
    public static void showAlert(Alert.AlertType type, String title, String msg) {
        // Khởi tạo cửa sổ thông báo Modal dựa trên kiểu cấu hình đồ họa (Error, Info, Warning)
        Alert alert = new Alert(type);
        alert.setTitle(title);

        // Cưỡng chế xóa bỏ nhãn Header Text mặc định để tối ưu không gian hiển thị scannable gọn gàng
        alert.setHeaderText(null);
        alert.setContentText(msg);

        // Kích hoạt hiển thị hộp thoại dưới dạng Blocking Modal (Chặn toàn bộ tương tác chuột/bàn phím phía dưới cho đến khi đóng)
        alert.showAndWait();
    }
}