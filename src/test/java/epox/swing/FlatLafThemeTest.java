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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FlatLafThemeTest {

    @Test
    void darkThemesAreClassifiedCorrectly() {
        assertTrue(FlatLafTheme.DARK.isDark());
        assertTrue(FlatLafTheme.DARCULA.isDark());
        assertTrue(FlatLafTheme.CATPPUCCIN_MOCHA.isDark());
        assertTrue(FlatLafTheme.MAC_DARK.isDark());
    }

    @Test
    void lightThemesAreClassifiedCorrectly() {
        assertFalse(FlatLafTheme.LIGHT.isDark());
        assertFalse(FlatLafTheme.INTELLIJ.isDark());
        assertFalse(FlatLafTheme.CATPPUCCIN_LATTE.isDark());
        assertFalse(FlatLafTheme.MAC_LIGHT.isDark());
    }

    @Test
    void availableThemes_darkFilter_returnsOnlyDarkThemes() {
        FlatLafTheme[] dark = FlatLafTheme.availableThemes(true);
        assertTrue(dark.length > 0);
        for (FlatLafTheme theme : dark) {
            assertTrue(theme.isDark(), theme + " should be dark");
        }
    }

    @Test
    void availableThemes_lightFilter_returnsOnlyLightThemes() {
        FlatLafTheme[] light = FlatLafTheme.availableThemes(false);
        assertTrue(light.length > 0);
        for (FlatLafTheme theme : light) {
            assertFalse(theme.isDark(), theme + " should be light");
        }
    }

    @Test
    void defaultDarkThemeIsDark() {
        assertTrue(FlatLafTheme.getDefaultDarkTheme().isDark());
    }

    @Test
    void defaultLightThemeIsLight() {
        assertFalse(FlatLafTheme.getDefaultLightTheme().isDark());
    }

    @Test
    void fromOptionValueRaw_knownValue_returnsTheme() {
        assertEquals(FlatLafTheme.DARK, FlatLafTheme.fromOptionValueRaw("dark"));
        assertEquals(FlatLafTheme.CATPPUCCIN_MOCHA, FlatLafTheme.fromOptionValueRaw("catppuccin_mocha"));
        assertEquals(FlatLafTheme.LIGHT, FlatLafTheme.fromOptionValueRaw("light"));
    }

    @Test
    void fromOptionValueRaw_unknownValue_returnsNull() {
        assertNull(FlatLafTheme.fromOptionValueRaw("nonexistent"));
        assertNull(FlatLafTheme.fromOptionValueRaw(null));
        assertNull(FlatLafTheme.fromOptionValueRaw(""));
    }

    @Test
    void fromOptionValueRaw_caseInsensitive() {
        assertEquals(FlatLafTheme.DARK, FlatLafTheme.fromOptionValueRaw("DARK"));
        assertEquals(FlatLafTheme.CATPPUCCIN_LATTE, FlatLafTheme.fromOptionValueRaw("Catppuccin_Latte"));
    }

    @Test
    void fromOptionValueRaw_doesNotNormalizePlatform() {
        // MAC_DARK should be returned even on non-macOS (no platform normalization)
        FlatLafTheme result = FlatLafTheme.fromOptionValueRaw("mac_dark");
        assertEquals(FlatLafTheme.MAC_DARK, result);
    }
}
