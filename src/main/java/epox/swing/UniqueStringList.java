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
 * Created on 22.01.05
 *
 * @version 	1.01
 * @author 		epoximator
 */
package epox.swing;

import java.util.Vector;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A ListModel that stores unique strings. Duplicates are silently ignored when adding.
 */
public class UniqueStringList implements ListModel {
    private final Vector<String> stringList;
    private final String separator;
    private ListDataListener listDataListener;

    public UniqueStringList(String separator) {
        this.separator = separator;
        stringList = new Vector<>();
    }

    public void add(String element) {
        if (!stringList.contains(element)) {
            int size = getSize();
            stringList.add(element);
            listDataListener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, size, size));
        }
    }

    public String removeElementAt(int index) {
        String removed = stringList.remove(index);
        listDataListener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
        return removed;
    }

    public void reset() {
        int size = getSize();
        stringList.removeAllElements();
        listDataListener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, 0, size - 1));
    }

    public boolean includes(String element) {
        return stringList.contains(element);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        Object[] strings = getStrings();
        for (Object string : strings) {
            result.append(string).append(separator);
        }
        return result.toString().trim();
    }

    public Object[] getStrings() {
        return stringList.toArray();
    }

    @Override
    public int getSize() {
        return stringList.size();
    }

    @Override
    public Object getElementAt(int row) {
        return stringList.elementAt(row);
    }

    public String getStringAt(int row) {
        return stringList.elementAt(row);
    }

    @Override
    public void addListDataListener(ListDataListener listener) {
        listDataListener = listener;
    }

    @Override
    public void removeListDataListener(ListDataListener listener) {
        listDataListener = null;
    }
}
