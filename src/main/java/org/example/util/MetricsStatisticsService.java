package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.service.CityScoresLoader;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

// Shared statistics, rankings, and pairwise comparisons used by charts, reports, and the AI insight engine.
public final class MetricsStatisticsService {
    private MetricsStatisticsService() {}
  @FunctionalInterface
  public interface MetricExtractor {
    Double get(NetworkMetrics m);
    default String label() { return "metric"; }
  }
  public record RankedCity(NetworkMetrics metrics, double compositeScore, int rank) {}
  public record PairComparison(
      NetworkMetrics cityA,
      NetworkMetrics cityB,
      List<String> lines
  ) {}
  public static double[] computeCompositeScores(List<NetworkMetrics> list) {
    if (list.isEmpty()) return new double[0];
    double[] ccArr   = list.stream().mapToDouble(m -> d(m.getClusteringCoefficient())).toArray();
    double[] denArr  = list.stream().mapToDouble(m -> d(m.getGraphDensity())).toArray();
    double[] dcArr   = list.stream().mapToDouble(m -> d(m.getAvgDegreeCentrality())).toArray();
    double[] diamArr = list.stream().mapToDouble(m -> m.getGraphDiameter() != null ? m.getGraphDiameter() : 0.0).toArray();
    double[] plArr   = list.stream().mapToDouble(m -> d(m.getAvgPathLength())).toArray();
    double[] adArr   = list.stream().mapToDouble(m -> d(m.getAssortativityDegree())).toArray();
    double[] score = new double[list.size()];
    for (int i = 0; i < list.size(); i++) {
      score[i] = (norm(ccArr, i, false) + norm(denArr, i, false) + norm(dcArr, i, false)
          + norm(diamArr, i, true) + norm(plArr, i, true) + norm(adArr, i, true)) / 6.0;
    }
    return score;
  }
  public static List<RankedCity> rankByCompositeScore(List<NetworkMetrics> list) {
    double[] scores = computeCompositeScores(list);
    Integer[] idx = new Integer[list.size()];
    for (int i = 0; i < idx.length; i++) idx[i] = i;
    Arrays.sort(idx, (a, b) -> Double.compare(scores[b], scores[a]));

    List<RankedCity> ranked = new ArrayList<>();
    for (int rank = 0; rank < idx.length; rank++) {
      int i = idx[rank];
      ranked.add(new RankedCity(list.get(i), scores[i], rank + 1));
    }
    return ranked;
  }
  public static NetworkMetrics bestCity(List<NetworkMetrics> list) {
    return rankByCompositeScore(list).isEmpty() ? null : rankByCompositeScore(list).get(0).metrics();
  }
  public static NetworkMetrics worstCity(List<NetworkMetrics> list) {
    List<RankedCity> ranked = rankByCompositeScore(list);
    return ranked.isEmpty() ? null : ranked.get(ranked.size() - 1).metrics();
  }
  public static String extractCountry(NetworkMetrics m) {
    String name = m.getGraphName();
    if (name == null) return "Unknown";
    int comma = name.lastIndexOf(',');
    if (comma >= 0 && comma < name.length() - 1) {
      return name.substring(comma + 1).trim().replace('_', ' ');
    }
    return "Unknown";
  }
  public static Map<String, List<NetworkMetrics>> groupByCountry(List<NetworkMetrics> list) {
    return list.stream().collect(Collectors.groupingBy(MetricsStatisticsService::extractCountry));
  }
  public static Map<String, Double> averageMetricByCountry(
      List<NetworkMetrics> list, MetricExtractor extractor) {
    Map<String, Double> result = new LinkedHashMap<>();
    groupByCountry(list).forEach((country, cities) -> {
      OptionalDouble avg = cities.stream()
          .filter(m -> extractor.get(m) != null)
          .mapToDouble(m -> extractor.get(m))
          .average();
      if (avg.isPresent()) result.put(country, avg.getAsDouble());
    });
    return result;
  }
  public static double pearsonCorrelation(List<NetworkMetrics> list,
                                          MetricExtractor a, MetricExtractor b) {
    List<Double> xs = new ArrayList<>();
    List<Double> ys = new ArrayList<>();
    for (NetworkMetrics m : list) {
      Double x = a.get(m);
      Double y = b.get(m);
      if (x != null && y != null) {
        xs.add(x);
        ys.add(y);
      }
    }
  if (xs.size() < 3) return 0.0;
    double meanX = xs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double meanY = ys.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    double sumXY = 0, sumX2 = 0, sumY2 = 0;
    for (int i = 0; i < xs.size(); i++) {
      double dx = xs.get(i) - meanX;
      double dy = ys.get(i) - meanY;
      sumXY += dx * dy;
      sumX2 += dx * dx;
      sumY2 += dy * dy;
    }
    if (sumX2 == 0 || sumY2 == 0) return 0.0;
    return sumXY / Math.sqrt(sumX2 * sumY2);
  }
  public static double[][] correlationMatrix(List<NetworkMetrics> list,
                                               List<MetricExtractor> extractors) {
    int n = extractors.size();
    double[][] matrix = new double[n][n];
    for (int i = 0; i < n; i++) {
      for (int j = 0; j < n; j++) {
        matrix[i][j] = pearsonCorrelation(list, extractors.get(i), extractors.get(j));
      }
    }
    return matrix;
  }
  public static List<MetricExtractor> standardExtractors() {
    return List.of(
        m -> m.getClusteringCoefficient(),
        m -> m.getGraphDensity(),
        m -> m.getAvgBetweennessCentrality(),
        m -> m.getAvgClosenessCentrality(),
        m -> m.getAvgDegreeCentrality(),
        m -> m.getGraphEntropy(),
        m -> m.getAvgPathLength() != null ? m.getAvgPathLength() : null,
        m -> m.getAssortativityDegree(),
        m -> m.getMeanDegree()
    );
  }
  public static List<String> standardExtractorLabels() {
    return List.of(
        "Clustering", "Density", "Betweenness", "Closeness", "Degree",
        "Entropy", "Avg Path", "Assortativity", "Mean Degree"
    );
  }
  public static PairComparison compareCities(NetworkMetrics a, NetworkMetrics b) {
    List<String> lines = new ArrayList<>();
    lines.add(String.format("Comparing %s vs %s:", a.getGraphName(), b.getGraphName()));
    lines.add("");
    String nameA = a.getGraphName();
    String nameB = b.getGraphName();
    compareLine(lines, nameA, nameB, "Nodes", a.getNodeCount(), b.getNodeCount(), false);
    compareLine(lines, nameA, nameB, "Edges", a.getEdgeCount(), b.getEdgeCount(), false);
    compareLine(lines, nameA, nameB, "Clustering ↑", a.getClusteringCoefficient(), b.getClusteringCoefficient(), false);
    compareLine(lines, nameA, nameB, "Density ↑", a.getGraphDensity(), b.getGraphDensity(), false);
    compareLine(lines, nameA, nameB, "Betweenness ↑", a.getAvgBetweennessCentrality(), b.getAvgBetweennessCentrality(), false);
    compareLine(lines, nameA, nameB, "Diameter ↓", a.getGraphDiameter(), b.getGraphDiameter(), true);
    compareLine(lines, nameA, nameB, "Avg Path ↓", a.getAvgPathLength(), b.getAvgPathLength(), true);
    compareLine(lines, nameA, nameB, "Entropy ↓", a.getGraphEntropy(), b.getGraphEntropy(), true);
    compareLine(lines, nameA, nameB, "Assortativity ↓", a.getAssortativityDegree(), b.getAssortativityDegree(), true);
    return new PairComparison(a, b, lines);
  }
  private static void compareLine(List<String> lines, String nameA, String nameB, String label,
                                  Object valA, Object valB, boolean lowerIsBetter) {
    if (valA == null && valB == null) return;
    String aStr = valA != null ? formatValue(valA) : "N/A";
    String bStr = valB != null ? formatValue(valB) : "N/A";
    String winner = "-";
    if (valA instanceof Number && valB instanceof Number) {
      double da = ((Number) valA).doubleValue();
      double db = ((Number) valB).doubleValue();
      if (da != db) {
        boolean aWins = lowerIsBetter ? da < db : da > db;
        winner = aWins ? "- " + nameA : "- " + nameB;
      } else {
        winner = "tie";
      }
    }
    lines.add(String.format("  %-22s  %12s  %12s  %s", label, aStr, bStr, winner));
  }
  private static String formatValue(Object v) {
    if (v instanceof Double) return String.format("%.4f", (Double) v);
    return v.toString();
  }
  public static Double getUmriScore(NetworkMetrics m, CityScoresLoader loader) {
    if (loader == null || !loader.isLoaded()) return null;
    double[] scores = loader.getScores(m.getGraphName() + ".graphml");
    if (scores == null) scores = loader.getScores(m.getGraphFile());
    return scores != null ? scores[1] : null;
  }
  public static List<NetworkMetrics> topNByMetric(List<NetworkMetrics> list,
                                                MetricExtractor extractor, int n) {
    return list.stream()
        .filter(m -> extractor.get(m) != null)
        .sorted(Comparator.comparingDouble(m -> -extractor.get(m)))
        .limit(n)
        .collect(Collectors.toList());
  }
  public static List<NetworkMetrics> bottomNByMetric(List<NetworkMetrics> list,
                                                     MetricExtractor extractor, int n) {
    return list.stream()
        .filter(m -> extractor.get(m) != null)
        .sorted(Comparator.comparingDouble(m -> extractor.get(m)))
        .limit(n)
        .collect(Collectors.toList());
  }
    public static String buildFocusedContext(
            String question, List<NetworkMetrics> list, CityScoresLoader loader) {
        StringBuilder sb = new StringBuilder();
        List<NetworkMetrics> matched = findMatchingCities(question, list);
        sb.append("Loaded ").append(list.size()).append(" urban mobility street networks.\n\n");
        if (!matched.isEmpty()) {
            sb.append("Cities matching the question:\n");
            for (NetworkMetrics m : matched) {
                sb.append(formatCityDetail(m, loader)).append("\n");
            }
            sb.append("\n");
        }
        sb.append(buildMetricsContext(list, loader));
        return sb.toString();
    }
    public static List<NetworkMetrics> findMatchingCities(String question, List<NetworkMetrics> list) {
        if (question == null || question.isBlank()) return List.of();
        String q = question.toLowerCase(Locale.ROOT);
        List<NetworkMetrics> matches = new ArrayList<>();
        for (NetworkMetrics m : list) {
            if (cityMatchesQuestion(q, m)) {
                matches.add(m);
            }
        }
        return matches;
    }
    private static boolean cityMatchesQuestion(String questionLower, NetworkMetrics m) {
        String graphName = m.getGraphName();
        if (graphName == null) return false;
        String normalized = graphName.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (questionLower.contains(normalized)) return true;
        String city = normalized;
        int comma = city.indexOf(',');
        if (comma > 0) {
            city = city.substring(0, comma).trim();
        }
        return city.length() >= 3 && questionLower.contains(city);
    }
    public static String formatCityDetail(NetworkMetrics m, CityScoresLoader loader) {
        StringBuilder sb = new StringBuilder();
        sb.append("--- ").append(m.getGraphName()).append(" ---\n");
        sb.append(String.format("  Nodes: %d  Edges: %d  Directed: %s  Analysis: %s%n",
                m.getNodeCount(), m.getEdgeCount(), m.isDirected(), m.getAnalysisModeLabel()));
        sb.append(String.format("  Clustering: %s  Density: %s  Diameter: %s  Avg path: %s%n",
                fmt(m.getClusteringCoefficient()), fmt(m.getGraphDensity()),
                m.getGraphDiameter() != null ? m.getGraphDiameter() : "N/A",
                fmt(m.getAvgPathLength())));
        sb.append(String.format("  Betweenness: %s  Closeness: %s  Degree: %s  Entropy: %s%n",
                fmt(m.getAvgBetweennessCentrality()), fmt(m.getAvgClosenessCentrality()),
                fmt(m.getAvgDegreeCentrality()), fmt(m.getGraphEntropy())));
        sb.append(String.format("  Mean degree: %s  Reciprocity: %s  Assortativity: %s%n",
                fmt(m.getMeanDegree()), fmt(m.getReciprocity()), fmt(m.getAssortativityDegree())));
        Double umri = getUmriScore(m, loader);
        if (umri != null) {
            sb.append(String.format("  UMRi score: %.1f%n", umri));
        }
        StreetNetworkStats street = m.getStreetStats();
        if (street != null && street.hasData()) {
            sb.append(String.format("  Road length: %.1f km  Streets/node: %.2f  Oneway: %.0f%%%n",
                    street.getTotalLengthKm(), street.getStreetsPerNode(), street.getOnewayRatio() * 100));
            sb.append(String.format("  Dominant highway: %s  Avg speed: %.0f km/h%n",
                    street.getDominantHighwayType() != null ? street.getDominantHighwayType() : "N/A",
                    street.getAvgMaxSpeedKmh()));
            sb.append(String.format("  Residential: %.0f%%  Car: %.0f%%  Pedestrian: %.0f%%  Cycling: %.0f%%%n",
                    street.getPctResidential(), street.getCarOrientedLengthPct(),
                    street.getPedestrianFriendlyLengthPct(), street.getCyclingFriendlyLengthPct()));
            sb.append(String.format("  Named roads: %.0f%%%n", street.getNamedRoadRatio() * 100));
        }
        return sb.toString().trim();
    }
    private static String fmt(Double v) {
        return v != null ? String.format("%.4f", v) : "N/A";
    }
    public static String buildMetricsContext(List<NetworkMetrics> list, CityScoresLoader loader) {
    StringBuilder sb = new StringBuilder();
    sb.append("Loaded ").append(list.size()).append(" urban mobility street networks.\n\n");
    List<RankedCity> ranked = rankByCompositeScore(list);
    sb.append("Composite structural readiness ranking (within loaded set):\n");
    for (RankedCity rc : ranked) {
      Double umri = getUmriScore(rc.metrics(), loader);
      sb.append(String.format("  #%d %s, score %.3f", rc.rank(), rc.metrics().getGraphName(), rc.compositeScore()));
      if (umri != null) sb.append(String.format(", UMRi=%.1f", umri));
      sb.append(String.format(" (nodes=%d, clustering=%.4f, density=%.6f",
          rc.metrics().getNodeCount(),
          d(rc.metrics().getClusteringCoefficient()),
          d(rc.metrics().getGraphDensity())));
      StreetNetworkStats street = rc.metrics().getStreetStats();
      if (street != null && street.hasData()) {
        sb.append(String.format(", road=%.0fkm, dominant=%s, oneway=%.0f%%",
            street.getTotalLengthKm(),
            street.getDominantHighwayType() != null ? street.getDominantHighwayType() : "?",
            street.getOnewayRatio() * 100));
      }
      sb.append(")\n");
    }
    sb.append("\nDataset averages:\n");
    sb.append(String.format("  Clustering: %.4f  Density: %.6f  Avg path: %.2f\n",
        avg(list, NetworkMetrics::getClusteringCoefficient),
        avg(list, NetworkMetrics::getGraphDensity),
        avg(list, NetworkMetrics::getAvgPathLength)));
    List<NetworkMetrics> withStreet = list.stream()
        .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
        .toList();
    if (!withStreet.isEmpty()) {
      sb.append("\nOSM street infrastructure (from GraphML):\n");
      sb.append(String.format("  Avg road length: %.1f km  Streets/node: %.2f  Oneway: %.0f%%\n",
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getTotalLengthKm()).average().orElse(0),
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getStreetsPerNode()).average().orElse(0),
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getOnewayRatio() * 100).average().orElse(0)));
      sb.append(String.format("  Residential: %.0f%%  Car-oriented: %.0f%%  Pedestrian: %.0f%%  Cycling: %.0f%%\n",
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getPctResidential()).average().orElse(0),
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getCarOrientedLengthPct()).average().orElse(0),
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getPedestrianFriendlyLengthPct()).average().orElse(0),
          withStreet.stream().mapToDouble(m -> m.getStreetStats().getCyclingFriendlyLengthPct()).average().orElse(0)));
    }
    if (loader != null && loader.isLoaded()) {
      sb.append("\nUMRi reference data available for ").append(loader.size()).append(" cities.\n");
    }
    return sb.toString();
  }
  private static double avg(List<NetworkMetrics> list, Function<NetworkMetrics, Double> g) {
    return list.stream().filter(m -> g.apply(m) != null).mapToDouble(m -> g.apply(m)).average().orElse(0);
  }
  private static double d(Double v) { return v != null ? v : 0.0; }
  private static double norm(double[] arr, int i, boolean invertForBetter) {
    double min = Arrays.stream(arr).min().orElse(0);
    double max = Arrays.stream(arr).max().orElse(1);
    if (max == min) return 0.5;
    double n = (arr[i] - min) / (max - min);
    return invertForBetter ? 1.0 - n : n;
  }
}