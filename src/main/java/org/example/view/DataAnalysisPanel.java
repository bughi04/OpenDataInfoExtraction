package org.example.view;

import org.example.theme.ThemeManager;
import org.example.util.FileAnalyzerService;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class DataAnalysisPanel extends JPanel {
    private JTable dataTable;
    private JScrollPane tableScrollPane;
    private JTextArea summaryTextArea;
    private JScrollPane summaryScrollPane;
    private JLabel fileInfoLabel;
    private JPanel chartsPanel;
    private JTabbedPane chartsTabbedPane;
    private Map<String, Object> currentAnalysisResults;

    public DataAnalysisPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        infoPanel.setBorder(ThemeManager.createThemedTitleBorder("File Information"));

        fileInfoLabel = new JLabel("No data file loaded");
        fileInfoLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        fileInfoLabel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        infoPanel.add(fileInfoLabel, BorderLayout.CENTER);

        JTabbedPane contentTabbedPane = new JTabbedPane();
        contentTabbedPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        contentTabbedPane.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JPanel dataPanel = new JPanel(new BorderLayout());
        dataPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        dataTable = new JTable();
        dataTable.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        dataTable.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        dataTable.setGridColor(ThemeManager.getCurrentTheme().getBorderColor());
        dataTable.setRowHeight(22);
        dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        tableScrollPane = new JScrollPane(dataTable);
        tableScrollPane.getViewport().setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        dataPanel.add(tableScrollPane, BorderLayout.CENTER);

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        summaryTextArea = new JTextArea();
        summaryTextArea.setEditable(false);
        summaryTextArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        summaryTextArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        summaryTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        summaryTextArea.setText("Data analysis will appear here after loading a file.");

        summaryScrollPane = new JScrollPane(summaryTextArea);
        summaryScrollPane.setBorder(BorderFactory.createEmptyBorder());
        summaryPanel.add(summaryScrollPane, BorderLayout.CENTER);

        chartsPanel = new JPanel(new BorderLayout());
        chartsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        chartsTabbedPane = new JTabbedPane();
        chartsTabbedPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        chartsTabbedPane.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        chartsPanel.add(chartsTabbedPane, BorderLayout.CENTER);
        chartsPanel.add(new JLabel("Load a file to generate charts", SwingConstants.CENTER), BorderLayout.NORTH);

        contentTabbedPane.addTab("Data Preview", null, dataPanel, "View data from the file");
        contentTabbedPane.addTab("Analysis Summary", null, summaryPanel, "View statistical analysis");
        contentTabbedPane.addTab("Charts", null, chartsPanel, "View data visualizations");

        add(infoPanel, BorderLayout.NORTH);
        add(contentTabbedPane, BorderLayout.CENTER);
    }

    public void displayDataAnalysis(Map<String, Object> analysisResults) {
        this.currentAnalysisResults = analysisResults;

        if (analysisResults.containsKey("error")) {
            String error = (String) analysisResults.get("error");
            fileInfoLabel.setText("<html><font color='red'>" + error + "</font></html>");
            summaryTextArea.setText("Error: " + error);
            return;
        }

        StringBuilder fileInfo = new StringBuilder();
        fileInfo.append("<html>");
        fileInfo.append("File: <b>").append(analysisResults.get("fileName")).append("</b> | ");
        fileInfo.append("Type: ").append(analysisResults.get("fileType")).append(" | ");
        fileInfo.append("Size: ").append(formatFileSize((long) analysisResults.get("fileSize"))).append(" | ");

        if (analysisResults.containsKey("delimiter")) {
            fileInfo.append("Delimiter: '").append(analysisResults.get("delimiter")).append("' | ");
        }

        if (analysisResults.containsKey("sheetName")) {
            fileInfo.append("Sheet: ").append(analysisResults.get("sheetName")).append(" | ");
        }

        fileInfo.append("Rows: ").append(analysisResults.get("rowCount")).append(" | ");
        fileInfo.append("Columns: ").append(analysisResults.get("columnCount"));
        fileInfo.append("</html>");

        fileInfoLabel.setText(fileInfo.toString());

        List<String> headers = (List<String>) analysisResults.get("headers");
        List<List<String>> data = (List<List<String>>) analysisResults.get("sampleData");

        DefaultTableModel model = new DefaultTableModel();

        for (String header : headers) {
            model.addColumn(header);
        }

        for (List<String> row : data) {
            model.addRow(row.toArray());
        }

        dataTable.setModel(model);

        for (int i = 0; i < dataTable.getColumnCount(); i++) {
            int width = 100;

            String header = dataTable.getColumnName(i);
            int headerWidth = header.length() * 8 + 20;

            int maxDataWidth = 100;
            for (int j = 0; j < Math.min(20, dataTable.getRowCount()); j++) {
                Object value = dataTable.getValueAt(j, i);
                if (value != null) {
                    int cellWidth = value.toString().length() * 7 + 10;
                    maxDataWidth = Math.max(maxDataWidth, cellWidth);
                }
            }

            width = Math.max(headerWidth, maxDataWidth);
            width = Math.min(width, 300);

            dataTable.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        updateSummary(analysisResults);

        generateCharts(analysisResults);
    }

    private void updateSummary(Map<String, Object> analysisResults) {
        StringBuilder summary = new StringBuilder();
        summary.append("Data Analysis Summary\n");
        summary.append("===================\n\n");

        summary.append("File: ").append(analysisResults.get("fileName")).append("\n");
        summary.append("Type: ").append(analysisResults.get("fileType")).append("\n");
        summary.append("Size: ").append(formatFileSize((long) analysisResults.get("fileSize"))).append("\n");

        if (analysisResults.containsKey("delimiter")) {
            summary.append("Delimiter: '").append(analysisResults.get("delimiter")).append("'\n");
        }

        if (analysisResults.containsKey("sheetName")) {
            summary.append("Sheet: ").append(analysisResults.get("sheetName")).append("\n");
            if (analysisResults.containsKey("sheetCount")) {
                summary.append("Total Sheets: ").append(analysisResults.get("sheetCount")).append("\n");
            }
        }

        summary.append("Total Rows: ").append(analysisResults.get("rowCount")).append("\n");
        summary.append("Total Columns: ").append(analysisResults.get("columnCount")).append("\n\n");

        summary.append("Column Analysis:\n");
        Map<String, Map<String, Object>> columnStats = (Map<String, Map<String, Object>>) analysisResults.get("columnStats");

        if (columnStats != null) {
            for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
                String columnName = entry.getKey();
                Map<String, Object> stats = entry.getValue();

                summary.append("\nColumn: ").append(columnName).append("\n");
                summary.append("  Type: ").append(stats.get("type")).append("\n");

                if (stats.containsKey("nonEmptyCount")) {
                    int nonEmptyCount = (int) stats.get("nonEmptyCount");
                    int rowCount = (int) analysisResults.get("rowCount");
                    double filledPercentage = ((double) nonEmptyCount / rowCount) * 100;

                    summary.append("  Filled: ").append(String.format("%.1f%%", filledPercentage));
                    summary.append(" (").append(nonEmptyCount).append(" non-empty values)\n");
                }

                if (stats.containsKey("uniqueValues")) {
                    int uniqueValues = (int) stats.get("uniqueValues");
                    summary.append("  Unique Values: ").append(uniqueValues).append("\n");
                }

                if (stats.containsKey("minValue")) {
                    summary.append("  Min Value: ").append(stats.get("minValue")).append("\n");
                }

                if (stats.containsKey("maxValue")) {
                    summary.append("  Max Value: ").append(stats.get("maxValue")).append("\n");
                }

                if (stats.containsKey("avgValue")) {
                    summary.append("  Average: ").append(String.format("%.2f", (double) stats.get("avgValue"))).append("\n");
                }

                if (stats.containsKey("sum")) {
                    summary.append("  Sum: ").append(String.format("%.2f", (double) stats.get("sum"))).append("\n");
                }
            }

            summary.append("\nData Quality Summary:\n");
            int totalCells = (int) analysisResults.get("rowCount") * (int) analysisResults.get("columnCount");
            int filledCells = 0;

            for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
                Map<String, Object> stats = entry.getValue();
                if (stats.containsKey("nonEmptyCount")) {
                    filledCells += (int) stats.get("nonEmptyCount");
                }
            }

            double completenessPercentage = totalCells > 0 ? ((double) filledCells / totalCells) * 100 : 0;
            summary.append("  Completeness: ").append(String.format("%.1f%%", completenessPercentage));
            summary.append(" (").append(filledCells).append(" out of ").append(totalCells).append(" cells filled)\n");
        } else {
            summary.append("No column statistics available.");
        }

        summaryTextArea.setText(summary.toString());
        summaryTextArea.setCaretPosition(0);
    }

    private void generateCharts(Map<String, Object> analysisResults) {
        chartsTabbedPane.removeAll();

        try {
            JFreeChart distributionChart = FileAnalyzerService.createValueDistributionChart(analysisResults);
            if (distributionChart != null) {
                ChartPanel distributionPanel = new ChartPanel(distributionChart);
                distributionPanel.setPreferredSize(new Dimension(600, 400));
                distributionPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                chartsTabbedPane.addTab("Distribution", distributionPanel);
            }

            JFreeChart pieChart = FileAnalyzerService.createPieChart(analysisResults);
            if (pieChart != null) {
                ChartPanel piePanel = new ChartPanel(pieChart);
                piePanel.setPreferredSize(new Dimension(600, 400));
                piePanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                chartsTabbedPane.addTab("Pie Chart", piePanel);
            }

            JFreeChart barChart = FileAnalyzerService.createCountByColumnChart(analysisResults);
            if (barChart != null) {
                ChartPanel barPanel = new ChartPanel(barChart);
                barPanel.setPreferredSize(new Dimension(600, 400));
                barPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                chartsTabbedPane.addTab("Bar Chart", barPanel);
            }

            if (chartsTabbedPane.getTabCount() > 0) {
                chartsPanel.removeAll();
                chartsPanel.add(chartsTabbedPane, BorderLayout.CENTER);

                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

                JButton exportImageButton = ThemeManager.createThemedButton("Export Chart as Image");
                exportImageButton.addActionListener(e -> exportCurrentChartAsImage());

                buttonPanel.add(exportImageButton);
                chartsPanel.add(buttonPanel, BorderLayout.SOUTH);
            } else {
                chartsPanel.removeAll();
                JLabel noChartsLabel = new JLabel("No suitable data for charts", SwingConstants.CENTER);
                noChartsLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
                chartsPanel.add(noChartsLabel, BorderLayout.CENTER);
            }

            chartsPanel.revalidate();
            chartsPanel.repaint();

        } catch (Exception e) {
            e.printStackTrace();
            JLabel errorLabel = new JLabel("Error generating charts: " + e.getMessage(), SwingConstants.CENTER);
            errorLabel.setForeground(Color.RED);
            chartsPanel.removeAll();
            chartsPanel.add(errorLabel, BorderLayout.CENTER);
            chartsPanel.revalidate();
            chartsPanel.repaint();
        }
    }

    private void exportCurrentChartAsImage() {
        int selectedIndex = chartsTabbedPane.getSelectedIndex();
        if (selectedIndex < 0) return;

        Component comp = chartsTabbedPane.getComponentAt(selectedIndex);
        if (!(comp instanceof ChartPanel)) return;

        ChartPanel chartPanel = (ChartPanel) comp;
        JFreeChart chart = chartPanel.getChart();

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Chart as PNG");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "PNG Images", "png"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                java.io.File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new java.io.File(file.getAbsolutePath() + ".png");
                }

                java.awt.image.BufferedImage image = chart.createBufferedImage(800, 600);
                javax.imageio.ImageIO.write(image, "png", file);

                JOptionPane.showMessageDialog(this,
                        "Chart exported successfully to " + file.getName(),
                        "Export Successful", JOptionPane.INFORMATION_MESSAGE);

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Error exporting chart: " + ex.getMessage(),
                        "Export Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String formatFileSize(long size) {
        if (size < 1024) {
            return size + " bytes";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        }
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        updateComponentTheme(this);
    }

    private void updateComponentTheme(Container container) {
        if (container instanceof JPanel) {
            ((JPanel) container).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        }

        if (container instanceof JScrollPane) {
            ((JScrollPane) container).getViewport().setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        }

        if (container instanceof JTabbedPane) {
            ((JTabbedPane) container).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            ((JTabbedPane) container).setForeground(ThemeManager.getCurrentTheme().getTextColor());
        }

        for (Component component : container.getComponents()) {
            if (component instanceof JLabel) {
                ((JLabel) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (component instanceof JTextArea) {
                ((JTextArea) component).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextArea) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (component instanceof JTable) {
                ((JTable) component).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTable) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
                ((JTable) component).setGridColor(ThemeManager.getCurrentTheme().getBorderColor());
            } else if (component instanceof JButton) {
                ((JButton) component).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JButton) component).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (component instanceof Container) {
                updateComponentTheme((Container) component);
            }
        }
    }
}