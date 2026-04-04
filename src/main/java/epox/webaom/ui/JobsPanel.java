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

import epox.swing.JScrollTable;
import epox.swing.layout.TableColumnSizing;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.Options;
import epox.webaom.data.AniDBFile;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;

public class JobsPanel extends JPanel implements ActionListener {
    private static final List<JobColumn> DEFAULT_VISIBLE_COLUMNS =
            List.of(JobColumn.NUMB, JobColumn.FILE, JobColumn.STAT);
    private static final String DEFAULT_FILE_PATH_SAMPLE =
            "/Volumes/Anime Library/Series Name/Series Name - 01 [BluRay 1080p][AAC].mkv";
    private static final String DEFAULT_STATUS_SAMPLE = "Waiting for network I/O";
    private static final String DEFAULT_ROW_NUMBER_SAMPLE = "00000";
    private static final int INDEX_NORMAL = 0;
    private static final int INDEX_PAUSED = 1;
    private static final int INDEX_WAITING = 2;
    private static final int INDEX_DOING = 3;
    private static final int INDEX_DISK_IO = 4;
    private static final int INDEX_CRC_ERROR = 5;
    private static final int INDEX_AUTO_UPDATE = 6;
    private static final int INDEX_MISSING = 7;
    private static final int INDEX_DONE = 8;
    private static final int INDEX_FAILED = 9;
    private static final int INDEX_NET_IO = 10;
    private static final int INDEX_CENSORED = 11;
    private static final int INDEX_UNKNOWN = 12;
    private static final int FILTER_CHECKBOX_COUNT = 13;
    private final JTableJobs jobsTable;
    private final JScrollTable scrollTable;
    private final TableModelJobs tableModel;
    private final JCheckBox[] filterCheckboxes;
    private int statusFilterMask = 0;
    private int fileStateFilterMask = 0;
    private boolean showUnknownFiles = false;
    private int updateCounter = 0;
    private boolean usesScaleAwareDefaultColumns;

    public JobsPanel(JTableJobs jobsTable, TableModelJobs tableModel) {
        super(new BorderLayout());
        this.jobsTable = jobsTable;
        this.tableModel = tableModel;
        this.scrollTable = new JScrollTable(jobsTable);

        JPanel southPanel = new JPanel(new GridLayout(2, FILTER_CHECKBOX_COUNT / 2));
        filterCheckboxes = new JCheckBox[FILTER_CHECKBOX_COUNT];
        for (int i = 0; i < FILTER_CHECKBOX_COUNT; i++) {
            filterCheckboxes[i] = new JCheckBox(getCheckboxLabel(i), false);
            filterCheckboxes[i].addActionListener(this);
            southPanel.add(filterCheckboxes[i]);
        }

        add(scrollTable, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);
    }

    private static String getCheckboxLabel(int checkboxIndex) {
        return switch (checkboxIndex) {
            case INDEX_DISK_IO -> "DiskIO";
            case INDEX_DOING -> "Doing";
            case INDEX_DONE -> "Done";
            case INDEX_FAILED -> "Failed";
            case INDEX_MISSING -> "Missing";
            case INDEX_NET_IO -> "NetIO";
            case INDEX_NORMAL -> "Normal";
            case INDEX_PAUSED -> "Paused";
            case INDEX_AUTO_UPDATE -> "Auto Update";
            case INDEX_WAITING -> "Waiting";
            case INDEX_UNKNOWN -> "Unknown";
            case INDEX_CRC_ERROR -> "Corrupt";
            case INDEX_CENSORED -> "Censored";
            default -> "No such checkbox";
        };
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        statusFilterMask = 0;
        fileStateFilterMask = 0;
        if (filterCheckboxes[INDEX_NORMAL].isSelected()) {
            statusFilterMask |= Job.H_NORMAL;
        }
        if (filterCheckboxes[INDEX_PAUSED].isSelected()) {
            statusFilterMask |= Job.H_PAUSED;
        }
        if (filterCheckboxes[INDEX_MISSING].isSelected()) {
            statusFilterMask |= Job.H_MISSING;
        }
        if (filterCheckboxes[INDEX_WAITING].isSelected()) {
            statusFilterMask |= Job.S_DO;
        }
        if (filterCheckboxes[INDEX_DOING].isSelected()) {
            statusFilterMask |= Job.S_DOING;
        }
        if (filterCheckboxes[INDEX_DONE].isSelected()) {
            statusFilterMask |= Job.S_DONE;
        }
        if (filterCheckboxes[INDEX_FAILED].isSelected()) {
            statusFilterMask |= Job.S_FAILED;
        }
        if (filterCheckboxes[INDEX_NET_IO].isSelected()) {
            statusFilterMask |= Job.D_NIO;
        }
        if (filterCheckboxes[INDEX_DISK_IO].isSelected()) {
            statusFilterMask |= Job.D_DIO;
        }
        if (filterCheckboxes[INDEX_CRC_ERROR].isSelected()) {
            fileStateFilterMask |= AniDBFile.F_CRCERR;
        }
        if (filterCheckboxes[INDEX_CENSORED].isSelected()) {
            fileStateFilterMask |= AniDBFile.F_UNC;
        }
        showUnknownFiles = filterCheckboxes[INDEX_UNKNOWN].isSelected();

        applyFilter();
    }

    public void saveOptions(Options options) {
        StringBuilder columnConfig = new StringBuilder();
        Enumeration<TableColumn> columnEnumeration = jobsTable.getColumnModel().getColumns();
        while (columnEnumeration.hasMoreElements()) {
            TableColumn column = columnEnumeration.nextElement();
            appendColumnConfig(columnConfig, column);
        }
        options.setString(Options.STR_JOB_COLUMNS, columnConfig.toString());
    }

    public void loadOptions(Options options) {
        String columnConfig = options.getString(Options.STR_JOB_COLUMNS);
        if (columnConfig == null || columnConfig.isEmpty()) {
            applyDefaultColumnConfig();
            usesScaleAwareDefaultColumns = true;
            return;
        }
        List<ColumnConfig> parsedColumns = parseColumnConfig(columnConfig);
        if (parsedColumns.isEmpty()) {
            applyDefaultColumnConfig();
            usesScaleAwareDefaultColumns = true;
            return;
        }
        usesScaleAwareDefaultColumns = false;
        applyColumnConfig(parsedColumns);
    }

    public void reapplyScaleAwareSizing() {
        if (usesScaleAwareDefaultColumns) {
            applyDefaultColumnConfig();
        }
    }

    public void scaleCurrentColumnWidths(double scaleFactor) {
        TableColumnSizing.scalePreferredWidths(jobsTable.getHeaderListener().getAllColumns(), scaleFactor);
    }

    private void appendColumnConfig(StringBuilder builder, TableColumn column) {
        builder.append(column.getModelIndex())
                .append(",")
                .append(column.getPreferredWidth())
                .append(";");
    }

    private List<ColumnConfig> parseColumnConfig(String columnConfig) {
        List<ColumnConfig> parsedColumns = new ArrayList<>();
        StringTokenizer tokenizer = new StringTokenizer(columnConfig, ";");
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken();
            String[] columnParts = token.split(",", 3);
            if (columnParts.length < 2) {
                continue;
            }
            int modelIndex = StringUtilities.i(columnParts[0]);
            int columnWidth = StringUtilities.i(columnParts[1]);
            boolean visible = columnParts.length < 3 || StringUtilities.i(columnParts[2]) != 0;
            parsedColumns.add(new ColumnConfig(modelIndex, columnWidth, visible));
        }
        return parsedColumns;
    }

    private void applyColumnConfig(List<ColumnConfig> parsedColumns) {
        Set<Integer> configuredModelIndices = new HashSet<>();
        List<Integer> visibleModelIndices = new ArrayList<>();
        for (ColumnConfig columnConfig : parsedColumns) {
            if (!columnConfig.isValid() || !configuredModelIndices.add(columnConfig.modelIndex())) {
                continue;
            }
            TableColumn column = jobsTable.getHeaderListener().getColumnByModelIndex(columnConfig.modelIndex());
            if (column == null) {
                continue;
            }
            column.setPreferredWidth(columnConfig.width());
            if (columnConfig.visible()) {
                visibleModelIndices.add(columnConfig.modelIndex());
            }
        }
        if (visibleModelIndices.isEmpty()) {
            jobsTable.getHeaderListener().setMask(TableModelJobs.MASK);
            return;
        }
        jobsTable.getHeaderListener().setVisibleColumns(visibleModelIndices);
    }

    private void applyDefaultColumnConfig() {
        jobsTable
                .getHeaderListener()
                .setVisibleColumns(DEFAULT_VISIBLE_COLUMNS.stream()
                        .map(JobColumn::getIndex)
                        .toList());
        applyDefaultColumnWidth(JobColumn.NUMB, 48, DEFAULT_ROW_NUMBER_SAMPLE);
        applyDefaultColumnWidth(JobColumn.FILE, 360, DEFAULT_FILE_PATH_SAMPLE);
        applyDefaultColumnWidth(JobColumn.STAT, 140, DEFAULT_STATUS_SAMPLE);
    }

    private void applyDefaultColumnWidth(JobColumn jobColumn, int minimumWidth, String... sampleValues) {
        TableColumn column = jobsTable.getHeaderListener().getColumnByModelIndex(jobColumn.getIndex());
        if (column != null) {
            TableColumnSizing.applyPreferredWidth(jobsTable, column, minimumWidth, sampleValues);
        }
    }

    public void update() {
        updateCounter++;
        if (updateCounter % 4 == 0 && filterCheckboxes[INDEX_AUTO_UPDATE].isSelected()) {
            applyFilter();
        } else {
            int topRow = scrollTable.getTopVisibleRow();
            int bottomRow = scrollTable.getBottomVisibleRow();

            if (topRow >= 0 || bottomRow > topRow) {
                tableModel.fireTableRowsUpdated(topRow, bottomRow);
            }
        }
    }

    private void applyFilter() {
        AppContext.jobs.filter(statusFilterMask, fileStateFilterMask, showUnknownFiles);
        tableModel.fireTableDataChanged();
        jobsTable.updateUI();
    }

    JTableJobs getJobsTable() {
        return jobsTable;
    }

    private record ColumnConfig(int modelIndex, int width, boolean visible) {
        boolean isValid() {
            return modelIndex >= 0 && modelIndex < JobColumn.getColumnCount();
        }
    }
}
