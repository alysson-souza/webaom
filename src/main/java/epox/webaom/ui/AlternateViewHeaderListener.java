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

/*
 * Created on 26.des.2005 15:17:36
 * Filename: AnimeHeaderListener.java
 */
package epox.webaom.ui;

import epox.webaom.data.Anime;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.JTableHeader;

/**
 * Header click listener for the alternate (tree-based) file view. Handles column sorting by clicking column headers.
 */
public class AlternateViewHeaderListener extends MouseAdapter {
    /** The tree table this listener manages */
    protected JobTreeTable treeTable;
    /** Table header for mouse events */
    protected JTableHeader tableHeader;
    /** Current sort column index (positive = ascending, negative = descending) */
    private int currentSortColumn = 0;

    public AlternateViewHeaderListener(JobTreeTable treeTable) {
        this.treeTable = treeTable;
        tableHeader = treeTable.getTableHeader();
        tableHeader.addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent event) {
        int clickedColumn = tableHeader.columnAtPoint(event.getPoint());
        int sortColumn = tableHeader.getTable().convertColumnIndexToModel(clickedColumn) + 1;
        if (currentSortColumn == sortColumn) {
            sortColumn *= -1; // Toggle sort direction
        }
        currentSortColumn = sortColumn;
        synchronized (treeTable) {
            Anime.setSortColumn(sortColumn);
            treeTable.updateUI();
        }
    }
}
