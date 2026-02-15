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

package epox.webaom.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AnimeTest {
    @Test
    void registerEpisode_commaSeparatedNumbers_marksEachEpisode() {
        Anime anime = new Anime(1);
        anime.episodeCount = 5;
        anime.latestEpisode = 5;
        anime.romajiTitle = "Anime";
        anime.type = "TV";
        anime.init();

        Episode episode = new Episode(1);
        episode.num = "1,3";
        episode.eng = "Episode";

        anime.registerEpisode(episode, true);

        assertTrue(anime.episodeProgress.get(1));
        assertFalse(anime.episodeProgress.get(2));
        assertTrue(anime.episodeProgress.get(3));
    }
}
