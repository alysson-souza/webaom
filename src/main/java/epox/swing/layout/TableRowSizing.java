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

import java.awt.Font;
import java.awt.FontMetrics;
import javax.swing.JTable;

/** Font-metric based row-height sizing shared by Swing tables in the application. */
public final class TableRowSizing {
    private static final int ROW_VERTICAL_PADDING = 3;

    private TableRowSizing() {
        // utility class
    }

    public static void applyPreferredRowHeight(JTable table, int minimumRowHeight) {
        Font font = table.getFont();
        if (font == null) {
            return;
        }

        FontMetrics fontMetrics = table.getFontMetrics(font);
        int preferredRowHeight =
                Math.max(fontMetrics.getHeight() + ROW_VERTICAL_PADDING, Math.max(1, minimumRowHeight));
        table.setRowHeight(preferredRowHeight);
    }
}
