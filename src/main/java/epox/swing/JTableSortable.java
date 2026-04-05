// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

package epox.swing;

import epox.swing.layout.TableRowSizing;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;

/**
 * A JTable with sortable columns. Uses built-in TableRowSorter for sorting.
 */
public class JTableSortable extends JTable {
    /** Header click listener for column visibility menu */
    private transient HeaderListener headerListener;

    public JTableSortable(AbstractTableModel model) {
        super(model);
        setAutoCreateColumnsFromModel(false);

        // Use built-in TableRowSorter for sorting
        TableRowSorter<AbstractTableModel> sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);

        installHeaderListener();
    }

    @Override
    public void updateUI() {
        ColumnStateSnapshot snapshot = captureColumnState();
        if (headerListener != null && headerListener.tableHeader != null) {
            headerListener.tableHeader.removeMouseListener(headerListener);
        }

        super.updateUI();
        setAutoCreateColumnsFromModel(false);
        installHeaderListener();
        restoreColumnState(snapshot);
        TableRowSizing.applyPreferredRowHeight(this, UIManager.getInt("Table.rowHeight"));
    }

    @Override
    public void setFont(Font font) {
        super.setFont(font);
        TableRowSizing.applyPreferredRowHeight(this, UIManager.getInt("Table.rowHeight"));
    }

    public HeaderListener getHeaderListener() {
        return headerListener;
    }

    private void installHeaderListener() {
        if (headerListener == null) {
            headerListener = new HeaderListener(getTableHeader(), getColumnModel());
        } else {
            headerListener.rebind(getTableHeader(), getColumnModel());
        }
        if (getTableHeader() != null) {
            getTableHeader().addMouseListener(headerListener);
        }
    }

    private ColumnStateSnapshot captureColumnState() {
        if (headerListener == null) {
            return null;
        }

        List<Integer> visibleModelIndices = new ArrayList<>();
        for (int index = 0; index < getColumnModel().getColumnCount(); index++) {
            visibleModelIndices.add(getColumnModel().getColumn(index).getModelIndex());
        }

        List<ColumnWidthState> columnWidths = new ArrayList<>();
        for (TableColumn column : headerListener.getAllColumns()) {
            columnWidths.add(new ColumnWidthState(column.getModelIndex(), column.getPreferredWidth()));
        }
        return new ColumnStateSnapshot(visibleModelIndices, columnWidths);
    }

    private void restoreColumnState(ColumnStateSnapshot snapshot) {
        if (snapshot == null || headerListener == null) {
            return;
        }

        for (ColumnWidthState columnWidth : snapshot.columnWidths()) {
            TableColumn column = headerListener.getColumnByModelIndex(columnWidth.modelIndex());
            if (column != null) {
                column.setPreferredWidth(columnWidth.preferredWidth());
            }
        }
        headerListener.setVisibleColumns(snapshot.visibleModelIndices());
    }

    private record ColumnStateSnapshot(List<Integer> visibleModelIndices, List<ColumnWidthState> columnWidths) {}

    private record ColumnWidthState(int modelIndex, int preferredWidth) {}
}
