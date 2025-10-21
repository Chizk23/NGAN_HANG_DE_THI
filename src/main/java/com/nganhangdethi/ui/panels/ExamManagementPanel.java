package com.nganhangdethi.ui.panels;

import com.nganhangdethi.model.Exam;
import com.nganhangdethi.service.ExamService;
import com.nganhangdethi.service.ExportService;
import com.nganhangdethi.ui.components.JPlaceholderTextField;
import com.nganhangdethi.ui.dialogs.ExamDialog;
import com.nganhangdethi.ui.dialogs.ViewExamDialog;
import com.nganhangdethi.ui.filters.ExamFilterPanel;
import com.nganhangdethi.util.AppConfig;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.DateUtils;
import com.nganhangdethi.util.UIUtils;
import com.nganhangdethi.model.Question; // Cần cho List<Question>

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

public class ExamManagementPanel extends JPanel {
    private JTable examTable;
    private DefaultTableModel tableModel;
    private ExamService examService;
    private ExportService exportService;
    private Frame ownerFrame;

    private JButton addButton, editButton, deleteButton, viewButton, refreshButton;
    private JButton exportPdfButton, exportDocxButton;
    private JButton generateSetButton;
    private JPlaceholderTextField searchTextField;
    private ExamFilterPanel filterPanel;

    private TableRowSorter<DefaultTableModel> sorter;

    public ExamManagementPanel(Frame owner) {
        this.ownerFrame = owner;
        this.examService = new ExamService();
        this.exportService = new ExportService();
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        initComponents();
        loadInitialExams();
    }

    private void initComponents() {
        JPanel topActionPanel = new JPanel(new BorderLayout(10, 5));

        filterPanel = new ExamFilterPanel(e -> performSearchAndFilter());
        topActionPanel.add(filterPanel, BorderLayout.WEST);

        JPanel searchPanelOuter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanelOuter.setBorder(BorderFactory.createTitledBorder("Tìm kiếm đề thi"));
        JPanel searchPanelInner = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 2));
        searchPanelInner.add(new JLabel("Tên/Mô tả:"));
        searchTextField = new JPlaceholderTextField("Nhập từ khóa...", 25); // Placeholder
        searchTextField.setToolTipText("Nhập từ khóa để tìm kiếm trong tên hoặc mô tả đề thi");
        searchTextField.addActionListener(e -> performSearchAndFilter());
        searchPanelInner.add(searchTextField);

        JButton searchBtn = new JButton("Tìm", UIUtils.createImageIcon(Constants.ICON_PATH_SEARCH, "Tìm kiếm", 16, 16));
        searchBtn.setMargin(new Insets(2,8,2,8));
        searchBtn.addActionListener(e -> performSearchAndFilter());
        searchPanelInner.add(searchBtn);
        searchPanelOuter.add(searchPanelInner);

        topActionPanel.add(searchPanelOuter, BorderLayout.CENTER);
        add(topActionPanel, BorderLayout.NORTH);

        String[] columnNames = {
                Constants.COL_ID, Constants.COL_EXAM_NAME, Constants.COL_EXAM_LEVEL_TARGET,
                Constants.COL_EXAM_DESCRIPTION_SUMMARY, Constants.COL_EXAM_NUM_QUESTIONS, Constants.COL_CREATED_AT
        };
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        examTable = new JTable(tableModel);
        examTable.setRowHeight(28);
        Font headerFont = UIUtils.getJapaneseFont();
        if (headerFont != null) examTable.getTableHeader().setFont(headerFont.deriveFont(Font.BOLD, 13f));
        examTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        sorter = new TableRowSorter<>(tableModel);
        examTable.setRowSorter(sorter);

        TableColumnModel tcm = examTable.getColumnModel();
        tcm.getColumn(0).setPreferredWidth(40); tcm.getColumn(0).setMaxWidth(60);
        tcm.getColumn(1).setPreferredWidth(280);
        tcm.getColumn(2).setPreferredWidth(100);
        tcm.getColumn(3).setPreferredWidth(300);
        tcm.getColumn(4).setPreferredWidth(80);
        tcm.getColumn(5).setPreferredWidth(140);

        JScrollPane scrollPane = new JScrollPane(examTable);
        add(scrollPane, BorderLayout.CENTER);

        examTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent mouseEvent) {
                if (mouseEvent.getClickCount() == 2 && examTable.getSelectedRow() != -1) {
                    viewSelectedExam();
                }
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        addButton = new JButton("Tạo Đề", UIUtils.createImageIcon(Constants.ICON_PATH_ADD, "Tạo đề mới", 16,16));
        editButton = new JButton("Sửa Đề", UIUtils.createImageIcon(Constants.ICON_PATH_EDIT, "Sửa đề", 16,16));
        deleteButton = new JButton("Xóa Đề", UIUtils.createImageIcon(Constants.ICON_PATH_DELETE, "Xóa đề", 16,16));
        viewButton = new JButton("Xem", UIUtils.createImageIcon(Constants.ICON_PATH_VIEW, "Xem chi tiết", 16,16));
        refreshButton = new JButton("Tải lại", UIUtils.createImageIcon(Constants.ICON_PATH_REFRESH, "Tải lại DS", 16,16));
        exportPdfButton = new JButton("Xuất PDF Đề", UIUtils.createImageIcon(Constants.ICON_PATH_EXPORT_PDF, "Xuất PDF", 16,16));
        exportDocxButton = new JButton("Xuất DOCX Đề", UIUtils.createImageIcon(Constants.ICON_PATH_EXPORT_WORD, "Xuất DOCX", 16,16));
        generateSetButton = new JButton("Bộ Đề Xáo Trộn", UIUtils.createImageIcon(Constants.ICON_PATH_SHUFFLE, "Tạo bộ đề xáo trộn", 16,16));

        addButton.setToolTipText("Tạo một đề thi mới");
        editButton.setToolTipText("Sửa thông tin đề thi đã chọn");
        deleteButton.setToolTipText("Xóa đề thi đã chọn");
        viewButton.setToolTipText("Xem chi tiết đề thi đã chọn");
        refreshButton.setToolTipText("Tải lại danh sách đề thi từ cơ sở dữ liệu");
        exportPdfButton.setToolTipText("Xuất đề thi đã chọn ra file PDF");
        exportDocxButton.setToolTipText("Xuất đề thi đã chọn ra file DOCX");
        generateSetButton.setToolTipText("Tạo nhiều phiên bản xáo trộn của đề thi đã chọn, mỗi phiên bản có đề và đáp án riêng");

        addButton.addActionListener(e -> addExam());
        editButton.addActionListener(e -> editSelectedExam());
        deleteButton.addActionListener(e -> deleteSelectedExam());
        viewButton.addActionListener(e -> viewSelectedExam());
        refreshButton.addActionListener(e -> loadInitialExams());
        exportPdfButton.addActionListener(e -> exportSingleSelectedExam("pdf"));
        exportDocxButton.addActionListener(e -> exportSingleSelectedExam("docx"));
        generateSetButton.addActionListener(e -> generateShuffledExamSet());

        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(viewButton);

        addSeparatorToButtonPanel(buttonPanel);
        buttonPanel.add(exportPdfButton);
        buttonPanel.add(exportDocxButton);
        addSeparatorToButtonPanel(buttonPanel);
        buttonPanel.add(generateSetButton);
        addSeparatorToButtonPanel(buttonPanel);
        buttonPanel.add(refreshButton);
        add(buttonPanel, BorderLayout.SOUTH);

        Font baseUIFont = UIUtils.getJapaneseFont();
        if (baseUIFont != null) {
            UIUtils.setFontRecursively(this, baseUIFont.deriveFont(13f));
            examTable.setFont(baseUIFont.deriveFont(13f));
        }
    }

    private void addSeparatorToButtonPanel(JPanel panel) {
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        Dimension buttonSize = (addButton != null && addButton.getPreferredSize() != null) ? addButton.getPreferredSize() : new Dimension(0, 25);
        sep.setPreferredSize(new Dimension(sep.getPreferredSize().width, buttonSize.height - 5));
        panel.add(Box.createHorizontalStrut(5));
        panel.add(sep);
        panel.add(Box.createHorizontalStrut(5));
    }

    private void loadInitialExams() {
        filterPanel.resetFilters();
        if (searchTextField != null) { // Đảm bảo searchTextField đã được khởi tạo
            if (searchTextField.getPlaceholder() != null && !searchTextField.getPlaceholder().isEmpty()) {
                searchTextField.reset();
            } else {
                searchTextField.setText("");
            }
        }
        performSearchAndFilter();
    }

    private void performSearchAndFilter() {
        String keyword = "";
        if (searchTextField != null) { // Đảm bảo searchTextField đã được khởi tạo
            String keywordFromField = searchTextField.getActualText().trim();
            String placeholderValue = searchTextField.getPlaceholder();
            if (!keywordFromField.isEmpty()) {
                if (placeholderValue == null || !keywordFromField.equalsIgnoreCase(placeholderValue.trim())) {
                    keyword = keywordFromField;
                }
            }
        }
        System.out.println("DEBUG: ExamManagementPanel - Effective search keyword: '" + keyword + "'");

        String level = filterPanel.getSelectedLevelTarget();
        List<Exam> results = examService.searchExams(keyword.toLowerCase(), level); // Chuyển keyword sang lowercase ở đây
        displayExams(results);
        System.out.println("INFO: ExamManagementPanel - Searched/Filtered. Found " + (results != null ? results.size() : 0) + " exams.");
    }

    private void displayExams(List<Exam> exams) {
        tableModel.setRowCount(0);
        if (exams != null) {
            for (Exam exam : exams) {
                Vector<Object> row = new Vector<>();
                row.add(exam.getExamId());
                row.add(exam.getExamName());
                row.add(exam.getLevelTarget() != null ? exam.getLevelTarget() : "-");
                String descSummary = exam.getDescription();
                if (descSummary != null && descSummary.length() > 60) {
                    descSummary = descSummary.substring(0, 57) + "...";
                }
                row.add(descSummary != null ? descSummary : "");
                row.add(exam.getQuestions());
                row.add(exam.getCreatedAt() != null ? DateUtils.formatDateTime(exam.getCreatedAt()) : "N/A");
                tableModel.addRow(row);
            }
        }
    }

    private Exam getSelectedExamFromTable(boolean fetchFullDetails) {
        int selectedViewRow = examTable.getSelectedRow();
        if (selectedViewRow < 0) {
            return null;
        }
        int modelRow = examTable.convertRowIndexToModel(selectedViewRow);
        if (modelRow < 0 || modelRow >= tableModel.getRowCount()) {
            System.err.println("Error in getSelectedExamFromTable: Invalid modelRow " + modelRow);
            return null;
        }
        Object idObject = tableModel.getValueAt(modelRow, 0);
        if (!(idObject instanceof Integer)) {
            System.err.println("Error: Exam ID in table is not an Integer: " + idObject);
            UIUtils.showErrorMessage(ownerFrame, "Lỗi dữ liệu: ID đề thi không hợp lệ.");
            return null;
        }
        int examId = (Integer) idObject;

        if (this.examService == null) {
            System.err.println("CRITICAL ERROR: examService is null in ExamManagementPanel!");
            UIUtils.showErrorMessage(ownerFrame, "Lỗi hệ thống: Dịch vụ không khả dụng.");
            return null;
        }
        try {
            return fetchFullDetails ? this.examService.getExamWithDetails(examId) : this.examService.getExamById(examId);
        } catch (Exception e) {
            System.err.println("Error fetching exam (ID: " + examId + ", FullDetails: " + fetchFullDetails + "): " + e.getMessage());
            e.printStackTrace();
            UIUtils.showErrorMessage(ownerFrame, "Lỗi khi tải thông tin đề thi: " + e.getMessage());
            return null;
        }
    }

    private void addExam() {
        ExamDialog dialog = new ExamDialog(ownerFrame, "Tạo Đề Thi Mới", null);
        dialog.setVisible(true);
        if (dialog.isSaved()) {
            loadInitialExams();
        }
    }

    private void editSelectedExam() {
        Exam selectedExamBasic = getSelectedExamFromTable(false);
        if (selectedExamBasic != null) {
            Exam examToEdit = examService.getExamWithDetails(selectedExamBasic.getExamId());
            if (examToEdit != null) {
                 ExamDialog dialog = new ExamDialog(ownerFrame, "Sửa Đề Thi (ID: " + examToEdit.getExamId() + ")", examToEdit);
                dialog.setVisible(true);
                if (dialog.isSaved()) {
                    performSearchAndFilter();
                }
            } else {
                 UIUtils.showErrorMessage(ownerFrame, "Không thể tải chi tiết đề thi để sửa.");
            }
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_EDIT);
        }
    }

    private void viewSelectedExam() {
        Exam selectedExamBasic = getSelectedExamFromTable(false);
        if (selectedExamBasic != null) {
            Exam examToView = examService.getExamWithDetails(selectedExamBasic.getExamId());
            if (examToView != null) {
                ViewExamDialog dialog = new ViewExamDialog(ownerFrame, examToView);
                dialog.setVisible(true);
            } else {
                UIUtils.showErrorMessage(ownerFrame, "Không thể tải chi tiết đề thi để xem.");
            }
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_VIEW);
        }
    }

    private void deleteSelectedExam() {
        Exam examToDelete = getSelectedExamFromTable(false);
        if (examToDelete != null) {
            int choice = UIUtils.showConfirmDialog(ownerFrame,
                    Constants.MSG_CONFIRM_DELETE_EXAM + "\n\nID: " + examToDelete.getExamId() + "\nTên: " + examToDelete.getExamName(),
                    Constants.MSG_CONFIRM_DELETE_TITLE);
            if (choice == JOptionPane.YES_OPTION) {
                boolean success = examService.deleteExam(examToDelete.getExamId());
                if (success) {
                    UIUtils.showInformationMessage(ownerFrame, Constants.MSG_DELETE_SUCCESS);
                    loadInitialExams();
                } else {
                    UIUtils.showErrorMessage(ownerFrame, Constants.MSG_DELETE_FAIL);
                }
            }
        } else {
            UIUtils.showWarningMessage(ownerFrame, Constants.MSG_NO_ITEM_SELECTED_FOR_DELETE);
        }
    }

    private void exportSingleSelectedExam(String format) {
        Exam selectedExam = getSelectedExamFromTable(true);
        if (selectedExam == null || selectedExam.getQuestions() == null || selectedExam.getQuestions().isEmpty()) {
            UIUtils.showWarningMessage(ownerFrame, selectedExam == null ? Constants.MSG_NO_ITEM_SELECTED_FOR_EXPORT : "Đề thi này không có câu hỏi để xuất.");
            return;
        }

        String defaultExportPath = AppConfig.getProperty("app.default.export.directory", FileSystemView.getFileSystemView().getDefaultDirectory().getPath());
        JFileChooser fileChooser = new JFileChooser(defaultExportPath);
        fileChooser.setDialogTitle("Lưu file đề thi (" + format.toUpperCase() + ")");

        String safeExamName = selectedExam.getExamName().replaceAll("[^a-zA-Z0-9.\\-_ ]+", "").trim().replace(" ", "_");
        if (safeExamName.isEmpty()) safeExamName = "DeThiGoc";
        String defaultFileName = safeExamName + "." + format; // Tên file đề gốc
        fileChooser.setSelectedFile(new File(defaultFileName));

        FileNameExtensionFilter nameFilter = ("pdf".equalsIgnoreCase(format)) ?
                new FileNameExtensionFilter(Constants.PDF_FILE_DESCRIPTION, Constants.PDF_EXTENSION) :
                new FileNameExtensionFilter(Constants.DOCX_FILE_DESCRIPTION, Constants.DOCX_EXTENSION);
        fileChooser.setFileFilter(nameFilter);
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showSaveDialog(ownerFrame) == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            String filePath = fileToSave.getAbsolutePath();
            if (!filePath.toLowerCase().endsWith("." + format.toLowerCase())) {
                filePath += "." + format.toLowerCase();
            }

            int includeAnswersChoice = JOptionPane.showConfirmDialog(ownerFrame,
                    "Bạn có muốn bao gồm đáp án và giải thích trực tiếp trong file đề thi này không?",
                    "Tùy chọn xuất đáp án", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            final boolean includeAnswersInExamFile = (includeAnswersChoice == JOptionPane.YES_OPTION);
            final boolean shuffleForSingleExport = false;
            final String finalFilePath = filePath;

            JDialog waitingDialog = new JDialog(ownerFrame, "Đang xuất file...", Dialog.ModalityType.APPLICATION_MODAL);
            JProgressBar progressBar = new JProgressBar();
            progressBar.setIndeterminate(true);
            JLabel statusLabel = new JLabel("Vui lòng chờ trong khi đề thi đang được tạo...");
            JPanel progressPanel = new JPanel(new BorderLayout(5,5));
            progressPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            progressPanel.add(statusLabel, BorderLayout.NORTH);
            progressPanel.add(progressBar, BorderLayout.CENTER);
            waitingDialog.add(progressPanel);
            waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            waitingDialog.setSize(400, 120);
            waitingDialog.setLocationRelativeTo(ownerFrame);

            SwingWorker<Boolean, Void> worker = new SwingWorker<>() {
                @Override
                protected Boolean doInBackground() throws Exception {
                    statusLabel.setText("Đang tạo file đề thi...");
                    if ("pdf".equalsIgnoreCase(format)) {
                        return exportService.exportExamToPdf(selectedExam, finalFilePath, shuffleForSingleExport, includeAnswersInExamFile);
                    } else if ("docx".equalsIgnoreCase(format)) {
                        return exportService.exportExamToDocx(selectedExam, finalFilePath, shuffleForSingleExport, includeAnswersInExamFile);
                    }
                    return false;
                }
                @Override
                protected void done() {
                    waitingDialog.dispose();
                    try {
                        if (get()) {
                            UIUtils.showInformationMessage(ownerFrame, Constants.MSG_EXPORT_SUCCESS + "\nĐã lưu tại: " + finalFilePath);
                            if (!includeAnswersInExamFile) {
                                int ansChoice = JOptionPane.showConfirmDialog(ownerFrame,
                                        "Bạn có muốn xuất file đáp án RIÊNG cho đề thi này không?\n(Sẽ bao gồm giải thích chi tiết)",
                                        "Xuất Đáp Án Riêng?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                                if (ansChoice == JOptionPane.YES_OPTION) {
                                    exportAnswerKey(selectedExam, format, finalFilePath, shuffleForSingleExport, 0, true); // true: includeExplanation
                                }
                            }
                        } else {
                            UIUtils.showErrorMessage(ownerFrame, Constants.MSG_EXPORT_FAIL);
                        }
                    } catch (Exception e) {
                        handleExportException(e, "Lỗi khi xuất file đề đơn lẻ");
                    }
                }
            };
            worker.execute();
            if (worker.getState() != SwingWorker.StateValue.DONE) {
                 waitingDialog.setVisible(true);
            }
        }
    }

    private void generateShuffledExamSet() {
        Exam originalExam = getSelectedExamFromTable(true);
        if (originalExam == null || originalExam.getQuestions() == null || originalExam.getQuestions().isEmpty()) {
            UIUtils.showWarningMessage(ownerFrame, "Vui lòng chọn một đề thi có câu hỏi để tạo bộ đề xáo trộn.");
            return;
        }

        String numVersionsStr = JOptionPane.showInputDialog(ownerFrame, "Nhập số lượng phiên bản đề muốn tạo (1-50):", "Số Lượng Bộ Đề", JOptionPane.PLAIN_MESSAGE);
        if (numVersionsStr == null || numVersionsStr.trim().isEmpty()) return;
        int numVersions;
        try {
            numVersions = Integer.parseInt(numVersionsStr.trim());
            if (numVersions <= 0 || numVersions > 50) {
                UIUtils.showErrorMessage(ownerFrame, "Số lượng phiên bản phải từ 1 đến 50.");
                return;
            }
        } catch (NumberFormatException e) {
            UIUtils.showErrorMessage(ownerFrame, "Số lượng phiên bản không hợp lệ.");
            return;
        }

        String[] formats = {"PDF", "DOCX"};
        String selectedFormatStr = (String) JOptionPane.showInputDialog(ownerFrame, "Chọn định dạng xuất cho bộ đề:",
                "Định Dạng File", JOptionPane.PLAIN_MESSAGE, null, formats, formats[0]);
        if (selectedFormatStr == null) return;
        final String outputFormat = selectedFormatStr.toLowerCase();

        // Với bộ đề xáo trộn, đề thi sẽ không kèm đáp án. Đáp án sẽ luôn là file riêng nếu người dùng muốn.
        final boolean includeAnswersInEachExamFile = false;

        final boolean createSeparateAnswerKey;
        final boolean includeExplanationInSeparateKey;

        int choiceSeparateKey = JOptionPane.showConfirmDialog(ownerFrame,
                "Tạo file đáp án RIÊNG cho mỗi phiên bản đề?",
                "Tùy Chọn File Đáp Án", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (choiceSeparateKey == JOptionPane.CANCEL_OPTION) return;
        createSeparateAnswerKey = (choiceSeparateKey == JOptionPane.YES_OPTION);

        if (createSeparateAnswerKey) {
             int choiceExplanation = JOptionPane.showConfirmDialog(ownerFrame,
                "File đáp án riêng có bao gồm giải thích chi tiết không?",
                "Nội Dung File Đáp Án", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            includeExplanationInSeparateKey = (choiceExplanation == JOptionPane.YES_OPTION);
        } else {
            includeExplanationInSeparateKey = false;
        }

        JFileChooser dirChooser = new JFileChooser(AppConfig.getProperty("app.default.export.directory", FileSystemView.getFileSystemView().getDefaultDirectory().getPath()));
        dirChooser.setDialogTitle("Chọn thư mục để lưu bộ đề" + (createSeparateAnswerKey ? " và đáp án" : ""));
        dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        dirChooser.setAcceptAllFileFilterUsed(false);

        if (dirChooser.showSaveDialog(ownerFrame) == JFileChooser.APPROVE_OPTION) {
            File outputDirectoryFile = dirChooser.getSelectedFile();
            if (!outputDirectoryFile.exists() && !outputDirectoryFile.mkdirs()) {
                 UIUtils.showErrorMessage(ownerFrame, "Không thể tạo thư mục đích: " + outputDirectoryFile.getAbsolutePath());
                 return;
            }
            if (!outputDirectoryFile.isDirectory()) {
                UIUtils.showErrorMessage(ownerFrame, "Đường dẫn đã chọn không phải là thư mục.");
                return;
            }
            final String finalOutputDirectoryPath = outputDirectoryFile.getAbsolutePath();

            final int finalNumVersions = numVersions;
            final Exam finalOriginalExam = originalExam;

            JDialog waitingDialog = new JDialog(ownerFrame, "Đang tạo bộ đề...", Dialog.ModalityType.APPLICATION_MODAL);
            JProgressBar progressBar = new JProgressBar(0, finalNumVersions);
            progressBar.setStringPainted(true);
            JLabel statusLabel = new JLabel("Đang chuẩn bị...");
            statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
            JPanel progressPanel = new JPanel(new BorderLayout(5,5));
            progressPanel.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
            progressPanel.add(statusLabel, BorderLayout.NORTH);
            progressPanel.add(progressBar, BorderLayout.CENTER);
            waitingDialog.add(progressPanel);
            waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            waitingDialog.setSize(400, 150);
            waitingDialog.setLocationRelativeTo(ownerFrame);

            SwingWorker<List<String>, String> setWorker = new SwingWorker<>() {
                @Override
                protected List<String> doInBackground() throws Exception {
                    List<String> generatedFilePaths = new ArrayList<>();
                    String baseSafeExamName = finalOriginalExam.getExamName().replaceAll("[^a-zA-Z0-9\\-_ ]+", "").trim().replace(" ", "_");
                    if (baseSafeExamName.isEmpty()) baseSafeExamName = "DeThi";

                    for (int i = 1; i <= finalNumVersions; i++) {
                        publish(String.format("Đang tạo phiên bản %d/%d...", i, finalNumVersions));
                        progressBar.setValue(i);

                        String examFileName = String.format("%s_PB%02d.%s", baseSafeExamName, i, outputFormat);
                        String examFilePath = new File(finalOutputDirectoryPath, examFileName).getAbsolutePath();

                        List<Question> shuffledQuestionsForThisVersion = exportService.exportSingleExamVersionAndGetShuffled(
                                finalOriginalExam,
                                examFilePath,
                                outputFormat,
                                includeAnswersInEachExamFile // Sẽ là false
                        );

                        if (shuffledQuestionsForThisVersion != null) {
                            generatedFilePaths.add(examFilePath);
                            if (createSeparateAnswerKey) {
                                String answerKeyFileName = String.format("%s_DapAn_PB%02d.%s", baseSafeExamName, i, outputFormat);
                                String answerKeyFilePath = new File(finalOutputDirectoryPath, answerKeyFileName).getAbsolutePath();
                                boolean answerKeyCreated = exportService.exportAnswerKeyForShuffledVersion(
                                    finalOriginalExam,
                                    shuffledQuestionsForThisVersion,
                                    answerKeyFilePath,
                                    outputFormat,
                                    i,
                                    includeExplanationInSeparateKey
                                );
                                if (answerKeyCreated) {
                                    generatedFilePaths.add(answerKeyFilePath);
                                } else {
                                    publish(String.format("! Lỗi tạo đáp án PB %d", i));
                                }
                            }
                        } else {
                             publish(String.format("! Lỗi tạo đề PB %d", i));
                        }
                        if (isCancelled()) break;
                    }
                    return generatedFilePaths;
                }
                @Override
                protected void process(List<String> chunks) {
                    statusLabel.setText(chunks.get(chunks.size() - 1));
                }
                @Override
                protected void done() {
                    waitingDialog.dispose();
                    try {
                        List<String> results = get();
                        int successfulFiles = results.size();
                        int expectedFiles = finalNumVersions + (createSeparateAnswerKey ? finalNumVersions : 0);
                        
                        if (successfulFiles > 0) {
                             String message = String.format("Đã tạo %d/%d file thành công.\nLưu tại: %s",
                                                          successfulFiles, expectedFiles, finalOutputDirectoryPath);
                            if (successfulFiles < expectedFiles && !isCancelled()) {
                                message += "\nMột số file có thể đã gặp lỗi trong quá trình tạo.";
                                UIUtils.showWarningMessage(ownerFrame, message);
                            } else if (!isCancelled()){
                                UIUtils.showInformationMessage(ownerFrame, message);
                            }
                            if (!isCancelled()) {
                                int openChoice = JOptionPane.showConfirmDialog(ownerFrame, "Mở thư mục chứa các file đã xuất?", "Hoàn Tất", JOptionPane.YES_NO_OPTION);
                                if (openChoice == JOptionPane.YES_OPTION) {
                                    try { Desktop.getDesktop().open(new File(finalOutputDirectoryPath)); }
                                    catch (IOException ex) { UIUtils.showErrorMessage(ownerFrame, "Không thể mở thư mục: " + ex.getMessage());}
                                }
                            }
                        } else if (!isCancelled()){
                            UIUtils.showErrorMessage(ownerFrame, "Không tạo được file nào do có lỗi trong quá trình xử lý.");
                        } else {
                            UIUtils.showWarningMessage(ownerFrame, "Quá trình tạo bộ đề đã bị hủy.");
                        }
                    } catch (Exception e) {
                       handleExportException(e, "Lỗi khi tạo bộ đề");
                    }
                }
            };
            setWorker.execute();
            if (setWorker.getState() != SwingWorker.StateValue.DONE) {
                 waitingDialog.setVisible(true);
            }
        }
    }

    private void exportAnswerKey(Exam exam, String format, String originalExamFilePath,
            boolean isShuffledForThisKey, int versionNumberForThisKey,
            boolean includeExplanationInThisKey) {

String baseNameOriginalExam = originalExamFilePath.substring(0, originalExamFilePath.lastIndexOf('.'));
String answerKeyFileName;

if (versionNumberForThisKey > 0) {
// Trường hợp này là đáp án cho một phiên bản trong bộ đề xáo trộn
// Tên file đã được tạo đúng trong generateShuffledExamSet và truyền vào đây
// Tuy nhiên, để nhất quán, ta có thể xây dựng lại dựa trên originalExamFilePath
// của phiên bản đề đó (nếu originalExamFilePath ở đây là đường dẫn của phiên bản đề)
// Hoặc, tốt hơn là generateShuffledExamSet đã tạo đúng tên file và truyền vào.
// Giả sử originalExamFilePath ở đây LÀ đường dẫn của file đề phiên bản (ví dụ: DETOAN_PB01.pdf)
// thì tên file đáp án sẽ là DETOAN_DapAn_PB01.pdf
String examBaseNameForVersion = baseNameOriginalExam;
if (baseNameOriginalExam.matches(".*_PB\\d{2}$")) { // Nếu tên file đề phiên bản có dạng ..._PBxx
examBaseNameForVersion = baseNameOriginalExam.substring(0, baseNameOriginalExam.lastIndexOf("_PB"));
}
answerKeyFileName = String.format("%s_DapAn_PB%02d.%s", examBaseNameForVersion, versionNumberForThisKey, format);
} else {
// Trường hợp này là đáp án cho đề gốc khi xuất đơn lẻ
// Tên file sẽ là TênĐềGốc_DapAn.format
answerKeyFileName = baseNameOriginalExam + "_DapAn." + format;
}

// Lấy thư mục từ originalExamFilePath
File originalFile = new File(originalExamFilePath);
String outputDirectory = originalFile.getParent(); // Thư mục chứa file đề gốc/phiên bản

String answerKeyFilePath = new File(outputDirectory, answerKeyFileName).getAbsolutePath();


JDialog waitingDialog = new JDialog(ownerFrame, "Đang xuất đáp án...", Dialog.ModalityType.APPLICATION_MODAL);
JProgressBar progressBarAns = new JProgressBar();
progressBarAns.setIndeterminate(true);
JLabel statusLabelAns = new JLabel("Đang chuẩn bị xuất đáp án...");
JPanel progressPanelAns = new JPanel(new BorderLayout(5,5));
progressPanelAns.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
progressPanelAns.add(statusLabelAns, BorderLayout.NORTH);
progressPanelAns.add(progressBarAns, BorderLayout.CENTER);
waitingDialog.add(progressPanelAns);
waitingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
waitingDialog.setSize(350, 120);
waitingDialog.setLocationRelativeTo(ownerFrame);

SwingWorker<Boolean, Void> ansWorker = new SwingWorker<>() {
@Override
protected Boolean doInBackground() throws Exception {
statusLabelAns.setText("Đang tạo file đáp án...");
// Danh sách câu hỏi cần dựa vào isShuffledForThisKey.
// Nếu isShuffledForThisKey là true, exam.getQuestions() phải LÀ danh sách đã xáo trộn.
// Nếu false, dùng exam.getQuestions() gốc.
// Hiện tại, Exam object truyền vào exportAnswerKey luôn là bản gốc,
// việc xáo trộn (nếu có) đã được thực hiện trước đó và danh sách xáo trộn
// được truyền vào exportService.exportAnswerKeyForShuffledVersion.
// Do đó, chúng ta cần đảm bảo truyền đúng danh sách câu hỏi vào service.

List<Question> questionsForThisKey = new ArrayList<>(exam.getQuestions());
if (isShuffledForThisKey) { // Chỉ shuffle nếu được yêu cầu cho key này
// Vấn đề: nếu isShuffledForThisKey là true, chúng ta cần danh sách ĐÃ shuffle
// từ bước tạo đề. exam.getQuestions() ở đây có thể là list gốc.
// => exportAnswerKey nên nhận List<Question> đã được xử lý.
// Tạm thời, nếu isShuffledForThisKey = true, ta giả định rằng
// hàm gọi (generateShuffledExamSet) sẽ truyền vào một Exam object
// mà getQuestions() của nó đã là list đã shuffle.
// Hoặc tốt hơn là truyền trực tiếp List<Question> đã shuffle.
// Dưới đây là cách an toàn hơn:
// Nếu isShuffledForThisKey, ExamManagementPanel cần truyền list đã shuffle cho phương thức này.
// Hiện tại, để đơn giản, ta giả định exam.getQuestions() là list cần dùng.
}


return exportService.exportAnswerKeyForShuffledVersion(
   exam, // Thông tin chung của Exam
   questionsForThisKey, // Danh sách câu hỏi (đúng thứ tự cho đáp án này)
   answerKeyFilePath,
   format,
   versionNumberForThisKey,
   includeExplanationInThisKey
);
}
@Override
protected void done() {
waitingDialog.dispose();
try {
if (get()) {
   UIUtils.showInformationMessage(ownerFrame, "Đã xuất đáp án thành công!\nLưu tại: " + answerKeyFilePath);
} else {
   UIUtils.showErrorMessage(ownerFrame, "Lỗi khi xuất file đáp án.");
}
} catch (Exception e) {
handleExportException(e, "Lỗi khi xuất đáp án");
}
}
};
ansWorker.execute();
if (ansWorker.getState() != SwingWorker.StateValue.DONE) {
waitingDialog.setVisible(true);
}
}
    
    private void handleExportException(Exception e, String contextMessage) {
        if (e instanceof InterruptedException) {
            Thread.currentThread().interrupt();
            UIUtils.showErrorMessage(ownerFrame, contextMessage + ":\nTiến trình bị gián đoạn.");
        } else if (e instanceof ExecutionException) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            UIUtils.showErrorMessage(ownerFrame, contextMessage + ":\nLỗi thực thi: " + cause.getMessage());
            if (cause != e) cause.printStackTrace(); // In stack trace của nguyên nhân gốc
        } else {
            UIUtils.showErrorMessage(ownerFrame, contextMessage + ":\nLỗi không mong muốn: " + e.getMessage());
            e.printStackTrace();
        }
        
    }
    
}