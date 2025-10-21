package com.nganhangdethi.dao;

import com.nganhangdethi.db.DatabaseManager;
import com.nganhangdethi.model.Question;
import com.nganhangdethi.util.Constants; // Đảm bảo bạn có lớp này
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDAO {

    private static final Logger logger = LoggerFactory.getLogger(QuestionDAO.class);

    /**
     * Thêm một câu hỏi mới vào cơ sở dữ liệu.
     *
     * @param question Đối tượng Question cần thêm.
     * @return true nếu thêm thành công, false nếu thất bại.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public boolean addQuestion(Question question) throws SQLException {
        String sql = "INSERT INTO Questions (question_text, option_a, option_b, option_c, option_d, " +
                     "correct_answer, explanation, level, type, audio_path, image_path, tags, source) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        logger.debug("Executing SQL: INSERT INTO Questions with text starting: {}",
                     question.getQuestionText() != null ? question.getQuestionText().substring(0, Math.min(question.getQuestionText().length(), 30)) : "null");

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            setPreparedStatementParametersForQuestion(pstmt, question);

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        question.setId(generatedKeys.getInt(1));
                        logger.info("Successfully added question with ID: {}", question.getId());
                        // Lấy lại timestamps từ CSDL để đảm bảo đối tượng question được cập nhật
                        Question dbQuestion = getQuestionById(question.getId());
                        if (dbQuestion != null) {
                            question.setCreatedAt(dbQuestion.getCreatedAt());
                            question.setUpdatedAt(dbQuestion.getUpdatedAt());
                        }
                    }
                }
                return true;
            }
            logger.warn("Failed to add question, no rows affected. SQL: {}", sql);
            return false;
        } catch (SQLException e) {
            logger.error("Error adding question: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Lấy một câu hỏi theo ID.
     *
     * @param id ID của câu hỏi.
     * @return Đối tượng Question nếu tìm thấy, null nếu không.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public Question getQuestionById(int id) throws SQLException {
        String sql = "SELECT * FROM Questions WHERE id = ?";
        logger.debug("Executing SQL: {} with ID: {}", sql, id);
        Question question = null;
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    question = mapResultSetToQuestion(rs);
                    logger.debug("Found question with ID: {}", id);
                } else {
                    logger.debug("No question found with ID: {}", id);
                }
            }
        } catch (SQLException e) {
            logger.error("Error getting question by ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
        return question;
    }

    /**
     * Lấy tất cả các câu hỏi từ cơ sở dữ liệu.
     *
     * @return Danh sách các đối tượng Question.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public List<Question> getAllQuestions() throws SQLException {
        String sql = "SELECT * FROM Questions ORDER BY id DESC";
        logger.debug("Executing SQL: {}", sql);
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                questions.add(mapResultSetToQuestion(rs));
            }
            logger.info("Retrieved {} questions from database.", questions.size());
        } catch (SQLException e) {
            logger.error("Error getting all questions: {}", e.getMessage(), e);
            throw e;
        }
        return questions;
    }

    /**
     * Cập nhật một câu hỏi hiện có trong cơ sở dữ liệu.
     *
     * @param question Đối tượng Question chứa thông tin cập nhật.
     * @return true nếu cập nhật thành công, false nếu không.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public boolean updateQuestion(Question question) throws SQLException {
        String sql = "UPDATE Questions SET question_text = ?, option_a = ?, option_b = ?, option_c = ?, option_d = ?, " +
                     "correct_answer = ?, explanation = ?, level = ?, type = ?, audio_path = ?, image_path = ?, " +
                     "tags = ?, source = ? WHERE id = ?"; // updated_at sẽ tự cập nhật nhờ CSDL
        logger.debug("Executing SQL: UPDATE Questions for question ID: {}", question.getId());
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            setPreparedStatementParametersForQuestion(pstmt, question);
            pstmt.setInt(14, question.getId()); // ID cho điều kiện WHERE

            boolean updated = pstmt.executeUpdate() > 0;
            if (updated) {
                logger.info("Successfully updated question with ID: {}", question.getId());
                // Lấy lại timestamp updated_at từ CSDL
                Question dbQuestion = getQuestionById(question.getId());
                if (dbQuestion != null) {
                    question.setUpdatedAt(dbQuestion.getUpdatedAt());
                }
            } else {
                logger.warn("Failed to update question with ID: {}, no rows affected or data unchanged.", question.getId());
            }
            return updated;
        } catch (SQLException e) {
            logger.error("Error updating question ID {}: {}", question.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Xóa một câu hỏi khỏi cơ sở dữ liệu theo ID.
     *
     * @param id ID của câu hỏi cần xóa.
     * @return true nếu xóa thành công, false nếu không.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public boolean deleteQuestion(int id) throws SQLException {
        String sql = "DELETE FROM Questions WHERE id = ?";
        logger.debug("Executing SQL: {} for question ID: {}", sql, id);
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            boolean deleted = pstmt.executeUpdate() > 0;
            if (deleted) {
                logger.info("Successfully deleted question with ID: {}", id);
            } else {
                logger.warn("Failed to delete question with ID: {}, question not found or no rows affected.", id);
            }
            return deleted;
        } catch (SQLException e) {
            logger.error("Error deleting question ID {}: {}", id, e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Tìm kiếm câu hỏi dựa trên nhiều tiêu chí.
     *
     * @param keyword    Từ khóa tìm kiếm trong nội dung, giải thích, tags, hoặc ID.
     * @param level      Cấp độ (N1-N5), hoặc "Tất cả".
     * @param type       Loại câu hỏi, hoặc "Tất cả".
     * @param tagsList   Danh sách các tags (tìm câu hỏi chứa BẤT KỲ tag nào trong list).
     * @param source     Nguồn gốc câu hỏi.
     * @param hasAudio   Lọc theo có file audio hay không.
     * @param hasImage   Lọc theo có file image hay không.
     * @return Danh sách các câu hỏi thỏa mãn.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public List<Question> searchQuestionsAdvanced(String keyword, String level, String type,
                                                  List<String> tagsList, String source,
                                                  Boolean hasAudio, Boolean hasImage) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM Questions WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            String trimmedKeyword = keyword.trim();
            sqlBuilder.append("AND (question_text LIKE ? OR explanation LIKE ? OR tags LIKE ? ");
            params.add("%" + trimmedKeyword + "%");
            params.add("%" + trimmedKeyword + "%");
            params.add("%" + trimmedKeyword + "%");
            // Thử chuyển keyword thành số để tìm theo ID
            try {
                int idKeyword = Integer.parseInt(trimmedKeyword);
                sqlBuilder.append("OR id = ?) ");
                params.add(idKeyword);
            } catch (NumberFormatException e) {
                sqlBuilder.append(") "); // Đóng ngoặc nếu không phải là số
            }
        }
        if (level != null && !level.trim().isEmpty() && !level.equals(Constants.ALL_FILTER_OPTION)) {
            sqlBuilder.append("AND level = ? ");
            params.add(level);
        }
        if (type != null && !type.trim().isEmpty() && !type.equals(Constants.ALL_FILTER_OPTION)) {
            sqlBuilder.append("AND type = ? ");
            params.add(type);
        }
        if (source != null && !source.trim().isEmpty()) {
            sqlBuilder.append("AND source LIKE ? ");
            params.add("%" + source.trim() + "%");
        }
        if (tagsList != null && !tagsList.isEmpty()) {
            sqlBuilder.append("AND (");
            for (int i = 0; i < tagsList.size(); i++) {
                if (tagsList.get(i) == null || tagsList.get(i).trim().isEmpty()) continue;
                sqlBuilder.append("tags LIKE ? ");
                params.add("%" + tagsList.get(i).trim() + "%");
                if (i < tagsList.size() - 1) {
                    sqlBuilder.append("OR ");
                }
            }
            // Xóa "OR " thừa nếu tag cuối cùng rỗng
            if (sqlBuilder.toString().endsWith("OR ")) {
                 sqlBuilder.setLength(sqlBuilder.length() - 3);
            }
            sqlBuilder.append(") ");
        }
        if (hasAudio != null) {
            if (hasAudio) {
                sqlBuilder.append("AND audio_path IS NOT NULL AND audio_path != '' ");
            } else {
                sqlBuilder.append("AND (audio_path IS NULL OR audio_path = '') ");
            }
        }
        if (hasImage != null) {
            if (hasImage) {
                sqlBuilder.append("AND image_path IS NOT NULL AND image_path != '' ");
            } else {
                sqlBuilder.append("AND (image_path IS NULL OR image_path = '') ");
            }
        }
        sqlBuilder.append("ORDER BY id DESC");

        logger.debug("Executing advanced search query: {} with params: {}", sqlBuilder.toString(), params);
        List<Question> questions = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    questions.add(mapResultSetToQuestion(rs));
                }
            }
            logger.info("Advanced search found {} questions.", questions.size());
        } catch (SQLException e) {
            logger.error("Error during advanced question search: {}", e.getMessage(), e);
            throw e;
        }
        return questions;
    }


    /**
     * Lấy danh sách các giá trị duy nhất của một cột (dùng cho ComboBox lọc).
     *
     * @param columnName Tên cột (ví dụ: "level", "type", "source").
     * @return Danh sách các giá trị duy nhất.
     * @throws SQLException Nếu có lỗi truy cập cơ sở dữ liệu.
     */
    public List<String> getDistinctValues(String columnName) throws SQLException {
        List<String> values = new ArrayList<>();
        // Validate columnName để tránh SQL Injection cơ bản
        if (!columnName.matches("^[a-zA-Z0-9_]+$")) {
            logger.error("Invalid column name for distinct query: {}", columnName);
            throw new IllegalArgumentException("Invalid column name for distinct query: " + columnName);
        }
        String sql = "SELECT DISTINCT " + columnName + " FROM Questions WHERE " + columnName + " IS NOT NULL AND " + columnName + " != '' ORDER BY " + columnName + " ASC";
        logger.debug("Executing SQL: {}", sql);
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                values.add(rs.getString(1));
            }
            logger.info("Retrieved {} distinct values for column: {}", values.size(), columnName);
        } catch (SQLException e) {
            logger.error("Error getting distinct values for column {}: {}", columnName, e.getMessage(), e);
            throw e;
        }
        return values;
    }

    // Helper method to set PreparedStatement parameters for a Question object
    private void setPreparedStatementParametersForQuestion(PreparedStatement pstmt, Question question) throws SQLException {
        pstmt.setString(1, question.getQuestionText());
        pstmt.setString(2, question.getOptionA());
        pstmt.setString(3, question.getOptionB());
        pstmt.setString(4, question.getOptionC());
        pstmt.setString(5, question.getOptionD());
        pstmt.setString(6, question.getCorrectAnswer());
        pstmt.setString(7, question.getExplanation());
        pstmt.setString(8, question.getLevel());
        pstmt.setString(9, question.getType());
        pstmt.setString(10, question.getAudioPath());
        pstmt.setString(11, question.getImagePath());
        pstmt.setString(12, question.getTags());
        pstmt.setString(13, question.getSource());
    }

    // Public để ExamQuestionDAO có thể sử dụng
    public Question mapResultSetToQuestion(ResultSet rs) throws SQLException {
        Question question = new Question();
        question.setId(rs.getInt("id"));
        question.setQuestionText(rs.getString("question_text"));
        question.setOptionA(rs.getString("option_a"));
        question.setOptionB(rs.getString("option_b"));
        question.setOptionC(rs.getString("option_c"));
        question.setOptionD(rs.getString("option_d"));
        question.setCorrectAnswer(rs.getString("correct_answer"));
        question.setExplanation(rs.getString("explanation"));
        question.setLevel(rs.getString("level"));
        question.setType(rs.getString("type"));
        question.setAudioPath(rs.getString("audio_path"));
        question.setImagePath(rs.getString("image_path"));
        question.setTags(rs.getString("tags"));
        question.setSource(rs.getString("source"));
        question.setCreatedAt(rs.getTimestamp("created_at"));
        question.setUpdatedAt(rs.getTimestamp("updated_at"));
        return question;
    }
}