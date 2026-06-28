package org.example.view;

import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.theme.ReadableTextKit;
import org.example.theme.UiStyles;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.*;
import java.util.List;
/*
Main application view: clean, single-theme Swing UI.
*/
public class ModernNetworkView {
    private static final Color C_ACCENT    = UiStyles.ACCENT;
    private static final Color C_BG        = UiStyles.BG;
    private static final Color C_PANEL     = UiStyles.PANEL;
    private static final Color C_TEXT      = UiStyles.TEXT;
    private static final Color C_BORDER    = UiStyles.BORDER;
    private static final Color C_GOOD      = new Color(56, 142, 60);
    private static final Color C_BAD       = new Color(211, 47, 47);
    private static final Color C_PENDING   = new Color(245, 124, 0);
    private static final Color C_AVG_ROW   = new Color(227, 242, 253);
    private JFrame frame;
    private JButton btnGenerateCharts;
    private JMenuItem miExport;
    private JMenuItem miExit;
    private JMenuItem miAbout;
    private JMenuItem miGenerateCharts;
    private JMenuItem miGenerateAnalysis;
    private JTabbedPane tabbedPane;
    private JLabel lblTotalGraphs;
    private JLabel lblTotalRoadKm, lblAvgOneway, lblAvgSpeed, lblAvgLanes;
    private JLabel lblResidentialPct, lblCarOrientedPct, lblPedestrianPct, lblCyclingPct;
    private JLabel lblNamedRoadsPct, lblInfraCount, lblStreetsPerNode;
    private DefaultTableModel dashboardTableModel;
    private JTable            dashboardTable;
    private JTextField  txtFolderPath;
    private JPanel      fileCheckListPanel;   // holds JCheckBoxes, one per .graphml file
    private JScrollPane fileCheckListScroll;
    private DefaultListModel<String> queueModel;
    private JList<String>            queueList;
    private JProgressBar             progressBar;
    private JLabel                   lblQueueStatus;
    private JRadioButton             rbQuickAnalysis;
    private JRadioButton             rbFullAnalysis;
    private NetworkChartDisplayPanel chartsPanel;
    private JTextPane analysisArea;
    private AiAdvisorPanel aiAdvisorPanel;
    private GraphMapTabPanel graphMapTabPanel;
    private WorldBankDataPanel worldBankDataPanel;
    private PredictionPanel predictionPanel;
    private DefaultTableModel resultsTableModel;
    private JTable            resultsTable;
    private JLabel statusLabel;
    private final List<ActionListener> loadFileListeners      = new ArrayList<>();
    private final List<ActionListener> loadDirectoryListeners = new ArrayList<>();
    private final List<ActionListener> processListeners          = new ArrayList<>();
    private final List<ActionListener> browseListeners           = new ArrayList<>();
    private final List<ActionListener> generateAnalysisListeners = new ArrayList<>();
    private final List<ActionListener> exportListeners           = new ArrayList<>();
    public ModernNetworkView() {
        buildFrame();
    }
    public void show()           { frame.setVisible(true); }
    public JFrame getFrame()     { return frame; }
    public void setStatusMessage(String msg) {
        SwingUtilities.invokeLater(() -> statusLabel.setText("  " + msg));
    }
    public void showErrorMessage(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }
    public void showInfoMessage(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Information", JOptionPane.INFORMATION_MESSAGE);
    }
    public void updateDashboard(List<NetworkMetrics> metrics) {
        SwingUtilities.invokeLater(() -> {
            if (metrics == null || metrics.isEmpty()) {
                lblTotalGraphs.setText("0");
                lblTotalRoadKm.setText("-");
                lblAvgOneway.setText("-");
                lblAvgSpeed.setText("-");
                lblAvgLanes.setText("-");
                lblResidentialPct.setText("-");
                lblCarOrientedPct.setText("-");
                lblPedestrianPct.setText("-");
                lblCyclingPct.setText("-");
                lblNamedRoadsPct.setText("-");
                lblInfraCount.setText("-");
                lblStreetsPerNode.setText("-");
                dashboardTableModel.setRowCount(0);
                return;
            }
            lblTotalGraphs.setText(String.valueOf(metrics.size()));
            List<NetworkMetrics> withStreet = metrics.stream()
                    .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                    .toList();
            if (withStreet.isEmpty()) {
                lblTotalRoadKm.setText("-");
                lblAvgOneway.setText("-");
                lblAvgSpeed.setText("-");
                lblAvgLanes.setText("-");
                lblResidentialPct.setText("-");
                lblCarOrientedPct.setText("-");
                lblPedestrianPct.setText("-");
                lblCyclingPct.setText("-");
                lblNamedRoadsPct.setText("-");
                lblInfraCount.setText("-");
                lblStreetsPerNode.setText("-");
            } else {
                lblTotalRoadKm.setText(String.format("%.0f km", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getTotalLengthKm()).sum()));
                lblAvgOneway.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getOnewayRatio() * 100).average().orElse(0)));
                lblAvgSpeed.setText(String.format("%.0f km/h", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getAvgMaxSpeedKmh()).filter(v -> v > 0).average().orElse(0)));
                lblAvgLanes.setText(String.format("%.1f", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getAvgLanes()).filter(v -> v > 0).average().orElse(0)));
                lblResidentialPct.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getPctResidential()).average().orElse(0)));
                lblCarOrientedPct.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getCarOrientedLengthPct()).average().orElse(0)));
                lblPedestrianPct.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getPedestrianFriendlyLengthPct()).average().orElse(0)));
                lblCyclingPct.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getCyclingFriendlyLengthPct()).average().orElse(0)));
                lblNamedRoadsPct.setText(String.format("%.0f%%", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getNamedRoadRatio() * 100).average().orElse(0)));
                lblInfraCount.setText(String.valueOf(withStreet.stream()
                        .mapToInt(m -> m.getStreetStats().getBridgeAndTunnelCount()).sum()));
                lblStreetsPerNode.setText(String.format("%.2f", withStreet.stream()
                        .mapToDouble(m -> m.getStreetStats().getStreetsPerNode()).average().orElse(0)));
            }
            dashboardTableModel.setRowCount(0);
            for (NetworkMetrics m : metrics) {
                dashboardTableModel.addRow(buildDashboardRow(m, false));
            }
            dashboardTableModel.addRow(buildDashboardAverageRow(metrics));
        });
    }
    private Object[] buildDashboardRow(NetworkMetrics m, boolean isAverage) {
        StreetNetworkStats s = m.getStreetStats();
        return new Object[] {
                isAverage ? "AVERAGE" : m.getGraphName(),
                streetKm(s),
                streetDominant(s, isAverage),
                streetOneway(s),
                streetSpeed(s),
                streetLanes(s),
                streetInfra(s),
                streetPct(s, StreetNetworkStats::getPedestrianFriendlyLengthPct),
                streetPct(s, StreetNetworkStats::getPctResidential),
                streetPct(s, StreetNetworkStats::getCarOrientedLengthPct),
                streetPct(s, StreetNetworkStats::getCyclingFriendlyLengthPct),
                s != null && s.hasData()
                        ? String.format("%.0f", s.getNamedRoadRatio() * 100) : "N/A",
                streetStreetsPerNode(s)
        };
    }
    private Object[] buildDashboardAverageRow(List<NetworkMetrics> metrics) {
        List<NetworkMetrics> withStreet = metrics.stream()
                .filter(m -> m.getStreetStats() != null && m.getStreetStats().hasData())
                .toList();
        return new Object[] {
                "AVERAGE",
                withStreet.isEmpty() ? "N/A" : String.format("%.1f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getTotalLengthKm()).average().orElse(0)),
                "-",
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getOnewayRatio() * 100).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getAvgMaxSpeedKmh()).filter(v -> v > 0).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.1f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getAvgLanes()).filter(v -> v > 0).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.valueOf((int) withStreet.stream()
                        .mapToInt(m -> m.getStreetStats().getBridgeAndTunnelCount()).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getPedestrianFriendlyLengthPct()).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getPctResidential()).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getCarOrientedLengthPct()).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getCyclingFriendlyLengthPct()).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.0f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getNamedRoadRatio() * 100).average().orElse(0)),
                withStreet.isEmpty() ? "N/A" : String.format("%.2f",
                        withStreet.stream().mapToDouble(m -> m.getStreetStats().getStreetsPerNode()).average().orElse(0))
        };
    }
    private static String streetKm(StreetNetworkStats s) {
        return s != null && s.hasData() ? String.format("%.1f", s.getTotalLengthKm()) : "N/A";
    }
    private static String streetDominant(StreetNetworkStats s, boolean isAverage) {
        if (isAverage) return "-";
        return s != null && s.getDominantHighwayType() != null ? s.getDominantHighwayType() : "N/A";
    }
    private static String streetOneway(StreetNetworkStats s) {
        return s != null && s.hasData() ? String.format("%.0f", s.getOnewayRatio() * 100) : "N/A";
    }
    private static String streetStreetsPerNode(StreetNetworkStats s) {
        return s != null && s.hasData() ? String.format("%.2f", s.getStreetsPerNode()) : "N/A";
    }
    private static String streetSpeed(StreetNetworkStats s) {
        return s != null && s.getAvgMaxSpeedKmh() > 0 ? String.format("%.0f", s.getAvgMaxSpeedKmh()) : "N/A";
    }
    private static String streetLanes(StreetNetworkStats s) {
        return s != null && s.getAvgLanes() > 0 ? String.format("%.1f", s.getAvgLanes()) : "N/A";
    }
    private static String streetPct(StreetNetworkStats s, java.util.function.Function<StreetNetworkStats, Double> getter) {
        return s != null && s.hasData() ? String.format("%.0f", getter.apply(s)) : "N/A";
    }
    private static String streetInfra(StreetNetworkStats s) {
        if (s == null || !s.hasData()) return "N/A";
        return s.getBridgeCount() + " / " + s.getTunnelCount();
    }
    public void setAvailableFiles(List<File> files) {
        SwingUtilities.invokeLater(() -> {
            fileCheckListPanel.removeAll();
            for (File f : files) {
                JCheckBox cb = new JCheckBox(f.getName());
                cb.setSelected(true);
                cb.setBackground(C_PANEL);
                cb.setForeground(C_TEXT);
                cb.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                cb.putClientProperty("file", f);
                cb.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
                fileCheckListPanel.add(cb);
            }
            fileCheckListPanel.revalidate();
            fileCheckListPanel.repaint();
            setStatusMessage(files.size() + " GraphML file(s) found in folder.");
        });
    }
    public void setFolderPath(String path) {
        SwingUtilities.invokeLater(() -> txtFolderPath.setText(path));
    }
    public List<File> getSelectedFiles() {
        List<File> selected = new ArrayList<>();
        for (Component comp : fileCheckListPanel.getComponents()) {
            if (comp instanceof JCheckBox) {
                JCheckBox cb = (JCheckBox) comp;
                if (cb.isSelected()) {
                    Object f = cb.getClientProperty("file");
                    if (f instanceof File) selected.add((File) f);
                }
            }
        }
        return selected;
    }
    public void addFileToQueue(String filename) {
        SwingUtilities.invokeLater(() -> queueModel.addElement("[QUEUED]  " + filename));
    }
    public void markFileProcessed(String filename) {
        SwingUtilities.invokeLater(() -> updateQueueEntry(filename, "[  DONE]  ", C_GOOD));
    }
    public void markFileFailed(String filename) {
        SwingUtilities.invokeLater(() -> updateQueueEntry(filename, "[ ERROR]  ", C_BAD));
    }
    private void updateQueueEntry(String filename, String prefix, Color color) {
        for (int i = 0; i < queueModel.size(); i++) {
            if (queueModel.get(i).contains(filename)) {
                queueModel.set(i, prefix + filename);
                break;
            }
        }
    }
    public void clearFileQueue() {
        SwingUtilities.invokeLater(queueModel::clear);
    }
    public void restoreProcessedFiles(List<String> filenames) {
        if (filenames == null || filenames.isEmpty()) return;
        SwingUtilities.invokeLater(() -> {
            queueModel.clear();
            for (String name : filenames) {
                queueModel.addElement("[  DONE]  " + name);
            }
            lblQueueStatus.setText("Restored " + filenames.size() + " processed file(s) from saved session.");
        });
    }
    public AnalysisMode getAnalysisMode() {
        return rbFullAnalysis != null && rbFullAnalysis.isSelected()
                ? AnalysisMode.FULL : AnalysisMode.QUICK;
    }
    public void setAnalysisMode(AnalysisMode mode) {
        if (rbQuickAnalysis == null || rbFullAnalysis == null) return;
        if (mode == AnalysisMode.FULL) {
            rbFullAnalysis.setSelected(true);
        } else {
            rbQuickAnalysis.setSelected(true);
        }
    }
    public void setProcessingProgress(int value, int max) {
        SwingUtilities.invokeLater(() -> {
            progressBar.setMaximum(max);
            progressBar.setValue(value);
            progressBar.setString(value + " / " + max);
            lblQueueStatus.setText(value == max
                    ? "All " + max + " file(s) processed."
                    : "Processing " + value + " of " + max + "...");
        });
    }
    public void displayChart(JFreeChart chart, String title) {
        SwingUtilities.invokeLater(() -> chartsPanel.addChart(chart, title));
    }
    public void clearCharts() {
        SwingUtilities.invokeLater(chartsPanel::clearCharts);
    }
    public void displayAnalysis(String text) {
        SwingUtilities.invokeLater(() -> {
            ReadableTextKit.setReportText(analysisArea, text);
            tabbedPane.setSelectedIndex(tabIndex("Analysis"));
        });
    }
    public String getAnalysisText() {
        return ReadableTextKit.plainText(analysisArea);
    }
    public void setAnalysisText(String text) {
        SwingUtilities.invokeLater(() -> {
            if (text != null && !text.isBlank()) {
                ReadableTextKit.setReportText(analysisArea, text);
            } else {
                ReadableTextKit.setWelcomeReport(analysisArea);
            }
        });
    }
    public String getChatHistory() {
        return aiAdvisorPanel.getChatText();
    }
    public void setChatHistory(String text) {
        aiAdvisorPanel.setChatText(text);
    }
    private int tabIndex(String titlePart) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            if (tabbedPane.getTitleAt(i).contains(titlePart)) {
                return i;
            }
        }
        return 0;
    }
    public AiAdvisorPanel getAiAdvisorPanel() { return aiAdvisorPanel; }
    public void showAiAdvisorTab() {
        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                if (tabbedPane.getTitleAt(i).contains("AI Advisor")) {
                    tabbedPane.setSelectedIndex(i);
                    break;
                }
            }
        });
    }
    public GraphMapTabPanel getGraphMapTabPanel() { return graphMapTabPanel; }
    public WorldBankDataPanel getWorldBankDataPanel() { return worldBankDataPanel; }
    public PredictionPanel getPredictionPanel() { return predictionPanel; }
    public void updateWorldBankData(List<NetworkMetrics> metrics) {
        if (worldBankDataPanel != null) {
            worldBankDataPanel.updateMetrics(metrics);
        }
    }
    public void updatePredictionData(List<NetworkMetrics> metrics) {
        if (predictionPanel != null) {
            predictionPanel.updateMetrics(metrics);
        }
    }
    public void showMapTab() {
        SwingUtilities.invokeLater(() -> tabbedPane.setSelectedIndex(tabIndex("Map")));
    }
    public void refreshMapFiles(List<File> files) {
        SwingUtilities.invokeLater(() -> {
            if (graphMapTabPanel != null) {
                graphMapTabPanel.setAvailableFiles(files);
            }
        });
    }
    public void updateResultsTable(List<NetworkMetrics> metricsList) {
        SwingUtilities.invokeLater(() -> {
            resultsTableModel.setRowCount(0);
            for (NetworkMetrics m : metricsList) {
                resultsTableModel.addRow(buildRow(m));
            }
        });
    }
    public void setLoadFileAction(ActionListener l)      { loadFileListeners.add(l); }
    public void setLoadDirectoryAction(ActionListener l) { loadDirectoryListeners.add(l); }
    public void setBrowseFolderAction(ActionListener l)  { browseListeners.add(l); }
    public void setProcessAction(ActionListener l) {
        processListeners.add(l);
    }
    public void setGenerateChartsAction(ActionListener l) {
        btnGenerateCharts.addActionListener(l);
        miGenerateCharts.addActionListener(l);
    }
    public void setGenerateAnalysisAction(ActionListener l) {
        generateAnalysisListeners.add(l);
        miGenerateAnalysis.addActionListener(l);
    }
    public void setExportAction(ActionListener l) {
        exportListeners.add(l);
        miExport.addActionListener(l);
    }
    private void buildFrame() {
        frame = new JFrame("Extracting information from open data");
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);
        frame.setBackground(C_BG);
        frame.setJMenuBar(buildMenuBar());
        frame.add(buildTabbedPane(), BorderLayout.CENTER);
        frame.add(buildStatusBar(),  BorderLayout.SOUTH);
    }
    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();
        JMenu file = new JMenu("File");
        JMenuItem miLoadFile = new JMenuItem("Load GraphML File…");
        miLoadFile.setAccelerator(KeyStroke.getKeyStroke("ctrl O"));
        miLoadFile.addActionListener(e -> loadFileListeners.forEach(l -> l.actionPerformed(e)));
        JMenuItem miLoadDir = new JMenuItem("Load GraphML Directory…");
        miLoadDir.setAccelerator(KeyStroke.getKeyStroke("ctrl D"));
        miLoadDir.addActionListener(e -> loadDirectoryListeners.forEach(l -> l.actionPerformed(e)));
        miExport = new JMenuItem("Export Results to CSV…");
        miExport.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        miExit = new JMenuItem("Exit");
        miExit.addActionListener(e -> System.exit(0));
        file.add(miLoadFile);  file.add(miLoadDir);
        file.addSeparator();   file.add(miExport);
        file.addSeparator();   file.add(miExit);
        JMenu tools = new JMenu("Tools");
        miGenerateCharts   = new JMenuItem("Generate Charts");
        miGenerateCharts.setAccelerator(KeyStroke.getKeyStroke("ctrl G"));
        miGenerateAnalysis = new JMenuItem("Generate Analysis Report");
        miGenerateAnalysis.setAccelerator(KeyStroke.getKeyStroke("ctrl R"));
        tools.add(miGenerateCharts);
        tools.add(miGenerateAnalysis);
        JMenu help = new JMenu("Help");
        miAbout = new JMenuItem("About");
        miAbout.addActionListener(e -> showAbout());
        JMenuItem miGuide = new JMenuItem("Data Guide");
        miGuide.addActionListener(e -> tabbedPane.setSelectedIndex(tabIndex("Data Guide")));
        help.add(miGuide);
        help.addSeparator();
        help.add(miAbout);
        bar.add(file); bar.add(tools); bar.add(help);
        return bar;
    }
    private JButton toolBtn(String label, String tooltip) {
        JButton b = UiStyles.toolButton(label);
        b.setToolTipText(tooltip);
        return b;
    }
    private JTabbedPane buildTabbedPane() {
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UiStyles.FONT_UI);
        tabbedPane.setBackground(UiStyles.BG);
        tabbedPane.addTab("Dashboard",    null, buildDashboardTab(),    "Everyday street network summary");
        tabbedPane.addTab("Files",         null, buildFileBrowserTab(),  "Select and process GraphML files");
        graphMapTabPanel = new GraphMapTabPanel();
        tabbedPane.addTab("Map",           null, graphMapTabPanel,       "Street network map coloured by OSM tags");
        tabbedPane.addTab("Charts",        null, buildChartsTab(),       "Street & mobility charts (OSM first)");
        worldBankDataPanel = new WorldBankDataPanel();
        tabbedPane.addTab("World Bank",    null, worldBankDataPanel,     "Country indicators from Data360 API");
        predictionPanel = new PredictionPanel();
        tabbedPane.addTab("Prediction",    null, predictionPanel,      "Predict UMRi from street network metrics");
        tabbedPane.addTab("Analysis",      null, buildAnalysisTab(),     "Full text analysis report");
        aiAdvisorPanel = new AiAdvisorPanel();
        tabbedPane.addTab("AI Advisor",    null, aiAdvisorPanel,         "Gemini-powered comparative insights");
        tabbedPane.addTab("Data Guide",    null, new MetricGuidePanel(), "What the street and map data means");
        tabbedPane.addTab("Results",       null, buildResultsTableTab(), "All metrics in a sortable table");
        return tabbedPane;
    }
    private JPanel buildDashboardTab() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        JLabel heading = UiStyles.sectionHeading("Street Networks at a Glance");
        panel.add(heading, BorderLayout.NORTH);
        JPanel kpiGrid = new JPanel(new GridLayout(4, 3, 14, 14));
        kpiGrid.setBackground(C_BG);
        lblTotalGraphs     = kpiValueLabel();
        lblTotalRoadKm     = kpiValueLabel();
        lblAvgSpeed        = kpiValueLabel();
        lblAvgOneway       = kpiValueLabel();
        lblResidentialPct  = kpiValueLabel();
        lblCarOrientedPct  = kpiValueLabel();
        lblPedestrianPct   = kpiValueLabel();
        lblCyclingPct      = kpiValueLabel();
        lblNamedRoadsPct   = kpiValueLabel();
        lblStreetsPerNode  = kpiValueLabel();
        lblInfraCount      = kpiValueLabel();
        lblAvgLanes        = kpiValueLabel();
        kpiGrid.add(kpiCard("Cities Analysed",       lblTotalGraphs,    "Number of street networks loaded"));
        kpiGrid.add(kpiCard("Total Road Length",     lblTotalRoadKm,    "Sum of all street lengths (km)"));
        kpiGrid.add(kpiCard("Avg Speed Limit",       lblAvgSpeed,       "Mean maxspeed where tagged (km/h)"));
        kpiGrid.add(kpiCard("One-Way Streets",       lblAvgOneway,      "Share of one-way road segments"));
        kpiGrid.add(kpiCard("Residential Streets",   lblResidentialPct, "Length share of residential roads"));
        kpiGrid.add(kpiCard("Car-Oriented Roads",    lblCarOrientedPct, "Motorways, primaries, and similar"));
        kpiGrid.add(kpiCard("Walkable Streets",      lblPedestrianPct,  "Footways, paths, and pedestrian areas"));
        kpiGrid.add(kpiCard("Cycling Infrastructure", lblCyclingPct,    "Cycleways and bike-friendly segments"));
        kpiGrid.add(kpiCard("Named Roads",           lblNamedRoadsPct,  "Segments with a street name in OSM"));
        kpiGrid.add(kpiCard("Streets per Junction",  lblStreetsPerNode, "How many roads meet at each node"));
        kpiGrid.add(kpiCard("Bridges + Tunnels",     lblInfraCount,     "Special infrastructure segments"));
        kpiGrid.add(kpiCard("Avg Lanes",             lblAvgLanes,       "Mean lane count where tagged"));
        JPanel center = new JPanel(new BorderLayout(0, 16));
        center.setOpaque(false);
        center.add(kpiGrid, BorderLayout.NORTH);
        String[] dashCols = {
                "City", "Road km", "Dominant", "Oneway %", "Speed", "Lanes",
                "Br/Tun", "Ped %", "Resid %", "Car %", "Cycle %", "Named %", "St/Node"
        };
        dashboardTableModel = new DefaultTableModel(dashCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        dashboardTable = new JTable(dashboardTableModel);
        dashboardTable.setFont(UiStyles.FONT_UI);
        dashboardTable.setRowHeight(28);
        dashboardTable.setGridColor(C_BORDER);
        dashboardTable.setDefaultRenderer(Object.class, new DashboardCellRenderer());
        dashboardTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 12));
        dashboardTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        for (int c = 0; c < dashCols.length; c++) {
            dashboardTable.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 150 : 68);
        }
        JScrollPane tableScroll = new JScrollPane(dashboardTable);
        tableScroll.setBorder(UiStyles.titledBorder("Per-City Street Profile"));
        tableScroll.setPreferredSize(new Dimension(0, 220));
        center.add(tableScroll, BorderLayout.CENTER);
        panel.add(center, BorderLayout.CENTER);
        JLabel hint = UiStyles.mutedLabel(
                "Load GraphML files in the Files tab and press Process. "
                        + "Graph metrics and OSM details are on the Analysis tab.");
        hint.setBorder(new EmptyBorder(8, 0, 0, 0));
        panel.add(hint, BorderLayout.SOUTH);
        return panel;
    }
    private JLabel kpiValueLabel() {
        JLabel l = new JLabel("-");
        l.setFont(new Font("Segoe UI", Font.BOLD, 26));
        l.setForeground(C_ACCENT);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }
    private JPanel kpiCard(String title, JLabel valueLabel, String tooltip) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(UiStyles.ACCENT_SOFT);
        card.setBorder(UiStyles.cardBorder());
        card.setToolTipText(tooltip);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiStyles.FONT_SMALL);
        titleLabel.setForeground(UiStyles.MUTED);
        card.add(titleLabel,  BorderLayout.NORTH);
        card.add(valueLabel,  BorderLayout.CENTER);
        return card;
    }
    private JSplitPane buildFileBrowserTab() {
        JPanel browserPanel = new JPanel(new BorderLayout(6, 6));
        browserPanel.setBackground(C_BG);
        browserPanel.setBorder(new EmptyBorder(12, 12, 12, 6));
        JPanel pathRow = new JPanel(new BorderLayout(6, 0));
        pathRow.setOpaque(false);
        pathRow.setBorder(new EmptyBorder(0, 0, 6, 0));
        txtFolderPath = new JTextField();
        txtFolderPath.setEditable(false);
        txtFolderPath.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        txtFolderPath.setText("No folder selected - click Browse to locate your GraphML files");
        txtFolderPath.setBackground(new Color(250, 250, 250));
        JButton btnBrowse = UiStyles.accentButton("Browse Folder");
        btnBrowse.addActionListener(e -> browseListeners.forEach(l -> l.actionPerformed(e)));
        JButton btnRefresh = UiStyles.secondaryButton("Refresh");
        btnRefresh.addActionListener(e -> browseListeners.forEach(l -> l.actionPerformed(e)));
        pathRow.add(txtFolderPath, BorderLayout.CENTER);
        pathRow.add(btnBrowse, BorderLayout.EAST);
        JPanel pathAndRefresh = new JPanel(new BorderLayout(4, 0));
        pathAndRefresh.setOpaque(false);
        pathAndRefresh.add(pathRow, BorderLayout.CENTER);
        pathAndRefresh.add(btnRefresh, BorderLayout.EAST);
        fileCheckListPanel = new JPanel();
        fileCheckListPanel.setLayout(new BoxLayout(fileCheckListPanel, BoxLayout.Y_AXIS));
        fileCheckListPanel.setBackground(C_PANEL);
        fileCheckListPanel.add(new JLabel("  (Browse a folder to see available .graphml files)"));
        fileCheckListScroll = new JScrollPane(fileCheckListPanel);
        fileCheckListScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_BORDER), "Available GraphML Files"));
        fileCheckListScroll.getVerticalScrollBar().setUnitIncrement(16);
        JPanel selectButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        selectButtons.setOpaque(false);
        JButton btnAll  = UiStyles.secondaryButton("Select All");
        JButton btnNone = UiStyles.secondaryButton("Select None");
        btnAll.addActionListener(e  -> setAllCheckboxes(true));
        btnNone.addActionListener(e -> setAllCheckboxes(false));
        JButton btnProcessSelected = UiStyles.accentButton("Process Selected");
        btnProcessSelected.addActionListener(e ->
                processListeners.forEach(l -> l.actionPerformed(e)));
        selectButtons.add(btnAll);
        selectButtons.add(btnNone);
        selectButtons.add(Box.createHorizontalStrut(20));
        selectButtons.add(btnProcessSelected);
        JPanel analysisModePanel = buildAnalysisModePanel();
        JPanel browserSouth = new JPanel();
        browserSouth.setLayout(new BoxLayout(browserSouth, BoxLayout.Y_AXIS));
        browserSouth.setOpaque(false);
        browserSouth.add(analysisModePanel);
        browserSouth.add(selectButtons);
        browserPanel.add(pathAndRefresh,     BorderLayout.NORTH);
        browserPanel.add(fileCheckListScroll, BorderLayout.CENTER);
        browserPanel.add(browserSouth,       BorderLayout.SOUTH);
        JPanel queuePanel = new JPanel(new BorderLayout(6, 6));
        queuePanel.setBackground(C_BG);
        queuePanel.setBorder(new EmptyBorder(12, 6, 12, 12));
        queueModel = new DefaultListModel<>();
        queueList  = new JList<>(queueModel);
        queueList.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        queueList.setBackground(C_PANEL);
        queueList.setForeground(C_TEXT);
        queueList.setCellRenderer(new QueueCellRenderer());
        JScrollPane queueScroll = new JScrollPane(queueList);
        queueScroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(C_BORDER), "Processing Status"));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setString("No files processing");
        progressBar.setForeground(C_ACCENT);
        lblQueueStatus = new JLabel("Select files and press Process to begin.");
        lblQueueStatus.setFont(new Font("Arial", Font.ITALIC, 11));
        lblQueueStatus.setForeground(new Color(90, 90, 90));
        JPanel progressPanel = new JPanel(new BorderLayout(4, 4));
        progressPanel.setOpaque(false);
        progressPanel.add(progressBar,    BorderLayout.CENTER);
        progressPanel.add(lblQueueStatus, BorderLayout.SOUTH);
        queuePanel.add(queueScroll,   BorderLayout.CENTER);
        queuePanel.add(progressPanel, BorderLayout.SOUTH);
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, browserPanel, queuePanel);
        split.setDividerLocation(620);
        split.setResizeWeight(0.55);
        return split;
    }
    private void setAllCheckboxes(boolean selected) {
        for (Component c : fileCheckListPanel.getComponents()) {
            if (c instanceof JCheckBox) ((JCheckBox) c).setSelected(selected);
        }
    }
    private JPanel buildAnalysisModePanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 4));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                new EmptyBorder(8, 0, 4, 0),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(C_BORDER), "Analysis Mode")));
        JPanel radios = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 2));
        radios.setOpaque(false);
        rbQuickAnalysis = new JRadioButton("Quick", true);
        rbFullAnalysis = new JRadioButton("Full");
        rbQuickAnalysis.setFont(new Font("Arial", Font.PLAIN, 12));
        rbFullAnalysis.setFont(new Font("Arial", Font.PLAIN, 12));
        rbQuickAnalysis.setOpaque(false);
        rbFullAnalysis.setOpaque(false);
        rbQuickAnalysis.setToolTipText(AnalysisMode.QUICK.getDescription());
        rbFullAnalysis.setToolTipText(AnalysisMode.FULL.getDescription());
        ButtonGroup group = new ButtonGroup();
        group.add(rbQuickAnalysis);
        group.add(rbFullAnalysis);
        radios.add(rbQuickAnalysis);
        radios.add(rbFullAnalysis);
        JLabel hint = new JLabel(
                "<html><span style='color:#666'>Quick is recommended for large cities (e.g. Jakarta). "
                        + "Full computes exact betweenness and may take hours.</span></html>");
        hint.setFont(new Font("Arial", Font.PLAIN, 11));
        panel.add(radios, BorderLayout.NORTH);
        panel.add(hint, BorderLayout.CENTER);
        return panel;
    }
    private class QueueCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                                                      int idx, boolean selected, boolean focus) {
            JLabel lbl = (JLabel) super.getListCellRendererComponent(
                    list, value, idx, selected, focus);
            String text = value == null ? "" : value.toString();
            if      (text.startsWith("[  DONE]")) lbl.setForeground(C_GOOD);
            else if (text.startsWith("[ ERROR]")) lbl.setForeground(C_BAD);
            else if (text.startsWith("[QUEUED]")) lbl.setForeground(C_PENDING);
            else lbl.setForeground(C_TEXT);
            return lbl;
        }
    }
    private JPanel buildChartsTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        btnGenerateCharts = toolBtn("Generate Charts", "Build comparison charts from loaded metrics");
        controls.add(btnGenerateCharts);
        panel.add(controls, BorderLayout.NORTH);
        chartsPanel = new NetworkChartDisplayPanel();
        JScrollPane scroll = new JScrollPane(chartsPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    private JPanel buildAnalysisTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        JButton genBtn  = toolBtn("Generate Report", "Generate full analysis report");
        JButton saveBtn = toolBtn("Save to .txt",   "Save report text to a file");
        genBtn.addActionListener(e -> generateAnalysisListeners.forEach(l -> l.actionPerformed(e)));
        saveBtn.addActionListener(e -> saveAnalysis());
        controls.add(genBtn);
        controls.add(saveBtn);
        panel.add(controls, BorderLayout.NORTH);
        analysisArea = ReadableTextKit.createReadingPane();
        ReadableTextKit.setWelcomeReport(analysisArea);
        JScrollPane scroll = ReadableTextKit.readingScroll(analysisArea, "Analysis Report");
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    private void saveAnalysis() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Save Analysis Report");
        fc.setSelectedFile(new File("network_analysis_report.txt"));
        if (fc.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
            try (java.io.PrintWriter pw =
                         new java.io.PrintWriter(fc.getSelectedFile(), "UTF-8")) {
                pw.print(getAnalysisText());
                showInfoMessage("Report saved to:\n" + fc.getSelectedFile().getName());
            } catch (Exception ex) {
                showErrorMessage("Could not save file: " + ex.getMessage());
            }
        }
    }
    private JPanel buildResultsTableTab() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(C_BG);
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        controls.setOpaque(false);
        JButton csvBtn = toolBtn("Export CSV", "Export this table as CSV");
        csvBtn.addActionListener(e -> exportListeners.forEach(l -> l.actionPerformed(e)));
        controls.add(csvBtn);
        JLabel infoLabel = new JLabel(
                "  Click any column header to sort. UMRi-significant columns: Clustering, Diameter, Density.");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 11));
        infoLabel.setForeground(new Color(80, 80, 80));
        controls.add(infoLabel);
        panel.add(controls, BorderLayout.NORTH);
        String[] cols = {
                "Graph Name", "Analysis", "Nodes", "Edges", "Directed",
                "Betweenness (high)", "Closeness (low)", "Degree (high)",
                "Entropy (low)", "Diameter (low)", "Density (high)",
                "Clustering (high)", "Avg Path (low)", "Mean Degree",
                "Reciprocity", "Diversity", "Assortativity (low)",
                "Road km", "Dominant Hwy", "Oneway %", "Avg Speed", "Streets/Node", "Resid %"
        };
        resultsTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        resultsTable = new JTable(resultsTableModel);
        resultsTable.setAutoCreateRowSorter(true);
        resultsTable.setFont(new Font("Arial", Font.PLAIN, 11));
        resultsTable.setBackground(C_PANEL);
        resultsTable.setForeground(C_TEXT);
        resultsTable.setGridColor(C_BORDER);
        resultsTable.setRowHeight(22);
        resultsTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));
        resultsTable.setFillsViewportHeight(true);
        JScrollPane scroll = new JScrollPane(resultsTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(C_PANEL);
        bar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, C_BORDER));
        bar.setPreferredSize(new Dimension(0, 28));
        statusLabel = new JLabel("  Ready: browse a folder in the Files tab to begin.");
        statusLabel.setFont(UiStyles.FONT_SMALL);
        statusLabel.setForeground(UiStyles.MUTED);
        bar.add(statusLabel, BorderLayout.WEST);
        JLabel versionLabel = new JLabel("Extracting information from open data   ");
        versionLabel.setFont(UiStyles.FONT_SMALL);
        versionLabel.setForeground(UiStyles.MUTED);
        bar.add(versionLabel, BorderLayout.EAST);
        return bar;
    }
    private void showAbout() {
        JOptionPane.showMessageDialog(frame,
                "Extracting information from open data\n\n"
                        + "Analyse urban street networks from OpenStreetMap GraphML files.\n\n"
                        + "• Dashboard: street KPIs across loaded cities\n"
                        + "• Files: browse, select, and process GraphML networks\n"
                        + "• Map: interactive network coloured by OSM tags\n"
                        + "• Charts: side-by-side street and network comparisons\n"
                        + "• World Bank: country indicators via the Data360 API\n"
                        + "• Prediction: urban mobility readiness (UMRi) estimates\n"
                        + "• Analysis: full written comparative report\n"
                        + "• AI Advisor: Gemini-powered Q&A about your cities\n"
                        + "• Results: sortable metrics table with CSV export\n\n"
                        + "Graph metrics are contextualised against urban mobility research:\n"
                        + "  Sierra-Porta, D. & Herrera-Acevedo, D.D. (2024)\n"
                        + "  'Network structure and urban mobility sustainability'\n"
                        + "  Universidad Tecnológica de Bolívar (UTB)\n\n"
                        + "Built with JGraphT, JFreeChart, and Google Gemini.",
                "About",
                JOptionPane.INFORMATION_MESSAGE);
    }
    private class DashboardCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean selected, boolean focus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(
                    table, value, selected, focus, row, column);
            Object graphName = table.getValueAt(row, 0);
            if ("AVERAGE".equals(graphName)) {
                c.setBackground(C_AVG_ROW);
                c.setFont(c.getFont().deriveFont(Font.BOLD));
            } else {
                c.setBackground(row % 2 == 0 ? C_PANEL : new Color(248, 249, 252));
            }
            if (!selected) {
                c.setForeground(C_TEXT);
            }
            return c;
        }
    }
    private Vector<Object> buildRow(NetworkMetrics m) {
        Vector<Object> row = new Vector<>();
        row.add(m.getGraphName());
        row.add(m.getAnalysisModeLabel());
        row.add(m.getNodeCount());
        row.add(m.getEdgeCount());
        row.add(m.isDirected());
        row.add(fmt(m.getAvgBetweennessCentrality()));
        row.add(fmt(m.getAvgClosenessCentrality()));
        row.add(fmt(m.getAvgDegreeCentrality()));
        row.add(fmt(m.getGraphEntropy()));
        row.add(m.getGraphDiameter() != null ? m.getGraphDiameter() : "N/A");
        row.add(fmt(m.getGraphDensity()));
        row.add(fmt(m.getClusteringCoefficient()));
        row.add(fmt(m.getAvgPathLength()));
        row.add(fmt(m.getMeanDegree()));
        row.add(fmt(m.getReciprocity()));
        row.add(fmt(m.getDiversity()));
        row.add(fmt(m.getAssortativityDegree()));
        StreetNetworkStats s = m.getStreetStats();
        row.add(streetKm(s));
        row.add(s != null && s.getDominantHighwayType() != null ? s.getDominantHighwayType() : "N/A");
        row.add(streetOneway(s));
        row.add(s != null && s.getAvgMaxSpeedKmh() > 0 ? String.format("%.0f", s.getAvgMaxSpeedKmh()) : "N/A");
        row.add(streetStreetsPerNode(s));
        row.add(s != null && s.hasData() ? String.format("%.1f", s.getPctResidential()) : "N/A");
        return row;
    }
    private static String fmt(Double v) {
        return v != null ? String.format("%.6f", v) : "N/A";
    }
}