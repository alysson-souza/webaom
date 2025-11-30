// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 22.01.05
 *
 * @version 	1.15, 1.09, 1.07, 1.06, 1.05, 1.03, 1.00 (WebAOMJPanel)
 * @author 		epoximator
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.DefaultKeyboardFocusManager;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
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
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

public class MainPanel extends JPanel
		implements
			Log,
			ActionListener,
			HyperlinkListener,
			ChangeListener,
			DropTargetListener {
	protected JTableJobs jobsTable;
	protected JScrollBar jobsScrollBar;
	protected TableModelJobs jobsTableModel;

	private JTextField newExtensionTextField;
	private JEditorPaneLog logEditorPane;
	private JTextArea hashTextArea;
	private JButton[] toolbarButtons;
	private JCheckBox autoAddToMylistCheckbox;
	private JTabbedPane tabbedPane;

	public JProgressBar statusProgressBar;
	public JProgressBar jobProgressBar;
	public RulesOptionsPanel rulesOptionsPanel;
	public ConnectionOptionsPanel connectionOptionsPanel;
	public MiscOptionsPanel miscOptionsPanel;
	public MylistOptionsPanel mylistOptionsPanel;
	public JobsPanel jobsPanel;
	public AlternateViewPanel altViewPanel;

	protected boolean cancelRecursiveWorker;
	private boolean isKilled;
	private boolean isDiskIoRunning;
	private boolean isNetworkIoRunning;
	private int updateCount = 0;
	private String lastStatusMessage;
	private final Runnable jobScrollDownRunnable;
	private Border originalJobsBorder;
	protected Timer diskIoTimer;
	protected Timer progressTimer;
	protected Timer unfreezeTimer;
	protected Timer guiUpdateTimer;
	public Thread diskIoThread;
	public Thread networkIoThread;
	public Thread workerThread;

	public MainPanel() {
		isKilled = isDiskIoRunning = isNetworkIoRunning = false;
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
			AppContext.fha.addExtension("3gp");
			AppContext.fha.addExtension("asf");
			AppContext.fha.addExtension("avi");
			AppContext.fha.addExtension("dat");
			AppContext.fha.addExtension("divx");
			AppContext.fha.addExtension("f4v");
			AppContext.fha.addExtension("flv");
			AppContext.fha.addExtension("m2ts");
			AppContext.fha.addExtension("m2v");
			AppContext.fha.addExtension("m4v");
			AppContext.fha.addExtension("mkv");
			AppContext.fha.addExtension("mov");
			AppContext.fha.addExtension("mp4");
			AppContext.fha.addExtension("mpeg");
			AppContext.fha.addExtension("mpg");
			AppContext.fha.addExtension("mts");
			AppContext.fha.addExtension("ogm");
			AppContext.fha.addExtension("ogv");
			AppContext.fha.addExtension("qt");
			AppContext.fha.addExtension("ram");
			AppContext.fha.addExtension("rm");
			AppContext.fha.addExtension("rmvb");
			AppContext.fha.addExtension("ts");
			AppContext.fha.addExtension("vob");
			AppContext.fha.addExtension("webm");
			AppContext.fha.addExtension("wmv");
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

	public void startup() {
		// Validate startup configuration and collect any issues
		List<StartupIssue> startupIssues = StartupValidator.validateStartup(AppContext.opt);

		// Try to start logging if enabled
		if (miscOptionsPanel.isAutoLogEnabled()) {
			String validatedLogPath = StartupValidator.validateLogging(true,
					miscOptionsPanel.logFilePathField.getText());
			if (validatedLogPath != null) {
				// Use the validated path (which may be a default if original was empty)
				// Disable if successful
				miscOptionsPanel.logFilePathField.setEnabled(!logEditorPane.openLogFile(validatedLogPath)); // Re-enable if failed
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
		if (networkIoThread != null && AppContext.conn != null && AppContext.conn.authenticated) {
			AniDBConnection.shutdown = true;
			toolbarButtons[BUTTON_CONNECTION].setEnabled(false); // disable the button
			isNetworkIoRunning = false;
			try {
				networkIoThread.join();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
		}
	}

	public void reset() {
		synchronized (AppContext.p) {
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
			AppContext.p.clear();
			AppContext.jobs.clear();
			AppContext.cache.clear();
			AppContext.jobc.reset();
			jobsTableModel.reset();
			jobsTable.updateUI();
			altViewPanel.altViewTreeTable.updateUI();
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

		@SuppressWarnings("unchecked") // UniqueStringList doesn't have generics
		final JList<String> extensionList = new JList<String>(AppContext.fha.allowedExtensions);
		extensionList.getInputMap().put(KeyStroke.getKeyStroke("DELETE"), "pressed");
		extensionList.getActionMap().put("pressed", new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				int[] selectedIndices = extensionList.getSelectedIndices();
				java.util.Arrays.sort(selectedIndices);
				for (int idx = selectedIndices.length - 1; idx >= 0; idx--) {
					AppContext.fha.removeExtension(selectedIndices[idx]);
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
		JPanel optionsPanel = new JPanel(new GridLayout(2, 1, 0, 0));
		optionsPanel.setBorder(new EtchedBorder());

		JPanel topOptionsRow = new JPanel(new GridLayout(1, 2, 0, 0));
		JPanel bottomOptionsRow = new JPanel(new BorderLayout());

		optionsPanel.add(topOptionsRow);
		optionsPanel.add(bottomOptionsRow);

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

		//////////////////////////////// HASH PANE///////////////////////////////
		hashTextArea = new JTextArea();

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
		JPanelCommand commandPanel = new JPanelCommand(chiiEmulator, "Chii Emulator - AniDB IRC bot commands\n"
				+ "Commands: !uptime !mystats !anime !group !randomanime !mylist !state !watched !storage !font\n"
				+ "Raw API: Start with '?' (e.g. ?PING) - session is added automatically.\n");
		chiiEmulator.setLog(commandPanel);

		////////////////////////////// TABBED PANE///////////////////////////////
		tabbedPane = new JTabbedPane();
		tabbedPane.addTab("Rules", rulesOptionsPanel);
		tabbedPane.addTab("Options", optionsPanel);
		tabbedPane.addTab("Jobs", jobsPanel);
		tabbedPane.addTab("Alt", altViewPanel);
		tabbedPane.addTab("Log", logScrollPane);
		tabbedPane.addTab("Hash", new JScrollPane(hashTextArea));
		tabbedPane.addTab("Info", new JScrollPane(infoTextArea));
		tabbedPane.addTab("Debug", new JPanelDebug(null));
		tabbedPane.addTab("Chii Emu", commandPanel);
		tabbedPane.setSelectedIndex(6);

		////////////////////////////// PROGRESSBARS//////////////////////////////
		statusProgressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 1000);
		statusProgressBar.setString("Welcome to WebAOM!");
		statusProgressBar.setStringPainted(true);
		jobProgressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, 1000);
		jobProgressBar.setString(AppContext.S_VER);
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

		KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
		focusManager.addKeyEventDispatcher(new DefaultKeyboardFocusManager() {
			public boolean dispatchKeyEvent(KeyEvent event) {
				if (event.getKeyCode() == KeyEvent.VK_F9 && (event.getID() == KeyEvent.KEY_PRESSED)) {
					reset();
					return true;
				}
				return super.dispatchKeyEvent(event);
			}
		});
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
		} else if (source == progressTimer)
		// System.err.println(A.frame.getFocusOwner());
		{
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
		} else if (source == newExtensionTextField) {
			AppContext.fha.addExtension(newExtensionTextField.getText());
			newExtensionTextField.setText("");
		} else if (source == miscOptionsPanel.logFilePathField) {
			startLogging();
		} else if (source == altViewPanel.sortModeComboBox) {
			Cache.treeSortMode = altViewPanel.sortModeComboBox.getSelectedIndex();
			altViewPanel.updateAlternativeView(true);
		} else if (source == altViewPanel.fileVisibilityComboBox) {
			Cache.hideNew = altViewPanel.fileVisibilityComboBox.getSelectedIndex() == 1;
			Cache.hideExisting = altViewPanel.fileVisibilityComboBox.getSelectedIndex() == 2;
			altViewPanel.updateAlternativeView(true);
		} else if (source == altViewPanel.pathRegexField) {
			String regexPattern = altViewPanel.pathRegexField.getText();
			if (regexPattern.isEmpty()) {
				AppContext.preg = null;
			} else {
				AppContext.preg = regexPattern;
			}
			altViewPanel.updateAlternativeView(true);
		} else if (source == altViewPanel.animeTitleComboBox) {
			Anime.TITLE_PRIORITY = altViewPanel.animeTitleComboBox.getSelectedIndex();
			altViewPanel.updateAlternativeView(false);
		} else if (source == altViewPanel.episodeTitleComboBox) {
			Episode.TITLE_PRIORITY = altViewPanel.episodeTitleComboBox.getSelectedIndex();
			altViewPanel.updateAlternativeView(false);
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
		if (isNetworkIoRunning) { // we want it to run
			setNetworkIoOptionsEnabled(false); // freeze settings
			isNetworkIoRunning = startNetworkIoInternal(); // try to start
			if (!isNetworkIoRunning) { // failed
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

	protected class JobScrollDown implements Runnable {
		@Override
		public void run() {
			if (!jobsScrollBar.getValueIsAdjusting()) {
				jobsScrollBar.setValue(jobsScrollBar.getMaximum());
			}
		}
	}

	public void println(Object message) {
		logEditorPane.println(message.toString());
		if (logEditorPane.isVisible()) {
			javax.swing.SwingUtilities.invokeLater(jobScrollDownRunnable);
		}
	}

	/** Sets the status message in the main status bar. Required by Log interface. */
	public void status0(String str) {
		statusProgressBar.setString(str);
		lastStatusMessage = str;
	}

	/** Sets the status message in the job progress bar. Required by Log interface. */
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
		jobProgressBar.setValue(AppContext.jobc.getProgress());
		if (AppContext.frame != null) {
			AppContext.frame.setTitle("WebAOM " + AppContext.S_VER + " " + AppContext.jobc.getStatus());
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
				Runtime.getRuntime().exec(new String[]{path, url});
			} else if (java.awt.Desktop.isDesktopSupported()
					&& java.awt.Desktop.getDesktop().isSupported(java.awt.Desktop.Action.BROWSE)) {
				java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
			} else {
				// Fallback for systems without Desktop support
				String os = System.getProperty("os.name").toLowerCase();
				if (os.contains("mac")) {
					Runtime.getRuntime().exec(new String[]{"open", url});
				} else if (os.contains("win")) {
					Runtime.getRuntime().exec(new String[]{"rundll32", "url.dll,FileProtocolHandler", url});
				} else {
					Runtime.getRuntime().exec(new String[]{"xdg-open", url});
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public AniDBFileClient createConnection() {
		AppContext.usetup = new AniDBConnectionSettings(getHost(), getRemotePort(), getLocalPort(),
				connectionOptionsPanel.getTimeout(), connectionOptionsPanel.getDelayMillis(), 3,
				connectionOptionsPanel.isNatKeepAliveEnabled());
		AniDBFileClient connection = new AniDBFileClient(this, AppContext.usetup);
		connection.set(AppContext.userPass.username, AppContext.userPass.password, AppContext.userPass.apiKey);
		return connection;
	}

	/////////////////////////////////// OPTIONS//////////////////////////////////
	public void saveOptions(Options options) {
		options.setBoolean(Options.BOOL_ADD_FILE, autoAddToMylistCheckbox.isSelected());
		options.setString(Options.STR_HTML_COLORS, HyperlinkBuilder.encodeColors());
		options.setString(Options.STR_USERNAME, AppContext.userPass.get(miscOptionsPanel.isStorePasswordEnabled()));

		mylistOptionsPanel.saveToOptions(options);
		AppContext.fha.saveOptions(options);
		connectionOptionsPanel.saveOptions(options);
		miscOptionsPanel.saveToOptions(options);
		AppContext.rules.saveToOptions(options);
		jobsPanel.saveOptions(options);

		options.setString(Options.STR_PATH_REGEX, AppContext.preg);
		options.setString(Options.STR_FONT, AppContext.font);
		options.setString(Options.STR_LOG_HEADER, JEditorPaneLog.htmlHeader);
	}

	public void loadOptions(Options options) {
		try {
			autoAddToMylistCheckbox.setSelected(options.getBoolean(Options.BOOL_ADD_FILE));
			HyperlinkBuilder.decodeColors(options.getString(Options.STR_HTML_COLORS));
			AppContext.userPass.set(options.getString(Options.STR_USERNAME));
			mylistOptionsPanel.loadFromOptions(options);
			AppContext.fha.loadOptions(options);
			connectionOptionsPanel.loadOptions(options);
			miscOptionsPanel.loadFromOptions(options);
			AppContext.rules.loadFromOptions(options);
			jobsPanel.loadOptions(options);
			rulesOptionsPanel.updateRules();

			String pathRegex = options.getString(Options.STR_PATH_REGEX);
			if (!pathRegex.isEmpty()) {
				AppContext.preg = pathRegex;
				altViewPanel.pathRegexField.setText(pathRegex);
			}
			AppContext.font = options.getString(Options.STR_FONT);
			logEditorPane.setHeader(options.getString(Options.STR_LOG_HEADER));
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
		fileChooser.setFileFilter(AppContext.fha.extensionFilter);
		fileChooser.setMultiSelectionEnabled(true);
		if (AppContext.dir != null) {
			fileChooser.setCurrentDirectory(new File(AppContext.dir));
		}
		int option = fileChooser.showDialog(AppContext.component, "Select File(s)");
		if (option == JFileChooser.APPROVE_OPTION) {
			selectFilesForProcessing(fileChooser.getSelectedFiles());
		} else {
			AppContext.dir = fileChooser.getCurrentDirectory().getAbsolutePath();
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
		if (AppContext.dir != null) {
			fileChooser.setCurrentDirectory(new File(AppContext.dir));
		}
		int option = fileChooser.showDialog(AppContext.component, "Select Directory(ies) (recursive)");
		if (option == JFileChooser.APPROVE_OPTION) {
			selectFilesForProcessing(fileChooser.getSelectedFiles());
		} else {
			AppContext.dir = fileChooser.getCurrentDirectory().getAbsolutePath();
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
			AppContext.dir = files[0].getParent();
		} else {
			AppContext.dir = files[0].getAbsolutePath();
		}
		workerThread = new RecursiveDirectoryScanner(files, false);
		workerThread.start();
	}

	/** Thread that recursively scans directories for files to process. */
	private class RecursiveDirectoryScanner extends Thread {
		private final File[] directories;
		private int directoryCount = 0;
		private final boolean shouldPrintStatus;

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
					statusMessage = "Added " + fileCount + " files in " + (System.currentTimeMillis() - startTime)
							+ " ms.";
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
				File[] files = file.listFiles(AppContext.fha.extensionFilter);
				if (files == null) {
					return 0;
				}
				for (File value : files) {
					fileCount += addFileRecursive(value);
				}
				return fileCount;
			}
			if (AppContext.fha.addFile(file)) {
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
			if (AppContext.databaseManager.initialize(databasePathField.getText())) {
				long startTime = System.currentTimeMillis();
				// A.mem3 = A.getUsed();
				AppContext.databaseManager.getJobs();
				Object[] jobArray = AppContext.jobs.array();
				Job job;

				// A.mem4 = A.getUsed();

				AppContext.databaseManager.debug = false;
				for (Object o : jobArray) {
					job = (Job) o;
					job.isFresh = false;
					AppContext.cache.gatherInfo(job, false);
					if (job.getStatus() == Job.MOVEWAIT) {
						JobManager.updatePath(job);
					}
					// updateJobTable(job);
				}
				int elapsedMs = (int) (System.currentTimeMillis() - startTime);
				println("Loaded db in " + HyperlinkBuilder.formatAsNumber(elapsedMs) + " ms. "
						+ HyperlinkBuilder.formatAsNumber(AppContext.jobs.size()) + " files found.");
				altViewPanel.updateAlternativeView(true);
			} else {
				databasePathField.setEnabled(true);
			}
			AppContext.databaseManager.debug = true;
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

	private static String getButtonName(int buttonIndex) {
		switch (buttonIndex) {
			case BUTTON_SELECT_FILES :
				return "Files...";
			case BUTTON_SELECT_DIRS :
				return "Folders...";
			case BUTTON_HASH :
				return LABEL_DISK_IO_ENABLE;
			case BUTTON_CONNECTION :
				return LABEL_NETWORK_IO_ENABLE;
			case BUTTON_SAVE :
				return "Save opt";
			case BUTTON_WIKI :
				return "Help!";
			case BUTTON_EXPORT :
				return "Export";
			case BUTTON_IMPORT :
				return "Import";
			default :
				return "No text!";
		}
	}

	private static String getButtonToolTip(int buttonIndex) {
		switch (buttonIndex) {
			case BUTTON_SELECT_FILES :
				return "Add files you want to hash";
			case BUTTON_SELECT_DIRS :
				return "Add folders with files you want to hash";
			case BUTTON_HASH :
				return "Start/stop the disk operations thread. (Hashing and moving)";
			case BUTTON_CONNECTION :
				return "Log on / log off the AniDB UDP Service";
			case BUTTON_SAVE :
				return "Save the options to disk";
			case BUTTON_WIKI :
				return "Check out the documentation @ AniDB WIKI";
			case BUTTON_EXPORT :
				return "Export loaded data";
			case BUTTON_IMPORT :
				return "Import exported data";
			default :
				return "No help!";
		}
	}

	private void clearHighlight() {
		if (jobsPanel != null) {
			if (originalJobsBorder != null) {
				jobsPanel.setBorder(originalJobsBorder);
			} else {
				jobsPanel.setBorder(new EtchedBorder());
			}
			jobsPanel.repaint();
		}
	}

	@Override
	public void dragEnter(DropTargetDragEvent dragEvent) {
		if (dragEvent.getTransferable().isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
			dragEvent.acceptDrag(DnDConstants.ACTION_COPY);
			switchToJobsTab();

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
				java.util.List<File> fileList = (java.util.List<File>) transferable
						.getTransferData(DataFlavor.javaFileListFlavor);
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
}
