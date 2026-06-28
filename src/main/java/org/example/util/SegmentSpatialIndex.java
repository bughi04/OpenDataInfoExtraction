package org.example.util;

import org.example.model.StreetSegment;

import java.util.ArrayList;
import java.util.List;

// Uniform grid over lat/lon for fast nearest-segment lookup (hover tooltips).
public class SegmentSpatialIndex {
    private static final int GRID = 64;
    private final StreetSegment[] segments;
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final List<Integer>[] cells;
    @SuppressWarnings("unchecked")
    public SegmentSpatialIndex(List<StreetSegment> segments,
                               double minLat, double maxLat, double minLon, double maxLon) {
        this.segments = segments.toArray(new StreetSegment[0]);
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.cells = new List[GRID * GRID];
        for (int i = 0; i < segments.size(); i++) {
            StreetSegment s = segments.get(i);
            int cx = cellX((s.getLon1() + s.getLon2()) * 0.5);
            int cy = cellY((s.getLat1() + s.getLat2()) * 0.5);
            int idx = cy * GRID + cx;
            if (cells[idx] == null) {
                cells[idx] = new ArrayList<>(4);
            }
            cells[idx].add(i);
        }
    }
    public StreetSegment findNearestScreen(
            double screenX, double screenY,
            ScreenProjector projector, double maxDistPx) {

        double lon = 0;
        double lat = 0;
        try {
            double[] world = projector.screenToWorld(screenX, screenY);
            lon = world[0];
            lat = world[1];
        } catch (Exception e) {
            return null;
        }
        int cx = cellX(lon);
        int cy = cellY(lat);
        StreetSegment best = null;
        double bestDist = maxDistPx;
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int gx = cx + dx;
                int gy = cy + dy;
                if (gx < 0 || gx >= GRID || gy < 0 || gy >= GRID) continue;
                List<Integer> bucket = cells[gy * GRID + gx];
                if (bucket == null) continue;
                for (int si : bucket) {
                    StreetSegment seg = segments[si];
                    double d = projector.segmentScreenDistance(seg, screenX, screenY);
                    if (d < bestDist) {
                        bestDist = d;
                        best = seg;
                    }
                }
            }
        }
        return best;
    }
    private int cellX(double lon) {
        double span = Math.max(maxLon - minLon, 1e-9);
        int c = (int) ((lon - minLon) / span * GRID);
        return Math.clamp(c, 0, GRID - 1);
    }
    private int cellY(double lat) {
        double span = Math.max(maxLat - minLat, 1e-9);
        int c = (int) ((lat - minLat) / span * GRID);
        return Math.clamp(c, 0, GRID - 1);
    }
    public interface ScreenProjector {
        double[] screenToWorld(double sx, double sy);
        double segmentScreenDistance(StreetSegment seg, double sx, double sy);
    }
}
