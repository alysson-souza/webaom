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

import javax.swing.table.AbstractTableModel;

/**
 * Abstract table model that supports sorting by column. Maintains an index array for row mapping.
 */
public abstract class TableModelSortable extends AbstractTableModel {
    /** Sorter for comparing and reordering rows */
    private final TableSorter tableSorter = new TableSorter(this);
    /** Index mapping from sorted row to original row */
    protected int[] sortedRowIndices;
    /** Currently sorted column index, -1 if not sorted */
    private int sortColumn;

    public void reset() {
        sortColumn = -1;
        tableSorter.reset();
        sortedRowIndices = null;
    }

    protected int getRowIndex(int row) {
        try {
            return sortedRowIndices[row];
        } catch (NullPointerException ex) {
            return row;
        } catch (ArrayIndexOutOfBoundsException ex) {
            return row;
        }
    }

    public void sortByColumn(int column) {
        sortColumn = column;
        sort(false);
    }

    public void sort(boolean refresh) {
        if (sortColumn < 0) {
            return;
        }
        tableSorter.sort(getIndexes(), sortColumn, refresh);

        fireTableDataChanged();
    }

    private int[] getIndexes() {
        int rowCount = getRowCount();
        if (sortedRowIndices != null && sortedRowIndices.length == rowCount) {
            return sortedRowIndices;
        }

        sortedRowIndices = new int[rowCount];
        for (int index = 0; index < rowCount; index++) {
            sortedRowIndices[index] = index;
        }

        return sortedRowIndices;
    }
}
