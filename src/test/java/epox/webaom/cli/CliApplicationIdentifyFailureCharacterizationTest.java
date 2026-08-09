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
 */

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.data.AniDBFile;
import epox.webaom.db.DatabaseManager;
import epox.webaom.net.AniDBException;
import epox.webaom.net.AniDBFileClient;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliApplicationIdentifyFailureCharacterizationTest {
    @TempDir
    Path tempDirectory;

    private JobList previousJobs;
    private JobCounter previousJobCounter;
    private DatabaseManager previousDatabaseManager;
    private Cache previousCache;
    private String previousUsername;
    private String previousPassword;
    private JobList jobs;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() {
        previousJobs = AppContext.jobs;
        previousJobCounter = AppContext.jobCounter;
        previousDatabaseManager = AppContext.databaseManager;
        previousCache = AppContext.cache;
        previousUsername = AppContext.userPass.username;
        previousPassword = AppContext.userPass.password;
        jobs = mock(JobList.class);
        databaseManager = mock(DatabaseManager.class);
        AppContext.jobs = jobs;
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = databaseManager;
        AppContext.cache = mock(Cache.class);
        AppContext.userPass.username = "user";
        AppContext.userPass.password = "password";
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = previousJobs;
        AppContext.jobCounter = previousJobCounter;
        AppContext.databaseManager = previousDatabaseManager;
        AppContext.cache = previousCache;
        AppContext.userPass.username = previousUsername;
        AppContext.userPass.password = previousPassword;
    }

    @Test
    void duplicateQueueEntryBecomesAPerFileFailure() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("duplicate.mkv"), "video");
        when(jobs.add(source.toFile())).thenReturn(null);

        CliFileResult result = application(mock(AniDBFileClient.class))
                .identify(List.of(source), silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.FAILED, result.status);
        assertEquals("File is already queued.", result.message);
    }

    @Test
    void emptyFileBecomesAPerFileFailureBeforeNetworkAccess() throws Exception {
        Path source = Files.createFile(tempDirectory.resolve("empty.mkv"));
        when(jobs.add(source.toFile())).thenReturn(new Job(source.toFile(), Job.FINISHED));

        CliFileResult result = application(mock(AniDBFileClient.class))
                .identify(List.of(source), silentOutput(), CliApplication.Refresh.FULL)
                .getFirst();

        assertEquals(CliFileResult.Status.FAILED, result.status);
        assertEquals("File size is less than 1 byte.", result.message);
    }

    @Test
    void protocolFailureAbortsTheWholeIdentificationBatch() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("timeout.mkv"), "video");
        Job job = new Job(source.toFile(), Job.FINISHED);
        job.fileSize = Files.size(source);
        job.ed2kHash = "cached-hash";
        when(jobs.add(source.toFile())).thenReturn(job);
        AniDBFileClient client = connectedClient();
        when(client.retrieveFileData(eq(job.fileSize), eq("cached-hash"), anyString()))
                .thenThrow(new AniDBException(AniDBException.ANIDB_UNREACHABLE, "timed out"));

        CliException failure = assertThrows(CliException.class, () -> application(client)
                .identify(List.of(source), silentOutput(), CliApplication.Refresh.REMOTE));

        assertEquals(2, failure.exitCode());
        assertEquals("timed out", failure.getMessage());
    }

    @Test
    void missingRemoteFileMarksTheJobUnknownAndReportsPersistenceFailure() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("unknown.mkv"), "video");
        Job job = new Job(source.toFile(), Job.FINISHED);
        job.fileSize = Files.size(source);
        job.ed2kHash = "cached-hash";
        AniDBFile cachedFile = new AniDBFile(55);
        cachedFile.setFileId(55);
        job.anidbFile = cachedFile;
        when(jobs.add(source.toFile())).thenReturn(job);
        when(databaseManager.update(0, job, DatabaseManager.INDEX_JOB)).thenReturn(false);
        AniDBFileClient client = connectedClient();
        when(client.retrieveFileData(55, source.getFileName().toString())).thenReturn(null);
        when(client.getLastError()).thenReturn("No such file");

        CliFileResult result = application(client)
                .identify(List.of(source), silentOutput(), CliApplication.Refresh.REMOTE)
                .getFirst();

        assertEquals(CliFileResult.Status.UNKNOWN, result.status);
        assertTrue(result.persistenceFailed);
        assertTrue(result.message.startsWith("No such file"));
        assertTrue(result.message.contains("not saved locally"));
        verify(client).retrieveFileData(55, source.getFileName().toString());
    }

    private CliApplication application(AniDBFileClient client) {
        return new CliApplication(
                new Options(tempDirectory.resolve("config")), new Rules(), Set.of("mkv"), settings -> client);
    }

    private static AniDBFileClient connectedClient() throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(20);
        when(client.login()).thenReturn(true);
        return client;
    }

    private static CliOutput silentOutput() {
        PrintWriter discard = new PrintWriter(OutputStream.nullOutputStream());
        return new CliOutput(discard, discard, CliOutput.Format.TABLE, true, false);
    }
}
