package com.nganhangdethi.dao;

import com.nganhangdethi.db.DatabaseManager;
import com.nganhangdethi.model.ExamQuestion;
import com.nganhangdethi.model.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamQuestionDAO {

    private static final Logger logger = LoggerFactory.getLogger(ExamQuestionDAO.class);
    private final QuestionDAO questionDAO;

    public ExamQuestionDAO() {
        this.questionDAO = new QuestionDAO(); // Khởi tạo QuestionDAO để sử dụng
    }

    public boolean addQuestionToExam(int examId, int questionId, int orderInExam) throws SQLException {
        String sql = "INSERT INTO ExamQuestions (exam_id, question_id, order_in_exam) VALUES (?, ?, ?)";
        logger.debug("Executing SQL: INSERT INTO ExamQuestions for examId: {}, questionId: {}, order: {}", sql, examId, questionId, orderInExam);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, questionId);
            pstmt.setInt(3, orderInExam);
            boolean added = pstmt.executeUpdate() > 0;
            if (added) {
                logger.info("Successfully added questionId: {} to examId: {} with order: {}", questionId, examId, orderInExam);
            } else {
                logger.warn("Failed to add questionId: {} to examId: {}, no rows affected.", questionId, examId);
            }
            return added;
        } catch (SQLException e) {
            logger.error("Error adding question to exam (examId: {}, questionId: {}): {}", examId, questionId, e.getMessage(), e);
            throw e;
        }
    }

    public boolean removeQuestionFromExam(int examId, int questionId) throws SQLException {
        String sql = "DELETE FROM ExamQuestions WHERE exam_id = ? AND question_id = ?";
        logger.debug("Executing SQL: DELETE FROM ExamQuestions for examId: {}, questionId: {}", sql, examId, questionId);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            pstmt.setInt(2, questionId);
            boolean removed = pstmt.executeUpdate() > 0;
            if (removed) {
                logger.info("Successfully removed questionId: {} from examId: {}", questionId, examId);
            } else {
                logger.warn("Failed to remove questionId: {} from examId: {}, mapping not found or no rows affected.", questionId, examId);
            }
            return removed;
        } catch (SQLException e) {
            logger.error("Error removing question from exam (examId: {}, questionId: {}): {}", examId, questionId, e.getMessage(), e);
            throw e;
        }
    }

    public int removeAllQuestionsFromExam(int examId) throws SQLException {
        String sql = "DELETE FROM ExamQuestions WHERE exam_id = ?";
        logger.debug("Executing SQL: DELETE All FROM ExamQuestions for examId: {}", sql, examId);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            int rowsAffected = pstmt.executeUpdate();
            logger.info("Removed {} questions from examId: {}", rowsAffected, examId);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Error removing all questions from examId {}: {}", examId, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Thêm hoặc cập nhật danh sách câu hỏi cho một đề thi một cách transactional.
     * Sẽ xóa tất cả các liên kết câu hỏi cũ của đề thi này trước, sau đó thêm các câu hỏi mới.
     *
     * @param examId             ID của đề thi.
     * @param questionsForExam   Danh sách các đối tượng Question cần gán cho đề thi.
     *                           Thứ tự trong danh sách này sẽ được dùng làm order_in_exam.
     * @return true nếu thao tác thành công.
     * @throws SQLException Nếu có lỗi cơ sở dữ liệu.
     */
    public boolean setQuestionsForExam(int examId, List<Question> questionsForExam) throws SQLException {
        Connection conn = null;
        boolean success = false;
        boolean previousAutoCommit = true;
        logger.debug("Starting transactional setQuestionsForExam for examId: {}, with {} questions.", examId, questionsForExam.size());

        try {
            conn = DatabaseManager.getConnection();
            previousAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false); // Bắt đầu transaction

            // 1. Xóa tất cả các liên kết câu hỏi hiện tại của đề thi này
            String sqlDelete = "DELETE FROM ExamQuestions WHERE exam_id = ?";
            try (PreparedStatement pstmtDelete = conn.prepareStatement(sqlDelete)) {
                pstmtDelete.setInt(1, examId);
                int deletedRows = pstmtDelete.executeUpdate();
                logger.debug("Deleted {} old question mappings for examId: {}", deletedRows, examId);
            }

            // 2. Thêm các câu hỏi mới vào đề
            if (questionsForExam != null && !questionsForExam.isEmpty()) {
                String sqlInsert = "INSERT INTO ExamQuestions (exam_id, question_id, order_in_exam) VALUES (?, ?, ?)";
                try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                    int order = 1;
                    for (Question question : questionsForExam) {
                        if (question.getId() <= 0) { // Bỏ qua câu hỏi không hợp lệ
                            logger.warn("Skipping question with invalid ID (<=0) while setting questions for examId: {}", examId);
                            continue;
                        }
                        pstmtInsert.setInt(1, examId);
                        pstmtInsert.setInt(2, question.getId());
                        pstmtInsert.setInt(3, order++);
                        pstmtInsert.addBatch();
                    }
                    int[] batchResult = pstmtInsert.executeBatch();
                    logger.debug("Batch inserted {} new question mappings for examId: {}", batchResult.length, examId);
                }
            }

            conn.commit(); // Commit transaction
            success = true;
            logger.info("Successfully set {} questions for exam ID: {}", questionsForExam.size(), examId);

        } catch (SQLException e) {
            logger.error("Error setting questions transactionally for exam ID {}: {}", examId, e.getMessage(), e);
            if (conn != null) {
                try {
                    conn.rollback();
                    logger.warn("Transaction rolled back for exam ID: {} due to error.", examId);
                } catch (SQLException exRollback) {
                    logger.error("Error rolling back transaction for exam ID {}: {}", examId, exRollback.getMessage(), exRollback);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(previousAutoCommit);
                } catch (SQLException exRestore) {
                    logger.warn("Could not restore auto-commit state for exam ID: {}. Error: {}", examId, exRestore.getMessage());
                }
                try { conn.close(); } catch (SQLException exClose) { logger.warn("Error closing connection for exam ID: {}. Error: {}", examId, exClose.getMessage());}
            }
        }
        return success;
    }

    public List<Question> getQuestionsForExam(int examId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT q.* FROM Questions q " +
                     "JOIN ExamQuestions eq ON q.id = eq.question_id " +
                     "WHERE eq.exam_id = ? " +
                     "ORDER BY eq.order_in_exam ASC";
        logger.debug("Executing SQL: getQuestionsForExam for examId: {}", examId);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(this.questionDAO.mapResultSetToQuestion(rs));
                }
            }
            logger.info("Retrieved {} questions for examId: {}", questions.size(), examId);
        } catch (SQLException e) {
            logger.error("Error getting questions for examId {}: {}", examId, e.getMessage(), e);
            throw e;
        }
        return questions;
    }

    public List<ExamQuestion> getExamQuestionMappings(int examId) throws SQLException {
        List<ExamQuestion> mappings = new ArrayList<>();
        String sql = "SELECT exam_id, question_id, order_in_exam FROM ExamQuestions WHERE exam_id = ? ORDER BY order_in_exam ASC";
        logger.debug("Executing SQL: getExamQuestionMappings for examId: {}", examId);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    mappings.add(new ExamQuestion(
                        rs.getInt("exam_id"),
                        rs.getInt("question_id"),
                        rs.getInt("order_in_exam")
                    ));
                }
            }
            logger.info("Retrieved {} ExamQuestion mappings for examId: {}", mappings.size(), examId);
        } catch (SQLException e) {
            logger.error("Error getting ExamQuestion mappings for examId {}: {}", examId, e.getMessage(), e);
            throw e;
        }
        return mappings;
    }
}