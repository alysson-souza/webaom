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

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.ui.actions.jobs.JobActionController;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KeyAdapterJobTest {
    private JobCounter originalJobCounter;
    private JobList originalJobs;

    @BeforeEach
    void setUp() {
        originalJobCounter = AppContext.jobCounter;
        originalJobs = AppContext.jobs;
        AppContext.jobCounter = new JobCounter();
        AppContext.jobs = new JobList();
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
    }

    @AfterEach
    void tearDown() {
        AppContext.jobCounter = originalJobCounter;
        AppContext.jobs = originalJobs;
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
    }

    @Test
    void deleteKey_isIgnoredWhenIncompatibleDatabaseIsLocked() {
        TrackingController controller = new TrackingController(JobDeleteScope.JOBS);
        JTable table = new JTable(new DefaultTableModel(new Object[][] {{"row"}}, new Object[] {"col"}));
        table.setRowSelectionInterval(0, 0);
        KeyAdapterJob keyAdapter = new KeyAdapterJob(table, row -> new Job[] {controller.jobs.getFirst()}, controller);
        KeyEvent deleteEvent =
                new KeyEvent(table, KeyEvent.KEY_PRESSED, System.currentTimeMillis(), 0, KeyEvent.VK_DELETE, '\0');
        AppContext.setAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);

        keyAdapter.keyPressed(deleteEvent);

        assertEquals(0, controller.deleteCalls);
    }

    private static final class TrackingController extends JobActionController {
        final List<Job> jobs = new ArrayList<>();
        int deleteCalls;
        Set<Job> deletedJobs = Set.of();

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
    }

    private static final class NoOpGateway implements epox.webaom.ui.actions.jobs.JobActionGateway {
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
            return true;
        }
    }
}
