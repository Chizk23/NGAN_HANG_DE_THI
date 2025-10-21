package com.nganhangdethi.model;

import java.util.Objects;

public class ExamQuestion {
    private int examId;
    private int questionId;
    private int orderInExam;

    // (Tùy chọn) Có thể thêm đối tượng Question nếu muốn lấy cả thông tin câu hỏi
    // private Question questionObject; // Đổi tên để tránh nhầm lẫn với thuộc tính questionId

    public ExamQuestion() {
    }

    public ExamQuestion(int examId, int questionId, int orderInExam) {
        this.examId = examId;
        this.questionId = questionId;
        this.orderInExam = orderInExam;
    }

    // Getters
    public int getExamId() { return examId; }
    public int getQuestionId() { return questionId; }
    public int getOrderInExam() { return orderInExam; }

    // Setters
    public void setExamId(int examId) { this.examId = examId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }
    public void setOrderInExam(int orderInExam) { this.orderInExam = orderInExam; }

    @Override
    public String toString() {
        return "ExamQuestion{" +
                "examId=" + examId +
                ", questionId=" + questionId +
                ", orderInExam=" + orderInExam +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExamQuestion that = (ExamQuestion) o;
        return examId == that.examId && questionId == that.questionId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(examId, questionId);
    }
}