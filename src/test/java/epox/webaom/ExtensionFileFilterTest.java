/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExtensionFileFilterTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void filtersExtensionsCaseInsensitivelyButAlwaysAllowsDirectories() throws Exception {
        ExtensionFileFilter filter = new ExtensionFileFilter("Videos", "MKV", "mp4");
        Path mkv = Files.createFile(temporaryDirectory.resolve("episode.MkV"));
        Path text = Files.createFile(temporaryDirectory.resolve("notes.txt"));
        Path noExtension = Files.createFile(temporaryDirectory.resolve("README"));
        Path trailingDot = Files.createFile(temporaryDirectory.resolve("episode."));

        assertTrue(filter.accept(temporaryDirectory.toFile()));
        assertTrue(filter.accept(mkv.toFile()));
        assertFalse(filter.accept(text.toFile()));
        assertFalse(filter.accept(noExtension.toFile()));
        assertFalse(filter.accept(trailingDot.toFile()));
        assertEquals("Videos", filter.getDescription());
        assertEquals(Set.of("mkv", "mp4"), filter.getExtensions());
        assertFalse(filter.acceptsAllExtensions());
    }

    @Test
    void iterableConstructorAndReturnedExtensionSetAreIndependent() {
        ExtensionFileFilter filter = new ExtensionFileFilter("Archives", List.of("ZIP", "tar"));

        Set<String> returned = filter.getExtensions();
        returned.clear();

        assertEquals(Set.of("zip", "tar"), filter.getExtensions());
        assertEquals(Set.of("zip", "tar"), Set.of(filter.getExtensionsString().split(", ")));
        assertTrue(filter.toString().startsWith("Archives ("));
    }

    @Test
    void emptyExtensionConfigurationsAcceptEveryFile() throws Exception {
        Path noExtension = Files.createFile(temporaryDirectory.resolve("README"));
        Path arbitrary = Files.createFile(temporaryDirectory.resolve("archive.unknown"));

        ExtensionFileFilter varargs = new ExtensionFileFilter("Everything", new String[0]);
        ExtensionFileFilter iterable = new ExtensionFileFilter("Everything", List.of());
        ExtensionFileFilter descriptionOnly = new ExtensionFileFilter("Everything");

        for (ExtensionFileFilter filter : List.of(varargs, iterable, descriptionOnly)) {
            assertTrue(filter.accept(noExtension.toFile()));
            assertTrue(filter.accept(arbitrary.toFile()));
            assertTrue(filter.acceptsAllExtensions());
            assertEquals("all files", filter.getExtensionsString());
            assertEquals("Everything (all files)", filter.toString());
        }
    }

    @Test
    void leadingDotFilenameIsTreatedAsHavingAnExtension() throws Exception {
        Path dotFile = Files.createFile(temporaryDirectory.resolve(".profile"));
        ExtensionFileFilter filter = new ExtensionFileFilter("Profiles", "profile");

        // TODO: Review whether a leading-dot filename should count as an extension for filtering.
        assertTrue(filter.accept(dotFile.toFile()));
    }
}
