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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.ui.actions.jobs.JobActionCommand;
import epox.webaom.ui.actions.jobs.JobActionController;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobContextMenuTest {
    private Cache originalCache;
    private epox.webaom.JobCounter originalJobCounter;
    private epox.webaom.JobList originalJobs;

    @BeforeEach
    void setUp() {
        originalCache = AppContext.cache;
        originalJobCounter = AppContext.jobCounter;
        originalJobs = AppContext.jobs;
        AppContext.cache = new Cache();
        AppContext.jobCounter = new epox.webaom.JobCounter();
        AppContext.jobs = new epox.webaom.JobList();
    }

    @AfterEach
    void tearDown() {
        AppContext.cache = originalCache;
        AppContext.jobCounter = originalJobCounter;
        AppContext.jobs = originalJobs;
        AppContext.secondaryPopupMenu = null;
    }

    @Test
    void alternateViewPanel_usesAltScopedContextMenuController() throws Exception {
        new AlternateViewPanel(event -> {});

        Component popupMenu = AppContext.secondaryPopupMenu;
        assertInstanceOf(JobContextMenu.class, popupMenu);

        Field controllerField = JobContextMenu.class.getDeclaredField("controller");
        controllerField.setAccessible(true);
        JobActionController controller = (JobActionController) controllerField.get(popupMenu);

        Field deleteScopeField = JobActionController.class.getDeclaredField("deleteScope");
        deleteScopeField.setAccessible(true);
        JobDeleteScope deleteScope = (JobDeleteScope) deleteScopeField.get(controller);

        assertEquals(JobDeleteScope.ALT, deleteScope);
    }

    @Test
    void jobsContextMenu_usesJobsRemoveLabelAndAniDbLabel() {
        JobContextMenu menu = new JobContextMenu(new JTable(), row -> new Job[0], JobDeleteScope.JOBS);

        List<String> labels = getMenuLabels(menu);

        assertTrue(labels.contains("Remove from Jobs"));
        assertTrue(labels.contains("Remove from AniDB MyList"));
        assertTrue(labels.stream().noneMatch("Remove from DB"::equals));
    }

    @Test
    void altContextMenu_usesAltRemoveLabelAndAniDbLabel() {
        JobContextMenu menu = new JobContextMenu(new JTable(), row -> new Job[0], JobDeleteScope.ALT);

        List<String> labels = getMenuLabels(menu);

        assertTrue(labels.contains("Remove from Alt"));
        assertTrue(labels.contains("Remove from AniDB MyList"));
        assertTrue(labels.stream().noneMatch("Remove from DB"::equals));
    }

    @Test
    void localRemoveCommand_routesThroughScopedDeleteFlow() {
        TrackingController controller = new TrackingController(JobDeleteScope.ALT);
        JTable table = new JTable(new DefaultTableModel(new Object[][] {{"row"}}, new Object[] {"col"}));
        table.setRowSelectionInterval(0, 0);
        Job job = controller.jobs.getFirst();
        JobContextMenu menu = new JobContextMenu(table, row -> new Job[] {job}, controller);

        menu.executeSelectedRowsCommand(JobActionCommand.REMOVE_LOCAL, new int[] {0}, null);

        assertEquals(Set.of(job), controller.deletedJobs);
        assertEquals(1, controller.deleteCalls);
        assertEquals(0, controller.executeCommandCalls);
    }

    @Test
    void removeFromAniDbMyListCommand_routesThroughCommandExecution() {
        TrackingController controller = new TrackingController(JobDeleteScope.JOBS);
        JTable table = new JTable(new DefaultTableModel(new Object[][] {{"row"}}, new Object[] {"col"}));
        table.setRowSelectionInterval(0, 0);
        Job job = controller.jobs.getFirst();
        JobContextMenu menu = new JobContextMenu(table, row -> new Job[] {job}, controller);

        menu.executeSelectedRowsCommand(JobActionCommand.REMOVE_FROM_MYLIST, new int[] {0}, null);

        assertEquals(0, controller.deleteCalls);
        assertEquals(1, controller.executeCommandCalls);
        assertEquals(JobActionCommand.REMOVE_FROM_MYLIST, controller.lastCommand);
    }

    @Test
    void singlePopupAction_usesModelRowWhenTableIsSorted() throws Exception {
        TrackingController controller = new TrackingController(JobDeleteScope.JOBS);
        controller.jobs.add(new Job(new java.io.File("episode-2.mkv"), Job.HASHWAIT));
        JTable table = new JTable(new DefaultTableModel(new Object[][] {{"a"}, {"b"}}, new Object[] {"col"}));
        table.setRowSorter(new TableRowSorter<>(table.getModel()));
        table.getRowSorter().toggleSortOrder(0);
        table.getRowSorter().toggleSortOrder(0);
        table.setRowSelectionInterval(0, 0);
        Job topViewJob = controller.jobs.get(1);
        JobContextMenu menu = new JobContextMenu(table, row -> new Job[] {controller.jobs.get(row)}, controller);

        menu.actionPerformed(
                new ActionEvent(menu, ActionEvent.ACTION_PERFORMED, String.valueOf(JobActionCommand.SHOW_INFO.id())));
        waitForWorker(menu);

        assertEquals(1, controller.executeSingleCommandCalls);
        assertEquals(JobActionCommand.SHOW_INFO, controller.lastSingleCommand);
        assertEquals(topViewJob, controller.lastSingleJob);
    }

    private void waitForWorker(JobContextMenu menu) throws Exception {
        Field workerField = JobContextMenu.class.getDeclaredField("worker");
        workerField.setAccessible(true);
        for (int i = 0; i < 100; i++) {
            Object worker = workerField.get(menu);
            if (worker == null) {
                return;
            }
            ((Thread) worker).join(10);
        }
        assertNull(workerField.get(menu));
    }

    private List<String> getMenuLabels(JobContextMenu menu) {
        List<String> labels = new ArrayList<>();
        for (Component component : menu.getComponents()) {
            if (component instanceof JMenuItem menuItem) {
                labels.add(menuItem.getText());
            }
        }
        return labels;
    }

    private static final class TrackingController extends JobActionController {
        final List<Job> jobs = new ArrayList<>();
        int deleteCalls;
        int executeCommandCalls;
        int executeSingleCommandCalls;
        Set<Job> deletedJobs = Set.of();
        JobActionCommand lastCommand;
        JobActionCommand lastSingleCommand;
        Job lastSingleJob;

        TrackingController(JobDeleteScope deleteScope) {
            super(new NoOpGateway(), deleteScope);
            jobs.add(new Job(new java.io.File("episode.mkv"), Job.HASHWAIT));
        }

        @Override
        public DeletionResult deleteSelectedJobs(java.util.Collection<Job> selectedJobs) {
            deleteCalls++;
            deletedJobs = new LinkedHashSet<>(selectedJobs);
            return new DeletionResult(true, deletedJobs.size());
        }

        @Override
        public void executeCommand(JobActionCommand command, Job job, String folderPath) {
            executeCommandCalls++;
            lastCommand = command;
        }

        @Override
        public void executeSingleCommand(JobActionCommand command, Job job, String editedName, String fidInput) {
            executeSingleCommandCalls++;
            lastSingleCommand = command;
            lastSingleJob = job;
        }
    }

    private static final class NoOpGateway implements epox.webaom.ui.actions.jobs.JobActionGateway {
        @Override
        public void updateStatus(Job job, int status) {}

        @Override
        public void updateStatus(Job job, int status, boolean checkIfBusy) {}

        @Override
        public boolean applyRulesForced(Job job) {
            return true;
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
            return jobs.size();
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
            return true;
        }
    }
}
