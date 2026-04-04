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
 * SHA-1 hash using the JDK's hardware-accelerated {@link MessageDigest}.
 */
public class Sha1Hash implements HashAlgorithm {

    private final MessageDigest sha1;

    public Sha1Hash() {
        try {
            sha1 = MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-1 algorithm not available", e);
        }
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        sha1.update(buffer, offset, length);
    }

    @Override
    public void reset() {
        sha1.reset();
    }

    @Override
    public String hexValue() {
        try {
            MessageDigest clone = (MessageDigest) sha1.clone();
            return HexFormat.of().formatHex(clone.digest());
        } catch (CloneNotSupportedException e) {
            return HexFormat.of().formatHex(sha1.digest());
        }
    }
}
