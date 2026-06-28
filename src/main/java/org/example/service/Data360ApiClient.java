package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.WorldBankIndicatorValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

// HTTP client for the World Bank Data360 API
public class Data360ApiClient {
    private static final Logger logger = LoggerFactory.getLogger(Data360ApiClient.class);
    private static final String BASE_URL = "https://data360api.worldbank.org/data360/data";
    private static final String DATABASE_ID = "WB_WDI";
    private static final int YEAR_FROM = 2010;
    private static final int YEAR_TO = 2024;
    private static final Duration TIMEOUT = Duration.ofSeconds(30);
    private final HttpClient http;
    private final ObjectMapper mapper;
    private final Path cacheDir;
    private final boolean offlineMode;
    public Data360ApiClient() {
        this.http = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper();
        this.cacheDir = Path.of(System.getProperty("user.home"), ".network-analysis-tool", "data360-cache");
        this.offlineMode = Boolean.getBoolean("data360.offline");
    }
    public Optional<WorldBankIndicatorValue> fetchLatest(String refAreaCode, WorldBankIndicators indicator) {
        if (refAreaCode == null || refAreaCode.isBlank() || indicator == null) {
            return Optional.empty();
        }
        Path cacheFile = cachePath(refAreaCode, indicator.getId());
        if (Files.isRegularFile(cacheFile)) {
            try {
                return Optional.of(mapper.readValue(cacheFile.toFile(), WorldBankIndicatorValue.class));
            } catch (IOException e) {
                logger.debug("Stale cache for {} / {}: {}", refAreaCode, indicator.getId(), e.getMessage());
            }
        }
        if (offlineMode) {
            return Optional.empty();
        }
        try {
            String url = buildUrl(refAreaCode, indicator.getId());
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                logger.warn("Data360 HTTP {} for {} / {}", response.statusCode(), refAreaCode, indicator.getId());
                return Optional.empty();
            }
            Optional<WorldBankIndicatorValue> parsed = parseLatest(response.body(), indicator);
            parsed.ifPresent(v -> writeCache(cacheFile, v));
            return parsed;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            logger.warn("Data360 fetch failed for {} / {}: {}", refAreaCode, indicator.getId(), e.getMessage());
            return Optional.empty();
        }
    }
    private String buildUrl(String refArea, String indicatorId) {
        String query = "DATABASE_ID=" + enc(DATABASE_ID)
                + "&REF_AREA=" + enc(refArea)
                + "&INDICATOR=" + enc(indicatorId)
                + "&timePeriodFrom=" + YEAR_FROM
                + "&timePeriodTo=" + YEAR_TO;
        return BASE_URL + "?" + query;
    }
    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
    private Optional<WorldBankIndicatorValue> parseLatest(String json, WorldBankIndicators indicator) throws IOException {
        JsonNode root = mapper.readTree(json);
        JsonNode values = root.path("value");
        if (!values.isArray() || values.isEmpty()) {
            return Optional.empty();
        }
        JsonNode latest = null;
        int latestYear = -1;
        for (JsonNode row : values) {
            if (!row.hasNonNull("OBS_VALUE")) continue;
            int year = parseYear(row.path("TIME_PERIOD").asText(""));
            boolean isLatestFlag = row.path("LATEST_DATA").asBoolean(false);
            if (isLatestFlag) {
                latest = row;
                latestYear = year;
                break;
            }
            if (year > latestYear) {
                latestYear = year;
                latest = row;
            }
        }
        if (latest == null) return Optional.empty();
        double numeric = Double.parseDouble(latest.path("OBS_VALUE").asText());
        String unit = mapUnit(latest.path("UNIT_MEASURE").asText(""), indicator);
        String year = latest.path("TIME_PERIOD").asText("");
        return Optional.of(new WorldBankIndicatorValue(
                indicator.getId(),
                indicator.getShortLabel(),
                indicator.getDescription(),
                numeric,
                year,
                unit));
    }
    private static int parseYear(String yearStr) {
        try {
            return Integer.parseInt(yearStr);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    private static String mapUnit(String apiUnit, WorldBankIndicators indicator) {
        if (apiUnit == null || apiUnit.isBlank()) return "";
        return switch (apiUnit.toUpperCase(Locale.ROOT)) {
            case "USD" -> "US$";
            case "PT" -> "%";
            case "KM" -> "km";
            case "P1" -> "per 1,000 people";
            case "KG" -> "kg oil eq./capita";
            case "MT", "T" -> "metric tons/capita";
            case "1_TO_5" -> "score (1-5)";
            case "PS" -> "people";
            case "PC_A" -> "% per year";
            case "KG_PS" -> "kg oil eq./capita";
            case "PT_POP" -> "% of population";
            default -> apiUnit;
        };
    }
    private Path cachePath(String refArea, String indicatorId) {
        String safe = refArea + "_" + indicatorId.replace('.', '_') + "_v2";
        return cacheDir.resolve(safe + ".json");
    }
    private void writeCache(Path file, WorldBankIndicatorValue value) {
        try {
            Files.createDirectories(cacheDir);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), value);
        } catch (IOException e) {
            logger.debug("Could not write Data360 cache {}: {}", file, e.getMessage());
        }
    }
    public void clearCache() {
        try {
            if (Files.isDirectory(cacheDir)) {
                Files.list(cacheDir).sorted(Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                });
            }
        } catch (IOException e) {
            logger.warn("Could not clear Data360 cache: {}", e.getMessage());
        }
    }
}