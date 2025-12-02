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
    protected static final String[] cNames = {"Name", "%", "M", "Type", "Year", "Number", "Size"};
    protected static final Class<?>[] cTypes = {
        TreeTableModel.class, String.class, Character.class, Integer.class, Integer.class, Integer.class, String.class
    };

    public AlternateViewTableModel() {
        super(AppContext.animeTreeRoot);
    }

    @Override
    public int getColumnCount() {
        return cNames.length;
    }

    @Override
    public String getColumnName(int c) {
        return cNames[c];
    }

    @Override
    public Class<?> getColumnClass(int c) {
        return cTypes[c];
    }

    @Override
    public Object getValueAt(Object node, int c) {
        if (node instanceof Anime a) {
            return getAnimeValue(a, c);
        }
        if (node instanceof Episode e) {
            return c == NUMB ? e.size() : null;
        }
        if (node instanceof AniDBFile f) {
            return getFileValue(f, c);
        }
        if (node instanceof AnimeGroup g) {
            return getAnimeGroupValue(g, c);
        }
        if (node instanceof Path p) {
            return c == NUMB ? p.size() : null;
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
            case YEAR -> a.year;
            case NUMB -> a.size();
            case PRCT -> a.getCompletionPercent();
            case LAST -> a.getMissingPattern();
            default -> null;
        };
    }

    private Object getFileValue(AniDBFile f, int c) {
        return switch (c) {
            case TYPE -> f.getJob() == null ? null : f.getJob().getStatusText();
            case YEAR -> f.getVideoCodec();
            case NUMB -> f.getAudioCodec();
            default -> null;
        };
    }

    private Object getAnimeGroupValue(AnimeGroup g, int c) {
        return switch (c) {
            case NUMB -> g.size();
            case PRCT -> g.getCompletionPercent();
            default -> null;
        };
    }

    private Object getRootValue(int c) {
        return switch (c) {
            case NAME -> AppContext.animeTreeRoot.toString();
            case NUMB -> AppContext.animeTreeRoot.size();
            default -> null;
        };
    }

    @Override
    public Object getChild(Object parent, int index) {
        if (parent instanceof AniDBEntity entity) {
            return entity.get(index);
        }
        StringUtilities.err(parent);
        return null;
    }

    @Override
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
