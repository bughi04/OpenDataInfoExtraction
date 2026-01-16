package org.example.controller;

import org.example.model.ProcurementItem;
import org.example.util.ChartGenerator;
import org.example.view.ModernMainView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static java.awt.print.Printable.NO_SUCH_PAGE;
import static java.awt.print.Printable.PAGE_EXISTS;

public class MainController {
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    private ModernMainView view;
    private DataController dataController;
    private boolean paapLoaded = false;
    private boolean cpvCodesLoaded = false;
    private File lastPaapFile = null;
    private File lastCpvFile = null;
    private File lastExportFile = null;

    public MainController(ModernMainView view, DataController dataController) {
        this.view = view;
        this.dataController = dataController;
        logger.info("MainController initialized for Excel file processing");

        initEventHandlers();
        view.setEnhancedImportListeners(this::loadCpvCodesFile, this::loadPaapFile);
    }

    private void initEventHandlers() {
        view.setLoadPaapAction(this::loadPaapFile);

        view.setLoadCpvCodesAction(this::loadCpvCodesFile);

        view.setSearchAction(this::searchProcurementItems);

        view.setGenerateChartsAction(this::generateAllCharts);

        view.setGenerateAnalysisAction(this::generateAnalysis);

        view.setExportAnalysisAction(this::exportAnalysis);

        view.setPrintAnalysisAction(this::printAnalysis);

        logger.info("Event handlers initialized for Excel-only processing");
    }

    private void loadPaapFile(ActionEvent e) {
        logger.info("Load PAAP file action triggered");

        if (e.getSource() instanceof File) {
            File selectedFile = (File) e.getSource();
            processPaapFile(selectedFile);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select PAAP Excel File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));

        if (lastPaapFile != null && lastPaapFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(lastPaapFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(view.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processPaapFile(selectedFile);
        }
    }

    private void processPaapFile(File selectedFile) {
        lastPaapFile = selectedFile;
        logger.info("Processing PAAP file: {}", selectedFile.getAbsolutePath());

        try {
            view.setStatusMessage("🔄 Loading PAAP file...");
            dataController.loadPaapFile(selectedFile);
            paapLoaded = true;

            List<ProcurementItem> items = dataController.getModel().getProcurementItems();
            view.updateProcurementItemsList(items);

            double totalValue = dataController.getModel().getTotalValueWithoutTVA();
            view.setStatusMessage(String.format("✅ Loaded %d procurement items (%.2f RON total)",
                    items.size(), totalValue));

            logger.info("PAAP file loaded successfully: {} items, total value: {}", items.size(), totalValue);

            view.updateAIAssistant();
            view.updateDashboard();
            view.updateScriptingService();

            if (cpvCodesLoaded) {
                logger.info("Both files loaded, auto-generating enhanced analysis");
                generateAllCharts(null);
                generateAnalysis(null);
            }

        } catch (Exception ex) {
            logger.error("Error loading PAAP file: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error loading PAAP file: " + ex.getMessage());
            view.setStatusMessage("❌ Failed to load PAAP file");
        }
    }

    private void loadCpvCodesFile(ActionEvent e) {
        logger.info("Load CPV codes file action triggered");

        if (e.getSource() instanceof File) {
            File selectedFile = (File) e.getSource();
            processCpvCodesFile(selectedFile);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select CPV Codes Excel File");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Excel Files (*.xlsx, *.xls)", "xlsx", "xls"));

        if (lastCpvFile != null && lastCpvFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(lastCpvFile.getParentFile());
        }

        int result = fileChooser.showOpenDialog(view.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            processCpvCodesFile(selectedFile);
        }
    }

    private void processCpvCodesFile(File selectedFile) {
        lastCpvFile = selectedFile;
        logger.info("Processing CPV codes file: {}", selectedFile.getAbsolutePath());

        try {
            view.setStatusMessage("🔄 Loading CPV codes file...");
            dataController.loadCpvCodesFile(selectedFile);
            cpvCodesLoaded = true;

            int codesCount = dataController.getModel().getCpvCodeMap().size();
            view.setStatusMessage("✅ Loaded " + codesCount + " CPV codes");
            logger.info("CPV codes file loaded successfully: {} codes", codesCount);

            view.updateAIAssistant();
            view.updateDashboard();
            view.updateScriptingService();

            if (paapLoaded) {
                logger.info("Both files loaded, auto-generating enhanced analysis");
                generateAllCharts(null);
                generateAnalysis(null);
            }

        } catch (Exception ex) {
            logger.error("Error loading CPV codes file: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error loading CPV codes file: " + ex.getMessage());
            view.setStatusMessage("❌ Failed to load CPV codes file");
        }
    }

    private void searchProcurementItems(ActionEvent e) {
        logger.info("Search procurement items action triggered");

        if (!paapLoaded) {
            view.showErrorMessage("Please load a PAAP file first");
            return;
        }

        String query = view.getSearchQuery();
        logger.info("Search query: '{}'", query);

        try {
            List<ProcurementItem> results = dataController.searchProcurementItems(query);
            view.updateProcurementItemsList(results);

            double totalValue = results.stream()
                    .mapToDouble(ProcurementItem::getValueWithoutTVA)
                    .sum();

            view.setStatusMessage(String.format("🔍 Found %d items matching '%s' (%.2f RON total)",
                    results.size(), query, totalValue));
            logger.info("Search completed: {} results with total value {}", results.size(), totalValue);

        } catch (Exception ex) {
            logger.error("Error searching procurement items: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error searching procurement items: " + ex.getMessage());
            view.setStatusMessage("❌ Search failed");
        }
    }

    private void generateAllCharts(ActionEvent e) {
        logger.info("Generate all charts action triggered");

        if (!paapLoaded) {
            view.showErrorMessage("Please load a PAAP file first");
            return;
        }

        if (!cpvCodesLoaded) {
            view.showErrorMessage("Please load a CPV codes file first for complete analysis");
            return;
        }

        try {
            view.setStatusMessage("🔄 Generating enhanced charts...");
            view.clearCharts();

            JFreeChart categoryChart = ChartGenerator.generateProcurementByCategory(
                    dataController.getModel(),
                    dataController.getModel().getCpvCodeMap()
            );
            view.displayChart(categoryChart, "Procurement by Category");

            JFreeChart topItemsChart = ChartGenerator.generateTopProcurementItems(
                    dataController.getModel(), 10
            );
            view.displayChart(topItemsChart, "Top 10 Procurement Items");

            JFreeChart valueRangeChart = ChartGenerator.generateProcurementByValueRange(
                    dataController.getModel()
            );
            view.displayChart(valueRangeChart, "Procurement by Value Range");

            view.setStatusMessage("✅ Enhanced charts generated successfully");
            logger.info("All enhanced charts generated successfully");

        } catch (Exception ex) {
            logger.error("Error generating charts: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error generating charts: " + ex.getMessage() +
                    "\n\nPlease ensure your Excel files are in the correct format.");
            view.setStatusMessage("❌ Failed to generate charts");
        }
    }

    private void generateAnalysis(ActionEvent e) {
        logger.info("Generate analysis action triggered");

        if (!paapLoaded) {
            view.showErrorMessage("Please load a PAAP file first");
            return;
        }

        try {
            view.setStatusMessage("🔄 Generating comprehensive analysis...");

            String analysis;
            if (cpvCodesLoaded) {
                analysis = AnalysisService.generateAnalysisReport(
                        dataController.getModel(),
                        dataController.getModel().getCpvCodeMap()
                );
            } else {
                analysis = dataController.getProcurementStatistics() + "\n\n" +
                        "Note: Load CPV codes file for more detailed analysis.";
            }

            view.displayAnalysis(analysis);
            view.setStatusMessage("✅ Comprehensive analysis generated successfully");
            logger.info("Analysis generated successfully");

        } catch (Exception ex) {
            logger.error("Error generating analysis: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error generating analysis: " + ex.getMessage());
            view.setStatusMessage("❌ Failed to generate analysis");
        }
    }

    private void exportAnalysis(ActionEvent e) {
        logger.info("Export analysis action triggered");

        String analysisText = view.getAnalysisText();
        if (analysisText == null || analysisText.trim().isEmpty()) {
            view.showErrorMessage("No analysis available to export. Generate analysis first.");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Export Analysis Report");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Text Files (*.txt)", "txt"));

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        fileChooser.setSelectedFile(new File("CPV_Analysis_Report_" + timestamp + ".txt"));

        if (lastExportFile != null && lastExportFile.getParentFile() != null) {
            fileChooser.setCurrentDirectory(lastExportFile.getParentFile());
        }

        int result = fileChooser.showSaveDialog(view.getFrame());
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();

            if (!selectedFile.getName().toLowerCase().endsWith(".txt")) {
                selectedFile = new File(selectedFile.getAbsolutePath() + ".txt");
            }

            if (selectedFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(
                        view.getFrame(),
                        "File already exists. Overwrite?",
                        "Confirm Overwrite",
                        JOptionPane.YES_NO_OPTION);

                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            lastExportFile = selectedFile;

            try (FileWriter writer = new FileWriter(selectedFile)) {
                writer.write("CPV ANALYSIS REPORT\n");
                writer.write("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n");
                writer.write("Source Files:\n");
                if (lastPaapFile != null) {
                    writer.write("- PAAP: " + lastPaapFile.getName() + "\n");
                }
                if (lastCpvFile != null) {
                    writer.write("- CPV Codes: " + lastCpvFile.getName() + "\n");
                }
                writer.write("\n" + "=".repeat(80) + "\n\n");

                writer.write(analysisText);

                view.setStatusMessage("✅ Analysis exported to " + selectedFile.getName());
                logger.info("Analysis exported to {}", selectedFile.getAbsolutePath());

            } catch (IOException ex) {
                logger.error("Error exporting analysis: {}", ex.getMessage(), ex);
                view.showErrorMessage("Error exporting analysis: " + ex.getMessage());
                view.setStatusMessage("❌ Failed to export analysis");
            }
        }
    }

    private void printAnalysis(ActionEvent e) {
        logger.info("Print analysis action triggered");

        String analysisText = view.getAnalysisText();
        if (analysisText == null || analysisText.trim().isEmpty()) {
            view.showErrorMessage("No analysis available to print. Generate analysis first.");
            return;
        }

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("CPV Analysis Report");

        JTextArea printArea = new JTextArea(analysisText);
        printArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));
        printArea.setEditable(false);

        if (job.printDialog()) {
            try {
                view.setStatusMessage("🔄 Printing analysis...");

                job.setPrintable((graphics, pageFormat, pageIndex) -> {
                    if (pageIndex > 0) {
                        return NO_SUCH_PAGE;
                    }

                    int x = (int) pageFormat.getImageableX();
                    int y = (int) pageFormat.getImageableY();
                    int width = (int) pageFormat.getImageableWidth();
                    int height = (int) pageFormat.getImageableHeight();

                    graphics.translate(x, y);

                    graphics.setFont(new Font("Arial", Font.BOLD, 12));
                    graphics.drawString("CPV ANALYSIS REPORT", 10, 20);
                    graphics.drawString("Generated: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()),
                            width - 180, 20);

                    graphics.drawLine(10, 25, width - 10, 25);

                    printArea.setBounds(0, 30, width, height - 40);
                    printArea.print(graphics);

                    return PAGE_EXISTS;
                });

                job.print();
                view.setStatusMessage("✅ Analysis printed successfully");
                logger.info("Analysis printed successfully");

            } catch (PrinterException ex) {
                logger.error("Error printing analysis: {}", ex.getMessage(), ex);
                view.showErrorMessage("Error printing analysis: " + ex.getMessage());
                view.setStatusMessage("❌ Failed to print analysis");
            }
        }
    }
}