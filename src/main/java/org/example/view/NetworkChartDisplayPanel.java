package org.example.view;

import org.example.theme.ThemeManager;
import org.example.theme.UiStyles;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

// Displays JFreeCharts in a 2-column grid, each with an Export and Reset Zoom button in a mini toolbar
public class NetworkChartDisplayPanel extends JPanel {
    private static final int CHART_W = 550;
    private static final int CHART_H = 420;
    private final List<ChartPanel> chartPanels = new ArrayList<>();
    public NetworkChartDisplayPanel() {
        setLayout(new GridLayout(0, 2, 12, 12));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
    }
    public void addChart(JFreeChart chart, String title) {
        if (chart == null) return;
        JPanel container = new JPanel(new BorderLayout(0, 4));
        container.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        container.setBorder(ThemeManager.createThemedTitleBorder(title));
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        toolbar.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        JButton exportBtn = makeToolButton("Export PNG");
        JButton resetBtn  = makeToolButton("Reset Zoom");
        toolbar.add(exportBtn);
        toolbar.addSeparator(new Dimension(6, 0));
        toolbar.add(resetBtn);
        ChartPanel cp = new ChartPanel(chart);
        cp.setPreferredSize(new Dimension(CHART_W, CHART_H));
        cp.setMinimumSize(new Dimension(300, 220));
        cp.setMaximumDrawWidth(2400);
        cp.setMaximumDrawHeight(1600);
        cp.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        cp.setMouseWheelEnabled(true);
        cp.setDomainZoomable(true);
        cp.setRangeZoomable(true);
        exportBtn.addActionListener(e -> exportChart(chart, title));
        resetBtn.addActionListener(e  -> cp.restoreAutoBounds());
        container.add(toolbar, BorderLayout.NORTH);
        container.add(cp,      BorderLayout.CENTER);
        chartPanels.add(cp);
        add(container);
        revalidate();
        repaint();
    }
    public void clearCharts() {
        removeAll();
        chartPanels.clear();
        revalidate();
        repaint();
    }
    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        for (ChartPanel cp : chartPanels) {
            cp.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            Container parent = cp.getParent();
            if (parent instanceof JPanel) {
                parent.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            }
        }
        revalidate();
        repaint();
    }
    private JButton makeToolButton(String text) {
        JButton btn = UiStyles.secondaryButton(text);
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        btn.setMargin(new Insets(2, 8, 2, 8));
        return btn;
    }
    private void exportChart(JFreeChart chart, String defaultName) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Chart as PNG");
        fc.setSelectedFile(new java.io.File(defaultName.replaceAll("[^a-zA-Z0-9]", "_") + ".png"));
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        java.io.File file = fc.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".png")) {
            file = new java.io.File(file.getAbsolutePath() + ".png");
        }
        try {
            BufferedImage img = chart.createBufferedImage(CHART_W, CHART_H);
            javax.imageio.ImageIO.write(img, "png", file);
            JOptionPane.showMessageDialog(this,
                "Chart exported to:\n" + file.getName(),
                "Export Complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                "Export failed: " + ex.getMessage(),
                "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}