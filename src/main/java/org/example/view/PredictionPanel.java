package org.example.view;

import org.example.model.NetworkMetrics;
import org.example.model.PredictionTarget;
import org.example.service.MobilityReadinessPredictor;
import org.example.service.MobilityReadinessPredictor.CityPrediction;
import org.example.service.MobilityReadinessPredictor.ModelEvaluation;
import org.example.service.MobilityReadinessPredictor.ModelType;
import org.example.theme.ReadableTextKit;
import org.example.theme.UiStyles;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Tab panel for UMRi prediction from graph metrics, with a beginner guide on the left
public class PredictionPanel extends JPanel {
    private static final PredictionTarget TARGET = PredictionTarget.UMRI;
    private final JComboBox<ModelType> modelCombo =
            new JComboBox<>(ModelType.values());
    private final JLabel statusLabel = new JLabel(" ");
    private final JTextPane guidePane = ReadableTextKit.createReadingPane();
    private final DefaultTableModel coeffModel = new DefaultTableModel(
            new String[]{"Feature", "Coefficient"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final DefaultTableModel predictionModel = new DefaultTableModel(
            new String[]{"City", "Actual UMRi (2023)", "Predicted (2023)", "Error"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private MobilityReadinessPredictor predictor;
    private List<NetworkMetrics> currentMetrics = List.of();
    public PredictionPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyles.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        buildUi();
        showBeginnerGuide(null, null);
    }
    public void setPredictor(MobilityReadinessPredictor predictor) {
        this.predictor = predictor;
        refresh();
    }
    public void updateMetrics(List<NetworkMetrics> metrics) {
        this.currentMetrics = metrics != null ? List.copyOf(metrics) : List.of();
        refresh();
    }
    private void buildUi() {
        JLabel heading = UiStyles.sectionHeading("UMRi Prediction");
        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        JLabel intro = new JLabel(
                "<html>Estimates the <b>Urban Mobility Readiness Index (UMRi)</b> from your street-network "
                        + "graph metrics, using a model trained on 62 reference cities.</html>");
        intro.setFont(UiStyles.FONT_UI);
        intro.setForeground(UiStyles.MUTED);
        north.add(intro, BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("Regression model:"));
        modelCombo.setFont(UiStyles.FONT_UI);
        modelCombo.setSelectedItem(ModelType.OLS_STRUCTURAL);
        modelCombo.addActionListener(e -> refresh());
        toolbar.add(modelCombo);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        JScrollPane guideScroll = ReadableTextKit.readingScroll(guidePane, "About UMRi & prediction");
        JTable coeffTable = new JTable(coeffModel);
        coeffTable.setFont(ReadableTextKit.FONT_MONO_SMALL);
        coeffTable.setRowHeight(22);
        JScrollPane coeffScroll = new JScrollPane(coeffTable);
        coeffScroll.setBorder(UiStyles.titledBorder("Regression coefficients"));
        JTable predTable = new JTable(predictionModel);
        predTable.setFont(ReadableTextKit.FONT_MONO_SMALL);
        predTable.setRowHeight(24);
        predTable.getTableHeader().setFont(UiStyles.FONT_UI_BOLD);
        JScrollPane predScroll = new JScrollPane(predTable);
        predScroll.setBorder(UiStyles.titledBorder("Predictions for loaded cities"));
        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, coeffScroll, predScroll);
        rightSplit.setResizeWeight(0.35);
        rightSplit.setBorder(null);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, guideScroll, rightSplit);
        mainSplit.setResizeWeight(0.45);
        mainSplit.setBorder(null);
        add(mainSplit, BorderLayout.CENTER);
        statusLabel.setFont(UiStyles.FONT_UI);
        statusLabel.setForeground(UiStyles.MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }
    private void refresh() {
        coeffModel.setRowCount(0);
        predictionModel.setRowCount(0);
        if (predictor == null || !predictor.isReady()) {
            showBeginnerGuide(null, null);
            statusLabel.setText("Prediction unavailable: place Results_Cities.csv in DataSets/.");
            return;
        }
        ModelType modelType = (ModelType) modelCombo.getSelectedItem();
        if (modelType == null) return;

        ModelEvaluation eval = predictor.evaluateModel(modelType, TARGET);
        if (eval == null) {
            showBeginnerGuide(modelType, null);
            statusLabel.setText("Could not train model: try Structural OLS.");
            return;
        }
        showBeginnerGuide(modelType, eval);
        for (var c : eval.coefficients()) {
            coeffModel.addRow(new Object[]{
                    c.name(),
                    String.format(Locale.ROOT, "%+.4f", c.coefficient())
            });
        }
        List<CityPrediction> predictions =
                predictor.predictLoadedCities(currentMetrics, modelType, TARGET);
        if (predictions.isEmpty()) {
            String hint = currentMetrics.isEmpty()
                    ? "process GraphML files first"
                    : predictor.explainMissingFeatures(currentMetrics, modelType);
            predictionModel.addRow(new Object[]{
                    "(" + hint + ")", "-", "-", "-"
            });
        } else {
            for (CityPrediction p : predictions) {
                predictionModel.addRow(new Object[]{
                        p.cityName(),
                        formatScore(p.actualScore()),
                        String.format(Locale.ROOT, "%.1f", p.predictedScore()),
                        formatError(p.residual())
                });
            }
        }
        long withActual = predictions.stream().filter(CityPrediction::hasReferenceLabel).count();
        statusLabel.setText(String.format(Locale.ROOT,
                "%d UMRi prediction(s); %d matched to official 2023 reference scores.",
                predictions.size(), withActual));
    }
    private void showBeginnerGuide(ModelType modelType, ModelEvaluation eval) {
        StringBuilder text = new StringBuilder();
        text.append("1. What is UMRi?\n");
        text.append("The Urban Mobility Readiness Index (UMRi) is an international benchmark published by "
                + "the Oliver Wyman Forum (2023). It scores how well a city is prepared for the future of "
                + "urban mobility on a scale of roughly 0–100 (higher = better).\n\n");
        text.append("UMRi combines many factors: public transit quality, sustainability, infrastructure, "
                + "and socioeconomic context. This tool focuses on the structural part: what can be "
                + "inferred from the shape of the street network alone.\n\n");

        text.append("2. How does this tab predict UMRi?\n");
        text.append("1. You process OSM street-network GraphML files (Files tab).\n");
        text.append("2. The app computes graph metrics: clustering, assortativity, streets per node, "
                + "diameter, and optionally GDP.\n");
        text.append("3. A regression model (trained on 62 cities from Results_Cities.csv) estimates "
                + "each city's UMRi from those metrics.\n");
        text.append("4. If your city is in the reference list, the table shows the official score "
                + "next to the prediction so you can compare.\n");
        text.append("(Computing metrics from GraphML can take seconds to hours depending on city size; "
                + "the UMRi estimate itself is then immediate.)\n\n");

        text.append("3. What time period does the predicted UMRi refer to?\n");
        text.append("The number in the Predicted column is an estimate of the 2023 UMRi, the same "
                + "Urban Mobility Readiness Index published by the Oliver Wyman Forum in 2023.\n\n");
        text.append("It is not a live score for today and not a forecast for future years. The model "
                + "answers: “Given this street network’s structure (and GDP if using Full OLS), what "
                + "UMRi score would this city likely have received under the 2023 benchmark?”\n\n");
        text.append("  • Official scores in the Actual column are also from that 2023 release.\n");
        text.append("  • Your GraphML street data may come from a different OSM snapshot date, but "
                + "the score scale and training labels are fixed to 2023.\n");
        text.append("  • The tool does not predict how UMRi will evolve in 2024, 2025, or beyond.\n\n");

        text.append("4. How accurate are predictions?\n");
        text.append("On the 62 reference cities, the structural model achieves leave-one-out "
                + "cross-validation with typical error around 7–10 UMRi points. That means a "
                + "prediction of 62 might correspond to an official score anywhere from ~52 to ~72.\n\n");
        text.append("Accuracy is best when your GraphML files were built the same way as the "
                + "reference dataset (OSMnx, similar city boundaries). Different graph sizes or "
                + "extraction methods will widen the gap.\n\n");

        text.append("5. Choosing a model\n");
        text.append("  • Structural OLS: four graph features only; works without World Bank GDP.\n");
        text.append("  • Full OLS: adds density, centrality, path length, and GDP (needs World Bank "
                + "tab or a reference-city match).\n");
        text.append("  • Paper linear: fixed coefficients from Sierra-Porta & Herrera-Acevedo (2024).\n\n");
        if (modelType != null && eval != null) {
            text.append("---\n\n");
            text.append("6. Current model run\n");
            text.append(eval.summaryLine()).append("\n");
            text.append("Model: ").append(modelType.getDescription()).append("\n");
            text.append(String.format(Locale.ROOT, "Intercept: %.4f%n%n", eval.intercept()));
            text.append("Feature correlations with UMRi (62 reference cities):\n");
            for (Map.Entry<String, Double> e : predictor.featureCorrelations(TARGET).entrySet()) {
                text.append(String.format(Locale.ROOT, "  %-18s r = %+.3f%n", e.getKey(), e.getValue()));
            }
            if (modelType == ModelType.OLS_FULL) {
                text.append("\n(Full OLS coefficients apply to standardized features.)\n");
            }
        } else if (predictor == null || !predictor.isReady()) {
            text.append("---\n\n");
            text.append("6. Setup required\n");
            text.append("Place Results_Cities.csv in the DataSets/ folder to enable prediction.\n");
        }
        ReadableTextKit.setReportText(guidePane, text.toString());
    }
    private static String formatScore(Double v) {
        return v != null ? String.format(Locale.ROOT, "%.1f", v) : "-";
    }
    private static String formatError(Double residual) {
        if (residual == null) return "-";
        return String.format(Locale.ROOT, "%+.1f", residual);
    }
}