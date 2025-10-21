package com.nganhangdethi.ui.components;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public class WrappedLabelCellRenderer extends JTextArea implements TableCellRenderer {

    public WrappedLabelCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        // setMargin(new Insets(5, 5, 5, 5)); // Thêm padding nếu muốn
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                 boolean isSelected, boolean hasFocus,
                                                 int row, int column) {
        setText((value == null) ? "" : value.toString());
        // Điều chỉnh kích thước của JTextArea để phù hợp với nội dung và chiều cao hàng
        // Việc này có thể phức tạp và phụ thuộc vào cách bạn muốn nó hiển thị.
        // Một cách đơn giản là set chiều cao cố định cho hàng và để JTextArea cuộn nếu cần.
        // Hoặc, bạn có thể tính toán chiều cao cần thiết.
        // setSize(table.getColumnModel().getColumn(column).getWidth(), getPreferredSize().height);
        // if (table.getRowHeight(row) != getPreferredSize().height) {
        //     table.setRowHeight(row, getPreferredSize().height);
        // }

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }
        setFont(table.getFont()); // Sử dụng font của bảng
        return this;
    }
}