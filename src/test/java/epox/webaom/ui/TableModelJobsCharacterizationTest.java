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

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import epox.webaom.AppContext;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.TableModelEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TableModelJobsCharacterizationTest {
    @TempDir
    Path temporaryDirectory;

    private JobCounter originalJobCounter;

    @BeforeEach
    void installCounter() {
        originalJobCounter = AppContext.jobCounter;
        AppContext.jobCounter = new JobCounter();
    }

    @AfterEach
    void restoreCounter() {
        AppContext.jobCounter = originalJobCounter;
    }

    @Test
    void completeJobMapsEveryColumnToItsDomainValue() throws Exception {
        JobList jobs = new JobList();
        Job job = completeJob();
        jobs.add(job);
        TableModelJobs model = new TableModelJobs(jobs);

        assertEquals(1, model.getValueAt(0, JobColumn.NUMB.getIndex()));
        assertEquals(77, model.getValueAt(0, JobColumn.LIDN.getIndex()));
        assertEquals(30, model.getValueAt(0, JobColumn.FIDN.getIndex()));
        assertEquals(10, model.getValueAt(0, JobColumn.AIDN.getIndex()));
        assertEquals(20, model.getValueAt(0, JobColumn.EIDN.getIndex()));
        assertEquals(40, model.getValueAt(0, JobColumn.GIDN.getIndex()));
        assertEquals(2024, model.getValueAt(0, JobColumn.AYEA.getIndex()));
        assertEquals(12, model.getValueAt(0, JobColumn.AEPS.getIndex()));
        assertEquals(9, model.getValueAt(0, JobColumn.ALEP.getIndex()));
        assertEquals(job.fileSize, model.getValueAt(0, JobColumn.FSIZ.getIndex()));
        assertEquals(1440, model.getValueAt(0, JobColumn.FLEN.getIndex()));
        assertEquals(job.getFile().getAbsolutePath(), model.getValueAt(0, JobColumn.FILE.getIndex()));
        assertEquals(job.getFile().getParent(), model.getValueAt(0, JobColumn.PATH.getIndex()));
        assertEquals(job.getFile().getName(), model.getValueAt(0, JobColumn.NAME.getIndex()));
        assertEquals("Finished", model.getValueAt(0, JobColumn.STAT.getIndex()));
        assertEquals("Romaji", model.getValueAt(0, JobColumn.AROM.getIndex()));
        assertEquals("Kanji", model.getValueAt(0, JobColumn.AKAN.getIndex()));
        assertEquals("English", model.getValueAt(0, JobColumn.AENG.getIndex()));
        assertEquals("TV", model.getValueAt(0, JobColumn.ATYP.getIndex()));
        assertEquals("3", model.getValueAt(0, JobColumn.ENUM.getIndex()));
        assertEquals("Episode English", model.getValueAt(0, JobColumn.EENG.getIndex()));
        assertEquals("Episode Kanji", model.getValueAt(0, JobColumn.EKAN.getIndex()));
        assertEquals("Episode Romaji", model.getValueAt(0, JobColumn.EROM.getIndex()));
        assertEquals("Group", model.getValueAt(0, JobColumn.GNAM.getIndex()));
        assertEquals("GRP", model.getValueAt(0, JobColumn.GSHO.getIndex()));
        assertEquals("japanese", model.getValueAt(0, JobColumn.FDUB.getIndex()));
        assertEquals("english", model.getValueAt(0, JobColumn.FSUB.getIndex()));
        assertEquals("BluRay", model.getValueAt(0, JobColumn.FSRC.getIndex()));
        assertEquals("high", model.getValueAt(0, JobColumn.FQUA.getIndex()));
        assertEquals("1920x1080", model.getValueAt(0, JobColumn.FRES.getIndex()));
        assertEquals("H264", model.getValueAt(0, JobColumn.FVID.getIndex()));
        assertEquals("AAC", model.getValueAt(0, JobColumn.FAUD.getIndex()));
        assertEquals("", model.getValueAt(0, JobColumn.FMDS.getIndex()));
        assertEquals("", model.getValueAt(0, JobColumn.FMDA.getIndex()));
        assertEquals(2025, model.getValueAt(0, JobColumn.AYEN.getIndex()));
        assertSame(job, model.getValueAt(0, TableModelJobs.JOB));
    }

    @Test
    void absentAniDbMetadataUsesTypedDefaultsAndEmptyStrings() throws Exception {
        JobList jobs = new JobList();
        Job job = new Job(
                Files.createFile(temporaryDirectory.resolve("defaults.mkv")).toFile(), Job.FINISHED);
        job.setStatus(Job.FINISHED, false);
        jobs.add(job);
        TableModelJobs model = new TableModelJobs(jobs);

        assertEquals(0, model.getValueAt(0, JobColumn.FIDN.getIndex()));
        assertEquals(0L, model.getValueAt(0, JobColumn.FSIZ.getIndex()));
        assertEquals("N/A", model.getValueAt(0, JobColumn.AROM.getIndex()));

        AniDBFile partial = new AniDBFile(1);
        partial.setDubLanguage(null);
        job.anidbFile = partial;
        assertEquals("", model.getValueAt(0, JobColumn.FDUB.getIndex()));
        assertEquals("N/A", model.getValueAt(0, JobColumn.AROM.getIndex()));
    }

    @Test
    void modelMetadataLookupAndRowAdaptersRemainStable() throws Exception {
        JobList jobs = new JobList();
        Job job = completeJob();
        jobs.add(job);
        TableModelJobs model = new TableModelJobs(jobs);

        assertEquals(JobColumn.getColumnCount(), model.getColumnCount());
        assertEquals(Integer.class, model.getColumnClass(JobColumn.FIDN.getIndex()));
        assertEquals(String.class, model.getColumnClass(-1));
        assertEquals("File ID", model.getColumnName(JobColumn.FIDN.getIndex()));
        assertEquals("", model.getColumnName(-1));
        assertEquals(JobColumn.FIDN.getIndex(), model.getColumnIndex("fid"));
        assertEquals(-1, model.getColumnIndex("missing"));
        assertNull(model.getValueAt(0, 100));
        assertArrayEquals(new int[] {0}, model.convertRow(0));
        assertArrayEquals(new Job[] {job}, model.getJobs(0));
        int[] rows = {2, 0, 1};
        model.convertRows(rows);
        assertArrayEquals(new int[] {2, 0, 1}, rows);
    }

    @Test
    void rowUpdateAndInsertionPublishNarrowTableEvents() {
        TableModelJobs model = new TableModelJobs(new JobList());
        List<TableModelEvent> events = new ArrayList<>();
        model.addTableModelListener(events::add);

        model.updateRow(4);
        model.insertJob(2);

        assertEquals(2, events.size());
        assertEquals(4, events.get(0).getFirstRow());
        assertEquals(4, events.get(0).getLastRow());
        assertEquals(TableModelEvent.UPDATE, events.get(0).getType());
        assertEquals(2, events.get(1).getFirstRow());
        assertEquals(TableModelEvent.INSERT, events.get(1).getType());
    }

    private Job completeJob() throws Exception {
        Job job = new Job(
                Files.writeString(temporaryDirectory.resolve("complete.mkv"), "video")
                        .toFile(),
                Job.FINISHED);
        job.setStatus(Job.FINISHED, false);
        job.mylistId = 77;

        Anime anime = new Anime(10);
        anime.year = 2024;
        anime.endYear = 2025;
        anime.episodeCount = 12;
        anime.latestEpisode = 9;
        anime.romajiTitle = "Romaji";
        anime.kanjiTitle = "Kanji";
        anime.englishTitle = "English";
        anime.type = "TV";
        anime.categories = "Action";
        anime.init();
        Episode episode = new Episode(20);
        episode.num = "3";
        episode.eng = "Episode English";
        episode.kan = "Episode Kanji";
        episode.rom = "Episode Romaji";
        Group group = new Group(40);
        group.name = "Group";
        group.shortName = "GRP";

        AniDBFile file = new AniDBFile(30);
        file.setAnimeId(10);
        file.setEpisodeId(20);
        file.setGroupId(40);
        file.setLengthInSeconds(1440);
        file.setAnime(anime);
        file.setEpisode(episode);
        file.setGroup(group);
        file.setCrcHash("crc");
        file.setMd5Hash("md5");
        file.setShaHash("sha1");
        file.setDubLanguage("japanese");
        file.setSubLanguage("english");
        file.setRipSource("BluRay");
        file.setQuality("high");
        file.setResolution("1920x1080");
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        job.anidbFile = file;
        return job;
    }
}
