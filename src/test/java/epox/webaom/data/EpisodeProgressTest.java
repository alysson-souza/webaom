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
 */

package epox.webaom.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EpisodeProgressTest {
    @Test
    void getAndSetUseOneBasedEpisodeNumbersAndRejectBounds() {
        EpisodeProgress progress = new EpisodeProgress(3);

        assertFalse(progress.set(0, true));
        assertFalse(progress.set(4, true));
        assertFalse(progress.get(0));
        assertFalse(progress.get(4));
        assertTrue(progress.set(1, true));
        assertTrue(progress.set(3, true));
        assertTrue(progress.get(1));
        assertTrue(progress.get(3));
        assertEquals(2, progress.countSetBits());
        assertEquals("101", progress.toString());
    }

    @Test
    void fillAddsFromTheStartAndRemovesFromTheEnd() {
        EpisodeProgress progress = new EpisodeProgress(2);

        assertTrue(progress.fill(true));
        assertEquals("10", progress.toString());
        assertTrue(progress.fill(true));
        assertEquals("11", progress.toString());
        assertFalse(progress.fill(true));

        assertTrue(progress.fill(false));
        assertEquals("10", progress.toString());
        assertTrue(progress.fill(false));
        assertEquals("00", progress.toString());
        assertFalse(progress.fill(false));
    }

    @Test
    void transitionAndHoleQueriesDescribeTheCurrentPattern() {
        EpisodeProgress progress = new EpisodeProgress(5);
        assertFalse(progress.first());
        assertFalse(progress.last());
        assertFalse(progress.hasHole());
        assertEquals(0, progress.switchCount());

        progress.set(1, true);
        progress.set(3, true);
        progress.set(5, true);
        assertTrue(progress.first());
        assertTrue(progress.last());
        assertTrue(progress.hasHole());
        assertEquals(4, progress.switchCount());

        progress.reset();
        assertEquals("00000", progress.toString());
        assertEquals(0, progress.countSetBits());

        EpisodeProgress empty = new EpisodeProgress(0);
        assertFalse(empty.first());
        assertFalse(empty.last());
        assertFalse(empty.fill(true));
        assertFalse(empty.fill(false));
        assertFalse(empty.hasHole());
        assertEquals(0, empty.switchCount());
        assertEquals("", empty.toString());
    }
}
