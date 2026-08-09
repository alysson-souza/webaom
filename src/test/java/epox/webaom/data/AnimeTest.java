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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.ui.AlternateViewTableModel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AnimeTest {
    @AfterEach
    void restoreStaticDisplaySettings() {
        Anime.titlePriority = 0;
        Anime.setSortColumn(AlternateViewTableModel.NAME + 1);
    }

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

    @Test
    void constructorsAcceptAniDBRangesSerializedEndYearsAndSingleYears() {
        Anime range = new Anime(fields("2020-2022"));
        Anime serialized =
                new Anime(new String[] {"2", "12", "10", "2021", "TV", "Serialized", "", "", "drama", "2023"});
        Anime singleYear = new Anime(fields("2024"));

        assertEquals(2020, range.year);
        assertEquals(2022, range.endYear);
        assertEquals(2021, serialized.year);
        assertEquals(2023, serialized.endYear);
        assertEquals(2024, singleYear.year);
        assertEquals(2024, singleYear.endYear);
        assertEquals("1|12|10|2020|TV|Romaji|null|null|action|2022", range.serialize());
    }

    @Test
    void titlePriorityFallsBackToRomajiWhenLocalizedTitlesAreMissing() {
        Anime anime = anime(3, 12, 7);
        anime.romajiTitle = "Romaji";
        anime.kanjiTitle = null;
        anime.englishTitle = null;

        Anime.titlePriority = 0;
        assertEquals("Romaji (12:7)", anime.toString());
        Anime.titlePriority = 1;
        assertEquals("Romaji (12:7)", anime.toString());
        anime.kanjiTitle = "Kanji";
        assertEquals("Kanji (12:7)", anime.toString());
        Anime.titlePriority = 2;
        assertEquals("Romaji (12:7)", anime.toString());
        anime.englishTitle = "English";
        assertEquals("English (12:7)", anime.toString());
    }

    @Test
    void getTotalUsesDeclaredCountThenLegacyUnknownCountHeuristics() {
        assertEquals(12, anime(4, 12, 7).getTotal());
        assertEquals(99, anime(5, 0, 9).getTotal());
        assertEquals(14, anime(6, 0, 14).getTotal());
    }

    @Test
    void initRetainsExistingProgressInsteadOfDiscardingIt() {
        Anime anime = anime(7, 3, 3);
        EpisodeProgress progress = anime.episodeProgress;
        progress.set(2, true);

        anime.init();

        assertSame(progress, anime.episodeProgress);
        assertTrue(anime.episodeProgress.get(2));
    }

    @Test
    void registerEpisodeSupportsNumericRangesAndUnregistersThem() {
        Anime anime = anime(8, 6, 6);
        Episode episode = episode("2-4,6", "Episode range");

        anime.registerEpisode(episode, true);

        assertFalse(anime.episodeProgress.get(1));
        assertTrue(anime.episodeProgress.get(2));
        assertTrue(anime.episodeProgress.get(3));
        assertTrue(anime.episodeProgress.get(4));
        assertFalse(anime.episodeProgress.get(5));
        assertTrue(anime.episodeProgress.get(6));

        anime.registerEpisode(episode, false);
        assertEquals("000000", anime.episodeProgress.toString());
    }

    @Test
    void registerEpisodeUsesOtherEpisodeTitlesForNumericRanges() {
        Anime anime = anime(9, 5, 5);
        Episode episode = episode("O1", "Episodes 2-4 recap");

        anime.registerEpisode(episode, true);

        assertEquals("01110", anime.episodeProgress.toString());
    }

    @Test
    void malformedOtherEpisodeTitlesAreIgnored() {
        Anime anime = anime(10, 4, 4);
        Episode noRange = episode("O1", "Compilation");
        Episode nullTitle = episode("O2", null);

        anime.registerEpisode(noRange, true);
        anime.registerEpisode(nullTitle, true);

        assertEquals("0000", anime.episodeProgress.toString());
    }

    @Test
    void outOfRangeEpisodeCurrentlyFillsTheNextAvailableSlot() {
        Anime anime = anime(11, 3, 3);

        anime.registerEpisode(episode("9", "Episode 9"), true);

        // TODO: Review whether an out-of-range episode should mark episode one through the fill fallback.
        assertEquals("100", anime.episodeProgress.toString());
    }

    @Test
    void completionPercentHandlesDeclaredAndEmptyEpisodeCounts() {
        Anime declared = anime(12, 4, 4);
        declared.episodeProgress.set(1, true);
        declared.episodeProgress.set(4, true);
        declared.updateCompletionPercent();

        Anime empty = anime(13, 0, 0);
        empty.updateCompletionPercent();

        assertEquals(50, declared.getCompletionPercent());
        assertEquals(0, empty.getCompletionPercent());
    }

    @Test
    void unknownEpisodeCountCurrentlyProducesANegativeCompletionPercentage() {
        Anime anime = anime(14, 0, 3);
        anime.episodeProgress.set(1, true);

        anime.updateCompletionPercent();

        // TODO: Review the negative divisor used when only latestEpisode is known.
        assertEquals(-33, anime.getCompletionPercent());
    }

    @Test
    void missingPatternCharacterizesEveryTransitionShape() {
        assertEquals(' ', missingPattern("00000"));
        assertEquals('f', missingPattern("00011"));
        assertEquals(' ', missingPattern("11100"));
        assertEquals('l', missingPattern("11011"));
        assertEquals('e', missingPattern("00100"));
        assertEquals('A', missingPattern("1010"));
    }

    @Test
    void compareBySupportsEveryAlternateViewColumnAndDirection() {
        Anime alpha = comparableAnime(15, "Alpha", "Movie", 2020, 25, 100_000, 1);
        Anime beta = comparableAnime(16, "Beta", "TV", 2022, 75, 400_000, 2);

        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.NAME + 1) < 0);
        assertTrue(alpha.compareBy(beta, -(AlternateViewTableModel.NAME + 1)) > 0);
        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.TYPE + 1) < 0);
        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.YEAR + 1) < 0);
        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.NUMB + 1) < 0);
        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.SIZE + 1) < 0);
        assertTrue(alpha.compareBy(beta, AlternateViewTableModel.PRCT + 1) > 0);
        assertEquals(
                beta.getMissingPattern() - alpha.getMissingPattern(),
                alpha.compareBy(beta, AlternateViewTableModel.LAST + 1));
        assertTrue(alpha.compareBy(new Episode(99), AlternateViewTableModel.NAME + 1) < 0);
    }

    @Test
    void compareToUsesThePreviousSortColumnAsATieBreaker() {
        Anime alpha = comparableAnime(17, "Alpha", "TV", 2020, 0, 0, 0);
        Anime beta = comparableAnime(18, "Beta", "TV", 2020, 0, 0, 0);
        Anime.setSortColumn(AlternateViewTableModel.NAME + 1);
        Anime.setSortColumn(AlternateViewTableModel.TYPE + 1);

        assertTrue(alpha.compareTo(beta) < 0);

        beta.type = "OVA";
        assertTrue(alpha.compareTo(beta) > 0);
    }

    @Test
    void clearRemovesChildrenAndResetsEpisodeProgress() {
        Anime anime = anime(19, 2, 2);
        AniDBFile file = new AniDBFile(100);
        anime.add(file);
        anime.episodeProgress.set(1, true);

        anime.clear();

        assertEquals(0, anime.size());
        assertEquals("00", anime.episodeProgress.toString());
    }

    private static String[] fields(String year) {
        return new String[] {"1", "12", "10", year, "TV", "Romaji", "", "", "action"};
    }

    private static Anime anime(int id, int episodeCount, int latestEpisode) {
        Anime anime = new Anime(id);
        anime.episodeCount = episodeCount;
        anime.latestEpisode = latestEpisode;
        anime.romajiTitle = "Anime " + id;
        anime.type = "TV";
        anime.init();
        return anime;
    }

    private static Episode episode(String number, String title) {
        Episode episode = new Episode(number.hashCode());
        episode.num = number;
        episode.eng = title;
        return episode;
    }

    private static char missingPattern(String bits) {
        Anime anime = anime(bits.hashCode(), bits.length(), bits.length());
        for (int index = 0; index < bits.length(); index++) {
            anime.episodeProgress.set(index + 1, bits.charAt(index) == '1');
        }
        return anime.getMissingPattern();
    }

    private static Anime comparableAnime(
            int id, String title, String type, int year, int percent, long childSize, int childCount) {
        Anime anime = anime(id, 4, 4);
        anime.romajiTitle = title;
        anime.type = type;
        anime.year = year;
        anime.completionPercent = percent;
        for (int index = 0; index < childCount; index++) {
            AniDBFile child = new AniDBFile(id * 100 + index);
            child.setTotalSize(childSize / Math.max(1, childCount));
            anime.add(child);
        }
        return anime;
    }
}
