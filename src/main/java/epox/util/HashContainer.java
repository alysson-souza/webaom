// Copyright (C) 2005-2006 epoximator
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License
// as published by the Free Software Foundation; either version 2
// of the License, or (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

/*
 * Created on 11.06.05
 *
 * @version 	01 (1.07)
 * @author 		epoximator
 */

package epox.util;

import jonelo.jacksum.algorithm.AbstractChecksum;

/**
 * Container for multiple hash algorithms, allowing simultaneous computation of multiple hash types.
 * Stores hash algorithm names, computed hex values, and the checksum algorithm instances.
 */
public class HashContainer {
	/** Names of the hash algorithms (e.g., "MD5", "SHA-1") */
	private final String[] hashNames;
	/** Computed hex string values for each hash */
	private final String[] hashValues;
	/** Checksum algorithm instances for computing hashes */
	private final AbstractChecksum[] checksumAlgorithms;
	/** Number of hash algorithms in this container */
	private final int size;

	public HashContainer(int capacity) {
		size = capacity;
		hashNames = new String[size];
		hashValues = new String[size];
		checksumAlgorithms = new AbstractChecksum[size];
	}

	/**
	 * Adds a hash algorithm to this container at the specified index.
	 *
	 * @param index
	 *            the slot index for this algorithm
	 * @param name
	 *            the name of the hash algorithm
	 * @param checksumAlgorithm
	 *            the checksum algorithm instance
	 */
	public void add(int index, String name, AbstractChecksum checksumAlgorithm) {
		hashNames[index] = name;
		hashValues[index] = null;
		checksumAlgorithms[index] = checksumAlgorithm;
	}

	/**
	 * Updates all hash algorithms with the given data.
	 *
	 * @param buffer
	 *            the byte buffer containing data
	 * @param offset
	 *            the starting offset in the buffer
	 * @param length
	 *            the number of bytes to process
	 */
	public void update(byte[] buffer, int offset, int length) {
		for (int index = 0; index < size; index++) {
			checksumAlgorithms[index].update(buffer, offset, length);
		}
	}

	/**
	 * Computes final hash values from all algorithms and resets them for reuse.
	 */
	public void finalizeHashes() {
		for (int index = 0; index < size; index++) {
			hashValues[index] = checksumAlgorithms[index].getHexValue();
			checksumAlgorithms[index].reset();
		}
	}

	@Override
	protected void finalize() {
		finalizeHashes();
	}

	@Override
	public String toString() {
		String result = "";
		for (int index = 1; index < size; index++) {
			result += hashNames[index] + ": " + hashValues[index] + "\n";
		}
		if (result.isEmpty()) {
			return "";
		}
		return result.substring(0, result.length() - 1);
	}

	/**
	 * Retrieves the computed hash value for the specified algorithm name.
	 *
	 * @param name
	 *            the name of the hash algorithm (case-insensitive)
	 * @return the hex hash value, or null if not found
	 */
	public String getHex(String name) {
		for (int index = 0; index < size; index++) {
			if (hashNames[index].equalsIgnoreCase(name)) {
				return hashValues[index];
			}
		}
		return null;
	} /*
		 * private void reset(){
		 * for(int i=0; i<size; i++){
		 * hasht[i].reset();
		 * }
		 * }
		 */
}
