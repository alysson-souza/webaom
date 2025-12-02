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

package epox.webaom.data;

import epox.util.StringUtilities;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Base class for all AniDB data entities (Anime, Episode, Group, File, etc.).
 * Provides hierarchical parent-child relationships and serialization support.
 */
public class AniDBEntity implements Comparable<AniDBEntity> {
    /** Separator character used for serialization. */
    protected static final char S = '|';

    private static final Logger LOGGER = Logger.getLogger(AniDBEntity.class.getName());
    private static final boolean IS_MAC =
            System.getProperty("os.name").toLowerCase().contains("mac");
    /** Map of child objects keyed by their unique identifier. */
    private final HashMap<Object, AniDBEntity> childMap = new HashMap<>();
    /** Unique identifier for this object. */
    protected int id;
    /** Total size in bytes (aggregated from children). */
    protected long totalSize;
    /** Sorted array of children (populated by buildSortedChildArray()). */
    private Object[] sortedChildren = null;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public AniDBEntity get(int index) {
        if (sortedChildren == null) {
            return null;
        }
        return (AniDBEntity) sortedChildren[index];
    }

    public boolean has(AniDBEntity child) {
        return childMap.containsKey(child.getKey());
    }

    public AniDBEntity get(Object key) {
        return childMap.get(key);
    }

    public void add(AniDBEntity child) {
        Object key = child.getKey();
        AniDBEntity existing = childMap.putIfAbsent(key, child);
        if (existing == null) {
            totalSize += child.totalSize;
        }
    }

    public void remove(AniDBEntity child) {
        Object key = child.getKey();
        AniDBEntity removed = childMap.remove(key);
        if (removed != null) {
            totalSize -= child.totalSize;
            if (totalSize < 0) {
                StringUtilities.err("AniDBEntity: Negative size: " + child + " (" + this + ")");
                totalSize = 0;
            }
        } else {
            StringUtilities.err("AniDBEntity: Tried to remove non ex obj: " + child + " (" + this + ")");
        }
    }

    public void clear() {
        totalSize = 0;
        for (AniDBEntity aniDBEntity : childMap.values()) {
            aniDBEntity.clear();
        }

        childMap.clear();
    }

    public void dump(String prefix) {
        LOGGER.info(() -> prefix + this);
        for (AniDBEntity aniDBEntity : childMap.values()) {
            aniDBEntity.dump(prefix + ".");
        }
    }

    public int size() {
        return childMap.size();
    }

    public String toString() {
        return IS_MAC ? "Press ⌘R to update." : "Press F5 to update.";
    }

    public void buildSortedChildArray() {
        sortedChildren = childMap.values().toArray();
        Arrays.sort(sortedChildren);
    }

    @Override
    public int compareTo(AniDBEntity other) {
        return toString().compareTo(other.toString());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        AniDBEntity other = (AniDBEntity) obj;
        return id == other.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public Object getKey() {
        return id;
    }

    public String serialize() {
        return null;
    }
}
