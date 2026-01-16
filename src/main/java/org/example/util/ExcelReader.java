package org.example.util;

import org.example.model.CpvCode;
import org.example.model.ProcurementItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.*;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExcelReader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);
    private static final Pattern CPV_CODE_PATTERN = Pattern.compile("\\d{8}-\\d");
    private static final Pattern CPV_CODE_PATTERN_SIMPLE = Pattern.compile("\\d{8}");
    private static final NumberFormat NUMBER_FORMAT = NumberFormat.getInstance(new Locale("ro", "RO"));

    private static final Pattern CPV_HEADER_PATTERN = Pattern.compile("(?i)(cpv|common procurement vocabulary|cod)");
    private static final Pattern DESCRIPTION_HEADER_PATTERN = Pattern.compile("(?i)(description|name|denumire|desc|text|obiect)");
    private static final Pattern VALUE_HEADER_PATTERN = Pattern.compile("(?i)(value|sum|amount|valoare|cost|price|pret)");

    public static Map<String, CpvCode> readCpvCodes(File file) throws Exception {
        Map<String, CpvCode> cpvCodes = new HashMap<>();

        logger.info("Starting to read CPV codes from file: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                logger.info("Processing sheet '{}' with {} rows",
                        sheet.getSheetName(), sheet.getLastRowNum() + 1);

                Map<String, CpvCode> standardCpvCodes = tryReadStandardCpvFormat(sheet);
                if (!standardCpvCodes.isEmpty()) {
                    logger.info("Successfully read {} CPV codes from standard format", standardCpvCodes.size());
                    cpvCodes.putAll(standardCpvCodes);
                    continue;
                }

                Map<String, CpvCode> alternativeCpvCodes = tryReadAlternativeCpvFormat(sheet);
                if (!alternativeCpvCodes.isEmpty()) {
                    logger.info("Successfully read {} CPV codes from alternative format", alternativeCpvCodes.size());
                    cpvCodes.putAll(alternativeCpvCodes);
                    continue;
                }

                Map<String, CpvCode> genericCpvCodes = tryReadGenericCpvFormat(sheet);
                if (!genericCpvCodes.isEmpty()) {
                    logger.info("Successfully read {} CPV codes from generic format", genericCpvCodes.size());
                    cpvCodes.putAll(genericCpvCodes);
                }
            }
        }

        logger.info("Read {} CPV codes from file {}", cpvCodes.size(), file.getName());

        if (cpvCodes.isEmpty()) {
            throw new Exception("No valid CPV codes found in the file. Please check the file format.");
        }

        return cpvCodes;
    }

    private static Map<String, CpvCode> tryReadStandardCpvFormat(Sheet sheet) {
        Map<String, CpvCode> cpvCodes = new HashMap<>();

        Row firstRow = sheet.getRow(0);
        boolean hasHeader = false;

        if (firstRow != null) {
            String firstCellValue = getCellValueAsString(firstRow.getCell(0)).trim();
            hasHeader = firstCellValue.equalsIgnoreCase("CODE") ||
                    firstCellValue.equalsIgnoreCase("COD") ||
                    CPV_HEADER_PATTERN.matcher(firstCellValue).find();
        }

        int startRow = hasHeader ? 1 : 0;

        int codeColumn = -1;
        int roNameColumn = -1;
        int enNameColumn = -1;

        for (int r = startRow; r <= Math.min(startRow + 5, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            if (codeColumn < 0) {
                for (int c = 0; c < Math.min(10, row.getLastCellNum()); c++) {
                    String value = getCellValueAsString(row.getCell(c)).trim();
                    if (CPV_CODE_PATTERN.matcher(value).matches() ||
                            CPV_CODE_PATTERN_SIMPLE.matcher(value).matches()) {
                        codeColumn = c;

                        int nextCol = c + 1;
                        if (nextCol < row.getLastCellNum()) {
                            roNameColumn = nextCol;
                            nextCol++;

                            if (nextCol < row.getLastCellNum()) {
                                enNameColumn = nextCol;
                            }
                        }

                        break;
                    }
                }
            }

            if (codeColumn >= 0) break;
        }

        if (codeColumn < 0) {
            return cpvCodes;
        }

        logger.info("Detected standard CPV format: code={}, roName={}, enName={}",
                codeColumn, roNameColumn, enNameColumn);

        for (int i = startRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            try {
                if (isEmptyRow(row)) continue;

                Cell codeCell = row.getCell(codeColumn);
                Cell roNameCell = roNameColumn >= 0 ? row.getCell(roNameColumn) : null;
                Cell enNameCell = enNameColumn >= 0 ? row.getCell(enNameColumn) : null;

                String code = getCellValueAsString(codeCell).trim();
                String roName = roNameCell != null ? getCellValueAsString(roNameCell).trim() : "";
                String enName = enNameCell != null ? getCellValueAsString(enNameCell).trim() : "";

                if (code.isEmpty()) continue;

                if (CPV_CODE_PATTERN.matcher(code).matches()) {
                    cpvCodes.put(code, new CpvCode(code, roName, enName));
                } else if (CPV_CODE_PATTERN_SIMPLE.matcher(code).matches()) {
                    code = code + "-0";
                    cpvCodes.put(code, new CpvCode(code, roName, enName));
                }
            } catch (Exception e) {
                logger.warn("Error processing CPV code at row {}: {}", i+1, e.getMessage());
            }
        }

        return cpvCodes;
    }

    private static Map<String, CpvCode> tryReadAlternativeCpvFormat(Sheet sheet) {
        Map<String, CpvCode> cpvCodes = new HashMap<>();

        int headerRow = -1;
        Map<String, Integer> columnMap = new HashMap<>();

        for (int i = 0; i <= Math.min(10, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Integer> potentialColumns = new HashMap<>();
            boolean hasCpvColumn = false;

            for (int c = 0; c < row.getLastCellNum(); c++) {
                String value = getCellValueAsString(row.getCell(c)).trim().toLowerCase();

                if (value.contains("cpv") || value.contains("cod") || value.equals("code")) {
                    potentialColumns.put("cpv", c);
                    hasCpvColumn = true;
                } else if (value.contains("descrip") || value.contains("name") ||
                        value.contains("denumire") || value.contains("text")) {
                    if (!potentialColumns.containsKey("roName")) {
                        potentialColumns.put("roName", c);
                    } else if (!potentialColumns.containsKey("enName")) {
                        potentialColumns.put("enName", c);
                    }
                } else if (value.contains("engl") || value.contains("en")) {
                    potentialColumns.put("enName", c);
                } else if (value.contains("rom") || value.contains("ro")) {
                    potentialColumns.put("roName", c);
                }
            }

            if (hasCpvColumn && potentialColumns.size() >= 2) {
                headerRow = i;
                columnMap = potentialColumns;
                break;
            }
        }

        if (headerRow < 0 || !columnMap.containsKey("cpv")) {
            return cpvCodes;
        }

        logger.info("Found alternative CPV format with header at row {}: {}", headerRow, columnMap);

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                int cpvColumn = columnMap.get("cpv");
                int roNameColumn = columnMap.getOrDefault("roName", -1);
                int enNameColumn = columnMap.getOrDefault("enName", -1);

                String code = getCellValueAsString(row.getCell(cpvColumn)).trim();
                String roName = roNameColumn >= 0 ? getCellValueAsString(row.getCell(roNameColumn)).trim() : "";
                String enName = enNameColumn >= 0 ? getCellValueAsString(row.getCell(enNameColumn)).trim() : "";

                if (code.isEmpty()) continue;

                Matcher matcher = CPV_CODE_PATTERN.matcher(code);
                if (matcher.find()) {
                    code = matcher.group();
                    cpvCodes.put(code, new CpvCode(code, roName, enName));
                } else {
                    matcher = CPV_CODE_PATTERN_SIMPLE.matcher(code);
                    if (matcher.find()) {
                        code = matcher.group() + "-0";
                        cpvCodes.put(code, new CpvCode(code, roName, enName));
                    }
                }
            } catch (Exception e) {
                logger.warn("Error processing alternative CPV format at row {}: {}", i+1, e.getMessage());
            }
        }

        return cpvCodes;
    }

    private static Map<String, CpvCode> tryReadGenericCpvFormat(Sheet sheet) {
        Map<String, CpvCode> cpvCodes = new HashMap<>();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int c = 0; c < row.getLastCellNum(); c++) {
                String cellValue = getCellValueAsString(row.getCell(c)).trim();

                Matcher matcher = CPV_CODE_PATTERN.matcher(cellValue);
                while (matcher.find()) {
                    String code = matcher.group();

                    String description = "";
                    if (c + 1 < row.getLastCellNum()) {
                        description = getCellValueAsString(row.getCell(c + 1)).trim();
                    } else if (c > 0) {
                        description = getCellValueAsString(row.getCell(c - 1)).trim();
                    }

                    cpvCodes.put(code, new CpvCode(code, description, ""));
                }

                if (!CPV_CODE_PATTERN.matcher(cellValue).find()) {
                    matcher = CPV_CODE_PATTERN_SIMPLE.matcher(cellValue);
                    while (matcher.find()) {
                        String code = matcher.group() + "-0";

                        String description = "";
                        if (c + 1 < row.getLastCellNum()) {
                            description = getCellValueAsString(row.getCell(c + 1)).trim();
                        } else if (c > 0) {
                            description = getCellValueAsString(row.getCell(c - 1)).trim();
                        }

                        cpvCodes.put(code, new CpvCode(code, description, ""));
                    }
                }
            }
        }

        return cpvCodes;
    }

    public static List<ProcurementItem> readProcurementItems(File file) throws Exception {
        List<ProcurementItem> items = new ArrayList<>();

        logger.info("Starting to read procurement items from file: {}", file.getName());

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = file.getName().endsWith(".xlsx") ? new XSSFWorkbook(fis) : new HSSFWorkbook(fis)) {

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                logger.info("Processing sheet '{}' with {} rows", sheet.getSheetName(), sheet.getLastRowNum() + 1);

                List<ProcurementItem> standardItems = tryReadStandardPaapFormat(sheet);
                if (!standardItems.isEmpty()) {
                    logger.info("Successfully read {} procurement items from standard format", standardItems.size());
                    items.addAll(standardItems);
                    continue;
                }

                List<ProcurementItem> genericItems = tryReadGenericProcurementFormat(sheet);
                if (!genericItems.isEmpty()) {
                    logger.info("Successfully read {} procurement items from generic format", genericItems.size());
                    items.addAll(genericItems);
                }
            }
        }

        logger.info("Read {} procurement items from file {}", items.size(), file.getName());

        if (items.isEmpty()) {
            throw new Exception("No valid procurement items found in the file. Please check the file format.");
        }

        return items;
    }

    private static List<ProcurementItem> tryReadStandardPaapFormat(Sheet sheet) {
        List<ProcurementItem> items = new ArrayList<>();

        int headerRowIndex = findHeaderRow(sheet);

        if (headerRowIndex < 0) {
            logger.warn("No header row found in sheet '{}'", sheet.getSheetName());
            return items;
        }

        logger.info("Found header row at index {} in sheet '{}'", headerRowIndex, sheet.getSheetName());

        Row headerRow = sheet.getRow(headerRowIndex);
        Map<String, Integer> columnIndices = mapColumnIndices(headerRow);

        if (columnIndices.isEmpty()) {
            logger.warn("Could not identify column headers in sheet '{}'", sheet.getSheetName());
            return items;
        }

        logger.info("Mapped column indices: {}", columnIndices);

        int dataStartRow = findFirstDataRow(sheet, headerRowIndex, columnIndices);
        if (dataStartRow < 0) {
            logger.warn("Could not find any data rows after header in sheet '{}'", sheet.getSheetName());
            return items;
        }

        logger.info("Starting to read data from row {} in sheet '{}'", dataStartRow, sheet.getSheetName());

        for (int i = dataStartRow; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                ProcurementItem item = extractProcurementItem(row, columnIndices);

                if (item != null && item.getObjectName() != null && !item.getObjectName().trim().isEmpty()) {
                    items.add(item);
                }
            } catch (Exception e) {
                logger.warn("Error extracting procurement item at row {}: {}", i+1, e.getMessage());
            }
        }

        return items;
    }

    private static List<ProcurementItem> tryReadGenericProcurementFormat(Sheet sheet) {
        List<ProcurementItem> items = new ArrayList<>();

        int headerRow = -1;
        Map<String, Integer> columnMap = new HashMap<>();

        for (int i = 0; i <= Math.min(20, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            Map<String, Integer> potentialColumns = new HashMap<>();
            boolean hasNameColumn = false;
            boolean hasValueColumn = false;

            for (int c = 0; c < row.getLastCellNum(); c++) {
                String value = getCellValueAsString(row.getCell(c)).trim().toLowerCase();

                if (value.isEmpty()) continue;

                if (value.contains("nr") || value.contains("no") || value.equals("#")) {
                    potentialColumns.put("rowNumber", c);
                } else if (DESCRIPTION_HEADER_PATTERN.matcher(value).find() ||
                        value.contains("obiect") || value.contains("denumire")) {
                    potentialColumns.put("objectName", c);
                    hasNameColumn = true;
                } else if (CPV_HEADER_PATTERN.matcher(value).find()) {
                    potentialColumns.put("cpvField", c);
                } else if (VALUE_HEADER_PATTERN.matcher(value).find()) {
                    if (value.contains("tva") || value.contains("vat") ||
                            value.contains("tax") || value.contains("impozit")) {
                        if (value.contains("fara") || value.contains("without") ||
                                value.contains("excluding") || value.contains("net")) {
                            potentialColumns.put("valueWithoutTVA", c);
                        } else {
                            potentialColumns.put("valueWithTVA", c);
                        }
                    } else {
                        potentialColumns.put("valueWithoutTVA", c);
                    }
                    hasValueColumn = true;
                } else if (value.contains("source") || value.contains("sursa") ||
                        value.contains("funding") || value.contains("finanțare") ||
                        value.contains("finantare")) {
                    potentialColumns.put("source", c);
                } else if (value.contains("start") || value.contains("begin") ||
                        value.contains("initiation") || value.contains("inițiere") ||
                        value.contains("initiere")) {
                    potentialColumns.put("initiationDate", c);
                } else if (value.contains("end") || value.contains("finish") ||
                        value.contains("completion") || value.contains("finalizare")) {
                    potentialColumns.put("completionDate", c);
                }
            }

            if (hasNameColumn && hasValueColumn) {
                headerRow = i;
                columnMap = potentialColumns;
                break;
            }
        }

        if (headerRow < 0) {
            columnMap = detectColumnsWithoutHeader(sheet);
            headerRow = 0;

            if (columnMap.isEmpty()) {
                return items;
            }

            logger.info("No header found, but detected column structure: {}", columnMap);
        } else {
            logger.info("Found generic procurement format with header at row {}: {}", headerRow, columnMap);
        }

        for (int i = headerRow + 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null || isEmptyRow(row)) continue;

            try {
                ProcurementItem item = new ProcurementItem();

                if (columnMap.containsKey("rowNumber")) {
                    item.setRowNumber(extractIntValue(row.getCell(columnMap.get("rowNumber"))));
                } else {
                    item.setRowNumber(i + 1 - headerRow);
                }

                if (columnMap.containsKey("objectName")) {
                    String objectName = getCellValueAsString(row.getCell(columnMap.get("objectName"))).trim();
                    item.setObjectName(objectName);

                    if (objectName.isEmpty()) continue;
                } else {
                    continue;
                }

                if (columnMap.containsKey("cpvField")) {
                    item.setCpvField(getCellValueAsString(row.getCell(columnMap.get("cpvField"))));
                }

                if (columnMap.containsKey("valueWithoutTVA")) {
                    item.setValueWithoutTVA(extractDoubleValue(row.getCell(columnMap.get("valueWithoutTVA"))));
                }

                if (columnMap.containsKey("valueWithTVA")) {
                    item.setValueWithTVA(extractDoubleValue(row.getCell(columnMap.get("valueWithTVA"))));
                }

                if (columnMap.containsKey("source")) {
                    item.setSource(getCellValueAsString(row.getCell(columnMap.get("source"))));
                }

                if (columnMap.containsKey("initiationDate")) {
                    item.setInitiationDate(getCellValueAsString(row.getCell(columnMap.get("initiationDate"))));
                }

                if (columnMap.containsKey("completionDate")) {
                    item.setCompletionDate(getCellValueAsString(row.getCell(columnMap.get("completionDate"))));
                }

                if (item.getObjectName() != null && !item.getObjectName().trim().isEmpty()) {
                    items.add(item);
                }

            } catch (Exception e) {
                logger.warn("Error processing generic format at row {}: {}", i+1, e.getMessage());
            }
        }

        return items;
    }

    private static Map<String, Integer> detectColumnsWithoutHeader(Sheet sheet) {
        Map<String, Integer> columnMap = new HashMap<>();

        int minRows = Math.min(10, sheet.getLastRowNum());
        if (minRows < 3) return columnMap;

        int maxColumn = -1;

        for (int i = 0; i <= minRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            maxColumn = Math.max(maxColumn, row.getLastCellNum() - 1);
        }

        if (maxColumn < 1) return columnMap;

        int[] numberCount = new int[maxColumn + 1];
        int[] dateCount = new int[maxColumn + 1];
        int[] cpvCount = new int[maxColumn + 1];
        int[] textLengthSum = new int[maxColumn + 1];

        for (int i = 0; i <= minRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int c = 0; c <= maxColumn; c++) {
                Cell cell = row.getCell(c);
                if (cell == null) continue;

                String cellValue = getCellValueAsString(cell).trim();
                if (cellValue.isEmpty()) continue;

                if (CPV_CODE_PATTERN.matcher(cellValue).find() ||
                        CPV_CODE_PATTERN_SIMPLE.matcher(cellValue).find()) {
                    cpvCount[c]++;
                }

                try {
                    Double.parseDouble(cellValue.replaceAll("[^\\d.]", ""));
                    numberCount[c]++;
                } catch (NumberFormatException e) {
                    // Not a number
                }

                if (cellValue.matches(".*\\d{1,2}[/.-]\\d{1,2}[/.-]\\d{2,4}.*") ||
                        cellValue.matches(".*\\d{4}[/.-]\\d{1,2}[/.-]\\d{1,2}.*") ||
                        cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    dateCount[c]++;
                }

                textLengthSum[c] += cellValue.length();
            }
        }

        int cpvColumn = -1;
        int maxCpvCount = 0;
        for (int c = 0; c <= maxColumn; c++) {
            if (cpvCount[c] > maxCpvCount) {
                maxCpvCount = cpvCount[c];
                cpvColumn = c;
            }
        }

        if (cpvColumn >= 0 && cpvCount[cpvColumn] >= minRows / 2) {
            columnMap.put("cpvField", cpvColumn);
        }

        int nameColumn = -1;
        int maxTextLength = 0;
        for (int c = 0; c <= maxColumn; c++) {
            if (c != cpvColumn && textLengthSum[c] > maxTextLength) {
                maxTextLength = textLengthSum[c];
                nameColumn = c;
            }
        }

        if (nameColumn >= 0) {
            columnMap.put("objectName", nameColumn);
        }

        List<Integer> valueColumns = new ArrayList<>();
        for (int c = 0; c <= maxColumn; c++) {
            if (c != cpvColumn && c != nameColumn &&
                    numberCount[c] >= minRows / 2 && dateCount[c] < numberCount[c] / 2) {
                valueColumns.add(c);
            }
        }

        if (!valueColumns.isEmpty()) {
            columnMap.put("valueWithoutTVA", valueColumns.get(0));
            if (valueColumns.size() > 1) {
                columnMap.put("valueWithTVA", valueColumns.get(1));
            }
        }

        List<Integer> dateColumns = new ArrayList<>();
        for (int c = 0; c <= maxColumn; c++) {
            if (c != cpvColumn && c != nameColumn && !valueColumns.contains(c) &&
                    dateCount[c] >= minRows / 3) {
                dateColumns.add(c);
            }
        }

        if (!dateColumns.isEmpty()) {
            columnMap.put("initiationDate", dateColumns.get(0));

            if (dateColumns.size() > 1) {
                columnMap.put("completionDate", dateColumns.get(1));
            }
        }

        return columnMap;
    }

    private static int findFirstDataRow(Sheet sheet, int headerRowIndex, Map<String, Integer> columnIndices) {
        int dataStartRow = headerRowIndex + 1;
        boolean foundData = false;

        for (int r = dataStartRow; r <= Math.min(headerRowIndex + 10, sheet.getLastRowNum()); r++) {
            Row row = sheet.getRow(r);
            if (row == null) continue;

            boolean hasData = false;
            for (int colIndex : columnIndices.values()) {
                if (colIndex >= 0 && colIndex < row.getLastCellNum()) {
                    String cellValue = getCellValueAsString(row.getCell(colIndex)).trim();
                    if (!cellValue.isEmpty()) {
                        hasData = true;
                        break;
                    }
                }
            }

            if (hasData) {
                dataStartRow = r;
                foundData = true;
                break;
            }
        }

        return foundData ? dataStartRow : -1;
    }

    private static boolean isEmptyRow(Row row) {
        if (row == null) return true;

        boolean isEmpty = true;
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            if (c >= 0 && !getCellValueAsString(row.getCell(c)).trim().isEmpty()) {
                isEmpty = false;
                break;
            }
        }

        return isEmpty;
    }

    private static int findHeaderRow(Sheet sheet) {
        Set<String> headerPatterns = new HashSet<>(Arrays.asList(
                "nr. crt", "nr.crt", "nr crt", "nr", "numar", "număr", "pozitie",
                "obiectul", "obiect", "achizitie", "achiziție", "denumire",
                "cod cpv", "cpv", "cod", "coduri", "value", "valoare", "pret", "preț"
        ));
        for (int i = 0; i <= Math.min(50, sheet.getLastRowNum()); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            int matchCount = 0;
            int headerCandidateCount = 0;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell == null) continue;

                String value = getCellValueAsString(cell).toLowerCase().trim();
                if (value.isEmpty()) continue;

                headerCandidateCount++;

                for (String pattern : headerPatterns) {
                    if (value.contains(pattern)) {
                        matchCount++;
                        break;
                    }
                }
            }

            if (matchCount >= 2 && headerCandidateCount > 0 &&
                    (double)matchCount / headerCandidateCount >= 0.15) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> mapColumnIndices(Row headerRow) {
        Map<String, Integer> columnIndices = new HashMap<>();

        if (headerRow == null) return columnIndices;

        Map<String, List<String>> patternGroups = new HashMap<>();
        patternGroups.put("rowNumber", Arrays.asList("nr. crt", "nr.crt", "nr crt", "nr", "#", "numar", "număr", "pozitie", "position"));
        patternGroups.put("objectName", Arrays.asList("obiectul", "obiect", "achizitie", "achiziției", "achizitiei", "achiziție", "denumire", "denumirea", "name", "object", "description", "item"));
        patternGroups.put("cpvField", Arrays.asList("cod cpv", "cpv", "cod", "coduri", "code"));
        patternGroups.put("valueWithoutTVA", Arrays.asList("valoare", "valoarea", "fără tva", "fara tva", "fără", "fara", "lei", "ron", "pret", "price", "netă", "neta", "excluding vat", "without vat"));
        patternGroups.put("valueWithTVA", Arrays.asList("valoare", "valoarea", "cu tva", "inclusiv tva", "cu", "lei", "ron", "brut", "brută", "including vat", "with vat"));
        patternGroups.put("source", Arrays.asList("sursa", "sursă", "finanțare", "finantare", "fonduri", "buget", "source", "funding"));
        patternGroups.put("initiationDate", Arrays.asList("inițiere", "initiere", "data", "dată", "începere", "incepere", "start", "beginning", "initiation"));
        patternGroups.put("completionDate", Arrays.asList("finalizare", "finalizarea", "data", "dată", "încheiere", "incheiere", "final", "sfarsit", "sfârșit", "completion", "end"));

        Map<String, Map<Integer, Integer>> fieldScores = new HashMap<>();
        for (String field : patternGroups.keySet()) {
            fieldScores.put(field, new HashMap<>());
        }

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;

            String headerValue = getCellValueAsString(cell).toLowerCase().trim();
            if (headerValue.isEmpty()) continue;

            for (Map.Entry<String, List<String>> entry : patternGroups.entrySet()) {
                String field = entry.getKey();
                List<String> patterns = entry.getValue();

                int score = 0;
                for (String pattern : patterns) {
                    if (headerValue.contains(pattern)) {
                        score += 3;
                    }
                }

                if (field.equals("valueWithTVA") && score > 0 && headerValue.contains("cu tva")) {
                    score += 5;
                } else if (field.equals("valueWithoutTVA") && score > 0 &&
                        (headerValue.contains("fără tva") || headerValue.contains("fara tva"))) {
                    score += 5;
                }

                if (score > 0) {
                    fieldScores.get(field).put(i, score);
                }
            }
        }

        for (String field : fieldScores.keySet()) {
            Map<Integer, Integer> scores = fieldScores.get(field);

            if (!scores.isEmpty()) {
                int bestColumn = -1;
                int highestScore = 0;

                for (Map.Entry<Integer, Integer> scoreEntry : scores.entrySet()) {
                    int column = scoreEntry.getKey();
                    int score = scoreEntry.getValue();

                    if (score > highestScore) {
                        highestScore = score;
                        bestColumn = column;
                    }
                }

                if (bestColumn >= 0) {
                    columnIndices.put(field, bestColumn);
                }
            }
        }

        return columnIndices;
    }

    private static ProcurementItem extractProcurementItem(Row row, Map<String, Integer> columnIndices) {
        if (row == null) return null;

        ProcurementItem item = new ProcurementItem();

        if (columnIndices.containsKey("rowNumber")) {
            int index = columnIndices.get("rowNumber");
            if (index >= 0) {
                item.setRowNumber(extractIntValue(row.getCell(index)));
            }
        }

        if (columnIndices.containsKey("objectName")) {
            int index = columnIndices.get("objectName");
            if (index >= 0) {
                String objectName = getCellValueAsString(row.getCell(index)).trim();
                item.setObjectName(objectName);

                if (objectName.isEmpty()) {
                    return null;
                }
            }
        }

        if (columnIndices.containsKey("cpvField")) {
            int index = columnIndices.get("cpvField");
            if (index >= 0) {
                String cpvField = getCellValueAsString(row.getCell(index));
                item.setCpvField(cpvField);
            }
        }

        if (columnIndices.containsKey("valueWithoutTVA")) {
            int index = columnIndices.get("valueWithoutTVA");
            if (index >= 0) {
                double value = extractDoubleValue(row.getCell(index));
                item.setValueWithoutTVA(value);
            }
        }

        if (columnIndices.containsKey("valueWithTVA")) {
            int index = columnIndices.get("valueWithTVA");
            if (index >= 0) {
                double value = extractDoubleValue(row.getCell(index));
                item.setValueWithTVA(value);
            }
        }

        if (columnIndices.containsKey("source")) {
            int index = columnIndices.get("source");
            if (index >= 0) {
                item.setSource(getCellValueAsString(row.getCell(index)));
            }
        }

        if (columnIndices.containsKey("initiationDate")) {
            int index = columnIndices.get("initiationDate");
            if (index >= 0) {
                item.setInitiationDate(getCellValueAsString(row.getCell(index)));
            }
        }

        if (columnIndices.containsKey("completionDate")) {
            int index = columnIndices.get("completionDate");
            if (index >= 0) {
                item.setCompletionDate(getCellValueAsString(row.getCell(index)));
            }
        }

        return item;
    }

    private static int extractIntValue(Cell cell) {
        if (cell == null) return 0;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return (int) cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return 0;

                    Matcher matcher = Pattern.compile("\\d+").matcher(value);
                    if (matcher.find()) {
                        return Integer.parseInt(matcher.group());
                    }
                    return 0;
                case FORMULA:
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);

                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            return (int) cellValue.getNumberValue();
                        } else if (cellValue.getCellType() == CellType.STRING) {
                            String strValue = cellValue.getStringValue();
                            Matcher m = Pattern.compile("\\d+").matcher(strValue);
                            if (m.find()) {
                                return Integer.parseInt(m.group());
                            }
                        }
                    } catch (Exception ex) {
                        logger.debug("Failed to evaluate formula for int: {}", ex.getMessage());
                    }
                    return 0;
                default:
                    return 0;
            }
        } catch (Exception e) {
            logger.debug("Failed to extract integer value: {}", e.getMessage());
            return 0;
        }
    }

    private static double extractDoubleValue(Cell cell) {
        if (cell == null) return 0.0;

        try {
            switch (cell.getCellType()) {
                case NUMERIC:
                    return cell.getNumericCellValue();
                case STRING:
                    String value = cell.getStringCellValue().trim();
                    if (value.isEmpty()) return 0.0;

                    value = value.replaceAll("[^0-9.,\\-]", "");

                    if (value.isEmpty()) return 0.0;

                    try {
                        return NUMBER_FORMAT.parse(value).doubleValue();
                    } catch (ParseException e1) {
                        try {
                            if (value.contains(",") && value.contains(".")) {
                                value = value.replace(",", "");
                            } else if (value.contains(",") && !value.contains(".")) {
                                value = value.replace(",", ".");
                            }
                            return Double.parseDouble(value);
                        } catch (NumberFormatException e2) {
                            logger.debug("Failed to parse double value: {}", value);
                            return 0.0;
                        }
                    }
                case FORMULA:
                    try {
                        FormulaEvaluator evaluator = cell.getSheet().getWorkbook().getCreationHelper().createFormulaEvaluator();
                        CellValue cellValue = evaluator.evaluate(cell);

                        if (cellValue.getCellType() == CellType.NUMERIC) {
                            return cellValue.getNumberValue();
                        } else if (cellValue.getCellType() == CellType.STRING) {
                            String strValue = cellValue.getStringValue().trim();
                            if (strValue.isEmpty()) return 0.0;

                            strValue = strValue.replaceAll("[^0-9.,\\-]", "");

                            if (strValue.isEmpty()) return 0.0;

                            try {
                                return NUMBER_FORMAT.parse(strValue).doubleValue();
                            } catch (ParseException e1) {
                                try {
                                    if (strValue.contains(",") && strValue.contains(".")) {
                                        strValue = strValue.replace(",", "");
                                    } else if (strValue.contains(",") && !strValue.contains(".")) {
                                        strValue = strValue.replace(",", ".");
                                    }
                                    return Double.parseDouble(strValue);
                                } catch (NumberFormatException e2) {
                                    return 0.0;
                                }
                            }
                        }
                    } catch (Exception ex) {
                        logger.debug("Failed to evaluate formula for double: {}", ex.getMessage());
                    }
                    return 0.0;
                default:
                    return 0.0;
            }
        } catch (Exception e) {
            logger.debug("Failed to extract double value: {}", e.getMessage());
            return 0.0;
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
                                if (DateUtil.isCellDateFormatted(cell)) {
                                    return new Date((long)cellValue.getNumberValue()).toString();
                                } else {
                                    double value = cellValue.getNumberValue();
                                    if (value == Math.floor(value) && !Double.isInfinite(value)) {
                                        return String.format("%.0f", value);
                                    } else {
                                        return String.valueOf(value);
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
                        try {
                            return cell.getStringCellValue();
                        } catch (Exception ex) {
                            try {
                                return String.valueOf(cell.getNumericCellValue());
                            } catch (Exception ex2) {
                                return "";
                            }
                        }
                    }
                case BLANK:
                    return "";
                default:
                    return "";
            }
        } catch (Exception e) {
            logger.debug("Error getting cell value as string: {}", e.getMessage());
            return "";
        }
    }
}