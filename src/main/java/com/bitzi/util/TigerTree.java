/*
 * Original: (PD) 2003 The Bitzi Corporation - http://bitzi.com/publicdomain
 * Improved: Jacksum 3.6.0 memory-efficient stack-based approach
 *
 * Implementation of THEX tree hash algorithm, with Tiger as the internal algorithm
 * (using the approach as revised in December 2002, to add unique prefixes to leaf
 * and node operations).
 *
 * This version uses a stack-based approach that collapses nodes as it goes,
 * rather than storing all intermediate hashes. This is more memory efficient
 * for large files.
 */
package com.bitzi.util;

import gnu.crypto.hash.Tiger;
import java.security.DigestException;
import java.security.MessageDigest;
import java.util.ArrayList;

public class TigerTree extends MessageDigest {
    private static final int BLOCKSIZE = 1024;
    private static final int HASHSIZE = 24;

    /** Marker for empty stack slots */
    private static final byte[] MARKER = new byte[0];

    /** 1024 byte buffer */
    private final byte[] buffer;

    /** Buffer offset */
    private int bufferOffset;

    /** Number of bytes hashed until now. */
    private long byteCount;

    /** Internal Tiger MD instance */
    private final Tiger tiger;

    /** Interim tree node hash values (stack-based) */
    private ArrayList<byte[]> nodes;

    /** Constructor */
    public TigerTree() {
        super("TigerTree");
        buffer = new byte[BLOCKSIZE];
        bufferOffset = 0;
        byteCount = 0;
        tiger = new Tiger();
        nodes = new ArrayList<>();
    }

    @Override
    protected int engineGetDigestLength() {
        return HASHSIZE;
    }

    @Override
    protected void engineUpdate(byte in) {
        byteCount += 1;
        buffer[bufferOffset++] = in;
        if (bufferOffset == BLOCKSIZE) {
            blockUpdate();
            bufferOffset = 0;
        }
    }

    @Override
    protected void engineUpdate(byte[] in, int offset, int length) {
        byteCount += length;
        nodes.ensureCapacity(log2Ceil(byteCount / BLOCKSIZE));

        if (bufferOffset > 0) {
            int remaining = BLOCKSIZE - bufferOffset;
            if (remaining > length) {
                remaining = length;
            }
            System.arraycopy(in, offset, buffer, bufferOffset, remaining);
            bufferOffset += remaining;
            if (bufferOffset == BLOCKSIZE) {
                blockUpdate();
                bufferOffset = 0;
            }
            length -= remaining;
            offset += remaining;
        }

        while (length >= BLOCKSIZE) {
            blockUpdate(in, offset, BLOCKSIZE);
            length -= BLOCKSIZE;
            offset += BLOCKSIZE;
        }

        if (length > 0) {
            System.arraycopy(in, offset, buffer, 0, length);
            bufferOffset = length;
        }
    }

    @Override
    protected byte[] engineDigest() {
        byte[] hash = new byte[HASHSIZE];
        try {
            engineDigest(hash, 0, HASHSIZE);
        } catch (DigestException e) {
            return null;
        }
        return hash;
    }

    @Override
    protected int engineDigest(byte[] buf, int offset, int len) throws DigestException {
        if (len < HASHSIZE) throw new DigestException();

        // hash any remaining fragments
        blockUpdate();

        byte[] result = collapse();
        System.arraycopy(result, 0, buf, offset, HASHSIZE);
        engineReset();
        return HASHSIZE;
    }

    /** Collapse the tree stack to a single root hash */
    private byte[] collapse() {
        byte[] last = null;
        for (int i = 0; i < nodes.size(); i++) {
            byte[] current = nodes.get(i);
            if (current == MARKER) {
                continue;
            }

            if (last == null) {
                last = current;
            } else {
                tiger.reset();
                tiger.update((byte) 1); // node prefix
                tiger.update(current, 0, current.length);
                tiger.update(last, 0, last.length);
                last = tiger.digest();
            }

            nodes.set(i, MARKER);
        }
        return last;
    }

    @Override
    protected void engineReset() {
        bufferOffset = 0;
        byteCount = 0;
        nodes.clear();
        tiger.reset();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    private void blockUpdate() {
        blockUpdate(buffer, 0, bufferOffset);
    }

    /**
     * Update the internal state with a single block of size 1024 (or less, in final block).
     */
    private void blockUpdate(byte[] buf, int pos, int len) {
        tiger.reset();
        tiger.update((byte) 0); // leaf prefix
        tiger.update(buf, pos, len);
        if ((len == 0) && (!nodes.isEmpty())) {
            return; // don't remember a zero-size hash except at very beginning
        }
        byte[] digestBytes = tiger.digest();
        push(digestBytes);
    }

    /** Push a hash onto the stack, collapsing as needed */
    private void push(byte[] data) {
        if (!nodes.isEmpty()) {
            for (int i = 0; i < nodes.size(); i++) {
                byte[] node = nodes.get(i);
                if (node == MARKER) {
                    nodes.set(i, data);
                    return;
                }

                tiger.reset();
                tiger.update((byte) 1);
                tiger.update(node, 0, node.length);
                tiger.update(data, 0, data.length);
                data = tiger.digest();
                nodes.set(i, MARKER);
            }
        }
        nodes.add(data);
    }

    /** Calculate ceil(log2(number)) for capacity estimation */
    private static int log2Ceil(long number) {
        int n = 0;
        while (number > 1) {
            number++;
            number >>>= 1;
            n++;
        }
        return n;
    }
}
