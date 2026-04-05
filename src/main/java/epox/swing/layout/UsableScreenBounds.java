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

import java.awt.Insets;
import java.awt.Rectangle;
import java.util.Objects;

/** Screen bounds plus the inset-adjusted area available to application windows. */
public record UsableScreenBounds(Rectangle screenBounds, Rectangle usableBounds) {
    public UsableScreenBounds {
        screenBounds = new Rectangle(Objects.requireNonNull(screenBounds));
        usableBounds = new Rectangle(Objects.requireNonNull(usableBounds));
    }

    public static UsableScreenBounds fromScreenBounds(Rectangle screenBounds, Insets insets) {
        Objects.requireNonNull(insets);
        Rectangle screen = new Rectangle(Objects.requireNonNull(screenBounds));
        Rectangle usable = new Rectangle(
                screen.x + insets.left,
                screen.y + insets.top,
                Math.max(1, screen.width - insets.left - insets.right),
                Math.max(1, screen.height - insets.top - insets.bottom));
        return new UsableScreenBounds(screen, usable);
    }

    @Override
    public Rectangle screenBounds() {
        return new Rectangle(screenBounds);
    }

    @Override
    public Rectangle usableBounds() {
        return new Rectangle(usableBounds);
    }
}
