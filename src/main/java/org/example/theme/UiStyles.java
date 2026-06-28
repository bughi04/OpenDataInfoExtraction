package org.example.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

// Shared colours, fonts, and Swing chrome for a consistent, readable UI.
public final class UiStyles {
    public static final Color ACCENT       = new Color(25, 118, 210);
    public static final Color ACCENT_HOVER = new Color(21, 101, 192);
    public static final Color ACCENT_SOFT  = new Color(227, 242, 253);
    public static final Color BG           = new Color(237, 240, 245);
    public static final Color PANEL        = Color.WHITE;
    public static final Color TEXT         = new Color(33, 37, 41);
    public static final Color BORDER       = new Color(206, 212, 218);
    public static final Color MUTED        = new Color(108, 117, 125);
    public static final Font FONT_UI       = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_UI_BOLD  = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_SMALL    = new Font("Segoe UI", Font.PLAIN, 11);
    private UiStyles() {}
    public static void applyGlobalTheme() {
        UIManager.put("TabbedPane.font", FONT_UI);
        UIManager.put("TabbedPane.selected", ACCENT_SOFT);
        UIManager.put("TabbedPane.contentBorderInsets", new Insets(4, 4, 4, 4));
        UIManager.put("Table.font", FONT_UI);
        UIManager.put("Table.rowHeight", 26);
        UIManager.put("Table.gridColor", BORDER);
        UIManager.put("Label.font", FONT_UI);
        UIManager.put("Button.font", FONT_UI);
        UIManager.put("TextField.font", FONT_UI);
        UIManager.put("TextArea.font", ReadableTextKit.FONT_BODY);
        UIManager.put("TextPane.font", ReadableTextKit.FONT_BODY);
        UIManager.put("Menu.font", FONT_UI);
        UIManager.put("MenuItem.font", FONT_UI);
        UIManager.put("MenuBar.font", FONT_UI);
        UIManager.put("ToolTip.font", FONT_SMALL);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ProgressBar.foreground", ACCENT);
        UIManager.put("TitledBorder.font", FONT_UI_BOLD);
        UIManager.put("TitledBorder.titleColor", ACCENT);
    }
    public static Border titledBorder(String title) {
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(BORDER, 1, true), title);
        tb.setTitleFont(FONT_UI_BOLD);
        tb.setTitleColor(ACCENT);
        return new CompoundBorder(tb, new EmptyBorder(4, 4, 4, 4));
    }
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                new LineBorder(BORDER, 1, true),
                new EmptyBorder(14, 16, 14, 16));
    }
    public static JButton secondaryButton(String label) {
        JButton b = solidButton(label);
        b.setFont(FONT_UI);
        b.setBackground(PANEL);
        b.setForeground(TEXT);
        b.setBorder(buttonBorder(BORDER));
        installHover(b, PANEL, new Color(248, 249, 252));
        return b;
    }
    public static JButton accentButton(String label) {
        JButton b = solidButton(label);
        b.setFont(FONT_UI_BOLD);
        b.setBackground(ACCENT);
        b.setForeground(Color.WHITE);
        b.setBorder(buttonBorder(ACCENT.darker()));
        installHover(b, ACCENT, ACCENT_HOVER);
        return b;
    }
    public static JButton toolButton(String label) {
        return secondaryButton(label);
    }
    public static JLabel sectionHeading(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lbl.setForeground(ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 12, 0));
        return lbl;
    }
    public static JLabel mutedLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SMALL);
        lbl.setForeground(MUTED);
        return lbl;
    }
    private static JButton solidButton(String label) {
        JButton b = new JButton(label) {
            @Override
            public void updateUI() {
                setUI(new BasicButtonUI());
            }
        };
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(true);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static Border buttonBorder(Color lineColor) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(lineColor, 1, true),
                new EmptyBorder(7, 16, 7, 16));
    }
    private static void installHover(JButton b, Color normal, Color hover) {
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hover);
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                if (b.isEnabled()) b.setBackground(normal);
            }
        });
    }
}