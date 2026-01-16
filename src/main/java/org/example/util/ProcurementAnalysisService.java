package org.example.util;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ProcurementAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(ProcurementAnalysisService.class);

    public static String generateComprehensiveAnalysis(DataModel model) {
        if (model == null || model.getProcurementItems().isEmpty()) {
            return "No procurement data available for analysis.";
        }

        StringBuilder report = new StringBuilder();

        report.append("==================================================\n");
        report.append("       COMPREHENSIVE PROCUREMENT DATA ANALYSIS     \n");
        report.append("==================================================\n\n");

        report.append(generateExecutiveSummary(model));
        report.append(generateGeneralStatistics(model));
        report.append(generateCategoryAnalysis(model));
        report.append(generateValueDistributionAnalysis(model));
        report.append(generateTimeAnalysis(model));
        report.append(generateTopItemsAnalysis(model));
        report.append(generateAnomalyDetection(model));
        report.append(generateStrategicRecommendations(model));

        return report.toString();
    }

    private static String generateExecutiveSummary(DataModel model) {
        StringBuilder summary = new StringBuilder();

        summary.append("1. EXECUTIVE SUMMARY\n");
        summary.append("--------------------------------------------------\n");

        int totalItems = model.getProcurementItems().size();
        double totalValue = model.getTotalValueWithoutTVA();
        int categoryCount = model.getProcurementItemsByCategory().size();

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();
        int highValueItems = itemsByRange.getOrDefault("100,000+", Collections.emptyList()).size();

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        List<Map.Entry<String, Double>> topCategories = valueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(3)
                .collect(Collectors.toList());

        summary.append(String.format("This analysis covers %d procurement items with a total value of %,.2f RON across %d CPV categories.\n\n",
                totalItems, totalValue, categoryCount));

        summary.append("Key Findings:\n");

        if (!topCategories.isEmpty()) {
            double topCategoryValue = topCategories.get(0).getValue();
            double topCategoryPercentage = (topCategoryValue / totalValue) * 100;
            String topCategoryCode = topCategories.get(0).getKey();
            String topCategoryName = getCategoryName(topCategoryCode, model.getCpvCodeMap());

            summary.append(String.format("- The %s category (CPV %s) represents %.1f%% of total procurement value\n",
                    topCategoryName, topCategoryCode, topCategoryPercentage));

            double top3Value = topCategories.stream().mapToDouble(Map.Entry::getValue).sum();
            double top3Percentage = (top3Value / totalValue) * 100;

            summary.append(String.format("- Top 3 categories represent %.1f%% of total procurement value\n", top3Percentage));

            if (top3Percentage > 75) {
                summary.append("  (High concentration - consider diversifying procurement)\n");
            }
        }

        summary.append(String.format("- %d high-value items (>100,000 RON) representing ", highValueItems));

        double highValueTotal = itemsByRange.getOrDefault("100,000+", Collections.emptyList()).stream()
                .mapToDouble(ProcurementItem::getValueWithoutTVA)
                .sum();
        double highValuePercentage = (highValueTotal / totalValue) * 100;

        summary.append(String.format("%.1f%% of total value\n", highValuePercentage));

        if (hasTimeData(model)) {
            String peakPeriod = getTopQuarter(model);
            summary.append(String.format("- Procurement activity peaks in %s\n", peakPeriod));
        }

        summary.append("\nThis analysis identifies opportunities for improved procurement efficiency, ");
        summary.append("better category management, and potential cost savings. Detailed sections ");
        summary.append("below provide in-depth analysis and specific recommendations.\n\n");

        return summary.toString();
    }

    private static String generateGeneralStatistics(DataModel model) {
        StringBuilder stats = new StringBuilder();

        stats.append("2. GENERAL STATISTICS\n");
        stats.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        int totalItems = items.size();
        double totalValueNoTVA = model.getTotalValueWithoutTVA();
        double totalValueWithTVA = model.getTotalValueWithTVA();

        stats.append(String.format("Total procurement items: %d\n", totalItems));
        stats.append(String.format("Total value (without TVA): %,.2f RON\n", totalValueNoTVA));
        stats.append(String.format("Total value (with TVA): %,.2f RON\n", totalValueWithTVA));

        double tvaAmount = totalValueWithTVA - totalValueNoTVA;
        double tvaPercentage = totalValueNoTVA > 0 ? (tvaAmount / totalValueNoTVA) * 100 : 0;

        stats.append(String.format("Total TVA amount: %,.2f RON (%.1f%%)\n", tvaAmount, tvaPercentage));

        double avgValue = totalItems > 0 ? totalValueNoTVA / totalItems : 0;
        stats.append(String.format("Average value per item: %,.2f RON\n", avgValue));

        if (items.size() > 0) {
            List<Double> values = items.stream()
                    .map(ProcurementItem::getValueWithoutTVA)
                    .sorted()
                    .collect(Collectors.toList());

            double median;
            if (values.size() % 2 == 0) {
                median = (values.get(values.size() / 2 - 1) + values.get(values.size() / 2)) / 2;
            } else {
                median = values.get(values.size() / 2);
            }

            stats.append(String.format("Median value: %,.2f RON\n", median));

            double minValue = values.stream().filter(v -> v > 0).min(Double::compare).orElse(0.0);
            double maxValue = values.stream().max(Double::compare).orElse(0.0);

            stats.append(String.format("Value range: %,.2f RON to %,.2f RON\n", minValue, maxValue));
        }

        int distinctCpvCodes = countDistinctCpvCodes(items);
        stats.append(String.format("Distinct CPV codes used: %d\n", distinctCpvCodes));

        int categoryCount = model.getProcurementItemsByCategory().size();
        stats.append(String.format("CPV categories: %d\n", categoryCount));

        long itemsWithoutCpv = items.stream()
                .filter(item -> item.getCpvCodes().isEmpty())
                .count();

        if (itemsWithoutCpv > 0) {
            double percentageWithoutCpv = (itemsWithoutCpv * 100.0) / totalItems;
            stats.append(String.format("Items without CPV codes: %d (%.1f%%)\n", itemsWithoutCpv, percentageWithoutCpv));
        }

        stats.append("\n");
        return stats.toString();
    }

    private static String generateCategoryAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("3. CATEGORY ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        Map<String, List<ProcurementItem>> itemsByCategory = model.getProcurementItemsByCategory();

        if (valueByCategory.isEmpty()) {
            analysis.append("No category data available.\n\n");
            return analysis.toString();
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(valueByCategory.entrySet());
        sortedCategories.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        double totalValue = model.getTotalValueWithoutTVA();

        analysis.append("Top CPV Categories by Value:\n\n");

        for (int i = 0; i < Math.min(10, sortedCategories.size()); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            String category = entry.getKey();
            double value = entry.getValue();

            if (value <= 0) continue;

            double percentage = (value / totalValue) * 100;
            String categoryName = getCategoryName(category, model.getCpvCodeMap());
            int itemCount = itemsByCategory.getOrDefault(category, Collections.emptyList()).size();

            analysis.append(String.format("%d. %s (%s)\n", i + 1, categoryName, category));
            analysis.append(String.format("   Value: %,.2f RON (%.1f%%)\n", value, percentage));
            analysis.append(String.format("   Items: %d\n", itemCount));
            analysis.append(String.format("   Avg Value: %,.2f RON\n", value / itemCount));
            analysis.append("\n");
        }

        analysis.append("Category Concentration Analysis:\n");

        double top3Value = 0;
        double top5Value = 0;
        double top10Value = 0;

        for (int i = 0; i < Math.min(sortedCategories.size(), 10); i++) {
            double value = sortedCategories.get(i).getValue();

            if (i < 3) top3Value += value;
            if (i < 5) top5Value += value;
            top10Value += value;
        }

        double top3Percentage = (top3Value / totalValue) * 100;
        double top5Percentage = (top5Value / totalValue) * 100;
        double top10Percentage = (top10Value / totalValue) * 100;

        analysis.append(String.format("Top 3 categories: %.1f%% of total value\n", top3Percentage));
        analysis.append(String.format("Top 5 categories: %.1f%% of total value\n", top5Percentage));
        analysis.append(String.format("Top 10 categories: %.1f%% of total value\n", top10Percentage));

        if (top3Percentage > 75) {
            analysis.append("\nWARNING: High concentration in top 3 categories (>75%). ");
            analysis.append("This indicates significant spend concentration which may ");
            analysis.append("present risks in terms of supplier dependency and market leverage.\n");
        } else if (top3Percentage > 50) {
            analysis.append("\nNOTE: Moderate concentration in top 3 categories (>50%). ");
            analysis.append("Consider developing category-specific strategies for these key areas.\n");
        } else {
            analysis.append("\nLow concentration across categories. ");
            analysis.append("Procurement spending is well distributed.\n");
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateValueDistributionAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("4. VALUE DISTRIBUTION ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        if (itemsByRange.isEmpty()) {
            analysis.append("No value distribution data available.\n\n");
            return analysis.toString();
        }

        String[] ranges = {"0-10,000", "10,000-50,000", "50,000-100,000", "100,000+"};

        double totalValue = model.getTotalValueWithoutTVA();
        int totalItems = model.getProcurementItems().size();

        analysis.append(String.format("%-15s %-8s %-12s %-15s %-12s\n",
                "Range (RON)", "Count", "% of Items", "Total Value", "% of Value"));
        analysis.append("---------------------------------------------------------------------\n");

        for (String range : ranges) {
            List<ProcurementItem> items = itemsByRange.getOrDefault(range, Collections.emptyList());
            int count = items.size();

            double rangeValue = items.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
            double countPercentage = (count * 100.0) / totalItems;
            double valuePercentage = (rangeValue * 100.0) / totalValue;

            analysis.append(String.format("%-15s %-8d %-12.1f %-15s %-12.1f\n",
                    range, count, countPercentage,
                    String.format("%,.2f", rangeValue), valuePercentage));
        }

        analysis.append("\nValue Distribution Analysis:\n");

        List<ProcurementItem> lowValueItems = itemsByRange.getOrDefault("0-10,000", Collections.emptyList());
        double lowValuePercentage = (lowValueItems.size() * 100.0) / totalItems;
        double lowValueTotalPercentage = (lowValueItems.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum() * 100.0) / totalValue;

        analysis.append(String.format("- Low-value items (<10,000 RON): %.1f%% of items representing %.1f%% of total value\n",
                lowValuePercentage, lowValueTotalPercentage));

        List<ProcurementItem> highValueItems = itemsByRange.getOrDefault("100,000+", Collections.emptyList());
        double highValuePercentage = (highValueItems.size() * 100.0) / totalItems;
        double highValueTotalPercentage = (highValueItems.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum() * 100.0) / totalValue;

        analysis.append(String.format("- High-value items (>100,000 RON): %.1f%% of items representing %.1f%% of total value\n",
                highValuePercentage, highValueTotalPercentage));

        analysis.append("\nPareto Analysis (80/20 Rule):\n");

        List<ProcurementItem> sortedItems = new ArrayList<>(model.getProcurementItems());
        sortedItems.sort(Comparator.comparingDouble(ProcurementItem::getValueWithoutTVA).reversed());

        double cumulativeValue = 0;
        int itemsFor80Percent = 0;

        for (ProcurementItem item : sortedItems) {
            cumulativeValue += item.getValueWithoutTVA();
            itemsFor80Percent++;

            if (cumulativeValue >= totalValue * 0.8) {
                break;
            }
        }

        double percentOfItems = (itemsFor80Percent * 100.0) / totalItems;
        analysis.append(String.format("%.1f%% of items (top %d) account for 80%% of total procurement value\n",
                percentOfItems, itemsFor80Percent));

        if (percentOfItems < 20) {
            analysis.append("This indicates extreme concentration in a small number of high-value items.\n");
        } else if (percentOfItems < 30) {
            analysis.append("This follows the Pareto principle (80/20 rule) fairly closely.\n");
        } else {
            analysis.append("This indicates a more even distribution than the typical 80/20 rule.\n");
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateTimeAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("5. TIME DISTRIBUTION ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        if (!hasTimeData(model)) {
            analysis.append("Insufficient time data available for analysis.\n");
            analysis.append("Consider adding initiation or completion dates to enable time-based analysis.\n\n");
            return analysis.toString();
        }

        String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
        Map<String, Double> valueByQuarter = new HashMap<>();
        Map<String, Integer> countByQuarter = new HashMap<>();

        for (String quarter : quarters) {
            valueByQuarter.put(quarter, 0.0);
            countByQuarter.put(quarter, 0);
        }

        for (ProcurementItem item : model.getProcurementItems()) {
            String date = item.getInitiationDate();

            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date == null || date.isEmpty()) {
                continue;
            }

            String lowerDate = date.toLowerCase();
            String quarter = null;

            if (lowerDate.contains("q1") || lowerDate.contains("quarter 1") ||
                    lowerDate.contains("ian") || lowerDate.contains("feb") || lowerDate.contains("mar")) {
                quarter = "Q1";
            }
            else if (lowerDate.contains("q2") || lowerDate.contains("quarter 2") ||
                    lowerDate.contains("apr") || lowerDate.contains("mai") || lowerDate.contains("iun")) {
                quarter = "Q2";
            }
            else if (lowerDate.contains("q3") || lowerDate.contains("quarter 3") ||
                    lowerDate.contains("iul") || lowerDate.contains("aug") || lowerDate.contains("sep")) {
                quarter = "Q3";
            }
            else if (lowerDate.contains("q4") || lowerDate.contains("quarter 4") ||
                    lowerDate.contains("oct") || lowerDate.contains("noi") || lowerDate.contains("dec")) {
                quarter = "Q4";
            }

            if (quarter != null) {
                valueByQuarter.put(quarter, valueByQuarter.get(quarter) + item.getValueWithoutTVA());
                countByQuarter.put(quarter, countByQuarter.get(quarter) + 1);
            }
        }

        double totalIdentifiedValue = valueByQuarter.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalIdentifiedItems = countByQuarter.values().stream().mapToInt(Integer::intValue).sum();

        if (totalIdentifiedValue == 0 || totalIdentifiedItems == 0) {
            analysis.append("Insufficient time data available for analysis.\n");
            analysis.append("Consider adding initiation or completion dates to enable time-based analysis.\n\n");
            return analysis.toString();
        }

        analysis.append(String.format("%-10s %-8s %-12s %-15s %-12s\n",
                "Quarter", "Count", "% of Items", "Total Value", "% of Value"));
        analysis.append("---------------------------------------------------------------------\n");

        for (String quarter : quarters) {
            int count = countByQuarter.get(quarter);
            double value = valueByQuarter.get(quarter);

            double countPercentage = (count * 100.0) / totalIdentifiedItems;
            double valuePercentage = (value * 100.0) / totalIdentifiedValue;

            analysis.append(String.format("%-10s %-8d %-12.1f %-15s %-12.1f\n",
                    quarter, count, countPercentage,
                    String.format("%,.2f", value), valuePercentage));
        }

        analysis.append("\nTime Distribution Analysis:\n");

        String peakQuarter = getTopQuarter(model);
        double peakValue = valueByQuarter.get(peakQuarter);
        double peakPercentage = (peakValue * 100.0) / totalIdentifiedValue;

        analysis.append(String.format("- Peak spending occurs in %s (%.1f%% of annual spending)\n",
                peakQuarter, peakPercentage));

        if (peakPercentage > 50) {
            analysis.append("WARNING: Highly concentrated spending in a single quarter (>50%).\n");
            analysis.append("This may cause operational challenges and supplier capacity issues.\n");
            analysis.append("Consider distributing procurement more evenly throughout the year.\n");
        } else if (peakPercentage > 35) {
            analysis.append("MODERATE concentration of spending in one quarter (>35%).\n");
            analysis.append("Consider evaluating if this concentration is operationally necessary\n");
            analysis.append("or if better planning could distribute procurement more evenly.\n");
        } else {
            analysis.append("Spending is relatively evenly distributed across quarters.\n");
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateTopItemsAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("6. TOP PROCUREMENT ITEMS ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            analysis.append("No procurement items available for analysis.\n\n");
            return analysis.toString();
        }

        List<ProcurementItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparingDouble(ProcurementItem::getValueWithoutTVA).reversed());

        analysis.append("Top 10 Highest-Value Procurement Items:\n\n");

        double totalValue = model.getTotalValueWithoutTVA();

        for (int i = 0; i < Math.min(10, sortedItems.size()); i++) {
            ProcurementItem item = sortedItems.get(i);
            double value = item.getValueWithoutTVA();
            double percentage = (value * 100.0) / totalValue;

            analysis.append(String.format("%d. %s\n", i + 1, item.getObjectName()));
            analysis.append(String.format("   Value: %,.2f RON (%.2f%% of total)\n", value, percentage));

            if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                analysis.append(String.format("   CPV: %s\n", item.getCpvField()));
            }

            if (item.getInitiationDate() != null && !item.getInitiationDate().isEmpty()) {
                analysis.append(String.format("   Initiation: %s\n", item.getInitiationDate()));
            }

            if (item.getCompletionDate() != null && !item.getCompletionDate().isEmpty()) {
                analysis.append(String.format("   Completion: %s\n", item.getCompletionDate()));
            }

            analysis.append("\n");
        }

        double top10Value = 0;
        for (int i = 0; i < Math.min(10, sortedItems.size()); i++) {
            top10Value += sortedItems.get(i).getValueWithoutTVA();
        }

        double top10Percentage = (top10Value * 100.0) / totalValue;

        analysis.append(String.format("The top 10 items represent %.1f%% of total procurement value.\n", top10Percentage));

        if (top10Percentage > 50) {
            analysis.append("This high concentration suggests these items should be prioritized for strategic procurement focus.\n");
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateAnomalyDetection(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("7. ANOMALY DETECTION\n");
        analysis.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            analysis.append("No procurement items available for anomaly detection.\n\n");
            return analysis.toString();
        }

        double mean = items.stream()
                .mapToDouble(ProcurementItem::getValueWithoutTVA)
                .average()
                .orElse(0);

        double sumSquaredDiff = items.stream()
                .mapToDouble(item -> Math.pow(item.getValueWithoutTVA() - mean, 2))
                .sum();

        double stdDev = Math.sqrt(sumSquaredDiff / items.size());

        double outlierThreshold = mean + (2 * stdDev);
        List<ProcurementItem> outliers = items.stream()
                .filter(item -> item.getValueWithoutTVA() > outlierThreshold)
                .sorted(Comparator.comparingDouble(ProcurementItem::getValueWithoutTVA).reversed())
                .collect(Collectors.toList());

        analysis.append("Statistical Outlier Analysis:\n");
        analysis.append(String.format("- Average item value: %,.2f RON\n", mean));
        analysis.append(String.format("- Standard deviation: %,.2f RON\n", stdDev));
        analysis.append(String.format("- Outlier threshold: %,.2f RON (mean + 2 std dev)\n", outlierThreshold));
        analysis.append(String.format("- Outliers detected: %d items\n\n", outliers.size()));

        if (!outliers.isEmpty()) {
            analysis.append("Top Outliers (Statistically Unusual High-Value Items):\n\n");

            for (int i = 0; i < Math.min(5, outliers.size()); i++) {
                ProcurementItem item = outliers.get(i);
                double value = item.getValueWithoutTVA();
                double deviation = (value - mean) / stdDev;

                analysis.append(String.format("%d. %s\n", i + 1, item.getObjectName()));
                analysis.append(String.format("   Value: %,.2f RON (%.1f standard deviations above mean)\n",
                        value, deviation));

                if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                    analysis.append(String.format("   CPV: %s\n", item.getCpvField()));
                }

                analysis.append("\n");
            }
        }

        analysis.append("Category Concentration Anomalies:\n");

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        double totalValue = model.getTotalValueWithoutTVA();

        List<Map.Entry<String, Double>> highConcentrationCategories = valueByCategory.entrySet().stream()
                .filter(e -> (e.getValue() / totalValue) > 0.25)
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        if (!highConcentrationCategories.isEmpty()) {
            for (Map.Entry<String, Double> entry : highConcentrationCategories) {
                String category = entry.getKey();
                double value = entry.getValue();
                double percentage = (value / totalValue) * 100;

                String categoryName = getCategoryName(category, model.getCpvCodeMap());

                analysis.append(String.format("- %s (%s): %.1f%% of total value\n",
                        categoryName, category, percentage));
            }

            if (highConcentrationCategories.size() == 1 && highConcentrationCategories.get(0).getValue() / totalValue > 0.5) {
                analysis.append("\nWARNING: Extreme concentration in a single category (>50% of total value).\n");
                analysis.append("This may indicate excessive reliance on a single type of procurement.\n");
            }
        } else {
            analysis.append("No significant category concentration anomalies detected.\n");
        }

        if (hasTimeData(model)) {
            analysis.append("\nTime Distribution Anomalies:\n");

            String peakQuarter = getTopQuarter(model);
            Map<String, Double> valueByQuarter = getValueByQuarter(model);
            double peakValue = valueByQuarter.get(peakQuarter);
            double totalQuarterlyValue = valueByQuarter.values().stream().mapToDouble(Double::doubleValue).sum();
            double peakPercentage = (peakValue / totalQuarterlyValue) * 100;

            if (peakPercentage > 50) {
                analysis.append(String.format("- %s contains %.1f%% of annual procurement value\n",
                        peakQuarter, peakPercentage));
                analysis.append("  This extreme quarterly concentration could cause operational challenges\n");
                analysis.append("  and may indicate poor procurement planning or end-of-budget spending.\n");
            } else if (peakPercentage > 35) {
                analysis.append(String.format("- %s contains %.1f%% of annual procurement value\n",
                        peakQuarter, peakPercentage));
                analysis.append("  This moderate concentration may warrant review of procurement timing.\n");
            } else {
                analysis.append("No significant time distribution anomalies detected.\n");
            }
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateStrategicRecommendations(DataModel model) {
        StringBuilder recommendations = new StringBuilder();

        recommendations.append("8. STRATEGIC RECOMMENDATIONS\n");
        recommendations.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            recommendations.append("No procurement items available for generating recommendations.\n\n");
            return recommendations.toString();
        }

        double totalValue = model.getTotalValueWithoutTVA();
        Map<String, Double> valueByCategory = model.getValueByCpvCategory();
        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        List<Map.Entry<String, Double>> sortedCategories = valueByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        recommendations.append("Category Management Recommendations:\n\n");

        if (!sortedCategories.isEmpty()) {
            String topCategory = sortedCategories.get(0).getKey();
            double topCategoryValue = sortedCategories.get(0).getValue();
            double topCategoryPercentage = (topCategoryValue / totalValue) * 100;
            String topCategoryName = getCategoryName(topCategory, model.getCpvCodeMap());

            recommendations.append(String.format("1. %s Category (%.1f%% of spend):\n",
                    topCategoryName, topCategoryPercentage));

            if (topCategoryPercentage > 30) {
                recommendations.append("   - Develop a dedicated category strategy for this high-spend area\n");
                recommendations.append("   - Consider strategic supplier relationships or framework agreements\n");
                recommendations.append("   - Implement category-specific KPIs and spend controls\n");
            } else {
                recommendations.append("   - Review supplier base and consider consolidation opportunities\n");
                recommendations.append("   - Evaluate potential for volume discounts or longer-term contracts\n");
            }
            recommendations.append("\n");

            double top3Value = 0;
            for (int i = 0; i < Math.min(3, sortedCategories.size()); i++) {
                top3Value += sortedCategories.get(i).getValue();
            }
            double top3Percentage = (top3Value / totalValue) * 100;

            if (top3Percentage > 70) {
                recommendations.append("2. Category Concentration:\n");
                recommendations.append("   - High concentration detected in top 3 categories (>70%)\n");
                recommendations.append("   - Implement specialized procurement expertise for these categories\n");
                recommendations.append("   - Consider strategic procurement planning to diversify spending\n");
                recommendations.append("   - Evaluate market dynamics and supplier power in these categories\n");
                recommendations.append("\n");
            }
        }

        recommendations.append("Value-Based Procurement Recommendations:\n\n");

        List<ProcurementItem> highValueItems = itemsByRange.getOrDefault("100,000+", Collections.emptyList());
        double highValueTotal = highValueItems.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
        double highValuePercentage = (highValueTotal / totalValue) * 100;

        if (!highValueItems.isEmpty()) {
            recommendations.append(String.format("3. High-Value Items (>100,000 RON, %.1f%% of total spend):\n",
                    highValuePercentage));
            recommendations.append("   - Implement strategic sourcing processes for these items\n");
            recommendations.append("   - Require competitive bidding and detailed cost analysis\n");
            recommendations.append("   - Consider value engineering and specification optimization\n");
            recommendations.append("   - Use supplier performance metrics and regular reviews\n");
            recommendations.append("\n");
        }

        List<ProcurementItem> lowValueItems = itemsByRange.getOrDefault("0-10,000", Collections.emptyList());
        int lowValueCount = lowValueItems.size();
        double lowValuePercentage = (lowValueCount * 100.0) / items.size();

        if (lowValuePercentage > 50) {
            recommendations.append("4. Low-Value Items (<10,000 RON):\n");
            recommendations.append("   - These represent a high proportion of transactions (>50%)\n");
            recommendations.append("   - Implement procurement cards for very small purchases\n");
            recommendations.append("   - Consider catalog-based purchasing and framework agreements\n");
            recommendations.append("   - Automate approval processes for routine, low-value items\n");
            recommendations.append("   - Consolidate similar purchases to reduce transaction costs\n");
            recommendations.append("\n");
        }

        if (hasTimeData(model)) {
            String peakQuarter = getTopQuarter(model);
            Map<String, Double> valueByQuarter = getValueByQuarter(model);
            double peakValue = valueByQuarter.get(peakQuarter);
            double totalQuarterlyValue = valueByQuarter.values().stream().mapToDouble(Double::doubleValue).sum();
            double peakPercentage = (peakValue / totalQuarterlyValue) * 100;

            if (peakPercentage > 40) {
                recommendations.append("5. Time Distribution Optimization:\n");
                recommendations.append(String.format("   - Current peak spending in %s (%.1f%% of annual spend)\n",
                        peakQuarter, peakPercentage));
                recommendations.append("   - Implement earlier procurement planning cycles\n");
                recommendations.append("   - Establish quarterly procurement plans with balanced distribution\n");
                recommendations.append("   - Consider framework agreements to smooth purchasing patterns\n");
                recommendations.append("   - Review budget allocation and spending approval processes\n");
                recommendations.append("\n");
            }
        }

        recommendations.append("Process Improvement Recommendations:\n\n");

        long itemsWithoutCpv = items.stream()
                .filter(item -> item.getCpvCodes().isEmpty())
                .count();
        double percentageWithoutCpv = (itemsWithoutCpv * 100.0) / items.size();

        if (percentageWithoutCpv > 10) {
            recommendations.append("6. CPV Code Usage:\n");
            recommendations.append(String.format("   - %.1f%% of items lack proper CPV codes\n", percentageWithoutCpv));
            recommendations.append("   - Improve CPV classification in procurement documentation\n");
            recommendations.append("   - Train procurement staff on CPV code usage\n");
            recommendations.append("   - Consider embedding CPV selection in procurement workflows\n");
            recommendations.append("\n");
        }

        recommendations.append("7. General Procurement Excellence:\n");
        recommendations.append("   - Implement category management approach for major spend areas\n");
        recommendations.append("   - Develop strategic supplier relationships for critical items\n");
        recommendations.append("   - Consider e-procurement tools to streamline processes\n");
        recommendations.append("   - Enhance procurement data quality and analysis capabilities\n");
        recommendations.append("   - Establish regular procurement performance reviews\n");

        return recommendations.toString();
    }

    private static String getCategoryName(String category, Map<String, CpvCode> cpvCodeMap) {
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
                    return romanianName;
                }
            }
        }

        return "Category " + category;
    }

    private static int countDistinctCpvCodes(List<ProcurementItem> items) {
        Set<String> distinctCodes = new HashSet<>();

        for (ProcurementItem item : items) {
            distinctCodes.addAll(item.getCpvCodes());
        }

        return distinctCodes.size();
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

    private static Map<String, Double> getValueByQuarter(DataModel model) {
        Map<String, Double> valueByQuarter = new HashMap<>();

        String[] quarters = {"Q1", "Q2", "Q3", "Q4"};
        for (String quarter : quarters) {
            valueByQuarter.put(quarter, 0.0);
        }

        for (ProcurementItem item : model.getProcurementItems()) {
            String date = item.getInitiationDate();

            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date == null || date.isEmpty()) {
                continue;
            }

            String lowerDate = date.toLowerCase();
            String quarter = null;

            if (lowerDate.contains("q1") || lowerDate.contains("quarter 1") ||
                    lowerDate.contains("ian") || lowerDate.contains("feb") || lowerDate.contains("mar")) {
                quarter = "Q1";
            }
            else if (lowerDate.contains("q2") || lowerDate.contains("quarter 2") ||
                    lowerDate.contains("apr") || lowerDate.contains("mai") || lowerDate.contains("iun")) {
                quarter = "Q2";
            }
            else if (lowerDate.contains("q3") || lowerDate.contains("quarter 3") ||
                    lowerDate.contains("iul") || lowerDate.contains("aug") || lowerDate.contains("sep")) {
                quarter = "Q3";
            }
            else if (lowerDate.contains("q4") || lowerDate.contains("quarter 4") ||
                    lowerDate.contains("oct") || lowerDate.contains("noi") || lowerDate.contains("dec")) {
                quarter = "Q4";
            }

            if (quarter != null) {
                valueByQuarter.put(quarter, valueByQuarter.get(quarter) + item.getValueWithoutTVA());
            }
        }

        return valueByQuarter;
    }

    private static String getTopQuarter(DataModel model) {
        Map<String, Double> valueByQuarter = getValueByQuarter(model);

        return valueByQuarter.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }
}