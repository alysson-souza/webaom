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

import java.util.HexFormat;
import java.util.zip.CRC32;

/**
 * CRC32 hash using the JDK's hardware-accelerated {@link java.util.zip.CRC32}.
 *
 * <p>On Java 21+, the JDK implementation uses SSE4.2 intrinsics on x86 and CRC instructions on
 * ARM, making this the fastest CRC32 possible on the JVM.
 */
public class Crc32Hash implements HashAlgorithm {

    private final CRC32 crc32 = new CRC32();

    @Override
    public void update(byte[] buffer, int offset, int length) {
        crc32.update(buffer, offset, length);
    }

    @Override
    public void reset() {
        crc32.reset();
    }

    @Override
    public String hexValue() {
        return HexFormat.of().toHexDigits((int) crc32.getValue());
    }
}
