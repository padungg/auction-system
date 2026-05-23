package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <h2>Launcher</h2>
 * <p>
 * Điểm khởi chạy trung tâm của ứng dụng Đấu giá (Main Application Launcher Core) phía Client.
 * Hiện thực hóa lớp cơ sở {@link Application} của cấu trúc JavaFX để thiết lập vòng đời vận hành chương trình.
 * </p>
 * * <p><b>Trách nhiệm cấu trúc và chu kỳ vận hành (Lifecycle):</b></p>
 * <ul>
 * <li><b>Khởi tạo hạ tầng mạng (Network Bootstrapping):</b> Tự động trích xuất các thông số cấu hình Host/Port động từ môi trường và thiết lập kết nối Socket mạng TCP duy nhất thông qua `ClientSocketManager` trước khi kết xuất đồ họa.</li>
 * <li><b>Nạp giao diện sơ khởi (UI Orchestration):</b> Đóng vai trò cấu trúc tải tệp màn hình FXML `/Login.fxml`, thiết lập vùng không gian Stage và hiển thị cửa sổ Đăng nhập ban đầu.</li>
 * <li><b>Giải phóng tài nguyên phòng vệ (Graceful Resource Teardown):</b> Đăng ký cơ chế hook chặn bắt sự kiện đóng cửa sổ đồ họa (`setOnCloseRequest`), đảm bảo phát lệnh ngắt kết nối an toàn, giải phóng Thread Pool ngầm và chống hiện tượng rò rỉ luồng chạy rác (Zombie Threads).</li>
 * <li><b>Cô lập ngoại lệ (Fault-Tolerant Crashing):</b> Bọc toàn bộ quy trình trong khối `try-catch`, tự động đẩy thông báo lỗi hệ thống thân thiện qua `AlertUtils` nếu Server chưa được bật, tránh hiện tượng sập ứng dụng im lặng (Silent App Crash).</li>
 * </ul>
 * * @since 1.0
 * @see javafx.application.Application
 * @see com.auction.client.network.ClientSocketManager
 * @see com.auction.client.util.AlertUtils
 */
public class Launcher extends Application {

    /** * Bộ ghi nhật ký tập trung (SLF4J Logger) cấu hình theo tiêu chuẩn an toàn đa luồng.
     * Chịu trách nhiệm lưu vết trạng thái khởi động đồ họa và các lỗi kết nối nghiêm trọng cấp mạng.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Launcher.class);

    /**
     * Khởi động vòng đời ứng dụng JavaFX (Main Entry Point Lifecycle Step).
     * Thiết lập cấu hình tham số Socket mạng, khởi tạo giao diện Login và đăng ký cơ chế giải phóng tài nguyên khi thoát.
     * * @param primaryStage Cửa sổ hiển thị đồ họa nền tảng (Primary Stage) do môi trường chạy JavaFX runtime cung cấp
     */
    @Override
    public void start(Stage primaryStage) {
        // Tải cấu hình Host và Port động từ tham số dòng lệnh cấu hình JVM (Fallback mặc định hệ thống: localhost:8080)
        String host = System.getProperty("server.host", "localhost");
        int port = Integer.getInteger("server.port", 8080);

        try {
            // =========================================================================
            // BƯỚC 1: THIẾT LẬP KẾT NỐI MẠNG HẠ TẦNG (NETWORK INITIALIZATION)
            // =========================================================================
            // Kết nối Socket TCP đến máy chủ Server trước khi kết xuất bất kỳ thành phần UI nào để tránh trạng thái mồ côi dữ liệu
            com.auction.client.network.ClientSocketManager.getInstance().connect(host, port);

            // =========================================================================
            // BƯỚC 2: KHỞI TẠO VÀ KẾT XUẤT ĐỒ HỌA (UI RENDERING CORRIDOR)
            // =========================================================================
            // Kích hoạt nạp tệp tin cấu hình giao diện đồ họa XML của phân hệ Đăng nhập
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setScene(scene);
            primaryStage.setTitle("UET Auction System");
            primaryStage.setResizable(false); // Cố định kích thước cửa sổ để đảm bảo bố cục tỷ lệ Responsive chuẩn

            // =========================================================================
            // BƯỚC 3: ĐĂNG KÝ MÓC CHẶN GIẢI PHÓNG TÀI NGUYÊN (CLEANUP STATE HOOK)
            // =========================================================================
            // Đăng ký bộ lắng nghe sự kiện đóng cửa sổ để cưỡng chế ngắt kết nối Socket và giải phóng tài nguyên hệ thống
            primaryStage.setOnCloseRequest(event -> {
                LOGGER.info("Người dùng phát lệnh đóng ứng dụng. Đang giải phóng tài nguyên mạng...");
                com.auction.client.network.ClientSocketManager.getInstance().disconnect();
                System.exit(0); // Kết thúc tiến trình hệ thống an toàn
            });

            primaryStage.show(); // Hiển thị Stage giao diện lên màn hình

        } catch (Exception e) {
            // LOG lỗi chi tiết phục vụ kỹ sư vận hành hệ thống gỡ lỗi (Debugging log)
            LOGGER.error("Gặp sự cố lỗi nghiêm trọng trong tiến trình khởi chạy ứng dụng hệ thống Đấu giá tại {}:{}", host, port, e);

            // =========================================================================
            // BƯỚC 4: XỬ LÝ LỖI PHÒNG VỆ SƠ KHỞI (FALLBACK ERROR HANDLING)
            // =========================================================================
            // Hiển thị hộp thoại lỗi tập trung thân thiện với người dùng thông qua AlertUtils
            String errorMsg = String.format("Không thể thiết lập kết nối tới Server tại địa chỉ %s:%d.\n" +
                    "Chi tiết: %s\n\nVui lòng kiểm tra lại xem Server đã được bật chưa và thử lại.", host, port, e.getMessage());

            com.auction.client.util.AlertUtils.showError("Lỗi Kết Nối", errorMsg);

            // Tắt môi trường chạy của JavaFX và kết thúc luồng phần mềm lập tức
            javafx.application.Platform.exit();
            System.exit(0);
        }
    }

    /**
     * Điểm mấu chốt kích hoạt khởi chạy tiến trình ứng dụng Java (Standard Java Entry Point).
     * Chuyển giao quyền thực thi cho lõi JavaFX Application Runtime điều phối vòng đời.
     * * @param args Mảng tham số dòng lệnh truyền vào khi chạy file Jar hoặc mã nguồn
     */
    public static void main(String[] args) {
        launch(args);
    }
}