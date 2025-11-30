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

import java.awt.Component;
import java.awt.Insets;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Table cell renderer that renders column headers as clickable buttons for sorting.
 */
public class SortButtonRenderer extends JButton implements TableCellRenderer {
	/** Currently pressed column index, -1 if NONE */
	private int pressedColumn;

	public SortButtonRenderer() {
		pressedColumn = -1;
		setMargin(new Insets(0, 0, 0, 0));
		setToolTipText("Right click to select columns");
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		setText((value == null) ? "" : value.toString());
		boolean isPressed = (column == pressedColumn);
		getModel().setPressed(isPressed);
		getModel().setArmed(isPressed);
		return this;
	}

	public void setPressedColumn(int column) {
		pressedColumn = column;
	}
}
