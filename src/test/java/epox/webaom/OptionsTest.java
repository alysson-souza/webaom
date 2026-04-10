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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.*;

import epox.swing.FlatLafTheme;
import org.junit.jupiter.api.Test;

class OptionsTest {

    @Test
    void optionsEquals_nullVsEmpty_doesNotThrow() {
        Options left = new Options();
        Options right = new Options();

        left.setString(Options.STR_THEME_LIGHT, null);
        right.setString(Options.STR_THEME_LIGHT, "");

        assertDoesNotThrow(() -> left.equals(right));
        assertTrue(left.equals(right));
        assertTrue(right.equals(left));
    }

    @Test
    void migrateThemeOptions_oldLightTheme_assignsDefaultDark() {
        Options options = new Options();
        options.setString(Options.STR_THEME_LIGHT, "light");
        options.setString(Options.STR_THEME_DARK, "");

        options.migrateThemeOptions();

        assertEquals("light", options.getString(Options.STR_THEME_LIGHT));
        assertEquals(FlatLafTheme.getDefaultDarkTheme().getOptionValue(), options.getString(Options.STR_THEME_DARK));
    }

    @Test
    void migrateThemeOptions_oldDarkTheme_movesToDarkSlot() {
        Options options = new Options();
        options.setString(Options.STR_THEME_LIGHT, "catppuccin_mocha");
        options.setString(Options.STR_THEME_DARK, "");

        options.migrateThemeOptions();

        assertEquals("catppuccin_mocha", options.getString(Options.STR_THEME_DARK));
        assertEquals(FlatLafTheme.getDefaultLightTheme().getOptionValue(), options.getString(Options.STR_THEME_LIGHT));
    }

    @Test
    void migrateThemeOptions_alreadyMigrated_doesNotChange() {
        Options options = new Options();
        options.setString(Options.STR_THEME_LIGHT, "intellij");
        options.setString(Options.STR_THEME_DARK, "darcula");

        options.migrateThemeOptions();

        assertEquals("intellij", options.getString(Options.STR_THEME_LIGHT));
        assertEquals("darcula", options.getString(Options.STR_THEME_DARK));
    }

    @Test
    void migrateThemeOptions_noThemeStored_setsDefaults() {
        Options options = new Options();
        options.setString(Options.STR_THEME_LIGHT, "");
        options.setString(Options.STR_THEME_DARK, "");

        options.migrateThemeOptions();

        assertEquals(FlatLafTheme.getDefaultLightTheme().getOptionValue(), options.getString(Options.STR_THEME_LIGHT));
        assertEquals(FlatLafTheme.getDefaultDarkTheme().getOptionValue(), options.getString(Options.STR_THEME_DARK));
    }

    @Test
    void migrateThemeOptions_unrecognizedTheme_treatedAsLight() {
        Options options = new Options();
        options.setString(Options.STR_THEME_LIGHT, "unknown_theme");
        options.setString(Options.STR_THEME_DARK, "");

        options.migrateThemeOptions();

        // Unrecognized theme stays in light slot
        assertEquals("unknown_theme", options.getString(Options.STR_THEME_LIGHT));
        assertEquals(FlatLafTheme.getDefaultDarkTheme().getOptionValue(), options.getString(Options.STR_THEME_DARK));
    }

    @Test
    void roundTrip_encodeDecode_preservesDualThemes() {
        Options original = new Options();
        original.setString(Options.STR_THEME_LIGHT, "catppuccin_latte");
        original.setString(Options.STR_THEME_DARK, "catppuccin_mocha");

        String encoded = original.encodeAllOptions();
        Options decoded = new Options();
        assertTrue(decoded.decodeAllOptions(encoded));

        assertEquals("catppuccin_latte", decoded.getString(Options.STR_THEME_LIGHT));
        assertEquals("catppuccin_mocha", decoded.getString(Options.STR_THEME_DARK));
    }

    @Test
    void decode_oldConfigWithOnlyOneThemeSlot_migratesCorrectly() {
        // Simulate old config: 20 string slots (no STR_THEME_DARK)
        Options old = new Options();
        old.setString(Options.STR_THEME_LIGHT, "darcula");
        String encoded = old.encodeAllOptions();

        // The old encoded data has 21 slots now (since STRING_OPTIONS_COUNT=21),
        // but if dark slot is empty the migration should fire
        Options loaded = new Options();
        assertTrue(loaded.decodeAllOptions(encoded));
        assertEquals("darcula", loaded.getString(Options.STR_THEME_DARK));
        assertEquals(FlatLafTheme.getDefaultLightTheme().getOptionValue(), loaded.getString(Options.STR_THEME_LIGHT));
    }
}
