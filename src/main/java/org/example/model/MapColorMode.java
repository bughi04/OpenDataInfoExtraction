package org.example.model;

// Color encoding for the street-network map view.
public enum MapColorMode {
    HIGHWAY("Highway type"),
    SPEED("Speed limit"),
    ONEWAY("Oneway vs two-way"),
    INFRASTRUCTURE("Bridges & tunnels");
    private final String label;
    MapColorMode(String label) {
        this.label = label;
    }
    public String getLabel() { return label; }
    @Override public String toString() { return label; }
}
