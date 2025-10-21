package com.nganhangdethi.dao;

import com.nganhangdethi.db.DatabaseManager;
import com.nganhangdethi.model.Exam;
import com.nganhangdethi.util.Constants; // Đảm bảo bạn có lớp này
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDAO {

    private static final Logger logger = LoggerFactory.getLogger(ExamDAO.class);

    public boolean addExam(Exam exam) throws SQLException {
        String sql = "INSERT INTO Exams (exam_name, description, level_target) VALUES (?, ?, ?)";
        logger.debug("Executing SQL: INSERT INTO Exams with name: {}", exam.getExamName());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            pstmt.setString(1, exam.getExamName());
            pstmt.setString(2, exam.getDescription());
            pstmt.setString(3, exam.getLevelTarget());

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        exam.setExamId(generatedKeys.getInt(1));
                        logger.info("Successfully added exam with ID: {}", exam.getExamId());
                        Exam dbExam = getExamById(exam.getExamId()); // Lấy lại để có timestamp
                        if (dbExam != null) {
                            exam.setCreatedAt(dbExam.getCreatedAt());
                        }
                    }
                }
                return true;
            }
            logger.warn("Failed to add exam, no rows affected. SQL: {}", sql);
            return false;
        } catch (SQLException e) {
            logger.error("Error adding exam: {}", e.getMessage(), e);
            throw e;
        }
    }

    public Exam getExamById(int examId) throws SQLException {
        String sql = "SELECT * FROM Exams WHERE exam_id = ?";
        logger.debug("Executing SQL: {} with ID: {}", sql, examId);
        Exam exam = null;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    exam = mapResultSetToExam(rs);
                    logger.debug("Found exam with ID: {}", examId);
                } else {
                    logger.debug("No exam found with ID: {}", examId);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting exam by ID {}: {}", examId, e.getMessage(), e);
            throw e;
        }
        return exam;
    }

    public List<Exam> getAllExams() throws SQLException {
        String sql = "SELECT * FROM Exams ORDER BY exam_id DESC";
        logger.debug("Executing SQL: {}", sql);
        List<Exam> exams = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                exams.add(mapResultSetToExam(rs));
            }
            logger.info("Retrieved {} exams from database.", exams.size());
        } catch (SQLException e) {
            logger.error("Error getting all exams: {}", e.getMessage(), e);
            throw e;
        }
        return exams;
    }

    public boolean updateExam(Exam exam) throws SQLException {
        String sql = "UPDATE Exams SET exam_name = ?, description = ?, level_target = ? WHERE exam_id = ?";
        logger.debug("Executing SQL: UPDATE Exams for exam ID: {}", exam.getExamId());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, exam.getExamName());
            pstmt.setString(2, exam.getDescription());
            pstmt.setString(3, exam.getLevelTarget());
            pstmt.setInt(4, exam.getExamId());

            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                logger.info("Successfully updated exam with ID: {}", exam.getExamId());
            } else {
                logger.warn("Failed to update exam with ID: {}, no rows affected or data unchanged.", exam.getExamId());
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Error updating exam ID {}: {}", exam.getExamId(), e.getMessage(), e);
            throw e;
        }
    }

    public boolean deleteExam(int examId) throws SQLException {
        // Liên kết trong ExamQuestions sẽ tự động bị xóa do ON DELETE CASCADE
        String sql = "DELETE FROM Exams WHERE exam_id = ?";
        logger.debug("Executing SQL: {} for exam ID: {}", sql, examId);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            boolean deleted = pstmt.executeUpdate() > 0;
            if (deleted) {
                logger.info("Successfully deleted exam with ID: {}", examId);
            } else {
                logger.warn("Failed to delete exam with ID: {}, exam not found or no rows affected.", examId);
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Error deleting exam ID {}: {}", examId, e.getMessage(), e);
            throw e;
        }
    }

    public List<Exam> searchExams(String keyword, String levelTarget) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Exams WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sqlBuilder.append("AND (exam_name LIKE ? OR description LIKE ?) ");
            String likeKeyword = "%" + keyword.trim() + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
        }
        if (levelTarget != null && !levelTarget.trim().isEmpty() && !levelTarget.equals(Constants.ALL_FILTER_OPTION)) {
            sqlBuilder.append("AND level_target = ? ");
            params.add(levelTarget);
        }
        sqlBuilder.append("ORDER BY exam_id DESC");

        logger.debug("Executing exam search query: {} with params: {}", sqlBuilder.toString(), params);
        List<Exam> exams = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    exams.add(mapResultSetToExam(rs));
                }
            }
            logger.info("Exam search found {} exams.", exams.size());
        } catch (SQLException e) {
            logger.error("Error during exam search: {}", e.getMessage(), e);
            throw e;
        }
        return exams;
    }

    private Exam mapResultSetToExam(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setExamId(rs.getInt("exam_id"));
        exam.setExamName(rs.getString("exam_name"));
        exam.setDescription(rs.getString("description"));
        exam.setLevelTarget(rs.getString("level_target"));
        exam.setCreatedAt(rs.getTimestamp("created_at"));
        return exam;
    }
}