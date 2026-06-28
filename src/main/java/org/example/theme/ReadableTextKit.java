package org.example.theme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.*;
import java.awt.*;

// Typography and styled text for reports, AI chat, and long-form reading areas.
public final class ReadableTextKit {
    public static final Font FONT_BODY       = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_BODY_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TITLE      = new Font("Segoe UI", Font.BOLD, 20);
    public static final Font FONT_SECTION    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_LABEL      = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_MONO       = new Font("Consolas", Font.PLAIN, 12);
    public static final Font FONT_MONO_SMALL = new Font("Consolas", Font.PLAIN, 11);
    public static final Color COLOR_BODY     = UiStyles.TEXT;
    public static final Color COLOR_MUTED    = UiStyles.MUTED;
    public static final Color COLOR_ACCENT   = UiStyles.ACCENT;
    public static final Color COLOR_USER      = new Color(21, 101, 192);
    public static final Color COLOR_ASSISTANT = new Color(46, 125, 50);
    public static final Color COLOR_READING_BG = new Color(252, 252, 254);
    public static final Color COLOR_TECH_BG    = new Color(248, 249, 252);
    private static final float LINE_SPACING = 0.22f;
    private ReadableTextKit() {}
    public static JScrollPane readingScroll(JTextPane pane, String title) {
        pane.setBorder(new EmptyBorder(16, 20, 16, 20));
        JScrollPane scroll = new JScrollPane(pane);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        scroll.setBorder(UiStyles.titledBorder(title));
        return scroll;
    }
    public static JTextPane createReadingPane() {
        JTextPane pane = new JTextPane();
        pane.setEditable(false);
        pane.setOpaque(true);
        pane.setBackground(COLOR_READING_BG);
        pane.setForeground(COLOR_BODY);
        pane.setFont(FONT_BODY);
        pane.setCaretColor(COLOR_ACCENT);
        pane.setMargin(new Insets(0, 0, 0, 0));
        return pane;
    }
    public static JTextPane createTechnicalPane() {
        JTextPane pane = createReadingPane();
        pane.setBackground(COLOR_TECH_BG);
        pane.setFont(FONT_MONO_SMALL);
        return pane;
    }
    public static void setPlainText(JTextPane pane, String text, String styleName) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            if (text == null || text.isEmpty()) return;
            Style style = ensureStyle(doc, styleName, FONT_BODY, COLOR_BODY, false);
            doc.insertString(0, text, style);
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(0);
    }
    public static void setReportText(JTextPane pane, String raw) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            if (raw == null || raw.isBlank()) return;
            Style titleStyle = ensureStyle(doc, "title", FONT_TITLE, COLOR_ACCENT, false);
            Style sectionStyle = ensureStyle(doc, "section", FONT_SECTION, COLOR_ACCENT, false);
            Style bodyStyle = ensureStyle(doc, "body", FONT_BODY, COLOR_BODY, false);
            Style mutedStyle = ensureStyle(doc, "muted", FONT_BODY_SMALL, COLOR_MUTED, false);
            Style monoStyle = ensureStyle(doc, "mono", FONT_MONO_SMALL, COLOR_BODY, true);
            boolean wroteTitle = false;
            for (String line : raw.split("\n", -1)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    doc.insertString(doc.getLength(), "\n", bodyStyle);
                    continue;
                }
                if (isRuleLine(trimmed)) {
                    continue;
                }
                if (!wroteTitle && looksLikeReportTitle(trimmed)) {
                    doc.insertString(doc.getLength(), trimmed + "\n\n", titleStyle);
                    wroteTitle = true;
                    continue;
                }
                if (looksLikeSectionHeader(trimmed)) {
                    doc.insertString(doc.getLength(), "\n" + trimmed + "\n", sectionStyle);
                    continue;
                }
                if (trimmed.startsWith("Reference:") || trimmed.startsWith("Generated :")
                        || trimmed.startsWith("Networks  :")) {
                    doc.insertString(doc.getLength(), line + "\n", mutedStyle);
                    continue;
                }
                if (line.startsWith("    ") || line.startsWith("\t")) {
                    doc.insertString(doc.getLength(), line + "\n", monoStyle);
                    continue;
                }
                doc.insertString(doc.getLength(), line + "\n", bodyStyle);
            }
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(0);
    }
    public static void setTechnicalText(JTextPane pane, String raw) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            if (raw == null || raw.isBlank()) return;
            Style headerStyle = ensureStyle(doc, "techHeader", FONT_SECTION, COLOR_ACCENT, false);
            Style monoStyle = ensureStyle(doc, "techMono", FONT_MONO_SMALL, COLOR_BODY, true);
            Style mutedStyle = ensureStyle(doc, "techMuted", FONT_BODY_SMALL, COLOR_MUTED, false);
            for (String line : raw.split("\n", -1)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    doc.insertString(doc.getLength(), "\n", monoStyle);
                    continue;
                }
                if (isRuleLine(trimmed)) continue;
                if (looksLikeSectionHeader(trimmed) || trimmed.startsWith("TECHNICAL DETAILS")) {
                    doc.insertString(doc.getLength(), "\n" + trimmed + "\n", headerStyle);
                } else if (trimmed.startsWith("Auto-updated") || trimmed.startsWith("Process GraphML")) {
                    doc.insertString(doc.getLength(), line + "\n", mutedStyle);
                } else {
                    doc.insertString(doc.getLength(), line + "\n", monoStyle);
                }
            }
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(0);
    }
    public static void setWelcomeReport(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            Style title = ensureStyle(doc, "welcomeTitle", FONT_TITLE, COLOR_ACCENT, false);
            Style body = ensureStyle(doc, "welcomeBody", FONT_BODY, COLOR_BODY, false);
            Style bullet = ensureStyle(doc, "welcomeBullet", FONT_BODY, COLOR_BODY, false);
            doc.insertString(doc.getLength(),
                    "Analysis Report\n\n", title);
            doc.insertString(doc.getLength(),
                    "Press Generate Report for a written summary of your street networks, "
                            + "including comparisons and research context.\n\n", body);
            doc.insertString(doc.getLength(), "The report includes:\n", body);
            String[] items = {
                    "Summary statistics and street infrastructure",
                    "Rankings of cities by key metrics",
                    "Automated observations vs global benchmarks",
                    "Research notes (Sierra-Porta & Herrera-Acevedo, 2024)",
                    "Full metrics table"
            };
            for (String item : items) {
                doc.insertString(doc.getLength(), "  -  " + item + "\n", bullet);
            }
            doc.insertString(doc.getLength(),
                    "\nPress Generate Report (or Tools > Generate Analysis Report) after processing files.\n",
                    body);
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(0);
    }
    public static void setWelcomeChat(JTextPane pane) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            doc.remove(0, doc.getLength());
            Style title = ensureStyle(doc, "chatWelcomeTitle", FONT_TITLE, COLOR_ACCENT, false);
            Style body = ensureStyle(doc, "chatWelcomeBody", FONT_BODY, COLOR_BODY, false);
            Style hint = ensureStyle(doc, "chatWelcomeHint", FONT_BODY_SMALL, COLOR_MUTED, false);
            doc.insertString(doc.getLength(), "AI Street Network Advisor\n\n", title);
            doc.insertString(doc.getLength(),
                    "Ask questions about the cities you have loaded, in plain language.\n\n", body);
            doc.insertString(doc.getLength(), "Try asking:\n", body);
            String[] examples = {
                    "Which city has the most walkable streets?",
                    "Compare Amsterdam and Barcelona",
                    "Where are speed limits highest?",
                    "Summarise the main differences across my cities"
            };
            for (String ex : examples) {
                doc.insertString(doc.getLength(), "  -  \"" + ex + "\"\n", body);
            }
            doc.insertString(doc.getLength(),
                    "\nUse the quick-action buttons above, or type your own question below.\n"
                            + "Powered by Google Gemini: configure your API key in Settings.\n", hint);
        } catch (BadLocationException ignored) { }
        pane.setCaretPosition(0);
    }
    public static void appendUserMessage(JTextPane pane, String text) {
        appendMessage(pane, "You", text, COLOR_USER, false);
    }
    public static void appendAssistantMessage(JTextPane pane, String text) {
        appendMessage(pane, "Assistant", text, COLOR_ASSISTANT, true);
    }
    private static void appendMessage(JTextPane pane, String role, String text,
                                      Color roleColor, boolean renderMarkdown) {
        StyledDocument doc = pane.getStyledDocument();
        try {
            Style roleStyle = ensureStyle(doc, "role_" + role, FONT_LABEL, roleColor, false);
            Style bodyStyle = ensureStyle(doc, "msgBody", FONT_BODY, COLOR_BODY, false);

            doc.insertString(doc.getLength(), "\n", bodyStyle);
            doc.insertString(doc.getLength(), role + "\n", roleStyle);
            if (renderMarkdown) {
                appendMarkdownBlock(doc, normalizeMarkdown(text), bodyStyle);
            } else {
                doc.insertString(doc.getLength(), (text != null ? text : "") + "\n", bodyStyle);
            }
            pane.setCaretPosition(doc.getLength());
        } catch (BadLocationException ignored) { }
    }
    private static void appendMarkdownBlock(StyledDocument doc, String text, Style baseStyle)
            throws BadLocationException {
        if (text == null || text.isBlank()) {
            doc.insertString(doc.getLength(), "\n", baseStyle);
            return;
        }
        Style boldStyle = derivedStyle(doc, "mdBold", baseStyle, true, false, false, null);
        Style italicStyle = derivedStyle(doc, "mdItalic", baseStyle, false, true, false, null);
        Style boldItalicStyle = derivedStyle(doc, "mdBoldItalic", baseStyle, true, true, false, null);
        Style codeStyle = derivedStyle(doc, "mdCode", baseStyle, false, false, false, FONT_MONO_SMALL);
        Style codeBlockStyle = derivedStyle(doc, "mdCodeBlock", baseStyle, false, false, true, FONT_MONO_SMALL);
        Style h1Style = ensureStyle(doc, "mdH1", FONT_SECTION, COLOR_ACCENT, false);
        Style h2Style = ensureStyle(doc, "mdH2", FONT_LABEL, COLOR_ACCENT, false);
        StyleConstants.setBold(h2Style, true);
        Style h3Style = derivedStyle(doc, "mdH3", baseStyle, true, false, false, null);
        boolean inCodeFence = false;
        String[] lines = text.split("\n", -1);
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            String trimmed = line.trim();

            if (trimmed.startsWith("```")) {
                inCodeFence = !inCodeFence;
                if (!inCodeFence) {
                    doc.insertString(doc.getLength(), "\n", baseStyle);
                }
                continue;
            }
            if (inCodeFence) {
                doc.insertString(doc.getLength(), line + "\n", codeBlockStyle);
                continue;
            }

            if (trimmed.isEmpty()) {
                doc.insertString(doc.getLength(), "\n", baseStyle);
                continue;
            }

            if (trimmed.startsWith("### ")) {
                doc.insertString(doc.getLength(), trimmed.substring(4) + "\n", h3Style);
                continue;
            }
            if (trimmed.startsWith("## ")) {
                doc.insertString(doc.getLength(), trimmed.substring(3) + "\n", h2Style);
                continue;
            }
            if (trimmed.startsWith("# ")) {
                doc.insertString(doc.getLength(), trimmed.substring(2) + "\n", h1Style);
                continue;
            }
            java.util.regex.Matcher numList = java.util.regex.Pattern
                    .compile("^(\\d+)\\.\\s+(.*)").matcher(trimmed);
            if (numList.matches()) {
                doc.insertString(doc.getLength(), "  " + numList.group(1) + ". ", baseStyle);
                appendInlineMarkdown(doc, numList.group(2), baseStyle, boldStyle, italicStyle,
                        boldItalicStyle, codeStyle);
                doc.insertString(doc.getLength(), "\n", baseStyle);
                continue;
            }
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                doc.insertString(doc.getLength(), "  - ", baseStyle);
                appendInlineMarkdown(doc, trimmed.substring(2), baseStyle, boldStyle, italicStyle,
                        boldItalicStyle, codeStyle);
                doc.insertString(doc.getLength(), "\n", baseStyle);
                continue;
            }
            appendInlineMarkdown(doc, line, baseStyle, boldStyle, italicStyle, boldItalicStyle, codeStyle);
            doc.insertString(doc.getLength(), "\n", baseStyle);
        }
    }
    private static void appendInlineMarkdown(StyledDocument doc, String text, Style base,
                                             Style bold, Style italic, Style boldItalic, Style code)
            throws BadLocationException {
        int i = 0;
        while (i < text.length()) {
            if (text.startsWith("***", i)) {
                int end = text.indexOf("***", i + 3);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 3, end), boldItalic);
                    i = end + 3;
                    continue;
                }
            }
            if (text.startsWith("**", i)) {
                int end = text.indexOf("**", i + 2);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 2, end), bold);
                    i = end + 2;
                    continue;
                }
            }
            if (text.startsWith("__", i)) {
                int end = text.indexOf("__", i + 2);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 2, end), bold);
                    i = end + 2;
                    continue;
                }
            }
            if (text.charAt(i) == '`') {
                int end = text.indexOf('`', i + 1);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 1, end), code);
                    i = end + 1;
                    continue;
                }
            }
            if (text.charAt(i) == '*' && i + 1 < text.length() && text.charAt(i + 1) != '*') {
                int end = text.indexOf('*', i + 1);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 1, end), bold);
                    i = end + 1;
                    continue;
                }
            }
            if (text.charAt(i) == '_' && i + 1 < text.length() && text.charAt(i + 1) != '_') {
                int end = text.indexOf('_', i + 1);
                if (end > i) {
                    doc.insertString(doc.getLength(), text.substring(i + 1, end), italic);
                    i = end + 1;
                    continue;
                }
            }
            if (text.startsWith("~~", i)) {
                int end = text.indexOf("~~", i + 2);
                if (end > i) {
                    Style strike = derivedStyle(doc, "mdStrike", base, false, false, false, null);
                    StyleConstants.setStrikeThrough(strike, true);
                    doc.insertString(doc.getLength(), text.substring(i + 2, end), strike);
                    i = end + 2;
                    continue;
                }
            }
            int next = nextMarkdownSpecial(text, i);
            doc.insertString(doc.getLength(), text.substring(i, next), base);
            i = next;
        }
    }
    private static int nextMarkdownSpecial(String text, int from) {
        int next = text.length();
        for (int j = from + 1; j < text.length(); j++) {
            char c = text.charAt(j);
            if (c == '*' || c == '_' || c == '`' || c == '~') {
                return j;
            }
        }
        return next;
    }
    private static String normalizeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("\r\n", "\n").strip();
    }
    private static Style derivedStyle(StyledDocument doc, String name, Style base,
                                      boolean bold, boolean italic, boolean shaded, Font font) {
        Style style = doc.getStyle(name);
        if (style == null) {
            style = doc.addStyle(name, base);
        }
        if (font != null) {
            StyleConstants.setFontFamily(style, font.getFamily());
            StyleConstants.setFontSize(style, font.getSize());
        }
        StyleConstants.setBold(style, bold);
        StyleConstants.setItalic(style, italic);
        if (shaded) {
            StyleConstants.setBackground(style, new Color(243, 244, 246));
        }
        return style;
    }
    public static String plainText(JTextPane pane) {
        return pane.getText();
    }
    private static boolean isRuleLine(String trimmed) {
        if (trimmed.length() < 3) return false;
        char c = trimmed.charAt(0);
        return (c == '=' || c == '-') && trimmed.chars().allMatch(ch -> ch == c);
    }
    private static boolean looksLikeReportTitle(String line) {
        return line.contains("REPORT") || line.contains("ANALYSIS REPORT");
    }
    private static boolean looksLikeSectionHeader(String line) {
        return line.matches("^[0-9]+[a-z]?\\.\\s+[A-Z].*")
                || line.matches("^[0-9]+\\.\\s+[A-Z].*");
    }
    private static Style ensureStyle(StyledDocument doc, String name, Font font, Color fg, boolean mono) {
        Style style = doc.getStyle(name);
        if (style == null) {
            style = doc.addStyle(name, null);
        }
        StyleConstants.setFontFamily(style, font.getFamily());
        StyleConstants.setFontSize(style, font.getSize());
        StyleConstants.setBold(style, font.getStyle() == Font.BOLD);
        StyleConstants.setForeground(style, fg);
        StyleConstants.setLineSpacing(style, mono ? 0.1f : LINE_SPACING);
        return style;
    }
}