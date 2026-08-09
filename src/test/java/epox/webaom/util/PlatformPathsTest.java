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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class PlatformPathsTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void getApplicationName_usesEnvVarFallbackWhenPropertyMissing() {
        assertEquals("webaom-dev", PlatformPaths.getApplicationName(null, "true"));
    }

    @Test
    void getApplicationName_propertyFalseOverridesEnvVarTrue() {
        assertEquals("webaom", PlatformPaths.getApplicationName("false", "true"));
    }

    @Test
    void getDefaultLogFilePath_usesDevNameWhenPropertyEnabled() {
        String originalProperty = System.getProperty("webaom.dev");
        try {
            System.setProperty("webaom.dev", "true");

            assertEquals("webaom-dev.log", new File(PlatformPaths.getDefaultLogFilePath()).getName());
        } finally {
            restoreDevProperty(originalProperty);
        }
    }

    @Test
    void migrateIfNeededReportsToTheSelectedDiagnosticsStream() throws Exception {
        Path legacy = Files.writeString(temporaryDirectory.resolve("legacy"), "options");
        Path destination = temporaryDirectory.resolve("new/config");
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        boolean migrated;
        try (PrintStream diagnostics = new PrintStream(output, true, StandardCharsets.UTF_8)) {
            migrated = PlatformPaths.migrateIfNeeded(legacy.toString(), destination.toString(), diagnostics);
        }

        assertTrue(migrated);
        assertEquals("options", Files.readString(destination));
        assertTrue(output.toString(StandardCharsets.UTF_8).contains("$ Migrated config:"));
    }

    @Test
    void developmentModeGivesThePropertyPriorityOverTheEnvironment() {
        assertFalse(PlatformPaths.isDevelopmentMode(null, null));
        assertFalse(PlatformPaths.isDevelopmentMode(null, "false"));
        assertTrue(PlatformPaths.isDevelopmentMode(null, "true"));
        assertTrue(PlatformPaths.isDevelopmentMode("true", "false"));
        assertFalse(PlatformPaths.isDevelopmentMode("false", "true"));
        assertEquals("webaom", PlatformPaths.getApplicationName(null, null));
        assertEquals("webaom-dev", PlatformPaths.getApplicationName("true", null));
    }

    @Test
    void pathHelpersComposeTheCurrentApplicationName() {
        String originalProperty = System.getProperty("webaom.dev");
        try {
            System.setProperty("webaom.dev", "true");

            assertEquals(
                    "webaom-dev.log",
                    Path.of(PlatformPaths.getDefaultLogFilePath()).getFileName().toString());
            assertEquals(
                    "webaom-dev.db",
                    Path.of(PlatformPaths.getDefaultEmbeddedDatabasePath())
                            .getFileName()
                            .toString());
            assertEquals(
                    "config",
                    Path.of(PlatformPaths.getConfigFilePath()).getFileName().toString());
            assertEquals(
                    "template.htm",
                    Path.of(PlatformPaths.getTemplateFilePath()).getFileName().toString());
            assertTrue(PlatformPaths.getDefaultLogDirectory().contains("webaom-dev"));
            assertTrue(PlatformPaths.getDefaultDatabaseDirectory().contains("webaom-dev"));
            assertTrue(PlatformPaths.getConfigDirectory().contains("webaom-dev"));
        } finally {
            restoreDevProperty(originalProperty);
        }
    }

    @Test
    void legacyPathsAndPlatformFlagsDescribeTheRunningHost() {
        assertTrue(PlatformPaths.getLegacyConfigFilePath().endsWith(File.separator + ".webaom"));
        assertTrue(PlatformPaths.getLegacyTemplateFilePath().endsWith(File.separator + ".webaom.htm"));

        String osName = PlatformPaths.getOSName();
        assertTrue(osName.equals("Linux")
                || osName.equals("macOS")
                || osName.equals("Windows")
                || osName.equals("Unknown"));
        assertEquals(osName.equals("Linux"), PlatformPaths.isLinux());
        assertEquals(osName.equals("macOS"), PlatformPaths.isMac());
        assertEquals(osName.equals("Windows"), PlatformPaths.isWindows());
    }

    @Test
    void isolatedClassLoadingCharacterizesMacWindowsAndUnknownFallbacks() throws Exception {
        String originalOsName = System.getProperty("os.name");
        String originalUserHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", temporaryDirectory.toString());

            assertEquals("macOS", callIsolated("Mac OS X", "getOSName"));
            assertTrue(((String) callIsolated("Mac OS X", "getDefaultLogDirectory"))
                    .contains("Library" + File.separator + "Logs"));
            assertTrue(((String) callIsolated("Mac OS X", "getDefaultDatabaseDirectory"))
                    .contains("Library" + File.separator + "Application Support"));

            assertEquals("Windows", callIsolated("Windows 11", "getOSName"));
            assertTrue((Boolean) callIsolated("Windows 11", "isWindows"));

            assertEquals("Unknown", callIsolated("Plan 9", "getOSName"));
            assertTrue(((String) callIsolated("Plan 9", "getConfigDirectory")).endsWith(File.separator + ".webaom"));
        } finally {
            System.setProperty("os.name", originalOsName);
            System.setProperty("user.home", originalUserHome);
        }
    }

    @Test
    void migrateIfNeededSkipsMissingSourcesAndExistingDestinations() throws Exception {
        Path missing = temporaryDirectory.resolve("missing");
        Path destination = temporaryDirectory.resolve("destination");
        assertFalse(PlatformPaths.migrateIfNeeded(missing.toString(), destination.toString()));

        Path legacy = Files.writeString(temporaryDirectory.resolve("legacy-existing"), "old");
        Files.writeString(destination, "new");
        assertFalse(PlatformPaths.migrateIfNeeded(legacy.toString(), destination.toString()));
        assertEquals("new", Files.readString(destination));
    }

    @Test
    void migrationFailureLeavesTheLegacyFileUntouched() throws Exception {
        Path legacy = Files.writeString(temporaryDirectory.resolve("legacy-failure"), "old");
        Path regularFile = Files.writeString(temporaryDirectory.resolve("not-a-directory"), "blocker");
        Path destination = regularFile.resolve("config");

        assertFalse(PlatformPaths.migrateIfNeeded(legacy.toString(), destination.toString()));
        assertEquals("old", Files.readString(legacy));
        assertFalse(Files.exists(destination));
    }

    @Test
    void directoryHelpersCreateParentsAndRejectInvalidTargets() throws Exception {
        Path directory = temporaryDirectory.resolve("one/two");
        assertTrue(PlatformPaths.ensureDirectoryExists(directory.toString()));
        assertTrue(Files.isDirectory(directory));
        assertTrue(PlatformPaths.ensureDirectoryExists(directory.toString()));

        Path regularFile = Files.writeString(temporaryDirectory.resolve("regular-file"), "data");
        assertFalse(PlatformPaths.ensureDirectoryExists(regularFile.toString()));
        assertFalse(PlatformPaths.ensureDirectoryExists(null));
        assertFalse(PlatformPaths.ensureDirectoryExists(""));
        assertFalse(PlatformPaths.ensureDirectoryExists("invalid\0path"));

        Path nestedFile = temporaryDirectory.resolve("parent/child/config");
        assertTrue(PlatformPaths.ensureParentDirectoryExists(nestedFile.toString()));
        assertTrue(Files.isDirectory(nestedFile.getParent()));
        assertFalse(PlatformPaths.ensureParentDirectoryExists(null));
        assertFalse(PlatformPaths.ensureParentDirectoryExists(""));
        assertFalse(PlatformPaths.ensureParentDirectoryExists("config"));
    }

    private static Object callIsolated(String osName, String methodName) throws Exception {
        System.setProperty("os.name", osName);
        URL classesLocation =
                PlatformPaths.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader loader = new URLClassLoader(new URL[] {classesLocation}, null)) {
            Class<?> isolatedClass = Class.forName(PlatformPaths.class.getName(), true, loader);
            Method method = isolatedClass.getMethod(methodName);
            return method.invoke(null);
        }
    }

    private static void restoreDevProperty(String originalProperty) {
        if (originalProperty == null) {
            System.clearProperty("webaom.dev");
        } else {
            System.setProperty("webaom.dev", originalProperty);
        }
    }
}
