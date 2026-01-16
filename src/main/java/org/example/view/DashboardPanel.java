package org.example.view;

import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class DashboardPanel extends JPanel {
    private JLabel totalItemsLabel;
    private JLabel totalValueLabel;
    private JLabel categoriesLabel;
    private JLabel avgValueLabel;
    private JLabel highValueItemsLabel;
    private JLabel topCategoryLabel;
    private JProgressBar concentrationBar;
    private JPanel miniChartsPanel;
    private JTextArea alertsArea;
    private DataModel dataModel;

    public DashboardPanel() {
        initializeUI();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel headerPanel = createHeaderPanel();

        JPanel contentPanel = new JPanel(new BorderLayout(10, 10));
        contentPanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel kpiPanel = createKPIPanel();

        JPanel middlePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        middlePanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        miniChartsPanel = createMiniChartsPanel();

        JPanel alertsPanel = createAlertsPanel();

        middlePanel.add(miniChartsPanel);
        middlePanel.add(alertsPanel);

        JPanel actionsPanel = createQuickActionsPanel();

        contentPanel.add(kpiPanel, BorderLayout.NORTH);
        contentPanel.add(middlePanel, BorderLayout.CENTER);
        contentPanel.add(actionsPanel, BorderLayout.SOUTH);

        add(headerPanel, BorderLayout.NORTH);
        add(contentPanel, BorderLayout.CENTER);
    }

    private JPanel createHeaderPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, ThemeManager.getCurrentTheme().getAccentColor()),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("Procurement Dashboard");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JLabel subtitleLabel = new JLabel("Real-time overview of your procurement data");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JPanel textPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        textPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        panel.add(textPanel, BorderLayout.WEST);

        JButton refreshButton = ThemeManager.createThemedButton("Refresh Data");
        refreshButton.addActionListener(e -> refreshDashboard());
        panel.add(refreshButton, BorderLayout.EAST);

        return panel;
    }

    private JPanel createKPIPanel() {
        JPanel panel = new JPanel(new GridLayout(2, 3, 15, 15));
        panel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        panel.add(createKPICard("Total Items", "0", "procurement items", Color.decode("#2196F3")));
        panel.add(createKPICard("Total Value", "0.00 RON", "without TVA", Color.decode("#4CAF50")));
        panel.add(createKPICard("Categories", "0", "CPV categories", Color.decode("#FF9800")));
        panel.add(createKPICard("Average Value", "0.00 RON", "per item", Color.decode("#9C27B0")));
        panel.add(createKPICard("High Value Items", "0", ">100K RON", Color.decode("#F44336")));
        panel.add(createConcentrationCard());

        return panel;
    }

    private JPanel createKPICard(String title, String value, String subtitle, Color accentColor) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor(), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 28));
        valueLabel.setForeground(accentColor);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        subtitleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(valueLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(subtitleLabel);

        valueLabel.setName(title.toLowerCase().replace(" ", "_") + "_value");
        card.add(valueLabel);

        return card;
    }

    private JPanel createConcentrationCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor(), 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        JLabel titleLabel = new JLabel("Top Category");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 12));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        topCategoryLabel = new JLabel("No data");
        topCategoryLabel.setFont(new Font("Arial", Font.BOLD, 16));
        topCategoryLabel.setForeground(Color.decode("#607D8B"));
        topCategoryLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel concentrationLabel = new JLabel("Concentration");
        concentrationLabel.setFont(new Font("Arial", Font.PLAIN, 11));
        concentrationLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        concentrationLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        concentrationBar = new JProgressBar(0, 100);
        concentrationBar.setStringPainted(true);
        concentrationBar.setString("0%");
        concentrationBar.setForeground(Color.decode("#607D8B"));
        concentrationBar.setBackground(ThemeManager.getCurrentTheme().getHighlightColor());
        concentrationBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        concentrationBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));

        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(topCategoryLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(concentrationLabel);
        card.add(Box.createRigidArea(new Dimension(0, 5)));
        card.add(concentrationBar);

        return card;
    }

    private JPanel createMiniChartsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("Quick Insights");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JPanel chartsContainer = new JPanel(new GridLayout(2, 1, 5, 5));
        chartsContainer.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JLabel chartPlaceholder1 = new JLabel("Value Distribution Chart", SwingConstants.CENTER);
        chartPlaceholder1.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        chartPlaceholder1.setBorder(BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor()));
        chartPlaceholder1.setPreferredSize(new Dimension(200, 80));

        JLabel chartPlaceholder2 = new JLabel("Category Overview", SwingConstants.CENTER);
        chartPlaceholder2.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        chartPlaceholder2.setBorder(BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor()));
        chartPlaceholder2.setPreferredSize(new Dimension(200, 80));

        chartsContainer.add(chartPlaceholder1);
        chartsContainer.add(chartPlaceholder2);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(chartsContainer, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAlertsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor(), 1),
                BorderFactory.createEmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLabel = new JLabel("Alerts & Insights");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 14));
        titleLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        alertsArea = new JTextArea();
        alertsArea.setEditable(false);
        alertsArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        alertsArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        alertsArea.setFont(new Font("Arial", Font.PLAIN, 12));
        alertsArea.setText("Load procurement data to see insights and alerts.");
        alertsArea.setWrapStyleWord(true);
        alertsArea.setLineWrap(true);

        JScrollPane scrollPane = new JScrollPane(alertsArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createQuickActionsPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        panel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JButton generateReportButton = ThemeManager.createThemedButton("Generate Full Report");
        JButton exportDataButton = ThemeManager.createThemedButton("Export Summary");
        JButton viewChartsButton = ThemeManager.createThemedButton("View All Charts");

        panel.add(generateReportButton);
        panel.add(exportDataButton);
        panel.add(viewChartsButton);

        return panel;
    }

    public void updateDashboard(DataModel dataModel) {
        this.dataModel = dataModel;

        if (dataModel == null || dataModel.getProcurementItems().isEmpty()) {
            resetDashboard();
            return;
        }

        List<ProcurementItem> items = dataModel.getProcurementItems();

        updateKPICard("total_items_value", String.valueOf(items.size()));

        double totalValue = dataModel.getTotalValueWithoutTVA();
        updateKPICard("total_value_value", String.format("%.2f RON", totalValue));

        Map<String, List<ProcurementItem>> categories = dataModel.getProcurementItemsByCategory();
        updateKPICard("categories_value", String.valueOf(categories.size()));

        double avgValue = items.size() > 0 ? totalValue / items.size() : 0;
        updateKPICard("average_value_value", String.format("%.2f RON", avgValue));

        long highValueItems = items.stream()
                .filter(item -> item.getValueWithoutTVA() > 100000)
                .count();
        updateKPICard("high_value_items_value", String.valueOf(highValueItems));

        updateTopCategory(dataModel);

        updateAlerts(dataModel);

        revalidate();
        repaint();
    }

    private void updateKPICard(String cardName, String value) {
        updateLabelInContainer(this, cardName, value);
    }

    private void updateLabelInContainer(Container container, String labelName, String value) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && labelName.equals(comp.getName())) {
                ((JLabel) comp).setText(value);
                return;
            } else if (comp instanceof Container) {
                updateLabelInContainer((Container) comp, labelName, value);
            }
        }
    }

    private void updateTopCategory(DataModel dataModel) {
        Map<String, Double> valueByCategory = dataModel.getValueByCpvCategory();

        if (valueByCategory.isEmpty()) {
            topCategoryLabel.setText("No categories");
            concentrationBar.setValue(0);
            concentrationBar.setString("0%");
            return;
        }

        String topCategory = valueByCategory.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        double topCategoryValue = valueByCategory.get(topCategory);
        double totalValue = dataModel.getTotalValueWithoutTVA();
        double concentration = totalValue > 0 ? (topCategoryValue / totalValue) * 100 : 0;

        String categoryName = getCategoryName(topCategory);
        topCategoryLabel.setText(categoryName);

        concentrationBar.setValue((int) concentration);
        concentrationBar.setString(String.format("%.1f%%", concentration));

        if (concentration > 50) {
            concentrationBar.setForeground(Color.decode("#F44336"));
        } else if (concentration > 30) {
            concentrationBar.setForeground(Color.decode("#FF9800"));
        } else {
            concentrationBar.setForeground(Color.decode("#4CAF50"));
        }
    }

    private void updateAlerts(DataModel dataModel) {
        StringBuilder alerts = new StringBuilder();
        List<ProcurementItem> items = dataModel.getProcurementItems();

        if (items.isEmpty()) {
            alerts.append("No data available for analysis.");
            alertsArea.setText(alerts.toString());
            return;
        }

        Map<String, Double> valueByCategory = dataModel.getValueByCpvCategory();
        if (!valueByCategory.isEmpty()) {
            double topCategoryValue = valueByCategory.values().stream()
                    .max(Double::compareTo)
                    .orElse(0.0);
            double totalValue = dataModel.getTotalValueWithoutTVA();
            double concentration = totalValue > 0 ? (topCategoryValue / totalValue) * 100 : 0;

            if (concentration > 50) {
                alerts.append("⚠️ HIGH CONCENTRATION: Over 50% of spending is in one category. ")
                        .append("Consider diversifying procurement.\n\n");
            }
        }

        long highValueCount = items.stream()
                .filter(item -> item.getValueWithoutTVA() > 100000)
                .count();

        if (highValueCount > 0) {
            alerts.append("💰 HIGH VALUE ITEMS: ").append(highValueCount)
                    .append(" items exceed 100,000 RON. Ensure proper oversight.\n\n");
        }

        long lowValueCount = items.stream()
                .filter(item -> item.getValueWithoutTVA() < 10000 && item.getValueWithoutTVA() > 0)
                .count();

        double lowValuePercentage = items.size() > 0 ? (lowValueCount * 100.0) / items.size() : 0;

        if (lowValuePercentage > 60) {
            alerts.append("📊 OPTIMIZATION OPPORTUNITY: ").append(String.format("%.0f%%", lowValuePercentage))
                    .append(" of items are under 10,000 RON. Consider consolidation.\n\n");
        }

        long missingCpv = items.stream()
                .filter(item -> item.getCpvCodes().isEmpty())
                .count();

        if (missingCpv > 0) {
            double missingPercentage = (missingCpv * 100.0) / items.size();
            alerts.append("⚡ DATA QUALITY: ").append(String.format("%.0f%%", missingPercentage))
                    .append(" of items lack CPV codes. Improve classification.\n\n");
        }

        if (alerts.length() == 0) {
            alerts.append("✅ No critical alerts detected.\n\n")
                    .append("Your procurement data appears well-structured and balanced.");
        }

        alertsArea.setText(alerts.toString());
    }

    private String getCategoryName(String categoryCode) {
        if (dataModel == null) return categoryCode;

        return dataModel.getCpvCodeMap().values().stream()
                .filter(cpv -> cpv.getCode().startsWith(categoryCode))
                .findFirst()
                .map(cpv -> {
                    String name = cpv.getRomanianName();
                    if (name != null && name.length() > 20) {
                        return name.substring(0, 17) + "...";
                    }
                    return name != null ? name : categoryCode;
                })
                .orElse("Category " + categoryCode);
    }

    private void resetDashboard() {
        updateKPICard("total_items_value", "0");
        updateKPICard("total_value_value", "0.00 RON");
        updateKPICard("categories_value", "0");
        updateKPICard("average_value_value", "0.00 RON");
        updateKPICard("high_value_items_value", "0");

        topCategoryLabel.setText("No data");
        concentrationBar.setValue(0);
        concentrationBar.setString("0%");
        concentrationBar.setForeground(Color.decode("#607D8B"));

        alertsArea.setText("Load procurement data to see insights and alerts.");
    }

    private void refreshDashboard() {
        if (dataModel != null) {
            updateDashboard(dataModel);
        }
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        updateComponentTheme(this);
        revalidate();
        repaint();
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            } else if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTextArea) {
                ((JTextArea) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextArea) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            }

            if (comp instanceof Container) {
                updateComponentTheme((Container) comp);
            }
        }
    }
}