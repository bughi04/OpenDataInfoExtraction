package org.example.view;

import org.example.service.GeminiInsightService;
import org.example.service.NetworkInsightFacade;
import org.example.theme.ReadableTextKit;
import org.example.theme.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.util.function.Consumer;

// AI Advisor tab: chat-style panel powered by Google Gemini.
public class AiAdvisorPanel extends JPanel {
    private final NetworkInsightFacade insightFacade = new NetworkInsightFacade();
    private final JTextPane chatPane = ReadableTextKit.createReadingPane();
    private final JTextField questionField = new JTextField();
    private final JLabel statusBadge = new JLabel();
    private Consumer<String> overviewAction;
    private Consumer<String> bestWorstAction;
    private Consumer<String> askAction;
    private Runnable settingsAction;
    private Runnable clearChatAction;
    public AiAdvisorPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiStyles.BG);
        setBorder(new EmptyBorder(14, 16, 14, 16));
        buildUi();
        refreshStatusBadge();
    }
    public NetworkInsightFacade getInsightFacade() { return insightFacade; }
    public String getChatText() { return ReadableTextKit.plainText(chatPane); }
    public void setChatText(String text) {
        SwingUtilities.invokeLater(() -> {
            if (text != null && !text.isBlank()) {
                ReadableTextKit.setPlainText(chatPane, text, "restored");
            } else {
                ReadableTextKit.setWelcomeChat(chatPane);
            }
        });
    }
    public void setOverviewAction(Consumer<String> action) { this.overviewAction = action; }
    public void setBestWorstAction(Consumer<String> action) { this.bestWorstAction = action; }
    public void setAskAction(Consumer<String> action) { this.askAction = action; }
    public void setSettingsAction(Runnable action) { this.settingsAction = action; }
    public void setClearChatAction(Runnable action) { this.clearChatAction = action; }
    public void appendAssistantMessage(String text) {
        SwingUtilities.invokeLater(() -> ReadableTextKit.appendAssistantMessage(chatPane, text));
    }
    public void appendUserMessage(String text) {
        SwingUtilities.invokeLater(() -> ReadableTextKit.appendUserMessage(chatPane, text));
    }
    public void setThinking(boolean thinking) {
        SwingUtilities.invokeLater(() -> {
            questionField.setEnabled(!thinking);
            statusBadge.setText(thinking ? "  Thinking…" : statusBadgeText());
            statusBadge.setForeground(thinking ? UiStyles.MUTED : statusColor());
        });
    }
    public void refreshStatusBadge() {
        SwingUtilities.invokeLater(() -> {
            statusBadge.setText(statusBadgeText());
            statusBadge.setForeground(statusColor());
        });
    }
    public void clearChat() {
        SwingUtilities.invokeLater(() -> ReadableTextKit.setWelcomeChat(chatPane));
    }
    private String statusBadgeText() {
        if (insightFacade.isGeminiAvailable()) {
            return "  Connected · " + insightFacade.getGeminiService().getModel();
        }
        return "  API key required";
    }
    private Color statusColor() {
        return insightFacade.isGeminiAvailable()
                ? new Color(46, 125, 50)
                : new Color(180, 90, 0);
    }
    private void buildUi() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("AI Street Network Advisor");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(UiStyles.ACCENT);
        header.add(title, BorderLayout.WEST);
        statusBadge.setFont(UiStyles.FONT_SMALL);
        header.add(statusBadge, BorderLayout.EAST);
        JPanel quickActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        quickActions.setOpaque(false);
        quickActions.add(quickBtn("City overview", () -> {
            if (overviewAction != null) overviewAction.accept("overview");
        }));
        quickActions.add(quickBtn("Best vs worst", () -> {
            if (bestWorstAction != null) bestWorstAction.accept("best vs worst");
        }));
        quickActions.add(quickBtn("Compare regions", () -> {
            if (askAction != null) askAction.accept("compare cities by country and region");
        }));
        quickActions.add(quickBtn("Walkability", () -> {
            if (askAction != null) askAction.accept(
                    "Which cities are most pedestrian and cycling friendly based on the OSM data?");
        }));
        quickActions.add(quickBtn("Settings", () -> {
            if (settingsAction != null) settingsAction.run();
        }));
        quickActions.add(quickBtn("Clear chat", this::confirmAndClearChat));
        ReadableTextKit.setWelcomeChat(chatPane);
        JScrollPane chatScroll = ReadableTextKit.readingScroll(chatPane, "Conversation");
        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setOpaque(false);
        inputPanel.setBorder(new EmptyBorder(8, 0, 0, 0));
        questionField.setFont(ReadableTextKit.FONT_BODY);
        questionField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiStyles.BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        questionField.setToolTipText("Ask in plain language, e.g. Which city has the most residential streets?");
        JButton askBtn = UiStyles.accentButton("Ask");
        askBtn.addActionListener(e -> submitQuestion());
        questionField.addActionListener(e -> submitQuestion());
        inputPanel.add(questionField, BorderLayout.CENTER);
        inputPanel.add(askBtn, BorderLayout.EAST);
        JPanel north = new JPanel(new BorderLayout(0, 8));
        north.setOpaque(false);
        north.add(header, BorderLayout.NORTH);
        north.add(quickActions, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(chatScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.SOUTH);
    }
    private void confirmAndClearChat() {
        int choice = JOptionPane.showConfirmDialog(
                SwingUtilities.getWindowAncestor(this),
                "Clear the AI conversation?",
                "Clear chat",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
        clearChat();
        if (clearChatAction != null) {
            clearChatAction.run();
        }
    }
    private void submitQuestion() {
        String q = questionField.getText().trim();
        if (q.isEmpty()) return;
        questionField.setText("");
        appendUserMessage(q);
        if (askAction != null) askAction.accept(q);
    }
    private JButton quickBtn(String label, Runnable action) {
        JButton b = UiStyles.secondaryButton(label);
        b.setFont(UiStyles.FONT_SMALL);
        b.addActionListener(e -> action.run());
        return b;
    }
    public static void showGeminiSettingsDialog(Component parent, NetworkInsightFacade facade) {
        GeminiInsightService gemini = facade.getGeminiService();
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel fields = new JPanel(new GridLayout(2, 2, 10, 10));
        JTextField keyField = new JTextField(gemini.isConfigured() ? "********" : "");
        keyField.setFont(UiStyles.FONT_UI);
        JComboBox<String> modelCombo = new JComboBox<>(
                GeminiInsightService.getRecommendedModels().toArray(new String[0]));
        modelCombo.setEditable(true);
        modelCombo.setFont(UiStyles.FONT_UI);
        String currentModel = gemini.getModel();
        modelCombo.setSelectedItem(currentModel);
        if (modelCombo.getSelectedItem() == null) {
            modelCombo.getEditor().setItem(currentModel);
        }
        JLabel keyLbl = new JLabel("API key");
        keyLbl.setFont(UiStyles.FONT_UI_BOLD);
        JLabel modelLbl = new JLabel("Model");
        modelLbl.setFont(UiStyles.FONT_UI_BOLD);
        fields.add(keyLbl);
        fields.add(keyField);
        fields.add(modelLbl);
        fields.add(modelCombo);
        JLabel hint = new JLabel("<html><span style='color:#6c757d;font-family:Segoe UI;font-size:11px'>"
                + "Recommended: <b>gemini-3.1-flash-lite</b> (fast, free-tier friendly) or "
                + "<b>gemini-3.5-flash</b> (smarter).<br>"
                + "Older gemini-2.0 models are retired; the app upgrades them automatically.<br>"
                + "Get a key at <a href='https://aistudio.google.com/apikey'>Google AI Studio</a>."
                + "</span></html>");
        panel.add(fields);
        panel.add(Box.createVerticalStrut(12));
        panel.add(hint);
        int result = JOptionPane.showConfirmDialog(parent, panel,
                "Gemini Settings", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String key = keyField.getText();
            if (key.equals("********")) key = null;
            Object modelSelection = modelCombo.getSelectedItem();
            String model = modelSelection != null
                    ? modelSelection.toString().trim()
                    : modelCombo.getEditor().getItem().toString().trim();
            try {
                gemini.updateConfig(key, model);
                JOptionPane.showMessageDialog(parent,
                        "Settings saved.\nModel: " + gemini.getModel(),
                        "Saved", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(parent, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}