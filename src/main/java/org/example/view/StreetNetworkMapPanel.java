package org.example.view;

import org.example.model.MapColorMode;
import org.example.model.MapDisplayOptions;
import org.example.model.StreetNetworkMapData;
import org.example.model.StreetSegment;
import org.example.theme.UiStyles;
import org.example.util.HighwayColorScheme;
import org.example.util.MapSegmentSelector;
import org.example.util.SegmentSpatialIndex;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

// Interactive street-network map: renders to an off-screen cache in screen pixels.
public class StreetNetworkMapPanel extends JPanel {
    private static final Color MAP_BACKGROUND = new Color(220, 224, 232);
    private static final int PADDING = 24;
    private static final int HOVER_THROTTLE_MS = 100;
    private static final int CACHE_DEBOUNCE_MS = 150;
    private StreetNetworkMapData mapData;
    private List<StreetSegment> displaySegments = List.of();
    private SegmentSpatialIndex spatialIndex;
    private int totalSegmentCount;
    private boolean displaySubsampled;
    private MapColorMode colorMode = MapColorMode.HIGHWAY;
    private final MapDisplayOptions displayOptions = new MapDisplayOptions();
    private double zoom = 1.0;
    private double panX;
    private double panY;
    private Point dragStart;
    private StreetSegment hovered;
    private volatile BufferedImage cacheImage;
    private volatile double cacheZoom;
    private volatile double cachePanX;
    private volatile double cachePanY;
    private volatile int cacheWidth;
    private volatile int cacheHeight;
    private volatile MapColorMode cacheColorMode;
    private final AtomicInteger renderGeneration = new AtomicInteger();
    private final AtomicBoolean rebuildPending = new AtomicBoolean();
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "map-render");
        t.setDaemon(true);
        return t;
    });
    private Timer cacheDebounceTimer;
    private Timer hoverTimer;
    private Point pendingHoverPoint;
    private final JLabel infoLabel = new JLabel(" ");
    private final JPanel legendPanel = new JPanel();
    public StreetNetworkMapPanel() {
        setBackground(MAP_BACKGROUND);
        setPreferredSize(new Dimension(900, 600));
        setOpaque(true);
        setFocusable(true);
        infoLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        infoLabel.setForeground(new Color(60, 60, 60));
        infoLabel.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        legendPanel.setLayout(new BoxLayout(legendPanel, BoxLayout.Y_AXIS));
        legendPanel.setBackground(UiStyles.PANEL);
        legendPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, UiStyles.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        cacheDebounceTimer = new Timer(CACHE_DEBOUNCE_MS, e -> {
            cacheDebounceTimer.stop();
            scheduleCacheRebuild();
        });
        cacheDebounceTimer.setRepeats(false);
        hoverTimer = new Timer(HOVER_THROTTLE_MS, e -> {
            hoverTimer.stop();
            if (pendingHoverPoint != null) {
                updateHoverNow(pendingHoverPoint);
            }
        });
        hoverTimer.setRepeats(false);
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragStart = e.getPoint();
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragStart != null) {
                    dragStart = null;
                    requestCacheRebuildDebounced();
                }
            }
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragStart != null) {
                    panX += e.getX() - dragStart.x;
                    panY += e.getY() - dragStart.y;
                    dragStart = e.getPoint();
                    repaint();
                }
            }
            @Override
            public void mouseMoved(MouseEvent e) {
                pendingHoverPoint = e.getPoint();
                if (!hoverTimer.isRunning()) {
                    hoverTimer.start();
                }
            }
            @Override
            public void mouseExited(MouseEvent e) {
                pendingHoverPoint = null;
                hovered = null;
                infoLabel.setText(" ");
                repaint();
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
        addMouseWheelListener(e -> {
            double factor = e.getWheelRotation() < 0 ? 1.12 : 1 / 1.12;
            Point p = e.getPoint();
            panX = p.x - (p.x - panX) * factor;
            panY = p.y - (p.y - panY) * factor;
            zoom = Math.max(0.15, Math.min(40, zoom * factor));
            repaint();
            requestCacheRebuildDebounced();
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                requestCacheRebuildDebounced();
            }
            @Override
            public void componentShown(ComponentEvent e) {
                requestCacheRebuildDebounced();
            }
        });
    }
    public JLabel getInfoLabel() { return infoLabel; }
    public JPanel getLegendPanel() { return legendPanel; }
    public void setMapDataAsync(StreetNetworkMapData data, Runnable onReady) {
        renderGeneration.incrementAndGet();
        mapData = data;
        totalSegmentCount = data != null ? data.getSegments().size() : 0;
        cacheImage = null;
        hovered = null;
        zoom = 1.0;
        panX = 0;
        panY = 0;
        infoLabel.setText("Preparing map…");
        if (data == null || data.isEmpty()) {
            displaySegments = List.of();
            spatialIndex = null;
            displaySubsampled = false;
            rebuildLegend();
            repaint();
            if (onReady != null) onReady.run();
            return;
        }
        int gen = renderGeneration.incrementAndGet();
        renderExecutor.submit(() -> {
            List<StreetSegment> selected = MapSegmentSelector.selectForDisplay(
                    data.getSegments(), displayOptions);
            applyDisplaySelection(gen, data, selected, onReady);
        });
    }
    public void setShowSecondaryAndLocalStreets(boolean show) {
        if (displayOptions.isShowSecondaryAndLocalStreets() == show) {
            return;
        }
        displayOptions.setShowSecondaryAndLocalStreets(show);
        if (mapData == null || mapData.isEmpty()) {
            return;
        }
        infoLabel.setText("Updating map…");
        int gen = renderGeneration.incrementAndGet();
        renderExecutor.submit(() -> {
            List<StreetSegment> selected = MapSegmentSelector.selectForDisplay(
                    mapData.getSegments(), displayOptions);
            applyDisplaySelection(gen, mapData, selected, null);
        });
    }
    public boolean isShowSecondaryAndLocalStreets() {
        return displayOptions.isShowSecondaryAndLocalStreets();
    }
    private void applyDisplaySelection(int gen, StreetNetworkMapData data,
                                       List<StreetSegment> selected, Runnable onReady) {
        SegmentSpatialIndex index = new SegmentSpatialIndex(
                selected, data.getMinLat(), data.getMaxLat(), data.getMinLon(), data.getMaxLon());
        SwingUtilities.invokeLater(() -> {
            if (gen != renderGeneration.get()) return;
            displaySegments = selected;
            spatialIndex = index;
            totalSegmentCount = data.getSegments().size();
            displaySubsampled = selected.size() < totalSegmentCount;
            rebuildLegend();
            updateDisplayInfoText();
            cacheImage = null;
            requestCacheRebuildAfterLayout();
            if (onReady != null) onReady.run();
        });
    }
    private void updateDisplayInfoText() {
        if (mapData == null || mapData.isEmpty()) {
            infoLabel.setText(" ");
            return;
        }
        if (displaySubsampled) {
            String note = displayOptions.isShowSecondaryAndLocalStreets()
                    ? "secondary & local streets included; other types subsampled"
                    : "major roads prioritized";
            infoLabel.setText(String.format("Showing %,d of %,d segments (%s)",
                    displaySegments.size(), totalSegmentCount, note));
        } else {
            infoLabel.setText(" ");
        }
    }
    public void setColorMode(MapColorMode mode) {
        this.colorMode = mode != null ? mode : MapColorMode.HIGHWAY;
        rebuildLegend();
        scheduleCacheRebuild();
    }
    public MapColorMode getColorMode() { return colorMode; }
    public void resetView() {
        zoom = 1.0;
        panX = 0;
        panY = 0;
        hovered = null;
        scheduleCacheRebuild();
    }
    private void requestCacheRebuildAfterLayout() {
        scheduleCacheRebuild();
        SwingUtilities.invokeLater(this::scheduleCacheRebuild);
    }
    private void requestCacheRebuildDebounced() {
        cacheDebounceTimer.restart();
    }
    private void scheduleCacheRebuild() {
        if (mapData == null || displaySegments.isEmpty()) {
            cacheImage = null;
            repaint();
            return;
        }
        int w = getWidth();
        int h = getHeight();
        if (w < 50 || h < 50) {
            rebuildPending.set(true);
            return;
        }
        final int gen = renderGeneration.incrementAndGet();
        final double z = zoom;
        final double px = panX;
        final double py = panY;
        final MapColorMode mode = colorMode;
        final List<StreetSegment> segments = displaySegments;
        final StreetNetworkMapData data = mapData;
        renderExecutor.submit(() -> {
            BufferedImage img = renderCache(data, segments, w, h, z, px, py, mode);
            SwingUtilities.invokeLater(() -> {
                if (gen != renderGeneration.get()) return;
                cacheImage = img;
                cacheZoom = z;
                cachePanX = px;
                cachePanY = py;
                cacheWidth = w;
                cacheHeight = h;
                cacheColorMode = mode;
                rebuildPending.set(false);
                repaint();
            });
        });
    }
    private static BufferedImage renderCache(
            StreetNetworkMapData data, List<StreetSegment> segments,
            int w, int h, double zoom, double panX, double panY, MapColorMode mode) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(MAP_BACKGROUND);
        g2.fillRect(0, 0, w, h);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        AffineTransform worldToScreen = buildTransform(data, w, h, zoom, panX, panY);
        float lineZoom = (float) Math.sqrt(Math.max(0.2, zoom));
        Point2D p1 = new Point2D.Double();
        Point2D p2 = new Point2D.Double();
        for (StreetSegment seg : segments) {
            worldToScreen.transform(new Point2D.Double(seg.getLon1(), seg.getLat1()), p1);
            worldToScreen.transform(new Point2D.Double(seg.getLon2(), seg.getLat2()), p2);
            drawScreenSegment(g2, p1.getX(), p1.getY(), p2.getX(), p2.getY(), seg, lineZoom, mode, false);
        }
        g2.dispose();
        return img;
    }
    private static void drawScreenSegment(
            Graphics2D g2, double x1, double y1, double x2, double y2,
            StreetSegment seg, float lineZoom, MapColorMode mode, boolean highlight) {
        Color color = HighwayColorScheme.colorFor(seg, mode);
        if (isLightRoadColor(color)) {
            color = new Color(160, 160, 165);
        }
        float width = HighwayColorScheme.strokeWidthFor(seg, lineZoom) + (highlight ? 1.5f : 0f);
        if (highlight) {
            g2.setStroke(new BasicStroke(width + 2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(30, 30, 30, 100));
            g2.draw(new Line2D.Double(x1, y1, x2, y2));
        }
        g2.setStroke(new BasicStroke(Math.max(0.6f, width), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(highlight ? color.brighter() : color);
        g2.draw(new Line2D.Double(x1, y1, x2, y2));
    }
    private static boolean isLightRoadColor(Color c) {
        return c.getRed() > 250 && c.getGreen() > 250 && c.getBlue() > 250;
    }
    private void rebuildLegend() {
        legendPanel.removeAll();
        JLabel title = new JLabel("Legend: " + colorMode.getLabel());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 11f));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        legendPanel.add(title);
        legendPanel.add(Box.createVerticalStrut(6));
        if (displaySubsampled) {
            String note = displayOptions.isShowSecondaryAndLocalStreets()
                    ? "secondary &amp; local streets kept; other types subsampled"
                    : "major roads prioritized; enable toggle below for secondary streets";
            JLabel noteLbl = new JLabel("<html><span style='color:#666'>" + note + "</span></html>");
            noteLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            noteLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
            legendPanel.add(noteLbl);
            legendPanel.add(Box.createVerticalStrut(4));
        }
        for (Map.Entry<String, Color> entry : HighwayColorScheme.legendEntries(colorMode)) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            JPanel swatch = new JPanel();
            swatch.setPreferredSize(new Dimension(18, 10));
            Color swatchColor = entry.getValue();
            if (isLightRoadColor(swatchColor)) {
                swatchColor = new Color(160, 160, 165);
            }
            swatch.setBackground(swatchColor);
            swatch.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
            row.add(swatch);
            JLabel lbl = new JLabel(entry.getKey());
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            row.add(lbl);
            legendPanel.add(row);
        }
        legendPanel.revalidate();
        legendPanel.repaint();
    }
    private void updateHoverNow(Point screen) {
        if (spatialIndex == null || mapData == null) return;
        ViewProjector projector = new ViewProjector(getWidth(), getHeight());
        StreetSegment nearest = spatialIndex.findNearestScreen(
                screen.x, screen.y, projector, 10.0);
        if (nearest != hovered) {
            hovered = nearest;
            infoLabel.setText(hovered != null ? hovered.describe() : " ");
            repaint();
        }
    }
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int w = getWidth();
        int h = getHeight();
        if (mapData == null || mapData.isEmpty()) {
            drawPlaceholder(g, w, h);
            return;
        }
        if (w < 50 || h < 50) {
            return;
        }
        if (rebuildPending.get() && displaySegments != null && !displaySegments.isEmpty()) {
            rebuildPending.set(false);
            scheduleCacheRebuild();
        }
        boolean cacheStale = cacheImage == null
                || cacheWidth != w || cacheHeight != h
                || cacheColorMode != colorMode
                || Math.abs(cacheZoom - zoom) > 1e-6
                || Math.abs(cachePanX - panX) > 0.5
                || Math.abs(cachePanY - panY) > 0.5;
        if (cacheStale && cacheImage == null) {
            drawPlaceholder(g, w, h, "Rendering map…");
            if (!rebuildPending.getAndSet(true)) {
                scheduleCacheRebuild();
            }
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (cacheImage != null) {
            int dx = (int) (panX - cachePanX);
            int dy = (int) (panY - cachePanY);
            if (cacheWidth == w && cacheHeight == h) {
                g2.drawImage(cacheImage, dx, dy, null);
            } else {
                g2.drawImage(cacheImage, dx, dy, w, h, null);
            }
        }
        if (cacheStale && !rebuildPending.getAndSet(true)) {
            scheduleCacheRebuild();
        }
        if (hovered != null) {
            AffineTransform worldToScreen = buildTransform(mapData, w, h, zoom, panX, panY);
            Point2D a = worldToScreen.transform(
                    new Point2D.Double(hovered.getLon1(), hovered.getLat1()), null);
            Point2D b = worldToScreen.transform(
                    new Point2D.Double(hovered.getLon2(), hovered.getLat2()), null);
            float lineZoom = (float) Math.sqrt(Math.max(0.2, zoom));
            drawScreenSegment(g2, a.getX(), a.getY(), b.getX(), b.getY(),
                    hovered, lineZoom * 1.2f, colorMode, true);
        }
        g2.dispose();
    }
    private void drawPlaceholder(Graphics g, int w, int h) {
        drawPlaceholder(g, w, h, mapData == null
                ? "Select a GraphML file and click Load Map"
                : "No drawable segments (missing node coordinates)");
    }
    private void drawPlaceholder(Graphics g, int w, int h, String msg) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(80, 80, 90));
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(msg, Math.max(8, (w - fm.stringWidth(msg)) / 2), h / 2);
    }
    private static AffineTransform buildTransform(
            StreetNetworkMapData data, int width, int height,
            double zoom, double panX, double panY) {
        double latSpan = Math.max(data.getLatSpan(), 1e-6);
        double lonSpan = Math.max(data.getLonSpan(), 1e-6);
        double availW = width - PADDING * 2.0;
        double availH = height - PADDING * 2.0;
        double scale = Math.min(availW / lonSpan, availH / latSpan) * zoom;
        double cx = (data.getMinLon() + data.getMaxLon()) / 2.0;
        double cy = (data.getMinLat() + data.getMaxLat()) / 2.0;
        AffineTransform tx = new AffineTransform();
        tx.translate(width / 2.0 + panX, height / 2.0 + panY);
        tx.scale(scale, -scale);
        tx.translate(-cx, -cy);
        return tx;
    }
    private class ViewProjector implements SegmentSpatialIndex.ScreenProjector {
        private final AffineTransform worldToScreen;
        private final AffineTransform screenToWorld;
        ViewProjector(int w, int h) {
            worldToScreen = buildTransform(mapData, w, h, zoom, panX, panY);
            screenToWorld = new AffineTransform(worldToScreen);
            try {
                screenToWorld.invert();
            } catch (NoninvertibleTransformException e) {
                screenToWorld.setToIdentity();
            }
        }
        @Override
        public double[] screenToWorld(double sx, double sy) {
            Point2D p = screenToWorld.transform(new Point2D.Double(sx, sy), null);
            return new double[]{p.getX(), p.getY()};
        }
        @Override
        public double segmentScreenDistance(StreetSegment seg, double sx, double sy) {
            Point2D p1 = worldToScreen.transform(new Point2D.Double(seg.getLon1(), seg.getLat1()), null);
            Point2D p2 = worldToScreen.transform(new Point2D.Double(seg.getLon2(), seg.getLat2()), null);
            return Line2D.ptSegDist(p1.getX(), p1.getY(), p2.getX(), p2.getY(), sx, sy);
        }
    }
}