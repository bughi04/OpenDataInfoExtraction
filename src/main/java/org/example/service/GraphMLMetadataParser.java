package org.example.service;

import org.example.model.StreetNetworkStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

// Streams a GraphML file and aggregates every OSMnx node/edge/graph attribute
public class GraphMLMetadataParser {
    private static final Logger logger = LoggerFactory.getLogger(GraphMLMetadataParser.class);
    public static final String GEOMETRY_PRESENT = "\u0001geometry";
    public record KeyDef(String forType, String attrName) {}
    private static final Set<String> PEDESTRIAN_TYPES = Set.of(
            "footway", "pedestrian", "steps", "living_street");
    private static final Set<String> CYCLING_TYPES = Set.of("cycleway", "cyclestreet");
    private static final Set<String> CAR_TYPES = Set.of(
            "motorway", "motorway_link", "trunk", "trunk_link",
            "primary", "primary_link", "secondary", "secondary_link",
            "tertiary", "tertiary_link");
    private static final Set<String> RESTRICTED_ACCESS = Set.of(
            "private", "no", "destination", "customers", "permit", "agricultural");
    public StreetNetworkStats parse(File file) {
        if (file == null || !file.isFile()) return new StreetNetworkStats();
        try {
            return new GraphMLStreamLoader().load(file).stats();
        } catch (Exception e) {
            logger.warn("GraphML metadata parse failed for {}: {}", file.getName(), e.getMessage());
            return new StreetNetworkStats();
        }
    }
    public static void applyGraphData(StreetNetworkStats stats, Map<String, String> data, Map<String, KeyDef> keys) {
        Map<String, String> graphAttrs = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : data.entrySet()) {
            KeyDef key = keys.get(e.getKey());
            if (key == null || !"graph".equals(key.forType)) continue;
            graphAttrs.put(key.attrName, e.getValue());
            switch (key.attrName) {
                case "created_with" -> stats.setCreatedWith(e.getValue());
                case "created_date" -> stats.setCreatedDate(e.getValue());
                case "crs" -> stats.setCrs(e.getValue());
                case "simplified" -> stats.setSimplified(parseBool(e.getValue()));
                default -> { }
            }
        }
        stats.setGraphAttributes(graphAttrs);
    }
    public static String readElementText(XMLStreamReader reader) throws Exception {
        StringBuilder sb = new StringBuilder();
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {
                sb.append(reader.getText());
            } else if (event == XMLStreamConstants.END_ELEMENT) {
                break;
            }
        }
        return sb.toString().trim();
    }
    public static void skipDataContent(XMLStreamReader reader) throws Exception {
        while (reader.hasNext()) {
            int event = reader.next();
            if (event == XMLStreamConstants.END_ELEMENT
                    && "data".equals(reader.getLocalName())) {
                return;
            }
        }
    }
    private static Boolean parseBool(String raw) {
        if (raw == null || raw.isBlank()) return null;
        return "true".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw) || "1".equals(raw);
    }
    public static final class Aggregator {
        private final Map<String, Integer> highwayCounts = new LinkedHashMap<>();
        private final Map<String, Double> highwayLengths = new LinkedHashMap<>();
        private final Map<String, Integer> nodeCoverage = new LinkedHashMap<>();
        private final Map<String, Integer> edgeCoverage = new LinkedHashMap<>();
        private final Map<String, Integer> accessCounts = new LinkedHashMap<>();
        private final Map<String, Integer> junctionCounts = new LinkedHashMap<>();
        private final Map<String, Integer> serviceCounts = new LinkedHashMap<>();
        private final Map<String, Integer> areaCounts = new LinkedHashMap<>();
        private final Map<String, Integer> nodeHighwayCounts = new LinkedHashMap<>();
        private double totalLength;
        private double restrictedAccessLength;
        private int edgesWithLength;
        private int onewayEdges;
        private int twowayEdges;
        private int reversedEdges;
        private int notReversedEdges;
        private int namedEdges;
        private int totalEdges;
        private int bridgeCount;
        private int tunnelCount;
        private int edgesWithOsmid;
        private int edgesWithRef;
        private int nodesWithRef;
        private int edgesWithGeometry;
        private long geometryVertexCount;
        private double speedSum;
        private int speedCount;
        private double laneSum;
        private int laneCount;
        private double widthSum;
        private int widthCount;
        private double estWidthSum;
        private int estWidthCount;
        private int nodeCount;
        private double streetCountSum;
        private int maxStreetCount;
        private int complexIntersections;
        private int trafficInfraNodes;
        private int nodesWithCoordinates;
        private double minLat = Double.POSITIVE_INFINITY;
        private double maxLat = Double.NEGATIVE_INFINITY;
        private double minLon = Double.POSITIVE_INFINITY;
        private double maxLon = Double.NEGATIVE_INFINITY;
        void acceptNode(Map<String, String> data, Map<String, KeyDef> keys) {
            nodeCount++;
            Double lat = null;
            Double lon = null;
            Integer streetCount = null;
            for (Map.Entry<String, String> e : data.entrySet()) {
                KeyDef key = keys.get(e.getKey());
                if (key == null || !"node".equals(key.forType)) continue;
                String val = e.getValue();
                if (val == null || val.isBlank()) continue;
                nodeCoverage.merge(key.attrName, 1, Integer::sum);
                switch (key.attrName) {
                    case "y" -> lat = parseDouble(firstToken(val));
                    case "x" -> lon = parseDouble(firstToken(val));
                    case "street_count" -> streetCount = parseInt(firstToken(val));
                    case "highway" -> {
                        String hw = normalizeTag(firstToken(val));
                        nodeHighwayCounts.merge(hw, 1, Integer::sum);
                        trafficInfraNodes++;
                    }
                    case "ref" -> {
                        if (!val.isBlank()) nodesWithRef++;
                    }
                    default -> { }
                }
            }
            if (lat != null && lon != null && lat >= -90 && lat <= 90 && lon >= -180 && lon <= 180) {
                nodesWithCoordinates++;
                minLat = Math.min(minLat, lat);
                maxLat = Math.max(maxLat, lat);
                minLon = Math.min(minLon, lon);
                maxLon = Math.max(maxLon, lon);
            }
            if (streetCount != null) {
                streetCountSum += streetCount;
                if (streetCount > maxStreetCount) maxStreetCount = streetCount;
                if (streetCount >= 4) complexIntersections++;
            }
        }
        void acceptEdge(Map<String, String> data, Map<String, KeyDef> keys) {
            totalEdges++;
            String highway = null;
            Double length = null;
            Boolean oneway = null;
            Boolean reversed = null;
            String access = null;
            for (Map.Entry<String, String> e : data.entrySet()) {
                KeyDef key = keys.get(e.getKey());
                if (key == null || !"edge".equals(key.forType)) continue;
                String val = e.getValue();
                if (val == null || val.isBlank()) continue;
                edgeCoverage.merge(key.attrName, 1, Integer::sum);
                switch (key.attrName) {
                    case "highway" -> highway = normalizeTag(firstToken(val));
                    case "length" -> length = parseDouble(firstToken(val));
                    case "oneway" -> oneway = parseBool(val);
                    case "reversed" -> reversed = parseBool(val);
                    case "name" -> {
                        if (!val.isBlank() && !"nan".equalsIgnoreCase(val)) namedEdges++;
                    }
                    case "bridge" -> { if (isTruthy(val)) bridgeCount++; }
                    case "tunnel" -> { if (isTruthy(val)) tunnelCount++; }
                    case "osmid" -> edgesWithOsmid++;
                    case "ref" -> { if (!val.isBlank()) edgesWithRef++; }
                    case "maxspeed" -> {
                        Double speed = parseMaxSpeed(val);
                        if (speed != null) { speedSum += speed; speedCount++; }
                    }
                    case "lanes" -> {
                        Double lanes = parseLanes(val);
                        if (lanes != null) { laneSum += lanes; laneCount++; }
                    }
                    case "width" -> {
                        Double w = parseWidth(val);
                        if (w != null) { widthSum += w; widthCount++; }
                    }
                    case "est_width" -> {
                        Double w = parseWidth(val);
                        if (w != null) { estWidthSum += w; estWidthCount++; }
                    }
                    case "access" -> {
                        access = normalizeTag(firstToken(val));
                        accessCounts.merge(access, 1, Integer::sum);
                    }
                    case "junction" -> junctionCounts.merge(normalizeTag(firstToken(val)), 1, Integer::sum);
                    case "service" -> serviceCounts.merge(normalizeTag(firstToken(val)), 1, Integer::sum);
                    case "area" -> areaCounts.merge(normalizeTag(firstToken(val)), 1, Integer::sum);
                    case "geometry" -> {
                        edgesWithGeometry++;
                        if (!GEOMETRY_PRESENT.equals(val)) {
                            geometryVertexCount += countGeometryVertices(val);
                        }
                    }
                    default -> { }
                }
            }
            if (highway != null) {
                highwayCounts.merge(highway, 1, Integer::sum);
                double lenWeight = length != null && length > 0 ? length : 1.0;
                highwayLengths.merge(highway, lenWeight, Double::sum);
            }
            if (length != null && length > 0) {
                totalLength += length;
                edgesWithLength++;
                if (access != null && RESTRICTED_ACCESS.contains(access)) {
                    restrictedAccessLength += length;
                }
            }
            if (oneway != null) {
                if (oneway) onewayEdges++;
                else twowayEdges++;
            }
            if (reversed != null) {
                if (reversed) reversedEdges++;
                else notReversedEdges++;
            }
        }
        void finalizeInto(StreetNetworkStats stats, Map<String, KeyDef> keys) {
            Map<String, String> schema = new LinkedHashMap<>();
            keys.forEach((id, k) -> schema.put(id, k.forType + ":" + k.attrName));
            stats.setKeySchema(schema);
            stats.setGraphAttributeNames(attrNamesFor(keys, "graph"));
            stats.setNodeAttributeNames(attrNamesFor(keys, "node"));
            stats.setEdgeAttributeNames(attrNamesFor(keys, "edge"));
            stats.setHighwayTypeCounts(new LinkedHashMap<>(highwayCounts));
            stats.setNodeAttributeCoverage(new LinkedHashMap<>(nodeCoverage));
            stats.setEdgeAttributeCoverage(new LinkedHashMap<>(edgeCoverage));
            stats.setAccessValueCounts(new LinkedHashMap<>(accessCounts));
            stats.setJunctionValueCounts(new LinkedHashMap<>(junctionCounts));
            stats.setServiceValueCounts(new LinkedHashMap<>(serviceCounts));
            stats.setAreaValueCounts(new LinkedHashMap<>(areaCounts));
            stats.setNodeHighwayValueCounts(new LinkedHashMap<>(nodeHighwayCounts));
            stats.setTotalLengthMeters(totalLength);
            stats.setEdgesWithLength(edgesWithLength);
            stats.setAverageEdgeLengthMeters(edgesWithLength > 0 ? totalLength / edgesWithLength : 0);
            stats.setRestrictedAccessLengthPct(totalLength > 0 ? 100.0 * restrictedAccessLength / totalLength : 0);
            stats.setOnewayEdges(onewayEdges);
            stats.setTwowayEdges(twowayEdges);
            int dirTotal = onewayEdges + twowayEdges;
            stats.setOnewayRatio(dirTotal > 0 ? onewayEdges / (double) dirTotal : 0);
            stats.setReversedEdges(reversedEdges);
            stats.setNotReversedEdges(notReversedEdges);
            int revTotal = reversedEdges + notReversedEdges;
            stats.setReversedRatio(revTotal > 0 ? reversedEdges / (double) revTotal : 0);
            stats.setNamedRoadRatio(totalEdges > 0 ? namedEdges / (double) totalEdges : 0);
            stats.setBridgeCount(bridgeCount);
            stats.setTunnelCount(tunnelCount);
            stats.setEdgesWithOsmid(edgesWithOsmid);
            stats.setEdgesWithRef(edgesWithRef);
            stats.setNodesWithRef(nodesWithRef);
            stats.setEdgesWithGeometry(edgesWithGeometry);
            stats.setGeometryVertexCount(geometryVertexCount);
            stats.setAvgGeometryVertices(edgesWithGeometry > 0
                    ? geometryVertexCount / (double) edgesWithGeometry : 0);
            stats.setAvgMaxSpeedKmh(speedCount > 0 ? speedSum / speedCount : 0);
            stats.setEdgesWithMaxSpeed(speedCount);
            stats.setAvgLanes(laneCount > 0 ? laneSum / laneCount : 0);
            stats.setEdgesWithLanes(laneCount);
            stats.setAvgWidthMeters(widthCount > 0 ? widthSum / widthCount : 0);
            stats.setAvgEstWidthMeters(estWidthCount > 0 ? estWidthSum / estWidthCount : 0);
            stats.setEdgesWithWidth(widthCount);
            stats.setEdgesWithEstWidth(estWidthCount);
            stats.setAvgStreetCountPerNode(nodeCount > 0 ? streetCountSum / nodeCount : 0);
            stats.setMaxStreetCount(maxStreetCount);
            stats.setComplexIntersections(complexIntersections);
            stats.setTrafficInfrastructureNodes(trafficInfraNodes);
            double osmStreetsPerNode = stats.getAvgStreetCountPerNode();
            stats.setStreetsPerNode(osmStreetsPerNode > 0
                    ? osmStreetsPerNode
                    : (nodeCount > 0 ? totalEdges / (double) nodeCount : 0));
            stats.setNodesWithCoordinates(nodesWithCoordinates);
            if (nodesWithCoordinates > 0) {
                stats.setMinLatitude(minLat);
                stats.setMaxLatitude(maxLat);
                stats.setMinLongitude(minLon);
                stats.setMaxLongitude(maxLon);
                stats.setBoundingBoxSpanKm(computeBBoxSpanKm(minLat, maxLat, minLon, maxLon));
            }
            double lengthBasis = highwayLengths.values().stream().mapToDouble(Double::doubleValue).sum();
            if (lengthBasis <= 0) {
                lengthBasis = highwayCounts.values().stream().mapToInt(Integer::intValue).sum();
            }
            stats.setPctMotorway(pctLength("motorway", lengthBasis) + pctLength("motorway_link", lengthBasis));
            stats.setPctTrunk(pctLength("trunk", lengthBasis) + pctLength("trunk_link", lengthBasis));
            stats.setPctPrimary(pctLength("primary", lengthBasis) + pctLength("primary_link", lengthBasis));
            stats.setPctSecondary(pctLength("secondary", lengthBasis) + pctLength("secondary_link", lengthBasis));
            stats.setPctTertiary(pctLength("tertiary", lengthBasis) + pctLength("tertiary_link", lengthBasis));
            stats.setPctResidential(pctLength("residential", lengthBasis) + pctLength("living_street", lengthBasis));
            stats.setPctServiceHighway(pctLength("service", lengthBasis));
            stats.setPctFootway(pctLength("footway", lengthBasis) + pctLength("pedestrian", lengthBasis) + pctLength("steps", lengthBasis));
            stats.setPctCycleway(pctLength("cycleway", lengthBasis) + pctLength("cyclestreet", lengthBasis));
            stats.setPctPath(pctLength("path", lengthBasis));
            double categorized = stats.getPctMotorway() + stats.getPctTrunk() + stats.getPctPrimary()
                    + stats.getPctSecondary() + stats.getPctTertiary() + stats.getPctResidential()
                    + stats.getPctServiceHighway() + stats.getPctFootway() + stats.getPctCycleway() + stats.getPctPath();
            stats.setPctOtherHighway(Math.max(0, 100 - categorized));
            stats.setDominantHighwayType(highwayLengths.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(highwayCounts.entrySet().stream()
                            .max(Map.Entry.comparingByValue())
                            .map(Map.Entry::getKey)
                            .orElse(null)));
            stats.setPedestrianFriendlyLengthPct(lengthShare(PEDESTRIAN_TYPES, lengthBasis));
            stats.setCyclingFriendlyLengthPct(lengthShare(CYCLING_TYPES, lengthBasis));
            stats.setCarOrientedLengthPct(lengthShare(CAR_TYPES, lengthBasis));
        }
        private List<String> attrNamesFor(Map<String, KeyDef> keys, String forType) {
            return keys.values().stream()
                    .filter(k -> forType.equals(k.forType))
                    .map(k -> k.attrName)
                    .distinct()
                    .sorted()
                    .collect(Collectors.toList());
        }
        private double pctLength(String type, double basis) {
            if (basis <= 0) return 0;
            double len = highwayLengths.getOrDefault(type, 0.0);
            if (len == 0 && highwayCounts.containsKey(type)) {
                len = highwayCounts.get(type);
            }
            return 100.0 * len / basis;
        }
        private double lengthShare(Set<String> types, double basis) {
            if (basis <= 0) return 0;
            double len = 0;
            for (String t : types) {
                len += highwayLengths.getOrDefault(t, 0.0);
                if (!highwayLengths.containsKey(t) && highwayCounts.containsKey(t)) {
                    len += highwayCounts.get(t);
                }
            }
            return 100.0 * len / basis;
        }
    }
    private static double computeBBoxSpanKm(double minLat, double maxLat, double minLon, double maxLon) {
        double midLat = (minLat + maxLat) / 2.0;
        double latKm = (maxLat - minLat) * 111.0;
        double lonKm = (maxLon - minLon) * 111.0 * Math.cos(Math.toRadians(midLat));
        return Math.sqrt(latKm * latKm + lonKm * lonKm);
    }
    private static long countGeometryVertices(String geometry) {
        if (geometry == null || geometry.isBlank()) return 0;
        int open = geometry.indexOf('(');
        if (open < 0) return 0;
        String coords = geometry.substring(open + 1).replace(")", "").trim();
        if (coords.isEmpty()) return 0;
        return coords.split(",").length;
    }
    private static String normalizeTag(String raw) {
        if (raw == null || raw.isBlank()) return "unknown";
        return raw.toLowerCase(Locale.ROOT).trim();
    }
    private static String firstToken(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.startsWith("[")) {
            int end = s.indexOf(',');
            if (end < 0) end = s.indexOf(']');
            if (end > 1) return s.substring(1, end).replace("'", "").replace("\"", "").trim();
        }
        return s.replace("'", "").replace("\"", "");
    }
    private static Integer parseInt(String raw) {
        try {
            return raw != null ? Integer.parseInt(raw.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static Double parseDouble(String raw) {
        try {
            return raw != null ? Double.parseDouble(raw.trim()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static Double parseWidth(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = firstToken(raw).toLowerCase(Locale.ROOT).replace("m", "").trim();
        try {
            double v = Double.parseDouble(token);
            return v > 0 && v < 100 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static Double parseMaxSpeed(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = firstToken(raw).toLowerCase(Locale.ROOT);
        if (token.contains("walk") || token.contains("slow") || token.equals("none")) return null;
        token = token.replace("km/h", "").replace("mph", "").trim();
        try {
            double v = Double.parseDouble(token);
            if (raw.toLowerCase(Locale.ROOT).contains("mph")) v *= 1.60934;
            return v > 0 && v < 200 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static Double parseLanes(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String token = firstToken(raw);
        try {
            double v = Double.parseDouble(token);
            return v > 0 && v <= 12 ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private static boolean isTruthy(String raw) {
        if (raw == null || raw.isBlank()) return false;
        String s = raw.toLowerCase(Locale.ROOT);
        return "yes".equals(s) || "true".equals(s) || "1".equals(s);
    }
}