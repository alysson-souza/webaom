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

package epox.webaom.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import epox.webaom.Job;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliOutputTest {
    private static final String ESC = "\u001B";

    @TempDir
    Path temporaryDirectory;

    @Test
    void identifyTableShowsEveryAvailableMetadataSectionByDefault() {
        CliFileResult result = richResult();
        String output = table(result, CliOutput.ResultView.IDENTIFY, CliStyle.plain());

        assertTrue(output.contains("File"));
        assertTrue(output.contains("File ID:"));
        assertTrue(output.contains("444"));
        assertTrue(output.contains("MyList ID:"));
        assertTrue(output.contains("555"));
        assertTrue(output.contains("(1610612736 bytes)"));
        assertTrue(output.contains("1:01:12 (3672 seconds)"));
        assertTrue(output.contains("State:"));
        assertTrue(output.contains("70 (0x46)"));
        assertTrue(output.contains("Version:"));
        assertTrue(output.contains("v2"));
        assertTrue(output.contains("Censorship:"));
        assertTrue(output.contains("unc"));
        assertTrue(output.contains("Integrity:"));
        assertTrue(output.contains("invalid crc"));
        assertTrue(output.contains("Missing required:"));
        assertTrue(output.contains("Missing optional:"));
        assertTrue(output.contains("none"));

        assertTrue(output.contains("Anime"));
        assertTrue(output.contains("Example Anime"));
        assertTrue(output.contains("アニメ"));
        assertTrue(output.contains("Action, Sci-Fi"));
        assertTrue(output.contains("Episode"));
        assertTrue(output.contains("The Pilot"));
        assertTrue(output.contains("Group"));
        assertTrue(output.contains("Example Fansubs"));
        assertTrue(output.contains("Local Hashes"));
        assertTrue(output.contains("local-ed2k"));
        assertTrue(output.contains("AniDB Hashes"));
        assertTrue(output.contains("server-ed2k"));
    }

    @Test
    void standardTableViewRemainsCompactForOtherCommands() {
        CliFileResult result = richResult();
        String output = table(result, CliOutput.ResultView.STANDARD, CliStyle.plain());

        assertTrue(output.contains("Anime:"));
        assertTrue(output.contains("Episode:"));
        assertTrue(output.contains("Group:"));
        assertFalse(output.contains("Romaji title:"));
        assertFalse(output.contains("Audio language:"));
        assertFalse(output.contains("Local Hashes"));
        assertFalse(output.contains("AniDB Hashes"));
    }

    @Test
    void structuredOutputUsesNestedEntitiesAndKeepsLocalAndAniDbHashesSeparate() {
        CliFileResult result = richResult();
        result.job.anidbFile.getAnime().englishTitle = "Example\nAnime";
        String json = json(result, CliOutput.Format.JSON);

        assertTrue(json.contains("\"anidb\":{\"file\":{\"id\":444"));
        assertTrue(json.contains("\"mylistId\":555"));
        assertTrue(json.contains("\"sizeBytes\":1610612736"));
        assertTrue(json.contains("\"lengthSeconds\":3672"));
        assertTrue(json.contains("\"state\":70"));
        assertTrue(json.contains("\"missingData\":{\"strict\":[],\"additional\":[]}"));
        assertTrue(json.contains("\"hashes\":{\"ed2k\":\"server-ed2k\""));
        assertTrue(json.contains("\"anime\":{\"id\":111"));
        assertTrue(json.contains("\"titles\":{\"romaji\":\"Example Anime Romaji\""));
        assertTrue(json.contains("\"english\":\"Example\\nAnime\""));
        assertTrue(json.contains("\"categories\":[\"Action\",\"Sci-Fi\"]"));
        assertTrue(json.contains("\"episode\":{\"id\":222"));
        assertTrue(json.contains("\"group\":{\"id\":333,\"raw\":false"));
        assertTrue(json.contains("\"hashes\":{\"ed2k\":\"local-ed2k\",\"crc32\":\"local-crc32\""));
        assertFalse(json.contains("\"anime\":\""));
        assertFalse(json.contains(ESC));
    }

    @Test
    void structuredOutputOmitsUnavailableValuesAndRepresentsRawGroupExplicitly() {
        CliFileResult result = richResult();
        AniDBFile file = result.job.anidbFile;
        result.job.mylistId = 0;
        file.setGroupId(0);
        file.setGroup(Group.NONE);
        file.setExtension(null);
        file.setDubLanguage(null);
        file.getAnime().kanjiTitle = null;
        file.getAnime().englishTitle = null;
        file.getAnime().categories = null;
        file.getEpisode().kan = null;
        file.getEpisode().rom = null;

        String json = json(result, CliOutput.Format.JSON);

        assertTrue(json.contains("\"group\":{\"id\":0,\"raw\":true}"));
        assertFalse(json.contains("\"mylistId\""));
        assertFalse(json.contains("\"extension\""));
        assertFalse(json.contains("\"audioLanguages\""));
        assertFalse(json.contains("\"categories\""));
        assertTrue(json.contains("\"missingData\":{\"additional\":[\"k\",\"r\"]}"));
        assertFalse(json.contains("\"strict\""));
    }

    @Test
    void missingRelatedEntitiesDoNotBecomeMissingDataCodes() {
        CliFileResult result = richResult();
        result.job.anidbFile.setAnime(null);
        result.job.anidbFile.setEpisode(null);

        String json = json(result, CliOutput.Format.JSON);
        String table = table(result, CliOutput.ResultView.IDENTIFY, CliStyle.plain());

        assertFalse(json.contains("\"missingData\""));
        assertFalse(table.contains("Missing required:"));
        assertFalse(table.contains("Missing optional:"));
        assertFalse(table.contains("N, /, A"));
    }

    @Test
    void identifyTableLinksEveryEntityAndJsonlWritesOneObjectPerLine() {
        CliFileResult first = richResult();
        CliFileResult second = richResult();
        String table = table(
                first,
                CliOutput.ResultView.IDENTIFY,
                CliStyle.resolve(CliStyle.Mode.NEVER, CliStyle.Mode.ALWAYS, true, false));

        assertTrue(table.contains(ESC + "]8;;" + first.job.anidbFile.getFileUrl() + ESC + "\\444"));
        assertTrue(table.contains(ESC + "]8;;" + first.job.anidbFile.getAnimeUrl() + ESC + "\\111"));
        assertTrue(table.contains(ESC + "]8;;" + first.job.anidbFile.getEpisodeUrl() + ESC + "\\222"));
        assertTrue(table.contains(ESC + "]8;;" + first.job.anidbFile.getGroupUrl() + ESC + "\\333"));

        String jsonl = json(List.of(first, second), CliOutput.Format.JSONL);
        assertEquals(2, jsonl.lines().count());
        assertTrue(jsonl.lines().allMatch(line -> line.startsWith("{") && line.endsWith("}")));
        assertFalse(jsonl.contains(ESC));
    }

    private CliFileResult richResult() {
        CliFileResult result = new CliFileResult(temporaryDirectory.resolve("episode.mkv"));
        result.status = CliFileResult.Status.IDENTIFIED;
        result.hashes = Map.of(
                FileHasher.Algorithm.ED2K,
                "local-ed2k",
                FileHasher.Algorithm.CRC32,
                "local-crc32",
                FileHasher.Algorithm.TTH,
                "local-tth");

        AniDBFile file = new AniDBFile(444);
        file.setAnimeId(111);
        file.setEpisodeId(222);
        file.setGroupId(333);
        file.setTotalSize(1_610_612_736L);
        file.setLengthInSeconds(3672);
        file.setExtension("mkv");
        file.setState(AniDBFile.F_ISV2 | AniDBFile.F_UNC | AniDBFile.F_CRCERR);
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
        anime.categories = "Action, Sci-Fi";

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

        Job job = mock(Job.class);
        job.mylistId = 555;
        job.anidbFile = file;
        result.job = job;
        return result;
    }

    private String table(CliFileResult result, CliOutput.ResultView view, CliStyle style) {
        StringWriter out = new StringWriter();
        new CliOutput(
                        new PrintWriter(out, true),
                        new PrintWriter(new StringWriter(), true),
                        CliOutput.Format.TABLE,
                        false,
                        false,
                        style)
                .printResults(List.of(result), view);
        return out.toString();
    }

    private String json(CliFileResult result, CliOutput.Format format) {
        return json(List.of(result), format);
    }

    private String json(List<CliFileResult> results, CliOutput.Format format) {
        StringWriter out = new StringWriter();
        new CliOutput(
                        new PrintWriter(out, true),
                        new PrintWriter(new StringWriter(), true),
                        format,
                        false,
                        false,
                        CliStyle.resolve(CliStyle.Mode.ALWAYS, CliStyle.Mode.ALWAYS, true, false))
                .printResults(results, CliOutput.ResultView.IDENTIFY);
        return out.toString();
    }
}
