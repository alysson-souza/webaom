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
import epox.util.U;
import epox.webaom.A;
import epox.webaom.data.AFile;
import epox.webaom.data.AG;
import epox.webaom.data.Anime;
import epox.webaom.data.Base;
import epox.webaom.data.Ep;
import epox.webaom.data.Path;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TableModelAlt extends AbstractTreeTableModel implements TreeTableModel {
	public static final int NAME = 0, PRCT = 1, LAST = 2, TYPE = 3, YEAR = 4, NUMB = 5, SIZE = 6;
	protected static String[] cNames = {"Name", "%", "M", "Type", "Year", "Number", "Size"};
	protected static Class[] cTypes = {TreeTableModel.class, String.class, Character.class, Integer.class,
			Integer.class, Integer.class, String.class};

	public TableModelAlt() {
		super(A.p);
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
					return U.sbyte(g.mLs);
			}
		}
		if (node instanceof Anime a) {
			switch (c) {
				case NAME :
					return a.rom;
				case TYPE :
					return a.typ;
				case YEAR :
					return Integer.valueOf(a.yea);
				case NUMB :
					return Integer.valueOf(a.size());
				case PRCT :
					return Integer.valueOf(a.getPct()); // return new Integer(a.getPct());
				case LAST :
					return new Character(a.miss());
				default :
					return null;
			}
		}
		if (node instanceof Ep e) {
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
					return f.vid; // new Integer(f.fid);
				case NUMB :
					return f.aud; // U.sbyte(f.mLs);
				default :
					return null;
			}
		}
		if (node instanceof AG g) {
			switch (c) {
				case NUMB :
					return Integer.valueOf(g.size());
				case PRCT :
					return Integer.valueOf(g.getPct());
				default :
					return null;
			}
		}
		if (node instanceof Path p) {
			switch (c) {
				case NUMB :
					return Integer.valueOf(p.size());
				// case PRCT: return new Integer(g.getPct());
				default :
					return null;
			}
		}
		if (node == A.p) {
			switch (c) {
				case NAME :
					return A.p.toString();
				case NUMB :
					return Integer.valueOf(A.p.size());
				default :
					return null;
			}
		}
		U.err("AnimeModel: Unknown object: " + node);
		return null;
	}

	public Object getChild(Object parent, int index) {
		if (parent instanceof Base) {
			return ((Base) parent).get(index);
		}
		U.err(parent);
		return null;
	}

	public int getChildCount(Object parent) {
		Base p = (Base) parent;
		p.mkArray();

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
