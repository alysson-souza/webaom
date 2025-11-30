/*
 * Created on 27.mai.2006 17:48:04
 * Filename: JPanelJobs.java
 */
package epox.webaom.ui;

import epox.swing.JScrollTable;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.Options;
import epox.webaom.data.AniDBFile;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Enumeration;
import java.util.StringTokenizer;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class JobsPanel extends JPanel implements ActionListener {
    private static final int INDEX_NORMAL = 0;
    private static final int INDEX_PAUSED = 1;
    private static final int INDEX_WAITING = 2;
    private static final int INDEX_DOING = 3;
    private static final int INDEX_DISK_IO = 4;
    private static final int INDEX_CRC_ERROR = 5;
    private static final int INDEX_AUTO_UPDATE = 6;
    private static final int INDEX_MISSING = 7;
    private static final int INDEX_DELETED = 8;
    private static final int INDEX_DONE = 9;
    private static final int INDEX_FAILED = 10;
    private static final int INDEX_NET_IO = 11;
    private static final int INDEX_CENSORED = 12;
    private static final int INDEX_UNKNOWN = 13;
    private static final int FILTER_CHECKBOX_COUNT = 14;
    private final JTableJobs jobsTable;
    private final JScrollTable scrollTable;
    private final TableModelJobs tableModel;
    private final JCheckBox[] filterCheckboxes;
    private int statusFilterMask = 0;
    private int fileStateFilterMask = 0;
    private boolean showUnknownFiles = false;
    private int updateCounter = 0;

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
        switch (checkboxIndex) {
            case INDEX_DELETED:
                return "Deleted";
            case INDEX_DISK_IO:
                return "DiskIO";
            case INDEX_DOING:
                return "Doing";
            case INDEX_DONE:
                return "Done";
            case INDEX_FAILED:
                return "Failed";
            case INDEX_MISSING:
                return "Missing";
            case INDEX_NET_IO:
                return "NetIO";
            case INDEX_NORMAL:
                return "Normal";
            case INDEX_PAUSED:
                return "Paused";
            case INDEX_AUTO_UPDATE:
                return "Auto Update";
            case INDEX_WAITING:
                return "Waiting";
            case INDEX_UNKNOWN:
                return "Unknown";
            case INDEX_CRC_ERROR:
                return "Corrupt";
            case INDEX_CENSORED:
                return "Censored";
            default:
                return "No such checkbox";
        }
    }

    public void actionPerformed(ActionEvent event) {
        statusFilterMask = 0;
        fileStateFilterMask = 0;
        if (filterCheckboxes[INDEX_NORMAL].isSelected()) {
            statusFilterMask |= Job.H_NORMAL;
        }
        if (filterCheckboxes[INDEX_PAUSED].isSelected()) {
            statusFilterMask |= Job.H_PAUSED;
        }
        if (filterCheckboxes[INDEX_DELETED].isSelected()) {
            statusFilterMask |= Job.H_DELETED;
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
            columnConfig
                    .append(column.getModelIndex())
                    .append(",")
                    .append(column.getPreferredWidth())
                    .append(";");
        }
        options.setString(Options.STR_JOB_COLUMNS, columnConfig.toString());
    }

    public void loadOptions(Options options) {
        String columnConfig = options.getString(Options.STR_JOB_COLUMNS);
        if (columnConfig == null || columnConfig.isEmpty()) {
            // default column configuration
            columnConfig = "0,55;11,973;14,132";
        }
        TableColumnModel columnModel = jobsTable.getColumnModel();
        long visibilityMask = 0;
        int columnIndex;
        int columnWidth;
        int position = 0;
        StringTokenizer tokenizer = new StringTokenizer(columnConfig, ";");
        int[] columnIndices = new int[tokenizer.countTokens()];
        while (tokenizer.hasMoreTokens()) {
            String[] columnParts = tokenizer.nextToken().split(",", 2);
            columnIndex = StringUtilities.i(columnParts[0]);
            columnWidth = StringUtilities.i(columnParts[1]);
            visibilityMask |= 1L << position;
            columnIndices[position++] = columnIndex;
            columnModel.getColumn(columnIndex).setPreferredWidth(columnWidth);
        }
        for (position = 0; position < columnIndices.length; position++) {
            columnModel.moveColumn(columnIndices[position], position);
            for (int j = position + 1; j < columnIndices.length; j++) {
                if (columnIndices[j] < columnIndices[position]) {
                    columnIndices[j]++;
                }
            }
        }
        if (visibilityMask == 0) {
            visibilityMask = TableModelJobs.MASK;
        }
        jobsTable.getHeaderListener().setMask(visibilityMask);
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
}
