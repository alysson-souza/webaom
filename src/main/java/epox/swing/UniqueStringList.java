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

package epox.swing;

import java.util.ArrayList;
import java.util.List;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 * A ListModel that stores unique strings. Duplicates are silently ignored when adding.
 */
public class UniqueStringList implements ListModel<String> {
    private final List<String> stringList;
    private final String separator;
    private ListDataListener listDataListener;

    public UniqueStringList(String separator) {
        this.separator = separator;
        stringList = new ArrayList<>();
    }

    public void add(String element) {
        if (!stringList.contains(element)) {
            int size = getSize();
            stringList.add(element);
            listDataListener.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, size, size));
        }
    }

    public void removeElementAt(int index) {
        String removed = stringList.remove(index);
        listDataListener.intervalRemoved(new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index, index));
    }

    public void reset() {
        int size = getSize();
        stringList.clear();
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
    public String getElementAt(int row) {
        return stringList.get(row);
    }

    public String getStringAt(int row) {
        return stringList.get(row);
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
