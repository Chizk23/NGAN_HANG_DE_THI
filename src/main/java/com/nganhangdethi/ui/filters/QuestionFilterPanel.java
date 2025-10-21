package com.nganhangdethi.ui.filters;

import com.nganhangdethi.service.QuestionService;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class QuestionFilterPanel extends JPanel {
    private JComboBox<String> levelComboBox;
    private JComboBox<String> typeComboBox;
    private QuestionService questionService;

    public QuestionFilterPanel(ActionListener filterAction) {
        this.questionService = new QuestionService();

        setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(),
                "Bộ lọc câu hỏi",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                UIUtils.getJapaneseFont(Font.ITALIC, 13f)
        ));

        initComponents();

        if (filterAction != null) {
            levelComboBox.addActionListener(filterAction);
            typeComboBox.addActionListener(filterAction);
        }
    }

    private void initComponents() {
        Font font = UIUtils.getJapaneseFont(13f);

        JLabel levelLabel = new JLabel("Cấp độ:");
        levelLabel.setFont(font);
        levelComboBox = new JComboBox<>(Constants.QUESTION_LEVELS);
        levelComboBox.setFont(font);

        JLabel typeLabel = new JLabel("Loại:");
        typeLabel.setFont(font);
        typeComboBox = new JComboBox<>(Constants.QUESTION_TYPES);
        typeComboBox.setFont(font);

        add(levelLabel);
        add(levelComboBox);
        add(Box.createHorizontalStrut(15)); // khoảng cách
        add(typeLabel);
        add(typeComboBox);

        UIUtils.setFontRecursively(this, font); // Đảm bảo tất cả component con đều được gán font
    }

    public String getSelectedLevel() {
        if (levelComboBox.getSelectedItem() == null)
            return Constants.ALL_FILTER_OPTION;
        return (String) levelComboBox.getSelectedItem();
    }

    public String getSelectedType() {
        if (typeComboBox.getSelectedItem() == null)
            return Constants.ALL_FILTER_OPTION;
        return (String) typeComboBox.getSelectedItem();
    }

    public void resetFilters() {
        levelComboBox.setSelectedItem(Constants.ALL_FILTER_OPTION);
        typeComboBox.setSelectedItem(Constants.ALL_FILTER_OPTION);
    }

    // Nếu muốn load dữ liệu từ database thay vì Constants
    public void loadDistinctLevels() {
        List<String> levels = questionService.getDistinctLevels();
        levelComboBox.removeAllItems();
        levelComboBox.addItem(Constants.ALL_FILTER_OPTION);
        for (String level : levels) {
            levelComboBox.addItem(level);
        }
    }

    public void loadDistinctTypes() {
        List<String> types = questionService.getDistinctTypes();
        typeComboBox.removeAllItems();
        typeComboBox.addItem(Constants.ALL_FILTER_OPTION);
        for (String type : types) {
            typeComboBox.addItem(type);
        }
    }
}