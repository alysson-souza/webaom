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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileHasherTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void computesKnownEd2kAndOptionalHashesInOnePass() throws Exception {
        Path file = Files.writeString(temporaryDirectory.resolve("sample.mkv"), "abc");

        Map<FileHasher.Algorithm, String> hashes =
                new FileHasher().hash(file, EnumSet.of(FileHasher.Algorithm.ED2K, FileHasher.Algorithm.MD5));

        assertEquals("a448017aaf21d8525fc10ae87aa6729d", hashes.get(FileHasher.Algorithm.ED2K));
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hashes.get(FileHasher.Algorithm.MD5));
    }
}
