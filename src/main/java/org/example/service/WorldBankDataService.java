package org.example.service;

import org.example.model.CityLocation;
import org.example.model.NetworkMetrics;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.example.util.CityLocationParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// Enriches city networks with World Bank country-level indicators via Data360
public class WorldBankDataService {
    private static final Logger logger = LoggerFactory.getLogger(WorldBankDataService.class);
    private final Data360ApiClient api;
    private final Map<String, List<WorldBankIndicatorValue>> countryCache = new ConcurrentHashMap<>();
    public WorldBankDataService() {
        this(new Data360ApiClient());
    }
    public WorldBankDataService(Data360ApiClient api) {
        this.api = api;
    }
    public WorldBankCityData enrich(NetworkMetrics metrics) {
        CityLocation location = metrics.getGraphFile() != null
                ? CityLocationParser.parseFromFile(metrics.getGraphFile())
                : CityLocationParser.parse(metrics.getGraphName());
        WorldBankCityData data = fetchForLocation(location);
        metrics.setWorldBankData(data);
        return data;
    }
    public WorldBankCityData fetchForLocation(CityLocation location) {
        WorldBankCityData data = new WorldBankCityData(location);
        data.setFetchedAt(LocalDateTime.now());
        if (location == null || !location.hasCountryCode()) {
            data.setStatus(WorldBankCityData.FetchStatus.NO_COUNTRY);
            data.setStatusMessage(location != null && location.getCountry() != null
                    ? "Could not map country \"" + location.getCountry() + "\" to a World Bank code."
                    : "No country found in the GraphML filename. Expected format: City,_Country.graphml");
            return data;
        }
        String refArea = location.getRefAreaCode();
        List<WorldBankIndicatorValue> indicators = countryCache.computeIfAbsent(refArea, this::fetchCountryIndicators);
        data.setIndicators(new ArrayList<>(indicators));
        int withValues = data.countWithValues();
        int total = WorldBankIndicators.values().length;
        if (withValues == 0) {
            data.setStatus(WorldBankCityData.FetchStatus.FAILED);
            data.setStatusMessage("No indicator data returned for " + location.getCountry()
                    + " (" + refArea + "). Check your internet connection or try Refresh.");
        } else if (withValues < total) {
            data.setStatus(WorldBankCityData.FetchStatus.PARTIAL);
            data.setStatusMessage("Loaded " + withValues + " of " + total + " indicators for "
                    + location.getCountry() + " (" + refArea + ").");
        } else {
            data.setStatus(WorldBankCityData.FetchStatus.OK);
            data.setStatusMessage("Loaded " + withValues + " indicators for "
                    + location.getCountry() + " (" + refArea + ").");
        }
        logger.info("World Bank data for {}: {} indicators ({})",
                location.displayLabel(), withValues, data.getStatus());
        return data;
    }
    private List<WorldBankIndicatorValue> fetchCountryIndicators(String refArea) {
        List<WorldBankIndicatorValue> results = new ArrayList<>();
        for (WorldBankIndicators indicator : WorldBankIndicators.values()) {
            Optional<WorldBankIndicatorValue> value = api.fetchLatest(refArea, indicator);
            if (value.isPresent()) {
                results.add(value.get());
            } else {
                WorldBankIndicatorValue missing = new WorldBankIndicatorValue(
                        indicator.getId(),
                        indicator.getShortLabel(),
                        indicator.getDescription(),
                        null, null, null);
                results.add(missing);
            }
        }
        return results;
    }
    public void clearCaches() {
        countryCache.clear();
        api.clearCache();
    }
}