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

import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.layout.TableColumnSizing;
import epox.webaom.AppContext;
import epox.webaom.Cache;
import java.awt.Font;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AlternateViewTableModelTest {
    private Cache originalCache;

    @BeforeEach
    void setUp() {
        originalCache = AppContext.cache;
        AppContext.cache = new Cache();
    }

    @AfterEach
    void tearDown() {
        AppContext.cache = originalCache;
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
}
