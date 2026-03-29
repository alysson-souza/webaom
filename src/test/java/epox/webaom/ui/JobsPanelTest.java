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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import epox.webaom.AppContext;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobsPanelTest {
    @BeforeEach
    void setUp() {
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
    }

    @Test
    void saveAndLoadOptions_preservesVisibleColumnsOrderWidthsAndHiddenState() {
        JobsPanel sourcePanel = createJobsPanel();
        sourcePanel.loadOptions(new Options());

        sourcePanel.getJobsTable().moveColumn(2, 0);
        sourcePanel.getJobsTable().getColumnModel().getColumn(0).setPreferredWidth(321);
        sourcePanel.getJobsTable().getColumnModel().getColumn(1).setPreferredWidth(222);
        sourcePanel
                .getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(java.util.List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));

        Options options = new Options();
        sourcePanel.saveOptions(options);

        JobsPanel restoredPanel = createJobsPanel();
        restoredPanel.loadOptions(options);

        assertEquals(2, restoredPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        assertEquals(
                321,
                restoredPanel
                        .getJobsTable()
                        .getHeaderListener()
                        .getColumnByModelIndex(JobColumn.STAT.getIndex())
                        .getPreferredWidth());
        assertEquals(
                222,
                restoredPanel
                        .getJobsTable()
                        .getHeaderListener()
                        .getColumnByModelIndex(JobColumn.NUMB.getIndex())
                        .getPreferredWidth());
        assertFalse(restoredPanel.getJobsTable().getHeaderListener().isColumnVisible(JobColumn.FILE.getIndex()));
    }

    @Test
    void loadOptions_legacyFormatRestoresVisibleColumnsAndHidesTheRest() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "14,200;0,60");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);

        assertEquals(2, panel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        assertFalse(panel.getJobsTable().getHeaderListener().isColumnVisible(JobColumn.FILE.getIndex()));
    }

    @Test
    void setVisibleColumns_keepsAtLeastOneColumnVisible() {
        JobsPanel panel = createJobsPanel();

        panel.getJobsTable().getHeaderListener().setVisibleColumns(java.util.Collections.emptyList());

        assertEquals(1, panel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                panel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
    }

    @Test
    void formatTable_alignsColumnsByDataKind() {
        JTableJobs jobsTable = createJobsTable();

        assertEquals(SwingConstants.RIGHT, getAlignment(jobsTable, JobColumn.NUMB));
        assertEquals(SwingConstants.CENTER, getAlignment(jobsTable, JobColumn.STAT));
        assertEquals(SwingConstants.LEFT, getAlignment(jobsTable, JobColumn.FILE));
    }

    @Test
    void updateComponentTreeUi_preservesHiddenColumnsForLaterSave() {
        JobsPanel panel = createJobsPanel();
        panel.getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(java.util.List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));

        SwingUtilities.updateComponentTreeUI(panel);

        assertEquals(2, panel.getJobsTable().getColumnModel().getColumnCount());

        Options options = new Options();
        panel.saveOptions(options);

        JobsPanel restoredPanel = createJobsPanel();
        restoredPanel.loadOptions(options);

        assertEquals(2, restoredPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                restoredPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
    }

    @Test
    void saveOptions_usesLegacyVisibleOnlyFormatForCrossVersionCompatibility() {
        JobsPanel panel = createJobsPanel();
        panel.getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(java.util.List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));
        panel.getJobsTable().getColumnModel().getColumn(0).setPreferredWidth(200);
        panel.getJobsTable().getColumnModel().getColumn(1).setPreferredWidth(60);

        Options options = new Options();
        panel.saveOptions(options);

        assertEquals("14,200;0,60;", options.getString(Options.STR_JOB_COLUMNS));
    }

    @Test
    void loadOptions_newFormatThenSave_rewritesToLegacyVisibleOnlyFormat() {
        Options options = new Options();
        options.setString(Options.STR_JOB_COLUMNS, "14,200,1;0,60,1;11,973,0;");

        JobsPanel panel = createJobsPanel();
        panel.loadOptions(options);
        panel.saveOptions(options);

        assertEquals("14,200;0,60;", options.getString(Options.STR_JOB_COLUMNS));
    }

    private JobsPanel createJobsPanel() {
        TableModelJobs tableModel = new TableModelJobs(AppContext.jobs);
        JTableJobs jobsTable = new JTableJobs(tableModel);
        TableModelJobs.formatTable(jobsTable);
        return new JobsPanel(jobsTable, tableModel);
    }

    private JTableJobs createJobsTable() {
        return createJobsPanel().getJobsTable();
    }

    private int getAlignment(JTableJobs jobsTable, JobColumn column) {
        TableColumn tableColumn = jobsTable.getColumnModel().getColumn(column.getIndex());
        TableCellRenderer renderer = tableColumn.getCellRenderer();
        return ((DefaultTableCellRenderer) renderer).getHorizontalAlignment();
    }
}
