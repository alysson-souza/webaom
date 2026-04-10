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

import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;

public enum FlatLafTheme {
    LIGHT("Light", "light", FlatLightLaf.class.getName(), false, false),
    DARK("Dark", "dark", FlatDarkLaf.class.getName(), false, true),
    INTELLIJ("IntelliJ", "intellij", FlatIntelliJLaf.class.getName(), false, false),
    DARCULA("Darcula", "darcula", FlatDarculaLaf.class.getName(), false, true),
    CATPPUCCIN_LATTE("Catppuccin Latte", "catppuccin_latte", CatppuccinLatteLaf.class.getName(), false, false),
    CATPPUCCIN_MOCHA("Catppuccin Mocha", "catppuccin_mocha", CatppuccinMochaLaf.class.getName(), false, true),
    MAC_LIGHT("macOS Light", "mac_light", FlatMacLightLaf.class.getName(), true, false),
    MAC_DARK("macOS Dark", "mac_dark", FlatMacDarkLaf.class.getName(), true, true);

    private final String displayName;
    private final String optionValue;
    private final String lookAndFeelClassName;
    private final boolean macOnly;
    private final boolean dark;

    FlatLafTheme(String displayName, String optionValue, String lookAndFeelClassName, boolean macOnly, boolean dark) {
        this.displayName = displayName;
        this.optionValue = optionValue;
        this.lookAndFeelClassName = lookAndFeelClassName;
        this.macOnly = macOnly;
        this.dark = dark;
    }

    public boolean isDark() {
        return dark;
    }

    public String getOptionValue() {
        return optionValue;
    }

    public String getLookAndFeelClassName() {
        return lookAndFeelClassName;
    }

    public static FlatLafTheme fromOptionValue(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return getDefaultTheme();
        }

        for (FlatLafTheme theme : values()) {
            if (theme.optionValue.equalsIgnoreCase(optionValue)) {
                return theme.isSupportedOnCurrentPlatform() ? theme : getDefaultTheme();
            }
            if (theme.lookAndFeelClassName.equals(optionValue)) {
                return theme.isSupportedOnCurrentPlatform() ? theme : getDefaultTheme();
            }
            if (theme.name().equalsIgnoreCase(optionValue)) {
                return theme.isSupportedOnCurrentPlatform() ? theme : getDefaultTheme();
            }
        }

        return getDefaultTheme();
    }

    /**
     * Resolves a theme from its stored option value without platform normalization.
     * Returns null if the value doesn't match any known theme. Used for migration
     * where the raw classification matters regardless of the current platform.
     */
    public static FlatLafTheme fromOptionValueRaw(String optionValue) {
        if (optionValue == null || optionValue.isBlank()) {
            return null;
        }
        for (FlatLafTheme theme : values()) {
            if (theme.optionValue.equalsIgnoreCase(optionValue)
                    || theme.lookAndFeelClassName.equals(optionValue)
                    || theme.name().equalsIgnoreCase(optionValue)) {
                return theme;
            }
        }
        return null;
    }

    public static FlatLafTheme[] availableThemes() {
        int count = 0;
        for (FlatLafTheme theme : values()) {
            if (theme.isSupportedOnCurrentPlatform()) {
                count++;
            }
        }

        FlatLafTheme[] available = new FlatLafTheme[count];
        int index = 0;
        for (FlatLafTheme theme : values()) {
            if (theme.isSupportedOnCurrentPlatform()) {
                available[index++] = theme;
            }
        }
        return available;
    }

    public boolean isSupportedOnCurrentPlatform() {
        if (!macOnly) {
            return true;
        }

        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac");
    }

    public static FlatLafTheme fromLookAndFeelClassName(String className) {
        if (className == null || className.isBlank()) {
            return getDefaultTheme();
        }

        for (FlatLafTheme theme : values()) {
            if (theme.lookAndFeelClassName.equals(className)) {
                return theme.isSupportedOnCurrentPlatform() ? theme : getDefaultTheme();
            }
        }

        return getDefaultTheme();
    }

    public static FlatLafTheme getDefaultTheme() {
        return getDefaultLightTheme();
    }

    public static FlatLafTheme getDefaultLightTheme() {
        return isMacOS() ? MAC_LIGHT : LIGHT;
    }

    public static FlatLafTheme getDefaultDarkTheme() {
        return isMacOS() ? MAC_DARK : DARK;
    }

    /** Returns available themes filtered by dark/light classification. */
    public static FlatLafTheme[] availableThemes(boolean dark) {
        FlatLafTheme[] all = availableThemes();
        int count = 0;
        for (FlatLafTheme theme : all) {
            if (theme.dark == dark) {
                count++;
            }
        }
        FlatLafTheme[] filtered = new FlatLafTheme[count];
        int index = 0;
        for (FlatLafTheme theme : all) {
            if (theme.dark == dark) {
                filtered[index++] = theme;
            }
        }
        return filtered;
    }

    private static boolean isMacOS() {
        String osName = System.getProperty("os.name", "").toLowerCase();
        return osName.contains("mac");
    }

    @Override
    public String toString() {
        return displayName;
    }
}
