package org.example.view;

import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;

import javax.swing.*;
import java.awt.*;

public class ModernProcurementItemCellRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                  boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

        if (value instanceof ProcurementItem) {
            ProcurementItem item = (ProcurementItem) value;

            StringBuilder html = new StringBuilder("<html>");

            String accentColorHex = String.format("#%02x%02x%02x",
                    ThemeManager.getCurrentTheme().getAccentColor().getRed(),
                    ThemeManager.getCurrentTheme().getAccentColor().getGreen(),
                    ThemeManager.getCurrentTheme().getAccentColor().getBlue());

            html.append("<b style='color:").append(accentColorHex).append("'>#")
                    .append(item.getRowNumber()).append(":</b> ");

            html.append(item.getObjectName());

            if (item.getValueWithoutTVA() > 0) {
                html.append("<span style='float:right; margin-left:15px;'><b>")
                        .append(String.format("%,.2f", item.getValueWithoutTVA()))
                        .append(" RON</b></span>");
            }

            if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                html.append("<br><span style='color:gray; font-size:smaller;'>CPV: ")
                        .append(item.getCpvField()).append("</span>");
            }

            html.append("</html>");
            label.setText(html.toString());

            if (item.getValueWithoutTVA() >= 100000) {
                label.setIcon(createColorIcon(new Color(192, 80, 77)));
            } else if (item.getValueWithoutTVA() >= 50000) {
                label.setIcon(createColorIcon(new Color(247, 150, 70)));
            } else if (item.getValueWithoutTVA() >= 10000) {
                label.setIcon(createColorIcon(new Color(155, 187, 89)));
            } else if (item.getValueWithoutTVA() > 0) {
                label.setIcon(createColorIcon(new Color(75, 172, 198)));
            } else {
                label.setIcon(createColorIcon(new Color(128, 128, 128)));
            }

            StringBuilder tooltip = new StringBuilder("<html>");
            tooltip.append("<h3 style='margin:3px 0;'>").append(item.getObjectName()).append("</h3>");

            if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                tooltip.append("<b>CPV:</b> ").append(item.getCpvField()).append("<br>");
            }

            if (item.getValueWithoutTVA() > 0) {
                tooltip.append("<b>Value (without TVA):</b> ")
                        .append(String.format("%,.2f", item.getValueWithoutTVA()))
                        .append(" RON<br>");
            }

            if (item.getValueWithTVA() > 0) {
                tooltip.append("<b>Value (with TVA):</b> ")
                        .append(String.format("%,.2f", item.getValueWithTVA()))
                        .append(" RON<br>");
            }

            if (item.getSource() != null && !item.getSource().isEmpty()) {
                tooltip.append("<b>Source:</b> ").append(item.getSource()).append("<br>");
            }

            if (item.getInitiationDate() != null && !item.getInitiationDate().isEmpty()) {
                tooltip.append("<b>Initiation Date:</b> ").append(item.getInitiationDate()).append("<br>");
            }

            if (item.getCompletionDate() != null && !item.getCompletionDate().isEmpty()) {
                tooltip.append("<b>Completion Date:</b> ").append(item.getCompletionDate()).append("<br>");
            }

            tooltip.append("</html>");
            label.setToolTipText(tooltip.toString());
        }

        if (!isSelected) {
            label.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            label.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        } else {
            label.setBackground(ThemeManager.getCurrentTheme().getAccentColor());
            label.setForeground(Color.WHITE);
        }

        label.setBorder(BorderFactory.createCompoundBorder(
                label.getBorder(),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)
        ));

        return label;
    }

    private Icon createColorIcon(Color color) {
        return new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(color);
                g2d.fillOval(x, y, getIconWidth(), getIconHeight());
                g2d.dispose();
            }

            @Override
            public int getIconWidth() {
                return 10;
            }

            @Override
            public int getIconHeight() {
                return 10;
            }
        };
    }
}