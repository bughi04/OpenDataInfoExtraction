package org.example.service;

import org.example.model.NetworkMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests the rule-based local insight engine: overview, best-vs-worst, question routing, and LLM system-prompt content.
class LocalInsightEngineTest {
    private LocalInsightEngine engine;
    private List<NetworkMetrics> metrics;
    @BeforeEach
    void setUp() {
        engine = new LocalInsightEngine();
        metrics = List.of(
            city("Helsinki,_Finland", 0.09, 0.0012, 0.04, 55, 9.0),
            city("Lagos,_Nigeria", 0.02, 0.0006, 0.015, 130, 14.0),
            city("Paris,_France", 0.06, 0.0010, 0.03, 75, 11.0)
        );
    }
    @Test
    void overviewHighlightsBestAndWorstInLoadedSet() {
        String text = engine.generateOverview(metrics, null);
        assertTrue(text.contains("AI NETWORK ADVISOR"));
        assertTrue(text.contains("Helsinki,_Finland"));
        assertTrue(text.contains("Lagos,_Nigeria"));
        assertTrue(text.contains("Composite structural readiness ranking"));
        assertTrue(text.contains("Strongest Correlations"));
    }
    @Test
    void bestVsWorstRequiresAtLeastTwoNetworks() {
        assertTrue(engine.generateBestVsWorst(List.of(metrics.get(0)), null)
            .contains("Need at least 2"));
    }
    @Test
    void bestVsWorstProducesHeadToHeadComparison() {
        String text = engine.generateBestVsWorst(metrics, null);
        assertTrue(text.contains("BEST vs WORST"));
        assertTrue(text.contains("Comparing"));
        assertTrue(text.contains("Interpretation"));
    }
    @Test
    void answerQuestionRoutesToCityComparison() {
        String text = engine.answerQuestion("compare Helsinki vs Lagos", metrics, null);
        assertTrue(text.contains("CITY COMPARISON"));
        assertTrue(text.contains("Helsinki,_Finland"));
        assertTrue(text.contains("Lagos,_Nigeria"));
    }
    @Test
    void answerQuestionRoutesToRegionalAnalysis() {
        String text = engine.answerQuestion("which country has the most cities?", metrics, null);
        assertTrue(text.contains("REGIONAL / COUNTRY COMPARISON"));
        assertTrue(text.contains("Average clustering by country"));
    }
    @Test
    void answerQuestionEmptyPromptReturnsHint() {
        assertTrue(engine.answerQuestion("   ", metrics, null).contains("Please type a question"));
    }
    @Test
    void buildLlmSystemPromptMentionsUmri() {
        assertTrue(LocalInsightEngine.buildLlmSystemPrompt().contains("UMRi"));
    }
    private static NetworkMetrics city(
            String name, double clustering, double density, double degree, int diameter, double path) {
        NetworkMetrics m = new NetworkMetrics("C:/graphs/" + name + ".graphml");
        m.setNodeCount(10_000);
        m.setEdgeCount(20_000);
        m.setClusteringCoefficient(clustering);
        m.setGraphDensity(density);
        m.setAvgDegreeCentrality(degree);
        m.setGraphDiameter(diameter);
        m.setAvgPathLength(path);
        m.setAssortativityDegree(0.0);
        m.setAvgBetweennessCentrality(0.1);
        m.setAvgClosenessCentrality(0.2);
        m.setGraphEntropy(3.0);
        return m;
    }
}