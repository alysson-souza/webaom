// Copyright (C) 2025 Alysson Souza
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

package epox.webaom.data;

import java.util.BitSet;

/**
 * Tracks which episodes of an anime have been watched using a BitSet.
 * Episodes are 1-indexed externally but 0-indexed internally.
 */
public class EpisodeProgress {
    private final BitSet bitSet;
    private final int episodeCount;

    public EpisodeProgress(int episodeCount) {
        this.episodeCount = episodeCount;
        this.bitSet = new BitSet(episodeCount);
    }

    /**
     * Gets the watched status of an episode.
     *
     * @param episodeNumber 1-based episode number
     * @return true if watched, false otherwise (including out of bounds)
     */
    public boolean get(int episodeNumber) {
        if (episodeNumber < 1 || episodeNumber > episodeCount) {
            return false;
        }
        return bitSet.get(episodeNumber - 1);
    }

    /**
     * Sets the watched status of an episode.
     *
     * @param episodeNumber 1-based episode number
     * @param watched true if watched, false if not
     * @return false if episodeNumber is out of bounds
     */
    public boolean set(int episodeNumber, boolean watched) {
        if (episodeNumber < 1 || episodeNumber > episodeCount) {
            return false;
        }
        bitSet.set(episodeNumber - 1, watched);
        return true;
    }

    /**
     * Marks the next unwatched episode as watched (left-to-right),
     * or unmarks the last watched episode (right-to-left).
     *
     * @param shouldSet true to set next unset bit, false to unset last set bit
     * @return true if a bit was changed
     */
    public boolean fill(boolean shouldSet) {
        if (shouldSet) {
            int nextClear = bitSet.nextClearBit(0);
            if (nextClear < episodeCount) {
                bitSet.set(nextClear);
                return true;
            }
            return false;
        } else {
            for (int i = episodeCount - 1; i >= 0; i--) {
                if (bitSet.get(i)) {
                    bitSet.clear(i);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Counts total watched episodes.
     */
    public int countSetBits() {
        return bitSet.cardinality();
    }

    /**
     * Checks if first episode is watched.
     */
    public boolean first() {
        return episodeCount > 0 && bitSet.get(0);
    }

    /**
     * Checks if last episode is watched.
     */
    public boolean last() {
        return episodeCount > 0 && bitSet.get(episodeCount - 1);
    }

    /**
     * Counts transitions between watched/unwatched states.
     * Used to detect gaps in viewing progress.
     */
    public int switchCount() {
        if (episodeCount == 0) return 0;

        boolean currentValue = bitSet.get(0);
        int transitions = 0;

        for (int i = 1; i < episodeCount; i++) {
            boolean nextValue = bitSet.get(i);
            if (nextValue != currentValue) {
                transitions++;
                currentValue = nextValue;
            }
        }
        return transitions;
    }

    /**
     * Checks if there's a gap (unwatched episode) before the last watched episode.
     * Scans right-to-left.
     */
    public boolean hasHole() {
        boolean foundSetBit = false;
        for (int i = episodeCount - 1; i >= 0; i--) {
            if (foundSetBit && !bitSet.get(i)) {
                return true;
            }
            if (bitSet.get(i)) {
                foundSetBit = true;
            }
        }
        return false;
    }

    /**
     * Clears all watched status.
     */
    public void reset() {
        bitSet.clear();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(episodeCount);
        for (int i = 0; i < episodeCount; i++) {
            sb.append(bitSet.get(i) ? '1' : '0');
        }
        return sb.toString();
    }
}
