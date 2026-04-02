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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHandlerTest {
    @TempDir
    Path tempDir;

    private JobList originalJobs;
    private JobCounter originalJobCounter;

    @BeforeEach
    void setUp() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.setAppMode(AppContext.AppMode.NORMAL);
    }

    @Test
    void addFile_returnsFalseWhenIncompatibleDatabaseIsLocked() throws Exception {
        File file = Files.writeString(tempDir.resolve("blocked.mkv"), "video").toFile();
        FileHandler fileHandler = new FileHandler();
        AppContext.setAppMode(AppContext.AppMode.INCOMPATIBLE_DATABASE);

        assertFalse(fileHandler.addFile(file));
        assertEquals(0, AppContext.jobs.size());
    }

    @Test
    void addFile_readdsHiddenExistingJob_withoutDoubleCountingOrPausingIt() throws Exception {
        File file = Files.writeString(tempDir.resolve("episode.mkv"), "video").toFile();
        Job job = new Job(file, Job.HASHWAIT);
        assertTrue(AppContext.jobs.add(job));
        job.updateHealth(Job.H_PAUSED);
        AppContext.jobCounter.unregister(job.getStatus(), job.getHealth());
        AppContext.jobs.updateQueues(job, job.getStatus(), -1);
        job.setJobsVisible(false);

        FileHandler fileHandler = new FileHandler();

        assertTrue(fileHandler.addFile(file));
        assertTrue(job.isJobsVisible());
        assertEquals(Job.H_NORMAL, job.getHealth());
        assertTrue(AppContext.jobCounter.getStatus().contains("tot=1"));
        assertTrue(AppContext.jobCounter.getStatus().contains("dio=1"));
    }

    @Test
    void addFile_readdsHiddenMissingJobWhenFileReturns() throws Exception {
        File file = tempDir.resolve("missing-episode.mkv").toFile();
        Job job = new Job(file, Job.HASHWAIT);
        assertTrue(AppContext.jobs.add(job));
        AppContext.jobCounter.unregister(job.getStatus(), job.getHealth());
        job.setJobsVisible(false);

        assertEquals(Job.H_MISSING, job.getHealth());
        assertTrue(Files.writeString(file.toPath(), "video").toFile().exists());

        FileHandler fileHandler = new FileHandler();

        assertTrue(fileHandler.addFile(file));
        assertTrue(job.isJobsVisible());
        assertEquals(Job.H_NORMAL, job.getHealth());
        assertTrue(AppContext.jobCounter.getStatus().contains("tot=1"));
        assertTrue(AppContext.jobCounter.getStatus().contains("dio=1"));
    }
}
