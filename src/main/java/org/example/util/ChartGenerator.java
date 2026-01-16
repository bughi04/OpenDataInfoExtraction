package org.example.util;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.labels.StandardCategoryItemLabelGenerator;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.BasicStroke;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.HashMap;
import java.util.ArrayList;

public class ChartGenerator {
    private static final Logger logger = LoggerFactory.getLogger(ChartGenerator.class);
    private static final Color[] CHART_COLORS = {
            new Color(79, 129, 189),
            new Color(192, 80, 77),
            new Color(155, 187, 89),
            new Color(128, 100, 162),
            new Color(75, 172, 198),
            new Color(247, 150, 70),
            new Color(165, 165, 165),
            new Color(255, 192, 0)
    };

    public static JFreeChart generateProcurementByCategory(DataModel model, Map<String, CpvCode> cpvCodeMap) {
        logger.info("Generating procurement by CPV category chart");

        DefaultPieDataset dataset = new DefaultPieDataset();

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();

        if (valueByCategory.isEmpty()) {
            logger.warn("No procurement data available for category chart");
            dataset.setValue("No Data", 1.0);

            JFreeChart chart = ChartFactory.createPieChart(
                    "Procurement Value by CPV Category",
                    dataset,
                    true,
                    true,
                    false
            );

            chart.setBackgroundPaint(Color.WHITE);
            chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));

            PiePlot plot = (PiePlot) chart.getPlot();
            plot.setLabelFont(new Font("Arial", Font.PLAIN, 12));
            plot.setBackgroundPaint(Color.WHITE);
            plot.setOutlineVisible(false);
            plot.setShadowPaint(null);
            plot.setSimpleLabels(true);

            plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                    "{0} ({2})", NumberFormat.getInstance(), NumberFormat.getPercentInstance()
            ));

            plot.setSectionPaint("No Data", Color.LIGHT_GRAY);

            logger.info("Generated empty pie chart");
            return chart;
        }

        logger.info("Processing {} CPV categories", valueByCategory.size());

        ArrayList<CategoryValue> categoryValues = new ArrayList<>();

        for (Map.Entry<String, Double> entry : valueByCategory.entrySet()) {
            String category = entry.getKey();
            Double value = entry.getValue();
            if (value > 0) {
                categoryValues.add(new CategoryValue(category, value));
            }
        }

        for (int i = 0; i < categoryValues.size() - 1; i++) {
            for (int j = 0; j < categoryValues.size() - i - 1; j++) {
                if (categoryValues.get(j).value < categoryValues.get(j + 1).value) {
                    CategoryValue temp = categoryValues.get(j);
                    categoryValues.set(j, categoryValues.get(j + 1));
                    categoryValues.set(j + 1, temp);
                }
            }
        }

        int otherCount = 0;
        double otherValue = 0.0;
        int maxCategories = 10;

        for (int i = 0; i < categoryValues.size(); i++) {
            CategoryValue cv = categoryValues.get(i);

            if (i < maxCategories) {
                String categoryName = getCategoryName(cv.category, cpvCodeMap);
                String label = categoryName + " (" + cv.category + ")";

                logger.debug("Adding category: {}, value: {}", label, cv.value);
                dataset.setValue(label, cv.value);
            } else {
                otherCount++;
                otherValue += cv.value;
            }
        }

        if (otherCount > 0 && otherValue > 0) {
            dataset.setValue("Other (" + otherCount + " categories)", otherValue);
        }

        JFreeChart chart = ChartFactory.createPieChart(
                "Procurement Value by CPV Category",
                dataset,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));

        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.setBackgroundPaint(Color.WHITE);
        plot.setOutlineVisible(false);
        plot.setShadowPaint(null);
        plot.setSimpleLabels(true);

        plot.setLabelGenerator(new StandardPieSectionLabelGenerator(
                "{0} ({2})", NumberFormat.getInstance(), NumberFormat.getPercentInstance()
        ));

        int colorIndex = 0;

        for (int i = 0; i < Math.min(maxCategories, categoryValues.size()); i++) {
            CategoryValue cv = categoryValues.get(i);
            String categoryName = getCategoryName(cv.category, cpvCodeMap);
            String label = categoryName + " (" + cv.category + ")";

            plot.setSectionPaint(label, CHART_COLORS[colorIndex % CHART_COLORS.length]);
            colorIndex++;
        }

        if (otherCount > 0 && otherValue > 0) {
            plot.setSectionPaint("Other (" + otherCount + " categories)",
                    CHART_COLORS[colorIndex % CHART_COLORS.length]);
        }

        logger.info("Pie chart generated successfully with {} slices", dataset.getItemCount());
        return chart;
    }

    public static JFreeChart generateTopProcurementItems(DataModel model, int n) {
        logger.info("Generating top {} procurement items chart", n);
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        List<ProcurementItem> items = model.getProcurementItems();

        if (items == null || items.isEmpty()) {
            logger.warn("No procurement items available for top items chart");
            dataset.addValue(0, "No Data", "No Items");
        } else {
            List<ProcurementItem> validItems = new ArrayList<>();
            for (ProcurementItem item : items) {
                if (item.getValueWithoutTVA() > 0) {
                    validItems.add(item);
                }
            }

            if (validItems.isEmpty()) {
                logger.warn("No items with positive values found");
                dataset.addValue(0, "No Valid Data", "No Items with Values");
            } else {
                for (int i = 0; i < validItems.size() - 1; i++) {
                    for (int j = 0; j < validItems.size() - i - 1; j++) {
                        if (validItems.get(j).getValueWithoutTVA() < validItems.get(j + 1).getValueWithoutTVA()) {
                            ProcurementItem temp = validItems.get(j);
                            validItems.set(j, validItems.get(j + 1));
                            validItems.set(j + 1, temp);
                        }
                    }
                }

                List<ProcurementItem> topItems = new ArrayList<>();
                for (int i = 0; i < Math.min(n, validItems.size()); i++) {
                    topItems.add(validItems.get(i));
                }

                logger.info("Found {} top items with valid values", topItems.size());

                for (int i = topItems.size() - 1; i >= 0; i--) {
                    ProcurementItem item = topItems.get(i);
                    String name = truncateName(item.getObjectName(), 30);
                    double value = item.getValueWithoutTVA();

                    logger.debug("Adding item: {}, value: {}", name, value);
                    dataset.addValue(value, "Value", name);
                }
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Top " + n + " Procurement Items by Value",
                "Item",
                "Value (RON)",
                dataset,
                PlotOrientation.HORIZONTAL,
                false,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(200, 200, 200));
        plot.setRangeGridlinePaint(new Color(200, 200, 200));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        rangeAxis.setNumberFormatOverride(new DecimalFormat("#,##0"));

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, CHART_COLORS[0]);
        renderer.setShadowVisible(false);
        renderer.setDrawBarOutline(true);
        renderer.setDefaultOutlinePaint(new Color(100, 100, 100));
        renderer.setDefaultOutlineStroke(new BasicStroke(0.5f));

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator(
                "{2}", new DecimalFormat("#,##0 RON")));
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("Arial", Font.PLAIN, 10));

        logger.info("Top items chart generated successfully");
        return chart;
    }

    public static JFreeChart generateProcurementByValueRange(DataModel model) {
        logger.info("Generating procurement by value range chart");
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        if (itemsByRange.isEmpty() || model.getProcurementItems().isEmpty()) {
            logger.warn("No procurement data available for value range chart");
            dataset.addValue(0, "Count", "No Data");
            dataset.addValue(0, "Value (x10,000 RON)", "No Data");
        } else {
            logger.info("Processing {} value ranges", itemsByRange.size());

            String[] ranges = {"0-10,000", "10,000-50,000", "50,000-100,000", "100,000+"};

            double totalValue = model.getTotalValueWithoutTVA();

            int totalItemCount = model.getProcurementItems().size();

            for (int i = 0; i < ranges.length; i++) {
                String range = ranges[i];
                List<ProcurementItem> items = itemsByRange.get(range);

                if (items == null) {
                    items = new ArrayList<>();
                }

                int count = items.size();

                double rangeValue = 0.0;
                for (ProcurementItem item : items) {
                    rangeValue += item.getValueWithoutTVA();
                }

                double scaledValue = rangeValue / 10000;

                double countPercentage = totalItemCount > 0 ? (count * 100.0) / totalItemCount : 0;
                double valuePercentage = totalValue > 0 ? (rangeValue * 100.0) / totalValue : 0;

                logger.debug("Range: {}, Count: {} ({}%), Total Value: {} ({}%), Scaled Value: {}",
                        range, count, String.format("%.1f", countPercentage),
                        String.format("%,.2f", rangeValue), String.format("%.1f", valuePercentage),
                        String.format("%.2f", scaledValue));

                dataset.addValue(count, "Count", range);

                dataset.addValue(scaledValue, "Value (x10,000 RON)", range);
            }
        }

        JFreeChart chart = ChartFactory.createBarChart(
                "Procurement Items by Value Range",
                "Value Range (RON)",
                "Count / Value (x10,000 RON)",
                dataset,
                PlotOrientation.VERTICAL,
                true,
                true,
                false
        );

        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("Arial", Font.BOLD, 18));

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(new Color(220, 220, 220));
        plot.setRangeGridlinePaint(new Color(220, 220, 220));

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        domainAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelFont(new Font("Arial", Font.PLAIN, 11));
        rangeAxis.setLabelFont(new Font("Arial", Font.BOLD, 12));
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setSeriesPaint(0, CHART_COLORS[0]);
        renderer.setSeriesPaint(1, CHART_COLORS[1]);
        renderer.setShadowVisible(false);
        renderer.setItemMargin(0.1);

        renderer.setDefaultItemLabelGenerator(new StandardCategoryItemLabelGenerator());
        renderer.setDefaultItemLabelsVisible(true);
        renderer.setDefaultItemLabelFont(new Font("Arial", Font.PLAIN, 10));

        logger.info("Value range chart generated successfully");
        return chart;
    }

    private static String getCategoryName(String category, Map<String, CpvCode> cpvCodeMap) {
        if (category == null || category.isEmpty()) {
            return "Unknown";
        }

        for (CpvCode cpvCode : cpvCodeMap.values()) {
            String code = cpvCode.getCode();
            if (code != null && code.startsWith(category)) {
                String romanianName = cpvCode.getRomanianName();
                if (romanianName != null && !romanianName.isEmpty()) {
                    String[] words = romanianName.split("\\s+");

                    if (words.length > 0) {
                        if (words.length > 1 && words[0].length() + words[1].length() < 20) {
                            return words[0] + " " + words[1];
                        } else {
                            return words[0];
                        }
                    }
                    return romanianName;
                }
            }
        }

        return "Category " + category;
    }

    private static String truncateName(String name, int maxLength) {
        if (name == null || name.isEmpty()) {
            return "Unnamed Item";
        }

        if (name.length() <= maxLength) {
            return name;
        }

        return name.substring(0, maxLength - 3) + "...";
    }

    private static class CategoryValue {
        public String category;
        public double value;

        public CategoryValue(String category, double value) {
            this.category = category;
            this.value = value;
        }
    }
}