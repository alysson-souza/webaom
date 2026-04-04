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

/**
 * Catppuccin Mocha — a rich, dark theme from the Catppuccin palette.
 *
 * @see <a href="https://github.com/catppuccin/catppuccin">Catppuccin</a>
 */
public class CatppuccinMochaLaf extends FlatDarkLaf {
    public static final String NAME = "Catppuccin Mocha";

    public static boolean setup() {
        return setup(new CatppuccinMochaLaf());
    }

    public static void installLafInfo() {
        installLafInfo(NAME, CatppuccinMochaLaf.class);
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return "Catppuccin Mocha — a soothing pastel dark theme";
    }
}
