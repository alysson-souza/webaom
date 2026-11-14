/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.data.AniDBFile;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.table.TableColumn;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class JobsPanelFilterCharacterizationTest {
    private JobList originalJobs;
    private JobCounter originalCounter;
    private Component originalPrimaryPopup;
    private Set<ShortcutInfo> originalShortcuts;

    @BeforeEach
    void isolateGlobals() {
        originalJobs = AppContext.jobs;
        originalCounter = AppContext.jobCounter;
        originalPrimaryPopup = AppContext.primaryPopupMenu;
        originalShortcuts = Collections.newSetFromMap(new IdentityHashMap<>());
        originalShortcuts.addAll(AppContext.shortcutRegistry.getAllShortcuts());
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void restoreGlobals() {
        for (ShortcutInfo shortcut : AppContext.shortcutRegistry.getAllShortcuts()) {
            if (!originalShortcuts.contains(shortcut)) {
                AppContext.shortcutRegistry.unregister(shortcut);
            }
        }
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalCounter;
        AppContext.primaryPopupMenu = originalPrimaryPopup;
    }

    @Test
    void selectedFilterCheckboxesProduceTheCombinedStatusAndFileStateMasks() {
        JobList jobs = mock(JobList.class);
        AppContext.jobs = jobs;
        TableModelJobs model = mock(TableModelJobs.class);
        JTableJobs table = new JTableJobs(model);
        JobsPanel panel = new JobsPanel(table, model);
        for (String label : List.of(
                "Normal",
                "Paused",
                "Missing",
                "Waiting",
                "Doing",
                "Done",
                "Failed",
                "NetIO",
                "DiskIO",
                "Corrupt",
                "Censored",
                "Unknown")) {
            checkbox(panel, label).setSelected(true);
        }

        panel.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "filters"));

        int statusMask = Job.H_NORMAL
                | Job.H_PAUSED
                | Job.H_MISSING
                | Job.S_DO
                | Job.S_DOING
                | Job.S_DONE
                | Job.S_FAILED
                | Job.D_NIO
                | Job.D_DIO;
        verify(jobs).filter(statusMask, AniDBFile.F_CRCERR | AniDBFile.F_UNC, true);
        verify(model).fireTableDataChanged();

        for (JCheckBox checkBox : components(panel, JCheckBox.class)) {
            checkBox.setSelected(false);
        }
        panel.actionPerformed(new ActionEvent(panel, ActionEvent.ACTION_PERFORMED, "clear"));
        verify(jobs).filter(0, 0, false);
    }

    @Test
    void everyFourthUpdateReappliesAutoFilterWhileOtherUpdatesRefreshVisibleRows() {
        JobList jobs = mock(JobList.class);
        AppContext.jobs = jobs;
        TableModelJobs model = mock(TableModelJobs.class);
        JTableJobs table = new JTableJobs(model) {
            private int pointCalls;

            @Override
            public int rowAtPoint(Point point) {
                return pointCalls++ % 2 == 0 ? 1 : 3;
            }

            @Override
            public int getRowCount() {
                return 4;
            }
        };
        JobsPanel panel = new JobsPanel(table, model);
        checkbox(panel, "Auto Update").setSelected(true);

        panel.update();
        panel.update();
        panel.update();
        panel.update();

        verify(model, times(3)).fireTableRowsUpdated(1, 3);
        verify(jobs).filter(0, 0, false);
        verify(model).fireTableDataChanged();
    }

    @Test
    void malformedAndAllHiddenColumnConfigurationsFallBackToDefaultColumns() {
        AppContext.jobs = new JobList();
        JobsPanel malformedPanel = createRealPanel();
        Options malformed = new Options();
        malformed.setString(Options.STR_JOB_COLUMNS, "invalid;still-invalid;");

        malformedPanel.loadOptions(malformed);
        assertDefaultColumns(malformedPanel);

        JobsPanel hiddenPanel = createRealPanel();
        Options hidden = new Options();
        hidden.setString(Options.STR_JOB_COLUMNS, "999,80,1;14,111,0;14,222,1;0,95,0;");
        hiddenPanel.loadOptions(hidden);

        assertDefaultColumns(hiddenPanel);
        assertEquals(111, column(hiddenPanel, JobColumn.STAT).getPreferredWidth());
        assertEquals(95, column(hiddenPanel, JobColumn.NUMB).getPreferredWidth());
    }

    @Test
    void scaleAwareDefaultsCanBeReappliedAndAllCurrentColumnsCanBeScaled() {
        AppContext.jobs = new JobList();
        JobsPanel panel = createRealPanel();
        panel.loadOptions(new Options());
        TableColumn titleColumn = column(panel, JobColumn.TITL);
        titleColumn.setPreferredWidth(20);

        panel.reapplyScaleAwareSizing();
        assertTrue(titleColumn.getPreferredWidth() >= 360);

        int originalWidth = titleColumn.getPreferredWidth();
        panel.scaleCurrentColumnWidths(1.5);
        assertTrue(titleColumn.getPreferredWidth() > originalWidth);
    }

    private static JobsPanel createRealPanel() {
        TableModelJobs model = new TableModelJobs(AppContext.jobs);
        JTableJobs table = new JTableJobs(model);
        TableModelJobs.formatTable(table);
        return new JobsPanel(table, model);
    }

    private static void assertDefaultColumns(JobsPanel panel) {
        JobsPanelTest.assertVisibleColumns(panel, JobColumn.TITL, JobColumn.STAT);
    }

    private static TableColumn column(JobsPanel panel, JobColumn column) {
        return panel.getJobsTable().getHeaderListener().getColumnByModelIndex(column.getIndex());
    }

    private static JCheckBox checkbox(Container root, String label) {
        return components(root, JCheckBox.class).stream()
                .filter(checkBox -> label.equals(checkBox.getText()))
                .findFirst()
                .orElseThrow();
    }

    private static <T extends Component> List<T> components(Container root, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component component : root.getComponents()) {
            if (type.isInstance(component)) {
                matches.add(type.cast(component));
            }
            if (component instanceof Container container) {
                matches.addAll(components(container, type));
            }
        }
        return matches;
    }
}
