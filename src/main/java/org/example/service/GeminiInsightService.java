package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Google Gemini API client for AI-powered network insights
public class GeminiInsightService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiInsightService.class);
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Path CONFIG_PATH =
            Path.of(System.getProperty("user.home"), ".network-analysis-tool", "gemini.properties");
    private static final String DEFAULT_MODEL = "gemini-3.1-flash-lite";
    private static final List<String> RECOMMENDED_MODELS = List.of(
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash"
    );
    private static final List<String> FALLBACK_MODELS = List.of(
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash",
            "gemini-2.5-flash-lite",
            "gemini-2.5-flash"
    );
    private static final int MAX_RETRIES_PER_MODEL = 2;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();
    private String apiKey;
    private String model;
    public GeminiInsightService() {
        loadConfig();
    }
    public static List<String> getRecommendedModels() {
        return RECOMMENDED_MODELS;
    }
    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }
    public String getModel() { return model; }
    public void updateConfig(String key, String modelName) throws IOException {
        if (key != null && !key.isBlank()) {
            this.apiKey = key;
        }
        this.model = migrateDeprecatedModel(normalizeModel(modelName));
        saveConfig();
    }
    public String ask(String systemPrompt, String userMessage) throws IOException, InterruptedException {
        if (!isConfigured()) {
            throw new IllegalStateException("Gemini API key not configured");
        }
        List<String> modelsToTry = buildModelAttemptOrder(model);
        IOException lastError = null;
        for (String tryModel : modelsToTry) {
            try {
                String response = askWithModel(systemPrompt, userMessage, tryModel);
                if (!tryModel.equals(model)) {
                    logger.info("Gemini succeeded with fallback model {} (was {})", tryModel, model);
                    this.model = tryModel;
                    try {
                        saveConfig();
                    } catch (IOException e) {
                        logger.warn("Could not persist fallback model choice: {}", e.getMessage());
                    }
                }
                return response;
            } catch (IOException e) {
                lastError = e;
                if (shouldTryNextModel(e, tryModel, modelsToTry)) {
                    logger.warn("Gemini model {} unavailable, trying next model: {}",
                            tryModel, e.getMessage());
                    continue;
                }
                throw e;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Gemini request failed: no models available");
    }
    private String askWithModel(String systemPrompt, String userMessage, String modelId)
            throws IOException, InterruptedException {
        ObjectNode body = JSON.createObjectNode();
        ObjectNode systemInstruction = body.putObject("systemInstruction");
        ArrayNode systemParts = systemInstruction.putArray("parts");
        systemParts.addObject().put("text", systemPrompt);
        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        userContent.put("role", "user");
        ArrayNode userParts = userContent.putArray("parts");
        userParts.addObject().put("text", userMessage);
        ObjectNode generationConfig = body.putObject("generationConfig");
        generationConfig.put("temperature", 0.7);
        generationConfig.put("maxOutputTokens", 2048);
        String url = String.format(
                "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s",
                modelId, apiKey);
        String payload = body.toString();
        IOException lastError = null;
        for (int attempt = 0; attempt <= MAX_RETRIES_PER_MODEL; attempt++) {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(90))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if ((status == 429 || status == 503) && attempt < MAX_RETRIES_PER_MODEL) {
                long delayMs = parseRetryDelayMs(response.body());
                logger.warn("Gemini {} on {} (attempt {}/{}), retrying in {} ms",
                        status, modelId, attempt + 1, MAX_RETRIES_PER_MODEL + 1, delayMs);
                Thread.sleep(delayMs);
                continue;
            }
            if (status >= 400) {
                throw new IOException(formatApiError(status, response.body(), modelId));
            }
            try {
                return parseSuccessResponse(response.body());
            } catch (IOException e) {
                lastError = e;
                break;
            }
        }
        if (lastError != null) {
            throw lastError;
        }
        throw new IOException("Gemini request failed for model " + modelId);
    }
    private static List<String> buildModelAttemptOrder(String preferred) {
        Set<String> order = new LinkedHashSet<>();
        if (preferred != null && !preferred.isBlank()) {
            order.add(migrateDeprecatedModel(preferred.trim()));
        }
        order.addAll(FALLBACK_MODELS);
        return new ArrayList<>(order);
    }
    private static boolean shouldTryNextModel(IOException error, String currentModel, List<String> all) {
        String msg = error.getMessage() != null ? error.getMessage().toLowerCase() : "";
        boolean quotaOrLimit = msg.contains("quota") || msg.contains("rate limit")
                || msg.contains("resource_exhausted") || msg.contains("exceeded");
        boolean modelUnavailable = msg.contains("not available") || msg.contains("not found for your api key");
        boolean hasMore = all.indexOf(currentModel) < all.size() - 1;
        return hasMore && (quotaOrLimit || modelUnavailable);
    }
    public static String formatApiError(int status, String body, String modelName) {
        try {
            JsonNode root = JSON.readTree(body);
            JsonNode error = root.path("error");
            String message = error.path("message").asText("").trim();
            String shortMsg = message.contains("\n")
                    ? message.substring(0, message.indexOf('\n')).trim()
                    : message;
            if (status == 429 || error.path("status").asText("").equals("RESOURCE_EXHAUSTED")) {
                String retryHint = formatRetryHint(error);
                return "Gemini quota or rate limit exceeded for model \"" + modelName + "\".\n\n"
                        + "What to try:\n"
                        + "  1. Open AI Advisor - Gemini Settings and select gemini-3.1-flash-lite "
                        + "or gemini-3.5-flash\n"
                        + "  2. " + retryHint + "\n"
                        + "  3. The app will also try alternate models automatically when possible\n"
                        + "  4. Check usage at https://aistudio.google.com/apikey\n"
                        + "  5. Enable billing on your Google AI project for higher limits\n\n"
                        + (shortMsg.isEmpty() ? "" : "API: " + shortMsg);
            }
            if (status == 400 && message.toLowerCase().contains("model")) {
                return "Gemini model \"" + modelName + "\" is not available for your API key.\n\n"
                        + "Open Gemini Settings and pick gemini-3.1-flash-lite or gemini-3.5-flash.\n\n"
                        + (shortMsg.isEmpty() ? "" : "API: " + shortMsg);
            }
            if (status == 403) {
                return "Gemini API key rejected (403). Check that your key is valid and "
                        + "Generative Language API is enabled.\n\n"
                        + (shortMsg.isEmpty() ? "" : "API: " + shortMsg);
            }
            return "Gemini API error " + status
                    + (shortMsg.isEmpty() ? "" : ": " + shortMsg);
        } catch (Exception e) {
            return "Gemini API error " + status + ": " + truncate(body, 300);
        }
    }
    public static String buildSystemPrompt() {
        return "You are an expert urban mobility network analyst. "
                + "You interpret graph-theoretic metrics of city street networks "
                + "(clustering, density, centrality, diameter, path length, entropy, assortativity) "
                + "and OSM street infrastructure data (highway types, road length, speed limits, "
                + "oneway segments, lanes, streets-per-node) "
                + "in the context of the Urban Mobility Readiness Index (UMRi). "
                + "Reference Sierra-Porta and Herrera-Acevedo (2024) when relevant. "
                + "Be specific, compare cities when data allows, and highlight actionable insights. "
                + "Use plain text with bullet points. Keep responses under 500 words unless asked for detail.";
    }
    private String parseSuccessResponse(String body) throws IOException {
        JsonNode root = JSON.readTree(body);
        JsonNode candidates = root.path("candidates");
        if (candidates.isEmpty()) {
            String blockReason = root.path("promptFeedback").path("blockReason").asText("");
            throw new IOException(blockReason.isBlank()
                    ? "Empty response from Gemini"
                    : "Gemini blocked the request: " + blockReason);
        }
        JsonNode parts = candidates.path(0).path("content").path("parts");
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            if (part.has("text")) {
                text.append(part.path("text").asText());
            }
        }
        if (text.length() == 0) {
            throw new IOException("Empty response from Gemini");
        }
        return text.toString().trim();
    }
    private static long parseRetryDelayMs(String body) {
        try {
            JsonNode root = JSON.readTree(body);
            for (JsonNode detail : root.path("error").path("details")) {
                if (detail.path("@type").asText("").contains("RetryInfo")) {
                    String delay = detail.path("retryDelay").asText("");
                    Matcher m = Pattern.compile("(\\d+)").matcher(delay);
                    if (m.find()) {
                        return Long.parseLong(m.group(1)) * 1000L + 500L;
                    }
                }
            }
            String message = root.path("error").path("message").asText("");
            Matcher m = Pattern.compile("retry in ([0-9.]+)s", Pattern.CASE_INSENSITIVE).matcher(message);
            if (m.find()) {
                return (long) (Double.parseDouble(m.group(1)) * 1000) + 500L;
            }
        } catch (Exception ignored) { }
        return 45_000L;
    }
    private static String formatRetryHint(JsonNode error) {
        for (JsonNode detail : error.path("details")) {
            if (detail.path("@type").asText("").contains("RetryInfo")) {
                String delay = detail.path("retryDelay").asText("");
                if (!delay.isBlank()) {
                    return "Wait about " + delay.replace("s", " seconds") + ", then try again";
                }
            }
        }
        return "Wait a minute, then try again";
    }
    private void loadConfig() {
        Properties props = new Properties();
        if (Files.isRegularFile(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
            } catch (IOException e) {
                logger.warn("Could not read Gemini config: {}", e.getMessage());
            }
        }
        apiKey = firstNonBlank(
                System.getenv("GEMINI_API_KEY"),
                System.getenv("GOOGLE_API_KEY"),
                props.getProperty("api.key"));
        String savedModel = firstNonBlank(
                System.getenv("GEMINI_MODEL"),
                props.getProperty("model"),
                DEFAULT_MODEL);
        String migrated = migrateDeprecatedModel(normalizeModel(savedModel));
        if (!migrated.equals(savedModel)) {
            logger.info("Upgrading Gemini model {} - {}", savedModel, migrated);
        }
        model = migrated;
        if (props.getProperty("model") != null
                && !migrated.equals(props.getProperty("model").trim())
                && System.getenv("GEMINI_MODEL") == null) {
            try {
                saveConfig();
            } catch (IOException e) {
                logger.warn("Could not save upgraded Gemini model: {}", e.getMessage());
            }
        }
    }
    private static String migrateDeprecatedModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return DEFAULT_MODEL;
        }
        String m = modelName.trim();
        if (m.startsWith("gemini-2.0")) {
            return DEFAULT_MODEL;
        }
        if ("gemini-3.1-flash-lite-preview".equals(m)) {
            return "gemini-3.1-flash-lite";
        }
        return m;
    }
    private static String normalizeModel(String modelName) {
        if (modelName == null || modelName.isBlank()) {
            return DEFAULT_MODEL;
        }
        return modelName.trim();
    }
    private void saveConfig() throws IOException {
        Files.createDirectories(CONFIG_PATH.getParent());
        Properties props = new Properties();
        props.setProperty("api.key", apiKey != null ? apiKey : "");
        props.setProperty("model", model);
        try (var out = Files.newOutputStream(CONFIG_PATH)) {
            props.store(out, "Extracting information from open data: Gemini settings");
        }
    }
    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
