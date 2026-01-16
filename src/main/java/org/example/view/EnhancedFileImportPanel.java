package org.example.view;

import org.example.theme.ThemeManager;
import org.example.util.FileDetectionService;
import org.example.util.SoundManager;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Map;

public class EnhancedFileImportPanel extends JPanel {
    private JButton selectFileButton;
    private JButton loadFileButton;
    private JComboBox<String> importTypeComboBox;
    private JTextArea fileInfoTextArea;
    private JLabel filePathLabel;
    private JPanel filePreviewPanel;

    private File selectedFile;
    private String detectedFileType;

    private ActionListener cpvCodeImportListener;
    private ActionListener procurementDataImportListener;

    public EnhancedFileImportPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        titlePanel.setBorder(ThemeManager.createThemedBorder());

        JLabel titleLabel = new JLabel("Enhanced File Import");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 16));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel subtitleLabel = new JLabel("Import CPV codes and procurement data from various file formats");
        subtitleLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        subtitleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

        JPanel labelPanel = new JPanel(new GridLayout(2, 1));
        labelPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        labelPanel.add(titleLabel);
        labelPanel.add(subtitleLabel);

        titlePanel.add(labelPanel, BorderLayout.CENTER);

        JPanel fileSelectionPanel = new JPanel(new BorderLayout(5, 0));
        fileSelectionPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        fileSelectionPanel.setBorder(ThemeManager.createThemedTitleBorder("Select File"));

        selectFileButton = new JButton("Browse...");
        selectFileButton.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        selectFileButton.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        selectFileButton.addActionListener(e -> selectFile());

        filePathLabel = new JLabel("No file selected");
        filePathLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        filePathLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));

        fileSelectionPanel.add(filePathLabel, BorderLayout.CENTER);
        fileSelectionPanel.add(selectFileButton, BorderLayout.EAST);

        fileInfoTextArea = new JTextArea();
        fileInfoTextArea.setEditable(false);
        fileInfoTextArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        fileInfoTextArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        fileInfoTextArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        fileInfoTextArea.setText("Select a file to see information and import options.");

        JScrollPane fileInfoScrollPane = new JScrollPane(fileInfoTextArea);
        fileInfoScrollPane.setBorder(BorderFactory.createEmptyBorder());

        JPanel fileInfoPanel = new JPanel(new BorderLayout());
        fileInfoPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        fileInfoPanel.setBorder(ThemeManager.createThemedTitleBorder("File Information"));
        fileInfoPanel.add(fileInfoScrollPane, BorderLayout.CENTER);

        filePreviewPanel = new JPanel(new BorderLayout());
        filePreviewPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        filePreviewPanel.setBorder(ThemeManager.createThemedTitleBorder("Preview"));
        filePreviewPanel.add(new JLabel("Select a file to preview its contents", SwingConstants.CENTER), BorderLayout.CENTER);

        JPanel importOptionsPanel = new JPanel(new BorderLayout(5, 0));
        importOptionsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        importOptionsPanel.setBorder(ThemeManager.createThemedTitleBorder("Import Options"));

        importTypeComboBox = new JComboBox<>(new String[] {
                "Select import type...",
                "CPV Codes File",
                "Procurement Data File",
                "Auto-detect (Recommended)"
        });
        importTypeComboBox.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        importTypeComboBox.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        loadFileButton = new JButton("Load File");
        loadFileButton.setBackground(ThemeManager.getCurrentTheme().getAccentColor());
        loadFileButton.setForeground(Color.WHITE);
        loadFileButton.setEnabled(false);
        loadFileButton.addActionListener(e -> loadSelectedFile());

        importOptionsPanel.add(importTypeComboBox, BorderLayout.CENTER);
        importOptionsPanel.add(loadFileButton, BorderLayout.EAST);

        JPanel centerPanel = new JPanel(new GridLayout(2, 1, 0, 10));
        centerPanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        centerPanel.add(fileInfoPanel);
        centerPanel.add(filePreviewPanel);

        JPanel topPanel = new JPanel(new GridLayout(3, 1, 0, 10));
        topPanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        topPanel.add(titlePanel);
        topPanel.add(fileSelectionPanel);
        topPanel.add(importOptionsPanel);

        add(topPanel, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
    }

    private void selectFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select File");
        fileChooser.setFileFilter(new FileNameExtensionFilter(
                "Excel Files", "xls", "xlsx"));

        int result = fileChooser.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            filePathLabel.setText(selectedFile.getName());

            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

            analyzeSelectedFile();
        }
    }

    private void analyzeSelectedFile() {
        if (selectedFile == null) return;

        fileInfoTextArea.setText("Analyzing file, please wait...");
        loadFileButton.setEnabled(false);

        SwingWorker<Map<String, Object>, Void> worker = new SwingWorker<>() {
            @Override
            protected Map<String, Object> doInBackground() {
                return FileDetectionService.analyzeFile(selectedFile);
            }

            @Override
            protected void done() {
                try {
                    Map<String, Object> analysis = get();
                    updateFileInfo(analysis);

                    String recommendedType = (String) analysis.getOrDefault("recommendedImportType", "unknown");
                    detectedFileType = recommendedType;

                    if ("cpv_codes".equals(recommendedType)) {
                        importTypeComboBox.setSelectedIndex(1);
                    } else if ("procurement_data".equals(recommendedType)) {
                        importTypeComboBox.setSelectedIndex(2);
                    } else {
                        importTypeComboBox.setSelectedIndex(3);
                    }

                    loadFileButton.setEnabled(true);

                    JPanel previewContent = new JPanel(new BorderLayout());
                    previewContent.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

                    JLabel previewLabel = new JLabel("<html>File preview would be shown here.<br>" +
                            "In a full implementation, this would show the first few rows of the file.</html>");
                    previewLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
                    previewLabel.setHorizontalAlignment(SwingConstants.CENTER);

                    previewContent.add(previewLabel, BorderLayout.CENTER);

                    filePreviewPanel.removeAll();
                    filePreviewPanel.add(previewContent, BorderLayout.CENTER);
                    filePreviewPanel.revalidate();
                    filePreviewPanel.repaint();

                } catch (Exception e) {
                    fileInfoTextArea.setText("Error analyzing file: " + e.getMessage());
                }
            }
        };

        worker.execute();
    }

    private void updateFileInfo(Map<String, Object> analysis) {
        StringBuilder info = new StringBuilder();

        info.append("File: ").append(analysis.get("fileName")).append("\n");
        info.append("Size: ").append(formatFileSize((long) analysis.get("fileSize"))).append("\n");
        info.append("Type: ").append(analysis.get("fileType")).append("\n\n");

        if (analysis.containsKey("error")) {
            info.append("Error: ").append(analysis.get("error")).append("\n");
        } else {
            info.append("Detected Content: ").append(analysis.get("detectedType")).append("\n");
            info.append("Confidence: ").append(analysis.get("confidenceScore")).append("%\n\n");

            info.append("Sheet Count: ").append(analysis.get("sheetCount")).append("\n");
            info.append("CPV Codes Found: ").append(analysis.get("totalCpvCodes")).append("\n");
            info.append("Procurement Items Found: ").append(analysis.get("totalProcurementItems")).append("\n\n");

            info.append("Recommended Import Type: ");
            String recommendedType = (String) analysis.get("recommendedImportType");

            if ("cpv_codes".equals(recommendedType)) {
                info.append("CPV Codes File\n");
            } else if ("procurement_data".equals(recommendedType)) {
                info.append("Procurement Data File\n");
            } else {
                info.append("Unknown (try both options)\n");
            }
        }

        fileInfoTextArea.setText(info.toString());
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " bytes";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        }
    }

    private void loadSelectedFile() {
        if (selectedFile == null) return;

        int selectedIndex = importTypeComboBox.getSelectedIndex();

        if (selectedIndex == 0) {
            JOptionPane.showMessageDialog(this,
                    "Please select an import type.",
                    "Import Type Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        ActionListener listener = null;

        if (selectedIndex == 1 || (selectedIndex == 3 && "cpv_codes".equals(detectedFileType))) {
            listener = cpvCodeImportListener;
        } else if (selectedIndex == 2 || (selectedIndex == 3 && "procurement_data".equals(detectedFileType))) {
            listener = procurementDataImportListener;
        } else if (selectedIndex == 3) {
            listener = cpvCodeImportListener;
        }

        if (listener != null) {
            listener.actionPerformed(
                    new java.awt.event.ActionEvent(selectedFile, java.awt.event.ActionEvent.ACTION_PERFORMED, "loadFile")
            );
        }
    }

    public void setCpvCodeImportListener(ActionListener listener) {
        this.cpvCodeImportListener = listener;
    }

    public void setProcurementDataImportListener(ActionListener listener) {
        this.procurementDataImportListener = listener;
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        updateComponentTheme(this);

        for (Component component : getComponents()) {
            if (component instanceof JPanel) {
                JPanel panel = (JPanel) component;

                Border border = panel.getBorder();
                if (border instanceof javax.swing.border.TitledBorder) {
                    panel.setBorder(ThemeManager.createThemedTitleBorder(
                            ((javax.swing.border.TitledBorder) border).getTitle()
                    ));
                } else {
                    panel.setBorder(ThemeManager.createThemedBorder());
                }
            }
        }

        loadFileButton.setBackground(ThemeManager.getCurrentTheme().getAccentColor());
        loadFileButton.setForeground(Color.WHITE);

        selectFileButton.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        selectFileButton.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        importTypeComboBox.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        importTypeComboBox.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        fileInfoTextArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        fileInfoTextArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        filePathLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        revalidate();
        repaint();
    }

    private void updateComponentTheme(Container container) {
        if (container instanceof JPanel) {
            ((JPanel) container).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        }

        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                ((JLabel) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (component instanceof JButton) {
                ((JButton) component).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JButton) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (component instanceof Container) {
                updateComponentTheme((Container) component);
            }
        }
    }
}