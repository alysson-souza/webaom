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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDiscoveryTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void recursivelyFiltersDeduplicatesAndSortsFiles() throws Exception {
        Path nested = Files.createDirectories(temporaryDirectory.resolve("nested"));
        Path second = Files.writeString(nested.resolve("02.MKV"), "two");
        Path first = Files.writeString(temporaryDirectory.resolve("01.mkv"), "one");
        Files.writeString(temporaryDirectory.resolve("notes.txt"), "ignored");

        List<Path> files = new FileDiscovery().discover(List.of(temporaryDirectory, first), Set.of("mkv"));

        assertEquals(List.of(first.toAbsolutePath(), second.toAbsolutePath()), files);
    }

    @Test
    void explicitFilesAreNotFilteredByExtension() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("unknown.data"), "content");

        List<Path> files = new FileDiscovery().discover(List.of(file), Set.of("mkv"));

        assertEquals(List.of(file.toAbsolutePath()), files);
    }

    @Test
    void resolvesAnExplicitDirectorySymlinkWithoutFollowingNestedSymlinks() throws Exception {
        Path directory = Files.createDirectories(temporaryDirectory.resolve("library"));
        Path file = Files.writeString(directory.resolve("episode.mkv"), "content");
        Path externalDirectory = Files.createDirectories(temporaryDirectory.resolve("external"));
        Files.writeString(externalDirectory.resolve("other.mkv"), "other");
        Path nestedLink = directory.resolve("external-link");
        Path link = temporaryDirectory.resolve("library-link");
        try {
            Files.createSymbolicLink(nestedLink, externalDirectory);
            Files.createSymbolicLink(link, directory);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }

        List<Path> files = new FileDiscovery().discover(List.of(link), Set.of("mkv"));

        assertEquals(List.of(file.toRealPath()), files);
    }

    @Test
    void resolvesAnExplicitFileSymlinkToTheFileBeingProcessed() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("episode.mkv"), "content");
        Path link = temporaryDirectory.resolve("linked-episode.mkv");
        try {
            Files.createSymbolicLink(link, file);
        } catch (Exception exception) {
            Assumptions.assumeTrue(false, "Symbolic links are unavailable: " + exception.getMessage());
        }

        List<Path> files = new FileDiscovery().discover(List.of(link), Set.of("mkv"));

        assertEquals(List.of(file.toRealPath()), files);
    }

    @Test
    void traversalFailuresIncludeTheUnreadablePath() {
        Path unreadable = temporaryDirectory.resolve("unreadable");

        IOException exception = assertThrows(IOException.class, () -> {
            throw FileDiscovery.traversalFailure(unreadable, new IOException("permission denied"));
        });

        assertEquals("Could not access " + unreadable + ": permission denied", exception.getMessage());
    }
}
