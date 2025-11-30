/*
 * Created on 10.mar.2006 15:53:35
 * Filename: Progr.java
 */
package epox.util;

public class Bits {
    /** Total number of bits in this bit set. */
    private final int length;
    /** Byte array storing the bit values (each byte holds 8 bits). */
    private final byte[] bitStorage;

    public Bits(int length) {
        this.length = length;
        this.bitStorage = new byte[(length + 7) / 8];
    }

    public boolean get(int index) {
        if (index >= length) {
            return false;
        }
        return (1 & (bitStorage[index / 8] >> (index % 8))) == 1;
    }

    /**
     * Sets the value at specified index.
     *
     * @param index
     *            Index of bit.
     * @param value
     *            Value of bit.
     * @return false if the index is out of bounds.
     */
    public boolean set(int index, boolean value) {
        if (index >= length) {
            return false;
        }
        if (value) {
            bitStorage[index / 8] |= (byte) (1 << (index % 8));
        } else {
            bitStorage[index / 8] &= (byte) ~(1 << (index % 8));
        }
        return true;
    }

    /**
     * Set/unset, the next unset/set bit from 'left to right'/'right to left'.
     *
     * @param shouldSet
     *            true to set the next unset bit, false to unset the last set bit
     * @return true if one bit was changed.
     */
    public boolean fill(boolean shouldSet) {
        if (shouldSet) {
            for (int index = 0; index < length; index++) {
                if (!get(index)) {
                    set(index, true);
                    return true;
                }
            }
            return false;
        }
        for (int index = length - 1; index >= 0; index--) {
            if (get(index)) {
                set(index, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(get(index) ? 1 : 0);
        }
        return builder.toString();
    }

    /**
     * Counts all set bits.
     *
     * @return Number of set bits.
     */
    public int countSetBits() {
        int count = 0;
        for (int index = 0; index < length; index++) {
            if (get(index)) {
                count++;
            }
        }
        return count;
    }

    /**
     * First bit.
     *
     * @return The value of the first bit.
     */
    public boolean first() {
        return get(0);
    }

    /**
     * Last bit.
     *
     * @return The value of the last bit.
     */
    public boolean last() {
        return get(length - 1);
    }

    /**
     * Counts the number of transitions between 0 and 1 (or 1 and 0).
     *
     * @return Number of bit value changes in the sequence.
     */
    public int switchCount() {
        boolean currentValue = get(0);
        int transitions = 0;
        for (int index = 1; index < length; index++) {
            if (get(index) != currentValue) {
                transitions++;
                currentValue = !currentValue;
            }
        }
        return transitions;
    }

    /**
     * Checks if there is a "hole" (unset bit) before the last set bit.
     * Scans from right to left, and returns true if an unset bit is found
     * after encountering a set bit.
     *
     * @return true if there is a gap in the set bits.
     */
    public boolean hasHole() {
        boolean foundSetBit = false;
        for (int index = length - 1; index >= 0; index--) {
            if (foundSetBit) {
                if (!get(index)) {
                    return true;
                }
            } else if (get(index)) {
                foundSetBit = true;
            }
        }
        return false;
    }

    /**
     * Clears all bits, setting them to 0.
     */
    public void reset() {
        for (int index = 0; index < bitStorage.length; index++) {
            bitStorage[index] = 0;
        }
    }
}
