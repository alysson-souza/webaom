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

/** Pure policy for initial window size and placement in logical Swing coordinates. */
public final class WindowLayoutPolicy {
    public static final double DEFAULT_MAX_SCREEN_USAGE = 0.95;

    public WindowPlacement layoutWindow(Dimension preferredSize, Dimension minimumFloor, Rectangle usableBounds) {
        return layoutWindow(preferredSize, minimumFloor, usableBounds, DEFAULT_MAX_SCREEN_USAGE);
    }

    public WindowPlacement layoutWindow(
            Dimension preferredSize, Dimension minimumFloor, Rectangle usableBounds, double maxScreenUsage) {
        Objects.requireNonNull(preferredSize);
        Objects.requireNonNull(minimumFloor);
        Objects.requireNonNull(usableBounds);

        Rectangle usable = new Rectangle(usableBounds);
        double normalizedUsage = Math.max(0.1, Math.min(maxScreenUsage, 1.0));
        Dimension maximumSize =
                new Dimension(Math.max(1, (int) Math.floor(usable.width * normalizedUsage)), Math.max(1, (int)
                        Math.floor(usable.height * normalizedUsage)));

        Dimension normalizedPreferred = normalize(preferredSize);
        Dimension normalizedMinimum = clampToMaximum(normalize(minimumFloor), maximumSize);
        Dimension resolvedSize = clampToMaximum(max(normalizedPreferred, normalizedMinimum), maximumSize);

        Rectangle centeredBounds = new Rectangle(
                usable.x + Math.max(0, (usable.width - resolvedSize.width) / 2),
                usable.y + Math.max(0, (usable.height - resolvedSize.height) / 2),
                resolvedSize.width,
                resolvedSize.height);

        return new WindowPlacement(normalizedMinimum, centeredBounds);
    }

    public Rectangle expandCurrentBounds(Rectangle currentBounds, Dimension targetSize, Rectangle usableBounds) {
        Objects.requireNonNull(currentBounds);
        Objects.requireNonNull(targetSize);
        Objects.requireNonNull(usableBounds);

        Rectangle usable = new Rectangle(usableBounds);
        Dimension normalizedCurrent = normalize(currentBounds.getSize());
        Dimension normalizedTarget = normalize(targetSize);
        Dimension resolvedSize = clampToMaximum(max(normalizedCurrent, normalizedTarget), usable.getSize());

        int maxX = usable.x + Math.max(0, usable.width - resolvedSize.width);
        int maxY = usable.y + Math.max(0, usable.height - resolvedSize.height);
        int clampedX = Math.max(usable.x, Math.min(currentBounds.x, maxX));
        int clampedY = Math.max(usable.y, Math.min(currentBounds.y, maxY));

        return new Rectangle(clampedX, clampedY, resolvedSize.width, resolvedSize.height);
    }

    private static Dimension normalize(Dimension size) {
        return new Dimension(Math.max(1, size.width), Math.max(1, size.height));
    }

    private static Dimension clampToMaximum(Dimension size, Dimension maximumSize) {
        return new Dimension(Math.min(size.width, maximumSize.width), Math.min(size.height, maximumSize.height));
    }

    private static Dimension max(Dimension a, Dimension b) {
        return new Dimension(Math.max(a.width, b.width), Math.max(a.height, b.height));
    }
}
