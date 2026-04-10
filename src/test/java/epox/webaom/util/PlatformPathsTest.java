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

import java.io.File;
import org.junit.jupiter.api.Test;

class PlatformPathsTest {

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

    private static void restoreDevProperty(String originalProperty) {
        if (originalProperty == null) {
            System.clearProperty("webaom.dev");
        } else {
            System.setProperty("webaom.dev", originalProperty);
        }
    }
}
