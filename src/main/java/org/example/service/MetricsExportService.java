package org.example.service;

import com.opencsv.CSVWriter;
import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.util.WorldBankAnalysisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
// Writes NetworkMetrics to CSV files in legacy Python-compatible formats
public class MetricsExportService {
    private static final Logger logger = LoggerFactory.getLogger(MetricsExportService.class);
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public File exportMetrics1(NetworkMetrics metrics, File outputFile) throws IOException {
        logger.info("Exporting metrics1 to: {}", outputFile.getAbsolutePath());
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            String[] header = {
                "Graph File",
                "Average Betweenness Centrality (Normalized)",
                "Average Closeness Centrality (Normalized)",
                "Average Degree Centrality (Normalized)",
                "Graph Entropy",
                "Graph Diameter",
                "Graph Density",
                "Clustering Coefficient",
                "Average Path Length"
            };
            writer.writeNext(header);
            String[] data = {
                metrics.getGraphFile(),
                formatDouble(metrics.getAvgBetweennessCentrality()),
                formatDouble(metrics.getAvgClosenessCentrality()),
                formatDouble(metrics.getAvgDegreeCentrality()),
                formatDouble(metrics.getGraphEntropy()),
                formatInteger(metrics.getGraphDiameter()),
                formatDouble(metrics.getGraphDensity()),
                formatDouble(metrics.getClusteringCoefficient()),
                formatDouble(metrics.getAvgPathLength())
            };
            writer.writeNext(data);
        }
        logger.info("Metrics1 exported successfully");
        return outputFile;
    }
    public File exportAllMetrics(NetworkMetrics metrics, File outputFile) throws IOException {
        logger.info("Exporting all metrics to: {}", outputFile.getAbsolutePath());
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            // Write header
            String[] header = {
                "Graph File",
                "Graph Name",
                "Analysis Timestamp",
                "Node Count",
                "Edge Count",
                "Is Directed",
                "Avg Betweenness Centrality",
                "Avg Closeness Centrality",
                "Avg Degree Centrality",
                "Graph Entropy",
                "Graph Diameter",
                "Graph Density",
                "Clustering Coefficient",
                "Average Path Length",
                "Constraints",
                "Assortativity Degree",
                "Mean Degree",
                "Reciprocity",
                "Diversity",
                "Road Length km",
                "Dominant Highway",
                "Oneway Ratio",
                "Avg Max Speed km/h",
                "Streets Per Node",
                "Pct Residential",
                "Pct Car Oriented",
                "Pct Pedestrian",
                "Pct Cycling",
                "Bridge Count",
                "Tunnel Count"
            };
            writer.writeNext(header);
            String[] data = {
                metrics.getGraphFile(),
                metrics.getGraphName(),
                metrics.getAnalysisTimestamp() != null ? 
                    metrics.getAnalysisTimestamp().format(DATE_FORMATTER) : "",
                String.valueOf(metrics.getNodeCount()),
                String.valueOf(metrics.getEdgeCount()),
                String.valueOf(metrics.isDirected()),
                formatDouble(metrics.getAvgBetweennessCentrality()),
                formatDouble(metrics.getAvgClosenessCentrality()),
                formatDouble(metrics.getAvgDegreeCentrality()),
                formatDouble(metrics.getGraphEntropy()),
                formatInteger(metrics.getGraphDiameter()),
                formatDouble(metrics.getGraphDensity()),
                formatDouble(metrics.getClusteringCoefficient()),
                formatDouble(metrics.getAvgPathLength()),
                formatDouble(metrics.getConstraints()),
                formatDouble(metrics.getAssortativityDegree()),
                formatDouble(metrics.getMeanDegree()),
                formatDouble(metrics.getReciprocity()),
                formatDouble(metrics.getDiversity()),
                formatStreet(metrics, StreetNetworkStats::getTotalLengthKm),
                formatStreetStr(metrics, StreetNetworkStats::getDominantHighwayType),
                formatStreet(metrics, s -> s.getOnewayRatio() * 100),
                formatStreet(metrics, StreetNetworkStats::getAvgMaxSpeedKmh),
                formatStreet(metrics, StreetNetworkStats::getStreetsPerNode),
                formatStreet(metrics, StreetNetworkStats::getPctResidential),
                formatStreet(metrics, StreetNetworkStats::getCarOrientedLengthPct),
                formatStreet(metrics, StreetNetworkStats::getPedestrianFriendlyLengthPct),
                formatStreet(metrics, StreetNetworkStats::getCyclingFriendlyLengthPct),
                formatStreetInt(metrics, StreetNetworkStats::getBridgeCount),
                formatStreetInt(metrics, StreetNetworkStats::getTunnelCount)
            };
            writer.writeNext(data);
        }
        logger.info("All metrics exported successfully");
        return outputFile;
    }
    public File exportMultipleMetrics(List<NetworkMetrics> metricsList, File outputFile) throws IOException {
        logger.info("Exporting {} metric sets to: {}", metricsList.size(), outputFile.getAbsolutePath());
        try (CSVWriter writer = new CSVWriter(new FileWriter(outputFile))) {
            String[] header = {
                "Graph File",
                "Graph Name",
                "Node Count",
                "Edge Count",
                "Avg Betweenness",
                "Avg Closeness",
                "Avg Degree",
                "Entropy",
                "Diameter",
                "Density",
                "Clustering",
                "Avg Path Length",
                "Mean Degree",
                "Reciprocity",
                "Diversity",
                "Road km",
                "Dominant Hwy",
                "Oneway %",
                "Streets/Node",
                "WB Country Code",
                "WB GDP per capita",
                "WB Urban pop %",
                "WB CO2 per capita"
            };
            writer.writeNext(header);
            for (NetworkMetrics metrics : metricsList) {
                String[] data = {
                    metrics.getGraphFile(),
                    metrics.getGraphName(),
                    String.valueOf(metrics.getNodeCount()),
                    String.valueOf(metrics.getEdgeCount()),
                    formatDouble(metrics.getAvgBetweennessCentrality()),
                    formatDouble(metrics.getAvgClosenessCentrality()),
                    formatDouble(metrics.getAvgDegreeCentrality()),
                    formatDouble(metrics.getGraphEntropy()),
                    formatInteger(metrics.getGraphDiameter()),
                    formatDouble(metrics.getGraphDensity()),
                    formatDouble(metrics.getClusteringCoefficient()),
                    formatDouble(metrics.getAvgPathLength()),
                    formatDouble(metrics.getMeanDegree()),
                    formatDouble(metrics.getReciprocity()),
                    formatDouble(metrics.getDiversity()),
                    formatStreet(metrics, StreetNetworkStats::getTotalLengthKm),
                    formatStreetStr(metrics, StreetNetworkStats::getDominantHighwayType),
                    formatStreet(metrics, s -> s.getOnewayRatio() * 100),
                    formatStreet(metrics, StreetNetworkStats::getStreetsPerNode),
                    wbCountryCode(metrics),
                    wbValue(metrics, WorldBankIndicators.GDP_PER_CAPITA),
                    wbValue(metrics, WorldBankIndicators.URBAN_POP_PCT),
                    wbValue(metrics, WorldBankIndicators.CO2_PER_CAPITA)
                };
                writer.writeNext(data);
            }
        }
        logger.info("Multiple metrics exported successfully");
        return outputFile;
    }
    public File generateOutputFilename(File inputFile, String suffix) {
        String name = inputFile.getName();
        if (name.endsWith(".graphml")) {
            name = name.substring(0, name.length() - 8);
        }
        String outputName = name + suffix + ".csv";
        return new File(inputFile.getParent(), outputName);
    }
    private String formatDouble(Double value) {
        return value != null ? String.format("%.6f", value) : "";
    }
    private String formatInteger(Integer value) {
        return value != null ? String.valueOf(value) : "";
    }
    private String formatStreet(NetworkMetrics m, java.util.function.Function<StreetNetworkStats, Double> getter) {
        if (m.getStreetStats() == null) return "";
        Double v = getter.apply(m.getStreetStats());
        return v != null ? String.format("%.4f", v) : "";
    }
    private String formatStreetStr(NetworkMetrics m, java.util.function.Function<StreetNetworkStats, String> getter) {
        if (m.getStreetStats() == null) return "";
        String v = getter.apply(m.getStreetStats());
        return v != null ? v : "";
    }
    private String formatStreetInt(NetworkMetrics m, java.util.function.Function<StreetNetworkStats, Integer> getter) {
        if (m.getStreetStats() == null) return "";
        Integer v = getter.apply(m.getStreetStats());
        return v != null ? String.valueOf(v) : "";
    }
    private static String wbCountryCode(NetworkMetrics m) {
        if (m.getWorldBankData() == null || m.getWorldBankData().getRefAreaCode() == null) return "";
        return m.getWorldBankData().getRefAreaCode();
    }
    private static String wbValue(NetworkMetrics m, WorldBankIndicators indicator) {
        Double v = WorldBankAnalysisUtil.getValue(m, indicator);
        return v != null ? String.format("%.4f", v) : "";
    }
}