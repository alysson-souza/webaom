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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

public enum FlatLafTheme {
    LIGHT("Light", "light", FlatLightLaf.class.getName()),
    DARK("Dark", "dark", FlatDarkLaf.class.getName());

    private final String displayName;
    private final String optionValue;
    private final String lookAndFeelClassName;

    FlatLafTheme(String displayName, String optionValue, String lookAndFeelClassName) {
        this.displayName = displayName;
        this.optionValue = optionValue;
        this.lookAndFeelClassName = lookAndFeelClassName;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getLookAndFeelClassName() {
        return lookAndFeelClassName;
    }

    public static FlatLafTheme fromOptionValue(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return LIGHT;
        }

        for (FlatLafTheme theme : values()) {
            if (theme.optionValue.equalsIgnoreCase(optionValue)) {
                return theme;
            }
            if (theme.lookAndFeelClassName.equals(optionValue)) {
                return theme;
            }
            if (theme.name().equalsIgnoreCase(optionValue)) {
                return theme;
            }
        }

        return LIGHT;
    }

    public static FlatLafTheme fromLookAndFeelClassName(String className) {
        if (className == null || className.isBlank()) {
            return LIGHT;
        }

        for (FlatLafTheme theme : values()) {
            if (theme.lookAndFeelClassName.equals(className)) {
                return theme;
            }
        }

        return LIGHT;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
