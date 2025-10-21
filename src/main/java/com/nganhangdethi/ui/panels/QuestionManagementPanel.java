package com.nganhangdethi.ui.panels;

import com.nganhangdethi.model.Question;
import com.nganhangdethi.service.QuestionService;
import com.nganhangdethi.ui.components.JPlaceholderTextField;
import com.nganhangdethi.ui.dialogs.QuestionDialog;
import com.nganhangdethi.ui.dialogs.ViewQuestionDialog;
import com.nganhangdethi.ui.filters.QuestionFilterPanel;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;
import com.nganhangdethi.ui.dialogs.CreateMultipleQuestionsDialog; 
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors; // Dùng cho tags

public class QuestionManagementPanel extends JPanel {
    private JTable questionTable;
    private DefaultTableModel tableModel;
    private QuestionService questionService;
    private Frame ownerFrame; // Để truyền vào các dialog

    private JButton addButton, editButton, deleteButton, viewButton, refreshButton, searchButton;
    private QuestionFilterPanel filterPanel;
    private JPlaceholderTextField searchTextField;
    private JCheckBox hasAudioCheckBox;
    private JCheckBox hasImageCheckBox;
    private JTextField tagsSearchField; // Tìm kiếm theo tags


    private TableRowSorter<DefaultTableModel> sorter;

    public QuestionManagementPanel(Frame owner) {
        this.ownerFrame = owner;
        this.questionService = new QuestionService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadQuestions(); // Tải câu hỏi khi panel được tạo
    }

    private void initComponents() {
        // Panel Top: Chứa FilterPanel và SearchPanel
        JPanel topPanelContainer = new JPanel(new BorderLayout(0,5)); // khoảng cách dọc 5px

        // Filter Panel
        filterPanel = new QuestionFilterPanel(e -> performSearchAndFilter()); // Action listener cho filter
        topPanelContainer.add(filterPanel, BorderLayout.NORTH);

        // Search Panel: Chứa các trường tìm kiếm khác
        JPanel searchFieldsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcSearch = new GridBagConstraints();
        gbcSearch.insets = new Insets(2, 5, 2, 5);
        gbcSearch.anchor = GridBagConstraints.WEST;

        gbcSearch.gridx = 0; gbcSearch.gridy = 0;
        searchTextField = new JPlaceholderTextField("Tìm nội dung, giải thích, ID...", 25);
        searchTextField.addActionListener(e -> performSearchAndFilter());
        searchFieldsPanel.add(searchTextField, gbcSearch);

        gbcSearch.gridx = 1; gbcSearch.gridy = 0;
        tagsSearchField = new JPlaceholderTextField("Tìm tags (vd: tag1,tag2)", 20);
        tagsSearchField.addActionListener(e -> performSearchAndFilter());
        searchFieldsPanel.add(tagsSearchField, gbcSearch);
        
        gbcSearch.gridx = 2; gbcSearch.gridy = 0;
        hasAudioCheckBox = new JCheckBox("Có Audio");
        hasAudioCheckBox.addActionListener(e -> performSearchAndFilter());
        searchFieldsPanel.add(hasAudioCheckBox, gbcSearch);

        gbcSearch.gridx = 3; gbcSearch.gridy = 0;
        hasImageCheckBox = new JCheckBox("Có Hình ảnh");
        hasImageCheckBox.addActionListener(e -> performSearchAndFilter());
        searchFieldsPanel.add(hasImageCheckBox, gbcSearch);
        
        gbcSearch.gridx = 4; gbcSearch.gridy = 0; gbcSearch.weightx = 0.1; gbcSearch.fill = GridBagConstraints.HORIZONTAL;
        searchButton = new JButton("Tìm kiếm", UIUtils.createImageIcon(Constants.ICON_PATH_SEARCH, "Tìm kiếm", 16, 16));
        searchButton.setToolTipText("Thực hiện tìm kiếm và lọc câu hỏi");
        searchButton.addActionListener(e -> performSearchAndFilter());
        searchFieldsPanel.add(searchButton, gbcSearch);
        gbcSearch.weightx = 0; gbcSearch.fill = GridBagConstraints.NONE;


        topPanelContainer.add(searchFieldsPanel, BorderLayout.CENTER);
        add(topPanelContainer, BorderLayout.NORTH);

        // Bảng hiển thị câu hỏi
        String[] columnNames = {
                Constants.COL_ID, Constants.COL_QUESTION_SUMMARY, Constants.COL_LEVEL,
                Constants.COL_TYPE, Constants.COL_CORRECT_ANSWER, Constants.COL_TAGS, "Audio", "Image"
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        questionTable = new JTable(tableModel);
        questionTable.setRowHeight(28); // Tăng chiều cao hàng
        questionTable.getTableHeader().setFont(UIUtils.getJapaneseFont(Font.BOLD, 14f));
        questionTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionTable.setAutoCreateRowSorter(true); // Cho phép sắp xếp cột mặc định
        sorter = (TableRowSorter<DefaultTableModel>) questionTable.getRowSorter();


        // Thiết lập độ rộng cột
        TableColumnModel columnModel = questionTable.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(40);  columnModel.getColumn(0).setMaxWidth(70);   // ID
        columnModel.getColumn(1).setPreferredWidth(350); // Nội dung
        columnModel.getColumn(2).setPreferredWidth(70);                                             // Cấp độ
        columnModel.getColumn(3).setPreferredWidth(90);                                             // Loại
        columnModel.getColumn(4).setPreferredWidth(60);                                             // Đáp án
        columnModel.getColumn(5).setPreferredWidth(150);                                            // Tags
        columnModel.getColumn(6).setPreferredWidth(50);  columnModel.getColumn(6).setMaxWidth(70);   // Audio
        columnModel.getColumn(7).setPreferredWidth(50);  columnModel.getColumn(7).setMaxWidth(70);   // Image


        JScrollPane scrollPane = new JScrollPane(questionTable);
        add(scrollPane, BorderLayout.CENTER);

        // Double click để xem chi tiết
        questionTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && questionTable.getSelectedRow() != -1) {
                    viewSelectedQuestion();
                }
            }
        });

        // Panel nút chức năng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        addButton = new JButton("Thêm", UIUtils.createImageIcon(Constants.ICON_PATH_ADD, "Thêm câu hỏi mới", 16, 16));
        editButton = new JButton("Sửa", UIUtils.createImageIcon(Constants.ICON_PATH_EDIT, "Sửa câu hỏi đã chọn", 16, 16));
        deleteButton = new JButton("Xóa", UIUtils.createImageIcon(Constants.ICON_PATH_DELETE, "Xóa câu hỏi đã chọn", 16, 16));
        viewButton = new JButton("Xem", UIUtils.createImageIcon(Constants.ICON_PATH_VIEW, "Xem chi tiết câu hỏi", 16, 16));
        refreshButton = new JButton("Tải lại", UIUtils.createImageIcon(Constants.ICON_PATH_REFRESH, "Tải lại danh sách câu hỏi", 16, 16));

        addButton.setToolTipText("Thêm một câu hỏi mới vào ngân hàng");
        editButton.setToolTipText("Sửa thông tin câu hỏi đã chọn");
        deleteButton.setToolTipText("Xóa câu hỏi đã chọn khỏi ngân hàng");
        viewButton.setToolTipText("Xem chi tiết thông tin câu hỏi đã chọn");
        refreshButton.setToolTipText("Tải lại toàn bộ danh sách câu hỏi từ cơ sở dữ liệu");

        addButton.addActionListener(e -> addQuestion());
        editButton.addActionListener(e -> editSelectedQuestion());
        deleteButton.addActionListener(e -> deleteSelectedQuestion());
        viewButton.addActionListener(e -> viewSelectedQuestion());
        refreshButton.addActionListener(e -> loadQuestions());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        // Áp dụng font cho các component trong panel này
        UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(13f));
        questionTable.setFont(UIUtils.getJapaneseFont(13f)); // Đảm bảo font cho bảng
        questionTable.getTableHeader().setFont(UIUtils.getJapaneseFont(Font.BOLD, 13f));
    }

    private void loadQuestions() {
        // Reset các bộ lọc về trạng thái mặc định khi tải lại toàn bộ
        searchTextField.setText(""); // Xóa text và hiện placeholder
        tagsSearchField.setText("");
        filterPanel.resetFilters(); // Cần thêm phương thức này trong QuestionFilterPanel
        hasAudioCheckBox.setSelected(false);
        hasImageCheckBox.setSelected(false);
        
        List<Question> questions = questionService.getAllQuestions();
        displayQuestions(questions);
        UIUtils.showInformationMessage(ownerFrame, "Đã tải " + questions.size() + " câu hỏi.");
    }

    private void performSearchAndFilter() {
        String keyword = searchTextField.getActualText().trim(); // Lấy text thực sự, bỏ qua placeholder
        String level = filterPanel.getSelectedLevel();
        String type = filterPanel.getSelectedType();
        
        String tagsInput = tagsSearchField.getText().trim();
        List<String> tagsList = null;
        if (!tagsInput.isEmpty()) {
            tagsList = Arrays.stream(tagsInput.split(","))
                             .map(String::trim)
                             .filter(tag -> !tag.isEmpty())
                             .collect(Collectors.toList());
        }

        Boolean hasAudio = hasAudioCheckBox.isSelected() ? Boolean.TRUE : null; // null nếu không check
        Boolean hasImage = hasImageCheckBox.isSelected() ? Boolean.TRUE : null; // null nếu không check


        // Nếu không có tiêu chí nào, có thể gọi loadQuestions() hoặc getAllQuestions()
        if (keyword.isEmpty() && Constants.ALL_FILTER_OPTION.equals(level) &&
            Constants.ALL_FILTER_OPTION.equals(type) && (tagsList == null || tagsList.isEmpty()) &&
            hasAudio == null && hasImage == null) {
            // Không nên gọi loadQuestions() vì nó reset filter, gây vòng lặp.
            // Thay vào đó, có thể hiện getAll trực tiếp hoặc không làm gì nếu người dùng xóa hết tiêu chí
             List<Question> allQuestions = questionService.getAllQuestions();
             displayQuestions(allQuestions);
             return;
        }

        List<Question> results = questionService.searchQuestions(keyword, level, type, tagsList, null, hasAudio, hasImage);
        displayQuestions(results);
        // UIUtils.showInformationMessage(ownerFrame, "Tìm thấy " + results.size() + " câu hỏi.");
    }


    private void displayQuestions(List<Question> questions) {
        tableModel.setRowCount(0); // Xóa dữ liệu cũ
        if (questions != null) {
            for (Question q : questions) {
                Vector<Object> row = new Vector<>();
                row.add(q.getId());
                String summary = q.getQuestionText();
                if (summary != null && summary.length() > 80) {
                    summary = summary.substring(0, 77) + "...";
                }
                row.add(summary);
                row.add(q.getLevel());
                row.add(q.getType());
                row.add(q.getCorrectAnswer());
                row.add(q.getTags() != null ? q.getTags() : "");
                row.add(q.getAudioPath() != null && !q.getAudioPath().isEmpty() ? "Có" : "Không");
                row.add(q.getImagePath() != null && !q.getImagePath().isEmpty() ? "Có" : "Không");
                tableModel.addRow(row);
            }
        }
    }

    private Question getSelectedQuestionFromTableModel() {
        int selectedViewRow = questionTable.getSelectedRow();
        if (selectedViewRow >= 0) {
            int modelRow = questionTable.convertRowIndexToModel(selectedViewRow);
            int questionId = (int) tableModel.getValueAt(modelRow, 0); // Giả sử cột ID là cột 0
            return questionService.getQuestionById(questionId);
        }
        return null;
    }

 // SỬA PHƯƠNG THỨC addQuestion()
    private void addQuestion() {
        String[] options = {"Tạo một câu hỏi", "Tạo nhiều câu hỏi từ File Ảnh", "Hủy"};
        int choice = JOptionPane.showOptionDialog(ownerFrame,
                "Chọn phương thức tạo câu hỏi:",
                "Thêm Câu Hỏi Mới",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null, // Không dùng icon tùy chỉnh
                options,
                options[0]);

        if (choice == JOptionPane.YES_OPTION) { // Tạo một câu hỏi
            QuestionDialog dialog = new QuestionDialog(ownerFrame, "Thêm Câu Hỏi Mới (Thủ công)", null);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                performSearchAndFilter();
            }
        } else if (choice == JOptionPane.NO_OPTION) { // Tạo nhiều câu hỏi từ File
            CreateMultipleQuestionsDialog multiDialog = new CreateMultipleQuestionsDialog(ownerFrame);
            multiDialog.setVisible(true);
            // Sau khi multiDialog đóng, nó sẽ tự xử lý việc thêm vào DB.
            // Chúng ta cần refresh lại bảng ở đây.
            performSearchAndFilter(); // Hoặc loadQuestions() nếu muốn reset hoàn toàn filter
        }
    }  

    private void editSelectedQuestion() {
        Question selectedQuestion = getSelectedQuestionFromTableModel();
        if (selectedQuestion != null) {
            QuestionDialog dialog = new QuestionDialog(ownerFrame, "Sửa Câu Hỏi (ID: " + selectedQuestion.getId() + ")", selectedQuestion);
            dialog.setVisible(true);
            if (dialog.isSaved()) {
                performSearchAndFilter();
            }
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_EDIT);
        }
    }

    private void viewSelectedQuestion() {
        Question selectedQuestion = getSelectedQuestionFromTableModel();
        if (selectedQuestion != null) {
            ViewQuestionDialog dialog = new ViewQuestionDialog(ownerFrame, selectedQuestion);
            dialog.setVisible(true);
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_VIEW);
        }
    }

    private void deleteSelectedQuestion() {
        Question selectedQuestion = getSelectedQuestionFromTableModel();
        if (selectedQuestion != null) {
            String questionSummary = selectedQuestion.getQuestionText();
            if (questionSummary.length() > 50) questionSummary = questionSummary.substring(0, 47) + "...";

            int choice = UIUtils.showConfirmDialog(ownerFrame,
                    Constants.MSG_CONFIRM_DELETE_QUESTION + "\n\nID: " + selectedQuestion.getId() + "\nNội dung: " + questionSummary,
                    Constants.MSG_CONFIRM_DELETE_TITLE);

            if (choice == JOptionPane.YES_OPTION) {
                boolean success = questionService.deleteQuestionAndAssociatedFiles(selectedQuestion.getId());
                if (success) {
                    UIUtils.showInformationMessage(ownerFrame, Constants.MSG_DELETE_SUCCESS);
                    performSearchAndFilter();
                } else {
                    UIUtils.showErrorMessage(ownerFrame, Constants.MSG_DELETE_FAIL);
                }
            }
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_DELETE);
        }
    }
}