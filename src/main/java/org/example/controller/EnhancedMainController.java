package org.example.controller;

import org.example.model.ProcurementItem;
import org.example.util.EnhancedChartGenerator;
import org.example.util.ProcurementAnalysisService;
import org.example.view.ModernMainView;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;

public class EnhancedMainController {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedMainController.class);

    private ModernMainView view;
    private DataController dataController;

    public EnhancedMainController(ModernMainView view, DataController dataController) {
        this.view = view;
        this.dataController = dataController;
        logger.info("Enhanced Main Controller initialized");

        initEventHandlers();
    }

    private void initEventHandlers() {
        view.setLoadPaapAction(this::loadPaapFile);

        view.setLoadCpvCodesAction(this::loadCpvCodesFile);

        view.setSearchAction(this::searchProcurementItems);

        view.setGenerateChartsAction(this::generateEnhancedCharts);

        view.setGenerateAnalysisAction(this::generateEnhancedAnalysis);

        view.setExportAnalysisAction(this::exportAnalysis);

        view.setPrintAnalysisAction(this::printAnalysis);

        logger.info("Event handlers initialized");
    }

    private void loadPaapFile(ActionEvent e) {
        logger.info("Load PAAP file action triggered");
        if (e.getSource() instanceof File) {
            File file = (File) e.getSource();
            loadPaapFileInternal(file);
        } else {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select PAAP File");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Excel Files", "xls", "xlsx"));

            int result = fileChooser.showOpenDialog(view.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                loadPaapFileInternal(selectedFile);
            }
        }
    }

    private void loadPaapFileInternal(File file) {
        try {
            view.setStatusMessage("Loading PAAP file...");
            dataController.loadPaapFile(file);

            List<ProcurementItem> items = dataController.getModel().getProcurementItems();
            view.updateProcurementItemsList(items);

            double totalValue = dataController.getModel().getTotalValueWithoutTVA();
            view.setStatusMessage(String.format("Loaded %d procurement items (%.2f RON total)",
                    items.size(), totalValue));
            logger.info("PAAP file loaded successfully: {} items, total value: {}",
                    items.size(), totalValue);

            view.updateAIAssistant();

            if (cpvCodesLoaded()) {
                logger.info("Both files loaded, auto-generating charts and analysis");
                generateEnhancedCharts(null);
                generateEnhancedAnalysis(null);
            }
        } catch (Exception ex) {
            logger.error("Error loading PAAP file: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error loading PAAP file: " + ex.getMessage());
            view.setStatusMessage("Failed to load PAAP file");
        }
    }

    private void loadCpvCodesFile(ActionEvent e) {
        logger.info("Load CPV codes file action triggered");
        if (e.getSource() instanceof File) {
            File file = (File) e.getSource();
            loadCpvCodesFileInternal(file);
        } else {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Select CPV Codes File");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "Excel Files", "xls", "xlsx"));

            int result = fileChooser.showOpenDialog(view.getFrame());
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                loadCpvCodesFileInternal(selectedFile);
            }
        }
    }

    private void loadCpvCodesFileInternal(File file) {
        try {
            view.setStatusMessage("Loading CPV codes file...");
            dataController.loadCpvCodesFile(file);

            int codesCount = dataController.getModel().getCpvCodeMap().size();
            view.setStatusMessage("Loaded " + codesCount + " CPV codes");
            logger.info("CPV codes file loaded successfully: {} codes", codesCount);

            view.updateAIAssistant();

            if (paapLoaded()) {
                logger.info("Both files loaded, auto-generating charts and analysis");
                generateEnhancedCharts(null);
                generateEnhancedAnalysis(null);
            }
        } catch (Exception ex) {
            logger.error("Error loading CPV codes file: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error loading CPV codes file: " + ex.getMessage());
            view.setStatusMessage("Failed to load CPV codes file");
        }
    }

    private void searchProcurementItems(ActionEvent e) {
        logger.info("Search procurement items action triggered");

        if (!paapLoaded()) {
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

            view.setStatusMessage(String.format("Found %d items matching '%s' (%.2f RON total)",
                    results.size(), query, totalValue));
            logger.info("Search completed: {} results with total value {}", results.size(), totalValue);
        } catch (Exception ex) {
            logger.error("Error searching procurement items: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error searching procurement items: " + ex.getMessage());
            view.setStatusMessage("Search failed");
        }
    }

    private void generateEnhancedCharts(ActionEvent e) {
        logger.info("Generate enhanced charts action triggered");

        if (!paapLoaded()) {
            view.showErrorMessage("Please load a PAAP file first");
            return;
        }

        if (!cpvCodesLoaded()) {
            view.showErrorMessage("Please load a CPV codes file for complete analysis");
            return;
        }

        try {
            view.setStatusMessage("Generating enhanced charts...");
            view.clearCharts();

            JFreeChart categoryPieChart = EnhancedChartGenerator.generateEnhancedCategoryPieChart(
                    dataController.getModel());
            view.displayChart(categoryPieChart, "Procurement by Category");
            logger.info("Category pie chart generated");

            JFreeChart topItemsChart = EnhancedChartGenerator.generateEnhancedTopItemsBarChart(
                    dataController.getModel(), 10);
            view.displayChart(topItemsChart, "Top 10 Procurement Items");
            logger.info("Top items chart generated");

            JFreeChart valueRangeChart = EnhancedChartGenerator.generateEnhancedValueRangeChart(
                    dataController.getModel());
            view.displayChart(valueRangeChart, "Procurement by Value Range");
            logger.info("Value range chart generated");

            JFreeChart categoryRadarChart = EnhancedChartGenerator.generateCategoryRadarChart(
                    dataController.getModel());
            view.displayChart(categoryRadarChart, "Category Analysis");
            logger.info("Category radar chart generated");

            JFreeChart heatmapChart = EnhancedChartGenerator.generateProcurementHeatmapChart(
                    dataController.getModel());
            view.displayChart(heatmapChart, "Procurement Concentration");
            logger.info("Procurement heatmap generated");

            JFreeChart timeChart = EnhancedChartGenerator.generateTimeDimensionChart(
                    dataController.getModel());
            view.displayChart(timeChart, "Quarterly Distribution");
            logger.info("Time dimension chart generated");

            view.setStatusMessage("Enhanced charts generated successfully");
        } catch (Exception ex) {
            logger.error("Error generating enhanced charts: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error generating charts: " + ex.getMessage());
            view.setStatusMessage("Failed to generate charts");
        }
    }

    private void generateEnhancedAnalysis(ActionEvent e) {
        logger.info("Generate enhanced analysis action triggered");

        if (!paapLoaded()) {
            view.showErrorMessage("Please load a PAAP file first");
            return;
        }

        try {
            view.setStatusMessage("Generating comprehensive analysis...");

            String analysis;
            if (cpvCodesLoaded()) {
                analysis = ProcurementAnalysisService.generateComprehensiveAnalysis(
                        dataController.getModel());
            } else {
                analysis = "*** LIMITED ANALYSIS (CPV CODES NOT LOADED) ***\n\n" +
                        dataController.getProcurementStatistics() + "\n\n" +
                        "Note: Load CPV codes file for comprehensive analysis.";
            }

            view.displayAnalysis(analysis);

            view.setStatusMessage("Comprehensive analysis generated successfully");
            logger.info("Enhanced analysis generated successfully");
        } catch (Exception ex) {
            logger.error("Error generating enhanced analysis: {}", ex.getMessage(), ex);
            view.showErrorMessage("Error generating analysis: " + ex.getMessage());
            view.setStatusMessage("Failed to generate analysis");
        }
    }

    private void exportAnalysis(ActionEvent e) {
    }

    private void printAnalysis(ActionEvent e) {
    }

    private boolean paapLoaded() {
        return dataController.getModel() != null &&
                !dataController.getModel().getProcurementItems().isEmpty();
    }

    private boolean cpvCodesLoaded() {
        return dataController.getModel() != null &&
                !dataController.getModel().getCpvCodeMap().isEmpty();
    }
}