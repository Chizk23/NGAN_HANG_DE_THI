package com.nganhangdethi.util; // Giữ nguyên package của bạn

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths; // Cho việc xử lý đường dẫn
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    // Thử load từ thư mục làm việc hiện tại trước (nơi JAR được chạy)
    private static final File CONFIG_FILE_EXTERNAL = new File(System.getProperty("user.dir"), CONFIG_FILE_NAME);
    // Nếu không có, thử load từ classpath (bên trong JAR hoặc thư mục resources khi phát triển)
    private static final String CONFIG_FILE_CLASSPATH = "/" + CONFIG_FILE_NAME; // Bắt đầu bằng / để tìm từ gốc classpath


    private static Properties properties = new Properties();
    private static boolean configLoaded = false;

    // Load config ngay khi lớp được tham chiếu lần đầu tiên.
    // Hoặc bạn có thể gọi tường minh AppConfig.loadConfig() ở đầu MainApplication.
    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        if (configLoaded) {
            return;
        }

        InputStream input = null;
        boolean loadedFromExternal = false;

        try {
            // 1. Ưu tiên load từ file bên ngoài (cùng thư mục JAR hoặc thư mục project)
            if (CONFIG_FILE_EXTERNAL.exists() && CONFIG_FILE_EXTERNAL.isFile()) {
                input = new FileInputStream(CONFIG_FILE_EXTERNAL);
                logger.info("Đang tải cấu hình từ file ngoài: {}", CONFIG_FILE_EXTERNAL.getAbsolutePath());
                loadedFromExternal = true;
            } else {
                // 2. Nếu không có, thử tải từ classpath (trong resources)
                input = AppConfig.class.getResourceAsStream(CONFIG_FILE_CLASSPATH);
                if (input != null) {
                    logger.info("Đang tải cấu hình từ classpath (resources): {}", CONFIG_FILE_CLASSPATH);
                } else {
                    logger.warn("Không tìm thấy file cấu hình '{}' ở thư mục ứng dụng hoặc classpath.", CONFIG_FILE_NAME);
                    logger.info("Đang cố gắng tạo file cấu hình mặc định tại: {}", CONFIG_FILE_EXTERNAL.getAbsolutePath());
                    createDefaultConfigFile(CONFIG_FILE_EXTERNAL);
                    // Thử tải lại sau khi tạo file mặc định
                    if (CONFIG_FILE_EXTERNAL.exists()) {
                        input = new FileInputStream(CONFIG_FILE_EXTERNAL);
                        logger.info("Đang tải file cấu hình mặc định vừa tạo từ: {}", CONFIG_FILE_EXTERNAL.getAbsolutePath());
                        loadedFromExternal = true;
                    }
                }
            }

            if (input != null) {
                properties.load(input);
                configLoaded = true;
                logger.info("Đã tải cấu hình thành công.");
            } else {
                logger.error("Không thể tải file cấu hình. Ứng dụng có thể không hoạt động như mong đợi.");
            }

        } catch (IOException ex) {
            logger.error("Lỗi khi tải file cấu hình: {}", ex.getMessage(), ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.error("Lỗi khi đóng luồng đọc file cấu hình: {}", e.getMessage(), e);
                }
            }
        }
    }

    private static void createDefaultConfigFile(File configFile) {
        Properties defaultConfig = new Properties();
        // Database Config
        defaultConfig.setProperty("db.url.base", "jdbc:mysql://localhost:3306/");
        defaultConfig.setProperty("db.name", "NGAN_HANG_DE_THI");
        defaultConfig.setProperty("db.user", "root");
        defaultConfig.setProperty("db.password", "YOUR_DB_PASSWORD_HERE"); // YÊU CẦU NGƯỜI DÙNG CẬP NHẬT
        defaultConfig.setProperty("db.params", "?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&useUnicode=true&characterEncoding=UTF-8&allowPublicKeyRetrieval=true");
        defaultConfig.setProperty("db.driver", "com.mysql.cj.jdbc.Driver");


        // AI Config - THÊM CHO GEMINI
        defaultConfig.setProperty("ai.gemini.apikey", "YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE"); // YÊU CẦU NGƯỜI DÙNG CẬP NHẬT

        // Application Directories
        // Thư mục gốc để lưu trữ dữ liệu của ứng dụng (ví dụ: audio, images)
        // Để trống nếu muốn dùng thư mục hiện tại của ứng dụng (nơi JAR được chạy)
        defaultConfig.setProperty("app.data.storage.dir", "app_data"); // Sẽ nằm trong thư mục chạy ứng dụng
        defaultConfig.setProperty("app.audio.dir.name", "audio_files"); // Tên thư mục con cho audio
        defaultConfig.setProperty("app.image.dir.name", "image_files"); // Tên thư mục con cho image
        defaultConfig.setProperty("app.default.export.directory", ""); // Để trống sẽ dùng thư mục Documents của người dùng

        // Tạo thư mục cha nếu cần
        File parentDir = configFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (OutputStream output = new FileOutputStream(configFile)) {
            defaultConfig.store(output, "Default Application Configuration - PLEASE UPDATE YOUR DB PASSWORD AND API KEY");
            logger.info("Đã tạo file cấu hình mặc định: {}", configFile.getAbsolutePath());
        } catch (IOException io) {
            logger.error("Không thể tạo file cấu hình mặc định '{}': {}", configFile.getAbsolutePath(), io.getMessage(), io);
        }
    }

    public static String getProperty(String key) {
        if (!configLoaded) {
            logger.warn("Cấu hình chưa được tải. Đang cố gắng tải lại cho key: '{}'", key);
            loadConfig();
        }
        return properties.getProperty(key);
    }

    public static String getProperty(String key, String defaultValue) {
        if (!configLoaded) {
            logger.warn("Cấu hình chưa được tải. Đang cố gắng tải lại cho key: '{}'", key);
            loadConfig();
        }
        return properties.getProperty(key, defaultValue);
    }

    // --- THAY ĐỔI CHO GEMINI API KEY ---
    public static String getGeminiApiKey() {
        return getProperty("ai.gemini.apikey", ""); // Trả về chuỗi rỗng nếu không có
    }

    public static void saveGeminiApiKey(String apiKey) {
        if (!configLoaded) loadConfig(); // Đảm bảo properties đã được load
        properties.setProperty("ai.gemini.apikey", apiKey != null ? apiKey : "");
        saveProperties();
    }
    // ----------------------------------

    private static synchronized void saveProperties() {
        // Ưu tiên lưu vào file bên ngoài nếu nó đã được load từ đó hoặc được tạo
        File configFileToSave = CONFIG_FILE_EXTERNAL;
        if (!configFileToSave.exists()) {
             // Nếu file ngoài không tồn tại (ví dụ khi chạy từ IDE mà không có file config ở gốc)
             // thì không nên cố tạo và lưu vào đó, trừ khi đó là hành vi mong muốn.
             // Trong trường hợp này, việc lưu có thể không cần thiết nếu config chỉ load từ resources.
             // Tuy nhiên, nếu SettingsDialog cho phép thay đổi thì cần một nơi để lưu.
             // Hiện tại, sẽ cố gắng lưu vào vị trí file ngoài.
             logger.info("File cấu hình ngoài không tồn tại, sẽ tạo mới tại: {}", configFileToSave.getAbsolutePath());
        }

        try (OutputStream output = new FileOutputStream(configFileToSave)) {
            properties.store(output, "Application Configuration - Updated by application");
            logger.info("Cấu hình đã được lưu vào: {}", configFileToSave.getAbsolutePath());
        } catch (IOException ex) {
            logger.error("Lỗi khi lưu file cấu hình '{}': {}", configFileToSave.getAbsolutePath(), ex.getMessage(), ex);
        }
    }

    /**
     * Lấy thư mục gốc dữ liệu của ứng dụng.
     * @return Đường dẫn tuyệt đối đến thư mục gốc dữ liệu.
     */
    public static String getApplicationDataStorageDirectory() {
        String baseDirProp = getProperty("app.data.storage.dir", "app_data"); // Mặc định là "app_data"
        File baseDir = new File(baseDirProp);
        if (!baseDir.isAbsolute()) {
            // Nếu là đường dẫn tương đối, nó sẽ tương đối so với thư mục chạy ứng dụng
            baseDir = new File(System.getProperty("user.dir"), baseDirProp);
        }
        return baseDir.getAbsolutePath();
    }

    public static String getAudioDirectory() {
        String audioDirName = getProperty("app.audio.dir.name", "audio_files");
        return Paths.get(getApplicationDataStorageDirectory(), audioDirName).toAbsolutePath().toString();
    }

    public static String getImageDirectory() {
        String imageDirName = getProperty("app.image.dir.name", "image_files");
        return Paths.get(getApplicationDataStorageDirectory(), imageDirName).toAbsolutePath().toString();
    }

    // Phương thức isLoaded (tùy chọn, để kiểm tra từ bên ngoài)
    public static boolean isConfigLoaded() {
        return configLoaded;
    }
}