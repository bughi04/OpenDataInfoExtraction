package org.example.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ProcurementItem {
    private static final Pattern CPV_CODE_PATTERN = Pattern.compile("\\d{8}-\\d");

    private int rowNumber;
    private String objectName;
    private String cpvField;
    private double valueWithoutTVA;
    private double valueWithTVA;
    private String source;
    private String initiationDate;
    private String completionDate;
    private List<String> cpvCodes;

    public ProcurementItem() {
        this.cpvCodes = new ArrayList<>();
        this.rowNumber = 0;
        this.valueWithoutTVA = 0.0;
        this.valueWithTVA = 0.0;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public String getCpvField() {
        return cpvField;
    }

    public void setCpvField(String cpvField) {
        this.cpvField = cpvField;
        extractCpvCodes();
    }

    public double getValueWithoutTVA() {
        return valueWithoutTVA;
    }

    public void setValueWithoutTVA(double valueWithoutTVA) {
        this.valueWithoutTVA = valueWithoutTVA;
    }

    public double getValueWithTVA() {
        return valueWithTVA;
    }

    public void setValueWithTVA(double valueWithTVA) {
        this.valueWithTVA = valueWithTVA;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getInitiationDate() {
        return initiationDate;
    }

    public void setInitiationDate(String initiationDate) {
        this.initiationDate = initiationDate;
    }

    public String getCompletionDate() {
        return completionDate;
    }

    public void setCompletionDate(String completionDate) {
        this.completionDate = completionDate;
    }

    public List<String> getCpvCodes() {
        return new ArrayList<>(cpvCodes);
    }

    private void extractCpvCodes() {
        cpvCodes.clear();

        if (cpvField == null || cpvField.trim().isEmpty()) {
            return;
        }

        Matcher matcher = CPV_CODE_PATTERN.matcher(cpvField);
        while (matcher.find()) {
            cpvCodes.add(matcher.group());
        }

        if (cpvCodes.isEmpty()) {
            Pattern numericPattern = Pattern.compile("\\d{8}");
            Matcher numericMatcher = numericPattern.matcher(cpvField);

            while (numericMatcher.find()) {
                String code = numericMatcher.group() + "-0";
                cpvCodes.add(code);
            }
        }
    }

    @Override
    public String toString() {
        return "#" + rowNumber + ": " + objectName +
                (valueWithoutTVA > 0 ? " - " + String.format("%,.2f", valueWithoutTVA) + " RON" : "");
    }
}