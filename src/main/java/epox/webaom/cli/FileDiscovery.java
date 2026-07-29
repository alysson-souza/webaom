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

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Resolves files and recursively scans directories in stable order. */
public final class FileDiscovery {
    public List<Path> discover(Collection<Path> inputs, Set<String> extensions) throws IOException {
        LinkedHashSet<Path> discovered = new LinkedHashSet<>();
        for (Path input : inputs) {
            Path absolute = input.toAbsolutePath().normalize();
            if (!Files.exists(absolute)) {
                throw new IOException("Path does not exist: " + input);
            }
            Path resolved = Files.isSymbolicLink(absolute) ? absolute.toRealPath() : absolute;
            if (Files.isDirectory(resolved, LinkOption.NOFOLLOW_LINKS)) {
                scanDirectory(resolved, extensions, discovered);
            } else if (Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS)) {
                discovered.add(resolved);
            }
        }
        ArrayList<Path> sorted = new ArrayList<>(discovered);
        sorted.sort(Comparator.comparing(Path::toString));
        return sorted;
    }

    private static void scanDirectory(Path directory, Set<String> extensions, Set<Path> output) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                if (attributes.isRegularFile() && accepts(file, extensions)) {
                    output.add(file.toAbsolutePath().normalize());
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception) throws IOException {
                throw traversalFailure(file, exception);
            }
        });
    }

    static IOException traversalFailure(Path file, IOException cause) {
        return new IOException("Could not access " + file + ": " + cause.getMessage(), cause);
    }

    static boolean accepts(Path path, Set<String> extensions) {
        if (extensions.isEmpty()) {
            return true;
        }
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 && extensions.contains(name.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public static Set<String> normalizeExtensions(Object[] values) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (Object value : values) {
            if (value != null) {
                String extension = value.toString().trim().toLowerCase(Locale.ROOT);
                if (extension.startsWith(".")) {
                    extension = extension.substring(1);
                }
                if (!extension.isEmpty()) {
                    result.add(extension);
                }
            }
        }
        return result;
    }
}
