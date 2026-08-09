/*
 * WebAOM - Web Anime-O-Matic
 * Copyright (C) 2005-2010 epoximator 2026 Alysson Souza
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.Options;
import epox.webaom.Rules;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.db.DatabaseManager;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CliApplicationRenameCharacterizationTest {
    @TempDir
    Path tempDirectory;

    private JobList previousJobs;
    private JobCounter previousJobCounter;
    private DatabaseManager previousDatabaseManager;
    private Cache previousCache;
    private JobList jobs;
    private DatabaseManager databaseManager;

    @BeforeEach
    void setUp() {
        previousJobs = AppContext.jobs;
        previousJobCounter = AppContext.jobCounter;
        previousDatabaseManager = AppContext.databaseManager;
        previousCache = AppContext.cache;
        jobs = mock(JobList.class);
        databaseManager = mock(DatabaseManager.class);
        AppContext.jobs = jobs;
        AppContext.jobCounter = new JobCounter();
        AppContext.databaseManager = databaseManager;
        AppContext.cache = mock(Cache.class);
        when(databaseManager.update(anyInt(), any(), anyInt())).thenReturn(true);
    }

    @AfterEach
    void tearDown() {
        AppContext.jobs = previousJobs;
        AppContext.jobCounter = previousJobCounter;
        AppContext.databaseManager = previousDatabaseManager;
        AppContext.cache = previousCache;
    }

    @Test
    void cachedIdentificationProducesARenamePreviewWithoutNetworkAccess() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);
        Rules rules = renameRules("renamed");
        CliApplication application = application(rules);

        List<CliFileResult> results = application.rename(List.of(source), false, false, null, silentOutput());

        CliFileResult result = results.getFirst();
        assertEquals(CliFileResult.Status.RENAME_PLANNED, result.status, result.message);
        assertEquals(tempDirectory.resolve("renamed.mkv"), result.target);
        assertEquals("Preview only.", result.message);
        assertFalse(result.applied);
        assertTrue(Files.exists(source));
    }

    @Test
    void applyingRenameMovesPrimaryAndCompanionAndPersistsTheNewJobPath() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Path subtitle = Files.writeString(tempDirectory.resolve("episode.en.srt"), "subtitle");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);
        CliApplication application = application(renameRules("renamed"));

        CliFileResult result = application
                .rename(List.of(source), true, true, null, silentOutput(), CliApplication.Refresh.NONE)
                .getFirst();

        Path target = tempDirectory.resolve("renamed.mkv");
        assertEquals(CliFileResult.Status.RENAMED, result.status, result.message);
        assertTrue(result.applied);
        assertEquals(target.toFile(), job.currentFile);
        assertNull(job.targetFile);
        assertEquals(-1, job.directoryId);
        assertTrue(Files.exists(target));
        assertTrue(Files.exists(tempDirectory.resolve("renamed.en.srt")));
        assertFalse(Files.exists(source));
        assertFalse(Files.exists(subtitle));
        verify(databaseManager).update(0, job, DatabaseManager.INDEX_JOB);
    }

    @Test
    void unchangedRulesPreserveTheSourceAndExplainTheCurrentResult() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);
        CliApplication application = application(renameRules("episode"));

        CliFileResult result = application
                .rename(List.of(source), true, false, null, silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.UNCHANGED, result.status, result.message);
        assertEquals("Rules leave the path unchanged.", result.message);
        assertEquals(source, result.target);
    }

    @Test
    void commandRuleDocumentOverridesConfiguredRules() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);
        RuleDocument override = new RuleDocument("DO SET 'override'", "# no move rule");

        CliFileResult result = application(renameRules("configured"))
                .rename(List.of(source), true, false, override, silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.RENAME_PLANNED, result.status);
        assertEquals(tempDirectory.resolve("override.mkv"), result.target);
    }

    @Test
    void persistedRulesAreLoadedWhenTheOptionsContainScripts() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);
        Options options = new Options(tempDirectory.resolve("configured-options"));
        renameRules("from-options").saveToOptions(options);

        CliFileResult result = application(options, renameRules("ignored"))
                .rename(List.of(source), true, false, null, silentOutput())
                .getFirst();

        assertEquals(tempDirectory.resolve("from-options.mkv"), result.target);
    }

    @Test
    void excessivelyLongRuleResultIsTruncatedBeforePreflight() throws Exception {
        Path source = Files.writeString(tempDirectory.resolve("episode.mkv"), "video");
        Job job = identifiedJob(source);
        when(jobs.add(source.toFile())).thenReturn(job);

        CliFileResult result = application(renameRules("x".repeat(300)))
                .rename(List.of(source), true, false, null, silentOutput())
                .getFirst();

        assertEquals(CliFileResult.Status.RENAME_PLANNED, result.status);
        assertTrue(result.target.getFileName().toString().endsWith(".mkv"));
        assertTrue(result.target.getFileName().toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length
                <= Rules.MAX_FILENAME_BYTES);
    }

    private CliApplication application(Rules rules) {
        return application(new Options(tempDirectory.resolve("config")), rules);
    }

    private CliApplication application(Options options, Rules rules) {
        return new CliApplication(options, rules, Set.of("mkv"), settings -> {
            throw new AssertionError("Cached identification must not access the network");
        });
    }

    private static Rules renameRules(String baseName) {
        Rules rules = new Rules();
        rules.setRenameRules("DO SET '" + baseName + "'");
        rules.setMoveRules("# no move rule");
        return rules;
    }

    private static Job identifiedJob(Path source) {
        Job job = new Job(source.toFile(), Job.FINISHED);
        job.ed2kHash = "cached-ed2k";
        job.fileSize = source.toFile().length();
        AniDBFile file = new AniDBFile(100);
        file.setExtension("mkv");
        file.setDefaultName("episode");
        file.setEd2kHash("cached-ed2k");
        file.setCrcHash("crc");
        file.setMd5Hash("md5");
        file.setShaHash("sha1");
        file.setDubLanguage("jap");
        file.setSubLanguage("eng");
        file.setQuality("HD");
        file.setRipSource("BluRay");
        file.setResolution("1920x1080");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        Anime anime = new Anime(10);
        anime.year = 2026;
        anime.endYear = 2026;
        anime.episodeCount = 1;
        anime.latestEpisode = 1;
        anime.type = "TV";
        anime.romajiTitle = "Anime";
        anime.kanjiTitle = "Anime";
        anime.englishTitle = "Anime";
        anime.categories = "test";
        anime.init();
        Episode episode = new Episode(20);
        episode.num = "1";
        episode.eng = "Episode";
        episode.rom = "Episode";
        episode.kan = "Episode";
        Group group = new Group(30);
        group.name = "Group";
        group.shortName = "Group";
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        file.setJob(job);
        job.anidbFile = file;
        return job;
    }

    private static CliOutput silentOutput() {
        PrintWriter discard = new PrintWriter(OutputStream.nullOutputStream());
        return new CliOutput(discard, discard, CliOutput.Format.TABLE, true, false);
    }
}
