# Extracting information from open data

A Java desktop application for analysing **urban street networks** from OpenStreetMap **GraphML** files. Load city networks, compute graph- and street-level metrics, compare cities visually, enrich results with **World Bank** indicators, estimate **Urban Mobility Readiness (UMRi)**, and explore findings through reports, charts, and an optional **Google Gemini** AI advisor.

The app runs as a **Swing GUI** (default) or a **command-line batch tool** for scripted exports.

## Features

| Tab / area | What it does |
|------------|--------------|
| **Dashboard** | Everyday street KPIs: road length, one-way share, speed limits, walk/cycle share, and more |
| **Files** | Browse a folder of `.graphml` files, select cities, choose Quick or Full analysis, and process |
| **Map** | Interactive street map coloured by OSM tags (highway type, speed, one-way, bridges/tunnels) |
| **Charts** | Side-by-side bar charts for street and network metrics; generate from the Charts tab |
| **World Bank** | Country-level socioeconomic indicators via the [Data360 API](https://data360.worldbank.org/) |
| **Prediction** | UMRi estimate from network structure (trained on 62 reference cities) |
| **Analysis** | Full written comparative report with research context |
| **AI Advisor** | Gemini-powered Q&A about your loaded cities (optional API key) |
| **Data Guide** | Plain-language explanation of OSM tags, metrics, and the full metrics table |
| **Results** | Sortable table of all computed values with CSV export |

**Session persistence:** processed metrics, UI state, analysis text, and chat history are saved automatically and restored on the next launch.

## Data Files

The application analyses two kinds of data, **both included in a single dataset that must be downloaded separately** (see below):

1. **City Street Networks** (`DataSets/Graphs/*.graphml`): 62 world-city OpenStreetMap street networks in GraphML format, named `City,_Country.graphml` (underscores instead of spaces).
2. **Reference Scores** (`DataSets/Results_Cities.csv`): Reference graph metrics and official UMRi / SMi / PTi scores used to train the prediction model and to draw the comparison charts.


## Getting Started

### Prerequisites
- **Java 17+** (the project compiles with `release 21` by default; Java 17 is supported via the `prod` Maven profile)
- **Maven 3.6+**
- Internet access for the World Bank Data360 API and Gemini (optional, already-cached and local-only features work offline)
- Gemini API key for using the Gemini AI assistant (also optional)

### 1. Clone the repository

```bash
git clone https://github.com/bughi04/OpenDataInfoExtraction.git
cd OpenDataInfoExtraction
```

### 2. Download the dataset

The GraphML networks and the reference CSV are **not bundled in the repository** and must be downloaded from Mendeley Data:

**https://data.mendeley.com/datasets/gmyt9wrgst/1**

After downloading and unzipping, place the files so the project root looks like this:

```
OpenDataInfoExtraction/
├── pom.xml
├── DataSets/
│   ├── Graphs/
│   │   ├── Amsterdam,_Netherlands.graphml
│   │   ├── Barcelona,_Spain.graphml
│   │   └── ... (62 city files)
│   └── Results_Cities.csv
└── src/
```

> **Important**: The app looks for `DataSets/Graphs/` and `DataSets/Results_Cities.csv` relative to the directory you run it from. Keep the `DataSets/` folder in the project root (or launch from there) so cities and prediction/charts load automatically.

### 3. Build

```bash
mvn clean package
```

This produces a runnable JAR:

```
target/network-analysis-tool.jar
```

### 4. Run the GUI

```bash
java -jar target/network-analysis-tool.jar
```

Or without packaging:

```bash
mvn exec:java
```

On first launch, open the **Files** tab, click **Browse Folder**, and point to `DataSets/Graphs/`. Select cities, choose **Quick** or **Full** analysis, and click **Process Selected**.

### Gemini API (optional)

1. Get a key from [Google AI Studio](https://aistudio.google.com/apikey).
2. In the app: **AI Advisor** tab - **Gemini Settings**.
3. Or set environment variables (these override the properties file):
   - `GEMINI_API_KEY` or `GOOGLE_API_KEY`
   - `GEMINI_MODEL` (default: `gemini-3.1-flash-lite`)

### 5. Run from the command line (optional)

Process a single file (Quick analysis; exports `*_all_metrics.csv` next to the input):

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

### 6. Run tests

```bash
mvn test
```

Some prediction and round-trip tests require `DataSets/Results_Cities.csv` in the project root.

## Usage Workflow

1. **Download Data**: Get the dataset from Mendeley and place it under `DataSets/`
2. **Load**: Open the **Files** tab, browse to `DataSets/Graphs/`, and process selected cities
3. **Explore**: Inspect the **Dashboard** KPIs and the interactive **Map**
4. **Analyse**: Generate **Charts** and read the written **Analysis** report
5. **Enrich**: Pull country indicators in the **World Bank** tab
6. **Predict**: View the UMRi estimate in the **Prediction** tab
7. **Query**: Ask the **AI Advisor** questions about your loaded cities (optional Gemini key)
8. **Export**: Save all computed values from the **Results** tab to CSV

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
- UMRi reference scores: Oliver Wyman Forum (2024), via `Results_Cities.csv`
