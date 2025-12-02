/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2025 Alysson Souza
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the Free
 * Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom.ui;

import epox.util.ReplacementRule;
import java.util.List;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class ReplacementTableModel extends AbstractTableModel {
    private static final long serialVersionUID = 1L;

    public static final int COLUMN_SELECTED = 0;
    public static final int COLUMN_SOURCE = 1;
    public static final int COLUMN_DESTINATION = 2;
    private final String sourceColumnTitle;
    private final String destinationColumnTitle;
    private final transient ReplacementRule emptyRowPlaceholder;
    private transient List<ReplacementRule> rowDataList;

    public ReplacementTableModel(List<ReplacementRule> dataList, String sourceTitle, String destinationTitle) {
        setData(dataList);
        sourceColumnTitle = sourceTitle;
        destinationColumnTitle = destinationTitle;
        emptyRowPlaceholder = new ReplacementRule("", "", false);
    }

    public static void formatTable(JTable table) {
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(COLUMN_SELECTED).setMaxWidth(50);
        columnModel.getColumn(COLUMN_SOURCE).setPreferredWidth(100);
        columnModel.getColumn(COLUMN_DESTINATION).setPreferredWidth(100);
        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        columnModel.getColumn(COLUMN_SOURCE).setCellRenderer(centeredRenderer);
        columnModel.getColumn(COLUMN_DESTINATION).setCellRenderer(centeredRenderer);
    }

    public List<ReplacementRule> getData() {
        return rowDataList;
    }

    public void setData(List<ReplacementRule> dataList) {
        rowDataList = dataList;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case COLUMN_SELECTED -> Boolean.class;
            default -> String.class;
        };
    }

    @Override
    public int getRowCount() {
        return rowDataList.size() + 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ReplacementRule rowData;
        if (rowIndex == rowDataList.size()) {
            rowData = emptyRowPlaceholder;
        } else {
            rowData = rowDataList.get(rowIndex);
        }
        return switch (columnIndex) {
            case COLUMN_SELECTED -> rowData.isEnabled();
            case COLUMN_SOURCE -> rowData.getSource();
            case COLUMN_DESTINATION -> rowData.getDestination();
            default -> null;
        };
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        ReplacementRule rowData;
        if (rowIndex == rowDataList.size()) {
            rowData = new ReplacementRule("", "", false);
            rowDataList.add(rowData);
            fireTableRowsInserted(rowIndex, rowIndex);
        } else {
            rowData = rowDataList.get(rowIndex);
        }
        if (rowData != null) {
            switch (columnIndex) {
                case COLUMN_SELECTED:
                    if (!rowData.hasEmptySource()) {
                        rowData.setEnabled((Boolean) value);
                    }
                    break;
                case COLUMN_SOURCE:
                    rowData.setSource((String) value);
                    if (rowData.hasEmptySource()) {
                        rowDataList.remove(rowData);
                    }
                    break;
                case COLUMN_DESTINATION:
                    rowData.setDestination(getValidatedDestination(value));
                    break;
                default:
                    break;
            }
        }
    }

    /**
     * Validates the destination value, ensuring it doesn't duplicate an existing source.
     */
    private String getValidatedDestination(Object destinationValue) {
        String destination = (String) destinationValue;
        ReplacementRule rowData;
        for (ReplacementRule replacementRule : rowDataList) {
            rowData = replacementRule;
            if (rowData.getSource().equals(destination)) {
                return "";
            }
        }
        return destination;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case COLUMN_SELECTED -> "Enabled";
            case COLUMN_SOURCE -> sourceColumnTitle;
            case COLUMN_DESTINATION -> destinationColumnTitle;
            default -> "No such column!";
        };
    }
}
