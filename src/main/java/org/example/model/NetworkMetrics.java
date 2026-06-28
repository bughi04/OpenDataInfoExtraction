package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;
/*
 Serializable container for graph, street-network, and World Bank metrics
 computed for one processed GraphML city network. Used for session persistence,
 charts, reports, and CSV export.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkMetrics implements Serializable {
    private static final long serialVersionUID = 1L;

    private String graphFile;
    private String graphName;
    private LocalDateTime analysisTimestamp;

    private int nodeCount;
    private int edgeCount;
    @JsonProperty("directed")
    private boolean isDirected;

    private Double avgBetweennessCentrality;
    private Double avgClosenessCentrality;
    private Double avgDegreeCentrality;

    private Double graphEntropy;
    private Integer graphDiameter;
    private Double graphDensity;
    private Double clusteringCoefficient;
    private Double avgPathLength;

    private Double constraints;
    private Double assortativityDegree;
    private Double meanDegree;
    private Double reciprocity;
    private Double diversity;

    private StreetNetworkStats streetStats;

    private WorldBankCityData worldBankData;

    private boolean metricsApproximated;

    private AnalysisMode analysisMode;
    public NetworkMetrics() {
        this.analysisTimestamp = LocalDateTime.now();
    }
    public NetworkMetrics(String graphFile) {
        this();
        this.graphFile = graphFile;
        this.graphName = extractGraphName(graphFile);
    }
    private String extractGraphName(String filepath) {
        if (filepath == null) return "Unknown";
        String name = new java.io.File(filepath).getName();
        if (name.endsWith(".graphml")) {
            name = name.substring(0, name.length() - 8);
        }
        return name;
    }
    public String getGraphFile() {
        return graphFile;
    }

    public String getGraphName() {
        return graphName;
    }
    public LocalDateTime getAnalysisTimestamp() {
        return analysisTimestamp;
    }

    public int getNodeCount() {
        return nodeCount;
    }
    public void setNodeCount(int nodeCount) {
        this.nodeCount = nodeCount;
    }
    public int getEdgeCount() {
        return edgeCount;
    }
    public void setEdgeCount(int edgeCount) {
        this.edgeCount = edgeCount;
    }
    public boolean isDirected() {
        return isDirected;
    }
    public void setDirected(boolean directed) {
        isDirected = directed;
    }
    public Double getAvgBetweennessCentrality() {
        return avgBetweennessCentrality;
    }
    public void setAvgBetweennessCentrality(Double avgBetweennessCentrality) {
        this.avgBetweennessCentrality = avgBetweennessCentrality;
    }
    public Double getAvgClosenessCentrality() {
        return avgClosenessCentrality;
    }
    public void setAvgClosenessCentrality(Double avgClosenessCentrality) {
        this.avgClosenessCentrality = avgClosenessCentrality;
    }
    public Double getAvgDegreeCentrality() {
        return avgDegreeCentrality;
    }
    public void setAvgDegreeCentrality(Double avgDegreeCentrality) {
        this.avgDegreeCentrality = avgDegreeCentrality;
    }
    public Double getGraphEntropy() {
        return graphEntropy;
    }
    public void setGraphEntropy(Double graphEntropy) {
        this.graphEntropy = graphEntropy;
    }
    public Integer getGraphDiameter() {
        return graphDiameter;
    }
    public void setGraphDiameter(Integer graphDiameter) {
        this.graphDiameter = graphDiameter;
    }
    public Double getGraphDensity() {
        return graphDensity;
    }
    public void setGraphDensity(Double graphDensity) {
        this.graphDensity = graphDensity;
    }
    public Double getClusteringCoefficient() {
        return clusteringCoefficient;
    }
    public void setClusteringCoefficient(Double clusteringCoefficient) {
        this.clusteringCoefficient = clusteringCoefficient;
    }
    public Double getAvgPathLength() {
        return avgPathLength;
    }
    public void setAvgPathLength(Double avgPathLength) {
        this.avgPathLength = avgPathLength;
    }
    public Double getConstraints() {
        return constraints;
    }
    public void setConstraints(Double constraints) {
        this.constraints = constraints;
    }
    public Double getAssortativityDegree() {
        return assortativityDegree;
    }
    public void setAssortativityDegree(Double assortativityDegree) {
        this.assortativityDegree = assortativityDegree;
    }
    public Double getMeanDegree() {
        return meanDegree;
    }
    public void setMeanDegree(Double meanDegree) {
        this.meanDegree = meanDegree;
    }
    public Double getReciprocity() {
        return reciprocity;
    }
    public void setReciprocity(Double reciprocity) {
        this.reciprocity = reciprocity;
    }
    public Double getDiversity() {
        return diversity;
    }
    public void setDiversity(Double diversity) {
        this.diversity = diversity;
    }
    public StreetNetworkStats getStreetStats() { return streetStats; }
    public void setStreetStats(StreetNetworkStats streetStats) {
        this.streetStats = streetStats;
    }
    public WorldBankCityData getWorldBankData() {
        return worldBankData;
    }
    public void setWorldBankData(WorldBankCityData worldBankData) {
        this.worldBankData = worldBankData;
    }
    public boolean isMetricsApproximated() { return metricsApproximated; }
    public void setMetricsApproximated(boolean metricsApproximated) {
        this.metricsApproximated = metricsApproximated;
    }

    public void setAnalysisMode(AnalysisMode analysisMode) {
        this.analysisMode = analysisMode;
    }

    // Display labels for results table
    @JsonIgnore
    public String getAnalysisModeLabel() {
        if (analysisMode == null) {
            return metricsApproximated ? "Quick (approx.)" : "Full";
        }
        if (analysisMode == AnalysisMode.QUICK && metricsApproximated) {
            return "Quick (approx.)";
        }
        return analysisMode.getLabel();
    }
    @Override
    public String toString() {
        return "NetworkMetrics{" +
                "graphName='" + graphName + '\'' +
                ", nodes=" + nodeCount +
                ", edges=" + edgeCount +
                ", directed=" + isDirected +
                ", avgBetweenness=" + String.format("%.4f", avgBetweennessCentrality != null ? avgBetweennessCentrality : 0.0) +
                ", avgCloseness=" + String.format("%.4f", avgClosenessCentrality != null ? avgClosenessCentrality : 0.0) +
                ", avgDegree=" + String.format("%.4f", avgDegreeCentrality != null ? avgDegreeCentrality : 0.0) +
                ", entropy=" + String.format("%.4f", graphEntropy != null ? graphEntropy : 0.0) +
                ", diameter=" + graphDiameter +
                ", density=" + String.format("%.4f", graphDensity != null ? graphDensity : 0.0) +
                ", clustering=" + String.format("%.4f", clusteringCoefficient != null ? clusteringCoefficient : 0.0) +
                ", avgPathLength=" + String.format("%.4f", avgPathLength != null ? avgPathLength : 0.0) +
                '}';
    }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkMetrics that = (NetworkMetrics) o;
        return Objects.equals(graphFile, that.graphFile) &&
                Objects.equals(analysisTimestamp, that.analysisTimestamp);
    }
    @Override
    public int hashCode() {
        return Objects.hash(graphFile, analysisTimestamp);
    }
}