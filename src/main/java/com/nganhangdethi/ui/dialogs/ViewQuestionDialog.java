package com.nganhangdethi.ui.dialogs;

import com.nganhangdethi.model.Question;
import com.nganhangdethi.util.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import javax.imageio.ImageIO; // Import ImageIO

public class ViewQuestionDialog extends JDialog {
    private static final Logger logger = LoggerFactory.getLogger(ViewQuestionDialog.class);

    private Question question;

    // Các thành phần UI để hiển thị thông tin câu hỏi
    private JTextArea questionTextDisplay;
    private JLabel optionADisplay, optionBDisplay, optionCDisplay, optionDDisplay;
    private JLabel correctAnswerDisplay;
    private JTextArea explanationDisplay;
    private JLabel levelDisplay, typeDisplay, tagsDisplay, sourceDisplay;
    private JLabel imageDisplayLabel; // JLabel để hiển thị ảnh
    private JLabel audioPathDisplay; // JLabel để hiển thị đường dẫn audio

    private static final int MAX_IMAGE_WIDTH = 400;
    private static final int MAX_IMAGE_HEIGHT = 300;


    public ViewQuestionDialog(Frame owner, Question question) {
        super(owner, "Xem Chi Tiết Câu Hỏi (ID: " + question.getId() + ")", true);
        this.question = question;

        initComponents();
        loadQuestionDetails();

        // Kích thước dialog có thể cần điều chỉnh tùy thuộc vào nội dung
        setSize(750, 750); // Tăng chiều cao để có không gian cho ảnh
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel detailsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int yPos = 0;

        // Nội dung câu hỏi
        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.NORTHEAST;
        detailsPanel.add(new JLabel("Nội dung:"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos++; gbc.gridwidth = 3; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.BOTH;
        questionTextDisplay = new JTextArea(5, 40);
        questionTextDisplay.setEditable(false);
        questionTextDisplay.setLineWrap(true);
        questionTextDisplay.setWrapStyleWord(true);
        detailsPanel.add(new JScrollPane(questionTextDisplay), gbc);
        gbc.gridwidth = 1; gbc.weightx = 0; gbc.fill = GridBagConstraints.HORIZONTAL;

        // Các lựa chọn
        optionADisplay = addDetailRow(detailsPanel, gbc, yPos++, "Lựa chọn A:");
        optionBDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Lựa chọn B:");
        optionCDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Lựa chọn C:");
        optionDDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Lựa chọn D:");
        correctAnswerDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Đáp án đúng:");

        // Giải thích
        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.NORTHEAST;
        detailsPanel.add(new JLabel("Giải thích:"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos++; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH;
        explanationDisplay = new JTextArea(3, 40);
        explanationDisplay.setEditable(false);
        explanationDisplay.setLineWrap(true);
        explanationDisplay.setWrapStyleWord(true);
        detailsPanel.add(new JScrollPane(explanationDisplay), gbc);
        gbc.gridwidth = 1;

        // Các thông tin khác
        levelDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Cấp độ:");
        typeDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Loại:");
        tagsDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Tags:");
        sourceDisplay = addDetailRow(detailsPanel, gbc, yPos++, "Nguồn:");
        audioPathDisplay = addDetailRow(detailsPanel, gbc, yPos++, "File Audio:");


        // Hiển thị ảnh
        gbc.gridx = 0; gbc.gridy = yPos; gbc.anchor = GridBagConstraints.NORTHEAST;
        detailsPanel.add(new JLabel("Hình ảnh:"), gbc);
        gbc.gridx = 1; gbc.gridy = yPos++; gbc.gridwidth = 3; gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0; // Cho phép panel ảnh mở rộng nếu cần
        imageDisplayLabel = new JLabel("Không có ảnh", SwingConstants.CENTER);
        imageDisplayLabel.setPreferredSize(new Dimension(MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT));
        imageDisplayLabel.setBorder(BorderFactory.createEtchedBorder());
        JScrollPane imageScrollPane = new JScrollPane(imageDisplayLabel); // Cho phép cuộn nếu ảnh lớn
        imageScrollPane.setPreferredSize(new Dimension(MAX_IMAGE_WIDTH + 20, MAX_IMAGE_HEIGHT + 20));
        detailsPanel.add(imageScrollPane, gbc);
        gbc.weighty = 0;


        // Nút Đóng
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton closeButton = new JButton("Đóng");
        closeButton.addActionListener(e -> dispose());
        buttonPanel.add(closeButton);

        JScrollPane mainScrollPane = new JScrollPane(detailsPanel);
        mainScrollPane.setBorder(BorderFactory.createEmptyBorder());
        mainScrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(mainScrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        Font defaultFont = UIUtils.getJapaneseFont(13f);
        if (defaultFont != null) {
            UIUtils.setFontRecursively(this, defaultFont);
            closeButton.setFont(defaultFont.deriveFont(Font.BOLD));
        }
    }

    private JLabel addDetailRow(JPanel panel, GridBagConstraints gbc, int yPos, String labelText) {
        gbc.gridx = 0; gbc.gridy = yPos; gbc.gridwidth = 1; gbc.anchor = GridBagConstraints.NORTHEAST;
        panel.add(new JLabel(labelText), gbc);
        JLabel valueLabel = new JLabel();
        gbc.gridx = 1; gbc.gridy = yPos; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.WEST;
        panel.add(valueLabel, gbc);
        return valueLabel;
    }

    private void loadQuestionDetails() {
        if (question == null) return;

        questionTextDisplay.setText(question.getQuestionText());
        optionADisplay.setText(formatOption(question.getOptionA()));
        optionBDisplay.setText(formatOption(question.getOptionB()));
        optionCDisplay.setText(formatOption(question.getOptionC()));
        optionDDisplay.setText(formatOption(question.getOptionD()));
        correctAnswerDisplay.setText(question.getCorrectAnswer());
        explanationDisplay.setText(question.getExplanation());
        levelDisplay.setText(question.getLevel());
        typeDisplay.setText(question.getType());
        tagsDisplay.setText(question.getTags());
        sourceDisplay.setText(question.getSource());
        audioPathDisplay.setText(question.getAudioPath() != null && !question.getAudioPath().isEmpty() ? question.getAudioPath() : "Không có");

        // Tải và hiển thị ảnh
        String imagePath = question.getImagePath();
        if (imagePath != null && !imagePath.trim().isEmpty()) {
            logger.info("ViewQuestionDialog: Attempting to load image from path: '{}'", imagePath);
            File imageFile = new File(imagePath);
            if (imageFile.exists() && imageFile.isFile() && imageFile.canRead()) {
                try {
                    // Đọc ảnh bằng ImageIO để xử lý lỗi tốt hơn
                    Image img = ImageIO.read(imageFile);
                    if (img != null) {
                        ImageIcon imageIcon = new ImageIcon(img);
                        // Điều chỉnh kích thước ảnh để vừa với JLabel
                        Image scaledImage = scaleImage(imageIcon.getImage(), MAX_IMAGE_WIDTH, MAX_IMAGE_HEIGHT);
                        imageDisplayLabel.setIcon(new ImageIcon(scaledImage));
                        imageDisplayLabel.setText(null); // Xóa text "Không có ảnh"
                        logger.info("ViewQuestionDialog: Image loaded successfully from '{}'", imagePath);
                    } else {
                        logger.warn("ViewQuestionDialog: ImageIO.read returned null for path: '{}'. File might be corrupted or unsupported format.", imagePath);
                        imageDisplayLabel.setIcon(null);
                        imageDisplayLabel.setText("Lỗi định dạng ảnh");
                    }
                } catch (Exception e) {
                    logger.error("ViewQuestionDialog: Error loading image from path: '{}'", imagePath, e);
                    imageDisplayLabel.setIcon(null);
                    imageDisplayLabel.setText("Lỗi tải ảnh");
                }
            } else {
                logger.warn("ViewQuestionDialog: Image file not found, is not a file, or cannot be read at path: '{}'", imagePath);
                imageDisplayLabel.setIcon(null);
                imageDisplayLabel.setText("Ảnh không tìm thấy!");
            }
        } else {
            logger.info("ViewQuestionDialog: No image path provided for this question.");
            imageDisplayLabel.setIcon(null);
            imageDisplayLabel.setText("Không có ảnh");
        }
    }
    
    private String formatOption(String optionText) {
        return (optionText == null || optionText.trim().isEmpty()) ? "<i>(Trống)</i>" : optionText;
    }

    private Image scaleImage(Image sourceImage, int maxWidth, int maxHeight) {
        int originalWidth = sourceImage.getWidth(null);
        int originalHeight = sourceImage.getHeight(null);

        if (originalWidth <= 0 || originalHeight <= 0) { // Ảnh không hợp lệ
            return sourceImage;
        }

        if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
            return sourceImage; // Không cần scale
        }

        double aspectRatio = (double) originalWidth / originalHeight;

        int newWidth = maxWidth;
        int newHeight = (int) (newWidth / aspectRatio);

        if (newHeight > maxHeight) {
            newHeight = maxHeight;
            newWidth = (int) (newHeight * aspectRatio);
        }
        
        // Đảm bảo newWidth không vượt quá maxWidth sau khi điều chỉnh newHeight
        if (newWidth > maxWidth) {
            newWidth = maxWidth;
            newHeight = (int) (newWidth / aspectRatio); // Tính lại newHeight dựa trên newWidth đã giới hạn
        }


        return sourceImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
    }
}