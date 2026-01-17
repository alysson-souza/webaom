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

package epox.webaom.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class for obtaining platform-specific default paths. Provides sensible defaults for logs,
 * data, and configuration directories based on the operating system and platform conventions.
 */
public final class PlatformPaths {

    private static final String OS_NAME = System.getProperty("os.name").toLowerCase();
    private static final String USER_HOME = System.getProperty("user.home");

    // Platform detection constants
    private static final boolean IS_MAC = OS_NAME.contains("mac");
    private static final boolean IS_WINDOWS = OS_NAME.contains("win");
    private static final boolean IS_LINUX = OS_NAME.contains("linux");

    private static final String APP_NAME = "webaom";

    private PlatformPaths() {}

    /**
     * Get the default log directory for the current platform.
     *
     * <p>
     * macOS: ~/Library/Logs/webaom/ Linux: ~/.local/share/webaom/logs/ Windows:
     * %APPDATA%\webaom\logs\ Other: ~/.webaom/logs/
     *
     * @return the default log directory path
     */
    public static String getDefaultLogDirectory() {
        if (IS_MAC) {
            return new File(USER_HOME, "Library/Logs/" + APP_NAME).getAbsolutePath();
        } else if (IS_WINDOWS) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return new File(appData, APP_NAME + "/logs").getAbsolutePath();
            }
        } else if (IS_LINUX) {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
                return new File(xdgDataHome, APP_NAME + "/logs").getAbsolutePath();
            } else {
                return new File(USER_HOME, ".local/share/" + APP_NAME + "/logs").getAbsolutePath();
            }
        }
        // Fallback for unknown platforms
        return new File(USER_HOME, "." + APP_NAME + "/logs").getAbsolutePath();
    }

    /**
     * Get the default log file path for the current platform.
     *
     * @return the default log file path (directory + webaom.log)
     */
    public static String getDefaultLogFilePath() {
        return new File(getDefaultLogDirectory(), APP_NAME + ".log").getAbsolutePath();
    }

    /**
     * Get the default database directory for the current platform. Uses the same base directory
     * structure as logs but under a 'database' subdirectory.
     *
     * @return the default database directory path
     */
    public static String getDefaultDatabaseDirectory() {
        if (IS_MAC) {
            return new File(USER_HOME, "Library/Application Support/" + APP_NAME + "/database").getAbsolutePath();
        } else if (IS_WINDOWS) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return new File(appData, APP_NAME + "/database").getAbsolutePath();
            }
        } else if (IS_LINUX) {
            String xdgDataHome = System.getenv("XDG_DATA_HOME");
            if (xdgDataHome != null && !xdgDataHome.isEmpty()) {
                return new File(xdgDataHome, APP_NAME + "/database").getAbsolutePath();
            } else {
                return new File(USER_HOME, ".local/share/" + APP_NAME + "/database").getAbsolutePath();
            }
        }
        // Fallback for unknown platforms
        return new File(USER_HOME, "." + APP_NAME + "/database").getAbsolutePath();
    }

    /**
     * Get the default embedded database file path.
     *
     * @return the full path to the embedded SQLite database file (with .db extension)
     */
    public static String getDefaultEmbeddedDatabasePath() {
        return new File(getDefaultDatabaseDirectory(), APP_NAME + ".db").getAbsolutePath();
    }

    /**
     * Get the configuration directory for the current platform (XDG compliant on Linux).
     *
     * <p>
     * macOS: ~/Library/Application Support/webaom/ Linux: $XDG_CONFIG_HOME/webaom/ (default:
     * ~/.config/webaom/) Windows: %APPDATA%\webaom\ Other: ~/.webaom/
     *
     * @return the configuration directory path
     */
    public static String getConfigDirectory() {
        if (IS_MAC) {
            return new File(USER_HOME, "Library/Application Support/" + APP_NAME).getAbsolutePath();
        } else if (IS_WINDOWS) {
            String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isEmpty()) {
                return new File(appData, APP_NAME).getAbsolutePath();
            }
        } else if (IS_LINUX) {
            String xdgConfigHome = System.getenv("XDG_CONFIG_HOME");
            if (xdgConfigHome != null && !xdgConfigHome.isEmpty()) {
                return new File(xdgConfigHome, APP_NAME).getAbsolutePath();
            } else {
                return new File(USER_HOME, ".config/" + APP_NAME).getAbsolutePath();
            }
        }
        // Fallback for unknown platforms
        return new File(USER_HOME, "." + APP_NAME).getAbsolutePath();
    }

    /**
     * Get the configuration file path for the current platform.
     *
     * @return the full path to the configuration file
     */
    public static String getConfigFilePath() {
        return new File(getConfigDirectory(), "config").getAbsolutePath();
    }

    /**
     * Get the HTML template file path for the current platform.
     *
     * @return the full path to the custom template file
     */
    public static String getTemplateFilePath() {
        return new File(getConfigDirectory(), "template.htm").getAbsolutePath();
    }

    /**
     * Get the legacy configuration file path (~/.webaom).
     *
     * @return the legacy config file path
     */
    public static String getLegacyConfigFilePath() {
        return new File(USER_HOME, ".webaom").getAbsolutePath();
    }

    /**
     * Get the legacy HTML template file path (~/.webaom.htm).
     *
     * @return the legacy template file path
     */
    public static String getLegacyTemplateFilePath() {
        return new File(USER_HOME, ".webaom.htm").getAbsolutePath();
    }

    /**
     * Migrate a file from legacy path to new path if needed. Migration occurs only when the new
     * file doesn't exist but the legacy file does. The legacy file is preserved as a backup.
     *
     * @param legacyPath
     *            the old file path
     * @param newPath
     *            the new file path
     * @return true if migration was performed, false otherwise
     */
    public static boolean migrateIfNeeded(String legacyPath, String newPath) {
        File legacyFile = new File(legacyPath);
        File newFile = new File(newPath);

        // Already migrated or fresh install
        if (newFile.exists() || !legacyFile.exists()) {
            return false;
        }

        try {
            ensureParentDirectoryExists(newPath);
            Files.copy(legacyFile.toPath(), newFile.toPath());
            System.out.println("$ Migrated config: " + legacyPath + " -> " + newPath);
            return true;
        } catch (IOException e) {
            System.err.println("! Migration failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Ensure that a directory exists, creating it if necessary. Creates parent directories as
     * needed.
     *
     * @param path
     *            the directory path to ensure exists
     * @return true if directory exists or was created successfully, false on failure
     */
    public static boolean ensureDirectoryExists(String path) {
        if (path == null || path.isEmpty()) {
            return false;
        }

        try {
            Path dirPath = Paths.get(path);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            return Files.isDirectory(dirPath);
        } catch (Exception e) {
            System.err.println("Failed to create directory: " + path);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Ensure that the parent directory of a file exists, creating it if necessary.
     *
     * @param filePath
     *            the file path
     * @return true if parent directory exists or was created successfully, false on failure
     */
    public static boolean ensureParentDirectoryExists(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }

        File file = new File(filePath);
        File parentDir = file.getParentFile();

        if (parentDir == null) {
            return false;
        }

        return ensureDirectoryExists(parentDir.getAbsolutePath());
    }

    /**
     * Get the operating system name.
     *
     * @return the OS name
     */
    public static String getOSName() {
        if (IS_MAC) {
            return "macOS";
        } else if (IS_WINDOWS) {
            return "Windows";
        } else if (IS_LINUX) {
            return "Linux";
        }
        return "Unknown";
    }

    /**
     * Check if the current platform is macOS.
     *
     * @return true if running on macOS
     */
    public static boolean isMac() {
        return IS_MAC;
    }

    /**
     * Check if the current platform is Windows.
     *
     * @return true if running on Windows
     */
    public static boolean isWindows() {
        return IS_WINDOWS;
    }

    /**
     * Check if the current platform is Linux.
     *
     * @return true if running on Linux
     */
    public static boolean isLinux() {
        return IS_LINUX;
    }
}
