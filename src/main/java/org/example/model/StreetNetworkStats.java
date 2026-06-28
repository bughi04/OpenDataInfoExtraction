package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Aggregated OSM / OSMnx street-network attributes extracted from GraphML metadata.
@JsonIgnoreProperties(ignoreUnknown = true)
public class StreetNetworkStats implements Serializable {
    private static final long serialVersionUID = 1L;

    private Map<String, String> keySchema = new LinkedHashMap<>();
    private List<String> graphAttributeNames;
    private List<String> nodeAttributeNames;
    private List<String> edgeAttributeNames;

    private Map<String, String> graphAttributes = new LinkedHashMap<>();
    private String createdWith;
    private String createdDate;
    private String crs;
    private Boolean simplified;

    private double minLatitude;
    private double maxLatitude;
    private double minLongitude;
    private double maxLongitude;
    private double boundingBoxSpanKm;
    private int nodesWithCoordinates;

    private double totalLengthMeters;
    private double averageEdgeLengthMeters;
    private int edgesWithLength;

    private double pctMotorway;
    private double pctTrunk;
    private double pctPrimary;
    private double pctSecondary;
    private double pctTertiary;
    private double pctResidential;
    private double pctServiceHighway;
    private double pctFootway;
    private double pctCycleway;
    private double pctPath;
    private double pctOtherHighway;
    private String dominantHighwayType;
    private Map<String, Integer> highwayTypeCounts = new LinkedHashMap<>();

    private double onewayRatio;
    private int onewayEdges;
    private int twowayEdges;
    private double reversedRatio;
    private int reversedEdges;
    private int notReversedEdges;
    private double avgMaxSpeedKmh;
    private int edgesWithMaxSpeed;
    private double avgLanes;
    private int edgesWithLanes;
    private double avgWidthMeters;
    private double avgEstWidthMeters;
    private int edgesWithWidth;
    private int edgesWithEstWidth;
    private double namedRoadRatio;
    private int bridgeCount;
    private int tunnelCount;
    private int edgesWithOsmid;
    private int edgesWithRef;
    private int nodesWithRef;
    private int edgesWithGeometry;
    private long geometryVertexCount;
    private double avgGeometryVertices;

    private double restrictedAccessLengthPct;
    private Map<String, Integer> accessValueCounts = new LinkedHashMap<>();
    private Map<String, Integer> junctionValueCounts = new LinkedHashMap<>();
    private Map<String, Integer> serviceValueCounts = new LinkedHashMap<>();
    private Map<String, Integer> areaValueCounts = new LinkedHashMap<>();
    private Map<String, Integer> nodeHighwayValueCounts = new LinkedHashMap<>();

    private Map<String, Integer> nodeAttributeCoverage = new LinkedHashMap<>();
    private Map<String, Integer> edgeAttributeCoverage = new LinkedHashMap<>();

    private double avgStreetCountPerNode;
    private int maxStreetCount;
    private int complexIntersections;
    private int trafficInfrastructureNodes;

    private double streetsPerNode;
    private double pedestrianFriendlyLengthPct;
    private double cyclingFriendlyLengthPct;
    private double carOrientedLengthPct;

    public Map<String, String> getKeySchema() { return keySchema; }
    public void setKeySchema(Map<String, String> keySchema) {
        this.keySchema = keySchema != null ? keySchema : new LinkedHashMap<>();
    }
    public List<String> getGraphAttributeNames() { return graphAttributeNames; }
    public void setGraphAttributeNames(List<String> graphAttributeNames) { this.graphAttributeNames = graphAttributeNames; }
    public List<String> getNodeAttributeNames() { return nodeAttributeNames; }
    public void setNodeAttributeNames(List<String> nodeAttributeNames) { this.nodeAttributeNames = nodeAttributeNames; }
    public List<String> getEdgeAttributeNames() { return edgeAttributeNames; }
    public void setEdgeAttributeNames(List<String> edgeAttributeNames) { this.edgeAttributeNames = edgeAttributeNames; }
    public Map<String, String> getGraphAttributes() { return graphAttributes; }
    public void setGraphAttributes(Map<String, String> graphAttributes) {
        this.graphAttributes = graphAttributes != null ? graphAttributes : new LinkedHashMap<>();
    }
    public String getCreatedWith() { return createdWith; }
    public void setCreatedWith(String createdWith) { this.createdWith = createdWith; }
    public String getCreatedDate() { return createdDate; }
    public void setCreatedDate(String createdDate) { this.createdDate = createdDate; }
    public String getCrs() { return crs; }
    public void setCrs(String crs) { this.crs = crs; }
    public Boolean getSimplified() { return simplified; }
    public void setSimplified(Boolean simplified) { this.simplified = simplified; }
    public double getMinLatitude() { return minLatitude; }
    public void setMinLatitude(double minLatitude) { this.minLatitude = minLatitude; }
    public double getMaxLatitude() { return maxLatitude; }
    public void setMaxLatitude(double maxLatitude) { this.maxLatitude = maxLatitude; }
    public double getMinLongitude() { return minLongitude; }
    public void setMinLongitude(double minLongitude) { this.minLongitude = minLongitude; }
    public double getMaxLongitude() { return maxLongitude; }
    public void setMaxLongitude(double maxLongitude) { this.maxLongitude = maxLongitude; }
    public double getBoundingBoxSpanKm() { return boundingBoxSpanKm; }
    public void setBoundingBoxSpanKm(double boundingBoxSpanKm) { this.boundingBoxSpanKm = boundingBoxSpanKm; }
    public int getNodesWithCoordinates() { return nodesWithCoordinates; }
    public void setNodesWithCoordinates(int nodesWithCoordinates) { this.nodesWithCoordinates = nodesWithCoordinates; }
    public double getTotalLengthMeters() { return totalLengthMeters; }
    public void setTotalLengthMeters(double totalLengthMeters) { this.totalLengthMeters = totalLengthMeters; }
    @JsonIgnore
    public double getTotalLengthKm() { return totalLengthMeters / 1000.0; }
    public double getAverageEdgeLengthMeters() { return averageEdgeLengthMeters; }
    public void setAverageEdgeLengthMeters(double v) { this.averageEdgeLengthMeters = v; }
    public int getEdgesWithLength() { return edgesWithLength; }
    public void setEdgesWithLength(int edgesWithLength) { this.edgesWithLength = edgesWithLength; }
    public double getPctMotorway() { return pctMotorway; }
    public void setPctMotorway(double pctMotorway) { this.pctMotorway = pctMotorway; }
    public double getPctTrunk() { return pctTrunk; }
    public void setPctTrunk(double pctTrunk) { this.pctTrunk = pctTrunk; }
    public double getPctPrimary() { return pctPrimary; }
    public void setPctPrimary(double pctPrimary) { this.pctPrimary = pctPrimary; }
    public double getPctSecondary() { return pctSecondary; }
    public void setPctSecondary(double pctSecondary) { this.pctSecondary = pctSecondary; }
    public double getPctTertiary() { return pctTertiary; }
    public void setPctTertiary(double pctTertiary) { this.pctTertiary = pctTertiary; }
    public double getPctResidential() { return pctResidential; }
    public void setPctResidential(double pctResidential) { this.pctResidential = pctResidential; }
    public double getPctServiceHighway() { return pctServiceHighway; }
    public void setPctServiceHighway(double pctServiceHighway) { this.pctServiceHighway = pctServiceHighway; }
    public double getPctFootway() { return pctFootway; }
    public void setPctFootway(double pctFootway) { this.pctFootway = pctFootway; }
    public double getPctCycleway() { return pctCycleway; }
    public void setPctCycleway(double pctCycleway) { this.pctCycleway = pctCycleway; }
    public double getPctPath() { return pctPath; }
    public void setPctPath(double pctPath) { this.pctPath = pctPath; }
    public double getPctOtherHighway() { return pctOtherHighway; }
    public void setPctOtherHighway(double pctOtherHighway) { this.pctOtherHighway = pctOtherHighway; }
    public String getDominantHighwayType() { return dominantHighwayType; }
    public void setDominantHighwayType(String dominantHighwayType) { this.dominantHighwayType = dominantHighwayType; }
    public Map<String, Integer> getHighwayTypeCounts() { return highwayTypeCounts; }
    public void setHighwayTypeCounts(Map<String, Integer> highwayTypeCounts) {
        this.highwayTypeCounts = highwayTypeCounts != null ? highwayTypeCounts : new LinkedHashMap<>();
    }
    public double getOnewayRatio() { return onewayRatio; }
    public void setOnewayRatio(double onewayRatio) { this.onewayRatio = onewayRatio; }
    public int getOnewayEdges() { return onewayEdges; }
    public void setOnewayEdges(int onewayEdges) { this.onewayEdges = onewayEdges; }
    public int getTwowayEdges() { return twowayEdges; }
    public void setTwowayEdges(int twowayEdges) { this.twowayEdges = twowayEdges; }

    public double getReversedRatio() { return reversedRatio; }
    public void setReversedRatio(double reversedRatio) { this.reversedRatio = reversedRatio; }

    public int getReversedEdges() { return reversedEdges; }
    public void setReversedEdges(int reversedEdges) { this.reversedEdges = reversedEdges; }

    public int getNotReversedEdges() { return notReversedEdges; }
    public void setNotReversedEdges(int notReversedEdges) { this.notReversedEdges = notReversedEdges; }

    public double getAvgMaxSpeedKmh() { return avgMaxSpeedKmh; }
    public void setAvgMaxSpeedKmh(double avgMaxSpeedKmh) { this.avgMaxSpeedKmh = avgMaxSpeedKmh; }

    public int getEdgesWithMaxSpeed() { return edgesWithMaxSpeed; }
    public void setEdgesWithMaxSpeed(int edgesWithMaxSpeed) { this.edgesWithMaxSpeed = edgesWithMaxSpeed; }

    public double getAvgLanes() { return avgLanes; }
    public void setAvgLanes(double avgLanes) { this.avgLanes = avgLanes; }

    public int getEdgesWithLanes() { return edgesWithLanes; }
    public void setEdgesWithLanes(int edgesWithLanes) { this.edgesWithLanes = edgesWithLanes; }

    public double getAvgWidthMeters() { return avgWidthMeters; }
    public void setAvgWidthMeters(double avgWidthMeters) { this.avgWidthMeters = avgWidthMeters; }

    public double getAvgEstWidthMeters() { return avgEstWidthMeters; }
    public void setAvgEstWidthMeters(double avgEstWidthMeters) { this.avgEstWidthMeters = avgEstWidthMeters; }

    public int getEdgesWithWidth() { return edgesWithWidth; }
    public void setEdgesWithWidth(int edgesWithWidth) { this.edgesWithWidth = edgesWithWidth; }

    public int getEdgesWithEstWidth() { return edgesWithEstWidth; }
    public void setEdgesWithEstWidth(int edgesWithEstWidth) { this.edgesWithEstWidth = edgesWithEstWidth; }

    public double getNamedRoadRatio() { return namedRoadRatio; }
    public void setNamedRoadRatio(double namedRoadRatio) { this.namedRoadRatio = namedRoadRatio; }

    public int getBridgeCount() { return bridgeCount; }
    public void setBridgeCount(int bridgeCount) { this.bridgeCount = bridgeCount; }

    public int getTunnelCount() { return tunnelCount; }
    public void setTunnelCount(int tunnelCount) { this.tunnelCount = tunnelCount; }

    public int getEdgesWithOsmid() { return edgesWithOsmid; }
    public void setEdgesWithOsmid(int edgesWithOsmid) { this.edgesWithOsmid = edgesWithOsmid; }

    public int getEdgesWithRef() { return edgesWithRef; }
    public void setEdgesWithRef(int edgesWithRef) { this.edgesWithRef = edgesWithRef; }

    public int getNodesWithRef() { return nodesWithRef; }
    public void setNodesWithRef(int nodesWithRef) { this.nodesWithRef = nodesWithRef; }

    public int getEdgesWithGeometry() { return edgesWithGeometry; }
    public void setEdgesWithGeometry(int edgesWithGeometry) { this.edgesWithGeometry = edgesWithGeometry; }

    public long getGeometryVertexCount() { return geometryVertexCount; }
    public void setGeometryVertexCount(long geometryVertexCount) { this.geometryVertexCount = geometryVertexCount; }

    public double getAvgGeometryVertices() { return avgGeometryVertices; }
    public void setAvgGeometryVertices(double avgGeometryVertices) { this.avgGeometryVertices = avgGeometryVertices; }

    public double getRestrictedAccessLengthPct() { return restrictedAccessLengthPct; }
    public void setRestrictedAccessLengthPct(double restrictedAccessLengthPct) {
        this.restrictedAccessLengthPct = restrictedAccessLengthPct;
    }

    public Map<String, Integer> getAccessValueCounts() { return accessValueCounts; }
    public void setAccessValueCounts(Map<String, Integer> accessValueCounts) {
        this.accessValueCounts = accessValueCounts != null ? accessValueCounts : new LinkedHashMap<>();
    }

    public Map<String, Integer> getJunctionValueCounts() { return junctionValueCounts; }
    public void setJunctionValueCounts(Map<String, Integer> junctionValueCounts) {
        this.junctionValueCounts = junctionValueCounts != null ? junctionValueCounts : new LinkedHashMap<>();
    }

    public Map<String, Integer> getServiceValueCounts() { return serviceValueCounts; }
    public void setServiceValueCounts(Map<String, Integer> serviceValueCounts) {
        this.serviceValueCounts = serviceValueCounts != null ? serviceValueCounts : new LinkedHashMap<>();
    }

    public Map<String, Integer> getAreaValueCounts() { return areaValueCounts; }
    public void setAreaValueCounts(Map<String, Integer> areaValueCounts) {
        this.areaValueCounts = areaValueCounts != null ? areaValueCounts : new LinkedHashMap<>();
    }

    public Map<String, Integer> getNodeHighwayValueCounts() { return nodeHighwayValueCounts; }
    public void setNodeHighwayValueCounts(Map<String, Integer> nodeHighwayValueCounts) {
        this.nodeHighwayValueCounts = nodeHighwayValueCounts != null ? nodeHighwayValueCounts : new LinkedHashMap<>();
    }

    public Map<String, Integer> getNodeAttributeCoverage() { return nodeAttributeCoverage; }
    public void setNodeAttributeCoverage(Map<String, Integer> nodeAttributeCoverage) {
        this.nodeAttributeCoverage = nodeAttributeCoverage != null ? nodeAttributeCoverage : new LinkedHashMap<>();
    }

    public Map<String, Integer> getEdgeAttributeCoverage() { return edgeAttributeCoverage; }
    public void setEdgeAttributeCoverage(Map<String, Integer> edgeAttributeCoverage) {
        this.edgeAttributeCoverage = edgeAttributeCoverage != null ? edgeAttributeCoverage : new LinkedHashMap<>();
    }

    public double getAvgStreetCountPerNode() { return avgStreetCountPerNode; }
    public void setAvgStreetCountPerNode(double avgStreetCountPerNode) {
        this.avgStreetCountPerNode = avgStreetCountPerNode;
    }

    public int getMaxStreetCount() { return maxStreetCount; }
    public void setMaxStreetCount(int maxStreetCount) { this.maxStreetCount = maxStreetCount; }

    public int getComplexIntersections() { return complexIntersections; }
    public void setComplexIntersections(int complexIntersections) {
        this.complexIntersections = complexIntersections;
    }

    public int getTrafficInfrastructureNodes() { return trafficInfrastructureNodes; }
    public void setTrafficInfrastructureNodes(int trafficInfrastructureNodes) {
        this.trafficInfrastructureNodes = trafficInfrastructureNodes;
    }

    public double getStreetsPerNode() { return streetsPerNode; }
    public void setStreetsPerNode(double streetsPerNode) { this.streetsPerNode = streetsPerNode; }

    public double getPedestrianFriendlyLengthPct() { return pedestrianFriendlyLengthPct; }
    public void setPedestrianFriendlyLengthPct(double v) { this.pedestrianFriendlyLengthPct = v; }

    public double getCyclingFriendlyLengthPct() { return cyclingFriendlyLengthPct; }
    public void setCyclingFriendlyLengthPct(double v) { this.cyclingFriendlyLengthPct = v; }

    public double getCarOrientedLengthPct() { return carOrientedLengthPct; }
    public void setCarOrientedLengthPct(double v) { this.carOrientedLengthPct = v; }

    @JsonIgnore
    public int getBridgeAndTunnelCount() { return bridgeCount + tunnelCount; }

    @JsonIgnore
    public boolean hasData() {
        return totalLengthMeters > 0 || !highwayTypeCounts.isEmpty()
                || !edgeAttributeCoverage.isEmpty() || !nodeAttributeCoverage.isEmpty();
    }

    @JsonIgnore
    public int getEdgeAttributeCount() { return edgeAttributeCoverage.size(); }
    @JsonIgnore
    public int getNodeAttributeCount() { return nodeAttributeCoverage.size(); }

    @JsonIgnore
    public String formatHighwayMixSummary() {
        if (highwayTypeCounts.isEmpty()) return "N/A";
        return highwayTypeCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(5)
                .map(e -> e.getKey() + " " + e.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("N/A");
    }

    @JsonIgnore
    public String formatAttributeCoverageSummary() {
        return edgeAttributeCoverage.size() + " edge attrs, " + nodeAttributeCoverage.size() + " node attrs";
    }
}
