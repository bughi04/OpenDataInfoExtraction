package org.example.service;

import org.example.model.PredictionTarget;
import org.example.service.MobilityReadinessPredictor.CityPrediction;
import org.example.service.MobilityReadinessPredictor.ModelEvaluation;
import org.example.service.MobilityReadinessPredictor.ModelType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
// Integration tests for UMRi/SMi/PTi prediction models trained on the 62-city reference dataset, including cross-validation metrics.
class MobilityReadinessPredictorTest {
    private static MobilityReadinessPredictor predictor;
    @BeforeAll
    static void loadReferenceData() {
        File csv = new File("DataSets/Results_Cities.csv");
        if (!csv.isFile()) {
            csv = new File("Results_Cities.csv");
        }
        assertTrue(csv.isFile(), "Results_Cities.csv required for predictor tests");

        CityScoresLoader scores = new CityScoresLoader(csv);
        ReferenceCityDatasetLoader reference = new ReferenceCityDatasetLoader(csv);
        predictor = new MobilityReadinessPredictor(reference, scores);
        assertTrue(predictor.isReady());
    }
    @Test
    void structuralOlsTrainsOnReferenceCities() {
        ModelEvaluation eval = predictor.evaluateModel(ModelType.OLS_STRUCTURAL, PredictionTarget.UMRI);
        assertNotNull(eval);
        assertEquals(62, eval.trainingSamples());
        assertEquals(4, eval.coefficients().size());
        assertTrue(eval.rSquared() > 0.3, "UMRi should correlate with structural metrics");
        assertTrue(eval.mae() < 15.0);
    }
    @Test
    void leaveOneOutHoldoutCoversAllReferenceCities() {
        List<CityPrediction> holdout =
                predictor.predictReferenceHoldout(ModelType.OLS_STRUCTURAL, PredictionTarget.UMRI);
        assertEquals(62, holdout.size());
        holdout.forEach(p -> {
            assertNotNull(p.actualScore());
            assertTrue(p.predictedScore() > 0 && p.predictedScore() < 100);
        });
    }
    @Test
    void featureCorrelationsAreFinite() {
        Map<String, Double> corr = predictor.featureCorrelations(PredictionTarget.UMRI);
        assertFalse(corr.isEmpty());
        corr.values().forEach(v -> assertTrue(Math.abs(v) <= 1.0));
    }
    @Test
    void paperModelOnlySupportsUmri() {
        assertNotNull(predictor.evaluateModel(ModelType.PAPER_LINEAR, PredictionTarget.UMRI));
        assertNull(predictor.evaluateModel(ModelType.PAPER_LINEAR, PredictionTarget.SMI));
    }
}