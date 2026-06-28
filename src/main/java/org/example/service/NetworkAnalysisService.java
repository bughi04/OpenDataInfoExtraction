package org.example.service;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.example.model.AnalysisMode;
import org.example.model.NetworkMetrics;
import org.jgrapht.Graph;
import org.jgrapht.alg.scoring.ClusteringCoefficient;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/*
Parallel network metrics calculator
Execution model: two phases:
Phase 1: five independent task groups run concurrently
(a) Degree-based metrics: single pass, O(V)
(b) Clustering coefficient
(c) Reciprocity
(d) p chunked Brandes tasks: betweenness centrality,   O(VE / p)
(e) p chunked BFS tasks: closeness, diameter, APL,  O(V(V+E) / p)
Phase 2: aggregate the p partial results and write to NetworkMetrics
 */
public class NetworkAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(NetworkAnalysisService.class);
    private static final int LARGE_GRAPH_NODES = 18_000;
    private static final int LARGE_GRAPH_EDGES = 70_000;
    private static final int SKIP_CLUSTERING_NODES = 45_000;
    private static final int SAMPLE_SOURCES = 350;
    private final ExecutorService executor;
    private final int threadCount;
    public NetworkAnalysisService() {
        this(Runtime.getRuntime().availableProcessors());
    }
    public NetworkAnalysisService(int threadCount) {
        this.threadCount = threadCount;
        this.executor = Executors.newFixedThreadPool(threadCount);
        logger.info("NetworkAnalysisService initialized with {} threads", threadCount);
    }
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    public NetworkMetrics calculateMetrics(Graph<String, DefaultEdge> graph, String filename) {
        return calculateMetrics(graph, filename, AnalysisMode.QUICK);
    }
    public NetworkMetrics calculateMetrics(
            Graph<String, DefaultEdge> graph, String filename, AnalysisMode mode) {
        AnalysisMode effectiveMode = mode != null ? mode : AnalysisMode.QUICK;
        logger.info("Starting {} metrics calculation for: {}", effectiveMode.getLabel(), filename);
        NetworkMetrics metrics = new NetworkMetrics(filename);
        metrics.setAnalysisMode(effectiveMode);
        try {
            final int n = graph.vertexSet().size();
            final int m = graph.edgeSet().size();
            final boolean isDirected = graph.getType().isDirected();
            metrics.setNodeCount(n);
            metrics.setEdgeCount(m);
            metrics.setDirected(isDirected);
            if (n < 2) {
                logger.warn("Graph too small for meaningful metrics");
                return metrics;
            }
            final boolean largeGraph = n > LARGE_GRAPH_NODES || m > LARGE_GRAPH_EDGES;
            final boolean useQuickPath = effectiveMode == AnalysisMode.QUICK && largeGraph;
            if (useQuickPath) {
                metrics.setMetricsApproximated(true);
                logger.info("Large graph ({} nodes, {} edges): quick analysis (approximate metrics)", n, m);
            } else if (effectiveMode == AnalysisMode.FULL && largeGraph) {
                logger.info("Large graph ({} nodes, {} edges): full analysis (this may take a long time)", n, m);
            }
            final String[] vertices = graph.vertexSet().toArray(new String[0]);
            final Map<String, Integer> vertexIndex = new HashMap<>(n * 2);
            for (int i = 0; i < n; i++) vertexIndex.put(vertices[i], i);
            final int chunks = Math.min(threadCount, n);
            final int[] chunkStart = chunkBoundaries(n, chunks);
            CompletableFuture<Void> degreeTask = CompletableFuture.runAsync(
                    () -> computeDegreeMetrics(graph, metrics, n, m, isDirected), executor);
            CompletableFuture<Void> clusterTask;
            if (useQuickPath && n > SKIP_CLUSTERING_NODES) {
                logger.info("Skipping clustering coefficient (quick analysis, very large graph)");
                clusterTask = CompletableFuture.completedFuture(null);
            } else {
                clusterTask = CompletableFuture.runAsync(
                        () -> computeClustering(graph, metrics), executor);
            }
            CompletableFuture<Void> reciprocityTask = CompletableFuture.runAsync(
                    () -> computeReciprocity(graph, metrics, m, isDirected), executor);

            List<Future<double[]>> brandFutures = new ArrayList<>();
            List<Future<BFSChunkResult>> bfsFutures = new ArrayList<>();
            if (useQuickPath) {
                metrics.setAvgBetweennessCentrality(null);
                bfsFutures.add(executor.submit(
                        () -> sampledBfsChunk(graph, vertices, n, isDirected, SAMPLE_SOURCES)));
            } else {
                for (int c = 0; c < chunks; c++) {
                    final int from = chunkStart[c];
                    final int to   = chunkStart[c + 1];
                    brandFutures.add(executor.submit(
                            () -> brandesChunk(graph, vertices, vertexIndex, from, to, n, isDirected)));
                }
                for (int c = 0; c < chunks; c++) {
                    final int from = chunkStart[c];
                    final int to   = chunkStart[c + 1];
                    bfsFutures.add(executor.submit(
                            () -> bfsChunk(graph, vertices, from, to, n, isDirected)));
                }
            }
            CompletableFuture.allOf(degreeTask, clusterTask, reciprocityTask).join();
            if (useQuickPath) {
                aggregateSampledBFSResults(bfsFutures, metrics);
            } else {
                aggregateBFSResults(bfsFutures, metrics, n);
                aggregateBrandesResults(brandFutures, metrics, n, isDirected);
            }
            metrics.setConstraints(null);
            logger.info("All metrics done for: {}", filename);
        } catch (Exception e) {
            logger.error("Error calculating metrics: {}", e.getMessage(), e);
        }
        return metrics;
    }
    private static int[] chunkBoundaries(int n, int chunks) {
        int[] starts = new int[chunks + 1];
        int base = n / chunks;
        int remainder = n % chunks;
        int idx = 0;
        for (int c = 0; c < chunks; c++) {
            starts[c] = idx;
            idx += base + (c < remainder ? 1 : 0);
        }
        starts[chunks] = n;
        return starts;
    }
    private void aggregateSampledBFSResults(
            List<Future<BFSChunkResult>> futures,
            NetworkMetrics metrics) throws InterruptedException, ExecutionException {
        BFSChunkResult r = futures.get(0).get();
        double closenessSum = r.closenessSum;
        int sampleCount = r.sampleCount;
        metrics.setAvgClosenessCentrality(sampleCount > 0 ? closenessSum / sampleCount : null);
        if (r.pathCount > 0) {
            metrics.setGraphDiameter((int) Math.round(r.maxDist));
            metrics.setAvgPathLength(r.totalDist / r.pathCount);
            if (!r.fullyConnected) {
                logger.info("Sampled path metrics use reachable pairs only (graph has disconnected components)");
            }
        }
        logger.info("Sampled path metrics: diameter: {}, avg path: {}",
                metrics.getGraphDiameter(), metrics.getAvgPathLength());
    }
    private BFSChunkResult sampledBfsChunk(
            Graph<String, DefaultEdge> graph,
            String[] vertices,
            int n,
            boolean isDirected,
            int sampleCount) {
        int samples = Math.min(sampleCount, n);
        double closenessSum = 0.0;
        double maxDist = 0.0;
        double totalDist = 0.0;
        long pathCount = 0;
        boolean fullyConnected = true;
        Map<String, Integer> dist = new HashMap<>(Math.min(n, 4096));
        Queue<String> queue = new ArrayDeque<>();
        for (int s = 0; s < samples; s++) {
            int si = (int) ((long) s * n / samples);
            String source = vertices[si];
            dist.clear();
            queue.clear();
            dist.put(source, 0);
            queue.add(source);
            while (!queue.isEmpty()) {
                String curr = queue.poll();
                int d = dist.get(curr);
                for (DefaultEdge e : graph.edgesOf(curr)) {
                    if (isDirected && !graph.getEdgeSource(e).equals(curr)) continue;
                    String nb = otherEndpoint(graph, e, curr);
                    if (!dist.containsKey(nb)) {
                        dist.put(nb, d + 1);
                        queue.add(nb);
                    }
                }
            }
            int reachable = dist.size() - 1;
            if (reachable < n - 1) fullyConnected = false;
            long srcTotal = 0;
            int srcMax = 0;
            for (int vi = 0; vi < n; vi++) {
                String v = vertices[vi];
                if (v == source) continue;
                Integer d = dist.get(v);
                if (d != null) {
                    srcTotal += d;
                    if (d > srcMax) srcMax = d;
                }
            }
            if (srcMax > maxDist) maxDist = srcMax;
            totalDist += srcTotal;
            pathCount += reachable;
            double closeness = (reachable > 0 && srcTotal > 0)
                    ? (reachable / (double) srcTotal) * (n - 1.0)
                    : 0.0;
            closenessSum += closeness;
        }
        BFSChunkResult result = new BFSChunkResult(closenessSum, maxDist, totalDist, pathCount, fullyConnected);
        result.sampleCount = samples;
        return result;
    }
    private void aggregateBFSResults(
            List<Future<BFSChunkResult>> futures,
            NetworkMetrics metrics,
            int n) throws InterruptedException, ExecutionException {
        double closenessSum   = 0.0;
        double globalMaxDist  = 0.0;
        double globalTotalDist = 0.0;
        long   globalPathCount = 0;
        boolean fullyConnected = true;
        for (Future<BFSChunkResult> f : futures) {
            BFSChunkResult r = f.get();
            closenessSum    += r.closenessSum;
            globalTotalDist += r.totalDist;
            globalPathCount += r.pathCount;
            if (r.maxDist > globalMaxDist) globalMaxDist = r.maxDist;
            if (!r.fullyConnected) fullyConnected = false;
        }
        metrics.setAvgClosenessCentrality(closenessSum / n);
        logger.info("Closeness centrality done: {}", metrics.getAvgClosenessCentrality());
        if (globalPathCount > 0) {
            metrics.setGraphDiameter((int) Math.round(globalMaxDist));
            metrics.setAvgPathLength(globalTotalDist / globalPathCount);
            if (fullyConnected) {
                logger.info("Diameter: {}, Avg path length: {}",
                        metrics.getGraphDiameter(), metrics.getAvgPathLength());
            } else {
                logger.info("Graph has disconnected components: diameter: {}, avg path length: {} "
                                + "(computed over reachable pairs within components)",
                        metrics.getGraphDiameter(), metrics.getAvgPathLength());
            }
        } else {
            logger.info("No reachable paths found: diameter and avg path length skipped");
        }
    }
    private void aggregateBrandesResults(
            List<Future<double[]>> futures,
            NetworkMetrics metrics,
            int n,
            boolean isDirected) throws InterruptedException, ExecutionException {

        double[] betweenness = new double[n];
        for (Future<double[]> f : futures) {
            double[] partial = f.get();
            for (int v = 0; v < n; v++) betweenness[v] += partial[v];
        }
        if (!isDirected) {
            for (int v = 0; v < n; v++) betweenness[v] /= 2.0;
        }
        double maxB = (n - 1.0) * (n - 2.0) / 2.0;
        double avgBetweenness = 0.0;
        if (maxB > 0) {
            for (double b : betweenness) avgBetweenness += b / maxB;
            avgBetweenness /= n;
        }
        metrics.setAvgBetweennessCentrality(avgBetweenness);
        logger.info("Betweenness centrality done: {}", avgBetweenness);
    }
    private BFSChunkResult bfsChunk(
            Graph<String, DefaultEdge> graph,
            String[] vertices,
            int from, int to,
            int n,
            boolean isDirected) {
        double closenessSum   = 0.0;
        double maxDist        = 0.0;
        double totalDist      = 0.0;
        long   pathCount      = 0;
        boolean fullyConnected = true;
        Map<String, Integer> dist = new HashMap<>(n * 2);
        Queue<String> queue = new ArrayDeque<>();
        for (int si = from; si < to; si++) {
            String source = vertices[si];
            dist.clear();
            queue.clear();
            dist.put(source, 0);
            queue.add(source);
            while (!queue.isEmpty()) {
                String curr = queue.poll();
                int d = dist.get(curr);
                for (DefaultEdge e : graph.edgesOf(curr)) {
                    if (isDirected && !graph.getEdgeSource(e).equals(curr)) continue;
                    String nb = otherEndpoint(graph, e, curr);
                    if (!dist.containsKey(nb)) {
                        dist.put(nb, d + 1);
                        queue.add(nb);
                    }
                }
            }
            int reachable = dist.size() - 1;
            if (reachable < n - 1) fullyConnected = false;
            long srcTotal = 0;
            int  srcMax   = 0;
            for (int vi = 0; vi < n; vi++) {
                String v = vertices[vi];
                if (v == source) continue;
                Integer d = dist.get(v);
                if (d != null) {
                    srcTotal += d;
                    if (d > srcMax) srcMax = d;
                }
            }
            if (srcMax > maxDist) maxDist = srcMax;
            totalDist   += srcTotal;
            pathCount   += reachable;
            double closeness = (reachable > 0 && srcTotal > 0)
                    ? (reachable / (double) srcTotal) * (n - 1.0)
                    : 0.0;
            closenessSum += closeness;
        }
        return new BFSChunkResult(closenessSum, maxDist, totalDist, pathCount, fullyConnected);
    }
    private double[] brandesChunk(
            Graph<String, DefaultEdge> graph,
            String[] vertices,
            Map<String, Integer> vertexIndex,
            int from, int to,
            int n,
            boolean isDirected) {
        double[] betweenness = new double[n];
        double[]        sigma = new double[n];
        int[]           dist  = new int[n];
        double[]        dp    = new double[n];
        @SuppressWarnings("unchecked")
        List<Integer>[] pred  = new List[n];
        for (int i = 0; i < n; i++) pred[i] = new ArrayList<>();
        Queue<Integer> queue = new ArrayDeque<>();
        Deque<Integer> stack = new ArrayDeque<>();
        for (int si = from; si < to; si++) {
            Arrays.fill(sigma, 0.0);
            Arrays.fill(dist, -1);
            Arrays.fill(dp, 0.0);
            for (int i = 0; i < n; i++) pred[i].clear();
            queue.clear();
            stack.clear();
            sigma[si] = 1.0;
            dist[si]  = 0;
            queue.add(si);
            while (!queue.isEmpty()) {
                int vi = queue.poll();
                stack.push(vi);
                String v = vertices[vi];
                for (DefaultEdge e : graph.edgesOf(v)) {
                    if (isDirected && !graph.getEdgeSource(e).equals(v)) continue;
                    int wi = vertexIndex.get(otherEndpoint(graph, e, v));
                    if (dist[wi] < 0) {
                        dist[wi] = dist[vi] + 1;
                        queue.add(wi);
                    }
                    if (dist[wi] == dist[vi] + 1) {
                        sigma[wi] += sigma[vi];
                        pred[wi].add(vi);
                    }
                }
            }
            while (!stack.isEmpty()) {
                int wi = stack.pop();
                for (int vi : pred[wi]) {
                    if (sigma[wi] > 0) {
                        dp[vi] += (sigma[vi] / sigma[wi]) * (1.0 + dp[wi]);
                    }
                }
                if (wi != si) {
                    betweenness[wi] += dp[wi];
                }
            }
        }
        return betweenness;
    }
    private void computeDegreeMetrics(
            Graph<String, DefaultEdge> graph,
            NetworkMetrics metrics,
            int n, int m, boolean isDirected) {
        try {
            logger.info("[Thread] Degree-based metrics...");
            List<Integer> degrees = graph.vertexSet().stream()
                    .map(graph::degreeOf)
                    .collect(Collectors.toList());
            double avgDegCentrality = degrees.stream()
                    .mapToDouble(d -> d / (n - 1.0))
                    .average().orElse(0.0);
            metrics.setAvgDegreeCentrality(avgDegCentrality);
            double totalDeg = degrees.stream().mapToLong(Integer::longValue).sum();
            double entropy = 0.0;
            if (totalDeg > 0) {
                for (int d : degrees) {
                    double p = d / totalDeg;
                    if (p > 0) entropy -= p * Math.log(p);
                }
            }
            metrics.setGraphEntropy(entropy);
            double maxEdges = isDirected ? n * (n - 1.0) : n * (n - 1.0) / 2.0;
            metrics.setGraphDensity(maxEdges > 0 ? m / maxEdges : 0.0);
            DescriptiveStatistics stats = new DescriptiveStatistics();
            degrees.forEach(stats::addValue);
            metrics.setMeanDegree(stats.getMean());
            metrics.setDiversity(stats.getVariance());
            computeAssortativity(graph, metrics, isDirected);
            logger.info("[Thread] Degree-based metrics done");
        } catch (Exception e) {
            logger.warn("Degree-based metrics failed: {}", e.getMessage());
        }
    }
    private void computeClustering(Graph<String, DefaultEdge> graph, NetworkMetrics metrics) {
        try {
            logger.info("[Thread] Clustering coefficient...");
            ClusteringCoefficient<String, DefaultEdge> clust = new ClusteringCoefficient<>(graph);
            metrics.setClusteringCoefficient(clust.getAverageClusteringCoefficient());
            logger.info("[Thread] Clustering done");
        } catch (Exception e) {
            logger.warn("Clustering coefficient failed: {}", e.getMessage());
        }
    }
    private void computeAssortativity(
            Graph<String, DefaultEdge> graph,
            NetworkMetrics metrics,
            boolean isDirected) {
        try {
            int edgeCount = graph.edgeSet().size();
            if (edgeCount == 0) {
                metrics.setAssortativityDegree(null);
                return;
            }
            double sumJk = 0, sumJ = 0, sumK = 0, sumJ2 = 0, sumK2 = 0;
            for (DefaultEdge e : graph.edgeSet()) {
                String source = graph.getEdgeSource(e);
                String target = graph.getEdgeTarget(e);
                double j = isDirected ? graph.outDegreeOf(source) : graph.degreeOf(source);
                double k = isDirected ? graph.inDegreeOf(target) : graph.degreeOf(target);
                sumJk += j * k;
                sumJ += j;
                sumK += k;
                sumJ2 += j * j;
                sumK2 += k * k;
            }
            double m = edgeCount;
            double meanJk = sumJk / m;
            double meanJ = sumJ / m;
            double meanK = sumK / m;
            double varJ = sumJ2 / m - meanJ * meanJ;
            double varK = sumK2 / m - meanK * meanK;
            double cov = meanJk - meanJ * meanK;
            if (varJ <= 1e-12 || varK <= 1e-12) {
                metrics.setAssortativityDegree(0.0);
            } else {
                metrics.setAssortativityDegree(cov / Math.sqrt(varJ * varK));
            }
        } catch (Exception e) {
            logger.warn("Assortativity failed: {}", e.getMessage());
            metrics.setAssortativityDegree(null);
        }
    }
    private void computeReciprocity(
            Graph<String, DefaultEdge> graph,
            NetworkMetrics metrics,
            int m, boolean isDirected) {
        try {
            logger.info("[Thread] Reciprocity...");
            if (isDirected) {
                long reciprocal = graph.edgeSet().parallelStream()
                        .filter(e -> graph.containsEdge(
                                graph.getEdgeTarget(e), graph.getEdgeSource(e)))
                        .count();
                metrics.setReciprocity(m > 0 ? reciprocal / (double) m : 0.0);
            } else {
                metrics.setReciprocity(1.0);
            }
            logger.info("[Thread] Reciprocity done");
        } catch (Exception e) {
            logger.warn("Reciprocity failed: {}", e.getMessage());
        }
    }
    private static String otherEndpoint(
            Graph<String, DefaultEdge> graph, DefaultEdge e, String v) {
        String src = graph.getEdgeSource(e);
        return src.equals(v) ? graph.getEdgeTarget(e) : src;
    }
    private static final class BFSChunkResult {
        final double  closenessSum;
        final double  maxDist;
        final double  totalDist;
        final long    pathCount;
        final boolean fullyConnected;
        int sampleCount;
        BFSChunkResult(double closenessSum, double maxDist,
                       double totalDist, long pathCount, boolean fullyConnected) {
            this.closenessSum   = closenessSum;
            this.maxDist        = maxDist;
            this.totalDist      = totalDist;
            this.pathCount      = pathCount;
            this.fullyConnected = fullyConnected;
        }
    }
}