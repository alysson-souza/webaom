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

/*
 * Created on 28.feb.2006 19:11:21
 * Filename: Path.java
 */
package epox.webaom.data;

/**
 * Represents a file system path in the data tree.
 * Used for organizing files by their parent directory.
 */
public class Path extends AniDBEntity {
    private final String pathString;

    public Path(String path) {
        pathString = path;
    }

    @Override
    public Object getKey() {
        return pathString;
    }

    public String toString() {
        return pathString;
    }
}
