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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.AniDBFile;
import epox.webaom.db.DatabaseManager;
import epox.webaom.ui.MainPanel;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobManagerCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private MainPanel originalGui;
    private Options originalOptions;
    private Rules originalRules;
    private Cache originalCache;
    private boolean originalAutoadd;
    private int originalTreeSortMode;

    @BeforeEach
    void installIsolatedDependencies() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalGui = AppContext.gui;
        originalOptions = AppContext.opt;
        originalRules = AppContext.rules;
        originalCache = AppContext.cache;
        originalAutoadd = AppContext.autoadd;
        originalTreeSortMode = Cache.getTreeSortMode();

        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.gui = mock(MainPanel.class);
        AppContext.opt = new Options(temporaryDirectory.resolve("config"));
        AppContext.rules = mock(Rules.class);
        AppContext.cache = mock(Cache.class);
        AppContext.autoadd = false;
        Cache.setTreeSortMode(Cache.MODE_ANIME_EPISODE_FILE);
        when(AppContext.databaseManager.update(
                        org.mockito.ArgumentMatchers.anyInt(),
                        org.mockito.ArgumentMatchers.any(Job.class),
                        org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(true);
    }

    @AfterEach
    void restoreDependencies() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.gui = originalGui;
        AppContext.opt = originalOptions;
        AppContext.rules = originalRules;
        AppContext.cache = originalCache;
        AppContext.autoadd = originalAutoadd;
        Cache.setTreeSortMode(originalTreeSortMode);
        JobManager.resetBatchChoice();
    }

    @Test
    void hashedRequiresAHashThenRoutesUnknownAndKnownFilesDifferently() throws Exception {
        Job job = normalJob("hashed.mkv", Job.HASHING);

        JobManager.updateStatus(job, Job.HASHED);
        assertEquals(Job.HASHING, job.getStatus());

        job.ed2kHash = "hash";
        JobManager.updateStatus(job, Job.HASHED);
        assertEquals(Job.IDENTWAIT, job.getStatus());
        assertFalse(job.isFresh);

        Job known = normalJob("known-hash.mkv", Job.HASHING);
        known.ed2kHash = "hash";
        known.anidbFile = new AniDBFile(10);
        JobManager.updateStatus(known, Job.HASHED);
        assertEquals(Job.FINISHED, known.getStatus());
    }

    @Test
    void identifiedRoutesInvalidAutoaddMoveAndFinishedCases() throws Exception {
        AppContext.opt.setBoolean(Options.BOOL_AUTO_RENAME, false);
        Job invalid = normalJob("invalid-id.mkv", Job.IDENTIFYING);
        JobManager.updateStatus(invalid, Job.IDENTIFIED);
        assertEquals(Job.FAILED, invalid.getStatus());

        AppContext.autoadd = true;
        Job autoadd = identifiedJob("autoadd.mkv");
        JobManager.updateStatus(autoadd, Job.IDENTIFIED);
        assertEquals(Job.ADDWAIT, autoadd.getStatus());

        AppContext.autoadd = false;
        Job moving = identifiedJob("moving.mkv");
        moving.targetFile = temporaryDirectory.resolve("target.mkv").toFile();
        JobManager.updateStatus(moving, Job.IDENTIFIED);
        assertEquals(Job.MOVEWAIT, moving.getStatus());

        Job finished = identifiedJob("finished.mkv");
        JobManager.updateStatus(finished, Job.IDENTIFIED);
        assertEquals(Job.FINISHED, finished.getStatus());
    }

    @Test
    void addedMovedFailedAndUnknownAddTransitionsPreserveCurrentRules() throws Exception {
        Job invalidAdded = normalJob("invalid-added.mkv", Job.ADDING);
        JobManager.updateStatus(invalidAdded, Job.ADDED);
        assertEquals(Job.ADDING, invalidAdded.getStatus());

        Job added = identifiedJob("added.mkv");
        added.setStatus(Job.ADDING, false);
        added.targetFile = temporaryDirectory.resolve("added-target.mkv").toFile();
        JobManager.updateStatus(added, Job.ADDED);
        assertEquals(Job.MOVEWAIT, added.getStatus());

        added.targetFile = null;
        added.setStatus(Job.MOVED, false);
        JobManager.updateStatus(added, Job.MOVED);
        assertEquals(Job.FINISHED, added.getStatus());
        assertNull(added.targetFile);

        Job failed = normalJob("failed.mkv", Job.HASHWAIT);
        JobManager.updateStatus(failed, Job.FAILED);
        assertEquals(Job.FAILED, failed.getStatus());

        Job unknownAdd = normalJob("unknown-add.mkv", Job.IDENTWAIT);
        JobManager.updateStatus(unknownAdd, Job.ADDWAIT);
        assertEquals(Job.FAILED, unknownAdd.getStatus());
        assertEquals("Can't add unknown file to mylist.", unknownAdd.errorMessage);
    }

    @Test
    void busyAndMissingJobsRejectInapplicableStatusChanges() throws Exception {
        Job busy = normalJob("busy.mkv", Job.HASHING);
        JobManager.updateStatus(busy, Job.FAILED, true);
        assertEquals(Job.HASHING, busy.getStatus());

        Job missing = new Job(temporaryDirectory.resolve("missing.mkv").toFile(), Job.HASHWAIT);
        JobManager.updateStatus(missing, Job.HASHED);
        assertEquals(Job.HASHWAIT, missing.getStatus());
        JobManager.updateStatus(missing, Job.FINISHED);
        assertEquals(Job.FINISHED, missing.getStatus());
        assertEquals(Job.H_MISSING, missing.getHealth());
    }

    @Test
    void setPathFindsAMissingFileIncludingItsParentFolderName() throws Exception {
        Path originalParent = Files.createDirectory(temporaryDirectory.resolve("original-parent"));
        Job missing = new Job(originalParent.resolve("episode.mkv").toFile(), Job.HASHWAIT);
        Path destination = Files.createDirectory(temporaryDirectory.resolve("destination"));
        Path nested = Files.createDirectories(destination.resolve("original-parent"));
        Path found = Files.createFile(nested.resolve("episode.mkv"));

        JobManager.setPath(missing, destination.toString(), true);

        assertEquals(found.toFile(), missing.getFile());
        assertEquals(Job.H_PAUSED, missing.getHealth());
        verify(AppContext.databaseManager).update(0, missing, DatabaseManager.INDEX_JOB);
    }

    @Test
    void setNameAndRestoreNameRenameOnlyFinishedFiles() throws Exception {
        Path original = Files.writeString(temporaryDirectory.resolve("original.mkv"), "video");
        Job job = new Job(original.toFile(), Job.FINISHED);
        job.setStatus(Job.FINISHED, false);

        JobManager.setName(job, "renamed.mkv");
        assertEquals("renamed.mkv", job.getFile().getName());
        assertTrue(job.getFile().exists());

        JobManager.restoreName(job);
        assertEquals("original.mkv", job.getFile().getName());
        assertTrue(original.toFile().exists());

        Job waiting = normalJob("waiting.mkv", Job.HASHWAIT);
        JobManager.setName(waiting, "ignored.mkv");
        assertEquals("waiting.mkv", waiting.getFile().getName());
    }

    @Test
    void updatePathHandlesNullIdentityConflictsAndUncreatableParents() throws Exception {
        Job job = normalJob("source.mkv", Job.HASHWAIT);
        assertTrue(JobManager.updatePath(job, null));
        assertTrue(JobManager.updatePath(job, job.getFile()));

        File conflict = Files.writeString(temporaryDirectory.resolve("conflict.mkv"), "different length")
                .toFile();
        assertFalse(JobManager.updatePath(job, conflict));
        assertEquals("File cannot be moved. Destination file already exists!", job.errorMessage);

        Path blocker = Files.writeString(temporaryDirectory.resolve("blocker"), "not a directory");
        File impossible = blocker.resolve("child/target.mkv").toFile();
        assertFalse(JobManager.updatePath(job, impossible));
        assertTrue(job.errorMessage.contains("cannot be created"));
    }

    @Test
    void applyRulesForcedRejectsIncompleteMetadataBeforeConsultingRules() throws Exception {
        Job job = normalJob("incomplete.mkv", Job.FINISHED);

        assertFalse(JobManager.applyRulesForced(job));

        assertEquals("Extensive fileinfo not available.", job.errorMessage);
        verify(AppContext.rules, never()).apply(job);
    }

    @Test
    void missingFilesShortCircuitExternalLaunchHelpers() {
        Job missing = new Job(temporaryDirectory.resolve("not-there.mkv").toFile(), Job.FINISHED);

        JobManager.openInDefaultPlayer(missing);
        JobManager.openInExplorer(missing);
        JobManager.runAvdump(missing);
    }

    @Test
    void alternateDeletionRefreshesCacheAndEventuallyRemovesFullyHiddenJob() throws Exception {
        AppContext.gui = null;
        Job job = normalJob("delete-alt.mkv", Job.HASHWAIT);
        AppContext.jobs.add(job);

        assertEquals(1, JobManager.deleteJobs(List.of(job), JobDeleteScope.ALT));
        assertFalse(job.isAltVisible());
        assertTrue(job.isJobsVisible());
        verify(AppContext.cache).rebuildTree();

        assertEquals(1, JobManager.deleteJobs(List.of(job), JobDeleteScope.JOBS));
        assertEquals(0, AppContext.jobs.array().length);
    }

    @Test
    void deleteJobsRejectsEmptyNullAndAlreadyHiddenSelections() throws Exception {
        assertEquals(0, JobManager.deleteJobs(null, JobDeleteScope.JOBS));
        assertEquals(0, JobManager.deleteJobs(List.of(), JobDeleteScope.JOBS));
        assertEquals(0, JobManager.deleteJobs(List.of(), null));

        Job hidden = normalJob("hidden-delete.mkv", Job.HASHWAIT);
        hidden.setJobsVisible(false);
        assertEquals(0, JobManager.deleteJobs(List.of(hidden), JobDeleteScope.JOBS));
    }

    @Test
    void siblingMoveWithoutDestinationExtensionThrowsForAnExtensionSibling() throws Exception {
        Path sourceDirectory = Files.createDirectory(temporaryDirectory.resolve("siblings"));
        File source = Files.createFile(sourceDirectory.resolve("episode")).toFile();
        Files.createFile(sourceDirectory.resolve("episode.srt"));
        Path destinationDirectory = Files.createDirectory(temporaryDirectory.resolve("renamed"));
        File destination = destinationDirectory.resolve("renamed").toFile();

        // TODO: Review extension handling when a destination has no dot. Current behavior indexes at -1.
        assertThrows(StringIndexOutOfBoundsException.class, () -> JobManager.moveSiblingFiles(source, destination));
    }

    private Job normalJob(String name, int status) throws Exception {
        Job job = new Job(Files.createFile(temporaryDirectory.resolve(name)).toFile(), status);
        job.setHealth0(Job.H_NORMAL);
        return job;
    }

    private Job identifiedJob(String name) throws Exception {
        Job job = normalJob(name, Job.IDENTIFYING);
        AniDBFile file = new AniDBFile(10);
        file.setFileId(10);
        job.anidbFile = file;
        return job;
    }
}
