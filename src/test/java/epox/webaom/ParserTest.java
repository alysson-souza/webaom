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

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.db.DatabaseManager;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ParserTest {
    @TempDir
    Path tempDir;

    private int originalAssumedEpisodeCount;
    private int originalAssumedSpecialCount;
    private Cache originalCache;
    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private DatabaseManager originalDatabaseManager;
    private List<AniDBEntity> originalRootEntries;

    @BeforeEach
    void setUp() {
        originalAssumedEpisodeCount = AppContext.assumedEpisodeCount;
        originalAssumedSpecialCount = AppContext.assumedSpecialCount;
        originalCache = AppContext.cache;
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalDatabaseManager = AppContext.databaseManager;
        originalRootEntries = rootEntries();

        AppContext.assumedEpisodeCount = 99;
        AppContext.assumedSpecialCount = 99;
        AppContext.cache = new Cache();
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.animeTreeRoot.clear();
    }

    @AfterEach
    void tearDown() {
        AppContext.assumedEpisodeCount = originalAssumedEpisodeCount;
        AppContext.assumedSpecialCount = originalAssumedSpecialCount;
        AppContext.cache = originalCache;
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.animeTreeRoot.clear();
        originalRootEntries.forEach(AppContext.animeTreeRoot::add);
    }

    @Test
    void parserPad_numericEpisode_padsBasedOnTotal() {
        assertEquals("01", Parser.pad("1", 12));
    }

    @Test
    void parserPad_specialEpisode_usesSpecialAssumptionAndKeepsPrefix() {
        String result = Parser.pad("S1", 0);

        assertEquals("S01", result);
        assertTrue(result.startsWith("S"));
    }

    @Test
    void parserPad_rangeInput_padsBothSides() {
        assertEquals("01-02", Parser.pad("1-2", 12));
    }

    @Test
    void parserPad_commaRangeSpecialAndOverflowUseCurrentRecursiveFormatting() {
        assertEquals("01,02-03", Parser.pad("1,2-3", 12));
        assertEquals("S01-S02", Parser.pad("S1-S2", 0));
        assertEquals("100", Parser.pad("100", 12));
        assertEquals("00", Parser.pad("0", 12));
    }

    @Test
    void parseGroup_mapsTheAniDbFieldPositions() {
        assertNull(Parser.parseGroup(null));

        Group group = Parser.parseGroup(new String[] {"7", "", "", "", "", "Group Name", "GN"});

        assertEquals(7, group.getId());
        assertEquals("Group Name", group.name);
        assertEquals("GN", group.shortName);
    }

    @Test
    void parseEpisode_mapsTheAniDbFieldPositions() {
        assertNull(Parser.parseEpisode(null));

        Episode episode = Parser.parseEpisode(new String[] {"101", "", "", "", "", "3", "English", "Romaji", "Kanji"});

        assertEquals(101, episode.getId());
        assertEquals("3", episode.num);
        assertEquals("English", episode.eng);
        assertEquals("Romaji", episode.rom);
        assertEquals("Kanji", episode.kan);
    }

    @Test
    void parseAnime_mapsSingleAndRangeYears() {
        assertNull(Parser.parseAnime(null));
        String[] fields = {
            "42",
            "26",
            "24",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "2001-2002",
            "TV",
            "Romaji",
            "Kanji",
            "English",
            "",
            "",
            "",
            "action,space"
        };

        Anime anime = Parser.parseAnime(fields);

        assertEquals(42, anime.getId());
        assertEquals(26, anime.episodeCount);
        assertEquals(24, anime.latestEpisode);
        assertEquals(2001, anime.year);
        assertEquals(2002, anime.endYear);
        assertEquals("TV", anime.type);
        assertEquals("Romaji", anime.romajiTitle);
        assertEquals("Kanji", anime.kanjiTitle);
        assertEquals("English", anime.englishTitle);
        assertEquals("action,space", anime.categories);

        fields[10] = "2003";
        assertEquals(2003, Parser.parseAnime(fields).endYear);
    }

    @Test
    void explicitExportAndImportRoundTripTheCurrentTreeWithoutAChooser() throws Exception {
        Anime anime = anime();
        Episode episode = episode();
        AniDBFile file = aniDbFile(anime, episode);
        Path mediaPath = Files.writeString(tempDir.resolve("episode.mkv"), "payload");
        Job job = new Job(mediaPath.toFile(), Job.FINISHED);
        job.fileSize = file.getTotalSize();
        job.ed2kHash = file.getEd2kHash();
        job.md5Hash = file.getMd5Hash();
        job.sha1Hash = file.getShaHash();
        job.crc32Hash = file.getCrcHash();
        job.anidbFile = file;
        file.setJob(job);
        episode.add(file);
        anime.add(episode);
        AppContext.animeTreeRoot.add(anime);
        File exportFile = tempDir.resolve("database-export.txt").toFile();

        Parser.exportDatabaseTo(exportFile);

        String exportText = Files.readString(exportFile.toPath());
        assertTrue(exportText.startsWith("a0\r\n"));
        assertTrue(exportText.contains("\r\ne101|1|Asteroid Blues"));
        assertTrue(exportText.contains("\r\nf1001|42|101|7|"));
        assertTrue(exportText.contains("\r\nj" + Job.FINISHED + "|"));

        AppContext.animeTreeRoot.clear();
        AppContext.cache.clear();
        AppContext.jobs.clear();
        Parser.importDatabaseFrom(exportFile);

        AppContext.animeTreeRoot.buildSortedChildArray();
        assertEquals(1, AppContext.animeTreeRoot.size());
        Anime importedAnime = (Anime) AppContext.animeTreeRoot.get(0);
        assertEquals("Cowboy Bebop", importedAnime.romajiTitle);
        assertEquals(1, AppContext.jobs.array().length);
        Job importedJob = AppContext.jobs.array()[0];
        assertEquals("abcdef", importedJob.ed2kHash);
        assertEquals(1001, importedJob.anidbFile.getFileId());
        assertEquals("Cowboy Bebop - 1 - Asteroid Blues - [EX]", importedJob.anidbFile.getDefaultName());
        verify(AppContext.databaseManager, atLeastOnce()).update(anyInt(), any(), anyInt());
    }

    @Test
    void explicitImportSupportsLegacyJobsAndRejectsUnknownFormats() throws Exception {
        Anime anime = anime();
        Episode episode = episode();
        AniDBFile file = aniDbFile(anime, episode);
        episode.add(file);
        anime.add(episode);
        AppContext.animeTreeRoot.add(anime);
        File modern = tempDir.resolve("modern.txt").toFile();
        Parser.exportDatabaseTo(modern);
        List<String> lines = Files.readAllLines(modern.toPath());
        lines.set(0, "s0");
        lines.removeIf(line -> line.startsWith("j"));
        lines.add("jlegacy/path/episode.mkv");
        File legacy = tempDir.resolve("legacy.txt").toFile();
        Files.write(legacy.toPath(), lines);

        AppContext.animeTreeRoot.clear();
        Parser.importDatabaseFrom(legacy);

        assertEquals(1, AppContext.jobs.array().length);
        Job imported = AppContext.jobs.array()[0];
        assertEquals("legacy/path/episode.mkv", imported.originalName);
        assertEquals("abcdef", imported.ed2kHash);
        assertEquals(file.getTotalSize(), imported.fileSize);

        File unsupported =
                Files.writeString(tempDir.resolve("unsupported.txt"), "z9\n").toFile();
        Exception error = assertThrows(Exception.class, () -> Parser.importDatabaseFrom(unsupported));
        assertEquals("format not supported", error.getMessage());

        File empty = Files.createFile(tempDir.resolve("empty-export.txt")).toFile();
        // TODO: Review whether an empty import should report an unsupported format instead of leaking an NPE.
        assertThrows(NullPointerException.class, () -> Parser.importDatabaseFrom(empty));
    }

    private List<AniDBEntity> rootEntries() {
        List<AniDBEntity> entries = new ArrayList<>();
        AppContext.animeTreeRoot.buildSortedChildArray();
        for (int index = 0; index < AppContext.animeTreeRoot.size(); index++) {
            entries.add(AppContext.animeTreeRoot.get(index));
        }
        return entries;
    }

    private Anime anime() {
        Anime anime = new Anime(42);
        anime.episodeCount = 26;
        anime.latestEpisode = 26;
        anime.year = 2001;
        anime.endYear = 2001;
        anime.type = "TV";
        anime.romajiTitle = "Cowboy Bebop";
        anime.kanjiTitle = "Cowboy Bebop";
        anime.englishTitle = "Cowboy Bebop English";
        anime.categories = "action,space";
        anime.init();
        return anime;
    }

    private Episode episode() {
        Episode episode = new Episode(101);
        episode.num = "1";
        episode.eng = "Asteroid Blues";
        episode.rom = "Asteroid Blues Romaji";
        episode.kan = "Asteroid Blues Kanji";
        return episode;
    }

    private AniDBFile aniDbFile(Anime anime, Episode episode) {
        Group group = new Group(7);
        group.name = "Example Fansubs";
        group.shortName = "EX";
        AniDBFile file = new AniDBFile(1001);
        file.setAnimeId(42);
        file.setEpisodeId(101);
        file.setGroupId(7);
        file.setMylistEntryId(77);
        file.setState(AniDBFile.F_CRCOK);
        file.setTotalSize(7);
        file.setLengthInSeconds(1440);
        file.setEd2kHash("abcdef");
        file.setMd5Hash("1234");
        file.setShaHash("5678");
        file.setCrcHash("90ab");
        file.setDubLanguage("jap");
        file.setSubLanguage("eng");
        file.setQuality("HD");
        file.setRipSource("BluRay");
        file.setAudioCodec("AAC");
        file.setVideoCodec("H264");
        file.setResolution("1920x1080");
        file.setExtension("mkv");
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        return file;
    }
}
