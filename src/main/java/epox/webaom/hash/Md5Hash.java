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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * MD5 hash using the JDK's hardware-accelerated {@link MessageDigest}.
 *
 * <p>Modern JVMs (Java 8+) use CPU intrinsics for MD5 that outperform any pure-Java implementation.
 */
public class Md5Hash implements HashAlgorithm {

    private final MessageDigest md5;
    private byte[] cachedDigest;

    public Md5Hash() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("MD5 algorithm not available", e);
        }
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        md5.update(buffer, offset, length);
        cachedDigest = null;
    }

    @Override
    public void reset() {
        md5.reset();
        cachedDigest = null;
    }

    @Override
    public String hexValue() {
        if (cachedDigest == null) {
            try {
                cachedDigest = ((MessageDigest) md5.clone()).digest();
            } catch (CloneNotSupportedException e) {
                cachedDigest = md5.digest();
            }
        }
        return HexFormat.of().formatHex(cachedDigest);
    }
}
