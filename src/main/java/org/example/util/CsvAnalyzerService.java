package org.example.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class CsvAnalyzerService {
    private static final Logger logger = LoggerFactory.getLogger(CsvAnalyzerService.class);

    public static final char COMMA_DELIMITER = ',';
    public static final char SEMICOLON_DELIMITER = ';';
    public static final char TAB_DELIMITER = '\t';

    public static Map<String, Object> analyzeCSV(File file) {
        Map<String, Object> result = new HashMap<>();
        result.put("fileName", file.getName());
        result.put("fileSize", file.length());

        try {
            char delimiter = detectDelimiter(file);
            result.put("delimiter", String.valueOf(delimiter));

            Map<String, Object> csvData = readCsvData(file, delimiter);
            result.putAll(csvData);

            List<String> headers = (List<String>) csvData.get("headers");
            List<List<String>> allData = (List<List<String>>) csvData.get("allData");

            Map<String, Map<String, Object>> columnStats = analyzeColumns(headers, allData);
            result.put("columnStats", columnStats);

        } catch (Exception e) {
            logger.error("Error analyzing CSV file: {}", e.getMessage(), e);
            result.put("error", "Error analyzing CSV file: " + e.getMessage());
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

    private static Map<String, Object> readCsvData(File file, char delimiter) throws IOException {
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

    private static int countOccurrences(String str, char ch) {
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
}