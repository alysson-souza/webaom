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
 * Created on 02.09.05
 *
 * @version 	1.09
 * @author 		epoximator
 */
package epox.webaom.ui;

import epox.swing.TableModelSortable;
import epox.webaom.Job;
import epox.webaom.JobList;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TableModelJobs extends TableModelSortable implements RowModel {
    public static final int JOB = JobColumn.JOB;
    public static final long MASK =
            (1L << JobColumn.NUMB.getIndex())
                    | (1L << JobColumn.FILE.getIndex())
                    | (1L << JobColumn.STAT.getIndex());

    private final JobList jl;
    private int m_current_row;
    private Job m_current_job;

    public TableModelJobs(JobList j) {
        jl = j;
        reset();
    }

    public void reset() {
        m_current_row = -1;
        m_current_job = null;
        super.reset();
    }

    public int getColumnCount() {
        return JobColumn.getColumnCount();
    }

    public Class<?> getColumnClass(int col) {
        JobColumn column = JobColumn.fromIndex(col);
        return column != null ? column.getType() : String.class;
    }

    public int getRowCount() {
        return jl.size();
    }

    public Object getValueAt(int row, int col) {
        row = getRowIndex(row);

        if (col < 1 || row != m_current_row) {
            m_current_row = row;
            m_current_job = jl.get(row);
        }

        if (col == JOB) {
            return m_current_job;
        }

        JobColumn column = JobColumn.fromIndex(col);
        if (column == null) {
            return null;
        }

        return extractValue(column, m_current_job, row);
    }

    public boolean isCellEditable(int row, int col) {
        return false;
    }

    /**
     * Extract column value from Job. Hybrid approach: enum defines metadata, this method handles
     * extraction. Switch statement is debuggable and clear.
     */
    private Object extractValue(JobColumn column, Job job, int rowNumber) {
        if (column == JobColumn.NUMB) {
            return Integer.valueOf(rowNumber + 1);
        }

        switch (column) {
            case LIDN:
                return Integer.valueOf(job.mIlid);
            case FSIZ:
                return Long.valueOf(job.mLs);
            case FILE:
                return job.getFile().getAbsolutePath();
            case PATH:
                return job.getFile().getParent();
            case NAME:
                return job.getFile().getName();
            case STAT:
                return job.getStatusText();
            default:
                break;
        }

        if (job.m_fa == null) {
            return getDefaultValue(column);
        }

        switch (column) {
            case FIDN:
                return Integer.valueOf(job.m_fa.fid);
            case AIDN:
                return Integer.valueOf(job.m_fa.aid);
            case EIDN:
                return Integer.valueOf(job.m_fa.eid);
            case GIDN:
                return Integer.valueOf(job.m_fa.gid);
            case FLEN:
                return Integer.valueOf(job.m_fa.len);
            case FDUB:
                return defaultString(job.m_fa.dub);
            case FSUB:
                return defaultString(job.m_fa.sub);
            case FSRC:
                return defaultString(job.m_fa.rip);
            case FQUA:
                return defaultString(job.m_fa.qua);
            case FRES:
                return defaultString(job.m_fa.res);
            case FVID:
                return defaultString(job.m_fa.vid);
            case FAUD:
                return defaultString(job.m_fa.aud);
            case FMDS:
                return defaultString(job.m_fa.mds());
            case FMDA:
                return defaultString(job.m_fa.mda());
            default:
                break;
        }

        if (job.m_fa.anime != null) {
            switch (column) {
                case AYEA:
                    return Integer.valueOf(job.m_fa.anime.yea);
                case AEPS:
                    return Integer.valueOf(job.m_fa.anime.eps);
                case ALEP:
                    return Integer.valueOf(job.m_fa.anime.lep);
                case AROM:
                    return defaultString(job.m_fa.anime.rom);
                case AKAN:
                    return defaultString(job.m_fa.anime.kan);
                case AENG:
                    return defaultString(job.m_fa.anime.eng);
                case ATYP:
                    return defaultString(job.m_fa.anime.typ);
                case AYEN:
                    return Integer.valueOf(job.m_fa.anime.yen);
                default:
                    break;
            }
        }

        if (job.m_fa.ep != null) {
            switch (column) {
                case ENUM:
                    return defaultString(job.m_fa.ep.num);
                case EENG:
                    return defaultString(job.m_fa.ep.eng);
                case EKAN:
                    return defaultString(job.m_fa.ep.kan);
                case EROM:
                    return defaultString(job.m_fa.ep.rom);
                default:
                    break;
            }
        }

        if (job.m_fa.group != null) {
            switch (column) {
                case GNAM:
                    return defaultString(job.m_fa.group.name);
                case GSHO:
                    return defaultString(job.m_fa.group.sname);
                default:
                    break;
            }
        }

        return getDefaultValue(column);
    }

    /**
     * Return appropriate default value for column type. Defensive approach: never return null, show
     * empty/zero instead.
     */
    private Object getDefaultValue(JobColumn column) {
        Class<?> type = column.getType();
        if (type == Integer.class) return Integer.valueOf(0);
        if (type == Long.class) return Long.valueOf(0L);
        return "N/A";
    }

    /** Handle null/empty strings consistently. */
    private String defaultString(String s) {
        return (s == null || s.isEmpty()) ? "" : s;
    }

    // public void setValueAt(Object obj, int row, int col){
    // }
    public String getColumnName(int columnIndex) {
        JobColumn column = JobColumn.fromIndex(columnIndex);
        return column != null ? column.getDescription() : "";
    }

    public int getColumnIndex(String tag) {
        JobColumn column = JobColumn.fromTag(tag);
        return column != null ? column.getIndex() : -1;
    }

    public static void formatTable(JTable table) {
        table.setShowGrid(false);
        TableColumnModel m = table.getColumnModel();
        DefaultTableCellRenderer centerRend = new DefaultTableCellRenderer();
        centerRend.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < JobColumn.getColumnCount(); i++)
            if (i != JobColumn.FILE.getIndex()) m.getColumn(i).setCellRenderer(centerRend);
    }

    public void updateRow(int id) {
        this.fireTableRowsUpdated(id, id);
    }

    /*
     * public void updateRow(Job j){
     * try{
     * updateRow(m_rev[j.mIid]);
     * }catch(Exception e){
     * updateRow(j.mIid);
     * }
     * }
     */
    public void insertJob(int x) {
        // fireTableRowsInserted(j.mIid, j.mIid);
        fireTableRowsInserted(x, x);
    }

    public int[] convertRow(int row) {
        return new int[] {getRowIndex(row)};
    }

    public Job[] getJobs(int row) {
        return new Job[] {(Job) getValueAt(row, JOB)};
    }

    public void convertRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) rows[i] = getRowIndex(rows[i]);
    }
}
