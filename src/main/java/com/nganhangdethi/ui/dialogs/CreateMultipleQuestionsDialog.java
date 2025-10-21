package com.nganhangdethi.ui.dialogs;

import com.nganhangdethi.model.Question;
import com.nganhangdethi.service.AIService;
import com.nganhangdethi.service.QuestionService;
import com.nganhangdethi.ui.tablemodels.AIGeneratedQuestionTableModel;
import com.nganhangdethi.util.ButtonColumn;
import com.nganhangdethi.util.AppConfig;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CreateMultipleQuestionsDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(CreateMultipleQuestionsDialog.class);

    private JButton chooseImageFileButton;
    private JTextField imagePathField;
    private JTextArea aiInstructionsTextArea;
    private JSpinner numQuestionsSpinner;
    private JButton extractWithAIButton;
    private JTable aiGeneratedQuestionsTable;
    private AIGeneratedQuestionTableModel tableModel;
    private JButton addSelectedToBankButton;
    private JButton cancelButton;

    private File selectedImageFile;
    private AIService aiService;
    private QuestionService questionService;
    private Frame ownerFrame;

    public CreateMultipleQuestionsDialog(Frame owner) {
        super(owner, Constants.TITLE_CREATE_MULTIPLE_QUESTIONS, true);
        this.ownerFrame = owner;
        this.aiService = new AIService();
        this.questionService = new QuestionService();

        initComponents();
        setupEventListeners();
        applyFonts();

        setSize(950, 700);
        setMinimumSize(new Dimension(800, 600));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleCancel();
            }
        });
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Panel Input
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Thông số đầu vào cho AI"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        chooseImageFileButton = new JButton(Constants.LABEL_CHOOSE_IMAGE_FILE);
        inputPanel.add(chooseImageFileButton, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        imagePathField = new JTextField(30);
        imagePathField.setEditable(false);
        inputPanel.add(imagePathField, gbc);
        gbc.gridwidth = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.NONE;

        gbc.gridx = 0; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHEAST;
        inputPanel.add(new JLabel(Constants.LABEL_AI_INSTRUCTIONS_MULTI), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        aiInstructionsTextArea = new JTextArea(3, 30);
        aiInstructionsTextArea.setLineWrap(true);
        aiInstructionsTextArea.setWrapStyleWord(true);
        inputPanel.add(new JScrollPane(aiInstructionsTextArea), gbc);
        gbc.gridwidth = 1;

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel(Constants.LABEL_NUM_QUESTIONS_TO_EXTRACT), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.WEST;
        numQuestionsSpinner = new JSpinner(new SpinnerNumberModel(5, 1, 50, 1));
        inputPanel.add(numQuestionsSpinner, gbc);

        gbc.gridx = 2; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE;
        extractWithAIButton = new JButton(Constants.BUTTON_EXTRACT_WITH_AI, UIUtils.createImageIcon(Constants.ICON_PATH_AI_SUGGEST, "AI", 16, 16));
        inputPanel.add(extractWithAIButton, gbc);

        add(inputPanel, BorderLayout.NORTH);

        // Panel Bảng câu hỏi
        tableModel = new AIGeneratedQuestionTableModel();
        aiGeneratedQuestionsTable = new JTable(tableModel);
        aiGeneratedQuestionsTable.setRowHeight(28);
        aiGeneratedQuestionsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        aiGeneratedQuestionsTable.setAutoCreateRowSorter(true);

        Action editAction = new AbstractAction(Constants.COL_EDIT_ACTION) {
            public void actionPerformed(ActionEvent e) {
                int modelRow = Integer.parseInt(e.getActionCommand());
                editQuestionFromTable(modelRow);
            }
        };
        new ButtonColumn(aiGeneratedQuestionsTable, editAction, tableModel.getColumnCount() - 2);

        Action deleteAction = new AbstractAction(Constants.COL_DELETE_ACTION) {
            public void actionPerformed(ActionEvent e) {
                int modelRow = Integer.parseInt(e.getActionCommand());
                deleteQuestionFromTable(modelRow);
            }
        };
        new ButtonColumn(aiGeneratedQuestionsTable, deleteAction, tableModel.getColumnCount() - 1);

        aiGeneratedQuestionsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        aiGeneratedQuestionsTable.getColumnModel().getColumn(0).setMaxWidth(60);
        aiGeneratedQuestionsTable.getColumnModel().getColumn(1).setPreferredWidth(350);
        aiGeneratedQuestionsTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        aiGeneratedQuestionsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        aiGeneratedQuestionsTable.getColumnModel().getColumn(4).setPreferredWidth(70);

        aiGeneratedQuestionsTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = aiGeneratedQuestionsTable.rowAtPoint(e.getPoint());
                    if (viewRow >= 0) {
                        int modelRow = aiGeneratedQuestionsTable.convertRowIndexToModel(viewRow);
                        Question questionToShow = tableModel.getQuestionAt(modelRow);
                        if (questionToShow != null) {
                            Question clonedForView = cloneQuestion(questionToShow);
                            ViewQuestionDialog viewDialog = new ViewQuestionDialog(ownerFrame, clonedForView);
                            viewDialog.setVisible(true);
                        }
                    }
                }
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(aiGeneratedQuestionsTable);
        add(tableScrollPane, BorderLayout.CENTER);

        // Panel nút chức năng dưới cùng
        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        addSelectedToBankButton = new JButton(Constants.BUTTON_ADD_SELECTED_TO_BANK, UIUtils.createImageIcon(Constants.ICON_PATH_SAVE, "Lưu", 16, 16));
        cancelButton = new JButton(Constants.MSG_CANCEL_ACTION, UIUtils.createImageIcon(Constants.ICON_PATH_CANCEL, "Hủy", 16, 16));

        bottomButtonPanel.add(addSelectedToBankButton);
        bottomButtonPanel.add(cancelButton);
        add(bottomButtonPanel, BorderLayout.SOUTH);
    }

    private void applyFonts() {
        Font defaultFont = UIUtils.getJapaneseFont(13f);
        Font boldFont = defaultFont.deriveFont(Font.BOLD);

        UIUtils.setFontRecursively(this, defaultFont);
        extractWithAIButton.setFont(boldFont);
        addSelectedToBankButton.setFont(boldFont);
        aiGeneratedQuestionsTable.setFont(defaultFont);
        aiGeneratedQuestionsTable.getTableHeader().setFont(boldFont);
    }

    private void setupEventListeners() {
        chooseImageFileButton.addActionListener(e -> selectImageFile());
        extractWithAIButton.addActionListener(e -> extractQuestionsWithAI());
        addSelectedToBankButton.addActionListener(e -> addSelectedQuestionsToBank());
        cancelButton.addActionListener(e -> handleCancel());
    }

    private void selectImageFile() {
        JFileChooser fileChooser = new JFileChooser(AppConfig.getProperty("app.default.import.directory", System.getProperty("user.home")));
        fileChooser.setDialogTitle("Chọn file ảnh chứa câu hỏi");
        fileChooser.setFileFilter(new FileNameExtensionFilter(Constants.IMAGE_FILE_DESCRIPTION, Constants.IMAGE_EXTENSIONS));
        fileChooser.setAcceptAllFileFilterUsed(false);

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            selectedImageFile = fileChooser.getSelectedFile();
            imagePathField.setText(selectedImageFile.getAbsolutePath());
            // Consider saving the parent directory for next time, perhaps in AppConfig or a session variable
            // AppConfig.setProperty("app.default.import.directory", selectedImageFile.getParent());
        }
    }

    private void extractQuestionsWithAI() {
        String apiKey = AppConfig.getGeminiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("YOUR_")) {
            UIUtils.showErrorMessage(this, "Vui lòng cấu hình Google AI (Gemini) API Key trong Cài đặt.");
            return;
        }

        if (selectedImageFile == null || !selectedImageFile.exists()) {
            UIUtils.showErrorMessage(this, Constants.MSG_NO_FILE_SELECTED_FOR_AI);
            return;
        }

        String userInstructions = aiInstructionsTextArea.getText().trim();
        int numQuestions = (Integer) numQuestionsSpinner.getValue();

        setInteractionEnabled(false);
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "AI đang trích xuất câu hỏi từ ảnh...", "Vui lòng chờ...", 0, 100);
        progressMonitor.setMillisToPopup(100);

        SwingWorker<List<Question>, String> worker = new SwingWorker<>() {
            @Override
            protected List<Question> doInBackground() throws Exception {
                publish("Đang chuẩn bị dữ liệu...");
                progressMonitor.setProgress(10);
                byte[] imageBytes = Files.readAllBytes(selectedImageFile.toPath());
                String base64Image = Base64.getEncoder().encodeToString(imageBytes);

                String fileName = selectedImageFile.getName().toLowerCase();
                String mimeType = "image/jpeg";
                if (fileName.endsWith(".png")) mimeType = "image/png";
                else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                else if (fileName.endsWith(".bmp")) mimeType = "image/bmp";
                
                publish("Đang gửi yêu cầu đến Gemini...");
                progressMonitor.setNote("Đang gửi yêu cầu đến Gemini...");
                progressMonitor.setProgress(30);

                List<Question> extracted = aiService.getMultipleQuestionsFromImage(userInstructions, base64Image, mimeType, numQuestions);
                
                progressMonitor.setProgress(90);
                publish("Hoàn tất trích xuất.");
                return extracted;
            }
            
            @Override
            protected void process(List<String> chunks) {
                for (String message : chunks) {
                    if (progressMonitor.isCanceled()) break;
                    progressMonitor.setNote(message);
                }
            }

            @Override
            protected void done() {
                if (!progressMonitor.isCanceled()) { // Chỉ đóng nếu không bị hủy bởi người dùng
                    progressMonitor.close();
                }
                setInteractionEnabled(true);
                try {
                    List<Question> result = get(); // Sẽ ném CancellationException nếu task bị hủy
                    if (result != null && !result.isEmpty()) {
                        tableModel.setQuestions(result);
                        UIUtils.showInformationMessage(CreateMultipleQuestionsDialog.this,
                                String.format(Constants.MSG_AI_EXTRACTION_SUCCESS, result.size()));
                    } else {
                        UIUtils.showWarningMessage(CreateMultipleQuestionsDialog.this, Constants.MSG_AI_EXTRACTION_FAIL);
                        tableModel.setQuestions(null);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("AI extraction interrupted", e);
                    UIUtils.showErrorMessage(CreateMultipleQuestionsDialog.this, "Quá trình trích xuất bị gián đoạn.");
                } catch (ExecutionException e) {
                    logger.error("AI extraction execution error", e.getCause() != null ? e.getCause() : e);
                    String causeMsg = (e.getCause() != null) ? e.getCause().getMessage() : e.getMessage();
                    UIUtils.showErrorMessage(CreateMultipleQuestionsDialog.this, "Lỗi khi AI trích xuất: " + causeMsg);
                } catch (java.util.concurrent.CancellationException e) {
                    logger.info("AI extraction cancelled by user.");
                    UIUtils.showInformationMessage(CreateMultipleQuestionsDialog.this, "Quá trình trích xuất đã được hủy.");
                }
                catch (Exception e) {
                     logger.error("Unexpected error during AI extraction processing", e);
                     UIUtils.showErrorMessage(CreateMultipleQuestionsDialog.this, "Lỗi không mong muốn khi xử lý kết quả AI: " + e.getMessage());
                }
            }
        };
        worker.execute();
    }

    private void editQuestionFromTable(int modelRow) {
        Question questionToEdit = tableModel.getQuestionAt(modelRow);
        if (questionToEdit == null) return;

        Question clonedQuestion = cloneQuestion(questionToEdit);

        QuestionDialog editDialog = new QuestionDialog(
            this.ownerFrame, 
            "Sửa Câu Hỏi AI Tạo (ID Tạm: " + (clonedQuestion.getId() == 0 ? "Mới" : clonedQuestion.getId() ) +")", 
            clonedQuestion
        );
        editDialog.setSuppressSaveMessages(true);
        editDialog.setVisible(true);

        // Sau khi editDialog đóng, các thay đổi đã được áp dụng cho clonedQuestion
        // (nếu người dùng nhấn "Lưu" trong QuestionDialog).
        // Cập nhật lại tableModel với clonedQuestion đã được sửa.
        tableModel.updateQuestionAt(modelRow, clonedQuestion); 
        logger.info("Question at row {} in temporary list potentially updated via QuestionDialog.", modelRow);
    }
    
    private Question cloneQuestion(Question original) {
        if (original == null) return null;
        Question clone = new Question(); // Sử dụng constructor mặc định
        clone.setId(original.getId()); // Giữ ID gốc cho tham chiếu, nhưng khi lưu thật sẽ là 0
        clone.setQuestionText(original.getQuestionText());
        clone.setOptionA(original.getOptionA());
        clone.setOptionB(original.getOptionB());
        clone.setOptionC(original.getOptionC());
        clone.setOptionD(original.getOptionD());
        clone.setCorrectAnswer(original.getCorrectAnswer());
        clone.setExplanation(original.getExplanation());
        clone.setLevel(original.getLevel());
        clone.setType(original.getType());
        clone.setTags(original.getTags());
        clone.setSource(original.getSource());
        // AI thường không trả về audio/image path, nên có thể để null hoặc clone nếu có
        clone.setAudioPath(original.getAudioPath());
        clone.setImagePath(original.getImagePath());
        return clone;
    }

    private void deleteQuestionFromTable(int modelRow) {
        int choice = UIUtils.showConfirmDialog(this,
                "Bạn có chắc muốn xóa câu hỏi này khỏi danh sách tạm thời?",
                "Xác nhận xóa");
        if (choice == JOptionPane.YES_OPTION) {
            tableModel.removeQuestionAt(modelRow);
            logger.info("Question at row {} removed from temporary list.", modelRow);
        }
    }

    private void addSelectedQuestionsToBank() {
        List<Question> questionsToAdd = tableModel.getSelectedQuestions();
        if (questionsToAdd.isEmpty()) {
            UIUtils.showWarningMessage(this, Constants.MSG_NO_QUESTIONS_SELECTED_FOR_BANK);
            return;
        }
        
        // Đảm bảo tất cả câu hỏi được thêm mới sẽ có ID = 0
        for(Question q : questionsToAdd) {
            q.setId(0); 
            // Các trường như audioPath, imagePath sẽ là null nếu AI không trả về
            // và QuestionDialog (khi suppressSaveMessages=true) không xử lý file.
        }

        setInteractionEnabled(false);
        // QuestionService.addMultipleQuestions nên trả về số lượng câu hỏi đã thêm thành công
        // hoặc ném exception nếu có lỗi nghiêm trọng.
        // Hiện tại, nó trả về true nếu ít nhất một câu hỏi được thêm.
        
        int successCount = 0;
        List<String> errorMessages = new ArrayList<>();

        for (Question question : questionsToAdd) {
            if (questionService.addQuestion(question)) { // Gọi addQuestion cho từng câu
                successCount++;
            } else {
                errorMessages.add("Không thể thêm câu hỏi: " + question.getQuestionText().substring(0, Math.min(30, question.getQuestionText().length())) + "...");
            }
        }
        
        setInteractionEnabled(true);

        if (successCount == questionsToAdd.size()) {
            UIUtils.showInformationMessage(this, String.format("Đã thêm thành công %d câu hỏi vào ngân hàng.", successCount));
            dispose();
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("Thêm thành công %d/%d câu hỏi.\n", successCount, questionsToAdd.size()));
            if (!errorMessages.isEmpty()) {
                sb.append("Các lỗi:\n");
                errorMessages.forEach(err -> sb.append("- ").append(err).append("\n"));
            }
            UIUtils.showWarningMessage(this, sb.toString());
            if (successCount > 0) { // Nếu có một số thành công, vẫn có thể đóng
                dispose();
            }
            // Nếu tất cả thất bại (successCount == 0), không đóng dialog để người dùng xem lại.
        }
    }

    private void handleCancel() {
        if (tableModel.getRowCount() > 0) {
            int choice = UIUtils.showConfirmDialog(this,
                    Constants.MSG_CONFIRM_CANCEL_MULTI_ADD,
                    "Xác nhận hủy");
            if (choice == JOptionPane.YES_OPTION) {
                dispose();
            }
        } else {
            dispose();
        }
    }

    private void setInteractionEnabled(boolean enabled) {
        chooseImageFileButton.setEnabled(enabled);
        aiInstructionsTextArea.setEnabled(enabled);
        numQuestionsSpinner.setEnabled(enabled);
        extractWithAIButton.setEnabled(enabled);
        aiGeneratedQuestionsTable.setEnabled(enabled);
        addSelectedToBankButton.setEnabled(enabled);
        cancelButton.setEnabled(enabled);
    }
}