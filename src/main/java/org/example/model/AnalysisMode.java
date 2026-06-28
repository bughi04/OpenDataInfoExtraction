package org.example.model;

// Quick and full analysis mode for the files
public enum AnalysisMode {
    QUICK("Quick", "Fast: skips exact betweenness and samples path metrics on large graphs"),
    FULL("Full", "Exact: computes all centrality metrics (can take hours on large cities)");
    private final String label;
    private final String description;
    AnalysisMode(String label, String description) {
        this.label = label;
        this.description = description;
    }
    public String getLabel() { return label; }
    public String getDescription() { return description; }
    public static AnalysisMode fromString(String value) {
        if (value == null || value.isBlank()) return QUICK;
        try {
            return AnalysisMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return QUICK;
        }
    }
}
