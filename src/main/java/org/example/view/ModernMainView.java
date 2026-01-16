package org.example.view;

import org.example.ai.AIAssistantPanel;
import org.example.controller.DataController;
import org.example.controller.MainController;
import org.example.model.ProcurementItem;
import org.example.scripting.PythonScriptingService;
import org.example.theme.ThemeManager;
import org.example.util.EnhancedChartGenerator;
import org.example.util.SoundManager;
import org.example.util.DeepAnalysisChartGenerator;
import org.jfree.chart.JFreeChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.example.view.MachineLearningPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class ModernMainView {
    private JFrame frame;
    private JMenuBar menuBar;
    private JToolBar toolBar;
    private JTabbedPane tabbedPane;
    private JPanel mainPanel;
    private JList<ProcurementItem> procurementItemsList;
    private JScrollPane procurementItemsScrollPane;
    private JTextField searchField;
    private JButton searchButton;
    private ModernChartDisplayPanel chartsPanel;
    private JTextArea analysisTextArea;
    private JScrollPane analysisScrollPane;
    private JLabel statusLabel;
    private JButton exportAnalysisButton;
    private JButton printAnalysisButton;
    private JComboBox<String> themeSelector;
    private AIAssistantPanel aiAssistantPanel;
    private DashboardPanel dashboardPanel;
    private EnhancedFileImportPanel enhancedImportPanel;
    private ScriptingPanel scriptingPanel;
    private MachineLearningPanel machineLearningPanel;

    private JMenuItem loadPaapMenuItem;
    private JMenuItem loadCpvCodesMenuItem;
    private JMenuItem exportAnalysisMenuItem;
    private JMenuItem printAnalysisMenuItem;
    private JMenuItem exitMenuItem;
    private JMenuItem aboutMenuItem;

    private JButton loadPaapButton;
    private JButton loadCpvCodesButton;
    private JButton generateChartsButton;
    private JButton generateAnalysisButton;

    private MainController mainController;
    private DataController dataController;

    public ModernMainView() {
        initialize();
    }

    private void initialize() {
        frame = new JFrame("CPV Analysis Tool - Professional Edition");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1400, 900);
        frame.setLocationRelativeTo(null);

        createMenuBar();
        frame.setJMenuBar(menuBar);

        createToolBar();

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        tabbedPane.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        createTabs();

        mainPanel.add(tabbedPane, BorderLayout.CENTER);

        createStatusBar();

        frame.add(toolBar, BorderLayout.NORTH);
        frame.add(mainPanel, BorderLayout.CENTER);
    }

    private void createTabs() {
        dashboardPanel = new DashboardPanel();
        tabbedPane.addTab("📊 Dashboard", null, dashboardPanel, "Executive dashboard with KPIs and insights");

        JPanel searchPanel = createSearchPanel();
        tabbedPane.addTab("🔍 Search", null, searchPanel, "Search and filter procurement items");

        chartsPanel = new ModernChartDisplayPanel();
        JScrollPane chartsScrollPane = new JScrollPane(chartsPanel);
        chartsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chartsScrollPane.setBorder(BorderFactory.createEmptyBorder());
        chartsScrollPane.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        tabbedPane.addTab("📈 Charts", null, chartsScrollPane, "Advanced data visualizations and analytics");

        JPanel analysisPanel = createAnalysisPanel();
        tabbedPane.addTab("📋 Analysis", null, analysisPanel, "Comprehensive procurement analysis reports");

        aiAssistantPanel = new AIAssistantPanel();
        tabbedPane.addTab("🤖 AI Assistant", null, aiAssistantPanel, "AI-powered data analysis and insights");

        enhancedImportPanel = new EnhancedFileImportPanel();
        tabbedPane.addTab("📂 Import", null, enhancedImportPanel, "Import PAAP and CPV files");

        scriptingPanel = new ScriptingPanel();
        tabbedPane.addTab("🐍 Scripting", null, scriptingPanel, "Python scripting for custom analysis");

        machineLearningPanel = new MachineLearningPanel();
        tabbedPane.addTab("🤖 Machine Learning", null, machineLearningPanel,
                "AI-powered analysis: prediction, classification, clustering, and anomaly detection");
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();
        menuBar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JMenu fileMenu = new JMenu("File");
        fileMenu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        loadPaapMenuItem = new JMenuItem("Load PAAP File...");
        loadPaapMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl P"));
        loadPaapMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        loadCpvCodesMenuItem = new JMenuItem("Load CPV Codes File...");
        loadCpvCodesMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl C"));
        loadCpvCodesMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        exportAnalysisMenuItem = new JMenuItem("Export Analysis...");
        exportAnalysisMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl E"));
        exportAnalysisMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        printAnalysisMenuItem = new JMenuItem("Print Analysis...");
        printAnalysisMenuItem.setAccelerator(KeyStroke.getKeyStroke("ctrl P"));
        printAnalysisMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        exitMenuItem = new JMenuItem("Exit");
        exitMenuItem.setAccelerator(KeyStroke.getKeyStroke("alt F4"));
        exitMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        exitMenuItem.addActionListener(e -> System.exit(0));

        fileMenu.add(loadPaapMenuItem);
        fileMenu.add(loadCpvCodesMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exportAnalysisMenuItem);
        fileMenu.add(printAnalysisMenuItem);
        fileMenu.addSeparator();
        fileMenu.add(exitMenuItem);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JMenu themesMenu = new JMenu("Themes");
        themesMenu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        for (String themeName : ThemeManager.getAvailableThemes()) {
            JMenuItem themeItem = new JMenuItem(themeName);
            themeItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
            themeItem.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            themeItem.addActionListener(e -> {
                ThemeManager.applyTheme(themeName);
                themeSelector.setSelectedItem(themeName);
                updateTheme();
                SoundManager.playSound(SoundManager.SOUND_THEME_CHANGE);
            });
            themesMenu.add(themeItem);
        }

        JMenuItem soundToggleItem = new JMenuItem("Toggle Sound");
        soundToggleItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        soundToggleItem.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        soundToggleItem.addActionListener(e -> {
            SoundManager.toggleSound();
            JOptionPane.showMessageDialog(frame,
                    "Sound " + (SoundManager.isSoundEnabled() ? "enabled" : "disabled"),
                    "Sound Settings", JOptionPane.INFORMATION_MESSAGE);
            if (SoundManager.isSoundEnabled()) {
                SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            }
        });

        viewMenu.add(themesMenu);
        viewMenu.addSeparator();
        viewMenu.add(soundToggleItem);

        JMenu analyticsMenu = new JMenu("Analytics");
        analyticsMenu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JMenuItem deepAnalysisItem = new JMenuItem("Generate Deep Analysis");
        deepAnalysisItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        deepAnalysisItem.addActionListener(e -> generateDeepAnalysis());

        JMenuItem riskAssessmentItem = new JMenuItem("Risk Assessment");
        riskAssessmentItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        riskAssessmentItem.addActionListener(e -> generateRiskAssessment());

        JMenuItem maturityAssessmentItem = new JMenuItem("Maturity Assessment");
        maturityAssessmentItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        maturityAssessmentItem.addActionListener(e -> generateMaturityAssessment());

        JMenuItem mlPredictionItem = new JMenuItem("ML Prediction");
        mlPredictionItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        mlPredictionItem.addActionListener(e -> {
            tabbedPane.setSelectedIndex(8);
            if (machineLearningPanel != null) {
                machineLearningPanel.mlTabbedPane.setSelectedIndex(0);
            }
        });

        JMenuItem mlClusteringItem = new JMenuItem("ML Clustering");
        mlClusteringItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        mlClusteringItem.addActionListener(e -> {
            tabbedPane.setSelectedIndex(8);
            if (machineLearningPanel != null) {
                machineLearningPanel.mlTabbedPane.setSelectedIndex(2);
            }
        });

        JMenuItem mlAnomalyItem = new JMenuItem("ML Anomaly Detection");
        mlAnomalyItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        mlAnomalyItem.addActionListener(e -> {
            tabbedPane.setSelectedIndex(8);
            if (machineLearningPanel != null) {
                machineLearningPanel.mlTabbedPane.setSelectedIndex(3);
            }
        });

        analyticsMenu.add(deepAnalysisItem);
        analyticsMenu.add(riskAssessmentItem);
        analyticsMenu.add(maturityAssessmentItem);
        analyticsMenu.add(mlPredictionItem);
        analyticsMenu.add(mlClusteringItem);
        analyticsMenu.add(mlAnomalyItem);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        aboutMenuItem = new JMenuItem("About");
        aboutMenuItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        aboutMenuItem.addActionListener(e -> showAboutDialog());

        JMenuItem userGuideItem = new JMenuItem("User Guide");
        userGuideItem.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        userGuideItem.addActionListener(e -> showUserGuide());

        helpMenu.add(userGuideItem);
        helpMenu.addSeparator();
        helpMenu.add(aboutMenuItem);

        menuBar.add(fileMenu);
        menuBar.add(viewMenu);
        menuBar.add(analyticsMenu);
        menuBar.add(helpMenu);
    }

    private void createToolBar() {
        toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        toolBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, ThemeManager.getCurrentTheme().getBorderColor()),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        loadPaapButton = createToolbarButton("📄 Load PAAP", "Load PAAP Excel file");
        loadCpvCodesButton = createToolbarButton("📋 Load CPV", "Load CPV Codes Excel file");

        toolBar.add(loadPaapButton);
        toolBar.add(loadCpvCodesButton);
        toolBar.addSeparator(new Dimension(15, 30));

        generateChartsButton = createToolbarButton("📊 Charts", "Generate all charts");
        generateAnalysisButton = createToolbarButton("📈 Analysis", "Generate analysis report");

        toolBar.add(generateChartsButton);
        toolBar.add(generateAnalysisButton);
        toolBar.addSeparator(new Dimension(15, 30));

        JButton dashboardButton = createToolbarButton("🏠 Dashboard", "Go to dashboard");
        dashboardButton.addActionListener(e -> tabbedPane.setSelectedIndex(0));
        toolBar.add(dashboardButton);
    }

    private JButton createToolbarButton(String text, String tooltip) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        button.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor()),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        return button;
    }

    private JPanel createSearchPanel() {
        JPanel panel = ThemeManager.createThemedPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel searchControlsPanel = new JPanel(new BorderLayout(10, 0));
        searchControlsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        searchControlsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Search Procurement Items"),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        JLabel searchLabel = new JLabel("🔍 Search:");
        searchLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        searchField = new JTextField();
        searchField.setToolTipText("Enter CPV code, item name, or description");
        searchField.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        searchField.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        searchField.setCaretColor(ThemeManager.getCurrentTheme().getTextColor());
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ThemeManager.getCurrentTheme().getBorderColor()),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));

        searchButton = ThemeManager.createThemedButton("Search");
        JButton clearButton = ThemeManager.createThemedButton("Clear");
        clearButton.addActionListener(e -> {
            searchField.setText("");
            if (dataController != null) {
                updateProcurementItemsList(dataController.getModel().getProcurementItems());
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        buttonPanel.add(searchButton);
        buttonPanel.add(clearButton);

        searchControlsPanel.add(searchLabel, BorderLayout.WEST);
        searchControlsPanel.add(searchField, BorderLayout.CENTER);
        searchControlsPanel.add(buttonPanel, BorderLayout.EAST);

        procurementItemsList = new JList<>(new DefaultListModel<>());
        procurementItemsList.setCellRenderer(new ModernProcurementItemCellRenderer());
        procurementItemsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        procurementItemsList.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        procurementItemsList.setSelectionBackground(ThemeManager.getCurrentTheme().getAccentColor());
        procurementItemsList.setSelectionForeground(Color.WHITE);

        procurementItemsScrollPane = new JScrollPane(procurementItemsList);
        procurementItemsScrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));
        procurementItemsScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        procurementItemsScrollPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        procurementItemsScrollPane.getViewport().setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        statsPanel.setBorder(BorderFactory.createTitledBorder("Search Statistics"));

        JLabel resultsLabel = new JLabel("No search performed");
        resultsLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        resultsLabel.setName("searchStats");
        statsPanel.add(resultsLabel);

        panel.add(searchControlsPanel, BorderLayout.NORTH);
        panel.add(procurementItemsScrollPane, BorderLayout.CENTER);
        panel.add(statsPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAnalysisPanel() {
        JPanel panel = ThemeManager.createThemedPanel();
        panel.setLayout(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        controlsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        controlsPanel.setBorder(BorderFactory.createTitledBorder("Analysis Options"));

        JButton basicAnalysisBtn = ThemeManager.createThemedButton("📊 Basic Analysis");
        JButton deepAnalysisBtn = ThemeManager.createThemedButton("🔍 Deep Analysis");
        JButton riskAnalysisBtn = ThemeManager.createThemedButton("⚠️ Risk Analysis");
        JButton maturityAnalysisBtn = ThemeManager.createThemedButton("📈 Maturity Analysis");

        basicAnalysisBtn.addActionListener(e -> generateBasicAnalysis());
        deepAnalysisBtn.addActionListener(e -> generateDeepAnalysis());
        riskAnalysisBtn.addActionListener(e -> generateRiskAssessment());
        maturityAnalysisBtn.addActionListener(e -> generateMaturityAssessment());

        controlsPanel.add(basicAnalysisBtn);
        controlsPanel.add(deepAnalysisBtn);
        controlsPanel.add(riskAnalysisBtn);
        controlsPanel.add(maturityAnalysisBtn);

        analysisTextArea = new JTextArea();
        analysisTextArea.setEditable(false);
        analysisTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        analysisTextArea.setMargin(new Insets(15, 15, 15, 15));
        analysisTextArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        analysisTextArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        analysisTextArea.setText("Select an analysis type to begin comprehensive procurement analysis.");

        analysisScrollPane = new JScrollPane(analysisTextArea);
        analysisScrollPane.setBorder(BorderFactory.createTitledBorder("Analysis Results"));
        analysisScrollPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JPanel exportPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        exportPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        exportAnalysisButton = ThemeManager.createThemedButton("💾 Export");
        exportAnalysisButton.setToolTipText("Export analysis to file");

        printAnalysisButton = ThemeManager.createThemedButton("🖨️ Print");
        printAnalysisButton.setToolTipText("Print analysis report");

        exportPanel.add(exportAnalysisButton);
        exportPanel.add(printAnalysisButton);

        panel.add(controlsPanel, BorderLayout.NORTH);
        panel.add(analysisScrollPane, BorderLayout.CENTER);
        panel.add(exportPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void createStatusBar() {
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, ThemeManager.getCurrentTheme().getBorderColor()),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        statusLabel = new JLabel("🟢 Ready - Load PAAP and CPV files to begin analysis");
        statusLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        themeSelector = new JComboBox<>(ThemeManager.getAvailableThemes());
        themeSelector.setSelectedItem(ThemeManager.getCurrentThemeName());
        themeSelector.setToolTipText("Change application theme");
        themeSelector.addActionListener(e -> {
            String selectedTheme = (String) themeSelector.getSelectedItem();
            ThemeManager.applyTheme(selectedTheme);
            updateTheme();
            SoundManager.playSound(SoundManager.SOUND_THEME_CHANGE);
        });

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        infoPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        JLabel themeLabel = new JLabel("Theme:");
        themeLabel.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        infoPanel.add(themeLabel);
        infoPanel.add(themeSelector);

        statusPanel.add(statusLabel, BorderLayout.CENTER);
        statusPanel.add(infoPanel, BorderLayout.EAST);

        mainPanel.add(statusPanel, BorderLayout.SOUTH);
    }

    private void generateBasicAnalysis() {
        if (dataController == null || dataController.getModel().getProcurementItems().isEmpty()) {
            showErrorMessage("Please load procurement data first.");
            return;
        }

        setStatusMessage("🔄 Generating basic analysis...");

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return org.example.controller.AnalysisService.generateAnalysisReport(
                        dataController.getModel(), dataController.getModel().getCpvCodeMap());
            }

            @Override
            protected void done() {
                try {
                    String analysis = get();
                    displayAnalysis(analysis);
                    setStatusMessage("✅ Basic analysis complete");
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    showErrorMessage("Error generating analysis: " + e.getMessage());
                    setStatusMessage("❌ Analysis failed");
                }
            }
        };
        worker.execute();
    }

    private void generateDeepAnalysis() {
        if (dataController == null || dataController.getModel().getProcurementItems().isEmpty()) {
            showErrorMessage("Please load procurement data first.");
            return;
        }

        setStatusMessage("🔄 Generating deep analysis with advanced charts...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                String analysis = org.example.util.ProcurementAnalysisService.generateComprehensiveAnalysis(
                        dataController.getModel());

                SwingUtilities.invokeLater(() -> {
                    displayAnalysis(analysis);
                    generateAllAdvancedCharts();
                });

                return null;
            }

            @Override
            protected void done() {
                setStatusMessage("✅ Deep analysis complete with advanced visualizations");
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
            }
        };
        worker.execute();
    }
    private static final Logger logger = LoggerFactory.getLogger(ModernMainView.class);
    private void generateAllAdvancedCharts() {
        if (dataController == null || dataController.getModel().getProcurementItems().isEmpty()) {
            showErrorMessage("Please load procurement data first.");
            return;
        }

        if (dataController.getModel().getCpvCodeMap().isEmpty()) {
            showErrorMessage("Please load CPV codes file for complete analysis.");
            return;
        }

        setStatusMessage("🔄 Generating comprehensive charts with monthly analysis...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                try {
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.clearCharts();
                    });

                    JFreeChart categoryChart = EnhancedChartGenerator.generateEnhancedCategoryPieChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(categoryChart, "Procurement by Category (Fixed Labels)");
                    });

                    JFreeChart topItemsChart = EnhancedChartGenerator.generateEnhancedTopItemsBarChart(dataController.getModel(), 10);
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(topItemsChart, "Top 10 Items (Fixed Labels)");
                    });

                    JFreeChart valueRangeChart = EnhancedChartGenerator.generateEnhancedValueRangeChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(valueRangeChart, "Value Range Analysis (Fixed Labels)");
                    });

                    JFreeChart monthlyChart = EnhancedChartGenerator.generateMonthlyAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(monthlyChart, "Monthly Procurement Analysis");
                    });

                    JFreeChart monthlyTrendChart = EnhancedChartGenerator.generateMonthlyTrendChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(monthlyTrendChart, "Monthly Procurement Trend");
                    });

                    JFreeChart seasonalChart = EnhancedChartGenerator.generateSeasonalAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(seasonalChart, "Seasonal Analysis");
                    });

                    JFreeChart valueAnalysis = DeepAnalysisChartGenerator.generateValueAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(valueAnalysis, "Value Analysis (Enhanced)");
                    });

                    JFreeChart concentration = DeepAnalysisChartGenerator.generateCategoryConcentrationChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(concentration, "Category Concentration (Fixed Labels)");
                    });

                    JFreeChart efficiency = DeepAnalysisChartGenerator.generateEfficiencyAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(efficiency, "Efficiency Analysis (Fixed Labels)");
                    });

                    JFreeChart timeline = DeepAnalysisChartGenerator.generateTimelineAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(timeline, "Timeline Analysis");
                    });

                    JFreeChart maturity = DeepAnalysisChartGenerator.generateMaturityAssessmentChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(maturity, "Maturity Assessment");
                    });

                    JFreeChart risk = DeepAnalysisChartGenerator.generateRiskAssessmentChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(risk, "Risk Assessment");
                    });

                    JFreeChart monthlyTrendAnalysis = DeepAnalysisChartGenerator.generateMonthlyTrendAnalysisChart(dataController.getModel());
                    SwingUtilities.invokeLater(() -> {
                        chartsPanel.addChart(monthlyTrendAnalysis, "Monthly Trend Analysis (Advanced)");
                    });

                } catch (Exception e) {
                    logger.error("Error generating charts: {}", e.getMessage(), e);
                    SwingUtilities.invokeLater(() -> {
                        showErrorMessage("Error generating charts: " + e.getMessage());
                    });
                }

                return null;
            }

            @Override
            protected void done() {
                setStatusMessage("✅ All charts generated successfully with fixed labels and monthly analysis");
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
            }
        };

        worker.execute();
    }
    public void setupScriptingService() {
        if (dataController != null && scriptingPanel != null) {
            PythonScriptingService scriptingService = new PythonScriptingService(dataController.getModel());
            scriptingPanel.setScriptingService(scriptingService);
        }
    }
    private void generateRiskAssessment() {
        if (dataController == null || dataController.getModel().getProcurementItems().isEmpty()) {
            showErrorMessage("Please load procurement data first.");
            return;
        }

        setStatusMessage("🔄 Generating risk assessment...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                JFreeChart riskChart = DeepAnalysisChartGenerator.generateRiskAssessmentChart(dataController.getModel());

                SwingUtilities.invokeLater(() -> {
                    chartsPanel.clearCharts();
                    chartsPanel.addChart(riskChart, "Risk Assessment");
                    tabbedPane.setSelectedIndex(2);
                });

                return null;
            }

            @Override
            protected void done() {
                setStatusMessage("✅ Risk assessment complete");
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
            }
        };
        worker.execute();
    }

    private void generateMaturityAssessment() {
        if (dataController == null || dataController.getModel().getProcurementItems().isEmpty()) {
            showErrorMessage("Please load procurement data first.");
            return;
        }

        setStatusMessage("🔄 Generating maturity assessment...");

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                JFreeChart maturityChart = DeepAnalysisChartGenerator.generateMaturityAssessmentChart(dataController.getModel());

                SwingUtilities.invokeLater(() -> {
                    chartsPanel.clearCharts();
                    chartsPanel.addChart(maturityChart, "Procurement Maturity Assessment");
                    tabbedPane.setSelectedIndex(2);
                });

                return null;
            }

            @Override
            protected void done() {
                setStatusMessage("✅ Maturity assessment complete");
                SoundManager.playSound(SoundManager.SOUND_SUCCESS);
            }
        };
        worker.execute();
    }

    public void updateAIAssistant() {
        if (aiAssistantPanel != null && dataController != null) {
            aiAssistantPanel.setDataModel(dataController.getModel());
        }
        updateMLPanel();
    }

    public void updateDashboard() {
        if (dashboardPanel != null && dataController != null) {
            dashboardPanel.updateDashboard(dataController.getModel());
        }
    }

    public void updateMLPanel() {
        if (machineLearningPanel != null && dataController != null) {
            machineLearningPanel.setDataModel(dataController.getModel());
        }
    }

    public void setLoadPaapAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            listener.actionPerformed(e);
        };
        loadPaapMenuItem.addActionListener(soundWrapper);
        loadPaapButton.addActionListener(soundWrapper);
    }

    public void setLoadCpvCodesAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            listener.actionPerformed(e);
        };
        loadCpvCodesMenuItem.addActionListener(soundWrapper);
        loadCpvCodesButton.addActionListener(soundWrapper);
    }

    public void setSearchAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            listener.actionPerformed(e);
        };
        searchButton.addActionListener(soundWrapper);
        searchField.addActionListener(soundWrapper);
    }

    public void setGenerateChartsAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            generateAllAdvancedCharts();
        };
        generateChartsButton.addActionListener(soundWrapper);
    }

    public void setGenerateAnalysisAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            generateBasicAnalysis();
        };
        generateAnalysisButton.addActionListener(soundWrapper);
    }

    public void setExportAnalysisAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            listener.actionPerformed(e);
        };
        exportAnalysisMenuItem.addActionListener(soundWrapper);
        exportAnalysisButton.addActionListener(soundWrapper);
    }

    public void setPrintAnalysisAction(ActionListener listener) {
        ActionListener soundWrapper = e -> {
            SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);
            listener.actionPerformed(e);
        };
        printAnalysisMenuItem.addActionListener(soundWrapper);
        printAnalysisButton.addActionListener(soundWrapper);
    }

    public void setEnhancedImportListeners(ActionListener cpvCodeListener, ActionListener procurementDataListener) {
        if (enhancedImportPanel != null) {
            enhancedImportPanel.setCpvCodeImportListener(cpvCodeListener);
            enhancedImportPanel.setProcurementDataImportListener(procurementDataListener);
        }
    }

    public void updateProcurementItemsList(List<ProcurementItem> items) {
        DefaultListModel<ProcurementItem> model = new DefaultListModel<>();
        for (ProcurementItem item : items) {
            model.addElement(item);
        }
        procurementItemsList.setModel(model);

        if (!items.isEmpty()) {
            procurementItemsList.setSelectedIndex(0);
        }

        updateSearchStats(items.size());

        updateDashboard();
    }
    public void updateScriptingService() {
        if (dataController != null && scriptingPanel != null) {
            PythonScriptingService scriptingService = new PythonScriptingService(dataController.getModel());
            scriptingPanel.setScriptingService(scriptingService);
            setStatusMessage("🐍 Python scripting service initialized");
        }
    }
    private void updateSearchStats(int resultCount) {
        JLabel statsLabel = findLabelByName(this.getFrame(), "searchStats");
        if (statsLabel != null) {
            if (resultCount == 0) {
                statsLabel.setText("📊 No results found");
            } else {
                double totalValue = 0;
                if (dataController != null) {
                    totalValue = dataController.getModel().getProcurementItems().stream()
                            .mapToDouble(org.example.model.ProcurementItem::getValueWithoutTVA)
                            .sum();
                }
                statsLabel.setText(String.format("📊 Found %d items • Total value: %.2f RON",
                        resultCount, totalValue));
            }
        }
    }

    private JLabel findLabelByName(Container container, String name) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JLabel && name.equals(comp.getName())) {
                return (JLabel) comp;
            } else if (comp instanceof Container) {
                JLabel found = findLabelByName((Container) comp, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    public String getSearchQuery() {
        return searchField.getText().trim();
    }

    public void displayChart(JFreeChart chart, String title) {
        chartsPanel.addChart(chart, title);
        tabbedPane.setSelectedIndex(2);
    }

    public void clearCharts() {
        chartsPanel.clearCharts();
    }

    public void displayAnalysis(String analysis) {
        analysisTextArea.setText(analysis);
        analysisTextArea.setCaretPosition(0);
        tabbedPane.setSelectedIndex(3);
    }

    public String getAnalysisText() {
        return analysisTextArea.getText();
    }

    public void showErrorMessage(String message) {
        SoundManager.playSound(SoundManager.SOUND_ERROR);
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public void setStatusMessage(String message) {
        statusLabel.setText(" " + message);
    }

    public JFrame getFrame() {
        return frame;
    }

    public void show() {
        frame.setVisible(true);
    }

    private void showAboutDialog() {
        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        String aboutText = """
            <html><body style='width: 300px; font-family: Arial;'>
            <h2 style='color: #1976D2;'>CPV Analysis Tool</h2>
            <h3>Professional Edition v1.0</h3>
            
            <p><b>Features:</b></p>
            <ul>
                <li>📊 Interactive Dashboard</li>
                <li>🔍 Advanced Search & Filtering</li>
                <li>📈 Deep Analytics & Insights</li>
                <li>⚠️ Risk Assessment</li>
                <li>📋 Maturity Analysis</li>
                <li>🤖 AI-Powered Assistant</li>
                <li>🎨 Multiple Themes</li>
            </ul>
            
            <p><b>Supported Files:</b></p>
            <ul>
                <li>PAAP Excel files (.xlsx, .xls)</li>
                <li>CPV Codes Excel files (.xlsx, .xls)</li>
            </ul>
            
            <hr>
            <p><small>© 2025 Your Organization</small></p>
            </body></html>
            """;

        JOptionPane.showMessageDialog(frame, aboutText, "About CPV Analysis Tool",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showUserGuide() {
        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        String guideText = """
            <html><body style='width: 400px; font-family: Arial;'>
            <h2>Quick Start Guide</h2>
            
            <h3>1. Load Your Data</h3>
            <p>• Load your PAAP Excel file (File → Load PAAP File)</p>
            <p>• Load your CPV Codes Excel file (File → Load CPV Codes File)</p>
            
            <h3>2. Explore Your Data</h3>
            <p>• <b>Dashboard:</b> View KPIs and key insights</p>
            <p>• <b>Search:</b> Find specific procurement items</p>
            <p>• <b>Charts:</b> Visualize your procurement data</p>
            
            <h3>3. Generate Analysis</h3>
            <p>• <b>Basic Analysis:</b> Standard procurement report</p>
            <p>• <b>Deep Analysis:</b> Comprehensive insights</p>
            <p>• <b>Risk Assessment:</b> Identify procurement risks</p>
            <p>• <b>Maturity Analysis:</b> Assess procurement maturity</p>
            
            <h3>4. Use AI Assistant</h3>
            <p>Ask natural language questions about your data!</p>
            
            <h3>5. Export Results</h3>
            <p>Export analysis reports and charts for presentations.</p>
            
            </body></html>
            """;

        JOptionPane.showMessageDialog(frame, guideText, "User Guide",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public void updateTheme() {
        frame.getContentPane().setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        menuBar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        updateMenuColors(menuBar);

        toolBar.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        for (Component c : toolBar.getComponents()) {
            if (c instanceof JButton) {
                JButton button = (JButton) c;
                button.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                button.setForeground(ThemeManager.getCurrentTheme().getTextColor());
            }
        }

        mainPanel.setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        tabbedPane.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        tabbedPane.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        if (dashboardPanel != null) dashboardPanel.updateTheme();
        if (chartsPanel != null) chartsPanel.updateTheme();
        if (aiAssistantPanel != null) aiAssistantPanel.updateTheme();
        if (enhancedImportPanel != null) enhancedImportPanel.updateTheme();
        if (machineLearningPanel != null) machineLearningPanel.updateTheme();

        updateComponentTheme(this.mainPanel);

        SwingUtilities.updateComponentTreeUI(frame);
    }

    private void updateMenuColors(JMenuBar menuBar) {
        for (int i = 0; i < menuBar.getMenuCount(); i++) {
            JMenu menu = menuBar.getMenu(i);
            menu.setForeground(ThemeManager.getCurrentTheme().getTextColor());

            for (int j = 0; j < menu.getItemCount(); j++) {
                JMenuItem item = menu.getItem(j);
                if (item != null) {
                    item.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                    item.setForeground(ThemeManager.getCurrentTheme().getTextColor());
                }
            }
        }
    }

    private void updateComponentTheme(Container container) {
        for (Component comp : container.getComponents()) {
            if (comp instanceof JPanel) {
                ((JPanel) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            } else if (comp instanceof JLabel) {
                ((JLabel) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JButton) {
                ((JButton) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JButton) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTextField) {
                ((JTextField) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextField) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
                ((JTextField) comp).setCaretColor(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JTextArea) {
                ((JTextArea) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextArea) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JList) {
                ((JList<?>) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JList<?>) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
                ((JList<?>) comp).setSelectionBackground(ThemeManager.getCurrentTheme().getAccentColor());
                ((JList<?>) comp).setSelectionForeground(Color.WHITE);
            } else if (comp instanceof JScrollPane) {
                ((JScrollPane) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JScrollPane) comp).getViewport().setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            }

            if (comp instanceof Container) {
                updateComponentTheme((Container) comp);
            }
        }
    }

    public void setMainController(MainController controller) {
        this.mainController = controller;
    }

    public void setDataController(DataController controller) {
        this.dataController = controller;
    }
}