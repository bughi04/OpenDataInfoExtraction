package org.example.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DataModel {
    private static final Logger logger = LoggerFactory.getLogger(DataModel.class);

    private List<ProcurementItem> procurementItems;
    private Map<String, CpvCode> cpvCodeMap;

    public DataModel() {
        procurementItems = new ArrayList<>();
        cpvCodeMap = new HashMap<>();
    }

    public void setProcurementItems(List<ProcurementItem> items) {
        this.procurementItems = new ArrayList<>(items);
        logger.info("Set {} procurement items in data model", items.size());
    }

    public List<ProcurementItem> getProcurementItems() {
        return new ArrayList<>(procurementItems);
    }

    public void setCpvCodes(Map<String, CpvCode> cpvCodes) {
        this.cpvCodeMap = new HashMap<>(cpvCodes);
        logger.info("Set {} CPV codes in data model", cpvCodes.size());
    }

    public Map<String, CpvCode> getCpvCodeMap() {
        return new HashMap<>(cpvCodeMap);
    }

    public CpvCode getCpvCodeByCode(String code) {
        return cpvCodeMap.get(code);
    }

    public List<ProcurementItem> searchProcurementItems(String query) {
        if (query == null || query.trim().isEmpty()) {
            logger.info("Empty search query, returning all {} items", procurementItems.size());
            return new ArrayList<>(procurementItems);
        }

        String searchQuery = query.toLowerCase();
        logger.info("Searching for '{}' in {} procurement items", searchQuery, procurementItems.size());

        List<ProcurementItem> results = procurementItems.stream()
                .filter(item ->
                        (item.getObjectName() != null && item.getObjectName().toLowerCase().contains(searchQuery)) ||
                                (item.getCpvField() != null && item.getCpvField().toLowerCase().contains(searchQuery)) ||
                                item.getCpvCodes().stream().anyMatch(code -> code.toLowerCase().contains(searchQuery)) ||
                                cpvCodeMatchesQuery(item, searchQuery)
                )
                .collect(Collectors.toList());

        logger.info("Found {} items matching '{}'", results.size(), searchQuery);
        return results;
    }

    private boolean cpvCodeMatchesQuery(ProcurementItem item, String query) {
        return item.getCpvCodes().stream()
                .anyMatch(code -> {
                    CpvCode cpvCode = cpvCodeMap.get(code);
                    if (cpvCode == null) {
                        return false;
                    }
                    return cpvCode.getRomanianName().toLowerCase().contains(query) ||
                            cpvCode.getEnglishName().toLowerCase().contains(query);
                });
    }

    public List<ProcurementItem> filterProcurementItems(Predicate<ProcurementItem> predicate) {
        return procurementItems.stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    public double getTotalValueWithoutTVA() {
        double total = procurementItems.stream()
                .mapToDouble(ProcurementItem::getValueWithoutTVA)
                .sum();
        logger.debug("Calculated total value without TVA: {}", total);
        return total;
    }

    public double getTotalValueWithTVA() {
        double total = procurementItems.stream()
                .mapToDouble(ProcurementItem::getValueWithTVA)
                .sum();
        logger.debug("Calculated total value with TVA: {}", total);
        return total;
    }

    public Map<String, List<ProcurementItem>> getProcurementItemsByCategory() {
        Map<String, List<ProcurementItem>> result = new HashMap<>();

        for (ProcurementItem item : procurementItems) {
            boolean categorized = false;

            for (String cpvCode : item.getCpvCodes()) {
                CpvCode code = cpvCodeMap.get(cpvCode);
                if (code != null) {
                    String category = code.getCategory();
                    if (category != null && !category.isEmpty()) {
                        result.computeIfAbsent(category, k -> new ArrayList<>()).add(item);
                        categorized = true;
                        break;
                    }
                }
            }

            if (!categorized && item.getObjectName() != null && !item.getObjectName().isEmpty()) {
                result.computeIfAbsent("00", k -> new ArrayList<>()).add(item);
            }
        }

        logger.info("Grouped procurement items into {} categories", result.size());
        return result;
    }

    public Map<String, List<ProcurementItem>> getProcurementItemsByValueRange() {
        Map<String, List<ProcurementItem>> result = new LinkedHashMap<>();

        result.put("0-10,000", filterProcurementItems(item -> item.getValueWithoutTVA() >= 0 && item.getValueWithoutTVA() < 10000));
        result.put("10,000-50,000", filterProcurementItems(item -> item.getValueWithoutTVA() >= 10000 && item.getValueWithoutTVA() < 50000));
        result.put("50,000-100,000", filterProcurementItems(item -> item.getValueWithoutTVA() >= 50000 && item.getValueWithoutTVA() < 100000));
        result.put("100,000+", filterProcurementItems(item -> item.getValueWithoutTVA() >= 100000));

        for (Map.Entry<String, List<ProcurementItem>> entry : result.entrySet()) {
            logger.debug("Value range {}: {} items", entry.getKey(), entry.getValue().size());
        }

        return result;
    }

    public Map<String, Double> getValueByCpvCategory() {
        Map<String, Double> result = new HashMap<>();

        Map<String, List<ProcurementItem>> itemsByCategory = getProcurementItemsByCategory();
        for (Map.Entry<String, List<ProcurementItem>> entry : itemsByCategory.entrySet()) {
            double totalValue = entry.getValue().stream()
                    .mapToDouble(ProcurementItem::getValueWithoutTVA)
                    .sum();

            if (totalValue > 0) {
                result.put(entry.getKey(), totalValue);
                logger.debug("Category {}: total value {}", entry.getKey(), totalValue);
            }
        }

        logger.info("Calculated values for {} CPV categories", result.size());
        return result;
    }

    public List<ProcurementItem> getTopProcurementItemsByValue(int n) {
        if (procurementItems.isEmpty()) {
            logger.warn("No procurement items available to get top items");
            return Collections.emptyList();
        }

        List<ProcurementItem> validItems = procurementItems.stream()
                .filter(item -> item.getValueWithoutTVA() > 0)
                .collect(Collectors.toList());

        if (validItems.isEmpty()) {
            logger.warn("No procurement items with positive values available");
            return Collections.emptyList();
        }

        List<ProcurementItem> topItems = validItems.stream()
                .sorted(Comparator.comparingDouble(ProcurementItem::getValueWithoutTVA).reversed())
                .limit(n)
                .collect(Collectors.toList());

        logger.info("Retrieved top {} procurement items by value", topItems.size());
        return topItems;
    }
}