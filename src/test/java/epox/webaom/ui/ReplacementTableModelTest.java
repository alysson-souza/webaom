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

import epox.swing.layout.TableColumnSizing;
import epox.util.ReplacementRule;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
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

    @Test
    void metadataAndRowsExposeRulesPlusAnEmptyPlaceholder() {
        ReplacementRule rule = new ReplacementRule("source", "destination", true);
        List<ReplacementRule> rules = new ArrayList<>(List.of(rule));
        ReplacementTableModel model = new ReplacementTableModel(rules, "From", "To");

        assertSame(rules, model.getData());
        assertEquals(3, model.getColumnCount());
        assertEquals(2, model.getRowCount());
        assertEquals(Boolean.class, model.getColumnClass(ReplacementTableModel.COLUMN_SELECTED));
        assertEquals(String.class, model.getColumnClass(ReplacementTableModel.COLUMN_SOURCE));
        assertEquals("Enabled", model.getColumnName(ReplacementTableModel.COLUMN_SELECTED));
        assertEquals("From", model.getColumnName(ReplacementTableModel.COLUMN_SOURCE));
        assertEquals("To", model.getColumnName(ReplacementTableModel.COLUMN_DESTINATION));
        assertEquals("No such column!", model.getColumnName(99));
        assertTrue(model.isCellEditable(0, ReplacementTableModel.COLUMN_SOURCE));

        assertEquals(true, model.getValueAt(0, ReplacementTableModel.COLUMN_SELECTED));
        assertEquals("source", model.getValueAt(0, ReplacementTableModel.COLUMN_SOURCE));
        assertEquals("destination", model.getValueAt(0, ReplacementTableModel.COLUMN_DESTINATION));
        assertEquals(false, model.getValueAt(1, ReplacementTableModel.COLUMN_SELECTED));
        assertEquals("", model.getValueAt(1, ReplacementTableModel.COLUMN_SOURCE));
        assertEquals("", model.getValueAt(1, ReplacementTableModel.COLUMN_DESTINATION));
        assertNull(model.getValueAt(0, 99));
    }

    @Test
    void setDataReplacesTheBackingListByReference() {
        ReplacementTableModel model = new ReplacementTableModel(new ArrayList<>(), "From", "To");
        List<ReplacementRule> replacement = new ArrayList<>(List.of(new ReplacementRule("a", "b", false)));

        model.setData(replacement);

        assertSame(replacement, model.getData());
        assertEquals(2, model.getRowCount());
    }

    @Test
    void editingPlaceholderSourceAppendsARuleAndFiresAnInsertEvent() {
        List<ReplacementRule> rules = new ArrayList<>();
        ReplacementTableModel model = new ReplacementTableModel(rules, "From", "To");
        List<TableModelEvent> events = new ArrayList<>();
        TableModelListener listener = events::add;
        model.addTableModelListener(listener);

        model.setValueAt("new-source", 0, ReplacementTableModel.COLUMN_SOURCE);
        model.setValueAt("new-destination", 0, ReplacementTableModel.COLUMN_DESTINATION);
        model.setValueAt(true, 0, ReplacementTableModel.COLUMN_SELECTED);

        assertEquals(1, rules.size());
        assertEquals("new-source", rules.get(0).getSource());
        assertEquals("new-destination", rules.get(0).getDestination());
        assertTrue(rules.get(0).isEnabled());
        assertEquals(1, events.size());
        assertEquals(TableModelEvent.INSERT, events.get(0).getType());
        assertEquals(0, events.get(0).getFirstRow());
    }

    @Test
    void clearingAnExistingSourceRemovesTheRule() {
        ReplacementRule rule = new ReplacementRule("source", "destination", true);
        List<ReplacementRule> rules = new ArrayList<>(List.of(rule));
        ReplacementTableModel model = new ReplacementTableModel(rules, "From", "To");

        model.setValueAt("", 0, ReplacementTableModel.COLUMN_SOURCE);

        assertTrue(rules.isEmpty());
        assertEquals(1, model.getRowCount());
    }

    @Test
    void emptySourcesCannotBeEnabled() {
        ReplacementRule rule = new ReplacementRule("", "destination", false);
        ReplacementTableModel model = new ReplacementTableModel(new ArrayList<>(List.of(rule)), "From", "To");

        model.setValueAt(true, 0, ReplacementTableModel.COLUMN_SELECTED);

        assertFalse(rule.isEnabled());
    }

    @Test
    void destinationMatchingAnySourceIsSilentlyCleared() {
        ReplacementRule first = new ReplacementRule("alpha", "one", true);
        ReplacementRule second = new ReplacementRule("beta", "two", true);
        ReplacementTableModel model = new ReplacementTableModel(new ArrayList<>(List.of(first, second)), "From", "To");

        model.setValueAt("beta", 0, ReplacementTableModel.COLUMN_DESTINATION);

        // TODO: Review whether silently clearing a destination is clear enough feedback for users.
        assertEquals("", first.getDestination());

        model.setValueAt("gamma", 0, ReplacementTableModel.COLUMN_DESTINATION);
        assertEquals("gamma", first.getDestination());
    }

    @Test
    void editingPlaceholderSelectionCreatesAnEmptyBackingRule() {
        List<ReplacementRule> rules = new ArrayList<>();
        ReplacementTableModel model = new ReplacementTableModel(rules, "From", "To");

        model.setValueAt(true, 0, ReplacementTableModel.COLUMN_SELECTED);

        // TODO: Review why selecting the placeholder creates a disabled empty rule that remains in the model.
        assertEquals(1, rules.size());
        assertTrue(rules.get(0).hasEmptySource());
        assertFalse(rules.get(0).isEnabled());
        assertEquals(2, model.getRowCount());
    }

    @Test
    void unknownColumnEditLeavesTheRuleUnchanged() {
        ReplacementRule rule = new ReplacementRule("source", "destination", true);
        ReplacementTableModel model = new ReplacementTableModel(new ArrayList<>(List.of(rule)), "From", "To");

        model.setValueAt("ignored", 0, 99);

        assertEquals("source", rule.getSource());
        assertEquals("destination", rule.getDestination());
        assertTrue(rule.isEnabled());
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
