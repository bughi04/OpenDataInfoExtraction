package org.example.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;
// Tests session JSON save/load round-trips, legacy JSON compatibility, empty-save merge rules, and backup recovery.
class SessionPersistenceServiceTest {
    private ObjectMapper mapper;
    @BeforeEach
    void setUp() {
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
    @Test
    void roundTripDoesNotWriteComputedFields(@TempDir Path tempDir) throws Exception {
        NetworkMetrics m = new NetworkMetrics("C:/data/Barcelona,_Spain.graphml");
        m.setNodeCount(8912);
        m.setAnalysisMode(AnalysisMode.FULL);
        StreetNetworkStats street = new StreetNetworkStats();
        street.setTotalLengthMeters(1250000);
        m.setStreetStats(street);
        SessionPersistenceService.SessionData data = new SessionPersistenceService.SessionData();
        data.metrics.add(m);
        Path file = tempDir.resolve("session.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(file.toFile(), data);
        String json = Files.readString(file);
        assertFalse(json.contains("totalLengthKm"), "computed km field must not be serialized");
        assertFalse(json.contains("analysisModeLabel"), "computed label must not be serialized");
        SessionPersistenceService.SessionData loaded =
                mapper.readValue(file.toFile(), SessionPersistenceService.SessionData.class);
        assertEquals(8912, loaded.metrics.get(0).getNodeCount());
        assertEquals(1250000, loaded.metrics.get(0).getStreetStats().getTotalLengthMeters(), 1);
    }
    @Test
    void loadsLegacyJsonWithComputedFields(@TempDir Path tempDir) throws Exception {
        String legacy = """
                {
                  "metrics" : [ {
                    "graphFile" : "C:/Barcelona,_Spain.graphml",
                    "nodeCount" : 100,
                    "edgeCount" : 200,
                    "directed" : true,
                    "streetStats" : {
                      "totalLengthMeters" : 5000,
                      "totalLengthKm" : 5.0,
                      "hasData" : true
                    }
                  } ]
                }
                """;
        Path file = tempDir.resolve("session.json");
        Files.writeString(file, legacy);
        SessionPersistenceService.SessionData loaded =
                mapper.readValue(file.toFile(), SessionPersistenceService.SessionData.class);
        assertEquals(1, loaded.metrics.size());
        assertEquals(100, loaded.metrics.get(0).getNodeCount());
        assertEquals(5000, loaded.metrics.get(0).getStreetStats().getTotalLengthMeters(), 1);
    }
    @Test
    void emptySaveMergesUiStateWithoutDroppingMetrics(@TempDir Path tempDir) {
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        SessionPersistenceService.SessionData withMetrics = new SessionPersistenceService.SessionData();
        NetworkMetrics m = new NetworkMetrics("C:/data/Barcelona,_Spain.graphml");
        m.setNodeCount(42);
        withMetrics.metrics.add(m);
        assertTrue(store.save(withMetrics));
        SessionPersistenceService.SessionData empty = new SessionPersistenceService.SessionData();
        empty.currentFolder = "C:/graphs";
        assertTrue(store.save(empty));
        SessionPersistenceService.SessionData loaded = store.load();
        assertNotNull(loaded);
        assertEquals(1, loaded.metrics.size());
        assertEquals(42, loaded.metrics.get(0).getNodeCount());
        assertEquals("C:/graphs", loaded.currentFolder);
    }
    @Test
    void emptySaveSkippedWhenNoExistingSession(@TempDir Path tempDir) {
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        SessionPersistenceService.SessionData empty = new SessionPersistenceService.SessionData();
        empty.currentFolder = "C:/graphs";
        assertFalse(store.save(empty));
        assertFalse(Files.exists(tempDir.resolve("session.json")));
    }
    @Test
    void loadUsesBackupWhenMainSessionHasNoMetrics(@TempDir Path tempDir) throws Exception {
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        SessionPersistenceService.SessionData good = new SessionPersistenceService.SessionData();
        NetworkMetrics m = new NetworkMetrics("C:/data/Amsterdam,_Netherlands.graphml");
        m.setNodeCount(99);
        good.metrics.add(m);
        assertTrue(store.save(good));
        Files.copy(
                tempDir.resolve("session.json"),
                tempDir.resolve("session.json.bak"),
                StandardCopyOption.REPLACE_EXISTING);
        Files.writeString(
                tempDir.resolve("session.json"),
                """
                {"metrics":[],"currentFolder":"C:/graphs","analysisMode":"QUICK","savedAt":1}
                """);
        SessionPersistenceService.SessionData loaded = store.load();
        assertNotNull(loaded);
        assertEquals(1, loaded.metrics.size());
        assertEquals(99, loaded.metrics.get(0).getNodeCount());
    }
    @Test
    void roundTripThroughService(@TempDir Path tempDir) {
        SessionPersistenceService store = new SessionPersistenceService(tempDir);
        NetworkMetrics m = new NetworkMetrics("C:/data/Barcelona,_Spain.graphml");
        m.setNodeCount(8912);
        m.setAnalysisMode(AnalysisMode.FULL);
        StreetNetworkStats street = new StreetNetworkStats();
        street.setTotalLengthMeters(1250000);
        m.setStreetStats(street);
        SessionPersistenceService.SessionData data = new SessionPersistenceService.SessionData();
        data.metrics.add(m);
        data.currentFolder = "C:/graphs";
        data.analysisMode = "FULL";
        assertTrue(store.save(data));
        SessionPersistenceService.SessionData loaded = store.load();
        assertNotNull(loaded);
        assertEquals(1, loaded.metrics.size());
        assertEquals(8912, loaded.metrics.get(0).getNodeCount());
        assertEquals("C:/graphs", loaded.currentFolder);
    }
}