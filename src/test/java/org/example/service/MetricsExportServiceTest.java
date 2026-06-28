package org.example.service;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.example.model.NetworkMetrics;
import org.example.model.StreetNetworkStats;
import org.example.model.WorldBankCityData;
import org.example.model.WorldBankIndicatorValue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
// Verifies CSV export column layouts for metrics1, metrics2, and the combined all-metrics export, including World Bank fields.
class MetricsExportServiceTest {
    private final MetricsExportService exporter = new MetricsExportService();
    @Test
    void exportMetrics1WritesExpectedColumns(@TempDir Path tempDir) throws Exception {
        NetworkMetrics m = sampleMetrics();
        File out = tempDir.resolve("out.csv").toFile();
        exporter.exportMetrics1(m, out);
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(out)).build()) {
            String[] header = reader.readNext();
            String[] row = reader.readNext();
            assertNull(reader.readNext());

            assertEquals("Graph File", header[0]);
            assertEquals("Clustering Coefficient", header[7]);
            assertEquals(m.getGraphFile(), row[0]);
            assertEquals("0.065432", row[7]);
        }
    }
    @Test
    void exportMultipleMetricsIncludesWorldBankColumns(@TempDir Path tempDir) throws Exception {
        NetworkMetrics m = sampleMetrics();
        WorldBankCityData wb = new WorldBankCityData();
        wb.setRefAreaCode("NLD");
        wb.addIndicator(new WorldBankIndicatorValue(
                WorldBankIndicators.GDP_PER_CAPITA.getId(), "GDP", "USD", 52000.0, "2023", "USD"));
        wb.addIndicator(new WorldBankIndicatorValue(
                WorldBankIndicators.URBAN_POP_PCT.getId(), "Urban", "%", 92.0, "2023", "%"));
        m.setWorldBankData(wb);
        File out = tempDir.resolve("batch.csv").toFile();
        exporter.exportMultipleMetrics(List.of(m), out);
        try (CSVReader reader = new CSVReaderBuilder(new FileReader(out)).build()) {
            String[] header = reader.readNext();
            String[] row = reader.readNext();
            assertEquals("WB Country Code", header[19]);
            assertEquals("NLD", row[19]);
            assertEquals("52000.0000", row[20]);
            assertEquals("92.0000", row[21]);
        }
    }
    @Test
    void generateOutputFilenameStripsGraphmlExtension() {
        File input = new File("C:/graphs/Barcelona,_Spain.graphml");
        File out = exporter.generateOutputFilename(input, "_metrics");
        assertEquals("Barcelona,_Spain_metrics.csv", out.getName());
        assertEquals("C:/graphs", out.getParent().replace('\\', '/'));
    }
    private static NetworkMetrics sampleMetrics() {
        NetworkMetrics m = new NetworkMetrics("C:/graphs/Amsterdam,_Netherlands.graphml");
        m.setNodeCount(5000);
        m.setEdgeCount(12000);
        m.setDirected(false);
        m.setClusteringCoefficient(0.0654321);
        m.setGraphDensity(0.001234);
        m.setAvgBetweennessCentrality(0.12);
        m.setAvgClosenessCentrality(0.34);
        m.setAvgDegreeCentrality(0.56);
        m.setGraphEntropy(3.21);
        m.setGraphDiameter(42);
        m.setAvgPathLength(8.5);
        m.setConstraints(0.11);
        m.setAssortativityDegree(-0.05);
        m.setMeanDegree(2.4);
        m.setReciprocity(0.0);
        m.setDiversity(1.2);
        StreetNetworkStats street = new StreetNetworkStats();
        street.setTotalLengthMeters(750_000);
        street.setDominantHighwayType("residential");
        street.setOnewayRatio(0.35);
        street.setStreetsPerNode(2.8);
        m.setStreetStats(street);
        return m;
    }
}