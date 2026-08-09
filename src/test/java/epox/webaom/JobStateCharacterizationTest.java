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
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, see <https://www.gnu.org/licenses/>.
 */

package epox.webaom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import epox.av.FileInfo;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AttributeMap;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JobStateCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    private JobList originalJobs;
    private JobCounter originalJobCounter;

    @BeforeEach
    void installIsolatedJobState() {
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void restoreJobState() {
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
    }

    @Test
    void constructorsDeriveHealthFromExistenceAndPreserveSerializedVisibility() throws Exception {
        Path existing = Files.writeString(temporaryDirectory.resolve("existing.mkv"), "video");
        Job existingJob = new Job(existing.toFile(), Job.HASHWAIT);
        Job missingJob = new Job(temporaryDirectory.resolve("missing.mkv").toFile(), Job.IDENTWAIT);

        assertEquals(Job.H_PAUSED, existingJob.getHealth());
        assertEquals(Job.H_MISSING, missingJob.getHealth());
        assertEquals("Wait/Hash [P]", existingJob.getStatusText());
        assertEquals("Wait/ID [M]", missingJob.getStatusText());

        Job restored = new Job(new String[] {
            Integer.toString(Job.FINISHED),
            existing.toString(),
            "5",
            "ed2k",
            "null",
            "",
            "tth",
            "crc",
            "original.mkv",
            "0",
            "1"
        });

        assertEquals(Job.FINISHED, restored.getStatus());
        assertFalse(restored.isJobsVisible());
        assertTrue(restored.isAltVisible());
        assertNull(restored.md5Hash);
        assertNull(restored.sha1Hash);
        assertTrue(restored.serialize().endsWith("|0|1"));
    }

    @Test
    void statusChecksCoverMasksLocksAndFileFlags() throws Exception {
        Job job = new Job(
                Files.createFile(temporaryDirectory.resolve("checks.mkv")).toFile(), Job.HASHWAIT);
        job.setHealth0(Job.H_NORMAL);

        assertTrue(job.check(Job.S_DO));
        assertTrue(job.checkOr(Job.S_DO | Job.S_FAILED));
        assertTrue(job.checkSep(Job.S_DO | Job.H_NORMAL | Job.D_DIO, 0, true));
        assertFalse(job.checkSep(Job.S_DO | Job.H_NORMAL | Job.D_DIO, AniDBFile.F_CRCERR, false));
        assertFalse(job.isLocked(Job.FINISHED));

        AniDBFile file = new AniDBFile(1);
        file.setState(AniDBFile.F_CRCERR | AniDBFile.F_ISV3);
        job.anidbFile = file;
        assertTrue(job.checkSep(Job.S_DO | Job.H_NORMAL | Job.D_DIO, AniDBFile.F_CRCERR, false));
        assertFalse(job.checkSep(Job.S_DO | Job.H_NORMAL | Job.D_DIO, AniDBFile.F_UNC, false));
        assertTrue(job.isCorrupt());
        assertTrue(job.incompl());

        job.setHealth0(Job.H_MISSING);
        assertFalse(job.isLocked(Job.FINISHED));
        assertFalse(job.isLocked(Job.ADDWAIT));
        assertFalse(job.isLocked(Job.REMWAIT));
        assertTrue(job.isLocked(Job.HASHWAIT));
    }

    @Test
    void healthUpdatesTogglePauseButNeverReviveAMissingJob() throws Exception {
        Job job = new Job(
                Files.createFile(temporaryDirectory.resolve("health.mkv")).toFile(), Job.HASHWAIT);
        job.setHealth0(Job.H_NORMAL);
        AppContext.jobs.updateQueues(job, -1, Job.HASHWAIT);

        job.updateHealth(Job.H_PAUSED);
        assertEquals(Job.H_PAUSED, job.getHealth());
        assertFalse(AppContext.jobs.workForDio());

        job.updateHealth(Job.H_PAUSED);
        assertEquals(Job.H_NORMAL, job.getHealth());
        assertTrue(AppContext.jobs.workForDio());

        job.updateHealth(Job.H_MISSING);
        job.updateHealth(Job.H_PAUSED);
        assertEquals(Job.H_MISSING, job.getHealth());
    }

    @Test
    void findOnlyRelocatesMissingJobsAndPausesThemWhenFound() throws Exception {
        Job job = new Job(temporaryDirectory.resolve("lost.mkv").toFile(), Job.HASHWAIT);
        Path found = Files.createFile(temporaryDirectory.resolve("found.mkv"));

        job.find(found.toFile());

        assertEquals(found.toFile(), job.getFile());
        assertEquals(Job.H_PAUSED, job.getHealth());
        assertEquals(-1, job.directoryId);

        Path ignored = Files.createFile(temporaryDirectory.resolve("ignored.mkv"));
        job.find(ignored.toFile());
        assertEquals(found.toFile(), job.getFile());
    }

    @Test
    void extensionAndHideUseAniDbMetadataWhenPresent() throws Exception {
        Path path = Files.createFile(temporaryDirectory.resolve("Episode.MKV"));
        Job job = new Job(path.toFile(), Job.HASHWAIT);

        assertEquals("MKV", job.getExtension());
        assertFalse(job.hide(null));
        assertFalse(job.hide(""));
        assertFalse(job.hide(".*Episode.*"));
        assertTrue(job.hide(".*Other.*"));

        AniDBFile file = new AniDBFile(1);
        file.setExtension("m4v");
        job.anidbFile = file;
        assertEquals("m4v", job.getExtension());
        assertEquals(path.toFile(), job.getFile());
    }

    @Test
    void generatedMapIncludesHashesMetadataTitlesAndGroup() throws Exception {
        Path path = Files.writeString(temporaryDirectory.resolve("map.mkv"), "video");
        Job job = new Job(path.toFile(), Job.FINISHED);
        job.targetFile = temporaryDirectory.resolve("renamed.mkv").toFile();
        job.mylistId = 55;

        Anime anime = new Anime(11);
        anime.romajiTitle = "Romaji";
        anime.kanjiTitle = "Kanji";
        anime.englishTitle = "English";
        anime.categories = "Action,Drama";
        anime.type = "TV";
        anime.year = 2024;
        anime.endYear = 2025;
        anime.episodeCount = 12;
        anime.latestEpisode = 8;
        anime.init();
        Episode episode = new Episode(22);
        episode.num = "3";
        episode.eng = "Episode English";
        episode.rom = "Episode Romaji";
        episode.kan = "Episode Kanji";
        Group group = new Group(33);
        group.name = "Group Name";
        group.shortName = "GRP";

        AniDBFile file = new AniDBFile(44);
        file.setAnimeId(11);
        file.setEpisodeId(22);
        file.setGroupId(33);
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        file.setEd2kHash("abcdef");
        file.setMd5Hash("aabb");
        file.setShaHash("ccdd");
        file.setCrcHash("eeff");
        file.setDubLanguage("japanese");
        file.setSubLanguage("english");
        file.setRipSource("BluRay");
        file.setResolution("1920x1080");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        file.setQuality("high");
        job.anidbFile = file;

        AttributeMap map = job.genMap();

        assertEquals("map.mkv", map.get("fil"));
        assertEquals("44", map.get("fid"));
        assertEquals("55", map.get("lid"));
        assertEquals("ABCDEF", map.get("ED2"));
        assertEquals("AABB", map.get("MD5"));
        assertEquals("CCDD", map.get("SHA"));
        assertEquals("EEFF", map.get("CRC"));
        assertEquals("Action, Drama", map.get("gen"));
        assertEquals("03", map.get("enr"));
        assertEquals("GRP", map.get("grp"));
        assertEquals("Group Name", map.get("grn"));
    }

    @Test
    void convertExpandsEachAvSectionAndThenJobCodes() throws Exception {
        Job job = new Job(
                Files.createFile(temporaryDirectory.resolve("convert.mkv")).toFile(), Job.FINISHED);
        FileInfo avInfo = mock(FileInfo.class);
        when(avInfo.convert("%vcd", 0)).thenReturn("video");
        when(avInfo.convert("%acd", 1)).thenReturn("audio");
        when(avInfo.convert("%scd", 2)).thenReturn("subtitle");
        job.avFileInfo = avInfo;

        String converted = job.convert("<avinfo><vid>%vcd</vid><aud>%acd</aud><sub>%scd</sub></avinfo> %fil");

        assertEquals("<avinfo><vid>video</vid><aud>audio</aud><sub>subtitle</sub></avinfo> convert.mkv", converted);
    }
}
