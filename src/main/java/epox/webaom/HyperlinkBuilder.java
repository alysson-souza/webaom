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
 * Created on 29.05.05
 *
 * @version 	02 (1.09,1.06)
 * @author 		epoximator
 */
package epox.webaom;

import java.util.StringTokenizer;

public final class HyperlinkBuilder {

    /** Hex color code for warning/error text (red) */
    public static String warningColor = "F00000";
    /** Hex color code for name text (teal) */
    public static String nameColor = "006699";
    /** Hex color code for number text (navy) */
    public static String numberColor = "000080";

    private HyperlinkBuilder() {
        // static only
    }

    public static String createHyperlink(String url, String linkText) {
        return "<a href=\"" + url + "\">" + linkText + "</a>";
    }

    public static String wrapInColor(String hexColor, String text) {
        return "<font color=#" + hexColor + ">" + text + "</font>";
    }

    public static String formatAsError(String text) {
        return wrapInColor(warningColor, text);
    }

    public static String formatAsName(String text) {
        return wrapInColor(nameColor, text);
    }

    public static String formatAsName(Object obj) {
        if (obj != null) {
            return wrapInColor(nameColor, obj.toString());
        }
        return "null";
    }

    public static String formatAsNumber(int value) {
        return wrapInColor(numberColor, "" + value);
    }

    public static String formatAsNumber(String text) {
        return wrapInColor(numberColor, text);
    }

    public static String encodeColors() {
        return warningColor + Options.FIELD_SEPARATOR + nameColor + Options.FIELD_SEPARATOR + numberColor;
    }

    public static void decodeColors(String encodedColors) {
        if (encodedColors == null) {
            return;
        }
        StringTokenizer tokenizer = new StringTokenizer(encodedColors, Options.FIELD_SEPARATOR);
        if (tokenizer.countTokens() != 3) {
            return;
        }
        String colorToken = tokenizer.nextToken();
        if (colorToken.length() == 6) {
            warningColor = colorToken;
        }
        colorToken = tokenizer.nextToken();
        if (colorToken.length() == 6) {
            nameColor = colorToken;
        }
        colorToken = tokenizer.nextToken();
        if (colorToken.length() == 6) {
            numberColor = colorToken;
        }
    }
}
