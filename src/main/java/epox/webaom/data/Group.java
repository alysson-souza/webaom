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
 * Created on 03.10.05
 *
 * @version 	01
 * @author 		epoximator
 */
package epox.webaom.data;

/**
 * Represents a fansub/release group from AniDB.
 */
public class Group extends AniDBEntity {
    /** Singleton representing no group (raw release). */
    public static final Group NONE = new Group(0);
    /** Full group name. */
    public String name = "NONE";
    /** Short group name/tag (e.g., "a-f" for "Anime-Fansubs"). */
    public String shortName = "";

    public Group(int id) {
        this.id = id;
    }

    public String serialize() {
        return name + S + shortName;
    }

    public String toString() {
        return name + "|" + shortName;
    }
}
