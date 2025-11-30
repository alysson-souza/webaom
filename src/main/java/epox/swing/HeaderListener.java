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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
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
    protected JPopupMenu columnPopup = null;

    public HeaderListener(JTableHeader header, TableColumnModel columns) {
        tableHeader = header;
        columnModel = columns;
        columnPopup = null;
    }

    public void setMask(long mask) {
        if (columnModel != null) {
            columnPopup = new JPopupMenu();
            for (int index = 0; index < columnModel.getColumnCount(); index++) {
                TableColumn column = columnModel.getColumn(index);
                JCheckBoxMenuItem menuItem =
                        new JCheckBoxMenuItem(column.getHeaderValue().toString(), (1L << index & mask) == 1L << index);
                menuItem.addActionListener(new ColumnAction(columnModel, column, menuItem));
                columnPopup.add(menuItem);
            }
            for (int index = columnModel.getColumnCount() - 1; index >= 0; index--) {
                if ((1L << index & mask) != 1L << index) {
                    columnModel.removeColumn(columnModel.getColumn(index));
                }
            }
        }
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
        private final TableColumnModel model;
        private final TableColumn column;
        private final JCheckBoxMenuItem menuItem;

        ColumnAction(TableColumnModel model, TableColumn column, JCheckBoxMenuItem menuItem) {
            this.model = model;
            this.column = column;
            this.menuItem = menuItem;
        }

        @Override
        public void actionPerformed(ActionEvent event) {
            if (menuItem.isSelected()) {
                model.addColumn(column);
            } else if (model.getColumnCount() > 1) {
                model.removeColumn(column);
            } else {
                menuItem.setSelected(true);
            }
        }
    }
}
