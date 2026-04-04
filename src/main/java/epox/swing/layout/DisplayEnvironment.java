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

package epox.swing.layout;

import java.awt.Dimension;
import java.awt.Window;

/** Adapter for monitor and usable screen information needed by window layout policy. */
public interface DisplayEnvironment {
    static DisplayEnvironment current() {
        return AwtDisplayEnvironment.INSTANCE;
    }

    UsableScreenBounds getUsableScreenBounds(Window window);

    /** Returns the UI component scale factor for the display (1.0 at standard DPI). */
    double getUiScaleFactor(Window window);

    /** Scales a base dimension by the UI scale factor for the given window's display. */
    default Dimension scaleDimension(Dimension base, Window window) {
        double factor = getUiScaleFactor(window);
        if (factor <= 0.0 || Math.abs(factor - 1.0) < 0.01) {
            return new Dimension(base);
        }
        return new Dimension(Math.max(1, (int) Math.round(base.width * factor)), Math.max(1, (int)
                Math.round(base.height * factor)));
    }
}
