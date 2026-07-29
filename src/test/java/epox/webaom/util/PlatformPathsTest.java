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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

    private static void restoreDevProperty(String originalProperty) {
        if (originalProperty == null) {
            System.clearProperty("webaom.dev");
        } else {
            System.setProperty("webaom.dev", originalProperty);
        }
    }
}
