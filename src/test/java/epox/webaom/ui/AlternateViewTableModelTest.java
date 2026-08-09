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

package epox.webaom.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import epox.swing.layout.TableColumnSizing;
import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.Job;
import epox.webaom.data.AniDBEntity;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.AnimeGroup;
import epox.webaom.data.Episode;
import epox.webaom.data.Group;
import epox.webaom.data.Path;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlternateViewTableModelTest {
    private Cache originalCache;

    @BeforeEach
    void setUp() {
        originalCache = AppContext.cache;
        AppContext.cache = new Cache();
        AppContext.animeTreeRoot.clear();
    }

    @AfterEach
    void tearDown() {
        AppContext.cache = originalCache;
        AppContext.animeTreeRoot.clear();
    }

    @Test
    void formatTable_scalesPreferredWidthsWithFontSize() {
        int smallWidth = formatNameColumnWidth(12);
        int largeWidth = formatNameColumnWidth(24);

        assertTrue(largeWidth > smallWidth);
    }

    @Test
    void formatTable_nameColumnIncludesExtraTreePadding() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        JobTreeTable table = createTable(model, 12);
        model.formatTable(table);

        int plainTextWidth = TableColumnSizing.calculatePreferredWidth(
                table,
                table.getColumnModel().getColumn(AlternateViewTableModel.NAME).getHeaderValue(),
                320,
                "Very Long Series Name - Episode 01 [BluRay 1080p][Dual Audio]");
        int actualWidth =
                table.getColumnModel().getColumn(AlternateViewTableModel.NAME).getPreferredWidth();

        assertTrue(actualWidth > plainTextWidth);
    }

    @Test
    void columnMetadataMatchesTheAlternateViewContract() {
        AlternateViewTableModel model = new AlternateViewTableModel();

        assertEquals(7, model.getColumnCount());
        assertEquals("Name", model.getColumnName(AlternateViewTableModel.NAME));
        assertEquals("%", model.getColumnName(AlternateViewTableModel.PRCT));
        assertEquals("M", model.getColumnName(AlternateViewTableModel.LAST));
        assertEquals("Type", model.getColumnName(AlternateViewTableModel.TYPE));
        assertEquals("Year", model.getColumnName(AlternateViewTableModel.YEAR));
        assertEquals("Number", model.getColumnName(AlternateViewTableModel.NUMB));
        assertEquals("Size", model.getColumnName(AlternateViewTableModel.SIZE));
        assertEquals(com.sun.swing.TreeTableModel.class, model.getColumnClass(AlternateViewTableModel.NAME));
        assertEquals(String.class, model.getColumnClass(AlternateViewTableModel.PRCT));
        assertEquals(Character.class, model.getColumnClass(AlternateViewTableModel.LAST));
        assertEquals(Integer.class, model.getColumnClass(AlternateViewTableModel.TYPE));
    }

    @Test
    void animeValuesExposeTitlesProgressShapeAndChildCount() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Anime anime = anime(1, "Series", 2024, 4);
        anime.episodeProgress.set(1, true);
        anime.episodeProgress.set(3, true);
        anime.updateCompletionPercent();
        anime.add(new AniDBFile(11));

        assertEquals("Series", model.getValueAt(anime, AlternateViewTableModel.NAME));
        assertEquals("TV", model.getValueAt(anime, AlternateViewTableModel.TYPE));
        assertEquals(2024, model.getValueAt(anime, AlternateViewTableModel.YEAR));
        assertEquals(1, model.getValueAt(anime, AlternateViewTableModel.NUMB));
        assertEquals(50, model.getValueAt(anime, AlternateViewTableModel.PRCT));
        assertEquals('A', model.getValueAt(anime, AlternateViewTableModel.LAST));
        assertNull(model.getValueAt(anime, AlternateViewTableModel.SIZE));
    }

    @Test
    void episodePathAndGenericEntityValuesExposeOnlyTheirSupportedColumns() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Episode episode = episode(2, "1");
        episode.add(new AniDBFile(21));
        Path path = new Path("/media/anime");
        path.add(new AniDBFile(22));
        AniDBEntity generic = new AniDBEntity();
        generic.setTotalSize(1024);

        assertEquals(1, model.getValueAt(episode, AlternateViewTableModel.NUMB));
        assertNull(model.getValueAt(episode, AlternateViewTableModel.NAME));
        assertEquals(1, model.getValueAt(path, AlternateViewTableModel.NUMB));
        assertNull(model.getValueAt(path, AlternateViewTableModel.NAME));
        assertEquals("1.00 KB", model.getValueAt(generic, AlternateViewTableModel.SIZE));
        assertNull(model.getValueAt(generic, AlternateViewTableModel.NAME));
    }

    @Test
    void fileValuesUseJobStatusAndCodecMetadata() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        AniDBFile file = new AniDBFile(3);
        file.setVideoCodec("H264");
        file.setAudioCodec("AAC");

        assertNull(model.getValueAt(file, AlternateViewTableModel.TYPE));
        Job job = mock(Job.class);
        when(job.getStatusText()).thenReturn("Finished");
        file.setJob(job);

        assertEquals("Finished", model.getValueAt(file, AlternateViewTableModel.TYPE));
        assertEquals("H264", model.getValueAt(file, AlternateViewTableModel.YEAR));
        assertEquals("AAC", model.getValueAt(file, AlternateViewTableModel.NUMB));
        assertNull(model.getValueAt(file, AlternateViewTableModel.NAME));
    }

    @Test
    void animeGroupValuesExposeCountAndCompletion() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Anime anime = anime(4, "Grouped", 2020, 4);
        Group group = new Group(5);
        AnimeGroup animeGroup = new AnimeGroup(anime, group);
        animeGroup.add(new AniDBFile(31));

        assertEquals(1, model.getValueAt(animeGroup, AlternateViewTableModel.NUMB));
        assertEquals(25, model.getValueAt(animeGroup, AlternateViewTableModel.PRCT));
        assertNull(model.getValueAt(animeGroup, AlternateViewTableModel.NAME));
    }

    @Test
    void rootAndUnknownValuesCharacterizeFallbackBehavior() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        AppContext.animeTreeRoot.add(anime(6, "Root child", 2022, 1));

        assertEquals(
                AppContext.animeTreeRoot.toString(),
                model.getValueAt(AppContext.animeTreeRoot, AlternateViewTableModel.NAME));
        assertEquals(1, model.getValueAt(AppContext.animeTreeRoot, AlternateViewTableModel.NUMB));
        assertNull(model.getValueAt(AppContext.animeTreeRoot, AlternateViewTableModel.TYPE));
        assertNull(model.getValueAt(new Object(), AlternateViewTableModel.NAME));
    }

    @Test
    void childAccessBuildsTheSortedViewAndRejectsNonEntityParents() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        Anime anime = anime(7, "Parent", 2021, 1);
        Episode episode = episode(8, "1");
        anime.add(episode);

        assertEquals(1, model.getChildCount(anime));
        assertSame(episode, model.getChild(anime, 0));
        assertNull(model.getChild("not an entity", 0));
        assertFalse(model.isLeaf(anime));
        assertTrue(model.isLeaf(new AniDBFile(9)));
    }

    @Test
    void formatTableCentersMetadataAndRightAlignsSizes() {
        AlternateViewTableModel model = new AlternateViewTableModel();
        JobTreeTable table = createTable(model, 12);

        model.formatTable(table);

        DefaultTableCellRenderer typeRenderer = (DefaultTableCellRenderer)
                table.getColumnModel().getColumn(AlternateViewTableModel.TYPE).getCellRenderer();
        DefaultTableCellRenderer sizeRenderer = (DefaultTableCellRenderer)
                table.getColumnModel().getColumn(AlternateViewTableModel.SIZE).getCellRenderer();
        assertEquals(SwingConstants.CENTER, typeRenderer.getHorizontalAlignment());
        assertEquals(SwingConstants.RIGHT, sizeRenderer.getHorizontalAlignment());
    }

    private int formatNameColumnWidth(int fontSize) {
        AlternateViewTableModel model = new AlternateViewTableModel();
        JobTreeTable table = createTable(model, fontSize);
        model.formatTable(table);
        return table.getColumnModel().getColumn(AlternateViewTableModel.NAME).getPreferredWidth();
    }

    private JobTreeTable createTable(AlternateViewTableModel model, int fontSize) {
        JobTreeTable table = new JobTreeTable(model);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        table.setFont(font);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(font);
        }
        return table;
    }

    private static Anime anime(int id, String title, int year, int episodeCount) {
        Anime anime = new Anime(id);
        anime.romajiTitle = title;
        anime.type = "TV";
        anime.year = year;
        anime.episodeCount = episodeCount;
        anime.latestEpisode = episodeCount;
        anime.init();
        return anime;
    }

    private static Episode episode(int id, String number) {
        Episode episode = new Episode(id);
        episode.num = number;
        episode.eng = "Episode " + number;
        return episode;
    }
}
