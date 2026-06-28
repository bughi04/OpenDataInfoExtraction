package org.example.service;

import com.opencsv.CSVReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

// Reads urban-mobility ranking scores from Results_Cities.csv
public class CityScoresLoader {
    private static final Logger logger = LoggerFactory.getLogger(CityScoresLoader.class);
    private final Map<String, double[]> scoresByKey  = new LinkedHashMap<>();
    private final Map<String, String>   displayNames = new LinkedHashMap<>();
    private boolean loaded = false;
    public CityScoresLoader(File csvFile) {
        if (csvFile != null && csvFile.exists()) {
            load(csvFile);
        }
    }
    // API
    public double[] getScores(String graphmlFilename) {
        if (!loaded || graphmlFilename == null) return null;
        String key = normalise(graphmlFilename.replace(".graphml", ""));
        return scoresByKey.get(key);
    }
    public boolean isLoaded() { return loaded; }
    public int size() { return scoresByKey.size(); }
    public Map<String, double[]> getAllScores() {
        return Collections.unmodifiableMap(scoresByKey);
    }
    // CSV parsing
    private void load(File f) {
        try (CSVReader r = new CSVReader(
                new InputStreamReader(Files.newInputStream(f.toPath()), StandardCharsets.UTF_8))) {

            String[] headers = r.readNext();
            if (headers == null) { logger.warn("Empty CSV: {}", f.getName()); return; }
            int idxCity    = indexOf(headers, "City");
            int idxAddress = indexOf(headers, "City, address");
            int idxRank    = indexOf(headers, "UMR Index (Abs)");
            int idxUMRi    = indexOf(headers, "UMR Index (Scr)");
            int idxSMi     = indexOf(headers, "Sustainable Mobility (Scr)");
            int idxPTi     = indexOf(headers, "Public Transit (Scr)");
            if (idxUMRi < 0) {
                logger.warn("Results CSV '{}' does not contain expected column 'UMR Index (Scr)'",
                        f.getName());
                return;
            }
            String[] row;
            while ((row = r.readNext()) != null) {
                if (row.length <= idxUMRi) continue;
                try {
                    String address = (idxAddress >= 0 && idxAddress < row.length)
                            ? row[idxAddress]
                            : (idxCity >= 0 && idxCity < row.length ? row[idxCity] : "");
                    String key = normalise(address);
                    if (key.isEmpty()) continue;
                    double[] scores = new double[]{
                            safeGet(row, idxRank),
                            safeGet(row, idxUMRi),
                            safeGet(row, idxSMi),
                            safeGet(row, idxPTi)
                    };
                    scoresByKey.put(key, scores);
                    displayNames.put(key, address.trim());
                } catch (Exception ex) {
                    logger.debug("Skipping malformed row: {}", Arrays.toString(row));
                }
            }
            loaded = !scoresByKey.isEmpty();
            logger.info("CityScoresLoader: loaded {} cities from {}", scoresByKey.size(), f.getName());
        } catch (Exception e) {
            logger.warn("Could not load city scores from '{}': {}", f.getName(), e.getMessage());
        }
    }
    public static String normalise(String s) {
        return s.replaceAll("[,_\\.\\-]", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }
    private static int indexOf(String[] arr, String target) {
        for (int i = 0; i < arr.length; i++)
            if (arr[i].trim().equalsIgnoreCase(target)) return i;
        return -1;
    }
    private static double safeGet(String[] row, int idx) {
        if (idx < 0 || idx >= row.length) return 0.0;
        try { return Double.parseDouble(row[idx].trim()); }
        catch (NumberFormatException e) { return 0.0; }
    }
}