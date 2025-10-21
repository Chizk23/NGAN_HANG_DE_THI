package com.nganhangdethi.ui.tablemodels; // Tạo package này nếu chưa có

import com.nganhangdethi.model.Question;
import com.nganhangdethi.util.Constants;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;

public class AIGeneratedQuestionTableModel extends AbstractTableModel {

    private final List<QuestionWrapper> questions;
    private final String[] columnNames = {
            Constants.COL_SELECT, Constants.COL_QUESTION_SUMMARY, Constants.COL_LEVEL,
            Constants.COL_TYPE, Constants.COL_CORRECT_ANSWER,
            Constants.COL_EDIT_ACTION, Constants.COL_DELETE_ACTION
    };

    public AIGeneratedQuestionTableModel() {
        this.questions = new ArrayList<>();
    }

    public void setQuestions(List<Question> newQuestions) {
        this.questions.clear();
        if (newQuestions != null) {
            for (Question q : newQuestions) {
                this.questions.add(new QuestionWrapper(q, true)); // Mặc định chọn tất cả
            }
        }
        fireTableDataChanged();
    }

    public List<Question> getSelectedQuestions() {
        List<Question> selected = new ArrayList<>();
        for (QuestionWrapper wrapper : questions) {
            if (wrapper.isSelected()) {
                selected.add(wrapper.getQuestion());
            }
        }
        return selected;
    }

    public Question getQuestionAt(int rowIndex) {
        return questions.get(rowIndex).getQuestion();
    }

    public void updateQuestionAt(int rowIndex, Question updatedQuestion) {
        questions.get(rowIndex).setQuestion(updatedQuestion);
        fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void removeQuestionAt(int rowIndex) {
        questions.remove(rowIndex);
        fireTableRowsDeleted(rowIndex, rowIndex);
    }
    
    public void addQuestion(Question question, boolean selected) {
        this.questions.add(new QuestionWrapper(question, selected));
        fireTableRowsInserted(this.questions.size() -1, this.questions.size() -1);
    }
    
    public List<QuestionWrapper> getAllQuestionWrappers() {
        return this.questions;
    }


    @Override
    public int getRowCount() {
        return questions.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }

    @Override
    public String getColumnName(int column) {
        return columnNames[column];
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex == 0) { // Cột "Chọn"
            return Boolean.class;
        }
        return String.class; // Các cột khác là String hoặc Object cho button
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == 0 || columnIndex == getColumnCount() - 1 || columnIndex == getColumnCount() - 2; // "Chọn", "Sửa", "Xóa"
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        QuestionWrapper wrapper = questions.get(rowIndex);
        Question q = wrapper.getQuestion();
        switch (columnIndex) {
            case 0: return wrapper.isSelected();
            case 1:
                String summary = q.getQuestionText();
                return (summary != null && summary.length() > 70) ? summary.substring(0, 67) + "..." : summary;
            case 2: return q.getLevel();
            case 3: return q.getType();
            case 4: return q.getCorrectAnswer();
            case 5: return Constants.COL_EDIT_ACTION; // Sẽ được render/edit bởi ButtonColumn
            case 6: return Constants.COL_DELETE_ACTION; // Sẽ được render/edit bởi ButtonColumn
            default: return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex == 0 && aValue instanceof Boolean) {
            questions.get(rowIndex).setSelected((Boolean) aValue);
            fireTableCellUpdated(rowIndex, columnIndex);
        }
    }

    // Lớp Wrapper nội bộ để chứa Question và trạng thái selected
    public static class QuestionWrapper {
        private Question question;
        private boolean selected;

        public QuestionWrapper(Question question, boolean selected) {
            this.question = question;
            this.selected = selected;
        }

        public Question getQuestion() { return question; }
        public void setQuestion(Question question) { this.question = question; }
        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }
}