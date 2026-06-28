package org.example.model;

// One labeled row from {@code Results_Cities.csv}: graph metrics plus official scores
public class ReferenceCityRecord {
    private final String cityAddress;
    private final double umriScore;
    private final double smiScore;
    private final double ptiScore;
    private final double gdpPerCapita;
    private final double clusteringCoefficient;
    private final double assortativityDegree;
    private final double streetsPerNode;
    private final double graphDiameter;
    private final double graphDensity;
    private final double avgDegreeCentrality;
    private final double avgPathLength;
    private final double meanDegree;
    public ReferenceCityRecord(String cityAddress,
                               double umriScore, double smiScore, double ptiScore,
                               double gdpPerCapita,
                               double clusteringCoefficient, double assortativityDegree,
                               double streetsPerNode, double graphDiameter,
                               double graphDensity, double avgDegreeCentrality,
                               double avgPathLength, double meanDegree) {
        this.cityAddress = cityAddress;
        this.umriScore = umriScore;
        this.smiScore = smiScore;
        this.ptiScore = ptiScore;
        this.gdpPerCapita = gdpPerCapita;
        this.clusteringCoefficient = clusteringCoefficient;
        this.assortativityDegree = assortativityDegree;
        this.streetsPerNode = streetsPerNode;
        this.graphDiameter = graphDiameter;
        this.graphDensity = graphDensity;
        this.avgDegreeCentrality = avgDegreeCentrality;
        this.avgPathLength = avgPathLength;
        this.meanDegree = meanDegree;
    }
    public String getCityAddress() { return cityAddress; }
    public double getUmriScore() { return umriScore; }
    public double getGdpPerCapita() { return gdpPerCapita; }
    public double getClusteringCoefficient() { return clusteringCoefficient; }
    public double getAssortativityDegree() { return assortativityDegree; }
    public double getStreetsPerNode() { return streetsPerNode; }
    public double getGraphDiameter() { return graphDiameter; }
    public double getGraphDensity() { return graphDensity; }
    public double getAvgDegreeCentrality() { return avgDegreeCentrality; }
    public double getAvgPathLength() { return avgPathLength; }
    public double targetScore(PredictionTarget target) {
        return switch (target) {
            case UMRI -> umriScore;
            case SMI -> smiScore;
            case PTI -> ptiScore;
        };
    }
}