package org.example.model;

public class CpvCode {
    private String code;
    private String romanianName;
    private String englishName;

    public CpvCode(String code, String romanianName, String englishName) {
        this.code = code;
        this.romanianName = romanianName;
        this.englishName = englishName;
    }

    public String getCode() {
        return code;
    }

    public String getRomanianName() {
        return romanianName;
    }

    public String getEnglishName() {
        return englishName;
    }

    @Override
    public String toString() {
        return code + " - " + romanianName + " / " + englishName;
    }

    public String getCategory() {
        if (code != null && code.length() >= 2) {
            return code.substring(0, 2);
        }
        return "";
    }
}