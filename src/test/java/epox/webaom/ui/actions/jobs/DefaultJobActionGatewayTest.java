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

package epox.webaom.ui.actions.jobs;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.JobManager;
import epox.webaom.ui.MainPanel;
import java.awt.Component;
import java.io.File;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class DefaultJobActionGatewayTest {
    private JobList originalJobs;
    private JobCounter originalCounter;
    private MainPanel originalGui;
    private Component originalComponent;

    @BeforeEach
    void isolateGlobals() {
        originalJobs = AppContext.jobs;
        originalCounter = AppContext.jobCounter;
        originalGui = AppContext.gui;
        originalComponent = AppContext.component;
        AppContext.jobCounter = new JobCounter();
        AppContext.gui = null;
        AppContext.component = null;
    }

    @AfterEach
    void restoreGlobals() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalCounter;
        AppContext.gui = originalGui;
        AppContext.component = originalComponent;
    }

    @Test
    void delegatesJobOperationsToJobManagerWithoutLaunchingDesktopActions() {
        DefaultJobActionGateway gateway = new DefaultJobActionGateway();
        Job job = new Job(new File("missing.mkv"), Job.HASHWAIT);
        Set<Job> jobs = Set.of(job);
        try (MockedStatic<JobManager> manager = mockStatic(JobManager.class)) {
            manager.when(() -> JobManager.applyRulesForced(job)).thenReturn(true);
            manager.when(() -> JobManager.deleteJobs(jobs, JobDeleteScope.ALT)).thenReturn(1);

            gateway.updateStatus(job, Job.FINISHED);
            gateway.updateStatus(job, Job.H_PAUSED, true);
            assertTrue(gateway.applyRulesForced(job));
            gateway.setPath(job, "/library", true);
            gateway.restoreName(job);
            gateway.showInfo(job);
            gateway.openInDefaultPlayer(job);
            gateway.openInExplorer(job);
            gateway.setName(job, "renamed.mkv");
            assertEquals(1, gateway.deleteJobs(jobs, JobDeleteScope.ALT));

            manager.verify(() -> JobManager.updateStatus(job, Job.FINISHED));
            manager.verify(() -> JobManager.updateStatus(job, Job.H_PAUSED, true));
            manager.verify(() -> JobManager.applyRulesForced(job));
            manager.verify(() -> JobManager.setPath(job, "/library", true));
            manager.verify(() -> JobManager.restoreName(job));
            manager.verify(() -> JobManager.showInfo(job));
            manager.verify(() -> JobManager.openInDefaultPlayer(job));
            manager.verify(() -> JobManager.openInExplorer(job));
            manager.verify(() -> JobManager.setName(job, "renamed.mkv"));
            manager.verify(() -> JobManager.deleteJobs(jobs, JobDeleteScope.ALT));
        }
    }

    @Test
    void reportsCurrentJobCountAndTreatsMissingListAsEmpty() {
        DefaultJobActionGateway gateway = new DefaultJobActionGateway();
        AppContext.jobs = null;
        assertEquals(0, gateway.jobCount());

        JobList jobs = mock(JobList.class);
        when(jobs.size()).thenReturn(7);
        AppContext.jobs = jobs;
        assertEquals(7, gateway.jobCount());
    }

    @Test
    void guiMessagesAreNoOpsWithoutGuiAndDelegateWhenGuiExists() {
        DefaultJobActionGateway gateway = new DefaultJobActionGateway();

        assertDoesNotThrow(() -> {
            gateway.printLine("line");
            gateway.setStatusMessage("status");
            gateway.updateProgressBar();
        });

        MainPanel gui = mock(MainPanel.class);
        AppContext.gui = gui;
        gateway.printLine("line");
        gateway.setStatusMessage("status");
        gateway.updateProgressBar();

        verify(gui).println("line");
        verify(gui).setStatusMessage("status");
        verify(gui).updateProgressBar();
    }

    @Test
    void confirmationReturnsFalseWhenNoUiSurfaceIsAvailable() {
        DefaultJobActionGateway gateway = new DefaultJobActionGateway();

        assertFalse(gateway.confirm("Remove", "Remove item?", "Yes", "No"));
    }
}
