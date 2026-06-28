package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.example.service.WorldBankIndicators;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/*
Generates a human-readable analysis report from a list of NetworkMetrics.
Report sections:
0. Research context
1. Summary statistics
2. Centrality rankings
3. Structural rankings
4. Automated observations (benchmarked against paper's 62-city dataset)
5. UMRi composite predictor score per city
6. Full metrics table
*/
public class NetworkAnalysisReportService {
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SEP  = "=".repeat(70) + "\n";
    private static final String DASH = "-".repeat(70) + "\n";
    public static String generateTechnicalDetails(List<NetworkMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return "Process GraphML files to see graph-structure metrics and OSM data coverage here.\n\n"
                    + "This panel shows mathematical network metrics (clustering, density, path length),\n"
                    + "GraphML attribute schemas, and raw OSM tag coverage; details aimed at analysts.\n"
                    + "Everyday street statistics appear on the Dashboard tab.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("TECHNICAL DETAILS  (graph structure + OSM data coverage)\n");
        sb.append("Auto-updated when networks are processed. For street summaries see the Dashboard.\n");
        sb.append(SEP).append("\n");

        sb.append(section1_Summary(metricsList));
        sb.append(section1c_GraphMetricsTable(metricsList));
        sb.append(section1d_OsmTechnicalDetails(metricsList));
        return sb.toString();
    }
    public static String generateReport(List<NetworkMetrics> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return "No graph metrics available. Load and process GraphML files first.";
        }
        StringBuilder sb = new StringBuilder();
        sb.append(SEP);
        sb.append("  URBAN MOBILITY NETWORK ANALYSIS REPORT\n");
        sb.append("  Generated : ").append(LocalDateTime.now().format(FMT)).append("\n");
        sb.append("  Networks  : ").append(metricsList.size()).append(" city graph(s)\n");
        sb.append(SEP).append("\n");
        sb.append("  Reference: Sierra-Porta & Herrera-Acevedo (2024)\n");
        sb.append("  'Network structure and urban mobility sustainability'\n");
        sb.append("  Universidad Tecnológica de Bolívar (UTB)\n\n");
        sb.append(section0_ResearchContext());
        sb.append(section1_Summary(metricsList));
        sb.append(section1b_StreetInfrastructure(metricsList));
        sb.append(section2_CentralityRankings(metricsList));
        sb.append(section3_StructureRankings(metricsList));
        sb.append(section4_Observations(metricsList));
        sb.append(section5_CompositeScore(metricsList));
        sb.append(section6_WorldBankContext(metricsList));
        sb.append(section7_FullTable(metricsList));
        return sb.toString();
    }
    private static String section0_ResearchContext() {
        return SEP
                + "0. RESEARCH CONTEXT\n" + DASH
                + "  This report contextualises computed graph metrics against the UTB paper.\n\n"
                + "  The paper analysed 62 world cities using OSMnx-extracted street networks\n"
                + "  and correlated graph metrics with:\n"
                + "    - UMRi: Urban Mobility Readiness Index   (Oliver Wyman Forum 2023)\n"
                + "    - SMi: Sustainable Mobility Index\n"
                + "    - PTi: Public Transit Index\n\n"
                + "  Key OLS regression results for UMRi  (R² = 0.815, adj-R² = 0.755):\n\n"
                + "    Variable                   Coeff.   p-value   Direction\n"
                + "    " + "-".repeat(56) + "\n"
                + "    Clustering Coefficient     +190.4   < 0.001   ↑ higher is better\n"
                + "    Assortativity Degree        -45.2   < 0.001   ↓ lower  is better\n"
                + "    Streets per Node (avg)      +29.1     0.001   ↑ higher is better\n"
                + "    Graph Diameter              -0.05     0.014   ↓ lower  is better\n"
                + "    GDP per capita              +0.0002 < 0.001   ↑ [socioeconomic]\n\n"
                + "  Key Kendall correlations with UMRi:\n"
                + "    Degree Centrality  +0.31   Graph Diameter  -0.35\n"
                + "    Clustering         +0.32   Avg Path Length -0.36\n"
                + "    Betweenness        +0.24   Graph Entropy   -0.30\n"
                + "    Density            +0.31   Closeness       -0.24\n\n"
                + "  Interpretation key used throughout this report:\n"
                + "    ↑ = higher value - better urban mobility readiness\n"
                + "    ↓ = lower  value - better urban mobility readiness\n\n";
    }
    private static String section1_Summary(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("1. SUMMARY STATISTICS\n").append(DASH);
        DoubleSummaryStatistics nodeS = list.stream().mapToDouble(NetworkMetrics::getNodeCount).summaryStatistics();
        DoubleSummaryStatistics edgeS = list.stream().mapToDouble(NetworkMetrics::getEdgeCount).summaryStatistics();
        sb.append("  City networks loaded:  ").append(list.size()).append("\n\n");
        sb.append("  Size\n");
        sb.append("    Nodes - min/mean/max:  ")
                .append(fmt(nodeS.getMin())).append(" / ").append(fmt(nodeS.getAverage()))
                .append(" / ").append(fmt(nodeS.getMax())).append("\n");
        sb.append("    Edges - min/mean/max:  ")
                .append(fmt(edgeS.getMin())).append(" / ").append(fmt(edgeS.getAverage()))
                .append(" / ").append(fmt(edgeS.getMax())).append("\n\n");
        sb.append("  Centrality (avg across all networks)\n");
        appendAvg(sb, "    Betweenness Centrality  ↑ : ", list, m -> m.getAvgBetweennessCentrality());
        appendAvg(sb, "    Closeness Centrality    ↓ : ", list, m -> m.getAvgClosenessCentrality());
        appendAvg(sb, "    Degree Centrality       ↑ : ", list, m -> m.getAvgDegreeCentrality());

        sb.append("\n  Structural metrics\n");
        appendAvg(sb, "    Clustering Coefficient  ↑ : ", list, m -> m.getClusteringCoefficient());
        appendAvg(sb, "    Graph Density           ↑ : ", list, m -> m.getGraphDensity());
        appendAvg(sb, "    Graph Entropy           ↓ : ", list, m -> m.getGraphEntropy());
        appendAvg(sb, "    Avg Path Length         ↓ : ", list, m -> m.getAvgPathLength());

        OptionalDouble avgDiam = list.stream()
                .filter(m -> m.getGraphDiameter() != null)
                .mapToDouble(NetworkMetrics::getGraphDiameter).average();
        sb.append("    Graph Diameter          ↓ : ")
                .append(avgDiam.isPresent() ? String.format("%.1f hops", avgDiam.getAsDouble()) : "N/A")
                .append("\n");

        sb.append("\n  Additional metrics\n");
        appendAvg(sb, "    Mean Degree               : ", list, m -> m.getMeanDegree());
        appendAvg(sb, "    Assortativity Degree    ↓ : ", list, m -> m.getAssortativityDegree());
        appendAvg(sb, "    Degree Variance           : ", list, m -> m.getDiversity());

        long dir = list.stream().filter(NetworkMetrics::isDirected).count();
        sb.append("\n  Directed: ").append(dir)
                .append("  |  Undirected: ").append(list.size() - dir).append("\n\n");
        return sb.toString();
    }
    private static String section1b_StreetInfrastructure(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("1b. STREET NETWORK INFRASTRUCTURE  (from GraphML OSM attributes)\n").append(DASH);
        List<NetworkMetrics> withStreet = list.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .toList();
        if (withStreet.isEmpty()) {
            sb.append("  No OSM street metadata found in loaded GraphML files.\n\n");
            return sb.toString();
        }
        sb.append("  Networks with OSM data: ").append(withStreet.size()).append(" / ").append(list.size()).append("\n\n");

        sb.append("  Dataset averages (OSM-derived)\n");
        appendStreetAvg(sb, "    Total road length (km)     : ", withStreet, s -> s.getTotalLengthKm());
        appendStreetAvg(sb, "    Streets per node           : ", withStreet, StreetNetworkStats::getStreetsPerNode);
        appendStreetAvg(sb, "    Avg street count / node    : ", withStreet, StreetNetworkStats::getAvgStreetCountPerNode);
        appendStreetAvg(sb, "    Oneway segment ratio       : ", withStreet, s -> s.getOnewayRatio() * 100);
        appendStreetAvg(sb, "    Avg max speed (km/h)       : ", withStreet, StreetNetworkStats::getAvgMaxSpeedKmh);
        appendStreetAvg(sb, "    Avg lanes                  : ", withStreet, StreetNetworkStats::getAvgLanes);
        appendStreetAvg(sb, "    Residential length %       : ", withStreet, StreetNetworkStats::getPctResidential);
        appendStreetAvg(sb, "    Car-oriented length %      : ", withStreet, StreetNetworkStats::getCarOrientedLengthPct);
        appendStreetAvg(sb, "    Pedestrian-friendly %      : ", withStreet, StreetNetworkStats::getPedestrianFriendlyLengthPct);
        appendStreetAvg(sb, "    Reversed segment ratio     : ", withStreet, s -> s.getReversedRatio() * 100);
        appendStreetAvg(sb, "    Avg width (m)              : ", withStreet, StreetNetworkStats::getAvgWidthMeters);
        appendStreetAvg(sb, "    Avg est. width (m)         : ", withStreet, StreetNetworkStats::getAvgEstWidthMeters);
        appendStreetAvg(sb, "    Restricted access length % : ", withStreet, StreetNetworkStats::getRestrictedAccessLengthPct);
        appendStreetAvg(sb, "    Named roads ratio          : ", withStreet, s -> s.getNamedRoadRatio() * 100);
        appendStreetAvg(sb, "    Bridges (count)            : ", withStreet, s -> (double) s.getBridgeCount());
        appendStreetAvg(sb, "    Tunnels (count)            : ", withStreet, s -> (double) s.getTunnelCount());
        appendStreetAvg(sb, "    Edges with geometry        : ", withStreet, s -> (double) s.getEdgesWithGeometry());
        appendStreetAvg(sb, "    BBox span (km)             : ", withStreet, StreetNetworkStats::getBoundingBoxSpanKm);

        sb.append("\n  Per-city street profile\n");
        sb.append("    ").append(padR("City", 22))
                .append(padR("Road km", 9))
                .append(padR("Dominant", 12))
                .append(padR("1way%", 6))
                .append(padR("Spd", 5))
                .append(padR("Ln", 4))
                .append(padR("Br/Tn", 7))
                .append(padR("Ped%", 6))
                .append(padR("St/N", 6))
                .append("Attrs\n");
        sb.append("    ").append("-".repeat(82)).append("\n");

        for (NetworkMetrics m : withStreet) {
            StreetNetworkStats s = m.getStreetStats();
            sb.append("    ").append(padR(m.getGraphName(), 22))
                    .append(padR(String.format("%.0f", s.getTotalLengthKm()), 9))
                    .append(padR(nullToDash(s.getDominantHighwayType()), 12))
                    .append(padR(String.format("%.0f", s.getOnewayRatio() * 100), 6))
                    .append(padR(s.getAvgMaxSpeedKmh() > 0 ? String.format("%.0f", s.getAvgMaxSpeedKmh()) : "-", 5))
                    .append(padR(s.getAvgLanes() > 0 ? String.format("%.1f", s.getAvgLanes()) : "-", 4))
                    .append(padR(s.getBridgeCount() + "/" + s.getTunnelCount(), 7))
                    .append(padR(String.format("%.0f", s.getPedestrianFriendlyLengthPct()), 6))
                    .append(padR(String.format("%.1f", s.getStreetsPerNode()), 6))
                    .append(s.getEdgeAttributeCount()).append("e/").append(s.getNodeAttributeCount()).append("n")
                    .append("\n");
        }

        sb.append("\n  GraphML attribute schema (example from first network)\n");
        StreetNetworkStats sample = withStreet.get(0).getStreetStats();
        sb.append("    Edge fields : ").append(String.join(", ", sample.getEdgeAttributeNames())).append("\n");
        sb.append("    Node fields : ").append(String.join(", ", sample.getNodeAttributeNames())).append("\n");
        sb.append("    Graph fields: ").append(String.join(", ", sample.getGraphAttributeNames())).append("\n");

        sb.append("\n  Top highway types (edge counts, all cities combined)\n");
        Map<String, Integer> combined = new LinkedHashMap<>();
        for (NetworkMetrics m : withStreet) {
            for (var e : m.getStreetStats().getHighwayTypeCounts().entrySet()) {
                combined.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        combined.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .forEach(e -> sb.append("    ").append(padR(e.getKey(), 18))
                        .append(String.format("%,d segments\n", e.getValue())));

        sb.append(osmTechnicalFooter());
        return sb.toString();
    }
    private static String section1c_GraphMetricsTable(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("1c. PER-NETWORK GRAPH METRICS\n").append(DASH);
        sb.append("    ").append(padR("Graph", 22))
                .append(padR("Nodes", 8))
                .append(padR("Edges", 8))
                .append(padR("Clust.", 8))
                .append(padR("Density", 9))
                .append(padR("Path", 8))
                .append(padR("Diam.", 6))
                .append(padR("Betw.", 8))
                .append("Entropy\n");
        sb.append("    ").append("-".repeat(85)).append("\n");

        for (NetworkMetrics m : list.stream().sorted(Comparator.comparing(NetworkMetrics::getGraphName)).toList()) {
            sb.append("    ").append(padR(m.getGraphName(), 22))
                    .append(padR(String.valueOf(m.getNodeCount()), 8))
                    .append(padR(String.valueOf(m.getEdgeCount()), 8))
                    .append(padR(f4(m.getClusteringCoefficient()), 8))
                    .append(padR(f4(m.getGraphDensity()), 9))
                    .append(padR(f4(m.getAvgPathLength()), 8))
                    .append(padR(m.getGraphDiameter() != null ? String.valueOf(m.getGraphDiameter()) : "N/A", 6))
                    .append(padR(f4(m.getAvgBetweennessCentrality()), 8))
                    .append(f4(m.getGraphEntropy())).append("\n");
        }
        sb.append("\n");
        return sb.toString();
    }
    private static String section1d_OsmTechnicalDetails(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("1d. OSM DATA COVERAGE & SCHEMA\n").append(DASH);

        List<NetworkMetrics> withStreet = list.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .toList();
        if (withStreet.isEmpty()) {
            sb.append("  No OSM street metadata found in loaded GraphML files.\n\n");
            return sb.toString();
        }

        sb.append("  GraphML attribute coverage per network\n");
        sb.append("    ").append(padR("Graph", 22))
                .append(padR("Edge Attrs", 11))
                .append(padR("Node Attrs", 11))
                .append(padR("Geometry", 10))
                .append(padR("OSM IDs", 9))
                .append(padR("Reversed%", 10))
                .append(padR("Restricted%", 11))
                .append("BBox\n");
        sb.append("    ").append("-".repeat(90)).append("\n");

        for (NetworkMetrics m : withStreet) {
            StreetNetworkStats s = m.getStreetStats();
            sb.append("    ").append(padR(m.getGraphName(), 22))
                    .append(padR(String.valueOf(s.getEdgeAttributeCount()), 11))
                    .append(padR(String.valueOf(s.getNodeAttributeCount()), 11))
                    .append(padR(String.valueOf(s.getEdgesWithGeometry()), 10))
                    .append(padR(String.valueOf(s.getEdgesWithOsmid()), 9))
                    .append(padR(String.format("%.0f", s.getReversedRatio() * 100), 10))
                    .append(padR(String.format("%.1f", s.getRestrictedAccessLengthPct()), 11))
                    .append(s.getBoundingBoxSpanKm() > 0
                            ? String.format("%.1f km", s.getBoundingBoxSpanKm()) : "N/A")
                    .append("\n");
        }

        sb.append("\n  GraphML attribute schema (from first network)\n");
        StreetNetworkStats sample = withStreet.get(0).getStreetStats();
        sb.append("    Edge fields : ").append(String.join(", ", sample.getEdgeAttributeNames())).append("\n");
        sb.append("    Node fields : ").append(String.join(", ", sample.getNodeAttributeNames())).append("\n");
        sb.append("    Graph fields: ").append(String.join(", ", sample.getGraphAttributeNames())).append("\n");

        sb.append("\n  Top highway types (edge counts, all cities combined)\n");
        Map<String, Integer> combined = new LinkedHashMap<>();
        for (NetworkMetrics m : withStreet) {
            for (var e : m.getStreetStats().getHighwayTypeCounts().entrySet()) {
                combined.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        combined.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .forEach(e -> sb.append("    ").append(padR(e.getKey(), 18))
                        .append(String.format("%,d segments\n", e.getValue())));

        sb.append(osmTechnicalFooter());
        return sb.toString();
    }
    private static String osmTechnicalFooter() {
        return "\n  Note: Every GraphML file defines d0..dN keys in its header; the parser resolves\n"
                + "  each data line via attr.name (highway, length, maxspeed, lanes, oneway, reversed,\n"
                + "  geometry, access, junction, width, est_width, service, area, bridge, tunnel,\n"
                + "  osmid, ref, name, x, y, street_count, etc.) regardless of d-number ordering.\n\n";
    }
    private static void appendStreetAvg(StringBuilder sb, String label,
                                        List<NetworkMetrics> list,
                                        java.util.function.Function<StreetNetworkStats, Double> getter) {
        OptionalDouble avg = list.stream()
                .map(NetworkMetrics::getStreetStats)
                .filter(Objects::nonNull)
                .mapToDouble(getter::apply)
                .average();
        sb.append(label).append(avg.isPresent() ? String.format("%.2f", avg.getAsDouble()) : "N/A").append("\n");
    }
    private static String nullToDash(String s) {
        return s != null && !s.isBlank() ? s : "-";
    }
    private static String section2_CentralityRankings(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("2. CENTRALITY RANKINGS  (Top 5)\n").append(DASH);

        sb.append("  Highest Betweenness ↑: most critical transit hubs  (Kendall +0.24)\n");
        topN(list, m -> m.getAvgBetweennessCentrality(), 5).forEach(m ->
                sb.append("    ").append(padR(m.getGraphName(),26)).append(f6(m.getAvgBetweennessCentrality())).append("\n"));

        sb.append("\n  Highest Closeness ↓: nodes closest to all others  (Kendall -0.24)\n");
        sb.append("  [High closeness alone does not predict better UMRi; see research context]\n");
        topN(list, m -> m.getAvgClosenessCentrality(), 5).forEach(m ->
                sb.append("    ").append(padR(m.getGraphName(),26)).append(f6(m.getAvgClosenessCentrality())).append("\n"));

        sb.append("\n  Highest Degree Centrality ↑: most connected hubs  (Kendall +0.31, p=0.001)\n");
        topN(list, m -> m.getAvgDegreeCentrality(), 5).forEach(m ->
                sb.append("    ").append(padR(m.getGraphName(),26)).append(f6(m.getAvgDegreeCentrality())).append("\n"));

        sb.append("\n");
        return sb.toString();
    }
    private static String section3_StructureRankings(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("3. STRUCTURAL RANKINGS\n").append(DASH);

        sb.append("  Most clustered ↑  [strongest positive UMRi predictor, p < 0.001]\n");
        topN(list, m -> m.getClusteringCoefficient(), 5).forEach(m ->
                sb.append("    ").append(padR(m.getGraphName(),26)).append(f6(m.getClusteringCoefficient())).append("\n"));

        sb.append("\n  Densest networks ↑  [Kendall +0.31 with UMRi]\n");
        topN(list, m -> m.getGraphDensity(), 5).forEach(m ->
                sb.append("    ").append(padR(m.getGraphName(),26)).append(f6(m.getGraphDensity())).append("\n"));

        sb.append("\n  Smallest diameter ↓  [negative predictor p=0.014; smaller = more accessible]\n");
        list.stream().filter(m -> m.getGraphDiameter() != null)
                .sorted(Comparator.comparingInt(NetworkMetrics::getGraphDiameter)).limit(5)
                .forEach(m -> sb.append("    ").append(padR(m.getGraphName(),26))
                        .append(m.getGraphDiameter()).append(" hops\n"));

        sb.append("\n  Shortest avg path ↓  [Kendall -0.36 with UMRi]\n");
        list.stream().filter(m -> m.getAvgPathLength() != null)
                .sorted(Comparator.comparingDouble(NetworkMetrics::getAvgPathLength)).limit(5)
                .forEach(m -> sb.append("    ").append(padR(m.getGraphName(),26))
                        .append(f6(m.getAvgPathLength())).append("\n"));

        sb.append("\n  Lowest entropy ↓  [more ordered degree dist.; Kendall -0.30]\n");
        list.stream().filter(m -> m.getGraphEntropy() != null)
                .sorted(Comparator.comparingDouble(NetworkMetrics::getGraphEntropy)).limit(5)
                .forEach(m -> sb.append("    ").append(padR(m.getGraphName(),26))
                        .append(f6(m.getGraphEntropy())).append(" bits\n"));

        sb.append("\n");
        return sb.toString();
    }
    private static String section4_Observations(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("4. AUTOMATED OBSERVATIONS\n").append(DASH);
        sb.append("  Benchmarks derived from the 62-city analysis in the paper.\n\n");

        OptionalDouble avgCC = avg(list, m -> m.getClusteringCoefficient());
        if (avgCC.isPresent()) {
            double cc = avgCC.getAsDouble();
            String tier = cc > 0.06 ? "HIGH" : cc > 0.03 ? "MODERATE" : "LOW";
            sb.append("  Clustering Coefficient: ").append(tier)
                    .append(" (avg = ").append(sf4(cc)).append(")\n");
            if (cc > 0.06)
                sb.append("    Tightly-knit neighbourhoods comparable to top UMRi cities (Helsinki, Amsterdam).\n");
            else if (cc < 0.03)
                sb.append("    Sparse local connectivity associated with lower UMRi rankings.\n");
            else
                sb.append("    Moderate clustering; densifying local connections could improve readiness.\n");
            sb.append("\n");
        }

        OptionalDouble avgD = list.stream().filter(m -> m.getGraphDiameter() != null)
                .mapToDouble(NetworkMetrics::getGraphDiameter).average();
        if (avgD.isPresent()) {
            double d = avgD.getAsDouble();
            String tier = d < 40 ? "COMPACT" : d < 80 ? "MODERATE" : "LARGE";
            sb.append("  Graph Diameter: ").append(tier)
                    .append(" (avg = ").append(sf4(d)).append(" hops)\n");
            if (d > 80)
                sb.append("    Dispersed network; significant structural distance between outlying areas.\n");
            else if (d < 40)
                sb.append("    Compact network consistent with high-UMRi cities.\n");
            sb.append("\n");
        }

        OptionalDouble avgAD = avg(list, m -> m.getAssortativityDegree());
        if (avgAD.isPresent()) {
            double ad = avgAD.getAsDouble();
            if (ad > 0.05)
                sb.append("  Assortativity [high] (avg = ").append(sf4(ad)).append(")\n")
                        .append("    Hubs cluster with other hubs, creating bottlenecks between zones.\n")
                        .append("    This is a STRONG NEGATIVE predictor of UMRi (p < 0.001).\n\n");
            else if (ad < -0.05)
                sb.append("  Assortativity [good] disassortative (avg = ").append(sf4(ad)).append(")\n")
                        .append("    Hubs connect to peripheral nodes, efficient transit-like topology.\n\n");
        }

        OptionalDouble avgE = avg(list, m -> m.getGraphEntropy());
        if (avgE.isPresent()) {
            double en = avgE.getAsDouble();
            if (en > 2.5)
                sb.append("  Entropy [high] (avg = ").append(sf4(en)).append(" bits)\n")
                        .append("    Very heterogeneous degree distribution; few super-hubs dominate routing.\n\n");
            else
                sb.append("  Entropy [good] ordered (avg = ").append(sf4(en)).append(" bits)\n")
                        .append("    Relatively homogeneous intersection connectivity supports predictable routing.\n\n");
        }

        return sb.toString();
    }
    private static String section5_CompositeScore(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("5. COMPOSITE STRUCTURAL READINESS SCORE\n").append(DASH);
        sb.append("  Combines the five structural UMRi predictors (min-max normalised):\n");
        sb.append("  Score = mean( CC↑, Density↑, DegCent↑, 1-Diameter, 1-AvgPath, 1-Assort )\n\n");

        double[] ccArr     = list.stream().mapToDouble(m -> d(m.getClusteringCoefficient())).toArray();
        double[] denArr    = list.stream().mapToDouble(m -> d(m.getGraphDensity())).toArray();
        double[] dcArr     = list.stream().mapToDouble(m -> d(m.getAvgDegreeCentrality())).toArray();
        double[] diamArr   = list.stream().mapToDouble(m -> m.getGraphDiameter() != null ? m.getGraphDiameter() : 0.0).toArray();
        double[] plArr     = list.stream().mapToDouble(m -> d(m.getAvgPathLength())).toArray();
        double[] adArr     = list.stream().mapToDouble(m -> d(m.getAssortativityDegree())).toArray();

        double[] score = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            score[i] = (norm(ccArr, i, false) + norm(denArr, i, false) + norm(dcArr, i, false)
                    + norm(diamArr, i, true) + norm(plArr, i, true) + norm(adArr, i, true)) / 6.0;
        }

        Integer[] idx = new Integer[list.size()];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        Arrays.sort(idx, (a, b) -> Double.compare(score[b], score[a]));

        sb.append("    ")
                .append(padR("Rk", 4))
                .append(padR("Network", 24))
                .append(padR("Cluster.", 9))
                .append(padR("Density", 9))
                .append(padR("DegCent", 9))
                .append(padR("Diam", 7))
                .append(padR("AvgPath", 9))
                .append(padR("Assort.", 9))
                .append(padR("Score", 7))
                .append("\n");
        sb.append("    ").append("-".repeat(92)).append("\n");

        for (int rank = 0; rank < idx.length; rank++) {
            int i = idx[rank];
            NetworkMetrics m = list.get(i);
            sb.append("    ")
                    .append(padR(String.valueOf(rank + 1), 4))
                    .append(padR(m.getGraphName(), 24))
                    .append(padR(f4(m.getClusteringCoefficient()), 9))
                    .append(padR(f4(m.getGraphDensity()), 9))
                    .append(padR(f4(m.getAvgDegreeCentrality()), 9))
                    .append(padR(m.getGraphDiameter() != null ? String.valueOf(m.getGraphDiameter()) : "N/A", 7))
                    .append(padR(f4(m.getAvgPathLength()), 9))
                    .append(padR(f4(m.getAssortativityDegree()), 9))
                    .append(padR(String.format("%.3f", score[i]), 7))
                    .append("\n");
        }

        sb.append("\n  Note: This score ranks structural mobility readiness within the loaded set only.\n");
        sb.append("    It does NOT account for GDP or socioeconomic factors from the full UMRi.\n\n");
        return sb.toString();
    }
    private static String section6_WorldBankContext(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("6. WORLD BANK COUNTRY CONTEXT  (Data360 / WDI)\n").append(DASH);
        sb.append("  Source: World Bank Data360 API, World Development Indicators.\n");
        sb.append("  City/country parsed from GraphML filenames. Values are country-level.\n\n");

        long withData = list.stream()
                .filter(m -> m.getWorldBankData() != null && m.getWorldBankData().isUsable())
                .count();
        sb.append("  Cities with World Bank data: ").append(withData).append(" / ").append(list.size()).append("\n\n");

        if (withData == 0) {
            sb.append("  No World Bank data loaded. Process files while online to fetch indicators.\n\n");
            return sb.toString();
        }

        sb.append("    ")
                .append(padR("City", 24))
                .append(padR("Code", 6))
                .append(padR("GDP/cap", 12))
                .append(padR("Urban%", 10))
                .append(padR("CO2/cap", 10))
                .append(padR("Logistics", 10))
                .append(padR("Trans.Infra", 11))
                .append("\n");
        sb.append("    ").append("-".repeat(83)).append("\n");

        for (NetworkMetrics m : list) {
            WorldBankCityData wb = m.getWorldBankData();
            if (wb == null || !wb.isUsable()) continue;
            sb.append("    ")
                    .append(padR(m.getGraphName(), 24))
                    .append(padR(wb.getRefAreaCode() != null ? wb.getRefAreaCode() : "-", 6))
                    .append(padR(formatWb(wb, WorldBankIndicators.GDP_PER_CAPITA), 12))
                    .append(padR(formatWb(wb, WorldBankIndicators.URBAN_POP_PCT), 10))
                    .append(padR(formatWb(wb, WorldBankIndicators.CO2_PER_CAPITA), 10))
                    .append(padR(formatWb(wb, WorldBankIndicators.LOGISTICS_INDEX), 10))
                    .append(padR(formatWb(wb, WorldBankIndicators.TRANSPORT_INFRA), 11))
                    .append("\n");
        }

        long countries = list.stream()
                .map(NetworkMetrics::getWorldBankData)
                .filter(Objects::nonNull)
                .map(WorldBankCityData::getRefAreaCode)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        sb.append("\n  Summary: ").append(withData).append(" cities with data across ")
                .append(countries).append(" countries. Values are country-level (shared by cities in the same country).\n");
        sb.append("  Note: Motor-vehicle and paved-road series are not published in Data360; logistics and transport-infrastructure indices are shown instead.\n\n");
        return sb.toString();
    }
    private static String formatWb(WorldBankCityData wb, WorldBankIndicators key) {
        return wb.findIndicator(key.getId())
                .filter(WorldBankIndicatorValue::hasValue)
                .map(WorldBankIndicatorValue::formattedValue)
                .orElse("N/A");
    }
    private static String section7_FullTable(List<NetworkMetrics> list) {
        StringBuilder sb = new StringBuilder();
        sb.append("7. FULL METRICS TABLE  (sorted by node count, descending)\n").append(DASH);
        sb.append("    ")
                .append(padR("Network", 24))
                .append(padR("Nodes", 8))
                .append(padR("Edges", 8))
                .append(padR("Betw.", 10))
                .append(padR("Clos.", 10))
                .append(padR("Deg.", 10))
                .append(padR("Clust.", 9))
                .append(padR("Diam", 6))
                .append(padR("Density", 9))
                .append(padR("AvgPath", 9))
                .append("\n");
        sb.append("    ").append("-".repeat(103)).append("\n");
        list.stream()
                .sorted(Comparator.comparingInt(NetworkMetrics::getNodeCount).reversed())
                .forEach(m -> sb.append("    ")
                        .append(padR(m.getGraphName(), 24))
                        .append(padR(String.valueOf(m.getNodeCount()), 8))
                        .append(padR(String.valueOf(m.getEdgeCount()), 8))
                        .append(padR(f6(m.getAvgBetweennessCentrality()), 10))
                        .append(padR(f6(m.getAvgClosenessCentrality()), 10))
                        .append(padR(f6(m.getAvgDegreeCentrality()), 10))
                        .append(padR(f6(m.getClusteringCoefficient()), 9))
                        .append(padR(m.getGraphDiameter() != null ? String.valueOf(m.getGraphDiameter()) : "N/A", 6))
                        .append(padR(f6(m.getGraphDensity()), 9))
                        .append(padR(f6(m.getAvgPathLength()), 9))
                        .append("\n"));

        return sb.toString();
    }
    @FunctionalInterface interface DG { Double get(NetworkMetrics m); }
    private static List<NetworkMetrics> topN(List<NetworkMetrics> list, DG g, int n) {
        return list.stream().filter(m -> g.get(m) != null)
                .sorted(Comparator.comparingDouble(m -> -g.get(m))).limit(n)
                .collect(Collectors.toList());
    }
    private static void appendAvg(StringBuilder sb, String label, List<NetworkMetrics> list, DG g) {
        OptionalDouble a = list.stream().filter(m -> g.get(m) != null).mapToDouble(m -> g.get(m)).average();
        sb.append(label).append(a.isPresent() ? String.format("%.6f", a.getAsDouble()) : "N/A").append("\n");
    }
    private static OptionalDouble avg(List<NetworkMetrics> list, DG g) {
        return list.stream().filter(m -> g.get(m) != null).mapToDouble(m -> g.get(m)).average();
    }
    private static double d(Double v)  { return v != null ? v : 0.0; }
    private static double norm(double[] arr, int i, boolean invertForBetter) {
        double min = Arrays.stream(arr).min().orElse(0);
        double max = Arrays.stream(arr).max().orElse(1);
        if (max == min) return 0.5;
        double n = (arr[i] - min) / (max - min);
        return invertForBetter ? 1.0 - n : n;
    }
    private static String fmt(double d) {
        return d == Math.floor(d) ? String.valueOf((long) d) : String.format("%.1f", d);
    }
    private static String f6(Double v)  { return v != null ? String.format("%.6f", v) : "   N/A  "; }
    private static String f4(Double v)  { return v != null ? String.format("%.4f", v) : "  N/A"; }
    private static String sf4(double v) { return String.format("%.4f", v); }
    private static String padR(String s, int len) {
        if (s == null) s = "";
        return s.length() >= len ? s.substring(0, len) : s + " ".repeat(len - s.length());
    }
}