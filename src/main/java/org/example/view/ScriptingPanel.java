package org.example.view;

import org.example.scripting.PythonScriptingService;
import org.example.theme.ThemeManager;
import org.example.util.SoundManager;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScriptingPanel extends JPanel {
    private PythonScriptingService scriptingService;
    private RSyntaxTextArea scriptArea;
    private JTextArea outputArea;
    private JComboBox<String> scriptSelector;
    private JButton runButton;
    private JButton saveButton;
    private JButton loadButton;
    private JPanel exampleButtonsPanel;

    private List<String> exampleScripts;

    public ScriptingPanel() {
        initializeUI();
        initializeExampleScripts();
    }

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());

        JPanel controlPanel = new JPanel(new BorderLayout(5, 0));
        controlPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        controlPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder("Python Script Controls"),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        scriptSelector = new JComboBox<>(new String[] {"New Script..."});
        scriptSelector.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        scriptSelector.setForeground(ThemeManager.getCurrentTheme().getTextColor());
        scriptSelector.addActionListener(e -> {
            if (scriptSelector.getSelectedIndex() > 0) {
                loadScript((String) scriptSelector.getSelectedItem());
            } else {
                scriptArea.setText("");
            }
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        buttonPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());

        runButton = ThemeManager.createThemedButton("▶ Run");
        runButton.setToolTipText("Run the current script");
        runButton.addActionListener(e -> runScript());

        saveButton = ThemeManager.createThemedButton("💾 Save");
        saveButton.setToolTipText("Save the current script");
        saveButton.addActionListener(e -> saveScript());

        loadButton = ThemeManager.createThemedButton("📂 Load");
        loadButton.setToolTipText("Load a script from file");
        loadButton.addActionListener(e -> loadScriptFromFile());

        buttonPanel.add(runButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(loadButton);

        controlPanel.add(scriptSelector, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.EAST);

        scriptArea = new RSyntaxTextArea(20, 60);
        scriptArea.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_PYTHON);
        scriptArea.setCodeFoldingEnabled(true);
        scriptArea.setAntiAliasingEnabled(true);
        scriptArea.setAutoIndentEnabled(true);
        scriptArea.setBracketMatchingEnabled(true);

        applyThemeToSyntaxEditor();

        scriptArea.setText("""
        # CPV Analysis Tool - Python Script
        # This script has access to the following variables:
        # - procurement_items: List of all procurement items
        # - cpv_codes: Dictionary of all CPV codes
        
        # Example: Calculate total value of all items
        total_value = get_total_value()
        print("Total procurement value: " + format_currency(total_value))
        
        # Example: Get items by CPV code prefix
        construction_items = get_items_by_cpv("45")
        print("Number of construction items: " + str(len(construction_items)))
        print("Value of construction items: " + format_currency(get_total_value(construction_items)))
        
        # Example: Group items by category
        categories = group_by_category()
        print("\\nTop 5 categories by value:")
        
        # Calculate value by category
        category_values = {}
        for category, items in categories.items():
            category_values[category] = get_total_value(items)
        
        # Sort and display top categories
        sorted_categories = sorted(category_values.items(), key=lambda x: x[1], reverse=True)
        count = 0
        for category, value in sorted_categories:
            if count >= 5:
                break
            name = get_category_name(category)
            print(category + " - " + name + ": " + format_currency(value))
            count += 1
        """);

        RTextScrollPane scriptScrollPane = new RTextScrollPane(scriptArea);
        scriptScrollPane.setLineNumbersEnabled(true);

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        outputArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        JScrollPane outputScrollPane = new JScrollPane(outputArea);
        outputScrollPane.setBorder(BorderFactory.createTitledBorder("Script Output"));

        exampleButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        exampleButtonsPanel.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        exampleButtonsPanel.setBorder(BorderFactory.createTitledBorder("Example Scripts"));

        JSplitPane contentSplitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                scriptScrollPane,
                outputScrollPane
        );
        contentSplitPane.setResizeWeight(0.7);
        contentSplitPane.setBorder(BorderFactory.createEmptyBorder());

        add(controlPanel, BorderLayout.NORTH);
        add(contentSplitPane, BorderLayout.CENTER);
        add(exampleButtonsPanel, BorderLayout.SOUTH);
    }

    private void applyThemeToSyntaxEditor() {
        try {
            String themeName = ThemeManager.getCurrentThemeName().equals("Dark") ? "dark" : "default";
            Theme syntaxTheme = Theme.load(getClass().getResourceAsStream(
                    "/org/fife/ui/rsyntaxtextarea/themes/" + themeName + ".xml"));
            syntaxTheme.apply(scriptArea);
        } catch (Exception e) {
            scriptArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
            scriptArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());
            scriptArea.setCaretColor(ThemeManager.getCurrentTheme().getTextColor());
        }
    }

    private void initializeExampleScripts() {
        exampleScripts = new ArrayList<>();

        exampleScripts.add("Category Analysis");

        exampleScripts.add("Monthly Distribution");

        exampleScripts.add("Value Analysis");

        exampleScripts.add("Anomaly Detection");

        for (String example : exampleScripts) {
            JButton exampleButton = ThemeManager.createThemedButton(example);
            exampleButton.addActionListener(e -> loadExampleScript(example));
            exampleButtonsPanel.add(exampleButton);
        }
    }

    private void loadExampleScript(String scriptName) {
        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        String script = "";

        switch (scriptName) {
            case "Category Analysis":
                script = "# Category Analysis Script - Jython Compatible\n" +
                        "# Perform a detailed analysis of procurement by CPV category\n" +
                        "\n" +
                        "# Group items by category\n" +
                        "categories = group_by_category()\n" +
                        "\n" +
                        "# Calculate key metrics for each category\n" +
                        "results = []\n" +
                        "for category, items in categories.items():\n" +
                        "    if len(items) > 0:\n" +
                        "        category_name = get_category_name(category)\n" +
                        "        total_value = get_total_value(items)\n" +
                        "        avg_value = total_value / len(items)\n" +
                        "        max_value = max(item.get('valueWithoutTVA', 0) for item in items)\n" +
                        "        min_values = [item.get('valueWithoutTVA', 0) for item in items if item.get('valueWithoutTVA', 0) > 0]\n" +
                        "        min_value = min(min_values) if min_values else 0\n" +
                        "        \n" +
                        "        results.append({\n" +
                        "            'category': category,\n" +
                        "            'name': category_name,\n" +
                        "            'count': len(items),\n" +
                        "            'total_value': total_value,\n" +
                        "            'avg_value': avg_value,\n" +
                        "            'max_value': max_value,\n" +
                        "            'min_value': min_value\n" +
                        "        })\n" +
                        "\n" +
                        "# Sort by total value\n" +
                        "results.sort(key=lambda x: x['total_value'], reverse=True)\n" +
                        "\n" +
                        "# Calculate overall total for percentages\n" +
                        "overall_total = get_total_value()\n" +
                        "\n" +
                        "# Display formatted results\n" +
                        "print(\"===== CATEGORY ANALYSIS REPORT =====\\n\")\n" +
                        "print(\"Category    Name                           Count   Total Value      % of Total   Avg Value\\n\")\n" +
                        "\n" +
                        "for result in results:\n" +
                        "    percentage = (result['total_value'] / overall_total) * 100 if overall_total > 0 else 0\n" +
                        "    category_str = result['category'][:10].ljust(10)\n" +
                        "    name_str = result['name'][:30].ljust(30)\n" +
                        "    count_str = str(result['count']).rjust(5)\n" +
                        "    total_str = (\"%.2f\" % result['total_value']).rjust(15)\n" +
                        "    pct_str = (\"%.1f%%\" % percentage).rjust(10)\n" +
                        "    avg_str = (\"%.2f\" % result['avg_value']).rjust(15)\n" +
                        "    print(category_str + \" \" + name_str + \" \" + count_str + \" \" + total_str + \" \" + pct_str + \" \" + avg_str)\n" +
                        "\n" +
                        "# Concentration analysis\n" +
                        "print(\"\\n===== CONCENTRATION ANALYSIS =====\\n\")\n" +
                        "\n" +
                        "top_3_value = sum(r['total_value'] for r in results[:3])\n" +
                        "top_5_value = sum(r['total_value'] for r in results[:5])\n" +
                        "top_10_value = sum(r['total_value'] for r in results[:min(10, len(results))])\n" +
                        "\n" +
                        "print(\"Top 3 categories: \" + format_currency(top_3_value) + \" (\" + (\"%.1f%%\" % (top_3_value/overall_total*100)) + \")\")\n" +
                        "print(\"Top 5 categories: \" + format_currency(top_5_value) + \" (\" + (\"%.1f%%\" % (top_5_value/overall_total*100)) + \")\")\n" +
                        "print(\"Top 10 categories: \" + format_currency(top_10_value) + \" (\" + (\"%.1f%%\" % (top_10_value/overall_total*100)) + \")\")\n" +
                        "\n" +
                        "# Concentration assessment\n" +
                        "if top_3_value / overall_total > 0.7:\n" +
                        "    print(\"\\nHIGH CONCENTRATION: Over 70% of spending in top 3 categories\")\n" +
                        "    print(\"Recommendation: Consider diversifying procurement across more categories\")\n" +
                        "elif top_3_value / overall_total > 0.5:\n" +
                        "    print(\"\\nMODERATE CONCENTRATION: Over 50% of spending in top 3 categories\")\n" +
                        "    print(\"Recommendation: Monitor concentration and develop category-specific strategies\")\n" +
                        "else:\n" +
                        "    print(\"\\nLOW CONCENTRATION: Spending is well distributed across categories\")\n" +
                        "    print(\"Recommendation: Continue balanced approach to category management\")\n";
                break;

            case "Monthly Distribution":
                script = "# Monthly Distribution Analysis - Jython Compatible\n" +
                        "# Analyze procurement distribution across months\n" +
                        "\n" +
                        "# Define months in order\n" +
                        "months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', \n" +
                        "          'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']\n" +
                        "\n" +
                        "# Initialize counters\n" +
                        "value_by_month = {}\n" +
                        "count_by_month = {}\n" +
                        "for month in months:\n" +
                        "    value_by_month[month] = 0.0\n" +
                        "    count_by_month[month] = 0\n" +
                        "\n" +
                        "# Analyze dates for month information\n" +
                        "for item in procurement_items:\n" +
                        "    # Try initiation date first, then completion date\n" +
                        "    date_str = item.get('initiationDate') or item.get('completionDate')\n" +
                        "    if not date_str:\n" +
                        "        continue\n" +
                        "    \n" +
                        "    month = extract_month(date_str)\n" +
                        "    if month in value_by_month:\n" +
                        "        value_by_month[month] += item.get('valueWithoutTVA', 0)\n" +
                        "        count_by_month[month] += 1\n" +
                        "\n" +
                        "# Calculate total for percentages\n" +
                        "total_value = sum(value_by_month.values())\n" +
                        "total_count = sum(count_by_month.values())\n" +
                        "\n" +
                        "# Display monthly distribution\n" +
                        "print(\"===== MONTHLY DISTRIBUTION ANALYSIS =====\\n\")\n" +
                        "print(\"Month   Count   % of Items      Value      % of Value\\n\")\n" +
                        "\n" +
                        "for month in months:\n" +
                        "    count = count_by_month[month]\n" +
                        "    value = value_by_month[month]\n" +
                        "    \n" +
                        "    count_pct = (count / total_count * 100) if total_count > 0 else 0\n" +
                        "    value_pct = (value / total_value * 100) if total_value > 0 else 0\n" +
                        "    \n" +
                        "    month_str = month.ljust(5)\n" +
                        "    count_str = str(count).rjust(6)\n" +
                        "    count_pct_str = (\"%.1f%%\" % count_pct).rjust(11)\n" +
                        "    value_str = (\"%.2f\" % value).rjust(15)\n" +
                        "    value_pct_str = (\"%.1f%%\" % value_pct).rjust(11)\n" +
                        "    \n" +
                        "    print(month_str + \" \" + count_str + \" \" + count_pct_str + \" \" + value_str + \" \" + value_pct_str)\n" +
                        "\n" +
                        "# Find peak month\n" +
                        "peak_month = None\n" +
                        "peak_value = 0\n" +
                        "for month, value in value_by_month.items():\n" +
                        "    if value > peak_value:\n" +
                        "        peak_value = value\n" +
                        "        peak_month = month\n" +
                        "\n" +
                        "peak_pct = (peak_value / total_value * 100) if total_value > 0 else 0\n" +
                        "\n" +
                        "print(\"\\n===== MONTHLY INSIGHTS =====\\n\")\n" +
                        "print(\"Peak spending month: \" + str(peak_month) + \" (\" + (\"%.1f%%\" % peak_pct) + \" of annual value)\")\n" +
                        "\n" +
                        "# Calculate monthly variability\n" +
                        "avg_monthly_value = total_value / 12\n" +
                        "variance = sum((value - avg_monthly_value) ** 2 for value in value_by_month.values()) / 12\n" +
                        "std_dev = variance ** 0.5\n" +
                        "cv = (std_dev / avg_monthly_value * 100) if avg_monthly_value > 0 else 0\n" +
                        "\n" +
                        "print(\"Monthly spending variability: \" + (\"%.1f%%\" % cv) + \" coefficient of variation\")\n" +
                        "\n" +
                        "# Variability assessment\n" +
                        "if cv > 100:\n" +
                        "    print(\"EXTREME variability - procurement is very unevenly distributed\")\n" +
                        "    print(\"Recommendation: Implement procurement planning to distribute activities more evenly\")\n" +
                        "elif cv > 70:\n" +
                        "    print(\"HIGH variability - significant monthly fluctuations in procurement\")\n" +
                        "    print(\"Recommendation: Review planning and budgeting processes\")\n" +
                        "elif cv > 40:\n" +
                        "    print(\"MODERATE variability - some seasonal patterns in procurement\")\n" +
                        "    print(\"Recommendation: Consider quarterly planning to smooth procurement activities\")\n" +
                        "else:\n" +
                        "    print(\"LOW variability - procurement is relatively even throughout the year\")\n" +
                        "    print(\"Recommendation: Maintain current planning approach\")\n";
                break;

            case "Value Analysis":
                script = "# Value Analysis Script - Jython Compatible\n" +
                        "# Analyze procurement items by value ranges\n" +
                        "\n" +
                        "# Define value ranges\n" +
                        "ranges = [\n" +
                        "    {'name': '0-10,000', 'min': 0, 'max': 10000},\n" +
                        "    {'name': '10,000-50,000', 'min': 10000, 'max': 50000},\n" +
                        "    {'name': '50,000-100,000', 'min': 50000, 'max': 100000},\n" +
                        "    {'name': '100,000+', 'min': 100000, 'max': float('inf')}\n" +
                        "]\n" +
                        "\n" +
                        "# Group items by range\n" +
                        "items_by_range = {}\n" +
                        "for r in ranges:\n" +
                        "    items_by_range[r['name']] = []\n" +
                        "\n" +
                        "for item in procurement_items:\n" +
                        "    value = item.get('valueWithoutTVA', 0)\n" +
                        "    for r in ranges:\n" +
                        "        if r['min'] <= value < r['max']:\n" +
                        "            items_by_range[r['name']].append(item)\n" +
                        "            break\n" +
                        "\n" +
                        "# Calculate statistics for each range\n" +
                        "total_items = len(procurement_items)\n" +
                        "total_value = get_total_value()\n" +
                        "\n" +
                        "print(\"===== VALUE DISTRIBUTION ANALYSIS =====\\n\")\n" +
                        "print(\"Value Range       Count   % of Items   Total Value      % of Value   Avg Value\\n\")\n" +
                        "\n" +
                        "for r in ranges:\n" +
                        "    range_items = items_by_range[r['name']]\n" +
                        "    count = len(range_items)\n" +
                        "    range_value = sum(item.get('valueWithoutTVA', 0) for item in range_items)\n" +
                        "    \n" +
                        "    count_pct = (count / total_items * 100) if total_items > 0 else 0\n" +
                        "    value_pct = (range_value / total_value * 100) if total_value > 0 else 0\n" +
                        "    avg_value = (range_value / count) if count > 0 else 0\n" +
                        "    \n" +
                        "    name_str = r['name'].ljust(15)\n" +
                        "    count_str = str(count).rjust(6)\n" +
                        "    count_pct_str = (\"%.1f%%\" % count_pct).rjust(11)\n" +
                        "    range_value_str = (\"%.2f\" % range_value).rjust(15)\n" +
                        "    value_pct_str = (\"%.1f%%\" % value_pct).rjust(11)\n" +
                        "    avg_value_str = (\"%.2f\" % avg_value).rjust(15)\n" +
                        "    \n" +
                        "    print(name_str + \" \" + count_str + \" \" + count_pct_str + \" \" + range_value_str + \" \" + value_pct_str + \" \" + avg_value_str)\n" +
                        "\n" +
                        "# Pareto analysis (80/20 rule)\n" +
                        "print(\"\\n===== PARETO ANALYSIS =====\\n\")\n" +
                        "\n" +
                        "# Sort items by value (descending)\n" +
                        "sorted_items = sorted(procurement_items, key=lambda x: x.get('valueWithoutTVA', 0), reverse=True)\n" +
                        "\n" +
                        "cumulative_value = 0\n" +
                        "items_for_80pct = 0\n" +
                        "\n" +
                        "for item in sorted_items:\n" +
                        "    cumulative_value += item.get('valueWithoutTVA', 0)\n" +
                        "    items_for_80pct += 1\n" +
                        "    \n" +
                        "    if cumulative_value >= total_value * 0.8:\n" +
                        "        break\n" +
                        "\n" +
                        "pct_of_items = (items_for_80pct / total_items * 100) if total_items > 0 else 0\n" +
                        "\n" +
                        "print((\"%.1f%%\" % pct_of_items) + \" of items (top \" + str(items_for_80pct) + \") account for 80% of total procurement value\")\n" +
                        "\n" +
                        "# Interpretation\n" +
                        "if pct_of_items < 20:\n" +
                        "    print(\"This indicates extreme concentration in a small number of high-value items\")\n" +
                        "    print(\"Recommendation: Focus procurement strategy on these critical high-value items\")\n" +
                        "elif pct_of_items < 30:\n" +
                        "    print(\"This follows the Pareto principle (80/20 rule) fairly closely\")\n" +
                        "    print(\"Recommendation: Implement differentiated strategies for high and low value items\")\n" +
                        "else:\n" +
                        "    print(\"This indicates a more even distribution than the typical 80/20 rule\")\n" +
                        "    print(\"Recommendation: Consider uniform procurement policies across items\")\n";
                break;

            case "Anomaly Detection":
                script = "# Custom Anomaly Detection Script - Jython Compatible\n" +
                        "# Identify unusual patterns and outliers in procurement data\n" +
                        "\n" +
                        "import math\n" +
                        "\n" +
                        "# Calculate basic statistics\n" +
                        "all_values = []\n" +
                        "for item in procurement_items:\n" +
                        "    value = item.get('valueWithoutTVA', 0)\n" +
                        "    if value > 0:\n" +
                        "        all_values.append(value)\n" +
                        "\n" +
                        "if not all_values:\n" +
                        "    print(\"No valid values found for analysis\")\n" +
                        "    exit()\n" +
                        "\n" +
                        "# Basic statistics\n" +
                        "mean = sum(all_values) / len(all_values)\n" +
                        "\n" +
                        "# Calculate standard deviation\n" +
                        "squared_diffs = []\n" +
                        "for value in all_values:\n" +
                        "    squared_diffs.append((value - mean) ** 2)\n" +
                        "variance = sum(squared_diffs) / len(all_values)\n" +
                        "std_dev = math.sqrt(variance)\n" +
                        "\n" +
                        "# Define thresholds for outliers\n" +
                        "mild_outlier_threshold = mean + (2 * std_dev)\n" +
                        "extreme_outlier_threshold = mean + (3 * std_dev)\n" +
                        "\n" +
                        "print(\"===== STATISTICAL ANOMALY DETECTION =====\\n\")\n" +
                        "print(\"Average item value: \" + format_currency(mean))\n" +
                        "print(\"Standard deviation: \" + format_currency(std_dev))\n" +
                        "print(\"Mild outlier threshold (mean + 2σ): \" + format_currency(mild_outlier_threshold))\n" +
                        "print(\"Extreme outlier threshold (mean + 3σ): \" + format_currency(extreme_outlier_threshold) + \"\\n\")\n" +
                        "\n" +
                        "# Find outliers\n" +
                        "mild_outliers = []\n" +
                        "extreme_outliers = []\n" +
                        "\n" +
                        "for item in procurement_items:\n" +
                        "    value = item.get('valueWithoutTVA', 0)\n" +
                        "    if value > extreme_outlier_threshold:\n" +
                        "        extreme_outliers.append(item)\n" +
                        "    elif value > mild_outlier_threshold:\n" +
                        "        mild_outliers.append(item)\n" +
                        "\n" +
                        "# Sort outliers by value\n" +
                        "def get_value(item):\n" +
                        "    return item.get('valueWithoutTVA', 0)\n" +
                        "\n" +
                        "mild_outliers.sort(key=get_value, reverse=True)\n" +
                        "extreme_outliers.sort(key=get_value, reverse=True)\n" +
                        "\n" +
                        "print(\"Found \" + str(len(mild_outliers)) + \" mild outliers and \" + str(len(extreme_outliers)) + \" extreme outliers\\n\")\n" +
                        "\n" +
                        "# Display extreme outliers\n" +
                        "if extreme_outliers:\n" +
                        "    print(\"===== EXTREME OUTLIERS =====\\n\")\n" +
                        "    \n" +
                        "    for i in range(min(5, len(extreme_outliers))):\n" +
                        "        item = extreme_outliers[i]\n" +
                        "        value = item.get('valueWithoutTVA', 0)\n" +
                        "        stddev_factor = (value - mean) / std_dev\n" +
                        "        \n" +
                        "        print(str(i+1) + \". \" + item.get('objectName', ''))\n" +
                        "        print(\"   Value: \" + format_currency(value) + \" (\" + str(round(stddev_factor, 1)) + \"σ above mean)\")\n" +
                        "        print(\"   CPV: \" + item.get('cpvField', 'N/A') + \"\\n\")\n" +
                        "\n" +
                        "# Detect category anomalies\n" +
                        "print(\"===== CATEGORY CONCENTRATION ANOMALIES =====\\n\")\n" +
                        "\n" +
                        "# Group by category\n" +
                        "categories = group_by_category()\n" +
                        "total_value = get_total_value()\n" +
                        "\n" +
                        "# Calculate concentration\n" +
                        "category_values = {}\n" +
                        "for category, items in categories.items():\n" +
                        "    value = 0\n" +
                        "    for item in items:\n" +
                        "        value += item.get('valueWithoutTVA', 0)\n" +
                        "    category_values[category] = value\n" +
                        "\n" +
                        "# Identify categories with unusually high concentration\n" +
                        "high_concentration = []\n" +
                        "\n" +
                        "for category, value in category_values.items():\n" +
                        "    percentage = (value / total_value) * 100 if total_value > 0 else 0\n" +
                        "    if percentage > 25:  # Categories with >25% of total value\n" +
                        "        category_name = get_category_name(category)\n" +
                        "        high_concentration.append({\n" +
                        "            'category': category,\n" +
                        "            'name': category_name,\n" +
                        "            'value': value,\n" +
                        "            'percentage': percentage\n" +
                        "        })\n" +
                        "\n" +
                        "# Sort by percentage\n" +
                        "def get_percentage(item):\n" +
                        "    return item['percentage']\n" +
                        "\n" +
                        "high_concentration.sort(key=get_percentage, reverse=True)\n" +
                        "\n" +
                        "if high_concentration:\n" +
                        "    for item in high_concentration:\n" +
                        "        category = item['category']\n" +
                        "        name = item['name']\n" +
                        "        value = item['value']\n" +
                        "        percentage = item['percentage']\n" +
                        "        print(category + \" - \" + name + \": \" + format_currency(value) + \" (\" + str(round(percentage, 1)) + \"% of total)\")\n" +
                        "    \n" +
                        "    if len(high_concentration) > 0 and high_concentration[0]['percentage'] > 50:\n" +
                        "        print(\"\\nWARNING: Extreme concentration in a single category (>50% of total value)\")\n" +
                        "        print(\"This may indicate excessive reliance on a single type of procurement\")\n" +
                        "else:\n" +
                        "    print(\"No significant category concentration anomalies detected\")\n" +
                        "\n" +
                        "# Detect monthly anomalies\n" +
                        "print(\"\\n===== MONTHLY DISTRIBUTION ANOMALIES =====\\n\")\n" +
                        "\n" +
                        "# Define months\n" +
                        "months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', \n" +
                        "          'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']\n" +
                        "\n" +
                        "# Initialize counters\n" +
                        "value_by_month = {}\n" +
                        "for month in months:\n" +
                        "    value_by_month[month] = 0.0\n" +
                        "\n" +
                        "# Analyze dates for month information\n" +
                        "for item in procurement_items:\n" +
                        "    date_str = item.get('initiationDate') or item.get('completionDate')\n" +
                        "    if date_str:\n" +
                        "        month = extract_month(date_str)\n" +
                        "        if month in value_by_month:\n" +
                        "            value_by_month[month] += item.get('valueWithoutTVA', 0)\n" +
                        "\n" +
                        "# Calculate total for percentages\n" +
                        "total_monthly_value = sum(value_by_month.values())\n" +
                        "\n" +
                        "if total_monthly_value > 0:\n" +
                        "    # Find peak month\n" +
                        "    peak_month = None\n" +
                        "    peak_value = 0\n" +
                        "    for month, value in value_by_month.items():\n" +
                        "        if value > peak_value:\n" +
                        "            peak_value = value\n" +
                        "            peak_month = month\n" +
                        "    \n" +
                        "    peak_percentage = (peak_value / total_monthly_value) * 100\n" +
                        "    \n" +
                        "    if peak_percentage > 30:\n" +
                        "        print(peak_month + \" contains \" + str(round(peak_percentage, 1)) + \"% of annual procurement value\")\n" +
                        "        print(\"This monthly concentration could indicate uneven planning or end-of-budget spending\")\n" +
                        "    else:\n" +
                        "        print(\"No significant monthly distribution anomalies detected\")\n" +
                        "else:\n" +
                        "    print(\"Insufficient monthly data for anomaly detection\")";
                break;

            default:
                script = "# No example script found for: " + scriptName;
                break;
        }

        scriptArea.setText(script);
        outputArea.setText("");
    }

    private void runScript() {
        if (scriptingService == null) {
            outputArea.setText("Error: Scripting service not initialized. Please load data first.");
            return;
        }

        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        String script = scriptArea.getText();
        if (script.trim().isEmpty()) {
            outputArea.setText("Error: No script to run.");
            return;
        }

        SwingWorker<String, Void> worker = new SwingWorker<>() {
            @Override
            protected String doInBackground() {
                return scriptingService.executeScript(script);
            }

            @Override
            protected void done() {
                try {
                    String result = get();
                    outputArea.setText(result);
                    outputArea.setCaretPosition(0);
                    SoundManager.playSound(SoundManager.SOUND_SUCCESS);
                } catch (Exception e) {
                    outputArea.setText("Error executing script: " + e.getMessage());
                    SoundManager.playSound(SoundManager.SOUND_ERROR);
                }
            }
        };

        outputArea.setText("Running script...");
        worker.execute();
    }

    private void saveScript() {
        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        String script = scriptArea.getText();
        if (script.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "No script to save.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String scriptName = JOptionPane.showInputDialog(this, "Enter a name for the script:", "Save Script", JOptionPane.QUESTION_MESSAGE);
        if (scriptName == null || scriptName.trim().isEmpty()) {
            return;
        }

        boolean exists = false;
        for (int i = 0; i < scriptSelector.getItemCount(); i++) {
            if (scriptName.equals(scriptSelector.getItemAt(i))) {
                exists = true;
                break;
            }
        }

        if (!exists) {
            scriptSelector.addItem(scriptName);
        }

        scriptSelector.setSelectedItem(scriptName);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Script");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".py");
            }

            @Override
            public String getDescription() {
                return "Python Files (*.py)";
            }
        });

        fileChooser.setSelectedFile(new File(scriptName + ".py"));

        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".py")) {
                file = new File(file.getAbsolutePath() + ".py");
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(script);
                JOptionPane.showMessageDialog(this, "Script saved to " + file.getName(), "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving script: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                SoundManager.playSound(SoundManager.SOUND_ERROR);
            }
        }
    }

    private void loadScriptFromFile() {
        SoundManager.playSound(SoundManager.SOUND_BUTTON_CLICK);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Load Script");
        fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
            @Override
            public boolean accept(File f) {
                return f.isDirectory() || f.getName().toLowerCase().endsWith(".py");
            }

            @Override
            public String getDescription() {
                return "Python Files (*.py)";
            }
        });

        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();

            try (FileReader reader = new FileReader(file)) {
                StringBuilder content = new StringBuilder();
                char[] buffer = new char[1024];
                int read;

                while ((read = reader.read(buffer)) != -1) {
                    content.append(buffer, 0, read);
                }

                scriptArea.setText(content.toString());

                String scriptName = file.getName();
                if (scriptName.toLowerCase().endsWith(".py")) {
                    scriptName = scriptName.substring(0, scriptName.length() - 3);
                }

                boolean exists = false;
                for (int i = 0; i < scriptSelector.getItemCount(); i++) {
                    if (scriptName.equals(scriptSelector.getItemAt(i))) {
                        exists = true;
                        break;
                    }
                }

                if (!exists) {
                    scriptSelector.addItem(scriptName);
                }

                scriptSelector.setSelectedItem(scriptName);

            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error loading script: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                SoundManager.playSound(SoundManager.SOUND_ERROR);
            }
        }
    }

    private void loadScript(String scriptName) {
        if (exampleScripts.contains(scriptName)) {
            loadExampleScript(scriptName);
            return;
        }

        int option = JOptionPane.showConfirmDialog(this,
                "Would you like to load '" + scriptName + "' from file?",
                "Load Script", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            loadScriptFromFile();
        }
    }

    public void setScriptingService(PythonScriptingService service) {
        this.scriptingService = service;
    }

    public void updateTheme() {
        setBackground(ThemeManager.getCurrentTheme().getBackgroundColor());
        outputArea.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        outputArea.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        scriptSelector.setBackground(ThemeManager.getCurrentTheme().getPanelColor());
        scriptSelector.setForeground(ThemeManager.getCurrentTheme().getTextColor());

        updateComponentTheme(this);

        applyThemeToSyntaxEditor();
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
            } else if (comp instanceof JTextArea) {
                ((JTextArea) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JTextArea) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            } else if (comp instanceof JComboBox) {
                ((JComboBox<?>) comp).setBackground(ThemeManager.getCurrentTheme().getPanelColor());
                ((JComboBox<?>) comp).setForeground(ThemeManager.getCurrentTheme().getTextColor());
            }

            if (comp instanceof Container) {
                updateComponentTheme((Container) comp);
            }
        }
    }
}