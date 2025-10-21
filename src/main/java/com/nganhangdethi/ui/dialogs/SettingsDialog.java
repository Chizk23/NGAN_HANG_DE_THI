package com.nganhangdethi.ui.dialogs; // Giữ nguyên package của bạn

import com.nganhangdethi.util.AppConfig;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter; // Thêm nếu bạn dùng MouseListener trực tiếp ở đây
import java.net.URI; // Cho Desktop.browse

// --- THÊM CÁC IMPORT CHO LOGGER ---
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// ---------------------------------


public class SettingsDialog extends JDialog {
    // --- KHAI BÁO LOGGER ---
    private static final Logger logger = LoggerFactory.getLogger(SettingsDialog.class);
    // -----------------------

    private JPasswordField apiKeyField;
    private JButton saveButton;
    private JButton cancelButton;
    private JLabel apiKeyLabelInfo;

    public SettingsDialog(Frame owner) {
        super(owner, "Cài đặt ứng dụng", true);
        initComponents();
        loadSettings();

        setSize(550, 220);
        setMinimumSize(new Dimension(500, 200));
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        JLabel apiKeyLabel = new JLabel("Google AI (Gemini) API Key:");
        formPanel.add(apiKeyLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 1.0; gbc.fill = GridBagConstraints.HORIZONTAL;
        apiKeyField = new JPasswordField(35);
        apiKeyField.setToolTipText("Nhập API Key bạn nhận được từ Google AI Studio.");
        formPanel.add(apiKeyField, gbc);
        gbc.weightx = 0;

        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.WEST;
        apiKeyLabelInfo = new JLabel("<html><i>Lấy API Key của bạn từ <a href=''>Google AI Studio</a>.</i></html>"); // Bỏ href rỗng ở đây, sẽ set trong code
        apiKeyLabelInfo.setCursor(new Cursor(Cursor.HAND_CURSOR));
        apiKeyLabelInfo.addMouseListener(new MouseAdapter() { // Sử dụng java.awt.event.MouseAdapter
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                try {
                    Desktop.getDesktop().browse(new URI("https://aistudio.google.com/app/apikey"));
                } catch (Exception e) {
                    // Sử dụng logger đã khai báo
                    logger.error("Không thể mở link lấy API Key.", e);
                    UIUtils.showErrorMessage(SettingsDialog.this, "Không thể mở trình duyệt. Vui lòng truy cập thủ công:\nhttps://aistudio.google.com/app/apikey");
                }
            }
        });
        formPanel.add(apiKeyLabelInfo, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        saveButton = new JButton("Lưu cài đặt", UIUtils.createImageIcon(Constants.ICON_PATH_SAVE, "Lưu", 16, 16));
        saveButton.addActionListener(e -> saveSettings());

        cancelButton = new JButton("Hủy", UIUtils.createImageIcon(Constants.ICON_PATH_CANCEL, "Hủy", 16, 16));
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        Font defaultFont = UIUtils.getJapaneseFont(13f);
        if (defaultFont != null) {
            UIUtils.setFontRecursively(formPanel, defaultFont);
            saveButton.setFont(defaultFont.deriveFont(Font.BOLD));
            cancelButton.setFont(defaultFont);
            if (apiKeyLabel != null) apiKeyLabel.setFont(defaultFont.deriveFont(Font.BOLD));
        }

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadSettings() {
        String apiKey = AppConfig.getGeminiApiKey();
        if (apiKey != null && !apiKey.trim().isEmpty() &&
            !"YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE".equalsIgnoreCase(apiKey.trim())) {
            apiKeyField.setText(apiKey);
        } else {
            apiKeyField.setText("");
        }
    }

    private void saveSettings() {
        String apiKey = new String(apiKeyField.getPassword());

        if (apiKey.trim().isEmpty()){
            int choice = UIUtils.showConfirmDialog(this,
                    "Bạn chưa nhập API Key. Nếu bạn để trống, các chức năng AI sẽ không hoạt động.\n" +
                    "API Key hiện tại (nếu có) sẽ bị xóa.\n\nBạn có chắc chắn muốn tiếp tục không?",
                    "Xác nhận API Key rỗng");
            if(choice == JOptionPane.NO_OPTION) {
                return;
            }
        } else if ("YOUR_GOOGLE_AI_STUDIO_API_KEY_HERE".equalsIgnoreCase(apiKey.trim())) {
            UIUtils.showWarningMessage(this, "Vui lòng thay thế giá trị placeholder bằng API Key thực của bạn.");
            apiKeyField.requestFocus();
            return;
        }

        AppConfig.saveGeminiApiKey(apiKey.trim());
        UIUtils.showInformationMessage(this, "Cài đặt API Key cho Google AI (Gemini) đã được lưu.");
        dispose();
    }
}