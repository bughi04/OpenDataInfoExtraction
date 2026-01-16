package org.example.view;

import org.example.ml.*;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;
import org.example.util.SoundManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Objects;

public class MachineLearningPanel extends JPanel {
    private MachineLearningService mlService;
    private DataModel dataModel;

    public JTabbedPane mlTabbedPane;
    private JTextArea mlResultsArea;
    private JProgressBar mlProgressBar;
    private JLabel mlStatusLabel;

    private JComboBox<String> cpvCategoryCombo;
    private JTextField objectDescriptionField;
    private JComboBox<String> sourceCombo;
    private JSpinner durationSpinner;
    private JLabel predictionResultLabel;

    private JTable riskClassificationTable;
    private JButton classifyAllButton;

    private JSpinner clusterCountSpinner;
    private JTable clusterResultsTable;
    private JTextArea clusterInsightsArea;

    private JSlider sensitivitySlider;
    private JTable anomalyTable;
    private JTextArea anomalyExplanationsArea;

    public MachineLearningPanel() {
        this.mlService = new MachineLearningService();
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel headerPanel = createHeaderPanel();

        mlTabbedPane = new JTabbedPane();
        mlTabbedPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        mlTabbedPane.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        mlTabbedPane.addTab("🔮 Prediction", createPredictionPanel());
        mlTabbedPane.addTab("🏷️ Classification", createClassificationPanel());
        mlTabbedPane.addTab("🎯 Clustering", createClusteringPanel());
        mlTabbedPane.addTab("⚠️ Anomaly Detection", createAnomalyDetectionPanel());
        mlTabbedPane.addTab("📊 ML Analysis", createAnalysisPanel());

        JPanel statusPanel = createStatusPanel();

        add(headerPanel, BorderLayout.NORTH);
        add(mlTabbedPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ThemeManager.getCurrentTheme().getAccentColor()),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("🤖 Machine Learning Analysis");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JLabel subtitleLabel = new JLabel("AI-powered procurement data analysis and predictions");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        JButton trainModelsButton = ThemeManager.createThemedButton("🎓 Train Models");
        trainModelsButton.setToolTipText("Train ML models with current data");
        trainModelsButton.addActionListener(e -> trainModels());

        panel.add(textPanel, BorderLayout.CENTER);
        panel.add(trainModelsButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createPredictionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        inputPanel.setBorder(BorderFactory.createTitledBorder("Prediction Parameters"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("CPV Category:"), gbc);

        gbc.gridx = 1;
        cpvCategoryCombo = new JComboBox<>(new String[]{"45 - Construction", "34 - Transport", "72 - IT Services", "71 - Architecture"});
        cpvCategoryCombo.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        cpvCategoryCombo.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        inputPanel.add(cpvCategoryCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Description:"), gbc);

        gbc.gridx = 1;
        objectDescriptionField = new JTextField(20);
        objectDescriptionField.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        objectDescriptionField.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        objectDescriptionField.setText("Road construction project");
        inputPanel.add(objectDescriptionField, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Source:"), gbc);

        gbc.gridx = 1;
        sourceCombo = new JComboBox<>(new String[]{"Local Budget", "EU Funds", "National Budget", "Private"});
        sourceCombo.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        sourceCombo.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        inputPanel.add(sourceCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        inputPanel.add(new JLabel("Duration (days):"), gbc);

        gbc.gridx = 1;
        durationSpinner = new JSpinner(new SpinnerNumberModel(30, 1, 365, 1));
        inputPanel.add(durationSpinner, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2;
        JButton predictButton = ThemeManager.createThemedButton("🔮 Predict Value");
        predictButton.addActionListener(e -> performPrediction());
        inputPanel.add(predictButton, gbc);

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        resultsPanel.setBorder(BorderFactory.createTitledBorder("Prediction Results"));

        predictionResultLabel = new JLabel("No prediction made yet");
        predictionResultLabel.setFont(new Font("Arial", Font.BOLD, 16));
        predictionResultLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        predictionResultLabel.setHorizontalAlignment(SwingConstants.CENTER);
        predictionResultLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        resultsPanel.add(predictionResultLabel, BorderLayout.CENTER);

        panel.add(inputPanel, BorderLayout.NORTH);
        panel.add(resultsPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createClassificationPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        classifyAllButton = ThemeManager.createThemedButton("🏷️ Classify All Items");
        classifyAllButton.addActionListener(e -> performRiskClassification());
        controlPanel.add(classifyAllButton);

        String[] columnNames = {"Item Name", "Value (RON)", "Risk Level", "CPV Category"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        riskClassificationTable = new JTable(tableModel);
        riskClassificationTable.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        riskClassificationTable.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        riskClassificationTable.setGridColor(ThemeManager.getCurrentTheme().getBorderColor());
        riskClassificationTable.setSelectionBackground(ThemeManager.getCurrentTheme().getAccentColor());
        riskClassificationTable.setSelectionForeground(Color.WHITE);

        JScrollPane tableScrollPane = new JScrollPane(riskClassificationTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Risk Classification Results"));

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(tableScrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createClusteringPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        controlPanel.add(new JLabel("Number of clusters:"));
        clusterCountSpinner = new JSpinner(new SpinnerNumberModel(5, 2, 10, 1));
        controlPanel.add(clusterCountSpinner);

        JButton clusterButton = ThemeManager.createThemedButton("🎯 Perform Clustering");
        clusterButton.addActionListener(e -> performClustering());
        controlPanel.add(clusterButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        String[] columnNames = {"Cluster", "Items Count", "Avg Value (RON)", "Dominant Category"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        clusterResultsTable = new JTable(tableModel);
        clusterResultsTable.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        clusterResultsTable.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JScrollPane tableScrollPane = new JScrollPane(clusterResultsTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Cluster Summary"));
        tableScrollPane.setPreferredSize(new Dimension(0, 200));

        clusterInsightsArea = new JTextArea(8, 40);
        clusterInsightsArea.setEditable(false);
        clusterInsightsArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        clusterInsightsArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        clusterInsightsArea.setFont(new Font("Arial", Font.PLAIN, 12));
        clusterInsightsArea.setText("Perform clustering to see insights about data patterns.");

        JScrollPane insightsScrollPane = new JScrollPane(clusterInsightsArea);
        insightsScrollPane.setBorder(BorderFactory.createTitledBorder("Clustering Insights"));

        splitPane.setTopComponent(tableScrollPane);
        splitPane.setBottomComponent(insightsScrollPane);
        splitPane.setResizeWeight(0.6);

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAnomalyDetectionPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        controlPanel.add(new JLabel("Sensitivity:"));
        sensitivitySlider = new JSlider(1, 50, 10);
        sensitivitySlider.setMajorTickSpacing(10);
        sensitivitySlider.setMinorTickSpacing(5);
        sensitivitySlider.setPaintTicks(true);
        sensitivitySlider.setPaintLabels(true);
        sensitivitySlider.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        sensitivitySlider.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        controlPanel.add(sensitivitySlider);

        JButton detectButton = ThemeManager.createThemedButton("⚠️ Detect Anomalies");
        detectButton.addActionListener(e -> performAnomalyDetection());
        controlPanel.add(detectButton);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        String[] columnNames = {"Item Name", "Value (RON)", "Anomaly Score", "CPV Code"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        anomalyTable = new JTable(tableModel);
        anomalyTable.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        anomalyTable.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JScrollPane tableScrollPane = new JScrollPane(anomalyTable);
        tableScrollPane.setBorder(BorderFactory.createTitledBorder("Detected Anomalies"));
        tableScrollPane.setPreferredSize(new Dimension(0, 200));

        anomalyExplanationsArea = new JTextArea(8, 40);
        anomalyExplanationsArea.setEditable(false);
        anomalyExplanationsArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        anomalyExplanationsArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        anomalyExplanationsArea.setFont(new Font("Arial", Font.PLAIN, 12));
        anomalyExplanationsArea.setText("Run anomaly detection to see explanations for unusual items.");

        JScrollPane explanationsScrollPane = new JScrollPane(anomalyExplanationsArea);
        explanationsScrollPane.setBorder(BorderFactory.createTitledBorder("Anomaly Explanations"));

        splitPane.setTopComponent(tableScrollPane);
        splitPane.setBottomComponent(explanationsScrollPane);
        splitPane.setResizeWeight(0.6);

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controlPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JButton generateReportButton = ThemeManager.createThemedButton("📊 Generate ML Analysis Report");
        generateReportButton.addActionListener(e -> generateMLAnalysisReport());
        controlPanel.add(generateReportButton);

        JButton exportReportButton = ThemeManager.createThemedButton("💾 Export Report");
        exportReportButton.addActionListener(e -> exportMLReport());
        controlPanel.add(exportReportButton);

        mlResultsArea = new JTextArea();
        mlResultsArea.setEditable(false);
        mlResultsArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        mlResultsArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        mlResultsArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        mlResultsArea.setText("Click 'Generate ML Analysis Report' to see comprehensive machine learning insights.");

        JScrollPane scrollPane = new JScrollPane(mlResultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("ML Analysis Results"));

        panel.add(controlPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getCurrentTheme().getBorderColor()),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        mlStatusLabel = new JLabel("🤖 Ready - Load data and train models to begin ML analysis");
        mlStatusLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        mlProgressBar = new JProgressBar();
        mlProgressBar.setVisible(false);
        mlProgressBar.setStringPainted(true);
        mlProgressBar.setForeground(ThemeManager.getCurrentTheme().getAccentColor());

        panel.add(mlStatusLabel, BorderLayout.CENTER);
        panel.add(mlProgressBar, BorderLayout.EAST);

        return panel;
    }


    private void trainModels() {
        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            showError("Please load procurement data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        setStatus("🎓 Training ML models...", true);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() throws Exception {
                mlService.trainModels();
                return null;
            }

            @Override
            protected void done() {
                setStatus("✅ ML models trained successfully", false);
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                populateComboBoxes();
            }
        };
        worker.execute();
    }

    private void performPrediction() {
        if (dataModel == null) {
            showError("Please load data and train models first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        setStatus("🔮 Making prediction...", true);

        String selectedCategory = (String) cpvCategoryCombo.getSelectedItem();
        String cpvCode = extractCpvCode(selectedCategory);
        String description = objectDescriptionField.getText();
        String source = (String) sourceCombo.getSelectedItem();
        int duration = (Integer) durationSpinner.getValue();

        SwingWorker<MLPredictionResult, Void> worker = new SwingWorker<>() {
            @Override
            protected MLPredictionResult doInBackground() throws Exception {
                return mlService.predictProcurementValue(cpvCode, description, source, duration);
            }

            @Override
            protected void done() {
                try {
                    MLPredictionResult result = get();
                    displayPredictionResult(result);
                    setStatus("✅ Prediction completed", false);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showError("Error in prediction: " + e.getMessage());
                    setStatus("❌ Prediction failed", false);
                }
            }
        };
        worker.execute();
    }

    private void performRiskClassification() {
        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            showError("Please load procurement data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        setStatus("🏷️ Classifying risk levels...", true);

        SwingWorker<MLClassificationResult, Void> worker = new SwingWorker<>() {
            @Override
            protected MLClassificationResult doInBackground() throws Exception {
                return mlService.classifyRiskLevel(dataModel.getProcurementItems());
            }

            @Override
            protected void done() {
                try {
                    MLClassificationResult result = get();
                    displayClassificationResults(result);
                    setStatus("✅ Risk classification completed", false);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showError("Error in classification: " + e.getMessage());
                    setStatus("❌ Classification failed", false);
                }
            }
        };
        worker.execute();
    }

    private void performClustering() {
        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            showError("Please load procurement data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        int clusterCount = (Integer) clusterCountSpinner.getValue();
        setStatus("🎯 Performing clustering with " + clusterCount + " clusters...", true);

        SwingWorker<MLClusteringResult, Void> worker = new SwingWorker<>() {
            @Override
            protected MLClusteringResult doInBackground() throws Exception {
                return mlService.performClustering(clusterCount);
            }

            @Override
            protected void done() {
                try {
                    MLClusteringResult result = get();
                    displayClusteringResults(result);
                    setStatus("✅ Clustering completed", false);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showError("Error in clustering: " + e.getMessage());
                    setStatus("❌ Clustering failed", false);
                }
            }
        };
        worker.execute();
    }

    private void performAnomalyDetection() {
        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            showError("Please load procurement data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        double sensitivity = sensitivitySlider.getValue() / 100.0;
        setStatus("⚠️ Detecting anomalies with sensitivity " + String.format("%.1f%%", sensitivity * 100) + "...", true);

        SwingWorker<MLAnomalyResult, Void> worker = new SwingWorker<>() {
            @Override
            protected MLAnomalyResult doInBackground() throws Exception {
                return mlService.detectAnomalies(sensitivity);
            }

            @Override
            protected void done() {
                try {
                    MLAnomalyResult result = get();
                    displayAnomalyResults(result);
                    setStatus("✅ Anomaly detection completed", false);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showError("Error in anomaly detection: " + e.getMessage());
                    setStatus("❌ Anomaly detection failed", false);
                }
            }
        };
        worker.execute();
    }

    private void generateMLAnalysisReport() {
        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            showError("Please load procurement data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
        setStatus("📊 Generating comprehensive ML analysis report...", true);

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() throws Exception {
                return mlService.generateMLAnalysisReport();
            }

            @Override
            protected void done() {
                try {
                    String report = get();
                    mlResultsArea.setText(report);
                    mlResultsArea.setCaretPosition(0);
                    setStatus("✅ ML analysis report generated", false);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showError("Error generating ML report: " + e.getMessage());
                    setStatus("❌ Report generation failed", false);
                }
            }
        };
        worker.execute();
    }

    private void exportMLReport() {
        String reportText = mlResultsArea.getText();
        if (reportText == null || reportText.trim().isEmpty()) {
            showError("No ML analysis report to export. Generate report first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export ML Analysis Report");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Text Files (*.txt)", "txt"));

        String timestamp = java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        fileChooser.setSelectedFile(new java.io.File("ML_Analysis_Report_" + timestamp + ".txt"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new java.io.File(file.getAbsolutePath() + ".txt");
            }

            try (java.io.FileWriter writer = new java.io.FileWriter(file)) {
                writer.write("MACHINE LEARNING ANALYSIS REPORT\n");
                writer.write("Generated: " + java.time.LocalDateTime.now().format(
                        java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "\n");
                writer.write("=" + "=".repeat(80) + "\n\n");
                writer.write(reportText);

                setStatus("✅ ML report exported to " + file.getName(), false);
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
            } catch (java.io.IOException ex) {
                showError("Error exporting report: " + ex.getMessage());
            }
        }
    }

    private void displayPredictionResult(MLPredictionResult result) {
        if ("Success".equals(result.getStatus())) {
            String resultText = String.format(
                    "<html><div style='text-align: center;'>" +
                            "<h2 style='color: #4CAF50;'>Predicted Value</h2>" +
                            "<h1 style='color: #2196F3;'>%.2f RON</h1>" +
                            "<p>Confidence: <b>%.1f%%</b></p>" +
                            "</div></html>",
                    result.getPredictedValue(), result.getConfidence() * 100
            );
            predictionResultLabel.setText(resultText);
        } else {
            predictionResultLabel.setText("<html><div style='text-align: center; color: red;'>" +
                    "<h3>Prediction Error</h3><p>" + result.getStatus() + "</p></div></html>");
        }
    }

    private void displayClassificationResults(MLClassificationResult result) {
        DefaultTableModel model = (DefaultTableModel) riskClassificationTable.getModel();
        model.setRowCount(0);

        if ("Success".equals(result.getStatus())) {
            result.getClassifications().forEach((item, risk) -> {
                String category = item.getCpvCodes().isEmpty() ? "N/A" :
                        item.getCpvCodes().get(0).substring(0, 2);

                model.addRow(new Object[] {
                        truncateText(item.getObjectName(), 40),
                        String.format("%.2f", item.getValueWithoutTVA()),
                        risk,
                        category
                });
            });
        }
    }

    private void displayClusteringResults(MLClusteringResult result) {
        DefaultTableModel model = (DefaultTableModel) clusterResultsTable.getModel();
        model.setRowCount(0);

        if ("Success".equals(result.getStatus())) {
            result.getClusters().forEach((clusterId, items) -> {
                double avgValue = items.stream()
                        .mapToDouble(ProcurementItem::getValueWithoutTVA)
                        .average().orElse(0);

                Map<String, Long> categoryCount = items.stream()
                        .flatMap(item -> item.getCpvCodes().stream())
                        .collect(java.util.stream.Collectors.groupingBy(
                                code -> code.substring(0, 2),
                                java.util.stream.Collectors.counting()));

                String dominantCategory = categoryCount.entrySet().stream()
                        .max(Map.Entry.comparingByValue())
                        .map(Map.Entry::getKey)
                        .orElse("N/A");

                model.addRow(new Object[] {
                        "Cluster " + (clusterId + 1),
                        items.size(),
                        String.format("%.2f", avgValue),
                        dominantCategory
                });
            });

            StringBuilder insights = new StringBuilder();
            insights.append("Clustering Analysis Insights:\n\n");
            result.getInsights().forEach(insight ->
                    insights.append("• ").append(insight).append("\n"));

            clusterInsightsArea.setText(insights.toString());
        }
    }

    private void displayAnomalyResults(MLAnomalyResult result) {
        DefaultTableModel model = (DefaultTableModel) anomalyTable.getModel();
        model.setRowCount(0);

        if ("Success".equals(result.getStatus())) {
            result.getAnomalies().forEach(item -> {
                String cpvCode = item.getCpvCodes().isEmpty() ? "N/A" : item.getCpvCodes().get(0);

                model.addRow(new Object[] {
                        truncateText(item.getObjectName(), 40),
                        String.format("%.2f", item.getValueWithoutTVA()),
                        "High",
                        cpvCode
                });
            });

            StringBuilder explanations = new StringBuilder();
            explanations.append("Anomaly Explanations:\n\n");
            result.getExplanations().forEach(explanation ->
                    explanations.append("• ").append(explanation).append("\n"));

            anomalyExplanationsArea.setText(explanations.toString());
        }
    }

    private void populateComboBoxes() {
        if (dataModel == null) return;

        Set<String> categories = dataModel.getProcurementItems().stream()
                .flatMap(item -> item.getCpvCodes().stream())
                .map(code -> code.length() >= 2 ? code.substring(0, 2) : "00")
                .collect(java.util.stream.Collectors.toSet());

        DefaultComboBoxModel<String> cpvModel = new DefaultComboBoxModel<>();
        categories.forEach(cat -> cpvModel.addElement(cat + " - Category " + cat));
        if (cpvModel.getSize() == 0) {
            cpvModel.addElement("45 - Construction");
        }
        cpvCategoryCombo.setModel(cpvModel);

        Set<String> sources = dataModel.getProcurementItems().stream()
                .map(ProcurementItem::getSource)
                .filter(Objects::nonNull)
                .filter(s -> !s.trim().isEmpty())
                .collect(java.util.stream.Collectors.toSet());

        DefaultComboBoxModel<String> sourceModel = new DefaultComboBoxModel<>();
        if (sources.isEmpty()) {
            sourceModel.addElement("Local Budget");
            sourceModel.addElement("EU Funds");
            sourceModel.addElement("National Budget");
        } else {
            sources.forEach(sourceModel::addElement);
        }
        sourceCombo.setModel(sourceModel);
    }

    private String extractCpvCode(String categorySelection) {
        if (categorySelection == null || categorySelection.length() < 2) {
            return "45000000-0";
        }
        return categorySelection.substring(0, 2) + "000000-0";
    }

    private String truncateText(String text, int maxLength) {
        if (text == null) return "N/A";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private void setStatus(String message, boolean showProgress) {
        mlStatusLabel.setText(message);
        mlProgressBar.setVisible(showProgress);
        if (showProgress) {
            mlProgressBar.setIndeterminate(true);
        } else {
            mlProgressBar.setIndeterminate(false);
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Machine Learning Error", JOptionPane.ERROR_MESSAGE);
        SoundManager.playSound(SoundManager.SOUND_ERROR);
    }

    public void setDataModel(DataModel dataModel) {
        this.dataModel = dataModel;
        mlService.setDataModel(dataModel);

        if (dataModel != null && !dataModel.getProcurementItems().isEmpty()) {
            setStatus("📊 Data loaded - " + dataModel.getProcurementItems().size() + " items ready for ML analysis", false);
            populateComboBoxes();
        } else {
            setStatus("🤖 Ready - Load data to begin ML analysis", false);
        }
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        updateComponentTheme(this);
        revalidate();
        repaint();
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            } else if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTextArea) {
                ((JTextArea) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextArea) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTable) {
                ((JTable) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTable) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
                ((JTable) comp).setGridColor(ThemeManager.getCurrentTheme().getBorderColor());
                ((JTable) comp).setSelectionBackground(ThemeManager.getCurrentTheme().getAccentColor());
                ((JTable) comp).setSelectionForeground(Color.WHITE);
            } else if (comp instanceof JComboBox) {
                ((JComboBox<?>) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JComboBox<?>) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTextField) {
                ((JTextField) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextField) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
                ((JTextField) comp).setCaretColor(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof Container) {
                updateComponentTheme((Container) comp);
            }
        }
    }
}