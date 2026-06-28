package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.service.CityScoresLoader;
import org.example.service.WorldBankIndicators;
import org.example.theme.ThemeManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

// Factory for JFreeChart charts that visualise NetworkMetrics data
public class NetworkChartGenerator {
    private static final Logger logger = LoggerFactory.getLogger(NetworkChartGenerator.class);
    private static final int MAX_LABELS = 30;
    public static JFreeChart generateCentralityComparisonChart(List<NetworkMetrics> metrics) {
        logger.info("Generating centrality comparison chart for {} graphs", metrics.size());
        List<NetworkMetrics> sorted = metrics.stream()
            .filter(m -> m.getAvgBetweennessCentrality() != null)
            .sorted(Comparator.comparingDouble(NetworkMetrics::getAvgBetweennessCentrality).reversed())
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            safeAdd(ds, "Betweenness", label, m.getAvgBetweennessCentrality());
            safeAdd(ds, "Closeness",   label, m.getAvgClosenessCentrality());
            safeAdd(ds, "Degree",      label, m.getAvgDegreeCentrality());
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Centrality Metrics by Graph",
            "Graph", "Centrality (normalised)",
            ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateClusteringAndDensityChart(List<NetworkMetrics> metrics) {
        logger.info("Generating clustering & density chart");
        List<NetworkMetrics> sorted = metrics.stream()
            .filter(m -> m.getClusteringCoefficient() != null)
            .sorted(Comparator.comparingDouble(NetworkMetrics::getClusteringCoefficient).reversed())
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            safeAdd(ds, "Clustering Coefficient", label, m.getClusteringCoefficient());
            safeAdd(ds, "Graph Density",          label, m.getGraphDensity());
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Clustering Coefficient and Graph Density",
            "Graph", "Value",
            ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateDiameterAndPathLengthChart(List<NetworkMetrics> metrics) {
        logger.info("Generating diameter & path-length chart");
        List<NetworkMetrics> sorted = metrics.stream()
            .filter(m -> m.getGraphDiameter() != null)
            .sorted(Comparator.comparingInt(NetworkMetrics::getGraphDiameter))
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            if (m.getGraphDiameter() != null)
                ds.addValue(m.getGraphDiameter(), "Diameter", label);
            safeAdd(ds, "Avg Path Length", label, m.getAvgPathLength());
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Graph Diameter and Average Path Length",
            "Graph", "Hops",
            ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateNodeEdgeCountChart(List<NetworkMetrics> metrics) {
        logger.info("Generating node/edge count chart");
        List<NetworkMetrics> sorted = metrics.stream()
            .sorted(Comparator.comparingInt(NetworkMetrics::getNodeCount).reversed())
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            ds.addValue(m.getNodeCount(), "Nodes", label);
            ds.addValue(m.getEdgeCount(), "Edges", label);
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Node and Edge Count per Graph",
            "Graph", "Count",
            ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateEntropyChart(List<NetworkMetrics> metrics) {
        logger.info("Generating entropy chart");
        List<NetworkMetrics> sorted = metrics.stream()
            .filter(m -> m.getGraphEntropy() != null)
            .sorted(Comparator.comparingDouble(NetworkMetrics::getGraphEntropy))
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            safeAdd(ds, "Graph Entropy", shortName(m), m.getGraphEntropy());
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Graph Entropy per Network",
            "Graph", "Entropy (bits)",
            ds, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, metrics.size());
        colorGradientBars(chart, sorted, NetworkMetrics::getGraphEntropy);
        return chart;
    }
    public static JFreeChart generateMeanDegreeAndDiversityChart(List<NetworkMetrics> metrics) {
        logger.info("Generating mean-degree chart");
        List<NetworkMetrics> sorted = metrics.stream()
            .filter(m -> m.getMeanDegree() != null)
            .sorted(Comparator.comparingDouble(NetworkMetrics::getMeanDegree).reversed())
            .limit(MAX_LABELS)
            .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            safeAdd(ds, "Mean Degree",       label, m.getMeanDegree());
            safeAdd(ds, "Degree Variance",   label, m.getDiversity());
        }
        JFreeChart chart = ChartFactory.createBarChart(
            "Mean Degree and Degree Variance",
            "Graph", "Value",
            ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateDensityVsClusteringScatter(List<NetworkMetrics> metrics) {
        logger.info("Generating density-vs-clustering scatter");
        XYSeriesCollection ds = new XYSeriesCollection();
        XYSeries series = new XYSeries("Graphs");
        for (NetworkMetrics m : metrics) {
            if (m.getGraphDensity() != null && m.getClusteringCoefficient() != null) {
                series.add(m.getGraphDensity(), m.getClusteringCoefficient());
            }
        }
        ds.addSeries(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Graph Density vs. Clustering Coefficient",
            "Graph Density", "Clustering Coefficient",
            ds, PlotOrientation.VERTICAL, false, true, false);
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setDomainGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        plot.setRangeGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        renderer.setSeriesPaint(0, ThemeManager.getCurrentTheme().getAccentColor());
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        plot.setRenderer(renderer);
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());
        return chart;
    }
    public static JFreeChart generateCompositeReadinessChart(List<NetworkMetrics> metrics) {
        logger.info("Generating composite readiness chart");
        List<MetricsStatisticsService.RankedCity> ranked =
                MetricsStatisticsService.rankByCompositeScore(metrics);
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        int limit = Math.min(ranked.size(), MAX_LABELS);
        for (int i = 0; i < limit; i++) {
            MetricsStatisticsService.RankedCity rc = ranked.get(i);
            ds.addValue(rc.compositeScore(), "Readiness", shortName(rc.metrics()));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Composite Structural Readiness Score",
                "Network", "Score (0–1, higher = better)",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, limit);
        return chart;
    }
    public static JFreeChart generateAssortativityReciprocityChart(List<NetworkMetrics> metrics) {
        logger.info("Generating assortativity & reciprocity chart");
        List<NetworkMetrics> sorted = metrics.stream()
                .filter(m -> m.getAssortativityDegree() != null)
                .sorted(Comparator.comparingDouble(NetworkMetrics::getAssortativityDegree))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            String label = shortName(m);
            safeAdd(ds, "Assortativity ↓", label, m.getAssortativityDegree());
            safeAdd(ds, "Reciprocity", label, m.getReciprocity());
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Assortativity & Reciprocity by Network",
                "Network", "Value",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, metrics.size());
        return chart;
    }
    public static JFreeChart generateUmriVsClusteringScatter(
            List<NetworkMetrics> metrics, CityScoresLoader loader) {
        logger.info("Generating UMRi vs clustering scatter");
        XYSeries withUmri = new XYSeries("Cities with UMRi data");
        XYSeries without = new XYSeries("No UMRi match");
        for (NetworkMetrics m : metrics) {
            if (m.getClusteringCoefficient() == null) continue;
            Double umri = MetricsStatisticsService.getUmriScore(m, loader);
            if (umri != null) {
                withUmri.add(m.getClusteringCoefficient().doubleValue(), umri.doubleValue());
            } else {
                without.add(m.getClusteringCoefficient().doubleValue(), 0.0);
            }
        }
        XYSeriesCollection ds = new XYSeriesCollection();
        if (withUmri.getItemCount() > 0) ds.addSeries(withUmri);
        if (without.getItemCount() > 0) ds.addSeries(without);
        JFreeChart chart = ChartFactory.createScatterPlot(
                "UMRi Score vs Clustering Coefficient",
                "Clustering Coefficient", "UMRi Score",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleScatterChart(chart);
        return chart;
    }
    public static JFreeChart generateStructuralVsUmriScatter(
            List<NetworkMetrics> metrics, CityScoresLoader loader) {
        logger.info("Generating structural vs UMRi scatter");
        double[] scores = MetricsStatisticsService.computeCompositeScores(metrics);
        XYSeries series = new XYSeries("Cities");
        for (int i = 0; i < metrics.size(); i++) {
            Double umri = MetricsStatisticsService.getUmriScore(metrics.get(i), loader);
            if (umri != null) {
                series.add(scores[i], umri);
            }
        }
        XYSeriesCollection ds = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Computed Readiness vs Official UMRi Score",
                "Composite Structural Score", "UMRi Score",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleScatterChart(chart);
        return chart;
    }
    public static JFreeChart generatePredictedVsActualChart(
            List<org.example.service.MobilityReadinessPredictor.CityPrediction> predictions,
            String targetLabel,
            String title) {
        logger.info("Generating predicted vs actual chart for {}", targetLabel);
        XYSeries series = new XYSeries("Cities");
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for (var p : predictions) {
            if (!p.hasReferenceLabel() || p.actualScore() == null) continue;
            double actual = p.actualScore();
            double predicted = p.predictedScore();
            series.add(predicted, actual);
            min = Math.min(min, Math.min(actual, predicted));
            max = Math.max(max, Math.max(actual, predicted));
        }
        XYSeriesCollection ds = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
                title,
                "Predicted " + targetLabel, "Actual " + targetLabel,
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleScatterChart(chart);
        if (series.getItemCount() > 0 && Double.isFinite(min) && Double.isFinite(max)) {
            XYPlot plot = chart.getXYPlot();
            double pad = Math.max(1.0, (max - min) * 0.05);
            double lo = min - pad;
            double hi = max + pad;
            plot.getDomainAxis().setRange(lo, hi);
            plot.getRangeAxis().setRange(lo, hi);
            XYSeries diagonal = new XYSeries("Perfect prediction");
            diagonal.add(lo, lo);
            diagonal.add(hi, hi);
            ds.addSeries(diagonal);
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            renderer.setSeriesLinesVisible(1, true);
            renderer.setSeriesShapesVisible(1, false);
            renderer.setSeriesPaint(1, new Color(180, 180, 180));
        }
        return chart;
    }
    public static JFreeChart generateMetricDistributionBoxPlot(List<NetworkMetrics> metrics) {
        logger.info("Generating metric distribution box plot");
        DefaultBoxAndWhiskerCategoryDataset ds = new DefaultBoxAndWhiskerCategoryDataset();
        addBox(ds, "Clustering ↑", metrics, NetworkMetrics::getClusteringCoefficient);
        addBox(ds, "Density ↑", metrics, NetworkMetrics::getGraphDensity);
        addBox(ds, "Betweenness ↑", metrics, NetworkMetrics::getAvgBetweennessCentrality);
        addBox(ds, "Avg Path ↓", metrics, NetworkMetrics::getAvgPathLength);
        addBox(ds, "Entropy ↓", metrics, NetworkMetrics::getGraphEntropy);
        addBox(ds, "Assortativity ↓", metrics, NetworkMetrics::getAssortativityDegree);
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
                "Metric Distributions Across Loaded Cities",
                "Metric", "Value",
                ds, true);
        styleBarChart(chart, 6);
        return chart;
    }
  private static void addBox(DefaultBoxAndWhiskerCategoryDataset ds, String metric,
                               List<NetworkMetrics> metrics,
                               java.util.function.Function<NetworkMetrics, Double> extractor) {
        List<Double> values = metrics.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (values.isEmpty()) return;
        ds.add(values, metric, "All Cities");
    }
    public static JFreeChart generateTopVsBottomComparisonChart(List<NetworkMetrics> metrics) {
        logger.info("Generating top vs bottom comparison");
        List<MetricsStatisticsService.RankedCity> ranked =
                MetricsStatisticsService.rankByCompositeScore(metrics);
        if (ranked.size() < 2) return null;
        int topN = Math.min(5, ranked.size());
        List<NetworkMetrics> top = ranked.stream().limit(topN).map(MetricsStatisticsService.RankedCity::metrics).toList();
        List<NetworkMetrics> bottom = ranked.stream()
                .skip(Math.max(0, ranked.size() - topN))
                .map(MetricsStatisticsService.RankedCity::metrics)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        addGroupAvg(ds, "Top " + topN, top);
        addGroupAvg(ds, "Bottom " + bottom.size(), bottom);
        JFreeChart chart = ChartFactory.createBarChart(
                "Top vs Bottom Cities: Normalised Metric Averages",
                "Metric", "Normalised value (0–1)",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, 2);
        return chart;
    }
    private static void addGroupAvg(DefaultCategoryDataset ds, String group, List<NetworkMetrics> cities) {
        if (cities.isEmpty()) return;
        ds.addValue(normAvg(cities, NetworkMetrics::getClusteringCoefficient), group, "Clustering");
        ds.addValue(normAvg(cities, NetworkMetrics::getGraphDensity), group, "Density");
        ds.addValue(normAvg(cities, NetworkMetrics::getAvgDegreeCentrality), group, "Degree Cent.");
        ds.addValue(normAvg(cities, NetworkMetrics::getAvgBetweennessCentrality), group, "Betweenness");
        ds.addValue(1.0 - normAvg(cities, m -> m.getGraphDiameter() != null ? (double) m.getGraphDiameter() : null), group, "Compactness");
        ds.addValue(1.0 - normAvg(cities, NetworkMetrics::getAvgPathLength), group, "Short Paths");
    }
    private static double normAvg(List<NetworkMetrics> cities,
                                  java.util.function.Function<NetworkMetrics, Double> g) {
        OptionalDouble avg = cities.stream().filter(m -> g.apply(m) != null).mapToDouble(m -> g.apply(m)).average();
        return avg.isPresent() ? Math.min(1.0, avg.getAsDouble()) : 0;
    }
    public static JFreeChart generateCountryClusteringChart(List<NetworkMetrics> metrics) {
        logger.info("Generating country clustering chart");
        Map<String, Double> byCountry = MetricsStatisticsService.averageMetricByCountry(
                metrics, m -> m.getClusteringCoefficient());
        List<Map.Entry<String, Double>> sorted = byCountry.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Double> e : sorted) {
            String label = e.getKey().length() > 18 ? e.getKey().substring(0, 16) + "…" : e.getKey();
            ds.addValue(e.getValue(), "Avg Clustering", label);
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Average Clustering by Country/Region",
                "Country", "Clustering Coefficient",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, sorted.size());
        return chart;
    }
    public static JFreeChart generateMetricCorrelationChart(List<NetworkMetrics> metrics) {
        logger.info("Generating metric correlation chart");
        List<MetricsStatisticsService.MetricExtractor> extractors =
                MetricsStatisticsService.standardExtractors();
        List<String> labels = MetricsStatisticsService.standardExtractorLabels();
        double[][] matrix = MetricsStatisticsService.correlationMatrix(metrics, extractors);
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (int i = 0; i < labels.size(); i++) {
            for (int j = i + 1; j < labels.size(); j++) {
                String pair = labels.get(i) + " × " + labels.get(j);
                if (pair.length() > 28) pair = pair.substring(0, 26) + "…";
                ds.addValue(matrix[i][j], "Correlation", pair);
            }
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Pearson Correlations Between Metrics",
                "Metric Pair", "Correlation (−1 to +1)",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, ds.getColumnCount());
        applyCorrelationColors(chart);
        return chart;
    }
    public static JFreeChart generatePathLengthVsDiameterScatter(List<NetworkMetrics> metrics) {
        logger.info("Generating path length vs diameter scatter");
        XYSeries series = new XYSeries("Networks");
        for (NetworkMetrics m : metrics) {
            if (m.getGraphDiameter() != null && m.getAvgPathLength() != null) {
                series.add(m.getGraphDiameter(), m.getAvgPathLength());
            }
        }
        XYSeriesCollection ds = new XYSeriesCollection(series);
        JFreeChart chart = ChartFactory.createScatterPlot(
                "Graph Diameter vs Average Path Length",
                "Diameter (hops) ↓ better", "Avg Path Length ↓ better",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleScatterChart(chart);
        return chart;
    }
    public static JFreeChart generateUmriRankingChart(
            List<NetworkMetrics> metrics, CityScoresLoader loader) {
        logger.info("Generating UMRi ranking chart");
        List<NetworkMetrics> withUmri = metrics.stream()
                .filter(m -> MetricsStatisticsService.getUmriScore(m, loader) != null)
                .sorted((a, b) -> Double.compare(
                        MetricsStatisticsService.getUmriScore(b, loader),
                        MetricsStatisticsService.getUmriScore(a, loader)))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withUmri) {
            ds.addValue(MetricsStatisticsService.getUmriScore(m, loader), "UMRi", shortName(m));
        }
        if (ds.getColumnCount() == 0) return null;
        JFreeChart chart = ChartFactory.createBarChart(
                "Official UMRi Scores (Oliver Wyman Forum 2023)",
                "City", "UMRi Score",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, withUmri.size());
        return chart;
    }
    private static void styleScatterChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setDomainGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        plot.setRangeGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, true);
        Color[] colours = ThemeManager.getCurrentTheme().getChartColors();
        for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colours[i % colours.length]);
            renderer.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        }
        plot.setRenderer(renderer);
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());
    }
    private static void applyCorrelationColors(JFreeChart chart) {
        if (!(chart.getPlot() instanceof CategoryPlot)) return;
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setShadowVisible(false);
        for (int i = 0; i < plot.getDataset().getColumnCount(); i++) {
            Number val = plot.getDataset().getValue(0, i);
            double r = val != null ? val.doubleValue() : 0;
            Color c = r > 0
                    ? new Color(46, 125, 50, 180)
                    : new Color(211, 47, 47, 180);
            renderer.setSeriesPaint(0, c);
        }
    }
    private static void styleBarChart(JFreeChart chart, int totalGraphs) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());
        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            plot.setDomainGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
            plot.setRangeGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
            plot.setOutlineVisible(false);
            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
            domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 9));
            domainAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
            if (totalGraphs > 10) {
                domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            }
            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setLabelFont(new Font("Arial", Font.PLAIN, 11));
            rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 9));
            rangeAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
            if (plot.getRenderer() instanceof BarRenderer) {
                BarRenderer renderer = (BarRenderer) plot.getRenderer();
                renderer.setMaximumBarWidth(0.12);
                renderer.setShadowVisible(false);
                Color[] colours = ThemeManager.getCurrentTheme().getChartColors();
                for (int i = 0; i < plot.getDataset().getRowCount(); i++) {
                    renderer.setSeriesPaint(i, colours[i % colours.length]);
                }
            }
        }
    }
    @FunctionalInterface
    interface DoubleExtractor { Double get(NetworkMetrics m); }
    private static void colorGradientBars(JFreeChart chart,
                                          List<NetworkMetrics> sorted,
                                          DoubleExtractor extractor) {
        if (!(chart.getPlot() instanceof CategoryPlot)) return;
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        if (!(plot.getRenderer() instanceof BarRenderer)) return;
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        int n = sorted.size();
        for (int i = 0; i < n; i++) {
            float ratio = n > 1 ? (float) i / (n - 1) : 0f;
            Color c = new Color(
                (int)(ratio * 220),
                (int)((1 - ratio) * 200),
                50);
            renderer.setSeriesPaint(0, c);
        }
    }
    public static JFreeChart generateHighwayTypeChart(NetworkMetrics m) {
        if (m.getStreetStats() == null || m.getStreetStats().getHighwayTypeCounts().isEmpty()) {
            return null;
        }
        List<Map.Entry<String, Integer>> top = m.getStreetStats().getHighwayTypeCounts().entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> e : top) {
            ds.addValue(e.getValue(), "Segments", e.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Highway Types: " + m.getGraphName(),
                "OSM highway tag", "Edge segments",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, top.size());
        return chart;
    }
    public static JFreeChart generateHighwayMixComparisonChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            ds.addValue(s.getPctResidential(), city, "Residential");
            ds.addValue(s.getPctPrimary() + s.getPctMotorway() + s.getPctTrunk(), city, "Primary+");
            ds.addValue(s.getPctSecondary(), city, "Secondary");
            ds.addValue(s.getPctTertiary(), city, "Tertiary");
            ds.addValue(s.getPctFootway(), city, "Footway");
            ds.addValue(s.getPctCycleway(), city, "Cycle");
            ds.addValue(s.getPctOtherHighway() + s.getPctServiceHighway() + s.getPctPath(), city, "Other");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Highway Mix by City (length-weighted %)",
                "Category", "Share of network length (%)",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateStreetMobilityIndicatorsChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            ds.addValue(s.getStreetsPerNode(), city, "Streets/Node");
            ds.addValue(s.getOnewayRatio() * 100, city, "Oneway %");
            ds.addValue(s.getAvgMaxSpeedKmh(), city, "Avg Speed km/h");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "OSM Mobility Indicators by City",
                "Indicator", "Value",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateRoadLengthComparisonChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(m -> m.getStreetStats().getTotalLengthKm(), Comparator.reverseOrder()))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            ds.addValue(m.getStreetStats().getTotalLengthKm(), "Road km", shortName(m));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Total Road Length by City",
                "City", "Kilometres of streets",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateMobilityModesChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            ds.addValue(s.getPctResidential(), city, "Residential");
            ds.addValue(s.getCarOrientedLengthPct(), city, "Car-oriented");
            ds.addValue(s.getPedestrianFriendlyLengthPct(), city, "Pedestrian");
            ds.addValue(s.getCyclingFriendlyLengthPct(), city, "Cycling");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "How Streets Are Used (length-weighted %)",
                "Mode", "Share of network length (%)",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateNamedRoadsAndAccessChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            ds.addValue(s.getNamedRoadRatio() * 100, city, "Named roads %");
            ds.addValue(s.getRestrictedAccessLengthPct(), city, "Restricted access %");
            ds.addValue(s.getReversedRatio() * 100, city, "Reversed segments %");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Street Naming & Access Restrictions",
                "Indicator", "Percentage",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateInfrastructureCountsChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            ds.addValue(s.getBridgeCount(), city, "Bridges");
            ds.addValue(s.getTunnelCount(), city, "Tunnels");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Bridges and Tunnels by City",
                "City", "Segment count",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateSpeedAndLanesChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            if (s.getAvgMaxSpeedKmh() > 0) {
                ds.addValue(s.getAvgMaxSpeedKmh(), city, "Avg speed km/h");
            }
            if (s.getAvgLanes() > 0) {
                ds.addValue(s.getAvgLanes(), city, "Avg lanes");
            }
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Speed Limits and Lane Counts",
                "Indicator", "Value",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateStreetWidthChart(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withData = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (withData.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : withData) {
            StreetNetworkStats s = m.getStreetStats();
            String city = shortName(m);
            if (s.getAvgWidthMeters() > 0) {
                ds.addValue(s.getAvgWidthMeters(), city, "Avg width (m)");
            }
            if (s.getAvgEstWidthMeters() > 0) {
                ds.addValue(s.getAvgEstWidthMeters(), city, "Est. width (m)");
            }
        }
        if (ds.getColumnCount() == 0) return null;
        JFreeChart chart = ChartFactory.createBarChart(
                "Street Width (where tagged in OSM)",
                "City", "Metres",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, withData.size());
        return chart;
    }
    public static JFreeChart generateCombinedHighwayTypeChart(List<NetworkMetrics> metrics) {
        Map<String, Integer> combined = new LinkedHashMap<>();
        for (NetworkMetrics m : metrics) {
            if (m.getStreetStats() == null) continue;
            for (var e : m.getStreetStats().getHighwayTypeCounts().entrySet()) {
                combined.merge(e.getKey(), e.getValue(), Integer::sum);
            }
        }
        if (combined.isEmpty()) return null;
        List<Map.Entry<String, Integer>> top = combined.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(12)
                .collect(Collectors.toList());
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (Map.Entry<String, Integer> e : top) {
            ds.addValue(e.getValue(), "Segments", e.getKey());
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Most Common Street Types (all cities)",
                "OSM highway tag", "Road segments",
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, top.size());
        return chart;
    }
    public static JFreeChart generateSingleNetworkOsmProfileChart(NetworkMetrics m) {
        if (m.getStreetStats() == null || !m.getStreetStats().hasData()) return null;
        StreetNetworkStats s = m.getStreetStats();
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        String label = shortName(m);
        ds.addValue(s.getPctResidential(), label, "Residential %");
        ds.addValue(s.getCarOrientedLengthPct(), label, "Car-oriented %");
        ds.addValue(s.getPedestrianFriendlyLengthPct(), label, "Pedestrian %");
        ds.addValue(s.getCyclingFriendlyLengthPct(), label, "Cycling %");
        ds.addValue(s.getOnewayRatio() * 100, label, "Oneway %");
        ds.addValue(s.getNamedRoadRatio() * 100, label, "Named roads %");
        if (s.getAvgMaxSpeedKmh() > 0) {
            ds.addValue(s.getAvgMaxSpeedKmh(), label, "Avg speed km/h");
        }
        if (s.getAvgLanes() > 0) {
            ds.addValue(s.getAvgLanes(), label, "Avg lanes");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Street Network Profile: " + m.getGraphName(),
                "Indicator", "Value",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, 1);
        return chart;
    }
    public static JFreeChart generateSingleNetworkProfileChart(NetworkMetrics m) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        String label = shortName(m);
        safeAdd(ds, label, "Betweenness", m.getAvgBetweennessCentrality());
        safeAdd(ds, label, "Closeness", m.getAvgClosenessCentrality());
        safeAdd(ds, label, "Degree", m.getAvgDegreeCentrality());
        safeAdd(ds, label, "Clustering", m.getClusteringCoefficient());
        safeAdd(ds, label, "Density", m.getGraphDensity());
        safeAdd(ds, label, "Entropy", m.getGraphEntropy());
        safeAdd(ds, label, "Mean Degree", m.getMeanDegree());
        safeAdd(ds, label, "Reciprocity", m.getReciprocity());
        safeAdd(ds, label, "Assortativity", m.getAssortativityDegree());
        if (m.getGraphDiameter() != null) {
            ds.addValue(m.getGraphDiameter(), label, "Diameter");
        }
        safeAdd(ds, label, "Avg Path", m.getAvgPathLength());

        JFreeChart chart = ChartFactory.createBarChart(
                "Metric Profile: " + m.getGraphName(),
                "Metric", "Value",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, 1);
        return chart;
    }
    public static JFreeChart generateSingleNetworkSizeChart(NetworkMetrics m) {
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        ds.addValue(m.getNodeCount(), "Count", "Nodes");
        ds.addValue(m.getEdgeCount(), "Count", "Edges");
        JFreeChart chart = ChartFactory.createBarChart(
                "Network Size: " + m.getGraphName(),
                "Component", "Count",
                ds, PlotOrientation.VERTICAL, false, true, false);
        styleBarChart(chart, 1);
        return chart;
    }
    public static JFreeChart generateCitiesVsAverageChart(List<NetworkMetrics> metrics) {
        if (metrics.size() < 2) return null;
        double avgClustering = avgOf(metrics, NetworkMetrics::getClusteringCoefficient);
        double avgDensity = avgOf(metrics, NetworkMetrics::getGraphDensity);
        double avgPath = avgOf(metrics, NetworkMetrics::getAvgPathLength);
        double avgBetween = avgOf(metrics, NetworkMetrics::getAvgBetweennessCentrality);
        double avgDiameter = metrics.stream()
                .filter(m -> m.getGraphDiameter() != null)
                .mapToInt(NetworkMetrics::getGraphDiameter)
                .average().orElse(0);
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        List<NetworkMetrics> sorted = metrics.stream()
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        for (NetworkMetrics m : sorted) {
            String city = shortName(m);
            safeAdd(ds, city, "Clustering", m.getClusteringCoefficient());
            safeAdd(ds, city, "Density", m.getGraphDensity());
            safeAdd(ds, city, "Avg Path", m.getAvgPathLength());
            safeAdd(ds, city, "Betweenness", m.getAvgBetweennessCentrality());
            if (m.getGraphDiameter() != null) {
                ds.addValue(m.getGraphDiameter(), city, "Diameter");
            }
        }
        safeAdd(ds, "Average", "Clustering", avgClustering);
        safeAdd(ds, "Average", "Density", avgDensity);
        safeAdd(ds, "Average", "Avg Path", avgPath);
        safeAdd(ds, "Average", "Betweenness", avgBetween);
        if (avgDiameter > 0) {
            ds.addValue(avgDiameter, "Average", "Diameter");
        }
        JFreeChart chart = ChartFactory.createBarChart(
                "Each City vs Dataset Average",
                "Metric", "Value",
                ds, PlotOrientation.VERTICAL, true, true, false);
        styleBarChart(chart, sorted.size());
        return chart;
    }
    public static JFreeChart generateMultiCityTrendChart(List<NetworkMetrics> metrics) {
        if (metrics.size() < 2) return null;
        List<NetworkMetrics> sorted = metrics.stream()
                .sorted(Comparator.comparing(NetworkMetrics::getGraphName))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        XYSeriesCollection collection = new XYSeriesCollection();
        for (NetworkMetrics m : sorted) {
            XYSeries series = new XYSeries(shortName(m));
            series.add(0, norm(m.getClusteringCoefficient()));
            series.add(1, norm(m.getGraphDensity()));
            series.add(2, norm(m.getAvgBetweennessCentrality()));
            series.add(3, 1.0 - norm(m.getAvgPathLength()));
            series.add(4, m.getGraphDiameter() != null
                    ? 1.0 - norm((double) m.getGraphDiameter()) : 0);
            collection.addSeries(series);
        }
        JFreeChart chart = ChartFactory.createXYLineChart(
                "Normalized Metrics Across Cities",
                "Metric Index", "Normalised value (0–1)",
                collection, PlotOrientation.VERTICAL, true, true, false);
        styleMultiLineChart(chart, sorted.size());
        return chart;
    }
    private static double norm(Double v) {
        if (v == null) return 0;
        return Math.min(1.0, Math.max(0, v));
    }
    private static double avgOf(List<NetworkMetrics> metrics,
                                java.util.function.Function<NetworkMetrics, Double> getter) {
        return metrics.stream()
                .filter(m -> getter.apply(m) != null)
                .mapToDouble(m -> getter.apply(m))
                .average().orElse(0);
    }
    private static void styleMultiLineChart(JFreeChart chart, int cityCount) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 14));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setDomainGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        plot.setRangeGridlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, true);
        Color[] colours = ThemeManager.getCurrentTheme().getChartColors();
        for (int i = 0; i < plot.getDataset().getSeriesCount(); i++) {
            renderer.setSeriesPaint(i, colours[i % colours.length]);
        }
        plot.setRenderer(renderer);
    }
    public static JFreeChart generateWorldBankGdpChart(List<NetworkMetrics> metrics) {
        return generateWorldBankBarChart(metrics, WorldBankIndicators.GDP_PER_CAPITA,
                "GDP per Capita by City (country data)", "US$");
    }
    public static JFreeChart generateWorldBankUrbanPopChart(List<NetworkMetrics> metrics) {
        return generateWorldBankBarChart(metrics, WorldBankIndicators.URBAN_POP_PCT,
                "Urban Population % (country data)", "%");
    }
    public static JFreeChart generateWorldBankCo2Chart(List<NetworkMetrics> metrics) {
        return generateWorldBankBarChart(metrics, WorldBankIndicators.CO2_PER_CAPITA,
                "CO2 Emissions per Capita (country data)", "metric tons");
    }
    public static JFreeChart generateGdpVsClusteringScatter(List<NetworkMetrics> metrics) {
        logger.info("Generating GDP vs clustering scatter");
        XYSeries withGdp = new XYSeries("With World Bank GDP");
        XYSeries without = new XYSeries("No GDP data");
        for (NetworkMetrics m : metrics) {
            if (m.getClusteringCoefficient() == null) continue;
            Double gdp = WorldBankAnalysisUtil.getValue(m, WorldBankIndicators.GDP_PER_CAPITA);
            if (gdp != null) {
                withGdp.add(gdp, m.getClusteringCoefficient());
            } else {
                without.add(0, m.getClusteringCoefficient());
            }
        }
        XYSeriesCollection collection = new XYSeriesCollection();
        if (withGdp.getItemCount() > 0) collection.addSeries(withGdp);
        if (without.getItemCount() > 0) collection.addSeries(without);
        JFreeChart chart = ChartFactory.createScatterPlot(
                "GDP per Capita vs Clustering Coefficient",
                "GDP per capita (US$, country)", "Clustering coefficient",
                collection, PlotOrientation.VERTICAL, true, true, false);
        styleScatterChart(chart);
        return chart;
    }
    private static JFreeChart generateWorldBankBarChart(List<NetworkMetrics> metrics,
                                                        WorldBankIndicators indicator,
                                                        String title, String unitLabel) {
        List<NetworkMetrics> sorted = metrics.stream()
                .filter(m -> WorldBankAnalysisUtil.getValue(m, indicator) != null)
                .sorted(Comparator.comparingDouble(m ->
                        -WorldBankAnalysisUtil.getValue(m, indicator)))
                .limit(MAX_LABELS)
                .collect(Collectors.toList());
        if (sorted.isEmpty()) return null;
        DefaultCategoryDataset ds = new DefaultCategoryDataset();
        for (NetworkMetrics m : sorted) {
            ds.addValue(WorldBankAnalysisUtil.getValue(m, indicator), indicator.getShortLabel(), shortName(m));
        }
        JFreeChart chart = ChartFactory.createBarChart(
                title, "City (country proxy)", unitLabel,
                ds, PlotOrientation.HORIZONTAL, false, true, false);
        styleBarChart(chart, sorted.size());
        return chart;
    }
    private static String shortName(NetworkMetrics m) {
        String name = m.getGraphName();
        if (name == null) return "?";
        return name.length() > 16 ? name.substring(0, 14) + "…" : name;
    }
    private static void safeAdd(DefaultCategoryDataset ds, String rowKey,
                                 String colKey, Double value) {
        if (value != null) ds.addValue(value, rowKey, colKey);
    }
}