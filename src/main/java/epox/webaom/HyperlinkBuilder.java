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

import epox.swing.ThemeColorSupport;
import java.awt.Color;

public final class HyperlinkBuilder {
    public static final String CSS_CLASS_WARNING = "log-warning";
    public static final String CSS_CLASS_NAME = "log-name";
    public static final String CSS_CLASS_NUMBER = "log-number";

    private static final Color BASE_WARNING_COLOR = new Color(240, 0, 0);
    private static final Color BASE_NAME_COLOR = new Color(0, 102, 153);
    private static final Color BASE_NUMBER_COLOR = new Color(0, 0, 128);
    private static final double MINIMUM_LOG_CONTRAST = 4.5;

    private HyperlinkBuilder() {
        // static only
    }

    public static String createHyperlink(String url, String linkText) {
        return "<a href=\"" + url + "\">" + linkText + "</a>";
    }

    public static String wrapInCssClass(String cssClass, String text) {
        return "<span class=\"" + cssClass + "\">" + text + "</span>";
    }

    public static String formatAsError(String text) {
        return wrapInCssClass(CSS_CLASS_WARNING, text);
    }

    public static String formatAsName(String text) {
        return wrapInCssClass(CSS_CLASS_NAME, text);
    }

    public static String formatAsName(Object obj) {
        if (obj != null) {
            return wrapInCssClass(CSS_CLASS_NAME, obj.toString());
        }
        return "null";
    }

    public static String formatAsNumber(int value) {
        return wrapInCssClass(CSS_CLASS_NUMBER, "" + value);
    }

    public static String formatAsNumber(String text) {
        return wrapInCssClass(CSS_CLASS_NUMBER, text);
    }

    public static Color resolveWarningColor() {
        return resolveSemanticColor(BASE_WARNING_COLOR);
    }

    public static Color resolveNameColor() {
        return resolveSemanticColor(BASE_NAME_COLOR);
    }

    public static Color resolveNumberColor() {
        return resolveSemanticColor(BASE_NUMBER_COLOR);
    }

    private static Color resolveSemanticColor(Color baseColor) {
        Color background = ThemeColorSupport.colorOrDefault(
                null,
                Color.white,
                "EditorPane.background",
                "TextPane.background",
                "TextArea.background",
                "Panel.background");
        Color foreground = ThemeColorSupport.colorOrDefault(
                null,
                Color.black,
                "EditorPane.foreground",
                "TextPane.foreground",
                "TextArea.foreground",
                "Label.foreground");
        return ThemeColorSupport.ensureContrast(baseColor, background, foreground, MINIMUM_LOG_CONTRAST);
    }
}
