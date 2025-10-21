// Trong file: com/nganhangdethi/service/ExamService.java
package com.nganhangdethi.service;

import com.nganhangdethi.dao.ExamDAO;
import com.nganhangdethi.dao.ExamQuestionDAO;
// import com.nganhangdethi.db.DatabaseManager; // Bỏ nếu không dùng transaction ở đây
import com.nganhangdethi.model.Exam;
import com.nganhangdethi.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import java.sql.Connection; // Bỏ nếu không dùng transaction ở đây
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public class ExamService {
    private static final Logger logger = LoggerFactory.getLogger(ExamService.class);
    private ExamDAO examDAO;
    private ExamQuestionDAO examQuestionDAO;
    // private QuestionDAO questionDAO; // Nếu bạn cần QuestionDAO ở đây, hãy khởi tạo nó

    public ExamService() {
        this.examDAO = new ExamDAO(); // Đảm bảo DAO được khởi tạo
        this.examQuestionDAO = new ExamQuestionDAO(); // Đảm bảo DAO được khởi tạo
        // this.questionDAO = new QuestionDAO(); // Nếu cần
    }

    public boolean createExamWithQuestions(Exam exam, List<Question> questions) {
        // TODO: Thêm Business Logic Validation
        if (exam == null || exam.getExamName() == null || exam.getExamName().trim().isEmpty()) {
            logger.warn("Service: Exam name is required to create an exam.");
            return false;
        }

        try {
            boolean examAdded = examDAO.addExam(exam);
            if (!examAdded || exam.getExamId() <= 0) {
                logger.error("Service: Failed to add base exam info for exam name: {}", exam.getExamName());
                return false;
            }

            // Chỉ liên kết câu hỏi nếu danh sách không rỗng
            if (questions != null && !questions.isEmpty()) {
                boolean questionsLinked = examQuestionDAO.setQuestionsForExam(exam.getExamId(), questions);
                if (!questionsLinked) {
                    logger.error("Service: Failed to link questions to exam ID: {}. Attempting to clean up partially created exam.", exam.getExamId());
                    // Cố gắng xóa exam đã tạo nếu không liên kết được câu hỏi (để tránh dữ liệu mồ côi)
                    try {
                        examDAO.deleteExam(exam.getExamId());
                        logger.info("Service: Cleaned up partially created exam ID: {}", exam.getExamId());
                    } catch (SQLException cleanupEx) {
                        logger.error("Service: Failed to cleanup partially created exam ID: {}. {}", exam.getExamId(), cleanupEx.getMessage());
                    }
                    return false;
                }
            }
            logger.info("Service: Successfully created exam '{}' (ID: {}) with {} questions.",
                        exam.getExamName(), exam.getExamId(), questions != null ? questions.size() : 0);
            return true;
        } catch (SQLException e) {
            logger.error("Service Error: Failed to create exam with questions for exam name '{}'. {}",
                         exam.getExamName(), e.getMessage(), e);
            return false;
        }
    }

    public Exam getExamWithDetails(int examId) {
        try {
            if (this.examDAO == null || this.examQuestionDAO == null) {
                 logger.error("Service Error: DAO instances are not initialized in getExamWithDetails.");
                 return null;
            }
            Exam exam = examDAO.getExamById(examId);
            if (exam != null) {
                List<Question> questions = examQuestionDAO.getQuestionsForExam(examId);
                exam.setQuestions(questions); // Gán danh sách câu hỏi vào đối tượng Exam
            }
            return exam;
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get exam details for ID {}. {}", examId, e.getMessage(), e);
            return null;
        }
    }
    
    // --- SỬA PHƯƠNG THỨC NÀY ---
    public Exam getExamById(int examId) {
        try {
            if (this.examDAO == null) {
                 logger.error("Service Error: examDAO is null in getExamById.");
                 return null;
            }
            return examDAO.getExamById(examId); // Gọi đến DAO
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get exam by id {}. {}", examId, e.getMessage(), e);
            return null;
        }
    }
    // --- KẾT THÚC SỬA ---

    public boolean updateExamWithQuestions(Exam exam, List<Question> questions) {
        // TODO: Business Logic Validation
        if (exam == null || exam.getExamId() <= 0) {
            logger.warn("Service: Invalid exam or exam ID for update.");
            return false;
        }
        try {
            boolean examInfoUpdated = examDAO.updateExam(exam);
            // updateExam trong DAO nên trả về true nếu có hàng bị ảnh hưởng,
            // false nếu không có hàng nào bị ảnh hưởng (ví dụ: examId không tồn tại hoặc dữ liệu không đổi).
            // Nếu nó trả về false vì examId không tồn tại, có thể không cần tiếp tục.
            if(!examInfoUpdated) {
                logger.warn("Service: Exam info for ID {} might not have been updated by DAO (or no changes needed/exam not found).", exam.getExamId());
                // Kiểm tra xem exam có tồn tại không nếu updateExam trả về false
                Exam existingExam = examDAO.getExamById(exam.getExamId());
                if (existingExam == null) {
                    logger.error("Service: Cannot update questions for non-existent exam ID: {}", exam.getExamId());
                    return false; // Exam không tồn tại, không thể cập nhật câu hỏi
                }
            }
            
            // Tiếp tục cập nhật danh sách câu hỏi ngay cả khi thông tin exam không thay đổi
            // vì người dùng có thể chỉ thay đổi danh sách câu hỏi.
            boolean questionsLinked = examQuestionDAO.setQuestionsForExam(exam.getExamId(), questions);
            if (!questionsLinked) {
                 logger.error("Service: Failed to update/link questions to exam ID: {}.", exam.getExamId());
                 return false;
            }
            logger.info("Service: Successfully updated exam '{}' (ID: {}) with {} questions.",
                        exam.getExamName(), exam.getExamId(), questions != null ? questions.size() : 0);
            return true;

        } catch (SQLException e) {
            logger.error("Service Error: Failed to update exam with questions for exam ID '{}'. {}",
                         exam.getExamId(), e.getMessage(), e);
            return false;
        }
    }

    public List<Exam> getAllExams() {
        try {
            if (this.examDAO == null) {
                 logger.error("Service Error: examDAO is null in getAllExams.");
                 return Collections.emptyList();
            }
            // Cân nhắc: getAllExams có nên trả về Exam đã có questionCount không?
            // Nếu có, DAO cần được sửa đổi. Hiện tại, nó trả về Exam cơ bản.
            return examDAO.getAllExams();
        } catch (SQLException e) {
            logger.error("Service Error: Failed to get all exams. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }
    
    public boolean deleteExam(int examId) {
        try {
            if (this.examDAO == null) {
                 logger.error("Service Error: examDAO is null in deleteExam.");
                 return false;
            }
            return examDAO.deleteExam(examId);
        } catch (SQLException e) {
            logger.error("Service Error: Failed to delete exam ID {}. {}", examId, e.getMessage(), e);
            return false;
        }
    }

    public List<Exam> searchExams(String keyword, String levelTarget) {
        try {
            if (this.examDAO == null) {
                 logger.error("Service Error: examDAO is null in searchExams.");
                 return Collections.emptyList();
            }
            return examDAO.searchExams(keyword, levelTarget);
        } catch (SQLException e) {
             logger.error("Service Error: Failed to search exams. {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public int getQuestionCountForExam(int examId) {
        try {
            if (this.examQuestionDAO == null) { // Hoặc examDAO nếu có phương thức count
                 logger.error("Service Error: examQuestionDAO is null in getQuestionCountForExam.");
                 return 0;
            }
            // Tốt nhất là ExamQuestionDAO có phương thức count riêng
            // return examQuestionDAO.countQuestionsByExamId(examId);
            // Cách hiện tại (load rồi đếm):
            Exam exam = getExamWithDetails(examId); // Sẽ gọi examDAO và examQuestionDAO
            return (exam != null && exam.getQuestions() != null) ? exam.getQuestions().size() : 0;
        } catch (Exception e) { // Bắt Exception chung vì getExamWithDetails có thể có lỗi
            logger.error("Service Error: Failed to get question count for exam ID {}. {}", examId, e.getMessage(), e);
            return 0;
        }
    }
}