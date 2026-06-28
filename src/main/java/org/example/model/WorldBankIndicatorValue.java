package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.Locale;
import java.util.Objects;

// A single World Bank indicator observation for a reference area
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldBankIndicatorValue implements Serializable {
    private static final long serialVersionUID = 1L;
    private String indicatorId;
    private String label;
    private String description;
    private Double value;
    private String year;
    private String unit;
    public WorldBankIndicatorValue() {}
    public WorldBankIndicatorValue(String indicatorId, String label, String description,
                                   Double value, String year, String unit) {
        this.indicatorId = indicatorId;
        this.label = label;
        this.description = description;
        this.value = value;
        this.year = year;
        this.unit = unit;
    }
    public String getIndicatorId() { return indicatorId; }
    public void setIndicatorId(String indicatorId) { this.indicatorId = indicatorId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Double getValue() { return value; }
    public void setValue(Double value) { this.value = value; }
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    @JsonIgnore
    public boolean hasValue() {
        return value != null && !Double.isNaN(value) && !Double.isInfinite(value);
    }
    @JsonIgnore
    public String formattedValue() {
        if (!hasValue()) return "N/A";
        double v = value;
        if (Math.abs(v) >= 1_000_000) return String.format(Locale.ROOT, "%.2f M", v / 1_000_000);
        if (Math.abs(v) >= 10_000) return String.format(Locale.ROOT, "%.0f", v);
        if (Math.abs(v) >= 100) return String.format(Locale.ROOT, "%.1f", v);
        return String.format(Locale.ROOT, "%.2f", v);
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldBankIndicatorValue that)) return false;
        return Objects.equals(indicatorId, that.indicatorId);
    }
    @Override
    public int hashCode() {
        return Objects.hash(indicatorId);
    }
}