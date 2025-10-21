package com.nganhangdethi.ui.components;
import com.nganhangdethi.model.Question;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class JPlaceholderTextField extends JTextField implements FocusListener {

    private String placeholder;
    private boolean showingPlaceholder;
    private Color placeholderColor = Color.GRAY;
    private Color defaultColor;

    public JPlaceholderTextField(String placeholder) {
        super(placeholder);
        this.placeholder = placeholder;
        this.showingPlaceholder = true;
        this.defaultColor = getForeground();
        setForeground(placeholderColor);
        addFocusListener(this);
    }

    public JPlaceholderTextField(String placeholder, int columns) {
        super(placeholder, columns);
        this.placeholder = placeholder;
        this.showingPlaceholder = true;
        this.defaultColor = getForeground();
        setForeground(placeholderColor);
        addFocusListener(this);
    }
    
    public void setPlaceholderColor(Color color) {
        this.placeholderColor = color;
        if (showingPlaceholder) {
            setForeground(placeholderColor);
        }
    }

    @Override
    public String getText() {
        return showingPlaceholder ? "" : super.getText();
    }

    public String getActualText() { // Lấy text thực sự, kể cả khi placeholder đang hiển thị
        return super.getText();
    }
    
    @Override
    public void setText(String t) {
        if (t == null || t.isEmpty()) {
            super.setText(placeholder);
            setForeground(placeholderColor);
            showingPlaceholder = true;
        } else {
            super.setText(t);
            setForeground(defaultColor);
            showingPlaceholder = false;
        }
    }


    @Override
    public void focusGained(FocusEvent e) {
        if (this.getText().isEmpty() || showingPlaceholder) {
            super.setText("");
            setForeground(defaultColor);
            showingPlaceholder = false;
        }
    }

    @Override
    public void focusLost(FocusEvent e) {
        if (this.getText().isEmpty()) {
            super.setText(placeholder);
            setForeground(placeholderColor);
            showingPlaceholder = true;
        }
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        if (showingPlaceholder || super.getText().isEmpty()) {
            super.setText(placeholder);
            setForeground(placeholderColor);
            showingPlaceholder = true;
        }
    }

	public void reset() {
		// TODO Auto-generated method stub
		
	}
}