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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.db.DatabaseManager;
import epox.webaom.hash.Ed2kHash;
import epox.webaom.ui.MainPanel;
import epox.webaom.ui.MiscOptionsPanel;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskIOManagerCharacterizationTest {
    @TempDir
    Path tempDir;

    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private MainPanel originalGui;
    private DatabaseManager originalDatabaseManager;
    private MainPanel gui;

    @BeforeEach
    void setUp() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalGui = AppContext.gui;
        originalDatabaseManager = AppContext.databaseManager;

        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        when(AppContext.databaseManager.isConnected()).thenReturn(false);

        gui = mock(MainPanel.class);
        gui.miscOptionsPanel = new MiscOptionsPanel();
        gui.statusProgressBar = new JProgressBar(0, 1000);
        when(gui.isDiskIoOk()).thenReturn(true);
        AppContext.gui = gui;
    }

    @AfterEach
    void tearDown() throws Exception {
        SwingUtilities.invokeAndWait(() -> {});
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.gui = originalGui;
        AppContext.databaseManager = originalDatabaseManager;
    }

    @Test
    void run_hashesAQueuedFileWithEverySelectedAlgorithm() throws Exception {
        byte[] content = "deterministic anime payload".getBytes(StandardCharsets.UTF_8);
        File source = Files.write(tempDir.resolve("episode.mkv"), content).toFile();
        for (int index = 1; index < gui.miscOptionsPanel.hashCheckBoxes.length; index++) {
            gui.miscOptionsPanel.hashCheckBoxes[index].setSelected(true);
        }
        Job job = queuedJob(source, Job.HASHWAIT);

        new DiskIOManager().run();
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(Job.IDENTWAIT, job.getStatus());
        assertEquals(1f, job.hashProgress);
        assertEquals(ed2k(content), job.ed2kHash);
        assertNotNull(job.md5Hash);
        assertNotNull(job.sha1Hash);
        assertNotNull(job.tthHash);
        assertNotNull(job.crc32Hash);
        assertNull(gui.diskIoThread);
        verify(gui, atLeastOnce()).printHash(contains("ed2k://|file|episode.mkv|"));
        verify(gui).setDiskIoOptionsEnabled(false);
        verify(gui).setDiskIoOptionsEnabled(true);
    }

    @Test
    void run_emptyFileFailsBeforeHashing() throws IOException {
        Job job = queuedJob(Files.createFile(tempDir.resolve("empty.mkv")).toFile(), Job.HASHWAIT);

        new DiskIOManager().run();

        assertEquals(Job.FAILED, job.getStatus());
        assertEquals("File size less than 1.", job.errorMessage);
        assertEquals(0f, job.hashProgress);
    }

    @Test
    void run_missingFileIsReportedAsZeroLengthByCurrentOrdering() {
        File missing = tempDir.resolve("missing.mkv").toFile();
        Job job = queuedMissingJob(missing, Job.HASHWAIT);

        new DiskIOManager().run();

        // TODO: Review whether a missing file should report "File does not exist" instead of a size error.
        assertEquals(Job.FAILED, job.getStatus());
        assertEquals("File size less than 1.", job.errorMessage);
    }

    @Test
    void run_interruptedHashReturnsJobToWaitState() throws IOException {
        File source =
                Files.writeString(tempDir.resolve("interrupt.mkv"), "payload").toFile();
        Job job = queuedJob(source, Job.HASHWAIT);
        AtomicBoolean running = new AtomicBoolean(true);
        when(gui.isDiskIoOk()).thenAnswer(invocation -> {
            if (Thread.currentThread().getName().startsWith("HashWorker-")) {
                running.set(false);
                return false;
            }
            return running.get();
        });

        new DiskIOManager().run();

        assertEquals(Job.HASHWAIT, job.getStatus());
        assertEquals(0f, job.hashProgress);
        assertNull(job.ed2kHash);
    }

    @Test
    void run_movesFileCopiesSiblingAndVerifiesChecksum() throws IOException {
        Path sourceDirectory = Files.createDirectory(tempDir.resolve("source"));
        Path targetDirectory = tempDir.resolve("target");
        byte[] content = "move payload".getBytes(StandardCharsets.UTF_8);
        File source =
                Files.write(sourceDirectory.resolve("episode.mkv"), content).toFile();
        Files.writeString(sourceDirectory.resolve("episode.en.srt"), "subtitle");
        File target = targetDirectory.resolve("renamed.mkv").toFile();
        Job job = queuedJob(source, Job.MOVEWAIT);
        job.targetFile = target;
        job.ed2kHash = ed2k(content);

        new DiskIOManager().run();

        assertEquals(Job.FINISHED, job.getStatus());
        assertEquals(target, job.currentFile);
        assertNull(job.targetFile);
        assertTrue(target.isFile());
        assertTrue(targetDirectory.resolve("renamed.srt").toFile().isFile());
        assertFalse(source.exists());
        verify(gui).println(contains("Renamed sibling"));
    }

    @Test
    void run_sameSourceAndTargetCompletesWithoutCopying() throws IOException {
        File source = Files.writeString(tempDir.resolve("same.mkv"), "payload").toFile();
        Job job = queuedJob(source, Job.MOVEWAIT);
        job.targetFile = source;

        new DiskIOManager().run();

        assertEquals(Job.FINISHED, job.getStatus());
        assertNull(job.targetFile);
        assertTrue(source.exists());
    }

    @Test
    void run_existingDestinationWithWrongChecksumFailsWithoutDeletingEitherFile() throws IOException {
        File source = Files.writeString(tempDir.resolve("source.mkv"), "source payload")
                .toFile();
        File target = Files.writeString(tempDir.resolve("target.mkv"), "different payload")
                .toFile();
        Job job = queuedJob(source, Job.MOVEWAIT);
        job.targetFile = target;
        job.ed2kHash = ed2k(Files.readAllBytes(source.toPath()));

        new DiskIOManager().run();

        assertEquals(Job.FAILED, job.getStatus());
        assertTrue(job.errorMessage.contains("Destination file does already exist"));
        assertTrue(source.exists());
        assertTrue(target.exists());
    }

    @Test
    void run_canceledVerificationDeletesOnlyTheNewCopyAndRequeuesMove() throws IOException {
        File source = Files.writeString(tempDir.resolve("cancel-source.mkv"), "payload")
                .toFile();
        File target = tempDir.resolve("cancel-target.mkv").toFile();
        Job job = queuedJob(source, Job.MOVEWAIT);
        job.targetFile = target;
        job.ed2kHash = ed2k(Files.readAllBytes(source.toPath()));
        when(gui.isDiskIoOk()).thenReturn(true, true, true, false, false);

        new DiskIOManager().run();

        assertEquals(Job.MOVEWAIT, job.getStatus());
        assertTrue(source.exists());
        assertFalse(target.exists());
    }

    @Test
    void run_parseJobFailsDeterministicallyWhenNativeAvLibraryIsUnavailable() throws IOException {
        File source =
                Files.writeString(tempDir.resolve("parse.mkv"), "not media").toFile();
        Job job = queuedJob(source, Job.PARSEWAIT);

        new DiskIOManager().run();

        assertEquals(Job.FAILED, job.getStatus());
    }

    private Job queuedJob(File file, int status) {
        Job job = new Job(file, status);
        AppContext.jobs.add(job);
        job.updateHealth(Job.H_PAUSED);
        return job;
    }

    private Job queuedMissingJob(File file, int status) {
        Job job = new Job(file, status);
        job.setHealth0(Job.H_NORMAL);
        AppContext.jobs.add(job);
        AppContext.jobs.updateQueues(job, 0, job.getStatus());
        return job;
    }

    private String ed2k(byte[] content) {
        Ed2kHash hash = new Ed2kHash();
        hash.update(content, 0, content.length);
        return hash.hexValue();
    }
}
