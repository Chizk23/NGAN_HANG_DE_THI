package com.nganhangdethi.ui.dialogs;

import com.nganhangdethi.model.Exam;
import com.nganhangdethi.model.Question;
import com.nganhangdethi.service.ExamService;
import com.nganhangdethi.service.QuestionService;
import com.nganhangdethi.ui.components.JPlaceholderTextField;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter; // <<<< THÊM IMPORT
import java.awt.event.MouseEvent;  // <<<< THÊM IMPORT
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class ExamDialog extends JDialog {
    // ... (các biến thành viên giữ nguyên như phiên bản hoàn chỉnh trước đó) ...
    private Exam currentExam;
    private boolean isEditMode;
    private boolean saved = false;

    private JTextField examNameField;
    private JTextArea descriptionTextArea;
    private JComboBox<String> levelTargetComboBox;

    private JTable availableQuestionsTable;
    private DefaultTableModel availableQuestionsTableModel;
    private TableRowSorter<TableModel> availableQuestionsSorter;
    private JTable selectedQuestionsTable;
    private DefaultTableModel selectedQuestionsTableModel;
    private TableRowSorter<TableModel> selectedQuestionsSorter;

    private JButton addButtonToExam, removeButtonFromExam, moveUpButton, moveDownButton;
    private JPlaceholderTextField searchAvailableQuestionField;
    private JComboBox<String> filterAvailableLevelComboBox, filterAvailableTypeComboBox;

    private ExamService examService;
    private QuestionService questionService; // Cần để lấy chi tiết câu hỏi nếu cần

    private final List<Question> allSystemQuestionsCache = new ArrayList<>();
    private final List<Question> currentSelectedExamQuestions = new ArrayList<>();

    public ExamDialog(Frame owner, String title, Exam examToEdit) {
        super(owner, title, true);
        System.out.println("DEBUG (ExamDialog): Constructor - Initializing...");
        this.currentExam = examToEdit;
        this.isEditMode = (examToEdit != null);
        this.examService = new ExamService();
        this.questionService = new QuestionService(); // Khởi tạo QuestionService

        initComponents();
        System.out.println("DEBUG (ExamDialog): Constructor - UI Components initialized.");

        loadAllSystemQuestions();

        if (isEditMode && currentExam != null) {
            System.out.println("DEBUG (ExamDialog): Constructor - Edit mode for Exam ID: " + currentExam.getExamId());
            populateExamFields();
            if (currentExam.getQuestions() != null) {
                currentExam.getQuestions().forEach(q -> {
                    if (q != null && currentSelectedExamQuestions.stream().noneMatch(selQ -> selQ.getId() == q.getId())) {
                        currentSelectedExamQuestions.add(q);
                    }
                });
            }
            refreshSelectedQuestionsTable();
        }

        pack();
        setMinimumSize(new Dimension(Constants.EXAM_DIALOG_WIDTH, Constants.EXAM_DIALOG_HEIGHT - 50));
        setSize(Constants.EXAM_DIALOG_WIDTH + 200, Constants.EXAM_DIALOG_HEIGHT + 100);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        System.out.println("DEBUG (ExamDialog): Constructor - Setup complete.");
    }

    private void initComponents() {
        // ... (phần initComponents giữ nguyên như phiên bản hoàn chỉnh trước đó) ...
        // Chỉ cần đảm bảo bạn gọi addDoubleClickListenersToTables() ở cuối initComponents
        // hoặc sau khi các bảng được tạo trong createQuestionSelectionPanel()

        setLayout(new BorderLayout(10, 10));
        if (getContentPane() instanceof JPanel) {
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        }

        JPanel examInfoPanel = createExamInfoPanel();
        JPanel questionSelectionPanel = createQuestionSelectionPanel(); // Bảng được tạo ở đây

        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, examInfoPanel, questionSelectionPanel);
        mainSplitPane.setResizeWeight(0.28);
        mainSplitPane.setBorder(null);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        Icon saveIcon = UIUtils.createImageIcon(Constants.ICON_PATH_SAVE, "Lưu", 16,16);
        JButton saveButton = new JButton("Lưu Đề Thi", saveIcon);
        saveButton.addActionListener(e -> saveExam());

        Icon cancelIcon = UIUtils.createImageIcon(Constants.ICON_PATH_CANCEL, "Hủy", 16,16);
        JButton cancelButton = new JButton("Hủy", cancelIcon);
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(mainSplitPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        Font baseFont = UIUtils.getJapaneseFont();
        if (baseFont != null) {
            UIUtils.setFontRecursively(examInfoPanel, baseFont.deriveFont(13f));
            saveButton.setFont(baseFont.deriveFont(Font.BOLD, 13f));
            cancelButton.setFont(baseFont.deriveFont(13f));
        } else {
             System.err.println("WARN (ExamDialog): initComponents - Japanese font not available.");
        }

        // Gọi sau khi các bảng đã được khởi tạo trong createQuestionSelectionPanel()
        addDoubleClickListenersToTables();
    }


    private JPanel createQuestionSelectionPanel() {
        // ... (Nội dung của createQuestionSelectionPanel giữ nguyên như phiên bản có GridBagLayout) ...
        // Quan trọng là availableQuestionsTable và selectedQuestionsTable được khởi tạo ở đây.
        JPanel mainQuestionSelectionPanel = new JPanel(new GridBagLayout());
        TitledBorder selectionTitle = BorderFactory.createTitledBorder("Chọn câu hỏi cho đề thi");
        Font panelTitleFont = UIUtils.getJapaneseFont();
        if (panelTitleFont != null) {
            selectionTitle.setTitleFont(panelTitleFont.deriveFont(Font.ITALIC, 13f));
        }
        mainQuestionSelectionPanel.setBorder(selectionTitle);

        JPanel leftPanel = new JPanel(new BorderLayout(0, 5));
        TitledBorder availableTitle = BorderFactory.createTitledBorder("Ngân hàng câu hỏi");
        if (panelTitleFont != null) {
            availableTitle.setTitleFont(panelTitleFont.deriveFont(Font.PLAIN, 12f));
        }
        leftPanel.setBorder(availableTitle);

        JPanel availableFilterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        availableFilterPanel.add(new JLabel("Cấp độ:"));
        filterAvailableLevelComboBox = new JComboBox<>(Constants.QUESTION_LEVELS);
        availableFilterPanel.add(filterAvailableLevelComboBox);
        availableFilterPanel.add(Box.createHorizontalStrut(5));
        availableFilterPanel.add(new JLabel("Loại:"));
        filterAvailableTypeComboBox = new JComboBox<>(Constants.QUESTION_TYPES);
        availableFilterPanel.add(filterAvailableTypeComboBox);
        availableFilterPanel.add(Box.createHorizontalStrut(5));
        availableFilterPanel.add(new JLabel("Tìm ID/Nội dung:"));
        searchAvailableQuestionField = new JPlaceholderTextField("Nhập ID hoặc từ khóa...", 15);
        availableFilterPanel.add(searchAvailableQuestionField);
        Icon searchIcon = UIUtils.createImageIcon(Constants.ICON_PATH_SEARCH, "Lọc", 12,12);
        JButton applyFilterBtn = new JButton("Lọc/Tìm", searchIcon);
        applyFilterBtn.setMargin(new Insets(2, 5, 2, 5));
        availableFilterPanel.add(applyFilterBtn);

        ActionListener filterListener = e -> filterAndRefreshAvailableQuestionsTable();
        filterAvailableLevelComboBox.addActionListener(filterListener);
        filterAvailableTypeComboBox.addActionListener(filterListener);
        searchAvailableQuestionField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    filterAndRefreshAvailableQuestionsTable();
                }
            }
        });
        applyFilterBtn.addActionListener(filterListener);
        leftPanel.add(availableFilterPanel, BorderLayout.NORTH);

        String[] colNames = {"ID", "Nội dung (tóm tắt)", "Cấp độ", "Loại"};
        availableQuestionsTableModel = new DefaultTableModel(colNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        availableQuestionsTable = new JTable(availableQuestionsTableModel); // << KHỞI TẠO BẢNG
        availableQuestionsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        availableQuestionsSorter = new TableRowSorter<>(availableQuestionsTableModel);
        availableQuestionsTable.setRowSorter(availableQuestionsSorter);
        JScrollPane availableScrollPane = new JScrollPane(availableQuestionsTable);
        leftPanel.add(availableScrollPane, BorderLayout.CENTER);

        JPanel rightPanel = new JPanel(new BorderLayout());
        TitledBorder selectedPanelTitle = BorderFactory.createTitledBorder("Câu hỏi đã chọn cho đề");
        if (panelTitleFont != null) {
            selectedPanelTitle.setTitleFont(panelTitleFont.deriveFont(Font.PLAIN, 12f));
        }
        rightPanel.setBorder(selectedPanelTitle);

        selectedQuestionsTableModel = new DefaultTableModel(colNames, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        selectedQuestionsTable = new JTable(selectedQuestionsTableModel); // << KHỞI TẠO BẢNG
        selectedQuestionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selectedQuestionsSorter = new TableRowSorter<>(selectedQuestionsTableModel);
        selectedQuestionsTable.setRowSorter(selectedQuestionsSorter);
        JScrollPane selectedScrollPane = new JScrollPane(selectedQuestionsTable);
        rightPanel.add(selectedScrollPane, BorderLayout.CENTER);

        JPanel middleControls = new JPanel();
        middleControls.setLayout(new BoxLayout(middleControls, BoxLayout.Y_AXIS));
        middleControls.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        Icon addIcon = UIUtils.createImageIcon(Constants.ICON_PATH_ADD_TO_LIST, "Thêm", 24, 24);
        addButtonToExam = new JButton(addIcon);
        if (addIcon == null) addButtonToExam.setText(">>");
        addButtonToExam.setToolTipText("Thêm câu hỏi vào đề (>>)");

        Icon removeIcon = UIUtils.createImageIcon(Constants.ICON_PATH_REMOVE_FROM_LIST, "Bỏ", 24, 24);
        removeButtonFromExam = new JButton(removeIcon);
        if (removeIcon == null) removeButtonFromExam.setText("<<");
        removeButtonFromExam.setToolTipText("Bỏ câu hỏi khỏi đề (<<)");

        Icon moveUpIcon = UIUtils.createImageIcon(Constants.ICON_PATH_MOVE_UP, "Lên", 24, 24);
        moveUpButton = new JButton(moveUpIcon);
        if (moveUpIcon == null) moveUpButton.setText("↑");
        moveUpButton.setToolTipText("Di chuyển câu hỏi lên (↑)");

        Icon moveDownIcon = UIUtils.createImageIcon(Constants.ICON_PATH_MOVE_DOWN, "Xuống", 24, 24);
        moveDownButton = new JButton(moveDownIcon);
        if (moveDownIcon == null) moveDownButton.setText("↓");
        moveDownButton.setToolTipText("Di chuyển câu hỏi xuống (↓)");

        Dimension controlButtonSize = new Dimension(70, 35);
        List<JButton> controlButtons = Arrays.asList(addButtonToExam, removeButtonFromExam, moveUpButton, moveDownButton);
        for (JButton btn : controlButtons) {
            btn.setPreferredSize(controlButtonSize);
            btn.setMaximumSize(controlButtonSize);
            btn.setAlignmentX(Component.CENTER_ALIGNMENT);
        }

        middleControls.add(Box.createVerticalGlue());
        middleControls.add(addButtonToExam);
        middleControls.add(Box.createRigidArea(new Dimension(0, 10)));
        middleControls.add(removeButtonFromExam);
        middleControls.add(Box.createRigidArea(new Dimension(0, 30)));
        middleControls.add(moveUpButton);
        middleControls.add(Box.createRigidArea(new Dimension(0, 10)));
        middleControls.add(moveDownButton);
        middleControls.add(Box.createVerticalGlue());

        addButtonToExam.addActionListener(e -> transferQuestions(availableQuestionsTable, availableQuestionsTableModel, currentSelectedExamQuestions, true));
        removeButtonFromExam.addActionListener(e -> transferQuestions(selectedQuestionsTable, selectedQuestionsTableModel, currentSelectedExamQuestions, false));
        moveUpButton.addActionListener(e -> moveQuestionInSelection(-1));
        moveDownButton.addActionListener(e -> moveQuestionInSelection(1));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.475; gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH; gbc.insets = new Insets(0, 0, 0, 5);
        mainQuestionSelectionPanel.add(leftPanel, gbc);

        gbc.gridx = 1; gbc.weightx = 0.05;  gbc.fill = GridBagConstraints.VERTICAL;
        gbc.anchor = GridBagConstraints.CENTER; gbc.insets = new Insets(0, 0, 0, 0);
        mainQuestionSelectionPanel.add(middleControls, gbc);

        gbc.gridx = 2; gbc.weightx = 0.475; gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(0, 5, 0, 0);
        mainQuestionSelectionPanel.add(rightPanel, gbc);

        if (panelTitleFont != null) {
            UIUtils.setFontRecursively(availableFilterPanel, panelTitleFont.deriveFont(12f));
            applyFilterBtn.setFont(panelTitleFont.deriveFont(Font.BOLD, 11f));
            Font tableHeaderFont = panelTitleFont.deriveFont(Font.BOLD, 12f);
            Font tableFont = panelTitleFont.deriveFont(12f);
            if(availableQuestionsTable.getTableHeader() != null) availableQuestionsTable.getTableHeader().setFont(tableHeaderFont);
            if(selectedQuestionsTable.getTableHeader() != null) selectedQuestionsTable.getTableHeader().setFont(tableHeaderFont);
            availableQuestionsTable.setFont(tableFont);
            selectedQuestionsTable.setFont(tableFont);
        }
        return mainQuestionSelectionPanel;

    }

    // ---- PHƯƠNG THỨC MỚI ĐỂ THÊM LISTENER ----
    private void addDoubleClickListenersToTables() {
        MouseAdapter viewQuestionListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    JTable sourceTable = (JTable) e.getSource();
                    int selectedRowInView = sourceTable.getSelectedRow();
                    if (selectedRowInView != -1) {
                        int modelRow = sourceTable.convertRowIndexToModel(selectedRowInView);
                        // Lấy ID câu hỏi từ model của bảng
                        int questionId = (int) sourceTable.getModel().getValueAt(modelRow, 0); // Giả sử cột 0 là ID

                        // Tìm Question object đầy đủ từ cache hoặc danh sách đã chọn
                        Question questionToView = findQuestionById(questionId, sourceTable == availableQuestionsTable);
                        
                        if (questionToView != null) {
                            // Lấy chi tiết đầy đủ của câu hỏi nếu cần (ví dụ: từ QuestionService)
                            // Giả sử Question object trong cache/list đã đủ thông tin cho ViewQuestionDialog
                            // Hoặc bạn có thể thêm một bước gọi questionService.getQuestionById(questionId)
                            // nếu các đối tượng trong cache/list chỉ là tóm tắt.
                            // Hiện tại, giả định questionToView đã đủ thông tin.

                            ViewQuestionDialog viewDialog = new ViewQuestionDialog(
                                    (Frame) SwingUtilities.getWindowAncestor(ExamDialog.this),
                                    questionToView
                            );
                            viewDialog.setVisible(true);
                        } else {
                            System.err.println("WARN (ExamDialog): Không tìm thấy câu hỏi với ID " + questionId + " để xem chi tiết.");
                            UIUtils.showWarningMessage(ExamDialog.this, "Không thể tìm thấy chi tiết câu hỏi đã chọn.");
                        }
                    }
                }
            }
        };

        availableQuestionsTable.addMouseListener(viewQuestionListener);
        selectedQuestionsTable.addMouseListener(viewQuestionListener);
    }

    // ---- PHƯƠNG THỨC MỚI ĐỂ TÌM QUESTION OBJECT ----
    private Question findQuestionById(int questionId, boolean fromAvailableCache) {
        if (fromAvailableCache) {
            synchronized (allSystemQuestionsCache) {
                return allSystemQuestionsCache.stream()
                        .filter(q -> q != null && q.getId() == questionId)
                        .findFirst()
                        .orElse(null);
            }
        } else { // from selectedQuestionsList
            synchronized (currentSelectedExamQuestions) {
                return currentSelectedExamQuestions.stream()
                        .filter(q -> q != null && q.getId() == questionId)
                        .findFirst()
                        .orElse(null);
            }
        }
    }


    // ... (loadAllSystemQuestions, showLoadingMessageInAvailableTable giữ nguyên) ...
    // ... (filterAndRefreshAvailableQuestionsTable giữ nguyên với bản sửa lỗi placeholder) ...
    // ... (refreshAvailableQuestionsTable, refreshSelectedQuestionsTable giữ nguyên) ...
    // ... (transferQuestions, moveQuestionInSelection giữ nguyên) ...
    // ... (populateExamFields, validateInput, saveExam, isSaved giữ nguyên) ...

    // Các phương thức cũ hơn (chỉ để bạn so sánh, đã được cập nhật ở trên)
    private JPanel createExamInfoPanel() { // Giữ nguyên như trước
        JPanel panel = new JPanel(new GridBagLayout());
        TitledBorder examInfoTitle = BorderFactory.createTitledBorder("Thông tin đề thi");
        Font baseFont = UIUtils.getJapaneseFont();
        if (baseFont != null) {
            examInfoTitle.setTitleFont(baseFont.deriveFont(Font.ITALIC, 13f));
        }
        panel.setBorder(examInfoTitle);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(new JLabel("Tên đề thi (*):"), gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        examNameField = new JTextField(30); panel.add(examNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;
        panel.add(new JLabel("Cấp độ mục tiêu (*):"), gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        levelTargetComboBox = new JComboBox<>(Arrays.copyOfRange(Constants.QUESTION_LEVELS, 1, Constants.QUESTION_LEVELS.length));
        panel.add(levelTargetComboBox, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.anchor = GridBagConstraints.NORTHEAST; panel.add(new JLabel("Mô tả:"), gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 1.0;
        descriptionTextArea = new JTextArea(3, 30);
        descriptionTextArea.setLineWrap(true);
        descriptionTextArea.setWrapStyleWord(true);
        JScrollPane descScrollPane = new JScrollPane(descriptionTextArea);
        panel.add(descScrollPane, gbc);
        return panel;
    }

    private void loadAllSystemQuestions() { // Giữ nguyên bản dùng SwingWorker
        System.out.println("DEBUG (ExamDialog): loadAllSystemQuestions - Starting SwingWorker...");
        SwingUtilities.invokeLater(this::showLoadingMessageInAvailableTable);

        SwingWorker<List<Question>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<Question> doInBackground() throws Exception {
                System.out.println("DEBUG (ExamDialog): loadAllSystemQuestions - Worker: Calling questionService.getAllQuestions()...");
                return questionService.getAllQuestions();
            }

            @Override
            protected void done() {
                System.out.println("DEBUG (ExamDialog): loadAllSystemQuestions - Worker done() entered.");
                try {
                    synchronized (allSystemQuestionsCache) {
                        allSystemQuestionsCache.clear();
                        List<Question> loadedQuestions = get();
                        if (loadedQuestions != null) {
                            allSystemQuestionsCache.addAll(loadedQuestions);
                            System.out.println("DEBUG (ExamDialog): loadAllSystemQuestions - Worker: Cache updated. Size: " + allSystemQuestionsCache.size());
                        } else {
                            System.err.println("ERROR (ExamDialog): loadAllSystemQuestions - Worker: questionService.getAllQuestions() returned null.");
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("ERROR (ExamDialog): loadAllSystemQuestions - Worker: Loading interrupted: " + e.getMessage());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("ERROR (ExamDialog): loadAllSystemQuestions - Worker: ExecutionException: " + cause.getMessage());
                    if (cause != e) cause.printStackTrace();
                } catch (Exception e) {
                    System.err.println("ERROR (ExamDialog): loadAllSystemQuestions - Worker: Unexpected exception in done(): " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    System.out.println("DEBUG (ExamDialog): loadAllSystemQuestions - Worker done() finally: Triggering table refresh.");
                    SwingUtilities.invokeLater(ExamDialog.this::filterAndRefreshAvailableQuestionsTable);
                }
            }
        };
        worker.execute();
    }
    
    private void showLoadingMessageInAvailableTable() { // Giữ nguyên
        if (availableQuestionsTableModel == null) {
            System.err.println("WARN (ExamDialog): showLoadingMessage - availableQuestionsTableModel is null!");
            return;
        }
        availableQuestionsTableModel.setRowCount(0);
        Vector<Object> loadingRow = new Vector<>();
        loadingRow.add(""); 
        loadingRow.add("Đang tải danh sách câu hỏi, vui lòng chờ..."); 
        loadingRow.add(""); 
        loadingRow.add(""); 
        availableQuestionsTableModel.addRow(loadingRow);
    }

    private void filterAndRefreshAvailableQuestionsTable() { // Giữ nguyên bản đã sửa placeholder
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::filterAndRefreshAvailableQuestionsTable);
            return;
        }

        if (availableQuestionsTableModel == null || filterAvailableLevelComboBox == null || 
            filterAvailableTypeComboBox == null || searchAvailableQuestionField == null) {
            System.out.println("DEBUG (ExamDialog): filterAndRefresh - UI Components not fully initialized. Skipping.");
            return;
        }

        String levelFilter = (String) filterAvailableLevelComboBox.getSelectedItem();
        String typeFilter = (String) filterAvailableTypeComboBox.getSelectedItem();

        String actualSearchText = searchAvailableQuestionField.getActualText().trim();
        String placeholderText = "Nhập ID hoặc từ khóa..."; // Cần khớp với placeholder thực tế

        String keyword;
        if (actualSearchText.isEmpty() || actualSearchText.equalsIgnoreCase(placeholderText)) {
            keyword = ""; 
        } else {
            keyword = actualSearchText.toLowerCase();
        }

        System.out.println("DEBUG (ExamDialog): filterAndRefresh - Filters: Level=" + levelFilter + 
                           ", Type=" + typeFilter + ", Effective Keyword='" + keyword + "'");

        List<Question> displayList;
        synchronized (allSystemQuestionsCache) {
             System.out.println("DEBUG (ExamDialog): filterAndRefresh - Cache size: " + allSystemQuestionsCache.size() +
                               ", Selected Qs: " + currentSelectedExamQuestions.size());
            displayList = allSystemQuestionsCache.stream()
                    .filter(q -> q != null) 
                    .filter(q -> currentSelectedExamQuestions.stream() 
                                    .noneMatch(selQ -> selQ != null && selQ.getId() == q.getId()))
                    .filter(q -> Constants.ALL_FILTER_OPTION.equals(levelFilter) || 
                                 (q.getLevel() != null && q.getLevel().equals(levelFilter)))
                    .filter(q -> Constants.ALL_FILTER_OPTION.equals(typeFilter) || 
                                 (q.getType() != null && q.getType().equals(typeFilter)))
                    .filter(q -> keyword.isEmpty() ||
                                 String.valueOf(q.getId()).toLowerCase().contains(keyword) || 
                                 (q.getQuestionText() != null && q.getQuestionText().toLowerCase().contains(keyword)))
                    .collect(Collectors.toList());
        }
        
        System.out.println("DEBUG (ExamDialog): filterAndRefresh - Display list size for available table: " + displayList.size());
        refreshAvailableQuestionsTable(displayList);
    }

    private void refreshAvailableQuestionsTable(List<Question> questionsToDisplay) { // Giữ nguyên
        if (availableQuestionsTableModel == null) return;
        availableQuestionsTableModel.setRowCount(0);

        if (questionsToDisplay == null || questionsToDisplay.isEmpty()) {
            String message = "Không có câu hỏi nào phù hợp với bộ lọc.";
            boolean isCacheEmpty;
            synchronized(allSystemQuestionsCache) {
                isCacheEmpty = allSystemQuestionsCache.isEmpty();
            }
            if (isCacheEmpty &&
                (searchAvailableQuestionField == null || searchAvailableQuestionField.getActualText().trim().isEmpty() ||
                 (searchAvailableQuestionField.getPlaceholder() != null && searchAvailableQuestionField.getActualText().trim().equalsIgnoreCase(searchAvailableQuestionField.getPlaceholder().trim())) ) && 
                (filterAvailableLevelComboBox == null || Constants.ALL_FILTER_OPTION.equals(filterAvailableLevelComboBox.getSelectedItem())) &&
                (filterAvailableTypeComboBox == null || Constants.ALL_FILTER_OPTION.equals(filterAvailableTypeComboBox.getSelectedItem())) ) {
                message = "Ngân hàng câu hỏi trống hoặc không thể tải.";
            }
            availableQuestionsTableModel.addRow(new Object[]{"", message, "", ""});
            System.out.println("DEBUG (ExamDialog): Refreshed availableQuestionsTable with message: " + message);
        } else {
            for (Question q : questionsToDisplay) {
                Vector<Object> row = new Vector<>();
                row.add(q.getId());
                String summary = q.getQuestionText();
                if (summary != null && summary.length() > 50) summary = summary.substring(0, 47) + "...";
                else if (summary == null) summary = "(Nội dung trống)"; 
                row.add(summary);
                row.add(q.getLevel() != null ? q.getLevel() : "-"); 
                row.add(q.getType() != null ? q.getType() : "-");   
                availableQuestionsTableModel.addRow(row);
            }
            System.out.println("DEBUG (ExamDialog): Refreshed availableQuestionsTable with " + availableQuestionsTableModel.getRowCount() + " data rows.");
        }
    }

    private void refreshSelectedQuestionsTable() { // Giữ nguyên
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::refreshSelectedQuestionsTable);
            return;
        }
        if (selectedQuestionsTableModel == null) return;
        selectedQuestionsTableModel.setRowCount(0);
        
        currentSelectedExamQuestions.forEach(q -> { 
            if (q == null) return; 
            Vector<Object> row = new Vector<>();
            row.add(q.getId());
            String summary = q.getQuestionText();
            if (summary != null && summary.length() > 50) summary = summary.substring(0, 47) + "...";
            else if (summary == null) summary = "(Nội dung trống)";
            row.add(summary);
            row.add(q.getLevel() != null ? q.getLevel() : "-");
            row.add(q.getType() != null ? q.getType() : "-");
            selectedQuestionsTableModel.addRow(row);
        });
        System.out.println("DEBUG (ExamDialog): Refreshed selectedQuestionsTable with " + selectedQuestionsTableModel.getRowCount() + " rows.");
        
        filterAndRefreshAvailableQuestionsTable(); 
    }
    
    private void transferQuestions(JTable sourceTable, DefaultTableModel sourceModel, List<Question> destinationList, boolean isAddingToSelected) { // Giữ nguyên
        int[] selectedViewRows = sourceTable.getSelectedRows();
        if (selectedViewRows.length == 0) {
            UIUtils.showWarningMessage(this, "Vui lòng chọn ít nhất một câu hỏi để chuyển.");
            return;
        }

        List<Integer> idsToTransfer = new ArrayList<>();
        for (int viewRow : selectedViewRows) {
            int modelRow = sourceTable.convertRowIndexToModel(viewRow);
            if (modelRow >= 0 && modelRow < sourceModel.getRowCount()) {
                Object idObj = sourceModel.getValueAt(modelRow, 0);
                 if(idObj instanceof Integer) {
                    idsToTransfer.add((Integer) idObj);
                } else if (idObj instanceof String && !((String)idObj).trim().isEmpty()){
                    try { 
                        idsToTransfer.add(Integer.parseInt(((String)idObj).trim()));
                    } catch (NumberFormatException nfe) {
                        System.out.println("DEBUG (ExamDialog): transferQuestions - Skipped non-integer ID (String): '" + idObj + "'");
                    }
                } else {
                     System.out.println("DEBUG (ExamDialog): transferQuestions - Skipped empty or non-convertible ID: '" + idObj + "'");
                }
            }
        }
        
        if (idsToTransfer.isEmpty()) {
            UIUtils.showWarningMessage(this, "Các dòng đã chọn không chứa ID câu hỏi hợp lệ.");
            return;
        }
        System.out.println("DEBUG (ExamDialog): transferQuestions - IDs to transfer: " + idsToTransfer);

        List<Question> questionsFoundForTransfer = new ArrayList<>();
        List<Question> sourceListForLookup = isAddingToSelected ? allSystemQuestionsCache : currentSelectedExamQuestions;
        
        Object sourceLock = isAddingToSelected ? allSystemQuestionsCache : currentSelectedExamQuestions;

        synchronized(sourceLock) {
             if (isAddingToSelected && allSystemQuestionsCache.isEmpty()){ 
                System.err.println("WARN (ExamDialog): transferQuestions - Adding from empty cache.");
             }
            for (int id : idsToTransfer) {
                sourceListForLookup.stream()
                    .filter(q -> q != null && q.getId() == id)
                    .findFirst()
                    .ifPresent(questionsFoundForTransfer::add);
            }
        }

        if (isAddingToSelected) {
            questionsFoundForTransfer.forEach(qToAdd -> {
                synchronized(currentSelectedExamQuestions) { 
                    if (currentSelectedExamQuestions.stream().noneMatch(selQ -> selQ != null && selQ.getId() == qToAdd.getId())) {
                        currentSelectedExamQuestions.add(qToAdd);
                    }
                }
            });
        } else { 
            synchronized(destinationList) {
                destinationList.removeAll(questionsFoundForTransfer);
            }
        }
        refreshSelectedQuestionsTable(); 
    }

    private void moveQuestionInSelection(int direction) { // Giữ nguyên
        int selectedViewRow = selectedQuestionsTable.getSelectedRow();
        if (selectedViewRow < 0) {
            UIUtils.showWarningMessage(this, "Vui lòng chọn một câu hỏi trong danh sách đã chọn để di chuyển.");
            return;
        }
        int modelRow = selectedQuestionsTable.convertRowIndexToModel(selectedViewRow);
        int targetModelRow = -1; 

        synchronized (currentSelectedExamQuestions) {
            if (modelRow < 0 || modelRow >= currentSelectedExamQuestions.size()) {
                System.err.println("WARN (ExamDialog): moveQuestionInSelection - Invalid modelRow: " + modelRow);
                return;
            }
            targetModelRow = modelRow + direction; 
            if (targetModelRow >= 0 && targetModelRow < currentSelectedExamQuestions.size()) {
                Collections.swap(currentSelectedExamQuestions, modelRow, targetModelRow);
            } else {
                return;  
            }
        }
        
        refreshSelectedQuestionsTable(); 
        
        int newViewRow = selectedQuestionsTable.convertRowIndexToView(targetModelRow); 
        if(newViewRow != -1) {
            selectedQuestionsTable.setRowSelectionInterval(newViewRow, newViewRow);
            selectedQuestionsTable.scrollRectToVisible(selectedQuestionsTable.getCellRect(newViewRow, 0, true));
        }
    }

    private void populateExamFields() { // Giữ nguyên
        if (currentExam == null) return;
        examNameField.setText(currentExam.getExamName());
        descriptionTextArea.setText(currentExam.getDescription());
        levelTargetComboBox.setSelectedItem(currentExam.getLevelTarget());
    }

    private boolean validateInput() { // Giữ nguyên
        if (examNameField.getText().trim().isEmpty()) {
            UIUtils.showErrorMessage(this, "Tên đề thi không được để trống.");
            examNameField.requestFocusInWindow();
            return false;
        }
        if (levelTargetComboBox.getSelectedItem() == null || 
            Constants.ALL_FILTER_OPTION.equals(levelTargetComboBox.getSelectedItem())) {
            UIUtils.showErrorMessage(this, "Vui lòng chọn một cấp độ mục tiêu hợp lệ cho đề thi.");
            levelTargetComboBox.requestFocusInWindow();
            return false;
        }
        synchronized (currentSelectedExamQuestions) {
            if (currentSelectedExamQuestions.isEmpty()){
                int choice = UIUtils.showConfirmDialog(this, Constants.MSG_CONFIRM_SAVE_EMPTY_EXAM, Constants.MSG_CONFIRM_TITLE);
                return choice == JOptionPane.YES_OPTION;
            }
        }
        return true;
    }

    private void saveExam() { // Giữ nguyên bản dùng SwingWorker
        if (!validateInput()) {
            return;
        }
        String examName = examNameField.getText().trim();
        String description = descriptionTextArea.getText().trim();
        String levelTarget = (String) levelTargetComboBox.getSelectedItem();

        final JDialog waitingDialog = new JDialog(this, "Đang lưu...", Dialog.ModalityType.APPLICATION_MODAL);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        waitingDialog.setLayout(new BorderLayout(10,10));
        if (waitingDialog.getContentPane() instanceof JPanel) {
             ((JPanel)waitingDialog.getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        }
        waitingDialog.add(BorderLayout.CENTER, progressBar);
        waitingDialog.add(BorderLayout.NORTH, new JLabel("Vui lòng chờ, dữ liệu đang được xử lý...", SwingConstants.CENTER));
        waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        waitingDialog.setSize(350, 100);
        waitingDialog.setLocationRelativeTo(this);


        SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                System.out.println("DEBUG (ExamDialog): saveExam - Worker: Saving. isEditMode: " + isEditMode);
                List<Question> questionsToSave;
                synchronized(currentSelectedExamQuestions) {
                    questionsToSave = new ArrayList<>(currentSelectedExamQuestions); 
                }

                if (isEditMode) {
                    if (currentExam == null) { 
                        System.err.println("ERROR (ExamDialog): saveExam - Worker: currentExam is null in edit mode!");
                        return false;  
                    }
                    currentExam.setExamName(examName);
                    currentExam.setDescription(description);
                    currentExam.setLevelTarget(levelTarget);
                    return examService.updateExamWithQuestions(currentExam, questionsToSave);
                } else {
                    Exam newExam = new Exam(examName, description, levelTarget); 
                    return examService.createExamWithQuestions(newExam, questionsToSave);
                }
            }

            @Override
            protected void done() {
                System.out.println("DEBUG (ExamDialog): saveExam - Worker done() entered.");
                SwingUtilities.invokeLater(waitingDialog::dispose);
                
                try {
                    boolean success = get();
                    if (success) {
                        saved = true;
                        UIUtils.showInformationMessage(ExamDialog.this, isEditMode ? Constants.MSG_UPDATE_SUCCESS : Constants.MSG_SAVE_SUCCESS);
                        ExamDialog.this.dispose();
                    } else {
                        UIUtils.showErrorMessage(ExamDialog.this, isEditMode ? Constants.MSG_UPDATE_FAIL : Constants.MSG_SAVE_FAIL);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println("ERROR (ExamDialog): saveExam - Worker: Save interrupted: " + e.getMessage());
                    UIUtils.showErrorMessage(ExamDialog.this, "Lưu bị gián đoạn.");
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    System.err.println("ERROR (ExamDialog): saveExam - Worker: ExecutionException: " + cause.getMessage());
                    if(cause != e) cause.printStackTrace();
                    UIUtils.showErrorMessage(ExamDialog.this, "Lỗi khi lưu: " + cause.getMessage());
                } catch (Exception e) {
                     System.err.println("ERROR (ExamDialog): saveExam - Worker: Unexpected exception: " + e.getMessage());
                     e.printStackTrace();
                     UIUtils.showErrorMessage(ExamDialog.this, "Lỗi không mong muốn khi lưu: " + e.getMessage());
                }
            }
        };
        worker.execute();
        if (worker.getState() != SwingWorker.StateValue.DONE) { 
            waitingDialog.setVisible(true);
        }
    }

    public boolean isSaved() { // Giữ nguyên
        return saved;
    }
}