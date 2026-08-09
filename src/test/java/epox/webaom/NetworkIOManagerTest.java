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
 */

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.data.Mylist;
import epox.webaom.db.DatabaseManager;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBException;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.ui.MainPanel;
import epox.webaom.ui.MylistOptionsPanel;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class NetworkIOManagerTest {
    @TempDir
    Path tempDirectory;

    private MainPanel previousGui;
    private AniDBFileClient previousConnection;
    private JobList previousJobs;
    private JobCounter previousJobCounter;
    private Cache previousCache;
    private DatabaseManager previousDatabaseManager;
    private Options previousOptions;
    private boolean previousAutoadd;
    private boolean previousShutdown;
    private MainPanel gui;

    @BeforeEach
    void setUp() {
        previousGui = AppContext.gui;
        previousConnection = AppContext.conn;
        previousJobs = AppContext.jobs;
        previousJobCounter = AppContext.jobCounter;
        previousCache = AppContext.cache;
        previousDatabaseManager = AppContext.databaseManager;
        previousOptions = AppContext.opt;
        previousAutoadd = AppContext.autoadd;
        previousShutdown = AniDBConnection.isShutdown();
        gui = mock(MainPanel.class);
        AppContext.gui = gui;
        AppContext.conn = null;
        AppContext.jobs = mock(JobList.class);
        AppContext.jobCounter = mock(JobCounter.class);
        AppContext.cache = mock(Cache.class);
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.opt = new Options(tempDirectory.resolve("config"));
        AppContext.opt.setBoolean(Options.BOOL_AUTO_RENAME, false);
        AppContext.autoadd = false;
        AniDBConnection.setShutdown(false);
    }

    @AfterEach
    void tearDown() {
        AppContext.gui = previousGui;
        AppContext.conn = previousConnection;
        AppContext.jobs = previousJobs;
        AppContext.jobCounter = previousJobCounter;
        AppContext.cache = previousCache;
        AppContext.databaseManager = previousDatabaseManager;
        AppContext.opt = previousOptions;
        AppContext.autoadd = previousAutoadd;
        AniDBConnection.setShutdown(previousShutdown);
        Thread.interrupted();
    }

    @Test
    void pingReportsReachabilityWithoutOpeningANetworkSocket() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.encrypt()).thenReturn(25);

        assertTrue(new NetworkIOManager().ping(connection));

        verify(gui).println("AniDB is reachable. Received reply in <span class=\"log-number\">25</span> ms.");
    }

    @Test
    void pingReportsTimeoutAndOffersConnectionGuidance() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.encrypt()).thenThrow(new SocketTimeoutException("timeout"));

        assertFalse(new NetworkIOManager().ping(connection));

        verify(gui).println("<span class=\"log-warning\">AniDB is not reachable.</span>");
        verify(gui).status1("AniDB is not reachable.");
        verify(gui).showMessage("AniDB is not reachable.");
        verify(gui).println("Check out the connection options or try again later.");
    }

    @Test
    void pingReportsInvalidNumbersAndOtherFailures() throws Exception {
        AniDBFileClient invalidNumberConnection = mock(AniDBFileClient.class);
        when(invalidNumberConnection.encrypt()).thenThrow(new NumberFormatException("bad port"));
        assertFalse(new NetworkIOManager().ping(invalidNumberConnection));
        verify(gui).showMessage("Invalid number. bad port");

        AniDBFileClient failedConnection = mock(AniDBFileClient.class);
        when(failedConnection.encrypt()).thenThrow(new IllegalStateException("broken"));
        assertFalse(new NetworkIOManager().ping(failedConnection));
        verify(gui).println("<span class=\"log-warning\">broken</span>");
        verify(gui).showMessage("broken");
    }

    @Test
    void runMapsBindFailuresAndAlwaysRestoresNetworkControls() {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(false);
        when(connection.getLastError()).thenReturn("SocketException: Cannot bind");

        new NetworkIOManager().run();

        String message = "The local port is already in use. Try another port.";
        verify(gui).println("<span class=\"log-warning\">" + message + "</span>");
        verify(gui).showMessage(message);
        verify(gui).setNetworkIoOptionsEnabled(true);
        verify(gui).setNetworkIoEnabled(false);
        assertNull(gui.networkIoThread);
    }

    @Test
    void runDisconnectsAfterAConnectedClientDeclinesLogin() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.login()).thenReturn(false);
        NetworkIOManager manager = spy(new NetworkIOManager());
        doReturn(true).when(manager).ping(connection);

        manager.run();

        verify(gui).status1("Checking connection...");
        verify(gui).status1("Authenticating...");
        verify(gui).println("NetIO thread terminated.");
        verify(connection).disconnect();
        verify(connection, never()).logout();
        assertNull(AppContext.conn);
    }

    @Test
    void runDisconnectsAfterFailedPingWithoutWaitingInRealTime() {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        NetworkIOManager manager = spy(new NetworkIOManager(milliseconds -> {}));
        doReturn(false).when(manager).ping(connection);

        manager.run();

        verify(connection).disconnect();
        verify(gui).status1("Sleeping...");
        verify(gui).status1("NetIO thread terminated.");
    }

    @Test
    void encryptionFailureDoesNotTriggerTheFatalErrorPath() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.login()).thenThrow(new AniDBException(AniDBException.ENCRYPTION, "bad key"));
        NetworkIOManager manager = spy(new NetworkIOManager());
        doReturn(true).when(manager).ping(connection);

        manager.run();

        verify(gui).status1("bad key");
        verify(gui).showMessage(" bad key");
        verify(gui, never()).kill();
        verify(gui, never()).handleFatalError(true);
        verify(connection).disconnect();
    }

    @Test
    void unexpectedRuntimeFailureTriggersTheFatalErrorPath() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.login()).thenThrow(new IllegalStateException("broken login"));
        NetworkIOManager manager = spy(new NetworkIOManager());
        doReturn(true).when(manager).ping(connection);

        manager.run();

        verify(gui).println(" <span class=\"log-warning\">broken login</span>");
        verify(gui).showMessage(" broken login");
        verify(gui).kill();
        verify(gui).handleFatalError(true);
        verify(connection).disconnect();
    }

    @Test
    void shutdownSystemFailureIsTreatedAsExpectedTermination() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        when(connection.login()).thenThrow(new AniDBException(AniDBException.CLIENT_SYSTEM, "socket closed"));
        NetworkIOManager manager = spy(new NetworkIOManager());
        doReturn(true).when(manager).ping(connection);
        AniDBConnection.setShutdown(true);

        manager.run();

        verify(gui, never()).showMessage(" socket closed");
        verify(gui, never()).kill();
        verify(connection).disconnect();
    }

    @Test
    void runPreservesConnectFailuresOtherThanCannotBind() {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(false);
        when(connection.getLastError()).thenReturn("Connection refused");

        new NetworkIOManager().run();

        verify(gui).println("<span class=\"log-warning\">Connection refused</span>");
        verify(gui).showMessage("Connection refused");
    }

    @Test
    void runPerformsAnExtraLogoutWhenTheClientStillReportsLoggedIn() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(false);
        when(connection.isLoggedIn()).thenReturn(true);
        when(connection.logout()).thenReturn(true);

        connectedManager(connection).run();

        verify(connection).logout();
        verify(gui).println("Logged out after extra check!");
    }

    @Test
    void runRestoresControlsWhenTheExtraLogoutFails() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(false);
        when(connection.isLoggedIn()).thenReturn(true);
        when(connection.logout()).thenThrow(new AniDBException(AniDBException.CLIENT_SYSTEM, "logout failed"));

        connectedManager(connection).run();

        verify(gui).setNetworkIoOptionsEnabled(true);
        verify(gui).setNetworkIoEnabled(false);
    }

    @Test
    void timeoutFailureSuppressesTheExtraLogoutAndTriggersFatalHandling() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenThrow(new AniDBException(AniDBException.CLIENT_USER, "REQUEST TIME OUT"));
        when(connection.isLoggedIn()).thenReturn(true);

        connectedManager(connection).run();

        verify(gui).status1("REQUEST TIME OUT");
        verify(gui).kill();
        verify(gui).handleFatalError(true);
        verify(connection, never()).logout();
    }

    @Test
    void nullAniDBFailureUsesTheLegacyNullPointerMessage() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenThrow(new AniDBException(AniDBException.CLIENT_USER, null));

        connectedManager(connection).run();

        // TODO: Review whether a null protocol error should still be presented as a null pointer exception.
        verify(gui).showMessage(" Null pointer exception.");
        verify(gui).kill();
    }

    @Test
    void shutdownRecognizesMessageBasedSocketFailures() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenThrow(new AniDBException(AniDBException.CLIENT_USER, "Remote socket closed"));
        AniDBConnection.setShutdown(true);

        connectedManager(connection).run();

        verify(gui, never()).showMessage(" Remote socket closed");
        verify(gui, never()).kill();
    }

    @Test
    void interruptedFailedPingPreservesTheInterruptFlag() {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        NetworkIOManager manager = spy(new NetworkIOManager(milliseconds -> {
            throw new InterruptedException("stop");
        }));
        doReturn(false).when(manager).ping(connection);

        manager.run();

        assertTrue(Thread.currentThread().isInterrupted());
        verify(connection).disconnect();
    }

    @Test
    void interruptedIdleLoopReportsTheInterruptionWithoutWaiting() throws Exception {
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(AppContext.jobs.getJobNio()).thenReturn(null);
        NetworkIOManager manager = connectedManager(connection, milliseconds -> {
            throw new InterruptedException("stop");
        });

        manager.run();

        assertTrue(Thread.currentThread().isInterrupted());
        verify(gui).status1("Idle");
        verify(gui).println(" <span class=\"log-warning\">Thread interrupted</span>");
    }

    @Test
    void runProcessesOneSuccessfulRemoval() throws Exception {
        Job job = createJob("remove.mkv", Job.REMWAIT);
        job.mylistId = 45;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.removeFromMylist(45, "remove.mkv")).thenReturn(true);
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertEquals(0, job.mylistId);
        assertEquals(Job.FINISHED, job.getStatus());
        verify(gui).println("Removed <span class=\"log-name\">" + job.getFile().getPath() + "</span>");
    }

    @Test
    void removalWithoutAMylistEntryFailsLocally() throws Exception {
        Job job = createJob("not-listed.mkv", Job.REMWAIT);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertEquals(Job.FAILED, job.getStatus());
        assertEquals("Was not in mylist", job.errorMessage);
        verify(connection, never()).removeFromMylist(any(Integer.class), any(String.class));
    }

    @Test
    void failedRemovalResponseMarksTheJobFailed() throws Exception {
        Job job = createJob("remove-failed.mkv", Job.REMWAIT);
        job.mylistId = 46;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.removeFromMylist(46, "remove-failed.mkv")).thenReturn(false);
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertEquals(Job.FAILED, job.getStatus());
        verify(gui).println("<span class=\"log-warning\">Could not remove: " + job.getFile() + "</span>");
    }

    @Test
    void removalExceptionCleansTheCurrentJobBeforeDisconnecting() throws Exception {
        Job job = createJob("remove-error.mkv", Job.REMWAIT);
        job.mylistId = 47;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.removeFromMylist(47, "remove-error.mkv"))
                .thenThrow(new AniDBException(AniDBException.CLIENT_USER, "remove unavailable"));
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertEquals(Job.FAILED, job.getStatus());
        assertEquals("remove unavailable", job.errorMessage);
        verify(gui).kill();
        verify(connection).disconnect();
    }

    @Test
    void identifyByHashAttachesTheParsedFileAndFinishesTheJob() throws Exception {
        Job job = createJob("identify.mkv", Job.IDENTWAIT);
        job.fileSize = 1000;
        job.ed2kHash = "ed2k";
        AniDBFile parsedFile = identifiedFile(101);
        String[] response = {"response"};
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.retrieveFileData(1000, "ed2k", "identify.mkv")).thenReturn(response);
        when(AppContext.cache.parseFile(response, job)).thenAnswer(invocation -> {
            job.anidbFile = parsedFile;
            return parsedFile;
        });
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertSame(parsedFile, job.anidbFile);
        assertSame(job, parsedFile.getJob());
        assertEquals(321, job.mylistId);
        assertEquals(Job.FINISHED, job.getStatus());
        verify(gui).println(org.mockito.ArgumentMatchers.startsWith("Found "));
    }

    @Test
    void identifyByOverrideMarksANullResponseUnknown() throws Exception {
        Job job = createJob("override.mkv", Job.IDENTWAIT);
        job.fileIdOverride = 88;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.retrieveFileData(88, "override.mkv")).thenReturn(null);
        when(AppContext.jobs.getJobNio()).thenReturn(job);

        connectedManager(connection).run();

        assertEquals(Job.UNKNOWN, job.getStatus());
        verify(connection).retrieveFileData(88, "override.mkv");
        verify(AppContext.cache, never()).parseFile(any(String[].class), eq(job));
    }

    @Test
    void identifyExistingFileLoadsGroupAndEpisodeButCurrentlySkipsAnime() throws Exception {
        Job job = createJob("cached.mkv", Job.IDENTWAIT);
        AniDBFile file = identifiedFile(102);
        file.setGroupId(12);
        file.setEpisodeId(23);
        file.setAnimeId(34);
        job.anidbFile = file;
        Group group = new Group(12);
        Episode episode = new Episode(23);
        when(AppContext.cache.get(12, DatabaseManager.INDEX_GROUP)).thenReturn(group);
        when(AppContext.cache.get(23, DatabaseManager.INDEX_EPISODE)).thenReturn(episode);
        when(AppContext.jobs.getJobNio()).thenReturn(job);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);

        connectedManager(connection).run();

        assertSame(group, file.getGroup());
        assertSame(episode, file.getEpisode());
        // TODO: Review the repeated group-null check, which leaves anime unloaded after a group cache hit.
        assertNull(file.getAnime());
        verify(AppContext.cache, never()).get(34, DatabaseManager.INDEX_ANIME);
        assertEquals(Job.FINISHED, job.getStatus());
    }

    @Test
    void identifyExistingFileLoadsAnimeWhenTheGroupRemainsMissing() throws Exception {
        Job job = createJob("raw.mkv", Job.IDENTWAIT);
        AniDBFile file = identifiedFile(103);
        file.setGroupId(0);
        file.setEpisodeId(24);
        file.setAnimeId(35);
        job.anidbFile = file;
        Episode episode = new Episode(24);
        Anime anime = new Anime(35);
        when(AppContext.cache.get(24, DatabaseManager.INDEX_EPISODE)).thenReturn(episode);
        when(AppContext.cache.get(35, DatabaseManager.INDEX_ANIME)).thenReturn(anime);
        when(AppContext.jobs.getJobNio()).thenReturn(job);
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);

        connectedManager(connection).run();

        assertNull(file.getGroup());
        assertSame(episode, file.getEpisode());
        assertSame(anime, file.getAnime());
        assertEquals(Job.FINISHED, job.getStatus());
    }

    @Test
    void mylistAddStoresTheReturnedEntryId() throws Exception {
        Job job = createJob("add.mkv", Job.ADDWAIT);
        job.anidbFile = identifiedFile(104);
        Mylist mylist = new Mylist();
        MylistOptionsPanel optionsPanel = mock(MylistOptionsPanel.class);
        when(optionsPanel.getMylistData()).thenReturn(mylist);
        gui.mylistOptionsPanel = optionsPanel;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.addFileToMylist(job, mylist)).thenReturn(777);
        when(AppContext.jobs.getJobNio()).thenReturn(job);
        when(gui.isNetworkIoOk()).thenReturn(true, false);

        connectedManager(connection).run();

        assertEquals(777, job.mylistId);
        assertEquals(Job.FINISHED, job.getStatus());
        verify(gui).println("Added <span class=\"log-name\">" + job.getFile().getPath() + "</span> to mylist");
    }

    @Test
    void zeroMylistResponseStillAdvancesTheJobToFinished() throws Exception {
        Job job = createJob("add-zero.mkv", Job.ADDWAIT);
        job.anidbFile = identifiedFile(105);
        Mylist mylist = new Mylist();
        MylistOptionsPanel optionsPanel = mock(MylistOptionsPanel.class);
        when(optionsPanel.getMylistData()).thenReturn(mylist);
        gui.mylistOptionsPanel = optionsPanel;
        AniDBFileClient connection = mock(AniDBFileClient.class);
        when(connection.login()).thenReturn(true);
        when(connection.addFileToMylist(job, mylist)).thenReturn(0);
        when(AppContext.jobs.getJobNio()).thenReturn(job);
        when(gui.isNetworkIoOk()).thenReturn(true, false);

        connectedManager(connection).run();

        // TODO: Review whether a zero mylist ID should advance an add operation to a finished job.
        assertEquals(0, job.mylistId);
        assertEquals(Job.FINISHED, job.getStatus());
        verify(gui, never()).println("Added <span class=\"log-name\">add-zero.mkv</span> to mylist");
    }

    private NetworkIOManager connectedManager(AniDBFileClient connection) {
        return connectedManager(connection, milliseconds -> {});
    }

    private NetworkIOManager connectedManager(AniDBFileClient connection, NetworkIOManager.Sleeper sleeper) {
        when(gui.createConnection()).thenReturn(connection);
        when(connection.connect()).thenReturn(true);
        NetworkIOManager manager = spy(new NetworkIOManager(sleeper));
        doReturn(true).when(manager).ping(connection);
        return manager;
    }

    private Job createJob(String fileName, int status) throws Exception {
        Path file = tempDirectory.resolve(fileName);
        Files.writeString(file, "test");
        return new Job(file.toFile(), status);
    }

    private static AniDBFile identifiedFile(int fileId) {
        AniDBFile file = new AniDBFile(fileId);
        file.setAnimeId(20);
        file.setEpisodeId(30);
        file.setDefaultName("Identified File");
        file.setMylistEntryId(321);
        return file;
    }
}
