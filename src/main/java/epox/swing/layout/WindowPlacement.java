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
import java.awt.Rectangle;
import java.util.Objects;

/** Immutable result of the window layout policy. */
public record WindowPlacement(Dimension minimumSize, Rectangle bounds) {
    public WindowPlacement {
        minimumSize = new Dimension(Objects.requireNonNull(minimumSize));
        bounds = new Rectangle(Objects.requireNonNull(bounds));
    }

    @Override
    public Dimension minimumSize() {
        return new Dimension(minimumSize);
    }

    @Override
    public Rectangle bounds() {
        return new Rectangle(bounds);
    }
}
