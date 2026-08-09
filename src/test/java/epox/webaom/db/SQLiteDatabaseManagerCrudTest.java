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

package epox.webaom.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SQLiteDatabaseManagerCrudTest {
    @TempDir
    Path tempDir;

    private DatabaseManager previousDatabaseManager;
    private Cache previousCache;
    private JobCounter previousJobCounter;
    private JobList previousJobs;
    private TestableSQLiteDatabaseManager manager;

    @BeforeEach
    void setUp() {
        previousDatabaseManager = AppContext.databaseManager;
        previousCache = AppContext.cache;
        previousJobCounter = AppContext.jobCounter;
        previousJobs = AppContext.jobs;

        manager = new TestableSQLiteDatabaseManager();
        manager.setUiPromptsAvailable(false);
        AppContext.databaseManager = manager;
        AppContext.cache = new Cache();
        AppContext.jobCounter = new JobCounter();
        AppContext.jobs = new JobList();

        Path database = tempDir.resolve("nested/database/webaom.sqlite");
        assertTrue(manager.initialize("jdbc:sqlite:" + database));
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdown();
        }
        AppContext.databaseManager = previousDatabaseManager;
        AppContext.cache = previousCache;
        AppContext.jobCounter = previousJobCounter;
        AppContext.jobs = previousJobs;
    }

    @Test
    void entityInsertUpdateAndGetRoundTripAllSupportedMetadataTypes() {
        Anime anime = anime(10);
        Episode episode = episode(20);
        Group group = group(30);

        assertTrue(manager.update(anime.getId(), anime, DatabaseManager.INDEX_ANIME));
        assertTrue(manager.update(episode.getId(), episode, DatabaseManager.INDEX_EPISODE));
        assertTrue(manager.update(group.getId(), group, DatabaseManager.INDEX_GROUP));

        Anime loadedAnime =
                assertInstanceOf(Anime.class, manager.getGeneric(anime.getId(), DatabaseManager.INDEX_ANIME));
        assertEquals("Romaji", loadedAnime.romajiTitle);
        assertEquals("Kanji", loadedAnime.kanjiTitle);
        assertEquals("English", loadedAnime.englishTitle);
        assertEquals(2024, loadedAnime.year);
        assertEquals(12, loadedAnime.episodeCount);
        assertEquals(8, loadedAnime.latestEpisode);
        assertEquals("TV", loadedAnime.type);
        assertEquals("Comedy", loadedAnime.categories);

        Episode loadedEpisode =
                assertInstanceOf(Episode.class, manager.getGeneric(episode.getId(), DatabaseManager.INDEX_EPISODE));
        assertEquals("1", loadedEpisode.num);
        assertEquals("Episode English", loadedEpisode.eng);
        assertEquals("Episode Romaji", loadedEpisode.rom);
        assertEquals("Episode Kanji", loadedEpisode.kan);

        Group loadedGroup =
                assertInstanceOf(Group.class, manager.getGeneric(group.getId(), DatabaseManager.INDEX_GROUP));
        assertEquals("Group's Name", loadedGroup.name);
        assertEquals("GN", loadedGroup.shortName);

        group.name = "Updated Group";
        assertTrue(manager.update(group.getId(), group, DatabaseManager.INDEX_GROUP));
        Group updated = assertInstanceOf(Group.class, manager.getGeneric(group.getId(), DatabaseManager.INDEX_GROUP));
        assertEquals("Updated Group", updated.name);

        assertNull(manager.getGeneric(999, DatabaseManager.INDEX_ANIME));
        assertNull(manager.getGeneric(999, DatabaseManager.INDEX_FILE));
    }

    @Test
    void directoryIdsAreStableCachedAndHandleQuotedAndNullPaths() {
        int quotedPathId =
                manager.getDirectoryId(tempDir.resolve("Group's Shows").toString());
        assertTrue(quotedPathId > 0);
        assertEquals(
                quotedPathId,
                manager.getDirectoryId(tempDir.resolve("Group's Shows").toString()));

        int otherPathId = manager.getDirectoryId(tempDir.resolve("Other").toString());
        assertTrue(otherPathId > 0);
        assertNotEquals(quotedPathId, otherPathId);

        int nullPathId = manager.getDirectoryId(null);
        assertTrue(nullPathId > 0);
        assertEquals(nullPathId, manager.getDirectoryId(""));
    }

    @Test
    void jobInsertUpdateLookupAndBulkLoadRoundTripPersistentFields() throws Exception {
        File mediaFile = createMediaFile("episode.mkv");
        persistFileDependencies();
        Job stored = job(mediaFile, "ed2k-job");

        assertTrue(manager.update(0, stored, DatabaseManager.INDEX_JOB));

        Job lookup = new Job(mediaFile, Job.HASHWAIT);
        int status = manager.getJob(lookup, false);
        assertEquals(Job.HASHWAIT, status);
        assertFalse(lookup.isFresh);
        assertNull(lookup.targetFile);
        assertEquals("ed2k-job", lookup.ed2kHash);
        assertEquals("md5", lookup.md5Hash);
        assertEquals("sha1", lookup.sha1Hash);
        assertEquals("tth", lookup.tthHash);
        assertEquals("crc32", lookup.crc32Hash);
        assertEquals(77, lookup.mylistId);
        assertTrue(lookup.isJobsVisible());
        assertTrue(lookup.isAltVisible());
        assertNotNull(lookup.anidbFile);
        assertEquals(40, lookup.anidbFile.getFileId());
        assertNull(lookup.anidbFile.getEpisode());
        Episode cachedEpisode =
                assertInstanceOf(Episode.class, AppContext.cache.get(20, DatabaseManager.INDEX_EPISODE));
        assertEquals("Episode English", cachedEpisode.eng);

        stored.md5Hash = "updated-md5";
        stored.setAltVisible(false);
        assertTrue(manager.update(0, stored, DatabaseManager.INDEX_JOB));
        Job hashLookup = new Job(mediaFile, Job.HASHWAIT);
        hashLookup.ed2kHash = stored.ed2kHash;
        assertEquals(Job.HASHWAIT, manager.getJob(hashLookup, true));
        assertEquals("updated-md5", hashLookup.md5Hash);
        assertFalse(hashLookup.isAltVisible());

        AppContext.jobs.clear();
        manager.setLoadAllJobs(true);
        assertTrue(manager.isLoadAllJobs());
        manager.getJobs();
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals("ed2k-job", AppContext.jobs.array()[0].ed2kHash);
    }

    @Test
    void visibilityUpdateCanHideOneViewOrDeleteThePersistentJob() throws Exception {
        File mediaFile = createMediaFile("visibility.mkv");
        persistFileDependencies();
        Job stored = job(mediaFile, "visibility-hash");
        assertTrue(manager.update(0, stored, DatabaseManager.INDEX_JOB));
        assertTrue(manager.hasPersistentJobIdentity(stored));

        stored.setJobsVisible(false);
        stored.setAltVisible(true);
        assertTrue(manager.updateJobVisibility(stored));
        Job visibleInAlternateView = new Job(mediaFile, Job.HASHWAIT);
        visibleInAlternateView.ed2kHash = stored.ed2kHash;
        assertEquals(Job.HASHWAIT, manager.getJob(visibleInAlternateView, true));
        assertFalse(visibleInAlternateView.isJobsVisible());
        assertTrue(visibleInAlternateView.isAltVisible());

        stored.setAltVisible(false);
        assertTrue(manager.updateJobVisibility(stored));
        assertFalse(manager.removeJob(stored));
        assertEquals(-1, manager.getJob(visibleInAlternateView, true));

        assertFalse(manager.hasPersistentJobIdentity(null));
        assertFalse(manager.hasPersistentJobIdentity(new Job(mediaFile, Job.HASHWAIT)));
    }

    @Test
    void shutdownIsIdempotentAndDisablesCrudOperations() {
        assertTrue(manager.isConnected());
        assertTrue(manager.wasSchemaCreated());
        assertFalse(manager.initialize("jdbc:sqlite:" + tempDir.resolve("other.sqlite")));

        manager.shutdown();
        manager.shutdown();

        assertFalse(manager.isConnected());
        assertEquals(-1, manager.getDirectoryId("after-shutdown"));
        assertNull(manager.getGeneric(1, DatabaseManager.INDEX_ANIME));
        assertFalse(manager.update(1, anime(1), DatabaseManager.INDEX_ANIME));
    }

    @Test
    void factoryAndSqliteDialectHelpersPreserveCurrentSelectionRules() {
        assertInstanceOf(SQLiteDatabaseManager.class, DatabaseManagerFactory.create(null));
        assertInstanceOf(SQLiteDatabaseManager.class, DatabaseManagerFactory.create(""));
        assertInstanceOf(SQLiteDatabaseManager.class, DatabaseManagerFactory.create("JDBC:SQLITE:file.sqlite"));
        assertInstanceOf(PostgreSQLDatabaseManager.class, DatabaseManagerFactory.create("jdbc:postgresql://db/test"));
        assertInstanceOf(MySQLDatabaseManager.class, DatabaseManagerFactory.create("jdbc:mysql://db/test"));

        // TODO: Review whether an unsupported explicit JDBC URL should silently select an embedded database.
        assertInstanceOf(SQLiteDatabaseManager.class, DatabaseManagerFactory.create("jdbc:unknown:test"));

        assertEquals("'O''Brien'", manager.quoteString("O'Brien"));
        assertEquals("NULL", manager.quoteString(null));
        assertEquals("", manager.escape(null));
        assertEquals("O''Brien", manager.escape("O'Brien"));
        assertEquals(7, manager.supportedSchemaVersion());
        assertTrue(manager.transactionalSchemaChanges());
        assertTrue(DatabaseManagerFactory.getEmbeddedConnectionString().startsWith("jdbc:sqlite:"));
    }

    private File createMediaFile(String name) throws Exception {
        Path file = tempDir.resolve("media").resolve(name);
        Files.createDirectories(file.getParent());
        Files.writeString(file, "media bytes");
        return file.toFile();
    }

    private void persistFileDependencies() {
        Anime anime = anime(10);
        Episode episode = episode(20);
        Group group = group(30);
        assertTrue(manager.update(anime.getId(), anime, DatabaseManager.INDEX_ANIME));
        assertTrue(manager.update(episode.getId(), episode, DatabaseManager.INDEX_EPISODE));
        assertTrue(manager.update(group.getId(), group, DatabaseManager.INDEX_GROUP));
        assertTrue(manager.update(40, file(), DatabaseManager.INDEX_FILE));
    }

    private static Anime anime(int id) {
        Anime anime = new Anime(id);
        anime.romajiTitle = "Romaji";
        anime.kanjiTitle = "Kanji";
        anime.englishTitle = "English";
        anime.year = 2024;
        anime.endYear = 2024;
        anime.episodeCount = 12;
        anime.latestEpisode = 8;
        anime.type = "TV";
        anime.categories = "Comedy";
        anime.init();
        return anime;
    }

    private static Episode episode(int id) {
        Episode episode = new Episode(id);
        episode.num = "1";
        episode.eng = "Episode English";
        episode.rom = "Episode Romaji";
        episode.kan = "Episode Kanji";
        return episode;
    }

    private static Group group(int id) {
        Group group = new Group(id);
        group.name = "Group's Name";
        group.shortName = "GN";
        return group;
    }

    private static AniDBFile file() {
        AniDBFile file = new AniDBFile(40);
        file.setAnimeId(10);
        file.setEpisodeId(20);
        file.setGroupId(30);
        file.setDefaultName("Default Name.mkv");
        file.setState(AniDBFile.F_CRCOK);
        file.setTotalSize(11);
        file.setLengthInSeconds(1500);
        file.setEd2kHash("file-ed2k");
        file.setMd5Hash("file-md5");
        file.setShaHash("file-sha1");
        file.setCrcHash("file-crc32");
        file.setDubLanguage("japanese");
        file.setSubLanguage("english");
        file.setQuality("high");
        file.setRipSource("BluRay");
        file.setAudioCodec("AAC");
        file.setVideoCodec("H264");
        file.setResolution("1920x1080");
        file.setExtension("mkv");
        return file;
    }

    private static Job job(File mediaFile, String ed2k) {
        Job job = new Job(mediaFile, Job.HASHWAIT);
        job.ed2kHash = ed2k;
        job.md5Hash = "md5";
        job.sha1Hash = "sha1";
        job.tthHash = "tth";
        job.crc32Hash = "crc32";
        job.mylistId = 77;
        job.anidbFile = file();
        return job;
    }

    private static final class TestableSQLiteDatabaseManager extends SQLiteDatabaseManager {
        String escape(String value) {
            return escapeString(value);
        }

        int supportedSchemaVersion() {
            return getSupportedSchemaVersion();
        }

        boolean transactionalSchemaChanges() {
            return supportsTransactionalSchemaChanges();
        }
    }
}
