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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import epox.webaom.ui.MainPanel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobManagerTest {
    @TempDir
    Path tempDir;

    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private MainPanel originalGui;

    @BeforeEach
    void setUp() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalGui = AppContext.gui;

        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.gui = mock(MainPanel.class);
        Cache.setTreeSortMode(Cache.MODE_ANIME_EPISODE_FILE);
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.gui = originalGui;
        JobManager.resetBatchChoice();
    }

    @Test
    void jobManagerUpdatePath_sourceMissing_returnsFalseAndSetsError() {
        File missingSource = tempDir.resolve("missing.mkv").toFile();
        Job job = new Job(missingSource, Job.HASHWAIT);
        File destination = tempDir.resolve("dest.mkv").toFile();

        boolean success = JobManager.updatePath(job, destination);

        assertFalse(success);
        assertEquals("File does not exist.", job.errorMessage);
        assertNull(job.targetFile);
    }

    @Test
    void jobManagerUpdatePath_destinationExistsSameLength_setsTargetForLaterMove() throws IOException {
        File source = Files.writeString(tempDir.resolve("source.mkv"), "abc").toFile();
        Path destinationDir = Files.createDirectory(tempDir.resolve("dest"));
        File destination =
                Files.writeString(destinationDir.resolve("source.mkv"), "xyz").toFile();
        Job job = new Job(source, Job.HASHWAIT);

        boolean success = JobManager.updatePath(job, destination);

        assertTrue(success);
        assertEquals(destination.getAbsolutePath(), job.targetFile.getAbsolutePath());
        assertEquals(source.getAbsolutePath(), job.currentFile.getAbsolutePath());
        assertTrue(source.exists());
        assertTrue(destination.exists());
    }

    @Test
    void jobManagerUpdatePath_successfulRename_movesSiblingFiles() throws IOException {
        Path sourceDir = Files.createDirectory(tempDir.resolve("sourceDir"));
        Path destinationDir = Files.createDirectory(tempDir.resolve("destinationDir"));
        File source =
                Files.writeString(sourceDir.resolve("episode.mkv"), "video").toFile();
        Files.writeString(sourceDir.resolve("episode.srt"), "subtitle");
        File destination = destinationDir.resolve("renamed.mkv").toFile();
        Job job = new Job(source, Job.HASHWAIT);

        boolean success = JobManager.updatePath(job, destination);

        assertTrue(success);
        assertNull(job.targetFile);
        assertEquals(destination.getAbsolutePath(), job.currentFile.getAbsolutePath());
        assertTrue(destination.exists());
        assertFalse(source.exists());
        assertTrue(destinationDir.resolve("renamed.srt").toFile().exists());
    }
}
