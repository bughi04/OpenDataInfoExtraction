package org.example.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;

public class AnalysisPanel extends JPanel {
    private JTextArea analysisTextArea;
    private JScrollPane scrollPane;
    private JButton generateButton;

    public AnalysisPanel() {
        setLayout(new BorderLayout());

        analysisTextArea = new JTextArea();
        analysisTextArea.setEditable(false);
        analysisTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        analysisTextArea.setMargin(new Insets(10, 10, 10, 10));

        scrollPane = new JScrollPane(analysisTextArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        generateButton = new JButton("Generate Analysis");
        generateButton.setToolTipText("Generate analysis report from the loaded data");
        controlPanel.add(generateButton);
        controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        add(controlPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public void setGenerateAnalysisAction(ActionListener listener) {
        generateButton.addActionListener(listener);
    }

    public void displayAnalysis(String analysis) {
        analysisTextArea.setText(analysis);
        analysisTextArea.setCaretPosition(0);
    }
}