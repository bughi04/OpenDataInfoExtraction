package org.example.service;

import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
// Tests that World Bank indicator payloads survive session persistence and that legacy session files with
// computed view fields still deserialize.
class SessionWorldBankRoundTripTest {
    @Test
    void roundTripMetricsWithWorldBankData(@TempDir Path tempDir) {
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        NetworkMetrics m = new NetworkMetrics("C:/data/Amsterdam,_Netherlands.graphml");
        m.setNodeCount(12000);
        m.setAnalysisMode(AnalysisMode.QUICK);
        m.setMetricsApproximated(true);
        WorldBankCityData wb = new WorldBankCityData();
        wb.setCity("Amsterdam");
        wb.setCountry("Netherlands");
        wb.setRefAreaCode("NLD");
        wb.setStatus(WorldBankCityData.FetchStatus.OK);
        wb.setFetchedAt(LocalDateTime.of(2025, 6, 1, 12, 0));
        wb.addIndicator(new WorldBankIndicatorValue(
                "NY.GDP.PCAP.CD", "GDP per capita", "GDP", 55000.0, "2023", "USD"));
        m.setWorldBankData(wb);
        SessionPersistenceService.SessionData data = new SessionPersistenceService.SessionData();
        data.metrics.add(m);
        data.currentFolder = "C:/data/Graphs";
        data.analysisMode = "QUICK";
        assertTrue(store.save(data), "save must succeed with World Bank payload");
        SessionPersistenceService.SessionData loaded = store.load();
        assertNotNull(loaded);
        assertEquals(1, loaded.metrics.size());
        assertEquals(12000, loaded.metrics.get(0).getNodeCount());
        assertNotNull(loaded.metrics.get(0).getWorldBankData());
        assertEquals(WorldBankCityData.FetchStatus.OK, loaded.metrics.get(0).getWorldBankData().getStatus());
        assertEquals(1, loaded.metrics.get(0).getWorldBankData().getIndicators().size());
        assertEquals(55000.0, loaded.metrics.get(0).getWorldBankData().getNumericValue("NY.GDP.PCAP.CD"), 1);
    }
    @Test
    void loadsLegacyJsonWithIndicatorsViewAndComputedFields(@TempDir Path tempDir) throws Exception {
        String legacy = """
                {
                  "metrics" : [ {
                    "graphFile" : "C:/Barcelona,_Spain.graphml",
                    "nodeCount" : 100,
                    "worldBankData" : {
                      "city" : "Barcelona",
                      "country" : "Spain",
                      "status" : "OK",
                      "usable" : true,
                      "indicators" : [ {
                        "indicatorId" : "NY.GDP.PCAP.CD",
                        "value" : 42000.0,
                        "hasValue" : true,
                        "formattedValue" : "42,000"
                      } ],
                      "indicatorsView" : [ {
                        "indicatorId" : "NY.GDP.PCAP.CD",
                        "value" : 42000.0
                      } ]
                    }
                  } ]
                }
                """;
        Path file = tempDir.resolve("session.json");
        Files.writeString(file, legacy);
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        SessionPersistenceService.SessionData loaded = store.load();
        assertNotNull(loaded);
        assertEquals(1, loaded.metrics.size());
        assertEquals(100, loaded.metrics.get(0).getNodeCount());
        assertNotNull(loaded.metrics.get(0).getWorldBankData());
        assertEquals(1, loaded.metrics.get(0).getWorldBankData().getIndicators().size());
    }
}