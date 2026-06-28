package org.example.util;

import org.example.model.StreetSegment;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Tests nearest-segment lookup on the map spatial grid, including screen-space projection and hover-radius behavior.
class SegmentSpatialIndexTest {
    @Test
    void findNearestScreenReturnsClosestSegmentWithinRadius() {
        StreetSegment near = new StreetSegment(52.0, 4.0, 52.01, 4.01, "residential", "Near", false, 30, false, false);
        StreetSegment far = new StreetSegment(53.0, 5.0, 53.01, 5.01, "primary", "Far", true, 50, false, false);
        List<StreetSegment> segments = List.of(near, far);
        SegmentSpatialIndex index = new SegmentSpatialIndex(segments, 51.5, 53.5, 3.5, 5.5);
        SegmentSpatialIndex.ScreenProjector projector = new SegmentSpatialIndex.ScreenProjector() {
            @Override
            public double[] screenToWorld(double sx, double sy) {
                return new double[] {sx / 100.0, sy / 100.0};
            }
            @Override
            public double segmentScreenDistance(StreetSegment seg, double sx, double sy) {
                double midLon = (seg.getLon1() + seg.getLon2()) * 0.5;
                double midLat = (seg.getLat1() + seg.getLat2()) * 0.5;
                double wx = midLon * 100.0;
                double wy = midLat * 100.0;
                return Math.hypot(wx - sx, wy - sy);
            }
        };
        double nearMidLon = (near.getLon1() + near.getLon2()) * 0.5;
        double nearMidLat = (near.getLat1() + near.getLat2()) * 0.5;
        double screenX = nearMidLon * 100.0;
        double screenY = nearMidLat * 100.0;
        StreetSegment hit = index.findNearestScreen(screenX, screenY, projector, 50.0);
        assertNotNull(hit);
        assertEquals("Near", hit.getName());
        StreetSegment miss = index.findNearestScreen(9999, 9999, projector, 10.0);
        assertNull(miss);
    }
    @Test
    void findNearestScreenReturnsNullWhenProjectionFails() {
        StreetSegment seg = new StreetSegment(52.0, 4.0, 52.01, 4.01, "residential", "A", false, 0, false, false);
        SegmentSpatialIndex index = new SegmentSpatialIndex(List.of(seg), 52.0, 52.01, 4.0, 4.01);
        SegmentSpatialIndex.ScreenProjector failing = new SegmentSpatialIndex.ScreenProjector() {
            @Override
            public double[] screenToWorld(double sx, double sy) {
                throw new IllegalStateException("bad transform");
            }
            @Override
            public double segmentScreenDistance(StreetSegment seg, double sx, double sy) {
                return Double.MAX_VALUE;
            }
        };
        assertNull(index.findNearestScreen(10, 10, failing, 100));
    }
}