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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.AniDBFile;
import epox.webaom.db.DatabaseManager;
import epox.webaom.ui.TableModelJobs;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobListCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private Cache originalCache;

    @BeforeEach
    void installIsolatedDependencies() {
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalCache = AppContext.cache;
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.cache = mock(Cache.class);
        when(AppContext.databaseManager.getJob(any(Job.class), anyBoolean())).thenReturn(-1);
    }

    @AfterEach
    void restoreDependencies() {
        AppContext.jobCounter = originalJobCounter;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.cache = originalCache;
    }

    @Test
    void addFileRejectsDuplicatesAndTracksVisibleJobs() throws Exception {
        JobList jobs = new JobList();
        Path path = Files.createFile(temporaryDirectory.resolve("duplicate.mkv"));

        Job added = jobs.add(path.toFile());

        assertSame(added, jobs.get(0));
        assertTrue(jobs.has(path.toFile()));
        assertEquals(1, jobs.size());
        assertNull(jobs.add(path.toFile()));
        assertTrue(jobs.toString().contains("HashSet: 1, ArrayList: 1"));
    }

    @Test
    void hiddenJobsRemainInTheBackingArrayButNotTheVisibleView() throws Exception {
        JobList jobs = new JobList();
        Job visible = new Job(
                Files.createFile(temporaryDirectory.resolve("visible.mkv")).toFile(), Job.HASHWAIT);
        Job hidden = new Job(
                Files.createFile(temporaryDirectory.resolve("hidden.mkv")).toFile(), Job.HASHWAIT);
        hidden.setJobsVisible(false);

        assertTrue(jobs.add(visible));
        assertTrue(jobs.add(hidden));

        assertEquals(1, jobs.size());
        assertSame(visible, jobs.get(0));
        assertArrayEquals(new Job[] {visible, hidden}, jobs.array());
        assertFalse(jobs.has(hidden.getFile()));
    }

    @Test
    void filteringCanSelectOnlyUnknownFinishedJobsWhenDatabaseLoadsAll() throws Exception {
        when(AppContext.databaseManager.isLoadAllJobs()).thenReturn(true);
        JobList jobs = new JobList();
        Job unknown = new Job(
                Files.createFile(temporaryDirectory.resolve("unknown.mkv")).toFile(), Job.FINISHED);
        unknown.setStatus(Job.FINISHED, false);
        Job known = new Job(
                Files.createFile(temporaryDirectory.resolve("known.mkv")).toFile(), Job.FINISHED);
        known.setStatus(Job.FINISHED, false);
        known.anidbFile = new AniDBFile(10);
        jobs.add(unknown);
        jobs.add(known);

        jobs.filter(Job.S_DONE | Job.H_NORMAL, 0, true);

        assertEquals(1, jobs.size());
        assertSame(unknown, jobs.get(0));

        jobs.filter(0, 0, false);
        assertEquals(2, jobs.size());
    }

    @Test
    void diskQueueRespectsInsertionOrderLimitsVisibilityStatusAndExclusions() throws Exception {
        JobList jobs = new JobList();
        Job first = normalJob("first.mkv", Job.HASHWAIT);
        Job second = normalJob("second.mkv", Job.HASHWAIT);
        Job hidden = normalJob("hidden-queue.mkv", Job.HASHWAIT);
        hidden.setJobsVisible(false);
        jobs.updateQueues(first, -1, Job.HASHWAIT);
        jobs.updateQueues(second, -1, Job.HASHWAIT);
        jobs.updateQueues(hidden, -1, Job.HASHWAIT);

        assertSame(first, jobs.getJobDio());
        assertEquals(List.of(first), jobs.getJobsDio(1, Job.HASHWAIT, Set.of()));
        assertEquals(List.of(second), jobs.getJobsDio(2, Job.HASHWAIT, Set.of(first)));
        assertTrue(jobs.workForDio());

        jobs.updateQueues(first, Job.HASHWAIT, Job.MOVEWAIT);
        assertSame(first, jobs.getJobDio());
        assertEquals(List.of(first, second), jobs.getJobsDio(10, Job.HASHWAIT, Set.of()));
    }

    @Test
    void networkQueueAndClearExposeWorkAvailability() throws Exception {
        JobList jobs = new JobList();
        Job network = normalJob("network.mkv", Job.IDENTWAIT);
        jobs.updateQueues(network, -1, Job.IDENTWAIT);

        assertSame(network, jobs.getJobNio());
        assertTrue(jobs.workForNio());

        jobs.clear();
        assertFalse(jobs.workForNio());
        assertFalse(jobs.workForDio());
        assertEquals(0, jobs.size());
    }

    @Test
    void refreshViewNotifiesAnAttachedTableModel() {
        JobList jobs = new JobList();
        TableModelJobs tableModel = mock(TableModelJobs.class);
        jobs.tableModel = tableModel;

        jobs.refreshView();

        verify(tableModel).fireTableDataChanged();
    }

    @Test
    void removeJobsHandlesNullsDuplicatesAndJobsNotInTheList() throws Exception {
        JobList jobs = new JobList();
        Job first = normalJob("remove-first.mkv", Job.HASHWAIT);
        Job second = normalJob("remove-second.mkv", Job.HASHWAIT);
        Job outsider = normalJob("outsider.mkv", Job.HASHWAIT);
        jobs.add(first);
        jobs.add(second);
        jobs.updateQueues(first, -1, Job.HASHWAIT);
        jobs.updateQueues(second, -1, Job.HASHWAIT);

        assertEquals(0, jobs.removeJobs(null));
        assertEquals(0, jobs.removeJobs(List.of()));
        assertEquals(0, jobs.removeJobs(java.util.Arrays.asList(null, outsider)));
        assertEquals(1, jobs.removeJobs(java.util.Arrays.asList(first, first, null)));
        assertFalse(jobs.has(first.getFile()));
        assertSame(second, jobs.getJobDio());
        assertNull(jobs.get(5));
    }

    @Test
    void remLeavesTheRemovedJobInItsWorkQueue() throws Exception {
        JobList jobs = new JobList();
        Job job = normalJob("legacy-rem.mkv", Job.HASHWAIT);
        jobs.add(job);
        jobs.updateQueues(job, -1, Job.HASHWAIT);

        assertSame(job, jobs.rem(0));

        // TODO: Review whether the legacy rem API should also remove stale queue entries.
        assertSame(job, jobs.getJobDio());
        assertFalse(jobs.has(job.getFile()));
    }

    private Job normalJob(String name, int status) throws Exception {
        Job job = new Job(Files.createFile(temporaryDirectory.resolve(name)).toFile(), status);
        job.setHealth0(Job.H_NORMAL);
        return job;
    }
}
