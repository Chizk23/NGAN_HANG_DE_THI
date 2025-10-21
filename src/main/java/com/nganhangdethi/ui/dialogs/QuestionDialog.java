package com.nganhangdethi.ui.dialogs;

import com.nganhangdethi.model.Question;
import com.nganhangdethi.service.AIService;
import com.nganhangdethi.service.FileManagementService;
import com.nganhangdethi.service.QuestionService;
import com.nganhangdethi.util.AppConfig;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import com.google.gson.JsonSyntaxException;


public class QuestionDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(QuestionDialog.class);

    //<editor-fold defaultstate="collapsed" desc="Khai báo biến UI và Service">
    private JTextField questionIdField;
    private JTextArea questionTextArea;
    private JTextField optionAField, optionBField, optionCField, optionDField;
    private JComboBox<String> correctAnswerComboBox;
    private JTextArea explanationTextArea;
    private JComboBox<String> levelComboBox, typeComboBox;
    private JTextField tagsField, sourceField;
    private JTextField audioPathField, imagePathField;
    private JButton selectAudioButton, selectImageButton;
    private JButton clearAudioPathButton, clearImagePathButton;

    private JButton saveButton, cancelButton;
    private Question questionToEdit; // Đây là đối tượng sẽ được thao tác trực tiếp
    private boolean isEditMode;
    private boolean saved = false; // Cờ để báo hiệu dialog cha biết người dùng đã nhấn "Lưu"

    private QuestionService questionService;
    private AIService aiService;
    private FileManagementService fileManagementService;

    private JTextArea aiPromptTextArea;
    private JTextArea aiSuggestionTextArea;
    private JButton suggestQuestionButton, suggestOptionsButton, suggestExplanationButton;
    private JButton createImageQuestionButton;
    private JButton applyAISuggestionButton;

    private File selectedImageFileForAI; // Dùng cho AI tạo câu hỏi từ ảnh trong dialog này
    private boolean suppressSaveMessages = false; // Cờ để không hiện thông báo lưu/lỗi từ service
    //</editor-fold>

    public QuestionDialog(Frame owner, String title, Question question) {
        super(owner, title, true);
        this.questionToEdit = question; // questionToEdit có thể là null (tạo mới) hoặc một đối tượng (sửa)
                                         // Nếu là sửa từ CreateMultipleQuestionsDialog, đây sẽ là clonedQuestion
        this.isEditMode = (question != null);
        this.questionService = new QuestionService();
        this.aiService = new AIService();
        this.fileManagementService = new FileManagementService();

        initComponents();
        setupEventListeners();

        if (isEditMode && questionToEdit != null) {
            populateFieldsForEdit();
        } else {
            // Nếu tạo mới (questionToEdit là null), tạo một đối tượng Question mới để thao tác
            this.questionToEdit = new Question();
            setDefaultComboBoxValues();
        }

        pack();
        setMinimumSize(new Dimension(800, 800));
        setSize(new Dimension(900, 820));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        logger.info("QuestionDialog initialized. Mode: {}. Question ID (if edit): {}", 
                    isEditMode ? "Edit" : "Create", 
                    (isEditMode && questionToEdit != null ? questionToEdit.getId() : "N/A"));
    }

    public void setSuppressSaveMessages(boolean suppress) {
        this.suppressSaveMessages = suppress;
    }

    private void setDefaultComboBoxValues() {
        // Đặt giá trị mặc định cho các ComboBox nếu questionToEdit (tạo mới) chưa có
        if (questionToEdit != null) { // Chỉ đặt nếu questionToEdit đã được khởi tạo
            if (Constants.QUESTION_LEVELS_NO_ALL != null && Constants.QUESTION_LEVELS_NO_ALL.length > 0) {
                levelComboBox.setSelectedItem(questionToEdit.getLevel() != null ? questionToEdit.getLevel() : Constants.DEFAULT_QUESTION_LEVEL);
                if (levelComboBox.getSelectedItem() == null && levelComboBox.getItemCount() > 0) levelComboBox.setSelectedIndex(0);
            }
            if (Constants.QUESTION_TYPES_NO_ALL != null && Constants.QUESTION_TYPES_NO_ALL.length > 0) {
                typeComboBox.setSelectedItem(questionToEdit.getType() != null ? questionToEdit.getType() : Constants.DEFAULT_QUESTION_TYPE);
                if (typeComboBox.getSelectedItem() == null && typeComboBox.getItemCount() > 0) typeComboBox.setSelectedIndex(0);
            }
        }
    }


    private void initComponents() {
        setLayout(new BorderLayout(10,10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        JPanel mainFormPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int yPos = 0;

        // ID field (chỉ hiển thị và không cho sửa nếu isEditMode và ID > 0)
        JLabel idLabel = new JLabel("ID:");
        questionIdField = new JTextField(7);
        questionIdField.setEditable(false); // Luôn không cho sửa ID
        if (isEditMode && questionToEdit != null && questionToEdit.getId() > 0) {
             questionIdField.setText(String.valueOf(questionToEdit.getId()));
        } else {
            questionIdField.setText("Tự động");
        }
        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; mainFormPanel.add(idLabel, gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.WEST; gbc.gridwidth=3; mainFormPanel.add(questionIdField, gbc);
        yPos++;
        gbc.gridwidth=1;


        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.NORTHEAST; gbc.fill = GridBagConstraints.NONE;
        mainFormPanel.add(new JLabel("Nội dung câu hỏi (*):"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.3;
        questionTextArea = new JTextArea(5, 20);
        questionTextArea.setLineWrap(true);
        questionTextArea.setWrapStyleWord(true);
        mainFormPanel.add(new JScrollPane(questionTextArea), gbc);
        yPos++;
        gbc.gridwidth = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;

        addFormField(mainFormPanel, gbc, yPos++, "Lựa chọn A (*):", optionAField = new JTextField());
        addFormField(mainFormPanel, gbc, yPos++, "Lựa chọn B (*):", optionBField = new JTextField());
        addFormField(mainFormPanel, gbc, yPos++, "Lựa chọn C:", optionCField = new JTextField());
        addFormField(mainFormPanel, gbc, yPos++, "Lựa chọn D:", optionDField = new JTextField());

        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST; mainFormPanel.add(new JLabel("Đáp án đúng (*):"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.4;
        correctAnswerComboBox = new JComboBox<>(new String[]{"-", "A", "B", "C", "D"});
        mainFormPanel.add(correctAnswerComboBox, gbc);

        gbc.gridx = 2; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.1; mainFormPanel.add(new JLabel("Cấp độ (*):"), gbc);
        gbc.gridx = 3; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        levelComboBox = new JComboBox<>(Constants.QUESTION_LEVELS_NO_ALL != null ? Constants.QUESTION_LEVELS_NO_ALL : new String[]{"N/A"});
        mainFormPanel.add(levelComboBox, gbc);
        yPos++;
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST; mainFormPanel.add(new JLabel("Loại (*):"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0.4;
        typeComboBox = new JComboBox<>(Constants.QUESTION_TYPES_NO_ALL != null ? Constants.QUESTION_TYPES_NO_ALL : new String[]{"N/A"});
        mainFormPanel.add(typeComboBox, gbc);

        gbc.gridx = 2; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 0.1; mainFormPanel.add(new JLabel("Nguồn:"), gbc);
        gbc.gridx = 3; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 0.5;
        sourceField = new JTextField();
        mainFormPanel.add(sourceField, gbc);
        yPos++;
        gbc.weightx = 0;

        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.NORTHEAST;
        mainFormPanel.add(new JLabel("Giải thích:"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH; gbc.weighty = 0.2;
        explanationTextArea = new JTextArea(3, 20);
        explanationTextArea.setLineWrap(true);
        explanationTextArea.setWrapStyleWord(true);
        mainFormPanel.add(new JScrollPane(explanationTextArea), gbc);
        yPos++;
        gbc.gridwidth = 1; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weighty = 0;

        addFormField(mainFormPanel, gbc, yPos++, "Tags (phẩy cách):", tagsField = new JTextField());

        gbc.gridx = 0; gbc.gridy = yPos++; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 6, 0);
        mainFormPanel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(5, 5, 5, 5);

        imagePathField = new JTextField(20); imagePathField.setEditable(false);
        selectImageButton = new JButton("Chọn Ảnh...");
        clearImagePathButton = new JButton("Xóa");
        audioPathField = new JTextField(20); audioPathField.setEditable(false);
        selectAudioButton = new JButton("Chọn Audio...");
        clearAudioPathButton = new JButton("Xóa");

        yPos = addFileManagementRow(mainFormPanel, gbc, yPos, "File hình ảnh:", imagePathField, selectImageButton, clearImagePathButton);
        yPos = addFileManagementRow(mainFormPanel, gbc, yPos, "File audio:", audioPathField, selectAudioButton, clearAudioPathButton);


        gbc.gridx = 0; gbc.gridy = yPos++; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 0, 6, 0);
        mainFormPanel.add(new JSeparator(), gbc);
        gbc.insets = new Insets(5, 5, 5, 5);

        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 4; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1.0; gbc.weighty = 0.3;
        gbc.insets = new Insets(10, 0, 5, 0);
        JPanel aiPanel = new JPanel(new BorderLayout(5, 5));
        aiPanel.setBorder(BorderFactory.createTitledBorder("Hỗ trợ từ AI (Thử nghiệm)"));

        aiPromptTextArea = new JTextArea(3, 20);
        aiPromptTextArea.setLineWrap(true);
        aiPromptTextArea.setWrapStyleWord(true);
        aiPromptTextArea.setToolTipText("Nhập yêu cầu cho AI. Ví dụ: 'Tạo câu hỏi Kanji N3 về thời tiết'");
        aiPanel.add(new JScrollPane(aiPromptTextArea), BorderLayout.NORTH);

        JPanel aiButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 2));
        suggestQuestionButton = new JButton("Gợi ý Câu Hỏi & Đ.Án");
        suggestOptionsButton = new JButton("Gợi ý Lựa Chọn Sai");
        suggestExplanationButton = new JButton("Gợi ý Giải Thích");
        createImageQuestionButton = new JButton("AI từ Ảnh");
        aiButtonsPanel.add(suggestQuestionButton);
        aiButtonsPanel.add(suggestOptionsButton);
        aiButtonsPanel.add(suggestExplanationButton);
        aiButtonsPanel.add(createImageQuestionButton);
        aiPanel.add(aiButtonsPanel, BorderLayout.CENTER);

        aiSuggestionTextArea = new JTextArea(8, 20);
        aiSuggestionTextArea.setLineWrap(true);
        aiSuggestionTextArea.setWrapStyleWord(true);
        aiSuggestionTextArea.setEditable(false);
        aiSuggestionTextArea.setToolTipText("Kết quả gợi ý từ AI. Bạn có thể sao chép (Ctrl+C) và dán (Ctrl+V), hoặc sử dụng nút 'Áp dụng Gợi ý AI' nếu gợi ý là JSON.");

        JPanel suggestionDisplayPanel = new JPanel(new BorderLayout(5,0));
        suggestionDisplayPanel.add(new JScrollPane(aiSuggestionTextArea), BorderLayout.CENTER);
        applyAISuggestionButton = new JButton("Áp dụng Gợi ý AI");
        applyAISuggestionButton.setToolTipText("Phân tích và điền các gợi ý từ AI vào các trường tương ứng (nếu là JSON hợp lệ).");
        suggestionDisplayPanel.add(applyAISuggestionButton, BorderLayout.SOUTH);
        
        aiPanel.add(suggestionDisplayPanel, BorderLayout.SOUTH);

        mainFormPanel.add(aiPanel, gbc);
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);

        JPanel bottomButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Lưu", UIUtils.createImageIcon(Constants.ICON_PATH_SAVE, "Lưu", 16,16));
        cancelButton = new JButton("Hủy", UIUtils.createImageIcon(Constants.ICON_PATH_CANCEL, "Hủy", 16,16));
        bottomButtonPanel.add(saveButton);
        bottomButtonPanel.add(cancelButton);

        JScrollPane mainScrollPane = new JScrollPane(mainFormPanel);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(mainScrollPane, BorderLayout.CENTER);
        add(bottomButtonPanel, BorderLayout.SOUTH);

        Font defaultFont = UIUtils.getJapaneseFont(13f);
        if (defaultFont != null) {
            UIUtils.setFontRecursively(this, defaultFont);
            saveButton.setFont(defaultFont.deriveFont(Font.BOLD));
            cancelButton.setFont(defaultFont);
            suggestQuestionButton.setFont(defaultFont);
            suggestOptionsButton.setFont(defaultFont);
            suggestExplanationButton.setFont(defaultFont);
            createImageQuestionButton.setFont(defaultFont);
            if (applyAISuggestionButton != null) applyAISuggestionButton.setFont(defaultFont);
            
            Font smallButtonFont = defaultFont.deriveFont(11f);
            if (selectAudioButton != null) selectAudioButton.setFont(smallButtonFont);
            if (selectImageButton != null) selectImageButton.setFont(smallButtonFont);
            if (clearImagePathButton != null) clearImagePathButton.setFont(smallButtonFont);
            if (clearAudioPathButton != null) clearAudioPathButton.setFont(smallButtonFont);
        }
    }

    private int addFileManagementRow(JPanel panel, GridBagConstraints gbc, int yPos, String labelText,
                                     JTextField pathField, JButton selectButton, JButton clearButton) {
        gbc.gridx = 0; gbc.gridy = yPos;
        gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);

        JPanel controlGroup = new JPanel(new BorderLayout(5, 0));
        controlGroup.add(pathField, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 3, 0));
        buttonsPanel.add(selectButton);
        buttonsPanel.add(clearButton);
        controlGroup.add(buttonsPanel, BorderLayout.EAST);

        gbc.gridx = 1; gbc.gridy = yPos;
        gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(controlGroup, gbc);

        return yPos + 1;
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int yPos, String labelText, JComponent component) {
        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.EAST; gbc.fill = GridBagConstraints.NONE; gbc.weightx = 0;
        panel.add(new JLabel(labelText), gbc);
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST; gbc.fill = GridBagConstraints.HORIZONTAL; gbc.weightx = 1.0;
        panel.add(component, gbc);
    }

    private void setupEventListeners() {
        saveButton.addActionListener(e -> saveQuestion());
        cancelButton.addActionListener(e -> dispose());

        suggestQuestionButton.addActionListener(e -> getAISuggestion("question_full"));
        suggestOptionsButton.addActionListener(e -> getAISuggestion("options_distractors"));
        suggestExplanationButton.addActionListener(e -> getAISuggestion("explanation"));
        createImageQuestionButton.addActionListener(e -> createImageQuestionWithAI());
        applyAISuggestionButton.addActionListener(e -> parseAndApplySuggestion());

        selectImageButton.addActionListener(e -> selectFile("image"));
        selectAudioButton.addActionListener(e -> selectFile("audio"));

        clearImagePathButton.addActionListener(e -> {
            imagePathField.setText("");
            this.selectedImageFileForAI = null; // Quan trọng nếu dùng AI từ ảnh
            if (questionToEdit != null) {
                questionToEdit.setImagePath(null); // Cập nhật đối tượng đang sửa
            }
            logger.info("Image path cleared by user. Question object's image path set to null.");
        });

        clearAudioPathButton.addActionListener(e -> {
            audioPathField.setText("");
            if (questionToEdit != null) {
                questionToEdit.setAudioPath(null); // Cập nhật đối tượng đang sửa
            }
            logger.info("Audio path cleared by user. Question object's audio path set to null.");
        });
    }

    private void selectFile(String fileType) {
        String currentDirKey = "app.default.media.dir." + fileType; // Key để lưu thư mục chọn lần cuối
        String currentDir = AppConfig.getProperty(currentDirKey, System.getProperty("user.home"));

        JFileChooser fileChooser = new JFileChooser(currentDir);
        String description; String[] extensions; JTextField targetField;

        if ("image".equals(fileType)) {
            description = Constants.IMAGE_FILE_DESCRIPTION;
            extensions = Constants.IMAGE_EXTENSIONS;
            targetField = imagePathField;
        } else { // audio
            description = Constants.AUDIO_FILE_DESCRIPTION;
            extensions = Constants.AUDIO_EXTENSIONS;
            targetField = audioPathField;
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter(description, extensions));
        fileChooser.setAcceptAllFileFilterUsed(false); // Không cho phép "All Files"

        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile != null && selectedFile.exists() && selectedFile.isFile()) {
                String absolutePath = selectedFile.getAbsolutePath();
                targetField.setText(absolutePath); // Hiển thị đường dẫn tuyệt đối

                // Cập nhật trực tiếp vào đối tượng questionToEdit
                if (questionToEdit != null) {
                    if ("image".equals(fileType)) {
                        questionToEdit.setImagePath(absolutePath);
                        this.selectedImageFileForAI = selectedFile; // Vẫn cần cho chức năng AI từ ảnh trong dialog
                    } else {
                        questionToEdit.setAudioPath(absolutePath);
                    }
                }
                logger.info("User selected {} file: {}. Absolute path set for current question object.", fileType, absolutePath);
                
                // Lưu thư mục cha để JFileChooser mở lại tại đó lần sau (tùy chọn)
                // Đảm bảo AppConfig có phương thức setProperty hoặc bạn dùng cơ chế lưu config khác
                // AppConfig.setProperty(currentDirKey, selectedFile.getParent());
                // Nếu AppConfig không có setProperty, bạn có thể bỏ qua dòng trên hoặc tự implement.

            } else {
                logger.warn("Selected file is invalid or does not exist: {}", selectedFile);
                UIUtils.showWarningMessage(this, "File không hợp lệ hoặc không tồn tại.");
            }
        }
    }

    private void populateFieldsForEdit() {
         if (questionToEdit != null) { // questionToEdit là đối tượng được truyền vào (hoặc clone)
            if (questionIdField != null) { // ID field có thể không tồn tại nếu không isEditMode
                questionIdField.setText(questionToEdit.getId() > 0 ? String.valueOf(questionToEdit.getId()) : "Tạm (chưa lưu)");
            }
            questionTextArea.setText(questionToEdit.getQuestionText());
            optionAField.setText(questionToEdit.getOptionA());
            optionBField.setText(questionToEdit.getOptionB());
            optionCField.setText(questionToEdit.getOptionC());
            optionDField.setText(questionToEdit.getOptionD());
            correctAnswerComboBox.setSelectedItem(questionToEdit.getCorrectAnswer() != null ? questionToEdit.getCorrectAnswer() : "-");
            explanationTextArea.setText(questionToEdit.getExplanation());
            levelComboBox.setSelectedItem(questionToEdit.getLevel());
            typeComboBox.setSelectedItem(questionToEdit.getType());
            tagsField.setText(questionToEdit.getTags());
            sourceField.setText(questionToEdit.getSource());
            
            // Hiển thị đường dẫn file media (có thể là tương đối nếu đã lưu, hoặc tuyệt đối nếu mới chọn)
            imagePathField.setText(questionToEdit.getImagePath()); 
            audioPathField.setText(questionToEdit.getAudioPath());
            
            this.selectedImageFileForAI = null; // Reset, sẽ được đặt lại nếu người dùng chọn ảnh mới
        }
    }

    private String buildPromptForAI(String suggestionType) {
        StringBuilder promptBuilder = new StringBuilder();
        String userAiPrompt = aiPromptTextArea.getText().trim();
        String currentLevel = (String) levelComboBox.getSelectedItem();
        String currentType = (String) typeComboBox.getSelectedItem();
        String currentQuestionText = questionTextArea.getText().trim();
        String currentOptionA = optionAField.getText().trim();
        String currentOptionB = optionBField.getText().trim();
        String currentOptionC = optionCField.getText().trim();
        String currentOptionD = optionDField.getText().trim();
        String currentCorrectAnswer = correctAnswerComboBox.getSelectedItem() != null ? correctAnswerComboBox.getSelectedItem().toString() : "";

        promptBuilder.append("Bạn là một trợ lý chuyên gia về tiếng Nhật JLPT, có khả năng tạo và phân tích câu hỏi trắc nghiệm. Hãy trả lời một cách chính xác, ngắn gọn. ");
        if ("question_full".equals(suggestionType) || "options_distractors".equals(suggestionType) || "image_question".equals(suggestionType) ) {
             promptBuilder.append("Hãy trả lời bằng định dạng JSON được yêu cầu. ");
        }
        promptBuilder.append("\n--- NGỮ CẢNH HIỆN TẠI ---\n");
        promptBuilder.append("Trình độ: ").append(currentLevel).append(", Loại câu hỏi mong muốn: ").append(currentType).append(".\n");

        if ("question_full".equals(suggestionType)) {
            promptBuilder.append("YÊU CẦU: Tạo một câu hỏi tiếng Nhật HOÀN CHỈNH (nội dung câu hỏi, 4 lựa chọn A, B, C, D, đáp án đúng là chữ cái A/B/C/D, và giải thích ngắn gọn cho đáp án đúng).");
            if (!userAiPrompt.isEmpty()) {
                promptBuilder.append(" Dựa trên gợi ý/chủ đề từ người dùng: '").append(userAiPrompt).append("'.");
            }
            promptBuilder.append("\nĐỊNH DẠNG JSON OUTPUT:\n").append(Constants.AI_JSON_FORMAT_FULL_QUESTION);
        } else if ("options_distractors".equals(suggestionType)) {
            if (currentQuestionText.isEmpty()) { UIUtils.showWarningMessage(this, "Vui lòng nhập nội dung câu hỏi trước."); return null; }
            promptBuilder.append("Câu hỏi đã có: '").append(currentQuestionText).append("'.");
            
            if (currentCorrectAnswer != null && !"-".equals(currentCorrectAnswer)) {
                String correctAnswerText = "";
                if ("A".equals(currentCorrectAnswer) && !currentOptionA.isEmpty()) correctAnswerText = currentOptionA;
                else if ("B".equals(currentCorrectAnswer) && !currentOptionB.isEmpty()) correctAnswerText = currentOptionB;
                else if ("C".equals(currentCorrectAnswer) && !currentOptionC.isEmpty()) correctAnswerText = currentOptionC;
                else if ("D".equals(currentCorrectAnswer) && !currentOptionD.isEmpty()) correctAnswerText = currentOptionD;
                
                if (!correctAnswerText.isEmpty()) {
                    promptBuilder.append(" Đáp án đúng đã xác định là '").append(currentCorrectAnswer).append(": ").append(correctAnswerText).append("'.");
                } else if (!"-".equals(currentCorrectAnswer)){
                    promptBuilder.append(" Đáp án đúng được chọn là '").append(currentCorrectAnswer).append("', nhưng nội dung lựa chọn đó có thể chưa được điền.");
                }
            }

            promptBuilder.append("\nYÊU CẦU: Tạo ra 3 lựa chọn sai (distractors) hợp lý cho câu hỏi này, KHÁC với đáp án đúng (nếu đã cung cấp).");
            if (!userAiPrompt.isEmpty()) { promptBuilder.append(" Yêu cầu thêm: '").append(userAiPrompt).append("'."); }
            promptBuilder.append("\nĐỊNH DẠNG JSON OUTPUT:\n").append(Constants.AI_JSON_FORMAT_DISTRACTORS);
        } else if ("explanation".equals(suggestionType)) {
            if (currentQuestionText.isEmpty() || currentCorrectAnswer.isEmpty() || "-".equals(currentCorrectAnswer)) { UIUtils.showWarningMessage(this, "Vui lòng nhập đủ câu hỏi và đáp án đúng."); return null; }
            promptBuilder.append("Câu hỏi: '").append(currentQuestionText).append("'. ");
            promptBuilder.append("A: ").append(currentOptionA).append(", B: ").append(currentOptionB).append(", C: ").append(currentOptionC).append(", D: ").append(currentOptionD).append(". ");
            promptBuilder.append("Đáp án đúng: ").append(currentCorrectAnswer).append(".\n");
            promptBuilder.append("YÊU CẦU: Cung cấp một giải thích ngắn gọn và rõ ràng (không cần định dạng JSON).");
             if (!userAiPrompt.isEmpty()) { promptBuilder.append(" Yêu cầu thêm: '").append(userAiPrompt).append("'."); }
        } else if ("image_question".equals(suggestionType)) { // Dùng cho nút "AI từ Ảnh"
            promptBuilder.append("YÊU CẦU: Dựa trên hình ảnh được cung cấp, hãy tạo một câu hỏi tiếng Nhật HOÀN CHỈNH (nội dung câu hỏi, 4 lựa chọn A, B, C, D, đáp án đúng là A/B/C/D, và giải thích ngắn gọn).");
             if (!userAiPrompt.isEmpty()) { // userAiPrompt ở đây là từ aiPromptTextArea
                promptBuilder.append(" Yêu cầu bổ sung từ người dùng về hình ảnh: '").append(userAiPrompt).append("'.");
            }
            promptBuilder.append("\nĐỊNH DẠNG JSON OUTPUT:\n").append(Constants.AI_JSON_FORMAT_FULL_QUESTION);
        }
        return promptBuilder.toString();
    }

    private void getAISuggestion(String suggestionType) {
        String fullUserPrompt = buildPromptForAI(suggestionType);
        if (fullUserPrompt == null) return; // buildPromptForAI đã hiện warning
        aiSuggestionTextArea.setText("Đang xử lý yêu cầu với AI, vui lòng chờ...");
        setInteractionEnabled(false);
        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override protected String doInBackground() throws Exception { return aiService.getAiSuggestion(fullUserPrompt); }
            @Override protected void done() {
                try { 
                    String suggestion = get(); 
                    aiSuggestionTextArea.setText(suggestion != null ? suggestion : "Không nhận được gợi ý hoặc có lỗi.");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); aiSuggestionTextArea.setText("Yêu cầu AI bị gián đoạn."); logger.error("AI suggestion interrupted", e);
                } catch (ExecutionException e) { 
                    String causeMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    aiSuggestionTextArea.setText("Lỗi khi thực hiện yêu cầu AI: " + causeMsg); 
                    logger.error("AI suggestion execution error", e.getCause() != null ? e.getCause() : e);
                } finally { setInteractionEnabled(true); }
            }
        };
        worker.execute();
    }

    private void createImageQuestionWithAI() { // Dùng cho nút "AI từ Ảnh"
        String apiKey = AppConfig.getGeminiApiKey();
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.startsWith("YOUR_")) {
            UIUtils.showErrorMessage(this, "Vui lòng cấu hình Google AI (Gemini) API Key trong Cài đặt.");
            return;
        }
        if (this.selectedImageFileForAI == null || !this.selectedImageFileForAI.exists()) {
            UIUtils.showWarningMessage(this, "Vui lòng chọn một file ảnh trước bằng nút 'Chọn Ảnh...'.");
            selectImageButton.requestFocus();
            return;
        }
        final File imageToProcess = this.selectedImageFileForAI;

        // Lấy prompt từ aiPromptTextArea thay vì JOptionPane
        String userImagePromptText = aiPromptTextArea.getText().trim(); 

        aiSuggestionTextArea.setText("Đang xử lý ảnh và tạo câu hỏi với AI, vui lòng chờ...");
        setInteractionEnabled(false);

        SwingWorker<String, Void> imageWorker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                try {
                    byte[] imageBytes = Files.readAllBytes(imageToProcess.toPath());
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    String fileName = imageToProcess.getName().toLowerCase();
                    String mimeType = "image/jpeg"; 
                    if (fileName.endsWith(".png")) mimeType = "image/png";
                    else if (fileName.endsWith(".gif")) mimeType = "image/gif";
                    else if (fileName.endsWith(".bmp")) mimeType = "image/bmp";

                    // Sử dụng userImagePromptText đã lấy từ aiPromptTextArea
                    String promptForImageVision = buildPromptForAI("image_question"); // buildPromptForAI sẽ tự lấy userAiPrompt từ aiPromptTextArea

                    return aiService.getAiSuggestionFromImage(promptForImageVision, base64Image, mimeType);
                } catch (IOException e) {
                    logger.error("Lỗi khi đọc file ảnh để gửi cho AI: {}", imageToProcess.getAbsolutePath(), e);
                    return "Lỗi: Không thể đọc file ảnh: " + imageToProcess.getName();
                }
            }
            @Override
            protected void done() {
                try {
                    String suggestion = get();
                    aiSuggestionTextArea.setText(suggestion != null ? suggestion : "Không nhận được gợi ý hoặc có lỗi từ ảnh.");
                    if (suggestion != null && !suggestion.startsWith("Lỗi:")) {
                        // imagePathField đã được set khi chọn file, không cần set lại ở đây
                        UIUtils.showInformationMessage(QuestionDialog.this, "Đã nhận gợi ý từ AI cho ảnh. Xem và áp dụng nếu muốn.");
                    }
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); aiSuggestionTextArea.setText("Yêu cầu AI từ ảnh bị gián đoạn."); }
                catch (ExecutionException e) { 
                    String causeMsg = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
                    aiSuggestionTextArea.setText("Lỗi khi thực hiện yêu cầu AI từ ảnh: " + causeMsg); 
                    logger.error("AI from image execution error", e.getCause() != null ? e.getCause() : e);
                }
                finally { setInteractionEnabled(true); }
            }
        };
        imageWorker.execute();
    }

    private void parseAndApplySuggestion() {
        String suggestionJsonString = aiSuggestionTextArea.getText().trim();
        if (suggestionJsonString.isEmpty() || suggestionJsonString.startsWith("Lỗi:")) {
            UIUtils.showWarningMessage(this, "Không có gợi ý hợp lệ nào để áp dụng.");
            return;
        }

        try {
            // Cố gắng trích xuất JSON từ markdown hoặc trực tiếp
            Pattern patternMarkdown = Pattern.compile("```(?:json)?\\s*(\\{[\\s\\S]*?\\})\\s*```", Pattern.DOTALL);
            Matcher matcherMarkdown = patternMarkdown.matcher(suggestionJsonString);
            String jsonToParse;
            if (matcherMarkdown.find()) {
                jsonToParse = matcherMarkdown.group(1).trim();
            } else {
                // Nếu không có markdown, tìm JSON object đầu tiên
                int firstBrace = suggestionJsonString.indexOf('{');
                int lastBrace = suggestionJsonString.lastIndexOf('}');
                if (firstBrace != -1 && lastBrace > firstBrace) {
                    jsonToParse = suggestionJsonString.substring(firstBrace, lastBrace + 1);
                } else {
                    UIUtils.showErrorMessage(this, "Không tìm thấy cấu trúc JSON hợp lệ trong gợi ý.");
                    return;
                }
            }
            
            JsonElement parsedElement = JsonParser.parseString(jsonToParse);
            if (!parsedElement.isJsonObject()) {
                UIUtils.showErrorMessage(this, "Gợi ý không phải là đối tượng JSON hợp lệ.");
                return;
            }
            JsonObject suggestionObj = parsedElement.getAsJsonObject();

            boolean appliedSomething = false;

            // Áp dụng vào các trường (cẩn thận không ghi đè nếu không có key)
            if (suggestionObj.has("question_text") && suggestionObj.get("question_text").isJsonPrimitive()) {
                questionTextArea.setText(suggestionObj.get("question_text").getAsString());
                appliedSomething = true;
            }
            if (suggestionObj.has("option_a") && suggestionObj.get("option_a").isJsonPrimitive()) {
                optionAField.setText(suggestionObj.get("option_a").getAsString());
                appliedSomething = true;
            }
            if (suggestionObj.has("option_b") && suggestionObj.get("option_b").isJsonPrimitive()) {
                optionBField.setText(suggestionObj.get("option_b").getAsString());
                appliedSomething = true;
            }
            if (suggestionObj.has("option_c") && suggestionObj.get("option_c").isJsonPrimitive()) {
                optionCField.setText(suggestionObj.get("option_c").getAsString());
                appliedSomething = true;
            }
            if (suggestionObj.has("option_d") && suggestionObj.get("option_d").isJsonPrimitive()) {
                optionDField.setText(suggestionObj.get("option_d").getAsString());
                appliedSomething = true;
            }
            if (suggestionObj.has("correct_answer") && suggestionObj.get("correct_answer").isJsonPrimitive()) {
                String correctAnswer = suggestionObj.get("correct_answer").getAsString().toUpperCase();
                if (Arrays.asList("A", "B", "C", "D").contains(correctAnswer)) {
                    correctAnswerComboBox.setSelectedItem(correctAnswer);
                    appliedSomething = true;
                } else {
                    UIUtils.showWarningMessage(this, "Đáp án đúng từ AI ('" + correctAnswer + "') không hợp lệ. Vui lòng chọn thủ công.");
                }
            }
            if (suggestionObj.has("explanation") && suggestionObj.get("explanation").isJsonPrimitive()) {
                explanationTextArea.setText(suggestionObj.get("explanation").getAsString());
                appliedSomething = true;
            }
            
            // Áp dụng cho các distractors (nếu có)
            if (suggestionObj.has("distractor_1") && !suggestionObj.has("question_text")) { // Giả định đây là response chỉ cho distractors
                List<String> distractors = new ArrayList<>();
                if (suggestionObj.has("distractor_1") && suggestionObj.get("distractor_1").isJsonPrimitive()) distractors.add(suggestionObj.get("distractor_1").getAsString());
                if (suggestionObj.has("distractor_2") && suggestionObj.get("distractor_2").isJsonPrimitive()) distractors.add(suggestionObj.get("distractor_2").getAsString());
                if (suggestionObj.has("distractor_3") && suggestionObj.get("distractor_3").isJsonPrimitive()) distractors.add(suggestionObj.get("distractor_3").getAsString());

                if (!distractors.isEmpty()) {
                    String currentCorrectAnswerChoice = (String) correctAnswerComboBox.getSelectedItem();
                    JTextField[] optionFieldsArray = {optionAField, optionBField, optionCField, optionDField};
                    String[] optionLetters = {"A", "B", "C", "D"};
                    
                    int distIdx = 0;
                    for (int i = 0; i < optionFieldsArray.length; i++) {
                        if (distIdx >= distractors.size()) break;
                        // Chỉ điền vào các lựa chọn không phải là đáp án đúng đã chọn
                        if (currentCorrectAnswerChoice != null && !"-".equals(currentCorrectAnswerChoice) && optionLetters[i].equals(currentCorrectAnswerChoice)) {
                            continue; 
                        }
                        // Và chỉ điền nếu lựa chọn đó đang rỗng
                        if(optionFieldsArray[i].getText().trim().isEmpty()){
                           optionFieldsArray[i].setText(distractors.get(distIdx++));
                           appliedSomething = true; 
                        }
                    }
                }
            }

            if (appliedSomething) {
                UIUtils.showInformationMessage(this, "Đã áp dụng gợi ý từ AI vào các trường.");
            } else {
                UIUtils.showWarningMessage(this, "Không tìm thấy trường nào phù hợp trong gợi ý AI để tự động áp dụng, hoặc định dạng JSON không khớp.");
            }

        } catch (JsonSyntaxException e) {
            UIUtils.showErrorMessage(this, "Gợi ý không phải là định dạng JSON hợp lệ hoặc có lỗi cú pháp: " + e.getMessage());
            logger.warn("Lỗi phân tích JSON từ gợi ý AI: {}", e.getMessage());
        } catch (IllegalStateException e) {
            UIUtils.showErrorMessage(this, "Lỗi trạng thái khi phân tích JSON (ví dụ: key không phải string): " + e.getMessage());
            logger.warn("Lỗi trạng thái phân tích JSON từ gợi ý AI: {}", e.getMessage());
        }
         catch (Exception e) {
            UIUtils.showErrorMessage(this, "Lỗi không xác định khi áp dụng gợi ý: " + e.getMessage());
            logger.error("Lỗi không xác định khi áp dụng gợi ý AI", e);
        }
    }


    private void setInteractionEnabled(boolean enabled) {
        saveButton.setEnabled(enabled); cancelButton.setEnabled(enabled);
        questionTextArea.setEnabled(enabled); optionAField.setEnabled(enabled); optionBField.setEnabled(enabled);
        optionCField.setEnabled(enabled); optionDField.setEnabled(enabled); correctAnswerComboBox.setEnabled(enabled);
        explanationTextArea.setEnabled(enabled); levelComboBox.setEnabled(enabled); typeComboBox.setEnabled(enabled);
        tagsField.setEnabled(enabled); sourceField.setEnabled(enabled); 
        
        selectAudioButton.setEnabled(enabled);
        selectImageButton.setEnabled(enabled);
        clearImagePathButton.setEnabled(enabled);
        clearAudioPathButton.setEnabled(enabled);
        
        aiPromptTextArea.setEnabled(enabled);
        suggestQuestionButton.setEnabled(enabled); suggestOptionsButton.setEnabled(enabled);
        suggestExplanationButton.setEnabled(enabled); createImageQuestionButton.setEnabled(enabled);
        applyAISuggestionButton.setEnabled(enabled);
    }

    private boolean validateFields(){
        if (questionTextArea.getText().trim().isEmpty()) { UIUtils.showErrorMessage(this, "Nội dung câu hỏi không được để trống."); questionTextArea.requestFocus(); return false; }
        if (optionAField.getText().trim().isEmpty() || optionBField.getText().trim().isEmpty()) { UIUtils.showErrorMessage(this, "Ít nhất phải có Lựa chọn A và B."); optionAField.requestFocus(); return false; }
        if ("-".equals(correctAnswerComboBox.getSelectedItem())) { UIUtils.showErrorMessage(this, "Vui lòng chọn đáp án đúng."); correctAnswerComboBox.requestFocus(); return false; }
        String selectedLevel = (String) levelComboBox.getSelectedItem();
        if (selectedLevel == null || Constants.ALL_FILTER_OPTION.equals(selectedLevel) || "N/A".equals(selectedLevel) || selectedLevel.trim().isEmpty()) { 
            UIUtils.showErrorMessage(this, "Vui lòng chọn cấp độ hợp lệ."); levelComboBox.requestFocus(); return false; 
        }
        String selectedType = (String) typeComboBox.getSelectedItem();
        if (selectedType == null || Constants.ALL_FILTER_OPTION.equals(selectedType) || "N/A".equals(selectedType) || selectedType.trim().isEmpty()) { 
            UIUtils.showErrorMessage(this, "Vui lòng chọn loại câu hỏi hợp lệ."); typeComboBox.requestFocus(); return false; 
        }
        return true;
    }

    private void saveQuestion() {
        if (!validateFields()) return;

        // questionToEdit là đối tượng đang được chỉnh sửa (đã được khởi tạo)
        // Gán giá trị từ UI vào questionToEdit
        questionToEdit.setQuestionText(questionTextArea.getText().trim());
        questionToEdit.setOptionA(optionAField.getText().trim());
        questionToEdit.setOptionB(optionBField.getText().trim());
        questionToEdit.setOptionC(optionCField.getText().trim());
        questionToEdit.setOptionD(optionDField.getText().trim());
        questionToEdit.setCorrectAnswer((String) correctAnswerComboBox.getSelectedItem());
        questionToEdit.setExplanation(explanationTextArea.getText().trim());
        questionToEdit.setLevel((String) levelComboBox.getSelectedItem());
        questionToEdit.setType((String) typeComboBox.getSelectedItem());
        questionToEdit.setTags(tagsField.getText().trim());
        questionToEdit.setSource(sourceField.getText().trim());

        // Lấy đường dẫn tuyệt đối trực tiếp từ các text field
        // Nếu suppressSaveMessages là true (sửa từ CreateMultipleQuestionsDialog),
        // thì các đường dẫn này đã được cập nhật vào questionToEdit bởi selectFile hoặc clearButton
        // Nếu không suppress (sửa/thêm từ QuestionManagementPanel),
        // nó cũng đã được cập nhật bởi selectFile/clearButton trước khi nhấn Lưu.
        // Chỉ cần đảm bảo nó là null nếu field rỗng.

        String imagePathFromField = imagePathField.getText().trim();
        questionToEdit.setImagePath(imagePathFromField.isEmpty() ? null : imagePathFromField);

        String audioPathFromField = audioPathField.getText().trim();
        questionToEdit.setAudioPath(audioPathFromField.isEmpty() ? null : audioPathFromField);
        
        if (questionToEdit.getImagePath() != null) {
            logger.info("Saving image path (absolute): {}", questionToEdit.getImagePath());
        }
        if (questionToEdit.getAudioPath() != null) {
            logger.info("Saving audio path (absolute): {}", questionToEdit.getAudioPath());
        }

        // Bỏ qua hoàn toàn logic gọi FileManagementService.saveFile()
        // Bỏ qua logic xóa file cũ từ FileManagementService

        boolean successOperation = false;
        String messageForUser = "";

        if (isEditMode) {
            if (this.suppressSaveMessages) {
                successOperation = true; // Sửa trong bộ nhớ (cho CreateMultipleQuestionsDialog) luôn thành công
            } else if (questionToEdit.getId() > 0) { // Sửa câu hỏi đã có trong DB
                successOperation = questionService.updateQuestion(questionToEdit);
                messageForUser = successOperation ? Constants.MSG_UPDATE_SUCCESS : Constants.MSG_UPDATE_FAIL;
            } else {
                // isEditMode=true nhưng ID=0: trường hợp này không nên xảy ra nếu luồng đúng.
                // Nếu xảy ra, đó là lỗi logic. QuestionDialog khi sửa phải có ID > 0
                // hoặc suppressSaveMessages=true (nếu sửa từ CreateMultipleQuestionsDialog cho câu hỏi chưa có ID thật)
                logger.error("Logic error: Edit mode but question ID is 0 and suppressSaveMessages is false.");
                messageForUser = "Lỗi logic: Không thể cập nhật câu hỏi chưa có ID.";
                successOperation = false;
            }
        } else { // Tạo mới (isEditMode = false)
             if (!this.suppressSaveMessages) { // Thêm mới vào DB
                successOperation = questionService.addQuestion(questionToEdit);
                messageForUser = successOperation ? Constants.MSG_SAVE_SUCCESS : Constants.MSG_SAVE_FAIL;
             } else {
                // Không nên có trường hợp tạo mới và suppressSaveMessages=true cùng lúc.
                // Nếu có, coi như thành công cho mục đích sửa bộ nhớ (nhưng đây là lỗi thiết kế).
                successOperation = true;
                logger.warn("Logic warning: Create new question mode but suppressSaveMessages is true.");
             }
        }

        if (successOperation) {
            this.saved = true; // Đánh dấu dialog này đã hoàn thành "lưu"
            if (!this.suppressSaveMessages && !messageForUser.isEmpty()) {
                UIUtils.showInformationMessage(this, messageForUser + (questionToEdit.getId() > 0 ? " (ID: " + questionToEdit.getId() + ")" : ""));
            }
            dispose();
        } else {
            if (!this.suppressSaveMessages && !messageForUser.isEmpty()) {
                UIUtils.showErrorMessage(this, messageForUser);
            }
            // Nếu suppress và thất bại (không nên xảy ra), vẫn dispose vì có thể chỉ là sửa trong bộ nhớ thất bại không mong muốn
            if(this.suppressSaveMessages) {
                logger.warn("Operation considered failed even with suppressSaveMessages=true. Disposing dialog.");
                dispose();
            }
        }
    }
    public boolean isSaved() {
        return saved;
    }
}