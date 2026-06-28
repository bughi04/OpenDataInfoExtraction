package org.example.service;

import org.example.model.StreetNetworkStats;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

// Single-pass GraphML loader: builds topology and aggregates OSM attributes in one UTF-8 read
public class GraphMLStreamLoader {
    private static final Logger logger = LoggerFactory.getLogger(GraphMLStreamLoader.class);
    public record LoadResult(Graph<String, DefaultEdge> graph, StreetNetworkStats stats, boolean directed) {}
    public LoadResult load(File file) throws Exception {
        if (file == null || !file.isFile()) {
            throw new IllegalArgumentException("File not found: " + file);
        }
        Map<String, GraphMLMetadataParser.KeyDef> keys = new LinkedHashMap<>();
        GraphMLMetadataParser.Aggregator agg = new GraphMLMetadataParser.Aggregator();
        Map<String, String> graphData = new LinkedHashMap<>();
        boolean graphMetaOpen = false;
        boolean directed = true;
        Graph<String, DefaultEdge> graph = null;
        String pendingNodeId = null;
        String pendingEdgeSource = null;
        String pendingEdgeTarget = null;
        Map<String, String> currentData = null;
        XMLInputFactory factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, true);
        try (InputStreamReader in = new InputStreamReader(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8)) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> {
                        String name = reader.getLocalName();
                        switch (name) {
                            case "key" -> {
                                String id = attr(reader, "id");
                                String forType = attr(reader, "for");
                                String attrName = attr(reader, "attr.name");
                                if (id != null && attrName != null) {
                                    keys.put(id, new GraphMLMetadataParser.KeyDef(forType, attrName));
                                }
                            }
                            case "graph" -> {
                                graphMetaOpen = true;
                                graphData.clear();
                                currentData = null;
                                directed = "directed".equalsIgnoreCase(attr(reader, "edgedefault"));
                                graph = directed
                                        ? new DefaultDirectedGraph<>(DefaultEdge.class)
                                        : new DefaultUndirectedGraph<>(DefaultEdge.class);
                            }
                            case "node" -> {
                                graphMetaOpen = false;
                                pendingNodeId = attr(reader, "id");
                                currentData = new LinkedHashMap<>();
                            }
                            case "edge" -> {
                                graphMetaOpen = false;
                                pendingEdgeSource = attr(reader, "source");
                                pendingEdgeTarget = attr(reader, "target");
                                currentData = new LinkedHashMap<>();
                            }
                            case "data" -> {
                                String keyId = attr(reader, "key");
                                GraphMLMetadataParser.KeyDef keyDef = keys.get(keyId);
                                String attrName = keyDef != null ? keyDef.attrName() : null;
                                if (keyId == null) break;
                                if ("geometry".equals(attrName)) {
                                    GraphMLMetadataParser.skipDataContent(reader);
                                    if (currentData != null) {
                                        currentData.put(keyId, GraphMLMetadataParser.GEOMETRY_PRESENT);
                                    }
                                } else {
                                    String text = GraphMLMetadataParser.readElementText(reader);
                                    if (graphMetaOpen) {
                                        graphData.put(keyId, text);
                                    } else if (currentData != null) {
                                        currentData.put(keyId, text);
                                    }
                                }
                            }
                            default -> { }
                        }
                    }
                    case XMLStreamConstants.END_ELEMENT -> {
                        String name = reader.getLocalName();
                        switch (name) {
                            case "graph" -> graphMetaOpen = false;
                            case "node" -> {
                                if (graph != null && pendingNodeId != null) {
                                    graph.addVertex(pendingNodeId);
                                }
                                if (currentData != null) {
                                    agg.acceptNode(currentData, keys);
                                }
                                pendingNodeId = null;
                                currentData = null;
                            }
                            case "edge" -> {
                                if (graph != null && pendingEdgeSource != null && pendingEdgeTarget != null) {
                                    graph.addVertex(pendingEdgeSource);
                                    graph.addVertex(pendingEdgeTarget);
                                    try {
                                        graph.addEdge(pendingEdgeSource, pendingEdgeTarget);
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                if (currentData != null) {
                                    agg.acceptEdge(currentData, keys);
                                }
                                pendingEdgeSource = null;
                                pendingEdgeTarget = null;
                                currentData = null;
                            }
                            default -> { }
                        }
                    }
                    default -> { }
                }
            }
            reader.close();
        }
        if (graph == null) {
            graph = new DefaultUndirectedGraph<>(DefaultEdge.class);
        }
        StreetNetworkStats stats = new StreetNetworkStats();
        GraphMLMetadataParser.applyGraphData(stats, graphData, keys);
        agg.finalizeInto(stats, keys);
        logger.info("Stream-loaded {}: {} nodes, {} edges, directed={}",
                file.getName(), graph.vertexSet().size(), graph.edgeSet().size(), directed);
        return new LoadResult(graph, stats, directed);
    }
    private static String attr(XMLStreamReader reader, String name) {
        String v = reader.getAttributeValue(null, name);
        if (v == null) {
            v = reader.getAttributeValue(javax.xml.XMLConstants.XML_NS_URI, name);
        }
        return v;
    }
}