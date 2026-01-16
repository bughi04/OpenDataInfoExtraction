package org.example.view;

import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ModernChartDisplayPanel extends JPanel {
    private List<ChartPanel> chartPanels;
    private static final int PREFERRED_CHART_WIDTH = 500;
    private static final int PREFERRED_CHART_HEIGHT = 400;

    public ModernChartDisplayPanel() {
        setLayout(new GridLayout(0, 2, 10, 10));
        chartPanels = new ArrayList<>();
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    }

    public void addChart(JFreeChart chart, String title) {
        if (chart == null) {
            return;
        }

        JPanel chartContainer = new JPanel(new BorderLayout());
        chartContainer.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        chartContainer.setBorder(ThemeManager.createThemedTitleBorder(title));

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(PREFERRED_CHART_WIDTH, PREFERRED_CHART_HEIGHT));
        chartPanel.setMinimumSize(new Dimension(300, 200));
        chartPanel.setMaximumDrawWidth(2000);
        chartPanel.setMaximumDrawHeight(1500);
        chartPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setDomainZoomable(true);
        chartPanel.setRangeZoomable(true);

        JToolBar chartToolbar = new JToolBar();
        chartToolbar.setFloatable(false);
        chartToolbar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        chartToolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));

        JButton exportButton = new JButton("Export");
        exportButton.setToolTipText("Export chart as image");
        exportButton.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        exportButton.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JButton resetButton = new JButton("Reset Zoom");
        resetButton.setToolTipText("Reset chart zoom");
        resetButton.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        resetButton.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        exportButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Export Chart");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                    "PNG Images", "png"));

            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                try {
                    java.io.File file = fileChooser.getSelectedFile();
                    if (!file.getName().toLowerCase().endsWith(".png")) {
                        file = new java.io.File(file.getAbsolutePath() + ".png");
                    }

                    java.awt.image.BufferedImage image = chart.createBufferedImage(
                            PREFERRED_CHART_WIDTH, PREFERRED_CHART_HEIGHT);
                    javax.imageio.ImageIO.write(image, "png", file);

                    JOptionPane.showMessageDialog(this,
                            "Chart exported to " + file.getName(),
                            "Export Complete", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Error exporting chart: " + ex.getMessage(),
                            "Export Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        resetButton.addActionListener(e -> chartPanel.restoreAutoBounds());

        chartToolbar.add(exportButton);
        chartToolbar.add(resetButton);

        chartContainer.add(chartToolbar, BorderLayout.NORTH);
        chartContainer.add(chartPanel, BorderLayout.CENTER);

        chartPanels.add(chartPanel);

        add(chartContainer);

        revalidate();
        repaint();
    }

    public void clearCharts() {
        for (ChartPanel panel : chartPanels) {
            Component parent = panel.getParent();
            if (parent != null && parent.getParent() != null) {
                remove(parent.getParent());
            }
        }

        chartPanels.clear();
        revalidate();
        repaint();
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        for (ChartPanel panel : chartPanels) {
            Component parent = panel.getParent();
            if (parent instanceof JPanel) {
                JPanel container = (JPanel) parent;
                container.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                if (container.getComponentCount() > 0 && container.getComponent(0) instanceof JToolBar) {
                    JToolBar toolbar = (JToolBar) container.getComponent(0);
                    toolbar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                    for (Component c : toolbar.getComponents()) {
                        if (c instanceof JButton) {
                            JButton button = (JButton) c;
                            button.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                            button.setForeground(ThemeManager.getCurrentTheme().getTextColor());
                        }
                    }
                }
            }
            panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        }

        revalidate();
        repaint();
    }
}