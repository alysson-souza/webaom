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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import epox.swing.layout.TableColumnSizing;
import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.FileHandler;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.db.DatabaseManager;
import java.awt.Font;
import java.awt.event.ActionListener;
import javax.swing.Timer;
import javax.swing.UIManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class MainPanelScaleAwareSizingTest {
    private Options originalOptions;
    private FileHandler originalFileHandler;
    private Rules originalRules;
    private JobList originalJobs;
    private Cache originalCache;
    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private MainPanel originalGui;
    private String originalFont;

    @BeforeEach
    void setUp() {
        originalOptions = AppContext.opt;
        originalFileHandler = AppContext.fileHandler;
        originalRules = AppContext.rules;
        originalJobs = AppContext.jobs;
        originalCache = AppContext.cache;
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalGui = AppContext.gui;
        originalFont = AppContext.font;

        Options options = mock(Options.class);
        when(options.loadFromFile()).thenReturn(false);
        when(options.getString(org.mockito.ArgumentMatchers.anyInt())).thenReturn(null);
        when(options.getBoolean(org.mockito.ArgumentMatchers.anyInt())).thenReturn(false);

        AppContext.opt = options;
        AppContext.fileHandler = new FileHandler();
        AppContext.rules = new Rules();
        AppContext.jobs = new JobList();
        AppContext.cache = new Cache();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.font = "";
    }

    @AfterEach
    void tearDown() {
        AppContext.opt = originalOptions;
        AppContext.fileHandler = originalFileHandler;
        AppContext.rules = originalRules;
        AppContext.jobs = originalJobs;
        AppContext.cache = originalCache;
        AppContext.jobCounter = originalJobCounter;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.gui = originalGui;
        AppContext.font = originalFont;
    }

    @Test
    void setFont_reappliesScaleAwareWidthsForDefaultConfiguredTables() {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        AppContext.gui = panel;

        int jobsWidthBefore = panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.NUMB.getIndex())
                .getPreferredWidth();
        int alternateWidthBefore = panel.altViewPanel
                .getAltViewTreeTable()
                .getColumnModel()
                .getColumn(AlternateViewTableModel.NAME)
                .getPreferredWidth();
        int replacementWidthBefore = panel.rulesOptionsPanel
                .replacementsTable
                .getColumnModel()
                .getColumn(ReplacementTableModel.COLUMN_SOURCE)
                .getPreferredWidth();
        int rulesFontSizeBefore =
                panel.rulesOptionsPanel.rulesTextArea.getFont().getSize();

        AppContext.setFont("Dialog,72", true);

        int jobsWidthAfter = panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.NUMB.getIndex())
                .getPreferredWidth();
        int alternateWidthAfter = panel.altViewPanel
                .getAltViewTreeTable()
                .getColumnModel()
                .getColumn(AlternateViewTableModel.NAME)
                .getPreferredWidth();
        int replacementWidthAfter = panel.rulesOptionsPanel
                .replacementsTable
                .getColumnModel()
                .getColumn(ReplacementTableModel.COLUMN_SOURCE)
                .getPreferredWidth();
        int rulesFontSizeAfter = panel.rulesOptionsPanel.rulesTextArea.getFont().getSize();

        assertTrue(jobsWidthAfter > jobsWidthBefore);
        assertTrue(alternateWidthAfter > alternateWidthBefore);
        assertTrue(replacementWidthAfter > replacementWidthBefore);
        assertTrue(rulesFontSizeAfter > rulesFontSizeBefore);
    }

    @Test
    void setFont_runtimeScalingPreservesJobsTableCustomizationAndScalesCurrentWidths() {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        AppContext.gui = panel;

        panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(java.util.List.of(JobColumn.STAT.getIndex(), JobColumn.NUMB.getIndex()));
        int statusWidthBefore = panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.STAT.getIndex())
                .getPreferredWidth();
        int rulesFontSizeBefore =
                panel.rulesOptionsPanel.rulesTextArea.getFont().getSize();

        AppContext.setFont("Dialog,24");

        assertEquals(2, panel.jobsPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.STAT.getIndex(),
                panel.jobsPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.NUMB.getIndex(),
                panel.jobsPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        assertTrue(panel.jobsPanel
                        .getJobsTable()
                        .getHeaderListener()
                        .getColumnByModelIndex(JobColumn.STAT.getIndex())
                        .getPreferredWidth()
                > statusWidthBefore);
        assertTrue(panel.rulesOptionsPanel.rulesTextArea.getFont().getSize() > rulesFontSizeBefore);
    }

    @Test
    void setFont_runtimeScalingUsesFontMetricsWhenFamilyChangesAtSameSize() {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        AppContext.gui = panel;

        AppContext.setFont("Dialog,24", true);
        Font currentUiFont = UIManager.getFont("Label.font");
        Font updatedFont = new Font(Font.MONOSPACED, Font.PLAIN, 24);
        double expectedScale = TableColumnSizing.calculateFontScaleFactor(currentUiFont, updatedFont);
        panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .setVisibleColumns(java.util.List.of(JobColumn.FILE.getIndex(), JobColumn.STAT.getIndex()));

        int fileWidthBefore = panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.FILE.getIndex())
                .getPreferredWidth();

        AppContext.setFont("Monospaced,24");

        int fileWidthAfter = panel.jobsPanel
                .getJobsTable()
                .getHeaderListener()
                .getColumnByModelIndex(JobColumn.FILE.getIndex())
                .getPreferredWidth();

        assertEquals(2, panel.jobsPanel.getJobsTable().getColumnModel().getColumnCount());
        assertEquals(
                JobColumn.FILE.getIndex(),
                panel.jobsPanel.getJobsTable().getColumnModel().getColumn(0).getModelIndex());
        assertEquals(
                JobColumn.STAT.getIndex(),
                panel.jobsPanel.getJobsTable().getColumnModel().getColumn(1).getModelIndex());
        int expectedWidth = Math.max(1, (int) Math.round(fileWidthBefore * expectedScale));
        assertTrue(Math.abs(expectedWidth - fileWidthAfter) <= 4);
    }

    private void stopTimers(MainPanel panel) {
        panel.diskIoTimer.stop();
        panel.progressTimer.stop();
        panel.unfreezeTimer.stop();
        panel.guiUpdateTimer.stop();
    }

    private static final class TestRuntime implements MainPanelRuntime {
        @Override
        public Timer createTimer(int delayMs, ActionListener listener) {
            return new Timer(delayMs, listener);
        }

        @Override
        public Thread startBackgroundTask(String threadName, Runnable runnable) {
            return new Thread(runnable, threadName);
        }

        @Override
        public boolean requestCredentials() {
            return false;
        }

        @Override
        public void openHyperlink(String browserPath, String url) {}
    }
}
