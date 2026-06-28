package org.example.model;

// Controls which street types are included when the map subsamples large networks
public class MapDisplayOptions {
    private boolean showSecondaryAndLocalStreets;
    public boolean isShowSecondaryAndLocalStreets() {
        return showSecondaryAndLocalStreets;
    }
    public void setShowSecondaryAndLocalStreets(boolean showSecondaryAndLocalStreets) {
        this.showSecondaryAndLocalStreets = showSecondaryAndLocalStreets;
    }
}
