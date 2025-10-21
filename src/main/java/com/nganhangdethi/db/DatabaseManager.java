package com.nganhangdethi.db;

import com.nganhangdethi.util.AppConfig; // Giả sử bạn đã có lớp AppConfig để đọc file properties
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);

    // Các thông tin này sẽ được tải từ AppConfig
    private static String DB_URL_BASE;
    private static String DB_NAME;
    private static String USER;
    private static String PASS;
    private static String DB_PARAMS;
    private static String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";

    static {
        // Tải thông tin cấu hình khi lớp được load
        loadDatabaseConfiguration();
    }

    private static void loadDatabaseConfiguration() {
        // AppConfig.loadConfig(); // Đảm bảo AppConfig đã được load nếu cần
        DB_URL_BASE = AppConfig.getProperty("db.url.base", "jdbc:mysql://localhost:3306/");
        DB_NAME = AppConfig.getProperty("db.name", "NGAN_HANG_DE_THI"); // Sử dụng tên CSDL từ config
        USER = AppConfig.getProperty("db.user", "root");
        PASS = AppConfig.getProperty("db.password", "Thanh7778"); // Lấy password từ config, để trống nếu không có
        DB_PARAMS = AppConfig.getProperty("db.params", "?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true");
        logger.info("Database configuration loaded: DB_NAME={}, USER={}", DB_NAME, USER);
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            logger.error("MySQL JDBC Driver not found: {}", e.getMessage());
            throw new SQLException("MySQL JDBC Driver not found.", e);
        }
        String connectionUrl = DB_URL_BASE + DB_NAME + DB_PARAMS;
        logger.debug("Attempting to connect to database: {}", connectionUrl.replace(PASS, "****")); // Che password khi log
        return DriverManager.getConnection(connectionUrl, USER, PASS);
    }

    public static void initializeSchemaAndDatabase() {
        // 1. Tạo CSDL nếu chưa tồn tại
        String dbCreationUrl = DB_URL_BASE + DB_PARAMS;
        try (Connection conn = DriverManager.getConnection(dbCreationUrl, USER, PASS);
             Statement stmt = conn.createStatement()) {
            logger.info("Checking/creating database: {}", DB_NAME);
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + DB_NAME + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
            logger.info("Database '{}' checked/created successfully.", DB_NAME);
        } catch (SQLException e) {
            logger.error("Error creating database '{}' (it might already exist or due to permissions): {}", DB_NAME, e.getMessage());
            // Không dừng lại ở đây vì CSDL có thể đã tồn tại
        }

        // 2. Tạo các bảng (sử dụng CSDL đã tạo)
        try (Connection conn = getConnection(); // Bây giờ kết nối đến CSDL cụ thể
             Statement stmt = conn.createStatement()) {

            logger.info("Initializing database tables in '{}'...", DB_NAME);

            stmt.execute("CREATE TABLE IF NOT EXISTS Questions ("
                + "id INT AUTO_INCREMENT PRIMARY KEY,"
                + "question_text TEXT NOT NULL,"
                + "option_a TEXT, option_b TEXT, option_c TEXT, option_d TEXT,"
                + "correct_answer CHAR(1) NOT NULL,"
                + "explanation TEXT,"
                + "level VARCHAR(10) NOT NULL,"
                + "type VARCHAR(50) NOT NULL,"
                + "audio_path VARCHAR(255) DEFAULT NULL,"
                + "image_path VARCHAR(255) DEFAULT NULL,"
                + "tags TEXT,"
                + "source VARCHAR(255) DEFAULT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,"
                + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
            logger.info("Table 'Questions' checked/created.");

            stmt.execute("CREATE TABLE IF NOT EXISTS Exams ("
                + "exam_id INT AUTO_INCREMENT PRIMARY KEY,"
                + "exam_name VARCHAR(255) NOT NULL,"
                + "description TEXT DEFAULT NULL,"
                + "level_target VARCHAR(10) DEFAULT NULL,"
                + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
            logger.info("Table 'Exams' checked/created.");

            stmt.execute("CREATE TABLE IF NOT EXISTS ExamQuestions ("
                + "exam_id INT NOT NULL,"
                + "question_id INT NOT NULL,"
                + "order_in_exam INT NOT NULL,"
                + "PRIMARY KEY (exam_id, question_id),"
                + "FOREIGN KEY (exam_id) REFERENCES Exams(exam_id) ON DELETE CASCADE ON UPDATE CASCADE,"
                + "FOREIGN KEY (question_id) REFERENCES Questions(id) ON DELETE CASCADE ON UPDATE CASCADE"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;");
            logger.info("Table 'ExamQuestions' checked/created.");

            // Tạo index một cách an toàn
            createIndexIfNotExists(conn, "Questions", "idx_question_level", "level");
            createIndexIfNotExists(conn, "Questions", "idx_question_type", "type");

            logger.info("Database schema initialization complete for '{}'.", DB_NAME);

        } catch (SQLException e) {
            logger.error("Error initializing tables in database '{}': {}", DB_NAME, e.getMessage(), e);
            // Quyết định có ném lại lỗi hay không tùy thuộc vào yêu cầu
            // throw new RuntimeException("Failed to initialize database schema.", e);
        }
    }

    private static void createIndexIfNotExists(Connection conn, String tableName, String indexName, String columnName) {
        try {
            if (!indexExists(conn, tableName, indexName)) {
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE INDEX " + indexName + " ON " + tableName + " (" + columnName + ")";
                    stmt.executeUpdate(sql);
                    logger.info("Successfully created index '{}' on table '{}' (column '{}').", indexName, tableName, columnName);
                }
            } else {
                logger.debug("Index '{}' on table '{}' already exists.", indexName, tableName);
            }
        } catch (SQLException e) {
            logger.error("Error creating or checking index '{}' on table '{}': {}", indexName, tableName, e.getMessage());
        }
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        // Cần lấy tên CSDL hiện tại (catalog)
        String catalog = connection.getCatalog();
        // logger.debug("Checking for index '{}' in table '{}', catalog '{}'", indexName, tableName, catalog);

        try (ResultSet rs = metaData.getIndexInfo(catalog, null, tableName, false, false)) {
            while (rs.next()) {
                String existingIndexName = rs.getString("INDEX_NAME");
                if (indexName.equalsIgnoreCase(existingIndexName)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static void main(String[] args) {
        // Quan trọng: Đảm bảo AppConfig được load trước khi bất kỳ phương thức nào của DatabaseManager được gọi.
        // Điều này thường được thực hiện trong MainApplication.
        // Nếu bạn chạy main này độc lập, hãy chắc chắn AppConfig đã được load.
        // AppConfig.loadConfig(); // Nếu cần chạy độc lập

        // Chỉ chạy một lần để thiết lập CSDL và bảng nếu cần
        // initializeSchemaAndDatabase();

        logger.info("Testing database connection...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                logger.info("Kết nối CSDL MySQL ('{}') thành công!", DB_NAME);
            } else {
                logger.warn("Kết nối CSDL MySQL ('{}') thất bại. Connection is null or closed.", DB_NAME);
            }
        } catch (SQLException e) {
            logger.error("Lỗi kết nối CSDL ('{}'): {}", DB_NAME, e.getMessage(), e);
        }
    }
}