package com.nganhangdethi.ui;

import com.nganhangdethi.ui.panels.ExamManagementPanel; // Giả sử bạn sẽ tạo panel này
import com.nganhangdethi.ui.panels.QuestionManagementPanel;
import com.nganhangdethi.ui.dialogs.SettingsDialog;
import com.nganhangdethi.util.Constants;
import com.nganhangdethi.util.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class MainFrame extends JFrame {

    private JTabbedPane tabbedPane;
    private QuestionManagementPanel questionManagementPanel;
    private ExamManagementPanel examManagementPanel; // Sẽ được phát triển sau

    public MainFrame() {
        setTitle(Constants.APP_NAME + " - v" + Constants.APP_VERSION);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 800); // Kích thước lớn hơn một chút
        setLocationRelativeTo(null);

        // Nên gọi UIUtils.applyGlobalFontSettings() ở MainApplication trước khi tạo MainFrame
        // Hoặc bạn có thể áp dụng font cụ thể ở đây:
        // UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(13f));


        initComponents();
        createMenuBar();

        // Áp dụng font một cách đệ quy sau khi tất cả components được thêm vào
        // Điều này đảm bảo các component con cũng nhận font.
        // Tốt hơn là set LookAndFeel và font mặc định ở MainApplication.
        UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(13f));
        tabbedPane.setFont(UIUtils.getJapaneseFont(Font.BOLD, 14f)); // Font riêng cho tab
        getJMenuBar().setFont(UIUtils.getJapaneseFont(13f)); // Font cho menubar
        for(int i=0; i < getJMenuBar().getMenuCount(); i++) {
            getJMenuBar().getMenu(i).setFont(UIUtils.getJapaneseFont(13f));
            for(Component menuItem : getJMenuBar().getMenu(i).getMenuComponents()){
                menuItem.setFont(UIUtils.getJapaneseFont(13f));
            }
        }

        System.out.println("INFO: MainFrame - MainFrame initialized and components set up.");
    }

    private void initComponents() {
        tabbedPane = new JTabbedPane();
        // tabbedPane.setFont(UIUtils.getJapaneseFont(Font.BOLD, 14f)); // Đã chuyển lên trên

        // Panel quản lý câu hỏi
        questionManagementPanel = new QuestionManagementPanel(this); // Truyền MainFrame vào
        ImageIcon questionIcon = UIUtils.createImageIcon(Constants.ICON_PATH_QUESTION_TAB, "Quản lý Câu hỏi", 16, 16);
        tabbedPane.addTab("Quản Lý Câu Hỏi", questionIcon, questionManagementPanel, "Quản lý ngân hàng câu hỏi và các chi tiết");

        // Panel quản lý đề thi (placeholder)
        examManagementPanel = new ExamManagementPanel(this); // Truyền MainFrame vào
        ImageIcon examIcon = UIUtils.createImageIcon(Constants.ICON_PATH_EXAM_TAB, "Quản lý Đề thi", 16, 16);
        JPanel examPanelPlaceholder = new JPanel(new BorderLayout());
        examPanelPlaceholder.add(new JLabel("Khu vực Quản Lý Đề Thi sẽ được phát triển ở đây.", SwingConstants.CENTER), BorderLayout.CENTER);
        // tabbedPane.addTab("Quản Lý Đề Thi", examIcon, examPanelPlaceholder, "Tạo và quản lý các đề thi");
        tabbedPane.addTab("Quản Lý Đề Thi", examIcon, examManagementPanel, "Tạo và quản lý các đề thi");


        add(tabbedPane, BorderLayout.CENTER);
    }

    private void createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        // menuBar.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên

        // Menu File
        JMenu fileMenu = new JMenu("Tệp");
        // fileMenu.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên
        fileMenu.setMnemonic(KeyEvent.VK_T);

        JMenuItem settingsItem = new JMenuItem("Cài đặt...", UIUtils.createImageIcon(Constants.ICON_PATH_SETTINGS, "Cài đặt", 16, 16));
        // settingsItem.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên
        settingsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        settingsItem.addActionListener(e -> openSettingsDialog());
        fileMenu.add(settingsItem);

        fileMenu.addSeparator();

        JMenuItem exitItem = new JMenuItem("Thoát", UIUtils.createImageIcon(Constants.ICON_PATH_EXIT, "Thoát", 16, 16));
        // exitItem.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()));
        exitItem.addActionListener(e -> System.exit(0));
        fileMenu.add(exitItem);

        menuBar.add(fileMenu);

        // Menu Help
        JMenu helpMenu = new JMenu("Trợ giúp");
        // helpMenu.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên
        helpMenu.setMnemonic(KeyEvent.VK_H);

        JMenuItem aboutItem = new JMenuItem("Thông tin...", UIUtils.createImageIcon(Constants.ICON_PATH_ABOUT, "Thông tin", 16, 16));
        // aboutItem.setFont(UIUtils.getJapaneseFont(13f)); // Đã chuyển lên trên
        aboutItem.addActionListener(e -> showAboutDialog());
        helpMenu.add(aboutItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void openSettingsDialog() {
        SettingsDialog settingsDialog = new SettingsDialog(this);
        settingsDialog.setVisible(true);
    }

    private void showAboutDialog() {
        String aboutMessage = String.format("%s - Phiên bản %s\n\n" +
                        "Ứng dụng hỗ trợ quản lý ngân hàng đề thi Tiếng Nhật,\n" +
                        "tạo đề thi, và các tính năng tiện ích khác.\n\n" +
                        "Phát triển bởi [Tên của bạn/Nhóm của bạn]",
                Constants.APP_NAME, Constants.APP_VERSION);

        JTextArea textArea = new JTextArea(aboutMessage);
        textArea.setFont(UIUtils.getJapaneseFont(13f));
        textArea.setEditable(false);
        textArea.setOpaque(false); // Làm cho nền của JTextArea trong suốt
        textArea.setWrapStyleWord(true);
        textArea.setLineWrap(true);

        // Để JTextArea tự động điều chỉnh kích thước
        // JOptionPane sẽ tự điều chỉnh kích thước dialog cho phù hợp
        JOptionPane.showMessageDialog(this, textArea, "Thông tin ứng dụng", JOptionPane.INFORMATION_MESSAGE);
    }
}