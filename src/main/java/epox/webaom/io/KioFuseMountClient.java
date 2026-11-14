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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Optional command-line bridge to the session's KIO Fuse D-Bus service. */
final class KioFuseMountClient implements RemoteFileResolver.MountClient {
    private final List<String> commands;
    private final Duration timeout;

    KioFuseMountClient() {
        this(List.of("qdbus6", "qdbus", "qdbus-qt6", "qdbus-qt5"), Duration.ofSeconds(30));
    }

    KioFuseMountClient(List<String> commands, Duration timeout) {
        this.commands = List.copyOf(commands);
        this.timeout = timeout;
    }

    @Override
    public File mount(URI location) throws IOException {
        for (String command : commands) {
            Process process;
            try {
                // No shell: the URL is exactly one argument, including spaces or shell metacharacters.
                process = new ProcessBuilder(
                                command,
                                "org.kde.KIOFuse",
                                "/org/kde/KIOFuse",
                                "org.kde.KIOFuse.VFS.mountUrl",
                                location.toASCIIString())
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start();
            } catch (IOException e) {
                continue;
            }
            FutureTask<byte[]> output =
                    new FutureTask<>(() -> process.getInputStream().readNBytes(65537));
            Thread.ofVirtual().start(output);
            try {
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    throw new IOException(
                            "KIO Fuse took too long to mount the share. Check the connection and sign in through"
                                    + " Dolphin, then try again.");
                }
                byte[] bytes = output.get(1, TimeUnit.SECONDS);
                if (process.exitValue() != 0) {
                    throw new IOException(
                            "KIO Fuse could not mount the share. Check that KIO Fuse is installed and the share opens"
                                    + " in Dolphin, or use a mounted folder.");
                }
                String path = new String(bytes, StandardCharsets.UTF_8);
                // qdbus writes the returned string followed by one newline. Do not strip filename spaces.
                if (path.endsWith("\n")) path = path.substring(0, path.length() - 1);
                if (bytes.length > 65536
                        || path.isEmpty()
                        || path.indexOf('\0') >= 0
                        || path.contains("\n")
                        || path.contains("\r")
                        || !new File(path).isAbsolute()) {
                    throw new IOException("KIO Fuse returned an invalid mount path.");
                }
                return new File(path);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Opening the network share was interrupted.");
            } catch (ExecutionException | TimeoutException e) {
                throw new IOException("Could not read the KIO Fuse mount path.");
            } finally {
                if (process.isAlive()) process.destroyForcibly();
                process.getInputStream().close();
                output.cancel(true);
            }
        }
        throw new IOException(
                "SMB URL drops need KIO Fuse and qdbus on Linux. Install them, or mount the share and drop files from"
                        + " its local folder.");
    }
}
