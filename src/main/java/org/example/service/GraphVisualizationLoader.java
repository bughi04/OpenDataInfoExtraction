package org.example.service;

import org.example.model.StreetNetworkMapData;
import org.example.model.StreetSegment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

// Loads node coordinates and edge OSM tags from GraphML for map visualization
public class GraphVisualizationLoader {
    private static final Logger logger = LoggerFactory.getLogger(GraphVisualizationLoader.class);
    public StreetNetworkMapData load(File file) throws Exception {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + file);
        }
        Map<String, GraphMLMetadataParser.KeyDef> keys = new LinkedHashMap<>();
        Map<String, double[]> nodeCoords = new HashMap<>();
        List<StreetSegment> segments = new ArrayList<>();
        Map<String, Integer> highwayCounts = new LinkedHashMap<>();
        String pendingNodeId = null;
        String pendingSource = null;
        String pendingTarget = null;
        Map<String, String> currentData = null;
        double minLat = Double.POSITIVE_INFINITY;
        double maxLat = Double.NEGATIVE_INFINITY;
        double minLon = Double.POSITIVE_INFINITY;
        double maxLon = Double.NEGATIVE_INFINITY;
        int skippedEdges = 0;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        try (InputStreamReader in = new InputStreamReader(
                Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        String name = reader.getLocalName();
                        switch (name) {
                            case "key" -> {
                                String id = attr(reader, "id");
                                String forType = attr(reader, "for");
                                String attrName = attr(reader, "attr.name");
                                if (id != null && attrName != null) {
                                    keys.put(id, new GraphMLMetadataParser.KeyDef(forType, attrName));
                                }
                            }
                            case "node" -> {
                                pendingNodeId = attr(reader, "id");
                                currentData = new LinkedHashMap<>();
                            }
                            case "edge" -> {
                                pendingSource = attr(reader, "source");
                                pendingTarget = attr(reader, "target");
                                currentData = new LinkedHashMap<>();
                            }
                            case "data" -> {
                                String keyId = attr(reader, "key");
                                GraphMLMetadataParser.KeyDef keyDef = keys.get(keyId);
                                String attrName = keyDef != null ? keyDef.attrName() : null;
                                if (keyId == null || currentData == null) break;
                                if ("geometry".equals(attrName)) {
                                    GraphMLMetadataParser.skipDataContent(reader);
                                } else {
                                    currentData.put(keyId, GraphMLMetadataParser.readElementText(reader));
                                }
                            }
                            default -> { }
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        String name = reader.getLocalName();
                        switch (name) {
                            case "node" -> {
                                if (pendingNodeId != null && currentData != null) {
                                    Double lat = null;
                                    Double lon = null;
                                    for (Map.Entry<String, String> e : currentData.entrySet()) {
                                        GraphMLMetadataParser.KeyDef k = keys.get(e.getKey());
                                        if (k == null || !"node".equals(k.forType())) continue;
                                        switch (k.attrName()) {
                                            case "y" -> lat = parseCoord(e.getValue());
                                            case "x" -> lon = parseCoord(e.getValue());
                                            default -> { }
                                        }
                                    }
                                    if (lat != null && lon != null) {
                                        nodeCoords.put(pendingNodeId, new double[]{lat, lon});
                                        minLat = Math.min(minLat, lat);
                                        maxLat = Math.max(maxLat, lat);
                                        minLon = Math.min(minLon, lon);
                                        maxLon = Math.max(maxLon, lon);
                                    }
                                }
                                pendingNodeId = null;
                                currentData = null;
                            }
                            case "edge" -> {
                                if (pendingSource != null && pendingTarget != null && currentData != null) {
                                    double[] a = nodeCoords.get(pendingSource);
                                    double[] b = nodeCoords.get(pendingTarget);
                                    if (a == null || b == null) {
                                        skippedEdges++;
                                    } else {
                                        String highway = "unclassified";
                                        String streetName = null;
                                        boolean oneway = false;
                                        int maxSpeed = 0;
                                        boolean bridge = false;
                                        boolean tunnel = false;
                                        for (Map.Entry<String, String> e : currentData.entrySet()) {
                                            GraphMLMetadataParser.KeyDef k = keys.get(e.getKey());
                                            if (k == null || !"edge".equals(k.forType())) continue;
                                            String val = e.getValue();
                                            if (val == null || val.isBlank()) continue;
                                            switch (k.attrName()) {
                                                case "highway" -> highway = firstToken(val);
                                                case "name" -> streetName = val.trim();
                                                case "oneway" -> oneway = isTruthy(val);
                                                case "maxspeed" -> maxSpeed = parseSpeed(val);
                                                case "bridge" -> bridge = isTruthy(val);
                                                case "tunnel" -> tunnel = isTruthy(val);
                                                default -> { }
                                            }
                                        }
                                        highwayCounts.merge(highway, 1, Integer::sum);
                                        segments.add(new StreetSegment(
                                                a[0], a[1], b[0], b[1],
                                                highway, streetName, oneway, maxSpeed, bridge, tunnel));
                                    }
                                }
                                pendingSource = null;
                                pendingTarget = null;
                                currentData = null;
                            }
                            default -> { }
                        }
                    }
                    default -> { }
                }
            }
            reader.close();
        }
        if (segments.isEmpty()) {
            minLat = maxLat = minLon = maxLon = 0;
        }
        String graphName = file.getName();
        if (graphName.endsWith(".graphml")) {
            graphName = graphName.substring(0, graphName.length() - 8);
        }
        logger.info("Map data loaded from {}: {} segments, {} nodes, {} skipped edges",
                file.getName(), segments.size(), nodeCoords.size(), skippedEdges);
        return new StreetNetworkMapData(
                file.getAbsolutePath(), graphName, segments,
                minLat, maxLat, minLon, maxLon,
                nodeCoords.size(), skippedEdges, highwayCounts);
    }
    private static Double parseCoord(String v) {
        if (v == null || v.isBlank()) return null;
        try {
            return Double.parseDouble(v.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static String firstToken(String v) {
        int semi = v.indexOf(';');
        return (semi > 0 ? v.substring(0, semi) : v).trim().toLowerCase(Locale.ROOT);
    }
    private static boolean isTruthy(String v) {
        String s = v.trim().toLowerCase(Locale.ROOT);
        return "true".equals(s) || "yes".equals(s) || "1".equals(s);
    }
    private static int parseSpeed(String v) {
        String s = v.trim().toLowerCase(Locale.ROOT);
        if (s.endsWith("mph")) {
            try {
                return (int) Math.round(Double.parseDouble(s.replace("mph", "").trim()) * 1.60934);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        try {
            return (int) Math.round(Double.parseDouble(s.replaceAll("[^0-9.]", "")));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    private static String attr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        if (v == null) {
            v = reader.getAttributeValue(javax.xml.XMLConstants.XML_NS_URI, name);
        }
        return v;
    }
}
