package org.example.service;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

// Loads GraphML files via the fast single-pass UTF-8 stream loader.
public class GraphMLLoaderService {
    private static final Logger logger = LoggerFactory.getLogger(GraphMLLoaderService.class);
    private final GraphMLStreamLoader streamLoader = new GraphMLStreamLoader();
    public Graph<String, DefaultEdge> loadGraphML(File file, boolean directed) throws Exception {
        logger.info("Loading GraphML file: {}", file.getAbsolutePath());
        GraphMLStreamLoader.LoadResult result = streamLoader.load(file);
        if (result.directed() != directed) {
            logger.info("File edgedefault={} (requested directed={})",
                    result.directed() ? "directed" : "undirected", directed);
        }
        logger.info("Successfully loaded graph with {} vertices and {} edges",
                result.graph().vertexSet().size(), result.graph().edgeSet().size());
        return result.graph();
    }
    public Graph<String, DefaultEdge> loadGraphML(File file) throws Exception {
        return streamLoader.load(file).graph();
    }
    public GraphMLStreamLoader.LoadResult loadWithStats(File file) throws Exception {
        return streamLoader.load(file);
    }
    public java.util.Map<String, Graph<String, DefaultEdge>> loadMultipleGraphML(
            java.util.List<File> files, boolean directed) {
        java.util.Map<String, Graph<String, DefaultEdge>> graphs = new java.util.HashMap<>();
        for (File file : files) {
            try {
                GraphMLStreamLoader.LoadResult result = streamLoader.load(file);
                graphs.put(file.getName(), result.graph());
                logger.info("Loaded {}: {} nodes, {} edges",
                        file.getName(),
                        result.graph().vertexSet().size(),
                        result.graph().edgeSet().size());
            } catch (Exception e) {
                logger.error("Failed to load {}: {}", file.getName(), e.getMessage());
            }
        }
        return graphs;
    }
    public boolean isValidGraphML(File file) {
        if (!file.exists() || !file.isFile()) return false;
        if (!file.getName().toLowerCase().endsWith(".graphml")) return false;
        try {
            return streamLoader.load(file).graph().vertexSet().size() > 0;
        } catch (Exception e) {
            logger.warn("File {} is not a valid GraphML: {}", file.getName(), e.getMessage());
            return false;
        }
    }
}