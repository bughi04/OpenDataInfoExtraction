package org.example.view;

import org.example.model.NetworkMetrics;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.example.theme.ReadableTextKit;
import org.example.theme.UiStyles;
import org.example.util.WorldBankAnalysisUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

// Tab panel showing World Bank Data360 country indicators for each processed city.
public class WorldBankDataPanel extends JPanel {
    private final JComboBox<String> citySelector = new JComboBox<>();
    private final JLabel statusLabel = new JLabel(" ");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[]{"Indicator", "Value", "Year", "Unit"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable indicatorTable = new JTable(tableModel);
    private final JTextPane analysisPane = ReadableTextKit.createReadingPane();
    private List<NetworkMetrics> currentMetrics = List.of();
    private Runnable refreshAction;
    public WorldBankDataPanel() {
        setLayout(new BorderLayout(12, 12));
        setBackground(UiStyles.BG);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        JLabel heading = UiStyles.sectionHeading("World Bank Country Data");
        JPanel north = new JPanel(new BorderLayout(8, 8));
        north.setOpaque(false);
        north.add(heading, BorderLayout.NORTH);
        JLabel intro = new JLabel(
                "<html>Indicators from the World Bank Data360 API (WDI). Parsed from each GraphML filename "
                        + "(City, Country). Values are <b>country-level</b>. "
                        + "Use <b>Refresh data</b> to reload after indicator updates.</html>");
        intro.setFont(UiStyles.FONT_UI);
        intro.setForeground(UiStyles.MUTED);
        north.add(intro, BorderLayout.CENTER);
        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        toolbar.setOpaque(false);
        toolbar.add(new JLabel("City:"));
        citySelector.setFont(UiStyles.FONT_UI);
        citySelector.addActionListener(e -> showSelectedCity());
        toolbar.add(citySelector);
        JButton refreshBtn = UiStyles.secondaryButton("Refresh data");
        refreshBtn.setToolTipText("Re-fetch World Bank indicators from the API");
        refreshBtn.addActionListener(e -> {
            if (refreshAction != null) refreshAction.run();
        });
        toolbar.add(refreshBtn);
        north.add(toolbar, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH);
        indicatorTable.setFont(ReadableTextKit.FONT_MONO);
        indicatorTable.setRowHeight(24);
        indicatorTable.getTableHeader().setFont(UiStyles.FONT_UI_BOLD);
        JScrollPane tableScroll = new JScrollPane(indicatorTable);
        tableScroll.setBorder(UiStyles.titledBorder("Indicators"));
        analysisPane.setBorder(UiStyles.titledBorder("Analysis"));
        JScrollPane analysisScroll = new JScrollPane(analysisPane);
        analysisScroll.setPreferredSize(new Dimension(0, 220));
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScroll, analysisScroll);
        split.setResizeWeight(0.55);
        split.setBorder(null);
        split.setOpaque(false);
        add(split, BorderLayout.CENTER);
        statusLabel.setFont(UiStyles.FONT_UI);
        statusLabel.setForeground(UiStyles.MUTED);
        statusLabel.setBorder(new EmptyBorder(8, 0, 0, 0));
        add(statusLabel, BorderLayout.SOUTH);
    }
    public void setRefreshAction(Runnable action) {
        this.refreshAction = action;
    }
    public void updateMetrics(List<NetworkMetrics> metrics) {
        this.currentMetrics = metrics != null ? new ArrayList<>(metrics) : List.of();
        String selected = (String) citySelector.getSelectedItem();
        citySelector.removeAllItems();
        for (NetworkMetrics m : currentMetrics) {
            citySelector.addItem(m.getGraphName());
        }
        if (selected != null) {
            for (int i = 0; i < citySelector.getItemCount(); i++) {
                if (selected.equals(citySelector.getItemAt(i))) {
                    citySelector.setSelectedIndex(i);
                    break;
                }
            }
        }
        if (citySelector.getItemCount() > 0 && citySelector.getSelectedIndex() < 0) {
            citySelector.setSelectedIndex(0);
        }
        showSelectedCity();
        statusLabel.setText(WorldBankAnalysisUtil.generateMultiCitySummary(currentMetrics).replace("\n", " | "));
    }
    private void showSelectedCity() {
        tableModel.setRowCount(0);
        ReadableTextKit.setPlainText(analysisPane, "Select a city to view World Bank indicators.", "body");
        int idx = citySelector.getSelectedIndex();
        if (idx < 0 || idx >= currentMetrics.size()) return;
        NetworkMetrics m = currentMetrics.get(idx);
        WorldBankCityData wb = m.getWorldBankData();
        if (wb == null) {
            ReadableTextKit.setPlainText(analysisPane,
                    "World Bank data not loaded yet for " + m.getGraphName() + ".\n"
                            + "Process the file while online, or press Refresh data.", "body");
            return;
        }
        for (WorldBankIndicatorValue ind : wb.getIndicatorsView()) {
            tableModel.addRow(new Object[]{
                    ind.getLabel() != null ? ind.getLabel() : ind.getIndicatorId(),
                    ind.hasValue() ? ind.formattedValue() : "N/A",
                    ind.getYear() != null ? ind.getYear() : "",
                    ind.getUnit() != null ? ind.getUnit() : ""
            });
        }
        String narrative = WorldBankAnalysisUtil.generateCityNarrative(m, currentMetrics);
        ReadableTextKit.setPlainText(analysisPane, narrative, "body");
        if (wb.getStatusMessage() != null) {
            statusLabel.setText(wb.getStatusMessage());
        }
    }
}