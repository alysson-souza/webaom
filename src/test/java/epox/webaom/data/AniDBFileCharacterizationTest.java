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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import epox.webaom.Job;
import java.io.File;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class AniDBFileCharacterizationTest {
    @Test
    void protocolFieldsPopulateAllMetadataAndNormalizeNullableHashes() {
        AniDBFile file = new AniDBFile(fields("17", "23", "31", "41", "51", "6", "987654321", "ed2k"));

        assertEquals(17, file.getFileId());
        assertEquals(23, file.getAnimeId());
        assertEquals(31, file.getEpisodeId());
        assertEquals(41, file.getGroupId());
        assertEquals(51, file.getMylistEntryId());
        assertEquals(6, file.getState());
        assertEquals(987654321L, file.getTotalSize());
        assertEquals("ed2k", file.getEd2kHash());
        assertNull(file.getMd5Hash());
        assertNull(file.getShaHash());
        assertNull(file.getCrcHash());
        assertEquals("jap/eng", file.getDubLanguage());
        assertEquals("eng", file.getSubLanguage());
        assertEquals("high", file.getQuality());
        assertEquals("BluRay", file.getRipSource());
        assertEquals("AAC", file.getAudioCodec());
        assertEquals("H264/AVC", file.getVideoCodec());
        assertEquals("1920x1080", file.getResolution());
        assertEquals("mkv", file.getExtension());
        assertEquals(1440, file.getLengthInSeconds());
    }

    @Test
    void blankProtocolGroupAndSizeBecomeZeroAndFactoryReturnsAFile() {
        String[] fields = fields("18", "24", "32", "", "52", "0", "", "hash");

        AniDBFile direct = new AniDBFile(fields);
        AniDBFile created = assertInstanceOf(AniDBFile.class, AniDBFile.createFromFields(fields));

        assertEquals(0, direct.getGroupId());
        assertEquals(0, direct.getTotalSize());
        assertEquals(direct, created);
    }

    @Test
    void settersExposeRelatedEntitiesAndEveryMutableMetadataValue() {
        AniDBFile file = new AniDBFile(1);
        Anime anime = anime(2, 2024, "Anime");
        Episode episode = episode(3, "1", "Episode");
        Group group = group(4, "Group", "GRP");
        AnimeGroup animeGroup = new AnimeGroup(anime, group);
        Job job = mock(Job.class);

        file.setFileId(10);
        file.setAnimeId(20);
        file.setEpisodeId(30);
        file.setGroupId(40);
        file.setMylistEntryId(50);
        file.setState(AniDBFile.F_CRCOK);
        file.setLengthInSeconds(60);
        file.setEd2kHash("ed2k");
        file.setMd5Hash("md5");
        file.setShaHash("sha1");
        file.setCrcHash("crc32");
        file.setDubLanguage("japanese");
        file.setSubLanguage("english");
        file.setQuality("high");
        file.setRipSource("TV");
        file.setResolution("1280x720");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        file.setDefaultName("default.mkv");
        file.setExtension("mkv");
        file.setEpisode(episode);
        file.setAnimeGroup(animeGroup);
        file.setGroup(group);
        file.setAnime(anime);
        file.setJob(job);

        assertEquals(10, file.getFileId());
        assertEquals(20, file.getAnimeId());
        assertEquals(30, file.getEpisodeId());
        assertEquals(40, file.getGroupId());
        assertEquals(50, file.getMylistEntryId());
        assertEquals(AniDBFile.F_CRCOK, file.getState());
        assertEquals(60, file.getLengthInSeconds());
        assertEquals("ed2k", file.getEd2kHash());
        assertEquals("md5", file.getMd5Hash());
        assertEquals("sha1", file.getShaHash());
        assertEquals("crc32", file.getCrcHash());
        assertEquals("japanese", file.getDubLanguage());
        assertEquals("english", file.getSubLanguage());
        assertEquals("high", file.getQuality());
        assertEquals("TV", file.getRipSource());
        assertEquals("1280x720", file.getResolution());
        assertEquals("H264", file.getVideoCodec());
        assertEquals("AAC", file.getAudioCodec());
        assertEquals("default.mkv", file.getDefaultName());
        assertEquals("mkv", file.getExtension());
        assertSame(episode, file.getEpisode());
        assertSame(animeGroup, file.getAnimeGroup());
        assertSame(group, file.getGroup());
        assertSame(anime, file.getAnime());
        assertSame(job, file.getJob());
    }

    @Test
    void serializationIncludesTheOptionalGroupPayload() {
        AniDBFile file = new AniDBFile(fields("17", "23", "31", "41", "51", "6", "987", "ed2k"));
        Group group = group(41, "Long Name", "Short");

        assertEquals(
                "17|23|31|41|51|6|987|ed2k|null|null|null|jap/eng|eng|high|BluRay|AAC|H264/AVC|1920x1080|mkv|1440|",
                file.serialize());

        file.setGroup(group);
        assertTrue(file.serialize().endsWith("|Long Name|Short"));
        file.clear();
        assertSame(group, file.getGroup());
    }

    @Test
    void displayNameUsesTheDefaultUntilAJobOwnsTheFile() {
        AniDBFile file = new AniDBFile(1);
        file.setDefaultName("server-name.mkv");

        assertEquals("server-name.mkv", file.toString());

        Job job = mock(Job.class);
        when(job.getFile()).thenReturn(new File("/library/local-name.mkv"));
        file.setJob(job);

        assertEquals("local-name.mkv", file.toString());
    }

    @Test
    void identityUsesOnlyTheConcreteTypeAndFileId() {
        AniDBFile file = new AniDBFile(7);
        AniDBFile same = new AniDBFile(7);
        AniDBFile other = new AniDBFile(8);

        assertEquals(file, file);
        assertEquals(file, same);
        assertEquals(file.hashCode(), same.hashCode());
        assertNotEquals(file, other);
        assertNotEquals(file, new Episode(7));
        assertNotEquals(file, null);
        assertEquals(7, file.getKey());
    }

    @Test
    void packCompactsLegacyLanguageAndCodecDescriptions() {
        AniDBFile file = new AniDBFile(1);
        file.setDubLanguage("dual (jap/eng)");
        file.setVideoCodec("H264/AVC High");
        file.setAudioCodec("Vorbis (Ogg Vorbis)");

        file.pack();

        assertEquals("jap&eng", file.getDubLanguage());
        assertEquals("H264", file.getVideoCodec());
        assertEquals("Vorbis", file.getAudioCodec());
    }

    @Test
    void malformedDualLanguageStillFailsDuringPacking() {
        AniDBFile file = new AniDBFile(1);
        file.setDubLanguage("dual (jap/eng");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");

        // TODO: Review whether malformed legacy language metadata should disable processing through this exception.
        assertThrows(StringIndexOutOfBoundsException.class, file::pack);
    }

    @Test
    void entityUrlsUseAniDbHttpsPaths() {
        AniDBFile file = linkedFile();

        assertEquals("https://anidb.net/a17", file.getAnimeUrl());
        assertEquals("https://anidb.net/e23", file.getEpisodeUrl());
        assertEquals("https://anidb.net/f31", file.getFileUrl());
        assertEquals("https://anidb.net/g41", file.getGroupUrl());

        URI export = URI.create(file.getExportUrl());
        assertEquals("/anime/17/export", export.getPath());
        assertEquals(Map.of("show", "ed2kexport", "h", "1", "nonav", "1"), queryParameters(export));
    }

    @Test
    void mylistUrlsCharacterizeOverviewAndEditForms() {
        AniDBFile file = linkedFile();

        URI overview = URI.create(file.getMylistUrl());
        assertEquals("/perl-bin/animedb.pl", overview.getPath());
        assertEquals("a17", overview.getFragment());
        assertEquals(Map.of("show", "mylist", "expand", "17", "char", "R", "nonav", "1"), queryParameters(overview));
        assertEquals(file.getMylistUrl(), file.getMylistEditUrl(1));

        URI edit = URI.create(file.getMylistEditUrl(99));
        assertEquals("/perl-bin/animedb.pl", edit.getPath());
        assertEquals(
                Map.of("show", "mylist", "do", "add", "aid", "17", "eid", "23", "fid", "31", "lid", "99", "nonav", "1"),
                queryParameters(edit));
    }

    @Test
    void missingAnimeMetadataFallsBackToGenericNavigationUrls() {
        AniDBFile file = new AniDBFile(31);
        file.setAnimeId(17);

        // TODO: Review the broad exception fallback, which hides missing or malformed anime metadata.
        assertEquals("https://anidb.net/mylist", file.getMylistUrl());
        assertEquals("https://anidb.net/anime", file.getYearUrl());
    }

    @Test
    void yearUrlAndRangeMatchingUseTheRelatedAnimeYear() {
        AniDBFile file = linkedFile();

        URI year = URI.create(file.getYearUrl());
        assertEquals("/anime/", year.getPath());
        assertEquals(Map.of("season.year", "2024"), queryParameters(year));
        assertTrue(file.inYear("2024"));
        assertTrue(file.inYear(" 2022 - 2025 "));
        assertTrue(file.inYear("2025-2022"));
        assertFalse(file.inYear("2020-2023"));
        assertFalse(file.inYear("2025"));
    }

    @Test
    void stateDescriptionsCoverAllVersionCensorshipAndIntegrityValues() {
        AniDBFile file = new AniDBFile(1);

        file.setState(0);
        assertEquals("", file.getVersion());
        assertEquals("", file.getCensored());
        assertEquals("", file.getInvalid());

        file.setState(AniDBFile.F_ISV2 | AniDBFile.F_UNC | AniDBFile.F_CRCOK);
        assertEquals("v2", file.getVersion());
        assertEquals("unc", file.getCensored());
        assertEquals("", file.getInvalid());

        file.setState(AniDBFile.F_ISV3 | AniDBFile.F_CEN | AniDBFile.F_CRCERR);
        assertEquals("v3", file.getVersion());
        assertEquals("cen", file.getCensored());
        assertEquals("invalid crc", file.getInvalid());

        file.setState(AniDBFile.F_ISV4);
        assertEquals("v4", file.getVersion());
        file.setState(AniDBFile.F_ISV5);
        assertEquals("v5", file.getVersion());
        file.setState(AniDBFile.F_ISV2 | AniDBFile.F_ISV3);
        assertEquals("", file.getVersion());
    }

    @Test
    void missingDataReportsUnavailableCompleteAndMissingMetadata() {
        AniDBFile file = new AniDBFile(1);
        assertEquals("N/A", file.getMissingDataStrict());
        assertEquals("N/A", file.getMissingDataAdditional());

        file.setAnime(anime(2, 2024, "Anime"));
        file.setEpisode(episode(3, "1", "Episode"));
        file.setCrcHash("");
        file.setMd5Hash(null);
        file.setShaHash("");
        file.setLengthInSeconds(0);
        file.setDubLanguage("unknown");
        file.setSubLanguage("unknown");
        file.setAudioCodec("unknown");
        file.setVideoCodec("unknown");
        file.setResolution("0x0");
        file.setQuality("unknown");
        file.setRipSource("unknown");
        file.getEpisode().eng = "";

        assertEquals("chldsavx", file.getMissingDataStrict());
        assertEquals("qoekr", file.getMissingDataAdditional());

        file.setCrcHash("crc");
        file.setMd5Hash("md5");
        file.setShaHash("sha");
        file.setLengthInSeconds(1);
        file.setDubLanguage("japanese");
        file.setSubLanguage("english");
        file.setAudioCodec("AAC");
        file.setVideoCodec("H264");
        file.setResolution("1920x1080");
        file.setQuality("high");
        file.setRipSource("BluRay");
        Episode episode = file.getEpisode();
        episode.eng = "English";
        episode.rom = "Romaji";
        episode.kan = "Kanji";

        assertEquals("", file.getMissingDataStrict());
        assertEquals("", file.getMissingDataAdditional());
    }

    private static AniDBFile linkedFile() {
        AniDBFile file = new AniDBFile(31);
        file.setAnimeId(17);
        file.setEpisodeId(23);
        file.setGroupId(41);
        file.setAnime(anime(17, 2024, "Romaji"));
        return file;
    }

    private static Anime anime(int id, int year, String title) {
        Anime anime = new Anime(id);
        anime.year = year;
        anime.romajiTitle = title;
        anime.episodeCount = 12;
        anime.latestEpisode = 3;
        return anime;
    }

    private static Episode episode(int id, String number, String englishTitle) {
        Episode episode = new Episode(id);
        episode.num = number;
        episode.eng = englishTitle;
        return episode;
    }

    private static Group group(int id, String name, String shortName) {
        Group group = new Group(id);
        group.name = name;
        group.shortName = shortName;
        return group;
    }

    private static String[] fields(
            String fileId,
            String animeId,
            String episodeId,
            String groupId,
            String mylistEntryId,
            String state,
            String totalSize,
            String ed2k) {
        return new String[] {
            fileId,
            animeId,
            episodeId,
            groupId,
            mylistEntryId,
            state,
            totalSize,
            ed2k,
            "",
            "",
            "",
            "jap/eng",
            "eng",
            "high",
            "BluRay",
            "AAC",
            "H264/AVC",
            "1920x1080",
            "mkv",
            "1440"
        };
    }

    private static Map<String, String> queryParameters(URI uri) {
        return Arrays.stream(uri.getRawQuery().split("&"))
                .map(parameter -> parameter.split("=", 2))
                .collect(Collectors.toMap(
                        parts -> URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                        parts -> URLDecoder.decode(parts[1], StandardCharsets.UTF_8)));
    }
}
