package org.example.controller;

import org.example.model.CpvCode;
import org.example.model.DataModel;
import org.example.model.ProcurementItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.NumberFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisService.class);
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getCurrencyInstance(new Locale("ro", "RO"));

    static {
        CURRENCY_FORMAT.setMaximumFractionDigits(2);
    }
    public static String generateAnalysisReport(DataModel model, Map<String, CpvCode> cpvCodeMap) {
        StringBuilder report = new StringBuilder();

        report.append("==================================================\n");
        report.append("            PROCUREMENT DATA ANALYSIS              \n");
        report.append("==================================================\n\n");

        report.append(generateGeneralStatistics(model));

        report.append(generateCategoryAnalysis(model, cpvCodeMap));

        report.append(generateValueDistributionAnalysis(model));

        report.append(generateMonthlyDistributionAnalysis(model));

        report.append(generateTimeDistributionAnalysis(model));

        report.append(generateExtremeItemsAnalysis(model));

        report.append(generateFinancingSourceAnalysis(model));

        report.append(generateSeasonalAnalysis(model));

        report.append(generateRecommendations(model));

        return report.toString();
    }

    private static String generateMonthlyDistributionAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("4. MONTHLY DISTRIBUTION ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (!hasTimeData(model)) {
            analysis.append("Insufficient time data available for monthly analysis.\n");
            analysis.append("Consider adding initiation or completion dates to enable detailed time-based analysis.\n\n");
            return analysis.toString();
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

        double totalIdentifiedValue = valueByMonth.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalIdentifiedItems = countByMonth.values().stream().mapToInt(Integer::intValue).sum();

        if (totalIdentifiedValue == 0 || totalIdentifiedItems == 0) {
            analysis.append("Insufficient monthly time data available for analysis.\n\n");
            return analysis.toString();
        }

        analysis.append(String.format("%-8s %-8s %-12s %-15s %-12s\n",
                "Month", "Count", "% of Items", "Total Value", "% of Value"));
        analysis.append("---------------------------------------------------------------------\n");

        for (String month : months) {
            int count = countByMonth.get(month);
            double value = valueByMonth.get(month);

            double countPercentage = (count * 100.0) / totalIdentifiedItems;
            double valuePercentage = (value * 100.0) / totalIdentifiedValue;

            analysis.append(String.format("%-8s %-8d %-12.1f %-15s %-12.1f\n",
                    month, count, countPercentage,
                    String.format("%,.2f", value), valuePercentage));
        }

        analysis.append("\nMonthly Distribution Insights:\n");

        String peakMonth = valueByMonth.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        String lowMonth = valueByMonth.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        double peakValue = valueByMonth.get(peakMonth);
        double lowValue = valueByMonth.get(lowMonth);
        double peakPercentage = (peakValue * 100.0) / totalIdentifiedValue;
        double lowPercentage = (lowValue * 100.0) / totalIdentifiedValue;

        analysis.append(String.format("- Peak spending month: %s (%.1f%% of annual spending)\n",
                peakMonth, peakPercentage));
        analysis.append(String.format("- Lowest spending month: %s (%.1f%% of annual spending)\n",
                lowMonth, lowPercentage));

        double avgMonthlyValue = totalIdentifiedValue / 12;
        double variability = 0;
        int activeMonths = 0;

        for (double monthValue : valueByMonth.values()) {
            if (monthValue > 0) {
                variability += Math.pow(monthValue - avgMonthlyValue, 2);
                activeMonths++;
            }
        }

        if (activeMonths > 1) {
            double stdDev = Math.sqrt(variability / activeMonths);
            double coefficientOfVariation = (stdDev / avgMonthlyValue) * 100;

            analysis.append(String.format("- Monthly spending variability: %.1f%% coefficient of variation\n",
                    coefficientOfVariation));

            if (coefficientOfVariation > 50) {
                analysis.append("  HIGH variability - procurement spending is uneven across months\n");
            } else if (coefficientOfVariation > 25) {
                analysis.append("  MODERATE variability - some seasonal fluctuation in spending\n");
            } else {
                analysis.append("  LOW variability - relatively consistent monthly spending\n");
            }
        }

        analysis.append("\nMonthly Efficiency Analysis:\n");

        Map<String, Double> avgValuePerItem = new HashMap<>();
        for (String month : months) {
            int count = countByMonth.get(month);
            double value = valueByMonth.get(month);

            if (count > 0) {
                avgValuePerItem.put(month, value / count);
            }
        }

        String mostEfficientMonth = avgValuePerItem.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        String leastEfficientMonth = avgValuePerItem.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        if (!mostEfficientMonth.equals("Unknown") && !leastEfficientMonth.equals("Unknown")) {
            analysis.append(String.format("- Highest average value per item: %s (%,.2f RON/item)\n",
                    mostEfficientMonth, avgValuePerItem.get(mostEfficientMonth)));
            analysis.append(String.format("- Lowest average value per item: %s (%,.2f RON/item)\n",
                    leastEfficientMonth, avgValuePerItem.get(leastEfficientMonth)));
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateSeasonalAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("7. SEASONAL ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        if (!hasTimeData(model)) {
            analysis.append("Insufficient time data available for seasonal analysis.\n\n");
            return analysis.toString();
        }

        Map<String, String[]> seasons = new LinkedHashMap<>();
        seasons.put("Spring", new String[]{"Mar", "Apr", "May"});
        seasons.put("Summer", new String[]{"Jun", "Jul", "Aug"});
        seasons.put("Autumn", new String[]{"Sep", "Oct", "Nov"});
        seasons.put("Winter", new String[]{"Dec", "Jan", "Feb"});

        Map<String, Double> valueByMonth = new HashMap<>();
        Map<String, Integer> countByMonth = new HashMap<>();
        String[] allMonths = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (String month : allMonths) {
            valueByMonth.put(month, 0.0);
            countByMonth.put(month, 0);
        }

        for (ProcurementItem item : model.getProcurementItems()) {
            String date = item.getInitiationDate();
            if (date == null || date.isEmpty()) {
                date = item.getCompletionDate();
            }

            if (date != null) {
                String month = extractMonthFromDate(date);
                if (month != null) {
                    valueByMonth.put(month, valueByMonth.get(month) + item.getValueWithoutTVA());
                    countByMonth.put(month, countByMonth.get(month) + 1);
                }
            }
        }

        Map<String, Double> valueBySeasonItem = new LinkedHashMap<>();
        Map<String, Integer> countBySeason = new LinkedHashMap<>();

        for (Map.Entry<String, String[]> seasonEntry : seasons.entrySet()) {
            String season = seasonEntry.getKey();
            String[] seasonMonths = seasonEntry.getValue();

            double seasonalValue = 0.0;
            int seasonalCount = 0;

            for (String month : seasonMonths) {
                seasonalValue += valueByMonth.get(month);
                seasonalCount += countByMonth.get(month);
            }

            valueBySeasonItem.put(season, seasonalValue);
            countBySeason.put(season, seasonalCount);
        }

        double totalSeasonalValue = valueBySeasonItem.values().stream().mapToDouble(Double::doubleValue).sum();
        int totalSeasonalCount = countBySeason.values().stream().mapToInt(Integer::intValue).sum();

        if (totalSeasonalValue == 0) {
            analysis.append("No seasonal data available for analysis.\n\n");
            return analysis.toString();
        }

        analysis.append(String.format("%-10s %-8s %-12s %-15s %-12s %-15s\n",
                "Season", "Count", "% of Items", "Total Value", "% of Value", "Avg/Month"));
        analysis.append("---------------------------------------------------------------------------------\n");

        for (Map.Entry<String, Double> entry : valueBySeasonItem.entrySet()) {
            String season = entry.getKey();
            double value = entry.getValue();
            int count = countBySeason.get(season);

            double countPercentage = totalSeasonalCount > 0 ? (count * 100.0) / totalSeasonalCount : 0;
            double valuePercentage = (value * 100.0) / totalSeasonalValue;
            double avgMonthlyValue = value / 3.0;

            analysis.append(String.format("%-10s %-8d %-12.1f %-15s %-12.1f %-15s\n",
                    season, count, countPercentage,
                    String.format("%,.2f", value), valuePercentage,
                    String.format("%,.2f", avgMonthlyValue)));
        }

        analysis.append("\nSeasonal Insights:\n");

        String peakSeason = valueBySeasonItem.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        String lowSeason = valueBySeasonItem.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");

        double peakSeasonValue = valueBySeasonItem.get(peakSeason);
        double peakSeasonPercentage = (peakSeasonValue * 100.0) / totalSeasonalValue;

        analysis.append(String.format("- Peak procurement season: %s (%.1f%% of annual value)\n",
                peakSeason, peakSeasonPercentage));

        if (!lowSeason.equals("Unknown")) {
            double lowSeasonValue = valueBySeasonItem.get(lowSeason);
            double lowSeasonPercentage = (lowSeasonValue * 100.0) / totalSeasonalValue;
            analysis.append(String.format("- Lowest procurement season: %s (%.1f%% of annual value)\n",
                    lowSeason, lowSeasonPercentage));
        }

        double seasonalVariance = 0;
        double avgSeasonalValue = totalSeasonalValue / 4.0;

        for (double seasonValue : valueBySeasonItem.values()) {
            seasonalVariance += Math.pow(seasonValue - avgSeasonalValue, 2);
        }

        double seasonalStdDev = Math.sqrt(seasonalVariance / 4.0);
        double seasonalCV = (seasonalStdDev / avgSeasonalValue) * 100;

        analysis.append(String.format("- Seasonal variability: %.1f%% coefficient of variation\n", seasonalCV));

        if (seasonalCV < 15) {
            analysis.append("  EXCELLENT seasonal balance - procurement is well distributed across seasons\n");
        } else if (seasonalCV < 30) {
            analysis.append("  GOOD seasonal balance - minor seasonal variations\n");
        } else if (seasonalCV < 50) {
            analysis.append("  MODERATE seasonal imbalance - some seasons significantly busier\n");
        } else {
            analysis.append("  HIGH seasonal imbalance - consider redistributing procurement timing\n");
        }

        analysis.append("\nSeasonal Efficiency Analysis:\n");

        for (Map.Entry<String, Double> entry : valueBySeasonItem.entrySet()) {
            String season = entry.getKey();
            double value = entry.getValue();
            int count = countBySeason.get(season);

            if (count > 0) {
                double avgValuePerItem = value / count;
                analysis.append(String.format("- %s: %,.2f RON average per item\n", season, avgValuePerItem));
            }
        }

        analysis.append("\n");
        return analysis.toString();
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

    private static String generateGeneralStatistics(DataModel model) {
        StringBuilder stats = new StringBuilder();

        stats.append("1. GENERAL STATISTICS\n");
        stats.append("--------------------------------------------------\n");

        int totalItems = model.getProcurementItems().size();
        stats.append("Total procurement items: ").append(totalItems).append("\n");

        double totalWithoutTVA = model.getTotalValueWithoutTVA();
        double totalWithTVA = model.getTotalValueWithTVA();

        stats.append("Total value (without TVA): ").append(String.format("%,.2f", totalWithoutTVA)).append(" RON\n");
        stats.append("Total value (with TVA): ").append(String.format("%,.2f", totalWithTVA)).append(" RON\n");

        double totalTVA = totalWithTVA - totalWithoutTVA;
        stats.append("Total TVA amount: ").append(String.format("%,.2f", totalTVA)).append(" RON\n");

        if (totalWithoutTVA > 0 && totalWithTVA > 0) {
            double avgTVAPercentage = (totalTVA / totalWithoutTVA) * 100;
            stats.append("Effective TVA rate: ").append(String.format("%.2f", avgTVAPercentage)).append("%\n");
        }

        Map<String, List<ProcurementItem>> itemsByCategory = model.getProcurementItemsByCategory();
        stats.append("Number of CPV categories: ").append(itemsByCategory.size()).append("\n");

        double avgValue = totalItems > 0 ? totalWithoutTVA / totalItems : 0;
        stats.append("Average value per item: ").append(String.format("%,.2f", avgValue)).append(" RON\n");

        stats.append("\n");
        return stats.toString();
    }

    private static String generateCategoryAnalysis(DataModel model, Map<String, CpvCode> cpvCodeMap) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("2. CATEGORY ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        Map<String, Double> valueByCategory = model.getValueByCpvCategory();

        if (valueByCategory.isEmpty()) {
            analysis.append("No category data available.\n\n");
            return analysis.toString();
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(valueByCategory.entrySet());
        sortedCategories.sort(Map.Entry.<String, Double>comparingByValue().reversed());

        double totalValue = model.getTotalValueWithoutTVA();

        analysis.append("Top CPV Categories by Value:\n\n");

        int topCategoriesLimit = 10;
        for (int i = 0; i < Math.min(topCategoriesLimit, sortedCategories.size()); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            String category = entry.getKey();
            Double value = entry.getValue();

            if (value <= 0) continue;

            String categoryName = getCategoryName(category, cpvCodeMap);

            double percentage = totalValue > 0 ? (value * 100 / totalValue) : 0;

            analysis.append(String.format("%d. %s (%s): %,.2f RON (%.2f%%)\n",
                    i + 1, categoryName, category, value, percentage));
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateValueDistributionAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("3. VALUE DISTRIBUTION ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        Map<String, List<ProcurementItem>> itemsByRange = model.getProcurementItemsByValueRange();

        if (itemsByRange.isEmpty()) {
            analysis.append("No value distribution data available.\n\n");
            return analysis.toString();
        }

        String[] ranges = {"0-10,000", "10,000-50,000", "50,000-100,000", "100,000+"};

        double totalValue = model.getTotalValueWithoutTVA();
        int totalCount = model.getProcurementItems().size();

        analysis.append(String.format("%-15s %-10s %-15s %-15s %-15s\n",
                "Value Range", "Count", "% of Items", "Total Value", "% of Value"));
        analysis.append("------------------------------------------------------------------\n");

        for (String range : ranges) {
            List<ProcurementItem> items = itemsByRange.get(range);

            if (items == null) {
                items = new ArrayList<>();
            }

            int count = items.size();

            double rangeValue = 0;
            for (ProcurementItem item : items) {
                rangeValue += item.getValueWithoutTVA();
            }

            double countPercentage = totalCount > 0 ? (count * 100.0 / totalCount) : 0;
            double valuePercentage = totalValue > 0 ? (rangeValue * 100.0 / totalValue) : 0;

            analysis.append(String.format("%-15s %-10d %-15.2f %-15s %-15.2f\n",
                    range, count, countPercentage,
                    String.format("%,.2f RON", rangeValue), valuePercentage));
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateTimeDistributionAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("5. QUARTERLY DISTRIBUTION ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        if (!hasTimeData(model)) {
            analysis.append("No time distribution data available.\n\n");
            return analysis.toString();
        }

        Map<String, Double> valueByQuarter = new HashMap<>();
        valueByQuarter.put("Q1", 0.0);
        valueByQuarter.put("Q2", 0.0);
        valueByQuarter.put("Q3", 0.0);
        valueByQuarter.put("Q4", 0.0);

        for (ProcurementItem item : model.getProcurementItems()) {
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

        double totalValue = valueByQuarter.values().stream().mapToDouble(Double::doubleValue).sum();

        if (totalValue > 0) {
            analysis.append("Procurement Value by Quarter:\n\n");

            for (Map.Entry<String, Double> entry : valueByQuarter.entrySet()) {
                String quarter = entry.getKey();
                double value = entry.getValue();
                double percentage = totalValue > 0 ? (value * 100.0 / totalValue) : 0;

                if (value > 0) {
                    analysis.append(String.format("%-3s: %,.2f RON (%.2f%%)\n",
                            quarter, value, percentage));
                }
            }
        }

        analysis.append("\n");
        return analysis.toString();
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

    private static String generateExtremeItemsAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("6. NOTABLE PROCUREMENT ITEMS\n");
        analysis.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            analysis.append("No procurement items available for analysis.\n\n");
            return analysis.toString();
        }

        List<ProcurementItem> validItems = items.stream()
                .filter(item -> item.getValueWithoutTVA() > 0)
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            analysis.append("No items with positive values found.\n\n");
            return analysis.toString();
        }

        analysis.append("Top 5 Highest Value Items:\n\n");

        validItems.sort(Comparator.comparingDouble(ProcurementItem::getValueWithoutTVA).reversed());

        for (int i = 0; i < Math.min(5, validItems.size()); i++) {
            ProcurementItem item = validItems.get(i);
            analysis.append(String.format("%d. %s\n", i + 1, item.getObjectName()));
            analysis.append(String.format("   Value: %,.2f RON\n", item.getValueWithoutTVA()));

            if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                analysis.append(String.format("   CPV: %s\n", item.getCpvField()));
            }

            analysis.append("\n");
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateFinancingSourceAnalysis(DataModel model) {
        StringBuilder analysis = new StringBuilder();

        analysis.append("8. FINANCING SOURCE ANALYSIS\n");
        analysis.append("--------------------------------------------------\n");

        boolean hasSourceData = false;
        for (ProcurementItem item : model.getProcurementItems()) {
            if (item.getSource() != null && !item.getSource().isEmpty()) {
                hasSourceData = true;
                break;
            }
        }

        if (!hasSourceData) {
            analysis.append("No financing source data available.\n\n");
            return analysis.toString();
        }

        Map<String, List<ProcurementItem>> itemsBySource = new HashMap<>();

        for (ProcurementItem item : model.getProcurementItems()) {
            String source = item.getSource();

            if (source == null || source.isEmpty()) {
                source = "Unknown";
            }

            if (!itemsBySource.containsKey(source)) {
                itemsBySource.put(source, new ArrayList<>());
            }

            itemsBySource.get(source).add(item);
        }

        double totalValue = model.getTotalValueWithoutTVA();

        for (Map.Entry<String, List<ProcurementItem>> entry : itemsBySource.entrySet()) {
            String source = entry.getKey();
            List<ProcurementItem> sourceItems = entry.getValue();

            double sourceValue = sourceItems.stream().mapToDouble(ProcurementItem::getValueWithoutTVA).sum();
            double percentage = totalValue > 0 ? (sourceValue * 100.0) / totalValue : 0;

            analysis.append(String.format("%s: %,.2f RON (%.1f%%, %d items)\n",
                    source, sourceValue, percentage, sourceItems.size()));
        }

        analysis.append("\n");
        return analysis.toString();
    }

    private static String generateRecommendations(DataModel model) {
        StringBuilder recommendations = new StringBuilder();

        recommendations.append("9. STRATEGIC RECOMMENDATIONS\n");
        recommendations.append("--------------------------------------------------\n");

        List<ProcurementItem> items = model.getProcurementItems();

        if (items.isEmpty()) {
            recommendations.append("No procurement items available for generating recommendations.\n\n");
            return recommendations.toString();
        }

        if (hasTimeData(model)) {
            recommendations.append("Monthly Distribution Recommendations:\n");

            Map<String, Double> valueByMonth = new HashMap<>();
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

            for (String month : months) {
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

            String peakMonth = valueByMonth.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("Unknown");

            double totalMonthlyValue = valueByMonth.values().stream().mapToDouble(Double::doubleValue).sum();
            double peakPercentage = totalMonthlyValue > 0 ?
                    (valueByMonth.get(peakMonth) * 100.0) / totalMonthlyValue : 0;

            if (peakPercentage > 25) {
                recommendations.append(String.format("- Peak spending in %s (%.1f%% of annual procurement)\n",
                        peakMonth, peakPercentage));
                recommendations.append("  Consider distributing procurement more evenly across months\n");
                recommendations.append("  to reduce seasonal budget pressure and improve supplier capacity planning.\n\n");
            }
        }

        recommendations.append("General Procurement Excellence:\n");
        recommendations.append("- Implement category management approach for major spend areas\n");
        recommendations.append("- Develop strategic supplier relationships for critical items\n");
        recommendations.append("- Consider e-procurement tools to streamline processes\n");
        recommendations.append("- Enhance procurement data quality and analysis capabilities\n");
        recommendations.append("- Establish regular procurement performance reviews\n");

        return recommendations.toString();
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
                    return romanianName;
                }
            }
        }

        return "Category " + category;
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
}