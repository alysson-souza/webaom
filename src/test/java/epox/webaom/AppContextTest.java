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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import epox.webaom.util.PlatformPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AppContextTest {
    @TempDir
    Path tempDir;

    @Test
    void getLegacyTemplatePath_developmentModeDisablesLegacyTemplate() {
        assertNull(AppContext.getLegacyTemplatePath(true));
    }

    @Test
    void getLegacyTemplatePath_normalModeUsesLegacyTemplate() {
        assertEquals(PlatformPaths.getLegacyTemplateFilePath(), AppContext.getLegacyTemplatePath(false));
    }

    @Test
    void loadFileSchemaTemplate_skipsLegacyFallbackWhenDisabled() throws Exception {
        Path legacyTemplate = Files.writeString(tempDir.resolve("legacy-template.htm"), "legacy-template");

        String template = AppContext.loadFileSchemaTemplate(
                tempDir.resolve("missing-template.htm").toString(), null, () -> "bundled-template");

        assertEquals("bundled-template", template);
        assertEquals("legacy-template", Files.readString(legacyTemplate));
    }

    @Test
    void loadFileSchemaTemplate_usesLegacyFallbackWhenEnabled() throws Exception {
        Path legacyTemplate = Files.writeString(tempDir.resolve("legacy-template.htm"), "legacy-template");

        String template = AppContext.loadFileSchemaTemplate(
                tempDir.resolve("missing-template.htm").toString(),
                legacyTemplate.toString(),
                () -> "bundled-template");

        assertEquals("legacy-template", template);
    }
}
