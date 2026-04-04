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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Font;
import javax.swing.JTable;
import org.junit.jupiter.api.Test;

class TableRowSizingTest {
    @Test
    void applyPreferredRowHeight_growsWithLargerFonts() {
        JTable table = new JTable(1, 1);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        TableRowSizing.applyPreferredRowHeight(table, 0);
        int smallRowHeight = table.getRowHeight();

        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
        TableRowSizing.applyPreferredRowHeight(table, 0);
        int largeRowHeight = table.getRowHeight();

        assertTrue(largeRowHeight > smallRowHeight);
    }

    @Test
    void applyPreferredRowHeight_honorsMinimumRowHeight() {
        JTable table = new JTable(1, 1);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        TableRowSizing.applyPreferredRowHeight(table, 40);

        assertEquals(40, table.getRowHeight());
    }
}
