package org.example.util;

import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.example.service.WorldBankIndicators;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests plain-language World Bank narrative generation, including offline state, indicator listing, and peer-country comparison text.
class WorldBankAnalysisUtilTest {
    @Test
    void generateCityNarrativeWithoutDataExplainsOfflineState() {
        NetworkMetrics m = new NetworkMetrics("C:/graphs/Oslo,_Norway.graphml");
        String text = WorldBankAnalysisUtil.generateCityNarrative(m, List.of(m));
        assertTrue(text.contains("No World Bank data"));
    }
    @Test
    void generateCityNarrativeIncludesIndicatorsAndPeerComparison() {
        NetworkMetrics oslo = metricsWithWorldBank("Oslo,_Norway", "NOR", 65000, 82, 8.5);
        NetworkMetrics paris = metricsWithWorldBank("Paris,_France", "FRA", 45000, 81, 5.2);
        StreetNetworkStats street = new StreetNetworkStats();
        street.setCarOrientedLengthPct(45);
        street.setCyclingFriendlyLengthPct(3);
        oslo.setStreetStats(street);
        oslo.setClusteringCoefficient(0.08);

        String text = WorldBankAnalysisUtil.generateCityNarrative(oslo, List.of(oslo, paris));
        assertTrue(text.contains("Oslo, Norway"));
        assertTrue(text.contains("World Bank code NOR"));
        assertTrue(text.contains("GDP per capita"));
        assertTrue(text.contains("Comparison with loaded cities"));
        assertTrue(text.contains("GDP per capita across loaded cities"));
        assertTrue(text.contains("Street network vs country context"));
    }
    @Test
    void generateMultiCitySummaryCountsCountries() {
        NetworkMetrics a = metricsWithWorldBank("A,_X", "NLD", 50000, 90, 10);
        NetworkMetrics b = metricsWithWorldBank("B,_Y", "ESP", 40000, 80, 6);
        NetworkMetrics c = new NetworkMetrics("C:/graphs/C,_Z.graphml");

        String summary = WorldBankAnalysisUtil.generateMultiCitySummary(List.of(a, b, c));
        assertTrue(summary.contains("Cities with data: 2 / 3"));
        assertTrue(summary.contains("Countries covered: 2"));
        assertTrue(summary.contains("NLD"));
        assertTrue(summary.contains("ESP"));
    }
    @Test
    void getValueReturnsNumericIndicator() {
        NetworkMetrics m = metricsWithWorldBank("X,_Y", "DEU", 48000, 77, 9.0);
        Double gdp = WorldBankAnalysisUtil.getValue(m, WorldBankIndicators.GDP_PER_CAPITA);
        assertEquals(48000.0, gdp, 1e-6);
        assertNull(WorldBankAnalysisUtil.getValue(m, null));
        assertNull(WorldBankAnalysisUtil.getValue(new NetworkMetrics("z.graphml"), WorldBankIndicators.GDP_PER_CAPITA));
    }
    private static NetworkMetrics metricsWithWorldBank(
            String name, String code, double gdp, double urbanPct, double co2) {
        NetworkMetrics m = new NetworkMetrics("C:/graphs/" + name + ".graphml");
        WorldBankCityData wb = new WorldBankCityData();
        wb.setCity(name.split(",")[0].replace('_', ' ').trim());
        String countryPart = name.contains(",")
            ? name.substring(name.indexOf(',') + 1).replace('_', ' ').trim()
            : null;
        wb.setCountry(countryPart);
        wb.setRefAreaCode(code);
        wb.setStatus(WorldBankCityData.FetchStatus.OK);
        wb.addIndicator(new WorldBankIndicatorValue(
                WorldBankIndicators.GDP_PER_CAPITA.getId(), "GDP", "USD", gdp, "2023", "USD"));
        wb.addIndicator(new WorldBankIndicatorValue(
                WorldBankIndicators.URBAN_POP_PCT.getId(), "Urban", "%", urbanPct, "2023", "%"));
        wb.addIndicator(new WorldBankIndicatorValue(
                WorldBankIndicators.CO2_PER_CAPITA.getId(), "CO2", "t", co2, "2023", "t"));
        m.setWorldBankData(wb);
        return m;
    }
}