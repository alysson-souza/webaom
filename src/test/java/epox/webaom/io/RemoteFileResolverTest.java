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

package epox.webaom.io;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteFileResolverTest {
    @TempDir
    Path directory;

    @Test
    void smbFileUsesMountedShareAndDecodesItsPath() throws Exception {
        List<URI> requests = new ArrayList<>();
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            requests.add(uri);
            return directory.toFile();
        });

        File file = resolver.resolve(URI.create("smb://nas.example/Anime/Season%201/日本語%20%2B%20%231.mkv"));

        assertEquals(directory.resolve("Season 1/日本語 + #1.mkv").toFile(), file);
        assertEquals(List.of(URI.create("smb://nas.example/Anime")), requests);
    }

    @Test
    void serverNamesWithUnderscoresResolveAndStillRejectPasswords() throws Exception {
        List<URI> requests = new ArrayList<>();
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            requests.add(uri);
            return directory.toFile();
        });

        File file = resolver.resolve(URI.create("smb://my_nas/Anime/a.mkv"));

        assertEquals(directory.resolve("a.mkv").toFile(), file);
        assertEquals(List.of(URI.create("smb://my_nas/Anime")), requests);
        assertEquals(URI.create("smb://my_nas/Anime/a.mkv"), resolver.remoteUri(file));
        assertThrows(IOException.class, () -> resolver.resolve(URI.create("smb://user:secret@my_nas/Anime/a.mkv")));
        assertEquals(1, requests.size());
    }

    @Test
    void pathsThePlatformCannotRepresentAreNotRemote() throws Exception {
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> directory.toFile());
        File unrepresentable = new File(directory.toFile(), "saved\0elsewhere.mkv");

        assertNull(resolver.remoteUri(unrepresentable));
        resolver.resolve(URI.create("smb://nas.example/Anime/a.mkv"));
        assertNull(resolver.remoteUri(unrepresentable));
    }

    @Test
    void aMountedShareIsRequestedAgainOnlyAfterItsFolderDisappears() throws Exception {
        List<URI> requests = new ArrayList<>();
        Path mount = Files.createDirectory(directory.resolve("mount"));
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            requests.add(uri);
            return mount.toFile();
        });

        resolver.resolve(URI.create("smb://nas.example/Anime/a.mkv"));
        assertEquals(mount.resolve("b.mkv").toFile(), resolver.resolve(URI.create("smb://nas.example/Anime/b.mkv")));
        assertEquals(1, requests.size());

        Files.delete(mount);
        resolver.resolve(URI.create("smb://nas.example/Anime/c.mkv"));
        assertEquals(2, requests.size());
    }

    @Test
    void aBatchRequestsAnUnavailableShareOnceAndALaterLookupTriesAgain() {
        List<URI> requests = new ArrayList<>();
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            requests.add(uri);
            throw new IOException("Share is unavailable.");
        });

        RemoteFileResolver.Batch batch = resolver.batch();
        for (String name : List.of("a.mkv", "b.mkv")) {
            URI uri = URI.create("smb://nas.example/Anime/" + name);
            assertEquals(
                    "Share is unavailable.",
                    assertThrows(IOException.class, () -> batch.resolve(uri)).getMessage());
        }
        assertEquals(1, requests.size());

        assertThrows(IOException.class, () -> resolver.resolve(URI.create("smb://nas.example/Anime/a.mkv")));
        assertEquals(2, requests.size());
    }

    @Test
    void localFilesNeverNeedDesktopServicesOnAnyPlatform() throws Exception {
        File local = directory.resolve("episode + #1.mkv").toFile();
        for (String os : List.of("Windows 11", "Mac OS X", "Linux", "FreeBSD")) {
            RemoteFileResolver resolver = new RemoteFileResolver(os, uri -> {
                throw new AssertionError("A local file must not start KIO Fuse");
            });
            assertEquals(local, resolver.resolve(local.toURI()));
            assertEquals(
                    local,
                    resolver.resolve(
                            URI.create("file://localhost" + local.toURI().getRawPath())));
            assertNull(resolver.remoteUri(local));
        }
    }

    @Test
    void unsupportedPlatformsAndSchemesDoNotStartDesktopServices() {
        for (String os : List.of("Windows 11", "Mac OS X", "FreeBSD")) {
            RemoteFileResolver resolver = new RemoteFileResolver(os, uri -> {
                throw new AssertionError("KIO Fuse must only run on Linux");
            });
            assertTrue(
                    assertThrows(IOException.class, () -> resolver.resolve(URI.create("smb://nas.example/Anime/a.mkv")))
                            .getMessage()
                            .contains("Mount the share"));
        }
        RemoteFileResolver linux = new RemoteFileResolver("Linux", uri -> {
            throw new AssertionError("Unsupported URLs must not start a mount");
        });
        assertThrows(IOException.class, () -> linux.resolve(URI.create("https://example.test/a.mkv")));
    }

    @Test
    void invalidSmbUrlsAreRejectedBeforeStartingAMount() {
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            throw new AssertionError("Invalid URLs must not start a mount");
        });
        for (String uri : List.of(
                "smb://nas.example/",
                "smb://nas.example//Anime",
                "smb://nas.example/Anime/../private.mkv",
                "smb://nas.example/Anime/%2e%2e/private.mkv",
                "smb://nas.example/Anime/a%2fb.mkv",
                "smb://nas.example/Anime/a%00.mkv",
                "smb://user:secret@nas.example/Anime/a.mkv",
                "smb://nas.example/Anime/a.mkv?download")) {
            assertThrows(IOException.class, () -> resolver.resolve(URI.create(uri)));
        }
    }

    @Test
    void directoryChildrenAndRenamesKeepStableUrlsButOtherFilesStayLocal() throws Exception {
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> directory.toFile());
        resolver.resolve(URI.create("smb://user@nas.example/My%20Share/Season/"));
        assertEquals(
                URI.create("smb://user@nas.example/My%20Share/Season/renamed%20%231.mkv"),
                resolver.remoteUri(directory.resolve("Season/renamed #1.mkv").toFile()));
        assertNull(resolver.remoteUri(
                directory.resolveSibling(directory.getFileName() + "-other").toFile()));
    }

    @Test
    void linuxWithoutKioReportsTheMountFailure() {
        RemoteFileResolver resolver = new RemoteFileResolver("Linux", uri -> {
            throw new IOException("KIO Fuse is unavailable");
        });
        assertEquals(
                "KIO Fuse is unavailable",
                assertThrows(IOException.class, () -> resolver.resolve(URI.create("smb://nas.example/Anime/a.mkv")))
                        .getMessage());
    }
}
