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

import epox.swing.JPanelCommand;
import epox.swing.JPanelDebug;
import epox.swing.Log;
import epox.util.StringUtilities;
import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.ChiiEmu;
import epox.webaom.HyperlinkBuilder;
import epox.webaom.Job;
import epox.webaom.JobManager;
import epox.webaom.Options;
import epox.webaom.Parser;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.net.AniDBConnection;
import epox.webaom.net.AniDBConnectionSettings;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.net.Pinger;
import epox.webaom.startup.StartupIssue;
import epox.webaom.startup.StartupValidator;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public class MainPanel extends JPanel
        implements Log, ActionListener, HyperlinkListener, ChangeListener, DropTargetListener {

    private static final Logger LOGGER = Logger.getLogger(MainPanel.class.getName());
    private static final int BUTTON_WIKI = 0;
    private static final int BUTTON_SELECT_FILES = 1;
    private static final int BUTTON_SELECT_DIRS = 2;
    private static final int BUTTON_HASH = 3;
    private static final int BUTTON_CONNECTION = 4;
    private static final int BUTTON_SAVE = 5;
    private static final int BUTTON_EXPORT = 6;
    private static final int BUTTON_IMPORT = 7;
    private static final int BUTTON_COUNT = 8;
    private static final String LABEL_NETWORK_IO_ENABLE = "Login";
    private static final String LABEL_NETWORK_IO_DISABLE = "Log out";
    private static final String LABEL_DISK_IO_ENABLE = "Start";
    private static final String LABEL_DISK_IO_DISABLE = "Stop";
    private final Runnable jobScrollDownRunnable;
    public JProgressBar statusProgressBar;
    public JProgressBar jobProgressBar;
    public RulesOptionsPanel rulesOptionsPanel;
    public ConnectionOptionsPanel connectionOptionsPanel;
    public MiscOptionsPanel miscOptionsPanel;
    public MylistOptionsPanel mylistOptionsPanel;
    public JobsPanel jobsPanel;
    public AlternateViewPanel altViewPanel;
    public Thread diskIoThread;
    public Thread networkIoThread;
    public Thread workerThread;
    protected JTableJobs jobsTable;
    protected JScrollBar jobsScrollBar;
    protected TableModelJobs jobsTableModel;
    protected boolean cancelRecursiveWorker;
    protected final Timer diskIoTimer;
    protected final Timer progressTimer;
    protected final Timer unfreezeTimer;
    protected final Timer guiUpdateTimer;
    private JTextField newExtensionTextField;
    private JEditorPaneLog logEditorPane;
    private JTextArea hashTextArea;
    private JButton[] toolbarButtons;
    private JCheckBox autoAddToMylistCheckbox;
    private JTabbedPane tabbedPane;
    private boolean isKilled;
    private boolean isDiskIoRunning;
    private boolean isNetworkIoRunning;
    private int updateCount = 0;
    private String lastStatusMessage;
    private Border originalJobsBorder;

    public MainPanel() {
        isKilled = false;
        isDiskIoRunning = false;
        isNetworkIoRunning = false;
        initializeComponents();

        jobScrollDownRunnable = new JobScrollDown();
        diskIoTimer = new Timer(4000, this);
        progressTimer = new Timer(1000, this);
        unfreezeTimer = new Timer(1000 * 60 * 30, this);
        guiUpdateTimer = new Timer(500, this);

        progressTimer.start();
        if (AppContext.opt.loadFromFile()) {
            loadOptions(AppContext.opt);
        } else {
            AppContext.fileHandler.loadDefaultExtensions();
            jobsPanel.loadOptions(AppContext.opt); // default hack
        }
        try {
            Thread[] threads = new Thread[Thread.activeCount()];
            Thread.enumerate(threads);
            for (Thread thread : threads) {
                if (thread.getName().equals("AWT-EventQueue-0")) {
                    thread.setName("GUI");
                }
            }
        } catch (Exception ex) {
            /* don't care */
        }
    }

    private static String getButtonName(int buttonIndex) {
        return switch (buttonIndex) {
            case BUTTON_SELECT_FILES -> "Files...";
            case BUTTON_SELECT_DIRS -> "Folders...";
            case BUTTON_HASH -> LABEL_DISK_IO_ENABLE;
            case BUTTON_CONNECTION -> LABEL_NETWORK_IO_ENABLE;
            case BUTTON_SAVE -> "Save opt";
            case BUTTON_WIKI -> "Help!";
            case BUTTON_EXPORT -> "Export";
            case BUTTON_IMPORT -> "Import";
            default -> "No text!";
        };
    }

    private static String getButtonToolTip(int buttonIndex) {
        return switch (buttonIndex) {
            case BUTTON_SELECT_FILES -> "Add files you want to hash";
            case BUTTON_SELECT_DIRS -> "Add folders with files you want to hash";
            case BUTTON_HASH -> "Start/stop the disk operations thread. (Hashing and moving)";
            case BUTTON_CONNECTION -> "Log on / log off the AniDB UDP Service";
            case BUTTON_SAVE -> "Save the options to disk";
            case BUTTON_WIKI -> "Check out the documentation @ AniDB WIKI";
            case BUTTON_EXPORT -> "Export loaded data";
            case BUTTON_IMPORT -> "Import exported data";
            default -> "No help!";
        };
    }

    public void startup() {
        // Validate startup configuration and collect any issues
        List<StartupIssue> startupIssues = StartupValidator.validateStartup(AppContext.opt);

        // Try to start logging if enabled
        if (miscOptionsPanel.isAutoLogEnabled()) {
            String validatedLogPath =
                    StartupValidator.validateLogging(true, miscOptionsPanel.logFilePathField.getText());
            if (validatedLogPath != null) {
                // Use the validated path (which may be a default if original was empty)
                // Disable if successful
                miscOptionsPanel.logFilePathField.setEnabled(
                        !logEditorPane.openLogFile(validatedLogPath)); // Re-enable if failed
            }
            // If validatedLogPath is null, logging was disabled due to validation failure
        }

        // Try to start database if enabled
        if (miscOptionsPanel.isAutoLoadDatabaseEnabled()) {
            startDatabase();
        } else {
            guiUpdateTimer.start();
        }

        // Show startup issues to user if there are any
        if (!startupIssues.isEmpty() && StartupValidator.hasWarningsOrInfo(startupIssues)) {
            showStartupIssuesDialog(startupIssues);
        }

        // Initialize global keyboard shortcuts (after GUI is fully visible)
        new GlobalKeyboardShortcuts(getRootPane(), AppContext.shortcutRegistry);
    }

    /**
     * Show startup issues dialog to inform user about any problems encountered during
     * initialization (non-blocking, app continues to run).
     */
    private void showStartupIssuesDialog(List<StartupIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            return;
        }

        StringBuilder message = new StringBuilder();
        message.append("The following startup issues were detected:\n\n");

        for (StartupIssue issue : issues) {
            message.append("[").append(issue.severity().getDisplayName()).append("] ");
            message.append(issue.title()).append("\n");
            message.append(issue.message());
            if (issue.suggestion() != null && !issue.suggestion().isEmpty()) {
                message.append("\n").append("Suggestion: ").append(issue.suggestion());
            }
            message.append("\n\n");
        }

        // Show as informational dialog (not blocking)
        JOptionPane.showMessageDialog(this, message.toString(), "Startup Information", JOptionPane.INFORMATION_MESSAGE);
    }

    public void shutdown() {
        logEditorPane.closeLogFile();
        if (networkIoThread != null && AppContext.conn != null && AppContext.conn.isLoggedIn()) {
            AniDBConnection.setShutdown(true);
            toolbarButtons[BUTTON_CONNECTION].setEnabled(false); // disable the button
            isNetworkIoRunning = false;
            try {
                networkIoThread.join();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                LOGGER.warning("Interrupted during shutdown: " + ex.getMessage());
            }
        }
    }

    public void reset() {
        synchronized (AppContext.animeTreeRoot) {
            if (isDiskIoRunning) {
                toggleDiskIo();
            }
            setNetworkIoEnabled(false);
            try {
                diskIoThread.join(1000);
            } catch (Exception ex) {
                // Ignore timeout exception
            }
            AppContext.databaseManager.shutdown();
            miscOptionsPanel.databaseUrlField.setEnabled(true);
            AppContext.animeTreeRoot.clear();
            AppContext.jobs.clear();
            AppContext.cache.clear();
            AppContext.jobCounter.reset();
            jobsTableModel.reset();
            jobsTable.updateUI();
            altViewPanel.getAltViewTreeTable().updateUI();
            System.gc();
        }
    }

    private void initializeComponents() {
        /*
         * +---------------------------+
         * | BUTTONS | NORTH/TOP
         * +---------------------------+
         * |RLS|OPT|JOB|ALT|LOG|INF|DEB|
         * | | CENTER
         * | (TABBED PANE) |
         * | |
         * +---------------------------+
         * | PROGRESSBARS! | SOUTH
         * +---------------------------+
         */

        //////////////////////////////// BUTTONS/////////////////////////////////
        toolbarButtons = new JButton[BUTTON_COUNT];
        JPanel buttonsPanel = new JPanel(new GridLayout(1, toolbarButtons.length, 2, 2));
        buttonsPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
        for (int i = 0; i < toolbarButtons.length; i++) {
            toolbarButtons[i] = new JButton(getButtonName(i));
            toolbarButtons[i].addActionListener(this);
            toolbarButtons[i].setToolTipText(getButtonToolTip(i));
            buttonsPanel.add(toolbarButtons[i]);
        }
        //////////////////////////////// OPTIONS/////////////////////////////////
        // FILE OPTION PANEL
        mylistOptionsPanel = new MylistOptionsPanel();

        // EXTENSIONs PANEL
        newExtensionTextField = new JTextField();
        newExtensionTextField.addActionListener(this);

        final JList<String> extensionList = new JList<>(AppContext.fileHandler.allowedExtensions);
        extensionList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "pressed");
        extensionList.getActionMap().put("pressed", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                int[] selectedIndices = extensionList.getSelectedIndices();
                java.util.Arrays.sort(selectedIndices);
                for (int idx = selectedIndices.length - 1; idx >= 0; idx--) {
                    AppContext.fileHandler.removeExtension(selectedIndices[idx]);
                }
                extensionList.clearSelection();
            }
        });

        JPanel extensionsPanel = new JPanel(new BorderLayout());
        extensionsPanel.add(newExtensionTextField, BorderLayout.NORTH);
        extensionsPanel.add(new JScrollPane(extensionList));
        extensionsPanel.setBorder(new TitledBorder("Extensions"));

        // OTHER PANEL
        miscOptionsPanel = new MiscOptionsPanel();
        miscOptionsPanel.databaseUrlField.addActionListener(this);
        miscOptionsPanel.disconnectButton.addActionListener(this);
        miscOptionsPanel.logFilePathField.addActionListener(this);
        JPanel otherPanel = new JPanel(new BorderLayout());
        otherPanel.setBorder(new TitledBorder("Other"));
        otherPanel.add(miscOptionsPanel, BorderLayout.CENTER);

        // CONNECTION OPTIONS PANEL
        connectionOptionsPanel = new ConnectionOptionsPanel();
        connectionOptionsPanel.pingButton.addActionListener(this);
        JPanel connectionPanel = new JPanel(new BorderLayout());
        connectionPanel.setBorder(new TitledBorder("Connection"));
        connectionPanel.add(connectionOptionsPanel, BorderLayout.CENTER);

        // FILE OPTIONS
        autoAddToMylistCheckbox = new JCheckBox("Add files to mylist automatically", true);
        autoAddToMylistCheckbox.addChangeListener(this);
        JPanel mylistPanel = new JPanel(new BorderLayout());
        mylistPanel.setBorder(new TitledBorder("Mylist"));
        mylistPanel.add(autoAddToMylistCheckbox, BorderLayout.NORTH);
        mylistPanel.add(mylistOptionsPanel, BorderLayout.CENTER);

        // THE PANEL
        JPanel optionsPanel = new JPanel(new BorderLayout());
        optionsPanel.setBorder(new EtchedBorder());

        JPanel topOptionsRow = new JPanel(new GridLayout(1, 2, 4, 0));
        JPanel bottomOptionsRow = new JPanel(new BorderLayout());

        optionsPanel.add(topOptionsRow, BorderLayout.NORTH);
        optionsPanel.add(bottomOptionsRow, BorderLayout.CENTER);

        topOptionsRow.add(connectionPanel);
        topOptionsRow.add(mylistPanel);
        bottomOptionsRow.add(otherPanel, BorderLayout.CENTER);
        extensionsPanel.setPreferredSize(new Dimension(100, 100));
        bottomOptionsRow.add(extensionsPanel, BorderLayout.EAST);

        ////////////////////////////// HTML LOG PANE/////////////////////////////
        logEditorPane = new JEditorPaneLog();
        logEditorPane.addHyperlinkListener(this);

        JScrollPane logScrollPane = new JScrollPane(logEditorPane);
        jobsScrollBar = logScrollPane.getVerticalScrollBar();

        //////////////////////////////// INFO PANE///////////////////////////////
        JTextArea infoTextArea = new JTextArea(AppContext.getFileString("info.txt"));
        infoTextArea.setEditable(false);
        infoTextArea.setMargin(new Insets(2, 2, 2, 2));
        enableFileDrop(infoTextArea);

        //////////////////////////////// HASH PANE///////////////////////////////
        hashTextArea = new JTextArea();
        hashTextArea.setLineWrap(true);
        hashTextArea.setWrapStyleWord(true);
        enableFileDrop(hashTextArea);

        /////////////////////////////// RULES PANE///////////////////////////////
        rulesOptionsPanel = new RulesOptionsPanel(AppContext.rules);

        //////////////////////////////// JOBS PANE///////////////////////////////
        jobsTableModel = new TableModelJobs(AppContext.jobs);
        AppContext.jobs.tableModel = jobsTableModel;
        jobsTable = new JTableJobs(jobsTableModel);
        TableModelJobs.formatTable(jobsTable);
        jobsPanel = new JobsPanel(jobsTable, jobsTableModel);

        //////////////////////////////// ALT VIEW////////////////////////////////
        altViewPanel = new AlternateViewPanel(this);

        ////////////////////////////// CHII EMULATOR/////////////////////////////
        ChiiEmu chiiEmulator = new ChiiEmu(AppContext.conn);
        JPanelCommand commandPanel = new JPanelCommand(chiiEmulator, """
            Chii Emulator - AniDB IRC bot commands
            Commands: !uptime !mystats !anime !group !randomanime !mylist !state !watched !storage !font
            Raw API: Start with '?' (e.g. ?PING) - session is added automatically.
            """);
        chiiEmulator.setLog(commandPanel);
        commandPanel.setFileDropListener(this);

        ////////////////////////////// DEBUG PANEL///////////////////////////////
        JPanelDebug debugPanel = new JPanelDebug(null);
        debugPanel.setFileDropListener(this);

        ////////////////////////////// TABBED PANE///////////////////////////////
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Rules", rulesOptionsPanel);
        tabbedPane.addTab("Options", optionsPanel);
        tabbedPane.addTab("Jobs", jobsPanel);
        tabbedPane.addTab("Alt", altViewPanel);
        tabbedPane.addTab("Log", logScrollPane);
        tabbedPane.addTab("Hash", new JScrollPane(hashTextArea));
        tabbedPane.addTab("Info", new JScrollPane(infoTextArea));
        tabbedPane.addTab("Debug", debugPanel);
        tabbedPane.addTab("Chii Emu", commandPanel);
        tabbedPane.setSelectedIndex(6);

        ////////////////////////////// PROGRESSBARS//////////////////////////////
        statusProgressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 1000);
        statusProgressBar.setString("Welcome to WebAOM!");
        statusProgressBar.setStringPainted(true);
        jobProgressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 1000);
        jobProgressBar.setString(AppContext.VERSION);
        jobProgressBar.setStringPainted(true);
        JPanel progressPanel = new JPanel(new GridLayout(2, 1));
        progressPanel.add(statusProgressBar);
        progressPanel.add(jobProgressBar);
        ////////////////////////////////// MAIN//////////////////////////////////
        setLayout(new BorderLayout());
        add(buttonsPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(progressPanel, BorderLayout.SOUTH);
        ////////////////////////////////// END///////////////////////////////////

        // Enable drag-and-drop for the entire panel
        new DropTarget(this, this);
    }

    /*
     * public void updateJobTable(Job j){
     * //jobsTableModel.updateRow(j);
     * }
     */
    public void setDiskIoOptionsEnabled(boolean enabled) {
        miscOptionsPanel.setEnabled(enabled);
    }

    public void setNetworkIoOptionsEnabled(boolean enabled) {
        if (!isKilled()) {
            connectionOptionsPanel.setEnabled(enabled);
            autoAddToMylistCheckbox.setEnabled(enabled);
            mylistOptionsPanel.setEnabled(enabled && AppContext.autoadd);
        }
    }

    public void setNetworkIoEnabled(boolean enabled) {
        if (!isKilled()) {
            if (enabled) {
                toolbarButtons[BUTTON_CONNECTION].setText(LABEL_NETWORK_IO_DISABLE);
            } else {
                toolbarButtons[BUTTON_CONNECTION].setText(LABEL_NETWORK_IO_ENABLE);
                isNetworkIoRunning = false;
            }
            toolbarButtons[BUTTON_CONNECTION].setEnabled(true);
        }
    }

    public void handleFatalError(boolean isFatal) {
        if (isFatal) {
            if (isDiskIoRunning) {
                toggleDiskIo();
            }
            setDiskIoOptionsEnabled(false);
            setNetworkIoOptionsEnabled(false);
            for (int i = 1; i < toolbarButtons.length; i++) {
                toolbarButtons[i].setEnabled(false);
            }
            unfreezeTimer.start();
        } else {
            unfreezeTimer.stop();
            setDiskIoOptionsEnabled(true);
            setNetworkIoOptionsEnabled(true);
            for (int i = 1; i < toolbarButtons.length; i++) {
                toolbarButtons[i].setEnabled(true);
            }
        }
    }

    public void switchToJobsTab() {
        if (tabbedPane != null) {
            tabbedPane.setSelectedIndex(2);
        }
    }

    public String getHost() {
        return connectionOptionsPanel.hostTextField.getText();
    }

    public int getRemotePort() {
        return Integer.parseInt(connectionOptionsPanel.remotePortTextField.getText());
    }

    public int getLocalPort() {
        return Integer.parseInt(connectionOptionsPanel.localPortTextField.getText());
    }

    ///////////////////////////// IMPLEMENTATIONS////////////////////////////////
    @Override
    public void hyperlinkUpdate(HyperlinkEvent event) {
        if (event.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            openHyperlink(event.getDescription());
        } else if (event.getEventType() == HyperlinkEvent.EventType.ENTERED) {
            lastStatusMessage = statusProgressBar.getString();
            statusProgressBar.setString(event.getDescription());
        } else if (event.getEventType() == HyperlinkEvent.EventType.EXITED) {
            statusProgressBar.setString(lastStatusMessage);
        }
    }

    @Override
    public void actionPerformed(ActionEvent event) {
        Object source = event.getSource();
        if (source == toolbarButtons[BUTTON_SELECT_FILES]) {
            selectFiles();
        } else if (source == toolbarButtons[BUTTON_SELECT_DIRS]) {
            selectDirectories();
        } else if (source == toolbarButtons[BUTTON_HASH]) {
            toggleDiskIo();
        } else if (source == toolbarButtons[BUTTON_CONNECTION]) {
            toggleNetworkIo();
        } else if (source == toolbarButtons[BUTTON_EXPORT]) {
            if (workerThread != null) {
                return;
            }
            workerThread = new ExportImportThread(false);
            workerThread.start();
        } else if (source == toolbarButtons[BUTTON_IMPORT]) {
            if (workerThread != null) {
                return;
            }
            workerThread = new ExportImportThread(true);
            workerThread.start();
        } else if (source == toolbarButtons[BUTTON_SAVE]) {
            saveOptions(AppContext.opt);
            AppContext.opt.saveToFile();
        } else if (source == toolbarButtons[BUTTON_WIKI]) {
            openHyperlink("https://wiki.anidb.net/WebAOM");
        } else if (source == diskIoTimer) {
            startDiskIo();
        } else if (source == progressTimer) {
            // System.err.println(A.frame.getFocusOwner());
            updateProgressBar();
        } else if (source == unfreezeTimer) {
            isKilled = false;
            handleFatalError(false);
        } else if (source == guiUpdateTimer) {
            if (tabbedPane.getSelectedComponent() == jobsPanel) {
                jobsPanel.update();
            }
        } else if (source == connectionOptionsPanel.pingButton) {
            new Pinger(this);
        } else if (source == miscOptionsPanel.databaseUrlField) {
            startDatabase();
        } else if (source == miscOptionsPanel.disconnectButton) {
            disconnectDatabase();
        } else if (source == newExtensionTextField) {
            AppContext.fileHandler.addExtension(newExtensionTextField.getText());
            newExtensionTextField.setText("");
        } else if (source == miscOptionsPanel.logFilePathField) {
            startLogging();
        } else if (source == altViewPanel.getSortModeComboBox()) {
            Cache.setTreeSortMode(altViewPanel.getSortModeComboBox().getSelectedIndex());
            altViewPanel.updateAlternativeView(true);
        } else if (source == altViewPanel.getFileVisibilityComboBox()) {
            Cache.setHideNew(altViewPanel.getFileVisibilityComboBox().getSelectedIndex() == 1);
            Cache.setHideExisting(altViewPanel.getFileVisibilityComboBox().getSelectedIndex() == 2);
            altViewPanel.updateAlternativeView(true);
        } else if (source == altViewPanel.getPathRegexField()) {
            String regexPattern = altViewPanel.getPathRegexField().getText();
            if (regexPattern.isEmpty()) {
                AppContext.pathRegex = null;
            } else {
                AppContext.pathRegex = regexPattern;
            }
            altViewPanel.updateAlternativeView(true);
        } else if (source == altViewPanel.getAnimeTitleComboBox()) {
            Anime.titlePriority = altViewPanel.getAnimeTitleComboBox().getSelectedIndex();
            altViewPanel.updateAlternativeView(false);
        } else if (source == altViewPanel.getEpisodeTitleComboBox()) {
            Episode.titlePriority = altViewPanel.getEpisodeTitleComboBox().getSelectedIndex();
            altViewPanel.updateAlternativeView(false);
        } else if (source == altViewPanel.getLoadAllJobsCheckbox()) {
            reloadJobs();
        }
    }

    private void startLogging() {
        if (logEditorPane.openLogFile(miscOptionsPanel.logFilePathField.getText())) {
            miscOptionsPanel.logFilePathField.setEnabled(false);
        }
    }

    private void startDatabase() {
        if (AppContext.databaseManager.isConnected() || workerThread != null) {
            return;
        }
        workerThread = new DatabaseInitThread(miscOptionsPanel.databaseUrlField);
        workerThread.start();
    }

    private void reloadJobs() {
        if (!AppContext.databaseManager.isConnected()) {
            return;
        }
        boolean loadAll = altViewPanel.getLoadAllJobsCheckbox().isSelected();
        AppContext.databaseManager.setLoadAllJobs(loadAll);
        AppContext.jobs.clear();
        AppContext.cache.clear();
        AppContext.databaseManager.getJobs();
        // Gather anime/episode/group info for each job to populate the tree
        for (Job job : AppContext.jobs.array()) {
            AppContext.cache.gatherInfo(job, false);
        }
        altViewPanel.updateAlternativeView(true);
        println("Reloaded " + AppContext.jobs.size() + " jobs" + (loadAll ? " (including finished)" : "") + ".");
    }

    private void disconnectDatabase() {
        if (!AppContext.databaseManager.isConnected()) {
            return;
        }
        AppContext.databaseManager.shutdown();
        AppContext.jobs.clear();
        AppContext.cache.clear();
        altViewPanel.updateAlternativeView(true);
        miscOptionsPanel.databaseUrlField.setEnabled(true);
        miscOptionsPanel.disconnectButton.setEnabled(false);
        println("Disconnected from database.");
    }

    @Override
    public void stateChanged(ChangeEvent event) {
        Object source = event.getSource();
        if (source == autoAddToMylistCheckbox) {
            AppContext.autoadd = autoAddToMylistCheckbox.isSelected();
            mylistOptionsPanel.setEnabled(AppContext.autoadd);
        }
    }

    ///////////////////////////////// DIV METH///////////////////////////////////
    public void toggleDiskIo() {
        isDiskIoRunning = !isDiskIoRunning;
        if (isDiskIoRunning) {
            toolbarButtons[BUTTON_HASH].setText(LABEL_DISK_IO_DISABLE);
            startDiskIo();
            diskIoTimer.start();
        } else {
            toolbarButtons[BUTTON_HASH].setText(LABEL_DISK_IO_ENABLE);
            diskIoTimer.stop();
        }
    }

    private void startDiskIo() {
        if (diskIoThread == null) {
            if (!AppContext.jobs.workForDio()) {
                Thread recursiveWorker = new RecursiveDirectoryScanner(miscOptionsPanel.getHashDirectories(), true);
                recursiveWorker.start();
                Thread.yield();
                if (!AppContext.jobs.workForDio()) {
                    return;
                }
            }
            diskIoThread = new Thread(AppContext.dio, "DiskIO");
            diskIoThread.start();
        }
    }

    /*
     * public void stopDiskIo(){
     * isDiskIoRunning = false;
     * }
     */
    private void toggleNetworkIo() {
        toolbarButtons[BUTTON_CONNECTION].setEnabled(false); // disable the button
        isNetworkIoRunning = !isNetworkIoRunning; // change the state
        if (isNetworkIoRunning) {
            // we want it to run
            setNetworkIoOptionsEnabled(false); // freeze settings
            isNetworkIoRunning = startNetworkIoInternal(); // try to start
            if (!isNetworkIoRunning) {
                // failed
                toolbarButtons[BUTTON_CONNECTION].setEnabled(true); // enable the button
                setNetworkIoOptionsEnabled(true); // unfreeze settings
            }
        }
    }

    private boolean startNetworkIoInternal() {
        if (!isKilled() && networkIoThread == null) {
            // if(A.userPass.username==null||A.userPass.password==null||A.userPass.apiKey==null)
            if (new JDialogLogin().getPass() == null) {
                return false;
            }
            // A.conn = getConnection();
            networkIoThread = new Thread(AppContext.nio, "NetIO");
            networkIoThread.start();
            return true;
        }
        return false;
    }

    public void kill() {
        isKilled = true;
    }

    public boolean isDiskIoOk() {
        return isDiskIoRunning && !isKilled;
    }

    public boolean isNetworkIoOk() {
        return isNetworkIoRunning && !isKilled;
    }

    public boolean isKilled() {
        return isKilled;
    }

    public void showMessage(String msg) {
        AppContext.dialog("Message", msg);
    }

    public void showMessage(String title, String msg) {
        JOptionPane.showMessageDialog(AppContext.component, msg, title, JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void println(Object message) {
        logEditorPane.println(message.toString());
        if (logEditorPane.isVisible()) {
            javax.swing.SwingUtilities.invokeLater(jobScrollDownRunnable);
        }
    }

    /** Sets the status message in the main status bar. Required by Log interface. */
    @Override
    public void status0(String str) {
        statusProgressBar.setString(str);
        lastStatusMessage = str;
    }

    /** Sets the status message in the job progress bar. Required by Log interface. */
    @Override
    public void status1(String str) {
        jobProgressBar.setString(str);
    }

    /** Alias for status0 - sets the status message in the main status bar. */
    public void setStatusMessage(String str) {
        status0(str);
    }

    /** Alias for status1 - sets the status message in the job progress bar. */
    public void setJobStatusMessage(String str) {
        status1(str);
    }

    public void printHash(String msg) {
        hashTextArea.append(msg + "\r\n");
    }

    public void updateProgressBar() {
        jobProgressBar.setValue(AppContext.jobCounter.getProgress());
        if (AppContext.frame != null) {
            AppContext.frame.setTitle("WebAOM " + AppContext.VERSION + " " + AppContext.jobCounter.getStatus());
        }
        if (((updateCount++) % 10) == 0) {
            System.gc();
        }
    }

    public void openHyperlink(String url) {
        try {
            StringUtilities.out(url);
            String path = miscOptionsPanel.browserPathField.getText();
            if (!path.isEmpty()) {
                Runtime.getRuntime().exec(new String[] {path, url});
            } else if (java.awt.Desktop.isDesktopSupported()
                    && java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
                java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
            } else {
                // Fallback for systems without Desktop support
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("mac")) {
                    Runtime.getRuntime().exec(new String[] {"open", url});
                } else if (os.contains("win")) {
                    Runtime.getRuntime().exec(new String[] {
                        "rundll32", "url.dll,FileProtocolHandler", url,
                    });
                } else {
                    Runtime.getRuntime().exec(new String[] {"xdg-open", url});
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public AniDBFileClient createConnection() {
        AppContext.usetup = new AniDBConnectionSettings(
                getHost(),
                getRemotePort(),
                getLocalPort(),
                connectionOptionsPanel.getTimeout(),
                connectionOptionsPanel.getDelayMillis(),
                3,
                connectionOptionsPanel.isNatKeepAliveEnabled());
        AniDBFileClient connection = new AniDBFileClient(this, AppContext.usetup);
        connection.set(AppContext.userPass.username, AppContext.userPass.password, AppContext.userPass.apiKey);
        return connection;
    }

    /////////////////////////////////// OPTIONS//////////////////////////////////
    public void saveOptions(Options options) {
        options.setBoolean(Options.BOOL_ADD_FILE, autoAddToMylistCheckbox.isSelected());
        options.setBoolean(
                Options.BOOL_LOAD_ALL_JOBS,
                altViewPanel.getLoadAllJobsCheckbox().isSelected());
        options.setString(Options.STR_HTML_COLORS, HyperlinkBuilder.encodeColors());
        options.setString(Options.STR_USERNAME, AppContext.userPass.get(miscOptionsPanel.isStorePasswordEnabled()));

        mylistOptionsPanel.saveToOptions(options);
        AppContext.fileHandler.saveOptions(options);
        connectionOptionsPanel.saveOptions(options);
        miscOptionsPanel.saveToOptions(options);
        AppContext.rules.saveToOptions(options);
        jobsPanel.saveOptions(options);

        options.setString(Options.STR_PATH_REGEX, AppContext.pathRegex);
        options.setString(Options.STR_FONT, AppContext.font);
        options.setString(Options.STR_LOG_HEADER, JEditorPaneLog.htmlHeader);
        options.setString(Options.STR_LAST_DIRECTORY, AppContext.lastDirectory);
    }

    public void loadOptions(Options options) {
        try {
            autoAddToMylistCheckbox.setSelected(options.getBoolean(Options.BOOL_ADD_FILE));
            altViewPanel.getLoadAllJobsCheckbox().setSelected(options.getBoolean(Options.BOOL_LOAD_ALL_JOBS));
            HyperlinkBuilder.decodeColors(options.getString(Options.STR_HTML_COLORS));
            AppContext.userPass.set(options.getString(Options.STR_USERNAME));
            mylistOptionsPanel.loadFromOptions(options);
            AppContext.fileHandler.loadOptions(options);
            connectionOptionsPanel.loadOptions(options);
            miscOptionsPanel.loadFromOptions(options);
            AppContext.rules.loadFromOptions(options);
            jobsPanel.loadOptions(options);
            rulesOptionsPanel.updateRules();

            String pathRegex = options.getString(Options.STR_PATH_REGEX);
            if (!pathRegex.isEmpty()) {
                AppContext.pathRegex = pathRegex;
                altViewPanel.getPathRegexField().setText(pathRegex);
            }
            AppContext.font = options.getString(Options.STR_FONT);
            logEditorPane.setHeader(options.getString(Options.STR_LOG_HEADER));
            String lastDir = options.getString(Options.STR_LAST_DIRECTORY);
            if (lastDir != null && !lastDir.isEmpty()) {
                AppContext.lastDirectory = lastDir;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("! Options file is outdated. Could not load.");
            println("Options file is outdated. Could not load.");
        }
    }

    ///////////////////////////////// ADD FILES//////////////////////////////////
    private void selectFiles() {
        cancelRecursiveWorker = true;
        if (workerThread != null) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(AppContext.fileHandler.createExtensionFilter());
        fileChooser.setMultiSelectionEnabled(true);
        if (AppContext.lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(AppContext.lastDirectory));
        }
        int option = fileChooser.showDialog(AppContext.component, "Select File(s)");
        if (option == JFileChooser.APPROVE_OPTION) {
            selectFilesForProcessing(fileChooser.getSelectedFiles());
        } else {
            AppContext.lastDirectory = fileChooser.getCurrentDirectory().getAbsolutePath();
        }
    }

    private void selectDirectories() {
        cancelRecursiveWorker = true;
        if (workerThread != null) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setMultiSelectionEnabled(true);
        if (AppContext.lastDirectory != null) {
            fileChooser.setCurrentDirectory(new File(AppContext.lastDirectory));
        }
        int option = fileChooser.showDialog(AppContext.component, "Select Directory(ies) (recursive)");
        if (option == JFileChooser.APPROVE_OPTION) {
            selectFilesForProcessing(fileChooser.getSelectedFiles());
        } else {
            AppContext.lastDirectory = fileChooser.getCurrentDirectory().getAbsolutePath();
        }
    }

    public void selectFilesForProcessing(File[] files) {
        if (workerThread != null) {
            AppContext.dialog("Message", "There is already a thread like this running.");
            return;
        }
        if (files.length <= 0) {
            return;
        }
        if (files[0].getParent() != null) {
            AppContext.lastDirectory = files[0].getParent();
        } else {
            AppContext.lastDirectory = files[0].getAbsolutePath();
        }
        switchToJobsTab();
        workerThread = new RecursiveDirectoryScanner(files, false);
        workerThread.start();
    }

    private void clearHighlight() {
        if (jobsPanel != null) {
            jobsPanel.setBorder(Objects.requireNonNullElseGet(originalJobsBorder, EtchedBorder::new));
            jobsPanel.repaint();
        }
    }

    /**
     * Enables file drag-and-drop on a component by setting a DropTarget that delegates to this panel's drop handling.
     * This is needed for text components like JTextArea which have their own TransferHandler that blocks file drops.
     */
    private void enableFileDrop(java.awt.Component component) {
        new DropTarget(component, this);
    }

    @Override
    public void dragEnter(DropTargetDragEvent dragEvent) {
        if (dragEvent.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            dragEvent.acceptDrag(DnDConstants.ACTION_COPY);

            if (jobsPanel != null) {
                if (originalJobsBorder == null) {
                    originalJobsBorder = jobsPanel.getBorder();
                }
                jobsPanel.setBorder(new LineBorder(Color.BLUE, 1));
            }
        } else {
            dragEvent.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dragEvent) {
        // No action needed
    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dragEvent) {
        // No action needed
    }

    @Override
    public void dragExit(DropTargetEvent dragExitEvent) {
        clearHighlight();
    }

    @Override
    public void drop(DropTargetDropEvent dropEvent) {
        try {
            Transferable transferable = dropEvent.getTransferable();
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                dropEvent.acceptDrop(DnDConstants.ACTION_COPY);
                @SuppressWarnings("unchecked")
                java.util.List<File> fileList =
                        (java.util.List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                File[] files = fileList.toArray(new File[0]);
                java.util.Arrays.sort(files);
                selectFilesForProcessing(files);
                dropEvent.dropComplete(true);
                clearHighlight();
                if (jobsPanel != null) {
                    jobsPanel.repaint();
                }
            } else {
                dropEvent.rejectDrop();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            dropEvent.rejectDrop();
        }
    }

    protected class JobScrollDown implements Runnable {

        @Override
        public void run() {
            if (!jobsScrollBar.getValueIsAdjusting()) {
                jobsScrollBar.setValue(jobsScrollBar.getMaximum());
            }
        }
    }

    /** Thread that recursively scans directories for files to process. */
    private class RecursiveDirectoryScanner extends Thread {

        private final File[] directories;
        private final boolean shouldPrintStatus;
        private int directoryCount = 0;

        public RecursiveDirectoryScanner(File[] directories, boolean isHiddenDir) {
            super("RecursiveDirectoryScanner");
            this.directories = directories;
            cancelRecursiveWorker = false;
            shouldPrintStatus = !isHiddenDir && diskIoThread == null;
        }

        @Override
        public void run() {
            long startTime = System.currentTimeMillis();
            setEnabled(false);
            int fileCount = 0;
            for (File directory : directories) {
                String parent = directory.getParent();
                if (parent != null && parent.startsWith("\\\\")) {
                    showMessage("Windows network paths not supported: " + directory);
                } else {
                    fileCount += addFileRecursive(directory);
                }
            }
            if (shouldPrintStatus) {
                String statusMessage = "";
                if (fileCount == 1) {
                    statusMessage = "Added one file in " + (System.currentTimeMillis() - startTime) + " ms.";
                } else if (fileCount > 1) {
                    statusMessage =
                            "Added " + fileCount + " files in " + (System.currentTimeMillis() - startTime) + " ms.";
                }
                setStatusMessage(statusMessage);
                if (!statusMessage.isEmpty()) {
                    println(statusMessage);
                }
            }
            setEnabled(true);
            Thread.yield();
            workerThread = null;
        }

        private int addFileRecursive(File file) {
            if (cancelRecursiveWorker) {
                return 0;
            }
            if (file.isDirectory()) {
                if (shouldPrintStatus && (directoryCount++) % 100 == 0) {
                    setStatusMessage("Checking: " + file);
                }
                int fileCount = 0;
                File[] files = file.listFiles(AppContext.fileHandler.createFileFilter());
                if (files == null) {
                    return 0;
                }
                for (File value : files) {
                    fileCount += addFileRecursive(value);
                }
                return fileCount;
            }
            if (AppContext.fileHandler.addFile(file)) {
                return 1;
            }
            return 0;
        }
    }

    /** Thread that initializes the database and loads existing jobs. */
    private class DatabaseInitThread extends Thread {

        private final JTextField databasePathField;

        public DatabaseInitThread(JTextField databasePathField) {
            super("DatabaseInitThread");
            this.databasePathField = databasePathField;
        }

        @Override
        public void run() {
            guiUpdateTimer.stop();
            databasePathField.setEnabled(false);

            // Create the appropriate database manager for the connection string
            String connectionString = databasePathField.getText();
            if (connectionString == null || connectionString.isEmpty()) {
                connectionString = epox.webaom.db.DatabaseManagerFactory.getEmbeddedConnectionString();
            }
            AppContext.databaseManager = epox.webaom.db.DatabaseManagerFactory.create(connectionString);

            // Set load all jobs based on checkbox
            boolean loadCompleted = altViewPanel.getLoadAllJobsCheckbox().isSelected();
            AppContext.databaseManager.setLoadAllJobs(loadCompleted);

            if (AppContext.databaseManager.initialize(connectionString)) {
                long startTime = System.currentTimeMillis();
                // A.mem3 = A.getUsed();
                AppContext.databaseManager.getJobs();
                Object[] jobArray = AppContext.jobs.array();
                Job job;

                // A.mem4 = A.getUsed();

                AppContext.databaseManager.setDebug(false);
                for (Object o : jobArray) {
                    job = (Job) o;
                    job.isFresh = false;
                    AppContext.cache.gatherInfo(job, false);
                    if (job.getStatus() == Job.MOVEWAIT) {
                        JobManager.updatePath(job);
                    }
                }
                int elapsedMs = (int) (System.currentTimeMillis() - startTime);
                println("Loaded db in " + HyperlinkBuilder.formatAsNumber(elapsedMs)
                        + " ms. "
                        + HyperlinkBuilder.formatAsNumber(AppContext.jobs.size())
                        + " files found.");
                altViewPanel.updateAlternativeView(true);
                miscOptionsPanel.disconnectButton.setEnabled(true);
            } else {
                databasePathField.setEnabled(true);
            }
            workerThread = null;

            guiUpdateTimer.start();
        }
    }

    /** Thread that handles database export/import operations. */
    private class ExportImportThread extends Thread {

        private boolean isImport = true;

        public ExportImportThread(boolean isImport) {
            super("ExportImportThread");
            this.isImport = isImport;
        }

        @Override
        public void run() {
            try {
                if (isImport) {
                    Parser.importDB();
                    altViewPanel.updateAlternativeView(true);
                } else {
                    Parser.exportDB();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            workerThread = null;
        }
    }
}
