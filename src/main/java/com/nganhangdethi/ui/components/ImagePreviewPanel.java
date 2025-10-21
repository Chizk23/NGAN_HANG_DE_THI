package com.nganhangdethi.ui.components;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class ImagePreviewPanel extends JPanel {
    private BufferedImage image;
    private int preferredWidth;
    private int preferredHeight;
    private String errorMessage;

    public ImagePreviewPanel(int preferredWidth, int preferredHeight) {
        this.preferredWidth = preferredWidth;
        this.preferredHeight = preferredHeight;
        setPreferredSize(new Dimension(preferredWidth, preferredHeight));
        setBorder(BorderFactory.createLineBorder(Color.GRAY));
        setBackground(Color.WHITE);
    }

    public void setImage(File imageFile) {
        this.errorMessage = null;
        if (imageFile == null || !imageFile.exists()) {
            this.image = null;
            this.errorMessage = "File không tồn tại.";
            repaint();
            return;
        }
        try {
            this.image = ImageIO.read(imageFile);
            // Điều chỉnh preferred size dựa trên tỷ lệ ảnh nếu muốn
            // Hoặc để paintComponent tự scale
            repaint();
        } catch (IOException e) {
            this.image = null;
            this.errorMessage = "Lỗi đọc file ảnh.";
            System.err.println("Error reading image file: " + imageFile.getPath() + " - " + e.getMessage());
            repaint();
        }
    }
    
    public void setErrorMessage(String message) {
        this.image = null;
        this.errorMessage = message;
        repaint();
    }

    public void clearImage() {
        this.image = null;
        this.errorMessage = null;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        if (image != null) {
            // Tính toán tỷ lệ để fit ảnh vào panel mà vẫn giữ tỷ lệ khung hình
            int panelWidth = getWidth();
            int panelHeight = getHeight();
            
            double imgWidth = image.getWidth();
            double imgHeight = image.getHeight();
            
            double scale = Math.min(panelWidth / imgWidth, panelHeight / imgHeight);
            
            int scaledWidth = (int) (imgWidth * scale);
            int scaledHeight = (int) (imgHeight * scale);
            
            int x = (panelWidth - scaledWidth) / 2;
            int y = (panelHeight - scaledHeight) / 2;
            
            g2d.drawImage(image, x, y, scaledWidth, scaledHeight, this);
            
        } else if (errorMessage != null) {
            g2d.setColor(Color.RED);
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(errorMessage);
            int msgHeight = fm.getAscent();
            g2d.drawString(errorMessage, (getWidth() - msgWidth) / 2, (getHeight() + msgHeight) / 2);
        } else {
             g2d.setColor(Color.LIGHT_GRAY);
             String noImageMsg = "Không có hình ảnh";
            FontMetrics fm = g2d.getFontMetrics();
            int msgWidth = fm.stringWidth(noImageMsg);
            int msgHeight = fm.getAscent();
            g2d.drawString(noImageMsg, (getWidth() - msgWidth) / 2, (getHeight() + msgHeight) / 2);
        }
        g2d.dispose();
    }
}