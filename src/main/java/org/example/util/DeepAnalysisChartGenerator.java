package org.example.util;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.*;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DeepAnalysisChartGenerator {
    private static final Logger logger = LoggerFactory.getLogger(DeepAnalysisChartGenerator.class);

    private static final Color[] ENHANCED_COLORS = {
            Color.decode("#1f77b4"), Color.decode("#ff7f0e"), Color.decode("#2ca02c"),
            Color.decode("#d62728"), Color.decode("#9467bd"), Color.decode("#8c564b"),
            Color.decode("#e377c2"), Color.decode("#7f7f7f"), Color.decode("#bcbd22"),
            Color.decode("#17becf"), Color.decode("#aec7e8"), Color.decode("#ffbb78")
    };

    public static JFreeChart generateMonthlyTrendAnalysisChart(DataModel model) {
        logger.info("Generating monthly trend analysis chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createLineChart(
                    "Monthly Procurement Trend Analysis",
                    "Month", "Value (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeChart(chart);
            return chart;
        }

        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        Map<String, Double> valueByMonth = new LinkedHashMap<>();
        Map<String, Integer> countByMonth = new LinkedHashMap<>();

        for (String month : months) {
            valueByMonth.put(month, 0.0);
            countByMonth.put(month, 0);
        }

        for (ProcurementItem item : items) {
            String date = item.getInitiationDate();
            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date == null || date.isEmpty()) {
                continue;
            }

            String month = extractMonthFromDate(date);
            if (month != null) {
                valueByMonth.put(month, valueByMonth.get(month) + item.getValueWithoutTVA());
                countByMonth.put(month, countByMonth.get(month) + 1);
            }
        }

        for (String month : months) {
            double value = valueByMonth.get(month) / 1000;
            int count = countByMonth.get(month);

            dataset.addValue(value, "Value (1000s RON)", month);
            dataset.addValue(count * 10, "Item Count (×10)", month);
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Monthly Procurement Trend Analysis",
                "Month", "Value (1000s RON) / Count (×10)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();

        renderer.setSeriesPaint(0, ENHANCED_COLORS[0]);
        renderer.setSeriesPaint(1, ENHANCED_COLORS[1]);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 1.0f, new float[]{5.0f, 5.0f}, 0.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesVisible(1, true);

        plot.setRenderer(renderer);

        return chart;
    }

    public static JFreeChart generateValueAnalysisChart(DataModel model) {
        logger.info("Generating comprehensive value analysis chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        String[] ranges = {"0-10K", "10K-50K", "50K-100K", "100K+"};
        String[] originalRanges = {"0-10,000", "10,000-50,000", "50,000-100,000", "100,000+"};

        for (int i = 0; i < ranges.length; i++) {
            List<ProcurementItem> items = itemsByRange.getOrDefault(originalRanges[i], Collections.emptyList());
            int count = items.size();
            double totalValue = items.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
            double avgValue = count > 0 ? totalValue / count : 0;

            dataset.addValue(count, "Item Count", ranges[i]);
            dataset.addValue(totalValue / 1000, "Total Value (1000s RON)", ranges[i]);
            dataset.addValue(avgValue / 1000, "Average Value (1000s RON)", ranges[i]);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Value Analysis by Range",
                "Value Range (RON)",
                "Count / Value",
                dataset,
                PlotOrientation.VERTICAL,
                true, true, false
        );

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        renderer.setSeriesPaint(0, ENHANCED_COLORS[0]);
        renderer.setSeriesPaint(1, ENHANCED_COLORS[1]);
        renderer.setSeriesPaint(2, ENHANCED_COLORS[2]);

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);

        return chart;
    }

    public static JFreeChart generateCategoryConcentrationChart(DataModel model) {
        logger.info("Generating category concentration analysis chart");

        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<String, Double> valueByCategory = model.getValueByCpvCategory();

        if (valueByCategory.isEmpty()) {
            dataset.setValue("No Data", 1.0);
        } else {
            List<Map.Entry<String, Double>> sortedCategories = valueByCategory.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                    .collect(Collectors.toList());

            double totalValue = model.getTotalValueWithoutTVA();
            double otherValue = 0;
            int maxCategories = 8;

            for (int i = 0; i < sortedCategories.size(); i++) {
                Map.Entry<String, Double> entry = sortedCategories.get(i);
                String category = entry.getKey();
                double value = entry.getValue();

                if (i < maxCategories && value > 0) {
                    String categoryName = getShortCategoryName(category, model.getCpvCodeMap());
                    double percentage = (value / totalValue) * 100;

                    String label = String.format("%s (%s) (%.1f%%)",
                            categoryName,
                            category,
                            percentage);

                    dataset.setValue(label, value);
                } else {
                    otherValue += value;
                }
            }

            if (otherValue > 0) {
                double percentage = (otherValue / totalValue) * 100;
                dataset.setValue(String.format("Others (%.1f%%)", percentage), otherValue);
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Procurement Spending Concentration by Category",
                dataset, true, true, false
        );

        customizeChart(chart);
        customizePieChart(chart);

        return chart;
    }

    public static JFreeChart generateEfficiencyAnalysisChart(DataModel model) {
        logger.info("Generating efficiency analysis chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, List<ProcurementItem>> itemsByCategory = model.getProcurementItemsByCategory();

        if (itemsByCategory.isEmpty()) {
            dataset.addValue(0, "Efficiency", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Procurement Efficiency Analysis",
                    "Category", "Avg Value per Item (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeChart(chart);
            return chart;
        }

        List<Map.Entry<String, Double>> efficiencyData = new ArrayList<>();

        for (Map.Entry<String, List<ProcurementItem>> entry : itemsByCategory.entrySet()) {
            String category = entry.getKey();
            List<ProcurementItem> items = entry.getValue();

            if (!items.isEmpty()) {
                double totalValue = items.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
                double avgValue = totalValue / items.size();
                efficiencyData.add(new AbstractMap.SimpleEntry<>(category, avgValue));
            }
        }

        efficiencyData.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        for (int i = 0; i < Math.min(10, efficiencyData.size()); i++) {
            Map.Entry<String, Double> entry = efficiencyData.get(i);
            String category = entry.getKey();
            double avgValue = entry.getValue() / 1000;

            String categoryName = getShortCategoryName(category, model.getCpvCodeMap());
            dataset.addValue(avgValue, "Avg Value per Item", categoryName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Efficiency by Category (Top 10)",
                "Category", "Average Value per Item (1000s RON)",
                dataset, PlotOrientation.HORIZONTAL, false, true, false);

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setMaximumCategoryLabelWidthRatio(0.8f);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ENHANCED_COLORS[4]);

        return chart;
    }

    public static JFreeChart generateTimelineAnalysisChart(DataModel model) {
        logger.info("Generating timeline analysis chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty() || !hasTimeData(model)) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Quarterly Procurement Analysis",
                    "Quarter", "Value (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeChart(chart);
            return chart;
        }

        String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
        Map<String, Double> valueByQuarter = new LinkedHashMap<>();
        Map<String, Integer> countByQuarter = new LinkedHashMap<>();

        for (String quarter : quarters) {
            valueByQuarter.put(quarter, 0.0);
            countByQuarter.put(quarter, 0);
        }

        for (ProcurementItem item : items) {
            String date = item.getInitiationDate();
            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date == null || date.isEmpty()) {
                continue;
            }

            String quarter = extractQuarterFromDate(date);
            if (quarter != null) {
                valueByQuarter.put(quarter, valueByQuarter.get(quarter) + item.getValueWithoutTVA());
                countByQuarter.put(quarter, countByQuarter.get(quarter) + 1);
            }
        }

        for (String quarter : quarters) {
            double value = valueByQuarter.get(quarter) / 1000;
            int count = countByQuarter.get(quarter);

            dataset.addValue(value, "Value (1000s RON)", quarter);
            dataset.addValue(count, "Item Count", quarter);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Quarterly Procurement Distribution",
                "Quarter", "Value (1000s RON) / Count",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ENHANCED_COLORS[2]);
        renderer.setSeriesPaint(1, ENHANCED_COLORS[3]);

        return chart;
    }

    public static JFreeChart generateSupplierDependencyChart(DataModel model) {
        logger.info("Generating supplier dependency chart");

        DefaultPieDataset dataset = new DefaultPieDataset();
        Map<String, Double> valueByCategory = model.getValueByCpvCategory();

        if (valueByCategory.isEmpty()) {
            dataset.setValue("No Data", 1.0);
        } else {
            double totalValue = model.getTotalValueWithoutTVA();

            double concentrationScore = 0;
            for (double value : valueByCategory.values()) {
                double marketShare = value / totalValue;
                concentrationScore += marketShare * marketShare;
            }

            concentrationScore *= 100;

            if (concentrationScore > 25) {
                dataset.setValue("High Concentration Risk", concentrationScore);
                dataset.setValue("Diversified Spending", 100 - concentrationScore);
            } else {
                dataset.setValue("Well Diversified", 100 - concentrationScore);
                dataset.setValue("Concentration Risk", concentrationScore);
            }
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Procurement Concentration Risk Analysis",
                dataset, true, true, false);

        customizeChart(chart);
        customizePieChart(chart);

        return chart;
    }

    public static JFreeChart generateMaturityAssessmentChart(DataModel model) {
        logger.info("Generating maturity assessment chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Score", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Procurement Maturity Assessment",
                    "Maturity Area", "Score (0-100)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeChart(chart);
            return chart;
        }

        int totalItems = items.size();

        long itemsWithCpv = items.stream().filter(item -> !item.getCpvCodes().isEmpty()).count();
        long itemsWithDates = items.stream().filter(item ->
                (item.getInitiationDate() != null && !item.getInitiationDate().isEmpty()) ||
                        (item.getCompletionDate() != null && !item.getCompletionDate().isEmpty())
        ).count();
        long itemsWithValues = items.stream().filter(item -> item.getValueWithoutTVA() > 0).count();

        double dataQualityScore = ((itemsWithCpv + itemsWithDates + itemsWithValues) * 100.0) / (totalItems * 3);

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        double categoryScore = 100;
        if (!valueByCategory.isEmpty()) {
            double totalValue = model.getTotalValueWithoutTVA();
            double maxCategoryPercentage = valueByCategory.values().stream()
                    .mapToDouble(v -> v / totalValue * 100)
                    .max().orElse(0);

            if (maxCategoryPercentage > 50) {
                categoryScore -= (maxCategoryPercentage - 50);
            }
        }

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();
        List<ProcurementItem> smallItems = itemsByRange.getOrDefault("0-10,000", Collections.emptyList());
        double smallItemsPercentage = (smallItems.size() * 100.0) / totalItems;
        double processScore = 100 - Math.max(0, smallItemsPercentage - 30);

        double planningScore = hasTimeData(model) ? 85 : 40;

        dataset.addValue(Math.max(0, dataQualityScore), "Score", "Data Quality");
        dataset.addValue(Math.max(0, categoryScore), "Score", "Category Mgmt");
        dataset.addValue(Math.max(0, processScore), "Score", "Process Efficiency");
        dataset.addValue(Math.max(0, planningScore), "Score", "Strategic Planning");

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Maturity Assessment",
                "Maturity Area", "Score (0-100)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, ENHANCED_COLORS[5]);

        plot.addRangeMarker(new org.jfree.chart.plot.ValueMarker(70, Color.RED, new BasicStroke(2.0f)));

        return chart;
    }

    public static JFreeChart generateRiskAssessmentChart(DataModel model) {
        logger.info("Generating risk assessment chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Risk Level", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Procurement Risk Assessment",
                    "Risk Category", "Risk Level (1-10)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeChart(chart);
            return chart;
        }

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        double totalValue = model.getTotalValueWithoutTVA();

        double concentrationRisk = 1;
        if (!valueByCategory.isEmpty()) {
            double maxCategoryPercentage = valueByCategory.values().stream()
                    .mapToDouble(v -> v / totalValue * 100)
                    .max().orElse(0);
            concentrationRisk = Math.min(10, maxCategoryPercentage / 10);
        }

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();
        List<ProcurementItem> highValueItems = itemsByRange.getOrDefault("100,000+", Collections.emptyList());
        double highValuePercentage = (highValueItems.size() * 100.0) / items.size();
        double valueRisk = Math.min(10, highValuePercentage / 5);

        long itemsWithoutCpv = items.stream().filter(item -> item.getCpvCodes().isEmpty()).count();
        double dataQualityRisk = Math.min(10, (itemsWithoutCpv * 10.0) / items.size());

        double timeRisk = hasTimeData(model) ? 2 : 8;

        dataset.addValue(concentrationRisk, "Risk Level", "Concentration");
        dataset.addValue(valueRisk, "Risk Level", "High Value Items");
        dataset.addValue(dataQualityRisk, "Risk Level", "Data Quality");
        dataset.addValue(timeRisk, "Risk Level", "Time Planning");

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Risk Assessment",
                "Risk Category", "Risk Level (1-10, 10=highest risk)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        customizeChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        renderer.setSeriesPaint(0, concentrationRisk > 7 ? Color.RED :
                concentrationRisk > 4 ? Color.ORANGE : Color.GREEN);

        return chart;
    }

    private static String extractMonthFromDate(String date) {
        if (date == null || date.isEmpty()) return null;

        String lowerDate = date.toLowerCase();

        if (lowerDate.contains("ian") || lowerDate.contains("ianuarie")) return "Jan";
        if (lowerDate.contains("feb") || lowerDate.contains("februarie")) return "Feb";
        if (lowerDate.contains("mar") || lowerDate.contains("martie")) return "Mar";
        if (lowerDate.contains("apr") || lowerDate.contains("aprilie")) return "Apr";
        if (lowerDate.contains("mai")) return "May";
        if (lowerDate.contains("iun") || lowerDate.contains("iunie")) return "Jun";
        if (lowerDate.contains("iul") || lowerDate.contains("iulie")) return "Jul";
        if (lowerDate.contains("aug") || lowerDate.contains("august")) return "Aug";
        if (lowerDate.contains("sep") || lowerDate.contains("septembrie")) return "Sep";
        if (lowerDate.contains("oct") || lowerDate.contains("octombrie")) return "Oct";
        if (lowerDate.contains("noi") || lowerDate.contains("noiembrie")) return "Nov";
        if (lowerDate.contains("dec") || lowerDate.contains("decembrie")) return "Dec";

        if (lowerDate.contains("january")) return "Jan";
        if (lowerDate.contains("february")) return "Feb";
        if (lowerDate.contains("march")) return "Mar";
        if (lowerDate.contains("april")) return "Apr";
        if (lowerDate.contains("may")) return "May";
        if (lowerDate.contains("june")) return "Jun";
        if (lowerDate.contains("july")) return "Jul";
        if (lowerDate.contains("august")) return "Aug";
        if (lowerDate.contains("september")) return "Sep";
        if (lowerDate.contains("october")) return "Oct";
        if (lowerDate.contains("november")) return "Nov";
        if (lowerDate.contains("december")) return "Dec";

        Pattern datePattern = Pattern.compile("\\b(\\d{1,2})[-/](\\d{1,2})[-/](\\d{2,4})\\b");
        Matcher matcher = datePattern.matcher(date);

        if (matcher.find()) {
            try {
                int month1 = Integer.parseInt(matcher.group(1));
                int month2 = Integer.parseInt(matcher.group(2));

                int month = month2;
                if (month2 > 12 && month1 <= 12) {
                    month = month1;
                }

                if (month >= 1 && month <= 12) {
                    String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                            "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
                    return monthNames[month - 1];
                }
            } catch (NumberFormatException e) {
                // Ignore parsing errors
            }
        }

        return null;
    }

    private static String extractQuarterFromDate(String date) {
        String month = extractMonthFromDate(date);
        if (month == null) return null;

        switch (month) {
            case "Jan": case "Feb": case "Mar": return "Q1";
            case "Apr": case "May": case "Jun": return "Q2";
            case "Jul": case "Aug": case "Sep": return "Q3";
            case "Oct": case "Nov": case "Dec": return "Q4";
            default: return null;
        }
    }

    private static String getShortCategoryName(String category, Map<String, CpvCode> cpvCodeMap) {
        if (category == null || category.isEmpty()) return "Unknown";
        if (category.equals("00")) return "Uncategorized";

        return cpvCodeMap.values().stream()
                .filter(cpv -> cpv.getCode().startsWith(category))
                .findFirst()
                .map(cpv -> {
                    String name = cpv.getRomanianName();
                    if (name != null && name.length() > 0) {
                        if (name.toLowerCase().startsWith("servicii de ")) {
                            name = name.substring(12);
                        } else if (name.toLowerCase().startsWith("servicii privind ")) {
                            name = name.substring(16);
                        } else if (name.toLowerCase().startsWith("servicii ")) {
                            name = name.substring(9);
                        }

                        if (name.length() > 15) {
                            String[] words = name.split("\\s+");
                            StringBuilder result = new StringBuilder();
                            for (String word : words) {
                                if (result.length() + word.length() > 15) break;
                                if (result.length() > 0) result.append(" ");
                                result.append(word);
                            }
                            return result.length() > 0 ? result.toString() : name.substring(0, 15);
                        }
                        return name;
                    }
                    return "Cat. " + category;
                })
                .orElse("Cat. " + category);
    }

    private static boolean hasTimeData(DataModel model) {
        int itemsWithDates = 0;
        for (ProcurementItem item : model.getProcurementItems()) {
            if ((item.getInitiationDate() != null && !item.getInitiationDate().isEmpty()) ||
                    (item.getCompletionDate() != null && !item.getCompletionDate().isEmpty())) {
                itemsWithDates++;
            }
        }
        return itemsWithDates >= model.getProcurementItems().size() * 0.2;
    }

    private static void customizeChart(JFreeChart chart) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            chart.getLegend().setItemPaint(ThemeManager.getCurrentTheme().getTextColor());
        }

        if (chart.getPlot() instanceof CategoryPlot) {
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            plot.setRangeGridlinePaint(new Color(200, 200, 200, 100));
            plot.setOutlineVisible(false);

            CategoryAxis domainAxis = plot.getDomainAxis();
            domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
            domainAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
            domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
            domainAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());

            domainAxis.setCategoryLabelPositions(
                    org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
            domainAxis.setMaximumCategoryLabelWidthRatio(0.8f);

            NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
            rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
            rangeAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
            rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
            rangeAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
            rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));
        }
    }

    private static void customizePieChart(JFreeChart chart) {
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("Arial", Font.PLAIN, 10));
        plot.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        plot.setLabelBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setShadowPaint(null);
        plot.setSimpleLabels(true);
        plot.setLabelGap(0.05);
        plot.setMaximumLabelWidth(0.30);

        int colorIndex = 0;
        for (Object key : plot.getDataset().getKeys()) {
            plot.setSectionPaint((Comparable) key, ENHANCED_COLORS[colorIndex % ENHANCED_COLORS.length]);
            colorIndex++;
        }

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0}", NumberFormat.getInstance(), NumberFormat.getPercentInstance()
        ));
    }
}