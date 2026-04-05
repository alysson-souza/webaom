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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Dimension;
import java.awt.Rectangle;
import org.junit.jupiter.api.Test;

class WindowLayoutPolicyTest {
    private final WindowLayoutPolicy windowLayoutPolicy = new WindowLayoutPolicy();

    @Test
    void layoutWindow_usesMinimumFloorWhenPreferredSizeIsSmaller() {
        WindowPlacement placement = windowLayoutPolicy.layoutWindow(
                new Dimension(640, 480), new Dimension(800, 648), new Rectangle(0, 0, 1200, 800));

        assertEquals(new Dimension(800, 648), placement.minimumSize());
        assertEquals(new Rectangle(200, 76, 800, 648), placement.bounds());
    }

    @Test
    void layoutWindow_usesPreferredSizeWhenWithinUsableBounds() {
        WindowPlacement placement = windowLayoutPolicy.layoutWindow(
                new Dimension(900, 700), new Dimension(800, 648), new Rectangle(0, 0, 1200, 800));

        assertEquals(new Dimension(800, 648), placement.minimumSize());
        assertEquals(new Rectangle(150, 50, 900, 700), placement.bounds());
    }

    @Test
    void layoutWindow_clampsVeryLargeWindowsToScreenUsageLimit() {
        WindowPlacement placement = windowLayoutPolicy.layoutWindow(
                new Dimension(2000, 1600), new Dimension(800, 648), new Rectangle(0, 0, 1200, 800));

        assertEquals(new Dimension(800, 648), placement.minimumSize());
        assertEquals(new Rectangle(30, 20, 1140, 760), placement.bounds());
    }

    @Test
    void layoutWindow_clampsMinimumFloorWhenScreenIsSmallerThanDefaultWindow() {
        WindowPlacement placement = windowLayoutPolicy.layoutWindow(
                new Dimension(500, 300), new Dimension(800, 648), new Rectangle(10, 20, 600, 400));

        assertEquals(new Dimension(570, 380), placement.minimumSize());
        assertEquals(new Rectangle(25, 30, 570, 380), placement.bounds());
    }

    @Test
    void expandCurrentBounds_growsWindowWithoutRecentering() {
        Rectangle expandedBounds = windowLayoutPolicy.expandCurrentBounds(
                new Rectangle(120, 80, 820, 640), new Dimension(960, 720), new Rectangle(0, 0, 1280, 800));

        assertEquals(new Rectangle(120, 80, 960, 720), expandedBounds);
    }

    @Test
    void expandCurrentBounds_clampsExpandedWindowIntoUsableBounds() {
        Rectangle expandedBounds = windowLayoutPolicy.expandCurrentBounds(
                new Rectangle(500, 200, 820, 640), new Dimension(960, 720), new Rectangle(0, 0, 1200, 800));

        assertEquals(new Rectangle(240, 80, 960, 720), expandedBounds);
    }
}
