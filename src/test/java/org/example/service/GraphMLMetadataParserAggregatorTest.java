package org.example.service;

import org.example.model.StreetNetworkStats;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
// Exercises GraphMLMetadataParser.Aggregator without loading full GraphML files.
class GraphMLMetadataParserAggregatorTest {
    private Map<String, GraphMLMetadataParser.KeyDef> keys;
    @BeforeEach
    void setUpKeys() {
        keys = new LinkedHashMap<>();
        keys.put("d0", new GraphMLMetadataParser.KeyDef("node", "x"));
        keys.put("d1", new GraphMLMetadataParser.KeyDef("node", "y"));
        keys.put("d2", new GraphMLMetadataParser.KeyDef("node", "street_count"));
        keys.put("d3", new GraphMLMetadataParser.KeyDef("edge", "highway"));
        keys.put("d4", new GraphMLMetadataParser.KeyDef("edge", "length"));
        keys.put("d5", new GraphMLMetadataParser.KeyDef("edge", "oneway"));
        keys.put("d6", new GraphMLMetadataParser.KeyDef("edge", "name"));
        keys.put("d7", new GraphMLMetadataParser.KeyDef("edge", "maxspeed"));
        keys.put("d8", new GraphMLMetadataParser.KeyDef("edge", "access"));
        keys.put("d9", new GraphMLMetadataParser.KeyDef("graph", "crs"));
        keys.put("d10", new GraphMLMetadataParser.KeyDef("graph", "simplified"));
    }
    @Test
    void aggregatesEdgeAndNodeAttributesIntoStreetStats() {
        GraphMLMetadataParser.Aggregator agg = new GraphMLMetadataParser.Aggregator();
        Map<String, String> node = Map.of(
                "d0", "4.90",
                "d1", "52.37",
                "d2", "3");
        agg.acceptNode(node, keys);
        Map<String, String> residential = Map.of(
                "d3", "residential",
                "d4", "100",
                "d5", "false",
                "d6", "Main Street",
                "d7", "30 km/h");
        Map<String, String> primary = Map.of(
                "d3", "primary",
                "d4", "300",
                "d5", "true",
                "d6", "Ring Road",
                "d7", "50",
                "d8", "private");
        Map<String, String> footway = Map.of(
                "d3", "footway",
                "d4", "50",
                "d5", "false");
        agg.acceptEdge(residential, keys);
        agg.acceptEdge(primary, keys);
        agg.acceptEdge(footway, keys);
        StreetNetworkStats stats = new StreetNetworkStats();
        GraphMLMetadataParser.applyGraphData(stats, Map.of("d9", "EPSG:4326", "d10", "true"), keys);
        agg.finalizeInto(stats, keys);
        assertEquals("EPSG:4326", stats.getCrs());
        assertEquals(Boolean.TRUE, stats.getSimplified());
        assertEquals(450.0, stats.getTotalLengthMeters(), 1e-6);
        assertEquals(1, stats.getOnewayEdges());
        assertEquals(2, stats.getTwowayEdges());
        assertEquals(1.0 / 3.0, stats.getOnewayRatio(), 1e-6);
        assertEquals(2.0 / 3.0, stats.getNamedRoadRatio(), 1e-6);
        assertEquals("primary", stats.getDominantHighwayType());
        assertEquals(1, stats.getNodesWithCoordinates());
        assertEquals(52.37, stats.getMinLatitude(), 1e-6);
        assertEquals(4.90, stats.getMinLongitude(), 1e-6);
        assertTrue(stats.getAvgMaxSpeedKmh() > 35 && stats.getAvgMaxSpeedKmh() < 45);
        assertTrue(stats.getRestrictedAccessLengthPct() > 60);
        assertTrue(stats.getPctResidential() > 0);
        assertTrue(stats.getPctFootway() > 0);
        assertTrue(stats.getPedestrianFriendlyLengthPct() > 0);
        assertTrue(stats.getCarOrientedLengthPct() > 0);
    }
    @Test
    void parseMissingFileReturnsEmptyStats() {
        StreetNetworkStats stats = new GraphMLMetadataParser().parse(null);
        assertNotNull(stats);
        assertFalse(stats.hasData());
    }
}