package org.example;

import org.example.controller.NetworkController;
import org.example.service.GraphMLLoaderService;
import org.example.service.MetricsExportService;
import org.example.service.NetworkAnalysisService;
import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.example.theme.UiStyles;
import org.example.view.ModernNetworkView;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.Dimension;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
/*
 Application entry point.
 Behavior:
 No arguments: launches the GUI
 1+ arguments: runs in CLI mode (batch/single-file)
 */
public class NetworkAnalysisMain {
    private static final Logger logger = LoggerFactory.getLogger(NetworkAnalysisMain.class);
    private static volatile NetworkController activeController;
    public static void main(String[] args) {
        printBanner();
        if (args.length > 0) {
            runCLI(args);
        } else {
            launchGUI();
        }
    }
    private static void launchGUI() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UiStyles.applyGlobalTheme();
        } catch (Exception e) {
            System.err.println("Could not set system L&F: " + e.getMessage());
        }
        SwingUtilities.invokeLater(() -> {
            try {
                ModernNetworkView view = new ModernNetworkView();
                NetworkController ctrl = new NetworkController(view);
                activeController = ctrl;
                JFrame frame = view.getFrame();
                frame.setMinimumSize(new Dimension(1200, 800));
                frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
                frame.addWindowListener(new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosing(java.awt.event.WindowEvent ev) {
                        int opt = JOptionPane.showConfirmDialog(
                                frame,
                                "Exit Extracting information from open data?",
                                "Confirm Exit",
                                JOptionPane.YES_NO_OPTION,
                                JOptionPane.QUESTION_MESSAGE);
                        if (opt == JOptionPane.YES_OPTION) {
                            ctrl.shutdown();
                            activeController = null;
                            System.exit(0);
                        }
                    }
                });
                view.show();
                logger.info("Extracting information from open data: GUI started successfully.");
            } catch (Exception e) {
                logger.error("Failed to start GUI", e);
                JOptionPane.showMessageDialog(null,
                        "Failed to start application:\n" + e.getMessage(),
                        "Startup Error", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            NetworkController ctrl = activeController;
            if (ctrl != null) {
                ctrl.shutdown();
            }
        }, "session-save-shutdown-hook"));
    }
    private static final GraphMLLoaderService  cliLoader   = new GraphMLLoaderService();
    private static final NetworkAnalysisService cliAnalyser = new NetworkAnalysisService();
    private static final MetricsExportService   cliExporter = new MetricsExportService();
    private static AnalysisMode cliAnalysisMode = AnalysisMode.QUICK;
    private static void runCLI(String[] args) {
        List<String> argList = new ArrayList<>(Arrays.asList(args));
        if (argList.remove("--full")) {
            cliAnalysisMode = AnalysisMode.FULL;
        }
        if (argList.isEmpty()) {
            System.err.println("No input files. Usage: NetworkAnalysisMain [--full] file.graphml ...");
            return;
        }
        System.out.println("Analysis mode: " + cliAnalysisMode.getLabel());
        if (argList.size() == 1) {
            System.out.println("Processing single file: " + argList.get(0));
            processGraphMLFile(argList.get(0));
        } else {
            System.out.println("Batch mode: processing " + argList.size() + " files in parallel");
            processBatch(argList);
        }
        cliAnalyser.shutdown();
    }
    public static void processGraphMLFile(String filePath) {
        logger.info("Starting analysis for: {}", filePath);
        File inputFile = new File(filePath);
        if (!inputFile.exists()) {
            System.err.println("Error: File not found: " + filePath);
            return;
        }
        try {
            var loadResult = cliLoader.loadWithStats(inputFile);
            Graph<String, DefaultEdge> graph = loadResult.graph();
            System.out.printf("  Loaded: %d nodes, %d edges%n",
                    graph.vertexSet().size(), graph.edgeSet().size());

            NetworkMetrics metrics = cliAnalyser.calculateMetrics(
                    graph, inputFile.getAbsolutePath(), cliAnalysisMode);
            metrics.setStreetStats(loadResult.stats());
            System.out.println("  Analysis: " + metrics.getAnalysisModeLabel());
            if (metrics.isMetricsApproximated()) {
                System.out.println("  Note: quick analysis: some centrality metrics are approximate");
            }
            System.out.println(metrics);
            File outAll = cliExporter.generateOutputFilename(inputFile, "_all_metrics");
            cliExporter.exportAllMetrics(metrics, outAll);
            System.out.println("  Exported - " + outAll.getName());

        } catch (Exception e) {
            logger.error("Error processing {}: {}", filePath, e.getMessage(), e);
            System.err.println("Error: " + e.getMessage());
        }
    }
    public static void processBatch(List<String> filePaths) {
        int poolSize = Math.min(filePaths.size(), Runtime.getRuntime().availableProcessors());
        ExecutorService pool = Executors.newFixedThreadPool(poolSize);
        List<CompletableFuture<Void>> futures = filePaths.stream()
                .map(path -> CompletableFuture.runAsync(() -> processGraphMLFile(path), pool))
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        pool.shutdown();
        System.out.printf("Batch complete: %d files processed.%n", filePaths.size());
    }
    private static void printBanner() {
        System.out.println("=".repeat(60));
        System.out.println("       EXTRACTING INFORMATION FROM OPEN DATA");
        System.out.println("       Urban street network metrics from GraphML");
        System.out.println("=".repeat(60));
        System.out.println("  Usage:");
        System.out.println("    java NetworkAnalysisMain              - GUI mode");
        System.out.println("    java NetworkAnalysisMain file.graphml - CLI single");
        System.out.println("    java NetworkAnalysisMain f1 f2 …      - CLI batch");
        System.out.println("=".repeat(60) + "\n");
    }
}