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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import epox.webaom.Job;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.util.List;
import org.junit.jupiter.api.Test;

class CliIdentificationCharacterizationTest {
    @Test
    void missingJobOrFileHasNoIdentification() {
        assertNull(CliIdentification.from(null));
        assertNull(CliIdentification.from(mock(Job.class)));
    }

    @Test
    void richIdentificationCopiesEveryPublicMetadataCategory() {
        AniDBFile file = completeFile();
        Job job = mock(Job.class);
        job.mylistId = 555;
        job.anidbFile = file;

        CliIdentification identification = CliIdentification.from(job);

        assertEquals(444, identification.file().id());
        assertEquals(555, identification.file().mylistId());
        assertEquals(1_610_612_736L, identification.file().sizeBytes());
        assertEquals(3672, identification.file().lengthSeconds());
        assertEquals("mkv", identification.file().extension());
        assertEquals("v3", identification.file().version());
        assertEquals("cen", identification.file().censorship());
        assertEquals("invalid crc", identification.file().integrity());
        assertEquals(List.of(), identification.file().missingData().strict());
        assertEquals(List.of(), identification.file().missingData().additional());
        assertEquals("server-ed2k", identification.file().hashes().ed2k());
        assertEquals("server-crc32", identification.file().hashes().crc32());
        assertEquals("server-md5", identification.file().hashes().md5());
        assertEquals("server-sha1", identification.file().hashes().sha1());
        assertFalse(identification.file().hashes().isEmpty());
        assertTrue(identification.file().url().endsWith("/f444"));

        assertEquals(111, identification.anime().id());
        assertEquals("Example Anime Romaji", identification.anime().titles().romaji());
        assertEquals("Example Anime", identification.anime().titles().english());
        assertFalse(identification.anime().titles().isEmpty());
        assertEquals("TV Series", identification.anime().type());
        assertEquals(2024, identification.anime().yearStart());
        assertEquals(2025, identification.anime().yearEnd());
        assertEquals(12, identification.anime().episodeCount());
        assertEquals(10, identification.anime().latestEpisode());
        assertEquals(List.of("Action", "Sci-Fi"), identification.anime().categories());
        assertTrue(identification.anime().url().endsWith("/a111"));

        assertEquals(222, identification.episode().id());
        assertEquals("1", identification.episode().number());
        assertEquals("The Pilot", identification.episode().titles().english());
        assertTrue(identification.episode().url().endsWith("/e222"));

        assertEquals(333, identification.group().id());
        assertFalse(identification.group().raw());
        assertEquals("Example Fansubs", identification.group().name());
        assertEquals("Example", identification.group().shortName());
        assertTrue(identification.group().url().endsWith("/g333"));
    }

    @Test
    void absentRelatedEntitiesProduceNullSafeEmptyMetadata() {
        AniDBFile file = new AniDBFile(40);
        file.setAnimeId(10);
        file.setEpisodeId(20);
        file.setGroupId(0);
        Job job = mock(Job.class);
        job.anidbFile = file;

        CliIdentification identification = CliIdentification.from(job);

        assertTrue(identification.anime().titles().isEmpty());
        assertNull(identification.anime().type());
        assertEquals(List.of(), identification.anime().categories());
        assertTrue(identification.episode().titles().isEmpty());
        assertNull(identification.episode().number());
        assertTrue(identification.group().raw());
        assertNull(identification.group().name());
        assertNull(identification.group().shortName());
        assertNull(identification.group().url());
        assertNull(identification.file().missingData().strict());
        assertNull(identification.file().missingData().additional());
        assertTrue(identification.file().hashes().isEmpty());
    }

    @Test
    void blankTitlesAndCategoriesAreDiscardedAndMissingFlagsAreSplitIntoCodes() {
        AniDBFile file = completeFile();
        file.setCrcHash("");
        file.setMd5Hash(null);
        file.setLengthInSeconds(0);
        file.setDubLanguage("unknown");
        file.setSubLanguage("unknown");
        file.setAudioCodec("unknown");
        file.setVideoCodec("unknown");
        file.setResolution("0x0");
        file.setQuality("unknown");
        file.setRipSource("unknown");
        file.getAnime().categories = " ,  ";
        file.getAnime().englishTitle = "  ";
        file.getEpisode().eng = "";
        file.getEpisode().kan = null;
        file.getEpisode().rom = "";
        Job job = mock(Job.class);
        job.anidbFile = file;

        CliIdentification identification = CliIdentification.from(job);

        assertEquals(
                List.of("c", "h", "l", "d", "s", "a", "v", "x"),
                identification.file().missingData().strict());
        assertEquals(
                List.of("q", "o", "e", "k", "r"),
                identification.file().missingData().additional());
        assertEquals(List.of(), identification.anime().categories());
        assertNull(identification.anime().titles().english());
        assertTrue(identification.episode().titles().isEmpty());
    }

    @Test
    void runtimeFailureWhileDerivingMissingDataIsContained() {
        AniDBFile file = completeFile();
        file.setDubLanguage(null);
        Job job = mock(Job.class);
        job.anidbFile = file;

        CliIdentification identification = CliIdentification.from(job);

        assertNull(identification.file().missingData().strict());
        assertEquals(List.of(), identification.file().missingData().additional());
    }

    private static AniDBFile completeFile() {
        AniDBFile file = new AniDBFile(444);
        file.setAnimeId(111);
        file.setEpisodeId(222);
        file.setGroupId(333);
        file.setTotalSize(1_610_612_736L);
        file.setLengthInSeconds(3672);
        file.setExtension("mkv");
        file.setState(AniDBFile.F_ISV3 | AniDBFile.F_CEN | AniDBFile.F_CRCERR);
        file.setDubLanguage("japanese&english");
        file.setSubLanguage("english");
        file.setRipSource("BluRay");
        file.setQuality("high");
        file.setResolution("1920x1080");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        file.setEd2kHash("server-ed2k");
        file.setCrcHash("server-crc32");
        file.setMd5Hash("server-md5");
        file.setShaHash("server-sha1");

        Anime anime = new Anime(111);
        anime.romajiTitle = "Example Anime Romaji";
        anime.kanjiTitle = "アニメ";
        anime.englishTitle = "Example Anime";
        anime.type = "TV Series";
        anime.year = 2024;
        anime.endYear = 2025;
        anime.episodeCount = 12;
        anime.latestEpisode = 10;
        anime.categories = " Action, , Sci-Fi ";

        Episode episode = new Episode(222);
        episode.num = "1";
        episode.eng = "The Pilot";
        episode.rom = "Za Pairotto";
        episode.kan = "パイロット";

        Group group = new Group(333);
        group.name = "Example Fansubs";
        group.shortName = "Example";

        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        return file;
    }
}
