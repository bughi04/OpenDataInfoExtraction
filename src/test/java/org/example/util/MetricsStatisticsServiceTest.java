package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.util.MetricsStatisticsService.MetricExtractor;
import org.example.util.MetricsStatisticsService.PairComparison;
import org.example.util.MetricsStatisticsService.RankedCity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
// Tests composite-score ranking, best/worst selection, pairwise comparisons, and correlation helpers
// used by charts and the insight engine.
class MetricsStatisticsServiceTest {
  @Test
  void ranksCitiesByCompositeScore() {
    NetworkMetrics high = metrics("Alpha,_Land", 0.10, 0.002, 0.05, 50, 8.0, -0.1);
    NetworkMetrics low = metrics("Beta,_Land", 0.02, 0.0005, 0.01, 120, 15.0, 0.3);
    List<RankedCity> ranked = MetricsStatisticsService.rankByCompositeScore(List.of(low, high));
    assertEquals(2, ranked.size());
    assertEquals("Alpha,_Land", ranked.get(0).metrics().getGraphName());
    assertEquals(1, ranked.get(0).rank());
    assertEquals("Beta,_Land", ranked.get(1).metrics().getGraphName());
    assertTrue(ranked.get(0).compositeScore() > ranked.get(1).compositeScore());
  }
  @Test
  void bestAndWorstCityMatchRankingEnds() {
    NetworkMetrics a = metrics("A,_X", 0.08, 0.001, 0.04, 60, 10.0, 0.0);
    NetworkMetrics b = metrics("B,_X", 0.03, 0.0008, 0.02, 90, 12.0, 0.2);
    NetworkMetrics c = metrics("C,_X", 0.05, 0.0009, 0.03, 70, 11.0, 0.1);
    List<NetworkMetrics> list = List.of(a, b, c);

    assertEquals(MetricsStatisticsService.rankByCompositeScore(list).get(0).metrics(),
        MetricsStatisticsService.bestCity(list));
    assertEquals(MetricsStatisticsService.rankByCompositeScore(list).get(2).metrics(),
        MetricsStatisticsService.worstCity(list));
  }
  @Test
  void extractCountryFromGraphName() {
    NetworkMetrics m = new NetworkMetrics("C:/graphs/Paris,_France.graphml");
    assertEquals(" France", MetricsStatisticsService.extractCountry(m));
  }
  @Test
  void groupByCountryAndAverageMetric() {
    NetworkMetrics paris = metrics("Paris,_France", 0.06, 0.001, 0.03, 80, 11.0, 0.0);
    NetworkMetrics lyon = metrics("Lyon,_France", 0.04, 0.001, 0.02, 90, 12.0, 0.1);
    NetworkMetrics berlin = metrics("Berlin,_Germany", 0.08, 0.001, 0.04, 70, 10.0, -0.1);
    Map<String, List<NetworkMetrics>> groups =
        MetricsStatisticsService.groupByCountry(List.of(paris, lyon, berlin));
    assertEquals(2, groups.size());
    assertEquals(2, groups.get(" France").size());
    assertEquals(1, groups.get(" Germany").size());
    MetricExtractor clustering = NetworkMetrics::getClusteringCoefficient;
    Map<String, Double> avg = MetricsStatisticsService.averageMetricByCountry(
        List.of(paris, lyon, berlin), clustering);
    assertEquals(0.05, avg.get(" France"), 1e-9);
    assertEquals(0.08, avg.get(" Germany"), 1e-9);
  }
  @Test
  void pearsonCorrelationIsOneForIdenticalSeries() {
    NetworkMetrics a = metrics("A,_X", 0.01, 0.001, 0.02, 50, 8.0, 0.0);
    NetworkMetrics b = metrics("B,_X", 0.02, 0.002, 0.04, 60, 9.0, 0.1);
    NetworkMetrics c = metrics("C,_X", 0.03, 0.003, 0.06, 70, 10.0, 0.2);
    MetricExtractor density = NetworkMetrics::getGraphDensity;
    double r = MetricsStatisticsService.pearsonCorrelation(List.of(a, b, c), density, density);
    assertEquals(1.0, r, 1e-9);
  }
  @Test
  void pearsonReturnsZeroWithTooFewSamples() {
    NetworkMetrics a = metrics("A,_X", 0.01, 0.001, 0.02, 50, 8.0, 0.0);
    NetworkMetrics b = metrics("B,_X", 0.02, 0.002, 0.04, 60, 9.0, 0.1);
    double r = MetricsStatisticsService.pearsonCorrelation(
        List.of(a, b), NetworkMetrics::getGraphDensity, NetworkMetrics::getClusteringCoefficient);
    assertEquals(0.0, r, 1e-9);
  }
  @Test
  void compareCitiesDeclaresWinnerForClustering() {
    NetworkMetrics better = metrics("Good,_Land", 0.10, 0.002, 0.05, 50, 8.0, -0.1);
    NetworkMetrics worse = metrics("Weak,_Land", 0.02, 0.0005, 0.01, 120, 15.0, 0.3);
    PairComparison cmp = MetricsStatisticsService.compareCities(better, worse);
    String joined = String.join("\n", cmp.lines());
    assertTrue(joined.contains("Clustering"));
    assertTrue(joined.contains("0.1000"));
    assertTrue(joined.contains("0.0200"));
    assertTrue(joined.contains("Diameter"));
  }
  @Test
  void findMatchingCitiesFromQuestion() {
    NetworkMetrics paris = metrics("Paris,_France", 0.06, 0.001, 0.03, 80, 11.0, 0.0);
    NetworkMetrics berlin = metrics("Berlin,_Germany", 0.08, 0.001, 0.04, 70, 10.0, -0.1);
    List<NetworkMetrics> matches = MetricsStatisticsService.findMatchingCities(
        "How does Paris compare?", List.of(paris, berlin));
    assertEquals(1, matches.size());
    assertEquals("Paris,_France", matches.get(0).getGraphName());
  }
  @Test
  void topAndBottomNByMetric() {
    NetworkMetrics a = metrics("A,_X", 0.01, 0.001, 0.02, 50, 8.0, 0.0);
    NetworkMetrics b = metrics("B,_X", 0.05, 0.002, 0.04, 60, 9.0, 0.1);
    NetworkMetrics c = metrics("C,_X", 0.03, 0.003, 0.06, 70, 10.0, 0.2);
    MetricExtractor clustering = NetworkMetrics::getClusteringCoefficient;
    assertEquals("B,_X", MetricsStatisticsService.topNByMetric(List.of(a, b, c), clustering, 1)
        .get(0).getGraphName());
    assertEquals("A,_X", MetricsStatisticsService.bottomNByMetric(List.of(a, b, c), clustering, 1)
        .get(0).getGraphName());
  }
  @Test
  void buildMetricsContextIncludesRankingAndStreetStats() {
    NetworkMetrics m = metrics("Oslo,_Norway", 0.07, 0.001, 0.03, 65, 10.0, 0.0);
    StreetNetworkStats street = new StreetNetworkStats();
    street.setTotalLengthMeters(500_000);
    street.setOnewayRatio(0.4);
    street.setDominantHighwayType("residential");
    m.setStreetStats(street);
    String ctx = MetricsStatisticsService.buildMetricsContext(List.of(m), null);
    assertTrue(ctx.contains("Oslo,_Norway"));
    assertTrue(ctx.contains("Composite structural readiness ranking"));
    assertTrue(ctx.contains("OSM street infrastructure"));
  }
  private static NetworkMetrics metrics(
      String name, double clustering, double density, double degreeCentrality,
      int diameter, double avgPath, double assortativity) {
    NetworkMetrics m = new NetworkMetrics("C:/graphs/" + name + ".graphml");
    m.setNodeCount(1000);
    m.setEdgeCount(2000);
    m.setClusteringCoefficient(clustering);
    m.setGraphDensity(density);
    m.setAvgDegreeCentrality(degreeCentrality);
    m.setGraphDiameter(diameter);
    m.setAvgPathLength(avgPath);
    m.setAssortativityDegree(assortativity);
    return m;
  }
}