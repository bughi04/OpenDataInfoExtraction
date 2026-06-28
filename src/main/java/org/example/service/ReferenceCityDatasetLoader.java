package org.example.service;

import com.opencsv.CSVReader;
import org.example.model.ReferenceCityRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Loads the full 62-city reference dataset from Results_Cities.csv
public class ReferenceCityDatasetLoader {
    private static final Logger logger = LoggerFactory.getLogger(ReferenceCityDatasetLoader.class);
    private final List<ReferenceCityRecord> records = new ArrayList<>();
    private final Map<String, Double> gdpByCityKey = new HashMap<>();
    private boolean loaded;
    public ReferenceCityDatasetLoader(File csvFile) {
        if (csvFile != null && csvFile.isFile()) {
            load(csvFile);
        }
    }
    public boolean isLoaded() { return loaded; }
    public int size() { return records.size(); }
    public List<ReferenceCityRecord> getRecords() {
        return Collections.unmodifiableList(records);
    }
    public Double gdpForCityName(String cityOrFilename) {
        if (cityOrFilename == null) return null;
        String key = CityScoresLoader.normalise(
                cityOrFilename.replace(".graphml", ""));
        return gdpByCityKey.get(key);
    }
    public double medianGdpPerCapita() {
        double[] values = records.stream()
                .mapToDouble(ReferenceCityRecord::getGdpPerCapita)
                .filter(v -> !Double.isNaN(v) && !Double.isInfinite(v))
                .sorted()
                .toArray();
        if (values.length == 0) return Double.NaN;
        return values[values.length / 2];
    }
    private void load(File file) {
        try (CSVReader reader = new CSVReader(
                new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null) {
                logger.warn("Empty reference CSV: {}", file.getName());
                return;
            }
            int idxAddress = indexOf(headers, "City, address");
            int idxUmri = indexOf(headers, "UMR Index (Scr)");
            int idxSmi = indexOf(headers, "Sustainable Mobility (Scr)");
            int idxPti = indexOf(headers, "Public Transit (Scr)");
            int idxGdp = indexOf(headers, "GDP per capita ($)");
            int idxClustering = indexOf(headers, "Clustering Coefficient");
            int idxAssort = indexOf(headers, "Assortativity Degree");
            int idxStreets = indexOf(headers, "streets_per_node_avg");
            int idxDiameter = indexOf(headers, "Graph Diameter");
            int idxDensity = indexOf(headers, "Graph Density");
            int idxDegree = indexOf(headers, "Average Degree Centrality (Normalized)");
            int idxPath = indexOf(headers, "Average Path Length");
            int idxMeanDeg = indexOf(headers, "Mean Degree");
            if (idxUmri < 0 || idxClustering < 0) {
                logger.warn("Reference CSV missing required columns in {}", file.getName());
                return;
            }
            String[] row;
            while ((row = reader.readNext()) != null) {
                try {
                    String address = text(row, idxAddress);
                    if (address.isBlank()) continue;
                    records.add(new ReferenceCityRecord(
                            address.trim(),
                            parse(row, idxUmri),
                            parse(row, idxSmi),
                            parse(row, idxPti),
                            parse(row, idxGdp),
                            parse(row, idxClustering),
                            parse(row, idxAssort),
                            parse(row, idxStreets),
                            parse(row, idxDiameter),
                            parse(row, idxDensity),
                            parse(row, idxDegree),
                            parse(row, idxPath),
                            parse(row, idxMeanDeg)
                    ));
                    gdpByCityKey.put(CityScoresLoader.normalise(address), parse(row, idxGdp));
                } catch (Exception ex) {
                    logger.debug("Skipping reference row: {}", ex.getMessage());
                }
            }
            loaded = !records.isEmpty();
            logger.info("ReferenceCityDatasetLoader: {} labelled cities from {}", records.size(), file.getName());
        } catch (Exception e) {
            logger.warn("Could not load reference dataset from {}: {}", file.getName(), e.getMessage());
        }
    }
    private static int indexOf(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }
    private static String text(String[] row, int idx) {
        return idx >= 0 && idx < row.length ? row[idx].trim() : "";
    }
    private static double parse(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return Double.NaN;
        try {
            return Double.parseDouble(row[idx].trim());
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }
}