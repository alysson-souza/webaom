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

import gnu.crypto.hash.IMessageDigest;
import gnu.crypto.hash.MD4;
import java.util.HexFormat;

/**
 * ED2K (eDonkey/eMule) hash algorithm.
 *
 * <p>Computes the ed2k hash by splitting the input into 9500 KiB blocks, hashing each block with
 * MD4, then hashing the concatenation of all block hashes with MD4 again. Files smaller than one
 * block use the single MD4 hash directly.
 */
public class Ed2kHash implements HashAlgorithm {

    private static final int BLOCK_SIZE = 9728000; // 9500 * 1024

    private IMessageDigest md4;
    private IMessageDigest md4final;
    private final byte[] blockHash = new byte[16];
    private long length;

    public Ed2kHash() {
        md4 = new MD4();
        md4final = new MD4();
        length = 0;
    }

    @Override
    public void update(byte[] buffer, int offset, int length) {
        int remaining = length;
        int currentOffset = offset;

        while (remaining > 0) {
            int passed = (int) (this.length % BLOCK_SIZE);
            int space = BLOCK_SIZE - passed;
            int chunk = Math.min(space, remaining);

            md4.update(buffer, currentOffset, chunk);
            this.length += chunk;
            currentOffset += chunk;
            remaining -= chunk;

            if (this.length % BLOCK_SIZE == 0) {
                System.arraycopy(md4.digest(), 0, blockHash, 0, 16);
                md4final.update(blockHash, 0, 16);
                md4.reset();
            }
        }
    }

    @Override
    public void reset() {
        md4.reset();
        md4final.reset();
        length = 0;
    }

    @Override
    public String hexValue() {
        byte[] result;
        if (length < BLOCK_SIZE) {
            result = md4.digest();
        } else {
            IMessageDigest md4temp = (IMessageDigest) md4final.clone();
            md4temp.update(md4.digest(), 0, 16);
            result = md4temp.digest();
        }
        return HexFormat.of().formatHex(result);
    }
}
