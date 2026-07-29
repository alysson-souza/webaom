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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OptionsExplicitPathTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void savesExplicitConfigAtomicallyAndLoadsItAgain() throws Exception {
        Path path = temporaryDirectory.resolve("x");
        Options written = new Options(path);
        written.setString(Options.STR_HOST_URL, "api.anidb.net");
        written.setInteger(Options.INT_REMOTE_PORT, 9000);

        assertTrue(written.saveToFileUnchecked());

        Options loaded = new Options(path);
        assertTrue(loaded.loadFromFileQuietly());
        assertEquals("api.anidb.net", loaded.getString(Options.STR_HOST_URL));
        assertEquals(9000, loaded.getInteger(Options.INT_REMOTE_PORT));
        try (var files = Files.list(temporaryDirectory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void savingThroughSymlinkPreservesTheLinkAndUpdatesItsTarget() throws Exception {
        Path target = temporaryDirectory.resolve("actual-config");
        Path link = temporaryDirectory.resolve("config");
        Options original = new Options(target);
        original.setString(Options.STR_HOST_URL, "old.example");
        assertTrue(original.saveToFileUnchecked());
        try {
            Files.createSymbolicLink(link, target.getFileName());
        } catch (IOException | UnsupportedOperationException exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }

        Options linked = new Options(link);
        assertTrue(linked.loadFromFileQuietly());
        linked.setString(Options.STR_HOST_URL, "new.example");
        assertTrue(linked.saveToFileUnchecked());

        assertTrue(Files.isSymbolicLink(link));
        Options loaded = new Options(target);
        assertTrue(loaded.loadFromFileQuietly());
        assertEquals("new.example", loaded.getString(Options.STR_HOST_URL));
    }
}
