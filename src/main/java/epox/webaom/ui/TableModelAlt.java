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
 * Created on 25.des.2005 16:29:23
 * Filename: AnimeModel.java
 */
package epox.webaom.ui;

import com.sun.swing.AbstractTreeTableModel;
import com.sun.swing.TreeTableModel;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.data.AFile;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Episode;
import epox.webaom.data.Path;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TableModelAlt extends AbstractTreeTableModel implements TreeTableModel {
	public static final int NAME = 0;
	public static final int PRCT = 1;
	public static final int LAST = 2;
	public static final int TYPE = 3;
	public static final int YEAR = 4;
	public static final int NUMB = 5;
	public static final int SIZE = 6;
	protected static String[] cNames = {"Name", "%", "M", "Type", "Year", "Number", "Size"};
	protected static Class[] cTypes = {TreeTableModel.class, String.class, Character.class, Integer.class,
			Integer.class, Integer.class, String.class};

	public TableModelAlt() {
		super(AppContext.p);
	}

	public int getColumnCount() {
		return cNames.length;
	}

	public String getColumnName(int c) {
		return cNames[c];
	}

	public Class getColumnClass(int c) {
		return cTypes[c];
	}

	public Object getValueAt(Object node, int c) {
		if (node instanceof Base g) {
			switch (c) {
				case SIZE :
					return StringUtilities.sbyte(g.totalSize);
			}
		}
		if (node instanceof Anime a) {
			switch (c) {
				case NAME :
					return a.romajiTitle;
				case TYPE :
					return a.type;
				case YEAR :
					return Integer.valueOf(a.year);
				case NUMB :
					return Integer.valueOf(a.size());
				case PRCT :
					return Integer.valueOf(a.getCompletionPercent());
				case LAST :
					return Character.valueOf(a.getMissingPattern());
				default :
					return null;
			}
		}
		if (node instanceof Episode e) {
			switch (c) {
				case NUMB :
					return Integer.valueOf(e.size());
				default :
					return null;
			}
		}
		if (node instanceof AFile f) {
			switch (c) {
				case TYPE :
					return f.getJob() == null ? null : f.getJob().getStatusText();
				case YEAR :
					return f.videoCodec; // new Integer(f.fileId);
				case NUMB :
					return f.audioCodec; // U.sbyte(f.totalSize);
				default :
					return null;
			}
		}
		if (node instanceof AnimeGroup g) {
			switch (c) {
				case NUMB :
					return Integer.valueOf(g.size());
				case PRCT :
					return Integer.valueOf(g.getCompletionPercent());
				default :
					return null;
			}
		}
		if (node instanceof Path p) {
			switch (c) {
				case NUMB :
					return Integer.valueOf(p.size());
				// case PRCT: return new Integer(g.getCompletionPercent());
				default :
					return null;
			}
		}
		if (node == AppContext.p) {
			switch (c) {
				case NAME :
					return AppContext.p.toString();
				case NUMB :
					return Integer.valueOf(AppContext.p.size());
				default :
					return null;
			}
		}
		StringUtilities.err("AnimeModel: Unknown object: " + node);
		return null;
	}

	public Object getChild(Object parent, int index) {
		if (parent instanceof Base) {
			return ((Base) parent).get(index);
		}
		StringUtilities.err(parent);
		return null;
	}

	public int getChildCount(Object parent) {
		Base p = (Base) parent;
		p.buildSortedChildArray();

		return p.size();
	}

	public boolean isLeaf(Object node) {
		return node instanceof AFile;
	}

	public void formatTable(TableColumnModel m) {
		m.getColumn(TableModelAlt.NAME).setPreferredWidth(1200);
		m.getColumn(TableModelAlt.TYPE).setPreferredWidth(200);
		m.getColumn(TableModelAlt.YEAR).setPreferredWidth(100);
		m.getColumn(TableModelAlt.NUMB).setPreferredWidth(100);
		m.getColumn(TableModelAlt.SIZE).setPreferredWidth(140);
		m.getColumn(TableModelAlt.PRCT).setPreferredWidth(60);
		m.getColumn(TableModelAlt.LAST).setPreferredWidth(30);
		DefaultTableCellRenderer r0 = new DefaultTableCellRenderer();
		r0.setHorizontalAlignment(SwingConstants.CENTER);
		// m.getColumn(AnimeModel.NAME).setCellRenderer(centerRend);
		m.getColumn(TableModelAlt.TYPE).setCellRenderer(r0);
		m.getColumn(TableModelAlt.YEAR).setCellRenderer(r0);
		m.getColumn(TableModelAlt.NUMB).setCellRenderer(r0);
		m.getColumn(TableModelAlt.PRCT).setCellRenderer(r0);
		m.getColumn(TableModelAlt.LAST).setCellRenderer(r0);
		DefaultTableCellRenderer r1 = new DefaultTableCellRenderer();
		r1.setHorizontalAlignment(SwingConstants.RIGHT);
		m.getColumn(TableModelAlt.SIZE).setCellRenderer(r1);
	}
}
