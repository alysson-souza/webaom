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

package epox.webaom.hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import gnu.crypto.hash.IMessageDigest;
import gnu.crypto.hash.MD4;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** Correctness tests for all {@link HashAlgorithm} implementations. */
class HashAlgorithmTest {

    // --- CRC32 known test vectors ---

    @Test
    void crc32_emptyInput() {
        Crc32Hash hash = new Crc32Hash();
        assertEquals("00000000", hash.hexValue());
    }

    @Test
    void crc32_knownVector() {
        // "123456789" → CRC32 = 0xCBF43926 (ISO 3309)
        Crc32Hash hash = new Crc32Hash();
        hash.update("123456789".getBytes(StandardCharsets.US_ASCII), 0, 9);
        assertEquals("cbf43926", hash.hexValue());
    }

    @Test
    void crc32_chunkedInput() {
        Crc32Hash hash = new Crc32Hash();
        byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
        hash.update(data, 0, 5);
        hash.update(data, 5, 4);
        assertEquals("cbf43926", hash.hexValue());
    }

    // --- MD5 known test vectors (RFC 1321) ---

    @Test
    void md5_emptyInput() {
        Md5Hash hash = new Md5Hash();
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", hash.hexValue());
    }

    @Test
    void md5_abc() {
        Md5Hash hash = new Md5Hash();
        byte[] data = "abc".getBytes(StandardCharsets.US_ASCII);
        hash.update(data, 0, data.length);
        assertEquals("900150983cd24fb0d6963f7d28e17f72", hash.hexValue());
    }

    @Test
    void md5_multipleCallsWithoutReset() {
        Md5Hash hash = new Md5Hash();
        byte[] data = "abc".getBytes(StandardCharsets.US_ASCII);
        hash.update(data, 0, data.length);
        // hexValue() should be idempotent (uses clone internally)
        assertEquals(hash.hexValue(), hash.hexValue());
    }

    // --- SHA-1 known test vectors (FIPS 180-4) ---

    @Test
    void sha1_emptyInput() {
        Sha1Hash hash = new Sha1Hash();
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", hash.hexValue());
    }

    @Test
    void sha1_abc() {
        Sha1Hash hash = new Sha1Hash();
        byte[] data = "abc".getBytes(StandardCharsets.US_ASCII);
        hash.update(data, 0, data.length);
        assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", hash.hexValue());
    }

    // --- ED2K tests ---
    // ED2K splits into 9500 KiB blocks, MD4-hashes each, then MD4-hashes the concatenation.
    // For files < one block, ED2K hash = MD4 of the data.

    private static final int ED2K_BLOCK_SIZE = 9728000; // 9500 * 1024

    /** Reference ED2K computation using raw MD4 — independent of Ed2kHash. */
    private static String referenceEd2k(byte[] data) {
        if (data.length < ED2K_BLOCK_SIZE) {
            // Single incomplete block: hash = MD4 of the data
            IMessageDigest md4 = new MD4();
            if (data.length > 0) md4.update(data, 0, data.length);
            return HexFormat.of().formatHex(md4.digest());
        }

        // Multi-block path: process each complete block
        IMessageDigest md4final = new MD4();
        int offset;
        for (offset = 0; offset + ED2K_BLOCK_SIZE <= data.length; offset += ED2K_BLOCK_SIZE) {
            IMessageDigest md4 = new MD4();
            md4.update(data, offset, ED2K_BLOCK_SIZE);
            md4final.update(md4.digest(), 0, 16);
        }

        // Always hash the trailing data (partial last block, or empty if exact multiple)
        IMessageDigest md4tail = new MD4();
        if (offset < data.length) {
            md4tail.update(data, offset, data.length - offset);
        }
        md4final.update(md4tail.digest(), 0, 16);

        return HexFormat.of().formatHex(md4final.digest());
    }

    @Test
    void ed2k_emptyInput() {
        Ed2kHash hash = new Ed2kHash();
        assertEquals(referenceEd2k(new byte[0]), hash.hexValue());
    }

    @Test
    void ed2k_smallFile_nonTrivial() {
        byte[] data = new byte[1024 * 1024]; // 1 MB
        new Random(42).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        hash.update(data, 0, data.length);

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    @Test
    void ed2k_smallFile_chunkedInput() {
        // Feed the same data in small chunks — must produce the same hash
        byte[] data = new byte[1024 * 1024]; // 1 MB
        new Random(42).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        int chunkSize = 64 * 1024; // 64 KB chunks
        for (int pos = 0; pos < data.length; pos += chunkSize) {
            int len = Math.min(chunkSize, data.length - pos);
            hash.update(data, pos, len);
        }

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    @Test
    void ed2k_exactlyOneBlock() {
        byte[] data = new byte[ED2K_BLOCK_SIZE]; // exactly 9500 KiB
        new Random(55).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        int chunkSize = 3 * 1024 * 1024;
        for (int pos = 0; pos < data.length; pos += chunkSize) {
            int len = Math.min(chunkSize, data.length - pos);
            hash.update(data, pos, len);
        }

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    @Test
    void ed2k_twoBlocks() {
        // Just over one block — triggers multi-block logic
        byte[] data = new byte[ED2K_BLOCK_SIZE + 1];
        new Random(66).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        int chunkSize = 3 * 1024 * 1024;
        for (int pos = 0; pos < data.length; pos += chunkSize) {
            int len = Math.min(chunkSize, data.length - pos);
            hash.update(data, pos, len);
        }

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    @Test
    void ed2k_largeFile_multipleBlocks() {
        byte[] data = new byte[20 * 1024 * 1024]; // ~2.1 blocks
        new Random(99).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        int chunkSize = 3 * 1024 * 1024;
        for (int pos = 0; pos < data.length; pos += chunkSize) {
            int len = Math.min(chunkSize, data.length - pos);
            hash.update(data, pos, len);
        }

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    @Test
    void ed2k_singleUpdateSpanningMultipleBlocks() {
        // Single update() call with data spanning >2 blocks
        byte[] data = new byte[ED2K_BLOCK_SIZE * 3 + 1234];
        new Random(123).nextBytes(data);

        Ed2kHash hash = new Ed2kHash();
        hash.update(data, 0, data.length);

        assertEquals(referenceEd2k(data), hash.hexValue());
    }

    // --- TTH tests ---

    @Test
    void tth_deterministicOutput() {
        TthHash hash = new TthHash();
        byte[] data = new byte[1024 * 1024]; // 1 MB
        new Random(77).nextBytes(data);
        hash.update(data, 0, data.length);

        // Value verified against the old epox.util.TTH implementation
        assertEquals("ru7bto2vhp6r6tzg4jp5yt2uw6lvchb7bfo5eny", hash.hexValue());
    }

    // --- Reset behavior ---

    @Test
    void crc32_resetProducesConsistentResults() {
        Crc32Hash hash = new Crc32Hash();
        byte[] data = "test data".getBytes(StandardCharsets.US_ASCII);

        hash.update(data, 0, data.length);
        String first = hash.hexValue();

        hash.reset();
        hash.update(data, 0, data.length);
        assertEquals(first, hash.hexValue(), "Reset should produce identical results");
    }

    @Test
    void ed2k_resetProducesConsistentResults() {
        Ed2kHash hash = new Ed2kHash();
        byte[] data = "test data".getBytes(StandardCharsets.US_ASCII);

        hash.update(data, 0, data.length);
        String first = hash.hexValue();

        hash.reset();
        hash.update(data, 0, data.length);
        assertEquals(first, hash.hexValue(), "Reset should produce identical results");
    }

    // --- hexValue() idempotency across all implementations ---

    @Test
    void crc32_hexValueIsIdempotent() {
        Crc32Hash hash = new Crc32Hash();
        hash.update("abc".getBytes(StandardCharsets.US_ASCII), 0, 3);
        String first = hash.hexValue();
        assertEquals(first, hash.hexValue());
        assertEquals(first, hash.hexValue());
    }

    @Test
    void ed2k_hexValueIsIdempotent_smallFile() {
        Ed2kHash hash = new Ed2kHash();
        hash.update("abc".getBytes(StandardCharsets.US_ASCII), 0, 3);
        String first = hash.hexValue();
        assertEquals(first, hash.hexValue());
        assertEquals(first, hash.hexValue());
    }

    @Test
    void ed2k_hexValueIsIdempotent_multiBlock() {
        byte[] data = new byte[ED2K_BLOCK_SIZE + 100];
        new Random(88).nextBytes(data);
        Ed2kHash hash = new Ed2kHash();
        hash.update(data, 0, data.length);
        String first = hash.hexValue();
        assertEquals(first, hash.hexValue());
        assertEquals(first, hash.hexValue());
    }

    @Test
    void sha1_hexValueIsIdempotent() {
        Sha1Hash hash = new Sha1Hash();
        hash.update("abc".getBytes(StandardCharsets.US_ASCII), 0, 3);
        String first = hash.hexValue();
        assertEquals(first, hash.hexValue());
        assertEquals(first, hash.hexValue());
    }

    @Test
    void tth_hexValueIsIdempotent() {
        TthHash hash = new TthHash();
        hash.update("abc".getBytes(StandardCharsets.US_ASCII), 0, 3);
        String first = hash.hexValue();
        assertEquals(first, hash.hexValue());
        assertEquals(first, hash.hexValue());
    }
}
