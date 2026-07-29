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

import epox.webaom.hash.Crc32Hash;
import epox.webaom.hash.Ed2kHash;
import epox.webaom.hash.HashAlgorithm;
import epox.webaom.hash.Md5Hash;
import epox.webaom.hash.Sha1Hash;
import epox.webaom.hash.TthHash;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/** Computes WebAOM's supported hashes without depending on Swing workers. */
public final class FileHasher {
    private static final int BUFFER_SIZE = 3 * 1024 * 1024;

    public enum Algorithm {
        ED2K,
        CRC32,
        MD5,
        SHA1,
        TTH
    }

    public Map<Algorithm, String> hash(Path path, EnumSet<Algorithm> requested) throws IOException {
        EnumMap<Algorithm, HashAlgorithm> algorithms = new EnumMap<>(Algorithm.class);
        for (Algorithm algorithm : requested) {
            algorithms.put(algorithm, create(algorithm));
        }

        byte[] buffer = new byte[BUFFER_SIZE];
        try (InputStream input = Files.newInputStream(path)) {
            int read;
            while ((read = input.read(buffer)) >= 0) {
                for (HashAlgorithm algorithm : algorithms.values()) {
                    algorithm.update(buffer, 0, read);
                }
            }
        }

        EnumMap<Algorithm, String> result = new EnumMap<>(Algorithm.class);
        algorithms.forEach((name, algorithm) -> result.put(name, algorithm.hexValue()));
        return result;
    }

    private static HashAlgorithm create(Algorithm algorithm) {
        return switch (algorithm) {
            case ED2K -> new Ed2kHash();
            case CRC32 -> new Crc32Hash();
            case MD5 -> new Md5Hash();
            case SHA1 -> new Sha1Hash();
            case TTH -> new TthHash();
        };
    }
}
