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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.db.DatabaseManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CacheTest {
    @TempDir
    Path tempDir;

    private DatabaseManager originalDatabaseManager;
    private JobCounter originalJobCounter;
    private JobList originalJobs;
    private String originalPathRegex;
    private int originalTreeSortMode;
    private boolean originalHideExisting;
    private boolean originalHideNew;

    @BeforeEach
    void setUp() {
        originalDatabaseManager = AppContext.databaseManager;
        originalJobCounter = AppContext.jobCounter;
        originalJobs = AppContext.jobs;
        originalPathRegex = AppContext.pathRegex;
        originalTreeSortMode = Cache.getTreeSortMode();
        originalHideExisting = Cache.isHideExisting();
        originalHideNew = Cache.isHideNew();

        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.jobCounter = new JobCounter();
        AppContext.jobs = mock(JobList.class);
        AppContext.pathRegex = null;
        Cache.setTreeSortMode(Cache.MODE_ANIME_EPISODE_FILE);
        Cache.setHideExisting(false);
        Cache.setHideNew(false);
        AppContext.animeTreeRoot.clear();
    }

    @AfterEach
    void tearDown() {
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.jobCounter = originalJobCounter;
        AppContext.jobs = originalJobs;
        AppContext.pathRegex = originalPathRegex;
        Cache.setTreeSortMode(originalTreeSortMode);
        Cache.setHideExisting(originalHideExisting);
        Cache.setHideNew(originalHideNew);
        AppContext.animeTreeRoot.clear();
    }

    @Test
    void cacheTreeAdd_modeEpisodeFile_addsAnimeAndEpisodeNodes() throws IOException {
        Cache cache = new Cache();
        Job job = buildCompleteJob(1, "1", false);

        cache.treeAdd(job);

        Anime anime = job.anidbFile.getAnime();
        Episode episode = job.anidbFile.getEpisode();
        AniDBFile file = job.anidbFile;
        assertTrue(AppContext.animeTreeRoot.has(anime));
        assertTrue(anime.has(episode));
        assertTrue(episode.has(file));
        assertEquals(100, anime.getCompletionPercent());
    }

    @Test
    void cacheTreeRemove_lastFile_removesAnimeFromRoot() throws IOException {
        Cache cache = new Cache();
        Job job = buildCompleteJob(2, "1", false);

        cache.treeAdd(job);
        cache.treeRemove(job);

        assertFalse(AppContext.animeTreeRoot.has(job.anidbFile.getAnime()));
    }

    @Test
    void cacheTreeAdd_hideExisting_skipsHealthyFiles() throws IOException {
        Cache cache = new Cache();
        Cache.setHideExisting(true);
        Job job = buildCompleteJob(3, "1", false);

        cache.treeAdd(job);

        assertEquals(0, AppContext.animeTreeRoot.size());
    }

    @Test
    void cacheGatherInfo_groupZero_usesGroupNoneAndLoadsFromDatabase() throws IOException {
        Cache cache = new Cache();
        DatabaseManager databaseManager = AppContext.databaseManager;
        Anime anime = buildAnime(10);
        Episode episode = buildEpisode(20, "1");
        when(databaseManager.getGeneric(10, DatabaseManager.INDEX_ANIME)).thenReturn(anime);
        when(databaseManager.getGeneric(20, DatabaseManager.INDEX_EPISODE)).thenReturn(episode);

        Job job = new Job(Files.createFile(tempDir.resolve("lookup.mkv")).toFile(), Job.HASHWAIT);
        AniDBFile file = new AniDBFile(30);
        file.setAnimeId(10);
        file.setEpisodeId(20);
        file.setGroupId(0);
        job.anidbFile = file;

        cache.gatherInfo(job, false);

        assertSame(anime, file.getAnime());
        assertSame(episode, file.getEpisode());
        assertSame(Group.NONE, file.getGroup());
    }

    @Test
    void cacheAdd_updateModeOne_updatesDatabaseOnlyOnFirstInsert() {
        Cache cache = new Cache();
        DatabaseManager databaseManager = AppContext.databaseManager;
        Anime anime = buildAnime(55);

        cache.add(anime, 1, DatabaseManager.INDEX_ANIME);
        cache.add(anime, 1, DatabaseManager.INDEX_ANIME);

        verify(databaseManager, times(1)).update(55, anime, DatabaseManager.INDEX_ANIME);
    }

    @Test
    void parseFileWithPersistenceReportsAnyFailedMetadataWrite() throws IOException {
        Cache cache = new Cache();
        DatabaseManager databaseManager = AppContext.databaseManager;
        when(databaseManager.update(anyInt(), any(), anyInt())).thenReturn(true);
        when(databaseManager.update(eq(10), any(Anime.class), eq(DatabaseManager.INDEX_ANIME)))
                .thenReturn(false);
        Job job = new Job(Files.createFile(tempDir.resolve("identified.mkv")).toFile(), Job.HASHWAIT);

        Cache.FileParseResult result = cache.parseFileWithPersistence(fileResponseFields(), job);

        assertFalse(result.persisted());
        assertSame(job.anidbFile, result.file());
        verify(databaseManager, times(4)).update(anyInt(), any(), anyInt());
    }

    @Test
    void staticOptionsReturnDefensiveLabelsAndRetainSelectedFlags() {
        String[] labels = Cache.getSortModeLabels();
        labels[0] = "changed";

        assertNotSame(labels, Cache.getSortModeLabels());
        assertEquals("Anime, File", Cache.getSortModeLabels()[0]);

        Cache.setTreeSortMode(Cache.MODE_ANIME_FOLDER_FILE);
        Cache.setHideExisting(true);
        Cache.setHideNew(true);

        assertEquals(Cache.MODE_ANIME_FOLDER_FILE, Cache.getTreeSortMode());
        assertTrue(Cache.isHideExisting());
        assertTrue(Cache.isHideNew());
    }

    @Test
    void statsAndClearReflectAllThreeCacheMaps() {
        Cache cache = new Cache();
        cache.add(buildAnime(1), 0, DatabaseManager.INDEX_ANIME);
        cache.add(buildAnime(2), 0, DatabaseManager.INDEX_ANIME);
        cache.add(buildEpisode(3, "1"), 0, DatabaseManager.INDEX_EPISODE);
        cache.add(new Group(4), 0, DatabaseManager.INDEX_GROUP);

        assertEquals("2,1,1", cache.stats());
        // TODO: Review the legacy summary, which reports the anime count in the Group field.
        assertEquals("Anime: 2, Episode: 1, Group: 2", cache.toString());

        cache.clear();

        assertEquals("0,0,0", cache.stats());
    }

    @Test
    void updateModeTwoPersistsEveryCallAndReturnsTheLatestResult() {
        Cache cache = new Cache();
        Anime anime = buildAnime(5);
        when(AppContext.databaseManager.update(5, anime, DatabaseManager.INDEX_ANIME))
                .thenReturn(true, false);

        assertTrue(cache.add(anime, 2, DatabaseManager.INDEX_ANIME));
        assertFalse(cache.add(anime, 2, DatabaseManager.INDEX_ANIME));
        verify(AppContext.databaseManager, times(2)).update(5, anime, DatabaseManager.INDEX_ANIME);
    }

    @Test
    void getLoadsOnceFromPersistenceAndCachesBothHitsAndMisses() {
        Cache cache = new Cache();
        Anime anime = buildAnime(6);
        when(AppContext.databaseManager.getGeneric(6, DatabaseManager.INDEX_ANIME))
                .thenReturn(anime);

        assertSame(anime, cache.get(6, DatabaseManager.INDEX_ANIME));
        assertSame(anime, cache.get(6, DatabaseManager.INDEX_ANIME));
        assertNull(cache.get(7, DatabaseManager.INDEX_ANIME));

        verify(AppContext.databaseManager, times(1)).getGeneric(6, DatabaseManager.INDEX_ANIME);
        verify(AppContext.databaseManager, times(1)).getGeneric(7, DatabaseManager.INDEX_ANIME);
    }

    @Test
    void gatherInfoLoadsAllRelationsAndCanAddTheJobToTheTree() throws IOException {
        Cache cache = new Cache();
        Anime anime = buildAnime(10);
        Episode episode = buildEpisode(20, "1");
        Group group = new Group(30);
        when(AppContext.databaseManager.getGeneric(10, DatabaseManager.INDEX_ANIME))
                .thenReturn(anime);
        when(AppContext.databaseManager.getGeneric(20, DatabaseManager.INDEX_EPISODE))
                .thenReturn(episode);
        when(AppContext.databaseManager.getGeneric(30, DatabaseManager.INDEX_GROUP))
                .thenReturn(group);
        Job job = new Job(Files.createFile(tempDir.resolve("gather.mkv")).toFile(), Job.HASHWAIT);
        AniDBFile file = new AniDBFile(40);
        file.setAnimeId(10);
        file.setEpisodeId(20);
        file.setGroupId(30);
        file.setJob(job);
        job.anidbFile = file;

        cache.gatherInfo(job, true);

        assertSame(anime, file.getAnime());
        assertSame(episode, file.getEpisode());
        assertSame(group, file.getGroup());
        assertTrue(AppContext.animeTreeRoot.has(anime));
    }

    @Test
    void gatherInfoIgnoresMissingJobsAndFiles() {
        Cache cache = new Cache();

        cache.gatherInfo(null, true);
        cache.gatherInfo(mock(Job.class), true);

        assertEquals(0, AppContext.animeTreeRoot.size());
    }

    @Test
    void invalidFileResponseRecordsTheProtocolProblemWithoutPersisting() throws IOException {
        Cache cache = new Cache();
        Job job = new Job(Files.createFile(tempDir.resolve("invalid.mkv")).toFile(), Job.HASHWAIT);

        Cache.FileParseResult result = cache.parseFileWithPersistence(new String[] {"short"}, job);

        assertNull(result.file());
        assertFalse(result.persisted());
        assertEquals("Unexpected response from server.", job.errorMessage);
        verify(AppContext.databaseManager, times(0)).update(anyInt(), any(), anyInt());
    }

    @Test
    void parsingReusesCachedEntitiesAndCharacterizesMalformedYears() throws IOException {
        Cache cache = new Cache();
        when(AppContext.databaseManager.update(anyInt(), any(), anyInt())).thenReturn(true);
        Job firstJob = new Job(Files.createFile(tempDir.resolve("first.mkv")).toFile(), Job.HASHWAIT);
        Job secondJob = new Job(Files.createFile(tempDir.resolve("second.mkv")).toFile(), Job.HASHWAIT);
        String[] firstResponse = fileResponseFields();
        firstResponse[28] = "2020-2022";
        String[] secondResponse = fileResponseFields();
        secondResponse[0] = "101";
        secondResponse[28] = "unknown";

        AniDBFile first = cache.parseFile(firstResponse, firstJob);
        AniDBFile second = cache.parseFile(secondResponse, secondJob);

        assertSame(first.getAnime(), second.getAnime());
        assertSame(first.getEpisode(), second.getEpisode());
        assertSame(first.getGroup(), second.getGroup());
        // TODO: Review whether malformed year metadata should overwrite a previously valid cached year with zero.
        assertEquals(0, second.getAnime().year);
        assertEquals(0, second.getAnime().endYear);
        assertTrue(AppContext.animeTreeRoot.has(second.getAnime()));
    }

    @Test
    void rebuildTreeAddsEveryEligibleJobFromTheJobList() throws IOException {
        Cache cache = new Cache();
        Job first = buildCompleteJob(61, "1", false);
        Job second = buildCompleteJob(62, "1", false);
        when(AppContext.jobs.array()).thenReturn(new Job[] {first, second});

        cache.rebuildTree();

        assertEquals(2, AppContext.animeTreeRoot.size());
    }

    @Test
    void treeFiltersAltVisibilityIncompleteMissingAndPathRegexJobs() throws IOException {
        Cache cache = new Cache();
        Job invisible = buildCompleteJob(71, "1", false);
        invisible.setAltVisible(false);
        cache.treeAdd(invisible);

        Job incomplete =
                new Job(Files.createFile(tempDir.resolve("incomplete.mkv")).toFile(), Job.HASHWAIT);
        cache.treeAdd(incomplete);

        Cache.setHideNew(true);
        cache.treeAdd(buildCompleteJob(72, "1", true));

        Cache.setHideNew(false);
        AppContext.pathRegex = ".*allowed.*";
        cache.treeAdd(buildCompleteJob(73, "1", false));

        assertEquals(0, AppContext.animeTreeRoot.size());
    }

    @Test
    void animeFileModeAddsAndRemovesTheFileDirectly() throws IOException {
        Cache cache = new Cache();
        Cache.setTreeSortMode(Cache.MODE_ANIME_FILE);
        Job job = buildCompleteJob(81, "1", false);
        Anime anime = job.anidbFile.getAnime();

        cache.treeAdd(job);
        assertTrue(anime.has(job.anidbFile));

        cache.treeRemove(job);
        assertFalse(anime.has(job.anidbFile));
        assertFalse(AppContext.animeTreeRoot.has(anime));
    }

    @Test
    void animeGroupModeReusesTheGroupNodeAndKeepsItUntilItsLastFileIsRemoved() throws IOException {
        Cache cache = new Cache();
        Cache.setTreeSortMode(Cache.MODE_ANIME_GROUP_FILE);
        Job first = buildCompleteJob(91, "1", false);
        Job second = buildCompleteJob(92, "1", false);
        shareRelations(first, second);
        Anime anime = first.anidbFile.getAnime();
        Group group = first.anidbFile.getGroup();

        cache.treeAdd(first);
        cache.treeAdd(second);
        AnimeGroup groupNode = (AnimeGroup) anime.get(group.getKey());
        assertEquals(2, groupNode.size());

        cache.treeRemove(second);
        assertEquals(1, groupNode.size());
        assertTrue(anime.has(groupNode));

        cache.treeRemove(first);
        assertFalse(AppContext.animeTreeRoot.has(anime));
    }

    @Test
    void animeGroupModeUsesTheRawGroupForFilesWithoutAGroupId() throws IOException {
        Cache cache = new Cache();
        Cache.setTreeSortMode(Cache.MODE_ANIME_GROUP_FILE);
        Job job = buildCompleteJob(93, "1", false);
        job.anidbFile.setGroupId(0);
        job.anidbFile.setGroup(null);

        cache.treeAdd(job);

        assertSame(Group.NONE, job.anidbFile.getGroup());
        assertTrue(job.anidbFile.getAnime().has(Group.NONE));
    }

    @Test
    void animeFolderModeReusesFolderNodesAndRetainsNonEmptyFolders() throws IOException {
        Cache cache = new Cache();
        Cache.setTreeSortMode(Cache.MODE_ANIME_FOLDER_FILE);
        Job first = buildCompleteJob(101, "1", false);
        Job second = buildCompleteJob(102, "1", false);
        shareRelations(first, second);
        Anime anime = first.anidbFile.getAnime();
        Object folderKey = first.getFile().getParent();

        cache.treeAdd(first);
        cache.treeAdd(second);
        epox.webaom.data.Path folder = (epox.webaom.data.Path) anime.get(folderKey);
        assertEquals(2, folder.size());

        cache.treeRemove(second);
        assertEquals(1, folder.size());
        assertTrue(anime.has(folder));

        cache.treeRemove(first);
        assertFalse(AppContext.animeTreeRoot.has(anime));
    }

    @Test
    void episodeModeRetainsAnEpisodeNodeUntilItsLastFileIsRemoved() throws IOException {
        Cache cache = new Cache();
        Job first = buildCompleteJob(111, "1", false);
        Job second = buildCompleteJob(112, "1", false);
        shareRelations(first, second);
        Anime anime = first.anidbFile.getAnime();
        Episode episode = first.anidbFile.getEpisode();

        cache.treeAdd(first);
        cache.treeAdd(second);
        cache.treeRemove(second);

        assertTrue(anime.has(episode));
        assertEquals(1, episode.size());

        cache.treeRemove(first);
        assertFalse(AppContext.animeTreeRoot.has(anime));
    }

    @Test
    void treeRemoveIgnoresIncompleteJobs() throws IOException {
        Cache cache = new Cache();
        Job job = new Job(
                Files.createFile(tempDir.resolve("remove-incomplete.mkv")).toFile(), Job.HASHWAIT);

        cache.treeRemove(job);

        assertEquals(0, AppContext.animeTreeRoot.size());
    }

    private static String[] fileResponseFields() {
        return new String[] {
            "100",
            "10",
            "20",
            "30",
            "0",
            "0",
            "3",
            "a448017aaf21d8525fc10ae87aa6729d",
            "",
            "",
            "",
            "japanese",
            "english",
            "high",
            "BluRay",
            "AAC",
            "H264",
            "1920x1080",
            "mkv",
            "24",
            "Group",
            "GRP",
            "1",
            "Episode",
            "",
            "",
            "12",
            "12",
            "2024",
            "TV",
            "Anime",
            "",
            "",
            "action"
        };
    }

    private Job buildCompleteJob(int idBase, String episodeNumber, boolean missingFile) throws IOException {
        Path filePath = missingFile
                ? tempDir.resolve("missing-" + idBase + ".mkv")
                : Files.createFile(tempDir.resolve("file-" + idBase + ".mkv"));
        Job job = new Job(filePath.toFile(), Job.HASHWAIT);

        Anime anime = buildAnime(1000 + idBase);
        Episode episode = buildEpisode(2000 + idBase, episodeNumber);
        Group group = new Group(3000 + idBase);
        group.name = "Group" + idBase;
        group.shortName = "G" + idBase;

        AniDBFile file = new AniDBFile(4000 + idBase);
        file.setAnimeId(anime.getId());
        file.setEpisodeId(episode.getId());
        file.setGroupId(group.getId());
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        file.setJob(job);
        file.setDefaultName("File-" + idBase);
        job.anidbFile = file;
        return job;
    }

    private Anime buildAnime(int id) {
        Anime anime = new Anime(id);
        anime.romajiTitle = "Anime " + id;
        anime.kanjiTitle = null;
        anime.englishTitle = null;
        anime.type = "TV";
        anime.episodeCount = 1;
        anime.latestEpisode = 1;
        anime.init();
        return anime;
    }

    private Episode buildEpisode(int id, String num) {
        Episode episode = new Episode(id);
        episode.num = num;
        episode.eng = "Episode " + num;
        episode.rom = null;
        episode.kan = null;
        return episode;
    }

    private static void shareRelations(Job source, Job target) {
        target.anidbFile.setAnimeId(source.anidbFile.getAnimeId());
        target.anidbFile.setEpisodeId(source.anidbFile.getEpisodeId());
        target.anidbFile.setGroupId(source.anidbFile.getGroupId());
        target.anidbFile.setAnime(source.anidbFile.getAnime());
        target.anidbFile.setEpisode(source.anidbFile.getEpisode());
        target.anidbFile.setGroup(source.anidbFile.getGroup());
    }
}
