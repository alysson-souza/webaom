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

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.FileHandler;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.db.DatabaseManager;
import epox.webaom.io.RemoteFileResolver;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.Component;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Robot;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@Tag("ui")
class MainPanelBehaviorTest {
    @TempDir
    Path tempDir;

    private Options originalOptions;
    private FileHandler originalFileHandler;
    private Rules originalRules;
    private JobList originalJobs;
    private Cache originalCache;
    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private MainPanel originalGui;
    private AniDBFileClient originalConnection;
    private boolean originalAutoAdd;
    private String originalPathRegex;
    private String originalFont;
    private String originalLastDirectory;
    private AppContext.AppMode originalMode;
    private boolean originalConnectionShutdown;
    private int originalAnimeTitlePriority;
    private int originalEpisodeTitlePriority;
    private Component originalPrimaryPopupMenu;
    private Component originalSecondaryPopupMenu;
    private Set<ShortcutInfo> originalShortcuts;
    private RemoteFileResolver originalRemoteFiles;

    private DatabaseManager databaseManager;
    private RecordingRuntime runtime;
    private MainPanel panel;

    @BeforeEach
    void setUp() {
        originalOptions = AppContext.opt;
        originalFileHandler = AppContext.fileHandler;
        originalRules = AppContext.rules;
        originalJobs = AppContext.jobs;
        originalCache = AppContext.cache;
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalGui = AppContext.gui;
        originalConnection = AppContext.conn;
        originalAutoAdd = AppContext.autoadd;
        originalPathRegex = AppContext.pathRegex;
        originalFont = AppContext.font;
        originalLastDirectory = AppContext.lastDirectory;
        originalMode = AppContext.getAppMode();
        originalConnectionShutdown = AniDBConnection.isShutdown();
        originalAnimeTitlePriority = Anime.titlePriority;
        originalEpisodeTitlePriority = Episode.titlePriority;
        originalPrimaryPopupMenu = AppContext.primaryPopupMenu;
        originalSecondaryPopupMenu = AppContext.secondaryPopupMenu;
        originalRemoteFiles = AppContext.remoteFiles;
        originalShortcuts = Collections.newSetFromMap(new IdentityHashMap<>());
        originalShortcuts.addAll(AppContext.shortcutRegistry.getAllShortcuts());

        Options options = mock(Options.class);
        when(options.loadFromFile()).thenReturn(false);
        when(options.getString(org.mockito.ArgumentMatchers.anyInt())).thenReturn(null);

        databaseManager = mock(DatabaseManager.class);
        when(databaseManager.isConnected()).thenReturn(false);
        when(databaseManager.getJob(org.mockito.ArgumentMatchers.any(Job.class), anyBoolean()))
                .thenReturn(-1);

        AppContext.opt = options;
        AppContext.fileHandler = new FileHandler();
        AppContext.rules = new Rules();
        AppContext.jobs = new JobList();
        AppContext.cache = new Cache();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = databaseManager;
        AppContext.conn = mock(AniDBFileClient.class);
        AppContext.autoadd = false;
        AppContext.pathRegex = null;
        AppContext.font = "";
        AppContext.lastDirectory = "";
        AppContext.setAppMode(AppContext.AppMode.NORMAL);

        runtime = new RecordingRuntime();
        panel = new MainPanel(runtime);
        AppContext.gui = panel;
        stopTimers(panel);
    }

    @AfterEach
    void tearDown() {
        stopTimers(panel);
        for (ShortcutInfo shortcut : AppContext.shortcutRegistry.getAllShortcuts()) {
            if (!originalShortcuts.contains(shortcut)) {
                AppContext.shortcutRegistry.unregister(shortcut);
            }
        }
        AppContext.opt = originalOptions;
        AppContext.fileHandler = originalFileHandler;
        AppContext.rules = originalRules;
        AppContext.jobs = originalJobs;
        AppContext.cache = originalCache;
        AppContext.jobCounter = originalJobCounter;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.gui = originalGui;
        AppContext.conn = originalConnection;
        AppContext.autoadd = originalAutoAdd;
        AppContext.pathRegex = originalPathRegex;
        AppContext.font = originalFont;
        AppContext.lastDirectory = originalLastDirectory;
        AppContext.setAppMode(originalMode);
        Anime.titlePriority = originalAnimeTitlePriority;
        Episode.titlePriority = originalEpisodeTitlePriority;
        AppContext.primaryPopupMenu = originalPrimaryPopupMenu;
        AppContext.secondaryPopupMenu = originalSecondaryPopupMenu;
        AppContext.remoteFiles = originalRemoteFiles;
        AniDBConnection.setShutdown(originalConnectionShutdown);
    }

    @Test
    void nativeFileListDropKeepsLocalFilesWithoutDesktopServices() throws Exception {
        AppContext.remoteFiles = new RemoteFileResolver("Linux", uri -> {
            throw new AssertionError("Local file drops must not use KIO Fuse");
        });
        File file = Files.writeString(tempDir.resolve("local.mkv"), "video").toFile();
        Transferable transfer = mock(Transferable.class);
        when(transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)).thenReturn(true);
        when(transfer.getTransferData(DataFlavor.javaFileListFlavor)).thenReturn(List.of(file));
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(transfer);
        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        awaitWorker();
        verify(event).dropComplete(true);
        assertEquals(file, AppContext.jobs.array()[0].getFile());
    }

    @Test
    void finderDropWhoseUriListHoldsPlainPathsAddsTheListedFiles() throws Exception {
        File file = Files.writeString(tempDir.resolve("[Group] Show - 01.mkv"), "video")
                .toFile();
        // macOS maps a Finder drop to text/uri-list too, but fills it with plain paths instead of URLs.
        DataFlavor uriList = new DataFlavor("text/uri-list;class=java.lang.String");
        Transferable transfer = mock(Transferable.class);
        when(transfer.getTransferDataFlavors()).thenReturn(new DataFlavor[] {uriList, DataFlavor.javaFileListFlavor});
        when(transfer.isDataFlavorSupported(uriList)).thenReturn(true);
        when(transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)).thenReturn(true);
        when(transfer.getTransferData(uriList)).thenReturn(file.getPath() + "\n");
        when(transfer.getTransferData(DataFlavor.javaFileListFlavor)).thenReturn(List.of(file));
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(transfer);

        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        awaitWorker();

        verify(event).dropComplete(true);
        assertEquals(1, AppContext.jobs.size());
        assertEquals(file, AppContext.jobs.array()[0].getFile());
    }

    @Test
    void emptyDropSaysItHeldNoFiles() throws Exception {
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(NativeFileDropSource.uriList("# nothing was selected\r\n"));

        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        SwingUtilities.invokeAndWait(() -> {});

        verify(event).dropComplete(false);
        assertEquals("The drop contains no files.", panel.statusProgressBar.getString());
    }

    @Test
    void dropOfAPathWithoutAParentRemembersThePathItself() throws Exception {
        File orphan = new File("not-in-any-folder.mkv");
        Transferable transfer = mock(Transferable.class);
        when(transfer.isDataFlavorSupported(DataFlavor.javaFileListFlavor)).thenReturn(true);
        when(transfer.getTransferData(DataFlavor.javaFileListFlavor)).thenReturn(List.of(orphan));
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(transfer);

        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        awaitWorker();

        assertEquals(orphan.getAbsolutePath(), AppContext.lastDirectory);
    }

    @Test
    void idleDiskIoRescanLeavesTheRunningWorkerRegistered() throws Exception {
        Thread busy = new Thread(() -> {});
        panel.workerThread = busy;

        // With nothing to hash, disk I/O rescans the hash directories on a thread of its own.
        panel.toggleDiskIo();
        panel.toggleDiskIo();
        for (Thread thread : Thread.getAllStackTraces().keySet()) {
            if (thread.getName().equals("RecursiveDirectoryScanner")) thread.join(5000);
        }

        assertEquals(busy, panel.workerThread);
    }

    @Test
    void loadAllJobsCheckboxKeepsShowingWhatIsLoadedWhileAnotherWorkerRuns() {
        when(databaseManager.isConnected()).thenReturn(true);
        panel.workerThread = new Thread(() -> {});
        JCheckBox loadAll = panel.altViewPanel.getLoadAllJobsCheckbox();

        loadAll.setSelected(true);
        fire(loadAll);

        assertFalse(loadAll.isSelected());
        verify(databaseManager, never()).getJobs();
    }

    @Test
    void smbDirectoryDropResolvesOffTheUiThreadAndAddsItsChildren() throws Exception {
        Path share = Files.createDirectories(tempDir.resolve("share/Season 1"));
        File file = Files.writeString(share.resolve("episode.mkv"), "video").toFile();
        AtomicBoolean onEdt = new AtomicBoolean(true);
        AppContext.remoteFiles = new RemoteFileResolver("Linux", uri -> {
            onEdt.set(SwingUtilities.isEventDispatchThread());
            return share.getParent().toFile();
        });
        Transferable transfer = NativeFileDropSource.uriList("smb://nas.example/Anime/Season%201/\r\n");
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(transfer);
        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        awaitWorker();

        assertFalse(onEdt.get());
        assertEquals(1, AppContext.jobs.size());
        assertEquals(file, AppContext.jobs.array()[0].getFile());
        assertEquals(
                URI.create("smb://nas.example/Anime/Season%201/episode.mkv"),
                AppContext.jobs.array()[0].getRemoteUri());
    }

    @Test
    void failedRemoteDropReportsErrorAndAllowsTheNextDrop() throws Exception {
        AppContext.remoteFiles = new RemoteFileResolver("Linux", uri -> {
            throw new IOException("KIO Fuse is unavailable. Use a mounted folder.");
        });
        DropTargetDropEvent event = mock(DropTargetDropEvent.class);
        when(event.getTransferable()).thenReturn(NativeFileDropSource.uriList("smb://nas.example/Anime/a.mkv"));
        SwingUtilities.invokeAndWait(() -> panel.drop(event));
        awaitWorker();
        SwingUtilities.invokeAndWait(() -> {});

        assertEquals(0, AppContext.jobs.size());
        assertNull(panel.workerThread);
        assertTrue(panel.statusProgressBar.getString().contains("Use a mounted folder"));
        uriListDropAddsLocalFileWithEscapedCharacters();
    }

    @Test
    void nativeSmbDragAddsFileThroughTheWindow() throws Exception {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        File file = Files.writeString(tempDir.resolve("native.mkv"), "video").toFile();
        CountDownLatch resolved = new CountDownLatch(1);
        AppContext.remoteFiles = new RemoteFileResolver("Linux", uri -> {
            assertFalse(SwingUtilities.isEventDispatchThread());
            resolved.countDown();
            return tempDir.toFile();
        });
        Process source = new ProcessBuilder(
                        Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-cp",
                        Path.of(NativeFileDropSource.class
                                        .getProtectionDomain()
                                        .getCodeSource()
                                        .getLocation()
                                        .toURI())
                                .toString(),
                        NativeFileDropSource.class.getName())
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        JFrame target = new JFrame("WebAOM drop test");
        Point[] points = new Point[2];
        Robot robot = new Robot();
        try {
            java.util.concurrent.FutureTask<String> ready = new java.util.concurrent.FutureTask<>(
                    () -> source.inputReader().readLine());
            Thread.ofVirtual().start(ready);
            String[] coordinates = ready.get(5, TimeUnit.SECONDS).split(",");
            points[0] = new Point(Integer.parseInt(coordinates[0]), Integer.parseInt(coordinates[1]));
            SwingUtilities.invokeAndWait(() -> {
                target.add(panel);
                target.setBounds(260, 20, 1000, 800);
                target.setVisible(true);
                points[1] = panel.getLocationOnScreen();
                points[1].translate(panel.getWidth() / 2, panel.getHeight() - 2);
            });
            robot.waitForIdle();
            robot.mouseMove(points[0].x, points[0].y);
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            for (int step = 1; step <= 30; step++) {
                robot.mouseMove(
                        points[0].x + (points[1].x - points[0].x) * step / 30,
                        points[0].y + (points[1].y - points[0].y) * step / 30);
                robot.delay(30);
            }
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            assertTrue(resolved.await(5, TimeUnit.SECONDS), "Native drop must reach the resolver");
            awaitWorker();
            assertEquals(1, AppContext.jobs.size());
            assertEquals(file, AppContext.jobs.array()[0].getFile());
        } finally {
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            source.destroyForcibly();
            source.waitFor(5, TimeUnit.SECONDS);
            SwingUtilities.invokeAndWait(target::dispose);
        }
    }

    private void awaitWorker() throws InterruptedException {
        Thread worker = panel.workerThread;
        if (worker != null) worker.join(5000);
    }

    @Test
    void uriListDropAddsLocalFileWithEscapedCharacters() throws Exception {
        File file = Files.writeString(tempDir.resolve("episode + 日本語 #1.mkv"), "video")
                .toFile();
        Transferable transfer =
                NativeFileDropSource.uriList("# Files\r\n" + file.toURI().toASCIIString() + "\r\n");
        DropTargetDragEvent drag = mock(DropTargetDragEvent.class);
        DropTargetDropEvent drop = mock(DropTargetDropEvent.class);
        when(drag.getTransferable()).thenReturn(transfer);
        when(drop.getTransferable()).thenReturn(transfer);

        SwingUtilities.invokeAndWait(() -> {
            panel.dragEnter(drag);
            panel.drop(drop);
        });
        awaitWorker();

        verify(drag).acceptDrag(DnDConstants.ACTION_COPY);
        verify(drop).dropComplete(true);
        assertEquals(1, AppContext.jobs.size());
        assertEquals(file, AppContext.jobs.array()[0].getFile());
    }

    @Test
    void publicStatusHyperlinkAndConnectionSurfacesReflectPanelState() throws Exception {
        panel.connectionOptionsPanel.hostTextField.setText("api.example.test");
        panel.connectionOptionsPanel.remotePortTextField.setText("9000");
        panel.connectionOptionsPanel.localPortTextField.setText("9001");
        panel.miscOptionsPanel.browserPathField.setText("browser-command");

        panel.status0("Ready");
        panel.status1("Jobs ready");
        panel.setStatusMessage("Waiting");
        panel.setJobStatusMessage("Two jobs");
        panel.printHash("ed2k hash output");
        panel.updateProgressBar();
        panel.switchToJobsTab();

        assertEquals("api.example.test", panel.getHost());
        assertEquals(9000, panel.getRemotePort());
        assertEquals(9001, panel.getLocalPort());
        assertEquals("Waiting", panel.statusProgressBar.getString());
        assertEquals("Two jobs", panel.jobProgressBar.getString());
        assertTrue(
                privateField(panel, "hashTextArea", JTextArea.class).getText().contains("ed2k hash output"));
        assertEquals(2, privateField(panel, "tabbedPane", JTabbedPane.class).getSelectedIndex());

        panel.hyperlinkUpdate(
                new HyperlinkEvent(panel, HyperlinkEvent.EventType.ENTERED, null, "https://example.test"));
        assertEquals("https://example.test", panel.statusProgressBar.getString());
        panel.hyperlinkUpdate(new HyperlinkEvent(panel, HyperlinkEvent.EventType.EXITED, null, "https://example.test"));
        assertEquals("Waiting", panel.statusProgressBar.getString());
        panel.hyperlinkUpdate(
                new HyperlinkEvent(panel, HyperlinkEvent.EventType.ACTIVATED, null, "https://example.test/open"));

        assertEquals(List.of("browser-command|https://example.test/open"), runtime.openedUrls);
        assertInstanceOf(AniDBFileClient.class, panel.createConnection());
        assertEquals("api.example.test", AppContext.usetup.host);
    }

    @Test
    void optionsSaveAndLoadRoundTripAllChildPanelsAndGlobals() {
        panel.connectionOptionsPanel.hostTextField.setText("host.test");
        panel.connectionOptionsPanel.remotePortTextField.setText("1234");
        panel.connectionOptionsPanel.localPortTextField.setText("4321");
        panel.miscOptionsPanel.hashDirectoriesField.setText(tempDir.toString());
        panel.altViewPanel.getPathRegexField().setText(".*Anime.*");
        AppContext.pathRegex = ".*Anime.*";
        AppContext.font = "Dialog,18";
        AppContext.lastDirectory = tempDir.toString();
        Options saved = new Options();

        panel.saveOptions(saved);

        panel.connectionOptionsPanel.hostTextField.setText("changed");
        AppContext.pathRegex = null;
        AppContext.font = "";
        AppContext.lastDirectory = "";
        panel.loadOptions(saved);

        assertEquals("host.test", panel.connectionOptionsPanel.hostTextField.getText());
        assertEquals(1234, panel.getRemotePort());
        assertEquals(4321, panel.getLocalPort());
        assertEquals(tempDir.toString(), panel.miscOptionsPanel.hashDirectoriesField.getText());
        assertEquals(".*Anime.*", AppContext.pathRegex);
        assertEquals("Dialog,18", AppContext.font);
        assertEquals(tempDir.toString(), AppContext.lastDirectory);
    }

    @Test
    void actionAndChangeHandlersUpdateExtensionsFiltersTitlesAndDatabaseState() throws Exception {
        JTextField extensionField = privateField(panel, "newExtensionTextField", JTextField.class);
        extensionField.setText("customvideo");
        panel.actionPerformed(new ActionEvent(extensionField, ActionEvent.ACTION_PERFORMED, "enter"));
        assertTrue(AppContext.fileHandler.allowedExtensions.includes("customvideo"));
        assertEquals("", extensionField.getText());

        panel.altViewPanel.getPathRegexField().setText(".*Season.*");
        fire(panel.altViewPanel.getPathRegexField());
        assertEquals(".*Season.*", AppContext.pathRegex);
        panel.altViewPanel.getPathRegexField().setText("");
        fire(panel.altViewPanel.getPathRegexField());
        assertNull(AppContext.pathRegex);

        panel.altViewPanel.getAnimeTitleComboBox().setSelectedIndex(1);
        fire(panel.altViewPanel.getAnimeTitleComboBox());
        panel.altViewPanel.getEpisodeTitleComboBox().setSelectedIndex(2);
        fire(panel.altViewPanel.getEpisodeTitleComboBox());
        assertEquals(1, Anime.titlePriority);
        assertEquals(2, Episode.titlePriority);

        JCheckBox autoAdd = privateField(panel, "autoAddToMylistCheckbox", JCheckBox.class);
        autoAdd.setSelected(true);
        panel.stateChanged(new ChangeEvent(autoAdd));
        assertTrue(AppContext.autoadd);
        assertTrue(panel.mylistOptionsPanel.isEnabled());

        when(databaseManager.isConnected()).thenReturn(true);
        panel.altViewPanel.getLoadAllJobsCheckbox().setSelected(true);
        fire(panel.altViewPanel.getLoadAllJobsCheckbox());
        Thread reloader = panel.workerThread;
        if (reloader != null) reloader.join(5000);
        SwingUtilities.invokeAndWait(() -> {});
        verify(databaseManager).setLoadAllJobs(true);
        verify(databaseManager).getJobs();

        fire(panel.miscOptionsPanel.disconnectButton);
        verify(databaseManager).shutdown();
        assertTrue(panel.miscOptionsPanel.databaseUrlField.isEnabled());
        assertFalse(panel.miscOptionsPanel.disconnectButton.isEnabled());
    }

    @Test
    void diskAndNetworkControlsUseInjectedRuntimeWithoutStartingWorkers() throws Exception {
        File source =
                Files.writeString(tempDir.resolve("queued.mkv"), "payload").toFile();
        Job job = new Job(source, Job.HASHWAIT);
        AppContext.jobs.add(job);
        job.updateHealth(Job.H_PAUSED);

        panel.toggleDiskIo();

        assertTrue(panel.isDiskIoOk());
        assertNotNull(panel.diskIoThread);
        assertEquals("DiskIO", runtime.threadNames.getFirst());
        panel.toggleDiskIo();
        assertFalse(panel.isDiskIoOk());

        panel.setNetworkIoEnabled(true);
        assertTrue(privateField(panel, "toolbarButtons", JButton[].class)[4].isEnabled());
        panel.setNetworkIoEnabled(false);
        assertFalse(panel.isNetworkIoOk());

        panel.handleFatalError(true);
        assertTrue(panel.unfreezeTimer.isRunning());
        panel.handleFatalError(false);
        assertFalse(panel.unfreezeTimer.isRunning());
        panel.kill();
        assertTrue(panel.isKilled());
        assertFalse(panel.isDiskIoOk());
        assertFalse(panel.isNetworkIoOk());
    }

    @Test
    void recursiveSelectionAddsOnlyAllowedFilesAndRecordsDirectory() throws Exception {
        Path nested = Files.createDirectories(tempDir.resolve("library/Season 1"));
        Files.writeString(nested.resolve("episode01.mkv"), "one");
        Files.writeString(nested.resolve("episode02.webm"), "two");
        Files.writeString(nested.resolve("notes.txt"), "ignored");

        panel.selectFilesForProcessing(new File[] {tempDir.resolve("library").toFile()});
        Thread scanner = panel.workerThread;
        if (scanner != null) {
            scanner.join(5000);
        }

        assertEquals(2, AppContext.jobs.size());
        assertEquals(tempDir.toAbsolutePath().toString(), AppContext.lastDirectory);
        assertNull(panel.workerThread);
        assertTrue(panel.statusProgressBar.getString().startsWith("Added 2 files in "));
    }

    @Test
    void startupWithoutAutomaticServicesStartsOnlyGuiUpdatesAndShortcuts() {
        JRootPane rootPane = new JRootPane();
        rootPane.setContentPane(panel);

        panel.startup();

        assertTrue(panel.guiUpdateTimer.isRunning());
        assertNull(panel.workerThread);
        assertFalse(panel.isNetworkIoOk());
    }

    @Test
    void resetAndShutdownClearRuntimeStateAndDisconnectAStoppedNetworkWorker() throws Exception {
        Anime anime = new Anime(99);
        anime.episodeCount = 1;
        anime.latestEpisode = 1;
        anime.init();
        AppContext.animeTreeRoot.add(anime);
        File source = Files.writeString(tempDir.resolve("reset.mkv"), "payload").toFile();
        AppContext.jobs.add(new Job(source, Job.FINISHED));
        panel.diskIoThread = new Thread(() -> {}, "completed-disk-worker");

        panel.reset();

        assertEquals(0, AppContext.animeTreeRoot.size());
        assertEquals(0, AppContext.jobs.array().length);
        verify(databaseManager).shutdown();
        assertTrue(panel.miscOptionsPanel.databaseUrlField.isEnabled());

        panel.shutdown();
        panel.networkIoThread = new Thread(() -> {}, "completed-network-worker");
        panel.shutdown();

        assertTrue(AniDBConnection.isShutdown());
        verify(AppContext.conn).disconnect();
        assertFalse(panel.isNetworkIoOk());
    }

    @Test
    void databaseActionInitializesAndDisconnectsAnIsolatedSqliteDatabase() throws Exception {
        Path databasePath = tempDir.resolve("panel.sqlite");
        panel.miscOptionsPanel.databaseUrlField.setText("jdbc:sqlite:" + databasePath);

        fire(panel.miscOptionsPanel.databaseUrlField);
        Thread initializer = panel.workerThread;
        if (initializer != null) {
            initializer.join(5000);
        }

        assertNull(panel.workerThread);
        assertTrue(AppContext.databaseManager.isConnected());
        assertTrue(databasePath.toFile().isFile());
        assertTrue(panel.miscOptionsPanel.disconnectButton.isEnabled());

        fire(panel.miscOptionsPanel.disconnectButton);

        assertFalse(AppContext.databaseManager.isConnected());
        assertTrue(panel.miscOptionsPanel.databaseUrlField.isEnabled());
        assertFalse(panel.miscOptionsPanel.disconnectButton.isEnabled());
    }

    @Test
    void toolbarAndTimerActionsCoverSaveWikiNetworkFailureAndUnfreeze() throws Exception {
        JButton[] buttons = privateField(panel, "toolbarButtons", JButton[].class);

        fire(buttons[5]);
        verify(AppContext.opt).saveToFile();
        fire(buttons[0]);
        assertEquals(1, runtime.openedUrls.size());
        assertTrue(runtime.openedUrls.getFirst().endsWith("|https://wiki.anidb.net/WebAOM"));

        fire(buttons[4]);
        assertFalse(panel.isNetworkIoOk());
        assertTrue(buttons[4].isEnabled());

        panel.handleFatalError(true);
        fire(panel.unfreezeTimer);
        assertFalse(panel.unfreezeTimer.isRunning());
        fire(panel.progressTimer);
        fire(panel.guiUpdateTimer);
    }

    private void fire(Object source) {
        panel.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, "test"));
    }

    private void stopTimers(MainPanel target) {
        if (target == null) {
            return;
        }
        target.diskIoTimer.stop();
        target.progressTimer.stop();
        target.unfreezeTimer.stop();
        target.guiUpdateTimer.stop();
    }

    private <T> T privateField(Object target, String name, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }

    private static final class RecordingRuntime implements MainPanelRuntime {
        private final List<String> threadNames = new ArrayList<>();
        private final List<String> openedUrls = new ArrayList<>();

        @Override
        public Timer createTimer(int delayMs, ActionListener listener) {
            return new Timer(delayMs, listener);
        }

        @Override
        public Thread startBackgroundTask(String threadName, Runnable runnable) {
            threadNames.add(threadName);
            return new Thread(runnable, threadName);
        }

        @Override
        public boolean requestCredentials() {
            return false;
        }

        @Override
        public void openHyperlink(String browserPath, String url) {
            openedUrls.add(browserPath + "|" + url);
        }
    }
}
