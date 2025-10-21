package com.nganhangdethi.model;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections; // For unmodifiableList
import java.util.List;
import java.util.Objects;

public class Exam {
    private int examId;
    private String examName;
    private String description;
    private String levelTarget;
    private Timestamp createdAt;
    private List<Question> questions;

    public Exam() {
        // Luôn khởi tạo danh sách để tránh NullPointerException sau này
        this.questions = new ArrayList<>();
    }

    public Exam(String examName, String description, String levelTarget) {
        this(); // Gọi constructor mặc định để khởi tạo 'questions'
        this.examName = examName;
        this.description = description;
        this.levelTarget = levelTarget;
    }

    // Getters
    public int getExamId() { return examId; }
    public String getExamName() { return examName; }
    public String getDescription() { return description; }
    public String getLevelTarget() { return levelTarget; }
    public Timestamp getCreatedAt() { return createdAt; }

    /**
     * Trả về danh sách các câu hỏi.
     * Cân nhắc: Trả về một bản sao hoặc một danh sách không thể sửa đổi
     * nếu bạn muốn ngăn chặn việc thay đổi danh sách từ bên ngoài đối tượng Exam.
     * Ví dụ:
     * - return Collections.unmodifiableList(this.questions); // Không thể sửa đổi
     * - return new ArrayList<>(this.questions); // Trả về một bản sao có thể sửa đổi
     */
    public List<Question> getQuestions() {
        return questions; // Hiện tại: trả về tham chiếu trực tiếp
    }

    // Setters
    public void setExamId(int examId) { this.examId = examId; }
    public void setExamName(String examName) { this.examName = examName; }
    public void setDescription(String description) { this.description = description; }
    public void setLevelTarget(String levelTarget) { this.levelTarget = levelTarget; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    /**
     * Gán danh sách câu hỏi cho đề thi.
     * Cân nhắc: Tạo một bản sao của danh sách được truyền vào để đảm bảo tính đóng gói.
     * Ví dụ:
     * this.questions = (questions != null) ? new ArrayList<>(questions) : new ArrayList<>();
     */
    public void setQuestions(List<Question> questions) {
        this.questions = questions;
        // Đảm bảo this.questions không bao giờ là null sau khi gán
        if (this.questions == null) {
            this.questions = new ArrayList<>();
        }
    }

    // Tiện ích để thêm câu hỏi vào danh sách
    public void addQuestion(Question question) {
        // questions đã được đảm bảo không null trong constructor và setQuestions
        if (question != null) { // Chỉ thêm nếu câu hỏi không null
            this.questions.add(question);
        }
    }

    // Tiện ích để xóa câu hỏi khỏi danh sách
    public void removeQuestion(Question question) {
        if (question != null) {
            this.questions.remove(question);
        }
    }


    @Override
    public String toString() {
        return "Exam{" +
                "examId=" + examId +
                ", examName='" + examName + '\'' +
                ", levelTarget='" + levelTarget + '\'' +
                ", numberOfQuestions=" + (questions != null ? questions.size() : 0) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exam exam = (Exam) o;
        return examId == exam.examId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(examId);
    }
}