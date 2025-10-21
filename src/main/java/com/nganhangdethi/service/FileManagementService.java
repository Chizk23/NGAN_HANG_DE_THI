package com.nganhangdethi.service; // Đảm bảo package đúng

import com.nganhangdethi.util.AppConfig; // Import AppConfig của bạn
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class FileManagementService {
    private static final Logger logger = LoggerFactory.getLogger(FileManagementService.class);

    // Danh sách các phần mở rộng file được chấp nhận (có thể đưa vào Constants hoặc AppConfig)
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(".png", ".jpg", ".jpeg", ".gif", ".bmp");
    private static final List<String> ALLOWED_AUDIO_EXTENSIONS = Arrays.asList(".mp3", ".wav", ".m4a", ".ogg");

    public FileManagementService() {
        // Đảm bảo các thư mục cơ sở tồn tại khi service được khởi tạo
        // Nếu AppConfig.getAudioDirectory() hoặc getImageDirectory() trả về đường dẫn mà chưa tồn tại,
        // chúng ta có thể cố gắng tạo chúng ở đây.
        ensureDirectoryExists(AppConfig.getAudioDirectory());
        ensureDirectoryExists(AppConfig.getImageDirectory());
    }

    private void ensureDirectoryExists(String directoryPath) {
        if (directoryPath == null || directoryPath.trim().isEmpty()) {
            logger.error("Directory path is null or empty, cannot ensure its existence.");
            return;
        }
        File dir = new File(directoryPath);
        if (!dir.exists()) {
            logger.info("Directory {} does not exist, attempting to create it.", directoryPath);
            if (dir.mkdirs()) {
                logger.info("Successfully created directory: {}", directoryPath);
            } else {
                logger.error("Failed to create directory: {}", directoryPath);
                // Có thể ném một RuntimeException ở đây nếu thư mục là bắt buộc cho hoạt động
            }
        } else if (!dir.isDirectory()){
            logger.error("Path {} exists but is not a directory.", directoryPath);
        }
    }


    /**
     * Lưu một file nguồn vào thư mục được quản lý bởi ứng dụng (audio hoặc image).
     * File sẽ được đổi tên thành một UUID duy nhất để tránh trùng lặp.
     *
     * @param sourceFile File nguồn cần lưu.
     * @param directoryType Loại thư mục đích ("audio" hoặc "image").
     * @return Tên file duy nhất (ví dụ: "xxxxxxxx-xxxx.jpg") đã được lưu trong thư mục con tương ứng.
     *         Đây là tên file, không phải đường dẫn tương đối đầy đủ.
     *         Trả về null nếu có lỗi.
     * @throws IOException Nếu có lỗi trong quá trình copy file hoặc loại file không hợp lệ.
     */
    public String saveFile(File sourceFile, String directoryType) throws IOException {
        if (sourceFile == null || !sourceFile.exists() || !sourceFile.isFile()) {
            logger.warn("File nguồn không hợp lệ hoặc không tồn tại: {}", sourceFile);
            throw new IOException("File nguồn không hợp lệ hoặc không tồn tại.");
        }

        String targetBaseDir = getBaseDir(directoryType); // Lấy đường dẫn thư mục đích từ AppConfig
        if (targetBaseDir == null) {
            // getBaseDir đã log lỗi
            throw new IOException("Không thể xác định thư mục đích cho loại: " + directoryType);
        }
        // Đảm bảo thư mục đích tồn tại (đã gọi trong constructor, nhưng gọi lại cho chắc)
        ensureDirectoryExists(targetBaseDir);


        String originalFileName = sourceFile.getName();
        String fileExtension = "";
        int lastDot = originalFileName.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < originalFileName.length() - 1) { // lastDot >= 0 để xử lý file không có phần mở rộng nhưng có dấu chấm ở đầu
            fileExtension = originalFileName.substring(lastDot).toLowerCase();
        } else if (lastDot == -1 && originalFileName.length() > 0) { // File không có phần mở rộng
             logger.warn("File '{}' không có phần mở rộng. Không thể xác định loại file.", originalFileName);
             // Tùy chọn: có thể từ chối lưu hoặc mặc định một extension nào đó (không khuyến khích)
             // throw new IOException("File không có phần mở rộng, không thể xử lý: " + originalFileName);
        }


        // Kiểm tra loại file dựa trên phần mở rộng
        if ("image".equalsIgnoreCase(directoryType)) {
            if (!ALLOWED_IMAGE_EXTENSIONS.contains(fileExtension)) {
                logger.warn("Loại file hình ảnh không được hỗ trợ: {} (File: {})", fileExtension, originalFileName);
                throw new IOException("Loại file hình ảnh không được hỗ trợ: " + fileExtension);
            }
        } else if ("audio".equalsIgnoreCase(directoryType)) {
            if (!ALLOWED_AUDIO_EXTENSIONS.contains(fileExtension)) {
                logger.warn("Loại file âm thanh không được hỗ trợ: {} (File: {})", fileExtension, originalFileName);
                throw new IOException("Loại file âm thanh không được hỗ trợ: " + fileExtension);
            }
        } else {
            // Loại thư mục không hợp lệ đã được xử lý bởi getBaseDir
        }

        // Tạo tên file duy nhất (UUID + phần mở rộng gốc)
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        Path targetPath;
        try {
            targetPath = Paths.get(targetBaseDir, uniqueFileName);
        } catch (InvalidPathException e) {
            logger.error("Đường dẫn đích không hợp lệ: {} / {}. Lỗi: {}", targetBaseDir, uniqueFileName, e.getMessage());
            throw new IOException("Đường dẫn đích không hợp lệ được tạo ra.", e);
        }


        try {
            Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            logger.info("Đã lưu thành công file '{}' thành '{}' trong thư mục '{}'", originalFileName, uniqueFileName, targetBaseDir);
            return uniqueFileName; // Chỉ trả về tên file duy nhất (ví dụ: "uuid_string.jpg")
        } catch (IOException e) {
            logger.error("Lỗi khi copy file '{}' tới '{}': {}", originalFileName, targetPath.toString(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lấy đối tượng File từ tên file (đường dẫn tương đối so với thư mục audio/image) và loại thư mục.
     *
     * @param fileName Tên file duy nhất đã được lưu (ví dụ: "uuid_string.jpg").
     * @param directoryType Loại thư mục ("audio" hoặc "image").
     * @return Đối tượng File nếu tìm thấy và là file, ngược lại là null.
     */
    public File getAppFile(String fileName, String directoryType) {
        if (fileName == null || fileName.trim().isEmpty()) {
            logger.debug("Tên file rỗng hoặc null cho getAppFile, type: {}", directoryType);
            return null;
        }

        String baseDir = getBaseDir(directoryType);
        if (baseDir == null) {
            return null;
        }

        File file;
        try {
            file = new File(baseDir, fileName);
        } catch (NullPointerException e) { // Nếu baseDir hoặc fileName là null (dù đã kiểm tra)
            logger.error("Lỗi khi tạo đối tượng File từ baseDir: '{}' và fileName: '{}'", baseDir, fileName, e);
            return null;
        }


        if (!file.exists() || !file.isFile()) {
            logger.warn("File ứng dụng không được tìm thấy hoặc không phải là file hợp lệ tại: {}", file.getAbsolutePath());
            return null;
        }
        return file;
    }

    /**
     * Lấy đường dẫn thư mục cơ sở cho một loại file nhất định (audio hoặc image).
     * Đường dẫn này được lấy từ AppConfig.
     *
     * @param directoryType "audio" hoặc "image".
     * @return Đường dẫn tuyệt đối đến thư mục, hoặc null nếu loại không hợp lệ hoặc cấu hình thiếu/lỗi.
     */
    public String getBaseDir(String directoryType) {
        String dirPath = null;
        if ("audio".equalsIgnoreCase(directoryType)) {
            dirPath = AppConfig.getAudioDirectory();
        } else if ("image".equalsIgnoreCase(directoryType)) {
            dirPath = AppConfig.getImageDirectory();
        } else {
            logger.error("Loại thư mục không hợp lệ được yêu cầu: {}. Chỉ chấp nhận 'audio' hoặc 'image'.", directoryType);
            return null;
        }

        if (dirPath == null || dirPath.trim().isEmpty()) {
            logger.error("Đường dẫn thư mục cho '{}' chưa được cấu hình trong AppConfig hoặc giá trị rỗng. Kiểm tra file config.properties (app.{}.dir).", directoryType, directoryType.toLowerCase());
            return null;
        }
        // Đảm bảo trả về đường dẫn tuyệt đối nếu AppConfig trả về tương đối
        // File dirFile = new File(dirPath);
        // if (!dirFile.isAbsolute()) {
        //     dirPath = dirFile.getAbsolutePath();
        // }
        // Giả định AppConfig.getAudioDirectory() và getImageDirectory() đã trả về đường dẫn tuyệt đối hoặc giải quyết đúng.
        return dirPath;
    }

    /**
     * Xóa một file được quản lý bởi ứng dụng.
     *
     * @param fileName Tên file duy nhất cần xóa.
     * @param directoryType Loại thư mục ("audio" hoặc "image").
     * @return true nếu file được xóa thành công hoặc không tồn tại (đã được xóa trước đó),
     *         false nếu có lỗi xảy ra trong quá trình xóa.
     */
    public boolean deleteAppFile(String fileName, String directoryType) {
        if (fileName == null || fileName.trim().isEmpty()){
            logger.debug("Tên file rỗng để xóa, type: {}. Xem như thành công.", directoryType);
            return true;
        }

        File fileToDelete = getAppFile(fileName, directoryType);
        if (fileToDelete == null) { // File không tồn tại (getAppFile đã log)
            return true; // Coi như đã xóa thành công
        }

        try {
            boolean deleted = Files.deleteIfExists(fileToDelete.toPath());
            if (deleted) {
                logger.info("Đã xóa thành công file ứng dụng: {}", fileToDelete.getAbsolutePath());
            } else {
                // Có thể file đã bị xóa bởi một tiến trình khác
                logger.warn("File ứng dụng không bị xóa (có thể không tồn tại tại thời điểm xóa): {}", fileToDelete.getAbsolutePath());
            }
            return true; // deleteIfExists không ném lỗi nếu file không tồn tại, nó trả về false
        } catch (IOException e) {
            logger.error("Lỗi khi xóa file ứng dụng {}: {}", fileToDelete.getAbsolutePath(), e.getMessage(), e);
            return false;
        } catch (SecurityException e) {
            logger.error("Lỗi bảo mật khi xóa file {}: {}", fileToDelete.getAbsolutePath(), e.getMessage(), e);
            return false;
        }
    }
}