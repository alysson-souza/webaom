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

import org.junit.jupiter.api.Test;

class AnimeGroupTest {
    @Test
    void identityDisplayAndCompletionComeFromTheAssociatedGroupAndAnime() {
        Anime anime = new Anime(1);
        anime.episodeCount = 4;
        Group group = new Group(7);
        group.name = "Release Group";
        group.shortName = "RG";
        AnimeGroup animeGroup = new AnimeGroup(anime, group);
        animeGroup.add(new Episode(1));
        animeGroup.add(new Episode(2));

        assertEquals(7, animeGroup.getId());
        assertEquals("Release Group (RG)", animeGroup.toString());
        assertEquals(50, animeGroup.getCompletionPercent());
    }

    @Test
    void completionHandlesUnknownEpisodeCountsUsingCurrentLegacyArithmetic() {
        Anime anime = new Anime(1);
        Group group = new Group(7);
        AnimeGroup animeGroup = new AnimeGroup(anime, group);
        assertEquals(0, animeGroup.getCompletionPercent());

        anime.latestEpisode = 4;
        animeGroup.add(new Episode(1));
        animeGroup.add(new Episode(2));
        // TODO: Review the negated latestEpisode fallback, which yields a negative completion percentage.
        assertEquals(-50, animeGroup.getCompletionPercent());
    }
}
