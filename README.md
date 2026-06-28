# Extracting information from open data

A desktop application for analysing urban street networks from **OpenStreetMap (OSM) GraphML** files. Load city networks, compute graph and street-level metrics, compare cities visually, enrich results with **World Bank** indicators, estimate **urban mobility readiness (UMRi)**, and explore findings through reports, charts, and an optional **Google Gemini** AI advisor.

The app runs as a **Swing GUI** (default) or a **command-line batch tool** for scripted exports.

## Features

| Tab / area | What it does |
|------------|--------------|
| **Dashboard** | Everyday street KPIs: road length, one-way share, speed limits, walk/cycle share, and more |
| **Files** | Browse a folder of `.graphml` files, select cities, choose Quick or Full analysis, and process |
| **Map** | Interactive street map coloured by OSM tags (highway type, speed, one-way, bridges/tunnels) |
| **Charts** | Side-by-side bar charts for street and network metrics; generate from the Charts tab |
| **World Bank** | Country-level socioeconomic indicators via the [Data360 API](https://data360.worldbank.org/) |
| **Prediction** | UMRi / SMi / PTi estimates from network structure (trained on 62 reference cities) |
| **Analysis** | Full written comparative report with research context |
| **AI Advisor** | Gemini-powered Q&A about your loaded cities (optional API key) |
| **Data Guide** | Plain-language explanation of OSM tags, metrics, and the full metrics table |
| **Results** | Sortable table of all computed values with CSV export |

**Session persistence:** processed metrics, UI state, analysis text, and chat history are saved automatically and restored on the next launch.

## Requirements

- **Java 17+** (the project compiles with `release 21` by default; Java 17 is supported via the `prod` Maven profile)
- **Maven 3.6+**
- Internet access for World Bank Data360 and Gemini (optional for offline use of already-cached or local-only features)

## Quick start

### Build

```bash
mvn clean package
```

This produces a runnable fat JAR:

```
target/network-analysis-tool.jar
```

### Run the GUI

```bash
java -jar target/network-analysis-tool.jar
```

Or without packaging:

```bash
mvn exec:java
```

On first launch, open the **Files** tab, click **Browse Folder**, and point to a directory of `.graphml` files (sample data is in `DataSets/Graphs/`). Select cities and click **Process Selected**.

### Run the CLI

Process a single file (Quick analysis, exports `*_all_metrics.csv` next to the input):

```bash
java -jar target/network-analysis-tool.jar DataSets/Graphs/Barcelona,_Spain.graphml
```

Batch mode (parallel):

```bash
java -jar target/network-analysis-tool.jar DataSets/Graphs/*.graphml
```

Full analysis (exact centrality; can be very slow on large cities):

```bash
java -jar target/network-analysis-tool.jar --full DataSets/Graphs/Amsterdam,_Netherlands.graphml
```

### Run tests

```bash
mvn test
```

Some prediction tests require `DataSets/Results_Cities.csv` in the project root.

## Sample data

| Path | Purpose |
|------|---------|
| `DataSets/Graphs/*.graphml` | 62 world-city street networks (OSMnx-style GraphML) |
| `DataSets/Results_Cities.csv` | Reference metrics and official UMRi/SMi/PTi scores for prediction and charts |

GraphML filenames use the pattern `City,_Country.graphml` (underscores instead of spaces).

## Analysis modes

| Mode | Speed | Use when |
|------|-------|----------|
| **Quick** (default) | Fast | Comparing street types, speeds, walkability; large cities may use approximate or skipped deep metrics |
| **Full** | Slow (hours possible on huge graphs) | Research needing exact betweenness, closeness, and related structural metrics |

Street statistics from OSM tags are the same in both modes. Only the deeper graph calculations differ.

## Configuration

### Saved files (user home)

All persistent app data lives under:

```
~/.network-analysis-tool/
├── session.json          # Processed metrics, folder path, analysis text, chat history
├── session.json.bak      # Backup of the previous session
├── gemini.properties     # Gemini API key and model (if configured in the UI)
└── data360-cache/        # Cached World Bank API responses (one JSON file per indicator)
```

### Gemini API (optional)

1. Get a key from [Google AI Studio](https://aistudio.google.com/apikey).
2. In the app: **AI Advisor** tab - **Gemini Settings**.
3. Or set environment variables (these override the properties file):
   - `GEMINI_API_KEY` or `GOOGLE_API_KEY`
   - `GEMINI_MODEL` (default: `gemini-3.1-flash-lite`)

### World Bank offline mode

Set JVM flag `-Ddata360.offline=true` to skip live API calls and use only cached indicator files.

## Project structure

```
src/main/java/org/example/
├── NetworkAnalysisMain.java      # Entry point (GUI or CLI)
├── controller/
│   └── NetworkController.java    # MVC controller: file processing, charts, session, export
├── model/                        # NetworkMetrics, StreetNetworkStats, World Bank DTOs, etc.
├── service/                      # GraphML loading, analysis, export, prediction, Gemini, Data360
├── util/                         # Reports, charts, statistics, parsers, map helpers
├── view/                         # Swing tabs and panels
└── theme/                        # Shared UI styles and readable text rendering

src/test/java/                    # Unit and integration tests
DataSets/                         # Bundled GraphML networks and reference CSV
```

## Metrics overview

For each city the app computes:

- **Street / OSM metrics:** total road km, highway mix, speed limits, lanes, one-way ratio, pedestrian and cycling share, bridges/tunnels, streets per junction
- **Graph metrics:** node/edge counts, betweenness, closeness, degree centrality, clustering, diameter, density, average path length, entropy, assortativity, reciprocity, and more

Graph metrics are interpreted in the context of urban mobility research:

> Sierra-Porta, D. & Herrera-Acevedo, D.D. (2024). *Network structure and urban mobility sustainability.* Universidad Tecnológica de Bolívar (UTB).

See the in-app **Data Guide** tab for column-by-column explanations of the full metrics table.

## Technology

- [JGraphT](https://jgrapht.org/) – graph algorithms
- [JFreeChart](https://www.jfree.org/jfreechart/) – charts
- [OpenCSV](https://opencsv.sourceforge.net/) – CSV export
- [Jackson](https://github.com/FasterXML/jackson) – session JSON and API parsing
- [Apache Commons Math](https://commons.apache.org/proper/commons-math/) – regression and statistics
- [SLF4J](https://www.slf4j.org/) – logging

## Data sources and attribution

- Street network data: [OpenStreetMap contributors](https://www.openstreetmap.org/copyright) (typically extracted via OSMnx into GraphML)
- World Bank indicators: [Data360 API](https://data360.worldbank.org/)
- UMRi reference scores: Oliver Wyman Forum (2023), via `Results_Cities.csv`

## License

No license file is included in this repository. Add one if you plan to distribute the project.
