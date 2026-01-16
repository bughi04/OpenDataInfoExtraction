package org.example.util;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.example.theme.ThemeManager;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EnhancedChartGenerator {
    private static final Logger logger = LoggerFactory.getLogger(EnhancedChartGenerator.class);

    private static Color[] getChartColors() {
        return ThemeManager.getCurrentTheme().getChartColors();
    }

    public static JFreeChart generateEnhancedCategoryPieChart(DataModel model) {
        logger.info("Generating enhanced category pie chart with better labels");
        DefaultPieDataset dataset = new DefaultPieDataset();

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        double totalValue = model.getTotalValueWithoutTVA();

        if (valueByCategory.isEmpty()) {
            dataset.setValue("No Data", 1.0);
            JFreeChart chart = ChartFactory.createPieChart(
                    "Procurement Distribution by CPV Category",
                    dataset, true, true, false);
            customizePieChart(chart);
            return chart;
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(valueByCategory.entrySet());
        sortedCategories.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        int maxCategories = 8;
        double otherValue = 0;

        for (int i = 0; i < sortedCategories.size(); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            String category = entry.getKey();
            Double value = entry.getValue();

            if (value <= 0) continue;

            String categoryName = getShortCategoryName(category, model.getCpvCodeMap());

            if (i < maxCategories) {
                double percentage = (value / totalValue) * 100;
                String label = String.format("%s (%s) (%.0f%%)",
                        categoryName,
                        category,
                        percentage);
                dataset.setValue(label, value);
            } else {
                otherValue += value;
            }
        }

        if (otherValue > 0) {
            double otherPercentage = (otherValue / totalValue) * 100;
            String otherCount = String.valueOf(sortedCategories.size() - maxCategories);
            dataset.setValue(String.format("Other (%s categories) (%.0f%%)",
                    otherCount, otherPercentage), otherValue);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Procurement Value by CPV Category",
                dataset, true, true, false);

        customizePieChart(chart);

        chart.addSubtitle(new org.jfree.chart.title.TextTitle(
                String.format("Total Value: %,.2f RON", totalValue),
                new Font("Arial", Font.PLAIN, 12)));

        return chart;
    }

    public static JFreeChart generateEnhancedTopItemsBarChart(DataModel model, int n) {
        logger.info("Generating enhanced top {} items bar chart with fixed labels", n);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<ProcurementItem> items = model.getTopProcurementItemsByValue(n);

        if (items.isEmpty()) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Top Procurement Items by Value",
                    "Item", "Value (RON)",
                    dataset, PlotOrientation.HORIZONTAL, false, true, false);
            customizeBarChart(chart);
            return chart;
        }

        for (int i = items.size() - 1; i >= 0; i--) {
            ProcurementItem item = items.get(i);
            String name = truncateNameIntelligently(item.getObjectName(), 35);
            double value = item.getValueWithoutTVA();

            dataset.addValue(value, "Value", name);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top " + n + " Procurement Items by Value",
                "Item", "Value (RON)",
                dataset, PlotOrientation.HORIZONTAL, false, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        Color baseColor = getChartColors()[0];
        GradientPaint gp = new GradientPaint(
                0.0f, 0.0f, baseColor,
                0.0f, 0.0f, ThemeManager.getCurrentTheme().getAccentGradientLight(), false);
        renderer.setSeriesPaint(0, gp);

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator(
                "{2} RON", new DecimalFormat("#,##0")));
        renderer.setDefaultItemLabelsVisible(true);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setMaximumCategoryLabelWidthRatio(0.9f);
        domainAxis.setCategoryLabelPositions(
                org.jfree.chart.axis.CategoryLabelPositions.STANDARD);

        return chart;
    }

    public static JFreeChart generateMonthlyAnalysisChart(DataModel model) {
        logger.info("Generating monthly analysis chart with fixed labels");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Monthly Procurement Analysis",
                    "Month", "Value (RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeBarChart(chart);
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
            double value = valueByMonth.get(month);
            int count = countByMonth.get(month);

            dataset.addValue(value / 1000, "Value (1000s RON)", month);
            dataset.addValue(count, "Item Count", month);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Monthly Procurement Distribution",
                "Month", "Value (1000s RON) / Count",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        renderer.setSeriesPaint(0, getChartColors()[0]);
        renderer.setSeriesPaint(1, getChartColors()[1]);

        return chart;
    }

    public static JFreeChart generateMonthlyTrendChart(DataModel model) {
        logger.info("Generating monthly trend chart");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createLineChart(
                    "Monthly Procurement Trend",
                    "Month", "Value (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeLineChart(chart);
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
            dataset.addValue(count * 2, "Item Count (×2)", month);
        }

        JFreeChart chart = ChartFactory.createLineChart(
                "Monthly Procurement Trend",
                "Month", "Value (1000s RON) / Count (×2)",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        customizeLineChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();

        renderer.setSeriesPaint(0, getChartColors()[0]);
        renderer.setSeriesPaint(1, getChartColors()[1]);
        renderer.setSeriesStroke(0, new BasicStroke(3.0f));
        renderer.setSeriesStroke(1, new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                1.0f, new float[]{5.0f, 5.0f}, 0.0f));
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesVisible(1, true);

        plot.setRenderer(renderer);

        return chart;
    }

    public static JFreeChart generateSeasonalAnalysisChart(DataModel model) {
        logger.info("Generating seasonal analysis chart");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.addValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Seasonal Procurement Analysis",
                    "Season", "Value (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeBarChart(chart);
            return chart;
        }

        Map<String, String[]> seasons = new LinkedHashMap<>();
        seasons.put("Spring", new String[]{"Mar", "Apr", "May"});
        seasons.put("Summer", new String[]{"Jun", "Jul", "Aug"});
        seasons.put("Autumn", new String[]{"Sep", "Oct", "Nov"});
        seasons.put("Winter", new String[]{"Dec", "Jan", "Feb"});

        Map<String, Double> valueByMonth = new HashMap<>();
        String[] allMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (String month : allMonths) {
            valueByMonth.put(month, 0.0);
        }

        for (ProcurementItem item : items) {
            String date = item.getInitiationDate();
            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date != null) {
                String month = extractMonthFromDate(date);
                if (month != null) {
                    valueByMonth.put(month, valueByMonth.get(month) + item.getValueWithoutTVA());
                }
            }
        }

        for (Map.Entry<String, String[]> seasonEntry : seasons.entrySet()) {
            String season = seasonEntry.getKey();
            String[] seasonMonths = seasonEntry.getValue();

            double seasonalValue = 0.0;
            for (String month : seasonMonths) {
                seasonalValue += valueByMonth.get(month);
            }

            dataset.addValue(seasonalValue / 1000, "Value (1000s RON)", season);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Seasonal Procurement Analysis",
                "Season", "Value (1000s RON)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, getChartColors()[2]);

        return chart;
    }

    public static JFreeChart generateEnhancedValueRangeChart(DataModel model) {
        logger.info("Generating enhanced value range chart with fixed labels");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        if (itemsByRange.isEmpty()) {
            dataset.addValue(0, "Count", "No Data");
            dataset.addValue(0, "Total Value (scaled)", "No Data");
            JFreeChart chart = ChartFactory.createStackedBarChart(
                    "Procurement by Value Range",
                    "Value Range (RON)", "Count / Value",
                    dataset, PlotOrientation.VERTICAL, true, true, false);
            customizeBarChart(chart);
            return chart;
        }

        String[] ranges = {"0-10K", "10K-50K", "50K-100K", "100K+"};
        String[] originalRanges = {"0-10,000", "10,000-50,000", "50,000-100,000", "100,000+"};
        double scaleFactor = 10000;

        for (int i = 0; i < ranges.length; i++) {
            List<ProcurementItem> items = itemsByRange.getOrDefault(originalRanges[i], Collections.emptyList());
            int count = items.size();

            double rangeValue = items.stream()
                    .mapToDouble(ProcurementItem::getValueWithoutTVA)
                    .sum();

            double scaledValue = rangeValue / scaleFactor;

            dataset.addValue(count, "Count", ranges[i]);
            dataset.addValue(scaledValue, "Total Value (RON ÷ 10,000)", ranges[i]);
        }

        JFreeChart chart = ChartFactory.createStackedBarChart(
                "Procurement Analysis by Value Range",
                "Value Range (RON)", "Count / Scaled Value",
                dataset, PlotOrientation.VERTICAL, true, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, getChartColors()[0]);
        renderer.setSeriesPaint(1, getChartColors()[1]);

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
            }
        }

        return null;
    }

    private static String getShortCategoryName(String category, Map<String, CpvCode> cpvCodeMap) {
        if (category == null || category.isEmpty()) {
            return "Unknown";
        }

        if (category.equals("00")) {
            return "Uncategorized";
        }

        for (CpvCode cpvCode : cpvCodeMap.values()) {
            String code = cpvCode.getCode();
            if (code != null && code.startsWith(category)) {
                String romanianName = cpvCode.getRomanianName();
                if (romanianName != null && !romanianName.isEmpty()) {
                    String cleanName = romanianName;

                    if (cleanName.toLowerCase().startsWith("servicii de ")) {
                        cleanName = cleanName.substring(12);
                    } else if (cleanName.toLowerCase().startsWith("servicii privind ")) {
                        cleanName = cleanName.substring(16);
                    } else if (cleanName.toLowerCase().startsWith("servicii ")) {
                        cleanName = cleanName.substring(9);
                    }

                    if (cleanName.length() > 15) {
                        String[] words = cleanName.split("\\s+");
                        StringBuilder result = new StringBuilder();
                        int length = 0;

                        for (String word : words) {
                            if (length + word.length() + 1 > 15) break;
                            if (result.length() > 0) result.append(" ");
                            result.append(word);
                            length += word.length() + 1;
                        }

                        return result.length() > 0 ? result.toString() : cleanName.substring(0, 12) + "...";
                    }

                    return cleanName;
                }
            }
        }

        return "Cat. " + category;
    }

    private static String truncateNameIntelligently(String name, int maxLength) {
        if (name == null || name.isEmpty()) {
            return "Unnamed Item";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        String[] words = name.split("\\s+");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (result.length() + word.length() + 1 > maxLength - 3) {
                break;
            }
            if (result.length() > 0) {
                result.append(" ");
            }
            result.append(word);
        }

        if (result.length() == 0) {
            return name.substring(0, maxLength - 3) + "...";
        }

        result.append("...");
        return result.toString();
    }

    private static void customizePieChart(JFreeChart chart) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            chart.getLegend().setItemPaint(ThemeManager.getCurrentTheme().getTextColor());
            chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 11));

            try {
                Class<?> edgeClass = Class.forName("org.jfree.chart.ui.RectangleEdge");
                Object bottom = edgeClass.getField("BOTTOM").get(null);
                chart.getLegend().getClass().getMethod("setPosition", edgeClass).invoke(chart.getLegend(), bottom);
            } catch (Exception e1) {
                try {
                    Class<?> edgeClass = Class.forName("org.jfree.ui.RectangleEdge");
                    Object bottom = edgeClass.getField("BOTTOM").get(null);
                    chart.getLegend().getClass().getMethod("setPosition", edgeClass).invoke(chart.getLegend(), bottom);
                } catch (Exception e2) {
                    System.out.println("Could not set legend position: " + e2.getMessage());
                }
            }

        }

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setOutlineVisible(false);
        plot.setLabelFont(new Font("Arial", Font.PLAIN, 10));
        plot.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        plot.setLabelBackgroundPaint(new Color(255, 255, 255, 180));
        plot.setLabelOutlinePaint(ThemeManager.getCurrentTheme().getBorderColor());
        plot.setShadowPaint(null);
        plot.setSimpleLabels(true);

        plot.setLabelGap(0.02);
        plot.setMaximumLabelWidth(0.25);
        plot.setLabelLinkMargin(0.05);
        plot.setInteriorGap(0.04);

        plot.setLabelLinkStyle(org.jfree.chart.plot.PieLabelLinkStyle.STANDARD);
    }

    private static void customizeBarChart(JFreeChart chart) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            chart.getLegend().setItemPaint(ThemeManager.getCurrentTheme().getTextColor());
            chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 11));
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setRangeGridlinePaint(new Color(200, 200, 200, 80));
        plot.setOutlineVisible(false);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        domainAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        domainAxis.setMaximumCategoryLabelWidthRatio(0.9f);

        if (PlotOrientation.VERTICAL.equals(plot.getOrientation())) {
            domainAxis.setCategoryLabelPositions(
                    org.jfree.chart.axis.CategoryLabelPositions.createUpRotationLabelPositions(Math.PI / 6.0));
        } else {
            domainAxis.setCategoryLabelPositions(
                    org.jfree.chart.axis.CategoryLabelPositions.STANDARD);
        }

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        rangeAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));
    }

    private static void customizeLineChart(JFreeChart chart) {
        chart.setBackgroundPaint(ThemeManager.getCurrentTheme().getBackgroundColor());
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 16));
        chart.getTitle().setPaint(ThemeManager.getCurrentTheme().getTextColor());

        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
            chart.getLegend().setItemPaint(ThemeManager.getCurrentTheme().getTextColor());
            chart.getLegend().setItemFont(new Font("Arial", Font.PLAIN, 11));
        }

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(ThemeManager.getCurrentTheme().getPanelColor());
        plot.setRangeGridlinePaint(new Color(200, 200, 200, 80));
        plot.setDomainGridlinePaint(new Color(200, 200, 200, 60));
        plot.setOutlineVisible(false);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        domainAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        domainAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        rangeAxis.setLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 10));
        rangeAxis.setTickLabelPaint(ThemeManager.getCurrentTheme().getTextColor());
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));
    }

    public static JFreeChart generateCategoryRadarChart(DataModel model) {
        logger.info("Generating category radar chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        if (valueByCategory.isEmpty()) {
            dataset.setValue(0, "Analysis", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Category Analysis Overview", "Categories", "Relative Score",
                    dataset, PlotOrientation.VERTICAL, false, true, false);
            customizeBarChart(chart);
            return chart;
        }

        List<Map.Entry<String, Double>> topCategories = valueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(6)
                .collect(Collectors.toList());

        double maxValue = topCategories.isEmpty() ? 1 : topCategories.get(0).getValue();

        for (Map.Entry<String, Double> entry : topCategories) {
            String categoryName = getShortCategoryName(entry.getKey(), model.getCpvCodeMap());
            double normalizedValue = (entry.getValue() / maxValue) * 100;
            dataset.setValue(normalizedValue, "Category Strength", categoryName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Category Analysis Overview",
                "Categories", "Relative Strength (%)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, getChartColors()[4]);

        return chart;
    }

    public static JFreeChart generateProcurementHeatmapChart(DataModel model) {
        logger.info("Generating procurement heatmap chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Map<String, List<ProcurementItem>> itemsByCategory = model.getProcurementItemsByCategory();

        if (itemsByCategory.isEmpty()) {
            dataset.setValue(0, "Intensity", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Procurement Intensity Heatmap", "Categories", "Intensity",
                    dataset, PlotOrientation.VERTICAL, false, true, false);
            customizeBarChart(chart);
            return chart;
        }

        Map<String, Double> intensityByCategory = new HashMap<>();
        double maxIntensity = 0;

        for (Map.Entry<String, List<ProcurementItem>> entry : itemsByCategory.entrySet()) {
            String category = entry.getKey();
            List<ProcurementItem> items = entry.getValue();

            double totalValue = items.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
            int itemCount = items.size();

            double intensity = Math.sqrt(totalValue * itemCount);
            intensityByCategory.put(category, intensity);
            maxIntensity = Math.max(maxIntensity, intensity);
        }

        List<Map.Entry<String, Double>> topIntensity = intensityByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(8)
                .collect(Collectors.toList());

        for (Map.Entry<String, Double> entry : topIntensity) {
            String categoryName = getShortCategoryName(entry.getKey(), model.getCpvCodeMap());
            double normalizedIntensity = (entry.getValue() / maxIntensity) * 100;
            dataset.setValue(normalizedIntensity, "Procurement Intensity", categoryName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Intensity by Category",
                "Categories", "Intensity (%)",
                dataset, PlotOrientation.HORIZONTAL, false, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();

        for (int i = 0; i < topIntensity.size(); i++) {
            float intensity = (float) (topIntensity.get(i).getValue() / maxIntensity);
            Color color = new Color(intensity, 0.2f, 1.0f - intensity, 0.8f);
            renderer.setSeriesPaint(0, color);
        }

        return chart;
    }

    public static JFreeChart generateTimeDimensionChart(DataModel model) {
        logger.info("Generating time dimension chart");

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            dataset.setValue(0, "Value", "No Data");
            JFreeChart chart = ChartFactory.createBarChart(
                    "Quarterly Procurement Distribution", "Quarter", "Value (1000s RON)",
                    dataset, PlotOrientation.VERTICAL, false, true, false);
            customizeBarChart(chart);
            return chart;
        }

        String[] quarters = {"Q1 (Jan-Mar)", "Q2 (Apr-Jun)", "Q3 (Jul-Sep)", "Q4 (Oct-Dec)"};
        String[] quarterKeys = {"Q1", "Q2", "Q3", "Q4"};
        Map<String, Double> valueByQuarter = new LinkedHashMap<>();

        for (String quarter : quarterKeys) {
            valueByQuarter.put(quarter, 0.0);
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
            }
        }

        for (int i = 0; i < quarters.length; i++) {
            double value = valueByQuarter.get(quarterKeys[i]) / 1000;
            dataset.setValue(value, "Value (1000s RON)", quarters[i]);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Quarterly Procurement Distribution",
                "Quarter", "Value (1000s RON)",
                dataset, PlotOrientation.VERTICAL, false, true, false);

        customizeBarChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, getChartColors()[3]);

        return chart;
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
}