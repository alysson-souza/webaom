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

package epox.swing;

import com.formdev.flatlaf.util.SystemInfo;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/**
 * Monitors the operating system's dark/light mode preference and notifies a callback when it changes.
 * Detection is cross-platform: macOS (defaults), Windows (registry), Linux (gsettings).
 * Returns {@link Appearance#UNKNOWN} when detection is unavailable, in which case no switch occurs.
 *
 * <p>Polling runs on a background thread to avoid blocking the EDT.
 */
public final class OsAppearanceMonitor {
    private static final int POLL_INTERVAL_SECONDS = 3;
    private static final int COMMAND_TIMEOUT_SECONDS = 2;

    public enum Appearance {
        LIGHT,
        DARK,
        UNKNOWN
    }

    private static ScheduledExecutorService executor;
    private static Appearance lastKnown = Appearance.UNKNOWN;

    private OsAppearanceMonitor() {
        // static only
    }

    /** Queries the OS and returns the current dark/light appearance. Thread-safe. */
    public static Appearance getOsAppearance() {
        if (SystemInfo.isMacOS) {
            return detectMacOs();
        } else if (SystemInfo.isWindows) {
            return detectWindows();
        } else if (SystemInfo.isLinux) {
            return detectLinux();
        }
        return Appearance.UNKNOWN;
    }

    /** Convenience: returns true only when the OS is definitively in dark mode. */
    public static boolean isOsDarkMode() {
        return getOsAppearance() == Appearance.DARK;
    }

    /**
     * Starts polling the OS appearance on a background thread.
     * When a change is detected (and the new state is not UNKNOWN), the callback
     * is invoked on the EDT with {@code true} for dark, {@code false} for light.
     */
    public static synchronized void start(Consumer<Boolean> onChanged) {
        stop();
        lastKnown = getOsAppearance();
        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "OsAppearanceMonitor");
            t.setDaemon(true);
            return t;
        });
        executor.scheduleAtFixedRate(
                () -> {
                    try {
                        Appearance current = getOsAppearance();
                        if (current != Appearance.UNKNOWN && current != lastKnown) {
                            lastKnown = current;
                            boolean dark = current == Appearance.DARK;
                            SwingUtilities.invokeLater(() -> onChanged.accept(dark));
                        }
                    } catch (Exception e) {
                        // Suppress to avoid killing the scheduled task
                    }
                },
                POLL_INTERVAL_SECONDS,
                POLL_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }

    public static synchronized void stop() {
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    // macOS: `defaults read -g AppleInterfaceStyle` returns "Dark" in dark mode,
    // exits with error (status != 0) in light mode.
    static Appearance detectMacOs() {
        String output = runCommand("defaults", "read", "-g", "AppleInterfaceStyle");
        if (output == null) {
            return Appearance.UNKNOWN;
        }
        return output.trim().equalsIgnoreCase("Dark") ? Appearance.DARK : Appearance.LIGHT;
    }

    // Windows: `reg query` for AppsUseLightTheme; value 0x0 means dark mode.
    static Appearance detectWindows() {
        String output = runCommand(
                "reg",
                "query",
                "HKCU\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                "/v",
                "AppsUseLightTheme");
        if (output == null) {
            return Appearance.UNKNOWN;
        }
        return output.contains("0x0") ? Appearance.DARK : Appearance.LIGHT;
    }

    // Linux: Use freedesktop portal (works on GNOME, KDE, and other compliant desktops).
    // Portal returns: variant uint32 1 = prefer-dark, 0 = no preference, 2 = prefer-light.
    static Appearance detectLinux() {
        String output = runCommand(
                "dbus-send",
                "--session",
                "--print-reply=literal",
                "--dest=org.freedesktop.portal.Desktop",
                "/org/freedesktop/portal/desktop",
                "org.freedesktop.portal.Settings.Read",
                "string:org.freedesktop.appearance",
                "string:color-scheme");
        if (output == null) {
            return Appearance.UNKNOWN;
        }
        return parseLinuxPortal(output);
    }

    /** Parses dbus-send portal output. "uint32 1" = dark, anything else = light. */
    static Appearance parseLinuxPortal(String output) {
        return output.contains("uint32 1") ? Appearance.DARK : Appearance.LIGHT;
    }

    /** Runs a command with a timeout. Returns null on any failure. */
    static String runCommand(String... command) {
        try {
            Process process =
                    new ProcessBuilder(command).redirectErrorStream(true).start();
            try (BufferedReader reader =
                    new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                boolean finished = process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return null;
                }
                return sb.toString();
            }
        } catch (Exception e) {
            return null;
        }
    }
}
