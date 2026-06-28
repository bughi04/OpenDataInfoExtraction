package org.example.service;

import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;
import org.example.model.NetworkMetrics;
import org.example.model.PredictionTarget;
import org.example.model.ReferenceCityRecord;
import org.example.model.StreetNetworkStats;
import org.example.model.WorldBankCityData;
import org.example.util.MetricsStatisticsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.ToDoubleFunction;

// Predicts UMRi from graph and street-network metrics.
public class MobilityReadinessPredictor {
    public enum ModelType {
        OLS_STRUCTURAL("Structural OLS",
                "Clustering, assortativity, streets/node, diameter"),
        OLS_FULL("Full OLS",
                "Structural features + density, degree centrality, path length, GDP"),
        PAPER_LINEAR("Paper linear model",
                "Fixed coefficients from Sierra-Porta & Herrera-Acevedo (2024)");
        private final String label;
        private final String description;
        ModelType(String label, String description) {
            this.label = label;
            this.description = description;
        }
        public String getLabel() { return label; }
        public String getDescription() { return description; }
        @Override
        public String toString() { return label; }
    }
    public record FeatureCoefficient(String name, double coefficient) {}
    public record ModelEvaluation(
            ModelType modelType,
            PredictionTarget target,
            int trainingSamples,
            double rSquared,
            double adjustedRSquared,
            double mae,
            double rmse,
            List<FeatureCoefficient> coefficients,
            double intercept,
            double[] featureMeans,
            double[] featureStds
    ) {
        public String summaryLine() {
            return String.format(Locale.ROOT,
                    "%s - %s: trained on %d cities, LOOCV R²=%.3f, MAE=%.2f, RMSE=%.2f",
                    modelType.getLabel(), target.getShortLabel(),
                    trainingSamples, rSquared, mae, rmse);
        }
    }
    public record CityPrediction(
            String cityName,
            Double actualScore,
            double predictedScore,
            boolean hasReferenceLabel
    ) {
        public Double residual() {
            return actualScore != null ? actualScore - predictedScore : null;
        }
    }
    private static final String[] STRUCTURAL_FEATURES = {
            "Clustering coefficient",
            "Assortativity degree",
            "Streets per node",
            "Graph diameter"
    };
    private static final String[] FULL_FEATURES = {
            "Clustering coefficient",
            "Assortativity degree",
            "Streets per node",
            "Graph diameter",
            "Graph density",
            "Degree centrality",
            "Avg path length",
            "GDP per capita ($)"
    };
    private final ReferenceCityDatasetLoader referenceData;
    private final CityScoresLoader cityScores;
    public MobilityReadinessPredictor(ReferenceCityDatasetLoader referenceData, CityScoresLoader cityScores) {
        this.referenceData = referenceData;
        this.cityScores = cityScores;
    }
    public boolean isReady() {
        return referenceData != null && referenceData.isLoaded();
    }
    public Map<String, Double> featureCorrelations(PredictionTarget target) {
        return featureCorrelationsWithTarget(referenceData, target);
    }
    public ModelEvaluation evaluateModel(ModelType modelType, PredictionTarget target) {
        if (!isReady()) return null;
        return switch (modelType) {
            case OLS_STRUCTURAL, OLS_FULL -> evaluateOls(modelType, target);
            case PAPER_LINEAR -> evaluatePaperModel(target);
        };
    }
    public List<CityPrediction> predictLoadedCities(
            List<NetworkMetrics> metrics,
            ModelType modelType,
            PredictionTarget target) {
        if (metrics == null || metrics.isEmpty()) return List.of();
        ModelEvaluation eval = evaluateModel(modelType, target);
        if (eval == null) return List.of();
        List<CityPrediction> out = new ArrayList<>();
        for (NetworkMetrics m : metrics) {
            double[] features = extractFeatures(m, modelType);
            if (features == null) continue;

            double predicted = predictFromEvaluation(features, eval);
            Double actual = actualScore(m, target);
            out.add(new CityPrediction(m.getGraphName(), actual, predicted, actual != null));
        }
        return out;
    }
    public String explainMissingFeatures(List<NetworkMetrics> metrics, ModelType modelType) {
        if (metrics == null || metrics.isEmpty()) {
            return "no cities loaded";
        }
        List<String> issues = new ArrayList<>();
        for (NetworkMetrics m : metrics) {
            String issue = missingFeatureReason(m, modelType);
            if (issue != null) {
                issues.add(m.getGraphName() + ": " + issue);
            }
        }
        if (issues.isEmpty()) {
            return "unknown; try reprocessing after updating the app";
        }
        return String.join("; ", issues);
    }
    private String missingFeatureReason(NetworkMetrics m, ModelType modelType) {
        if (m.getClusteringCoefficient() == null) return "clustering coefficient missing";
        if (m.getAssortativityDegree() == null) {
            return "assortativity missing; re-process the GraphML file";
        }
        if (m.getGraphDiameter() == null) return "graph diameter missing";
        if (streetsPerNodeForModel(m.getStreetStats()) == null) {
            return "streets-per-node (OSM street_count) missing";
        }
        if (modelType == ModelType.OLS_FULL) {
            if (m.getGraphDensity() == null) return "graph density missing";
            if (m.getAvgDegreeCentrality() == null) return "degree centrality missing";
            if (m.getAvgPathLength() == null) return "average path length missing";
            if (gdpFromMetrics(m) == null) return "GDP unavailable (refresh World Bank or use Structural OLS)";
        }
        if (modelType == ModelType.PAPER_LINEAR && gdpFromMetrics(m) == null) {
            return "GDP unavailable (refresh World Bank data)";
        }
        return null;
    }
    public List<CityPrediction> predictReferenceHoldout(ModelType modelType, PredictionTarget target) {
        if (!isReady()) return List.of();
        List<ReferenceCityRecord> records = referenceData.getRecords();
        List<CityPrediction> out = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            ReferenceCityRecord row = records.get(i);
            double[] features = extractFeatures(row, modelType);
            if (features == null) continue;
            double predicted;
            if (modelType == ModelType.PAPER_LINEAR) {
                predicted = predictPaper(features);
            } else {
                FittedModel fold = trainOlsExcluding(records, i, modelType, target);
                if (fold == null) continue;
                predicted = fold.intercept();
                double[] scaled = scaleFeatures(features, fold.featureMeans(), fold.featureStds());
                for (int j = 0; j < scaled.length && j < fold.coefficients().length; j++) {
                    predicted += fold.coefficients()[j] * scaled[j];
                }
            }
            out.add(new CityPrediction(row.getCityAddress(), row.targetScore(target), predicted, true));
        }
        return out;
    }
    private static double predictFromEvaluation(double[] features, ModelEvaluation eval) {
        if (eval.modelType() == ModelType.PAPER_LINEAR) {
            return predictPaper(features);
        }
        double[] x = scaleFeatures(features, eval.featureMeans(), eval.featureStds());
        double sum = eval.intercept();
        for (int i = 0; i < x.length && i < eval.coefficients().size(); i++) {
            sum += eval.coefficients().get(i).coefficient() * x[i];
        }
        return sum;
    }
    private ModelEvaluation evaluateOls(ModelType modelType, PredictionTarget target) {
        List<ReferenceCityRecord> records = referenceData.getRecords();
        List<Double> actuals = new ArrayList<>();
        List<Double> preds = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            FittedModel fold = trainOlsExcluding(records, i, modelType, target);
            if (fold == null) continue;
            ReferenceCityRecord row = records.get(i);
            double[] features = extractFeatures(row, modelType);
            if (features == null) continue;
            double y = row.targetScore(target);
            double yHat = fold.intercept();
            double[] scaled = scaleFeatures(features, fold.featureMeans(), fold.featureStds());
            for (int j = 0; j < scaled.length && j < fold.coefficients().length; j++) {
                yHat += fold.coefficients()[j] * scaled[j];
            }
            actuals.add(y);
            preds.add(yHat);
        }
        if (actuals.size() < 5) return null;
        FittedModel full = trainOls(records, modelType, target);
        if (full == null) return null;
        double r2 = rSquared(actuals, preds);
        double mae = mae(actuals, preds);
        double rmse = rmse(actuals, preds);
        int k = full.coefficients().length;
        return new ModelEvaluation(
                modelType,
                target,
                records.size(),
                r2,
                adjustedRSquared(r2, actuals.size(), k),
                mae,
                rmse,
                toCoefficientList(modelType, full.coefficients()),
                full.intercept(),
                full.featureMeans(),
                full.featureStds()
        );
    }
    private FittedModel trainOls(List<ReferenceCityRecord> records, ModelType modelType, PredictionTarget target) {
        List<double[]> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();

        for (ReferenceCityRecord row : records) {
            double[] features = extractFeatures(row, modelType);
            if (features == null) continue;
            xs.add(features);
            ys.add(row.targetScore(target));
        }
        return fitOls(xs, ys, modelType);
    }
    private FittedModel trainOlsExcluding(
            List<ReferenceCityRecord> records, int excludeIndex,
            ModelType modelType, PredictionTarget target) {

        List<double[]> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();

        for (int i = 0; i < records.size(); i++) {
            if (i == excludeIndex) continue;
            ReferenceCityRecord row = records.get(i);
            double[] features = extractFeatures(row, modelType);
            if (features == null) continue;
            xs.add(features);
            ys.add(row.targetScore(target));
        }
        return fitOls(xs, ys, modelType);
    }
    private FittedModel fitOls(List<double[]> xs, List<Double> ys, ModelType modelType) {
        if (xs.isEmpty() || xs.size() != ys.size() || xs.size() < 8) return null;

        int n = xs.size();
        int k = xs.get(0).length;
        boolean standardize = modelType == ModelType.OLS_FULL;

        double[] means = new double[k];
        double[] stds = new double[k];
        for (int j = 0; j < k; j++) {
            double sum = 0;
            for (int i = 0; i < n; i++) {
                if (xs.get(i).length != k) return null;
                sum += xs.get(i)[j];
            }
            means[j] = sum / n;
            double var = 0;
            for (int i = 0; i < n; i++) {
                double d = xs.get(i)[j] - means[j];
                var += d * d;
            }
            stds[j] = standardize ? Math.sqrt(var / n) : 1.0;
            if (stds[j] < 1e-12) stds[j] = 1.0;
        }
        double[][] x = new double[n][k];
        double[] y = new double[n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < k; j++) {
                double raw = xs.get(i)[j];
                x[i][j] = standardize ? (raw - means[j]) / stds[j] : raw;
            }
            y[i] = ys.get(i);
        }
        try {
            OLSMultipleLinearRegression regression = new OLSMultipleLinearRegression();
            regression.newSampleData(y, x);
            double[] beta = regression.estimateRegressionParameters();
            double intercept = beta[0];
            double[] coeffs = new double[k];
            System.arraycopy(beta, 1, coeffs, 0, k);
            return new FittedModel(
                    intercept,
                    coeffs,
                    standardize ? means : null,
                    standardize ? stds : null);
        } catch (Exception e) {
            return null;
        }
    }
    private ModelEvaluation evaluatePaperModel(PredictionTarget target) {
        if (target != PredictionTarget.UMRI) {
            return null;
        }
        List<ReferenceCityRecord> records = referenceData.getRecords();
        List<Double> actuals = new ArrayList<>();
        List<Double> preds = new ArrayList<>();
        for (ReferenceCityRecord row : records) {
            double[] f = extractPaperFeatures(row);
            if (f == null) continue;
            actuals.add(row.getUmriScore());
            preds.add(predictPaper(f));
        }
        if (actuals.isEmpty()) return null;
        return new ModelEvaluation(
                ModelType.PAPER_LINEAR,
                target,
                actuals.size(),
                rSquared(actuals, preds),
                adjustedRSquared(rSquared(actuals, preds), actuals.size(), 5),
                mae(actuals, preds),
                rmse(actuals, preds),
                List.of(
                        new FeatureCoefficient("Clustering coefficient", 190.4),
                        new FeatureCoefficient("Assortativity degree", -45.2),
                        new FeatureCoefficient("Streets per node", 29.1),
                        new FeatureCoefficient("Graph diameter", -0.05),
                        new FeatureCoefficient("GDP per capita ($)", 0.0002)
                ),
                0.0,
                null,
                null
        );
    }
    private static double predictPaper(double[] f) {
        return 190.4 * f[0] - 45.2 * f[1] + 29.1 * f[2] - 0.05 * f[3] + 0.0002 * f[4];
    }
    private double[] extractPaperFeatures(ReferenceCityRecord row) {
        if (!finite(row.getClusteringCoefficient(), row.getAssortativityDegree(),
                row.getStreetsPerNode(), row.getGraphDiameter(), row.getGdpPerCapita())) {
            return null;
        }
        return new double[]{
                row.getClusteringCoefficient(),
                row.getAssortativityDegree(),
                row.getStreetsPerNode(),
                row.getGraphDiameter(),
                row.getGdpPerCapita()
        };
    }
    private double[] extractFeatures(ReferenceCityRecord row, ModelType modelType) {
        return switch (modelType) {
            case OLS_STRUCTURAL -> finite4(row.getClusteringCoefficient(), row.getAssortativityDegree(),
                    row.getStreetsPerNode(), row.getGraphDiameter())
                    ? new double[]{
                    row.getClusteringCoefficient(),
                    row.getAssortativityDegree(),
                    row.getStreetsPerNode(),
                    row.getGraphDiameter()
            } : null;
            case OLS_FULL -> finite(row.getClusteringCoefficient(), row.getAssortativityDegree(),
                    row.getStreetsPerNode(), row.getGraphDiameter(), row.getGraphDensity(),
                    row.getAvgDegreeCentrality(), row.getAvgPathLength(), row.getGdpPerCapita())
                    ? new double[]{
                    row.getClusteringCoefficient(),
                    row.getAssortativityDegree(),
                    row.getStreetsPerNode(),
                    row.getGraphDiameter(),
                    row.getGraphDensity(),
                    row.getAvgDegreeCentrality(),
                    row.getAvgPathLength(),
                    row.getGdpPerCapita()
            } : null;
            case PAPER_LINEAR -> extractPaperFeatures(row);
        };
    }
    private double[] extractFeatures(NetworkMetrics m, ModelType modelType) {
        Double clustering = m.getClusteringCoefficient();
        Double assort = m.getAssortativityDegree();
        Double diameter = m.getGraphDiameter() != null ? m.getGraphDiameter().doubleValue() : null;
        StreetNetworkStats street = m.getStreetStats();
        Double streetsPerNode = streetsPerNodeForModel(street);
        if (clustering == null || assort == null || diameter == null || streetsPerNode == null) {
            return null;
        }
        return switch (modelType) {
            case OLS_STRUCTURAL -> new double[]{
                    clustering, assort, streetsPerNode, diameter
            };
            case OLS_FULL -> {
                Double density = m.getGraphDensity();
                Double degree = m.getAvgDegreeCentrality();
                Double path = m.getAvgPathLength();
                Double gdp = gdpFromMetrics(m);
                if (density == null || degree == null || path == null || gdp == null) yield null;
                yield new double[]{clustering, assort, streetsPerNode, diameter, density, degree, path, gdp};
            }
            case PAPER_LINEAR -> {
                Double gdp = gdpFromMetrics(m);
                if (gdp == null) yield null;
                yield new double[]{clustering, assort, streetsPerNode, diameter, gdp};
            }
        };
    }
    private static Double streetsPerNodeForModel(StreetNetworkStats street) {
        if (street == null || !street.hasData()) {
            return null;
        }
        double osmAvg = street.getAvgStreetCountPerNode();
        if (osmAvg > 0) {
            return osmAvg;
        }
        double fallback = street.getStreetsPerNode();
        return fallback > 0 ? fallback : null;
    }
    private Double gdpFromMetrics(NetworkMetrics m) {
        WorldBankCityData wb = m.getWorldBankData();
        if (wb != null && wb.isUsable()) {
            Double v = wb.getNumericValue(WorldBankIndicators.GDP_PER_CAPITA.getId());
            if (v != null && !Double.isNaN(v) && !Double.isInfinite(v)) {
                return v;
            }
        }
        Double fromRef = referenceData.gdpForCityName(m.getGraphName());
        if (fromRef != null && !Double.isNaN(fromRef)) {
            return fromRef;
        }
        if (m.getGraphFile() != null) {
            fromRef = referenceData.gdpForCityName(new java.io.File(m.getGraphFile()).getName());
            if (fromRef != null && !Double.isNaN(fromRef)) {
                return fromRef;
            }
        }
        double median = referenceData.medianGdpPerCapita();
        return Double.isNaN(median) ? null : median;
    }
    private Double actualScore(NetworkMetrics m, PredictionTarget target) {
        if (cityScores == null || !cityScores.isLoaded()) return null;
        double[] scores = cityScores.getScores(m.getGraphName() + ".graphml");
        if (scores == null && m.getGraphFile() != null) {
            scores = cityScores.getScores(m.getGraphFile());
        }
        if (scores == null) return null;
        return switch (target) {
            case UMRI -> scores[1];
            case SMI -> scores[2];
            case PTI -> scores[3];
        };
    }
    private static List<FeatureCoefficient> toCoefficientList(ModelType type, double[] coeffs) {
        String[] names = type == ModelType.OLS_FULL ? FULL_FEATURES : STRUCTURAL_FEATURES;
        List<FeatureCoefficient> list = new ArrayList<>();
        for (int i = 0; i < coeffs.length && i < names.length; i++) {
            list.add(new FeatureCoefficient(names[i], coeffs[i]));
        }
        return list;
    }
    private static double rSquared(List<Double> actual, List<Double> predicted) {
        double mean = actual.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double ssTot = 0, ssRes = 0;
        for (int i = 0; i < actual.size(); i++) {
            double y = actual.get(i);
            double e = y - predicted.get(i);
            ssRes += e * e;
            ssTot += (y - mean) * (y - mean);
        }
        return ssTot < 1e-12 ? 0 : 1 - ssRes / ssTot;
    }
    private static double adjustedRSquared(double r2, int n, int k) {
        if (n <= k + 1) return r2;
        return 1 - (1 - r2) * (n - 1) / (n - k - 1);
    }
    private static double mae(List<Double> actual, List<Double> predicted) {
        double sum = 0;
        for (int i = 0; i < actual.size(); i++) {
            sum += Math.abs(actual.get(i) - predicted.get(i));
        }
        return sum / actual.size();
    }
    private static double rmse(List<Double> actual, List<Double> predicted) {
        double sum = 0;
        for (int i = 0; i < actual.size(); i++) {
            double e = actual.get(i) - predicted.get(i);
            sum += e * e;
        }
        return Math.sqrt(sum / actual.size());
    }
    private static boolean finite(double... values) {
        for (double v : values) {
            if (Double.isNaN(v) || Double.isInfinite(v)) return false;
        }
        return true;
    }
    private static boolean finite4(double a, double b, double c, double d) {
        return finite(a, b, c, d);
    }
    public static Map<String, Double> featureCorrelationsWithTarget(
            ReferenceCityDatasetLoader data, PredictionTarget target) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (data == null || !data.isLoaded()) return out;
        List<ReferenceCityRecord> rows = data.getRecords();
        out.put("Clustering", pearson(rows, ReferenceCityRecord::getClusteringCoefficient, target));
        out.put("Assortativity", pearson(rows, ReferenceCityRecord::getAssortativityDegree, target));
        out.put("Streets/node", pearson(rows, ReferenceCityRecord::getStreetsPerNode, target));
        out.put("Diameter", pearson(rows, ReferenceCityRecord::getGraphDiameter, target));
        out.put("Density", pearson(rows, ReferenceCityRecord::getGraphDensity, target));
        out.put("Degree centrality", pearson(rows, ReferenceCityRecord::getAvgDegreeCentrality, target));
        out.put("Avg path length", pearson(rows, ReferenceCityRecord::getAvgPathLength, target));
        out.put("GDP/capita", pearson(rows, ReferenceCityRecord::getGdpPerCapita, target));
        return out;
    }
    private static double pearson(List<ReferenceCityRecord> rows,
                                  ToDoubleFunction<ReferenceCityRecord> feature,
                                  PredictionTarget target) {
        List<Double> xs = new ArrayList<>();
        List<Double> ys = new ArrayList<>();
        for (ReferenceCityRecord row : rows) {
            double x = feature.applyAsDouble(row);
            double y = row.targetScore(target);
            if (!Double.isNaN(x) && !Double.isInfinite(x)) {
                xs.add(x);
                ys.add(y);
            }
        }
        if (xs.size() < 3) return 0;
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
        if (sumX2 == 0 || sumY2 == 0) return 0;
        return sumXY / Math.sqrt(sumX2 * sumY2);
    }
    public static double compositeHeuristicScore(NetworkMetrics m, List<NetworkMetrics> context) {
        int idx = context.indexOf(m);
        if (idx < 0) return Double.NaN;
        double[] scores = MetricsStatisticsService.computeCompositeScores(context);
        return scores[idx] * 100.0;
    }
    private record FittedModel(double intercept, double[] coefficients, double[] featureMeans, double[] featureStds) {}
    private static double[] scaleFeatures(double[] raw, double[] means, double[] stds) {
        if (means == null || stds == null) {
            return raw;
        }
        double[] out = new double[raw.length];
        for (int i = 0; i < raw.length; i++) {
            double std = stds[i] == 0 ? 1.0 : stds[i];
            out[i] = (raw[i] - means[i]) / std;
        }
        return out;
    }
}