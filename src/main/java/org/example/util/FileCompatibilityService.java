package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class FileCompatibilityService {
    private static final Logger logger = LoggerFactory.getLogger(FileCompatibilityService.class);

    private static final Pattern CPV_CODE_PATTERN = Pattern.compile("\\d{8}-\\d");
    private static final Pattern CPV_CODE_SIMPLE_PATTERN = Pattern.compile("\\d{8}");

    public static Map<String, Object> generateCpvCodeCompatibilityReport(File file) {
        Map<String, Object> report = new HashMap<>();
        report.put("fileName", file.getName());
        report.put("fileType", file.getName().endsWith(".xlsx") ? "Excel (XLSX)" : "Excel (XLS)");

        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            int totalCpvCodes = 0;
            int validCpvCodes = 0;
            int sheetsWithCpvCodes = 0;
            Map<String, Integer> sheetCpvCounts = new HashMap<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                Map<String, Integer> sheetAnalysis = analyzeCpvCodeSheet(sheet);
                int sheetTotal = sheetAnalysis.getOrDefault("totalCodes", 0);
                int sheetValid = sheetAnalysis.getOrDefault("validCodes", 0);

                totalCpvCodes += sheetTotal;
                validCpvCodes += sheetValid;

                if (sheetTotal > 0) {
                    sheetsWithCpvCodes++;
                    sheetCpvCounts.put(sheetName, sheetTotal);
                }
            }

            double validPercentage = totalCpvCodes > 0 ? (validCpvCodes * 100.0 / totalCpvCodes) : 0;

            report.put("totalCpvCodes", totalCpvCodes);
            report.put("validCpvCodes", validCpvCodes);
            report.put("validPercentage", validPercentage);
            report.put("sheetsWithCpvCodes", sheetsWithCpvCodes);
            report.put("sheetCpvCounts", sheetCpvCounts);

            String compatibilityLevel;
            if (validCpvCodes == 0) {
                compatibilityLevel = "Incompatible";
                issues.add("No valid CPV codes found in the file.");
                recommendations.add("Please ensure the file contains CPV codes in the format 12345678-9 or 12345678.");
            } else if (validPercentage < 50) {
                compatibilityLevel = "Partially Compatible";
                issues.add("Only " + validPercentage + "% of codes appear to be valid CPV codes.");
                recommendations.add("Check the file format to ensure CPV codes are in the expected format.");
            } else if (validPercentage < 90) {
                compatibilityLevel = "Mostly Compatible";
                issues.add("Some entries (" + (100 - validPercentage) + "%) do not appear to be valid CPV codes.");
                recommendations.add("The file will work but some entries may be skipped.");
            } else {
                compatibilityLevel = "Fully Compatible";
                recommendations.add("The file appears to be a valid CPV codes file.");
            }

            report.put("compatibilityLevel", compatibilityLevel);
            report.put("issues", issues);
            report.put("recommendations", recommendations);

        } catch (Exception e) {
            logger.error("Error generating CPV code compatibility report: {}", e.getMessage(), e);
            issues.add("Error analyzing file: " + e.getMessage());
            recommendations.add("Please ensure the file is a valid Excel file.");

            report.put("compatibilityLevel", "Error");
            report.put("issues", issues);
            report.put("recommendations", recommendations);
        }

        return report;
    }

    public static Map<String, Object> generateProcurementDataCompatibilityReport(File file) {
        Map<String, Object> report = new HashMap<>();
        report.put("fileName", file.getName());
        report.put("fileType", file.getName().endsWith(".xlsx") ? "Excel (XLSX)" : "Excel (XLS)");

        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            int sheetsWithHeaders = 0;
            int sheetsWithData = 0;
            int totalProcurementItems = 0;
            boolean hasNameColumn = false;
            boolean hasValueColumn = false;
            boolean hasCpvColumn = false;

            Map<String, Integer> sheetItemCounts = new HashMap<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();

                Map<String, Object> sheetAnalysis = analyzeProcurementDataSheet(sheet);
                boolean sheetHasHeaders = (boolean) sheetAnalysis.getOrDefault("hasHeaders", false);
                boolean sheetHasData = (boolean) sheetAnalysis.getOrDefault("hasData", false);
                int sheetItems = (int) sheetAnalysis.getOrDefault("itemCount", 0);

                if (sheetHasHeaders) {
                    sheetsWithHeaders++;

                    if ((boolean) sheetAnalysis.getOrDefault("hasNameColumn", false)) {
                        hasNameColumn = true;
                    }

                    if ((boolean) sheetAnalysis.getOrDefault("hasValueColumn", false)) {
                        hasValueColumn = true;
                    }

                    if ((boolean) sheetAnalysis.getOrDefault("hasCpvColumn", false)) {
                        hasCpvColumn = true;
                    }
                }

                if (sheetHasData) {
                    sheetsWithData++;
                    totalProcurementItems += sheetItems;
                    sheetItemCounts.put(sheetName, sheetItems);
                }
            }

            report.put("sheetsWithHeaders", sheetsWithHeaders);
            report.put("sheetsWithData", sheetsWithData);
            report.put("totalProcurementItems", totalProcurementItems);
            report.put("hasNameColumn", hasNameColumn);
            report.put("hasValueColumn", hasValueColumn);
            report.put("hasCpvColumn", hasCpvColumn);
            report.put("sheetItemCounts", sheetItemCounts);

            String compatibilityLevel;

            if (sheetsWithData == 0 || totalProcurementItems == 0) {
                compatibilityLevel = "Incompatible";
                issues.add("No procurement data found in the file.");
                recommendations.add("Please ensure the file contains procurement items with at least names and values.");
            } else if (!hasNameColumn || !hasValueColumn) {
                compatibilityLevel = "Partially Compatible";

                if (!hasNameColumn) {
                    issues.add("No column containing item names could be identified.");
                }

                if (!hasValueColumn) {
                    issues.add("No column containing item values could be identified.");
                }

                recommendations.add("Ensure the file has columns for item names and values.");
            } else if (!hasCpvColumn) {
                compatibilityLevel = "Mostly Compatible";
                issues.add("No column containing CPV codes could be identified.");
                recommendations.add("The file will work but CPV analysis will be limited without CPV codes.");
            } else {
                compatibilityLevel = "Fully Compatible";
                recommendations.add("The file appears to be a valid procurement data file.");
            }

            report.put("compatibilityLevel", compatibilityLevel);
            report.put("issues", issues);
            report.put("recommendations", recommendations);

        } catch (Exception e) {
            logger.error("Error generating procurement data compatibility report: {}", e.getMessage(), e);
            issues.add("Error analyzing file: " + e.getMessage());
            recommendations.add("Please ensure the file is a valid Excel file.");

            report.put("compatibilityLevel", "Error");
            report.put("issues", issues);
            report.put("recommendations", recommendations);
        }

        return report;
    }

    private static Map<String, Integer> analyzeCpvCodeSheet(Sheet sheet) {
        Map<String, Integer> analysis = new HashMap<>();
        int totalCodes = 0;
        int validCodes = 0;

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                String cellValue = getCellValueAsString(row.getCell(j)).trim();
                if (cellValue.isEmpty()) continue;

                if (cellValue.matches(".*\\d{8}.*")) {
                    totalCodes++;

                    if (CPV_CODE_PATTERN.matcher(cellValue).find() ||
                            CPV_CODE_SIMPLE_PATTERN.matcher(cellValue).matches()) {
                        validCodes++;
                    }
                }
            }
        }

        analysis.put("totalCodes", totalCodes);
        analysis.put("validCodes", validCodes);

        return analysis;
    }

    private static Map<String, Object> analyzeProcurementDataSheet(Sheet sheet) {
        Map<String, Object> analysis = new HashMap<>();
        boolean hasHeaders = false;
        boolean hasData = false;
        boolean hasNameColumn = false;
        boolean hasValueColumn = false;
        boolean hasCpvColumn = false;
        int itemCount = 0;

        int headerRow = findHeaderRow(sheet);
        if (headerRow >= 0) {
            hasHeaders = true;

            Row row = sheet.getRow(headerRow);
            for (int j = 0; j < row.getLastCellNum(); j++) {
                String cellValue = getCellValueAsString(row.getCell(j)).trim().toLowerCase();
                if (cellValue.isEmpty()) continue;

                if (cellValue.contains("name") || cellValue.contains("object") ||
                        cellValue.contains("item") || cellValue.contains("denumire") ||
                        cellValue.contains("obiect")) {
                    hasNameColumn = true;
                }

                if (cellValue.contains("value") || cellValue.contains("cost") ||
                        cellValue.contains("price") || cellValue.contains("valoare") ||
                        cellValue.contains("pret") || cellValue.contains("suma")) {
                    hasValueColumn = true;
                }

                if (cellValue.contains("cpv") || cellValue.contains("code") ||
                        cellValue.contains("cod")) {
                    hasCpvColumn = true;
                }
            }

            for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
                Row dataRow = sheet.getRow(i);
                if (dataRow == null) continue;

                boolean rowHasData = false;
                for (int j = 0; j < dataRow.getLastCellNum(); j++) {
                    if (!getCellValueAsString(dataRow.getCell(j)).trim().isEmpty()) {
                        rowHasData = true;
                        break;
                    }
                }

                if (rowHasData) {
                    itemCount++;
                    hasData = true;
                }
            }
        } else {
            int numericCellCount = 0;
            int longTextCellCount = 0;
            int cpvCodeCellCount = 0;

            for (int i = 0; i <= Math.min(20, sheet.getLastRowNum()); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                boolean rowHasNumeric = false;
                boolean rowHasLongText = false;
                boolean rowHasCpvCode = false;

                for (int j = 0; j < row.getLastCellNum(); j++) {
                    String cellValue = getCellValueAsString(row.getCell(j)).trim();
                    if (cellValue.isEmpty()) continue;

                    try {
                        Double.parseDouble(cellValue.replaceAll("[^\\d.]", ""));
                        rowHasNumeric = true;
                    } catch (NumberFormatException e) {
                        // Not a number
                    }

                    if (cellValue.length() > 15) {
                        rowHasLongText = true;
                    }

                    if (CPV_CODE_PATTERN.matcher(cellValue).find() ||
                            CPV_CODE_SIMPLE_PATTERN.matcher(cellValue).matches()) {
                        rowHasCpvCode = true;
                    }
                }

                if (rowHasNumeric) numericCellCount++;
                if (rowHasLongText) longTextCellCount++;
                if (rowHasCpvCode) cpvCodeCellCount++;

                if (rowHasNumeric && rowHasLongText) {
                    itemCount++;
                    hasData = true;
                }
            }

            hasValueColumn = numericCellCount >= 3;
            hasNameColumn = longTextCellCount >= 3;
            hasCpvColumn = cpvCodeCellCount >= 3;
        }

        analysis.put("hasHeaders", hasHeaders);
        analysis.put("hasData", hasData);
        analysis.put("hasNameColumn", hasNameColumn);
        analysis.put("hasValueColumn", hasValueColumn);
        analysis.put("hasCpvColumn", hasCpvColumn);
        analysis.put("itemCount", itemCount);

        return analysis;
    }

    private static int findHeaderRow(Sheet sheet) {
        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            int headerIndicatorCount = 0;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                String cellValue = getCellValueAsString(row.getCell(j)).trim().toLowerCase();
                if (cellValue.isEmpty()) continue;

                if (cellValue.contains("name") || cellValue.contains("value") ||
                        cellValue.contains("code") || cellValue.contains("cpv") ||
                        cellValue.contains("item") || cellValue.contains("object") ||
                        cellValue.contains("price") || cellValue.contains("cost") ||
                        cellValue.contains("denumire") || cellValue.contains("valoare") ||
                        cellValue.contains("cod") || cellValue.contains("obiect")) {
                    headerIndicatorCount++;
                }
            }

            if (headerIndicatorCount >= 2) {
                return i;
            }
        }

        return -1;
    }

    private static String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        try {
            switch (cell.getCellType()) {
                case STRING:
                    return cell.getStringCellValue();
                case NUMERIC:
                    if (DateUtil.isCellDateFormatted(cell)) {
                        return cell.getDateCellValue().toString();
                    } else {
                        double value = cell.getNumericCellValue();
                        if (value == Math.floor(value) && !Double.isInfinite(value)) {
                            return String.format("%.0f", value);
                        } else {
                            return String.valueOf(value);
                        }
                    }
                case BOOLEAN:
                    return String.valueOf(cell.getBooleanCellValue());
                case FORMULA:
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);

                        switch (cellValue.getCellType()) {
                            case NUMERIC:
                                return String.valueOf(cellValue.getNumberValue());
                            case STRING:
                                return cellValue.getStringValue();
                            case BOOLEAN:
                                return String.valueOf(cellValue.getBooleanValue());
                            default:
                                return "";
                        }
                    } catch (Exception e) {
                        return "";
                    }
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}