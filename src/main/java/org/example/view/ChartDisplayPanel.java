package org.example.view;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ChartDisplayPanel extends JPanel {
    private List<ChartPanel> chartPanels;

    public ChartDisplayPanel() {
        setLayout(new GridLayout(0, 1));
        chartPanels = new ArrayList<>();
        setBackground(Color.WHITE);
    }

    public void addChart(JFreeChart chart, String title) {
        if (chart == null) {
            return;
        }

        ChartPanel chartPanel = new ChartPanel(chart);
        chartPanel.setPreferredSize(new Dimension(400, 300));
        chartPanel.setMinimumSize(new Dimension(300, 200));
        chartPanel.setMaximumDrawWidth(1920);
        chartPanel.setMaximumDrawHeight(1080);
        chartPanel.setBorder(BorderFactory.createTitledBorder(title));
        chartPanel.setMouseWheelEnabled(true);
        chartPanel.setBackground(Color.WHITE);

        chartPanels.add(chartPanel);
        add(chartPanel);

        revalidate();
        repaint();
    }

    public void clearCharts() {
        for (ChartPanel panel : chartPanels) {
            remove(panel);
        }

        chartPanels.clear();

        revalidate();
        repaint();
    }
}