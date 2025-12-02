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

import epox.util.StringUtilities;
import epox.util.UserPass;
import epox.webaom.data.AniDBEntity;
import epox.webaom.db.DatabaseManager;
import epox.webaom.db.DatabaseManagerFactory;
import epox.webaom.net.AniDBConnectionSettings;
import epox.webaom.net.AniDBFileClient;
import epox.webaom.ui.MainPanel;
import java.awt.Component;
import java.awt.Font;
import java.io.File;
import java.io.InputStream;
import java.util.Properties;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Global state singleton holding all subsystems and configuration.
 * This is a legacy pattern from the original codebase.
 */
public final class AppContext {
    /** AniDB hostname for API and web URLs. */
    public static final String ANIDB_HOST = "anidb.net";
    /** Line separator for file export (Windows-style CRLF). */
    public static final String LINE_SEPARATOR = "\r\n";
    /** Application version string with build date. */
    public static final String VERSION = loadVersion();
    /** HTML template for file info dialog. */
    public static String fileSchemaTemplate;
    /** Last used directory for file/folder dialogs. */
    public static String lastDirectory = null;
    /** Path regex filter for alternate view (null means show all). */
    public static String pathRegex = null;

    public static String font = "";
    /** Assumed episode count for normal episodes when actual count unknown (for zero-padding). */
    public static int assumedEpisodeCount = 99;
    /** Assumed episode count for special episodes when actual count unknown (for zero-padding). */
    public static int assumedSpecialCount = 99;

    public static java.awt.Component component = null;
    public static java.awt.Frame frame = null;
    public static DatabaseManager databaseManager;
    public static NetworkIOManager nio;
    public static DiskIOManager dio;
    public static Options opt;
    public static Rules rules;
    public static Cache cache;
    public static AniDBFileClient conn;
    /** Job counter for tracking job status distribution. */
    public static JobCounter jobCounter;

    public static JobList jobs;
    public static AniDBConnectionSettings usetup;
    public static MainPanel gui;
    /** File handler for managing file extensions and file operations. */
    public static FileHandler fileHandler;
    /** Primary popup menu component (jobs table context menu). */
    public static Component primaryPopupMenu;
    /** Secondary popup menu component (alternate view context menu). */
    public static Component secondaryPopupMenu;
    /** Root node of the anime tree structure for alternate view. */
    public static final AniDBEntity animeTreeRoot = new AniDBEntity();

    public static boolean autoadd = false;
    public static boolean optionsChanged = false;
    public static final UserPass userPass = new UserPass(null, null, null);

    private AppContext() {
        // static only
    }

    private static String loadVersion() {
        try {
            Properties props = new Properties();
            InputStream is = AppContext.class.getClassLoader().getResourceAsStream("version.properties");
            if (is != null) {
                props.load(is);
                String version = props.getProperty("version");
                String buildDate = props.getProperty("buildDate");
                return version + " (" + buildDate + ")";
            }
        } catch (Exception e) {
            // Fallback if properties file can't be loaded
        }
        return "unknown";
    }

    public static void init() {
        // A.mem0 = A.getUsed();
        Thread.currentThread().setName("Main");
        jobs = new JobList();
        jobCounter = new JobCounter();
        rules = new Rules();
        cache = new Cache();
        databaseManager = DatabaseManagerFactory.createEmbedded();
        fileHandler = new FileHandler();
        opt = new Options();
        dio = new DiskIOManager();
        nio = new NetworkIOManager();
        // A.mem1 = A.getUsed();
        gui = new MainPanel();
        fileSchemaTemplate =
                StringUtilities.fileToString(System.getProperty("user.home") + File.separator + ".webaom.htm");
        if (fileSchemaTemplate == null) {
            fileSchemaTemplate = AppContext.getFileString("file.htm");
        }

        if (!font.isEmpty()) {
            setFont(font);
        }
        // A.mem2 = A.getUsed();
    }

    public static boolean shutdown(boolean opx) {
        if (opx) {
            Options o = new Options();
            if (o.existsOnDisk()) {
                gui.saveOptions(o);
                if (!AppContext.opt.equals(o)) {
                    if (o.getBoolean(Options.BOOL_AUTO_SAVE)) {
                        o.saveToFile();
                    } else {
                        String dialogTitle = "The options has changed";
                        String dialogMessage = "Do you want to save them?";
                        int response = showYesNoCancelDialog(dialogTitle, dialogMessage);
                        switch (response) {
                            case 0:
                                o.saveToFile();
                                break;
                            case -1:
                            case 2:
                                return false;
                            default:
                                // No (case 1) - continue without saving
                                break;
                        }
                    }
                }
            }
        }
        gui.reset();
        gui.shutdown();
        return true;
    }

    public static void setFont(String f) {
        int i = f.lastIndexOf(',');
        int size = 11;
        if (i > 0) {
            try {
                String s = f.substring(i + 1);
                f = f.substring(0, i).trim();
                size = Integer.parseInt(s);
            } catch (NumberFormatException e) {
                //
            }
        }
        Font fo = new Font(f, Font.PLAIN, size);
        WebAOM.setGlobalFont(fo, fo);
        SwingUtilities.updateComponentTreeUI(gui);
        SwingUtilities.updateComponentTreeUI(primaryPopupMenu);
        SwingUtilities.updateComponentTreeUI(secondaryPopupMenu);
    }

    public static void dialog(String title, String msg) {
        JOptionPane.showMessageDialog(component, msg, title, JOptionPane.PLAIN_MESSAGE);
    }

    public static void dialog2(String title, String msg) {
        new epox.webaom.ui.JFrameHtml(title, msg);
    }

    public static boolean confirm(String title, String msg, String pos, String neg) {
        Object[] o = {pos, neg};
        return JOptionPane.showOptionDialog(
                        AppContext.component,
                        msg,
                        title,
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        o,
                        o[0])
                == 0;
    }

    /**
     * Shows a Yes/No/Cancel dialog.
     *
     * @param title
     *            dialog title
     * @param msg
     *            dialog message
     * @return 0 for Yes, 1 for No, 2 for Cancel, -1 if closed
     */
    public static int showYesNoCancelDialog(String title, String msg) {
        Object[] options = {"Yes", "No", "Cancel"};
        return JOptionPane.showOptionDialog(
                AppContext.component,
                msg,
                title,
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[0]);
    }

    public static boolean bitcmp(int s, int m) {
        return (s & m) == m;
    }

    public static String getFileString(String name) {
        try {
            InputStream is = WebAOM.class.getClassLoader().getResourceAsStream(name);
            String str = "";
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int read;
            while ((read = is.read(buffer, 0, bufferSize)) > 0) {
                str += new String(buffer, 0, read);
            }
            return str;
        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
    }

    public static void deleteFileAndFolder(File f, String s) {
        deleteFile(f, s);
        deleteFile(f.getParentFile(), s);
    }

    public static void deleteFile(File f, String s) {
        if (f.delete()) {
            System.out.println("$ Deleted " + f + " (" + s + ")");
        }
    }

    public static void dumpStats() {
        System.out.println("@ JobList: " + AppContext.jobs);
        System.out.println("@ Cache: " + AppContext.cache);

        int sub0 = 0;
        int sub1 = 0;
        AniDBEntity b;
        AniDBEntity c;
        for (int i = 0; i < AppContext.animeTreeRoot.size(); i++) {
            b = AppContext.animeTreeRoot.get(i);
            if (b == null) {
                continue;
            }
            b.buildSortedChildArray();
            sub0 += b.size();
            for (int j = 0; j < b.size(); j++) {
                c = b.get(j);
                if (c != null) {
                    sub1 += c.size();
                }
            }
        }
        System.out.println("@ Tree: " + AppContext.animeTreeRoot.size() + ", " + sub0 + ", " + sub1);
    }
}
