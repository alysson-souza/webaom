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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.JobCounter;
import epox.webaom.JobList;
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.data.Path;
import java.awt.Component;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlternateViewPanelCharacterizationTest {
    @TempDir
    java.nio.file.Path temporaryDirectory;

    private Cache originalCache;
    private JobList originalJobs;
    private JobCounter originalJobCounter;
    private String originalPathRegex;
    private Component originalSecondaryPopup;
    private int originalSortMode;

    @BeforeEach
    void installIsolatedState() {
        originalCache = AppContext.cache;
        originalJobs = AppContext.jobs;
        originalJobCounter = AppContext.jobCounter;
        originalPathRegex = AppContext.pathRegex;
        originalSecondaryPopup = AppContext.secondaryPopupMenu;
        originalSortMode = Cache.getTreeSortMode();
        AppContext.cache = mock(Cache.class);
        when(AppContext.cache.stats()).thenReturn("test-cache");
        AppContext.jobs = new JobList();
        AppContext.jobCounter = new JobCounter();
        AppContext.pathRegex = ".*Anime.*";
        Cache.setTreeSortMode(Cache.MODE_ANIME_EPISODE_FILE);
    }

    @AfterEach
    void restoreState() {
        AppContext.cache = originalCache;
        AppContext.jobs = originalJobs;
        AppContext.jobCounter = originalJobCounter;
        AppContext.pathRegex = originalPathRegex;
        AppContext.secondaryPopupMenu = originalSecondaryPopup;
        Cache.setTreeSortMode(originalSortMode);
    }

    @Test
    void panelInitializesControlsFromCurrentCacheAndPathState() {
        AtomicInteger actions = new AtomicInteger();
        AlternateViewPanel panel = new AlternateViewPanel(event -> actions.incrementAndGet());

        assertEquals(Cache.getTreeSortMode(), panel.getSortModeComboBox().getSelectedIndex());
        assertEquals(3, panel.getAnimeTitleComboBox().getItemCount());
        assertEquals(3, panel.getEpisodeTitleComboBox().getItemCount());
        assertEquals(3, panel.getFileVisibilityComboBox().getItemCount());
        assertEquals(".*Anime.*", panel.getPathRegexField().getText());
        assertFalse(panel.getLoadAllJobsCheckbox().isSelected());
        assertTrue(AppContext.secondaryPopupMenu instanceof JobContextMenu);

        panel.getAnimeTitleComboBox().setSelectedIndex(1);
        panel.getPathRegexField().postActionEvent();
        assertEquals(2, actions.get());
    }

    @Test
    void updateAlternativeViewRebuildsOnlyWhenRequested() {
        AlternateViewPanel panel = new AlternateViewPanel(event -> {});

        panel.updateAlternativeView(false);
        verify(AppContext.cache, times(0)).rebuildTree();

        panel.updateAlternativeView(true);
        verify(AppContext.cache).rebuildTree();
    }

    @Test
    void sizingMethodsKeepEveryTreeTableColumnUsable() {
        AlternateViewPanel panel = new AlternateViewPanel(event -> {});

        panel.scaleCurrentColumnWidths(1.25);
        panel.applyScaleAwareSizing();

        for (int index = 0; index < panel.getAltViewTreeTable().getColumnCount(); index++) {
            assertTrue(panel.getAltViewTreeTable()
                            .getColumnModel()
                            .getColumn(index)
                            .getPreferredWidth()
                    > 0);
        }
    }

    @Test
    void alternateModelMapsEachSupportedNodeType() throws Exception {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Anime anime = new Anime(1);
        anime.romajiTitle = "Series";
        anime.type = "TV";
        anime.year = 2025;
        anime.episodeCount = 2;
        anime.latestEpisode = 2;
        anime.init();
        anime.episodeProgress.set(1, true);
        anime.updateCompletionPercent();
        Episode episode = new Episode(2);
        episode.num = "1";
        AniDBFile file = new AniDBFile(3);
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");
        Job job = new Job(
                Files.createFile(temporaryDirectory.resolve("episode.mkv")).toFile(), Job.FINISHED);
        job.setStatus(Job.FINISHED, false);
        file.setJob(job);
        Group group = new Group(4);
        group.name = "Group";
        group.shortName = "G";
        AnimeGroup animeGroup = new AnimeGroup(anime, group);
        Path path = new Path("/media/anime");
        AniDBEntity generic = new AniDBEntity();
        generic.setTotalSize(2048);

        assertEquals("Series", model.getValueAt(anime, AlternateViewTableModel.NAME));
        assertEquals("TV", model.getValueAt(anime, AlternateViewTableModel.TYPE));
        assertEquals(2025, model.getValueAt(anime, AlternateViewTableModel.YEAR));
        assertEquals(50, model.getValueAt(anime, AlternateViewTableModel.PRCT));
        assertEquals(0, model.getValueAt(episode, AlternateViewTableModel.NUMB));
        assertEquals("Finished", model.getValueAt(file, AlternateViewTableModel.TYPE));
        assertEquals("H264", model.getValueAt(file, AlternateViewTableModel.YEAR));
        assertEquals("AAC", model.getValueAt(file, AlternateViewTableModel.NUMB));
        assertEquals(0, model.getValueAt(animeGroup, AlternateViewTableModel.NUMB));
        assertEquals(0, model.getValueAt(animeGroup, AlternateViewTableModel.PRCT));
        assertEquals(0, model.getValueAt(path, AlternateViewTableModel.NUMB));
        assertEquals("2.00 KB", model.getValueAt(generic, AlternateViewTableModel.SIZE));
        assertNull(model.getValueAt(file, AlternateViewTableModel.NAME));
        assertTrue(model.isLeaf(file));
        assertFalse(model.isLeaf(anime));
    }

    @Test
    void alternateModelBuildsSortedChildrenAndExposesColumnMetadata() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Anime parent = new Anime(1);
        Episode later = episode(2, "2");
        Episode earlier = episode(1, "1");
        parent.add(later);
        parent.add(earlier);

        assertEquals(2, model.getChildCount(parent));
        assertSame(earlier, model.getChild(parent, 0));
        assertEquals(7, model.getColumnCount());
        assertEquals("Name", model.getColumnName(AlternateViewTableModel.NAME));
        assertEquals(com.sun.swing.TreeTableModel.class, model.getColumnClass(AlternateViewTableModel.NAME));
    }

    private static Episode episode(int id, String number) {
        Episode episode = new Episode(id);
        episode.num = number;
        episode.eng = number;
        return episode;
    }
}
