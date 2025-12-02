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

import epox.webaom.data.Anime;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.table.JTableHeader;

/**
 * Header click listener for the alternate (tree-based) file view. Handles column sorting by clicking column headers.
 */
public class AlternateViewHeaderListener extends MouseAdapter {
    /** The tree table this listener manages */
    protected final JobTreeTable treeTable;
    /** Table header for mouse events */
    protected final JTableHeader tableHeader;
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
