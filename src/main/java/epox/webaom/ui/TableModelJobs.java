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
            (1L << JobColumn.NUMB.getIndex()) | (1L << JobColumn.FILE.getIndex()) | (1L << JobColumn.STAT.getIndex());

    private final JobList jobList;
    private int currentRowIndex;
    private Job currentJob;

    public TableModelJobs(JobList jobList) {
        this.jobList = jobList;
        reset();
    }

    public void reset() {
        currentRowIndex = -1;
        currentJob = null;
        super.reset();
    }

    public int getColumnCount() {
        return JobColumn.getColumnCount();
    }

    public Class<?> getColumnClass(int columnIndex) {
        JobColumn column = JobColumn.fromIndex(columnIndex);
        return column != null ? column.getType() : String.class;
    }

    public int getRowCount() {
        return jobList.size();
    }

    public Object getValueAt(int row, int columnIndex) {
        row = getRowIndex(row);

        if (columnIndex < 1 || row != currentRowIndex) {
            currentRowIndex = row;
            currentJob = jobList.get(row);
        }

        if (columnIndex == JOB) {
            return currentJob;
        }

        JobColumn column = JobColumn.fromIndex(columnIndex);
        if (column == null) {
            return null;
        }

        return extractValue(column, currentJob, row);
    }

    public boolean isCellEditable(int row, int columnIndex) {
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
                return Integer.valueOf(job.mylistId);
            case FSIZ:
                return Long.valueOf(job.fileSize);
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

        if (job.anidbFile == null) {
            return getDefaultValue(column);
        }

        switch (column) {
            case FIDN:
                return Integer.valueOf(job.anidbFile.fileId);
            case AIDN:
                return Integer.valueOf(job.anidbFile.animeId);
            case EIDN:
                return Integer.valueOf(job.anidbFile.episodeId);
            case GIDN:
                return Integer.valueOf(job.anidbFile.groupId);
            case FLEN:
                return Integer.valueOf(job.anidbFile.lengthInSeconds);
            case FDUB:
                return defaultString(job.anidbFile.dubLanguage);
            case FSUB:
                return defaultString(job.anidbFile.subLanguage);
            case FSRC:
                return defaultString(job.anidbFile.ripSource);
            case FQUA:
                return defaultString(job.anidbFile.quality);
            case FRES:
                return defaultString(job.anidbFile.resolution);
            case FVID:
                return defaultString(job.anidbFile.videoCodec);
            case FAUD:
                return defaultString(job.anidbFile.audioCodec);
            case FMDS:
                return defaultString(job.anidbFile.getMissingDataStrict());
            case FMDA:
                return defaultString(job.anidbFile.getMissingDataAdditional());
            default:
                break;
        }

        if (job.anidbFile.anime != null) {
            switch (column) {
                case AYEA:
                    return Integer.valueOf(job.anidbFile.anime.year);
                case AEPS:
                    return Integer.valueOf(job.anidbFile.anime.episodeCount);
                case ALEP:
                    return Integer.valueOf(job.anidbFile.anime.latestEpisode);
                case AROM:
                    return defaultString(job.anidbFile.anime.romajiTitle);
                case AKAN:
                    return defaultString(job.anidbFile.anime.kanjiTitle);
                case AENG:
                    return defaultString(job.anidbFile.anime.englishTitle);
                case ATYP:
                    return defaultString(job.anidbFile.anime.type);
                case AYEN:
                    return Integer.valueOf(job.anidbFile.anime.endYear);
                default:
                    break;
            }
        }

        if (job.anidbFile.episode != null) {
            switch (column) {
                case ENUM:
                    return defaultString(job.anidbFile.episode.num);
                case EENG:
                    return defaultString(job.anidbFile.episode.eng);
                case EKAN:
                    return defaultString(job.anidbFile.episode.kan);
                case EROM:
                    return defaultString(job.anidbFile.episode.rom);
                default:
                    break;
            }
        }

        if (job.anidbFile.group != null) {
            switch (column) {
                case GNAM:
                    return defaultString(job.anidbFile.group.name);
                case GSHO:
                    return defaultString(job.anidbFile.group.shortName);
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
        if (type == Integer.class) {
            return Integer.valueOf(0);
        }
        if (type == Long.class) {
            return Long.valueOf(0L);
        }
        return "N/A";
    }

    /** Handle null/empty strings consistently. */
    private String defaultString(String value) {
        return (value == null || value.isEmpty()) ? "" : value;
    }

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
        TableColumnModel columnModel = table.getColumnModel();
        DefaultTableCellRenderer centeredRenderer = new DefaultTableCellRenderer();
        centeredRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        for (int i = 0; i < JobColumn.getColumnCount(); i++) {
            if (i != JobColumn.FILE.getIndex()) {
                columnModel.getColumn(i).setCellRenderer(centeredRenderer);
            }
        }
    }

    public void updateRow(int rowIndex) {
        this.fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void insertJob(int rowIndex) {
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public int[] convertRow(int row) {
        return new int[] {getRowIndex(row)};
    }

    public Job[] getJobs(int row) {
        return new Job[] {(Job) getValueAt(row, JOB)};
    }

    public void convertRows(int[] rows) {
        for (int i = 0; i < rows.length; i++) {
            rows[i] = getRowIndex(rows[i]);
        }
    }
}
