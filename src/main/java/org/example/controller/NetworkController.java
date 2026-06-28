package org.example.controller;

import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.example.model.PredictionTarget;
import org.example.service.CityScoresLoader;
import org.example.service.GraphVisualizationLoader;
import org.example.service.GraphMLLoaderService;
import org.example.service.MetricsExportService;
import org.example.service.MobilityReadinessPredictor;
import org.example.service.MobilityReadinessPredictor.ModelType;
import org.example.service.NetworkAnalysisService;
import org.example.service.NetworkInsightFacade;
import org.example.service.ReferenceCityDatasetLoader;
import org.example.service.SessionPersistenceService;
import org.example.service.WorldBankDataService;
import org.example.util.NetworkAnalysisReportService;
import org.example.util.NetworkChartGenerator;
import org.example.view.AiAdvisorPanel;
import org.example.view.ModernNetworkView;
import org.jfree.chart.JFreeChart;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
// MVC controller for the GUI.
public class NetworkController {
    private static final Logger logger = LoggerFactory.getLogger(NetworkController.class);
    private final GraphMLLoaderService loader;
    private final NetworkAnalysisService analyser;
    private final MetricsExportService exporter;
    private final CityScoresLoader cityScores;
    private final ReferenceCityDatasetLoader referenceDataset;
    private final MobilityReadinessPredictor readinessPredictor;
    private final WorldBankDataService worldBankService;
    private final NetworkInsightFacade insightFacade;
    private final SessionPersistenceService sessionStore = new SessionPersistenceService();
    private final GraphVisualizationLoader mapLoader = new GraphVisualizationLoader();
    private final List<NetworkMetrics> loadedMetrics = new ArrayList<>();
    private File currentFolder = null;
    private volatile boolean shutDown = false;
    private final AtomicLong saveGeneration = new AtomicLong(0);
    private final ModernNetworkView view;
    private final ExecutorService pool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    public NetworkController(ModernNetworkView view) {
        this.view = view;
        this.loader = new GraphMLLoaderService();
        this.analyser = new NetworkAnalysisService();
        this.exporter = new MetricsExportService();
        this.cityScores = loadCityScores();
        this.referenceDataset = loadReferenceDataset();
        this.readinessPredictor = new MobilityReadinessPredictor(referenceDataset, cityScores);
        this.worldBankService = new WorldBankDataService();
        this.insightFacade = view.getAiAdvisorPanel().getInsightFacade();
        bindActions();
        bindAiAdvisor();
        bindMapTab();
        bindWorldBankTab();
        view.getPredictionPanel().setPredictor(readinessPredictor);
        restoreSession();
        refreshMapFileList();
        autoLoadDefaultFolder();
        logger.info("NetworkController initialised");
    }
    public void shutdown() {
        if (shutDown) return;
        shutDown = true;
        saveSession();
        analyser.shutdown();
        pool.shutdown();
        logger.info("NetworkController shut down");
    }
    private void bindActions() {
        view.setBrowseFolderAction(this::onBrowseFolder);
        view.setLoadFileAction(this::onLoadSingleFile);
        view.setLoadDirectoryAction(this::onBrowseFolder);
        view.setProcessAction(this::onProcessSelected);
        view.setGenerateChartsAction(this::onGenerateCharts);
        view.setGenerateAnalysisAction(this::onGenerateAnalysis);
        view.setExportAction(this::onExport);
    }
    private CityScoresLoader loadCityScores() {
        String[] candidates = {
                "DataSets/Results_Cities.csv",
                "DataSets\\Results_Cities.csv",
                "Results_Cities.csv"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.isFile()) {
                CityScoresLoader scoreLoader = new CityScoresLoader(f);
                if (scoreLoader.isLoaded()) {
                    logger.info("UMRi reference data loaded: {} cities", scoreLoader.size());
                    return scoreLoader;
                }
            }
        }
        logger.warn("Results_Cities.csv not found: UMRi comparison charts will be limited");
        return new CityScoresLoader(null);
    }
    private ReferenceCityDatasetLoader loadReferenceDataset() {
        String[] candidates = {
                "DataSets/Results_Cities.csv",
                "DataSets\\Results_Cities.csv",
                "Results_Cities.csv"
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.isFile()) {
                ReferenceCityDatasetLoader loader = new ReferenceCityDatasetLoader(f);
                if (loader.isLoaded()) {
                    logger.info("Reference training data loaded: {} cities", loader.size());
                    return loader;
                }
            }
        }
        logger.warn("Results_Cities.csv not found: prediction tab will be unavailable");
        return new ReferenceCityDatasetLoader(null);
    }
    private void bindAiAdvisor() {
        var panel = view.getAiAdvisorPanel();
        panel.setOverviewAction(q -> runInsight("Generating overview...",
                () -> insightFacade.generateOverview(loadedMetrics, cityScores)));
        panel.setBestWorstAction(q -> runInsight("Comparing best vs worst...",
                () -> insightFacade.generateBestVsWorst(loadedMetrics, cityScores)));
        panel.setAskAction(question -> runInsight("Thinking...",
                () -> insightFacade.ask(question, loadedMetrics, cityScores)));
        panel.setSettingsAction(() -> {
            AiAdvisorPanel.showGeminiSettingsDialog(view.getFrame(), insightFacade);
            panel.refreshStatusBadge();
        });
        panel.setClearChatAction(this::saveSession);
    }
    private void runInsight(String statusMsg, java.util.function.Supplier<String> task) {
        if (loadedMetrics.isEmpty()) {
            view.showErrorMessage("No metrics loaded. Process files first.");
            return;
        }
        view.getAiAdvisorPanel().setThinking(true);
        view.setStatusMessage(statusMsg);
        view.showAiAdvisorTab();
        pool.submit(() -> {
            try {
                String response = task.get();
                SwingUtilities.invokeLater(() -> {
                    view.getAiAdvisorPanel().appendAssistantMessage(response);
                    view.getAiAdvisorPanel().setThinking(false);
                    view.setStatusMessage("AI advisor response ready.");
                    saveSession();
                });
            } catch (NetworkInsightFacade.InsightException ex) {
                SwingUtilities.invokeLater(() -> {
                    view.getAiAdvisorPanel().appendAssistantMessage(ex.getMessage());
                    view.getAiAdvisorPanel().setThinking(false);
                    view.setStatusMessage("AI advisor needs configuration.");
                });
            }
        });
    }
    private void bindMapTab() {
        view.getGraphMapTabPanel().setLoadListener(file -> onLoadMap(file));
    }
    private void bindWorldBankTab() {
        view.getWorldBankDataPanel().setRefreshAction(this::onRefreshWorldBankData);
    }
    private void onRefreshWorldBankData() {
        if (loadedMetrics.isEmpty()) {
            view.showInfoMessage("No cities loaded. Process GraphML files first.");
            return;
        }
        view.setStatusMessage("Refreshing World Bank data from Data360 API...");
        worldBankService.clearCaches();
        List<NetworkMetrics> snapshot;
        synchronized (loadedMetrics) {
            snapshot = new ArrayList<>(loadedMetrics);
        }
        pool.submit(() -> {
            for (NetworkMetrics m : snapshot) {
                worldBankService.enrich(m);
            }
            SwingUtilities.invokeLater(() -> {
                view.updateWorldBankData(loadedMetrics);
                view.updatePredictionData(loadedMetrics);
                view.setStatusMessage("World Bank data refreshed for " + snapshot.size() + " city network(s).");
                saveSession();
            });
        });
    }
    private void enrichWorldBankData(NetworkMetrics metrics) {
        try {
            worldBankService.enrich(metrics);
        } catch (Exception ex) {
            logger.warn("World Bank enrichment failed for {}: {}", metrics.getGraphName(), ex.getMessage());
        }
    }
    private void refreshWorldBankUi() {
        SwingUtilities.invokeLater(() -> {
            view.updateWorldBankData(loadedMetrics);
            view.updatePredictionData(loadedMetrics);
        });
    }
    private void onLoadMap(File file) {
        view.getGraphMapTabPanel().setLoading(true);
        view.showMapTab();
        view.setStatusMessage("Loading map for " + file.getName() + "…");
        pool.submit(() -> {
            try {
                var data = mapLoader.load(file);
                SwingUtilities.invokeLater(() -> {
                    view.getGraphMapTabPanel().displayMap(data);
                    view.setStatusMessage("Map loaded: " + data.getSegments().size() + " street segments.");
                });
            } catch (Exception ex) {
                logger.error("Map load failed for {}: {}", file.getName(), ex.getMessage());
                SwingUtilities.invokeLater(() -> {
                    view.getGraphMapTabPanel().setError("Failed to load map: " + ex.getMessage());
                    view.getGraphMapTabPanel().setLoading(false);
                    view.setStatusMessage("Map load failed.");
                });
            }
        });
    }
    private void refreshMapFileList() {
        Set<String> seen = new LinkedHashSet<>();
        List<File> files = new ArrayList<>();
        synchronized (loadedMetrics) {
            for (NetworkMetrics m : loadedMetrics) {
                if (m.getGraphFile() == null) continue;
                File f = new File(m.getGraphFile());
                if (f.isFile() && seen.add(f.getAbsolutePath())) {
                    files.add(f);
                }
            }
        }
        if (currentFolder != null) {
            File[] folderFiles = currentFolder.listFiles(f -> f.getName().endsWith(".graphml"));
            if (folderFiles != null) {
                for (File f : folderFiles) {
                    if (seen.add(f.getAbsolutePath())) {
                        files.add(f);
                    }
                }
            }
        }
        files.sort(Comparator.comparing(File::getName));
        view.refreshMapFiles(files);
    }
    private void saveSession() {
        Runnable task = this::saveSessionOnEdt;
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        } else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (Exception e) {
                logger.warn("Session save skipped (UI thread unavailable): {}", e.getMessage());
            }
        }
    }
    private void saveSessionOnEdt() {
        SessionPersistenceService.SessionData data = new SessionPersistenceService.SessionData();
        synchronized (loadedMetrics) {
            data.metrics = new ArrayList<>(loadedMetrics);
        }
        data.currentFolder = currentFolder != null ? currentFolder.getAbsolutePath() : null;
        data.lastAnalysisText = view.getAnalysisText();
        data.chatHistory = view.getChatHistory();
        data.analysisMode = view.getAnalysisMode().name();
        data.savedAt = System.currentTimeMillis();
        if (!sessionStore.save(data) && !data.metrics.isEmpty()) {
            view.setStatusMessage("Warning: could not save session to disk.");
        }
    }
    private void scheduleSaveSession() {
        long gen = saveGeneration.get();
        SwingUtilities.invokeLater(() -> {
            if (gen != saveGeneration.get()) {
                return;
            }
            saveSessionOnEdt();
        });
    }
    private void restoreSession() {
        SessionPersistenceService.SessionData data = sessionStore.load();
        if (data == null) {
            logger.info("No previous session to restore");
            return;
        }
        if (data.metrics.isEmpty()) {
            logger.info("Previous session contained no networks");
            restoreFolderFromSession(data);
            return;
        }
        synchronized (loadedMetrics) {
            loadedMetrics.clear();
            loadedMetrics.addAll(data.metrics);
        }
        restoreFolderFromSession(data);
        if (data.lastAnalysisText != null && !data.lastAnalysisText.isBlank()) {
            view.setAnalysisText(data.lastAnalysisText);
        }
        if (data.chatHistory != null && !data.chatHistory.isBlank()) {
            view.setChatHistory(data.chatHistory);
        }
        view.setAnalysisMode(AnalysisMode.fromString(data.analysisMode));
        List<String> processedFiles = loadedMetrics.stream()
                .map(m -> new File(m.getGraphFile()).getName())
                .collect(Collectors.toList());
        view.restoreProcessedFiles(processedFiles);
        view.updateDashboard(loadedMetrics);
        view.updateResultsTable(loadedMetrics);
        view.updateWorldBankData(loadedMetrics);
        view.updatePredictionData(loadedMetrics);
        refreshMapFileList();
        view.setStatusMessage("Restored " + loadedMetrics.size()
                + " network analysis result(s) from previous session.");
        logger.info("Restored session with {} metrics", loadedMetrics.size());
    }
    private void restoreFolderFromSession(SessionPersistenceService.SessionData data) {
        if (data.currentFolder == null) {
            return;
        }
        File folder = new File(data.currentFolder);
        if (!folder.isDirectory()) {
            return;
        }
        currentFolder = folder;
        view.setFolderPath(folder.getAbsolutePath());
        File[] files = folder.listFiles(f -> f.getName().endsWith(".graphml"));
        if (files != null && files.length > 0) {
            List<File> sorted = Arrays.stream(files)
                    .sorted(Comparator.comparing(File::getName))
                    .collect(Collectors.toList());
            view.setAvailableFiles(sorted);
        }
    }
    // Autoload default folder
    private void autoLoadDefaultFolder() {
        if (!loadedMetrics.isEmpty() || currentFolder != null) return;
        String[] candidates = {"DataSets/Graphs", "DataSets\\Graphs", "data/graphs", "graphs", "."};
        for (String candidate : candidates) {
            File dir = new File(candidate);
            if (dir.isDirectory()) {
                File[] files = dir.listFiles(f -> f.getName().endsWith(".graphml"));
                if (files != null && files.length > 0) {
                    logger.info("Auto-detected GraphML folder: {}", dir.getAbsolutePath());
                    loadFolder(dir);
                    return;
                }
            }
        }
        view.setStatusMessage("Ready: use Browse Folder in the Files tab to locate your .graphml files.");
    }
    // Folder browsing
    private void onBrowseFolder(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select Folder Containing GraphML Files");
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (currentFolder != null) fc.setCurrentDirectory(currentFolder);

        if (fc.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION) {
            loadFolder(fc.getSelectedFile());
        }
    }
    private void loadFolder(File dir) {
        currentFolder = dir;
        view.setFolderPath(dir.getAbsolutePath());

        File[] files = dir.listFiles(f -> f.getName().endsWith(".graphml"));
        if (files == null || files.length == 0) {
            view.showErrorMessage("No .graphml files found in:\n" + dir.getAbsolutePath());
            return;
        }
        List<File> sorted = Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .collect(Collectors.toList());

        view.setAvailableFiles(sorted);
        view.setStatusMessage(sorted.size() + " GraphML file(s) found: select files and press Process.");
        refreshMapFileList();
        logger.info("Loaded folder {}: {} files", dir.getName(), sorted.size());
    }
    private void onLoadSingleFile(ActionEvent e) {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Select GraphML File");
        fc.setFileFilter(new FileNameExtensionFilter("GraphML Files (*.graphml)", "graphml"));
        if (currentFolder != null) fc.setCurrentDirectory(currentFolder);
        if (fc.showOpenDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            currentFolder = f.getParentFile();
            loadFolder(currentFolder);
        }
    }
    // Process selected files
    private void onProcessSelected(ActionEvent e) {
        List<File> selected = view.getSelectedFiles();
        if (selected.isEmpty()) {
            view.showInfoMessage(
                    "No files selected.\n\nTick the checkboxes next to the files you want to analyse,\n"
                            + "then press Process Selected.");
            return;
        }
        Set<String> done = loadedMetrics.stream()
                .map(m -> new File(m.getGraphFile()).getAbsolutePath())
                .collect(Collectors.toSet());
        List<File> toProcess = selected.stream()
                .filter(f -> !done.contains(f.getAbsolutePath()))
                .collect(Collectors.toList());
        if (toProcess.isEmpty()) {
            view.showInfoMessage("All selected files have already been processed.");
            return;
        }
        AnalysisMode mode = view.getAnalysisMode();
        if (mode == AnalysisMode.FULL) {
            int confirm = JOptionPane.showConfirmDialog(
                    view.getFrame(),
                    "Full analysis computes exact betweenness centrality and all-path BFS.\n"
                            + "On large cities this can take hours and use significant memory.\n\n"
                            + "Continue with full analysis for " + toProcess.size() + " file(s)?",
                    "Full Analysis",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        int total = toProcess.size();
        view.setProcessingProgress(0, total);
        view.setStatusMessage(String.format(
                "Processing %d file(s): %s analysis...", total, mode.getLabel()));
        toProcess.forEach(f -> view.addFileToQueue(f.getName()));
        AnalysisMode processingMode = mode;
        int[] doneCount = {0};
        List<CompletableFuture<Void>> futures = toProcess.stream()
                .map(file -> CompletableFuture.runAsync(() -> {
                    processSingleFile(file, processingMode);
                    synchronized (doneCount) { doneCount[0]++; }
                    view.setProcessingProgress(doneCount[0], total);
                }, pool))
                .collect(Collectors.toList());
        pool.submit(() -> {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            SwingUtilities.invokeLater(() -> {
                view.setStatusMessage(String.format(
                        "Done: %d new network(s) processed. %d total city networks loaded.",
                        total, loadedMetrics.size()));
                view.updateDashboard(loadedMetrics);
                view.updateResultsTable(loadedMetrics);
                view.updateWorldBankData(loadedMetrics);
                view.updatePredictionData(loadedMetrics);
                refreshMapFileList();
                saveSession();
            });
        });
    }
    private void processSingleFile(File file, AnalysisMode mode) {
        try {
            logger.info("Analysing: {} [{}]", file.getName(), mode.getLabel());
            var loadResult = loader.loadWithStats(file);
            Graph<String, DefaultEdge> graph = loadResult.graph();
            NetworkMetrics metrics = analyser.calculateMetrics(graph, file.getAbsolutePath(), mode);
            metrics.setStreetStats(loadResult.stats());
            enrichWorldBankData(metrics);
            synchronized (loadedMetrics) { loadedMetrics.add(metrics); }
            view.markFileProcessed(file.getName());
            refreshWorldBankUi();
            scheduleSaveSession();
            String approx = metrics.isMetricsApproximated() ? " (approximate)" : "";
            logger.info("Finished: {} ({} nodes, {} edges): {}{}",
                    file.getName(), metrics.getNodeCount(), metrics.getEdgeCount(),
                    metrics.getAnalysisModeLabel(), approx);
        } catch (Exception ex) {
            logger.error("Failed to process {}: {}", file.getName(), ex.getMessage());
            view.markFileFailed(file.getName());
            SwingUtilities.invokeLater(() ->
                    view.setStatusMessage("Error: " + file.getName() + ": " + ex.getMessage()));
        }
    }
    // Charts
    private void onGenerateCharts(ActionEvent e) {
        if (loadedMetrics.isEmpty()) {
            view.showErrorMessage("No metrics loaded. Process files first.");
            return;
        }
        view.setStatusMessage("Generating charts...");
        view.clearCharts();

        List<NetworkMetrics> snapshot;
        synchronized (loadedMetrics) {
            snapshot = new ArrayList<>(loadedMetrics);
        }
        pool.submit(() -> {
            try {
                int chartCount = 0;
                if (snapshot.size() == 1) {
                    NetworkMetrics m = snapshot.get(0);
                    chartCount += addChart(NetworkChartGenerator.generateHighwayTypeChart(m),
                            "Highway Types: " + m.getGraphName());
                    chartCount += addChart(NetworkChartGenerator.generateSingleNetworkOsmProfileChart(m),
                            "Street Profile: " + m.getGraphName());
                    chartCount += addChart(NetworkChartGenerator.generateStreetMobilityIndicatorsChart(snapshot),
                            "OSM Mobility Indicators");
                    chartCount += addChart(NetworkChartGenerator.generateSingleNetworkSizeChart(m),
                            "Network Size: " + m.getGraphName());
                    chartCount += addChart(NetworkChartGenerator.generateSingleNetworkProfileChart(m),
                            "Graph Metric Profile: " + m.getGraphName());
                    chartCount += addChart(NetworkChartGenerator.generateCentralityComparisonChart(snapshot),
                            "Centrality Breakdown");
                    chartCount += addChart(NetworkChartGenerator.generateClusteringAndDensityChart(snapshot),
                            "Clustering and Density");
                } else {
                    chartCount += addChart(NetworkChartGenerator.generateHighwayMixComparisonChart(snapshot),
                            "Highway Mix by City");
                    chartCount += addChart(NetworkChartGenerator.generateMobilityModesChart(snapshot),
                            "How Streets Are Used");
                    chartCount += addChart(NetworkChartGenerator.generateRoadLengthComparisonChart(snapshot),
                            "Total Road Length by City");
                    chartCount += addChart(NetworkChartGenerator.generateStreetMobilityIndicatorsChart(snapshot),
                            "OSM Mobility Indicators");
                    chartCount += addChart(NetworkChartGenerator.generateSpeedAndLanesChart(snapshot),
                            "Speed Limits and Lane Counts");
                    chartCount += addChart(NetworkChartGenerator.generateNamedRoadsAndAccessChart(snapshot),
                            "Street Naming & Access");
                    chartCount += addChart(NetworkChartGenerator.generateInfrastructureCountsChart(snapshot),
                            "Bridges and Tunnels");
                    chartCount += addChart(NetworkChartGenerator.generateStreetWidthChart(snapshot),
                            "Street Width");
                    chartCount += addChart(NetworkChartGenerator.generateCombinedHighwayTypeChart(snapshot),
                            "Most Common Street Types");
                    chartCount += addChart(NetworkChartGenerator.generateCitiesVsAverageChart(snapshot),
                            "Each City vs Dataset Average");
                    chartCount += addChart(NetworkChartGenerator.generateMultiCityTrendChart(snapshot),
                            "Normalized Graph Metrics Across Cities");
                    chartCount += addChart(NetworkChartGenerator.generateCentralityComparisonChart(snapshot),
                            "Centrality: Betweenness / Closeness / Degree");
                    chartCount += addChart(NetworkChartGenerator.generateClusteringAndDensityChart(snapshot),
                            "Clustering Coefficient and Graph Density");
                    chartCount += addChart(NetworkChartGenerator.generateDiameterAndPathLengthChart(snapshot),
                            "Diameter and Avg Path Length");
                    chartCount += addChart(NetworkChartGenerator.generateNodeEdgeCountChart(snapshot),
                            "Node and Edge Count per Network");
                    chartCount += addChart(NetworkChartGenerator.generateEntropyChart(snapshot),
                            "Graph Entropy");
                    chartCount += addChart(NetworkChartGenerator.generateMeanDegreeAndDiversityChart(snapshot),
                            "Mean Degree and Degree Variance");
                    chartCount += addChart(NetworkChartGenerator.generateDensityVsClusteringScatter(snapshot),
                            "Density vs Clustering Scatter");
                    chartCount += addChart(NetworkChartGenerator.generateCompositeReadinessChart(snapshot),
                            "Composite Structural Readiness Ranking");
                    chartCount += addChart(NetworkChartGenerator.generateAssortativityReciprocityChart(snapshot),
                            "Assortativity and Reciprocity");
                    if (snapshot.size() >= 3) {
                        chartCount += addChart(NetworkChartGenerator.generateMetricDistributionBoxPlot(snapshot),
                                "Metric Distributions Across Cities");
                        chartCount += addChart(NetworkChartGenerator.generateMetricCorrelationChart(snapshot),
                                "Pearson Correlations Between Metrics");
                    }
                    if (snapshot.size() >= 2) {
                        chartCount += addChart(NetworkChartGenerator.generateTopVsBottomComparisonChart(snapshot),
                                "Top vs Bottom Cities");
                        chartCount += addChart(NetworkChartGenerator.generateCountryClusteringChart(snapshot),
                                "Country/Region Average Clustering");
                        chartCount += addChart(NetworkChartGenerator.generatePathLengthVsDiameterScatter(snapshot),
                                "Diameter vs Avg Path Length");
                    }
                    if (cityScores.isLoaded()) {
                        chartCount += addChart(NetworkChartGenerator.generateUmriRankingChart(snapshot, cityScores),
                                "Official UMRi Scores");
                        chartCount += addChart(NetworkChartGenerator.generateUmriVsClusteringScatter(snapshot, cityScores),
                                "UMRi vs Clustering");
                        chartCount += addChart(NetworkChartGenerator.generateStructuralVsUmriScatter(snapshot, cityScores),
                                "Computed Readiness vs Official UMRi");
                    }
                    if (readinessPredictor.isReady()) {
                        var refPreds = readinessPredictor.predictReferenceHoldout(
                                ModelType.OLS_STRUCTURAL, PredictionTarget.UMRI);
                        chartCount += addChart(NetworkChartGenerator.generatePredictedVsActualChart(
                                        refPreds, "UMRi",
                                        "LOOCV: Predicted vs Actual UMRi (62 reference cities)"),
                                "Predicted vs Actual UMRi (reference)");
                        var loadedPreds = readinessPredictor.predictLoadedCities(
                                snapshot, ModelType.OLS_STRUCTURAL, PredictionTarget.UMRI);
                        var matched = loadedPreds.stream()
                                .filter(org.example.service.MobilityReadinessPredictor.CityPrediction::hasReferenceLabel)
                                .toList();
                        if (!matched.isEmpty()) {
                            chartCount += addChart(NetworkChartGenerator.generatePredictedVsActualChart(
                                            matched, "UMRi",
                                            "Predicted vs Official UMRi (your cities)"),
                                    "Predicted vs Actual UMRi (loaded)");
                        }
                    }
                    if (snapshot.stream().anyMatch(m -> m.getWorldBankData() != null
                            && m.getWorldBankData().isUsable())) {
                        chartCount += addChart(NetworkChartGenerator.generateWorldBankGdpChart(snapshot),
                                "World Bank GDP per Capita");
                        chartCount += addChart(NetworkChartGenerator.generateWorldBankUrbanPopChart(snapshot),
                                "World Bank Urban Population %");
                        chartCount += addChart(NetworkChartGenerator.generateWorldBankCo2Chart(snapshot),
                                "World Bank CO2 per Capita");
                        chartCount += addChart(NetworkChartGenerator.generateGdpVsClusteringScatter(snapshot),
                                "GDP vs Clustering");
                    }
                }
                int finalCount = chartCount;
                SwingUtilities.invokeLater(() ->
                        view.setStatusMessage(finalCount + " charts generated for "
                                + snapshot.size() + " network(s)."));
            } catch (Exception ex) {
                logger.error("Chart generation failed", ex);
                SwingUtilities.invokeLater(() ->
                        view.showErrorMessage("Chart generation failed:\n" + ex.getMessage()));
            }
        });
    }
    private int addChart(JFreeChart chart, String title) {
        if (chart != null) {
            view.displayChart(chart, title);
            return 1;
        }
        return 0;
    }
    // Analysis report
    private void onGenerateAnalysis(ActionEvent e) {
        if (loadedMetrics.isEmpty()) {
            view.showErrorMessage("No metrics loaded. Process files first.");
            return;
        }
        view.setStatusMessage("Generating analysis report...");
        pool.submit(() -> {
            String report = NetworkAnalysisReportService.generateReport(loadedMetrics);
            SwingUtilities.invokeLater(() -> {
                view.displayAnalysis(report);
                view.setStatusMessage("Analysis report generated for " + loadedMetrics.size() + " network(s).");
                saveSession();
            });
        });
    }
    // Export
    private void onExport(ActionEvent e) {
        if (loadedMetrics.isEmpty()) {
            view.showErrorMessage("No metrics to export.");
            return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Export Results to CSV");
        fc.setSelectedFile(new File("network_metrics.csv"));
        fc.setFileFilter(new FileNameExtensionFilter("CSV Files (*.csv)", "csv"));
        if (fc.showSaveDialog(view.getFrame()) == JFileChooser.APPROVE_OPTION) {
            File out = fc.getSelectedFile();
            if (!out.getName().toLowerCase().endsWith(".csv"))
                out = new File(out.getAbsolutePath() + ".csv");
            try {
                exporter.exportMultipleMetrics(loadedMetrics, out);
                view.showInfoMessage("Exported " + loadedMetrics.size() + " records to:\n" + out.getName());
                view.setStatusMessage("Exported to: " + out.getName());
            } catch (Exception ex) {
                logger.error("Export failed", ex);
                view.showErrorMessage("Export failed:\n" + ex.getMessage());
            }
        }
    }
}