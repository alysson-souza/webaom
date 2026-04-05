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
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

/** Font-metric based helpers for scale-friendly default table column widths. */
public final class TableColumnSizing {
    private static final int DEFAULT_HORIZONTAL_PADDING = 24;
    private static final String FONT_SCALE_SAMPLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final FontRenderContext FONT_RENDER_CONTEXT = new FontRenderContext(null, true, true);

    private TableColumnSizing() {
        // utility class
    }

    public static void applyPreferredWidth(JTable table, TableColumn column, int minimumWidth, String... sampleValues) {
        int preferredWidth = calculatePreferredWidth(table, column.getHeaderValue(), minimumWidth, sampleValues);
        column.setPreferredWidth(preferredWidth);
    }

    public static void applyPreferredWidth(
            JTable table, TableColumn column, int minimumWidth, int additionalPadding, String... sampleValues) {
        int preferredWidth =
                calculatePreferredWidth(table, column.getHeaderValue(), minimumWidth, additionalPadding, sampleValues);
        column.setPreferredWidth(preferredWidth);
    }

    public static int calculatePreferredWidth(
            JTable table, Object headerValue, int minimumWidth, String... sampleValues) {
        return calculatePreferredWidth(table, headerValue, minimumWidth, DEFAULT_HORIZONTAL_PADDING, sampleValues);
    }

    public static int calculatePreferredWidth(
            JTable table, Object headerValue, int minimumWidth, int additionalPadding, String... sampleValues) {
        FontMetrics tableMetrics = table.getFontMetrics(resolveTableFont(table));
        FontMetrics headerMetrics = table.getFontMetrics(resolveHeaderFont(table));

        int contentWidth = calculateWidestText(tableMetrics, sampleValues);
        int headerWidth = calculateWidestText(headerMetrics, headerValue == null ? "" : headerValue.toString());
        return Math.max(Math.max(minimumWidth, contentWidth), headerWidth) + Math.max(0, additionalPadding);
    }

    public static void scalePreferredWidths(TableColumnModel columnModel, double scaleFactor) {
        List<TableColumn> columns = new ArrayList<>();
        Enumeration<TableColumn> enumeration = columnModel.getColumns();
        while (enumeration.hasMoreElements()) {
            columns.add(enumeration.nextElement());
        }
        scalePreferredWidths(columns, scaleFactor);
    }

    public static void scalePreferredWidths(Iterable<TableColumn> columns, double scaleFactor) {
        if (!(scaleFactor > 0.0) || Math.abs(scaleFactor - 1.0) < 0.01) {
            return;
        }

        for (TableColumn column : columns) {
            int currentWidth = Math.max(1, column.getPreferredWidth());
            column.setPreferredWidth(Math.max(1, (int) Math.round(currentWidth * scaleFactor)));
        }
    }

    public static double calculateFontScaleFactor(Font previousFont, Font updatedFont) {
        if (previousFont == null || updatedFont == null) {
            return 1.0;
        }

        double previousWidth = previousFont
                .getStringBounds(FONT_SCALE_SAMPLE, FONT_RENDER_CONTEXT)
                .getWidth();
        double updatedWidth = updatedFont
                .getStringBounds(FONT_SCALE_SAMPLE, FONT_RENDER_CONTEXT)
                .getWidth();
        if (!(previousWidth > 0.0) || !(updatedWidth > 0.0)) {
            return 1.0;
        }
        return updatedWidth / previousWidth;
    }

    private static Font resolveTableFont(JTable table) {
        return table.getFont() != null ? table.getFont() : new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    }

    private static Font resolveHeaderFont(JTable table) {
        JTableHeader header = table.getTableHeader();
        if (header != null && header.getFont() != null) {
            return header.getFont();
        }
        return resolveTableFont(table);
    }

    private static int calculateWidestText(FontMetrics fontMetrics, String... values) {
        int widest = 0;
        if (values == null) {
            return widest;
        }

        for (String value : values) {
            if (value == null) {
                continue;
            }
            String[] lines = value.split("\\R", -1);
            for (String line : lines) {
                widest = Math.max(widest, fontMetrics.stringWidth(line));
            }
        }
        return widest;
    }
}
