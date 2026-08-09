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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.db.DatabaseManager;
import epox.webaom.net.AniDBFileClient;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliApplicationMylistTest {
    @TempDir
    Path temporaryDirectory;

    private JobList previousJobs;
    private JobCounter previousJobCounter;
    private DatabaseManager previousDatabaseManager;
    private Cache previousCache;
    private String previousUsername;
    private String previousPassword;
    private String previousApiKey;

    @BeforeEach
    void setUp() {
        previousJobs = AppContext.jobs;
        previousJobCounter = AppContext.jobCounter;
        previousDatabaseManager = AppContext.databaseManager;
        previousCache = AppContext.cache;
        previousUsername = AppContext.userPass.username;
        previousPassword = AppContext.userPass.password;
        previousApiKey = AppContext.userPass.apiKey;

        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.cache = mock(Cache.class);
        AppContext.userPass.username = "user";
        AppContext.userPass.password = "password";
        AppContext.userPass.apiKey = null;
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = previousJobs;
        AppContext.jobCounter = previousJobCounter;
        AppContext.databaseManager = previousDatabaseManager;
        AppContext.cache = previousCache;
        AppContext.userPass.username = previousUsername;
        AppContext.userPass.password = previousPassword;
        AppContext.userPass.apiKey = previousApiKey;
    }

    @Test
    void addReportsCreatedWhenAniDBCreatesANewEntry() throws Exception {
        AniDBFileClient client = identifiedClient();
        when(client.addOrEditMylistEntry(any(Job.class), any()))
                .thenReturn(new AniDBFileClient.MylistAddOutcome(98765, true, false));
        CliApplication application = application(client);

        List<CliFileResult> results = application.mylist(
                List.of(mediaFile()),
                CliApplication.MylistAction.ADD,
                CliApplication.MylistOverrides.none(),
                silentOutput());

        assertEquals(1, results.size());
        assertEquals(CliFileResult.Status.MYLIST_ADDED, results.getFirst().status);
        assertEquals(98765, results.getFirst().job.mylistId);
    }

    @Test
    void addReportsUpdatedWhenAniDBEditsAnExistingEntry() throws Exception {
        AniDBFileClient client = identifiedClient();
        when(client.addOrEditMylistEntry(any(Job.class), any()))
                .thenReturn(new AniDBFileClient.MylistAddOutcome(55555, false, true));
        CliApplication application = application(client);

        List<CliFileResult> results = application.mylist(
                List.of(mediaFile()),
                CliApplication.MylistAction.ADD,
                CliApplication.MylistOverrides.none(),
                silentOutput());

        assertEquals(1, results.size());
        assertEquals(CliFileResult.Status.MYLIST_ADDED, results.getFirst().status);
        assertTrue(results.getFirst().message.contains("entry updated"));
    }

    @Test
    void addReportsFailedWhenAniDBRejectsTheEdit() throws Exception {
        AniDBFileClient client = identifiedClient();
        when(client.addOrEditMylistEntry(any(Job.class), any()))
                .thenReturn(new AniDBFileClient.MylistAddOutcome(55555, false, false));
        when(client.getLastError()).thenReturn("Entry is locked");
        CliApplication application = application(client);

        List<CliFileResult> results = application.mylist(
                List.of(mediaFile()),
                CliApplication.MylistAction.ADD,
                CliApplication.MylistOverrides.none(),
                silentOutput());

        assertEquals(1, results.size());
        assertEquals(CliFileResult.Status.FAILED, results.getFirst().status);
        assertTrue(results.getFirst().message.contains("Entry is locked"));
    }

    @Test
    void statusReportsCachedMylistPresenceWithoutMutatingTheEntry() throws Exception {
        AniDBFileClient client = identifiedClient(321);

        CliFileResult result = application(client)
                .mylist(
                        List.of(mediaFile()),
                        CliApplication.MylistAction.STATUS,
                        CliApplication.MylistOverrides.none(),
                        silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.MYLIST_PRESENT, result.status);
        assertEquals(321, result.job.mylistId);
    }

    @Test
    void statusAndRemoveReportAnAbsentMylistEntry() throws Exception {
        AniDBFileClient statusClient = identifiedClient();
        CliFileResult status = application(statusClient)
                .mylist(
                        List.of(mediaFile()),
                        CliApplication.MylistAction.STATUS,
                        CliApplication.MylistOverrides.none(),
                        silentOutput())
                .getFirst();
        assertEquals(CliFileResult.Status.MYLIST_ABSENT, status.status);

        AppContext.jobs = new JobList();
        AniDBFileClient removeClient = identifiedClient();
        CliFileResult remove = application(removeClient)
                .mylist(
                        List.of(mediaFile()),
                        CliApplication.MylistAction.REMOVE,
                        CliApplication.MylistOverrides.none(),
                        silentOutput())
                .getFirst();
        assertEquals(CliFileResult.Status.MYLIST_ABSENT, remove.status);
        assertEquals("File is not in MyList.", remove.message);
    }

    @Test
    void removeClearsAnExistingMylistEntryAndPersistsTheJob() throws Exception {
        AniDBFileClient client = identifiedClient(654);
        when(client.removeFromMylist(654, "episode.mkv")).thenReturn(true);

        CliFileResult result = application(client)
                .mylist(
                        List.of(mediaFile()),
                        CliApplication.MylistAction.REMOVE,
                        CliApplication.MylistOverrides.none(),
                        silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.MYLIST_REMOVED, result.status);
        assertEquals(0, result.job.mylistId);
        verify(client).removeFromMylist(654, "episode.mkv");
    }

    @Test
    void failedRemoveUsesTheCurrentConnectionError() throws Exception {
        AniDBFileClient client = identifiedClient(654);
        when(client.removeFromMylist(654, "episode.mkv")).thenReturn(false);
        when(client.getLastError()).thenReturn("Removal rejected");

        CliFileResult result = application(client)
                .mylist(
                        List.of(mediaFile()),
                        CliApplication.MylistAction.REMOVE,
                        CliApplication.MylistOverrides.none(),
                        silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.FAILED, result.status);
        assertEquals("Removal rejected", result.message);
    }

    private static CliOutput silentOutput() {
        PrintWriter discard = new PrintWriter(java.io.OutputStream.nullOutputStream());
        return new CliOutput(discard, discard, CliOutput.Format.TABLE, true, false);
    }

    private Path mediaFile() throws Exception {
        return Files.writeString(temporaryDirectory.resolve("episode.mkv"), "video content");
    }

    private CliApplication application(AniDBFileClient client) {
        Options options = new Options(temporaryDirectory.resolve("config"));
        return new CliApplication(options, new Rules(), Set.of("mkv"), settings -> client);
    }

    /** A mock client whose identify flow produces a complete IDENTIFIED result. */
    private AniDBFileClient identifiedClient() throws Exception {
        return identifiedClient(0);
    }

    private AniDBFileClient identifiedClient(int mylistEntryId) throws Exception {
        AniDBFileClient client = mock(AniDBFileClient.class);
        when(client.connect()).thenReturn(true);
        when(client.encrypt()).thenReturn(25);
        when(client.login()).thenReturn(true);
        when(client.retrieveFileData(anyInt(), anyString())).thenReturn(fileFields());
        when(client.retrieveFileData(anyLong(), any(), anyString())).thenReturn(fileFields());

        AniDBFile anidbFile = new AniDBFile(4242);
        anidbFile.setFileId(4242);
        anidbFile.setMylistEntryId(mylistEntryId);
        Anime anime = new Anime(10);
        anime.romajiTitle = "Test Anime";
        Episode episode = new Episode(20);
        episode.num = "1";
        Group group = new Group(30);
        group.shortName = "TestGroup";
        when(AppContext.cache.parseFileWithPersistence(any(), any(Job.class))).thenAnswer(invocation -> {
            Job job = invocation.getArgument(1);
            job.anidbFile = anidbFile;
            job.anidbFile.setAnime(anime);
            job.anidbFile.setEpisode(episode);
            job.anidbFile.setGroup(group);
            return new Cache.FileParseResult(anidbFile, true);
        });
        when(AppContext.databaseManager.update(anyInt(), any(), anyInt())).thenReturn(true);
        return client;
    }

    private static String[] fileFields() {
        // Minimal field array matching the AniDB FILE response format.
        String[] fields = new String[40];
        fields[0] = "4242"; // file id
        fields[1] = "10"; // anime id
        fields[2] = "20"; // episode id
        fields[3] = "30"; // group id
        fields[9] = "1"; // mylist id
        return fields;
    }
}
