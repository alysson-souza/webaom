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

import java.util.HashMap;
import java.util.Map;

public class AttributeMap extends HashMap<String, String> {
    @Override
    public String put(String key, String value) {
        if (value == null) {
            return null;
        }
        return super.put(key, value);
    }

    public void put(String key, int value) {
        put(key, Integer.toString(value));
    }

    public void put(String key, long value) {
        put(key, Long.toString(value));
    }

    public String[][] toArray() {
        int len = size();
        String[][] res = new String[len][2];
        int i = 0;
        for (Map.Entry<String, String> e : entrySet()) {
            res[i][0] = e.getKey();
            res[i][1] = e.getValue();
            i++;
        }
        return res;
    }
}
