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

import epox.swing.LeadingEllipsisCellRenderer;
import epox.swing.layout.TableColumnSizing;
import epox.webaom.Job;
import epox.webaom.JobList;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class TableModelJobs extends AbstractTableModel implements RowModel {
    public static final int JOB = JobColumn.JOB;
    static final String ROW_NUMBER_SAMPLE = "00000";

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
        for (int i = 0; i < JobColumn.getColumnCount(); i++) {
            JobColumn column = JobColumn.fromIndex(i);
            if (column != null) {
                columnModel.getColumn(i).setCellRenderer(createRenderer(column));
            }
        }
        applyRowNumberMinimumWidth(table, columnModel.getColumn(JobColumn.NUMB.getIndex()));
    }

    /** Keeps the row-number column wide enough for its header and five digits, whatever width was saved. */
    static void applyRowNumberMinimumWidth(JTable table, TableColumn column) {
        column.setMinWidth(
                TableColumnSizing.calculatePreferredWidth(table, column.getHeaderValue(), 0, ROW_NUMBER_SAMPLE));
    }

    private static DefaultTableCellRenderer createRenderer(JobColumn column) {
        DefaultTableCellRenderer renderer =
                switch (column) {
                    case FILE, PATH -> new LeadingEllipsisCellRenderer();
                    case STAT -> new JobStatusCellRenderer();
                    default -> new DefaultTableCellRenderer();
                };
        renderer.setHorizontalAlignment(column.getHorizontalAlignment());
        return renderer;
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
            case TITL:
                return buildTitle(job);
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

    /** "Anime - 05 - Episode Title [Group]" once AniDB identified the file, the filename until then. */
    private String buildTitle(Job job) {
        AniDBFile file = job.anidbFile;
        if (file == null || file.getAnime() == null || isBlank(file.getAnime().romajiTitle)) {
            return job.getFile().getName();
        }
        StringBuilder title = new StringBuilder(file.getAnime().romajiTitle);
        Episode episode = file.getEpisode();
        if (episode != null) {
            if (!isBlank(episode.num)) {
                title.append(" - ").append(episode.num);
            }
            String episodeTitle = isBlank(episode.eng) ? episode.rom : episode.eng;
            if (!isBlank(episodeTitle)) {
                title.append(" - ").append(episodeTitle);
            }
        }
        Group group = file.getGroup();
        if (group != null && !isBlank(group.shortName)) {
            title.append(" [").append(group.shortName).append(']');
        }
        return title.toString();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
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
