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

package epox.swing;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/**
 * Header listener for column visibility popup menu. Right-click on table header to show/hide columns.
 * Sorting is now handled by TableRowSorter.
 */
public class HeaderListener extends MouseAdapter {
    protected JTableHeader tableHeader;
    protected TableColumnModel columnModel;
    private final List<TableColumn> allColumns;
    protected JPopupMenu columnPopup = null;

    public HeaderListener(JTableHeader header, TableColumnModel columns) {
        allColumns = new ArrayList<>();
        rebind(header, columns);
    }

    public void rebind(JTableHeader header, TableColumnModel columns) {
        tableHeader = header;
        columnModel = columns;
        if (columnModel == null) {
            columnPopup = null;
            return;
        }

        for (int index = 0; index < columnModel.getColumnCount(); index++) {
            TableColumn column = columnModel.getColumn(index);
            int knownIndex = indexOfModelColumn(column.getModelIndex());
            if (knownIndex >= 0) {
                allColumns.set(knownIndex, column);
            } else {
                allColumns.add(column);
            }
        }
        rebuildPopup();
    }

    public void setMask(long mask) {
        if (columnModel == null) {
            return;
        }

        List<Integer> visibleColumns = new ArrayList<>();
        for (TableColumn column : allColumns) {
            int modelIndex = column.getModelIndex();
            if ((mask & (1L << modelIndex)) == (1L << modelIndex)) {
                visibleColumns.add(modelIndex);
            }
        }
        setVisibleColumns(visibleColumns);
    }

    public void setVisibleColumns(List<Integer> visibleModelIndices) {
        if (columnModel == null) {
            return;
        }

        List<TableColumn> visibleColumns = new ArrayList<>();
        for (Integer modelIndex : visibleModelIndices) {
            if (modelIndex == null) {
                continue;
            }
            TableColumn column = getColumnByModelIndex(modelIndex);
            if (column != null && !visibleColumns.contains(column)) {
                visibleColumns.add(column);
            }
        }

        if (visibleColumns.isEmpty() && !allColumns.isEmpty()) {
            visibleColumns.add(allColumns.getFirst());
        }

        while (columnModel.getColumnCount() > 0) {
            columnModel.removeColumn(columnModel.getColumn(0));
        }
        for (TableColumn column : visibleColumns) {
            columnModel.addColumn(column);
        }

        rebuildPopup();
    }

    public TableColumn getColumnByModelIndex(int modelIndex) {
        if (columnModel != null) {
            int visibleIndex = getVisibleColumnIndex(modelIndex);
            if (visibleIndex >= 0) {
                return columnModel.getColumn(visibleIndex);
            }
        }
        for (TableColumn column : allColumns) {
            if (column.getModelIndex() == modelIndex) {
                return column;
            }
        }
        return null;
    }

    public boolean isColumnVisible(int modelIndex) {
        return getVisibleColumnIndex(modelIndex) >= 0;
    }

    public List<TableColumn> getAllColumns() {
        return Collections.unmodifiableList(allColumns);
    }

    private int getVisibleColumnIndex(int modelIndex) {
        if (columnModel == null) {
            return -1;
        }
        for (int index = 0; index < columnModel.getColumnCount(); index++) {
            if (columnModel.getColumn(index).getModelIndex() == modelIndex) {
                return index;
            }
        }
        return -1;
    }

    private int indexOfModelColumn(int modelIndex) {
        for (int index = 0; index < allColumns.size(); index++) {
            if (allColumns.get(index).getModelIndex() == modelIndex) {
                return index;
            }
        }
        return -1;
    }

    private void rebuildPopup() {
        columnPopup = new JPopupMenu();
        for (TableColumn column : allColumns) {
            JCheckBoxMenuItem menuItem =
                    new JCheckBoxMenuItem(column.getHeaderValue().toString(), isColumnVisible(column.getModelIndex()));
            menuItem.addActionListener(new ColumnAction(column, menuItem));
            columnPopup.add(menuItem);
        }
    }

    private void showColumn(TableColumn column) {
        if (isColumnVisible(column.getModelIndex())) {
            return;
        }

        columnModel.addColumn(column);
        int currentIndex = columnModel.getColumnCount() - 1;
        int targetIndex = getInsertionIndex(column.getModelIndex());
        if (targetIndex < currentIndex) {
            columnModel.moveColumn(currentIndex, targetIndex);
        }
    }

    private int getInsertionIndex(int modelIndex) {
        int insertionIndex = 0;
        for (TableColumn column : allColumns) {
            if (column.getModelIndex() == modelIndex) {
                break;
            }
            if (isColumnVisible(column.getModelIndex())) {
                insertionIndex++;
            }
        }
        return insertionIndex;
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        // Right-click shows column visibility popup
        if (event.getButton() != MouseEvent.BUTTON1 && columnPopup != null) {
            columnPopup.show(tableHeader, event.getX(), event.getY());
        }
        // Left-click sorting is now handled by TableRowSorter automatically
    }

    private class ColumnAction implements ActionListener {
        private final TableColumn column;
        private final JCheckBoxMenuItem menuItem;

        ColumnAction(TableColumn column, JCheckBoxMenuItem menuItem) {
            this.column = column;
            this.menuItem = menuItem;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (menuItem.isSelected()) {
                showColumn(column);
            } else if (columnModel.getColumnCount() > 1) {
                columnModel.removeColumn(column);
            } else {
                menuItem.setSelected(true);
            }
            rebuildPopup();
        }
    }
}
