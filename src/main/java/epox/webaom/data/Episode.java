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

import epox.util.StringUtilities;

/**
 * Represents an episode entry from AniDB.
 */
public class Episode extends AniDBEntity {
    /** Title display priority: 0=english, 1=romaji, 2=kanji. */
    public static int titlePriority = 0;
    /** Episode number/code (e.g., "1", "S1", "C2"). */
    public String num;
    /** Romaji title. */
    public String rom;
    /** Kanji/Japanese title. */
    public String kan;
    /** English title. */
    public String eng;

    public Episode(int id) {
        this.id = id;
    }

    public Episode(String[] fields) {
        int index = 0;
        id = StringUtilities.i(fields[index++]);
        num = fields[index++].intern();
        eng = fields[index++];
        rom = StringUtilities.n(fields[index++]);
        kan = StringUtilities.n(fields[index++]);
    }

    public String toString() {
        return switch (titlePriority) {
            case 1 -> num + " - " + (rom == null ? eng : rom);
            case 2 -> num + " - " + (kan == null ? eng : kan);
            default -> num + " - " + eng;
        };
    }

    public String serialize() {
        return "" + id + S + num + S + eng + S + rom + S + kan;
    }

    @Override
    public int compareTo(AniDBEntity obj) {
        if (obj instanceof Episode other) {
            try {
                int thisNum = Integer.parseInt(num);
                int otherNum = Integer.parseInt(other.num);
                return thisNum - otherNum;
            } catch (Exception ex) {
                return num.compareTo(other.num);
            }
        }
        return super.compareTo(obj);
    }
}
