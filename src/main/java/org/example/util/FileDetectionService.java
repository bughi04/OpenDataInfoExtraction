package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class FileDetectionService {
    private static final Logger logger = LoggerFactory.getLogger(FileDetectionService.class);

    // Patterns for detection
    private static final Pattern CPV_CODE_PATTERN = Pattern.compile("\\d{8}-\\d");
    private static final Pattern CPV_CODE_SIMPLE_PATTERN = Pattern.compile("\\d{8}");
    private static final Pattern CPV_HEADER_PATTERN = Pattern.compile("(?i)(cpv|common procurement vocabulary|cod)");
    private static final Pattern PROCUREMENT_HEADER_PATTERN = Pattern.compile(
            "(?i)(procurement|achizitie|achiziție|contract|purchase|paap|plan)");

    public static Map<String, Object> analyzeFile(File file) {
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getName());
        result.put("fileSize", file.length());
        result.put("fileType", getFileType(file));

        String detectedType = "unknown";
        int confidenceScore = 0;

        try {
            if (isExcelFile(file)) {
                Map<String, Object> excelAnalysis = analyzeExcelFile(file);
                result.putAll(excelAnalysis);

                detectedType = (String) excelAnalysis.get("detectedType");
                confidenceScore = (int) excelAnalysis.get("confidenceScore");
            } else {
                result.put("error", "Unsupported file format. Please upload an Excel file (.xls or .xlsx).");
            }
        } catch (Exception e) {
            logger.error("Error analyzing file: {}", e.getMessage(), e);
            result.put("error", "Error analyzing file: " + e.getMessage());
        }

        result.put("detectedType", detectedType);
        result.put("confidenceScore", confidenceScore);
        result.put("recommendedImportType", getRecommendedImportType(detectedType, confidenceScore));

        return result;
    }

    private static String getFileType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".xlsx")) return "Excel (XLSX)";
        if (name.endsWith(".xls")) return "Excel (XLS)";
        if (name.endsWith(".csv")) return "CSV";
        if (name.endsWith(".txt")) return "Text";
        return "Unknown";
    }

    private static boolean isExcelFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".xlsx") || name.endsWith(".xls");
    }

    private static Map<String, Object> analyzeExcelFile(File file) throws IOException {
        Map<String, Object> result = new HashMap<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            int cpvCodeCount = 0;
            int procurementItemCount = 0;
            int sheetCount = workbook.getNumberOfSheets();

            result.put("sheetCount", sheetCount);

            Map<String, Integer> sheetInfo = new HashMap<>();

            for (int i = 0; i < sheetCount; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                String sheetName = sheet.getSheetName();
                int rowCount = sheet.getLastRowNum() + 1;

                sheetInfo.put(sheetName, rowCount);

                Map<String, Integer> sheetIndicators = scanSheetForIndicators(sheet);

                cpvCodeCount += sheetIndicators.getOrDefault("cpvCodeCount", 0);
                procurementItemCount += sheetIndicators.getOrDefault("procurementItemCount", 0);

                result.put("sheet_" + i + "_name", sheetName);
                result.put("sheet_" + i + "_rows", rowCount);
                result.put("sheet_" + i + "_cpvCodes", sheetIndicators.getOrDefault("cpvCodeCount", 0));
                result.put("sheet_" + i + "_procurementItems", sheetIndicators.getOrDefault("procurementItemCount", 0));
            }

            result.put("sheets", sheetInfo);
            result.put("totalCpvCodes", cpvCodeCount);
            result.put("totalProcurementItems", procurementItemCount);

            String detectedType;
            int confidenceScore;

            if (cpvCodeCount > 0 && procurementItemCount > 0) {
                if (cpvCodeCount > procurementItemCount) {
                    detectedType = "CPV code list with some procurement data";
                    confidenceScore = 70;
                } else {
                    detectedType = "Procurement data with CPV codes";
                    confidenceScore = 80;
                }
            } else if (cpvCodeCount > 0) {
                detectedType = "CPV code list";
                confidenceScore = 90;
            } else if (procurementItemCount > 0) {
                detectedType = "Procurement data";
                confidenceScore = 85;
            } else {
                detectedType = "Unknown format";
                confidenceScore = 30;
            }

            result.put("detectedType", detectedType);
            result.put("confidenceScore", confidenceScore);

        }

        return result;
    }

    private static Map<String, Integer> scanSheetForIndicators(Sheet sheet) {
        Map<String, Integer> indicators = new HashMap<>();
        int cpvCodeCount = 0;
        int procurementItemCount = 0;

        boolean hasCpvHeader = false;
        boolean hasProcurementHeader = false;

        int maxRowsToCheck = Math.min(100, sheet.getLastRowNum());

        for (int i = 0; i <= maxRowsToCheck; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            boolean rowHasCpvCode = false;
            boolean rowHasValueColumn = false;

            for (int c = 0; c < row.getLastCellNum(); c++) {
                String cellValue = getCellValueAsString(row.getCell(c)).trim();
                if (cellValue.isEmpty()) continue;

                if (CPV_CODE_PATTERN.matcher(cellValue).find() ||
                        CPV_CODE_SIMPLE_PATTERN.matcher(cellValue).find()) {
                    rowHasCpvCode = true;
                }

                if (i < 10) {
                    if (CPV_HEADER_PATTERN.matcher(cellValue).find()) {
                        hasCpvHeader = true;
                    }

                    if (PROCUREMENT_HEADER_PATTERN.matcher(cellValue).find()) {
                        hasProcurementHeader = true;
                    }
                }

                try {
                    double value = Double.parseDouble(cellValue.replaceAll("[^\\d.]", ""));
                    if (value > 0) {
                        rowHasValueColumn = true;
                    }
                } catch (NumberFormatException e) {
                    // Not a number, ignore
                }
            }

            if (rowHasCpvCode) {
                cpvCodeCount++;
            }

            if (rowHasValueColumn) {
                procurementItemCount++;
            }
        }

        if (hasCpvHeader) {
            cpvCodeCount = Math.max(cpvCodeCount, 1);
        }

        if (hasProcurementHeader) {
            procurementItemCount = Math.max(procurementItemCount, 1);
        }

        indicators.put("cpvCodeCount", cpvCodeCount);
        indicators.put("procurementItemCount", procurementItemCount);
        indicators.put("hasCpvHeader", hasCpvHeader ? 1 : 0);
        indicators.put("hasProcurementHeader", hasProcurementHeader ? 1 : 0);

        return indicators;
    }

    private static String getRecommendedImportType(String detectedType, int confidenceScore) {
        if (confidenceScore < 50) {
            return "unknown";
        }

        switch (detectedType) {
            case "CPV code list":
                return "cpv_codes";
            case "Procurement data":
                return "procurement_data";
            case "CPV code list with some procurement data":
                return "cpv_codes";
            case "Procurement data with CPV codes":
                return "procurement_data";
            default:
                return "unknown";
        }
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
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }
}