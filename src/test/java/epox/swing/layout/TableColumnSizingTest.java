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

package epox.swing.layout;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import javax.swing.JTable;
import javax.swing.table.TableColumnModel;
import org.junit.jupiter.api.Test;

class TableColumnSizingTest {
    @Test
    void calculatePreferredWidth_growsWithLargerFonts() {
        JTable smallTable = createTable(12);
        JTable largeTable = createTable(24);

        int smallWidth = TableColumnSizing.calculatePreferredWidth(
                smallTable, "File Path", 120, "/Volumes/Anime/Series Name - 01 [1080p].mkv");
        int largeWidth = TableColumnSizing.calculatePreferredWidth(
                largeTable, "File Path", 120, "/Volumes/Anime/Series Name - 01 [1080p].mkv");

        assertTrue(largeWidth > smallWidth);
    }

    @Test
    void calculatePreferredWidth_respectsMinimumWidth() {
        JTable table = createTable(12);

        int width = TableColumnSizing.calculatePreferredWidth(table, "Id", 140, "1");

        assertTrue(width >= 140);
    }

    @Test
    void scalePreferredWidths_scalesExistingColumnWidthsInPlace() {
        JTable table = new JTable(1, 2);
        TableColumnModel columnModel = table.getColumnModel();
        columnModel.getColumn(0).setPreferredWidth(100);
        columnModel.getColumn(1).setPreferredWidth(240);

        TableColumnSizing.scalePreferredWidths(columnModel, 1.5);

        assertTrue(columnModel.getColumn(0).getPreferredWidth() >= 150);
        assertTrue(columnModel.getColumn(1).getPreferredWidth() >= 360);
    }

    private JTable createTable(int fontSize) {
        JTable table = new JTable(1, 1);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.PLAIN, fontSize));
        }
        return table;
    }
}
