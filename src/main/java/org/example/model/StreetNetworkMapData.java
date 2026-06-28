package org.example.model;

import java.util.List;
import java.util.Map;

// Drawable street network extracted from GraphML for map rendering
public class StreetNetworkMapData {
    private final String sourceFile;
    private final String graphName;
    private final List<StreetSegment> segments;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final int nodeCount;
    private final int skippedEdges;
    private final Map<String, Integer> highwayCounts;
    public StreetNetworkMapData(String sourceFile, String graphName,
                                List<StreetSegment> segments,
                                double minLat, double maxLat, double minLon, double maxLon,
                                int nodeCount, int skippedEdges,
                                Map<String, Integer> highwayCounts) {
        this.sourceFile = sourceFile;
        this.graphName = graphName;
        this.segments = segments != null ? List.copyOf(segments) : List.of();
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.nodeCount = nodeCount;
        this.skippedEdges = skippedEdges;
        this.highwayCounts = highwayCounts != null ? Map.copyOf(highwayCounts) : Map.of();
    }

    public String getGraphName() { return graphName; }
    public List<StreetSegment> getSegments() { return segments; }
    public double getMinLat() { return minLat; }
    public double getMaxLat() { return maxLat; }
    public double getMinLon() { return minLon; }
    public double getMaxLon() { return maxLon; }
    public int getNodeCount() { return nodeCount; }

    public boolean isEmpty() { return segments.isEmpty(); }
    public double getLatSpan() { return maxLat - minLat; }
    public double getLonSpan() { return maxLon - minLon; }
}