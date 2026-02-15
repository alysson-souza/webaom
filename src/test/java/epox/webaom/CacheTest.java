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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
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
    private String originalPathRegex;
    private int originalTreeSortMode;
    private boolean originalHideExisting;
    private boolean originalHideNew;

    @BeforeEach
    void setUp() {
        originalDatabaseManager = AppContext.databaseManager;
        originalJobCounter = AppContext.jobCounter;
        originalPathRegex = AppContext.pathRegex;
        originalTreeSortMode = Cache.getTreeSortMode();
        originalHideExisting = Cache.isHideExisting();
        originalHideNew = Cache.isHideNew();

        AppContext.databaseManager = mock(DatabaseManager.class);
        AppContext.jobCounter = new JobCounter();
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
}
