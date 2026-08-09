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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import epox.swing.FileChooserBuilder;
import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.ui.actions.jobs.JobActionCommand;
import epox.webaom.ui.actions.jobs.JobActionController;
import epox.webaom.ui.actions.jobs.JobActionGateway;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.MockedStatic;

@Tag("ui")
class JobContextMenuAdditionalCharacterizationTest {
    private JobCounter originalCounter;
    private AppContext.AppMode originalMode;
    private String originalLastDirectory;
    private Component originalComponent;

    @BeforeEach
    void isolateGlobals() {
        originalCounter = AppContext.jobCounter;
        originalMode = AppContext.getAppMode();
        originalLastDirectory = AppContext.lastDirectory;
        originalComponent = AppContext.component;
        AppContext.jobCounter = new JobCounter();
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
        AppContext.component = null;
    }

    @AfterEach
    void restoreGlobals() {
        AppContext.jobCounter = originalCounter;
        AppContext.setAppMode(originalMode);
        AppContext.lastDirectory = originalLastDirectory;
        AppContext.component = originalComponent;
    }

    @Test
    void commandMetadataPreservesLabelsSeparatorsAndSingleSelectionFlags() {
        assertEquals("Pause", JobContextMenu.commandText(JobActionCommand.PAUSE.id()));
        // TODO: Replace the legacy fallback text when invalid context-menu command IDs are reviewed.
        assertEquals("fook", JobContextMenu.commandText(-1));
        assertTrue(JobContextMenu.separator(JobActionCommand.SEPARATOR_0.id()));
        assertFalse(JobContextMenu.separator(-1));
        assertTrue(JobContextMenu.single(JobActionCommand.SHOW_INFO.id()));
        assertFalse(JobContextMenu.single(JobActionCommand.PAUSE.id()));
    }

    @Test
    void mouseLifecycleTogglesJobTableUpdatesAndShowsOnlyRightClickPopup() {
        JTableJobs jobsTable = mock(JTableJobs.class);
        jobsTable.updateEnabled = true;
        TrackingController controller = new TrackingController();
        TrackingMenu menu = new TrackingMenu(jobsTable, row -> null, controller);
        MouseEvent event = click(jobsTable, MouseEvent.BUTTON1);

        menu.mouseEntered(event);
        menu.mouseExited(event);
        menu.mousePressed(event);
        assertFalse(jobsTable.updateEnabled);
        menu.mouseReleased(event);
        assertTrue(jobsTable.updateEnabled);

        menu.mouseClicked(event);
        assertFalse(menu.popupShown);
        menu.mouseClicked(click(jobsTable, MouseEvent.BUTTON3));
        assertTrue(menu.popupShown);
    }

    @Test
    void guardedCommandsSkipEmptyInputsAndDeduplicateSelectedJobs() {
        Job first = job("first.mkv");
        Job second = job("second.mkv");
        TrackingController controller = new TrackingController();
        JTable table = table(2);
        RowModel rows = row -> row == 0 ? null : new Job[] {first, null, first, second};
        JobContextMenu menu = new JobContextMenu(table, rows, controller);

        menu.command(JobActionCommand.PAUSE, new Job[0], null);
        menu.command(JobActionCommand.PAUSE, new Job[] {null, first}, null);
        assertEquals(List.of(first), controller.commandJobs);

        menu.executeSelectedRowsCommand(null, new int[] {0}, null);
        menu.executeSelectedRowsCommand(JobActionCommand.PAUSE, new int[0], null);
        menu.executeSelectedRowsCommand(JobActionCommand.PAUSE, new int[] {0}, null);
        assertEquals(List.of(first), controller.commandJobs);

        menu.executeSelectedRowsCommand(JobActionCommand.REMOVE_LOCAL, new int[] {0, 1}, null);
        assertEquals(Set.of(first, second), controller.deletedJobs);
    }

    @Test
    void conversionFailureFallsBackToTheOriginalViewRow() {
        Job job = job("fallback.mkv");
        TrackingController controller = new TrackingController();
        JTable table = new JTable(new DefaultTableModel(2, 1)) {
            @Override
            public int convertRowIndexToModel(int viewRowIndex) {
                throw new IllegalStateException("sorter changed");
            }
        };
        List<Integer> requestedRows = new ArrayList<>();
        JobContextMenu menu = new JobContextMenu(
                table,
                row -> {
                    requestedRows.add(row);
                    return new Job[] {job};
                },
                controller);

        menu.executeSelectedRowsCommand(JobActionCommand.PAUSE, new int[] {1}, null);

        assertEquals(List.of(1), requestedRows);
        assertEquals(List.of(job), controller.commandJobs);
    }

    @Test
    void editCommandsUseDialogResultsWithoutOpeningDialogs() {
        File source = new File("source", "episode.mkv").getAbsoluteFile();
        Job job = new Job(source, Job.HASHWAIT);
        TrackingController controller = new TrackingController();
        JobContextMenu menu = new JobContextMenu(table(1), row -> new Job[] {job}, controller);
        try (MockedStatic<DialogHelper> dialogs = mockStatic(DialogHelper.class)) {
            dialogs.when(() -> DialogHelper.showInputDialog(null, "Edit path", source.getParent()))
                    .thenReturn(null, "x", "/library");

            menu.command(JobActionCommand.EDIT_PATH, new Job[] {job}, "ignored");
            menu.command(JobActionCommand.EDIT_PATH, new Job[] {job}, "ignored");
            assertTrue(controller.commandJobs.isEmpty());
            menu.command(JobActionCommand.EDIT_PATH, new Job[] {job}, "ignored");
            assertEquals("/library", controller.folderPaths.getFirst());

            dialogs.when(() -> DialogHelper.showInputDialog(null, "Edit name", "episode.mkv"))
                    .thenReturn("renamed.mkv");
            dialogs.when(() -> DialogHelper.showInputDialog(null, "Insert fid", ""))
                    .thenReturn("42");
            menu.commandSingle(JobActionCommand.EDIT_NAME, job);
            assertEquals("renamed.mkv", controller.editedName);
            menu.commandSingle(JobActionCommand.SET_FID, job);
            assertEquals("42", controller.fidInput);
            menu.commandSingle(JobActionCommand.SHOW_INFO, job);
            assertEquals(JobActionCommand.SHOW_INFO, controller.lastSingleCommand);
        }
    }

    @Test
    void folderSelectionTracksCurrentAndApprovedDirectoriesWithoutOpeningChooser() {
        TrackingController controller = new TrackingController();
        JobContextMenu menu = new JobContextMenu(table(1), row -> null, controller);
        FileChooserBuilder builder = mock(FileChooserBuilder.class);
        FileChooserBuilder.FileChooserResult approved = mock(FileChooserBuilder.FileChooserResult.class);
        FileChooserBuilder.FileChooserResult cancelled = mock(FileChooserBuilder.FileChooserResult.class);
        File selected = new File("/chosen/library");
        when(builder.forDirectories()).thenReturn(builder);
        when(builder.multiSelection(false)).thenReturn(builder);
        when(builder.withStateStoreId("job-folder")).thenReturn(builder);
        when(builder.showDialog(null, "Select Directory")).thenReturn(approved, cancelled);
        when(approved.getCurrentDirectory()).thenReturn("/current");
        when(approved.isApproved()).thenReturn(true);
        when(approved.getSelectedFile()).thenReturn(selected);
        when(cancelled.getCurrentDirectory()).thenReturn("/cancelled-current");
        AppContext.lastDirectory = "/start";
        try (MockedStatic<FileChooserBuilder> builders = mockStatic(FileChooserBuilder.class)) {
            builders.when(() -> FileChooserBuilder.createWithLastDirectory("/start"))
                    .thenReturn(builder);

            assertEquals(selected.getAbsolutePath(), menu.getFolder());
            assertEquals(selected.getAbsolutePath(), AppContext.lastDirectory);

            AppContext.lastDirectory = "/start";
            assertNull(menu.getFolder());
            assertEquals("/cancelled-current", AppContext.lastDirectory);
        }
    }

    @Test
    @Timeout(5)
    void menuWorkersDispatchCommandsAndClearSelection() throws Exception {
        Job job = job("worker.mkv");
        TrackingController controller = new TrackingController();
        JTable table = table(1);
        table.setRowSelectionInterval(0, 0);
        JobContextMenu menu = new JobContextMenu(table, row -> new Job[] {job}, controller);

        menu.actionPerformed(action(menu, JobActionCommand.PAUSE));
        waitForWorker(menu);

        assertEquals(List.of(job), controller.commandJobs);
        assertEquals(0, table.getSelectedRowCount());
        assertNull(menu.currentWorkerThread());

        table.setRowSelectionInterval(0, 0);
        JobContextMenu missingSelection = new JobContextMenu(table, row -> null, controller);
        missingSelection.actionPerformed(action(missingSelection, JobActionCommand.SHOW_INFO));
        waitForWorker(missingSelection);
        assertEquals(0, controller.singleCommandCalls);
    }

    @Test
    @Timeout(5)
    void stopDuringFolderSelectionPreventsThePendingCommand() throws Exception {
        Job job = job("stopped.mkv");
        TrackingController controller = new TrackingController();
        JTable table = table(1);
        table.setRowSelectionInterval(0, 0);
        BlockingFolderMenu menu = new BlockingFolderMenu(table, row -> new Job[] {job}, controller);
        menu.stop();

        menu.actionPerformed(action(menu, JobActionCommand.SET_FOLDER));
        assertTrue(menu.folderEntered.await(2, TimeUnit.SECONDS));
        menu.stop();
        menu.releaseFolder.countDown();
        waitForWorker(menu);

        assertTrue(controller.commandJobs.isEmpty());
        assertEquals(0, table.getSelectedRowCount());
    }

    private static void waitForWorker(JobContextMenu menu) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            Thread worker = menu.currentWorkerThread();
            if (worker == null) {
                return;
            }
            worker.join(Duration.ofMillis(20));
        }
        fail("Context-menu worker did not finish");
    }

    private static ActionEvent action(JobContextMenu menu, JobActionCommand command) {
        return new ActionEvent(menu, ActionEvent.ACTION_PERFORMED, Integer.toString(command.id()));
    }

    private static MouseEvent click(Component source, int button) {
        return new MouseEvent(source, MouseEvent.MOUSE_CLICKED, 1L, 0, 5, 5, 5, 5, 1, false, button);
    }

    private static JTable table(int rows) {
        return new JTable(new DefaultTableModel(rows, 1));
    }

    private static Job job(String name) {
        return new Job(new File(name), Job.HASHWAIT);
    }

    private static class TrackingMenu extends JobContextMenu {
        boolean popupShown;

        TrackingMenu(JTable table, RowModel rowModel, JobActionController controller) {
            super(table, rowModel, controller);
        }

        @Override
        public void show(Component invoker, int x, int y) {
            popupShown = true;
        }
    }

    private static final class BlockingFolderMenu extends JobContextMenu {
        final CountDownLatch folderEntered = new CountDownLatch(1);
        final CountDownLatch releaseFolder = new CountDownLatch(1);

        BlockingFolderMenu(JTable table, RowModel rowModel, JobActionController controller) {
            super(table, rowModel, controller);
        }

        @Override
        String getFolder() {
            folderEntered.countDown();
            try {
                if (!releaseFolder.await(2, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Folder selection was not released");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(exception);
            }
            return "/chosen";
        }
    }

    private static final class TrackingController extends JobActionController {
        final List<Job> commandJobs = new ArrayList<>();
        final List<String> folderPaths = new ArrayList<>();
        Set<Job> deletedJobs = Set.of();
        int singleCommandCalls;
        JobActionCommand lastSingleCommand;
        String editedName;
        String fidInput;

        TrackingController() {
            super(new NoOpGateway());
        }

        @Override
        public void executeCommand(JobActionCommand command, Job job, String folderPath) {
            commandJobs.add(job);
            folderPaths.add(folderPath);
        }

        @Override
        public void executeSingleCommand(JobActionCommand command, Job job, String editedName, String fidInput) {
            singleCommandCalls++;
            lastSingleCommand = command;
            this.editedName = editedName;
            this.fidInput = fidInput;
        }

        @Override
        public DeletionResult deleteSelectedJobs(Collection<Job> selectedJobs) {
            deletedJobs = new LinkedHashSet<>(selectedJobs);
            return new DeletionResult(true, deletedJobs.size());
        }
    }

    private static final class NoOpGateway implements JobActionGateway {
        @Override
        public void updateStatus(Job job, int status) {}

        @Override
        public void updateStatus(Job job, int status, boolean checkIfBusy) {}

        @Override
        public boolean applyRulesForced(Job job) {
            return false;
        }

        @Override
        public void setPath(Job job, String path, boolean includeParent) {}

        @Override
        public void restoreName(Job job) {}

        @Override
        public void showInfo(Job job) {}

        @Override
        public void openInDefaultPlayer(Job job) {}

        @Override
        public void openInExplorer(Job job) {}

        @Override
        public void setName(Job job, String newName) {}

        @Override
        public int deleteJobs(Set<Job> jobs, JobDeleteScope scope) {
            return 0;
        }

        @Override
        public int jobCount() {
            return 0;
        }

        @Override
        public void printLine(String message) {}

        @Override
        public void setStatusMessage(String message) {}

        @Override
        public void updateProgressBar() {}

        @Override
        public boolean confirm(String title, String message, String positive, String negative) {
            return false;
        }
    }
}
