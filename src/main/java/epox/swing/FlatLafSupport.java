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

import com.formdev.flatlaf.FlatLaf;
import javax.swing.UIManager;

public final class FlatLafSupport {
    private static final String CUSTOM_DEFAULTS_SOURCE = "epox.webaom.theme";
    private static boolean customDefaultsRegistered;

    private FlatLafSupport() {
        // static only
    }

    public static synchronized void registerCustomDefaultsSource() {
        if (customDefaultsRegistered) {
            return;
        }

        FlatLaf.registerCustomDefaultsSource(CUSTOM_DEFAULTS_SOURCE);
        customDefaultsRegistered = true;
    }

    public static void applyTheme(FlatLafTheme theme) throws Exception {
        FlatLafTheme targetTheme = (theme == null) ? FlatLafTheme.LIGHT : theme;
        registerCustomDefaultsSource();
        UIManager.setLookAndFeel(targetTheme.getLookAndFeelClassName());
    }

    public static FlatLafTheme getCurrentTheme() {
        if (UIManager.getLookAndFeel() == null) {
            return FlatLafTheme.LIGHT;
        }

        return FlatLafTheme.fromLookAndFeelClassName(
                UIManager.getLookAndFeel().getClass().getName());
    }
}
