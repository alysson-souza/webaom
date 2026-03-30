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

import epox.webaom.AppContext;
import epox.webaom.Cache;
import epox.webaom.data.AniDBFile;
import epox.webaom.data.Anime;
import epox.webaom.data.Episode;
import javax.swing.SwingUtilities;
import javax.swing.tree.TreePath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JobTreeTableTest {
    private Cache originalCache;

    @BeforeEach
    void setUp() {
        originalCache = AppContext.cache;
        AppContext.cache = new Cache();
        AppContext.animeTreeRoot.clear();
        AppContext.animeTreeRoot.add(buildAnimeNode());
    }

    @AfterEach
    void tearDown() {
        AppContext.cache = originalCache;
        AppContext.animeTreeRoot.clear();
    }

    @Test
    void expandAndCollapse_preserveSelectionOnExpandedParentNode() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JobTreeTable treeTable = new JobTreeTable(new AlternateViewTableModel());

            assertEquals(2, treeTable.getRowCount());

            treeTable.setRowSelectionInterval(1, 1);
            TreePath selectedPath = treeTable.getTree().getPathForRow(1);

            treeTable.expandRow();

            assertEquals(3, treeTable.getRowCount());
            assertEquals(1, treeTable.getSelectedRow());
            assertEquals(selectedPath, treeTable.getTree().getSelectionPath());

            treeTable.collapseRow();

            assertEquals(2, treeTable.getRowCount());
            assertEquals(1, treeTable.getSelectedRow());
            assertEquals(selectedPath, treeTable.getTree().getSelectionPath());
        });
    }

    private Anime buildAnimeNode() {
        Anime anime = new Anime(1);
        anime.romajiTitle = "Anime 1";
        anime.type = "TV";
        anime.episodeCount = 1;
        anime.latestEpisode = 1;
        anime.init();

        Episode episode = new Episode(2);
        episode.num = "1";
        episode.eng = "Episode 1";

        AniDBFile file = new AniDBFile(3);
        file.setAnime(anime);
        file.setEpisode(episode);

        episode.add(file);
        anime.add(episode);
        return anime;
    }
}
