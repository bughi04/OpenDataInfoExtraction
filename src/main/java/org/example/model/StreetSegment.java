package org.example.model;

// One drawable street edge with WGS84 coordinates and OSM tags
public class StreetSegment {
    private final double lat1;
    private final double lon1;
    private final double lat2;
    private final double lon2;
    private final String highway;
    private final String name;
    private final boolean oneway;
    private final int maxSpeedKmh;
    private final boolean bridge;
    private final boolean tunnel;
    public StreetSegment(double lat1, double lon1, double lat2, double lon2,
                         String highway, String name, boolean oneway,
                         int maxSpeedKmh, boolean bridge, boolean tunnel) {
        this.lat1 = lat1;
        this.lon1 = lon1;
        this.lat2 = lat2;
        this.lon2 = lon2;
        this.highway = highway != null ? highway : "unclassified";
        this.name = name;
        this.oneway = oneway;
        this.maxSpeedKmh = maxSpeedKmh;
        this.bridge = bridge;
        this.tunnel = tunnel;
    }
    public double getLat1() { return lat1; }
    public double getLon1() { return lon1; }
    public double getLat2() { return lat2; }
    public double getLon2() { return lon2; }
    public String getHighway() { return highway; }
    public String getName() { return name; }
    public boolean isOneway() { return oneway; }
    public int getMaxSpeedKmh() { return maxSpeedKmh; }
    public boolean isBridge() { return bridge; }
    public boolean isTunnel() { return tunnel; }
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(highway);
        if (name != null && !name.isBlank()) sb.append(" - ").append(name);
        if (maxSpeedKmh > 0) sb.append(" · ").append(maxSpeedKmh).append(" km/h");
        if (oneway) sb.append(" · oneway");
        if (bridge) sb.append(" · bridge");
        if (tunnel) sb.append(" · tunnel");
        return sb.toString();
    }
}