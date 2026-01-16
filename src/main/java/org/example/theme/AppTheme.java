package org.example.theme;

import java.awt.Color;

public class AppTheme {
    private final Color backgroundColor;
    private final Color panelColor;
    private final Color textColor;
    private final Color accentColor;
    private final Color chartPrimaryColor;
    private final Color borderColor;
    private final Color highlightColor;
    private final String name;
    private final String lookAndFeelClassName;

    public AppTheme(Color backgroundColor, Color panelColor, Color textColor,
                    Color accentColor, Color chartPrimaryColor, Color borderColor,
                    Color highlightColor, String name, String lookAndFeelClassName) {
        this.backgroundColor = backgroundColor;
        this.panelColor = panelColor;
        this.textColor = textColor;
        this.accentColor = accentColor;
        this.chartPrimaryColor = chartPrimaryColor;
        this.borderColor = borderColor;
        this.highlightColor = highlightColor;
        this.name = name;
        this.lookAndFeelClassName = lookAndFeelClassName;
    }


    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getPanelColor() {
        return panelColor;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getAccentColor() {
        return accentColor;
    }

    public Color getChartPrimaryColor() {
        return chartPrimaryColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public Color getHighlightColor() {
        return highlightColor;
    }

    public String getName() {
        return name;
    }

    public String getLookAndFeelClassName() {
        return lookAndFeelClassName;
    }

    public Color getAccentGradientLight() {
        return lighten(accentColor, 0.2f);
    }

    public Color getAccentGradientDark() {
        return darken(accentColor, 0.2f);
    }

    public Color[] getChartColors() {
        return new Color[] {
                chartPrimaryColor,
                accentColor,
                lighten(chartPrimaryColor, 0.2f),
                darken(chartPrimaryColor, 0.2f),
                lighten(accentColor, 0.2f),
                darken(accentColor, 0.2f),
                new Color(chartPrimaryColor.getRed(), accentColor.getGreen(), accentColor.getBlue()),
                new Color(accentColor.getRed(), chartPrimaryColor.getGreen(), accentColor.getBlue())
        };
    }

    public static Color lighten(Color color, float factor) {
        int r = Math.min(255, (int)(color.getRed() + 255 * factor));
        int g = Math.min(255, (int)(color.getGreen() + 255 * factor));
        int b = Math.min(255, (int)(color.getBlue() + 255 * factor));
        return new Color(r, g, b);
    }

    public static Color darken(Color color, float factor) {
        int r = Math.max(0, (int)(color.getRed() - 255 * factor));
        int g = Math.max(0, (int)(color.getGreen() - 255 * factor));
        int b = Math.max(0, (int)(color.getBlue() - 255 * factor));
        return new Color(r, g, b);
    }
}