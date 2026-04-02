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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.FileHandler;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.db.DatabaseManager;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Field;
import javax.swing.JButton;
import javax.swing.Timer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("ui")
class MainPanelIncompatibleDatabaseLockTest {
    private Options originalOptions;
    private FileHandler originalFileHandler;
    private Rules originalRules;
    private JobList originalJobs;
    private Cache originalCache;
    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private AppContext.AppMode originalAppMode;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() {
        originalOptions = AppContext.opt;
        originalFileHandler = AppContext.fileHandler;
        originalRules = AppContext.rules;
        originalJobs = AppContext.jobs;
        originalCache = AppContext.cache;
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalAppMode = AppContext.getAppMode();

        Options options = mock(Options.class);
        when(options.loadFromFile()).thenReturn(false);
        when(options.getString(org.mockito.ArgumentMatchers.anyInt())).thenReturn(null);

        databaseManager = mock(DatabaseManager.class);
        when(databaseManager.isConnected()).thenReturn(false);

        AppContext.opt = options;
        AppContext.fileHandler = new FileHandler();
        AppContext.rules = new Rules();
        AppContext.jobs = new JobList();
        AppContext.cache = new Cache();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = databaseManager;
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
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
        AppContext.setAppMode(originalAppMode);
    }

    @Test
    void incompatibleDatabaseLock_disablesToolbarAndJobTablesButKeepsDatabaseFieldEnabled() throws Exception {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);

        panel.applyAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);

        JButton[] toolbarButtons = getToolbarButtons(panel);
        for (JButton toolbarButton : toolbarButtons) {
            assertFalse(toolbarButton.isEnabled());
        }
        assertFalse(panel.jobsTable.isEnabled());
        assertFalse(panel.altViewPanel.getAltViewTreeTable().isEnabled());
        assertTrue(panel.miscOptionsPanel.databaseUrlField.isEnabled());
        assertTrue(AppContext.isInteractionBlocked());
        assertTrue(AppContext.getAppMode() == AppContext.AppMode.INCOMPATIBLE_DATABASE);
    }

    @Test
    void incompatibleDatabaseFailure_handoffLocksPanelAndReEnablesDatabaseField() {
        when(databaseManager.hasUnsupportedFutureSchema()).thenReturn(true);
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        panel.miscOptionsPanel.databaseUrlField.setEnabled(false);

        panel.handleDatabaseInitializationFailure(panel.miscOptionsPanel.databaseUrlField);

        assertTrue(AppContext.getAppMode() == AppContext.AppMode.INCOMPATIBLE_DATABASE);
        assertTrue(panel.miscOptionsPanel.databaseUrlField.isEnabled());
        assertFalse(panel.jobsTable.isEnabled());
        assertFalse(panel.altViewPanel.getAltViewTreeTable().isEnabled());
    }

    @Test
    void dragAndDrop_isRejectedWhenIncompatibleDatabaseIsLocked() {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        panel.applyAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);
        DropTargetDragEvent dragEvent = mock(DropTargetDragEvent.class);
        DropTargetDropEvent dropEvent = mock(DropTargetDropEvent.class);

        panel.dragEnter(dragEvent);
        panel.drop(dropEvent);

        verify(dragEvent).rejectDrag();
        verify(dropEvent).rejectDrop();
        verify(dropEvent, never()).acceptDrop(org.mockito.ArgumentMatchers.anyInt());
        verify(dropEvent, never()).dropComplete(true);
    }

    @Test
    void selectFilesForProcessing_doesNothingWhenInteractionIsBlocked() throws Exception {
        MainPanel panel = new MainPanel(new TestRuntime());
        stopTimers(panel);
        panel.applyAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);

        panel.selectFilesForProcessing(new java.io.File[] {new java.io.File("blocked.mkv")});

        assertTrue(AppContext.isInteractionBlocked());
        assertFalse(panel.jobsTable.isEnabled());
        assertTrue(panel.workerThread == null);
    }

    private void stopTimers(MainPanel panel) {
        panel.diskIoTimer.stop();
        panel.progressTimer.stop();
        panel.unfreezeTimer.stop();
        panel.guiUpdateTimer.stop();
    }

    private JButton[] getToolbarButtons(MainPanel panel) throws Exception {
        Field toolbarButtonsField = MainPanel.class.getDeclaredField("toolbarButtons");
        toolbarButtonsField.setAccessible(true);
        return (JButton[]) toolbarButtonsField.get(panel);
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
