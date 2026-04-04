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
import static org.junit.jupiter.api.Assertions.assertTrue;

import epox.swing.layout.TableColumnSizing;
import epox.util.ReplacementRule;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import org.junit.jupiter.api.Test;

class ReplacementTableModelTest {
    @Test
    void formatTable_scalesPreferredWidthsWithFontSize() {
        int smallWidth = formatSourceColumnWidth(12);
        int largeWidth = formatSourceColumnWidth(24);

        assertTrue(largeWidth > smallWidth);
    }

    @Test
    void formatTable_sizesEnabledColumnFromCheckboxRenderer() {
        ReplacementTableModel model =
                new ReplacementTableModel(new ArrayList<ReplacementRule>(), "Source", "Destination");
        JTable table = new JTable(model);

        ReplacementTableModel.formatTable(table);

        TableCellRenderer checkboxRenderer = table.getDefaultRenderer(Boolean.class);
        Component checkboxComponent = checkboxRenderer.getTableCellRendererComponent(
                table, Boolean.TRUE, false, false, 0, ReplacementTableModel.COLUMN_SELECTED);
        int enabledWidth = table.getColumnModel()
                .getColumn(ReplacementTableModel.COLUMN_SELECTED)
                .getPreferredWidth();
        int sourceWidth = table.getColumnModel()
                .getColumn(ReplacementTableModel.COLUMN_SOURCE)
                .getPreferredWidth();

        assertTrue(enabledWidth >= checkboxComponent.getPreferredSize().width);
        assertTrue(sourceWidth > enabledWidth);
    }

    @Test
    void scaleEnabledColumn_growsPreferredAndMaxWidthAfterFormatTable() {
        ReplacementTableModel model =
                new ReplacementTableModel(new ArrayList<ReplacementRule>(), "Source", "Destination");
        JTable table = new JTable(model);
        ReplacementTableModel.formatTable(table);

        TableColumn enabledColumn = table.getColumnModel().getColumn(ReplacementTableModel.COLUMN_SELECTED);
        int originalWidth = enabledColumn.getPreferredWidth();
        int originalMaxWidth = enabledColumn.getMaxWidth();
        assertEquals(originalWidth, originalMaxWidth, "formatTable should lock maxWidth to preferredWidth");

        // Simulate scaleCurrentColumnWidths: clear maxWidth, scale, re-lock
        enabledColumn.setMaxWidth(Integer.MAX_VALUE);
        TableColumnSizing.scalePreferredWidths(table.getColumnModel(), 2.0);
        int scaledWidth = enabledColumn.getPreferredWidth();
        enabledColumn.setMaxWidth(scaledWidth);

        assertTrue(scaledWidth > originalWidth, "preferred width should grow after 2x scale");
        assertEquals(scaledWidth, enabledColumn.getMaxWidth(), "maxWidth should be re-locked to scaled width");
    }

    private int formatSourceColumnWidth(int fontSize) {
        ReplacementTableModel model =
                new ReplacementTableModel(new ArrayList<ReplacementRule>(), "Source", "Destination");
        JTable table = new JTable(model);
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, fontSize);
        table.setFont(font);
        if (table.getTableHeader() != null) {
            table.getTableHeader().setFont(font);
        }
        ReplacementTableModel.formatTable(table);
        return table.getColumnModel()
                .getColumn(ReplacementTableModel.COLUMN_SOURCE)
                .getPreferredWidth();
    }
}
