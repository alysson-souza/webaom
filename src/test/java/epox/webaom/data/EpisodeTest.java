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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class EpisodeTest {
    @AfterEach
    void restoreTitlePriority() {
        Episode.titlePriority = 0;
    }

    @Test
    void fieldConstructorNormalizesOptionalTitlesAndSerializesThem() {
        Episode episode = new Episode(new String[] {"42", "S1", "English", "null", ""});

        assertEquals(42, episode.getId());
        assertEquals("S1", episode.num);
        assertEquals("English", episode.eng);
        assertNull(episode.rom);
        assertNull(episode.kan);
        assertEquals("42|S1|English|null|null", episode.serialize());
    }

    @Test
    void toStringUsesConfiguredTitleAndFallsBackToEnglish() {
        Episode episode = new Episode(1);
        episode.num = "3";
        episode.eng = "English";
        episode.rom = "Romaji";
        episode.kan = "Kanji";

        assertEquals("3 - English", episode.toString());
        Episode.titlePriority = 1;
        assertEquals("3 - Romaji", episode.toString());
        episode.rom = null;
        assertEquals("3 - English", episode.toString());
        Episode.titlePriority = 2;
        assertEquals("3 - Kanji", episode.toString());
        episode.kan = null;
        assertEquals("3 - English", episode.toString());
    }

    @Test
    void comparisonUsesNumericOrderOnlyWhenBothNumbersAreIntegers() {
        Episode two = episode(2, "2");
        Episode ten = episode(10, "10");
        Episode special = episode(11, "S1");

        assertTrue(two.compareTo(ten) < 0);
        assertTrue(ten.compareTo(two) > 0);
        assertTrue(special.compareTo(ten) > 0);
        assertEquals(0, special.compareTo(episode(12, "S1")));
    }

    private static Episode episode(int id, String number) {
        Episode episode = new Episode(id);
        episode.num = number;
        episode.eng = "Title";
        return episode;
    }
}
