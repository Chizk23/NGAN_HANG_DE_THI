package com.nganhangdethi.util;

import java.awt.Component;

import javax.swing.*;

public final class ValidationUtils {

    private ValidationUtils() {}

    /**
     * Kiểm tra xem một JTextField có rỗng không.
     * @param field JTextField cần kiểm tra.
     * @param fieldName Tên của trường (dùng cho thông báo lỗi).
     * @param parentComponent Component cha để hiển thị JOptionPane (có thể là null).
     * @return true nếu hợp lệ (không rỗng), false nếu rỗng.
     */
    public static boolean isNotEmpty(JTextField field, String fieldName, Component parentComponent) {
        if (field.getText().trim().isEmpty()) {
            if (parentComponent != null) {
                UIUtils.showWarningMessage(parentComponent, fieldName + " không được để trống.");
            } else {
                System.err.println("Validation Error: " + fieldName + " is empty.");
            }
            field.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra xem một JTextArea có rỗng không.
     */
    public static boolean isNotEmpty(JTextArea area, String fieldName, Component parentComponent) {
        if (area.getText().trim().isEmpty()) {
            if (parentComponent != null) {
                UIUtils.showWarningMessage(parentComponent, fieldName + " không được để trống.");
            } else {
                System.err.println("Validation Error: " + fieldName + " is empty.");
            }
            area.requestFocus();
            return false;
        }
        return true;
    }

    /**
     * Kiểm tra xem một JComboBox có item nào được chọn không.
     */
    public static boolean isSelected(JComboBox<?> comboBox, String fieldName, Component parentComponent) {
        if (comboBox.getSelectedIndex() == -1 || // Không có item nào
            (comboBox.getSelectedItem() != null && Constants.ALL_FILTER_OPTION.equals(comboBox.getSelectedItem().toString()) && !fieldName.toLowerCase().contains("lọc"))) { // Nếu là bộ lọc thì "Tất cả" là hợp lệ
            // Điều kiện trên hơi phức tạp, có thể cần điều chỉnh tùy theo ngữ cảnh sử dụng ALL_FILTER_OPTION
             if (comboBox.getSelectedItem() == null || (comboBox.getSelectedItem() != null && Constants.ALL_FILTER_OPTION.equals(comboBox.getSelectedItem().toString()) && !isFilterComboBox(fieldName)) ) {
                if (parentComponent != null) {
                    UIUtils.showWarningMessage(parentComponent, "Vui lòng chọn một giá trị cho " + fieldName + ".");
                } else {
                    System.err.println("Validation Error: No selection for " + fieldName);
                }
                comboBox.requestFocus();
                return false;
            }
        }
        return true;
    }
    
    private static boolean isFilterComboBox(String fieldName){
        // Hàm helper để xác định đây có phải combobox dùng cho filter không
        // (nơi mà "Tất cả" là một lựa chọn hợp lệ)
        return fieldName != null && (fieldName.toLowerCase().contains("cấp độ") || fieldName.toLowerCase().contains("loại"));
    }


    /**
     * Kiểm tra xem một chuỗi có phải là số nguyên hợp lệ không.
     */
    public static boolean isValidInteger(String text, String fieldName, Component parentComponent) {
        try {
            Integer.parseInt(text.trim());
            return true;
        } catch (NumberFormatException e) {
            if (parentComponent != null) {
                UIUtils.showWarningMessage(parentComponent, fieldName + " phải là một số nguyên hợp lệ.");
            } else {
                System.err.println("Validation Error: " + fieldName + " is not a valid integer: " + text);
            }
            return false;
        }
    }
    
    // Thêm các phương thức validation khác nếu cần (email, URL, etc.)
}