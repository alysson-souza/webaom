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

import epox.webaom.Job;
import epox.webaom.JobList;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

public class TableModelJobs extends AbstractTableModel implements RowModel {
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

    public void reset() {
        currentRowIndex = -1;
        currentJob = null;
    }

    @Override
    public int getColumnCount() {
        return JobColumn.getColumnCount();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        JobColumn column = JobColumn.fromIndex(columnIndex);
        return column != null ? column.getType() : String.class;
    }

    @Override
    public int getRowCount() {
        return jobList.size();
    }

    @Override
    public Object getValueAt(int row, int columnIndex) {
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

    /**
     * Extract column value from Job. Hybrid approach: enum defines metadata, this method handles
     * extraction. Switch statement is debuggable and clear.
     */
    private Object extractValue(JobColumn column, Job job, int rowNumber) {
        if (column == JobColumn.NUMB) {
            return rowNumber + 1;
        }

        switch (column) {
            case LIDN:
                return job.mylistId;
            case FSIZ:
                return job.fileSize;
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
            case FIDN -> {
                return job.anidbFile.getFileId();
            }
            case AIDN -> {
                return job.anidbFile.getAnimeId();
            }
            case EIDN -> {
                return job.anidbFile.getEpisodeId();
            }
            case GIDN -> {
                return job.anidbFile.getGroupId();
            }
            case FLEN -> {
                return job.anidbFile.getLengthInSeconds();
            }
            case FDUB -> {
                return defaultString(job.anidbFile.getDubLanguage());
            }
            case FSUB -> {
                return defaultString(job.anidbFile.getSubLanguage());
            }
            case FSRC -> {
                return defaultString(job.anidbFile.getRipSource());
            }
            case FQUA -> {
                return defaultString(job.anidbFile.getQuality());
            }
            case FRES -> {
                return defaultString(job.anidbFile.getResolution());
            }
            case FVID -> {
                return defaultString(job.anidbFile.getVideoCodec());
            }
            case FAUD -> {
                return defaultString(job.anidbFile.getAudioCodec());
            }
            case FMDS -> {
                return defaultString(job.anidbFile.getMissingDataStrict());
            }
            case FMDA -> {
                return defaultString(job.anidbFile.getMissingDataAdditional());
            }
            default -> {}
        }

        if (job.anidbFile.getAnime() != null) {
            switch (column) {
                case AYEA:
                    return job.anidbFile.getAnime().year;
                case AEPS:
                    return job.anidbFile.getAnime().episodeCount;
                case ALEP:
                    return job.anidbFile.getAnime().latestEpisode;
                case AROM:
                    return defaultString(job.anidbFile.getAnime().romajiTitle);
                case AKAN:
                    return defaultString(job.anidbFile.getAnime().kanjiTitle);
                case AENG:
                    return defaultString(job.anidbFile.getAnime().englishTitle);
                case ATYP:
                    return defaultString(job.anidbFile.getAnime().type);
                case AYEN:
                    return job.anidbFile.getAnime().endYear;
                default:
                    break;
            }
        }

        if (job.anidbFile.getEpisode() != null) {
            switch (column) {
                case ENUM:
                    return defaultString(job.anidbFile.getEpisode().num);
                case EENG:
                    return defaultString(job.anidbFile.getEpisode().eng);
                case EKAN:
                    return defaultString(job.anidbFile.getEpisode().kan);
                case EROM:
                    return defaultString(job.anidbFile.getEpisode().rom);
                default:
                    break;
            }
        }

        if (job.anidbFile.getGroup() != null) {
            switch (column) {
                case GNAM -> {
                    return defaultString(job.anidbFile.getGroup().name);
                }
                case GSHO -> {
                    return defaultString(job.anidbFile.getGroup().shortName);
                }
                default -> {}
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
            return 0;
        }
        if (type == Long.class) {
            return 0L;
        }
        return "N/A";
    }

    /** Handle null/empty strings consistently. */
    private String defaultString(String value) {
        return (value == null || value.isEmpty()) ? "" : value;
    }

    @Override
    public String getColumnName(int columnIndex) {
        JobColumn column = JobColumn.fromIndex(columnIndex);
        return column != null ? column.getDescription() : "";
    }

    public int getColumnIndex(String tag) {
        JobColumn column = JobColumn.fromTag(tag);
        return column != null ? column.getIndex() : -1;
    }

    public void updateRow(int rowIndex) {
        this.fireTableRowsUpdated(rowIndex, rowIndex);
    }

    public void insertJob(int rowIndex) {
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public int[] convertRow(int row) {
        return new int[] {row};
    }

    @Override
    public Job[] getJobs(int row) {
        return new Job[] {(Job) getValueAt(row, JOB)};
    }

    public void convertRows(int[] rows) {
        // No-op: with TableRowSorter, view-to-model conversion is done by JTable
    }
}
