package com.nganhangdethi.util;

// Import cho PDFBox
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
// import org.apache.pdfbox.pdmodel.font.PDType1Font; // Nếu muốn có font fallback chuẩn (không tiếng Nhật)

// Import cho Swing và AWT
import javax.swing.*;
import java.awt.*;
import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UIUtils {

    private static Font swingJapaneseFont;
    private static Font swingJapaneseFontBold;

    static {
        loadSwingJapaneseFont(); // Load font cho Swing khi lớp được khởi tạo
    }

    /**
     * Tải font tiếng Nhật cho giao diện Swing từ resource path.
     * Font được cache lại để sử dụng sau này.
     */
    private static void loadSwingJapaneseFont() {
        if (Constants.FONT_RESOURCE_PATH == null || Constants.FONT_RESOURCE_PATH.trim().isEmpty()) {
            System.err.println("ERROR (UIUtils): Constants.FONT_RESOURCE_PATH is not defined. Using SansSerif fallback for Swing.");
            swingJapaneseFont = new Font("SansSerif", Font.PLAIN, 13);
            swingJapaneseFontBold = new Font("SansSerif", Font.BOLD, 13);
            return;
        }

        try (InputStream is = UIUtils.class.getResourceAsStream(Constants.FONT_RESOURCE_PATH)) {
            if (is == null) {
                System.err.println("ERROR (UIUtils): Không tìm thấy file font cho Swing: " +
                                   Constants.FONT_RESOURCE_PATH + ". Sử dụng SansSerif làm font thay thế.");
                swingJapaneseFont = new Font("SansSerif", Font.PLAIN, 13);
                swingJapaneseFontBold = new Font("SansSerif", Font.BOLD, 13);
                return;
            }
            Font baseFont = Font.createFont(Font.TRUETYPE_FONT, is);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(baseFont);

            swingJapaneseFont = baseFont.deriveFont(Font.PLAIN, 13f);
            swingJapaneseFontBold = baseFont.deriveFont(Font.BOLD, 13f);

            System.out.println("INFO (UIUtils): Đã tải thành công font tiếng Nhật cho Swing: " +
                               (swingJapaneseFont != null ? swingJapaneseFont.getFontName() : "Không có") +
                               " từ " + Constants.FONT_RESOURCE_PATH);
        } catch (FontFormatException | IOException e) {
            System.err.println("ERROR (UIUtils): Lỗi khi tải font tiếng Nhật cho Swing từ " +
                               Constants.FONT_RESOURCE_PATH + ": " + e.getMessage());
            e.printStackTrace();
            swingJapaneseFont = new Font("SansSerif", Font.PLAIN, 13);
            swingJapaneseFontBold = new Font("SansSerif", Font.BOLD, 13);
        }
    }

    public static Font getJapaneseFont() {
        return swingJapaneseFont != null ? swingJapaneseFont : new Font("SansSerif", Font.PLAIN, 13);
    }

    public static Font getJapaneseFont(float size) {
        return swingJapaneseFont != null ? swingJapaneseFont.deriveFont(size) : new Font("SansSerif", Font.PLAIN, (int)size);
    }

    public static Font getJapaneseFont(int style, float size) {
        Font baseToUse = swingJapaneseFont;
        if (style == Font.BOLD) {
            baseToUse = (swingJapaneseFontBold != null) ? swingJapaneseFontBold : swingJapaneseFont;
        }
        // Nếu style là PLAIN hoặc một style khác mà không có biến thể BOLD riêng,
        // thì baseToUse vẫn là swingJapaneseFont (hoặc null nếu load lỗi)
        // Derive từ baseToUse nếu nó không null.
        if (baseToUse != null) {
            return baseToUse.deriveFont(style, size);
        }
        // Fallback cuối cùng nếu không load được font nào
        return new Font("SansSerif", style, (int)size);
    }

    /**
     * Tải font tiếng Nhật (ví dụ NotoSansJP-Regular.ttf) để sử dụng với thư viện PDFBox.
     * Font này sẽ được nhúng (subset) vào file PDF.
     *
     * @param document Đối tượng PDDocument mà font sẽ được load vào.
     * @return PDType0Font đã được load, hoặc ném IOException nếu có lỗi.
     * @throws IOException Nếu không tìm thấy file font hoặc có lỗi trong quá trình đọc/load.
     */
    public static PDType0Font getJapanesePdfFont(PDDocument document) throws IOException {
        if (document == null) {
            throw new IllegalArgumentException("PDDocument không được null khi load font PDF.");
        }
        if (Constants.FONT_RESOURCE_PATH == null || Constants.FONT_RESOURCE_PATH.trim().isEmpty()) {
            System.err.println("ERROR (UIUtils): Constants.FONT_RESOURCE_PATH is not defined for PDF font loading.");
            throw new IOException("Đường dẫn file font (FONT_RESOURCE_PATH) chưa được định nghĩa trong Constants.");
        }

        try (InputStream is = UIUtils.class.getResourceAsStream(Constants.FONT_RESOURCE_PATH)) {
            if (is == null) {
                System.err.println("ERROR (UIUtils): Không tìm thấy file font cho PDF: " + Constants.FONT_RESOURCE_PATH);
                throw new IOException("Không tìm thấy tài nguyên font: " + Constants.FONT_RESOURCE_PATH + ". Hãy kiểm tra đường dẫn và đảm bảo file nằm trong thư mục resources.");
            }
            PDType0Font pdfFont = PDType0Font.load(document, is, true);
            System.out.println("INFO (UIUtils): Đã tải thành công font tiếng Nhật cho PDF: " + pdfFont.getName() + " từ " + Constants.FONT_RESOURCE_PATH);
            return pdfFont;
        } catch (IOException e) { // Bắt lỗi cụ thể từ PDType0Font.load hoặc getResourceAsStream
            System.err.println("ERROR (UIUtils): Lỗi nghiêm trọng khi tải font tiếng Nhật cho PDF từ " +
                               Constants.FONT_RESOURCE_PATH + ": " + e.getMessage());
            e.printStackTrace();
            throw e; // Ném lại lỗi để lớp gọi (ví dụ: ExportService) xử lý
        }
    }

    public static void setFontRecursively(Component component, Font font) {
        if (component == null || font == null) return;
        component.setFont(font);
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setFontRecursively(child, font);
            }
        }
    }

    public static ImageIcon createImageIcon(String path, String description) {
        if (path == null || path.trim().isEmpty()) {
            System.err.println("Warning (UIUtils): Đường dẫn đến hình ảnh là null hoặc rỗng.");
            return null;
        }
        URL imgURL = UIUtils.class.getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            System.err.println("Không tìm thấy file: " + path + ". Kiểm tra đường dẫn có bắt đầu bằng '/' và đúng trong thư mục resources không.");
            return null;
        }
    }

    public static ImageIcon createImageIcon(String path, String description, int width, int height) {
        ImageIcon originalIcon = createImageIcon(path, description);
        if (originalIcon != null) {
            if (width <= 0 || height <= 0) {
                 System.err.println("Warning (UIUtils): Kích thước không hợp lệ để thay đổi kích thước ảnh: " + width + "x" + height);
                 return originalIcon;
            }
            Image img = originalIcon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(img);
        }
        return null;
    }

    public static void showInformationMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, Constants.APP_NAME, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showWarningMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Cảnh báo", JOptionPane.WARNING_MESSAGE);
    }

    public static void showErrorMessage(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    public static int showConfirmDialog(Component parent, String message, String title) {
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

     public static int showConfirmDialog(Component parent, String message) {
        // Sử dụng title mặc định từ Constants nếu Constants.MSG_CONFIRM_DELETE_TITLE được định nghĩa
        String title = (Constants.MSG_CONFIRM_DELETE_TITLE != null) ? Constants.MSG_CONFIRM_DELETE_TITLE : "Xác nhận";
        return JOptionPane.showConfirmDialog(parent, message, title, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
}