package org.example.service;

import org.example.model.NetworkMetrics;
import org.example.util.MetricsStatisticsService;
import org.example.util.MetricsStatisticsService.MetricExtractor;
import org.example.util.MetricsStatisticsService.PairComparison;
import org.example.util.MetricsStatisticsService.RankedCity;

import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * Rule-based insight engine that produces detailed comparative narratives
 * without requiring an external LLM. Used as fallback when no API key is set,
 * and as a fast local alternative for common analysis questions.
 */
public class LocalInsightEngine {

  private static final String SYSTEM_CONTEXT =
      "Urban mobility street-network analysis based on Sierra-Porta & Herrera-Acevedo (2024). "
          + "UMRi predictors: clustering↑, density↑, degree centrality↑, diameter↓, path length↓, assortativity↓.";

  public String generateOverview(List<NetworkMetrics> metrics, CityScoresLoader loader) {
    if (metrics == null || metrics.isEmpty()) {
      return "No networks loaded. Process GraphML files first.";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("=== AI NETWORK ADVISOR - OVERVIEW ===\n\n");
    sb.append(SYSTEM_CONTEXT).append("\n\n");
    sb.append(MetricsStatisticsService.buildMetricsContext(metrics, loader));

    List<RankedCity> ranked = MetricsStatisticsService.rankByCompositeScore(metrics);
    NetworkMetrics best = ranked.get(0).metrics();
    NetworkMetrics worst = ranked.get(ranked.size() - 1).metrics();

    sb.append("\n--- Key Findings ---\n\n");
    sb.append(String.format("- Best structural readiness in your set: %s (score %.3f)\n",
        best.getGraphName(), ranked.get(0).compositeScore()));
    appendUmriLine(sb, best, loader);

    sb.append(String.format("- Lowest structural readiness: %s (score %.3f)\n",
        worst.getGraphName(), ranked.get(ranked.size() - 1).compositeScore()));
    appendUmriLine(sb, worst, loader);

    OptionalDouble avgClustering = metrics.stream()
        .filter(m -> m.getClusteringCoefficient() != null)
        .mapToDouble(NetworkMetrics::getClusteringCoefficient).average();
    if (avgClustering.isPresent()) {
      double cc = avgClustering.getAsDouble();
      sb.append(String.format("\n- Average clustering across loaded cities: %.4f - ", cc));
      if (cc > 0.06) sb.append("comparable to top UMRi performers (Helsinki, Amsterdam).\n");
      else if (cc < 0.03) sb.append("below typical high-readiness cities; local connectivity is weak.\n");
      else sb.append("moderate; room for improvement in neighbourhood-level connectivity.\n");
    }

    sb.append("\n--- Strongest Correlations in Your Dataset ---\n");
    appendTopCorrelations(sb, metrics);

    sb.append("\n--- Country-Level Averages (clustering) ---\n");
    Map<String, Double> byCountry = MetricsStatisticsService.averageMetricByCountry(
        metrics, m -> m.getClusteringCoefficient());
    byCountry.entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .limit(8)
        .forEach(e -> sb.append(String.format("  %-28s %.4f\n", e.getKey(), e.getValue())));

    sb.append("\nTip: Ask \"compare Helsinki and Lagos\" or use Best vs Worst for a head-to-head.\n");
    return sb.toString();
  }

  public String generateBestVsWorst(List<NetworkMetrics> metrics, CityScoresLoader loader) {
    if (metrics.size() < 2) {
      return "Need at least 2 processed networks for a comparison.";
    }

    NetworkMetrics best = MetricsStatisticsService.bestCity(metrics);
    NetworkMetrics worst = MetricsStatisticsService.worstCity(metrics);
    if (best.equals(worst)) {
      return "Only one distinct network in the ranking.";
    }

    StringBuilder sb = new StringBuilder();
    sb.append("=== BEST vs WORST - HEAD-TO-HEAD ===\n\n");
    sb.append(String.format("Ranking uses composite structural readiness within your loaded set.\n"));
    sb.append(String.format("BEST:  %s\n", best.getGraphName()));
    appendUmriLine(sb, best, loader);
    sb.append(String.format("WORST: %s\n", worst.getGraphName()));
    appendUmriLine(sb, worst, loader);
    sb.append("\n");

    PairComparison cmp = MetricsStatisticsService.compareCities(best, worst);
    for (String line : cmp.lines()) sb.append(line).append("\n");

    sb.append("\n--- Interpretation ---\n");
    interpretComparison(sb, best, worst);
    return sb.toString();
  }

  public String answerQuestion(String question, List<NetworkMetrics> metrics, CityScoresLoader loader) {
    String q = question.toLowerCase().trim();
    if (q.isEmpty()) return "Please type a question about your loaded networks.";

    if (q.contains("best") && q.contains("worst") || q.contains("compare") && q.contains("vs")) {
      if (q.contains("best") && q.contains("worst")) {
        return generateBestVsWorst(metrics, loader);
      }
      NetworkMetrics a = findCityByQuestion(q, metrics);
      NetworkMetrics b = findSecondCityByQuestion(q, metrics, a);
      if (a != null && b != null) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CITY COMPARISON ===\n\n");
        PairComparison cmp = MetricsStatisticsService.compareCities(a, b);
        for (String line : cmp.lines()) sb.append(line).append("\n");
        sb.append("\n--- Interpretation ---\n");
        interpretComparison(sb, a, b);
        return sb.toString();
      }
    }

    if (q.contains("clustering") || q.contains("umri") || q.contains("readiness")) {
      return generateClusteringAnalysis(metrics, loader);
    }

    if (q.contains("country") || q.contains("region")) {
      return generateRegionalAnalysis(metrics);
    }

    if (q.contains("overview") || q.contains("summary")) {
      return generateOverview(metrics, loader);
    }

    // Default: overview + hint
    return generateOverview(metrics, loader)
        + "\n\n(I interpreted your question broadly. Try: \"compare Paris and Mumbai\" "
        + "or \"which country has highest clustering?\")";
  }

  private String generateClusteringAnalysis(List<NetworkMetrics> metrics, CityScoresLoader loader) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== CLUSTERING & MOBILITY READINESS ===\n\n");
    sb.append("Clustering coefficient is the strongest positive UMRi predictor (p < 0.001).\n\n");

    MetricExtractor clustering = m -> m.getClusteringCoefficient();
    List<NetworkMetrics> top5 = MetricsStatisticsService.topNByMetric(metrics, clustering, 5);
    List<NetworkMetrics> bottom5 = MetricsStatisticsService.bottomNByMetric(metrics, clustering, 5);

    sb.append("Top 5 by clustering:\n");
    for (NetworkMetrics m : top5) {
      sb.append(String.format("  %-30s %.4f", m.getGraphName(), m.getClusteringCoefficient()));
      appendUmriInline(sb, m, loader);
      sb.append("\n");
    }

    sb.append("\nBottom 5 by clustering:\n");
    for (NetworkMetrics m : bottom5) {
      sb.append(String.format("  %-30s %.4f", m.getGraphName(), m.getClusteringCoefficient()));
      appendUmriInline(sb, m, loader);
      sb.append("\n");
    }

    double corr = MetricsStatisticsService.pearsonCorrelation(metrics,
        m -> m.getClusteringCoefficient(),
        m -> MetricsStatisticsService.getUmriScore(m, loader));
    if (loader != null && loader.isLoaded()) {
      sb.append(String.format("\nPearson correlation (clustering vs UMRi) in your set: %.3f\n", corr));
    }
    return sb.toString();
  }

  private String generateRegionalAnalysis(List<NetworkMetrics> metrics) {
    StringBuilder sb = new StringBuilder();
    sb.append("=== REGIONAL / COUNTRY COMPARISON ===\n\n");

    Map<String, List<NetworkMetrics>> groups = MetricsStatisticsService.groupByCountry(metrics);
    sb.append("Cities per country/region in your loaded set:\n");
    groups.entrySet().stream()
        .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
        .forEach(e -> sb.append(String.format("  %-28s %d cities\n", e.getKey(), e.getValue().size())));

    sb.append("\nAverage clustering by country:\n");
    MetricsStatisticsService.averageMetricByCountry(metrics, m -> m.getClusteringCoefficient())
        .entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .forEach(e -> sb.append(String.format("  %-28s %.4f\n", e.getKey(), e.getValue())));

    sb.append("\nAverage graph density by country:\n");
    MetricsStatisticsService.averageMetricByCountry(metrics, m -> m.getGraphDensity())
        .entrySet().stream()
        .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
        .forEach(e -> sb.append(String.format("  %-28s %.6f\n", e.getKey(), e.getValue())));

    return sb.toString();
  }

  private void appendTopCorrelations(StringBuilder sb, List<NetworkMetrics> metrics) {
    List<MetricExtractor> extractors = MetricsStatisticsService.standardExtractors();
    List<String> labels = MetricsStatisticsService.standardExtractorLabels();
    double[][] matrix = MetricsStatisticsService.correlationMatrix(metrics, extractors);

    record Pair(String a, String b, double r) {}
    java.util.List<Pair> pairs = new java.util.ArrayList<>();
    for (int i = 0; i < labels.size(); i++) {
      for (int j = i + 1; j < labels.size(); j++) {
        pairs.add(new Pair(labels.get(i), labels.get(j), matrix[i][j]));
      }
    }
    pairs.stream()
        .sorted((x, y) -> Double.compare(Math.abs(y.r), Math.abs(x.r)))
        .limit(5)
        .forEach(p -> sb.append(String.format("  %s / %s: %.3f\n", p.a, p.b, p.r)));
  }

  private void interpretComparison(StringBuilder sb, NetworkMetrics a, NetworkMetrics b) {
    if (a.getClusteringCoefficient() != null && b.getClusteringCoefficient() != null) {
      double diff = a.getClusteringCoefficient() - b.getClusteringCoefficient();
      if (Math.abs(diff) > 0.02) {
        sb.append(String.format("- %s shows %.1f%% higher clustering - stronger local street connectivity.\n",
            diff > 0 ? a.getGraphName() : b.getGraphName(), Math.abs(diff) * 100));
      }
    }
    if (a.getGraphDiameter() != null && b.getGraphDiameter() != null) {
      int da = a.getGraphDiameter();
      int db = b.getGraphDiameter();
      if (da != db) {
        sb.append(String.format("- Diameter: %s (%d hops) vs %s (%d hops) - %s is more geographically compact.\n",
            a.getGraphName(), da, b.getGraphName(), db,
            da < db ? a.getGraphName() : b.getGraphName()));
      }
    }
    if (a.getAssortativityDegree() != null && b.getAssortativityDegree() != null) {
      sb.append("- Lower assortativity favours hub-to-periphery connections (better for mobility).\n");
    }
  }

  private NetworkMetrics findCityByQuestion(String q, List<NetworkMetrics> metrics) {
    for (NetworkMetrics m : metrics) {
      String name = m.getGraphName().toLowerCase().replace('_', ' ');
      if (q.contains(name) || containsCityToken(q, name)) return m;
    }
    return metrics.isEmpty() ? null : metrics.get(0);
  }

  private NetworkMetrics findSecondCityByQuestion(String q, List<NetworkMetrics> metrics, NetworkMetrics skip) {
    for (NetworkMetrics m : metrics) {
      if (m.equals(skip)) continue;
      String name = m.getGraphName().toLowerCase().replace('_', ' ');
      if (q.contains(name) || containsCityToken(q, name)) return m;
    }
    return metrics.size() > 1 ? metrics.get(1) : null;
  }

  private boolean containsCityToken(String q, String name) {
    String cityPart = name.split(",")[0].trim();
    return q.contains(cityPart);
  }

  private void appendUmriLine(StringBuilder sb, NetworkMetrics m, CityScoresLoader loader) {
    Double umri = MetricsStatisticsService.getUmriScore(m, loader);
    if (umri != null) sb.append(String.format("  UMRi reference score: %.1f\n", umri));
  }

  private void appendUmriInline(StringBuilder sb, NetworkMetrics m, CityScoresLoader loader) {
    Double umri = MetricsStatisticsService.getUmriScore(m, loader);
    if (umri != null) sb.append(String.format("  (UMRi %.1f)", umri));
  }

  public static String buildLlmSystemPrompt() {
    return "You are an expert urban mobility network analyst. "
        + "You interpret graph-theoretic metrics of city street networks "
        + "(clustering, density, centrality, diameter, path length, entropy, assortativity) "
        + "in the context of the Urban Mobility Readiness Index (UMRi). "
        + "Reference Sierra-Porta & Herrera-Acevedo (2024) when relevant. "
        + "Be specific, compare cities when data allows, and highlight actionable insights. "
        + "Use plain text with bullet points. Keep responses under 500 words unless asked for detail.";
  }
}
