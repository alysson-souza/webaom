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

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableRowSorter;

/**
 * A JTable with sortable columns. Uses built-in TableRowSorter for sorting.
 */
public class JTableSortable extends JTable {
    /** Header click listener for column visibility menu */
    private transient HeaderListener headerListener;

    /** Flag to trigger row height recalculation on next paint */
    private boolean needsRowHeightCalculation = true;

    public JTableSortable(AbstractTableModel model) {
        super(model);

        // Use built-in TableRowSorter for sorting
        TableRowSorter<AbstractTableModel> sorter = new TableRowSorter<>(model);
        setRowSorter(sorter);

        // Keep HeaderListener for column visibility popup (right-click)
        headerListener = new HeaderListener(getTableHeader(), getColumnModel());
        getTableHeader().addMouseListener(headerListener);
    }

    private void calculateRowHeight(Graphics graphics) {
        Font font = getFont();
        FontMetrics fontMetrics = graphics.getFontMetrics(font);
        setRowHeight(fontMetrics.getHeight() + 3);
    }

    @Override
    public void paint(Graphics graphics) {
        if (needsRowHeightCalculation) {
            calculateRowHeight(graphics);
            needsRowHeightCalculation = false;
        }
        super.paint(graphics);
    }

    @Override
    public void setFont(Font font) {
        needsRowHeightCalculation = true;
        super.setFont(font);
    }

    public HeaderListener getHeaderListener() {
        return headerListener;
    }
}
