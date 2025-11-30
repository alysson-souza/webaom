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
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Episode;
import epox.webaom.data.Path;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class AlternateViewTableModel extends AbstractTreeTableModel {
    public static final int NAME = 0;
    public static final int PRCT = 1;
    public static final int LAST = 2;
    public static final int TYPE = 3;
    public static final int YEAR = 4;
    public static final int NUMB = 5;
    public static final int SIZE = 6;
    protected static String[] cNames = {"Name", "%", "M", "Type", "Year", "Number", "Size"};
    protected static Class<?>[] cTypes = {
        TreeTableModel.class, String.class, Character.class, Integer.class, Integer.class, Integer.class, String.class
    };

    public AlternateViewTableModel() {
        super(AppContext.animeTreeRoot);
    }

    public int getColumnCount() {
        return cNames.length;
    }

    public String getColumnName(int c) {
        return cNames[c];
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return cTypes[c];
    }

    public Object getValueAt(Object node, int c) {
        if (node instanceof Anime a) {
            return getAnimeValue(a, c);
        }
        if (node instanceof Episode e) {
            return c == NUMB ? Integer.valueOf(e.size()) : null;
        }
        if (node instanceof AniDBFile f) {
            return getFileValue(f, c);
        }
        if (node instanceof AnimeGroup g) {
            return getAnimeGroupValue(g, c);
        }
        if (node instanceof Path p) {
            return c == NUMB ? Integer.valueOf(p.size()) : null;
        }
        if (node == AppContext.animeTreeRoot) {
            return getRootValue(c);
        }
        if (node instanceof AniDBEntity g) {
            return c == SIZE ? StringUtilities.sbyte(g.getTotalSize()) : null;
        }
        StringUtilities.err("AnimeModel: Unknown object: " + node);
        return null;
    }

    private Object getAnimeValue(Anime a, int c) {
        return switch (c) {
            case NAME -> a.romajiTitle;
            case TYPE -> a.type;
            case YEAR -> Integer.valueOf(a.year);
            case NUMB -> Integer.valueOf(a.size());
            case PRCT -> Integer.valueOf(a.getCompletionPercent());
            case LAST -> Character.valueOf(a.getMissingPattern());
            default -> null;
        };
    }

    private Object getFileValue(AniDBFile f, int c) {
        return switch (c) {
            case TYPE -> f.getJob() == null ? null : f.getJob().getStatusText();
            case YEAR -> f.videoCodec;
            case NUMB -> f.audioCodec;
            default -> null;
        };
    }

    private Object getAnimeGroupValue(AnimeGroup g, int c) {
        return switch (c) {
            case NUMB -> Integer.valueOf(g.size());
            case PRCT -> Integer.valueOf(g.getCompletionPercent());
            default -> null;
        };
    }

    private Object getRootValue(int c) {
        return switch (c) {
            case NAME -> AppContext.animeTreeRoot.toString();
            case NUMB -> Integer.valueOf(AppContext.animeTreeRoot.size());
            default -> null;
        };
    }

    public Object getChild(Object parent, int index) {
        if (parent instanceof AniDBEntity entity) {
            return entity.get(index);
        }
        StringUtilities.err(parent);
        return null;
    }

    public int getChildCount(Object parent) {
        AniDBEntity p = (AniDBEntity) parent;
        p.buildSortedChildArray();

        return p.size();
    }

    @Override
    public boolean isLeaf(Object node) {
        return node instanceof AniDBFile;
    }

    public void formatTable(TableColumnModel m) {
        m.getColumn(AlternateViewTableModel.NAME).setPreferredWidth(1200);
        m.getColumn(AlternateViewTableModel.TYPE).setPreferredWidth(200);
        m.getColumn(AlternateViewTableModel.YEAR).setPreferredWidth(100);
        m.getColumn(AlternateViewTableModel.NUMB).setPreferredWidth(100);
        m.getColumn(AlternateViewTableModel.SIZE).setPreferredWidth(140);
        m.getColumn(AlternateViewTableModel.PRCT).setPreferredWidth(60);
        m.getColumn(AlternateViewTableModel.LAST).setPreferredWidth(30);
        DefaultTableCellRenderer r0 = new DefaultTableCellRenderer();
        r0.setHorizontalAlignment(SwingConstants.CENTER);
        m.getColumn(AlternateViewTableModel.TYPE).setCellRenderer(r0);
        m.getColumn(AlternateViewTableModel.YEAR).setCellRenderer(r0);
        m.getColumn(AlternateViewTableModel.NUMB).setCellRenderer(r0);
        m.getColumn(AlternateViewTableModel.PRCT).setCellRenderer(r0);
        m.getColumn(AlternateViewTableModel.LAST).setCellRenderer(r0);
        DefaultTableCellRenderer r1 = new DefaultTableCellRenderer();
        r1.setHorizontalAlignment(SwingConstants.RIGHT);
        m.getColumn(AlternateViewTableModel.SIZE).setCellRenderer(r1);
    }
}
