package com.nganhangdethi.ui.filters;

import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;
import javax.swing.border.TitledBorder;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class ExamFilterPanel extends JPanel {
    private JComboBox<String> levelTargetComboBox;
    // Bạn có thể thêm các filter khác cho Exam nếu cần (ví dụ: theo ngày tạo, người tạo...)
    private ActionListener filterChangeListener;

    public ExamFilterPanel(ActionListener changeListener) {
        this.filterChangeListener = changeListener;
        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createEtchedBorder(), 
            "Lọc đề thi", 
            TitledBorder.LEFT, 
            TitledBorder.TOP, 
            UIUtils.getJapaneseFont(Font.ITALIC, 13f)
        ));

        initComponents();
        if (this.filterChangeListener != null) {
            levelTargetComboBox.addActionListener(this.filterChangeListener);
        }
    }

    private void initComponents() {
        JLabel levelLabel = new JLabel("Cấp độ mục tiêu:");
        levelTargetComboBox = new JComboBox<>(Constants.QUESTION_LEVELS); // Dùng chung mảng levels

        add(levelLabel);
        add(levelTargetComboBox);
        
        UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(13f));
    }

    public String getSelectedLevelTarget() {
        if (levelTargetComboBox.getSelectedItem() == null) return Constants.ALL_FILTER_OPTION;
        return (String) levelTargetComboBox.getSelectedItem();
    }

    public void resetFilters() {
        levelTargetComboBox.setSelectedItem(Constants.ALL_FILTER_OPTION);
    }
}