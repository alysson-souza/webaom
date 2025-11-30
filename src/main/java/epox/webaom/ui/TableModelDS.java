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
 * Created on 22.01.05
 *
 * @version 	1.09, 1.05
 * @author 		epoximator
 */
package epox.webaom.ui;

import epox.util.DSData;
import java.util.Vector;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TableModelDS extends AbstractTableModel {
	public static final int COLUMN_SELECTED = 0;
	public static final int COLUMN_SOURCE = 1;
	public static final int COLUMN_DESTINATION = 2;

	private Vector /* !<DSData> */ rowDataList;
	private final String sourceColumnTitle;
	private final String destinationColumnTitle;
	private final DSData emptyRowPlaceholder;

	public TableModelDS(Vector /* <DSData> */ dataVector, String sourceTitle, String destinationTitle) {
		setData(dataVector);
		sourceColumnTitle = sourceTitle;
		destinationColumnTitle = destinationTitle;
		emptyRowPlaceholder = new DSData("", "", false);
	}

	public Vector /* !<DSData> */ getData() {
		return rowDataList;
	}

	public void setData(Vector /* !<DSData> */ dataVector) {
		rowDataList = dataVector;
	}

	public int getColumnCount() {
		return 3;
	}

	public Class /* !<?> */ getColumnClass(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_SELECTED :
				return Boolean.class;
			default :
				return String.class;
		}
	}

	public int getRowCount() {
		return rowDataList.size() + 1;
	}

	public Object getValueAt(int rowIndex, int columnIndex) {
		DSData rowData;
		if (rowIndex == rowDataList.size()) {
			rowData = emptyRowPlaceholder;
		} else {
			rowData = (DSData) rowDataList.elementAt(rowIndex); // !
		}
		switch (columnIndex) {
			case COLUMN_SELECTED :
				return rowData.enabled;
			case COLUMN_SOURCE :
				return rowData.source;
			case COLUMN_DESTINATION :
				return rowData.destination;
		}
		return null;
	}

	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return true;
	}

	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		DSData rowData;
		if (rowIndex == rowDataList.size()) {
			rowData = new DSData("", "", false);
			rowDataList.add(rowData);
			fireTableRowsInserted(rowIndex, rowIndex);
		} else {
			rowData = (DSData) rowDataList.elementAt(rowIndex); // !
		}
		if (rowData != null) {
			switch (columnIndex) {
				case COLUMN_SELECTED :
					if (!rowData.source.isEmpty()) {
						rowData.enabled = (Boolean) value;
					}
					break;
				case COLUMN_SOURCE :
					rowData.source = (String) value;
					if (rowData.source.isEmpty()) {
						rowDataList.remove(rowData);
					}
					break;
				case COLUMN_DESTINATION :
					rowData.destination = getValidatedDestination(value);
					break;
			}
		}
	}

	/**
	 * Validates the destination value, ensuring it doesn't duplicate an existing source.
	 */
	private String getValidatedDestination(Object destinationValue) {
		String destination = (String) destinationValue;
		DSData rowData;
		for (int i = 0; i < rowDataList.size(); i++) {
			rowData = (DSData) rowDataList.elementAt(i); // !
			if (rowData.source.equals(destination)) {
				return "";
			}
		}
		return destination;
	}

	public String getColumnName(int columnIndex) {
		switch (columnIndex) {
			case COLUMN_SELECTED :
				return "Enabled";
			case COLUMN_SOURCE :
				return sourceColumnTitle;
			case COLUMN_DESTINATION :
				return destinationColumnTitle;
		}
		return "No such column!";
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
}
