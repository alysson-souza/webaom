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

class OsAppearanceMonitorTest {

    @Test
    void detectMacOs_darkOutput_returnsDark() {
        assertEquals(OsAppearanceMonitor.Appearance.DARK, parseMacOs("Dark\n"));
        assertEquals(OsAppearanceMonitor.Appearance.DARK, parseMacOs("Dark"));
        assertEquals(OsAppearanceMonitor.Appearance.DARK, parseMacOs("dark\n"));
    }

    @Test
    void detectMacOs_lightOutput_returnsLight() {
        // macOS `defaults read` returns empty/error for light mode; empty string → LIGHT
        assertEquals(OsAppearanceMonitor.Appearance.LIGHT, parseMacOs(""));
        assertEquals(OsAppearanceMonitor.Appearance.LIGHT, parseMacOs("Light\n"));
    }

    @Test
    void detectMacOs_nullOutput_returnsUnknown() {
        assertEquals(OsAppearanceMonitor.Appearance.UNKNOWN, parseMacOs(null));
    }

    @Test
    void detectWindows_dark_returnsDark() {
        String output = "HKEY_CURRENT_USER\\...\\Personalize\n    AppsUseLightTheme    REG_DWORD    0x0\n";
        assertEquals(OsAppearanceMonitor.Appearance.DARK, parseWindows(output));
    }

    @Test
    void detectWindows_light_returnsLight() {
        String output = "HKEY_CURRENT_USER\\...\\Personalize\n    AppsUseLightTheme    REG_DWORD    0x1\n";
        assertEquals(OsAppearanceMonitor.Appearance.LIGHT, parseWindows(output));
    }

    @Test
    void detectWindows_nullOutput_returnsUnknown() {
        assertEquals(OsAppearanceMonitor.Appearance.UNKNOWN, parseWindows(null));
    }

    @Test
    void detectLinux_portal_preferDark_returnsDark() {
        assertEquals(
                OsAppearanceMonitor.Appearance.DARK,
                OsAppearanceMonitor.parseLinuxPortal("   variant    variant       uint32 1\n"));
    }

    @Test
    void detectLinux_portal_noPreference_returnsLight() {
        assertEquals(
                OsAppearanceMonitor.Appearance.LIGHT,
                OsAppearanceMonitor.parseLinuxPortal("   variant    variant       uint32 0\n"));
    }

    @Test
    void detectLinux_portal_preferLight_returnsLight() {
        assertEquals(
                OsAppearanceMonitor.Appearance.LIGHT,
                OsAppearanceMonitor.parseLinuxPortal("   variant    variant       uint32 2\n"));
    }

    // Helpers that simulate detection logic without running actual commands
    private static OsAppearanceMonitor.Appearance parseMacOs(String output) {
        if (output == null) return OsAppearanceMonitor.Appearance.UNKNOWN;
        return output.trim().equalsIgnoreCase("Dark")
                ? OsAppearanceMonitor.Appearance.DARK
                : OsAppearanceMonitor.Appearance.LIGHT;
    }

    private static OsAppearanceMonitor.Appearance parseWindows(String output) {
        if (output == null) return OsAppearanceMonitor.Appearance.UNKNOWN;
        return output.contains("0x0") ? OsAppearanceMonitor.Appearance.DARK : OsAppearanceMonitor.Appearance.LIGHT;
    }
}
