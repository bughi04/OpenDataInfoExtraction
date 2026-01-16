package org.example.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class FileAnalyzerService {
    private static final Logger logger = LoggerFactory.getLogger(FileAnalyzerService.class);

    public static final char COMMA_DELIMITER = ',';
    public static final char SEMICOLON_DELIMITER = ';';
    public static final char TAB_DELIMITER = '\t';

    public static Map<String, Object> analyzeFile(File file) {
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getName());
        result.put("fileSize", file.length());

        String fileName = file.getName().toLowerCase();

        try {
            if (fileName.endsWith(".xlsx") || fileName.endsWith(".xls")) {
                Map<String, Object> excelAnalysis = analyzeExcelFile(file);
                result.putAll(excelAnalysis);
                result.put("fileType", fileName.endsWith(".xlsx") ? "Excel (XLSX)" : "Excel (XLS)");
            } else if (fileName.endsWith(".csv") || fileName.endsWith(".txt")) {
                char delimiter = detectDelimiter(file);
                result.put("delimiter", String.valueOf(delimiter));

                Map<String, Object> csvAnalysis = analyzeCsvFile(file, delimiter);
                result.putAll(csvAnalysis);
                result.put("fileType", "CSV");
            } else {
                throw new IllegalArgumentException("Unsupported file format. Please upload an Excel or CSV file.");
            }

        } catch (Exception e) {
            logger.error("Error analyzing file: {}", e.getMessage(), e);
            result.put("error", "Error analyzing file: " + e.getMessage());
        }

        return result;
    }

    private static Map<String, Object> analyzeExcelFile(File file) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> headers = new ArrayList<>();
        List<List<String>> allData = new ArrayList<>();
        List<List<String>> sampleData = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            result.put("sheetName", sheet.getSheetName());
            result.put("sheetCount", workbook.getNumberOfSheets());

            Row headerRow = sheet.getRow(0);
            if (headerRow != null) {
                for (int i = 0; i < headerRow.getLastCellNum(); i++) {
                    Cell cell = headerRow.getCell(i);
                    headers.add(cell != null ? getCellValueAsString(cell) : "Column " + (i + 1));
                }
            }

            if (headers.isEmpty()) {
                int maxColumns = 0;
                for (int i = 0; i <= Math.min(5, sheet.getLastRowNum()); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        maxColumns = Math.max(maxColumns, row.getLastCellNum());
                    }
                }

                for (int i = 0; i < maxColumns; i++) {
                    headers.add("Column " + (i + 1));
                }
            }

            int rowCount = 0;
            int sampleSize = 50;

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                List<String> rowData = new ArrayList<>();
                boolean hasData = false;

                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String value = cell != null ? getCellValueAsString(cell) : "";
                    rowData.add(value);
                    if (!value.trim().isEmpty()) {
                        hasData = true;
                    }
                }

                if (hasData) {
                    allData.add(rowData);
                    rowCount++;

                    if (rowCount <= sampleSize) {
                        sampleData.add(rowData);
                    }
                }
            }

            result.put("headers", headers);
            result.put("allData", allData);
            result.put("sampleData", sampleData);
            result.put("rowCount", rowCount);
            result.put("columnCount", headers.size());

            Map<String, Map<String, Object>> columnStats = analyzeColumns(headers, allData);
            result.put("columnStats", columnStats);
        }

        return result;
    }

    private static char detectDelimiter(File file) throws IOException {
        int linesToCheck = 5;
        int[] delimiterCounts = new int[3];
        char[] delimiters = {COMMA_DELIMITER, SEMICOLON_DELIMITER, TAB_DELIMITER};

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int lineCount = 0;

            while ((line = reader.readLine()) != null && lineCount < linesToCheck) {
                if (line.trim().isEmpty()) continue;

                delimiterCounts[0] += countOccurrences(line, COMMA_DELIMITER);
                delimiterCounts[1] += countOccurrences(line, SEMICOLON_DELIMITER);
                delimiterCounts[2] += countOccurrences(line, TAB_DELIMITER);

                lineCount++;
            }
        }

        int maxCount = -1;
        char detectedDelimiter = COMMA_DELIMITER;

        for (int i = 0; i < delimiterCounts.length; i++) {
            if (delimiterCounts[i] > maxCount) {
                maxCount = delimiterCounts[i];
                detectedDelimiter = delimiters[i];
            }
        }

        return detectedDelimiter;
    }

    private static Map<String, Object> analyzeCsvFile(File file, char delimiter) throws IOException {
        Map<String, Object> result = new HashMap<>();
        List<String> headers = new ArrayList<>();
        List<List<String>> allData = new ArrayList<>();
        List<List<String>> sampleData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {

            String line;
            int rowCount = 0;
            int sampleSize = 50;

            if ((line = reader.readLine()) != null) {
                headers = parseCsvLine(line, delimiter);
            }

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                List<String> values = parseCsvLine(line, delimiter);

                while (values.size() < headers.size()) {
                    values.add("");
                }

                if (values.size() > headers.size()) {
                    values = values.subList(0, headers.size());
                }

                allData.add(values);
                rowCount++;

                if (rowCount <= sampleSize) {
                    sampleData.add(values);
                }
            }

            result.put("headers", headers);
            result.put("allData", allData);
            result.put("sampleData", sampleData);
            result.put("rowCount", rowCount);
            result.put("columnCount", headers.size());

            Map<String, Map<String, Object>> columnStats = analyzeColumns(headers, allData);
            result.put("columnStats", columnStats);
        }

        return result;
    }

    private static Map<String, Map<String, Object>> analyzeColumns(List<String> headers, List<List<String>> data) {
        Map<String, Map<String, Object>> columnStats = new LinkedHashMap<>();

        for (int colIndex = 0; colIndex < headers.size(); colIndex++) {
            String header = headers.get(colIndex);
            Map<String, Object> stats = new HashMap<>();

            List<String> columnValues = new ArrayList<>();
            for (List<String> row : data) {
                if (colIndex < row.size()) {
                    columnValues.add(row.get(colIndex));
                } else {
                    columnValues.add("");
                }
            }

            int nonEmptyCount = 0;
            Set<String> uniqueValues = new HashSet<>();
            boolean canBeNumeric = true;
            boolean canBeDate = true;

            List<Double> numericValues = new ArrayList<>();

            for (String value : columnValues) {
                if (value != null && !value.trim().isEmpty()) {
                    nonEmptyCount++;
                    uniqueValues.add(value);

                    try {
                        double numVal = Double.parseDouble(value.replace(",", ".").trim());
                        numericValues.add(numVal);
                    } catch (NumberFormatException e) {
                        canBeNumeric = false;
                    }

                    if (!value.matches(".*\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}.*") &&
                            !value.matches(".*\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}.*")) {
                        canBeDate = false;
                    }
                }
            }

            String type;
            if (canBeNumeric) {
                type = "Numeric";

                if (!numericValues.isEmpty()) {
                    double min = Collections.min(numericValues);
                    double max = Collections.max(numericValues);
                    double sum = numericValues.stream().mapToDouble(Double::doubleValue).sum();
                    double avg = sum / numericValues.size();

                    stats.put("minValue", min);
                    stats.put("maxValue", max);
                    stats.put("avgValue", avg);
                    stats.put("sum", sum);
                }
            } else if (canBeDate) {
                type = "Date";
            } else {
                type = "Text";
            }

            stats.put("type", type);
            stats.put("nonEmptyCount", nonEmptyCount);
            stats.put("uniqueValues", uniqueValues.size());

            columnStats.put(header, stats);
        }

        return columnStats;
    }

    private static List<String> parseCsvLine(String line, char delimiter) {
        List<String> values = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append(c);
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == delimiter && !inQuotes) {
                values.add(sb.toString());
                sb = new StringBuilder();
            } else {
                sb.append(c);
            }
        }

        values.add(sb.toString());

        return values;
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
                            return String.format("%.2f", value);
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
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    return new Date((long)cellValue.getNumberValue()).toString();
                                } else {
                                    double value = cellValue.getNumberValue();
                                    if (value == Math.floor(value) && !Double.isInfinite(value)) {
                                        return String.format("%.0f", value);
                                    } else {
                                        return String.format("%.2f", value);
                                    }
                                }
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
            logger.error("Error getting cell value as string: {}", e.getMessage());
            return "";
        }
    }

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }

    public static org.jfree.chart.JFreeChart createValueDistributionChart(Map<String, Object> analysisData) {
        Map<String, Map<String, Object>> columnStats =
                (Map<String, Map<String, Object>>) analysisData.get("columnStats");

        if (columnStats == null) {
            logger.error("No column stats found in analysis data");
            return createEmptyChart("No suitable data for visualization");
        }

        String selectedColumn = null;

        for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            if ("Numeric".equals(stats.get("type")) && stats.containsKey("minValue")) {
                selectedColumn = entry.getKey();
                break;
            }
        }

        if (selectedColumn == null) {
            return createCountByColumnChart(analysisData);
        }

        List<List<String>> allData = (List<List<String>>) analysisData.get("allData");
        List<String> headers = (List<String>) analysisData.get("headers");

        if (allData == null || headers == null) {
            logger.error("Missing data or headers in analysis data");
            return createEmptyChart("Cannot create chart - missing data");
        }

        int columnIndex = headers.indexOf(selectedColumn);

        if (columnIndex < 0) return createEmptyChart("Column not found");

        List<Double> values = new ArrayList<>();
        for (List<String> row : allData) {
            if (columnIndex < row.size()) {
                String valueStr = row.get(columnIndex);
                try {
                    double value = Double.parseDouble(valueStr.replace(",", "."));
                    values.add(value);
                } catch (NumberFormatException e) {
                    // Skip non-numeric values
                }
            }
        }

        if (values.isEmpty()) {
            return createCountByColumnChart(analysisData);
        }

        org.jfree.data.statistics.HistogramDataset dataset = new org.jfree.data.statistics.HistogramDataset();

        double[] valuesArray = new double[values.size()];
        for (int i = 0; i < values.size(); i++) {
            valuesArray[i] = values.get(i);
        }

        int binCount = Math.min(20, values.size() / 5);
        if (binCount < 5) binCount = 5;

        dataset.addSeries(selectedColumn, valuesArray, binCount);

        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createHistogram(
                "Value Distribution: " + selectedColumn,
                selectedColumn,
                "Frequency",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(java.awt.Color.WHITE);
        org.jfree.chart.plot.XYPlot plot = (org.jfree.chart.plot.XYPlot) chart.getPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        return chart;
    }

    public static org.jfree.chart.JFreeChart createCountByColumnChart(Map<String, Object> analysisData) {
        List<String> headers = (List<String>) analysisData.get("headers");
        Map<String, Map<String, Object>> columnStats =
                (Map<String, Map<String, Object>>) analysisData.get("columnStats");

        if (headers == null || columnStats == null) {
            logger.error("Missing headers or column stats in analysis data");
            return createEmptyChart("Cannot create chart - missing data");
        }

        String selectedColumn = null;
        int bestUniqueCount = Integer.MAX_VALUE;

        for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            Integer uniqueValues = (Integer) stats.getOrDefault("uniqueValues", 0);

            if (uniqueValues >= 3 && uniqueValues <= 15 && uniqueValues < bestUniqueCount) {
                selectedColumn = entry.getKey();
                bestUniqueCount = uniqueValues;
            }
        }

        if (selectedColumn == null && !headers.isEmpty()) {
            selectedColumn = headers.get(0);
        }

        if (selectedColumn == null) {
            return createEmptyChart("No Suitable Data for Visualization");
        }

        List<List<String>> allData = (List<List<String>>) analysisData.get("allData");

        if (allData == null) {
            logger.error("Missing data in analysis data");
            return createEmptyChart("Cannot create chart - missing data");
        }

        int columnIndex = headers.indexOf(selectedColumn);

        Map<String, Integer> valueCounts = new HashMap<>();

        for (List<String> row : allData) {
            if (columnIndex < row.size()) {
                String value = row.get(columnIndex);
                if (value == null || value.trim().isEmpty()) {
                    value = "(Empty)";
                }
                valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
            }
        }

        org.jfree.data.category.DefaultCategoryDataset dataset =
                new org.jfree.data.category.DefaultCategoryDataset();

        List<Map.Entry<String, Integer>> sortedCounts = new ArrayList<>(valueCounts.entrySet());
        sortedCounts.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int maxValues = Math.min(15, sortedCounts.size());
        for (int i = 0; i < maxValues; i++) {
            Map.Entry<String, Integer> entry = sortedCounts.get(i);
            dataset.addValue(entry.getValue(), "Count", entry.getKey());
        }

        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
                "Value Distribution: " + selectedColumn,
                selectedColumn,
                "Count",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        chart.setBackgroundPaint(java.awt.Color.WHITE);
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        org.jfree.chart.axis.CategoryAxis domainAxis = plot.getDomainAxis();
        if (maxValues > 5) {
            domainAxis.setCategoryLabelPositions(
                    org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0)
            );
        }

        return chart;
    }

    public static org.jfree.chart.JFreeChart createPieChart(Map<String, Object> analysisData) {
        List<String> headers = (List<String>) analysisData.get("headers");
        Map<String, Map<String, Object>> columnStats =
                (Map<String, Map<String, Object>>) analysisData.get("columnStats");

        if (headers == null || columnStats == null) {
            logger.error("Missing headers or column stats in analysis data");
            return createEmptyChart("Cannot create pie chart - missing data");
        }

        String selectedColumn = null;
        int bestUniqueCount = Integer.MAX_VALUE;

        for (Map.Entry<String, Map<String, Object>> entry : columnStats.entrySet()) {
            Map<String, Object> stats = entry.getValue();
            Integer uniqueValues = (Integer) stats.getOrDefault("uniqueValues", 0);

            if (uniqueValues >= 3 && uniqueValues <= 10 && uniqueValues < bestUniqueCount) {
                selectedColumn = entry.getKey();
                bestUniqueCount = uniqueValues;
            }
        }

        if (selectedColumn == null && !headers.isEmpty()) {
            selectedColumn = headers.get(0);
        }

        if (selectedColumn == null) {
            return createEmptyChart("No Suitable Data for Visualization");
        }

        List<List<String>> allData = (List<List<String>>) analysisData.get("allData");

        if (allData == null) {
            logger.error("Missing data in analysis data");
            return createEmptyChart("Cannot create pie chart - missing data");
        }

        int columnIndex = headers.indexOf(selectedColumn);

        Map<String, Integer> valueCounts = new HashMap<>();

        for (List<String> row : allData) {
            if (columnIndex < row.size()) {
                String value = row.get(columnIndex);
                if (value == null || value.trim().isEmpty()) {
                    value = "(Empty)";
                }
                valueCounts.put(value, valueCounts.getOrDefault(value, 0) + 1);
            }
        }

        org.jfree.data.general.DefaultPieDataset dataset =
                new org.jfree.data.general.DefaultPieDataset();

        List<Map.Entry<String, Integer>> sortedCounts = new ArrayList<>(valueCounts.entrySet());
        sortedCounts.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        int maxValues = Math.min(10, sortedCounts.size());
        int otherCount = 0;

        for (int i = 0; i < sortedCounts.size(); i++) {
            Map.Entry<String, Integer> entry = sortedCounts.get(i);
            if (i < maxValues) {
                dataset.setValue(entry.getKey(), entry.getValue());
            } else {
                otherCount += entry.getValue();
            }
        }

        if (otherCount > 0) {
            dataset.setValue("Other", otherCount);
        }

        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createPieChart(
                "Distribution by " + selectedColumn,
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(java.awt.Color.WHITE);
        org.jfree.chart.plot.PiePlot plot = (org.jfree.chart.plot.PiePlot) chart.getPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        return chart;
    }

    private static org.jfree.chart.JFreeChart createEmptyChart(String message) {
        org.jfree.data.category.DefaultCategoryDataset dataset = new org.jfree.data.category.DefaultCategoryDataset();
        dataset.addValue(0, "No Data", "No Data");

        org.jfree.chart.JFreeChart chart = org.jfree.chart.ChartFactory.createBarChart(
                message,
                "",
                "Count",
                dataset,
                org.jfree.chart.plot.PlotOrientation.VERTICAL,
                false,
                true,
                false
        );

        chart.setBackgroundPaint(java.awt.Color.WHITE);
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(java.awt.Color.WHITE);

        return chart;
    }
}