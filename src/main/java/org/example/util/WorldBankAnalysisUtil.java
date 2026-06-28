package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.WorldBankCityData;
import org.example.service.WorldBankIndicators;

import java.util.*;
import java.util.stream.Collectors;

// Generates plain-language analysis text from World Bank indicator data
public final class WorldBankAnalysisUtil {
    private WorldBankAnalysisUtil() {}
    public static String generateCityNarrative(NetworkMetrics metrics, List<NetworkMetrics> allLoaded) {
        if (metrics == null || metrics.getWorldBankData() == null) {
            return "No World Bank data available for this city yet. Process the GraphML file while online.";
        }
        WorldBankCityData wb = metrics.getWorldBankData();
        StringBuilder sb = new StringBuilder();
        sb.append("Country context: ").append(wb.displayLabel());
        if (wb.getRefAreaCode() != null) {
            sb.append(" (World Bank code ").append(wb.getRefAreaCode()).append(")");
        }
        sb.append("\n\n");
        sb.append("Note: World Bank indicators are reported at country level, not city level. ");
        sb.append("Cities in the same country share the same values.\n\n");

        if (!wb.isUsable()) {
            sb.append(wb.getStatusMessage() != null ? wb.getStatusMessage() : "Data fetch failed.");
            return sb.toString();
        }
        appendIndicatorLine(sb, wb, WorldBankIndicators.GDP_PER_CAPITA, "GDP per capita");
        appendIndicatorLine(sb, wb, WorldBankIndicators.URBAN_POP_PCT, "Urbanisation");
        appendIndicatorLine(sb, wb, WorldBankIndicators.CO2_PER_CAPITA, "Carbon intensity");
        appendIndicatorLine(sb, wb, WorldBankIndicators.LOGISTICS_INDEX, "Logistics performance");
        appendIndicatorLine(sb, wb, WorldBankIndicators.INTERNET_USERS, "Digital connectivity");
        appendIndicatorLine(sb, wb, WorldBankIndicators.TRANSPORT_INFRA, "Transport infrastructure");
        sb.append("\nComparison with loaded cities\n");
        sb.append("-".repeat(40)).append("\n");
        List<NetworkMetrics> peers = allLoaded != null ? allLoaded : List.of(metrics);
        appendPeerComparison(sb, peers, WorldBankIndicators.GDP_PER_CAPITA, "GDP per capita");
        appendPeerComparison(sb, peers, WorldBankIndicators.URBAN_POP_PCT, "Urban population %");
        appendPeerComparison(sb, peers, WorldBankIndicators.CO2_PER_CAPITA, "CO2 per capita");
        appendNetworkContext(sb, metrics, peers);
        return sb.toString();
    }
    public static String generateMultiCitySummary(List<NetworkMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return "Process GraphML files to load World Bank country indicators.";
        }
        long withData = metrics.stream()
                .filter(m -> m.getWorldBankData() != null && m.getWorldBankData().isUsable())
                .count();
        Set<String> countries = metrics.stream()
                .map(m -> m.getWorldBankData())
                .filter(Objects::nonNull)
                .map(WorldBankCityData::getRefAreaCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        StringBuilder sb = new StringBuilder();
        sb.append("World Bank Data360 enrichment\n");
        sb.append("Cities with data: ").append(withData).append(" / ").append(metrics.size()).append("\n");
        sb.append("Countries covered: ").append(countries.size()).append("\n");
        if (!countries.isEmpty()) {
            sb.append("Reference areas: ").append(String.join(", ", countries)).append("\n");
        }
        sb.append("\nData source: World Bank World Development Indicators via Data360 API.\n");
        sb.append("Indicators are country-level; compare with street-network metrics for context.\n");
        return sb.toString();
    }
    private static void appendIndicatorLine(StringBuilder sb, WorldBankCityData wb,
                                            WorldBankIndicators key, String heading) {
        wb.findIndicator(key.getId()).ifPresent(ind -> {
            if (ind.hasValue()) {
                sb.append(String.format(Locale.ROOT, "%s: %s %s (%s)%n",
                        heading, ind.formattedValue(),
                        ind.getUnit() != null ? ind.getUnit() : "",
                        ind.getYear() != null ? ind.getYear() : "year n/a"));
            }
        });
    }
    private static void appendPeerComparison(StringBuilder sb, List<NetworkMetrics> peers,
                                             WorldBankIndicators key, String label) {
        List<Double> values = peers.stream()
                .map(m -> m.getWorldBankData())
                .filter(Objects::nonNull)
                .map(w -> w.getNumericValue(key.getId()))
                .filter(Objects::nonNull)
                .toList();
        if (values.isEmpty()) return;
        double avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double min = values.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double max = values.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        sb.append(String.format(Locale.ROOT, "%s across loaded cities: avg %.1f, range %.1f – %.1f%n",
                label, avg, min, max));
    }
    private static void appendNetworkContext(StringBuilder sb, NetworkMetrics metrics,
                                             List<NetworkMetrics> peers) {
        WorldBankCityData wb = metrics.getWorldBankData();
        if (wb == null || metrics.getStreetStats() == null) return;
        Double gdp = wb.getNumericValue(WorldBankIndicators.GDP_PER_CAPITA.getId());
        Double urban = wb.getNumericValue(WorldBankIndicators.URBAN_POP_PCT.getId());
        double carPct = metrics.getStreetStats().getCarOrientedLengthPct();
        double cyclingPct = metrics.getStreetStats().getCyclingFriendlyLengthPct();
        sb.append("\nStreet network vs country context\n");
        sb.append("-".repeat(40)).append("\n");
        if (gdp != null && gdp > 50_000 && carPct > 40) {
            sb.append("- High-income country with car-oriented street mix (")
                    .append(String.format(Locale.ROOT, "%.0f%%", carPct))
                    .append(" car-oriented length).\n");
        } else if (gdp != null && gdp < 20_000 && cyclingPct > 5) {
            sb.append("- Lower GDP context but notable cycling infrastructure in OSM (")
                    .append(String.format(Locale.ROOT, "%.0f%%", cyclingPct))
                    .append(" cycling-friendly length).\n");
        }
        if (urban != null && urban > 85) {
            sb.append("- Highly urbanised country (").append(String.format(Locale.ROOT, "%.0f%%", urban))
                    .append(" urban population), dense networks are typical.\n");
        }
        if (metrics.getClusteringCoefficient() != null && gdp != null) {
            double avgClustering = peers.stream()
                    .map(NetworkMetrics::getClusteringCoefficient)
                    .filter(Objects::nonNull)
                    .mapToDouble(Double::doubleValue)
                    .average().orElse(0);
            if (metrics.getClusteringCoefficient() > avgClustering && gdp > 40_000) {
                sb.append("- Clustering above your loaded-city average, consistent with compact wealthy metros.\n");
            }
        }
    }
    public static Double getValue(NetworkMetrics m, WorldBankIndicators indicator) {
        if (m == null || m.getWorldBankData() == null || indicator == null) return null;
        return m.getWorldBankData().getNumericValue(indicator.getId());
    }
}
