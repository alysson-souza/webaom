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
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.webaom.db.SQLiteDatabaseManager;
import epox.webaom.ui.actions.jobs.JobDeleteScope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobVisibilityPersistenceTest {
    @TempDir
    Path tempDir;

    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private Cache originalCache;
    private epox.webaom.db.DatabaseManager originalDatabaseManager;
    private String originalPathRegex;

    @BeforeEach
    void setUp() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalCache = AppContext.cache;
        originalDatabaseManager = AppContext.databaseManager;
        originalPathRegex = AppContext.pathRegex;

        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.cache = new Cache();
        AppContext.databaseManager = null;
        AppContext.pathRegex = null;
        AppContext.animeTreeRoot.clear();
        Cache.setTreeSortMode(Cache.MODE_ANIME_EPISODE_FILE);
    }

    @AfterEach
    void tearDown() {
        if (AppContext.databaseManager != null) {
            AppContext.databaseManager.shutdown();
        }
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.cache = originalCache;
        AppContext.databaseManager = originalDatabaseManager;
        AppContext.pathRegex = originalPathRegex;
        AppContext.animeTreeRoot.clear();
    }

    @Test
    void deletingFromJobsKeepsAltVisibleAcrossReload() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("episode.mkv"), "video");
        Path dbPath = tempDir.resolve("visibility.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(true);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 1, 1, "hash-1");

        loadJobsAndBuildTree(manager);
        assertEquals(1, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals(1, AppContext.animeTreeRoot.size());

        Job job = AppContext.jobs.array()[0];
        assertEquals(1, JobManager.deleteJobs(java.util.List.of(job), JobDeleteScope.JOBS));
        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertTrue(job.isAltVisible());
        assertEquals(1, AppContext.animeTreeRoot.size());

        AppContext.jobs.clear();
        AppContext.cache.clear();
        AppContext.animeTreeRoot.clear();

        loadJobsAndBuildTree(manager);
        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals(1, AppContext.animeTreeRoot.size());
    }

    @Test
    void loadingAltOnlyRow_doesNotCountAsJobsWorkOrCounterState() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("alt-only-media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("alt-only.mkv"), "video");
        Path dbPath = tempDir.resolve("alt-only.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(true);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 0, 1, "hash-alt");

        loadJobsAndBuildTree(manager);

        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals(1, AppContext.animeTreeRoot.size());
        assertTrue(AppContext.jobCounter.getStatus().contains("tot=0"));

        Job hiddenJob = AppContext.jobs.array()[0];
        assertEquals(1, JobManager.deleteJobs(java.util.List.of(hiddenJob), JobDeleteScope.ALT));
        assertTrue(AppContext.jobCounter.getStatus().contains("tot=0"));
    }

    @Test
    void loadingFinishedAltOnlyRow_ignoresLoadCompletedJobsFilter() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("alt-finished-media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("alt-finished.mkv"), "video");
        Path dbPath = tempDir.resolve("alt-finished.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(false);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 0, 1, "hash-finished-alt");

        loadJobsAndBuildTree(manager);

        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals(1, AppContext.animeTreeRoot.size());
    }

    @Test
    void deletingFromJobs_usesPersistedIdentityEvenIfCurrentFileChanges() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("mutated-media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("mutated.mkv"), "video");
        Path dbPath = tempDir.resolve("mutated.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(true);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 1, 1, "hash-mutated");

        loadJobsAndBuildTree(manager);
        Job job = AppContext.jobs.array()[0];

        Path movedFile = Files.writeString(mediaDirectory.resolve("mutated-moved.mkv"), "video-with-extra-bytes");
        JobManager.setJobFile(job, movedFile.toFile());

        assertEquals(1, JobManager.deleteJobs(java.util.List.of(job), JobDeleteScope.JOBS));
        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertTrue(job.isAltVisible());
    }

    @Test
    void loadingFinishedSharedRow_hidesItFromJobsWithoutChangingPersistedMembership() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("shared-finished-media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("shared-finished.mkv"), "video");
        Path dbPath = tempDir.resolve("shared-finished.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(false);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 1, 1, "hash-shared-finished");

        loadJobsAndBuildTree(manager);

        assertEquals(0, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertTrue(AppContext.jobs.array()[0].isJobsVisible());
        assertEquals(1, AppContext.animeTreeRoot.size());
    }

    @Test
    void deletingFromAlt_hiddenFinishedSharedRow_preservesJobsEntryForLoadCompleted() throws Exception {
        Path mediaDirectory = Files.createDirectory(tempDir.resolve("shared-alt-delete-media"));
        Path mediaFile = Files.writeString(mediaDirectory.resolve("shared-alt-delete.mkv"), "video");
        Path dbPath = tempDir.resolve("shared-alt-delete.sqlite");

        SQLiteDatabaseManager manager = new SQLiteDatabaseManager();
        assertTrue(manager.initialize("jdbc:sqlite:" + dbPath));
        manager.setLoadAllJobs(false);
        AppContext.databaseManager = manager;
        insertVisibleJobRecord(dbPath, mediaDirectory, mediaFile, 1, 1, "hash-shared-alt-delete");

        loadJobsAndBuildTree(manager);
        Job job = AppContext.jobs.array()[0];

        assertEquals(1, JobManager.deleteJobs(java.util.List.of(job), JobDeleteScope.ALT));

        AppContext.jobs.clear();
        AppContext.cache.clear();
        AppContext.animeTreeRoot.clear();
        manager.setLoadAllJobs(true);

        loadJobsAndBuildTree(manager);

        assertEquals(1, AppContext.jobs.size());
        assertEquals(1, AppContext.jobs.array().length);
        assertEquals(0, AppContext.animeTreeRoot.size());
    }

    private void loadJobsAndBuildTree(SQLiteDatabaseManager manager) {
        manager.getJobs();
        for (Job job : AppContext.jobs.array()) {
            AppContext.cache.gatherInfo(job, false);
        }
        AppContext.cache.rebuildTree();
    }

    private void insertVisibleJobRecord(
            Path dbPath, Path mediaDirectory, Path mediaFile, int jobsVisible, int altVisible, String hash)
            throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "INSERT INTO atb (aid, romaji, kanji, english, year, episodes, last_ep, type, genre, img) "
                            + "VALUES (1, 'Anime 1', NULL, NULL, 2024, 1, 1, 'TV', '', 0)");
            statement.executeUpdate(
                    "INSERT INTO etb (eid, english, kanji, romaji, number) VALUES (1, 'Episode 1', NULL, NULL, '1')");
            statement.executeUpdate(
                    "INSERT INTO dtb (did, name) VALUES (1, '" + escape(mediaDirectory.toString()) + "')");
            statement.executeUpdate(
                    "INSERT INTO ftb (fid, aid, eid, gid, state, size, len, ed2k, md5, sha1, crc32, ripsource, "
                            + "quality, audio, video, resolution, def_name, sublang, dublang, ext) VALUES "
                            + "(1, 1, 1, 0, 0, "
                            + Files.size(mediaFile)
                            + ", 0, '"
                            + hash
                            + "', NULL, NULL, NULL, '', '', '', '', '', 'Episode 1', '', '', 'mkv')");
            statement.executeUpdate(
                    "INSERT INTO jtb (orig, name, did, fid, status, jobs_visible, alt_visible, ed2k, md5, sha1, "
                            + "tth, crc32, size, uid, lid, avxml) VALUES ('episode.mkv', 'episode.mkv', 1, 1, "
                            + Job.FINISHED
                            + ", "
                            + jobsVisible
                            + ", "
                            + altVisible
                            + ", '"
                            + hash
                            + "', NULL, NULL, NULL, NULL, "
                            + Files.size(mediaFile)
                            + ", 1, 0, NULL)");
        }
    }

    private String escape(String value) {
        return value.replace("'", "''");
    }
}
