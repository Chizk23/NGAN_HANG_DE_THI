package com.nganhangdethi.ui.components;

import com.nganhangdethi.util.UIUtils; // Cần import

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayerPanel extends JPanel {
    private JButton playButton;
    private JButton stopButton;
    private JLabel statusLabel;
    private Clip audioClip;
    private File currentAudioFile;

    public AudioPlayerPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT));
        playButton = new JButton("Phát", UIUtils.createImageIcon("/icons/play_audio.png", "Phát", 16,16));
        stopButton = new JButton("Dừng", UIUtils.createImageIcon("/icons/stop_audio.png", "Dừng", 16,16));
        statusLabel = new JLabel("Chưa chọn file audio.");

        playButton.setEnabled(false);
        stopButton.setEnabled(false);

        playButton.addActionListener(e -> playAudio());
        stopButton.addActionListener(e -> stopAudio());

        add(playButton);
        add(stopButton);
        add(statusLabel);
        
        UIUtils.setFontRecursively(this, UIUtils.getJapaneseFont(12f));
        statusLabel.setFont(UIUtils.getJapaneseFont(Font.ITALIC, 11f));

    }

    public void setAudioFile(File audioFile) {
        stopAudio(); // Dừng audio hiện tại nếu có
        this.currentAudioFile = audioFile;
        if (audioFile != null && audioFile.exists()) {
            statusLabel.setText(audioFile.getName());
            playButton.setEnabled(true);
        } else {
            statusLabel.setText("File audio không hợp lệ.");
            playButton.setEnabled(false);
        }
    }
    
    public void setErrorMessage(String message) {
        statusLabel.setText(message);
        playButton.setEnabled(false);
        stopButton.setEnabled(false);
    }

    private void playAudio() {
        if (currentAudioFile == null) return;
        try {
            if (audioClip != null && audioClip.isRunning()) {
                audioClip.stop();
            }
            // THIS IS THE LINE THROWING THE EXCEPTION
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(currentAudioFile);
            audioClip = AudioSystem.getClip();
            // ... rest of the code
        } catch (UnsupportedAudioFileException uafe) { // CATCHES THE SPECIFIC EXCEPTION
            String shortMessage = "Lỗi: Định dạng file audio không được hỗ trợ.";
            if (currentAudioFile != null) {
                shortMessage += " (" + currentAudioFile.getName() + ")";
            }
            statusLabel.setText(shortMessage);
            // You might want to provide more user-friendly advice here
            UIUtils.showErrorMessage(this,
                    shortMessage + "\n\nVui lòng thử sử dụng file định dạng WAV, AU, hoặc AIFF.\n" +
                    "Chi tiết lỗi: " + uafe.getMessage());
            uafe.printStackTrace(); // Keep this for debugging
            playButton.setEnabled(currentAudioFile != null && currentAudioFile.exists());
            stopButton.setEnabled(false);
        } catch (IOException | LineUnavailableException e) {
            // ... other exception handling
            statusLabel.setText("Lỗi phát audio: " + e.getMessage().substring(0, Math.min(e.getMessage().length(), 30)));
            UIUtils.showErrorMessage(this, "Không thể phát file audio: " + e.getMessage());
            e.printStackTrace();
            playButton.setEnabled(true);
            stopButton.setEnabled(false);
        }
    }

    public void stopAudio() {
        if (audioClip != null) {
            audioClip.stop();
            audioClip.close(); // Giải phóng tài nguyên
            playButton.setEnabled(currentAudioFile != null && currentAudioFile.exists());
            stopButton.setEnabled(false);
            if (currentAudioFile != null) {
                statusLabel.setText(currentAudioFile.getName());
            } else {
                 statusLabel.setText("Chưa chọn file audio.");
            }
        }
    }
    
    
}