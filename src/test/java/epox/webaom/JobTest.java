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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.io.RemoteFileResolver;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        AppContext.jobCounter = new JobCounter();
        AppContext.jobs = new JobList();
    }

    @Test
    void jobGetExtension_noDot_returnsUnk() throws IOException {
        Path file = Files.createFile(tempDir.resolve("README"));
        Job job = new Job(file.toFile(), Job.HASHWAIT);

        assertEquals("unk", job.getExtension());
    }

    @Test
    void jobHide_negatedPattern_invertsMatch() throws IOException {
        Path file = Files.createFile(tempDir.resolve("AnimeEpisode01"));
        Job job = new Job(file.toFile(), Job.HASHWAIT);

        assertTrue(job.hide("!.*Anime.*"));
        assertFalse(job.hide(".*Anime.*"));
    }

    @Test
    void remoteUrlSurvivesExportAndTracksRenamesWithoutFollowingMovesToLocalStorage() throws Exception {
        RemoteFileResolver previous = AppContext.remoteFiles;
        try {
            Path mount = Files.createDirectories(tempDir.resolve("mount"));
            Path first = Files.writeString(mount.resolve("episode.mkv"), "video");
            AppContext.remoteFiles = new RemoteFileResolver("Linux", uri -> mount.toFile());
            URI uri = URI.create("smb://nas.example/Anime/episode.mkv");
            Job job = new Job(AppContext.remoteFiles.resolve(uri), Job.HASHWAIT);
            job.ed2kHash = "hash";
            String export = job.serialize();

            AppContext.remoteFiles = new RemoteFileResolver("Linux", location -> mount.toFile());
            Job imported = new Job(export.split("\\|", -1));
            assertEquals(uri, imported.getRemoteUri());
            imported.restoreRemoteFile(AppContext.remoteFiles.batch());
            assertEquals(first.toFile(), imported.currentFile);
            assertEquals(Job.H_PAUSED, imported.getHealth());

            imported.currentFile =
                    Files.move(first, mount.resolve("renamed #1.mkv")).toFile();
            assertEquals(URI.create("smb://nas.example/Anime/renamed%20%231.mkv"), imported.getRemoteUri());
            imported.currentFile = Files.move(imported.currentFile.toPath(), tempDir.resolve("local.mkv"))
                    .toFile();
            assertNull(imported.getRemoteUri());
            assertFalse(imported.serialize().contains("smb://"));
        } finally {
            AppContext.remoteFiles = previous;
        }
    }
}
