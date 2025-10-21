package com.nganhangdethi.ui.dialogs;

import com.nganhangdethi.model.Exam;
import com.nganhangdethi.model.Question;
import com.nganhangdethi.service.ExamService; // Có thể không cần trực tiếp nếu Exam đã có đủ thông tin
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.DateUtils;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.util.List;
import java.util.Vector;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent; 

public class ViewExamDialog extends JDialog {
    private Exam examToView;
    // private ExamService examService; // Không cần thiết nếu Exam object truyền vào đã có đủ chi tiết

    private JTable questionsInExamTable;
    private DefaultTableModel questionsTableModel;

    public ViewExamDialog(Frame owner, Exam exam) {
        super(owner, "Chi Tiết Đề Thi: " + exam.getExamName() + " (ID: " + exam.getExamId() + ")", true); // Modal
        this.examToView = exam;
        // this.examService = new ExamService(); // Không cần nếu exam đã có questions

        // Đảm bảo examToView có danh sách câu hỏi.
        // Nếu không, bạn có thể cần load lại ở đây hoặc đảm bảo nó được load trước khi gọi dialog.
        if (this.examToView.getQuestions() == null) {
            // Tạm thời, nếu không có câu hỏi, sẽ hiển thị danh sách rỗng.
            // Lý tưởng nhất, Exam object truyền vào nên được lấy từ examService.getExamWithDetails().
            System.err.println("WARN: ViewExamDialog - Exam object does not contain questions list. Displaying empty list.");
            this.examToView.setQuestions(new java.util.ArrayList<>()); // Khởi tạo list rỗng
        }

        initComponents();
        populateExamDetails(); // Điền thông tin đề thi
        populateQuestionsTable(); // Điền danh sách câu hỏi

        pack(); // Điều chỉnh kích thước cho vừa nội dung
        setMinimumSize(new Dimension(Constants.VIEW_EXAM_DIALOG_WIDTH - 100, Constants.VIEW_EXAM_DIALOG_HEIGHT - 200));
        setSize(Constants.VIEW_EXAM_DIALOG_WIDTH, Constants.VIEW_EXAM_DIALOG_HEIGHT);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        // Panel chứa thông tin cơ bản của Đề Thi
        JPanel examInfoPanel = new JPanel();
        examInfoPanel.setLayout(new GridBagLayout());
        examInfoPanel.setBorder(BorderFactory.createTitledBorder("Thông tin chung của Đề Thi"));
        GridBagConstraints gbcInfo = new GridBagConstraints();
        gbcInfo.insets = new Insets(5, 5, 5, 5);
        gbcInfo.anchor = GridBagConstraints.WEST;
        gbcInfo.fill = GridBagConstraints.HORIZONTAL;

        // Panel chứa bảng danh sách câu hỏi
        JPanel questionsListPanel = new JPanel(new BorderLayout());
        questionsListPanel.setBorder(BorderFactory.createTitledBorder("Các câu hỏi trong đề thi"));

        // --- Điền thông tin đề thi ---
        // (Sẽ được thực hiện trong populateExamDetails() và thêm vào examInfoPanel)

        // --- Bảng câu hỏi ---
        String[] columnNames = {Constants.COL_ORDER_IN_EXAM, Constants.COL_ID, Constants.COL_QUESTION_SUMMARY, Constants.COL_LEVEL, Constants.COL_TYPE};
        questionsTableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Read-only table
            }
        };
        questionsInExamTable = new JTable(questionsTableModel);
        questionsInExamTable.setRowHeight(25);
        questionsInExamTable.getTableHeader().setFont(UIUtils.getJapaneseFont(Font.BOLD, 13f));
        questionsInExamTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION); // Cho phép chọn 1 dòng

        // Thiết lập độ rộng cột cho bảng câu hỏi
        TableColumnModel qcm = questionsInExamTable.getColumnModel();
        qcm.getColumn(0).setPreferredWidth(40); qcm.getColumn(0).setMaxWidth(60);   // STT
        qcm.getColumn(1).setPreferredWidth(40); qcm.getColumn(1).setMaxWidth(70);   // ID Câu hỏi
        qcm.getColumn(2).setPreferredWidth(350);                                    // Nội dung
        qcm.getColumn(3).setPreferredWidth(80);                                     // Cấp độ
        qcm.getColumn(4).setPreferredWidth(100);                                    // Loại

        questionsListPanel.add(new JScrollPane(questionsInExamTable), BorderLayout.CENTER);
     // ---- THÊM MOUSE LISTENER CHO BẢNG CÂU HỎI ----
        questionsInExamTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) { // Double-click
                    int selectedRowInView = questionsInExamTable.getSelectedRow();
                    if (selectedRowInView != -1) {
                        // Chuyển đổi chỉ số dòng từ view sang model (quan trọng nếu có sort/filter trên bảng)
                        // Tuy nhiên, trong dialog này, bảng không có sort/filter động từ UI,
                        // và dữ liệu được thêm vào theo thứ tự của examToView.getQuestions().
                        // Nên modelRow có thể sẽ giống viewRow nếu không có TableRowSorter.
                        // Để an toàn, nếu bạn có ý định thêm sorter sau này:
                        // int modelRow = questionsInExamTable.convertRowIndexToModel(selectedRowInView);
                        // Nếu không có sorter, modelRow = selectedRowInView
                        int modelRow = selectedRowInView; // Giả sử không có sorter động trên bảng này

                        if (modelRow >= 0 && modelRow < examToView.getQuestions().size()) {
                            Question selectedQuestion = examToView.getQuestions().get(modelRow);
                            ViewQuestionDialog viewQuestionDialog = new ViewQuestionDialog((Frame) SwingUtilities.getWindowAncestor(ViewExamDialog.this), selectedQuestion);
                            viewQuestionDialog.setVisible(true);
                        }
                    }
                }
            }
        });

        // Nút Đóng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JButton closeButton = new JButton("Đóng", UIUtils.createImageIcon(Constants.ICON_PATH_CANCEL, "Đóng", 16,16));
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        // Sử dụng JSplitPane để chia khu vực thông tin và danh sách câu hỏi
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(examInfoPanel), questionsListPanel);
        splitPane.setResizeWeight(0.35); // Thông tin đề thi chiếm ít không gian hơn
        splitPane.setBorder(null); // Bỏ border của JSplitPane

        add(splitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Áp dụng font
        UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(13f)); // Áp dụng font chung
        closeButton.setFont(UIUtils.getJapaneseFont(Font.BOLD, 13f));
        // Font cho header bảng đã được set ở trên
    }

    private void addReadOnlyFieldToPanel(JPanel panel, GridBagConstraints gbc, String labelText, String valueText, int yPos) {
        gbc.gridx = 0; gbc.gridy = yPos; gbc.weightx = 0.25; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.EAST;
        JLabel label = new JLabel(labelText);
        panel.add(label, gbc);

        gbc.gridx = 1; gbc.gridy = yPos; gbc.weightx = 0.75; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;
        JTextField textField = new JTextField(valueText != null ? valueText : "");
        textField.setEditable(false);
        textField.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 2));
        textField.setBackground(UIManager.getColor("Panel.background")); // Nền giống panel
        panel.add(textField, gbc);
    }

    private void addReadOnlyTextAreaToPanel(JPanel panel, GridBagConstraints gbc, String labelText, String valueText, int yPos, int rows) {
        gbc.gridx = 0; gbc.gridy = yPos; gbc.weightx = 0.25; gbc.fill = GridBagConstraints.NONE; gbc.anchor = GridBagConstraints.NORTHEAST;
        JLabel label = new JLabel(labelText);
        panel.add(label, gbc);

        gbc.gridx = 1; gbc.gridy = yPos; gbc.weightx = 0.75; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.1 * rows;
        JTextArea textArea = new JTextArea(valueText != null ? valueText : "", rows, 20); // Giảm cột mặc định
        textArea.setEditable(false);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setBackground(UIManager.getColor("Panel.background"));
        textArea.setBorder(BorderFactory.createEmptyBorder(2,5,2,2));
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setMinimumSize(new Dimension(100, rows * 20));
        panel.add(scrollPane, gbc);
        gbc.weighty = 0; // Reset weighty
    }

    private void populateExamDetails() {
        // Panel chứa thông tin sẽ được lấy từ splitPane
        JScrollPane scrollPane = (JScrollPane) ((JSplitPane) getContentPane().getComponent(0)).getLeftComponent();
        JPanel examInfoPanel = (JPanel) scrollPane.getViewport().getView();
        GridBagConstraints gbc = new GridBagConstraints(); // Lấy gbc đã dùng để add
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;


        int yPos = 0;
        addReadOnlyFieldToPanel(examInfoPanel, gbc, "ID Đề Thi:", String.valueOf(examToView.getExamId()), yPos++);
        addReadOnlyFieldToPanel(examInfoPanel, gbc, "Tên Đề Thi:", examToView.getExamName(), yPos++);
        addReadOnlyFieldToPanel(examInfoPanel, gbc, "Cấp Độ Mục Tiêu:", examToView.getLevelTarget() != null ? examToView.getLevelTarget() : "-", yPos++);
        addReadOnlyTextAreaToPanel(examInfoPanel, gbc, "Mô Tả:", examToView.getDescription() != null ? examToView.getDescription() : "-", yPos++, 3);
        addReadOnlyFieldToPanel(examInfoPanel, gbc, "Ngày Tạo:", examToView.getCreatedAt() != null ? DateUtils.formatDateTime(examToView.getCreatedAt()) : "N/A", yPos++);
        addReadOnlyFieldToPanel(examInfoPanel, gbc, "Số Lượng Câu Hỏi:", String.valueOf(examToView.getQuestions() != null ? examToView.getQuestions().size() : 0), yPos++);
    }

    private void populateQuestionsTable() {
        questionsTableModel.setRowCount(0); // Xóa dữ liệu cũ
        List<Question> questions = examToView.getQuestions();
        if (questions != null && !questions.isEmpty()) {
            int order = 1;
            for (Question q : questions) {
                Vector<Object> row = new Vector<>();
                row.add(order++);
                row.add(q.getId());
                String summary = q.getQuestionText();
                if (summary != null && summary.length() > 70) { // Rút gọn nội dung
                    summary = summary.substring(0, 67) + "...";
                }
                row.add(summary != null ? summary : "");
                row.add(q.getLevel());
                row.add(q.getType());
                questionsTableModel.addRow(row);
            }
        } else {
            // (Tùy chọn) Hiển thị thông báo nếu không có câu hỏi
            // Vector<Object> row = new Vector<>();
            // row.add("-"); row.add("-"); row.add("Đề thi này chưa có câu hỏi nào."); row.add("-"); row.add("-");
            // questionsTableModel.addRow(row);
        }
    }
}