package org.example.view;

import org.example.model.ProcurementItem;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.List;

public class SearchPanel extends JPanel {
    private JTextField searchField;
    private JButton searchButton;
    private JList<ProcurementItem> resultsList;
    private JScrollPane resultsScrollPane;

    public SearchPanel() {
        setLayout(new BorderLayout());

        JPanel searchControlsPanel = new JPanel(new BorderLayout());
        searchField = new JTextField();
        searchField.setToolTipText("Enter search terms (CPV code, name, or description)");
        searchButton = new JButton("Search");

        searchControlsPanel.add(new JLabel("Search: "), BorderLayout.WEST);
        searchControlsPanel.add(searchField, BorderLayout.CENTER);
        searchControlsPanel.add(searchButton, BorderLayout.EAST);
        searchControlsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        resultsList = new JList<>(new DefaultListModel<>());
        resultsList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof ProcurementItem) {
                    ProcurementItem item = (ProcurementItem) value;
                    label.setText(item.toString());

                    StringBuilder tooltip = new StringBuilder("<html>");
                    tooltip.append("<b>").append(item.getObjectName()).append("</b><br>");

                    if (item.getCpvField() != null && !item.getCpvField().isEmpty()) {
                        tooltip.append("CPV: ").append(item.getCpvField()).append("<br>");
                    }

                    if (item.getValueWithoutTVA() > 0) {
                        tooltip.append("Value: ").append(String.format("%,.2f", item.getValueWithoutTVA())).append(" RON<br>");
                    }

                    if (item.getSource() != null && !item.getSource().isEmpty()) {
                        tooltip.append("Source: ").append(item.getSource());
                    }

                    tooltip.append("</html>");
                    label.setToolTipText(tooltip.toString());
                }

                return label;
            }
        });
        resultsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resultsScrollPane = new JScrollPane(resultsList);
        resultsScrollPane.setBorder(BorderFactory.createTitledBorder("Search Results"));

        add(searchControlsPanel, BorderLayout.NORTH);
        add(resultsScrollPane, BorderLayout.CENTER);
    }

    public void setSearchAction(ActionListener listener) {
        searchButton.addActionListener(listener);
        searchField.addActionListener(listener);
    }

    public String getSearchQuery() {
        return searchField.getText().trim();
    }

    public void updateResultsList(List<ProcurementItem> items) {
        DefaultListModel<ProcurementItem> model = new DefaultListModel<>();
        for (ProcurementItem item : items) {
            model.addElement(item);
        }
        resultsList.setModel(model);

        if (!items.isEmpty()) {
            resultsList.setSelectedIndex(0);
        }
    }

    public ProcurementItem getSelectedItem() {
        return resultsList.getSelectedValue();
    }
}