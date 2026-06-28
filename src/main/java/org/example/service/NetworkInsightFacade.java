package org.example.service;

import org.example.model.NetworkMetrics;
import org.example.util.MetricsStatisticsService;
import java.io.IOException;
import java.util.List;

// Routes insight requests to Google Gemini
public class NetworkInsightFacade {
    private final GeminiInsightService gemini = new GeminiInsightService();
    public boolean isGeminiAvailable() { return gemini.isConfigured(); }
    public GeminiInsightService getGeminiService() { return gemini; }
    public String generateOverview(List<NetworkMetrics> metrics, CityScoresLoader loader) {
        requireGemini();
        try {
            String context = MetricsStatisticsService.buildMetricsContext(metrics, loader);
            return gemini.ask(GeminiInsightService.buildSystemPrompt(),
                    "Provide a detailed comparative overview of these urban mobility networks:\n\n" + context);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String msg = e instanceof IOException io ? io.getMessage() : "Gemini request interrupted";
            throw new InsightException(msg, e);
        }
    }
    public String generateBestVsWorst(List<NetworkMetrics> metrics, CityScoresLoader loader) {
        requireGemini();
        try {
            NetworkMetrics best = MetricsStatisticsService.bestCity(metrics);
            NetworkMetrics worst = MetricsStatisticsService.worstCity(metrics);
            String context = MetricsStatisticsService.buildMetricsContext(metrics, loader);
            String pair = String.join("\n",
                    MetricsStatisticsService.compareCities(best, worst).lines());
            return gemini.ask(GeminiInsightService.buildSystemPrompt(),
                    "Compare the best vs worst cities in this dataset. Explain what structural differences "
                            + "drive mobility readiness and what the weaker city could improve.\n\n"
                            + context + "\n" + pair);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String msg = e instanceof IOException io ? io.getMessage() : "Gemini request interrupted";
            throw new InsightException(msg, e);
        }
    }
    public String ask(String question, List<NetworkMetrics> metrics, CityScoresLoader loader) {
        requireGemini();
        try {
            String context = MetricsStatisticsService.buildFocusedContext(question, metrics, loader);
            return gemini.ask(GeminiInsightService.buildSystemPrompt(),
                    "User question: " + question + "\n\nNetwork data:\n" + context);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String msg = e instanceof IOException io
                    ? io.getMessage()
                    : "Gemini request interrupted";
            throw new InsightException(msg, e);
        }
    }
    private void requireGemini() {
        if (!gemini.isConfigured()) {
            throw new InsightException(
                    "Gemini API key not configured. Open AI Advisor > Gemini Settings and enter your API key.",
                    null);
        }
    }
    public static class InsightException extends RuntimeException {
        public InsightException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}