package com.nganhangdethi; // Đảm bảo package này đúng với cấu trúc của bạn

import com.nganhangdethi.db.DatabaseManager;
import com.nganhangdethi.ui.MainFrame; // Giả sử bạn có lớp MainFrame cho UI
import com.nganhangdethi.util.AppConfig;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

public class MainApplication {

    public static void main(String[] args) {
        // 1. Load cấu hình ứng dụng TRƯỚC TIÊN
        System.out.println("INFO: MainApplication - Loading application configuration...");
        AppConfig.loadConfig();
        System.out.println("INFO: MainApplication - Application configuration loaded.");

        // 2. (Tùy chọn) Khởi tạo schema CSDL và các bảng nếu chạy lần đầu hoặc cần thiết lập lại
        // Bỏ comment dòng dưới đây NẾU bạn muốn tự động tạo/kiểm tra CSDL và bảng khi ứng dụng khởi động.
        // Chạy một lần rồi comment lại để tránh chạy lại mỗi lần khởi động.
        System.out.println("INFO: MainApplication - Initializing database schema (if enabled)...");
        // DatabaseManager.initializeSchemaAndDatabase();
        System.out.println("INFO: MainApplication - Database schema initialization step complete.");


        // 3. Tạo các thư mục cần thiết nếu chưa có (ví dụ: audio_files, image_files)
        System.out.println("INFO: MainApplication - Ensuring necessary directories exist...");
        ensureDirectoryExists(AppConfig.getAudioDirectory());
        ensureDirectoryExists(AppConfig.getImageDirectory());
        System.out.println("INFO: MainApplication - Directory check complete.");


        // 4. Test kết nối CSDL và hiển thị thông báo
        System.out.println("INFO: MainApplication - Testing database connection...");
        testDatabaseConnection();


        // 5. Thiết lập Look and Feel cho giao diện (tùy chọn, làm cho UI đẹp hơn)
        System.out.println("INFO: MainApplication - Setting up Look and Feel...");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            // Hoặc bạn có thể dùng Nimbus Look and Feel:
            // for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            // if ("Nimbus".equals(info.getName())) {
            // UIManager.setLookAndFeel(info.getClassName());
            // break;
            // }
            // }
            System.out.println("INFO: MainApplication - Look and Feel set to System Default.");
        } catch (Exception e) {
            System.err.println("ERROR: MainApplication - Could not set system Look and Feel: " + e.getMessage());
            // e.printStackTrace(); // In chi tiết lỗi nếu cần
        }

        // 6. Khởi chạy giao diện người dùng (UI) trên Event Dispatch Thread (EDT)
        System.out.println("INFO: MainApplication - Launching User Interface...");
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                MainFrame mainFrame = new MainFrame(); // Tạo instance của cửa sổ chính
                mainFrame.setVisible(true);
                System.out.println("INFO: MainApplication - MainFrame should be visible now.");
            }
        });
    }

    private static void testDatabaseConnection() {
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                // Lấy tên CSDL từ AppConfig để hiển thị chính xác
                String dbName = AppConfig.getProperty("db.name", "UNKNOWN_DB (Check AppConfig)");
                String successMessage = String.format("SUCCESS: MainApplication - Kết nối đến CSDL MySQL ('%s') thành công!", dbName);
                System.out.println(successMessage);
                // Bạn có thể hiển thị thông báo này trên UI nếu muốn, nhưng hiện tại chỉ in ra console
                // JOptionPane.showMessageDialog(null, successMessage, "Database Connection", JOptionPane.INFORMATION_MESSAGE);
            } else {
                String errorMessage = "ERROR: MainApplication - Kết nối CSDL thất bại. Connection is null or closed.";
                System.err.println(errorMessage);
                // JOptionPane.showMessageDialog(null, errorMessage, "Database Connection Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (SQLException e) {
            String errorMessage = String.format("ERROR: MainApplication - Lỗi khi kết nối CSDL: %s", e.getMessage());
            System.err.println(errorMessage);
            // e.printStackTrace(); // In chi tiết lỗi SQL nếu cần
            // JOptionPane.showMessageDialog(null, errorMessage, "Database Connection SQL Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            // Bắt các lỗi không mong muốn khác trong quá trình lấy connection
            String errorMessage = String.format("ERROR: MainApplication - Lỗi không mong muốn khi thử kết nối CSDL: %s", e.getMessage());
            System.err.println(errorMessage);
            // e.printStackTrace();
            // JOptionPane.showMessageDialog(null, errorMessage, "Database Connection Unexpected Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void ensureDirectoryExists(String path) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("ERROR: MainApplication - Invalid directory path provided (null or empty).");
            return;
        }
        File directory = new File(path);
        if (!directory.exists()) {
            System.out.println("INFO: MainApplication - Directory not found: " + path + ". Attempting to create...");
            if (directory.mkdirs()) {
                System.out.println("INFO: MainApplication - Successfully created directory: " + path);
            } else {
                System.err.println("ERROR: MainApplication - Failed to create directory: " + path);
            }
        } else {
            // System.out.println("INFO: MainApplication - Directory already exists: " + path);
        }
    }
}