package org.example.view;

import org.example.model.MapColorMode;
import org.example.model.StreetNetworkMapData;
import org.example.theme.UiStyles;
import org.example.util.MapSegmentSelector;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// Tab content: file picker, color mode, and interactive street map
public class GraphMapTabPanel extends JPanel {
    private final JComboBox<String> fileCombo = new JComboBox<>();
    private final JComboBox<MapColorMode> colorModeCombo =
            new JComboBox<>(MapColorMode.values());
    private final JCheckBox chkSecondaryStreets = new JCheckBox("Secondary & local streets", false);
    private final StreetNetworkMapPanel mapPanel = new StreetNetworkMapPanel();
    private final JLabel statusLabel = new JLabel("Select a processed network to visualise.");
    private final List<File> fileEntries = new ArrayList<>();
    private Consumer<File> loadListener;
    public GraphMapTabPanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(UiStyles.BG);
        setBorder(new EmptyBorder(8, 8, 8, 8));
        buildUi();
    }
    public void setLoadListener(Consumer<File> listener) {
        this.loadListener = listener;
    }
    public void setAvailableFiles(List<File> files) {
        fileEntries.clear();
        fileCombo.removeAllItems();
        if (files != null) {
            for (File f : files) {
                fileEntries.add(f);
                fileCombo.addItem(f.getName());
            }
        }
        if (fileCombo.getItemCount() > 0) {
            fileCombo.setSelectedIndex(0);
        }
        statusLabel.setText(fileEntries.isEmpty()
                ? "Process GraphML files first, or browse a folder in the Files tab."
                : fileEntries.size() + " file(s) available; select one and click Load Map.");
    }
    public void selectFileByPath(String absolutePath) {
        if (absolutePath == null) return;
        String name = new File(absolutePath).getName();
        for (int i = 0; i < fileEntries.size(); i++) {
            if (fileEntries.get(i).getName().equals(name)
                    || fileEntries.get(i).getAbsolutePath().equals(absolutePath)) {
                fileCombo.setSelectedIndex(i);
                return;
            }
        }
    }
    public void displayMap(StreetNetworkMapData data) {
        mapPanel.setMapDataAsync(data, () -> {
            if (data != null && !data.isEmpty()) {
                int shown = Math.min(data.getSegments().size(), MapSegmentSelector.MAX_DISPLAY_SEGMENTS);
                boolean subsampled = data.getSegments().size() > shown;
                String subsampleNote = subsampled
                        ? (mapPanel.isShowSecondaryAndLocalStreets()
                                ? " · secondary/local streets included"
                                : " · major roads only (toggle secondary streets for more)")
                        : "";
                statusLabel.setText(String.format(
                        "%s: %,d segments%s · %,d nodes · bbox %.1f × %.1f km",
                        data.getGraphName(),
                        data.getSegments().size(),
                        subsampleNote,
                        data.getNodeCount(),
                        data.getLatSpan() * 111.0,
                        data.getLonSpan() * 111.0 * Math.cos(Math.toRadians(
                                (data.getMinLat() + data.getMaxLat()) / 2.0))));
            }
            setLoading(false);
        });
    }
    public void setLoading(boolean loading) {
        fileCombo.setEnabled(!loading);
        colorModeCombo.setEnabled(!loading);
        chkSecondaryStreets.setEnabled(!loading);
        if (loading) {
            statusLabel.setText("Loading map data…");
        }
    }
    public void setError(String message) {
        statusLabel.setText(message);
    }
    private void buildUi() {
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        toolbar.setOpaque(false);
        fileCombo.setPreferredSize(new Dimension(280, 28));
        colorModeCombo.setSelectedItem(MapColorMode.HIGHWAY);
        colorModeCombo.addActionListener(e ->
                mapPanel.setColorMode((MapColorMode) colorModeCombo.getSelectedItem()));
        chkSecondaryStreets.setToolTipText(
                "Show all OSM secondary roads plus neighbourhood streets "
                        + "(residential, service, unclassified). May be slower on large cities.");
        chkSecondaryStreets.setOpaque(false);
        chkSecondaryStreets.addActionListener(e ->
                mapPanel.setShowSecondaryAndLocalStreets(chkSecondaryStreets.isSelected()));
        JButton btnLoad = UiStyles.accentButton("Load Map");
        btnLoad.addActionListener(e -> requestLoad());
        JButton btnReset = UiStyles.secondaryButton("Reset View");
        btnReset.addActionListener(e -> mapPanel.resetView());
        toolbar.add(new JLabel("Network:"));
        toolbar.add(fileCombo);
        toolbar.add(new JLabel("Colour by:"));
        toolbar.add(colorModeCombo);
        toolbar.add(chkSecondaryStreets);
        toolbar.add(btnLoad);
        toolbar.add(btnReset);
        statusLabel.setFont(UiStyles.FONT_SMALL);
        statusLabel.setForeground(UiStyles.MUTED);
        statusLabel.setBorder(new EmptyBorder(4, 4, 0, 4));
        mapPanel.getInfoLabel().setBorder(BorderFactory.createMatteBorder(
                1, 0, 0, 0, UiStyles.BORDER));
        JPanel mapColumn = new JPanel(new BorderLayout());
        mapColumn.setOpaque(false);
        mapColumn.add(mapPanel, BorderLayout.CENTER);
        mapColumn.add(mapPanel.getInfoLabel(), BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                mapColumn, mapPanel.getLegendPanel());
        split.setResizeWeight(0.82);
        split.setDividerLocation(820);
        split.setBorder(null);
        JPanel north = new JPanel(new BorderLayout());
        north.setOpaque(false);
        north.add(toolbar, BorderLayout.NORTH);
        north.add(statusLabel, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        add(split, BorderLayout.CENTER);
    }
    private void requestLoad() {
        int idx = fileCombo.getSelectedIndex();
        if (idx < 0 || idx >= fileEntries.size()) {
            statusLabel.setText("No file selected.");
            return;
        }
        if (loadListener != null) {
            loadListener.accept(fileEntries.get(idx));
        }
    }
}