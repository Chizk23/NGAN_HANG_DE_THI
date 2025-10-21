package com.nganhangdethi.model;

import java.sql.Timestamp;
import java.util.Objects;

public class Question {
    private int id;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer; // 'A', 'B', 'C', hoặc 'D'
    private String explanation;
    private String level;         // "N1", "N2", ...
    private String type;          // "Kanji", "Goi", "Bunpou", "Dokkai", "Choukai"
    private String audioPath;
    private String imagePath;
    private String tags;          // Có thể là chuỗi các tags cách nhau bằng dấu phẩy
    private String source;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public Question() {
    }

    public Question(String questionText, String optionA, String optionB, String optionC, String optionD,
                    String correctAnswer, String explanation, String level, String type) {
        this.questionText = questionText;
        this.optionA = optionA;
        this.optionB = optionB;
        this.optionC = optionC;
        this.optionD = optionD;
        this.correctAnswer = correctAnswer;
        this.explanation = explanation;
        this.level = level;
        this.type = type;
    }

    // Getters
    public int getId() { return id; }
    public String getQuestionText() { return questionText; }
    public String getOptionA() { return optionA; }
    public String getOptionB() { return optionB; }
    public String getOptionC() { return optionC; }
    public String getOptionD() { return optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public String getExplanation() { return explanation; }
    public String getLevel() { return level; }
    public String getType() { return type; }
    public String getAudioPath() { return audioPath; }
    public String getImagePath() { return imagePath; }
    public String getTags() { return tags; }
    public String getSource() { return source; }
    public Timestamp getCreatedAt() { return createdAt; }
    public Timestamp getUpdatedAt() { return updatedAt; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public void setExplanation(String explanation) { this.explanation = explanation; }
    public void setLevel(String level) { this.level = level; }
    public void setType(String type) { this.type = type; }
    public void setAudioPath(String audioPath) { this.audioPath = audioPath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }
    public void setTags(String tags) { this.tags = tags; }
    public void setSource(String source) { this.source = source; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        String textSummary = "N/A"; // Giá trị mặc định nếu questionText là null
        if (questionText != null) {
            textSummary = questionText.substring(0, Math.min(questionText.length(), 50));
            if (questionText.length() > 50) {
                textSummary += "...";
            }
        }
        return "Question{" +
                "id=" + id +
                ", questionText='" + textSummary + '\'' +
                ", level='" + level + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Question question = (Question) o;
        return id == question.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}