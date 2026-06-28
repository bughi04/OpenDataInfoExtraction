package org.example.util;

import org.example.model.MapColorMode;
import org.example.model.StreetSegment;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// OSM-inspired colors for street-network map rendering.
public final class HighwayColorScheme {
    private HighwayColorScheme() {}
    private static final Map<String, Color> HIGHWAY_COLORS = new LinkedHashMap<>();
    static {
        HIGHWAY_COLORS.put("motorway", new Color(224, 71, 92));
        HIGHWAY_COLORS.put("motorway_link", new Color(224, 110, 120));
        HIGHWAY_COLORS.put("trunk", new Color(241, 144, 110));
        HIGHWAY_COLORS.put("trunk_link", new Color(245, 170, 140));
        HIGHWAY_COLORS.put("primary", new Color(252, 198, 130));
        HIGHWAY_COLORS.put("primary_link", new Color(252, 215, 160));
        HIGHWAY_COLORS.put("secondary", new Color(247, 240, 175));
        HIGHWAY_COLORS.put("secondary_link", new Color(250, 245, 195));
        HIGHWAY_COLORS.put("tertiary", new Color(255, 255, 180));
        HIGHWAY_COLORS.put("tertiary_link", new Color(255, 255, 210));
        HIGHWAY_COLORS.put("residential", new Color(200, 200, 205));
        HIGHWAY_COLORS.put("living_street", new Color(210, 210, 215));
        HIGHWAY_COLORS.put("service", new Color(195, 195, 200));
        HIGHWAY_COLORS.put("unclassified", new Color(185, 185, 190));
        HIGHWAY_COLORS.put("footway", new Color(130, 195, 130));
        HIGHWAY_COLORS.put("pedestrian", new Color(110, 180, 110));
        HIGHWAY_COLORS.put("steps", new Color(90, 160, 90));
        HIGHWAY_COLORS.put("path", new Color(160, 210, 140));
        HIGHWAY_COLORS.put("cycleway", new Color(80, 170, 200));
        HIGHWAY_COLORS.put("track", new Color(190, 170, 130));
        HIGHWAY_COLORS.put("construction", new Color(180, 180, 180));
    }
    public static Color colorFor(StreetSegment seg, MapColorMode mode) {
        return switch (mode) {
            case HIGHWAY -> colorForHighway(seg.getHighway());
            case SPEED -> colorForSpeed(seg.getMaxSpeedKmh());
            case ONEWAY -> seg.isOneway()
                    ? new Color(66, 133, 244)
                    : new Color(120, 120, 120);
            case INFRASTRUCTURE -> {
                if (seg.isBridge()) yield new Color(180, 60, 200);
                if (seg.isTunnel()) yield new Color(80, 80, 120);
                yield colorForHighway(seg.getHighway());
            }
        };
    }
    public static Color colorForHighway(String highway) {
        if (highway == null || highway.isBlank()) {
            return HIGHWAY_COLORS.get("unclassified");
        }
        String key = highway.toLowerCase().trim();
        Color c = HIGHWAY_COLORS.get(key);
        if (c != null) return c;
        if (key.endsWith("_link")) {
            c = HIGHWAY_COLORS.get(key);
            if (c != null) return c;
        }
        return new Color(200, 200, 200);
    }
    public static Color colorForSpeed(int kmh) {
        if (kmh <= 0) return new Color(190, 190, 190);
        if (kmh < 30) return new Color(144, 202, 249);
        if (kmh < 50) return new Color(100, 181, 246);
        if (kmh < 70) return new Color(255, 213, 79);
        if (kmh < 90) return new Color(255, 152, 0);
        return new Color(229, 57, 53);
    }
    public static float strokeWidthFor(StreetSegment seg, float baseZoom) {
        String h = seg.getHighway().toLowerCase();
        float w = 1.0f;
        if (h.contains("motorway")) w = 3.5f;
        else if (h.contains("trunk")) w = 3.0f;
        else if (h.contains("primary")) w = 2.5f;
        else if (h.contains("secondary")) w = 2.0f;
        else if (h.contains("tertiary")) w = 1.6f;
        else if (h.contains("footway") || h.contains("path") || h.contains("cycleway")) w = 0.8f;
        else w = 1.2f;
        return Math.max(0.4f, w * baseZoom);
    }
    public static List<Map.Entry<String, Color>> legendEntries(MapColorMode mode) {
        return switch (mode) {
            case HIGHWAY -> HIGHWAY_COLORS.entrySet().stream()
                    .filter(e -> !e.getKey().endsWith("_link")
                            || List.of("motorway_link", "primary_link").contains(e.getKey()))
                    .limit(14)
                    .map(e -> Map.entry(prettyLabel(e.getKey()), e.getValue()))
                    .toList();
            case SPEED -> List.of(
                    Map.entry("No limit tagged", colorForSpeed(0)),
                    Map.entry("< 30 km/h", colorForSpeed(20)),
                    Map.entry("30–50 km/h", colorForSpeed(40)),
                    Map.entry("50–70 km/h", colorForSpeed(60)),
                    Map.entry("70–90 km/h", colorForSpeed(80)),
                    Map.entry("90+ km/h", colorForSpeed(100))
            );
            case ONEWAY -> List.of(
                    Map.entry("Two-way", new Color(120, 120, 120)),
                    Map.entry("Oneway", new Color(66, 133, 244))
            );
            case INFRASTRUCTURE -> List.of(
                    Map.entry("Bridge", new Color(180, 60, 200)),
                    Map.entry("Tunnel", new Color(80, 80, 120)),
                    Map.entry("Regular road", colorForHighway("residential"))
            );
        };
    }
    private static String prettyLabel(String highway) {
        return highway.replace('_', ' ');
    }
}
