package org.example.util;

import org.example.model.MapDisplayOptions;
import org.example.model.StreetSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

// Picks a drawable subset of street segments so maps stay responsive on large cities.
public final class MapSegmentSelector {
    public static final int MAX_DISPLAY_SEGMENTS = 12_000;
    private MapSegmentSelector() {}
    public static List<StreetSegment> selectForDisplay(List<StreetSegment> all) {
        return selectForDisplay(all, MAX_DISPLAY_SEGMENTS, new MapDisplayOptions());
    }
    public static List<StreetSegment> selectForDisplay(List<StreetSegment> all, MapDisplayOptions options) {
        return selectForDisplay(all, MAX_DISPLAY_SEGMENTS, options);
    }
    public static List<StreetSegment> selectForDisplay(List<StreetSegment> all, int max, MapDisplayOptions options) {
        if (all == null || all.isEmpty()) {
            return List.of();
        }
        MapDisplayOptions opts = options != null ? options : new MapDisplayOptions();

        if (opts.isShowSecondaryAndLocalStreets()) {
            return selectWithSecondaryAndLocal(all, max);
        }
        return selectMajorRoadsFirst(all, max);
    }
    public static List<StreetSegment> selectForDisplay(List<StreetSegment> all, int max) {
        return selectForDisplay(all, max, new MapDisplayOptions());
    }
    private static List<StreetSegment> selectMajorRoadsFirst(List<StreetSegment> all, int max) {
        if (all.size() <= max) {
            return List.copyOf(all);
        }
        List<StreetSegment> priority = new ArrayList<>();
        List<StreetSegment> minor = new ArrayList<>();
        for (StreetSegment seg : all) {
            if (seg.isBridge() || seg.isTunnel() || priorityScore(seg.getHighway()) >= 60) {
                priority.add(seg);
            } else {
                minor.add(seg);
            }
        }
        List<StreetSegment> result = new ArrayList<>(max);
        if (priority.size() >= max) {
            priority.sort(Comparator.comparingInt((StreetSegment s) -> priorityScore(s.getHighway())).reversed());
            for (int i = 0; i < max; i++) {
                result.add(priority.get(i));
            }
            return result;
        }
        result.addAll(priority);
        int remaining = max - result.size();
        minor.sort(Comparator.comparingInt((StreetSegment s) -> priorityScore(s.getHighway())).reversed());
        if (minor.size() <= remaining) {
            result.addAll(minor);
        } else {
            double step = (double) minor.size() / remaining;
            for (int i = 0; i < remaining; i++) {
                result.add(minor.get((int) (i * step)));
            }
        }
        return result;
    }
    private static List<StreetSegment> selectWithSecondaryAndLocal(List<StreetSegment> all, int max) {
        if (all.size() <= max) {
            return List.copyOf(all);
        }
        List<StreetSegment> pinned = new ArrayList<>();
        List<StreetSegment> rest = new ArrayList<>();
        for (StreetSegment seg : all) {
            if (isSecondaryOrLocalStreet(seg.getHighway())) {
                pinned.add(seg);
            } else {
                rest.add(seg);
            }
        }
        if (pinned.size() >= max) {
            return List.copyOf(pinned);
        }
        List<StreetSegment> result = new ArrayList<>(pinned);
        int remaining = max - pinned.size();
        if (!rest.isEmpty()) {
            result.addAll(selectMajorRoadsFirst(rest, remaining));
        }
        return result;
    }
    static boolean isSecondaryOrLocalStreet(String highway) {
        if (highway == null || highway.isBlank()) {
            return false;
        }
        return switch (highway.toLowerCase()) {
            case "secondary", "secondary_link",
                 "residential", "living_street", "service", "unclassified" -> true;
            default -> false;
        };
    }
    private static int priorityScore(String highway) {
        if (highway == null) return 15;
        return switch (highway.toLowerCase()) {
            case "motorway", "motorway_link" -> 100;
            case "trunk", "trunk_link" -> 95;
            case "primary", "primary_link" -> 90;
            case "secondary", "secondary_link" -> 80;
            case "tertiary", "tertiary_link" -> 70;
            case "cycleway", "footway", "pedestrian", "path" -> 55;
            case "living_street" -> 40;
            case "unclassified" -> 25;
            case "residential" -> 15;
            case "service" -> 10;
            default -> 30;
        };
    }
}