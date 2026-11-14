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

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

@EnabledOnOs(OS.LINUX)
class KioFuseMountClientTest {
    @TempDir
    Path directory;

    private String helper(String body) throws Exception {
        Path helper = Files.writeString(directory.resolve("qdbus-test"), "#!/bin/sh\n" + body);
        assertTrue(helper.toFile().setExecutable(true));
        return helper.toString();
    }

    @Test
    void invokesTheMountMethodWithoutAShellAndPreservesPathSpaces() throws Exception {
        String executable = helper("printf '%s\\n' \"$@\" > \"$0.args\"\nprintf '/tmp/mount with spaces \\n'\n");
        URI uri = URI.create("smb://host/share%20$(touch%20unexpected)");
        KioFuseMountClient client = new KioFuseMountClient(List.of(executable), Duration.ofSeconds(2));

        assertEquals(Path.of("/tmp/mount with spaces ").toFile(), client.mount(uri));
        assertEquals(
                List.of(
                        "org.kde.KIOFuse",
                        "/org/kde/KIOFuse",
                        "org.kde.KIOFuse.VFS.mountUrl",
                        "smb://host/share%20$(touch%20unexpected)"),
                Files.readAllLines(Path.of(executable + ".args")));
    }

    @Test
    void missingHelperAndServiceFailureGiveActionableErrors() throws Exception {
        KioFuseMountClient missing =
                new KioFuseMountClient(List.of(directory.resolve("absent").toString()), Duration.ofSeconds(2));
        assertTrue(assertThrows(IOException.class, () -> missing.mount(URI.create("smb://host/share")))
                .getMessage()
                .contains("qdbus"));
        KioFuseMountClient failed = new KioFuseMountClient(List.of(helper("exit 1\n")), Duration.ofSeconds(2));
        assertTrue(assertThrows(IOException.class, () -> failed.mount(URI.create("smb://host/share")))
                .getMessage()
                .contains("Dolphin"));
    }

    @Test
    void malformedOutputAndHungHelperAreRejected() throws Exception {
        KioFuseMountClient malformed =
                new KioFuseMountClient(List.of(helper("printf 'not-an-absolute-path\\n'\n")), Duration.ofSeconds(2));
        assertThrows(IOException.class, () -> malformed.mount(URI.create("smb://host/share")));
        KioFuseMountClient hung = new KioFuseMountClient(List.of(helper("exec sleep 10\n")), Duration.ofMillis(100));
        assertTimeoutPreemptively(Duration.ofSeconds(3), () -> {
            assertTrue(assertThrows(IOException.class, () -> hung.mount(URI.create("smb://host/share")))
                    .getMessage()
                    .contains("too long"));
        });
    }
}
