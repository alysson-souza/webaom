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

/**
 * Common interface for all hash/checksum algorithms used in the hashing pipeline.
 *
 * <p>Implementations must be safe to use from a single thread but are NOT required to be
 * thread-safe. Each {@code HashTask} worker creates its own instances.
 */
public interface HashAlgorithm {

    /** Feed a chunk of data into the hash computation. */
    void update(byte[] buffer, int offset, int length);

    /** Reset the algorithm state so it can be reused for a new computation. */
    void reset();

    /** Return the computed hash as a lowercase hex string. */
    String hexValue();
}
