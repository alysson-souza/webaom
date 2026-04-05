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
import java.util.Objects;

/** Applies display-aware window placement produced by the shared layout policy. */
public final class WindowLayoutSupport {
    private WindowLayoutSupport() {
        // utility class
    }

    public static WindowPlacement placeCentered(
            Window window,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor) {
        return placeCentered(window, window, displayEnvironment, windowLayoutPolicy, minimumFloor, true);
    }

    public static WindowPlacement placeCentered(
            Window window,
            Window referenceWindow,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor) {
        return placeCentered(window, referenceWindow, displayEnvironment, windowLayoutPolicy, minimumFloor, true);
    }

    public static WindowPlacement placeCentered(
            Window window,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor,
            boolean applyMinimumSize) {
        return placeCentered(window, window, displayEnvironment, windowLayoutPolicy, minimumFloor, applyMinimumSize);
    }

    public static WindowPlacement placeCentered(
            Window window,
            Window referenceWindow,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor,
            boolean applyMinimumSize) {
        return placeCenteredAt(
                window,
                resolvePreferredSize(window),
                referenceWindow,
                displayEnvironment,
                windowLayoutPolicy,
                minimumFloor,
                applyMinimumSize);
    }

    public static WindowPlacement placeCenteredAt(
            Window window,
            Dimension initialSize,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor) {
        return placeCenteredAt(window, initialSize, window, displayEnvironment, windowLayoutPolicy, minimumFloor, true);
    }

    public static WindowPlacement placeCenteredAt(
            Window window,
            Dimension initialSize,
            Window referenceWindow,
            DisplayEnvironment displayEnvironment,
            WindowLayoutPolicy windowLayoutPolicy,
            Dimension minimumFloor,
            boolean applyMinimumSize) {
        Objects.requireNonNull(window);
        Objects.requireNonNull(initialSize);
        Objects.requireNonNull(displayEnvironment);
        Objects.requireNonNull(windowLayoutPolicy);
        Objects.requireNonNull(minimumFloor);

        Window placementReference = referenceWindow != null ? referenceWindow : window;
        WindowPlacement placement = windowLayoutPolicy.layoutWindow(
                initialSize,
                minimumFloor,
                displayEnvironment.getUsableScreenBounds(placementReference).usableBounds());
        if (applyMinimumSize) {
            window.setMinimumSize(placement.minimumSize());
        }
        window.setBounds(placement.bounds());
        return placement;
    }

    private static Dimension resolvePreferredSize(Window window) {
        Dimension preferredSize = window.getPreferredSize();
        if (preferredSize != null && preferredSize.width > 0 && preferredSize.height > 0) {
            return preferredSize;
        }

        Dimension currentSize = window.getSize();
        return new Dimension(Math.max(1, currentSize.width), Math.max(1, currentSize.height));
    }
}
