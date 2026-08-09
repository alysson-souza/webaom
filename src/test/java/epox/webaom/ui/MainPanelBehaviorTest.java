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
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.ui.shortcuts.ShortcutInfo;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JRootPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.HyperlinkEvent;
import org.junit.jupiter.api.AfterEach;
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
        AniDBConnection.setShutdown(originalConnectionShutdown);
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
