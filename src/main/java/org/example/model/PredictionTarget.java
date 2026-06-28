package org.example.model;

// Supervised target for mobility-readiness prediction
public enum PredictionTarget {
    UMRI("UMRi", "Urban Mobility Readiness Index"),
    SMI("SMi", "Sustainable Mobility Index"),
    PTI("PTi", "Public Transit Index");
    private final String shortLabel;
    private final String description;
    PredictionTarget(String shortLabel, String description) {
        this.shortLabel = shortLabel;
        this.description = description;
    }
    public String getShortLabel() { return shortLabel; }
    public String getDescription() { return description; }
    @Override
    public String toString() { return shortLabel; }
}
