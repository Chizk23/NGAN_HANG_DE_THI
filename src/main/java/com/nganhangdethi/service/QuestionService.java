package com.nganhangdethi.service;

import com.nganhangdethi.dao.QuestionDAO; // Đảm bảo import này đúng
import com.nganhangdethi.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList; // Thêm cho addMultipleQuestions
import java.util.Collections;
import java.util.List;

public class QuestionService {
    private static final Logger logger = LoggerFactory.getLogger(QuestionService.class);
    private QuestionDAO questionDAO;
    private FileManagementService fileManagementService;

    public QuestionService() {
        // Khởi tạo DAO và các service cần thiết
        // Giả sử QuestionDAO và FileManagementService đã được khởi tạo đúng cách
        // và có thể inject qua constructor nếu dùng Dependency Injection framework
        this.questionDAO = new QuestionDAO();
        this.fileManagementService = new FileManagementService();
    }

    public List<Question> getAllQuestions() {
        try {
            return questionDAO.getAllQuestions();
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get all questions. {}", e.getMessage(), e);
            return Collections.emptyList(); // Trả về danh sách rỗng nếu có lỗi
        }
    }

    public Question getQuestionById(int id) {
        try {
            return questionDAO.getQuestionById(id);
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get question by id {}. {}", id, e.getMessage(), e);
            return null; // Trả về null nếu có lỗi
        }
    }

    public boolean addQuestion(Question question) {
        if (question == null) {
            logger.warn("Service: Attempted to add a null question.");
            return false;
        }
        try {
            // Logic nghiệp vụ có thể thêm ở đây, ví dụ:
            // - Validate dữ liệu question trước khi lưu (một phần đã làm ở dialog)
            // - Xử lý file (đường dẫn file đã được QuestionDialog xử lý và gán vào Question object)

            // Gọi DAO để thêm câu hỏi
            boolean success = questionDAO.addQuestion(question);
            if (success) {
                logger.info("Service: Successfully added question (Generated ID: {}).", question.getId());
            } else {
                logger.warn("Service: Failed to add question (DAO returned false). Question Text (first 50 char): {}",
                        question.getQuestionText() != null ? question.getQuestionText().substring(0, Math.min(50, question.getQuestionText().length())) : "N/A");
            }
            return success;
        } catch (SQLException e) {
            logger.error("Service Error: Failed to add question. {}. Question Text (first 50 char): {}", e.getMessage(),
                    question.getQuestionText() != null ? question.getQuestionText().substring(0, Math.min(50, question.getQuestionText().length())) : "N/A", e);
            return false;
        }
    }

    public boolean updateQuestion(Question question) {
        if (question == null || question.getId() <= 0) {
            logger.warn("Service: Attempted to update a null question or question with invalid ID.");
            return false;
        }
        try {
            // Logic nghiệp vụ tương tự như addQuestion
            // Ví dụ: kiểm tra xem câu hỏi có tồn tại trước khi cập nhật không
            // Question existingQuestion = questionDAO.getQuestionById(question.getId());
            // if (existingQuestion == null) {
            //     logger.warn("Service: Question with ID {} not found for update.", question.getId());
            //     return false;
            // }

            boolean success = questionDAO.updateQuestion(question);
            if (success) {
                logger.info("Service: Successfully updated question ID {}.", question.getId());
            } else {
                logger.warn("Service: Failed to update question ID {} (DAO returned false).", question.getId());
            }
            return success;
        } catch (SQLException e) {
            logger.error("Service Error: Failed to update question (id={}). {}", question.getId(), e.getMessage(), e);
            return false;
        }
    }

    /**
     * Xóa câu hỏi khỏi CSDL và các file media liên quan (audio, image) nếu có.
     *
     * @param questionId ID của câu hỏi cần xóa.
     * @return true nếu xóa thành công khỏi CSDL, false nếu ngược lại. Việc xóa file là best-effort.
     */
    public boolean deleteQuestionAndAssociatedFiles(int questionId) {
        if (questionId <= 0) {
            logger.warn("Service: Attempted to delete question with invalid ID: {}", questionId);
            return false;
        }
        try {
            Question questionToDelete = questionDAO.getQuestionById(questionId);
            if (questionToDelete == null) {
                logger.warn("Service: Question with ID {} not found. Cannot delete associated files or DB entry.", questionId);
                return false; // Hoặc true nếu "không tìm thấy để xóa" coi là thành công
            }

            // 1. Xóa khỏi cơ sở dữ liệu trước
            boolean deletedFromDB = questionDAO.deleteQuestion(questionId);

            if (deletedFromDB) {
                logger.info("Service: Successfully deleted question ID {} from database.", questionId);

                // 2. Cố gắng xóa file audio (nếu đường dẫn là tuyệt đối và file tồn tại)
                String audioPath = questionToDelete.getAudioPath();
                if (audioPath != null && !audioPath.isEmpty()) {
                    File audioFile = new File(audioPath);
                    if (audioFile.isAbsolute()) { // Chỉ thử xóa nếu là đường dẫn tuyệt đối
                        if (audioFile.exists()) {
                            if (audioFile.delete()) {
                                logger.info("Service: Successfully deleted absolute audio file '{}' for question ID {}.", audioPath, questionId);
                            } else {
                                logger.warn("Service: Could not delete absolute audio file '{}' (exists but delete failed - in use/permissions?) for question ID {}.", audioPath, questionId);
                            }
                        } else {
                            logger.warn("Service: Absolute audio file '{}' for question ID {} not found on disk. Not deleted.", audioPath, questionId);
                        }
                    } else {
                        logger.warn("Service: Audio path '{}' for question ID {} is not absolute. File not deleted by service.", audioPath, questionId);
                    }
                }

                // 3. Cố gắng xóa file image (tương tự)
                String imagePath = questionToDelete.getImagePath();
                if (imagePath != null && !imagePath.isEmpty()) {
                    File imageFile = new File(imagePath);
                     if (imageFile.isAbsolute()) {
                        if (imageFile.exists()) {
                            if (imageFile.delete()) {
                                logger.info("Service: Successfully deleted absolute image file '{}' for question ID {}.", imagePath, questionId);
                            } else {
                                logger.warn("Service: Could not delete absolute image file '{}' (exists but delete failed) for question ID {}.", imagePath, questionId);
                            }
                        } else {
                            logger.warn("Service: Absolute image file '{}' for question ID {} not found on disk. Not deleted.", imagePath, questionId);
                        }
                    } else {
                        logger.warn("Service: Image path '{}' for question ID {} is not absolute. File not deleted by service.", imagePath, questionId);
                    }
                }
                return true; // Xóa DB thành công là điều kiện chính
            } else {
                logger.warn("Service: Failed to delete question ID {} from database (DAO returned false). Associated files will not be touched.", questionId);
                return false;
            }
        } catch (SQLException e) {
            logger.error("Service Error: SQL exception while trying to delete question (id={}). {}", questionId, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            logger.error("Service Error: Unexpected error during deletion of question ID {} or its associated files. {}", questionId, e.getMessage(), e);
            return false;
        }
    }
    public List<Question> searchQuestions(String keyword, String level, String type, List<String> tags, String source, Boolean hasAudio, Boolean hasImage) {
        try {
            // Đảm bảo rằng Constants.ALL_FILTER_OPTION được chuyển thành null hoặc chuỗi rỗng
            // trước khi truyền vào DAO nếu DAO xử lý null/empty string cho "tất cả"
            String effectiveLevel = (level != null && level.equals(com.nganhangdethi.util.Constants.ALL_FILTER_OPTION)) ? null : level;
            String effectiveType = (type != null && type.equals(com.nganhangdethi.util.Constants.ALL_FILTER_OPTION)) ? null : type;
            String effectiveSource = (source != null && source.equals(com.nganhangdethi.util.Constants.ALL_FILTER_OPTION)) ? null : source;


            logger.debug("Service: Searching questions with params - Keyword: [{}], Level: [{}], Type: [{}], Tags: [{}], Source: [{}], HasAudio: [{}], HasImage: [{}]",
                    keyword, effectiveLevel, effectiveType, tags, effectiveSource, hasAudio, hasImage);

            return questionDAO.searchQuestionsAdvanced(keyword, effectiveLevel, effectiveType, tags, effectiveSource, hasAudio, hasImage);
        } catch (SQLException e) {
            logger.error("Service Error: Failed to search questions. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

     public List<String> getDistinctLevels() {
        try {
            return questionDAO.getDistinctValues("level");
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get distinct levels. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<String> getDistinctTypes() {
        try {
            return questionDAO.getDistinctValues("type");
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get distinct types. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public List<String> getDistinctSources() {
        try {
            return questionDAO.getDistinctValues("source");
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get distinct sources. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Thêm nhiều câu hỏi vào cơ sở dữ liệu.
     * Các câu hỏi này thường được tạo bởi AI và không có file media liên quan.
     *
     * @param questions Danh sách các đối tượng Question cần thêm.
     * @return true nếu ít nhất một câu hỏi được thêm thành công, false nếu tất cả đều thất bại.
     */
    public boolean addMultipleQuestions(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            logger.info("Service: No questions provided for batch adding.");
            return true; // Coi như thành công vì không có gì để làm
        }

        int successCount = 0;
        int failureCount = 0;
        List<String> errorMessages = new ArrayList<>();

        logger.info("Service: Attempting to batch add {} questions.", questions.size());

        for (Question question : questions) {
            if (question == null) {
                logger.warn("Service: Encountered a null question in batch add list. Skipping.");
                failureCount++;
                continue;
            }
            // Đảm bảo ID là 0 hoặc không được set để DAO tạo ID mới
            question.setId(0);
            // Các trường file media (imagePath, audioPath) nên là null/empty
            // vì AI không cung cấp file, chỉ cung cấp text.
            // Việc validate câu hỏi (ví dụ: questionText không rỗng) nên được thực hiện trước đó
            // hoặc trong phương thức addQuestion.

            if (addQuestion(question)) { // Gọi lại phương thức addQuestion đã được tinh chỉnh
                successCount++;
            } else {
                failureCount++;
                String errorMsg = "Failed to add question (batch): " + (question.getQuestionText() != null ? question.getQuestionText().substring(0, Math.min(30, question.getQuestionText().length())) + "..." : "N/A");
                errorMessages.add(errorMsg);
                // log chi tiết hơn đã có trong addQuestion
            }
        }

        if (failureCount > 0) {
            logger.warn("Service: Batch add complete. {} questions succeeded, {} questions failed.", successCount, failureCount);
            // for (String errMsg : errorMessages) {
            //     logger.debug("Service: Batch add error detail: {}", errMsg);
            // }
        } else {
            logger.info("Service: Successfully batch added all {} questions.", successCount);
        }

        // Trả về true nếu ít nhất một câu hỏi được thêm thành công.
        // Hoặc bạn có thể thay đổi logic: return failureCount == 0; (chỉ true nếu tất cả thành công)
        return successCount > 0;
    }
}