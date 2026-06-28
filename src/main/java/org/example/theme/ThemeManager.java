package org.example.theme;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;

// Simple fixed light theme
public class ThemeManager {
    public static final Color COLOR_BACKGROUND  = new Color(240, 243, 248);
    public static final Color COLOR_PANEL       = Color.WHITE;
    public static final Color COLOR_TEXT        = new Color(33, 33, 33);
    public static final Color COLOR_ACCENT      = new Color(25, 118, 210);   // blue
    public static final Color COLOR_CHART_PRI   = new Color(0, 150, 136);    // teal
    public static final Color COLOR_BORDER      = new Color(189, 189, 189);
    public static final Color COLOR_HIGHLIGHT   = new Color(227, 242, 253);
    private static final Color[] CHART_COLORS = {
            new Color(25,  118, 210),   // blue
            new Color(0,   150, 136),   // teal
            new Color(230, 81,  0),     // orange
            new Color(123, 31,  162),   // purple
            new Color(46,  125, 50),    // green
            new Color(183, 28,  28),    // red
            new Color(1,   87,  155),   // deep blue
            new Color(255, 193, 7),     // amber
    };
    public static Color[] getChartColors() { return CHART_COLORS; }
    public static AppTheme getCurrentTheme() { return SINGLE_THEME; }
    private static final AppTheme SINGLE_THEME = new AppTheme(
            COLOR_BACKGROUND, COLOR_PANEL, COLOR_TEXT, COLOR_ACCENT,
            COLOR_CHART_PRI, COLOR_BORDER, COLOR_HIGHLIGHT,
            "Light", javax.swing.UIManager.getSystemLookAndFeelClassName()
    );
    public static Border createThemedBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDER, 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5));
    }
    public static Border createThemedTitleBorder(String title) {
        return BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(COLOR_BORDER), title);
    }
}