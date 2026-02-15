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

package epox.webaom.ui.actions.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.data.AniDBFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobActionControllerTest {
    @TempDir
    Path tempDir;

    private JobCounter originalJobCounter;

    @BeforeEach
    void setUp() {
        originalJobCounter = AppContext.jobCounter;
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void tearDown() {
        AppContext.jobCounter = originalJobCounter;
    }

    @Test
    void executeCommand_applyRulesFailure_reportsErrorLine() throws IOException {
        FakeGateway gateway = new FakeGateway();
        gateway.applyRulesSuccess = false;
        JobActionController controller = new JobActionController(gateway);
        Job job = createJob("rules.mkv", Job.HASHWAIT);

        controller.executeCommand(JobActionCommand.APPLY_RULES, job, null);

        assertEquals(1, gateway.printedLines.size());
        assertTrue(gateway.printedLines.getFirst().contains("Failed to apply rules for"));
    }

    @Test
    void executeCommand_setParentFolder_usesIncludeParentFlag() throws IOException {
        FakeGateway gateway = new FakeGateway();
        JobActionController controller = new JobActionController(gateway);
        Job job = createJob("episode.mkv", Job.HASHWAIT);

        controller.executeCommand(JobActionCommand.SET_PAR_FLD, job, "/tmp/destination");

        assertEquals(1, gateway.pathCalls.size());
        PathCall pathCall = gateway.pathCalls.getFirst();
        assertEquals("/tmp/destination", pathCall.path());
        assertTrue(pathCall.includeParent());
    }

    @Test
    void executeSingleCommand_setFid_validInput_setsOverrideAndRequeues() throws IOException {
        FakeGateway gateway = new FakeGateway();
        JobActionController controller = new JobActionController(gateway);
        Job job = createJob("identify.mkv", Job.HASHWAIT);
        job.anidbFile = mock(AniDBFile.class);

        controller.executeSingleCommand(JobActionCommand.SET_FID, job, null, "42");

        assertEquals(42, job.fileIdOverride);
        assertNull(job.anidbFile);
        assertEquals(1, gateway.statusCalls.size());
        StatusCall statusCall = gateway.statusCalls.getFirst();
        assertEquals(Job.HASHED, statusCall.status());
        assertEquals(false, statusCall.checkIfBusy());
    }

    @Test
    void executeSingleCommand_setFid_invalidInput_doesNotRequeue() throws IOException {
        FakeGateway gateway = new FakeGateway();
        JobActionController controller = new JobActionController(gateway);
        Job job = createJob("identify.mkv", Job.HASHWAIT);

        controller.executeSingleCommand(JobActionCommand.SET_FID, job, null, "oops");

        assertEquals(-1, job.fileIdOverride);
        assertEquals(0, gateway.statusCalls.size());
    }

    @Test
    void deleteSelectedJobs_runningOnly_reportsAndDoesNotPrompt() throws IOException {
        FakeGateway gateway = new FakeGateway();
        JobActionController controller = new JobActionController(gateway);
        Job runningJob = createJob("running.mkv", Job.HASHING);

        JobActionController.DeletionResult result = controller.deleteSelectedJobs(List.of(runningJob));

        assertTrue(result.handled());
        assertEquals(0, result.removedCount());
        assertEquals(0, gateway.confirmCalls);
        assertEquals(1, gateway.printedLines.size());
        assertEquals("Cannot delete running job(s); stop them first.", gateway.printedLines.getFirst());
    }

    @Test
    void deleteSelectedJobs_cancelledConfirmation_stopsBeforeDelete() throws IOException {
        FakeGateway gateway = new FakeGateway();
        gateway.confirmResult = false;
        JobActionController controller = new JobActionController(gateway);
        Job job = createJob("one.mkv", Job.HASHWAIT);

        JobActionController.DeletionResult result = controller.deleteSelectedJobs(List.of(job));

        assertTrue(result.handled());
        assertEquals(0, result.removedCount());
        assertEquals(1, gateway.confirmCalls);
        assertEquals(0, gateway.deleteCalls);
    }

    @Test
    void deleteSelectedJobs_mixedSelection_deletesAndReportsSkipped() throws IOException {
        FakeGateway gateway = new FakeGateway();
        gateway.currentJobCount = 3;
        JobActionController controller = new JobActionController(gateway);
        Job deletableJob = createJob("done.mkv", Job.HASHWAIT);
        Job runningJob = createJob("busy.mkv", Job.HASHING);

        JobActionController.DeletionResult result =
                controller.deleteSelectedJobs(List.of(deletableJob, runningJob, runningJob));

        assertTrue(result.handled());
        assertEquals(1, result.removedCount());
        assertEquals(1, gateway.deleteCalls);
        assertEquals(Set.of(deletableJob), gateway.deletedJobs);
        assertEquals("Removed 1 job. 3 remaining.", gateway.statusMessage);
        assertEquals(1, gateway.progressUpdateCalls);
        assertEquals("Skipped 1 running job(s) during deletion.", gateway.printedLines.getFirst());
    }

    @Test
    void deleteSelectedJobs_emptySelection_notHandled() {
        FakeGateway gateway = new FakeGateway();
        JobActionController controller = new JobActionController(gateway);

        JobActionController.DeletionResult result = controller.deleteSelectedJobs(List.of());

        assertEquals(false, result.handled());
        assertEquals(0, gateway.confirmCalls);
        assertEquals(0, gateway.deleteCalls);
    }

    private Job createJob(String fileName, int initialStatus) throws IOException {
        Path file = Files.writeString(tempDir.resolve(fileName), "data");
        return new Job(file.toFile(), initialStatus);
    }

    private static final class FakeGateway implements JobActionGateway {
        final List<StatusCall> statusCalls = new ArrayList<>();
        final List<PathCall> pathCalls = new ArrayList<>();
        final List<String> printedLines = new ArrayList<>();
        boolean applyRulesSuccess = true;
        boolean confirmResult = true;
        int currentJobCount;
        int confirmCalls;
        int deleteCalls;
        int progressUpdateCalls;
        String statusMessage;
        Set<Job> deletedJobs = Set.of();

        @Override
        public void updateStatus(Job job, int status) {
            statusCalls.add(new StatusCall(job, status, false));
        }

        @Override
        public void updateStatus(Job job, int status, boolean checkIfBusy) {
            statusCalls.add(new StatusCall(job, status, checkIfBusy));
        }

        @Override
        public boolean applyRulesForced(Job job) {
            return applyRulesSuccess;
        }

        @Override
        public void setPath(Job job, String path, boolean includeParent) {
            pathCalls.add(new PathCall(job, path, includeParent));
        }

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
        public int deleteJobs(Set<Job> jobs) {
            deleteCalls++;
            deletedJobs = new LinkedHashSet<>(jobs);
            return jobs.size();
        }

        @Override
        public int jobCount() {
            return currentJobCount;
        }

        @Override
        public void printLine(String message) {
            printedLines.add(message);
        }

        @Override
        public void setStatusMessage(String message) {
            statusMessage = message;
        }

        @Override
        public void updateProgressBar() {
            progressUpdateCalls++;
        }

        @Override
        public boolean confirm(String title, String message, String positive, String negative) {
            confirmCalls++;
            return confirmResult;
        }
    }

    private record StatusCall(Job job, int status, boolean checkIfBusy) {}

    private record PathCall(Job job, String path, boolean includeParent) {}
}
