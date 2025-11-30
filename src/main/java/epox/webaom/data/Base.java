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
 * Created on 25.des.2005 16:37:58
 * Filename: aParent.java
 */
package epox.webaom.data;

import epox.util.StringUtilities;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

public class Base implements Comparable {
    /** Separator character used for serialization. */
    protected static final char S = '|';
    /** Map of child objects keyed by their unique identifier. */
    private final HashMap<Object, Base> childMap = new HashMap<>();
    /** Unique identifier for this object. */
    public int id;
    /** Total size in bytes (aggregated from children). */
    public long totalSize;
    /** Sorted array of children (populated by buildSortedChildArray()). */
    private Object[] sortedChildren = null;

    public static Base getInst(String[] arg) {
        if (arg == null) {
            return null;
        }
        return null;
    }

    public Base get(int index) {
        if (sortedChildren == null) {
            return null;
        }
        return (Base) sortedChildren[index];
    }

    public boolean has(Base child) {
        return childMap.containsKey(child.getKey());
    }

    public Base get(Object key) {
        return childMap.get(key);
    }

    public void add(Base child) {
        Object key = child.getKey();
        if (!childMap.containsKey(key)) {
            childMap.put(key, child);
            totalSize += child.totalSize;
        } // else U.err("Base: Tried to add ex obj: "+child+" ("+this+")");
    }

    public void remove(Base child) {
        Object key = child.getKey();
        if (childMap.containsKey(key)) {
            childMap.remove(key);
            totalSize -= child.totalSize;
            if (totalSize < 0) {
                StringUtilities.err("Base: Negative size: " + child + " (" + this + ")");
                totalSize = 0;
            }
        } else {
            StringUtilities.err("Base: Tried to remove non ex obj: " + child + " (" + this + ")");
        }
    }

    public void clear() {
        totalSize = 0;
        Iterator<Base> it = childMap.values().iterator();
        while (it.hasNext()) {
            it.next().clear();
        }

        childMap.clear();
    }

    public void dump(String prefix) {
        System.out.println(prefix + this);
        Iterator<Base> it = childMap.values().iterator();
        while (it.hasNext()) {
            it.next().dump(prefix + ".");
        }
    }

    public int size() {
        return childMap.size();
    }

    public String toString() {
        return "Press F5 to update.";
    }

    public void buildSortedChildArray() {
        sortedChildren = childMap.values().toArray();
        Arrays.sort(sortedChildren);
    }

    public int compareTo(Object other) {
        return toString().compareTo(other.toString());
    }

    public Object getKey() {
        return Integer.valueOf(id);
    }

    public String serialize() {
        return null;
    }
}
