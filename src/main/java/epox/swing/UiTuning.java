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

import javax.swing.UIManager;
import javax.swing.plaf.InsetsUIResource;

/** Small UI tweaks to keep control sizing consistent across LookAndFeels. */
public final class UiTuning {
    private UiTuning() {
        // static only
    }

    public static void applyForCurrentLookAndFeel() {
        // Text fields can end up visually shorter than buttons/combos (especially on HiDPI);
        // a small internal margin makes heights feel consistent.
        InsetsUIResource fieldMargins = new InsetsUIResource(3, 6, 3, 6);
        UIManager.put("TextField.margin", fieldMargins);
        UIManager.put("FormattedTextField.margin", fieldMargins);
        UIManager.put("PasswordField.margin", fieldMargins);

        // Nimbus uses contentMargins; setting both is harmless for other LAFs.
        UIManager.put("TextField.contentMargins", fieldMargins);
        UIManager.put("FormattedTextField.contentMargins", fieldMargins);
        UIManager.put("PasswordField.contentMargins", fieldMargins);

        // Some LAFs read combo padding for sizing.
        UIManager.put("ComboBox.padding", new InsetsUIResource(2, 6, 2, 6));
    }
}
