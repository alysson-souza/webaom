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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AniDBEntityTest {
    @Test
    void childOperationsTrackKeysSizesAndSortedAccess() {
        AniDBEntity parent = new AniDBEntity();
        Episode two = episode(2, "2", 200);
        Episode one = episode(1, "1", 100);

        assertNull(parent.get(0));
        parent.add(two);
        parent.add(one);
        parent.add(episode(1, "duplicate", 999));

        assertEquals(2, parent.size());
        assertEquals(300, parent.getTotalSize());
        assertTrue(parent.has(one));
        assertSame(one, parent.get(Integer.valueOf(1)));

        parent.buildSortedChildArray();
        assertSame(one, parent.get(0));
        assertSame(two, parent.get(1));

        parent.remove(one);
        assertEquals(1, parent.size());
        assertEquals(200, parent.getTotalSize());
        assertFalse(parent.has(one));

        parent.clear();
        assertEquals(0, parent.size());
        assertEquals(0, parent.getTotalSize());
        assertNull(parent.serialize());
    }

    @Test
    void identityRequiresTheSameConcreteEntityTypeAndId() {
        Episode first = new Episode(5);
        Episode same = new Episode(5);
        Episode other = new Episode(6);
        Group differentType = new Group(5);

        assertEquals(first, first);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, other);
        assertNotEquals(first, differentType);
        assertNotEquals(first, null);

        first.setId(9);
        assertEquals(9, first.getId());
    }

    @Test
    void pathAndGroupExposeTheirLegacyKeysAndSerialization() {
        Path path = new Path("/library/anime");
        assertEquals("/library/anime", path.getKey());
        assertEquals("/library/anime", path.toString());

        Group group = new Group(3);
        group.name = "Name";
        group.shortName = "Short";
        assertEquals("Name|Short", group.serialize());
        assertEquals("Name|Short", group.toString());
        assertEquals("NONE|", Group.NONE.toString());
    }

    private static Episode episode(int id, String number, long size) {
        Episode episode = new Episode(id);
        episode.num = number;
        episode.eng = "Title";
        episode.setTotalSize(size);
        return episode;
    }
}
