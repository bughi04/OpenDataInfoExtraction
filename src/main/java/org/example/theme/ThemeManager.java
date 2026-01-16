package org.example.theme;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.plaf.ColorUIResource;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class ThemeManager {

    public static final String LIGHT_THEME = "Light";
    public static final String DARK_THEME = "Dark";
    public static final String CYBERPUNK_THEME = "Cyberpunk";
    public static final String OCEAN_THEME = "Ocean";
    public static final String FOREST_THEME = "Forest";

    private static String currentTheme = LIGHT_THEME;

    private static final Map<String, AppTheme> themes = new HashMap<>();

    static {
        themes.put(LIGHT_THEME, new AppTheme(
                new Color(240, 240, 240),
                new Color(255, 255, 255),
                new Color(51, 51, 51),
                new Color(25, 118, 210),
                new Color(0, 150, 136),
                new Color(200, 200, 200),
                new Color(245, 245, 245),
                "Light Theme",
                UIManager.getSystemLookAndFeelClassName()
        ));

        themes.put(DARK_THEME, new AppTheme(
                new Color(35, 35, 35),
                new Color(50, 50, 50),
                new Color(220, 220, 220),
                new Color(0, 150, 136),
                new Color(79, 195, 247),
                new Color(70, 70, 70),
                new Color(60, 60, 60),
                "Dark Theme",
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"
        ));

        themes.put(CYBERPUNK_THEME, new AppTheme(
                new Color(20, 20, 35),
                new Color(30, 30, 45),
                new Color(250, 250, 210),
                new Color(255, 64, 129),
                new Color(0, 255, 255),
                new Color(90, 0, 120),
                new Color(40, 40, 65),
                "Cyberpunk Theme",
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"
        ));

        themes.put(OCEAN_THEME, new AppTheme(
                new Color(21, 101, 192),
                new Color(41, 121, 212),
                new Color(255, 255, 255),
                new Color(0, 229, 255),
                new Color(100, 181, 246),
                new Color(13, 71, 161),
                new Color(61, 141, 232),
                "Ocean Theme",
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"
        ));

        themes.put(FOREST_THEME, new AppTheme(
                new Color(27, 94, 32),
                new Color(46, 125, 50),
                new Color(245, 245, 245),
                new Color(255, 235, 59),
                new Color(129, 199, 132),
                new Color(0, 77, 64),
                new Color(67, 160, 71),
                "Forest Theme",
                "javax.swing.plaf.nimbus.NimbusLookAndFeel"
        ));
    }

    public static String[] getAvailableThemes() {
        return themes.keySet().toArray(new String[0]);
    }

    public static String getCurrentThemeName() {
        return currentTheme;
    }

    public static void applyTheme(String themeName) {
        if (!themes.containsKey(themeName)) {
            System.err.println("Theme not found: " + themeName);
            return;
        }

        currentTheme = themeName;
        AppTheme theme = themes.get(themeName);

        try {
            UIManager.setLookAndFeel(theme.getLookAndFeelClassName());

            UIManager.put("Panel.background", theme.getPanelColor());
            UIManager.put("OptionPane.background", theme.getPanelColor());
            UIManager.put("Button.background", theme.getPanelColor());
            UIManager.put("ComboBox.background", theme.getPanelColor());
            UIManager.put("TextField.background", theme.getPanelColor());

            UIManager.put("Label.foreground", theme.getTextColor());
            UIManager.put("Button.foreground", theme.getTextColor());
            UIManager.put("TabbedPane.foreground", theme.getTextColor());
            UIManager.put("TextField.foreground", theme.getTextColor());
            UIManager.put("TextArea.foreground", theme.getTextColor());
            UIManager.put("ComboBox.foreground", theme.getTextColor());
            UIManager.put("Menu.foreground", theme.getTextColor());
            UIManager.put("MenuItem.foreground", theme.getTextColor());

            UIManager.put("Button.select", theme.getAccentColor());
            UIManager.put("TabbedPane.selected", theme.getHighlightColor());
            UIManager.put("TabbedPane.selectedForeground", theme.getTextColor());
            UIManager.put("TabbedPane.focus", theme.getAccentColor());

            UIManager.put("Border.color", theme.getBorderColor());

            UIManager.put("ToolTip.background", theme.getHighlightColor());
            UIManager.put("ToolTip.foreground", theme.getTextColor());

            UIManager.put("ScrollBar.thumb", theme.getAccentColor());
            UIManager.put("ScrollBar.thumbDarkShadow", theme.getBorderColor());
            UIManager.put("ScrollBar.thumbHighlight", theme.getHighlightColor());
            UIManager.put("ScrollBar.thumbShadow", theme.getBorderColor());
            UIManager.put("ScrollBar.track", theme.getBackgroundColor());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static AppTheme getCurrentTheme() {
        return themes.get(currentTheme);
    }

    public static Border createThemedBorder() {
        AppTheme theme = getCurrentTheme();
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(theme.getBorderColor(), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
    }

    public static Border createThemedTitleBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(getCurrentTheme().getBorderColor()),
                title
        );
    }

    public static JPanel createThemedPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(getCurrentTheme().getPanelColor());
        panel.setBorder(createThemedBorder());
        return panel;
    }

    public static JPanel createThemedTitledPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBackground(getCurrentTheme().getPanelColor());
        panel.setBorder(createThemedTitleBorder(title));
        return panel;
    }

    public static JButton createThemedButton(String text) {
        JButton button = new JButton(text);
        AppTheme theme = getCurrentTheme();
        button.setBackground(theme.getPanelColor());
        button.setForeground(theme.getTextColor());
        button.setFocusPainted(false);
        return button;
    }
}