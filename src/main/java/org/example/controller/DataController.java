package org.example.controller;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.example.util.ExcelReader;
import org.example.util.FileCompatibilityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.io.File;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DataController {
    private static final Logger logger = LoggerFactory.getLogger(DataController.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("ro", "RO"));

    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
        CURRENCY_FORMAT.setCurrency(java.util.Currency.getInstance("RON"));
    }

    private DataModel model;
    private File paapFile;
    private File listaFile;

    public DataController() {
        model = new DataModel();
        logger.info("DataController initialized");
    }

    public DataModel getModel() {
        return model;
    }

    public void loadPaapFile(File file) throws Exception {
        this.paapFile = file;
        if (file == null || !file.exists()) {
            logger.error("PAAP file does not exist");
            throw new IllegalArgumentException("PAAP file does not exist");
        }

        logger.info("Loading PAAP file with enhanced detection: {}", file.getAbsolutePath());
        try {
            Map<String, Object> compatibilityReport = FileCompatibilityService.generateProcurementDataCompatibilityReport(file);
            String compatibilityLevel = (String) compatibilityReport.get("compatibilityLevel");

            if ("Incompatible".equals(compatibilityLevel) || "Error".equals(compatibilityLevel)) {
                List<String> issues = (List<String>) compatibilityReport.get("issues");
                List<String> recommendations = (List<String>) compatibilityReport.get("recommendations");

                StringBuilder errorMessage = new StringBuilder("File is not compatible as a procurement data file:\n");
                for (String issue : issues) {
                    errorMessage.append("- ").append(issue).append("\n");
                }

                errorMessage.append("\nRecommendations:\n");
                for (String recommendation : recommendations) {
                    errorMessage.append("- ").append(recommendation).append("\n");
                }

                throw new Exception(errorMessage.toString());
            }

            List<ProcurementItem> items = ExcelReader.readProcurementItems(file);
            model.setProcurementItems(items);
            logger.info("Successfully loaded {} procurement items", items.size());

            if ("Partially Compatible".equals(compatibilityLevel) || "Mostly Compatible".equals(compatibilityLevel)) {
                List<String> issues = (List<String>) compatibilityReport.get("issues");
                for (String issue : issues) {
                    logger.warn("Compatibility issue: {}", issue);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading PAAP file: {}", e.getMessage(), e);
            throw new Exception("Failed to load PAAP file: " + e.getMessage(), e);
        }
    }

    public void loadCpvCodesFile(File file) throws Exception {
        this.listaFile = file;
        if (file == null || !file.exists()) {
            logger.error("CPV codes file does not exist");
            throw new IllegalArgumentException("CPV codes file does not exist");
        }

        logger.info("Loading CPV codes file with enhanced detection: {}", file.getAbsolutePath());
        try {
            Map<String, Object> compatibilityReport = FileCompatibilityService.generateCpvCodeCompatibilityReport(file);
            String compatibilityLevel = (String) compatibilityReport.get("compatibilityLevel");

            if ("Incompatible".equals(compatibilityLevel) || "Error".equals(compatibilityLevel)) {
                List<String> issues = (List<String>) compatibilityReport.get("issues");
                List<String> recommendations = (List<String>) compatibilityReport.get("recommendations");

                StringBuilder errorMessage = new StringBuilder("File is not compatible as a CPV codes file:\n");
                for (String issue : issues) {
                    errorMessage.append("- ").append(issue).append("\n");
                }

                errorMessage.append("\nRecommendations:\n");
                for (String recommendation : recommendations) {
                    errorMessage.append("- ").append(recommendation).append("\n");
                }

                throw new Exception(errorMessage.toString());
            }

            Map<String, CpvCode> cpvCodes = ExcelReader.readCpvCodes(file);
            model.setCpvCodes(cpvCodes);
            logger.info("Successfully loaded {} CPV codes", cpvCodes.size());

            if ("Partially Compatible".equals(compatibilityLevel) || "Mostly Compatible".equals(compatibilityLevel)) {
                List<String> issues = (List<String>) compatibilityReport.get("issues");
                for (String issue : issues) {
                    logger.warn("Compatibility issue: {}", issue);
                }
            }
        } catch (Exception e) {
            logger.error("Error loading CPV codes file: {}", e.getMessage(), e);
            throw new Exception("Failed to load CPV codes file: " + e.getMessage(), e);
        }
    }

    public List<ProcurementItem> searchProcurementItems(String query) {
        return model.searchProcurementItems(query);
    }

    public String getCpvCodeName(String code, boolean romanian) {
        CpvCode cpvCode = model.getCpvCodeByCode(code);
        if (cpvCode == null) {
            return code;
        }

        return romanian ? cpvCode.getRomanianName() : cpvCode.getEnglishName();
    }

    public String getProcurementStatistics() {
        StringBuilder sb = new StringBuilder();

        try {
            int totalItems = model.getProcurementItems().size();
            sb.append("Total procurement items: ").append(totalItems).append("\n");

            double totalWithoutTVA = model.getTotalValueWithoutTVA();
            double totalWithTVA = model.getTotalValueWithTVA();

            sb.append("Total value (without TVA): ").append(String.format("%,.2f", totalWithoutTVA)).append(" RON\n");
            sb.append("Total value (with TVA): ").append(String.format("%,.2f", totalWithTVA)).append(" RON\n");

            Map<String, List<ProcurementItem>> itemsByCategory = model.getProcurementItemsByCategory();
            sb.append("Number of CPV categories: ").append(itemsByCategory.size()).append("\n");

            double avgValue = totalItems > 0 ? totalWithoutTVA / totalItems : 0;
            sb.append("Average value per item: ").append(String.format("%,.2f", avgValue)).append(" RON\n");

            return sb.toString();
        } catch (Exception e) {
            logger.error("Error generating procurement statistics: {}", e.getMessage(), e);
            return "Error generating statistics: " + e.getMessage();
        }
    }

    public String getTopCategoriesReport(int n) {
        StringBuilder sb = new StringBuilder();
        sb.append("Top ").append(n).append(" CPV Categories by Value:\n\n");

        try {
            Map<String, Double> valueByCategory = model.getValueByCpvCategory();
            double totalValue = model.getTotalValueWithoutTVA();

            if (valueByCategory.isEmpty()) {
                sb.append("No categories found or no items with valid values.");
                return sb.toString();
            }

            valueByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .limit(n)
                    .forEach(entry -> {
                        String category = entry.getKey();
                        double value = entry.getValue();
                        double percentage = totalValue > 0 ? value * 100 / totalValue : 0;

                        sb.append(category).append(": ")
                                .append(String.format("%,.2f", value)).append(" RON (")
                                .append(String.format("%.2f", percentage)).append("%)\n");
                    });

            return sb.toString();
        } catch (Exception e) {
            logger.error("Error generating top categories report: {}", e.getMessage(), e);
            return "Error generating top categories report: " + e.getMessage();
        }
    }
}