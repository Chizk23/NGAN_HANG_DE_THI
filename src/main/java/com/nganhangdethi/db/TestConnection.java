package com.nganhangdethi.db; // Hoặc package bạn muốn

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement; // Thêm import này nếu muốn thử câu lệnh SQL
import java.sql.ResultSet;  // Thêm import này nếu muốn thử câu lệnh SQL

public class TestConnection {

    // THAY THẾ CÁC GIÁ TRỊ NÀY BẰNG THÔNG TIN CỦA BẠN
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "NGAN_HANG_DE_THI"; // Tên CSDL của bạn
    private static final String DB_USER = "root";  // Username MySQL của bạn
    private static final String DB_PASS = ""; // Password MySQL của bạn

    private static final String JDBC_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
                                          "?useSSL=false" +
                                          "&serverTimezone=UTC" + // Quan trọng cho múi giờ
                                          "&useUnicode=true" +      // Quan trọng cho tiếng Nhật
                                          "&characterEncoding=UTF-8" + // Quan trọng cho tiếng Nhật
                                          "&allowPublicKeyRetrieval=true"; // Có thể cần cho các phiên bản MySQL mới hơn

    public static void main(String[] args) {
        Connection connection = null;
        System.out.println("--- Bắt đầu kiểm tra kết nối MySQL ---");
        try {
            // Bước 1: Cố gắng thiết lập kết nối
            System.out.println("Đang thử kết nối đến: " + JDBC_URL);
            // Không cần Class.forName() với JDBC 4.0+ và Maven quản lý driver
            connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASS);

            // Bước 2: Kiểm tra đối tượng Connection
            if (connection != null && !connection.isClosed()) {
                System.out.println(">>> THÀNH CÔNG: Kết nối đến MySQL Database '" + DB_NAME + "' đã được thiết lập!");
                System.out.println("Thông tin kết nối: " + connection.getMetaData().getDatabaseProductName() + " " + connection.getMetaData().getDatabaseProductVersion());

                // (Tùy chọn) Bước 3: Thử thực thi một câu lệnh SQL rất đơn giản
                // Điều này xác nhận bạn có thể tương tác với CSDL
                System.out.println("Đang thử thực thi một câu lệnh SQL đơn giản...");
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT 1 AS test_value")) { // Câu lệnh này luôn trả về 1 hàng
                    if (rs.next()) {
                        System.out.println(">>> THÀNH CÔNG: Thực thi câu lệnh SQL đơn giản thành công! Giá trị trả về: " + rs.getInt("test_value"));
                    } else {
                        System.out.println("CẢNH BÁO: Thực thi câu lệnh SQL thành công nhưng không có kết quả (không mong đợi với 'SELECT 1').");
                    }
                } catch (SQLException sqlEx) {
                    System.err.println("LỖI khi thực thi câu lệnh SQL: " + sqlEx.getMessage());
                    sqlEx.printStackTrace();
                }

            } else {
                System.err.println(">>> THẤT BẠI: Không nhận được đối tượng Connection hợp lệ hoặc kết nối đã bị đóng.");
            }

        } catch (SQLException e) {
            System.err.println(">>> THẤT BẠI: Kết nối MySQL thất bại!");
            System.err.println("Chi tiết lỗi SQL:");
            System.err.println("  Message: " + e.getMessage());
            System.err.println("  SQLState: " + e.getSQLState());
            System.err.println("  Error Code: " + e.getErrorCode());
            System.err.println("  Vui lòng kiểm tra lại:");
            System.err.println("    1. MySQL Server có đang chạy không?");
            System.err.println("    2. Thông tin JDBC URL, DB_NAME, DB_USER, DB_PASS có chính xác không?");
            System.err.println("    3. MySQL JDBC Driver đã được thêm vào project (trong pom.xml) chưa?");
            System.err.println("    4. Cơ sở dữ liệu '" + DB_NAME + "' đã tồn tại trên server chưa?");
            System.err.println("    5. User '" + DB_USER + "' có quyền truy cập vào CSDL '" + DB_NAME + "' không?");
            e.printStackTrace(); // In chi tiết stack trace để debug sâu hơn
        } finally {
            // Bước 4: Đóng kết nối (luôn luôn thực hiện)
            if (connection != null) {
                try {
                    connection.close();
                    System.out.println("Đã đóng kết nối CSDL.");
                } catch (SQLException e) {
                    System.err.println("Lỗi khi đóng kết nối: " + e.getMessage());
                }
            }
        }
        System.out.println("--- Kết thúc kiểm tra kết nối MySQL ---");
    }
}