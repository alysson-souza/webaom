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

import com.bitzi.util.Base32;
import com.bitzi.util.TigerTree;

/**
 * Tiger Tree Hash (TTH) used by various P2P networks.
 */
public class TthHash implements HashAlgorithm {

    private final TigerTree tigerTree;
    private byte[] cachedDigest;

    public TthHash() {
        tigerTree = new TigerTree();
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        tigerTree.update(buffer, offset, length);
        cachedDigest = null;
    }

    @Override
    public void reset() {
        tigerTree.reset();
        cachedDigest = null;
    }

    @Override
    public String hexValue() {
        if (cachedDigest == null) {
            cachedDigest = tigerTree.digest();
        }
        return Base32.encode(cachedDigest).toLowerCase();
    }
}
